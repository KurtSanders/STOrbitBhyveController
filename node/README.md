# STOrbitBhyveController Proxy Node
#### * SmartThings® Integration for the b•hyve™ hose faucet timers *
### Alpha Testing Version: 0.0.2

---

## Description:
### This alpha proxy node server provides: 
1. Support for controlling 'open' or 'closed' Bhyve states from SmartThings.   
2. Pushover Messaging Integration
3. Builds upon the base code for interfacing SmartThings and b•hyve™ with a Node.js/MQTT/Websocket server for real-time response.

## Requirements:

1. Lots of patience and consideration for this very rough alpha release that will require several more hours/days/weeks of debugging and re-testing.  *I am not a Javascript/Node developer by nature, but I feel this language/platform is optimal for creating the asynchronous Websocket connection and API integration with SmartThings.*
2. A **'Node.js'** compatible server on local lan
	* (e.g. Raspberry Pi Server, etc)
		* Model 3 or higher for performance 
		* Debian Version Stretch or Buster
3. Advanced knowledge of Linux commands and system, Node, NPM, Javascript, File Editing, MQTT, etc
4. SmartThings hub
	* Orbit Bhyve Controller SmartApp Installed [Version 4+](https://github.com/KurtSanders/STOrbitBhyveController)
5. Text file editor of your choice (e.g. Nano, etc)


## Installation & Configuration

### **Instructions**

### Orbit Bhyve Controller SmartApp

1. Enable the Orbit Bhyve Controller SmartApp API Setup and obtain the API strings displayed from the ST IDE Live Logging view of Orbit Bhyve Controller SmartApp  ([See 'ST IDE Live Logging View' below for details](https://github.com/KurtSanders/STOrbitBhyveController/tree/master/node#st-ide-live-logging-view))
2. Enter the local IP address of the proxy server and webserver port as required

<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/NodeProxySetup.jpg" width=400>
</p>

### ST IDE Live Logging View

1. When one enters and exist the API Setup section in the Orbit bhyve SmartApp (see above), the SmartThings API and Secret strings will be displayed in the Live Logging view.  This allows one to copy and paste these two strings into the Raspberry Pi's .env file.

<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/ST-IDE-API-Strings.jpg">
</p>
 

### Node.js Server

> These instructions are for a Raspberry Pi proxy server, please modify for alternate servers as needed

_It is highly recommended to make a backup of your server in case you want to return to the state before this install_

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
 	sudo systemctl start mosquitto
 	```
 	
 	`sudo systemctl status mosquitto`	

	> 
	> ● mosquitto.service - Mosquitto MQTT Broker<br>
	> Loaded: loaded (/lib/systemd/system/mosquitto.service; enabled; vendor preset: enabled)<br>
	> Active: active (running)<br>
	>  Docs: man:mosquitto.conf(5)<br>
	>        man:mosquitto(8)<br>
	> Main PID: 13905 (mosquitto)<br>
	> Tasks: 1 (limit: 4915)<br>
	> Memory: 924.0K<br>
	> CGroup: /system.slice/mosquitto.service<br>
	>        └─13905 /usr/sbin/mosquitto -c /etc/mosquitto/mosquitto.conf<br>
	> 	<br>
	> systemd[1]: Starting Mosquitto MQTT Broker...<br>
	> systemd[1]: Started Mosquitto MQTT Broker.<br>
 
3. Create a new folder named 'bhyve' to house the bhyve node.js application files.

	`mkdir bhyve`

4. Change to that directory to make it current.

	`cd bhyve`

5. Download the 'st-orbit-bhyve-controller-x.x.x.tgz' where x.x.x below is the [current version of this file](https://github.com/KurtSanders/STOrbitBhyveController/tree/master/node) to the server's 'bhyve' folder with the wget command below:

	`wget https://github.com/KurtSanders/STOrbitBhyveController/raw/master/node/st-orbit-bhyve-controller-x.x.x.tgz`
	
6. Unpack the downloaded tarball where x.x.x is the [current version](https://github.com/KurtSanders/STOrbitBhyveController/tree/master/node) into the 'bhyve' folder and list the files

	```
	tar -xvf st-orbit-bhyve-controller-x.x.x.tgz --strip-components 1
	ls -la
	```

7. Install st-orbit-bhyve-controller required node modules

	`npm install`

8. Configure Environment .env File

	* Copy the .env-sample template file to .env and edit the .env file to add the required information according to the table below:

	`cp .env-sample .env`
	
	`nano .env`

	* .env data fields (Add field values without any quotes or spaces)

	| Key                  | Type     | description                                       |
	|:---------------------|:--------:|:--------------------------------------------------|
	| `ORBIT_EMAIL=`         | Required | [Orbit Account](https://techsupport.orbitbhyve.com/) Email Address                     |
	| `ORBIT_PASSWORD=`      | Required | [Orbit Account](https://techsupport.orbitbhyve.com/) Password                 |   
	| `MQTT_BROKER_ADDRESS=mqtt://localhost:1883` | Required | MQTT Broker URL   |
	| `MQTT_PASSWORD=`       | Optional | MQTT Broker access password if a password was setup during MQTT install                            |
	| `ST_SMARTAPPURL=`       | Required | OAUTH HAS TO BE ENABLED IN THE ORBIT BHYVE CONTROLLER SMARTAPP IN THE ST IDE.  <br>The SmartThings API Rest string will be displayed in the ST Live Logging Screen when exiting the Orbit Bhyve Controller SmartApp. EXAMPLE:<BR>ST_SMARTAPPURL=https://graph.api.smartthings.com:443/api/smartapps/installations/xxxxxxxx/yyyyyyyyyyyy |
	|`ST_SECRET=`| Required|SmartThings secret string as displayed in ST IDE Live Logging|
	| **Advanced Section**|| (Be careful in changing from the default values below)| 
	| `ST_TEST=false`| Required | Set ST_TEST=true to prevent API POST commands to SmartThings for debugging. ST_TEST=false will send real event data to SmartThings API endpoint |
	| `ST_DEBUG=false`| Required | Set ST_DEBUG=true to generate verbose console messages for debugging, ST_DEBUG=false will restrict debug messages| 
	| `ST_REFRESH_INTERVAL_SEC=15` | Required | ST Polling Interval (Do not exceed a value of 15 or less than 10) |
	| `WEBSERVER_PORT=3000` | Required | If this listening port is changed due to a local server conflict, it must be changed in the ST User Preferences Section |
	| `PUSHOVER_MESSAGING=false`| Required | false is the default and true will send messages to Pushover.  [Pushover Messaging Service](https://pushover.net)   You MUST have a Pushover account to have messages sent to Pushover.  Change PUSHOVER_MESSAGING=true and your USER and TOKEN values are required below. 
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
 

12. After confirming no errors and that you can turn on/off your watering devices from SmartThings Tile, press Ctrl-C to end the process.

13. Run the node application as a detached process in the background:

	- Option 1 (No additional software but limited restart and logging capabilities)
	
		`node app.js &`

	- Option 2 Monitoring & Restart Capabilities (**Preferred**)
	
		* Install [PM2](https://pm2.keymetrics.io/docs/usage/quick-start/#installation)
		
		> PM2 is a production process manager for Node.js applications with a built-in load balancer. It allows you to keep applications alive forever, to reload them without downtime and to facilitate common system admin tasks.
		
		```
		sudo npm install pm2 -g
		pm2 startup
			* Follow instructions and execute the command as displayed
		* nano ecosystem.config.js
			* Edit and verify/change the 'args' string to reflect the correct directory path
			* Save the file
		* pm2 start
		* pm2 save
		* pm2 status
		* pm2 logs
		```
