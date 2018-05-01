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

 - Finish current basic settings implementations
 	- Finish settings methods
 	- Add User-joined Greetings
 - Create Database to store information about each server using GSON/JSON
	- Perhaps a Database package with proposéd classes as:
		- *Database.java* Keeps a list of all things to be written to file (ie. a Databse object saved with gson)
		- *Util* Package containing helper classes that build and update the database
			- *DatabaseWriter.java*
			- *DatabaseReader.java*
 - Work on *Joke/Insult Generator*(TM)
	- Possibly using a pool of joke templates and drawing from a pool of random words and phrases
 -  Make *Cards against NL*
	- Send private messages to each player with their decks, send the white-card into the chat.
 - Research chat-bot implementations for more natural conversations
	 - Possibly add a *sass-scale*(TM) to determine the level of jokes/insults/casual form with specific members.
		 - Will need to track stats and information about each individual member and define what acts increase or decrease relationship status with Weebot.
		 - ? Should User relation to Weebot be gloabal or local 
		* ? This would likelly invole a User wrapper class to keep track of the relationship
		* ? Though that would probably be needed anyway if we are to implement the
		* ? User-bot good/bad relationship meter thing (which I really wannt to)
- Fun Features (<>funhouse)
	- Russian Roulette Kick
	- Russian Roulette Porn DM (NSFW)
	- <>porn @Member searches for porn of member name	
- Localize to Japanese

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
- 28-30/4/18
	- *General*
		- **Serious repackaging has finnally ended in proper building & importing!**
		- Compiled gradle wrapper for cross-platofrm gradle-less systems.
		- More research on object Serialization and MySQL Databases.
		- The bot had a glitch that spammed all chats responding to itself for an hour while Jono slept (FIXED at dac944a).
		- Added Dernst as new registered Dev.
		- Collected some feature Ideas from friends.
		- Carefully considering [JDA-util pack](https://github.com/JDA-Applications/JDA-Utilities), not sure if it is the best path considering current communication paths (Listener->Bot(do a thing))
	- *Weebot*
		- **Major refractoring of command parsing and responding**
		- Parses commands by using ``String.split(regex, String[])``. Making looking at specific arguments a *lot* simpler.
		- Removed ugly & clunky if-else blocks for a ***clean*** ``switch`` statement based off the parsed ``command`` array.
		- Refractored most methods using switches and the command array
	- *comparators.Comparators*
		- Moved all custom comparators to a seperate package as nested classes in ``Comparators.java``
	- *GuildListener*
		- Uses Launcher JDA to ignore messeges sent by Weebot. (1a29acd)
	- *Cards Against Humanity*
		- Implemennted part of ``int endGame()`` to decide the winners of the game.
	- *Game*
		- Two new constructors for abstract ``Game`` class.
		- Changed ``PLAYERS`` from ``ArrayList`` to ``TreeMap`` using the players' ``User`` as keys.
		- Made Iterable getter for ``TreeMap Game.PLAYERS`` to make iterating easier where needed. Returns  ``ArrayList<Player>``
		- isRunning()
		- TreeMap getPlayers()
	- *Player*
		- made getUser() public (was protected)
