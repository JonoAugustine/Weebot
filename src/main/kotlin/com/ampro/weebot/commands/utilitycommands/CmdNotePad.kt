/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.utilitycommands

import com.ampro.weebot.Restriction
import com.ampro.weebot.commands.CAT_UTIL
import com.ampro.weebot.commands.utilitycommands.NotePad.Note
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.IdGenerator
import com.ampro.weebot.util.NOW
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.User
import java.time.OffsetDateTime


/** The maximum number of note pads a Weebot will hold for a single guild  */
internal val MAX_NOTEPADS = 20
internal val MAX_NOTES_PER = 100

internal val NOTEPAD_ID_GEN = IdGenerator(5)
internal val NOTE_ID_GEN    = IdGenerator(10)

/** List of words that cannot be used as [NotePad] names, to avoid parsing err. */
internal val keyWords = listOf(Regex("(?i)(make)"), Regex("(?i)(write)"),
    Regex("(?i)(add)"), Regex("(?i)(insert)"), Regex("(?i)(insert)"),
    Regex("(?i)(edit)"), Regex("(?i)(delete)"), Regex("(?i)(remove)"),
    Regex("(?i)(lockto)"), Regex("(?i)(lockout)"), Regex("(?i)(clear)"),
    Regex("(?i)(toss)"), Regex("(?i)(trash)") ,Regex("(?i)(bin)"))

/**
 * Check if a string is a reserved [NotePad.keyWords].
 * @param test The [String] to test.
 * @return `true` if test does not match any keyWord.
 */
internal fun String.isOk(test: String) = test.matchesAny(keyWords)

/** Convert any [String] to a [Note] */
internal infix fun String.toNote(authorID: Long) : Note = Note(this, authorID)
/** Convert a list of [String]s to a list of [Note]s */
internal infix fun List<String>.toNotes(authorID: Long) : List<Note> {
    return List(size) { Note(this[it], authorID) }
}

/**
 * Check if the event can edit the NotePad.
 * @param event
 * @return True if Author or Admin <br></br>
 * False if the channel is not allowed. <br></br>
 * False if the Member is not allowed. <br></br>
 * False if the Role is not allowed.
 */
infix fun CommandEvent.canWrite(notePad: NotePad): Boolean {
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
 * A way for members to keep a notepad of ideas and whatnot.
 * Each notepad has a name and [MutableList] of [Note]s
 *
 * A [Weebot] can have up to [MAX_NOTEPADS]
 * concurrent NotePads at once.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
data class NotePad(var name: String, val authorID: Long) : Iterable<Note> {

    /**
     * A note has a {@link String} note and a creation time & date.
     *
     * @since 1.0
     */
    data class Note(var note: String, val authorID: Long) {

        val id = NOTE_ID_GEN.next()

        val creationTime: OffsetDateTime = NOW()
        var lastEditTime: OffsetDateTime = NOW()

        var edits: Long = 0

        /** TODO Restrictions used only for user ids  */
        val writeRestrictions: Restriction = Restriction()
        val readRestriction: Restriction = Restriction()

        /**
         * Edit the note content and update the date-time.
         * @param note The new note.
         */
        fun edit(note: String, editor: User) {
            this.note = note
            this.lastEditTime = OffsetDateTime.now()
            this.edits++
        }

        override fun toString() = this.note

    }

    val id = NOTEPAD_ID_GEN.next()

    val initDate = NOW()
    val lastEdit = NOW()

    val notes = mutableListOf<Note>()
    /** An ordered history of each time the [NotePad] was edited */
    val editHistory = mutableListOf<Triple<Long, String, OffsetDateTime>>()

    val readRestriction = Restriction()
    val writeRestriction = Restriction()

    /**
     * Add notes to the NotePad.
     *
     * @param notes The [String] notes to add
     */
    fun addNotes(user: User, vararg notes: String) {
        notes.forEach { this.notes.add(Note(it, user.idLong)) }
    }

    /**
     * Add notes to the NotePad.
     *
     * @param notes The [Note]s to add
     */
    fun addNotes(user: User, notes: Collection<String>) {
        notes.forEach { this.notes.add(Note(it, user.idLong)) }
    }

    /**
     * Inserts notes at the given index, pushing the element at that index down.
     *
     * @param index The index to insert the notes at
     * @param notes The notes to insert
     */
    fun insertNotes(author: User, index: Int, notes: List<String>) {
        try {
            this.notes.addAll(if (index < 0) 0 else index, notes.toNotes(author.idLong))
        } catch (e: IndexOutOfBoundsException) {
            this.addNotes(author, notes)
        }
    }

    /**
     * Edit a note.
     * @param index The index of the note.
     * @param edit The new note.
     * @return The message previously held by the note.
     */
    @Throws(IndexOutOfBoundsException::class)
    fun editNote(index: Int, edit: String, editor: User): String {
        val old = this.notes[index].note
        this.notes[index].edit(edit, editor)
        return old
    }

    /**
     * Remove notes from the NotePad.
     *
     * @param indices The indeces of the notes to remove. (starting at 1)
     *
     * @return The removed notes.
     */
    fun deleteNotesByIndex(indices: List<Int>): List<Note> {
        val out = mutableListOf<Note>()
        indices.forEach { i -> out.add(this.notes.removeAt(i - 1)) }
        return out
    }

    /**
     * Remove notes from the NotePad.
     *
     * @param ids The ID Strings to remove
     *
     * @return The removed notes.
     */
    fun deleteNotesById(ids: List<String>) : List<Note> {
        val out = mutableListOf<Note>()
        ids.forEach { id ->
            val i = this.notes.indexOfFirst { it.id.equals(id, true) }
            out.add(this.notes.removeAt(i))
        }
        return out
    }

    /**
     * Clear the NotePad's notes
     *
     * @return The removed notes
     */
    fun clear(): List<Note> {
        val out = this.notes.toList()
        this.notes.clear()
        return out
    }

    override fun iterator() = notes.iterator()

}

/**
 * View and modify [Note Pads][NotePad].
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
 class CmdNotePad : WeebotCommand("NotePad",
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
    cooldown = 5, guildOnly = true) {


    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        val auth = event.author
        val mem = event.member
        val guild = event.guild
        val bot = getWeebotOrNew(event.guild)
        val pads = bot.notePads

        if (args.isEmpty()) {
            if (pads.isEmpty()) {
                event.respondThenDelete(strdEmbedBuilder
                    .setTitle("${guild.name} has no NotePads")
                    .setDescription("Use ``make [Notepad Name]`` to make a new NotePad")
                    .build(), 30)
                return
            }
            SelectablePaginator(setOf(auth),
                title = "${guild.name} NotePads",
                description = "${guild.name} NotePads available to ${mem.effectiveName}",
                itemsPerPage = 10, thumbnail = "https://47eaps32orgm24ec5k1dcrn1" +
                        "-wpengine.netdna-ssl.com/wp-content/uploads/2016/08/" +
                        "notepad-pen-sponsor.png",
                items = emptyList()
            ) { message ->

            }.display(event.textChannel)

        }

        /*
        when (args[0]) {
            "make" ->
            "write", "add" ->
            "insert" ->
            "edit" ->
            "get", "see" ->
            "delete", "remove" ->
            "clear" ->
            "toss", "trash", "bin", "garbo" ->
            "file" ->
            "lockto" ->
            "lockout" ->
        }
        */

    }

    /**
     * Attempts to parse a NotePad from a string. If it fails, an err message is
     * sent by the [event][BetterMessageEvent].
     * @param notes The NotePad [List] to retrieve from.
     * @param index The [String] to parse for an [Integer].
     * (Based on 1 based counting array [1,2,3] not [0,1,2])
     * @param event The event to reply from if an err occurs.
     * @return The NotePad at the passed index.<br></br>Null if and err occurred.
     */
    private fun parseNotePad(notes: List<NotePad>, index: String,
                             event: CommandEvent): NotePad? {
        return try {
            notes[Integer.parseInt(index) - 1]
        } catch (e: NumberFormatException) {
            event.reply(
                "Sorry, I couldn't understand '$index'. Please use an integer between 1 and 2,147,483,647.")
            null
        } catch (e: IndexOutOfBoundsException) {
            this.invalidNotePadIndexMessage(index, event, notes)
            null
        }

    }

    /**
     * I couldn't find the notepad (index). I will send you a message containing
     * all the notepads available to you on (server name). Use this format
     * (arg format) to write or edit a notepad.
     * @param arg
     * @param event
     * @param notes
     */
    private fun invalidNotePadIndexMessage(arg: String, event: CommandEvent,
                                           notes: List<NotePad>) {
        if (Integer.parseInt(arg) > notes.size) {
            val k = notes.size
            event.reply(
                "There ${if (k != 1) "are" else "is"} only ${notes.size} Note Pad${
                if (k != 1) "s" else ""}.\nUse ```${this.arguments
                }``` to write or edit a specific notepad.")
            return
        }
        var out = "I couldn't find NotePad $arg. I will send a private message of all the NotePads available to you."
        event.reply(out)
        out = "Here are the available NotePads " + (if (event.guild != null) "on " + event.guild.name
        else "") + "\n```"
        var i = 1
        for ((name) in notes) {
            out += (i++.toString() + ".) " + name + "\n")
        }
        event.replyInDm(
            "$out```\nUse ```${this.arguments}``` to make or edit a specific notepad.")
    }

    /**
     * "I couldn't find a note " + arg + ". Please use a number" +
     * " listed in the NotePad."
     * @param arg
     * @param event
     */
    private fun invalidNoteIndexMessage(arg: String, event: CommandEvent) {
        event.respondThenDelete(
            "I couldn't find a note $arg. Please use a number listed in the NotePad.", 30)
    }

    init {
        val sb = StringBuilder()
        sb.append("Write and edit server NotePads\n")
            .append("Note Pads can be locked to specific")
            .append("roles, members, and text channels.\n")

        val eb = strdEmbedBuilder.setTitle("Note Pad")
        sb.setLength(0)

        eb.addField("See all available NotePads", "notes", false)
            .addField("Make a NotePad",
                "notes make [The_Name]\n*If no name is given, the date and time will " + "be used as the name of the NotePad*",
                false)
            .addField("See the contents of a Note Pad",
                "notes <notepad_number>\n*To find the NotePad's number, use 'notes'*",
                false)
            .addField("Write to a NotePad",
                "notes write <notepad_number> <The Message>\n*Alias:* add", false)
            .addField("Insert a Note into the NotePad",
                "notes <notepad_number> insert <The Message>", false)
            .addField("Edit (replace) a Note",
                "notes <notepad_number> edit <note_number> <New Message>", false)
            .addField("Delete a Note from a NotePad",
                "notes <notepad_number> delete <note_number>\n*Alias*: remove\n", false)
            .addField("Clear a NotePad of all Notes", "notes <notepad_number> clear",
                false)
            .addField("Get a NotePad as a text file", "notes <notepad_number> file",
                false)
            .addField("Lock access to a NotePad",
                "notes <notepad_number> lockto [roles, members, or channels]", false)
            .addField("Lock a NotePad's access from Roles, Members, or Channels",
                "notes <notepad_number> lockout [roles, members, or channels]", false)
            .addField("Delete a NotePad",
                "notes <notepad_number> trash\n*Aliases*:toss, bin, garbo", false)

        sb.append("Any usagage of '*notes*' can be replaced with any of the following:\n")
            .append(this.aliases.toString() + ".\n\n")
            .append("<required>   [optional]   /situationally_required/")
        eb.addField("Extra", sb.toString(), false)
        helpBiConsumer = HelpBiConsumerBuilder("Weebot NotePads")
            //TODO NotePad help
            .build()
    }
}
