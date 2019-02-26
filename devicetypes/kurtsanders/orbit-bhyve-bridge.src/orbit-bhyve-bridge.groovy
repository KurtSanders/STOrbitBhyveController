/*
*  Name:	Orbit B•Hyve™ Bridge
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
def version() { return ["V1.0", "Requires Bhyve Orbit Controller"] }
// End Version Information
import groovy.time.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat;

metadata {
    definition (name: "Orbit Bhyve Bridge", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Contact Sensor"
        capability "Refresh"
        capability "Sensor"
    }
    tiles(scale: 2) {
        // Network Connected Status
        standardTile("contact", "device.contact",  width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "open",   label:'Offline',
                icon: "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/icons/offline.png"
            state "closed", label:'Online',
                icon:"https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/icons/broadcast.png"
        }
        valueTile("firmware_version", "device.firmware_version", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Firmware\n${currentValue}'
        }
        valueTile("schedulerFreq", "device.schedulerFreq", decoration: "flat", width: 3, height: 1, wordWrap: true) {
            state "default", label: 'Refresh Every\n${currentValue} min(s)', action:"refresh"
        }
        valueTile("lastupdate", "device.lastupdate", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Last Connected\n${currentValue}', action: "refresh"
        }
        valueTile("lastSTupdate", "device.lastSTupdate", width: 3, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}', action: "refresh"
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        valueTile("statusText", "device.statusText", width: 5, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}', action: "refresh"
        }
        main(["contact"])
        details(
            [
                "contact",
                "firmware_version",
                "refresh",
                "lastupdate",
                "lastSTupdate",
                "schedulerFreq",
                "statusText"
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