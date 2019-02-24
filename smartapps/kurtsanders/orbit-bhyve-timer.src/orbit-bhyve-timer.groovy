/*
* Orbit™ Bhyve Timer
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
    name: 		"Orbit Bhyve Timer",
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
	def bhyveLoginOK = true
    dynamicPage(name: "mainMenu",
                title: "Orbit B•Hyve™ Timer Login Information",
                nextPage: orbitBhyveLoginOK?"mainOptions":null,
                submitOnChange: true,
                install: false,
                uninstall: true)
    {
        section ("Orbit B•Hyve™ Login Information") {
            input ( name    : "username",
                   type     : "enum",
                   title    : "Select the login?",
                   options  : ["kurt@kurtsanders.com", "kurtsanders.myXnetgear.com"],
                   submitOnChange: true,
                   multiple : false,
                   required : true
                  )
            input ( name    : "password",
                   type     : "enum",
                   title    : "Select the password?",
                   options  : ["Apples55", "badpassword"],
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
    setScheduler(schedulerFreq)
    subscribe(app, appTouchHandler)
}

def installed() {
    add_bhyve_ChildDevice()
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
    main()

/*
    if (debugVerbose) {
        def children = app.getChildDevices()
        def thisdevice
        log.debug "SmartApp $app.name has ${children.size()} child devices"
        thisdevice = children.findAll { it.typeName }.sort { a, b -> a.deviceNetworkId <=> b.deviceNetworkId }.each {
            log.info "${it} <-> DNI: ${it.deviceNetworkId}"
        }
    }
*/

}

def add_bhyve_ChildDevice() {
    // add bhyve device
    if (!getChildDevice(DTHDNI())) {
        log.debug "Creating a NEW device named 'My Bhyve Timer' as ${DTHName()} with DNI: ${DTHDNI()}"
        try {
            addChildDevice("kurtsanders", DTHName(), DTHDNI(), null, ["name": "My Bhyve Timer", label: "My Bhyve Timer", completedSetup: true])
        } catch(e) {
            errorVerbose("The Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${e}'.  Please install this DTH device in the IDE's 'My Device Handlers'")
            return false
        }
        debugVerbose("Success: Added a new device named 'My Bhyve Timer' as ${DTHName()} with DNI: ${DTHDNI()}")
    } else {
        debugVerbose("Verification: Device exists named 'My Bhyve Timer' as ${DTHName()} with DNI: ${DTHDNI()}")
    }
}
def remove_bhyve_ChildDevice() {
    getAllChildDevices().each {
        log.debug "Deleting bhyve device: ${it.deviceNetworkId}"
        try {
            deleteChildDevice(it.deviceNetworkId)
        }
        catch (e) {
            log.debug "${e} deleting the bhyve device: ${it.deviceNetworkId}"
        }
    }
}

def refresh() {
    log.info "Executing Refresh Routine ID:${random()} at ${timestamp()}"
	main()
}

def main() {
    log.info "Executing Main Routine ID:${random()} at ${timestamp()}"
}

def getByveLogin() {
    def httpPostStatus = resp
    Date now = new Date()
    def timeString = new Date().format('EEE MMM d h:mm:ss a',location.timeZone)
    def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
    def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${state.devid}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
	def respParams = [:]
    def params = [
        'uri'			: Web_idigi_post,
        'headers'		: idigiHeaders(),
        'body'			: Web_postdata
    ]
    infoVerbose("Start httpPost =============")
    try {
        httpPost(params) {
            resp ->
            infoVerbose("httpPost resp.status: ${resp.status}")
            httpPostStatus = resp
        }
    }
    catch (Exception e)
    {
        debugVerbose("Catch HttpPost Error: ${e}")
        return null
    }
    if (httpPostStatus==null) {
        return null
    }
    def resp = httpPostStatus
    if(resp.status == 200) {
        debugVerbose("HttpPost Request was OK ${resp.status}")
        if(resp.data == "Device Not Connected") {
            errorVerbose("HttpPost Request: ${resp.data}")
            unschedule()
            state.statusText = "Spa Fatal Error ${resp.data} at\n${timeString}"
            state.contact = "open"
            def message = "Spa Error: ${resp.data}! at ${timeString}."
            if (phone) {
                sendSms(phone, message)
            }
            return null
        }
        else {
            state.statusText 			= "Spa data refreshed at\n${timeString}"
            state.contact 				= "closed"
            state.respdata				= resp.data
            state.B64decoded 			= resp.data.decodeBase64()
            B64decoded 					= resp.data.decodeBase64()
            log.debug "B64decoded: ${state.B64decoded}"
            // def byte[] B64decoded = B64encoded.decodeBase64()
            // def hexstring = B64decoded.encodeHex()
            // log.info "hexstring: ${hexstring}"
        }
    }
    else {
        errorVerbose("HttpPost Request got http status ${resp.status}")
        state.statusText = "Spa Fatal Error Http Status ${resp.status} at ${timeString}."
        return null
    }
    infoVerbose("getOnlineData: End")
    return B64decoded
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


// Constant Declarations
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
String appAuthor()	 { return "SanderSoft™" }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/KurtSanders/STByveOrbitTimer/master/images/$imgName" }
String DTHName() { return "Bhyve Orbit Timer Device" }
String DTHDNI() { return "orbit-bhyve-${app.id}" }
Map orbitHeaders() {
    return [
        'UserAgent'		: 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0',
        'Cookie'		: 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000',
        'Authorization'	: 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
}
