/*
*  Name:	Orbit B•Hyve™ Sprinler Timer
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
def version() { return ["V4.0", "Requires Bhyve Orbit Timer Controller"] }
// End Version Information

import groovy.time.*
import java.text.SimpleDateFormat;

String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveTimer/beta/images/$imgName" }

metadata {
    definition (name: "Orbit Bhyve Sprinkler Timer", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Refresh"
        capability "Sensor"
        capability "Battery"
        capability "Valve"

        attribute "battery_display", "string"
        attribute "icon", "string"
        attribute "is_connected", "enum", ['true','false']
        attribute "lastSTupdate", "string"
        attribute "lastupdate", "string"
        attribute "manual_preset_runtime_sec", "number"
        attribute "name","string"
        attribute "next_start_programs", "string"
        attribute "next_start_time", "string"
        attribute "presetRuntime", "number"
        attribute "programs", "string"
        attribute "rain_icon", "string"
        attribute "runmode", "enum", ["auto, manual"]
        attribute "schedulerFreq", "string"
        attribute "sprinkler_type", "string"
        attribute "start_times", "string"
        attribute "statusText", "string"
        attribute "updown", "number"
        attribute "water_volume_gal", "number"

        command "setLevelUp"
        command "setLevelDown"
    }
    tiles(scale: 2) {
        multiAttributeTile(name:"bigtile", type:"generic", width:6, height:4, canChangeIcon: false ) {
            tileAttribute("device.valve", key: "PRIMARY_CONTROL") {
                attributeState "default",
                    label:'',
                    icon:"st.valves.water.closed",
                    backgroundColor:"#00a0dc"
                attributeState "closing",
                    label:'Closing',
//                    action: "valve.open",
                    icon:"st.valves.water.closed",
                    backgroundColor:"#f1d801"
                attributeState "closed",
                    label:'Closed',
//                    action: "valve.open",
                    icon:"st.valves.water.closed",
                    backgroundColor:"#00a0dc",
                    nextState: "opening"
                attributeState "opening",
                    label:'Opening',
//                    action: "valve.close",
                    icon:"st.valves.water.open",
                    backgroundColor:"#f1d801"
                attributeState "open",
                    label:'Open',
//                    action: "valve.close",
                    icon:"st.valves.water.open",
                    backgroundColor:"#44b621",
                    nextState: "closing"
                /*
                attributeState "on", label:'${name}', backgroundColor:"#44b621", nextState:"turningOff"
                attributeState "off", label:'${name}', backgroundColor:"#00a0dc", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', backgroundColor:"#f1d801", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', backgroundColor:"#f1d801", nextState:"turningOn"
                */
            }
            tileAttribute("banner", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'${currentValue}')
            }
            tileAttribute("device.manual_preset_runtime_sec", key: "SLIDER_CONTROL") {
                attributeState "level", action:"setManual_preset_runtime_sec", defaultState: true
//                attributeState "level", action:"switch level.setLevel", defaultState: true
            }
            /*
            tileAttribute("updown", key: "VALUE_CONTROL") {
                attributeState "VALUE_UP", action: "setLevelUp"
                attributeState "VALUE_DOWN", action: "setLevelDown"
            } */

        }
        /*
        standardTile("valve", "device.valve", width: 2, height: 2) {
            state "open", 		label:'${currentValue}', icon:"st.valves.water.open", backgroundColor:"#44b621"
            state "closed", 	label:'${currentValue}', icon:"st.valves.water.closed", backgroundColor:"#00a0dc"
            state "open", 			label:'${currentValue}', action:"valve.closed", icon:"st.Outdoor.outdoor16", backgroundColor:"#44b621", nextState:"closing"
            state "closed", 		label:'${currentValue}', action:"valve.open",  icon:"st.Outdoor.outdoor16", backgroundColor:"#00a0dc", nextState:"opening"
            state "opening", 		label:'${currentValue}', action:"valve.closed", icon:"st.Outdoor.outdoor16", backgroundColor:"#f1d801", nextState:"closing"
            state "closing", 		label:'${currentValue}', action:"valve.open",  icon:"st.Outdoor.outdoor16", backgroundColor:"#f1d801", nextState:"opening"
        }
        */
        // Network Connected Status , icon: getAppImg('icons/ht25.png')
        standardTile("is_connected", "device.is_connected",  width: 2, height: 2, decoration: "flat" ) {
            state "false", label: 'Offline' , backgroundColor: "#e86d13", icon:"st.Health & Wellness.health9"
            state "true",  label: 'Online'  , backgroundColor: "#00a0dc", icon:"st.Health & Wellness.health9"
        }
        standardTile("icon", "icon", width: 2, height: 2, decoration: "flat") {
            state "default", 	action:"refresh", 	icon: getAppImg('icons/bhyve-b.jpg')
            state "1", 			action:"refresh", 	icon: getAppImg('icons/ht-1.jpg')
            state "6", 			action:"refresh", 	icon: getAppImg('icons/ht-6.jpg')
            state "12", 		action:"refresh", 	icon: getAppImg('icons/ht-12.jpg')
        }
        standardTile("rain_icon", "rain_icon", width: 1, height: 1, decoration: "flat") {
            state "sun",   icon:"st.custom.wuk.clear"
            state "rain",  icon:"st.custom.wu1.chancerain", backgroundColor: "#44b621"
        }
        valueTile("name", "device.name", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Timer Name\n${currentValue}'
        }
        valueTile("programs", "device.programs", width: 6, height: 2, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}'
        }
        standardTile("battery_display", "device.battery_display", width: 2, height: 2, decoration: "flat", wordWrap: true) {
            state "default",   icon: getAppImg('icons/battery-na.jpg')
            state "0",   icon: getAppImg('icons/battery-0.jpg')
            state "10",  icon: getAppImg('icons/battery-10.jpg')
            state "20",  icon: getAppImg('icons/battery-20.jpg')
            state "30",  icon: getAppImg('icons/battery-30.jpg')
            state "40",  icon: getAppImg('icons/battery-40.jpg')
            state "50",  icon: getAppImg('icons/battery-50.jpg')
            state "60",  icon: getAppImg('icons/battery-60.jpg')
            state "70",  icon: getAppImg('icons/battery-70.jpg')
            state "80",  icon: getAppImg('icons/battery-80.jpg')
            state "90",  icon: getAppImg('icons/battery-90.jpg')
            state "100", icon: getAppImg('icons/battery-100.jpg')
        }
        valueTile("battery", "device.battery", width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "battery", label: 'Bat\n${currentValue}%',
                backgroundColors:[
                    [value: 30, color: "#153591"],
                    [value: 40, color: "#1e9cbb"],
                    [value: 50, color: "#90d2a7"],
                    [value: 60, color: "#44b621"],
                    [value: 70, color: "#f1d801"],
                    [value: 80, color: "#d04e00"],
                    [value: 90, color: "#bc2323"]
                ]
        }
        valueTile("run_mode", "device.run_mode", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", 	label: 'Run Mode\n${currentValue}'
        }
        valueTile("presetRuntime", "device.presetRuntime", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Preset Runtime\n${currentValue} mins'
        }
        valueTile("sprinkler_type", "device.sprinkler_type", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Sprinkler Type\n${currentValue}'
        }
        valueTile("schedulerFreq", "device.schedulerFreq", decoration: "flat", width: 2, height: 1, wordWrap: true) {
            state "default", label: 'Refresh Auto\n${currentValue} min(s)'
        }
        valueTile("lastupdate", "device.lastupdate", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}', action: "refresh"
        }
        valueTile("lastSTupdate", "device.lastSTupdate", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}', action:"refresh"
        }
        valueTile("next_start_time", "device.next_start_time", width: 4, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Duration to Start\n${currentValue}'
        }
        valueTile("start_times", "device.start_times", width: 3, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Start Time(s)\n${currentValue}'
        }
        valueTile("next_start_programs", "device.next_start_programs", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Next Start Pgm\n${currentValue}'
        }
        valueTile("water_volume_gal", "device.water_volume_gal", width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "water_volume_gal", label: 'Gallons Used\n${currentValue}'
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main(["bigtile"])
        details(
            [
                "bigtile",
                "battery_display",
                "icon",
                "is_connected",
                "water_volume_gal",
                "next_start_time",
                "start_times",
                "rain_icon",
                "presetRuntime",
                "run_mode",
                "sprinkler_type",
                "next_start_programs",
                "schedulerFreq",
                "lastupdate",
                "lastSTupdate",
                "refresh",
                "battery",
                "programs"

            ]
        )
    }
}

def refresh() {
    Date now = new Date()
    def timeString = now.format("EEE MMM dd h:mm:ss a", location.timeZone)
    log.info "Manual Refresh Requested from Orbit B•Hyve™ Timer Device, sending refresh() request to parent smartApp"
    sendEvent(name: "lastSTupdate", value: "Cloud Refresh Requested at\n${timeString}...", "displayed":false)
    sendEvent(name: "banner", value: "Cloud Refresh Requested..", "displayed":false)
    parent.refresh()
}

def installed() {
	log.info "Orbit B•Hyve™ Sprinkler Timer Device Installed with default timer on time of 10 minutes"
    sendEvent(name: "valve", value: "closed")
    sendEvent(name: "level", value: 10)
    sendEvent(name: "updown", value: 10)
}

def on() {
    log.info "Orbit B•Hyve™ Device level is now ON"
    sendEvent(name: "valve", value: "open")
}

def off() {
    log.info "Orbit B•Hyve™ Device is now OFF"
    sendEvent(name: "valve", value: "closed")
}

def setLevel(level, rate = null) {
    log.info "Orbit B•Hyve™ Sprinkler Timer Device level was ${device.latestValue("manual_preset_runtime_sec")} and is now ${level}"
    sendEvent(name: "manual_preset_runtime_sec", value: level)
    sendEvent(name: "updown", value: level)
}

def setLevelUp() {
    def levelState = device.latestValue("level")
    log.info "Orbit B•Hyve™ Sprinkler Timer Device level was ${levelState} and is now ${levelState + 1}"
	setLevel(levelState+1)
}

def setLevelDown() {
    def levelState = device.latestValue("level")
    log.info "Orbit B•Hyve™ Sprinkler Timer Device level was ${levelState} and is now ${levelState - 1}"
	setLevel(levelState-1)
}