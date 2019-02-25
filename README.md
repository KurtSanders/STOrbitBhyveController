# STOrbitBhyveController
---

#### Version 1.0
 
## Description:

A custom SmartThings® SmartApp and Device Handlers (DTH) which provides a connection to ones Orbit b•hyve™ network attached devices.
This SmartThings application allows one to **view** the state of their [Orbit b•hyve™ devices](https://bhyve.orbitonline.com/hosefaucet/).  

This initial version will be updated to allow one to change the B•Hyve operating state of the timers in future updates. 

## b•hyve™ Tile and Details View

{Image placed here}

## Requirements:

1. One or more of the following [Orbit b•hyve™ Smart Hose Timer and/or Wi-Fi Hub](https://bhyve.orbitonline.com/hosefaucet/) shown below: 
<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/STOrbitBhyveController/master/images/icons/bhyveIcon.png" width=200><br>
[Orbit b•hyve™ Model 21004](https://www.amazon.com/Orbit-B-hyve-21004-Faucet-Compatible/dp/B0758NR8DJ/ref=sr_1_2?s=lawn-garden&ie=UTF8&qid=1519147062&sr=1-2&keywords=bhyve)<br>
</p>
2. A supported mobile device with **ST Legacy Client**. *This app will not work in the new Samsung SmartThings App*. 
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

| IDE Repository    | Filename | Status | Version |
| :---: | :----------| :---:  | :---:  |
| My SmartApps      | kurtsanders : Orbit Bhyve Controller | **New**  | 1.0.0 |
| My Device Handler | kurtsanders : Orbit Bhyve Sprinkler Timer | **New** | 1.0.0 |
| My Device Handler | kurtsanders : Orbit Bhyve Bridge | **New** | 1.0.0 |


**Instructions**

1. Using the 'Update from REPO' button in the 'My SmartApps' SmartThings IDE, check the 'Orbit Bhyve Timer' SmartApp and publish & press Save.  
2. Using the 'Update from REPO' button in the "My Device Handlers" SmartThings IDE, check the 'Orbit Bhyve Timer Device' and publish & press Save.  ([See GitHub IDE integration](https://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html?highlight=github)) from this STAmbientWeather(master) repository to your SmartThings IDE.
3. Locate the Balboa Spa Control app in the MarketPlace/SmartApps/My Apps list and click to launch the smartapp.
4. Enter your username and password to create the integration with SmartThings and B•Hyve API.
