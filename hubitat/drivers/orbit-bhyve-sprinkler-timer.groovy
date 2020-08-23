/*
*  Name:	Orbit B•Hyve™ Sprinler Timer
*  Author: Kurt Sanders & Dominick Meglio
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

import groovy.time.*
import java.text.SimpleDateFormat
import groovy.transform.Field
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

@Field static String wsHost = "wss://api.orbitbhyve.com/v1/events"
@Field static String timeStampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
@Field static boolean webSocketOpen = false
@Field static Object socketStatusLock = new Object()
@Field static Object wateringLock = new Object()

metadata {
    definition (name: "Orbit Bhyve Sprinkler Timer", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Refresh"
        capability "Sensor"
        capability "Battery"
        capability "Valve"
        capability "Initialize"

        attribute "is_connected", "bool"
        attribute "manual_preset_runtime_min", "number"
        attribute "next_start_programs", "string"
        attribute "next_start_time", "number"
        attribute "preset_runtime", "number"
        attribute "programs", "string"
        attribute "rain_icon", "string"
        attribute "rain_delay", "string"
        attribute "run_mode", "enum", ["auto, manual"]
        attribute "sprinkler_type", "string"
        attribute "start_times", "string"
        attribute "station", "string"
        attribute "scheduled_auto_on", "bool"
        attribute "last_watering_volume", "number"
        attribute "water_flow_rate", "number"

//        command "setRainDelay", ["number"]
        command(
            "setRainDelay",
            [
                [
                    "name":"Set a Manual Rain Delay",
                    "description":"Select a value for a rain delay",
                    "type":"ENUM",
                    "constraints":["24","48","72"]
                ]
            ]
        );
    }

    preferences {
        input "presetRunTime", "number", title: "How many minutes should the valve remain open?", defaultValue: 10, displayDuringSetup: false, required: false
    }
}

def refresh() {
    parent.refresh()
}

def installed() {
    setWebSocketStatus(false)
    state.retryCount = 0
    state.nextRetry = 0
    sendEvent(name: "valve", value: "closed")
}

def uninstalled() {
    unschedule()
    if (getDataValue("master") == "true") {
        try {
            interfaces.webSocket.close()
        }
        catch (e) {
            
        }
    }
}

def open() {
    def runTime = presetRunTime ?: device.latestValue('preset_runtime') ?: 10
    parent.sendRequest('open', parent.getOrbitDeviceIdFromDNI(device.deviceNetworkId), device.latestValue('station'), runTime)
}

def close() {
    def runTime = presetRunTime ?: device.latestValue('preset_runtime') ?: 10
    parent.sendRequest('close', parent.getOrbitDeviceIdFromDNI(device.deviceNetworkId), device.latestValue('station'), runTime)
}

def setRainDelay(hours) {
    def validRainDelay = ["24","48","72"]
    if (validRainDelay.contains(hours)) {
        log.info "Setting a Manual Rain Delay for ${hours} hrs"
        parent.sendRainDelay(parent.getOrbitDeviceIdFromDNI(device.deviceNetworkId), hours)
    } else {
        log.error "Rain Delay value of ${hours} is invalid. Valid Rain Delay values are: ${validRainDelay}.  Command ignored!"
    }
}

def sendRainDelay(device_id, hours) {
    safeWSSend([
        event: "rain_delay",
        device_id: device_id,
        delay: hours
    ], true)
}

def safeWSSend(obj, retry) {
    synchronized (socketStatusLock) {
        if (!isWebSocketOpen()) {
            try {
                interfaces.webSocket.close()
            }
            catch (e) {

            }
            if (state.nextRetry == 0 || now() >= state.nextRetry) {
                logDebug "Reconnecting to Web Socket"
                state.retryCount++
    
                state.nextRetry = now() + (30000*((state.retryCount < 10) ? state.retryCount : 10))
                interfaces.webSocket.connect(wsHost)
                if (retry)
                    state.retryCommand = new JsonOutput().toJson(obj)
                return
            }
            else {
                log.warn "Waiting until ${new Date(state.nextRetry)} to reconnect ${state.retryCount}"
                return
            }
        }
        else
            state.retryCount = 0
        
        interfaces.webSocket.sendMessage(new JsonOutput().toJson(obj))
    }
}

def initialize() {
    synchronized (socketStatusLock) {
        if (getDataValue("master") == "true") {
            try {
                setWebSocketStatus(false)
                interfaces.webSocket.close()
            }
            catch (e) {
                
            }
            logDebug "Connecting to Web Socket"
            interfaces.webSocket.connect(wsHost)
        }
    }
}

def parse(String message) {
    def payload = new JsonSlurper().parseText(message)

    switch (payload.event) {
        case "watering_in_progress_notification":
            parent.debugVerbose "Watering Started: ${payload.device_id}:${payload.current_station}"
            def dev = parent.getDeviceByIdAndStation(payload.device_id, payload.current_station)
            if (dev) {
                dev.sendEvent(name: "last_watering_volume", value: 0, unit: "gal")
                synchronized (wateringLock) {
                    dev.sendEvent(name: "valve", value: "open")
                    for (valveDevice in parent.getValveDevices(payload.device_id)) {
                        if (valveDevice.deviceNetworkId != dev.deviceNetworkId && valveDevice.currentValue("valve") == "open") {
                            parent.debugVerbose "Closing ${valveDevice.deviceNetworkId} because station ${payload.current_station} opened"
                            valveDevice.sendEvent(name: "valve", value: "closed")
                        }
                    }
                }
            }
            break
        case "device_disconnected":
            def dev = parent.getDeviceById(payload.device_id)
            if (dev) 
                dev*.sendEvent(name: "is_connected", value: false)               
            break
        case "device_connected":
            def dev = parent.getDeviceById(payload.device_id)
            if (dev) 
                dev*.sendEvent(name: "is_connected", value: true)               
            break
        case "watering_complete":
            parent.debugVerbose "Watering Complete: ${payload}"
            
            def dev = parent.getDeviceById(payload.device_id)
            if (dev) {
                synchronized (wateringLock) {
                    for (d in dev) {
                        if (d.hasCapability("Valve"))
                            d.sendEvent(name: "valve", value: "closed")
                    }
                }
                parent.refreshLastWateringAmount(payload.device_id)
            }
            break
        case "change_mode":
            if (payload.stations != null) {
                for (station in payload.stations) {
                    def dev = parent.getDeviceByIdAndStation(payload.device_id, station.station)
                    if (dev)
                        dev.sendEvent(name: "run_mode", value: payload.mode)
                }
            }
            else {
                def dev = parent.getDeviceById(payload.device_id)
                if (dev)
                    dev*.sendEvent(name: "run_mode", value: payload.mode)
            }
            break
        case "rain_delay":
            def dev = parent.getDeviceById(payload.device_id)
            if (dev)
                dev*.sendEvent(name: "rain_delay", value: payload.delay)
            break
        case "low_battery":
            parent.triggerLowBattery(device)
            break
        case "flow_sensor_state_changed":
            def dev = parent.getDeviceById(payload.device_id)
            if (dev)
                dev*.sendEvent(name: "water_flow_rate", value: payload.flow_rate_gpm)
            break
        case "set_manual_preset_time":
            def presetWateringInt = payload.seconds.toInteger()/60
            def dev = parent.getDeviceById(payload.device_id)
            if (dev) {
                dev*.sendEvent(name:"preset_runtime", value: presetWateringInt)
                dev*.sendEvent(name:"manual_preset_runtime_min", value: presetWateringInt)
            }
            break
        case "program_changed":
        case "device_idle":
        case "clear_low_battery":
        case "device_idle":
            // Do nothing
            break
        default:
            log.warn "Unknown message: ${message}"
    }
}

def webSocketStatus(String message) {
    if (message == "status: open") {
        synchronized (socketStatusLock) {
            logDebug "Reconnect successful"
            setWebSocketStatus(true)
            state.webSocketOpenTime = now()
            def loginMsg = [
                event: "app_connection",
                orbit_app_id: UUID.randomUUID().toString(),
                orbit_session_token: parent.getApiToken()
            ]
            
            interfaces.webSocket.sendMessage(new JsonOutput().toJson(loginMsg))
        }
        parent.refresh()
        pauseExecution(1000)
        if (state.retryCommand != null) {
            logDebug "Retrying command from before connection lost ${state.retryCommand}"
            interfaces.webSocket.sendMessage(state.retryCommand)
            state.retryCommand = null
        }
        schedule("0/25 * * * * ? *", pingWebSocket)
    }
    else if (message == "status: closing") {
        synchronized (socketStatusLock) {
            log.error "Lost connection to Web Socket: ${message}, will reconnect."
            setWebSocketStatus(false)
        }
    }
    else if (message.startsWith("failure:")) {
        synchronized (socketStatusLock) {
            log.error "Lost connection to Web Socket: ${message}, will reconnect."
            setWebSocketStatus(false)
        }
    }
    else {
        synchronized (socketStatusLock) {
            log.error "Websocket status: ${message}, will reconnect."
            setWebSocketStatus(false)
        }
    }
}

def sendWSMessage(valveState,device_id,zone,run_time) {
    def msg = [
        event: "change_mode",
        mode: "manual",
        device_id: device_id,
        timestamp: getTimestamp()
    ]

    if (valveState == "open") {
        msg.stations = [
            [
                station: zone.toInteger(),
                run_time: run_time
            ]
        ]
    }
    else {
        msg.stations = []
    }
    safeWSSend(msg, true)
}

def pingWebSocket() {
    if (now()-(30*60*1000) >= state.webSocketOpenTime) {
        logDebug "WebSocket has been open for 30 minutes, reconnecting"
        initialize()
        return
    }
    def pingMsg = [ event: "ping"]
    safeWSSend(pingMsg, false)
}

def getTimestamp() {
    def date = new Date()
    return date.format(timeStampFormat, TimeZone.getTimeZone('UTC'))
}

def setWebSocketStatus(status) {
    webSocketOpen = status
}

def isWebSocketOpen() {
    return webSocketOpen 
}

def logDebug(msg) {
    if (parent.isDebugLogEnabled())
        log.debug msg
}