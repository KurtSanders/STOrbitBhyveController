/*
* Orbit™ B•Hyve™ Controller
* 2019 (c) SanderSoft™
* 2020 (c) Dominick Meglio (Hubitat port and significant rewrites)
*
* Author:   Kurt Sanders
* Email:	Kurt@KurtSanders.com
* Date:	    3/2017
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
*/
import groovy.time.*
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.transform.Field
import groovy.time.TimeCategory

@Field String apiUrl = "https://api.orbitbhyve.com/v1/"

definition(
    name: 		"Orbit Bhyve Controller",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com & Dominick Meglio",
    description:"Control and monitor your network connected Orbit™ Bhyve Timer anywhere via Hubitat",
    category: 	"My Apps",
    iconUrl: 	"",
    iconX2Url: 	"",
    iconX3Url: 	"",
    singleInstance: true
)
preferences {
    page(name:"mainMenu")
    page(name:"mainOptions")
    page(name:"notificationOptions")
    page(name:"APIPage")
    page(name:"enableAPIPage")
    page(name:"disableAPIPage")
}

def mainMenu() {
    def orbitBhyveLoginOK = false
    if (username && password)
        orbitBhyveLoginOK = OrbitBhyveLogin()
    dynamicPage(name: "mainMenu", title: "Orbit B•Hyve™ Timer Account Login Information", nextPage: orbitBhyveLoginOK ? "mainOptions" : null, install: false, uninstall: true)
    {
        if (username && password) {
            if (state?.orbit_api_key) {
                section("Orbit B•Hyve™ Information") {
                    paragraph "Your Login Information is Valid"
                    paragraph "Account name: ${state.user_name}"
                }
            } else {
                section("Orbit B•Hyve™ Status/Information") {
                    paragraph "Your Login Information is INVALID ${state.statusText}"
                }
            }
        }
        section {
            input name: "username", type: "text", title: "Username", required: true, submitOnChange: true
            input name: "password", type: "password", title: "Password", required: true, submitOnChange: true
        }
    }
}

def mainOptions() {
    dynamicPage(name: "mainOptions", title: "Bhyve Timer Controller Options", install: true, uninstall: false)
    {
        section("Orbit Timer Refresh/Polling Update Interval") {
            input name: "schedulerFreq", type: "enum", title: "Run Bhyve Refresh Every (X mins)?", required: true, options: ['0':'Off','1':'1 min','2':'2 mins','3':'3 mins','4':'4 mins','5':'5 mins','10':'10 mins','15':'15 mins','30':'Every ½ Hour','60':'Every Hour','180':'Every 3 Hours']
        }
        section("Push Notification Options") {
            href(name: "Push Notification Options",
                 title: "Push Notification Options",
                 page: "notificationOptions",
                 description: "Tap to Select Notification Options")
        }
        section(hideable: true, hidden: true, "Logging Settings") {
            input name: "logDebugMsgs", type: "bool", title: "Log debug messages"
            input name: "logInfoMsgs", type: "bool", title: "Log info messages"
        }
    }
}

def notificationOptions() {
    dynamicPage(name: "notificationOptions", title: "Bhyve Timer Controller Notification Options", install: false, uninstall: false)
    {
        section("Enable Notifications:") {
            input "notificationsEnabled", "bool", title: "Use Notifications", required: false, submitOnChange: true
            if (notificationsEnabled)
                input "notificationDevices", "capability.notification", description: "Device(s) to notify", multiple: true, required: notificationsEnabled
        }

        section("Event Notification Filtering") {
            input name: "eventsToNotify", type: "enum", title: "Which Events", multiple: true, options: ['battery':'Battery Low','connected':'Online/Offline','rain':'Rain Delay','valve':'Valve Open/Close']
        }
    }
}

def initialize() {
    add_bhyve_ChildDevice()
    setScheduler(schedulerFreq)
    if (eventsToNotify) {
        if (eventsToNotify.contains('valve')) 
            getChildDevices().each { subscribe(it, "valve", valveHandler)}
        if (eventsToNotify.contains('battery')) 
            getChildDevices().each { subscribe(it, "battery", batteryHandler)}
        if (eventsToNotify.contains('rain'))
            getChildDevices().each { subscribe(it, "rain_delay", rain_delayHandler)}
        if (eventsToNotify.contains('connected'))
            getChildDevices().each { subscribe(it, "is_connected", is_connectedHandler)}
    }
    runIn(5, main)
}

def updated() {
    unsubscribe()
    initialize()
}

def installed() {
    initialize()
}

def uninstalled() {
    remove_bhyve_ChildDevice()
}

def findMasterDevice() {
    return getChildDevices().find { 
        it.hasCapability("Initialize") && it.getDataValue("master") == "true"
    }
}

def sendRainDelay(device_id, hours) {
    def bhyveHub = findMasterDevice()
    bhyveHub?.sendRainDelay(device_id, hours)
}

def sendRequest(valveState, device_id, zone, run_time) {
    def bhyveHub = findMasterDevice()
    bhyveHub?.sendWSMessage(valveState, device_id, zone, run_time)
    runIn(10, "main")
}

def valveHandler(evt) {
    if (evt.isStateChange)
        send_message("The ${evt.linkText} ${evt.name} is now ${evt.value.toUpperCase()} at ${timestamp()}")
}
def rain_delayHandler(evt) {
    if (evt.isStateChange)
        send_message("The ${evt.linkText}'s rain delay is now ${evt.value} hours at ${timestamp()}")
}
def batteryHandler(evt) {
    if (evt.isStateChange && (evt.value.toInteger() <= 40) ) 
        send_message("The ${evt.linkText}'s battery is now at ${evt.value}% at ${timestamp()}")
}
def is_connectedHandler(evt) {
    if (evt.isStateChange)
        send_message("The ${evt.linkText}'s WiFi Online Status is now ${evt.value?'Online':'Offline'} at ${timestamp()}")
}

def refreshLastWateringAmount(device_id) {
    def mostRecentWatering = OrbitGet('watering_events', device_id)[0]

    if (mostRecentWatering != null) {
        def latestIrrigation = mostRecentWatering.irrigation[-1] ?: null
        if (latestIrrigation != null) {
            def wateringEventStationDev = getDeviceByIdAndStation(device_id, latestIrrigation.station)
            wateringEventStationDev.sendEvent(name: "last_watering_volume", value: latestIrrigation.water_volume_gal?:0, unit: "gal")
        }
    }
}

def refresh() {
    debugVerbose "Executing Refresh Routine at ${timestamp()}"
    main()
}

def main() {
    infoVerbose "Executing Main Routine at ${timestamp()}"
    def data = OrbitGet("devices")
    if (data) 
        updateTiles(data)
}

def updateTiles(data) {
    infoVerbose "Executing updateTiles(data) started..."
    def d
    def zoneData
    def zone
    def zones
    def station
    def next_start_programs
    def i
    def stp
    def scheduled_auto_on
    data.each {
        def deviceType = it.type
        switch (deviceType) {
            case 'bridge':
                infoVerbose "Processing Orbit Bridge: '${it.name}'"
                zone = 0
                zones = 1
                break
            case 'sprinkler_timer':
                zones = it.zones.size()
                stp = OrbitGet("sprinkler_timer_programs", it.id)
                break
            default:
                log.error "Invalid Orbit Device: '${it.type}' received from Orbit API...Skipping: ${it}"
                d = null
                break
        }
        for (i = 0 ; i < zones; i++) {
            station = it.containsKey('zones') ? it.zones[i].station : zone
            d = getChildDevice("${DTHDNI(it.id)}:${station}")
            if (d) {
                // sendEvents for selected fields of the data record
                d.sendEvent(name:"is_connected",value: it.is_connected)
                // Check for Orbit WiFi bridge
                if (deviceType == 'bridge') {
                    d.sendEvent(name:"firmware_version", value: it?.firmware_version)
                    d.sendEvent(name:"hardware_version", value: it?.hardware_version)
                    return
                }

                // Check for Orbit sprinkler_timer device
                if (deviceType == 'sprinkler_timer') {
                    zoneData = it.zones[i]
                    station = zoneData.station
                    scheduled_auto_on = true
                    d = getChildDevice("${DTHDNI(it.id)}:${station}")
                    infoVerbose "Processing Orbit Sprinkler Device: '${it.name}', Orbit Station #${station}, Zone Name: '${zoneData.name}'"
                    
                    if (it.status.watering_status) {
                        if (it.status.watering_status.stations != null) {
                            for (valveDevice in getValveDevices(it.id)) {
                                def deviceStationId = getStationFromDNI(valveDevice.deviceNetworkId)
                                if (it.status.watering_status.stations.find { s -> s.station.toInteger() == deviceStationId.toInteger()}) {
                                    if (valveDevice.currentValue("valve") != "open") {
                                        debugVerbose "Opening station ${deviceStationId}"
                                        valveDevice.sendEvent(name:"valve", value: "open")
                                    }
                                }
                                else {
                                    if (valveDevice.currentValue("valve") !="closed") {
                                        debugVerbose "Closed station ${deviceStationId}"
                                        valveDevice.sendEvent(name:"valve", value: "closed")
                                    }
                                }
                            }
                        }
                        else {
                            for (valveDevice in getValveDevices(it.id)) {
                                def deviceStationId = getStationFromDNI(valveDevice.deviceNetworkId)
                                if (it.status.watering_status.current_station.toInteger() == deviceStationId.toInteger()) {
                                    if (valveDevice.currentValue("valve") != "open") {
                                        debugVerbose "Opening station ${deviceStationId}"
                                        valveDevice.sendEvent(name:"valve", value: "open")
                                    }
                                }
                                else {
                                    if (valveDevice.currentValue("valve") != "closed") {
                                        debugVerbose "Closed station ${deviceStationId}"
                                        valveDevice.sendEvent(name:"valve", value: "closed")
                                    }
                                }
                            }
                        }
                    }
                    else {

                        getValveDevices(it.id).each {
                            if (it.currentValue("valve") != "closed")
                                it.sendEvent(name:"valve", value: "closed") 
                        }
                    }
                    
                    if (it.manual_preset_runtime_sec != null) {
                        def presetWateringInt = it.manual_preset_runtime_sec.toInteger()/60
                        d.sendEvent(name:"preset_runtime", value: presetWateringInt)
                        d.sendEvent(name:"manual_preset_runtime_min", value: presetWateringInt)
                    }
                    d.sendEvent(name:"rain_delay", value: it.status.rain_delay)
                    d.sendEvent(name:"run_mode", value: it.status.run_mode)
                    d.sendEvent(name:"station", value: station)

                    next_start_programs = it.status.next_start_programs?it.status.next_start_programs.join(', ').toUpperCase():''
                    d.sendEvent(name:"next_start_programs", value: "Station ${station}: ${next_start_programs}")

                    d.sendEvent(name:"sprinkler_type", value: "${zoneData.num_sprinklers>0?:'Unknown'} ${zoneData.sprinkler_type?zoneData.sprinkler_type+'(s)':''} ")

                    if (it.containsKey('battery') && it.battery != null) 
                        d.sendEvent(name:"battery", value: it.battery.percent)
                    else
                        d.sendEvent(name:"battery", value: 100)

                    // Check for System On/Off Mode for this device
                    if (it.scheduled_modes?.containsKey('auto') && it.scheduled_modes?.containsKey('off')) {
                        def dateFormat = (it.scheduled_modes?.auto?.annually==true)?"MMdd":"YYYYMMdd"
                        def todayDate = new Date().format(dateFormat, location.timeZone)
                        def begAutoAtDate = it.scheduled_modes?.auto?.at?Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",it.scheduled_modes.auto.at).format(dateFormat):''
                        def begOffAtDate = it.scheduled_modes?.off?.at?Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",it.scheduled_modes.off.at).format(dateFormat):''
                        if (!(begAutoAtDate<=todayDate && begOffAtDate>=todayDate)) {
                            scheduled_auto_on = false
                            d.sendEvent(name:"rain_icon", value: "sun")
                            d.sendEvent(name:"next_start_time", value: 0)
                        }
                    }
                    d.sendEvent(name:"scheduled_auto_on", value: scheduled_auto_on)
                    if (scheduled_auto_on) {
                        if (it.status.rain_delay > 0) {
                            d.sendEvent(name:"rain_icon", value: "rain")
                            def nextRun = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time)
                            def rainDelay = it.status.rain_delay
                            use (TimeCategory) {
                                nextRun = nextRun + rainDelay.hours
                            }
                            d.sendEvent(name:"next_start_time", value: nextRun.getTime())
                        } 
                        else {
                            d.sendEvent(name:"rain_icon", value: "sun")
                            def next_start_time_local = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time)
                            d.sendEvent(name:"next_start_time", value: next_start_time_local.getTime())
                        }
                    }

                    // Sprinkler Timer Programs
                    if (stp) {
                        def msgList = []
                        def start_timesList = []
                        def freqMsg
                        stp.findAll{it.enabled.toBoolean()}.each {
                            def y = it.run_times.findAll{it.station == station}
                            start_timesList = []
                            if (y) {
                                it.start_times.each {
                                    start_timesList << Date.parse("HH:mm",it).format("h:mm a").replace("AM", "am").replace("PM","pm")
                                }
                                switch (it.frequency.type) {
                                    case 'interval':
                                    freqMsg = "every ${it.frequency.interval} day(s)"
                                    break
                                    case 'odd':
                                    case 'even':
                                    freqMsg = "every ${it.frequency.type} day"
                                    break
                                    case 'days':
                                    def dow = []
                                    def map=[
                                        0:"Sunday",
                                        1:"Monday",
                                        2:"Tuesday",
                                        3:"Wednesday",
                                        4:"Thusday",
                                        5:"Friday",
                                        6:"Saturday"]
                                    it.frequency.days.each {
                                        dow << map[it]
                                    }
                                    freqMsg = "every ${dow.join(' & ')}"
                                    break
                                    default:
                                        freqMsg = "'${it.frequency.type}' freq type unknown"
                                    break
                                }
                                msgList << "${it.name} (${it.program.toUpperCase()}): ${y.run_time[0]} mins ${freqMsg} at ${start_timesList.join(' & ')}"
                                d.sendEvent(name:"start_times", value: "${start_timesList.join(' & ')}")
                            }
                        }
                        if (msgList.size()>0)
                            d.sendEvent(name:"programs", value: "${zoneData.name} Programs\n${msgList.join(',\n')}")
                        else
                            d.sendEvent(name:"programs", value: "${zoneData.name} Programs\n${it.name}: None")
                    }
                }
            } 
            else
                log.error "Invalid Orbit Device ID: '${it.id}'. If you have added a NEW bhyve device, you must rerun the App setup to create a device"
        }
    }
}

def timestamp(type='long', mobileTZ=false) {
    def formatstring
    Date datenow = new Date()
    switch(type){
        case 'short':
            formatstring = 'h:mm:ss a'
            break
        default:
            formatstring = 'EEE MMM d, h:mm:ss a'
    }
    def tf = new java.text.SimpleDateFormat(formatstring)

    if (mobileTZ) 
        return datenow.format(formatstring, location.timeZone).replace("AM", "am").replace("PM","pm")
	else 
        tf.setTimeZone(TimeZone.getTimeZone(state.timezone))

    return tf.format(datenow).replace("AM", "am").replace("PM","pm")
}

def OrbitGet(command, device_id=null, mesh_id=null) {
    def respdata
    def params = [
        uri		: apiUrl,
        contentType: "application/json",
        'headers'	: OrbitBhyveLoginHeaders(),
    ]
    params.headers << ["orbit-api-key"	: state.orbit_api_key]
    switch (command) {
        case 'users':
            params.path = "${command}/${state.user_id}"
            break
        case 'user_feedback':
        case 'devices':
            params.path = "${command}"
            params.query = ["user_id": state.user_id]
            break
        case 'sprinkler_timer_programs':
            params.path = "${command}"
            params.query = ["device_id" : device_id]
            break
        case 'zone_reports':
        case 'watering_events':
        case 'landscape_descriptions':
        case 'event_logs':
            params.path = "${command}/${device_id}"
            break
        case 'meshes':
            params.path = "${command}/${mesh_id}"
            break
        default :
            log.error "Invalid command '${command}' to execute:"
            return false
    }
    try {
        httpGet(params) { resp ->
            if(resp.status == 200) {
                respdata = resp.data
            } 
            else {
                log.error "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ ${command}.  Data='${resp?.data}'"
                return null
            }
        }
    } 
    catch (groovyx.net.http.HttpResponseException e) {
        def status = e.getResponse().status
        if  (status >= 400 && status <= 499) {
            log.error "Received a 4xx error, logging back in"
            OrbitBhyveLogin()
            return null
        } 
    }
    catch (Exception e) {
        log.error "OrbitGet($command): something went wrong: $e"
        return null
    }
    if (command=='devices') {
        def bridgeCount = respdata.type.count { it == "bridge" }
        def bridgeMsg = (bridgeCount==0)?'in your Orbit b•hyve™ account':"and ${bridgeCount} network bridge device(s)"
        state.devices = "Found ${respdata.type.count { it== "sprinkler_timer" }} sprinkler timer(s) ${bridgeMsg}."
        state.timezone = respdata[0].timezone.timezone_id
    }
    return respdata
}

def OrbitBhyveLogin() {
    if ((username==null) || (password==null)) 
        return false
    def params = [
        uri: apiUrl,
        headers: OrbitBhyveLoginHeaders(),
        path: "session",
        body: [
            session: [
                email: username,
                password: password
            ]
        ],
        requestContentType: "application/json",
        contentType: "application/json"
    ]
    try {
        httpPost(params) {
            resp ->
            if(resp.status == 200) {
                debugVerbose "HttpPost Login Request was OK ${resp.status}"
                state.orbit_api_key = "${resp.data?.orbit_api_key}"
                state.user_id = "${resp.data?.user_id}"
                state.user_name = "${resp.data?.user_name}"
                state.statusText = "Success"
            }
            else {
                log.error "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ Login.  Data='${resp.data}'"
                state.orbit_api_key = null
                state.user_id = null
                state.user_name = null
                state.statusText = "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ Login.  Data='${resp.data}'"
                return false
            }
        }
    }
    catch (Exception e)
    {
        log.error "Catch HttpPost Login Error: ${e}"
        state.orbit_api_key = null
        state.user_id = null
        state.user_name = null
        state.statusText = "Fatal Error for Orbit B•Hyve™ Login '${e}'"
        return false
    }
    return true
}

def setScheduler(schedulerFreq) {
    state.schedulerFreq = "${schedulerFreq}"

    switch(schedulerFreq) {
        case '0':
            unschedule()
            break
        case '1':
            runEvery1Minute(refresh)
            break
        case '2':
        case '3':
        case '4':
            schedule("${random(60)} 3/${schedulerFreq} * * * ?","refresh")
            break
        case '5':
            runEvery5Minutes(refresh)
            break
        case '10':
            runEvery10Minutes(refresh)
            break
        case '15':
            runEvery15Minutes(refresh)
            break
        case '30':
            runEvery30Minutes(refresh)
            break
        case '60':
            runEvery1Hour(refresh)
            break
        case '180':
            runEvery3Hours(refresh)
            break
        default :
            infoVerbose("Unknown Schedule Frequency")
            unschedule()
        return
    }
    if(schedulerFreq=='0')
        infoVerbose("UNScheduled all RunEvery")
     else
        infoVerbose("Scheduled RunEvery${schedulerFreq}Minute")
}

def random(int value=10000) {
    def runID = new Random().nextInt(value)
    return runID
}

def add_bhyve_ChildDevice() {
    def data = [:]
    def i
    def respdata = OrbitGet("devices")
    if (respdata) {
        respdata.eachWithIndex { it, index ->
            switch (it.type) {
                case 'sprinkler_timer':
                    def numZones = it.zones.size()
                    infoVerbose "Orbit device (${index}): ${it.name} is a ${it.type}-${it.hardware_version} with ${it.num_stations} stations, ${numZones} zone(s) and last connected at: ${convertDateTime(it.last_connected_at)}"
                    for (i = 0 ; i < it.zones.size(); i++) {
                        data = [
                            DTHid: "${DTHDNI(it.id)}:${it.zones[i].station}",
                            DTHname: DTHName(it.type.split(" |-|_").collect{it.capitalize()}.join(" ")),
                            DTHlabel: "Bhyve ${it.zones[i].name?:it.name}"
                        ]
                        def sprinkler = createDevice(data)
                        if (!findMasterDevice()) {
                            sprinkler.updateDataValue("master", "true")
                            sprinkler.initialize()
                        }
                        else if (sprinkler.getDataValue("master") != "true")
                            sprinkler.updateDataValue("master", "false")
                    }
                    break
                case 'bridge':
                    data = [
                        DTHid: 	"${DTHDNI(it.id)}:0",
                        DTHname:	DTHName(it.type.split(" |-|_").collect{it.capitalize()}.join(" ")),
                        DTHlabel: 	"Bhyve ${it.name}"
                    ]
                    createDevice(data)
                    break
                default:
                    log.error "Skipping: Unknown Orbit b•hyve deviceType '${it?.type}' for '${it?.name}'"
                    data = [:]
            }
        }
    } 
    else 
        return false
    return true
}

def createDevice(data) {
    def d = getChildDevice(data.DTHid)
    if (d) {
        infoVerbose "VERIFIED DTH: '${d.name}' is DNI:'${d.deviceNetworkId}'"
        return d
    } 
    else {
        infoVerbose "MISSING DTH: Creating a NEW Orbit device for '${data.DTHname}' device as '${data.DTHlabel}' with DNI: ${data.DTHid}"
        try {
            d = addChildDevice("kurtsanders", data.DTHname, data.DTHid, null, ["name": "${data.DTHlabel}", label: "${data.DTHlabel}", completedSetup: true])
        } 
        catch(e) {
            return null
        }
        infoVerbose "Success: Added a new device named '${data.DTHlabel}' as DTH:'${data.DTHname}' with DNI:'${data.DTHid}'"
    }
    return d
}

def remove_bhyve_ChildDevice() {
    getAllChildDevices().each {
        debugVerbose "Deleting b•hyve™ device: ${it.deviceNetworkId}"
        try {
            deleteChildDevice(it.deviceNetworkId)
        }
        catch (e) {
            debugVerbose "${e} deleting the b•hyve™ device: ${it.deviceNetworkId}"
        }
    }
}

def convertDateTime(dt) {
    def timezone = TimeZone.getTimeZone(state.timezone)
    def rc
    switch (dt) {
        case ~/.*UTC.*/:
            rc = dt
            break
        case ~/.*Z.*/:
            rc = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", dt)
            break
        default:
            rc = Date.parse("yyyy-MM-dd'T'HH:mm:ssX", dt)
    }
    return rc.format('EEE MMM d, h:mm a', timezone).replace("AM", "am").replace("PM","pm")
}


def debugVerbose(String message) {
    if (logDebugMsgs) {
        log.debug "${message}"
    }
}

def infoVerbose(String message) {
    if (logInfoMsgs) {
        log.info "${message}"
    }
}

def isDebugLogEnabled() {
    return logDebugMsgs
}

// Constant Declarations
String DTHDNI(id) 					{(id.startsWith('bhyve'))?id:"bhyve-${app.id}-${id}"}
String DTHName(type) 			{ return "Orbit Bhyve ${type}" }
Map OrbitBhyveLoginHeaders() 	{
    return [
        'orbit-app-id':'Orbit Support Dashboard'
    ]
}

def getValveDevices(id) {
    return getChildDevices().findAll { it.hasCapability("Valve") == true && getDeviceIdFromDNI(it.deviceNetworkId) == id}
}

def getStationFromDNI(dni) {
    dni?.split(':')[1]
}

def getDeviceIdFromDNI(dni) {
    dni?.split('-')[2]?.split(':')[0]
}
// ======= Push Routines ============

def send_message(String msgData) {
    if (notificationsEnabled)
        notificationDevices*.deviceNotification(msgData)
}

def send_message(ArrayList msgData) {
    if (notificationsEnabled)
        notificationDevices*.deviceNotification(msgData[1])
}

// ======= WebSocket Helper Methods =======
def getApiToken() {
    return state.orbit_api_key
}

def getDeviceById(deviceId) {
    return getChildDevices().findAll { getOrbitDeviceIdFromDNI(it.deviceNetworkId) == deviceId }
}

def getDeviceByIdAndStation(deviceId, station) {
    return getChildDevices().find { getOrbitDeviceIdFromDNI(it.deviceNetworkId) == deviceId && it.currentValue("station").toInteger() == station.toInteger() }
}

def triggerLowBattery(dev) {
    if (eventsToNotify.contains('battery')) 
        send_message("The battery is low in ${dev.displayName}")
}

def getOrbitDeviceIdFromDNI(dni) {
    return dni?.split('-')[2]?.split(':')[0]
}

