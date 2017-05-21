/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Speaker Custom Message
 *
 *  Author: SmartThings
 *  Date: 2014-1-29
 */
definition(
	name: "Speaker Notify with Wrestling Sounds",
	namespace: "thetakerfan",
	author: "TheTakerFan",
	description: "Play a sound or custom message through your Speaker when the mode changes or other events occur.",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@2x.png"
)

preferences {
	page(name: "mainPage", title: "Play a message on your Speaker when something happens", install: true, uninstall: true)
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def mainPage() {
	dynamicPage(name: "mainPage") {
		def anythingSet = anythingSet()
		if (anythingSet) {
			section("Play message when"){
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
				ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
				ifSet "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
				ifSet "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
				ifSet "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
				ifSet "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
				ifSet "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
				ifSet "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
				ifSet "timeOfDay", "time", title: "At a Scheduled Time", required: false
			}
		}
		def hideable = anythingSet || app.installationState == "COMPLETE"
		def sectionTitle = anythingSet ? "Select additional triggers" : "Play message when..."

		section(sectionTitle, hideable: hideable, hidden: true){
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
			ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
			ifUnset "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
			ifUnset "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
			ifUnset "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
			ifUnset "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
			ifUnset "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
			ifUnset "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true
			ifUnset "timeOfDay", "time", title: "At a Scheduled Time", required: false
		}
		section{
			input "actionType", "enum", title: "Action?", required: true, defaultValue: "Bell 1", options: ["Wrestling"]
		}
		section {
			input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: true
		}
		section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
		}
		section("More options", hideable: true, hidden: true) {
			input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			if (settings.modes) {
            	input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            }
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}

private anythingSet() {
	for (name in ["motion","contact","contactClosed","acceleration","mySwitch","mySwitchOff","arrivalPresence","departurePresence","smoke","water","button1","timeOfDay","triggerModes","timeOfDay"]) {
		if (settings[name]) {
			return true
		}
	}
	return false
}

private ifUnset(Map options, String name, String capability) {
	if (!settings[name]) {
		input(options, name, capability)
	}
}

private ifSet(Map options, String name, String capability) {
	if (settings[name]) {
		input(options, name, capability)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
	subscribe(button1, "button.pushed", eventHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}

def eventHandler(evt) {
	log.trace "eventHandler($evt?.name: $evt?.value)"
	takeAction(evt)
}
def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

def scheduledTimeHandler() {
	eventHandler(null)
}

def appTouchHandler(evt) {
	takeAction(evt)
}

private takeAction(evt) {

	log.trace "takeAction()"
	if (actionType=="Wrestling") {
		def theme = get_theme()
		log.trace "Chose ${theme[0]}, playing ${theme[2]} for ${theme[1]} seconds"
		state.sound = [uri: "file:///home/pi/Music/Wrestling/" + theme[2], duration: theme[1]]
		sonos.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
			
		//hues*.stopLoop()
		log.trace "Lights ${theme[3]?.size()} - ${theme[3]}"
		if (theme[3]?.size() > 0) {
			rotateLights(theme[3], theme[1])
			return
		}
		switch (theme[3]?.size()) {
			case 3://Single color (one array with 3 parameters)
				//hues*.setColor([hue: theme[3].hue, saturation: theme[3].saturation, level: theme[3].level])
				def newlight = theme[3].clone()
				newlight.level = theme[3].level / 4
				swapLights([theme[3], newlight], theme[1])
				//hues*.setColor(theme[3])
				//hues*.setLevel(theme[3])
				break
			case 2://Two colors (two arrays)
				swapLights(theme[3], theme[1])
				break
			default:
				hues*.setColor([hue: 80, saturation:96, level:100], 0)
				hues*.startLoop([direction: "Up", time: 3])
				runIn(theme[1], stopLoops, [overwrite: true])
		}

	}
	else {
		sonos.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
	}
	log.trace "Exiting takeAction()"
}

def rotateLights(colors, duration) {
	def bulbs = hues.size()
	def num_colors = colors.size()
	def delay = 3
	def bulb = 0
	for (bulb = 0; bulb < bulbs; bulb++) {
		if (num_colors <= bulb) {
			log.trace "Check: ${num_colors} ; ${bulb} ; ${colors}"
			colors.putAt(bulb, colors[bulb-1].clone())
			colors[bulb].level = colors[bulb].level / 2
			num_colors++
		}
		log.trace "Done: ${num_colors} ; ${bulb} ; ${colors}"
		hues[bulb].setColor(colors[bulb], 0)
	}
	log.trace "Colors ${colors}"
	for (int i = 0; i < duration/delay; i = i + num_colors) {
		for (int color = 0; color < num_colors; color++) {
			log.trace "i = ${i}; color = ${color}; Delay: ${(i+color)*delay}; index: ${(i+color)%num_colors}"
			runIn((i+color)*delay,   setLight, [overwrite: false, data: [tick:(i+color)*delay, color: colors[((i+color-1)%num_colors)], bulb: ((i-1)%bulbs)]])
			runIn((i+color)*delay+1, setLight, [overwrite: false, data: [tick:(i+color)*delay, color: colors[(i+color)%num_colors], bulb: (i)%bulbs]])
		}
	}
}
def setLight(data) {
	log.trace "Data: ${data}"
	//log.trace "Hues: ${hues}"
	hues[data.bulb].setColor(data.color)
	
}

def swapLights(colors, duration) {
	//log.trace "Colors ${colors} - ${duration}"
	hues[0].setColor(colors[0])
	hues[1].setColor(colors[1])
	for (int i = 3; i < duration; i = i + 6) {
		//runIn(i, setLights1, [overwrite: false, data: [colors: colors]])
		//runIn(i+2, setLights2, [overwrite: false, data: [colors: colors]])
		runIn(i, setLights, [overwrite: false, data: [colors: colors, even: true]])
		runIn(i+3, setLights, [overwrite: false, data: [colors: colors, even: false]])
	}

}
def setLights(data) {
	//log.trace "Data: ${data}"
	hues[0].setColor(data.colors[(data.even==true) ? 1 : 0],1)
	hues[1].setColor(data.colors[(data.even==true) ? 0 : 1],1)
	
}

def stopLoops() {
    hues*.off()
}


private dayString(Date date) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	df.format(date)
}

private oncePerDayOk(Long lastTime) {
	def result = true
	if (oncePerDay) {
		result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
		log.trace "oncePerDayOk = $result"
	}
	result
}

// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}
//[hue: 70, saturation:93, level:100]],
private get_theme() {
		def colors = [
			"purple" : [hue: 280/3.6, saturation:96, level:100],
			"red" : [hue: 0, saturation:100, level:100],
			"blue" : [hue: 250/3.6, saturation:100, level:100],
			"green" : [hue: 100/3.6, saturation:100, level:100],
			"yellow" : [hue: 50/3.6, saturation:100, level:100],
			"black" : [hue: 99, saturation:0, level:1],
			"white" : [hue: 40,  saturation:5, level:100],
			"pink" : [hue: 320/3.6,  saturation:100, level:100]
		]
		def themes = [
		['Voltron', 53, "Lion_Voltron-_Closing_Sequence.mp3", [colors['red'], colors['blue'], colors['green'], colors['yellow'], colors['black']]],
		  ['Figment', 119, "One_Little_Spark.mp3", [colors['purple']]],
		  ['Undertaker', 25, "WWE/The_Undertaker_-_Ministry_Theme_(Dame_Grease).mp3", [colors['purple']]],
		  ['Razor', 118, "WWE/Razor_Ramon_-_The_Bad_Guy.mp3", [colors['purple'], colors['yellow']]],
		  ['Hogan', 25, "WWE/Hulk_Hogan_-_Real_American_(Rick_Derringer).mp3", [colors['red'], colors['yellow']]],
		  ['Los_Boriquas', 25, "WWE/Los_Boriquas_-_Los_Boricuas.mp3"],
		  ['Eddie_Guerrero', 105, "WCW/Eddie_Guerrero_-_la_Raza.mp3"],
		  ['Shawn_Michaels', 25, "WWE/Shawn_Michaels_-_Sexy_Boy_(Jimmy_Hart_and_Shawn_Michaels).mp3", [colors['red'], colors['white'], colors['blue']]],
		  ['Big_Bossman', 81, "WWE/Big_Bossman_-_Hard_Time.mp3", [colors['blue']]],
		  ['Bret_Hart', 25, "WWE/Bret_The_Hitman_Hart_-_Hitman.mp3", [colors['pink']]],
		  ['Chris_Jericho', 25, "WWE/Chris_Jericho_-_Break_the_Walls_Down_(Jim_Johnston).mp3"],
		  ['Hardys', 25, "WWE/Hardy_Boyz_-_2Xtreme_(Zach_Tempest).mp3"],
		  ['nWo', 25, "WCW/NWO_-_Rockhouse_(Original_Theme).mp3", [[hue: 70,  saturation:93, level:100]]],
		  ['Kurt',177,'WWE/Kurt_Angle_-_You_Suck.MP3', [colors['red'], colors['blue']]],
['Alex',211,'WCW/Alex_Wright_-_Heartbeat_Away.mp3'],
['Harlem',126,'WCW/Booker_T_and_Harlem_Heat_-_Harlem_Rumble.mp3'],
['Buff',74,'WCW/Buff_Bagwell_-_Buff_Daddy.mp3'],
['Dean',155,'WCW/Dean_Malenko_-_The_Iceman.mp3'],
['Disco',144,'WCW/Disco_Inferno_-_Disco_Fever.mp3'],
['Eddie',105,'WCW/Eddie_Guerrero_-_la_Raza.mp3'],
['Goldberg',73,'WCW/Goldberg_-_Invasion.mp3'],
['Hollywood',313,'WCW/Hollywood_Hulk_Hogan_-_Voodoo_Child_%28Jimi_Hendrix%29.mp3'],
['American',215,'WCW/Hulk_Hogan_-_American_Made_%28Jimmy_Hart%29.mp3'],
['Macho',54,'WCW/Macho_Man_Randy_Savage_-_What_Up_Mach.mp3'],
['Midnight',200,'WCW/Midnight_Express_-_The_Chase_%28Giorgio_Moroder%29.mp3'],
['NWO',130,'WCW/NWO_-_Rockhouse_%28Original_Theme%29.mp3'],
['Wolfpac',139,'WCW/NWO_-_Wolfpac_Theme.mp3'],
['Psychosis',168,'WCW/Psychosis_-_Get_Beck.mp3'],
['Rowdy',219,'WCW/Rowdy_Roddy_Piper_-_The_Green_Hills_of_Tyrol.mp3'],
['Sting',145,'WCW/Sting_-_The_Crow_Theme.mp3'],
['Sting',285,'WCW/Sting_-_The_Man_Called_Sting_%28Original%29.mp3'],
['The',204,'WCW/The_Crippler_Chris_Benoit_-_Scattered.mp3'],
['The',166,'WCW/The_Four_Horsemen_%28First_1980s_Theme%29.mp3'],
['The',158,'WCW/The_Four_Horsemen_%28First_1990s_Theme%29.mp3'],
['The',127,'WCW/The_Four_Horsemen_%28Second_1980s_Theme%29.mp3'],
['The',263,'WCW/The_Steiner_Brothers_-_Steinerized_%28Jimmy_Hart%29.mp3'],
['The',125,'WCW/The_Total_Package_Lex_Luger_-_Slammer.mp3'],
['WCW',123,'WCW/WCW_Monday_Nitro_Opening_Theme.mp3'],
['West',107,'WCW/West_Texas_Rednecks_-_Rap_is_Crap.mp3'],
['Ahmed',165,'WWE/Ahmed_Johnson_-_Pearl_River_Rip.mp3'],
['Big',81,'WWE/Big_Bossman_-_Hard_Time.mp3'],
['Billy',173,'WWE/Billy_Gunn_-_Assman_%28Jim_Johnston%29.mp3'],
['Blue',194,'WWE/Blue_Blazer_-_Superhero.mp3'],
['Booker',142,'WWE/Booker_T_-_Can_You_Dig_It.mp3'],
['Bret',120,'WWE/Bret_The_Hitman_Hart_-_Hitman.mp3'],
['British',84,'WWE/British_Bulldog_-_The_British_March.mp3'],
['Brock',153,'WWE/Brock_Lesnar_-_The_Next_Big_Thing.mp3'],
['Brutis',108,'WWE/Brutis_The_Barber_Beefcake_-_Struttin%27_and_Cuttin%27.mp3'],
['Bushwackers',73,'WWE/Bushwackers_-_Walkabout.mp3'],
['Charlie',155,'WWE/Charlie_Haas_-_World%27s_Greatest.mp3'],
['Chris',118,'WWE/Chris_Benoit_-_Rabid.mp3'],
['Chris',195,'WWE/Chris_Jericho_-_Break_the_Walls_Down_%28Jim_Johnston%29.mp3'],
['Chris',237,'WWE/Chris_Jericho_-_King_of_My_World_%28Saliva%29.mp3'],
['Christian',185,'WWE/Christian_-_At_Last.mp3'],
['Corporate',256,'WWE/Corporate_Ministry_-_No_Chance_Remix_%28The_Chris_Warren_Band%29.mp3'],
['Demolition',196,'WWE/Demolition_-_Pain_and_Destruction_%28Rick_Derringer%29.mp3'],
['D-Generation',170,'WWE/D-Generation_X_-_Break_it_Down_%28The_Chris_Warren_Band%29.mp3'],
['D-Generation',229,'WWE/D-Generation_X_-_The_Kings_%28Run_DMC%29.mp3'],
['Diesel',141,'WWE/Diesel_-_Diesel_Blues_%28Jim_Johnston%29.mp3'],
['D%27lo',120,'WWE/D%27lo_Brown_-_The_Real_Deal.mp3'],
['Doink',76,'WWE/Doink_-_March_of_the_Clowns.mp3'],
['Eddie',176,'WWE/Eddie_Guerrero_-_I_Lie_I_Cheat_I_Steal.mp3'],
['Eric',202,'WWE/Eric_Bischoff_-_I%27m_Back.mp3'],
['Evolution',218,'WWE/Evolution_-_Line_in_the_Sand_%28Motorhead%29.mp3'],
['Flash',121,'WWE/Flash_Funk_-_Can%27t_Get_Enough.mp3'],
['Gangrel',127,'WWE/Gangrel_and_The_Brood_-_Blood.mp3'],
['Goldberg',188,'WWE/Goldberg_-_Invasion_2.mp3'],
['Goldust',155,'WWE/Goldust_-_Gold-Lust.mp3'],
['Hardcore',118,'WWE/Hardcore_Holly_-_How_Do_You_Like_Me_Now.mp3'],
['Hardy',145,'WWE/Hardy_Boyz_-_2Xtreme_%28Zach_Tempest%29.mp3'],
['Hillbilly',169,'WWE/Hillbilly_Jim_-_Don%E2%80%99t_Mess_With_A_Country_Boy.mp3'],
['Honky',130,'WWE/Honky_Tonk_Man_-_Cool_Cocky_Bad.mp3'],
['Hulk',177,'WWE/Hulk_Hogan_-_Real_American_%28Rick_Derringer%29.mp3'],
['Hunter',158,'WWE/Hunter_Hearst_Helmsley_-_Ode_to_Joy_%28Ludvig_von_Beethoven%29.mp3'],
['Jake',129,'WWE/Jake_the_Snake_Roberts_-_Snake_Bit.mp3'],
['John',178,'WWE/John_Cena_-_The_Time_Is_Now_%28John_Cena%29.mp3'],
['Kane',153,'WWE/Kane_-_Burned.mp3'],
['Ken',135,'WWE/Ken_Shamrock_-_The_Ultimate.mp3'],
['K-Kwick',190,'WWE/K-Kwick_-_Rowdy.mp3'],
['Legion',73,'WWE/Legion_of_Doom_-_What_a_Rush.mp3'],
['Lex',91,'WWE/Lex_Luger_-_American.mp3'],
['Linda',149,'WWE/Linda_McMahon_-_Theme_From_Wrestlemania.mp3'],
['Los',117,'WWE/Los_Boriquas_-_Los_Boricuas.mp3'],
['Macho',102,'WWE/Macho_Man_Randy_Savage_-_Pomp_and_Circumstance_%28Edward_Elgar%29.mp3'],
['Michael',261,'WWE/Michael_Hayes_and_The_Fabulous_Freebirds_-_BadStreet_USA.mp3'],
['Mickie',53,'WWE/Mickie_James_-_You%27re_So_Fine_%28WWE_Remix%29.mp3'],
['Million',108,'WWE/Million_Dollar_Man_Ted_DiBiase_-_Million_Dollar_Rap.mp3'],
['Mr.',119,'WWE/Mr._McMahon_-_No_Chance_in_Hell_%28The_Chris_Warren_Band%29.mp3'],
['Mr',155,'WWE/Mr_Perfect_-_The_Perfect_Twist.mp3'],
['New',127,'WWE/New_Age_Outlaws_-_Oh_You_Didn%27t_Know.mp3'],
['Owen',122,'WWE/Owen_Hart_-_Enough_is_Enough.mp3'],
['Ravishing',171,'WWE/Ravishing_Rick_Rude_-_Simply_Ravishing.mp3'],
['Ric',138,'WWE/Ric_Flair_-_Also_sprach_Zarathustra_%28Richard_Strauss%29.mp3'],
['Rob',133,'WWE/Rob_Van_Dam_-_One_of_a_Kind_%28Breaking_Point%29.mp3'],
['Rowdy',44,'WWE/Rowdy_Roddy_Piper_-_Scottish_March_%28WWE_Mix%29.mp3'],
['Sable',118,'WWE/Sable_-_Wildcat.mp3'],
['Smoking',137,'WWE/Smoking_Gunns_-_Smokin%27.MP3'],
['Stone',182,'WWE/Stone_Cold_Steve_Austin_-_I_Won%27t_Do_What_You_Tell_Me_%28Jim_Johnston%29.mp3'],
['Sunny',123,'WWE/Sunny_-_I_Know_You_Want_Me.mp3'],
['Sycho',126,'WWE/Sycho_Sid_-_Snapped.mp3'],
['Tatanka',86,'WWE/Tatanka_-_War_Dance.MP3'],
['Tazz',137,'WWE/Tazz_-_13.mp3'],
['The',72,'WWE/The_American_Dream_Dusty_Rhodes_-_American_Dream.mp3'],
['The',269,'WWE/The_Big_Show_-_Crank_It_Up_%28Brand_New_Sin%29.mp3'],
['The',84,'WWE/The_British_Bulldog_-_The_British_March.mp3'],
['The',143,'WWE/The_Godfather_-_Pimpin%27_Ain%27t_Easy.mp3'],
['The',108,'WWE/The_Million_Dollar_Man_Ted_DiBiase_-_Million_Dollar_Rap.mp3'],
['The',63,'WWE/The_Rockers_-_Rockin%27_Rockers.mp3'],
['The',94,'WWE/The_Rock_%28heel%29_-_If_You_Smell..._%28Hollywood_Remix%29.mp3'],
['The',204,'WWE/The_Rock_-_If_You_Smell....mp3'],
['The',86,'WWE/The_Texas_Tornado_Kerry_Von_Erich_-_Texas_Tornado.mp3'],
['The',143,'WWE/The_Undertaker_-_The_Darkest_Side__%28Original_Theme%29.mp3'],
['Triple',244,'WWE/Triple_H_-_King_of_Kings_%28Motorhead%29.mp3'],
['Triple',186,'WWE/Triple_H_-_Our_Time_%28The_Chris_Warren_Band%29.mp3'],
['Triple',204,'WWE/Triple_H_-_The_Game_%28Motorhead%29.mp3'],
['Ultimate',103,'WWE/Ultimate_Warrior_-_Unstable.mp3'],

		]
		def index = new Random().nextInt(themes.size())
		//def index = new Random().nextInt(2)
		log.trace "${index} index chosen"
		themes[index]
}
