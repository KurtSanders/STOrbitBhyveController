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

String appVersion()	 	{ return "4.00" }
String appModified() 	{ return "2019-09-28" }
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
        log.debug "notifyList = ${notifyList}"
    }
    dynamicPage(name: "mainOptions",
                title: "Bhyve Timer Controller Options",
                install: true,
                uninstall: false)
    {
        section("API Setup - Optional") {
            href name: "APIPageLink", title: "API Setup (If Using Raspberry Pi API 4.0)", description: "This function has not been released yet", page: "APIPage"
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
        section("Enable Pushover Support:") {
            input ("pushoverEnabled", "bool", title: "Use Pushover Integration", required: false, submitOnChange: true)
            if(settings?.pushoverEnabled == true) {
                if(state?.isInstalled) {
                    if(!atomicState?.pushoverManager) {
                        paragraph "If this is the first time enabling Pushover than leave this page and come back if the devices list is empty"
                        pushover_init()
                    } else {
                        input "pushoverDevices", "enum", title: "Select Pushover Devices", description: "Tap to select", groupedOptions: getPushoverDevices(), multiple: true, required: false, submitOnChange: true
                        if(settings?.pushoverDevices) {
                            input "pushoverSound", "enum", title: "Notification Sound (Optional)", description: "Tap to select", defaultValue: "pushover", required: false, multiple: false, submitOnChange: true, options: getPushoverSounds()
                        }
                    }
                } else { paragraph "New Install Detected!!!\n\n1. Press Done to Finish the Install.\n2. Goto the Automations Tab at the Bottom\n3. Tap on the SmartApps Tab above\n4. Select ${app?.getLabel()} and Resume configuration", state: "complete" }
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
                   options: ['valves':'Watering','low_battery':'Low Battery','device_connected':'Device Connected'].sort(),
                   description: "Select Events to Notify",
                   defaultValue: eventsToNotify?:'Tap to Select',
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
    state[valveLastOpenEpoch] = [:]
    add_bhyve_ChildDevice()
    setScheduler(schedulerFreq)
    subscribe(app, appTouchHandler)
    runIn(15, main)
    pushover_init()
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
        log.debug "=> webEvent #${random.nextInt(1000)}: '${data.event}'-> data : ${(data.size()==2)?data.webdata:data}"
        switch(data.event) {
            case 'updatealldevices':
            runIn(2, "updateTiles", [data: data.webdata])
            break
            case 'watering_events':
            // data : [id:5d7cdad04f0cd2f6aa82f1d9, updated_at:2019-09-14T17:13:42.404Z, event:watering_events, precipitation:0, created_at:2019-09-14T12:19:28.417Z, device_id:5ba4d2694f0c7f7ff7b39480, date:2019-09-14T00:00:00.000Z, irrigation:[budget:100, status:complete, station:1, program:manual, water_volume_gal:33, program_name:manual, start_time:2019-09-14T17:03:42.000Z, run_time:10]]
            def i = data.irrigation
            def d = getChildDevice(DTHDNI("${data.device_id}:${i.station}"))
            if (d) {
            if (d.latestValue('valve')=='open') {
                    d.sendEvent(name:"water_volume_gal", 	value: "${i.water_volume_gal?:0}", descriptionText:"${i.water_volume_gal?:0} gallons")
                    d.sendEvent(name:"run_mode", 			value: "${i.program}", displayed: false)
                    d.sendEvent(name:"banner", 				value: "Active Watering - ${i.water_volume_gal?:0} gals at ${timestamp('short') }", displayed: false )
            } else {
                    d.sendEvent(name:"water_volume_gal", 	value: 0, descriptionText:"Gallons", displayed: false )
                    d.sendEvent(name:"level", 				value: i.run_time, displayed: false)
            }
            } else {
                log.error "Web watering_events: Invalid device DNI: ${data}"
            }
            break
            case 'low_battery':
            case 'device_connected':
            def d = getChildDevice(DTHDNI("${data.device_id}:1"))
            send_message(d,data.event.replaceAll("_"," ").toUpperCase())
            break
            case 'flow_sensor_state_changed':
            break
            case 'change_mode':
            // change_mode {"mode":"off","device_id":"5ba4d2694f0c7f7ff7b39480","event":"change_mode"}
            // change_mode {"event":"change_mode","delay":null,"mode":"off","device_id":"5ba4d2694f0c7f7ff7b39480"}
            // change_mode {"event":"change_mode","mode":"manual","program":null,"stations":[{"station":1,"run_time":2}],"device_id":"5ba4d2694f0c7f7ff7b39480"}
            def d = getChildDevice(DTHDNI("${data.device_id}:${data.stations.station?:1}"))
            if (d && data.containsKey("mode")) {
                d.sendEvent(name:"run_mode", value: data.mode, displayed: false)
            }
            break
            case 'watering_complete':
            // watering_complete {"event":"watering_complete","program":null,"current_station":null,"run_time":null,"started_watering_station_at":null,"rain_sensor_hold":null,"device_id":"5ba4d2694f0c7f7ff7b39480"}
            def d = getChildDevice(DTHDNI("${data.device_id}:${data.current_station?:1}"))
            d.sendEvent(name: "banner", value: "Watering Complete", "displayed":false)
            watering_battery_event(d,'closed')
            runIn(2, "refresh")
            d.sendEvent(name: "banner", value: "Cloud Refresh Requested..", "displayed":false)
            break
            case 'watering_in_progress_notification':
            // watering_in_progress_notification {"event":"watering_in_progress_notification","mode":null,"program":"manual","stations":null,"current_station":1,"run_time":1,"started_watering_station_at":"2019-09-09T00:05:50.000Z","rain_sensor_hold":false,"device_id":"5ba4d2694f0c7f7ff7b39480"}
            def d = getChildDevice(DTHDNI("${data.device_id}:${data.current_station?:1}"))
            watering_battery_event(d,'open')
            runIn(2, "refresh")
            break
            case 'program_changed':
            // {"event":"program_changed","program":{"pending_timer_ack":true,"name":"Pond","frequency":{"type":"interval","days":[2,5],"interval":5,"interval_start_time":"Sat, Sep 21, 2019, 08:00:00 PM"},"updated_at":"Tue, Sep 17, 2019, 01:30:46 PM","start_times":["13:00"],"id":"5d192c784f0c7d841e126850","budget":100,"device_id":"5ba4d2694f0c7f7ff7b39480","program":"a","run_times":[{"run_time":2,"station":1}],"enabled":true,"created_at":"Sun, Jun 30, 2019, 05:41:12 PM"},"lifecycle_phase":"update","timestamp":"Tue, Sep 17, 2019, 01:30:46 PM"}
            runIn(2, "refresh")
            break
            case 'rain_delay':
            // rain_delay {"event":"rain_delay","mode":null,"delay":0,"device_id":"5ba4d2694f0c7f7ff7b39480"}
            break
            case 'status':
            return allDeviceStatus()
            break
            default:
                def retMsg = "UNKNOWN device event '${webJSONdata?.event}' post_data: ${webJSONdata}"
                log.error retMsg
            return retMsg
            break
        }
    } else {
        def message = "WebData: The webData sent is an invalid JSON format, 'event' key missing"
        log.error message
        return message
    }
}

def send_message(d , event) {
    def message
    def durationTC
    String duration
    if (event.toLowerCase().contains('closed')) {
        Date lastOpen = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",state.valveLastOpenEpoch[d.deviceNetworkId])
        use (TimeCategory) {
            durationTC = new Date() - lastOpen
        }
        duration = durationTC
        message = "Orbit Byhve Timer: The ${d.name} has reported a '${event}' at ${timestamp()} and was actively watering for ${duration.replaceAll(/\.\d+/,'')}!"
    } else {
        message = "Orbit Byhve Timer: The ${d.name} has reported a '${event}' at ${timestamp()}!"
    }
    if (sendPushEnabled) 	sendPush(message)
    if (sendSMSEnabled) 	sendSms(phone, message)
    if (pushoverEnabled) 	sendPushoverMessage(message)
}

def sendPushoverMessage(data) {
    log.info "Pushover() ${random()} at ${timestamp()}"
    Map msgObj = [
        title: app.name, //Optional and can be what ever
        message: data, //Required (HTML markup requires html: true, parameter)
        priority: 0,  //Optional
        retry: 30, //Requried only when sending with High-Priority
        expire: 10800, //Requried only when sending with High-Priority
        html: false //Optional see: https://pushover.net/api#html
//        sound: settings?.pushoverSound //Optional
//        url: "", //Optional
//        url_title: "" //Optional
    ]
    /* buildPushMessage(List param1, Map param2, Boolean param3)
        Param1: List of pushover Device Names
        Param2: Map msgObj above
        Param3: Boolean add timeStamp
    */
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
}

def watering_battery_event(d,bhyve_valve_state=null,battery_percent=null) {
    def st_valve_state = d.latestValue('valve')
    if ( st_valve_state != bhyve_valve_state) {
        log.info "${d.name}: Valve state of '${st_valve_state}' CHANGED to '${bhyve_valve_state.toUpperCase()}'"
        d.sendEvent(name:"valve", value: bhyve_valve_state )
        send_message(d,"Valve: ${bhyve_valve_state.toUpperCase()}")
        if ((st_valve_state != bhyve_valve_state) && (bhyve_valve_state == 'open')) {
            state.valveLastOpenEpoch << ["${d.deviceNetworkId}" : now()]
        }
    }
    if (battery_percent) {
        d.sendEvent(name:"battery", value: battery_percent, displayed:false )
        d.sendEvent(name:"battery_display", value: (Math.round(battery_percent.toInteger()/10.0) * 10).toString(), displayed:false )
    }
    d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
}



def allDeviceStatus() {
    log.debug "allDeviceStatus(): Start Routine"
    def children = app.getChildDevices()
    log.debug "children = ${children}"
    def thisdevice
    def d
    def resp = []
    def map = []
    def id
    //    log.debug "This SmartApp '$app.name' has ${children.size()} b•hyve™ devices"
    thisdevice = children.findAll { it.typeName }.sort { a, b -> a.deviceNetworkId <=> b.deviceNetworkId }.each {
        d = getChildDevice(it.deviceNetworkId)
        if(d.latestValue('type')=="sprinkler_timer") {
            resp << [
                'deviceNetworkId': it.deviceNetworkId,
                'name'			: it.name,
                'valve'			: d.latestValue('valve'),
                'switchlevel'	: d.latestValue('level')
            ]
        }
    }
    return resp
}

def appTouchHandler(evt="") {
    log.info "App Touch ${random()}: '${evt.descriptionText}' at ${timestamp()}"
    def data = allDeviceStatus()
    log.debug "data = ${data}"
    data.each{k,v->
        log.info "{k}: ${v}"
    }
    return
    main()
    return
    OrbitGet("devices").each {
        def tz
        tz = TimeZone.getTimeZone(state.timezone)
        // tz = location.timeZone
        def date = new Date().format('EEE MMM d, h:mm a', tz)
        def next_start_time_local = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time).format('EEE MMM d, h:mm a', tz)

        def rainDelayDT = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time).format("yyyy-MM-dd'T'HH:mm:ssX", tz)
        log.debug "Local Time is ${date}. The sprinkler is rain delayed to ${next_start_time_local} which is ${durationFromNow(rainDelayDT)}"
    }
    return

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
    def byhveValveState
    def started_watering_station_at
    def banner
    def watering_events
    def watering_volume_gal
    def wateringTimeLeft
    def st_valve_state
    def zoneData
    def zone
    def zones
    def station
    def next_start_programs
    def i
    def stp
    data.each {
        def deviceType = it.type
        switch (deviceType) {
            case 'bridge':
            log.info "Procesing Orbit Bridge: '${it.name}'"
            zone = 0
            zones = 1
            break
            case 'sprinkler_timer':
            log.info "Procesing Orbit Sprinkler Device: '${it.name}'"
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
                d.sendEvent(name:"is_connected", 	value: it.is_connected, 					displayed: false)
                d.sendEvent(name:"icon",		 	value: it.num_stations, 					displayed: false)

                // Check for Orbit WiFi bridge
                if (deviceType == 'bridge') {
                    d.sendEvent(name:"firmware_version", value: it?.firmware_version, displayed: false)
                    d.sendEvent(name:"hardware_version", value: it?.hardware_version, displayed: false)
                }

                // Check for Orbit sprinkler_timer device
                if (deviceType == 'sprinkler_timer') {
                    zoneData 	= it.zones[i]
                    station 	= zoneData.station
                    d = getChildDevice("${DTHDNI(it.id)}:${station}")
                    log.info "Procesing Orbit Station #${station}: Zone Name: ${zoneData.name}"

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

                    if (it.status.rain_delay > 0) {
                        d.sendEvent(name:"rain_icon", 			value: "rain", displayed: false )
                        def rainDelayDT = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time).format("yyyy-MM-dd'T'HH:mm:ssX", location.timeZone)
                        d.sendEvent(name:"next_start_time", value: durationFromNow(rainDelayDT), displayed: false)
                        banner = "${it.status.rain_delay}hr Rain Delay - ${next_start_programs} ${convertDateTime(it.status.next_start_time)}"
                    } else {
                        d.sendEvent(name:"rain_icon", 		value: "sun", displayed: false )
                        def next_start_time_local = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it.status.next_start_time).format("yyyy-MM-dd'T'HH:mm:ssX", location.timeZone)
                        d.sendEvent(name:"next_start_time", value: durationFromNow(next_start_time_local), displayed: false)
                        banner = "Next Start: Pgm ${next_start_programs} - ${convertDateTime(it.status.next_start_time)}"
                    }

                    def testingMode = false
                    if (station == -1) {
                        testingMode = true
                        it.status.watering_status = [:]

                        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

                        String date = simpleDateFormat.format(new Date());

                        it.status.watering_status.started_watering_station_at = date
                        log.debug "it.status.watering_status.started_watering_station_at  = ${it.status.watering_status.started_watering_station_at }"
                    }

                    byhveValveState = it.status.watering_status?"open":"closed"
                    st_valve_state = d.latestValue('valve')
                    if (byhveValveState == 'open') {
                    if (!state.valveLastOpenEpoch) state.valveLastOpenEpoch = [:]
                        state.valveLastOpenEpoch["${d.deviceNetworkId}"] = it.status.watering_status.started_watering_station_at
                    }
                    if ( st_valve_state != byhveValveState) {
                        log.info "${d.name}: Valve state of '${st_valve_state}' CHANGED to '${byhveValveState.toUpperCase()}'"
                        d.sendEvent(name:"valve", 			value: byhveValveState )
                        if (byhveValveState == 'open') {
                            state.valveLastOpenEpoch["${d.deviceNetworkId}"] = it.status.watering_status.started_watering_station_at
                        }
                        send_message(d,"Valve: ${byhveValveState.toUpperCase()}")
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
                            d.sendEvent(name:"programs", value: "${zoneData.name} Programs\n${it.name}: None)", displayed: false)
                        }
                    }
                    // Watering Events
                    watering_events = OrbitGet('watering_events', it.id)[0]
                    watering_events.irrigation = watering_events.irrigation[-1]?:0

                    if ((watering_events) && (byhveValveState=='open')) {
                        def water_volume_gal = watering_events.irrigation.water_volume_gal?:0
                        started_watering_station_at = convertDateTime(it.status.watering_status.started_watering_station_at)
                        d.sendEvent(name:"water_volume_gal", value: water_volume_gal, descriptionText:"${water_volume_gal} gallons")
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
        log.debug "duration = ${duration}"
        switch (showOnly) {
            case 'minutes':
            log.debug rc =~ /\d+(?=\Wminutes)/
            return (rc =~ /\d+(?=\Wminutes)/)[0]
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

//PushOver-Manager Input Generation Functions
private getPushoverSounds(){return (Map) atomicState?.pushoverManager?.sounds?:[:]}
private getPushoverDevices(){List opts=[];Map pmd=atomicState?.pushoverManager?:[:];pmd?.apps?.each{k,v->if(v&&v?.devices&&v?.appId){Map dm=[:];v?.devices?.sort{}?.each{i->dm["${i}_${v?.appId}"]=i};addInputGrp(opts,v?.appName,dm);}};return opts;}
private inputOptGrp(List groups,String title){def group=[values:[],order:groups?.size()];group?.title=title?:"";groups<<group;return groups;}
private addInputValues(List groups,String key,String value){def lg=groups[-1];lg["values"]<<[key:key,value:value,order:lg["values"]?.size()];return groups;}
private listToMap(List original){original.inject([:]){r,v->r[v]=v;return r;}}
private addInputGrp(List groups,String title,values){if(values instanceof List){values=listToMap(values)};values.inject(inputOptGrp(groups,title)){r,k,v->return addInputValues(r,k,v)};return groups;}
private addInputGrp(values){addInputGrp([],null,values)}
//PushOver-Manager Location Event Subscription Events, Polling, and Handlers
public pushover_init(){subscribe(location,"pushoverManager",pushover_handler);pushover_poll()}
public pushover_cleanup(){state?.remove("pushoverManager");unsubscribe("pushoverManager");}
public pushover_poll(){sendLocationEvent(name:"pushoverManagerCmd",value:"poll",data:[empty:true],isStateChange:true,descriptionText:"Sending Poll Event to Pushover-Manager")}
public pushover_msg(List devs,Map data){if(devs&&data){sendLocationEvent(name:"pushoverManagerMsg",value:"sendMsg",data:data,isStateChange:true,descriptionText:"Sending Message to Pushover Devices: ${devs}");}}
public pushover_handler(evt){Map pmd=atomicState?.pushoverManager?:[:];switch(evt?.value){case"refresh":def ed = evt?.jsonData;String id = ed?.appId;Map pA = pmd?.apps?.size() ? pmd?.apps : [:];if(id){pA[id]=pA?."${id}"instanceof Map?pA[id]:[:];pA[id]?.devices=ed?.devices?:[];pA[id]?.appName=ed?.appName;pA[id]?.appId=id;pmd?.apps = pA;};pmd?.sounds=ed?.sounds;break;case "reset":pmd=[:];break;};atomicState?.pushoverManager=pmd;}
private buildPushMessage(List devices,Map msgData,timeStamp=false){if(!devices||!msgData){return};Map data=[:];data?.appId=app?.getId();data.devices=devices;data?.msgData=msgData;if(timeStamp){data?.msgData?.timeStamp=new Date().getTime()};pushover_msg(devices,data);}
