# STOrbitBhyveController Proxy Node
#### * SmartThings® Integration for the b•hyve™ hose faucet timers *
### Alpha Version: 0.0.1

---

## Description:
### This alpha proxy node server provides: 
1. Support for controlling 'open' or 'closed' Bhyve states from SmartThings.   
2. Pushover Messaging Integration
3. Builds upon the base code for interfacing SmartThings and b•hyve™ with a Node.js/MQTT/Websocket server for real-time response.

## Requirements:

1. Lots of patience and consideration for this very rough alpha release that will require several more hours/days/weeks of debugging and re-testing.  *I am not a Javascript/Node developer by nature, but I feel this language/platform is optimal for creating the asynchronous Websocket connection and API integration with SmartThings.*
2. Advanced knowledge of Raspberry Pi OS (previously called Raspbian) , Node, Javascript, File Editing, MQTT, etc
3. Raspberry Pi Server
	* Model 3 or higher for performance 
	* Debian Version Stretch or Buster
4. Orbit Bhyve Controller SmartApp
	* [Version 4+](https://github.com/KurtSanders/STOrbitBhyveController)
5. Node/NPM


## Installation & Configuration

### **Instructions**

### Raspberry Pi 

1. Install Node.js and npm *(If not already installed)*

	```
	sudo apt-get update
	sudo apt-get upgrade
	sudo apt-get install nodejs npm
	nodejs --version
	npm --version
	```

2. Install and verify the mosquitto MQTT Broker is active *(If not already installed)*

	```
 	sudo apt install mosquitto 	
 	sudo systemctl enable mosquitto
 	sudo systemctl status mosquitto
	```
 
3. Create a new folder named 'bhyve' to house the bhyve node.js application files.

	`mkdir bhyve`

4. Change to that directory to make it current.

	`cd bhyve`

5. Download the 'st-orbit-bhyve-controller-0.0.1.tgz' from to the 'bhyve' folder with the command below:

	```
wget https://github.com/KurtSanders/STOrbitBhyveController/raw/master/node/st-orbit-bhyve-controller-0.0.1.tgz
	```
	
6. Unpack the tarball into the 'bhyve' folder

	```
	tar -xvf st-orbit-bhyve-controller-0.0.1.tgz --strip-components 1

	```


## Environment Configuration

1. Copy the sample file to env and edit the file to add required the information

```
CP .env-sample env

```



| key                  | description                                                                |
|----------------------|----------------------------------------------------------------------------|
| ORBIT_EMAIL          | Orbit userid                                                                |
| ORBIT_PASSWORD       | Orbit password                                                                |
| MQTT_BROKER_ADDRESS  | MQTT broker URL (eg. `mqtt://localhost:1883`)                              |
| MQTT_PASSWORD        | Broker password                                                            |
| ST_SMARTAPPURL       |REQUIRED OAUTH HAS TO BE ENABLED IN THE ORBIT BHYVE CONTROLLER SMARTAPP IN THE ST IDE
| | REQUIRED SmartThings API Rest displayed from the ST Live Logging Screen when exiting the Orbit Bhyve Controller SmartApp.
| | EXAMPLE: ST_SMARTAPPURL=https://graph.api.smartthings.com:443/api/smartapps/installations/xxxxxxxx/yyyyyyyyyyyy
| | OPTIONAL Set ST_TEST to 'true' to restrict POST command to SmartThings, 'false' will send real event data to SmartThings API endpoint
| ST_TEST=false| OPTIONAL Set ST_DEBUG to 'true' to generate verbose console messages for debugging, 'false' will restrict debug messages
| ST_DEBUG=false| 
| | Advanced Section (Be careful in changing non default values)
| | ST Polling Interval (Do not exceed a value of 15 or less than 10)
| ST_REFRESH_INTERVAL_SEC=15 | If this listening port is changed due to a local server conflict, it must be changed in the ST User Preferences Section
| WEBSERVER_PORT=3000 | 
| | OPTIONAL Pushover Messaging https://pushover.net/ (You MUST have an account)
| | Change PUSHOVER_MESSAGING to true and add your USER and TOKEN values without quotes. If you use Pushover Messaging https://pushover.net/
| PUSHOVER_MESSAGING=false| 
| PUSHOVER_USER=| 
| PUSHOVER_TOKEN=	| 

