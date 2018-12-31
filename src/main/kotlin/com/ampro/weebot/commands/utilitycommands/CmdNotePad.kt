/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.utilitycommands

import com.ampro.weebot.Restriction
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_UNDER_CONSTRUCTION
import com.ampro.weebot.commands.CAT_UTIL
import com.ampro.weebot.commands.utilitycommands.NotePad.Note
import com.ampro.weebot.commands.utilitycommands.NotePad.NotePadEdit.EditType
import com.ampro.weebot.commands.utilitycommands.NotePad.NotePadEdit.EditType.*
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.extensions.MentionType.*
import com.ampro.weebot.main.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_CHANNEL
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_GUILD
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.*


/** The maximum number of note pads a Weebot will hold for a single guild  */
internal const val MAX_NOTEPADS = 20
internal const val MAX_NOTES_PER = 100

internal val NOTEPAD_ID_GEN = IdGenerator(5)
internal val NOTE_ID_GEN    = IdGenerator(10)

/** Convert any [String] to a [Note] */
internal fun String.toNote(authorID: Long, initTime: OffsetDateTime)
        : Note = Note(this, authorID, initTime)

/** Convert a list of [String]s to a list of [Note]s */
internal fun List<String>.toNotes(authorID: Long, init: OffsetDateTime)
        : List<Note> {
    return List(size) { Note(this[it], authorID, init) }
}

/**
 * Check if the event can edit the NotePad.
 * @param notePad
 * @return True if Author or Admin <br></br>
 * False if the channel is not allowed. <br></br>
 * False if the Member is not allowed. <br></br>
 * False if the Role is not allowed.
 */
infix fun CommandEvent.canWriteTo(notePad: NotePad): Boolean {
    return when {
        //The Author can always edit
        member.user.idLong == notePad.authorID -> true

        //Admins are admins so ya know...they win
        member.permissions.contains(ADMINISTRATOR) -> true

        //Check Channel
        !notePad.writeRestriction.isAllowed(textChannel) -> false

        //Check Role
        else -> {
            for (r in this.member.roles) {
                if (notePad.writeRestriction.isAllowed(r)) {
                    return true
                }
            }
            return notePad.writeRestriction.isAllowed(member.user)
        }
    }

}

/**
 * Check if the event can edit the NotePad.
 * @param notePad
 * @return True if Author or Admin <br></br>
 * False if the channel is not allowed. <br></br>
 * False if the Member is not allowed. <br></br>
 * False if the Role is not allowed.
 */
infix fun CommandEvent.canRead(notePad: NotePad): Boolean {
    return when {
        //The Author can always edit
        member.user.idLong == notePad.authorID -> true

        //Admins are admins so ya know...they win
        member.permissions.contains(ADMINISTRATOR) -> true

        //Check Channel
        !notePad.readRestriction.isAllowed(textChannel) -> false

        //Check Role
        else -> {
            for (r in this.member.roles) {
                if (notePad.readRestriction.isAllowed(r)) {
                    return true
                }
            }
            return notePad.readRestriction.isAllowed(member.user)
        }
    }

}

/** @return the first [NotePad] of the list or null */
fun List<NotePad>.getDefault() : NotePad? = if (this.isNotEmpty()) this[0] else null

/**
 * A way for members to keep a notepad of ideas and whatnot.
 * Each notepad has a name and [MutableList] of [Note]s
 *
 * A [Weebot] can have up to [MAX_NOTEPADS]
 * concurrent NotePads at once.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
data class NotePad(var name: String, val authorID: Long, val initTime: OffsetDateTime)
    : Iterable<Note> {

    /**
     * A note has a {@link String} note and a creation time & date.
     *
     * @since 1.0
     */
    class Note(var note: String, val authorID: Long, val initTime: OffsetDateTime) {

        val id = NOTE_ID_GEN.next()

        var lastEditTime: OffsetDateTime = initTime

        var edits: Long = 0

        /**
         * Edit the [Note.note] content and update the [NotePad.editHistory]
         *
         * @param edit The new note.
         * @param editor the [User] who edited the note
         * @param time The time of the edit
         */
        fun edit(edit: String, time: OffsetDateTime) : String {
            val old = note // save
            note = edit //edit
            this.lastEditTime = time
            this.edits++
            return old
        }

        fun toEmbed(guild: Guild): EmbedBuilder = strdEmbedBuilder
            .setTitle("Note $id").setDescription(note)
            .addField("Author", guild.getMemberById(authorID)?.effectiveName, true)
            .addField("Created at ${initTime.format(DD_MM_YYYY_HH_MM)}", "", true)
            .addField("Edits: $edits", "Time since last edit: ${ChronoUnit
                .SECONDS.between(lastEditTime, NOW()).formatTime()}", true)
            .addField("Guide", "To Edit: $Pencil\nTo Lock: $Lock\nTo Unlock: $Unlock" +
                    "\nTo delete: $X_Red", true)

        fun toSelectableEmbed(userSet: Set<User>, guild: Guild, notePad: NotePad,
                              finalAction: (Message) -> Unit) : SelectableEmbed {
            return SelectableEmbed(userSet, messageEmbed = toEmbed(guild).build(),
                options = listOf(Pencil to { m1 ->
                    m1.textChannel.sendMessage("To Edit, send the new note:").queue {
                        WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                            { e -> e.isValidUser(users = userSet, guild = e.guild) },
                            { e ->
                                notePad.editNote(this, e.message.contentDisplay, e.author,
                                    e.message.creationTime)
                                val nm = toSelectableEmbed(userSet, guild, notePad,
                                    finalAction)
                                try {
                                    nm.display(m1)
                                } catch (ex: Exception) {
                                    nm.display(e.channel)
                                }
                            }, 2, MINUTES, {
                                it.editMessage("~~${it.contentDisplay}~~ *timed out*")
                                    .queue()
                            })
                    }
                }, Lock to { m -> /*TODO Lock*/ },
                    Unlock to { m -> /*TODO Unlock*/ },
                    X_Red to { m ->
                        strdButtonMenu.setUsers(userSet.first())
                            .setText("*Are you sure you want to delete note:${this.id}?*")
                            .setDescription("${Fire.unicode} = YES,* ***ignore this to cancel***")
                            .addChoice(Fire.unicode).setTimeout(30, SECONDS)
                            .setAction {
                                if (it.toEmoji() == Fire) {
                                    val index = notePad.indexOf(this)
                                    if (index == -1) {
                                        m.channel.sendMessage(GENERIC_ERR_MESG).queue()
                                        return@setAction
                                    } else {
                                        notePad.deleteNote(index, userSet.first(),
                                            m.creationTime)
                                        try {
                                            m.delete().queueAfter(250, MILLISECONDS)
                                            m.channel.sendMessage("*Note deleted.*")
                                                .queueAfter(250, MILLISECONDS)
                                        } catch (ex: Exception) {
                                            m.clearReactions().queueAfter(250, MILLISECONDS)
                                        }
                                    }
                                }
                            }.setFinalAction {}
                            .build().display(m.channel)
                    }
                ), timoutAction = finalAction)
        }

        override fun toString() = this.note

    }

    /**
     * A data class holding information about an Edit made to the [NotePad].
     *
     * @param noteIDs The ids of the [Note]s edited
     * @param editorID the id of the [User] editing
     * @param type the type of edit
     * @param info any additional information about the edit
     * @param time the time of the edit
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    data class NotePadEdit(val noteIDs: List<String>, val editorID: Long,
                           val type: EditType, val info: String = "",
                           val time: OffsetDateTime) {
        constructor(noteIDs: List<String>, editorID: Long, type: EditType,
                    time: OffsetDateTime) : this(noteIDs, editorID, type, "",  time)

        enum class EditType {ADD, EDIT, DELETE, CLEAR, LOCK, UNLOCK}


    }

    val id = NOTEPAD_ID_GEN.next()

    val notes = mutableListOf<Note>()

    /** An ordered history of each time the [NotePad] was edited | Editor, NoteID, Time*/
    val editHistory = mutableListOf<NotePadEdit>()

    val readRestriction = Restriction()
    val writeRestriction = Restriction()
    val size: Int get() = notes.size

    /**
     * Sends the NotePad as an Embed. If the message couldnt be edited then resends.
     */
    fun send(event: CommandEvent, message: Message?, pads: List<NotePad>) {
        val guild = event.guild
        val mem = event.member
        val userSet = setOf(mem.user)
        val items = List<Pair<String, (Int, Message) -> Unit>>(notes.size) {
            "(${notes[it].id}) ${notes[it].note}" to { i, m ->
                try { m.clearReactions().queueAfter(250, MILLISECONDS)}
                catch (e: PermissionException) {}
                notes[i].toSelectableEmbed(userSet, guild, this) {
                    //final
                    try {
                        send(event, m, pads)
                    } catch (e: Exception) {
                        send(event, null, pads)
                        m.delete().queueAfter(1, SECONDS)
                    }
                }.display(m)
            }
        }
        val notePad = SelectablePaginator(users = userSet, title = this.name,
                itemsPerPage = 10, items = items, fields = listOf(
                //Field("Read Restrictions"), TODO NotePadView Restrictions
                //Field("Write Restrictions", ),
                Field("Created: ${initTime.format(DD_MM_YYYY_HH_MM)}","""
                    Edits: ${editHistory.size}
                    ${if(editHistory.isNotEmpty()) """
                    Last edit: ${editHistory[0].type} by ${guild.getMemberById
                    (editHistory[0].editorID) ?.effectiveName ?: getUser(editHistory[0].editorID)?.name ?: "Unknown User"
                } at ${editHistory[0].time.format(DD_MM_YYYY_HH_MM)}""" else ""}
                on notes [${editHistory[0].noteIDs.joinToString(", ")}]
                """.trimIndent(), true)
            )
        ) {
            //final action
            try { it.clearReactions().queueAfter(250, MILLISECONDS) }
            catch (ignored: Exception) {}
            sendNotePads(event, pads, it.textChannel)
            it.delete().queueAfter(1, SECONDS)
        }

        if (message != null) {
            notePad.display(message)
        } else {
            notePad.display(event.channel)
        }
    }

    /**
     * Add notes to the NotePad.
     *
     * @param notes The [String] notes to add
     * @return false if the user cannot write to this [NotePad]
     */
    fun addNotes(user: User, time: OffsetDateTime, vararg notes: String) {
        val ids = mutableListOf<String>()
        var n: Note
        notes.forEach {
            n = Note(it, user.idLong, time); this.notes.add(n); ids.add(n.id)
        }
        recordEdit(ids, user, ADD, time)
    }

    /**
     * Add notes to the NotePad.
     *
     * @param notes The [Note]s to add
     * @return false if the user cannot write to this [NotePad]
     */
    fun addNotes(user: User, time: OffsetDateTime, notes: Collection<String>) {
        val ids = mutableListOf<String>()
        var n: Note
        notes.forEach {
            n = Note(it, user.idLong, time); this.notes.add(n); ids.add(n.id)
        }
        recordEdit(ids, user, ADD, time)
    }

    /**
     * Inserts notes at the given index, pushing the element at that index down.
     *
     * @param index The index to insert the notes at
     * @param notes The notes to insert
     */
    fun insertNotes(author: User, time: OffsetDateTime, index: Int, notes: List<String>) {
        val ids = mutableListOf<String>()
        if (index >= this.notes.size) {
            this.addNotes(author, time, notes)
            return
        }
        try {
            val nn = notes.toNotes(author.idLong, time)
            this.notes.addAll(if (index < 0) 0 else index, nn)
            nn.forEach { ids.add(it.id) }
            recordEdit(List(ids.size) { nn[it].id }, author, ADD, time)
        } catch (e: IndexOutOfBoundsException) {
            this.addNotes(author, time, notes)
        }
    }

    /**
     * Inserts notes at the given index, pushing the element at that index down.
     *
     * @param index The index to insert the notes at
     * @param notes The notes to insert
     */
    fun insertNotes(author: User, time: OffsetDateTime, index: Int, vararg notes: String) {
        val ids = mutableListOf<String>()
        if (index >= this.notes.size) {
            this.addNotes(author, time, notes.toList())
            return
        }
        try {
            val nn = notes.toList().toNotes(author.idLong, time)
            this.notes.addAll(if (index < 0) 0 else index, nn)
            nn.forEach { ids.add(it.id) }
            recordEdit(List(ids.size) { nn[it].id }, author, ADD, time)
        } catch (e: IndexOutOfBoundsException) {
            this.addNotes(author, time, notes.toList())
        }
    }

    /**
     * Edit a note.
     * @param index The index of the note.
     * @param edit The new note.
     * @return The message previously held by the note.
     */
    @Throws(IndexOutOfBoundsException::class)
    fun editNote(note: Note, edit: String, editor: User, time: OffsetDateTime): String {
        val old = note.edit(edit, time) //edit
        recordEdit(listOf(note.id), editor, EDIT, time) //record
        return old
    }

    /**
     * Edit a note.
     * @param index The index of the note.
     * @param edit The new note.
     * @return The message previously held by the note.
     */
    @Throws(IndexOutOfBoundsException::class)
    fun editNote(index: Int, edit: String, editor: User, time: OffsetDateTime): String {
        return editNote(notes[index], edit, editor, time)
    }

    /**
     * @return false if the user cannot edit
     */
    fun deleteNote(note: Note, editor: User, time: OffsetDateTime): Boolean {
        return if (notes.remove(note)) {
            recordEdit(listOf(note.id), editor, DELETE, time)
            true
        } else false
    }

    fun deleteNote(index: Int, editor: User, time: OffsetDateTime) : Boolean {
        return if (index !in 0 until notes.size) false
        else {
            recordEdit(listOf(notes[index].id), editor, DELETE, time)
            true
        }
    }

    /**
     * Remove notes from the NotePad.
     *
     * @param ids The ID Strings to remove
     *
     * @return The removed notes.
     */
    fun deleteNotesById(ids: List<String>, editor: User, time: OffsetDateTime)
            : List<Note> {
        val out = mutableListOf<Note>()
        ids.forEach { id ->
            val i = this.notes.indexOfFirst { it.id.equals(id, true) }
            out.add(this.notes.removeAt(i))
        }
        recordEdit(List(out.size){ out[it].id }, editor, DELETE, time)
        return out
    }

    /**
     * Clear the NotePad's notes
     *
     * @return The removed notes
     */
    fun clear(editor: User, time: OffsetDateTime): List<Note> {
        val out = this.notes.toList()
        this.notes.clear()
        recordEdit(emptyList(), editor, CLEAR, time)
        return out
    }

    /**
     * Add a [NotePadEdit] to the [editHistory]
     *
     * @param noteIDs The ids of the [Note]s edited
     * @param editorID the id of the [User] editing
     * @param type the type of edit
     * @param info any additional information about the edit
     * @param time the time of the edit
     */
    fun recordEdit(ids: List<String>, editor: User, type: EditType,
                   time: OffsetDateTime, info: String = "") {
        editHistory.add(0, NotePadEdit(ids, editor.idLong, type, info, time))
    }

    override fun iterator() = notes.iterator()

    operator fun get(index: Int) = notes[index]

}

/**
 * View and modify [Note Pads][NotePad].
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
 class CmdNotePad : WeebotCommand("notepad",
    arrayOf("notepads", "notes", "jotter", "todo", "note"), CAT_UTIL,
    """notes
        notes make [the name]
        notes #
        notes # file
        notes # clear
        notes # toss/trash/bin
        notes # delete/remove #
        notes # write/add <new message>
        notes # insert # <new message>
        notes # edit # <new message>
        notes # lockto <roles, members, or channels>
        notes # lockout <roles, members, or channels>
        """.trimIndent(), "Write in-discord notes and organize them into NotePds",
    cooldown = 5, guildOnly = true, children = arrayOf(CmdMake(), CmdWriteTo(),
        CmdInsert(), CmdEdit(), CmdDeleteNote(), CmdLockTo(), CmdDeletePad(),
        CmdToFile() )
) {

    class CmdMake : WeebotCommand("make", arrayOf("add", "new"), CAT_UTIL,
        makeArgs, "Make a new NotePad", cooldown = 20,
        cooldownScope = USER_GUILD) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            if (args.isEmpty()) return
            val pads = getWeebotOrNew(event.guild).notePads
            if (pads.size >= MAX_NOTEPADS) {
                event.reply("*The maximum number of NotePads $MAX_NOTEPADS has " +
                        "been reached in this server.*")
                return
            }
            val readRestriction = Restriction()
            val writeRestriction = Restriction()
            val nameList = mutableListOf<String>()
            val notePad: NotePad

            //parse the response
            var currentChar = 'e'

            loop@ for (s in args) {
                when {
                    s matches "-r" -> currentChar = 'r'
                    s matches "-w" -> currentChar = 'w'
                    s matches "-b" -> currentChar = 'b'
                    s matches "-c" -> currentChar = 'c'
                    else -> {
                        when (currentChar) {
                            'e' -> nameList.add(s)
                            else -> {
                                val i = s.parseMentionId()
                                if (i == -1L) continue@loop
                                when (s.mentionType()) {
                                    USER -> when (currentChar) {
                                        'r' -> readRestriction.allowedUsers.add(i)
                                        'w' -> writeRestriction.allowedUsers.add(
                                            i)
                                        'b' -> readRestriction.blockedUsers.add(i)
                                    }
                                    ROLE -> when (currentChar) {
                                        'r' -> readRestriction.allowedRoles.add(i)
                                        'w' -> writeRestriction.allowedRoles.add(
                                            i)
                                        'b' -> readRestriction.blockedRoles.add(i)
                                    }
                                    CHANNEL -> if (currentChar == 'c') {
                                        readRestriction.allowedTextChannels.add(i)
                                    }
                                    else -> {
                                    }
                                }
                            }
                        }
                    }
                }
            }

            notePad = NotePad(nameList.joinToString(" "), event.author.idLong,
                event.creationTime)

            if (!pads.has { it.name.toLowerCase() == notePad.name.toLowerCase() }) {
                pads.add(notePad)
                event.reply("*NotePad \"${notePad.name}\" added!*")
            } else {
                event.reply("*There is already a NotePad by that name.*")
            }
        }
    }

    class CmdWriteTo : WeebotCommand("writeto", arrayOf("write"), CAT_UTIL,
        "<notepad_id> <the note>", "Write to a NotePad", cooldown = 10,
        cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            if (args.size < 2) return
            val pads = getWeebotOrNew(event.guild).notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }

            //<notepad_id/default> <the note>
            var noteStart = 1
            val notePad = cv.find { it.id.equals(args[0], true) } ?: run {
                val def = pads.getDefault()
                if (def != null && !args[0].matches(REG_DEFAULT)) noteStart = 0
                return@run def
            }

            if (notePad != null) {
                if (event canWriteTo notePad) {
                    if (notePad.size >= MAX_NOTES_PER) {
                        event.reply("*This NotePad is full (maximum=$MAX_NOTES_PER)*")
                        return
                    }
                    notePad.addNotes(event.author, event.creationTime,
                        args.subList(noteStart, args.size).joinToString(" "))
                    event.reply("*Note ${notePad.notes.last().id} Added to ${notePad.name}*")
                } else {
                    event.reply("*You do not have permission to write to this NotePad.*")
                }
            } else {
                event.reply("*No NotePad with that ID is available.*")
            }
        }
    }

    class CmdInsert : WeebotCommand("insert", emptyArray(), CAT_UTIL,
        "<notepad_id> <index> <TheMessage>", "Insert a Note into a NotePad",
        cooldown = 10, cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            if (args.size < 2) return
            val pads = getWeebotOrNew(event.guild).notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }

            //<notepad_id> <index> <TheMessage>
            var noteStart = 2
            val notePad = cv.find { it.id.equals(args[0], true) } ?: run {
                val def = pads.getDefault()
                if (def != null && !args[0].matches(REG_DEFAULT)) noteStart = 1
                return@run def
            }
            val insertIndex = try {
                val i = args[noteStart - 1].toInt()
                if (i !in 1..MAX_NOTES_PER) {
                    event.reply("*Please use a number between 1 and $MAX_NOTES_PER*")
                   return
                }
                i - 1
            } catch (e: NumberFormatException) {
                event.reply("*Please use a number between 1 and $MAX_NOTES_PER*")
                return
            }

            if (notePad != null) {
                if (event canWriteTo notePad) {
                    notePad.insertNotes(event.author, event.creationTime, insertIndex,
                        args.subList(noteStart, args.size).joinToString(" "))
                    event.reply("*Note inserted into ${notePad.name}.*")
                } else {
                    event.reply("*You do not have permission to write to this NotePad.*")
                }
            } else {
                event.reply("*No NotePad with that ID is available.*")
            }
        }
    }

    class CmdEdit : WeebotCommand("edit", emptyArray(), CAT_UTIL,
        "<notepad_id> <note_id or num> <TheMessage>", "Edit a note.", cooldown = 10,
        cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            val args = event.splitArgs()
            if (args.size < 2) return
            val bot = getWeebotOrNew(event.guild)
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val pads = bot.notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }

            //<notepad_id> <index> <TheMessage>
            //<notepad_id> <note_id> <TheMessage>
            var noteStart = 2
            val notePad = cv.find { it.id.equals(args[0], true) } ?: run {
                val def = pads.getDefault()
                if (def != null && !args[0].matches(REG_DEFAULT)) noteStart = 1
                return@run def
            }

            if (notePad != null) {
                if (event canWriteTo notePad) {
                    val note = notePad.find { it.id.equals(args[noteStart - 1], true) }
                            ?: run {
                                try {
                                    return@run notePad[args[noteStart - 1].toInt()]
                                } catch (e: NumberFormatException) {
                                    event.reply("*No Note with that ID is available.*")
                                    return
                                } catch (e: IndexOutOfBoundsException) {
                                    event.reply("*There is no note at index ${noteStart - 1}.*")
                                    return
                                }
                            }
                    notePad.editNote(note, args.subList(noteStart, args.size)
                        .joinToString(" "), event.author, event.creationTime)
                    event.reply("*${notePad.name}'s note ${note.id} edited.*")
                } else {
                    event.reply("*You do not have permission to write to this NotePad.*")
                }
            } else {
                event.reply("*No NotePad with that ID is available.*")
            }
        }
    }

    class CmdDeleteNote : WeebotCommand("scratch", arrayOf("erase"), CAT_UTIL,
        "<notepad_id> <note_id or number>", "Delete a Note.", cooldown = 30,
        cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            TODO("not implemented")
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
        }
    }

    class CmdLockTo : WeebotCommand("lockto", arrayOf("allow"), CAT_UTIL,
        "<notepad_id> [@/roles] [@/members] [#/channels]", "Lock access to a NotePad.",
        cooldown = 10, cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            val auth = event.author
            val pads = getWeebotOrNew(event.guild).notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }


            //<notepad_id> [@/roles] [@/members] [#/channels]
            val notePad = cv.find { it.id.equals(args[0], true) }
            if (notePad != null && event canWriteTo notePad) {
                TODO()
            }


        }
    }

    class CmdDeletePad : WeebotCommand("delete", arrayOf("del", "rem", "remove", "bin"),
        CAT_UTIL, "<notepad_id> [notepad_id2..]", "Delete one or more NotePads",
        cooldown = 30, cooldownScope = USER_GUILD) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            val pads = getWeebotOrNew(event.guild).notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }
            val cr = pads.filter { event canWriteTo it }

            val notePads = cr.filter { args.contains(it.id.toUpperCase()) }
            when {
                notePads.isNotEmpty() -> {
                    event.reply(
                        "Are you sure you want to delete NotePad(s): *${notePads.joinToString(
                            ", ") {
                            "${it.name} (${it.id})"
                        }}*? *(say ``yes`` or ``no``)*")
                    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                        { it.isValidUser(event.guild, setOf(event.author)) },
                        {
                            if (it.message.contentDisplay.matches(REG_YES)) {
                                if (pads.removeAll(notePads)) {
                                    event.reply("*NotePad(s) deleted.* $heavy_check_mark")
                                    val unUsed = args.filterNot { cr.has {
                                            np -> np.id.equals(it, true) } }
                                    if (unUsed.isNotEmpty()) {
                                        event.reply("I couldn't find any NotePads matching: ${unUsed
                                            .joinToString(", ")}")
                                    }
                                } else {
                                    event.reply("*Uhh...hmm.. sorry something went wrong. Please try again later*")
                                    MLOG.elog("Failed to delete valid NotePads")
                                }
                            } else if (it.message.contentDisplay.matches(
                                        REG_NO)) {
                                event.reply("Ok, deletion cancelled.")
                            }
                        }, 1L, MINUTES) {
                        event.reply("Deletion cancelled (timed out).")
                    }
                }
                notePads.isEmpty() -> {
                    if (cv.any { args.contains(it.id) })
                        event.reply("*You do not have permission to edit these NotePad(s).*")
                    else {
                        event.reply("No NotePad(s) could be found with the given ID(s).")
                    }
                }
                args.isEmpty() -> event.reply("No NotePad ID was provided.")
            }
        }
    }

    class CmdToFile : WeebotCommand("file", arrayOf("tofile", "txt", "asfile"),
        CAT_UNDER_CONSTRUCTION, "<notepad_id>", "Get the NotePad as a .txt file",
        guildOnly = true, cooldown = 90, cooldownScope = USER_GUILD) {

        override fun execute(event: CommandEvent) {
            TODO()
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
        }
    }

    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
        val args = event.splitArgs()
        val auth = event.author
        val pads = getWeebotOrNew(event.guild).notePads
        /** User's Viewable notepads */
        val cv = pads.filter { event canRead it }

        if (args.isEmpty()) {
            SelectableEmbed(auth, mainMenuEmbed.apply {
                if (pads.isNotEmpty() && cv.contains(pads[0])) addEmptyField(
                    "Default NotePad: ${pads[0].name}")
            }.build(), listOf(Eyes to seeNotePads(event, pads, cv),
                MagnifyingGlass to searchForNotePad(event, cv),
                Notebook to addNotePad(event), Pencil to writeToDefault(event, pads),
                EightSpokedAsterisk to setDefaultById(event, pads),
                FileFolder to notePadToFileById(event, cv),
                C to clearNotePadById(event, cv),
                X_Red to deleteNotePadById(event, cv))) {
                try {
                    it.delete().queue()
                } catch (e: Exception) {
                    try {
                        it.clearReactions().queue()
                    } catch (e: Exception) {
                    }
                }
            }.display(event.textChannel)
        } else {
            when (args[0].toLowerCase()) {
                "see", "view", "list" -> seeNotePads(event, pads, cv)(event.message)
                "clear" -> {
                    // ``clear <notepad_id>`` TODO
                }
                "allow", "lockto" -> {
                    //``allow <notepad_id> [@/roles] [@/members] [#/channels]``TODO
                }
                "block", "lockfrom" -> {
                    //block <notepad_id> [@/roles] [@/members] [#/channels] TODO
                }
                else -> {
                    //Check for notepad ID
                    cv.firstOrNull { it.id.equals(args[0], true) }
                        ?.send(event, null, cv) ?: run {
                        event.reply("*No NotePad with ID: ${args[0]} was found.*")
                    }
                }
            }
        }

    }

    /**
     * Send a list of viewable notePads or respond help
     *
     * @param allPads All [NotePad]s in the guild
     * @param viewable The notepads that the uesr can view
     */
    fun seeNotePads(event: CommandEvent, allPads: List<NotePad>,
                    viewable: List<NotePad>) : (Message) -> Unit = {
            when {
                allPads.isEmpty() -> event.respondThenDelete(strdEmbedBuilder.setTitle(
                    "${event.guild.name} has no NotePads").setDescription(
                    "Use ``make [Notepad Name]`` to make a new NotePad").build(), 30)
                viewable.isEmpty() -> event.respondThenDelete(strdEmbedBuilder.setTitle(
                    "${event.guild.name} has no NotePads you can view").setDescription(
                    "Use ``make [Notepad Name]`` to make a new NotePad").build(), 30)
                else -> sendNotePads(event, viewable, event.textChannel)
            }
        }

    fun searchForNotePad(event: CommandEvent, viewable: List<NotePad>)
            : (Message) -> Unit = {
        //TODO Send search criteria and wait for search request
        event.reply("Under Construction")
    }

    /**
     * Send dialogue for adding a new [NotePad]
     *
     * @param event The invoking event
     */
    fun addNotePad(event: CommandEvent) : (Message) -> Unit = { _ ->
        val eb = strdEmbedBuilder.setTitle("Make a NotePad").setDescription(makeArgs).build()
        event.reply(eb) {  m ->
            WAITER.waitForEvent(MessageReceivedEvent::class.java,
                { ev -> ev.isValidUser(event.guild, setOf(event.author)) }, { ev ->
                    (this.children.find { it is CmdMake } as CmdMake).execute(
                        CommandEvent(ev, ev.message.contentDisplay, CMD_CLIENT))
                    m.delete().queueAfter(250, MILLISECONDS)
                }, 3, MINUTES, { event.reply("*Timed Out*") })
        }
    }

    fun writeToDefault(event: CommandEvent, pads: List<NotePad>) : (Message) -> Unit
            = { _ ->
        val notePad = pads.getDefault()
        if (notePad != null) {
            if (event canWriteTo notePad) {
                if (notePad.size < MAX_NOTES_PER) {
                    event.reply("*What would you like to write:*")
                    WAITER.waitForEvent(MessageReceivedEvent::class.java,
                        { it.isValidUser(event.guild, setOf(event.author)) }, {
                            notePad.addNotes(event.author, event.creationTime,
                                it.message.contentDisplay)
                            event.reply("*Note ${notePad.notes.last().id} Added to ${notePad.name}*")
                        }, 3, MINUTES, { event.reply("*Timed Out*") })
                } else event.reply("*This NotePad is full (maximum=$MAX_NOTES_PER)*")
            } else {
                event.reply("*You do not have permission to write to this NotePad.*")
            }
        } else {
            event.reply("*No NotePad with that ID is available.*")
        }
    }

    fun setDefaultById(event: CommandEvent, pads: List<NotePad>) : (Message) -> Unit
            = {
        //TODO check if Moderator then ask for ID
    }

    fun notePadToFileById(event: CommandEvent, viewable: List<NotePad>)
            : (Message) -> Unit = {
        //TODO ask for ID and then send file in DM
        event.reply("Under Construction")
    }

    fun clearNotePadById(event: CommandEvent, viewable: List<NotePad>)
            : (Message) -> Unit = {
        //TODO ask for ID then confirm clear then clear notepad
        event.reply("Under Construction")
    }

    fun deleteNotePadById(event: CommandEvent, viewable: List<NotePad>)
            : (Message) -> Unit = { _ ->
        getNotePadByIdDialogue(event, viewable, { nps ->
            val it = nps.filter { event canWriteTo it }
            if (it.isEmpty()) {
                event.reply("*No NotePads were found. You may not have permission to" +
                        " edit them*")
            }
            event.reply("*Are you sure you want to delete NotePad(s):* ${
            it.joinToString(", ") { "${it.name} (${it.id})" }}? *(say ``yes`` or ``no``)*")
            WAITER.waitForEvent(GuildMessageReceivedEvent::class.java, { e ->
                e.isValidUser(users = setOf(event.author), guild = event.guild)
            }, { event_2 ->
                if (event_2.message.contentDisplay.matches(Regex("(?i)y+e+s+"))) {
                    if (getWeebotOrNew(event.guild).notePads.removeAll(it)) {
                        event.reply("*NotePad(s) deleted.* $heavy_check_mark")
                    } else {
                        event.reply("*Uhh...hmm.. sorry something went wrong. Please try again later*")
                        MLOG.elog("Failed to delete valid NotePads")
                    }
                } else if (event_2.message.contentDisplay.matches(Regex("(?i)n+o+"))) {
                    event.reply("Ok, deletion cancelled.")
                }
            }, 1L, MINUTES) {event.reply("Deletion cancelled (timed out).")}
        })
    }

    val mainMenuEmbed
        get() = strdEmbedBuilder.setTitle("Weebot NotePads").setDescription("""
            $Eyes to see NotePads
            $MagnifyingGlass to search for a NotePad
            $Notebook to add a new NotePad
            $Pencil to write to the default NotePad
            $EightSpokedAsterisk to change the default NotePad (by ID)
            $C to clear all of a NotePad's notes (by ID)
            $FileFolder to get a NotePad as a file (by ID)
            $X_Red to delete a NotePad (by ID)
            """.trimIndent()
    )

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Weebot NotePads", "Write and edit " +
                "server NotePads which can be locked to specificroles, members, " +
                "and channels.\nAny action can be performed through direct " +
                "commands or by opening the NotePad menu with ``notes`` or any " +
                "alias or called directly with the following commands.")
            .setAliases(aliases).addField("Make a NotePad","``make`` or ``add`` then \n$makeArgs")
            .addField("See the contents of a Note Pad", "``<notepad_id>``")
            .addField("See all NotePads", "``see`` or ``view`` or ``list``")
            .addField("Write to a NotePad", """``writeTo <notepad_id> <the note>``
                Write to the default NotePad: ``write [default] <the note>``""".trimIndent())
            .addField("Insert a Note into the NotePad",
                "``insert <notepad_id> <number> <The Message>``")
            .addField("Edit (replace) a Note",
                "``edit <notepad_id> <note_id or number> <New Message>``")
            .addField("Delete a Note from a NotePad",
                "``scratch <notepad_id> <note_id or number>``")
            .addField("Clear a NotePad of all Notes", "``clear <notepad_id>``")
            .addField("Delete a NotePad", "``del <notepad_id>``")
            .addField("Lock access to a NotePad",
                "``allow <notepad_id> [@/roles] [@/members] [#/channels]``")
            .addField("Block a NotePad's access from Roles, Members, or Channels",
                "``block <notepad_id> [@/roles] [@/members] [#/channels]``")
            .addField("Get a NotePad as a text file", "``file <notepad_id>``")
            .build()
    }

    companion object {
        val makeArgs = """
        ```css
        [name] [-r <@ Members or Roles>]
            [-w <@ Members or Roles>]
            [-b <@ Members or Roles>]
            [-c <# channel mention>]
        ```
        ``-r`` (read lock) will allow only the mentioned Members or Roles to see this NotePad
        ``-w`` (write lock) will allow only the mentioned Members or Roles to edit this NotePad
        ``-b`` (block) will block the mentioned users or Roles from seeing this NotePad
        ``-c`` (channel lock) will the NotePad to the mentioned TextChannels
        """.trimIndent()
    }

}

/**
 *
 */
fun getNotePadByIdDialogue(event: CommandEvent, viewable: List<NotePad>,
                           action: (List<NotePad>) -> Unit, timeout: () -> Unit = {}) {
    event.reply("*Please provide the ID(s) of the NotePad(s):*")
    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
        { e -> e.isValidUser(event.guild, setOf(event.author)) }, { e ->
            val IDs = e.message.contentDisplay.split(Regex("\\s+"))
            val notePads = viewable.filter { IDs.contains(it.id) }
            when {
                notePads.isNotEmpty() -> action(notePads)
                IDs.isEmpty() -> event.reply("No NotePad ID was provided.")
                notePads.isEmpty() -> event.reply(
                    "No NotePad(s) could be found with the given ID(s).")
            }
            val unUsed = IDs.filterNot { viewable.has { np -> np.id.equals(it, true) } }
            if (unUsed.isNotEmpty()) {
                event.reply("I couldn't find any NotePad(s) matching: ${unUsed.joinToString(", ")}")
            }
        }, 1L, MINUTES, timeout)
}

/**
 *
 */
fun getNoteByIdDialogue(event: CommandEvent, notes: List<Note>,
                        action: (List<Note>) -> Unit, timeout: () -> Unit = {}) {
    event.reply("Please provide the ID(s) of the Note(s):")
    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
        { e -> e.isValidUser(users = setOf(event.author), guild = event.guild) }, { e ->
            val IDs = e.message.contentDisplay.split(Regex("\\s+"))
            val notes_2 = notes.filter { IDs.contains(it.id) }
            when {
                notes_2.isNotEmpty() -> action(notes_2)
                IDs.isEmpty() -> event.reply("No Note ID was provided.")
                notes_2.isEmpty() -> event.reply("No Note(s) could be found with the given ID(s).")
            }
            val unUsed = IDs.filterNot { notes_2.has { np -> np.id.equals(it, true) } }
            if (unUsed.isNotEmpty()) {
                event.reply("I couldn't find any Note(s) matching: ${unUsed.joinToString(", ")}")
            }
        }, 1L, MINUTES, timeout)
}

/**
 * Send a list of [NotePad]s as a [ButtonPaginator] to the [textChannel]
 *
 * @param textChannel
 * @param pads
 * @param event
 */
internal fun sendNotePads(event: CommandEvent, pads: List<NotePad>, textChannel: TextChannel) {
    buildNotePadPaginator(event, pads).display(textChannel)
}

/**
 * Sends a [NotePad] [ButtonPaginator] as an edit of [message]
 *
 * @param message The message to replace
 */
internal fun sendNotePads(event: CommandEvent, pads: List<NotePad> , message: Message) {
    buildNotePadPaginator(event, pads).display(message)
}

/**
 * Builds a [ButtonPaginator] from the [NotePad] list
 *
 * @param event the inkoving event
 * @param pads The notepads to paginate
 *
 * @return a [ButtonPaginator] consisting of the [NotePad]s given
 */
internal fun buildNotePadPaginator(event: CommandEvent, pads: List<NotePad>)
        : SelectablePaginator {
    val mem = event.member
    return SelectablePaginator(setOf(mem.user), title = "${mem.guild.name} NotePads",
            description = "${mem.guild.name} NotePads available to ${mem.effectiveName}",
            itemsPerPage = 10,
            thumbnail = "https://47eaps32orgm24ec5k1dcrn1"
                    + "-wpengine.netdna-ssl.com/wp-content/uploads/2016/08/"
                    + "notepad-pen-sponsor.png",
            items = List<Pair<String, (Int, Message) -> Unit>>(pads.size) {
                (" ${pads[it].name} (${pads[it].id})") to { i, m ->
                    pads[i].send(event, null, pads)
                }
            }) { m ->
        try { m.clearReactions().queueAfter(250, MILLISECONDS) }
        catch (ignored: Exception) { }
        m.delete().queueAfter(2, SECONDS)
    }
}
