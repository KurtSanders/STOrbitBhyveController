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
def version()   { return ["V1.0", "Original Code Base"] }
// End Version Information

String appVersion()	 { return "1.0" }
String appModified() { return "2019-02-24" }

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
}

def mainMenu() {
    def orbitBhyveLoginOK = false
    if ( (username) && (password) ) {
        orbitBhyveLoginOK = OrbitBhyveLogin()
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
                        ""
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
        section ("${app.name} Information") {
            paragraph image : getAppImg("icons/bhyveIcon.png"),
                title	    : appAuthor(),
                    required: false,
                    "Version: ${version()[0]}\n" +
                    "Updates: ${version()[1]}"
        }
    }
}

def mainOptions() {
    dynamicPage(name: "mainOptions",
                title: "Bhyve Timer Controller Options",
                install: true,
                uninstall: false)
    {
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
        section("Bhyve Notifications") {
            input ( name	: "phone",
                   type		: "phone",
                   title	: "Text Messages for Alerts (optional)",
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

def initialize() {
    add_bhyve_ChildDevice()
    setScheduler(schedulerFreq)
    subscribe(app, appTouchHandler)
}

def installed() {
    initialize()
}

def uninstalled() {
    log.info "Removing ${app.name}..."
    remove_bhyve_ChildDevice()
    log.info "Good-Bye..."
}
def updated() {
    unsubscribe()
    initialize()
}

def appTouchHandler(evt="") {
    log.info "App Touch ${random()}: '${evt.descriptionText}' at ${timestamp()}"

    def children = app.getChildDevices()
    def thisdevice
    log.debug "This SmartApp '$app.name' has ${children.size()} timer/hub devices"
    thisdevice = children.findAll { it.typeName }.sort { a, b -> a.deviceNetworkId <=> b.deviceNetworkId }.each {
        log.info "${it} <-> DNI: ${it.deviceNetworkId}"
    }
    main()
}

def refresh() {
    log.info "Executing Refresh Routine ID:${random()} at ${timestamp()}"
    main()
}

def main() {
	def respdata
    log.info "Executing Main Routine ID:${random()} at ${timestamp()}"
    respdata = OrbitGet("devices")
    updateTiles(respdata)
}

def updateTiles(respdata) {
    def DTHname = null
    def DTHid = null
    def DTHlabel = null
    def d = null
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
//                    log.info "Device ${d} -> (${index}): ${it.name} is a ${it.type} and last connected at: ${it.last_connected_at}"
                    d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
                    d.sendEvent(name:"lastupdate", value: Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", it.last_connected_at).format('EEE MMM d, h:mm:ss a'))
                    d.sendEvent(name:"schedulerFreq", value: schedulerFreq)
                    d.sendEvent(name:"firmware_version", value: it.firmware_version)
                    d.sendEvent(name:"contact", value: it.is_connected?"closed":"open")
                    switch (it.type) {
                        case 'bridge':
                        break
                        default:
                            d.sendEvent(name:"battery", value: "${it.battery.charging?'Charging -':'Charged -'} ${it.battery.percent}" )
                            d.sendEvent(name:"switch", value: it.status.run_mode)
                        break
                    }
                }
            }
        }
    }
}

def tileLastUpdated() {
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    return sprintf("%s Tile Last Refreshed at\n%s","${version()[0]}", now)
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
        case 'device_history':
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
        log.info "Found ${respdata.type.count { it==typelist()[0]}} sprinkler hose timer(s) and ${respdata.type.count { it==typelist()[1]}} bridge device(s) in your b•hyve™ account."
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

def timestamp() {
    Date datenow = new Date()
    def tf = new java.text.SimpleDateFormat('EEE MMM d h:mm:ss a')
    def loc = getTwcLocation()?.location
    tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))
    return tf.format(datenow)
}

def random() {
    def runID = new Random().nextInt(10000)
    //    if (state?.runID == runID as String) {
    //        log.warn "DUPLICATE EXECUTION RUN AVOIDED: Current runID: ${runID} Past runID: ${state?.runID}"
    //        return
    //    }
    //    state.runID = runID
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
                        addChildDevice("kurtsanders", DTHname, DTHid, null, ["name": "${DTHlabel}", label: "${DTHlabel}", completedSetup: true])
                    } catch(e) {
                        log.error "The Device Handler '${DTHname}' was not found in your 'My Device Handlers', Error-> '${e}'.  Please install this DTH device in the IDE's 'My Device Handlers'"
                    }
                    log.debug "Success: Added a new device named '${DTHlabel}' as DTH:'${DTHname}' with DNI:'${DTHid}'"
                } else {
                    log.debug "Verification: Device exists named '${DTHlabel}' as DTH:'${DTHname}' with DNI:'${DTHid}'"
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

// Constant Declarations
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
String appAuthor()	 { return "SanderSoft™" }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/$imgName" }
String DTHName(type) { return "Orbit Bhyve ${type}" }
String DTHDNI(id) { return "orbit-bhyve-${id}" }
String orbitBhyveLoginAPI() { return "https://api.orbitbhyve.com/v1/" }
String web_postdata() { return "{\n    \"session\": {\n        \"email\": \"$username\",\n        \"password\": \"$password\"\n    }\n}" }
Map OrbitBhyveLoginHeaders() {
    return [
        'orbit-app-id':'Orbit Support Dashboard',
        'Content-Type':'application/json'
    ]
}
List typelist() { return ["sprinkler_timer","bridge"] }

