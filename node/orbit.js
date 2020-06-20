'use strict'
/*
 * orbit.js
 *
 * Orbit Bhyve API module for SmartThings STOrbitBhyveController
 * Bill Church - https://github.com/billchurch/bhyve-mqtt
 * Kurt Sanders - https://github.com/KurtSanders/STOrbitBhyveController  
 */
const axios = require('axios')
const EventEmitter = require('events').EventEmitter
const inherits = require('util').inherits
const WebSocket = require('ws')
const chalk = require('chalk');
const dotenv = require('dotenv');
const myEnv = dotenv.config().parsed

var tty = require('tty');

const log = console.log;
const get = require('lodash.get');

var PINGCOUNTER = 0
var MYDEVICES = {}
const PUSHOVER_MESSAGING =  (/true/i).test(process.env.PUSHOVER_MESSAGING) || false

function ts() {
    var options = {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true,
        weekday: 'short'
    };
    var d = new Date();
    return d.toLocaleDateString("en-US", options);
}

function Client() {
    if (!(this instanceof Client)) {
        return new Client()
    }
    EventEmitter.call(this)

    this.config = {
        wssURL: undefined,
        baseURL: undefined,
        timeout: undefined,
        email: undefined,
        password: undefined,
        debug: undefined
    }
    this._token = undefined
    this._user_id = undefined
    this._device_id = undefined
}
inherits(Client, EventEmitter)

// first step, get a token and generate an event on success or fail
Client.prototype.connect = function(cfg) {
    this.config.baseURL = cfg.baseURL || 'https://api.orbitbhyve.com'
    this.config.timeout = cfg.timeout || 10000
    this.config.email = cfg.email || undefined
    this.config.password = cfg.password || undefined
    this.config.debug = cfg.debug || false
    this.config.wssURL = cfg.wssURL || 'wss://api.orbitbhyve.com/v1/events'
    this.config.wsTimeout = cfg.wsTimeout || 30000
    this.config.debug = cfg.debug || false
    var self = this
    const getOrbitToken = () => {
        return new Promise((resolve, reject) => {
            const instance = axios.create({
                baseURL: self.config.baseURL,
                timeout: self.config.timeout
            })
            instance.post('/v1/session', {
                'session': {
                    'email': self.config.email,
                    'password': self.config.password
                }
            }).then(function(response) {
                self._token = response.data.orbit_session_token
                self._user_id = response.data.user_id
                // config for later sessions
                self._rest_config = {
                    baseURL: self.config.baseURL,
                    timeout: self.config.timeout,
                    headers: {
                        'orbit-session-token': self._token
                    }
                }
                if (self.config.debug) log(`${ts()} - response.data: ` + JSON.stringify(response.data))
                resolve(response)
            }).catch(function(err) {
                reject(err)
            })
        })
    }

    const doAccept = (response) => {
        if (self.config.debug) log(`${ts()} - token: ` + self._token + ' My UserID: ' + self._user_id)
        self.emit('token', self._token)
        self.emit('user_id', self._user_id)
    }
    const doReject = (err) => {
        if (self.config.debug) log(`${ts()} - error ` + err)
        self.emit('error', err)
    }

    let ost = getOrbitToken()
    ost.then(doAccept)
        .catch(doReject)
}

Client.prototype.send_message = function send_message(msgData, title = "Orbit Nodejs Proxy Server") {

    if (!PUSHOVER_MESSAGING) {return}
    
    log(chalk.yellow(`${ts()} - Sending PushOver '${msgData}'`))
    var self    = this
    var data = {
            token     : process.env.PUSHOVER_TOKEN,
            user      : process.env.PUSHOVER_USER,
            title     : title,
            message   : msgData
    }    
    const sendPromise = () => {
    return new Promise((resolve, reject) => {
        const instance = axios.create({
            baseURL: "https://api.pushover.net",
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 10000
        })
        instance.post(
                "/1/messages.json",
                JSON.stringify(data)
            )
            .then(function(response) {
            if (!self.config.debug)  log(chalk.yellow(`${ts()} - Pushover Message ${JSON.stringify(response.data)} sent successfully`));
            if (self.config.debug)  log(`${ts()} - Pushover response: ` + JSON.stringify(response.status))
                resolve(response)
            }).catch(function(err) {
                reject(err)
            })
        })
    }

    const doAccept = (response) => {
        if (!self.config.debug) log(chalk.blue(`${ts()} - Post Accepted Status: ${response.status}`))
    }
    const doReject = (err) => {
        log(`${ts()} - Pushover encountered a severe error: ${err}`);
        log(`${ts()} - ${err.stack}`);
        self.emit('error', err)
    }

    let ost = sendPromise()
    ost.then(doAccept)
        .catch(doReject)
}

 
Client.prototype.webserver = function() {
    const http = require('http')
    const port = process.env.WEBSERVER_PORT
    var self = this

    const requestHandler = (request, response) => {
        var requestData = require('url').parse(request.url,true)
        var queryData = get(requestData,'query',requestData)
        var objectsCount = Object.keys(queryData).length
        if (objectsCount) {
            var qd = JSON.stringify(queryData)
            log(chalk.magentaBright(`${ts()} - WEBSERVER DATA -> ${qd}`))
            response.end(`${ts()} - STOrbitBhyveController Node.js Server!\nReceived Data: ${qd}`)
            
            if ('state' in queryData) {
                var dataObj = {
                    'state'     : queryData.state.toUpperCase(),
                    'device_id' : queryData.device_id,
                    'timestamp' : ts(),
                    'zone'      : queryData.zone,
                    'run_time'  : parseInt(queryData.run_time,10)
                }
                log(chalk.red(`dataObj = ${JSON.stringify(dataObj)}`))
                self.emit('waterValve', dataObj)
            }
        }
    }

    const server = http.createServer(requestHandler)

    server.listen(port, (err) => {
      if (err) {
        log(`${ts()} - error ` + err)
        self.emit('error', err)      
        return 
      }
      log(chalk.red(`${ts()} - STOrbitBhyveController™ Webserver is listening on port: ${port}`))
//      if (PUSHOVER_MESSAGING) push.send('Orbit Server', `${ts()} - STOrbitBhyveController™ Webserver is listening on port: ${port}`)
    })
}

Client.prototype.smartthings = function(data) {
    var event = `${data.event.toUpperCase()}`
    var self = this
    
    if (data.hasOwnProperty('device_id')) {
        var deviceName = MYDEVICES[data.device_id].toUpperCase()
    } else {
        var deviceName = "All Orbit Devices"   
    }

    log(chalk.blue(`${ts()} - ${deviceName} -> Executing Client.prototype.smartthings for event: ${event}`))

    const getSmartThings = () => {
        const stsecret = "Bearer " + process.env.ST_SECRET
        var st_URLregex = process.env.ST_SMARTAPPURL.match(/^(http.*):443\/(api.*)(?<!\/$)/)

        var stbaseURL = st_URLregex[1]
        var stURLpath = st_URLregex[2] + '/event/'

        return new Promise((resolve, reject) => {
            const instance = axios.create({
                baseURL: stbaseURL,
                headers: {
                    'Authorization': stsecret,
                    'Content-Type': 'application/json'
                },
                timeout: 10000
            })
            instance.post(
                    stURLpath,
                    JSON.stringify(data)
                )
                .then(function(response) {
                    if (self.config.debug) log(`${ts()} - stresponse.data: ` + JSON.stringify(response.data))
                    resolve(response)
                }).catch(function(err) {
                    reject(err)
                })
        })
    }

    const doAccept = (response) => {
        if (!self.config.debug) log(chalk.blue(`${ts()} - ${deviceName} -> ST Post Accepted for ${event}`))
    }
    const doReject = (err) => {
        log(`${ts()} - ST Post RC Error with ${event} ${deviceName }: ` + err)
        self.emit('error', err)
    }

    let ost = getSmartThings()
    ost.then(doAccept)
        .catch(doReject)
}

Client.prototype.watering_events = function(device_id) {
    log(`${ts()} - Executing Client.prototype.watering_events for ${MYDEVICES[device_id]}`)
    var self = this
    const getWatering_Events = () => {
        return new Promise((resolve, reject) => {
            const instance = axios.create(self._rest_config)
            instance.get('/v1/watering_events/' + device_id)
                .then(function(response) {
                    if (self.config.debug) {
                        log(`${ts()} - ${JSON.stringify(response.data[0].irrigation[0].program_name).padEnd(25, '.')}: ${JSON.stringify(response.data[0].irrigation[0].water_volume_gal)} gals`)
                    }
                    self._device_id = response.data[0].device_id
                    resolve(response)
                }).catch(function(err) {
                    reject(err)
                })
        })
    }

    const doAccept = (response) => {
        if (!self.config.debug) log(`${ts()} - ST watering_events Accepted for ${MYDEVICES[device_id]}`)
        if (self.config.debug) log(`${ts()} - response.data: ` + JSON.stringify(response.data))
        self.emit('watering_events', response.data[0])
    }
    const doReject = (err) => {
        log(`${ts()} - watering_events error: ` + err)
        self.emit('error', `watering_events: ${err}`)
    }

    let WateringEvents = getWatering_Events()
    WateringEvents.then(doAccept)
        .catch(doReject)
}


Client.prototype.devices = function() {
    var self = this

    const getDevices = () => {
        return new Promise((resolve, reject) => {
            const instance = axios.create(self._rest_config)
            instance.get('/v1/devices?user_id=' + self._user_id)
                .then(function(response) {
                    if (self.config.debug) log(`${ts()} - response.data: ` + JSON.stringify(response.data))
                    self._device_id = response.data[0].id
                    for(let i = 0; i < response.data.length; i++){
                        MYDEVICES[response.data[i].id] = response.data[i].name
                    }
                    resolve(response)
                }).catch(function(err) {
                    reject(err)
                })
        })
    }

    const doAccept = (response) => {
        if (self.config.debug) log(`${ts()} - response.data: ` + JSON.stringify(response.data))
        self.emit('devices', response.data)
        self.emit('device_id', self._device_id)
    }
    const doReject = (err) => {
        if (self.config.debug) log(`${ts()} - error: ` + err)
        self.emit('error', err)
    }

    let Devices = getDevices()
    Devices.then(doAccept)
        .catch(doReject)
}

Client.prototype.send = function(message) {
    var self = this
    if (!self.config.debug) log('SEND JSON STRING TO MQTT: ' + JSON.stringify(message))
    self._stream.send(JSON.stringify(message))
}

Client.prototype.connectStream = function() {
    var self = this
    if (self.config.debug) log(`${ts()} - ` + 'Client.prototype.connectStream')
    if (PUSHOVER_MESSAGING) self.send_message(`${ts()} - Started WebSocket`);

    self._stream = new WebSocket(self.config.wssURL, {
        handshakeTimeout: self.config.wsTimeout
    })
    
    function printProgress(progress){
        var ttyFd = [1, 2, 4, 5].find(tty.isatty);
        if (ttyFd) {
            process.stdout.clearLine();
            process.stdout.cursorTo(0);
            process.stdout.write(progress);
        } else {
            log(progress)
        }
    }

    function sendPing() {
	    if (self.config.debug) log(chalk.green(`${ts()} - websocket.readyState (${self._stream.readyState}) sending keep alive ping #${++PINGCOUNTER}`))
	    printProgress(chalk.green(`${ts()} - websocket.readyState = (${self._stream.readyState}), sending keep alive ping #${++PINGCOUNTER}`));
	    
        try {
            if (self._stream.readyState==1) {
                self._stream.send('{"event":"ping"}')
            } else { 
                log(chalk.red(`${ts()} WebSocket readyState is CLOSED rc = ${self._stream.readyState}`))
                if (PUSHOVER_MESSAGING) self.send_message("NODEjs Servere Error WebSocket Closed");
                self.emit('restart')
//                process.exit(2)
            }
        } catch(e) {
            log (chalk.red(`${ts()} WebSocket ERROR - ${e}`))
            if (PUSHOVER_MESSAGING) self.send_message("NODEjs WebSocket Servere Error", `${ts()} - ${e}`);
        }

        setTimeout(sendPing, process.env.ST_REFRESH_INTERVAL_SEC * 1000)
    }

    const authenticate = () => {
        let message = {
            'event': 'app_connection',
            'orbit_session_token': self._token
        }
        if (self.config.debug) log(`${ts()} - websocket authenticate message: ` + JSON.stringify(message))
        self._stream.send(JSON.stringify(message))
        setTimeout(sendPing, process.env.ST_REFRESH_INTERVAL_SEC * 1000)
    }

    self._stream.on('open', authenticate)

    self._stream.on('message', function(data) {
        self.emit('message', JSON.parse(data))
        self.emit('st-message', JSON.parse(data))
    })

    self._stream.on('error', function(err) {
        self.emit('error', err)
    })

    self._stream.on('close', function(num, reason) {
        if (!self.config.debug) log(`${ts()} - close: ` + num + ' reason: ' + reason)
        if (PUSHOVER_MESSAGING) self.send_message("NODEjs Servere Error", `${ts()} - websocket stream close: ${num} reason: ${reason}`);
    })

    self._stream.on('ping', function(data) {
        if (!self.config.debug) log(`${ts()} - ping data: ` + data)
    })

    self._stream.on('unexpected-response', function(request, response) {
        console.error(`${ts()} - unexpected-response / request: ` + request + ' response: ' + response)
        if (PUSHOVER_MESSAGING) self.send_message("NODEjs Servere Error", `${ts()} - websocket stream - unexpected-response / request: ${request} response: ${response}`);
    })
}

module.exports = Client