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

// Start Version Information
def version()   { return ["V3.0", "Valve Open/Close Device Status & Monitoring, Rpi/Node"] }
// End Version Information

String appVersion()	 { return "3.0" }
String appModified() { return "2019-09-10" }

definition(
    name: 		"Orbit Bhyve Controller",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com",
    description:"Control and monitor your network connected Orbit™ Bhyve Timer anywhere via SmartThings®",
    category: 	"My Apps",
    iconUrl: 	getAppImg("icons/bhyveIcon.png"),
    iconX2Url: 	getAppImg("icons/bhyveIcon.png"),
    iconX3Url: 	getAppImg("icons/bhyveIcon.png"),
    singleInstance: true
)
preferences {
    page(name:"mainMenu")
    page(name:"mainOptions")
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
    dynamicPage(name: "mainOptions",
                title: "Bhyve Timer Controller Options",
                install: true,
                uninstall: false)
    {
        section("API Setup") {
            href name: "APIPageLink", title: "API Setup", description: "", page: "APIPage"
        }
        section("Spa Refresh Update Interval") {
            input ( name: "schedulerFreq",
                   type: "enum",
                   title: "Run Bhyve Refresh Every (X mins)?",
                   options: ['0':'Off','1':'1 min','2':'2 mins','3':'3 mins','4':'4 mins','5':'5 mins','10':'10 mins','15':'15 mins','30':'Every ½ Hour','60':'Every Hour','180':'Every 3 Hours'],
                   required: true
                  )
            mode ( title: "Limit Polling Bhyve to specific ST mode(s)",
                  image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                  required: false
                 )
        }
        section("SMS & Push Notifications for Timer On/Off activity?") {
            input ( name    : "sendPush",
                   type     : "bool",
                   title    : "Send Mobile Client Push Notification? (optional)",
                   required : false
                  )
            input ( name	: "phone",
                   type		: "phone",
                   title	: "Send SMS Text Messages (optional)",
                   description: "Mobile Phone Number",
                   required: false
                  )
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
                log.info "secret=${state.endpointSecret}"
                log.info "smartAppURL=${state.endpointURL}"
                log.info "##########################################################################################"
                log.info "The API has been setup. Please enter the next two strings exactly as shown into the .env file which is in your Raspberry Pi's Bhyve app directory."
                log.info "##########################################################################################"
                paragraph "API has been setup. Please enter the following two strings in your '.env' file in your Raspberry Pi bhyve directory."
                paragraph "smartAppURL=${state.endpointURL}"
                paragraph "secret=${state.endpointSecret}"
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
                log.info "secret=${state.endpointSecret}"
                log.info "smartAppURL=${state.endpointURL}"
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
    runIn(15, main)
}

def updated() {
    unsubscribe()
    initialize()
}

def installed() {
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
    def w = request.JSON
    def d
    log.debug "=> webEvent #${random.nextInt(1000)}: ${w.device_id?getChildDevice(DTHDNI(w.device_id)).name:''}: '${w?.event}'-> data: ${w.data?:w}"
    switch(w?.event) {
        case 'devices':
        /* 'devices'-> data: [
        [id:5b64e8344f0c7f7ff7af3619, rain_delay:0, watering_status:closed, battery:38, name:Front - Left Side],
        [id:5b6df1514f0c7f7ff7b00013, rain_delay:0, watering_status:closed, battery:31, name:Front - Center ],
        [id:5b7b488a4f0c7f7ff7b104d3, rain_delay:0, watering_status:closed, battery:41, name:Front - Right Side],
        [id:5b91523c4f0c7f7ff7b28295, rain_delay:0, watering_status:closed, battery:27, name:Front - Garage Side ],
        [id:5ba4d2694f0c7f7ff7b39480, rain_delay:0, watering_status:closed, battery:100, name:Back - Walkout]
        ]
        */
        w?.data.each {
            watering_battery_event(it.device_id,it.watering_status,it.battery)
}
        break
        case 'low_battery':
        case 'device_connected':
        send_message(w.device_id,w.event.replaceAll("_"," ").toUpperCase())
        break
        case 'flow_sensor_state_changed':
        break
        case 'change_mode':
        // change_mode {"mode":"off","device_id":"5ba4d2694f0c7f7ff7b39480","event":"change_mode"}
        // change_mode {"event":"change_mode","delay":null,"mode":"off","device_id":"5ba4d2694f0c7f7ff7b39480"}
        //change_mode {"event":"change_mode","mode":"manual","program":null,"stations":[{"station":1,"run_time":2}],"device_id":"5ba4d2694f0c7f7ff7b39480"}
        d = getChildDevice(DTHDNI(w.device_id))
        if (d) {
        d.sendEvent(name:"run_mode", value: w.mode, displayed: false)
        }
        break
        case 'watering_complete':
        // watering_complete {"event":"watering_complete","program":null,"current_station":null,"run_time":null,"started_watering_station_at":null,"rain_sensor_hold":null,"device_id":"5ba4d2694f0c7f7ff7b39480"}
        watering_battery_event(w.device_id,'closed')
        break
        case 'watering_in_progress_notification':
        // watering_in_progress_notification {"event":"watering_in_progress_notification","mode":null,"program":"manual","stations":null,"current_station":1,"run_time":1,"started_watering_station_at":"2019-09-09T00:05:50.000Z","rain_sensor_hold":false,"device_id":"5ba4d2694f0c7f7ff7b39480"}
        watering_battery_event(w.device_id,'open')
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
    return
        }

def send_message(device_id,event) {
    def d = getChildDevice(DTHDNI(device_id))
    if (d) {
        def message = "Orbit Byhve Timer: The ${d.name} has reported a '${event}' at ${timestamp()}!"
            if (sendPush) {
                sendPush(message)
            }
            if (phone) {
                sendSms(phone, message)
    }
    } else {
        log.error "Unknown b•hyve™ device id: ${device_id}"
    }
}

def watering_battery_event(device_id=null,bhyve_valve_state=null,battery_percent=null) {
    def d = getChildDevice(DTHDNI(device_id))
    if (d) {
        def st_valve_state = d.latestValue('valve')
        if ( st_valve_state != bhyve_valve_state) {
            log.info "${d.name}: Valve state of '${st_valve_state}' CHANGED to '${bhyve_valve_state.toUpperCase()}'"
            d.sendEvent(name:"valve", value: bhyve_valve_state )
            send_message(id,"Valve: ${bhyve_valve_state.toUpperCase()}")
        }
        if (battery_percent) {
            d.sendEvent(name:"battery", value: battery_percent, displayed:false )
            d.sendEvent(name:"battery_display", value: (Math.round(battery_percent.toInteger()/10.0) * 10).toString(), displayed:true, isStateChange: true )

        }
        d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
    } else {
        log.error "Unknown b•hyve™ device_id: ${device_id}"
    }
}



def allDeviceStatus() {
    log.debug "allDeviceStatus(): Start Routine"
    def children = app.getChildDevices()
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
    //    log.debug "Raw ${resp}"
    return resp
}

def appTouchHandler(evt="") {
    log.info "App Touch ${random()}: '${evt.descriptionText}' at ${timestamp()}"
    main()
    return

    def children = getAllChildDevices()
    def d
    children.findAll {
        if (it.deviceNetworkId == id) {
            log.debug "I found ${it.name}"
            d = getChildDevice(it.deviceNetworkId)
            log.debug d.latestValue('valve')
        }
    }
}

def refresh() {
    log.info "Executing Refresh Routine ID:${random()} at ${timestamp()}"
    main()
}

def main() {
    log.info "Executing Main Routine ID:${random()} at ${timestamp()}"
    def respdata = OrbitGet("devices")
    updateTiles(respdata)
}

def updateTiles(respdata) {
    def DTHname = null
    def DTHid = null
    def DTHlabel = null
    def d = null
    def rainDelayHrs
    def rainDelayDT
    def byhveValveState
    def message
    def banner
    def watering_events
    def watering_volume_gal
    def wateringTimeLeft
    if (respdata) {
        respdata.eachWithIndex { it, index ->
            if (typelist().contains(it.type)) {
                DTHid	= DTHDNI(it.id)
                DTHname = DTHName(it.type.split(" |-|_").collect{it.capitalize()}.join(" "))
                DTHlabel = "Bhyve ${it.name}"
            } else {
                log.error "Skipping: Unknown b•hyve™ device type ${it.type} for ${it.name}"
                DTHname = null
                DTHid = null
                DTHlabel = null
            }
            // Check to add any new bhyve device
            if (DTHid) {
                d = getChildDevice(DTHid)
                if (d) {
//                    log.info "Device ${it.id} -> (${index}): ${it.name} is a ${it.type} and last connected at: ${getMyDateTime(it.last_connected_at)}"
                    d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
                    d.sendEvent(name:"lastupdate", value: "${getMyDateTime(it.last_connected_at)}", displayed: false )
                    d.sendEvent(name:"schedulerFreq", value: schedulerFreq, displayed: false)
                    d.sendEvent(name:"firmware_version", value: it?.firmware_version, displayed: false)
                    d.sendEvent(name:"hardware_version", value: it?.hardware_version, displayed: false )
                    d.sendEvent(name:"name", value: it?.name, displayed: false)
                    d.sendEvent(name:"is_connected", value: it?.is_connected?'Online':'Offline')
                    d.sendEvent(name:"next_start_programs", value: it?.status?.next_start_programs, displayed: false )
                    d.sendEvent(name:"type", value: it?.type, displayed: false )
                    switch (it.type) {
                        case "sprinkler_timer":
                        byhveValveState = it?.status.watering_status?"open":"closed"
                        watering_battery_event(it.id,byhveValveState,it?.battery?.percent)
                        d.sendEvent(name:"presetRuntime", value: it?.manual_preset_runtime_sec/60, displayed: false )
                        d.sendEvent(name:"rain_delay", value: it?.rain_delay )
                        d.sendEvent(name:"batteryCharging", value: it?.battery.charging, displayed: false )
                        d.sendEvent(name:"run_mode", value: it.status?.run_mode, displayed: false)
                        d.sendEvent(name:"sprinkler_type", value: it?.zones[0].sprinkler_type, displayed: false)
                        def stp = OrbitGet("sprinkler_timer_programs", it.id)
                        if (stp) {
                            d.sendEvent(name:"start_times", value: "${stp?.start_times[0][0]} for ${stp?.run_times[0][0].run_time} mins", displayed: false)
                        }
                        if (it?.status?.rain_delay > 0) {
                            rainDelayHrs = it?.status?.rain_delay?:0
                            rainDelayDT = Date.parse("yyyy-MM-dd'T'HH:mm:ssX",it?.status?.next_start_time)
                            use (TimeCategory) {
                                rainDelayDT = (rainDelayDT + rainDelayHrs.hours).format('EEE MMM d, h:mm a', location.timeZone).replace("AM", "am").replace("PM","pm")
                            }
                            d.sendEvent(name:"rain_icon", value: "rain", displayed: false )
                            d.sendEvent(name:"next_start_time", value: rainDelayDT + " Rain Delay", displayed: false )
                            banner = "Next Start: ${rainDelayDT} Rain Delay"
                        } else {
                            d.sendEvent(name:"rain_icon", value: "sun", displayed: false )
                            d.sendEvent(name:"next_start_time", value: "${durationFromNow(it?.status?.next_start_time)}", displayed: false)
                            banner = "Next Start: ${getMyDateTime(it?.status?.next_start_time)}"
                        }
                        watering_events = OrbitGet('watering_events', it.id).first()
                        if (byhveValveState=='open') {
                            d.sendEvent(name:"power", value: watering_events?.irrigation?.water_volume_gal[0], descriptionText:"${watering_events?.irrigation?.water_volume_gal[0]} gallons")
//                            d.sendEvent(name:"level", value: watering_events?.irrigation?.run_time[0] )
                            wateringTimeLeft = durationFromNow(it?.status?.next_start_time, true)
                            d.sendEvent(name:"level", value: wateringTimeLeft, descriptionText: "${wateringTimeLeft} minutes left till end" )
                            banner ="Active Watering - ${watering_events?.irrigation?.water_volume_gal[0]} gals at ${timestamp('short') }"
                        } else {
                            d.sendEvent(name:"power", value: 0, descriptionText:"Gallons", displayed: false )
                            d.sendEvent(name:"level", value: watering_events?.irrigation?.run_time[0] )
                        }
                        d.sendEvent(name:"banner", value: banner, displayed: false )
                        break
                        case "bridge":
                        d.sendEvent(name:"num_stations", value: "${it?.num_stations}", displayed: false )
                        break
                        default:
                            log.error "Unknown Orbit b-hyve device type: '${it.type}'"
                    }
                }
            }
        }
    }
}

def durationFromNow(dt=null,mins=false) {
    if(dt == null){return ""}
    def dtpattern = dt.contains('Z')?"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'":"yyyy-MM-dd'T'HH:mm:ssX"
    def endDate
    def rc

    try {
        endDate = Date.parse(dtpattern, dt)
    } catch (e) {
        return "durationFromNow(dt): Error converting ${dt}: ${e}"
    }
    use (TimeCategory) {
        def now = new Date()
        if (mins) {
            rc = ((endDate - now) =~ /\d+(?=\Wminutes)/)[0]
        } else {
            rc = ((endDate - now) =~ /(.+)\b,/)[0][1]
        }
        return rc
    }
}

def tileLastUpdated() {
    return sprintf("%s Tile Last Refreshed at\n%s","${version()[0]}", timestamp())
}

def timestamp(type='long') {
    def formatstring = 'EEE MMM d, h:mm:ss a'
    if (type == 'short') {
        formatstring = 'h:mm:ss a'
    }
    Date datenow = new Date()
    def tf = new java.text.SimpleDateFormat(formatstring)
    def loc = getTwcLocation()?.location
    tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))
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
                log.error "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ ${command}.  Data='${resp?.data}' at ${timeString}."
                return null
            }
        }
    } catch (e) {
        log.error "OrbitGet($command): something went wrong: $e"
        return null
    }
    if (command=='devices') {
        state.devices = "Found ${respdata.type.count { it==typelist()[1]}} bridge device(s) and ${respdata.type.count { it==typelist()[0]}} sprinkler hose timer(s) in your Orbit b•hyve™ account."
    }
    return respdata
}

def OrbitBhyveLogin() {
    Date now = new Date()
    def timeString = new Date().format('EEE MMM d h:mm:ss a',location.timeZone)
    log.debug "Start OrbitBhyveLogin() at ${timeString} ============="
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
                log.error "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ Login.  Data='${resp.data}' at ${timeString}."
                state.orbit_api_key = null
                state.user_id 		= null
                state.user_name 	= null
                state.statusText = "Fatal Error Status '${resp.status}' from Orbit B•Hyve™ Login.  Data='${resp.data}' at ${timeString}."
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
    timeString = new Date().format('EEE MMM d h:mm:ss a',location.timeZone)
    log.debug "OrbitBhyveLogin(): End at ${timeString}"
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
    def DTHname = null
    def DTHid = null
    def DTHlabel = null
    def respdata = OrbitGet("devices")
    if (respdata) {
        respdata.eachWithIndex { it, index ->
            log.info "Device (${index}): ${it.name} is a ${it.type} and last connected at: ${it.last_connected_at}"
            if (typelist().contains(it.type)) {
                DTHid	= DTHDNI(it.id)
                DTHname = DTHName(it.type.split(" |-|_").collect{it.capitalize()}.join(" "))
                DTHlabel = "Bhyve ${it.name}"
            } else {
                log.error "Skipping: Unknown b•hyve™ device type ${it.type} for ${it.name}"
                DTHname = null
                DTHid = null
                DTHlabel = null
            }
            // Check to add any new bhyve device
            if (DTHid) {
                if (!getChildDevice(DTHid)) {
                    log.debug "Creating a NEW b•hyve™ '${DTHname}' device as '${DTHlabel}' with DNI: ${DTHid}"
                    try {
                        addChildDevice(DTHnamespace(), DTHname, DTHid, null, ["name": "${DTHlabel}", label: "${DTHlabel}", completedSetup: true])
                    } catch(e) {
                        log.error "The Device Handler '${DTHname}' was not found in your 'My Device Handlers', Error-> '${e}'.  Please install this DTH device in the IDE's 'My Device Handlers'"
                    }
//                    log.debug "Success: Added a new device named '${DTHlabel}' as DTH:'${DTHname}' with DNI:'${DTHid}'"
                } else {
//                    log.debug "Verification: Device exists named '${DTHlabel}' as DTH:'${DTHname}' with DNI:'${DTHid}'"
                }
            }
        }
    } else {
        return false
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
def getMyDateTime(dt) {
    def dtpattern = dt.contains('Z')?"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'":"yyyy-MM-dd'T'HH:mm:ssX"
    def rc
    try {
        rc = Date.parse(dtpattern, dt).format('EEE MMM d, h:mm a', location.timeZone).replace("AM", "am").replace("PM","pm")
    } catch (e) {
        return ""
    }
    return rc
}

// Constant Declarations
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
String DTHnamespace()			{ return "kurtsanders" }
String appAuthor()	 			{ return "SanderSoft™" }
String githubCodeURL()			{ return "https://github.com/KurtSanders/STOrbitBhyveController#storbitbhyvecontroller"}
String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/$imgName" }
String DTHName(type) 			{ return "Orbit Bhyve ${type}" }
String DTHDNI(id) 				{ return "bhyve${id}" }
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