/*
TuyAPI SmartPlug Device Handler

Derived from
	TP-Link HS Series Device Handler
	Copyright 2017 Dave Gutheinz


Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:
		http://www.apache.org/licenses/LICENSE-2.0
		
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Supported models and functions:  This device supports smart plugs that use the Tuya Smart Life app

Update History
01-04-2018	- Initial release
*/
metadata {
	definition (name: "TuyAPI Smart Plug&Strip", namespace: "slasherLee", author: "SeungCheol Lee") {
		capability "Switch"
		capability "refresh"

	}
	tiles(scale: 2) {
		standardTile("switch", "device.switch", width: 6, height: 4, canChangeIcon: true) {
        	state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"waiting"
			state "turningOff", label:'waiting', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#15EE10", nextState:"waiting"
			state "waiting", label:'${name}', action:"switch.on", icon:"st.switches.switch.on", backgroundColor:"#15EE10", nextState:"on"
			state "offline", label:'Comms Error', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#e86d13", nextState:"waiting"
		}
		standardTile("refresh", "device.refresh", width: 2, height: 2,  decoration: "flat") {
			state ("default", label:"Refresh", action:"refresh.refresh", icon:"st.secondary.refresh")
		}		 
		main("switch")
		details("switch", "refresh")
	}
}
preferences {
	input(name: "deviceIP", type: "text", title: "Device IP", required: true, displayDuringSetup: true)
	input(name: "gatewayIP", type: "text", title: "Gateway IP", required: true, displayDuringSetup: true)
	input(name: "deviceID", type: "text", title: "Device ID", required: true, displayDuringSetup: true)
	input(name: "localKey", type: "text", title: "Local Key", required: true, displayDuringSetup: true)
    input(name: "dps", type: "text", title: "Plug Number", required: true, displayDuringSetup: true)
}

def installed() {
	updated()
}

def updated() {
	unschedule()
	runEvery15Minutes(refresh)
	runIn(2, refresh)
}
//	----- BASIC PLUG COMMANDS ------------------------------------
def on() {
	sendCmdtoServer("on", dps, "deviceCommand", "onOffResponse")
}

def off() {
	sendCmdtoServer("off", dps, "deviceCommand", "onOffResponse")
}

def onOffResponse(response){
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod onOffResponse")
	} else {
    	if (response.headers["cmd-response"] == "true") {
            def cmd = response.headers["onoff"]
        	sendEvent(name: "switch", value: cmd, isStateChange: true)
            }
    }
	//refresh()
}

//	----- REFRESH ------------------------------------------------
def refresh(){
	//sendEvent(name: "switch", value: "waiting", isStateChange: true)
	sendCmdtoServer("status", dps, "deviceCommand", "refreshResponse")
}
def refreshResponse(response){
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod onOffResponse")
	} else {
        def status = response.headers["cmd-response"]
		log.info "${device.name} ${device.label}: Power: ${status}"
		sendEvent(name: "switch", value: status)
	}
}

//	----- SEND COMMAND DATA TO THE SERVER -------------------------------------
private sendCmdtoServer(command, dps, hubCommand, action){
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8083")	//	SET TO VALUE IN JAVA SCRIPT PKG.
	headers.put("tuyapi-ip", deviceIP)
	headers.put("tuyapi-devid", deviceID)
	headers.put("tuyapi-localkey", localKey)
	headers.put("tuyapi-command", command)
	headers.put("command", hubCommand)
    headers.put("dps", dps)
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: action]
	))
}
