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
String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/master/images/$imgName" }

metadata {
    definition (name: "Orbit Bhyve Bridge", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Refresh"
        capability "Sensor"

        attribute "is_connected", "enum", ['Online','Offline']
        attribute "firmware_version", "string"
        attribute "hardware_version", "string"
        attribute "schedulerFreq", "string"
        attribute "lastupdate", "string"
        attribute "statusText", "string"
        attribute "lastSTupdate", "string"
        attribute "name","string"
        attribute "next_start_time", "string"
        attribute "next_start_programs", "string"
        attribute "num_stations", "number"
    }
    tiles(scale: 2) {
        // Network Connected Status
        standardTile("is_connected", "device.is_connected",  width: 2, height: 2, decoration: "flat" ) {
            state "Offline", label: 'Offline' , backgroundColor: "#e86d13", icon:"st.Health & Wellness.health9"
            state "Online",  label: 'Online' , backgroundColor: "#00a0dc", icon:"st.Health & Wellness.health9"
        }
        valueTile("icon", "icon", width: 1, height: 1, decoration: "flat") {
            state "default", icon: getAppImg('icons/bh1.png')
        }
        valueTile("firmware_version", "device.firmware_version", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Firmware\n${currentValue}'
        }
        valueTile("hardware_version", "device.hardware_version", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Hardware\n${currentValue}'
        }
        valueTile("schedulerFreq", "device.schedulerFreq", decoration: "flat", width: 2, height: 1, wordWrap: true) {
            state "default", label: 'Refresh Every\n${currentValue} min(s)'
        }
        valueTile("lastupdate", "device.lastupdate", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Last Connected\n${currentValue}', action: "refresh"
        }
        valueTile("lastSTupdate", "device.lastSTupdate", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}', action:"refresh"
        }
        valueTile("name", "device.name", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}'
        }
        valueTile("next_start_time", "device.next_start_time", width: 3, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Next Start Time\n${currentValue}'
        }
        valueTile("next_start_programs", "device.next_start_programs", width: 3, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Next Start Pgm\n${currentValue}'
        }
        valueTile("num_stations", "device.num_stations", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Number Stations\n${currentValue}'
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main(["is_connected"])
        details(
            [
                "is_connected",
                "icon",
                "name",
                "firmware_version",
                "hardware_version",
                "next_start_time",
                "next_start_programs",
                "lastupdate",
                "lastSTupdate",
                "num_stations",
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
    sendEvent(name: "lastSTupdate", value: "Cloud Refresh Requested at\n${timeString}...", "displayed":false)
    parent.refresh()
}

def installed() {
	log.debug "Orbit B•Hyve™ Timer Device Installed"
}