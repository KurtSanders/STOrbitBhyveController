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
3. Make a backup of your server in case you want to return to the state before the install
4. Advanced knowledge of Raspberry Pi OS (previously called Raspbian) , Node, NPM, Javascript, File Editing, MQTT, etc
5. Raspberry Pi Server
	* Model 3 or higher for performance 
	* Debian Version Stretch or Buster
6. Orbit Bhyve Controller SmartApp
	* [Version 4+](https://github.com/KurtSanders/STOrbitBhyveController)
7. Node/NPM


## Installation & Configuration

### **Instructions**

### Orbit Bhyve Controller SmartApp

1. Complete the following information in the SmartApp preferences.
2. Complete API Setup and obtain the oauth API string from the ST IDE Live Logging view 
3. Enter the local IP address of the Raspberry Pi server and webserver port

<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/NodeProxySetup.jpg" width=400>
</p>


### Raspberry Pi Server

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

		● mosquitto.service - Mosquitto MQTT v3.1/v3.1.1 Broker<br>
		   Loaded: loaded (/lib/systemd/system/mosquitto.service; enabled; vendor preset: enabled)<br>
		   Active: inactive (dead) since Thu 2020-05-14 19:18:48 EDT; 3 weeks 0 days ago<br>
		     Docs: man:mosquitto.conf(5)<br>
		           man:mosquitto(8)<br>
		 Main PID: 30144 (code=exited, status=0/SUCCESS)<br>
 
3. Create a new folder named 'bhyve' to house the bhyve node.js application files.

	`mkdir bhyve`

4. Change to that directory to make it current.

	`cd bhyve`

5. Download the 'st-orbit-bhyve-controller-0.0.1.tgz' from to the 'bhyve' folder with the command below:

	`wget https://github.com/KurtSanders/STOrbitBhyveController/raw/master/node/st-orbit-bhyve-controller-0.0.1.tgz`
	
6. Unpack the tarball into the 'bhyve' folder and list the files

	```
	tar -xvf st-orbit-bhyve-controller-0.0.1.tgz --strip-components 1
	ls -la
	```

7. Install st-orbit-bhyve-controller required node modules

	`npm install`

8. Configure Environment .env File

	* Copy the .env-sample template file to .env and edit the .env file to add the required information according to the table below:

		```
cp .env-sample .env
nano .env

		```

	* .env data fields (Add field values without any quotes or spaces)

| Key                  | Type     | description                                       |
|:---------------------|:--------:|:--------------------------------------------------|
| `ORBIT_EMAIL=`         | Required | [Orbit Account](https://techsupport.orbitbhyve.com/) Email Address                     |
| `ORBIT_PASSWORD=`      | Required | [Orbit Account](https://techsupport.orbitbhyve.com/) Password                 |   
| `MQTT_BROKER_ADDRESS=mqtt://localhost:1883` | Required | MQTT Broker URL   |
| `MQTT_PASSWORD=`       | Optional | MQTT Broker access password if a password was setup during MQTT install                            |
| `ST_SMARTAPPURL=`       | Required | OAUTH HAS TO BE ENABLED IN THE ORBIT BHYVE CONTROLLER SMARTAPP IN THE ST IDE.  <br>The SmartThings API Rest string will be displayed in the ST Live Logging Screen when exiting the Orbit Bhyve Controller SmartApp. EXAMPLE:<BR>ST_SMARTAPPURL=https://graph.api.smartthings.com:443/api/smartapps/installations/xxxxxxxx/yyyyyyyyyyyy |
| **Advanced Section**|| (Be careful in changing from the default values below)| 
| `ST_TEST=false`| Required | Set ST_TEST=true to prevent API POST commands to SmartThings for debugging. ST_TEST=false will send real event data to SmartThings API endpoint |
| `ST_DEBUG=false`| Required | Set ST_DEBUG=true to generate verbose console messages for debugging, ST_DEBUG=false will restrict debug messages| 
| `ST_REFRESH_INTERVAL_SEC=15` | Required | ST Polling Interval (Do not exceed a value of 15 or less than 10) |
| `WEBSERVER_PORT=3000` | Required | If this listening port is changed due to a local server conflict, it must be changed in the ST User Preferences Section |
| `PUSHOVER_MESSAGING=false`| Required | Off by default.  [Pushover Messaging Service](https://pushover.net)   You MUST have a Pushover account to have messages sent to Pushover.  Change PUSHOVER_MESSAGING=true and your USER and TOKEN values are required below. 
| `PUSHOVER_USER=`    | Required | Pushover user string| 
| `PUSHOVER_TOKEN=`	| Required | Pushover token string| 

9. Run the application Interactively to Identify Errors

	`node app.js`

10. Sample Console Messages after app.js startup
	
	Fri, Jun 05, 2020, 2:30:31 PM - Sending PushOver 'Fri, Jun 05, 2020, 2:30:31 PM - MQTT connected at mqtt://localhost:1883'<br>
	Fri, Jun 05, 2020, 2:30:31 PM - STOrbitBhyveController™ Webserver is listening on port: 3000<br>
	Fri, Jun 05, 2020, 2:30:31 PM - Pushover Message {"status":1,"request":"0"} sent successfully<br>
	Fri, Jun 05, 2020, 2:30:31 PM - Post Accepted Status: 200<br>
	Fri, Jun 05, 2020, 2:30:32 PM - All 5 devices are reporting a 'CLOSED' state'<br>
	Fri, Jun 05, 2020, 2:30:32 PM - Sending PushOver 'Fri, Jun 05, 2020, 2:30:32 PM - All 5 devices are reporting a 'CLOSED' state''<br>
	Fri, Jun 05, 2020, 2:30:32 PM - All Devices -> POSTING Device Updates to SmartThings API<br>
	Fri, Jun 05, 2020, 2:30:32 PM - Sending PushOver 'NODEjs Client.prototype.connectStream'<br>
	Fri, Jun 05, 2020, 2:30:32 PM - Pushover Message {"status":1,"request":"9"} sent successfully<br>
	Fri, Jun 05, 2020, 2:30:32 PM - Post Accepted Status: 200<br>
	Fri, Jun 05, 2020, 2:30:32 PM - Pushover Message {"status":1,"request":"2"} sent successfully<br>
	Fri, Jun 05, 2020, 2:30:32 PM - Post Accepted Status: 200<p>
	^C<br>
	Fri, Jun 05, 2020, 2:30:37 PM - Ctrl-C interrupt signal 'SIGINT detected' - Shutting down<br>


11. After confirming no errors and that you can turn on/off your watering devices from SmartThings Tile, run the node application as a detached process in the background:

	```
	node app.js &
```

	* Alternative Options for Appication Startup, Monitoring & Restart
	
		* Install [PM2](https://pm2.keymetrics.io/docs/usage/quick-start/#installation)
		* Install [Docker](https://docs.docker.com/engine/install/debian/) and Build app as Docker Image
