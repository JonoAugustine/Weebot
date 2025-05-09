# Weebot

*A discord bot that isn't another music player*

Weebot is a bot for the social chat platform
[Discord](https://discordapp.com/). <br> Weebot is still under
construction, but you can see a [Feature List](#FeatureList) below as
well as the current [Roadmap](#Roadmap). <br>
Go to the [ChangeLogs](#Log) to see the most recent changes in
development progress.

----
<a name='FeatureList'></a>
**Feature List**
- Settings
    - Set bot nickname.
    - Change the call-sign (or prefix) used to call the bot directly.
    - Allow or disallow the bot to use explicit language.
    - Allow or disallow the bot to use NSFW commands.
    - Server-wide word bans. (Under construction)
    - Allow or disallow the bot to respond to actions not directed to it.
- Commands
    - Utility
        - Note Pads
            - Write and edit Note Pads to keep track of ideas and plans.
            - Lock Note Pads to specific roles, members, and text channels.
        - Reminders (Under construction)
    - Discord Games (Played on Discord chats)
        - Cards Against Humanity (Under construction).
            - Official decks up to Expansion 3.
            - In-sever Custom Decks.
        - "Secrete Message" (Under construction).
    - Novelty/misc
        - Self-destruct messages.
        - List all guilds hosting a Weebot.
        - Ping (Pong)


----
<a name='Roadmap'></a>
**Roadmap**
- Convert it all to Kotlin, oof
    1. (new) VoiceChannel temp mentionable Roles
    2. (transfer) Restrictions
    3. In-chat Reddit sim
    2. Custom commands/responses (regex or prefix)
    2. (transfer) Notepads
 - ~~Finish current basic settings implementations~~
 	- ~~Finish settings methods~~
 	- ~~Add User-joined Greetings~~
 - ~~Create Database to store information about each server using GSON/JSON~~
	- ~~Perhaps a Database package with proposéd classes as:~~
		- ~~*Database.java* Keeps a list of all things to be written to
		file (ie. a Databse object saved with gson)~~
		- ~~*Util* Package containing helper classes that build and
		update the database~~
			- ~~*DatabaseWriter.java*~~
			- ~~*DatabaseReader.java*~~
- ~~Make a NotePat/StickyNote functionality.~~
 - Make *Joke/Insult Generator*(TM)
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
	- Russian Roulette ~~Porn~~ DM (NSFW)

----
<a name='Log'></a>
**Log**
<br>*(New files introduced in a log are marked by ***bold-italics***)*

- 3/12/18
    - *General*
        - ``Launcher#saveTimer`` shortened from 1 min to 30 sec.
        - Solid Implementation of ``NotePadCommand``, though still needs some
        features ([see below](#NPC2))
    - <a name='NPC2'>*NotePadCommand*</a>
        - Several improvments and functionality!
            - Standardized the format of arguments to make parsing consistant and
            not a f**king mess.
            ```
            notes
            notes #
            notes # clear
            notes # toss/trash/bin
            notes # delete/remove #
            notes make [the name]
            notes # write/add <the message>
            notes # insert # <the message>
            notes # edit # <new message>
            ```
            - Ditched the "ALL SWITCHES EVERYWHERE" approach in place of mixed,
            sequential ifs and a final switch statement. since the argument format
            is consistant and somewhat linear in form progression/length, the
            process of parsing can be approched in a less hyper-modular fashion.
            - Added several more actions to interact with a NotePad and Note.
                - MAKE, WRITE, INSERT, EDIT, GET, DELETE, CLEAR, TRASH, FILE
                    - *GET* replies with details about the the requested note.
                    - *FILE* creates a file out of the requested NotePad, sends
                    the file as a reply, then deletes the local file.
            - Private methods for parsing ``NotePads``, ``Actions``, and sending
            generic err messages.
            - Disallow empty Note entries.
            - Delete the invoking message for some actions, to reduce clutter in
            the chat.
        - *NotePad*
            - Added author/editor parameters to writing and editing mehods.
            - Clear all notes method ``public Note[] clear()``
            - More keywords for new ``Actions``
            - *Note*
                - Added more information to keep:
                ```
                private OffsetDateTime lastEditTime;
                private long edits;
                private final long authorID;
                private final ArrayList<Long> editorIDs;
                void edit(String note, User editor) {
                    this.note = note;
                    this.lastEditTime = OffsetDateTime.now();
                    this.edits++;
                    if (!this.editorIDs.contains(editor.getIdLong()))
                        this.editorIDs.add(editor.getIdLong());
                }
                ```
        - ***WeebotSuggestionCommand***
            - A new ``MiscCommand`` to accept suggestions about Weebot development in a list readable to devs only.
        - *BetterMessageEvent*
            - Saves the ``Message`` of the wrapped event, so we don't lose the
            methods of JDA ``Message`` and only make 1 (one) ``.complete()``
            call to speed the thread up.
            - Added reply with file and reply with file + file consumer
            ```
            public void reply(File file, String filename)
            public void reply(File file, String name, Consumer<File> consumer)
            ```
        - *DatabaseManager*
            - Checks if the main ``database.wbot`` matches the last backup. If
            they do not match, then load the backup (assuming there was an
            improper shutdown last time and the database).
- 3/11/18
    - *General*
        - Created a new bot ***[Weebot (TestBuild)](https://discordapp.com/developers/applications/me/444323732010303488)***
        for testings new implementations on. Allowing Weebot to have longer
        uptimes between updates. The login sequence was added and commented out
        in ``Launcher``.
        - Created a [``NotePadCommand``](#NPC1).
        Still needs features finished/implemented.
        - Implemented the creation time variable for ``BetterMessageEvent`` and
        ``Weebot`` using ``OffsetDateTime``
    - <a name='NPC1'>***NotePadCommand & NotePad***</a>
        - A Command and Object for guilds to write persistant notepads/stickynotes
        that can be accessed on the guild channels, removing the need for Google
        Drive for small notes that need to be shared with guild memebers.
        - ``NotePadCommand`` holds a nested class ``NotePad`` that holds a list
        of notes that can be written, edited or deleted.
        - A ``ArrayList`` of ``NotePads`` is kept by a Weebot.
            - ***NotePad***
                - Several overloaded constructors and methods implemented for ``NotePad``.
                - ``NotePad`` has an array of key words that cannot be used as names
                - ***Note***
                    - A ``Note`` is essentially a "BetterString" that keeps track
                    of some extra information beyond the note's content like its
                    creation date & time.
                    - It can be edited and have it's creation time
        - **In its current implmenetation, all aspects of ``NotePadCommand`` need
        to be refractored (and finished...).**
    - ***MiscCommands*** (& *ManageSettingsCommand)*
        - Moved [SelfDestruct](#SDMC1), Spam, and Ping commands to a "container"
         class ``MiscCommands``, since they can't really warrant their own classes.
        - Added variable ``int spamLimit`` to ``Weebot`` and ``ManageSettignsCommand``
        to set a Guild-wide limit on bot spam attacks.
    - *Command*
        - Removed single-arg ``execute(BetterMessageEvent)`` abstract method.
    - *BetterMessageEvent*
        - ``reply(Sring, Consumer<Message>)`` added. Should add more of these.
            - Beginning to feel like it's getting closer and closer to just being
            the jagrosh util CommandEvent....


- 3/9-10/18
    - *General*
        - [``ManagaeSettingsCommand``](#MSC1)
        - Lots of minor code cleaning and optimizations using IntelliJ
        analitics
        - Updated Event flowchart
    - *Command*
        - Made ``String[] cleanArgs(Weebot,String[])`` and variations
        to clean message arguments of the callsign/bot-mention to make
        parsing simpler in ``Commands``.
    - <a name='MSC1'>***ManageSettingsCommand***</a>
        - Moved all settings management commands to a new ``Command``
        class and fixed bugs.
        - Syncronized Methods
    - <a name='SDMC1'> ***SelfDestructMessageCommand*** </a>
        - Command to set a message to "self destruct" after the given
        time. Used as
        ``<callsign><selfdestruct/deleteme> [time] [message]`` where
        the time is default 30 seconds.
    - *HelpCommand*
        - Added placeholder responses.
    - *Weebot*
        - Several s/getters
        - Removed methods now housed in
        [``ManagaeSettingsCommand``](#MSC1)
    - *BetterMessageEvent*
        - ``private final OffsetDateTime CREATION_TIME``
    - *Launcher*
        - More consol logging prints.
- 3/7-8/18
    - *General* A very late update for very many changes
        - Implemented use of ``Command`` classes being called from
        ``Weebot`` instances.
            - (JONO) I need to make a new flowchart of events.
            - <a name='CARR'></a>A ``static final List`` of ``Commands``
            is kept in ``Launcher`` to avoid Gson errors with rebuilding
            from abstract constructors. (Since the ``Launcher`` is not
            serialized)
            - A safe-shutdown method added to ``Launcher`` to disconnect
            all bots and save without interruption
            (see [ShutdownCommand](#SDC1)).
        - Made use of Java multithreadding in the new commands, since
        most of the commands should not hold up the main thread.
    - *Weebot*
        - Implemented ``runCommand(BetterMessageEvent, int)``
            1. Get the command calling argument from the front of the
            ``BetterMessageEvent``'s  args.
            2. Check if the command string applies to any ``Command``
            in the Launcher's list of ``Command``s.
            3. Check if the command is allowed in that ``Channel``.
            4. Run the command.
        - Made ``boolean commandIsAllowed(Command, BetterMessageEvent)``
        to check if the passed ``Command`` matches the a class in the
        diabled command map.
        - Changed reference to hosting guild to the guild's ID to avoid
        a circular reference breaking Gson (even though I have no idea
        where the circular reference is...)
        - Removed the ``COMMANDS`` list ([see this](#CARR))
        - Made banned-commands ``TreeMap`` value a ``List`` of
        ``Command`` classes, so multiple commands can be banned in a
        ``TextChannel``
        - Removed ``devHelp(TextChannel)`` method (to be replaced with
        [``HelpCommand``](#HPC)).
    - *Command*
        - Added 2 abstract methods
        ``void run(Weebot bot, BetterMessageEvent event)`` and
        ``void execute(Weebot bot, BetterMessageEvent event)`` that
        allow for the command to better interact with the bot (e.g. for
        settings changes and games).
        - Changed jagrosh's ``void run`` method to
        ``boolean check(BetterMessageEvent)`` that checks if the command
        call meets the permission requirenemts. (Currently only checks
        against ``ownerOnly`` ``Command`` setting).
        - Added empty constructor that sets everything to ``null``,
        ``false``, or ``0``.
        - A LOT of g/setters.
    - ***ListGuildsCommand***
        - Sends a list of guild names and bot names to non-developers.
        - Private messages developers a more detailed list.
        - Runs a new ``Thread``.
    - <a name='SDC1'>***ShutdownCommand***</a>
        - Begins the shutdown sequence in ``Launcher``:
        <pre><code>
        Launcher.JDA_CLIENT.shutdown();
        Launcher.saveTimer.interrupt();
        DatabaseManager.backUp(Launcher.DATABASE);
        DatabaseManager.save(Launcher.DATABASE);</code></pre>
    - <a name='HPC'>***HelpCommand***</a>
        - Doesn't really do anything but is set up to be implemented.
        - Runs a new ``Thread``.
    - *BetterMessageEvent*
        - Removed queue lambda expression and reverted back complete to
        get arguments.
        - getTextChannel
    - *Database*
        - Made stataic variables...not static. (*unstaticized?*).
        Makes more since this way, since a database is ***a*** Database
        and can differe from another save file (in theory).
    - *DatabaseManager*
        - Set Gson to pretty printing cuz why not.
- 3/6/18
    - *General*
        - Moving closer to a modified attempt at seperate ``Command``
        classes to excecute actions, while using the Weebots as a
        database for settings and games to draw from.
    - ***Command***
        - Implemented a modified ad rather stripped down jagrosh
        [JDA-util pack's](https://github.com/JDA-Applications/JDA-Utilities)
        Command abstract class.
    - *Weebot*
        - Removed ``GuildSettings`` class and moved all variables back
        to Weebot proper.
            - Weebot empty constructor sets up a private bot.
            with a null ``Guild`` and an ID of 0W.
            - Made disabled commands a map with commands mapped to
            channels for more specific banning of commands.
        - Added methods ``read(BetterMessageEvent)`` and
        ``matchesCommand(String)`` for taking in ``BetterEvents`` that
        call commands.
    - *BetterMessageEvent*
        - ```getGuild, getSelfUser,```getSelfMmeber
    - *BetterEvent*
        - Changed abstract reply methods to String parameters instead of
        ``Message``.
        - getJDA
- 3/5/18
    - *General*
        - Database working using JSON/GSON.
        - Weebot given now reader method to take BetterEvents.
    - *Launcher*
        - Serious restructuring.
        - JDA login and listener adding seperated into ``jdaLogIn`` and
        ``addListeners``.
        - Expanded ``setUpDatabase`` to add guilds from JDA to new
        databases.
        - Periodic back up thread ``startSaveTimer`` saves a backup every
        *x* minuets until the shutdown signal is received, in which case
        it will save a backup then save the main file.
        - Added ``getGuild`` by long ID method.
        - The main method now flows as such:
            <pre><code>
            Launcher.jdaLogIn();
            Launcher.setUpDatabase();
            Launcher.startSaveTimer(0.5);
            Launcher.addListeners();</code></pre>
    - *EventDispatcher*
        - Removed all methods other than ``onGuildJoin`` and ``onGenericMessave``.
    - *BetterEvent*
        - New exception ``InvalidEventException`` to warn of events
        attempted to be used in the wrong ``BetterEvent`` implementation.
    - *BetterMessageEvent*
        - Added ``InvalidEventException`` thro to constructor. Thrown
        on selfUser author.
    - *Weebot*
        - Replaced reference to hosting Guild with the guild long ID.
            - This cahnge *was* initially made to aid in issues with Java's
            Serialization, since most if not all of the JDA was not built
            with serialization in mind (I would assume). However, now that
            ``DatabseManager`` uses JSON/GSON to store information in files,
            the need for this change is somewhat non-existent and may
            very well be reverted.
        - ``validateCallsign(String[])`` returns -1 for an array of length 0 .
        - readEvent with initial implementation.
            - Since it takes in any child of BetterEvent, it will (quite
            unfortunately) need a series of if-else checking the type
            of evemt (*optionally, a BetterEventType enum may be made later*).
            - Old ``read`` methods are being deconstructed and refractored in
            ``private void runCommand(String[] args, int startIndex)``
            which will now be used to find and call the appropriate
            command (*unimplemented*).
        - ``ChangeNickName`` and ``listGuilds`` refractored to support
        BetterEvent implementation.
        - ``toString`` overrided to print: botID, Guild name, and guild
        bot nickname.
    - *DatabaseManager*
        - Refractored to read and write JSON format to files of
        ``database.wbot`` using [Gson](https://github.com/google/gson).
        - Backup method added, saves to ``databaseBK.wbot``.
    - *Database*
        - Removed databse name value.
- 2/5/18
    - *General*
        - Made a flowchart showing the flow of events and hand-offs between
        the classes and discord.
        - Major progress on Database
        - Considering making costum seperate Command classes taking
        inspiration from the [JDA-util pack](https://github.com/JDA-Applications/JDA-Utilities).
    - ***DatabaseManager*** (*previously known as DatabaseIO*)
         - Writes databse object to file with ObjectOutputStream in format
         <b><ode>databse.wbot</code></b> .
         - All methods are syncronized.
    - *Launcher*
        - Refractoring after database and betterEvent reworks (see below).
        - Static Database reference held in Launcher.
        - getGuilds replaced by getDatabase.
        - updateServers replaced by setUpDatabse
            - Loads database from file, failing that, it makes a new database.
        - getJDA.
    - *Database*
        - Removed saving and loading functions, converted Database class
        to just a databse contatining objects with syncronized access methods.
        - The databse is now saved and loaded by the DatabseManager class
        (see below).
    - <a name='BetterEvent_2/5/18'></a>***BetterEvent***
        - An abstract parent class for BetterMessageEvent and any future
        event wrappers.
            <pre><code>
            protected abstract Event getEvent();
            protected abstract User getAuthor();
            protected abstract void reply(Message message);
            protected abstract void privateReply(Message message);
            public BetterEvent(Event event){} </code></pre>
    - *BetterMessageEvent*
        - Extended new BetterEvent class and implemented new methods
        ([see BetterEvent](#BetterEvent_2/5/18)).
    - *EventDispatcher*
        - Implemented onGenericMessage event method override and
        primitive onGuildJoin.
    - *Weebot*
        - Added new ``validateCallsign`` method that takes a String[ ]
        - Added readEvent method to take in BetterEvents (*unimplemented*).
        - getBotId()
    - *Player*
        - Added methods for sending player a private message.
    - *CardGame*
        - Two new constructors, both with a weebot and one with a player
        array.
    - *CardsAgainstHumanity*
        - Implemented the two new from super contructors.
        - Javadocs
- 1/5/18
    - *General*
        - (JONO) After ***finally*** understanding the flow of the
        [JDA-util pack](https://github.com/JDA-Applications/JDA-Utilities)
        I decieded that the Weebot will not use the package in the intended
        fashion, if at all. Seperate ``Command`` classes are still under
        consideration, but these would be called by a Weebot object,
        rather than as a ``Listener`` as most bots use them (from what
        I hae gathered). <br>(See BetterMessageEvent and EventDispatcher)
        - The idea of a seperate ``Settings`` object to be held by a
        Weebot is growing ever more attractive
        - [Yui bot](https://github.com/DV8FromTheWorld/Yui)
        after much readign has proven to be a useful guid
        on how to go about some actions like Databases and GuildSettings.
        I used the [Command.java implementation](https://github.com/DV8FromTheWorld/Yui/blob/master/src/main/java/net/dv8tion/discord/commands/Command.java)
        as inspiration for ``BetterMessageEvent`` methods.
    - ***BetterMessageEvent***
        - A wrapper class for [``GenericMessageEvent``](http://home.dv8tion.net:8080/job/JDA/javadoc/net/dv8tion/jda/core/events/message/GenericMessageEvent.html)
        to give a more convienient abstraction from message events coming
         into the program.
        - The BetterMessageEvent provides *rather convienient* methods
                and variables that make the Weebot's job of interacting
                with both public and private chats much simpler.

                    private final GenericMessageEvent EVENT;
                    private final User AUTHOR;
                    private final String[] ARGUMENTS;
                    public BetterMessageEvent(GenericMessageEvent event)
                    //Replies to the message event in the same channel
                    public void reply(String message)
                    public void reply(Message message)
                    //Replies to the message event in a private channel
                    public void privateReply(String message)
                    public void privateReply(Message message)
    - ***EventDispatcher***
        - The "mailroom" of incoming events.
        - Currently unimplemented (beyond a ton of empty methods)
        - PLAN: Wrap incoming events in ``BetterMessageWrapper`` where
        logical, then distribute the event to the appropriate Weebot.
    - *Weebot*
        - Changed SERVER_ID to BOT_ID consisting of the host Guild's unique ID
        \+ "W" (e.g. 1234W).
        - Fixed some warnings about "extraineus" code.
        - Added some comments and Copyright.
    - *Game*
        - Added HOST_ID string to add to connection to hosting Guild.
     - *Rest In Pieces*
        - *PrivateListener & GuildListener* was removed, since it will
        be replaced by the EventDispatcher.
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
		- Parses commands by using ``String.split(regex, String[])``.
		Making looking at specific arguments a *lot* simpler.
		- Removed ugly & clunky if-else blocks for a ***clean***
		``switch`` statement based off the parsed ``command`` array.
		- Refractored most methods using switches and the command array
	- *comparators.Comparators*
		- Moved all custom comparators to a seperate package as nested
		classes in ``Comparators.java``
	- *GuildListener*
		- Uses Launcher JDA to ignore messeges sent by Weebot. (1a29acd)
	- *Cards Against Humanity*
		- Implemennted part of ``int endGame()`` to decide the winners
		of the game.
	- *Game*
		- Two new constructors for abstract ``Game`` class.
		- Changed ``PLAYERS`` from ``ArrayList`` to ``TreeMap`` using
		the players' ``User`` as keys.
		- Made Iterable getter for ``TreeMap Game.PLAYERS`` to make
		iterating easier where needed. Returns  ``ArrayList<Player>``
		- isRunning()
		- TreeMap getPlayers()
	- *Player*
		- made getUser() public (was protected)
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
		- Considering moving Weebot server-specific settings to a
		seperate Settings class, possibly with a number of enums for
		simple settings.
	- *Database*
		- Began research on java [serialization](https://www.tutorialspoint.com/java/java_serialization.htm) to save the state of Weebots.
		- I (Jono) am considering moving the ```GUILDS``` Map and
		refocusing it on Weebot objects (instead of Guilds) to make
		reading serializtions not require the list of guilds.
		Alternativly, we could save Guilds.
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
