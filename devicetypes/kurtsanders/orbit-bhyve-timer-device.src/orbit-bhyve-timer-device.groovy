/*
*  Name:	Orbit B•Hyve™ Device Handler
*  Author: Kurt Sanders
*  Email:	Kurt@KurtSanders.com
*  Date:	3/2019
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
def version() { return ["V1.0", "Requires Bhyve Orbit Timer App"] }
// End Version Information
import groovy.time.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat;

metadata {
    definition (name: "Orbit Bhyve Timer Device", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Contact Sensor"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"

    }
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type:"generic", width:6, height:4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState("default",label:'${currentValue}')
            }
            tileAttribute("contact", key: "SECONDARY_CONTROL") {
                attributeState("contact", label:'${currentValue}')
            }
        }
        // Network Connected Status
        standardTile("contact", "device.contact",  width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "open",   label:'Offline', action:"open",
                icon: "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/icons/offline.png",
                backgroundColor:yellowColor
            state "closed", label:'Online', action:"closed",
                icon:"https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/icons/broadcast.png",
                backgroundColor:greenColor
        }
        valueTile("schedulerFreq", "schedulerFreq", decoration: "flat", inactiveLabel: false, width: 3, height: 1, wordWrap: true) {
            state "schedulerFreq", label: 'Refresh Every\n${currentValue} min(s)', action:"refresh"
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main(["switch"])
        details(
            [
                "switch",
                "contact",
                "schedulerFreq",
                "refresh"
            ]
        )
    }
}

def refresh() {
    Date now = new Date()
    def timeString = now.format("EEE MMM dd h:mm:ss a", location.timeZone)
    log.info "==>Refresh Requested from Orbit B•Hyve™ Timer Device, sending refresh() request to parent smartApp"
    sendEvent(name: "statusText", value: "Cloud Refresh Requested at\n${timeString}...", "displayed":false)
    parent.refresh()
}

def installed() {
	log.debug "Orbit B•Hyve™ Timer Device Installed"
}
