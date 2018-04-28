# Weebot

*A discord bot that isn't another music player*

Weebot is a bot for the social chat platform [Discord](https://discordapp.com/). <br> Weebot is still under construction, but you can see a [Feature List](#FeatureList) below as well as the current [Roadmap](#Roadmap)

----
<a name='FeatureList'></a>
#Feature List

...

----
<a name='Roadmap'></a>
#Roadmap

 1. Finish current basic settings implementations
 	- Finish settings methods
 	- Add User-joined Greetings
 2. Create Database to store information about each server (plaintext works honestly)
 3. Work on *Joke/Insult Generator*(TM)
	- Possibly using a pool of joke templates and drawing from a pool of random words and phrases
 4.  Make *Cards against NL*
		- Send private messages to each player with their decks, send the white-card into the chat.
 5. Research chat-bot implementations for more natural conversations
	 - Possibly add a *sass-scale*(TM) to determine the level of jokes/insults/casual form with specific members.
		 - Will need to track stats and information about each individual member and define what acts increase or decrease relationship status with Weebot.
 6. Localize to Japanese
----
#Log
- 26/4/18
	- Work Begins!
	- JDA dependencies added (problem with lfs corruption on repository)
	- Launcher.java, Weebot.java, GuildListener.java, [JDABuilder.java](https://github.com/DV8FromTheWorld/JDA/blob/master/src/main/java/net/dv8tion/jda/core/JDABuilder.java)
	- **Launcher** builds host for all Weebots, holds data about all Guilds (Servers) and their Weebots, and global IDs for each registered Weebot Dev.
		- Nested class `GuildComparator` used to sort Guilds in a [`TreeMap<Guild, Weebot>`](https://docs.oracle.com/javase/8/docs/api/index.html?java/util/TreeMap.html)
	- **GuildListener** listens to each bot and hands instructions to each bot according to the event's origin.
	- **Weebot** Holds all the actions and settings of an individual bot.
		- Added Fields:
				
				private final Guild GUILD
				private final String SERVERNAME;
				private final Long SERVERID;
				private final Member SELF;
				private String NICKNAME;
				private String CALLSIGN;
				private boolean EXPLICIT;
				private boolean NSFW;
				private boolean ALWAYSLISTEN;
		- Completed Methods
					
				Constructor( takes in Guild )
				private int validateCallsign( Message ) 
				public void read( Message ) Takes in a Message and calls appropriate helper 

				private void nsfw( TextChannel, String ) Set or Get the NSFW setting
				private void changeNickName( Message, int )
				private void changeCallsign( TextChannel, String ) Set or Get callsign
				private void listServerSettings( TextChannel ) Lists all server settings
				private void spam( Message ) Spam chat with X messages (default 5)
				
				private void devRead( Message, String ) Reads commands for Devs only
				private static void listGuilds( TextChannel ) List all Guilds and their Weebot (Devs only)

		- Known Incomplete/Unimplemented Code
		
				private void explicit( TextChannel, String ) Set or Get the EXPLICIT setting
				private void alwaysListen( TextChannel, String ) Set or Get ALWAYSLISTEN setting
				
				private void devHelp( TextChannel ) List all Developer commands
			
				public class FileWriter() {} For writing server data to the "Database"
- 27/4/18
	- *General*
		- Re-structured project packages (hopefully last time)
	- *Gradle*
		- Converted project to a gradle project (gradle version 4.7-all) with much MUCH effort
	- *Games*
		- Implemented a framework for Games to be run by Weebot. As well as an initial pass at a Cards Against Humanity game.

				Game
				- Game.java
				- Player.java
				- CardGame
					- CardGame.java
					- Card.java
					- CarsAgainstHumanity.java
	- *Weebot*
		- Somehow managed to not actually finish the basic Weebot methods...
		- Considering moving Weebot server-specific settings to a seperate Settings class, possibly with a number of enums for simple settings.
	- *Database*
		- Began research on java [serialization](https://www.tutorialspoint.com/java/java_serialization.htm) to save the state of Weebots.
		- I (Jono) am considering moving the ```GUILDS``` Map and refocusing it on Weebot objects (instead of Guilds) to make reading serializtions not require the list of guilds. Alternativly, we could save Guilds.