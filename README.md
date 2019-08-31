# STOrbitBhyveController
#### * SmartThings® Integration for the b•hyve™ hose faucet timers *
### Version: 2.03 (Production Master Branch - Stable)
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/icons/readme.png" width="50">[Change-log & Version Release Features](https://github.com/KurtSanders/STOrbitBhyveController/wiki/Features-by-Version)

---

##### *This new version provides support for showing 'open' or 'closed' Bhyve Status and SMS/Push Messages*
	* WebCore™ can be used to detect and act on device valve 'open' or 'closed', Rain Delay, etc events

### Description:

A custom SmartThings® SmartApp and Device Handlers (DTH) which provides a connection to ones Orbit b•hyve™ network attached devices.
This SmartThings application allows one to **view** the state of their [Orbit b•hyve™ devices](https://bhyve.orbitonline.com/hosefaucet/).  


### SmartThings Room Tile and Details View

<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/Screen-HoseTimer0.PNG" width=200>
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/Screen-HoseTimer1.PNG" width=200>
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/Screen-WiFiHub1.PNG" width=200>
</p>

### Action Tiles Integration

<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/screenshots/bhve action tiles.gif">
</p>


## Requirements:

1. One or more of the following [Orbit b•hyve™ Smart Hose Timer and/or Wi-Fi Hub](https://bhyve.orbitonline.com/hosefaucet/) shown below: 
<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/icons/bhyveIcon.png" width=200><br>
<a href="https://www.amazon.com/Orbit-B-hyve-21004-Faucet-Compatible/dp/B0758NR8DJ/ref=sr_1_2?s=lawn-garden&ie=UTF8&qid=1519147062&sr=1-2&keywords=bhyve">Amazon™ Orbit b•hyve™ Model 21004</a>
</p>

2. A supported mobile device with **SmartThings Legacy Client**. *This app will not work in the new Samsung SmartThings App*.  

3. A working knowledge of the SmartThings IDE
	* Installing a SmartApp & DTH from a GitHub repository (see [SmartThings GitHub IDE integration documentation](https://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html?highlight=github) for example instructions and use the Repository Owner, Name and Branch from installation instructions below)

## Installation & Configuration

**GitHub Repository Integration**

Create a new SmartThings Repository entry in your SmartThings IDE under 'Settings' with the following values:

| Owner | Name | Branch |
|------|:-------:|--------|
| kurtsanders | STOrbitBhyveController | master |

**Required Files in your SmartThings IDE Repository**

You will need to use 'Update from Repo' to install into your SmartThings IDE repository:

| IDE Repository    | Filename | Status |
| :---: | :----------| :---:  |
| My SmartApps      | kurtsanders : Orbit Bhyve Controller | **Updated V2.02** |
| My Device Handler | kurtsanders : Orbit Bhyve Sprinkler Timer | **Updated V2.02** |
| My Device Handler | kurtsanders : Orbit Bhyve Bridge | UnChanged from V1 |


**Instructions**

1. Using the 'Update from REPO' button in the 'My SmartApps' SmartThings IDE, check the 'Orbit Bhyve Controller' SmartApp and publish & press Save.  
2. Using the 'Update from REPO' button in the "My Device Handlers" SmartThings IDE, check both the 'Orbit Bhyve Sprinker Timer' and 'Orbit Bhyve Sprinker Bridge' devices.  Publish & press Save.  ([See GitHub IDE integration](https://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html?highlight=github)) from this STOrbitBhyveController(master) repository to your SmartThings IDE.
3. Locate the Orbit Bhyve Control app in the MarketPlace/SmartApps/My Apps list and click to launch the smartapp.
4. Enter your Orbit b•hyve™ username and password to create the integration with SmartThings and b•hyve™.
5. Configure SmartApp prefernces.
6. Save and add devices to a SmartThings room.

**Known Issues & Limitations**

1. 'open' or 'closed' valve attribute watering detection and device updates are controlled by the user polling frequency setting in the SmartApp user preferences.  Please do not set the polling to be excessive.
2. The Enable API for the SmartApp is not functional in the v2 version. A future release will incorporate the ability to activate the b•hyve™ hose faucet timer from the SmartThings SmartApp.
3. The Orbit b•hyve™ Controller SmartApp v2 cannot activate a b•hyve™ hose faucet timer, or length of water duration from the SmartApp.  These controller functions must be done either manually at the b•hyve™ hose faucet timer, or through the b•hyve™ mobile App. 