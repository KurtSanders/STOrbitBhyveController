'use strict'
/*
 * app.js
 *
 * unofficial Orbit Bhyve API to MQTT gateway
 * Bill Church - https://github.com/billchurch/bhyve-mqtt
 * Kurt Sanders - https://github.com/KurtSanders/STOrbitBhyveController
 */

const Ajv = require('ajv');
const Orbit = require('./orbit');
const mqtt = require('mqtt');
const https = require('https');
const util = require('util');
const log = console.log;
const isEqual = require('lodash.isequal');
const dotenv = require('dotenv');
const myEnv = dotenv.config().parsed
const chalk = require('chalk');
const process = require('process');
const forEach = require('lodash.foreach');
console.log(process.env);
// const DEBUG = (/true/i).test(myEnv.ST_DEBUG)
// const TEST = (/true/i).test(myEnv.ST_TEST)
console.log(`process.env.ST_DEBUG = ${process.env.ST_DEBUG}`)
const DEBUG = (/true/i).test(process.env.ST_DEBUG)
const TEST = (/true/i).test(process.env.ST_TEST)
var DEVICEMAPLAST = {}
var MCLIENT_ONLINE = false
var REFRESHCOUNTER = 0
var LASTEVENT = {}
var LASTWATERING = {}
var MYDEVICES = {}

// Needed for PM2 Application http://pm2.keymetrics.io/docs/usage/application-declaration/
process.send = process.send || function () {};

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
log(chalk.bold.red('KurtSanders/STOrbitBhyveController\n'))
log(chalk.blue('b•hyve™ API Integration by Bill Church'))
log(chalk.green('SmartThings™ API Integration by SanderSoft™ 2019\n===================================================\n'))

log(chalk.green(`${ts()} - Setting a reoccurring 'Keep Connection alive Ping' to execute every ${process.env.ST_REFRESH_INTERVAL_SEC} seconds`))

log(chalk.blue("*".repeat(100)))
// Object.entries(myEnv).forEach(entry => {
forEach(process.env, (value, key) => {
//    let key = entry[0];
//    let value = entry[1];
    key.includes('PASSWORD') ? value = '*'.repeat(value.length) : ''    
//    log(chalk.blue(`${key.padEnd(Math.max(...Object.keys(myEnv).map(el => el.length)) + 2, '.')}: ${value}`))
    log(chalk.blue(`${key.padEnd(25,'.')}: ${value}`))
});
log(chalk.blue("*".repeat(100)))

const oClient = new Orbit()

const mClient = mqtt.connect(process.env.MQTT_BROKER_ADDRESS, {
    username: process.env.MQTT_USER,
    password: process.env.MQTT_PASSWORD,
    keepalive: 10000,
    connectTimeout: 120000,
    reconnectPeriod: 500,
    clientId: 'bhyve-mqtt_' + Math.random().toString(16).substr(2, 8)
})

function showData(data,jsonFlag=false) {
    if (jsonFlag) {
        data = JSON.stringify(data)
    }
    log(`${ts()} - ${data}`)
}

function refreshDevices() {
    log(`${ts()} - Auto Bhyve Device Refresh #${++REFRESHCOUNTER}`)
    oClient.devices()
//    setTimeout(refreshDevices, process.env.ST_REFRESH_INTERVAL_SEC * 1000)
}

process.on('SIGINT', function(data) {
    log(`\n${ts()} - Ctrl-C interrupt signal '${data} detected' - Shutting down`);
    process.exit()
});

const handleClientError = function(err) {
    console.error(`${ts()} - connection error to broker, exiting...`)
    console.error('    ' + err)
    process.exit()
}

mClient.on('error', handleClientError)

mClient.on('offline', function() {
    console.error(`${ts()} - MQTT BROKER ${process.env.MQTT_BROKER_ADDRESS} OFFLINE`)
    MCLIENT_ONLINE = false
    process.exit()
})

let publishHandler = function(err) {
    if (err) {
        return console.error(err)
    }
    if (DEBUG) log(`${ts()} - mqtt publish`)
}

// connect to oClient once mqtt is up:
mClient.on('connect', function() {
    if (DEBUG) log(`${ts()} - MQTT connected at ${process.env.MQTT_BROKER_ADDRESS}`)
    MCLIENT_ONLINE = true
    oClient.send_message(`${ts()} - MQTT connected at ${process.env.MQTT_BROKER_ADDRESS}`)
    process.send('ready')
    oClient.webserver()
    oClient.connect({
        email: process.env.ORBIT_EMAIL,
        password: process.env.ORBIT_PASSWORD
    })
//    setTimeout(refreshDevices, process.env.ST_REFRESH_INTERVAL_SEC * 1000)
})

// once we get a token, publish alive message
oClient.on('token', (token) => {
    if (MCLIENT_ONLINE) mClient.publish('bhyve/alive', ts(), publishHandler)
    if (DEBUG) log(`${ts()} - Token: ${token}`)
})

oClient.on('user_id', (userId) => {
    if (DEBUG) log(`${ts()} - user_id: ${userId}`)
    oClient.devices()
})

oClient.on('device_id', (deviceId) => {
    if (DEBUG) log(`${ts()} - device_id: ${deviceId}`)
})

oClient.on('waterValve', (dataObj) => {
    if (!DEBUG) log(`${ts()} - waterValve '==> '${JSON.stringify(dataObj)}'`)
    var message = JSON.stringify({ 
        "state": dataObj.state, 
        "time" : dataObj.run_time 
    })
    mClient.publish(`bhyve/device/${dataObj.device_id}/zone/${dataObj.zone}/set`,message)
    var msgData = `${ts()} - Turning ${MYDEVICES[dataObj.device_id]} Valve ${dataObj.state}`
    oClient.send_message(msgData)
    log(msgData)
})


oClient.on('restart',() => {
    if (!DEBUG) log(`${ts()} - Restarting WebSocket`)
    oClient.send_message(`${ts()} - Restarting WebSocket`)
    oClient.connectStream()
})

let subscribeHandler = function(topic) {
    if (DEBUG) log(`${ts()} - subscribe topic: ` + topic)
    mClient.subscribe(topic, (err, granted) => {
        if (err) {
            console.error(`mClient.subscribe ${topic} error:`)
            console.error('    ' + err)
        }
        if (DEBUG) log(`${ts()} -> granted: ` + JSON.stringify(granted))
    })
}

oClient.on('watering_events', (data) => {
    if (DEBUG) showData(data,true)
    data.event = "watering_events"
    var lastarray = data.irrigation.length - 1
    data.irrigation = data.irrigation[lastarray]
    
    if (!isEqual(data, LASTWATERING)) {
        if (TEST) {
            log(chalk.red(`${ts()} ${MYDEVICES[data.device_id]} -> FAKE POSTING Watering Updates to SmartThings API`))
        } else {
            log(chalk.green(`${ts()} ${MYDEVICES[data.device_id]} -> POSTING Watering Updates to SmartThings API`))
            log(chalk.red(`${ts()} ${MYDEVICES[data.device_id]} -> watering_events: ${JSON.stringify(data)}`))
            oClient.smartthings(data)
        }
    } else {
        log(chalk.blue(`${ts()} ${MYDEVICES[data.device_id]} -> No Change in Watering Events, SKIPPING POSTING watering event updates to SmartThings`))
    }

    LASTWATERING = data
})

oClient.on('devices', (data) => {
	var deviceActive = new Boolean(false)
    if (DEBUG) showData(data,true)
    if (MCLIENT_ONLINE) {
        let devices = []
        subscribeHandler(`bhyve/device/refresh`)
        subscribeHandler(`bhyve/command`)
        var deviceMap = {
        "event"     : "updatealldevices",
        "webdata"   : data
        } 

        for (let prop in data) {
            if (data.hasOwnProperty(prop)) {
                let deviceId = data[prop].id
                devices.push(deviceId)
                if (JSON.stringify(data[prop].type) == '"sprinkler_timer"') {
                MYDEVICES[deviceId] = data[prop].name
                    var lineout = util.format("%s %s %s%% %s %s",
                        `${ts()} - Name: ` + JSON.stringify(data[prop].name).padEnd(25, '.'),
                        ` Device ID: ` + JSON.stringify(data[prop].id),
                        ` Battery: ` + JSON.stringify(data[prop].battery.percent).padStart(3),
                        ` Rain Delay: ` + JSON.stringify(data[prop].status.rain_delay).padStart(5),
                        ` Watering State: ${data[prop].status.watering_status ? "open" : "closed"}`
                    )
                    
                    if (data[prop].status.watering_status) {
                        log(chalk.green(lineout))
                        oClient.watering_events(`${deviceId}`)
                        deviceActive = true
                        if (REFRESHCOUNTER == 0) {
                            log(chalk.green(`${ts()} - ${JSON.stringify(data[prop].name)} valve is reporting a 'OPEN' state`))
                            oClient.send_message(`${JSON.stringify(data[prop].name)} valve is reporting a 'OPEN' state`)
                        }
                    } else {
                        if (DEBUG)log(chalk.red(lineout))
                    }
                    
                    if (typeof data[prop].status.watering_status === 'object') {
                        mClient.publish(`bhyve/device/${deviceId}/status`, JSON.stringify(data[prop].status.watering_status))
                    } else {
                        mClient.publish(`bhyve/device/${deviceId}/status`, null)
                    }
                    if (!JSON.stringify(data[prop].status.watering_status)) {}
                    subscribeHandler(`bhyve/device/${deviceId}/refresh`)
                    mClient.publish(`bhyve/device/${deviceId}/details`, JSON.stringify(data[prop]), {
                        retain: true
                    })
                    for (let zone in data[prop].zones) {
                        let station = data[prop].zones[zone].station
                        mClient.publish(`bhyve/device/${deviceId}/zone/${station}`, JSON.stringify(data[prop].zones[zone]))
                        subscribeHandler(`bhyve/device/${deviceId}/zone/${station}/set`)
                    }
                }
            }
        }
        if (REFRESHCOUNTER == 0) {
            if (deviceActive==false) {
                log(chalk.red(`${ts()} - All ${Object.keys(MYDEVICES).length} devices are reporting a 'CLOSED' state'`))
                oClient.send_message(`${ts()} - All ${Object.keys(MYDEVICES).length} devices are reporting a 'CLOSED' state'`)
            }
        }
        
        mClient.publish(`bhyve/devices`, JSON.stringify(devices))
        if (!isEqual(deviceMap, DEVICEMAPLAST)) {
            if (TEST) {
                log(chalk.red(`${ts()} - All Devices -> FAKE POSTING Device Updates to SmartThings API`))
            } else {
                log(chalk.green(`${ts()} - All Devices -> POSTING Device Updates to SmartThings API`))
                oClient.smartthings(deviceMap)
            }
        } else {
            log(chalk.blue(`${ts()} - All Devices -> No Change in devices, SKIPPING POSTING device updates to SmartThings`))
        }

        DEVICEMAPLAST = deviceMap
        
//        log(`${ts()} - Waiting ${process.env.ST_REFRESH_INTERVAL_SEC} seconds..`)

        oClient.connectStream()
    }
})

const parseMessage = (topic, message) => {
    if (!DEBUG) log(`${ts()} - RECEIVED parseMessage topic: '${topic}' - message: '${message}'`)
    switch (topic) {
        case ('bhyve/command'):
            log(`${ts()} - ${topic} =>${message}<= received`)
            if (message.toString().toLowerCase() == 'quit') {
                log(`${ts()} - Exiting`)
                process.exit()
            } else {
                log(`${ts()} - UNKNOWN command "${message}"`)
                return
            }
            break
            // bhyve/device/{device_id}/zone/{station}/set
        case (topic.match(/bhyve\/device\/(.*)\/zone\/(\d)\/set/) || {}).input:
            let ajv = new Ajv()
            const cmdSchema = {
                'if': {
                    'properties': {
                        'state': {
                            'enum': ['ON', 'on']
                        }
                    }
                },
                'then': {
                    'required': ['time']
                },
                'properties': {
                    'time': {
                        'type': 'number',
                        'minimum': 1,
                        'maximum': 999
                    },
                    'state': {
                        'type': 'string',
                        'enum': ['ON', 'OFF', 'on', 'off']
                    }
                }
            }
            let JSONvalidate = ajv.compile(cmdSchema)
            const found = topic.match(/bhyve\/device\/(.*)\/zone\/(\d)\/set/)
            const deviceId = found[1]
            const station = Number(found[2])
            const command = JSON.parse(message.toString())
            let CMD_VALID = JSONvalidate(command)
            if (!CMD_VALID) {
                throw new Error(JSON.stringify(JSONvalidate.errors))
            }
            let myJSON = {}
            if (DEBUG) log(`${ts()} - deviceId: ` + deviceId + ' station: ' + station + ' command: ' + require('util').inspect(command))
            switch (command.state.toLowerCase()) {
                case 'on':
                    log(`${ts()} - is on`)
                    myJSON = {
                        'event': 'change_mode',
                        'mode': 'manual',
                        'device_id': deviceId,
                        'timestamp': ts(),
                        'stations': [{
                            'station': station,
                            'run_time': command.time
                        }]
                    }
                    break
                case 'off':
                    log(`${ts()} - is off`)
                    myJSON = {
                        'event': 'change_mode',
                        'device_id': deviceId,
                        'timestamp': ts(),
                        'mode': 'manual',
                        'stations': []
                    }
                    break
                default:
                    log(`${ts()} - is default`)
                    myJSON = {
                        'event': 'change_mode',
                        'device_id': deviceId,
                        'timestamp': ts(),
                        'mode': 'manual',
                        'stations': []
                    }
                    break
            }
            oClient.send(myJSON)
            if (DEBUG) log(`${ts()} - myJSON: ` + JSON.stringify(myJSON))
            break
            // bhyve/device/{device_id}/refresh
            // to do: refresh individual device instead of all
            // will require some work to orbit.js
        case (topic.match(/bhyve\/device\/(.*)\/refresh/) || {}).input:
        case 'bhyve/device/refresh':
            log(`${ts()} - refresh`)
            oClient.devices()
            break
        default:
            log(`${ts()} - default: ${topic}`)
            break
    }
}

mClient.on('message', (topic, message) => {
    log('topic: ' + topic + ' message: ' + message.toString())
    try {
        parseMessage(topic, message)
    } catch (e) {
        log(`${ts()} parseMessage ERROR: JSONvalidate failed: `)
        log('    validation error: ' + e)
        log('    client message: ' + message.toString())
    }
})

oClient.on('error', (err) => {
    log(`${ts()} - Orbit Error: ` + err)
})

const parseMode = (data) => {
    let mode = data.mode
    let stations = data.stations
    for (let station in stations) {
        log(`${ts()} ${MYDEVICES[data.device_id]} -> station: ` + data.stations[station].station + ' mode: ' + mode + ' run_time: ' + data.stations[station].run_time)
    }
}

oClient.on('st-message', (data) => {
    let event = data.event
    var ok2send = false
    var diffdata = {}
    switch (event) {
        case 'program_changed':
        case 'watering_complete':
        case 'watering_in_progress_notification':
        case 'change_mode':
        case 'low_battery':
            ok2send = true
            break
        default:
            if (DEBUG) console.error(`${ts()} ${MYDEVICES[data.device_id]} -> SKIPPING Event '${data.event}'`)
            break
    }

    if (ok2send) {
        if (!LASTEVENT[event]) {
            LASTEVENT[event] = {}
        }
        if (!isEqual(data, LASTEVENT[event])) {
            diffdata = diff(LASTEVENT[event], data)
            diffdata.device_id = data.device_id
            diffdata.event = data.event
            if (TEST) {
                log(chalk.red(`${ts()} ${MYDEVICES[data.device_id]} -> FAKE SENDING ${data.event} ${JSON.stringify(diffdata,null,0)} to SmartThings`))
            } else {
                log(chalk.green(`${ts()} ${MYDEVICES[data.device_id]} -> SENDING ${data.event} ${JSON.stringify(diffdata,null,0)} to SmartThings`))
                oClient.smartthings(data)
            }
            LASTEVENT[event] = data
        }
    }
})


oClient.on('message', (data) => {
    let event = data.event
    if (event != 'clear_low_battery') {
        if (!DEBUG) log(`${ts()} ${MYDEVICES[data.device_id]} -> message: ` + JSON.stringify(data, replacer, 0))
        if (MCLIENT_ONLINE) mClient.publish('bhyve/message', JSON.stringify(data))
        if (data == 'refresh') oClient.devices()
    }
})


function replacer(key, value) {
    // Filtering date values to correct local timezone
    if (/^\d{4}-\d{2}-\d{2}T\d{2}\:\d{2}\:\d{2}\.\d{3}Z$/.test(value)) {
        return (new Date(value).toLocaleDateString(
            'en-US', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: true,
                weekday: 'short'
            }
        ))
    } else if (value == null) {
        return false
    }
    return value;
}

/*!
 * Find the differences between two objects and push to a new object
 * (c) 2019 Chris Ferdinandi & Jascha Brinkmann, MIT License, https://gomakethings.com & https://twitter.com/jaschaio
 * @param  {Object} obj1 The original object
 * @param  {Object} obj2 The object to compare against it
 * @return {Object}      An object of differences between the two
 */
var diff = function(obj1, obj2) {

    // Make sure an object to compare is provided
    if (!obj2 || Object.prototype.toString.call(obj2) !== '[object Object]') {
        return obj1;
    }

    //
    // Variables
    //

    var diffs = {};
    var key;


    //
    // Methods
    //

    /**
     * Check if two arrays are equal
     * @param  {Array}   arr1 The first array
     * @param  {Array}   arr2 The second array
     * @return {Boolean}      If true, both arrays are equal
     */
    var arraysMatch = function(arr1, arr2) {

        // Check if the arrays are the same length
        if (arr1.length !== arr2.length) return false;

        // Check if all items exist and are in the same order
        for (var i = 0; i < arr1.length; i++) {
            if (arr1[i] !== arr2[i]) return false;
        }

        // Otherwise, return true
        return true;

    };

    /**
     * Compare two items and push non-matches to object
     * @param  {*}      item1 The first item
     * @param  {*}      item2 The second item
     * @param  {String} key   The key in our object
     */
    var compare = function(item1, item2, key) {

        // Get the object type
        var type1 = Object.prototype.toString.call(item1);
        var type2 = Object.prototype.toString.call(item2);

        // If type2 is undefined it has been removed
        if (type2 === '[object Undefined]') {
            diffs[key] = null;
            return;
        }

        // If items are different types
        if (type1 !== type2) {
            diffs[key] = item2;
            return;
        }

        // If an object, compare recursively
        if (type1 === '[object Object]') {
            var objDiff = diff(item1, item2);
            if (Object.keys(objDiff).length > 1) {
                diffs[key] = objDiff;
            }
            return;
        }

        // If an array, compare
        if (type1 === '[object Array]') {
            if (!arraysMatch(item1, item2)) {
                diffs[key] = item2;
            }
            return;
        }

        // Else if it's a function, convert to a string and compare
        // Otherwise, just compare
        if (type1 === '[object Function]') {
            if (item1.toString() !== item2.toString()) {
                diffs[key] = item2;
            }
        } else {
            if (item1 !== item2) {
                diffs[key] = item2;
            }
        }

    };


    //
    // Compare our objects
    //

    // Loop through the first object
    for (key in obj1) {
        if (obj1.hasOwnProperty(key)) {
            compare(obj1[key], obj2[key], key);
        }
    }

    // Loop through the second object and find missing items
    for (key in obj2) {
        if (obj2.hasOwnProperty(key)) {
            if (!obj1[key] && obj1[key] !== obj2[key]) {
                diffs[key] = obj2[key];
            }
        }
    }

    // Return the object of differences
    return diffs;

};