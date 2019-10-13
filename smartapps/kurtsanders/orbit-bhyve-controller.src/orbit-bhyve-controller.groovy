/*
* Orbit™ B•Hyve™ Controller
* 2019 (c) SanderSoft™
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
import java.text.SimpleDateFormat;
import groovy.json.JsonOutput
import groovy.json.JsonSlurper


String appVersion()	 	{ return "4.00" }
String appModified() 	{ return "2019-10-13" }
String appDesc()		{"Support for Orbit Single and Multi-zone Timers"}

definition(
    name: 		"Orbit Bhyve Controller",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com",
    description:"Control and monitor your network connected Orbit™ Bhyve Timer anywhere via SmartThings®",
    category: 	"My Apps",
    iconUrl: 	getAppImg("icons/bhyve-b.jpg"),
    iconX2Url: 	getAppImg("icons/bhyve-b.jpg"),
    iconX3Url: 	getAppImg("icons/bhyve-b.jpg"),
    singleInstance: false
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
    if ( (username) && (password) ) {
        orbitBhyveLoginOK = OrbitBhyveLogin()
        log.debug "orbitBhyveLoginOK= ${orbitBhyveLoginOK}"
        def respdata = orbitBhyveLoginOK?OrbitGet("devices"):null
    }
    dynamicPage(name: "mainMenu",
                title: "Orbit B•Hyve™ Timer Account Login Information",
                nextPage: (orbitBhyveLoginOK)?"mainOptions":null,
                submitOnChange: true,
                install: false,
                uninstall: true)
    {
        if ( (username) && (password) ) {
            if (state?.orbit_api_key) {
                section("Orbit B•Hyve™ Information") {
                    paragraph "Your Login Information is Valid"
                    paragraph image : getAppImg("icons/success-icon.png"),
                        title: "Account name: ${state.user_name}",
                        required: false,
                        state.devices
                }
                section {
                    href(name: "Orbit B•Hyve™ Timer Options",
                         page: "mainOptions",
                         description: "Complete Orbit B•Hyve™ Options")
                }
            } else {
                section("Orbit B•Hyve™ Status/Information") {
                    paragraph "Your Login Information is INVALID"
                    paragraph image: getAppImg("icons/failure-icon.png"),
                        required: false,
                        title: "$state.statusText",
                        ""
                }
            }
        }
        section () {
            input ( name    : "username",
                   type     : "text",
                   title    : "Account userid?",
                   submitOnChange: true,
                   multiple : false,
                   required : true
                  )
            input ( name    : "password",
                   type     : "password",
                   title    : "Account password?",
                   submitOnChange: true,
                   multiple : false,
                   required : true
                  )
        }
        section ("STOrbitBhyveController™ - ${appAuthor()}") {
            href(name: "hrefVersions",
                 image: getAppImg("icons/bhyveIcon.png"),
                 title: "${version()} : ${appModified()}",
                 required: false,
                 style:"embedded",
                 url: githubCodeURL()
                )
        }

    }
}

def mainOptions() {
    def notifyList = []
    if (pushoverEnabled || sendPushEnabled || sendSMSEnabled) {
        notifyList = []
        if (pushoverEnabled) 	notifyList << "PushOver"
        if (sendPushEnabled) 	notifyList << "ST Client"
        if (sendSMSEnabled) 	notifyList << "SMS"
    }
    dynamicPage(name: "mainOptions",
                title: "Bhyve Timer Controller Options",
                install: true,
                uninstall: false)
    {
        section("Raspberry Pi Server Setup for Valve Control (Optional):") {
            input ("serverEnabled", "bool", title: "Use Raspberry Pi/Nodejs Service Integration", required: false, defaultValue: false, submitOnChange: true)
            if (serverEnabled) {
                input ("httpServerIP", "string", title: "Raspberry Pi/Nodejs IP:PORT", required: serverEnabled, submitOnChange: true)
                href name: "APIPageLink", title: "API Setup", required: serverEnabled, description: (state.endpoint)?"Setup Complete":"Tap to Enable" , page: "APIPage"
            }
        }
        section("Orbit Timer Refresh/Polling Update Interval") {
            input ( name: "schedulerFreq",
                   type: "enum",
                   title: "Run Bhyve Refresh Every (X mins)?",
                   options: ['0':'Off','1':'1 min','2':'2 mins','3':'3 mins','4':'4 mins','5':'5 mins','10':'10 mins','15':'15 mins','30':'Every ½ Hour','60':'Every Hour','180':'Every 3 Hours'],
                   required: true
                  )
        }
        section("Push Notification Options") {
            href(name: "Push Notification Options",
                 title: "Push Notification Options",
                 page: "notificationOptions",
                 description: "Notification Options",
                 defaultValue: (notifyList.size()>0)?notifyList.join(', '):"Tap to Select Notification Options")
        }

        section() {
            label ( name: "name",
                   title: "This SmartApp's Name",
                   state: (name ? "complete" : null),
                   defaultValue: "${app.name}",
                   required: false
                  )
        }
        section(hideable: true, hidden: true, "Optional: SmartThings IDE Live Logging Levels") {
            input ( name: "debugVerbose", type: "bool",
                   title: "Show Debug Messages in IDE",
                   description: "Verbose Mode",
                   required: false
                  )
            input ( name: "infoVerbose", type: "bool",
                   title: "Show Info Messages in IDE",
                   description: "Verbose Mode",
                   required: false
                  )
            input ( name: "errorVerbose", type: "bool",
                   title: "Show Error Info Messages in IDE",
                   description: "Verbose Mode",
                   required: false
                  )
        }
    }
}

def notificationOptions() {
    dynamicPage(name: "notificationOptions",
                title: "Bhyve Timer Controller Notification Options",
                install: false,
                uninstall: false)
    {
        section("Enable Pushover Service Support:") {
            input ("pushoverEnabled", "bool", title: "Use Pushover Service Integration", required: false, submitOnChange: true)
            if (pushoverEnabled) {
                input "pushoverUser", "string", title: "Enter Pushover User API Key", description: "Enter User API Key", required: pushoverEnabled, submitOnChange: true
                input "pushoverToken", "string", title: "Enter Pushover Application API Key", description: "Enter Application API Key", required: pushoverEnabled, submitOnChange: true
                if ((pushoverUser) && (pushoverToken)) {
                    input "pushoverDevices", "enum", title: "Select Pushover Devices", description: "Tap to select", options: findMyPushoverDevices(), submitOnChange: true, multiple: true, required: pushoverEnabled
                        }
                    }
        }

        section("SMS & Push Notifications for Timer On/Off activity?") {
            input ( name    : "sendSMSEnabled",
                   type     : "bool",
                   title    : "Send Events to ST Mobile Client Push Notification? (optional)",
                   required : false
                  )
            input ( name	: "sendSMSEnabled",
                   type: "bool",
                   title: "Use SMS for Notifications (optional)",
                   required: false,
                   submitOnChange: true
                  )
            if(sendSMSEnabled) {
                input ( name	: "phone",
                       type		: "phone",
                       title	: "Send Notification Events as SMS Text Messages (optional)",
                       description: "Enter Mobile Phone Number to Enable",
                       required: sendSMS?:false
                      )
            }
        }
        section("Event Notification Filtering") {
            input ( name	: "eventsToNotify",
                   type		: "enum",
                   title	: "Which Events",
                   options: ['battery':'Battery Low','connected':'Online/Offline','rain':'Rain Delay','valve':'Valve Open/Close'],
                   description: "Select Events to Notify",
                   required: false,
                   multiple: true
                  )

        }
    }
}

def disableAPIPage() {
	dynamicPage(name: "disableAPIPage", title: "", uninstall:false, install:false) {
		section() {
			if (state.endpoint) {
				try {
					revokeAccessToken()
				}
				catch (e) {
					log.debug "Unable to revoke access token: $e"
				}
				state.endpoint = null
			}
			paragraph "It has been done. Your token has been REVOKED. You're no longer allowed in API Town (I mean, you can always have a new token). Tap Done to continue."
		}
	}
}

def APIPage() {
    dynamicPage(name: "APIPage", title: "", uninstall:false, install:false) {
        section("API Setup") {
            if (state.endpoint) {
                // Added additional logging from @kurtsanders
                log.info "##########################################################################################"
                log.info "ST_SECRET=${state.endpointSecret}"
                log.info "ST_SMARTAPPURL=${state.endpointURL}"
                log.info "##########################################################################################"
                log.info "The API has been setup. Please enter the next two strings exactly as shown into the .env file which is in your Raspberry Pi's Bhyve app directory."
                log.info "##########################################################################################"
                paragraph "API has been setup. Please enter the following two strings in your '.env' file in your Raspberry Pi bhyve directory."
                paragraph "ST_SMARTAPPURL=${state.endpointURL}"
                paragraph "ST_SECRET=${state.endpointSecret}"
                href "disableAPIPage", title: "Disable API (Only use this if you want to generate a new secret)", description: ""
            }
            else {
                paragraph "Required: The API has not been setup. Tap below to enable it."
                href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"
            }
        }
    }
}

def enableAPIPage() {
    dynamicPage(name: "enableAPIPage",title: "", uninstall:false, install:false) {
        section() {
            if (initializeAppEndpoint()) {
                // Added additional logging from @kurtsanders
                log.info "##########################################################################################"
                log.info "ST_SECRET=${state.endpointSecret}"
                log.info "ST_SMARTAPPURL=${state.endpointURL}"
                log.info "##########################################################################################"
                log.info "The API has been setup. Please enter the next two strings exactly as shown into the .env file which is in your Raspberry Pi's Bhyve app directory."
                log.info "##########################################################################################"
                paragraph "Woo hoo! The API is now enabled and will be displayed in the next screen and posted in the ST Live Logging window after you save this SmartApp. Tap Done to continue"
            }
            else {
                paragraph "It looks like OAuth is not enabled. Please login to your SmartThings IDE, click the My SmartApps menu item, click the 'Edit Properties' button for the BitBar Output App. Then click the OAuth section followed by the 'Enable OAuth in Smart App' button. Click the Update button and BAM you can finally tap Done here.", title: "Looks like we have to enable OAuth still", required: true, state: null
            }
        }
    }
}

def initialize() {
    add_bhyve_ChildDevice()
    setScheduler(schedulerFreq)
    subscribe(app, appTouchHandler)
    if (eventsToNotify.contains('valve')) 		{subscribe(app.getChildDevices(), "valve", valveHandler)}
    if (eventsToNotify.contains('battery')) 	{subscribe(app.getChildDevices(), "battery", batteryHandler)}
    if (eventsToNotify.contains('rain')) 		{subscribe(app.getChildDevices(), "rain_delay", rain_delayHandler)}
    if (eventsToNotify.contains('connected')) 	{subscribe(app.getChildDevices(), "is_connected", is_connectedHandler)}
    runIn(5, main)
}

def updated() {
    unsubscribe()
    initialize()
}

def installed() {
    state?.isInstalled = true
    initialize()
}

def uninstalled() {
    log.info "Removing ${app.name}..."
    remove_bhyve_ChildDevice()
    if (state.endpoint) {
        try {
            log.debug "Revoking API access token"
            revokeAccessToken()
        }
        catch (e) {
            log.warn "Unable to revoke API access token: $e"
        }
    }
    log.info "Good-Bye..."
}

mappings {
    path("/event") {
        action: [
            POST: "webEvent"
        ]
    }
}

def webEvent() {
    Random random = new Random()
    def data = request.JSON
    if (data.containsKey("event")) {
        log.debug "=> webEvent #${random.nextInt(1000)}: '${data.event}'-> data : ${data}"
        switch(data.event) {
            case 'updatealldevices':
            runIn(5, "updateTiles", [data: data.webdata])
            break
            case 'change_mode':
            if (!data.containsKey("mode")) return
            def station = data.containsKey('stations')?data.stations.station:1
            def d = getChildDevice(DTHDNI("${data.device_id}:${stations}"))
            if (d) {
                d.sendEvent(name:"run_mode", value: data.mode, displayed: false)
            }
            break
            case 'low_battery':
            if (eventsToNotify.contains('battery')) {
                send_message("Check your Orbit watering device for a low battery condition")
            }
            break
            case 'watering_events':
            case 'program_changed':
            case 'watering_in_progress_notification':
            case 'watering_complete':
            runIn(5, "refresh")
            break
            case 'status':
            return allDeviceStatus()
            break
            default:
            log.debug "Skipping WebEvent ${data.event}"
            break
        }
}
    }

def allDeviceStatus() {
    def results = [:]
//    children.findAll { it.typeName }.sort{ a, b -> a.name <=> b.name }.each{
    app.getChildDevices().each{
        def d = getChildDevice(it.deviceNetworkId)
        def type = d.latestValue('type')
        if (!results.containsKey(type)) {
            results[type] = []
        }
        results[type].add(
            [
                name 						: it.name,
                type 						: it.typeName,
                valve						: d.latestValue('valve'),
                id							: d.latestValue('id'),
                manual_preset_runtime_min	: d.latestValue('manual_preset_runtime_min')
            ]
        )
        }
    return JsonOutput.toJson(results)
}

def appTouchHandler(evt="") {
    log.info "App Touch ${random()}: '${evt.descriptionText}' at ${timestamp()}"
    main()
    app.getChildDevices().each{
        def d = getChildDevice(it.deviceNetworkId)
        if (d.name == "Bhyve Back - Walkout") {
            def valveState = (d.latestValue('valve')=="closed")?"open":"closed"
            log.trace "${d.name} will be set to ${valveState}"
            d.sendEvent(name:"valve", value: valveState )
        }
    }
}

def sendRequest(valveState,device_id,zone,run_time) {
    if (serverEnabled) {
        def httpRequest = [
            method:		settings.httpMethod,
            path: 		settings.httpResource,
            query:		[
                'state'     : (valveState=='open')?'ON':'OFF',
                'device_id' : device_id,
                'zone'      : zone,
                'run_time'  : run_time
            ],
            headers:	[
                HOST:		httpServerIP,
                Accept: 	"*/*",
            ]
        ]
        runIn(10, "main")
        def hubAction = new physicalgraph.device.HubAction(httpRequest)
        sendHubCommand(hubAction)
    } else {
        runIn(1, "main")
    }
}

def makeHtmlColor(data,color='red') {
return "<font color='${color}'>${data}</font>"
}

def valveHandler(evt) {
    if (evt.isStateChange()) {
        def msgData = []
        msgData.add("The ${evt.linkText} ${evt.name} is now ${evt.value.toUpperCase()} at ${timestamp()}")
        msgData.add("The ${makeHtmlColor(evt.linkText)} ${makeHtmlColor(evt.name,'green')} is now ${makeHtmlColor(evt.value.toUpperCase())} at ${timestamp()}")
        send_message(msgData)
    }
}
def rain_delayHandler(evt) {
    if (evt.isStateChange()) {
        def msgData = []
        msgData.add("The ${evt.linkText}'s rain delay is now ${evt.value} hours at ${timestamp()}")
        msgData.add("The ${makeHtmlColor(evt.linkText)} ${makeHtmlColor(evt.name,'green')} is now ${makeHtmlColor(evt.value.toUpperCase())} hours at ${timestamp()}")
        send_message(msgData)
    }
}
def batteryHandler(evt) {
    if (evt.isStateChange() && (evt.value.toInteger() <= 40) ) {
        def msgData = []
        msgData.add("The ${evt.linkText}'s battery is now at ${evt.value}% at ${timestamp()}")
        msgData.add("The ${makeHtmlColor(evt.linkText)} ${makeHtmlColor(evt.name,'green')} is now at ${makeHtmlColor(evt.value.toUpperCase())}% at ${timestamp()}")
        send_message(msgData)
    }
}
def is_connectedHandler(evt) {
    if (evt.isStateChange()) {
        def msgData = []
        msgData.add("The ${evt.linkText}'s WiFi Online Status is now ${evt.value?'Online':'Offline'} at ${timestamp()}")
        msgData.add("The ${makeHtmlColor(evt.linkText)} WiFi ${makeHtmlColor(evt.name,'green')} is now ${makeHtmlColor(evt.value?'Online':'Offline')} at ${timestamp()}")
        send_message(msgData)
    }
}

def refresh() {
    debugVerbose("Executing Refresh Routine ID:${random()} at ${timestamp()}")
    main()
}

def main() {
    log.info "Executing Main Routine ID:${random()} at ${timestamp()}"
    def data = OrbitGet("devices")
    if (data) {
        updateTiles(data)
    } else {
        log.error "OrbitGet(devices): No data returned, Critical Error: Exit"
    }
}

def updateTiles(data) {
    Random random = new Random()
    debugVerbose("Executing updateTiles(data) #${random.nextInt(1000)} started...")
    def d
    def started_watering_station_at
    def banner
    def watering_events
    def watering_volume_gal
    def wateringTimeLeft
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
            log.info "Procesing Orbit Bridge: '${it.name}'"
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
            station = it.containsKey('zones')?it.zones[i].station:zone
            d = getChildDevice("${DTHDNI(it.id)}:${station}")
            if (d) {
                // log.info "NetworkId:${d.deviceNetworkId} -> (${d.name} is a ${deviceType} and last connected at: ${convertDateTime(it?.last_connected_at)}"

                // sendEvents calculated values for all device types
                d.sendEvent(name:"lastSTupdate", 	value: tileLastUpdated(), 					displayed: false)
                d.sendEvent(name:"schedulerFreq", 	value: schedulerFreq, 						displayed: false)
                // sendEvents for selected fields of the data record

                d.sendEvent(name:"lastupdate", 		value: "Station ${station} last connected at\n${convertDateTime(it.last_connected_at)}", displayed: false)
                d.sendEvent(name:"name", 			value: it.name, 							displayed: false)
                d.sendEvent(name:"type", 			value: it.type, 							displayed: false)
                d.sendEvent(name:"id",	 			value: it.id, 								displayed: false)
                d.sendEvent(name:"is_connected", 	value: it.is_connected, 					displayed: false)
                d.sendEvent(name:"icon",		 	value: it.num_stations, 					displayed: false)

                // Check for Orbit WiFi bridge
                if (deviceType == 'bridge') {
                    d.sendEvent(name:"firmware_version", value: it?.firmware_version, displayed: false)
                    d.sendEvent(name:"hardware_version", value: it?.hardware_version, displayed: false)
                    return
                }

                // Check for Orbit sprinkler_timer device
                if (deviceType == 'sprinkler_timer') {
                    zoneData 	= it.zones[i]
                    station 	= zoneData.station
                    scheduled_auto_on 	= true
                    d = getChildDevice("${DTHDNI(it.id)}:${station}")
                    log.info "Procesing Orbit Sprinkler Device: '${it.name}', Orbit Station #${station}, Zone Name: '${zoneData.name}'"

                    def byhveValveState = it.status.watering_status?"open":"closed"
                    d.sendEvent(name:"valve", 						value: byhveValveState )
					def presetWateringInt = (it.manual_preset_runtime_sec.toInteger()/60)
                    d.sendEvent(name:"presetRuntime", 				value: presetWateringInt, displayed: false )
                    d.sendEvent(name:"manual_preset_runtime_min", 	value: presetWateringInt, displayed: false )
                    d.sendEvent(name:"rain_delay", 					value: it.status.rain_delay )
                    d.sendEvent(name:"run_mode", 					value: it.status.run_mode, displayed: false)
                    d.sendEvent(name:"station", 					value: station, displayed: false)

                    next_start_programs = it.status.next_start_programs.join(', ').toUpperCase()
                    d.sendEvent(name:"next_start_programs", value: "Station ${station}: ${next_start_programs}", displayed: false)

                    d.sendEvent(name:"sprinkler_type", 		value: "${zoneData.num_sprinklers} ${zoneData.sprinkler_type}(s) ", displayed: false)

                    if (it.containsKey('battery')) {
                        d.sendEvent(name:"battery", 			value: it.battery.percent, displayed:false )
                        d.sendEvent(name:"battery_display", 	value: (Math.round(it.battery.percent.toInteger()/10.0) * 10).toString(), displayed:false )
                    } else {
                        d.sendEvent(name:"battery", 			value: 100   , displayed:false )
                        d.sendEvent(name:"battery_display", 	value: "100" , displayed:false )
                    }

                    // Check for System On/Off Mode for this device
                    if (it.scheduled_modes.containsKey('auto') && it.scheduled_modes.containsKey('off')) {
                        def dateFormat = (it.scheduled_modes.auto.annually==true)?"MMdd":"YYYYMMdd"
                        def todayDate 		= new Date().format(dateFormat, location.timeZone)
                        def begAutoAtDate 	= Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",it.scheduled_modes.auto.at).format(dateFormat)
                        def begOffAtDate 	= Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",it.scheduled_modes.off.at).format(dateFormat)
                        log.debug "${begAutoAtDate} ${todayDate} ${begOffAtDate}"
                        log.debug "begAutoAtDate<=todayDate && begOffAtDate>=todayDate = ${begAutoAtDate<=todayDate && begOffAtDate>=todayDate}"
                        if (!(begAutoAtDate<=todayDate && begOffAtDate>=todayDate)) {
                            scheduled_auto_on = false
                            d.sendEvent(name:"rain_icon", 		value: "sun", displayed: false )
                            d.sendEvent(name:"next_start_time", value: "System Auto Off Mode", displayed: false)
                            banner = "Next Start: System OFF until ${Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",it.scheduled_modes.auto.at).format("MMM-dd")}"
                            log.warn "${zoneData.name} ${banner}"
                        }
                    }
                    d.sendEvent(name:"scheduled_auto_on", value: scheduled_auto_on, displayed: false )
                    if (scheduled_auto_on) {
                    if (it.status.rain_delay > 0) {
                        d.sendEvent(name:"rain_icon", 			value: "rain", displayed: false )
                        def rainDelayDT = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time).format("yyyy-MM-dd'T'HH:mm:ssX", location.timeZone)
                        d.sendEvent(name:"next_start_time", value: durationFromNow(rainDelayDT), displayed: false)
                        banner = "${it.status.rain_delay}hr Rain Delay - ${convertDateTime(it.status.next_start_time)}"
                    } else {
                        d.sendEvent(name:"rain_icon", 		value: "sun", displayed: false )
                        def next_start_time_local = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time).format("yyyy-MM-dd'T'HH:mm:ssX", location.timeZone)
                        d.sendEvent(name:"next_start_time", value: durationFromNow(next_start_time_local), displayed: false)
                        banner = "Next Start: Pgm ${next_start_programs} - ${convertDateTime(it.status.next_start_time)}"
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
                                    it.frequency.days.each{
                                        dow << map[it]
                                    }
                                    freqMsg = "every ${dow.join(' & ')}"
                                    break
                                    default:
                                        freqMsg = "'${it.frequency.type}' freq type unknown"
                                    break
                                }
                                msgList << "${it.name} (${it.program.toUpperCase()}): ${y.run_time[0]} mins ${freqMsg} at ${start_timesList.join(' & ')}"
                                d.sendEvent(name:"start_times", value: "${start_timesList.join(' & ')}", displayed: false)
                            }
                        }
                        if (msgList.size()>0) {
                            d.sendEvent(name:"programs", value: "${zoneData.name} Programs\n${msgList.join(',\n')}", displayed: false)
                        } else {
                            d.sendEvent(name:"programs", value: "${zoneData.name} Programs\n${it.name}: None", displayed: false)
                        }
                    }
                    // Watering Events
                    watering_events = OrbitGet('watering_events', it.id)[0]
                    watering_events.irrigation = watering_events.irrigation[-1]?:0

                    if ((watering_events) && (byhveValveState == 'open')) {
                        def water_volume_gal = watering_events.irrigation.water_volume_gal?:0
                        started_watering_station_at = convertDateTime(it.status.watering_status.started_watering_station_at)
                        d.sendEvent(name:"water_volume_gal", value: water_volume_gal, descriptionText:"${water_volume_gal} gallons")
                        wateringTimeLeft = durationFromNow(it.status.next_start_time, "minutes")
                        wateringTimeLeft = durationFromNow(it.status.next_start_time, "minutes")
                        d.sendEvent(name:"level", value: wateringTimeLeft, descriptionText: "${wateringTimeLeft} minutes left till end" )
                        banner ="Active Watering - ${water_volume_gal} gals at ${timestamp('short') }"
                    } else {
                        d.sendEvent(name:"water_volume_gal"	, value: 0, descriptionText:"gallons", displayed: false )
                        d.sendEvent(name:"level"			, value: watering_events?.irrigation.run_time, displayed: false)
                    }
                    d.sendEvent(name:"banner", value: banner, displayed: false )
                }
            } else {
                log.error "Invalid Orbit Device ID: '${it.id}'. If you have added a NEW bhyve device, you must rerun the SmartApp setup to create a SmartThings device"
            }
        }
    }
}

def durationFromNow(dt,showOnly=null) {
    def endDate
    String rc
    def duration
    def dtpattern = dt.contains('Z')?"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'":"yyyy-MM-dd'T'HH:mm:ssX"
    if (dtpattern) {
        try {
            endDate = Date.parse(dtpattern, dt)
        } catch (e) {
            log.error "durationFromNow(): Error converting ${dt}: ${e}"
            return false
        }
    } else {
        log.error "durationFromNow(): Invalid date format for ${dt}"
        return false
    }
    def now = new Date()
    use (TimeCategory) {
        try {
            duration = (endDate - now)
        } catch (e) {
            log.error "TimeCategory Duration Error with enddate: '${endDate}' and  now(): '${now}': ${e}"
            rc = false
        }
    }
    if (duration) {
        rc = duration
//        log.debug "duration = ${duration}"
        switch (showOnly) {
            case 'minutes':
// 			log.debug "rc = ${rc}"
//            log.debug "(/\\d+(?=\\Wminutes)/) = ${(/\d+(?=\Wminutes)/)}"
            if (/\d+(?=\Wminutes)/) {
            def result = (rc =~ /\d+(?=\Wminutes)/)
//                log.debug "result[0] = ${result[0]}"
                return (result[0])
            } else {
            return (rc.replaceAll(/\.\d+/,''))
            }
            break
            default:
//                return (rc.replaceAll(/\.\d+/,'') )
                return (rc.replaceAll(/\.\d+/,'').split(',').length<3)?rc.replaceAll(/\.\d+/,''):(rc.replaceAll(/\.\d+/,'').split(',')[0,1].join())

        }
    }
    return rc
}


def tileLastUpdated() {
    return sprintf("%s Tile Last Refreshed on\n%s","${version()[0]}", timestamp('long',true))
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
        break
    }
    def tf = new java.text.SimpleDateFormat(formatstring)

    if (mobileTZ) {
        return datenow.format(formatstring, location.timeZone).replace("AM", "am").replace("PM","pm")
	} else {
        tf.setTimeZone(TimeZone.getTimeZone(state.timezone))
    }
        return tf.format(datenow).replace("AM", "am").replace("PM","pm")
}

def OrbitGet(command, device_id=null, mesh_id=null) {
    def respdata
    def params = [
        'uri'		: orbitBhyveLoginAPI(),
        'headers'	: OrbitBhyveLoginHeaders(),
    ]
    params.headers << ["orbit-api-key"	: state.orbit_api_key]
    switch (command) {
        case 'users':
        params.path = "${command}/${state.user_id}"
        break
        case 'user_feedback':
        case 'devices':
        params.path 	= "${command}"
        params.query 	= ["user_id": state.user_id]
        break
        case 'sprinkler_timer_programs':
        params.path 	= "${command}"
        params.query	= ["device_id" : device_id]
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
    //    log.debug "params = ${params}"
    try {
        httpGet(params) { resp ->
            // log.debug "response data: ${resp.data}"
            if(resp.status == 200) {
                respdata = resp.data
            } else {
                log.error "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ ${command}.  Data='${resp?.data}'"
                return null
            }
        }
    } catch (e) {
        log.error "OrbitGet($command): something went wrong: $e"
        return null
    }
    if (command=='devices') {
        def bridgeCount = respdata.type.count {it==typelist()[1]}
        def bridgeMsg = (bridgeCount==0)?'in your Orbit b•hyve™ account':"and ${bridgeCount} network bridge device(s)"
        state.devices = "Found ${respdata.type.count { it==typelist()[0]}} sprinkler timer(s) ${bridgeMsg}."
        state.timezone = respdata[0].timezone.timezone_id
    }
    return respdata
}

def OrbitBhyveLogin() {
    log.debug "Start OrbitBhyveLogin() ============="
    if ((username==null) || (password==null)) { return false }
    def params = [
        'uri'			: orbitBhyveLoginAPI(),
        'headers'		: OrbitBhyveLoginHeaders(),
        'path'			: "session",
        'body'			: web_postdata()
    ]
    try {
        httpPost(params) {
            resp ->
            if(resp.status == 200) {
                log.debug "HttpPost Login Request was OK ${resp.status}"
                state.orbit_api_key = "${resp.data?.orbit_api_key}"
                state.user_id 		= "${resp.data?.user_id}"
                state.user_name 	= "${resp.data?.user_name}"
                state.statusText	= "Success"
            }
            else {
                log.error "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ Login.  Data='${resp.data}'"
                state.orbit_api_key = null
                state.user_id 		= null
                state.user_name 	= null
                state.statusText = "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ Login.  Data='${resp.data}'"
                return false
            }
        }
    }
    catch (Exception e)
    {
        log.debug "Catch HttpPost Login Error: ${e}"
        state.orbit_api_key = null
        state.user_id 		= null
        state.user_name 	= null
        state.statusText = "Fatal Error for Orbit B•Hyve™ Login '${e}'"
        return false
    }

    log.debug "OrbitBhyveLogin(): End=========="
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
        schedule("${random(60)} 3/${schedulerFreq} * * * ?","refresh")
        break
        case '3':
        schedule("${random(60)} 3/${schedulerFreq} * * * ?","refresh")
        break
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
    if(schedulerFreq=='0'){
        infoVerbose("UNScheduled all RunEvery")
    } else {
        infoVerbose("Scheduled RunEvery${schedulerFreq}Minute")
    }
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
                log.info "Orbit device (${index}): ${it.name} is a ${it.type}-${it.hardware_version} with ${it.num_stations} stations, ${numZones} zone(s) and last connected at: ${convertDateTime(it.last_connected_at)}"
                for (i = 0 ; i < it.zones.size(); i++) {
                    data = [
                        DTHid 	: "${DTHDNI(it.id)}:${it.zones[i].station}",
                        DTHname : DTHName(it.type.split(" |-|_").collect{it.capitalize()}.join(" ")),
                        DTHlabel: "Bhyve ${it.zones[i].name}"
                    ]
                    createDevice(data)
                }
                break
                case 'bridge':
                data = [
                    DTHid	: 	"${DTHDNI(it.id)}:0",
                    DTHname	:	DTHName(it.type.split(" |-|_").collect{it.capitalize()}.join(" ")),
                    DTHlabel: 	"Bhyve ${it.name}"
                ]
                createDevice(data)
                break
                default:
                    log.error "Skipping: Unknown Orbit b•hyve deviceType '${it?.type}' for '${it?.name}'"
                    data = [:]
                break
            }
        }
    } else {
        return false
    }
    return true
}

def createDevice(data) {
    def d = getChildDevice(data.DTHid)
    if (d) {
        log.info "VERIFIED DTH: '${d.name}' is DNI:'${d.deviceNetworkId}'"
        return true
    } else {
        log.info "MISSING DTH: Creating a NEW Orbit device for '${data.DTHname}' device as '${data.DTHlabel}' with DNI: ${data.DTHid}"
        try {
            addChildDevice(DTHnamespace(), data.DTHname, data.DTHid, null, ["name": "${data.DTHlabel}", label: "${data.DTHlabel}", completedSetup: true])
        } catch(e) {
            log.error "The Device Handler '${data.DTHname}' was not found in your 'My Device Handlers', Error-> '${e}'.  Please install this DTH device in the IDE's 'My Device Handlers'"
            return false
        }
        log.info "Success: Added a new device named '${DTHlabel}' as DTH:'${DTHname}' with DNI:'${DTHid}'"
    }
    return true
}

def remove_bhyve_ChildDevice() {
    getAllChildDevices().each {
        log.debug "Deleting b•hyve™ device: ${it.deviceNetworkId}"
        try {
            deleteChildDevice(it.deviceNetworkId)
        }
        catch (e) {
            log.debug "${e} deleting the b•hyve™ device: ${it.deviceNetworkId}"
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
        break
    }
    return rc.format('EEE MMM d, h:mm a', timezone).replace("AM", "am").replace("PM","pm")
}

// Constant Declarations
def version()   				{ return ["${appVersion()}", appDesc()]}
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
String DTHDNI(id) 					{(id.startsWith('bhyve'))?id:"bhyve-${app.id}-${id}"}
String DTHnamespace()			{ return "kurtsanders" }
String appAuthor()	 			{ return "SanderSoft™" }
String githubCodeURL()			{ return "https://github.com/KurtSanders/STOrbitBhyveController#storbitbhyvecontroller"}
String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/beta/images/$imgName" }
String DTHName(type) 			{ return "Orbit Bhyve ${type}" }
String orbitBhyveLoginAPI() 	{ return "https://api.orbitbhyve.com/v1/" }
String web_postdata() 			{ return "{\n    \"session\": {\n        \"email\": \"$username\",\n        \"password\": \"$password\"\n    }\n}" }
Map OrbitBhyveLoginHeaders() 	{
    return [
        'orbit-app-id':'Orbit Support Dashboard',
        'Content-Type':'application/json'
    ]
}
List typelist() { return ["sprinkler_timer","bridge"] }

private initializeAppEndpoint() {
	if (!state.endpoint) {
		try {
			def accessToken = createAccessToken()
			if (accessToken) {
				state.endpoint = apiServerUrl("/api/token/${accessToken}/smartapps/installations/${app.id}/")
                state.endpointURL = apiServerUrl("/api/smartapps/installations/${app.id}/")
                state.endpointSecret = accessToken
			}
		}
		catch(e) {
			state.endpoint = null
		}
	}
	return state.endpoint
}


// ======= Pushover Routines ============

def send_message(String msgData) {
    if (sendPushEnabled) 	{sendPush(msgData)}
    if (sendSMSEnabled) 	{sendSms(phone, msgData)}
    if (pushoverEnabled) 	{sendPushoverMessage(msgData)}
}

def send_message(ArrayList msgData) {
    if (sendPushEnabled) 	{sendPush(msgData[0])}
    if (sendSMSEnabled) 	{sendSms(phone, msgData[0])}
    if (pushoverEnabled) 	{sendPushoverMessage(msgData[1])}
}

def sendPushoverMessage(msgData) {
    log.info "sendPushoverMessage() ${random()} at ${timestamp()}"
    Map params = [
        uri					: "https://api.pushover.net/1/messages.json",
        requestContentType	: "application/json"
    ]
    Map bodyx = [
        token			: pushoverToken.trim() as String,
        user			: pushoverUser.trim() as String,
        title			: app.name as String,
        message			: msgData as String,
        html			: 1,
        device			: pushoverDevices.join(',') as String
    ]
    params.body = new JsonOutput().toJson(bodyx)
    include 'asynchttp_v1'
    asynchttp_v1.post(pushoverResponse, params)
    return
}

def findMyPushoverDevices() {
    Boolean validated = false
    List pushoverDevices = []
    Map params = [
        uri: "https://api.pushover.net",
        path: "/1/users/validate.json",
        contentType: "application/json",
        requestContentType: "application/json",
        body: [token: pushoverToken.trim() as String, user: pushoverUser.trim() as String] as Map
    ]
    try {
        httpPostJson(params) { resp ->
            if(resp?.status != 200) {
                log.error "Received HTTP error ${resp.status}. Check your User and App Pushover keys!"
            } else {
                if(resp?.data) {
                    if(resp?.data?.status && resp?.data?.status == 1) validated = true
                    if(resp?.data?.devices) {
                        log.debug "Found (${resp?.data?.devices?.size()}) Pushover Devices..."
                        pushoverDevices = resp?.data?.devices
                    } else {
                        log.error "Device List is empty"
                        pushoverDevices ['No devices found, Check your User and App Pushover keys!']
                    }
                } else { validated = false }
            }
            log.debug "findMyPushoverDevices | Validated: ${validated} | Resp | status: ${resp?.status} | data: ${resp?.data}"
        }
    } catch (Exception ex) {
        if(ex instanceof groovyx.net.http.HttpResponseException && ex?.response) {
            log.error "findMyPushoverDevices HttpResponseException | Status: (${ex?.response?.status}) | Data: ${ex?.response?.data}"
        } else log.error "An invalid key was probably entered. PushOver Server Returned: ${ex}"
    }
    return pushoverDevices
}

def pushoverResponse(resp, data) {
    try {
        Map headers = resp?.getHeaders()
        def limit = headers["X-Limit-App-Limit"]
        def remain = headers["X-Limit-App-Remaining"]
        def resetDt = headers["X-Limit-App-Reset"]
        if(resp?.status == 200) {
            log.debug "Message Received by Pushover Server ${(remain && limit) ? " | Monthly Messages Remaining (${remain} of ${limit})" : ""}"
        } else if (resp?.status == 429) {
            log.warn "Couldn't Send Pushover Notification... You have reached your (${limit}) notification limit for the month"
        } else {
            if(resp?.hasError()) {
                log.error "pushoverResponse: status: ${resp.status} | errorMessage: ${resp?.getErrorMessage()}"
                log.error "Received HTTP error ${resp?.status}. Check your keys!"
            }
        }
    } catch (ex) {
        if(ex instanceof groovyx.net.http.HttpResponseException && ex?.response) {
            def rData = (ex?.response?.data && ex?.response?.data != "") ? " | Data: ${ex?.response?.data}" : ""
            log.error "pushoverResponse() HttpResponseException | Status: (${ex?.response?.status})${rData}"
        } else { log.error "pushoverResponse() Exception:", ex }
    }
}