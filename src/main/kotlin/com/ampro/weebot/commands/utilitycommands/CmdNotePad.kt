/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.utilitycommands

import com.ampro.weebot.Restriction
import com.ampro.weebot.commands.CAT_UTIL
import com.ampro.weebot.commands.utilitycommands.NotePad.Note
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.exceptions.PermissionException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit


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
internal fun String.toNote(authorID: Long, initTime: OffsetDateTime)
        : Note = Note(this, authorID, initTime)
/** Convert a list of [String]s to a list of [Note]s */
internal fun List<String>.toNotes(authorID: Long, init: OffsetDateTime)
        : List<Note> {
    return List(size) { Note(this[it], authorID, init) }
}

/**
 * Check if the event can edit the NotePad.
 * @param event
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
 * @param event
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
    data class Note(var note: String, val authorID: Long,val initTime: OffsetDateTime) {

        val id = NOTE_ID_GEN.next()

        var lastEditTime: OffsetDateTime = initTime

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

        fun toEmbed(guild: Guild) = strdEmbedBuilder
            .setTitle("${if(readRestriction.restricted() || writeRestrictions.restricted())
             Lock else Unlock} Note $id").setDescription(note)
            .addField("Author", guild.getMemberById(authorID)?.effectiveName, true)
            .addField("Created at ${initTime.format(DD_MM_YYYY_HH_MM)}", "", true)
            .addField("Edits", "$edits\nTime since last edit: ${ChronoUnit
                .SECONDS.between(lastEditTime, NOW()).formatTime()}", true)
            .addField("Guide", "To Edit: $Pencil\nTo Lock: $Lock\nTo Unlock: $Unlock" +
                    "\nTo delete: $X_Red", true).build()

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
     * Sends the NotePad as an Embed. If the message couldnt be edited then resends.
     * //TODO Send as a new [ButtonPaginator] with a back button that resends
     * the note list
     */
    fun send(event: CommandEvent, message: Message?, pads: List<NotePad>) {
        val mem = event.member
        val cv = notes.filter { TODO() }
        val userSet = setOf(mem.user)
        val sp = SelectablePaginator(users = userSet, title = this.name,
                itemsPerPage = 10,
                items = List<Pair<String, (Int, Message) -> Unit>>(notes.size) {
                    "(${cv[it].id}) ${cv[it].note}" to { i, m ->
                        try { m.clearReactions().queue()} catch (e: PermissionException) {}
                        SelectableEmbed(userSet, messageEmbed = notes[i].toEmbed(event.guild),
                                options = listOf(),
                                cancelEmoji = Rewind
                        ) {message ->
                            send(event, message, pads)
                        }.display(m)
                    }
                }
        ) { message ->
            sendNotePads(event, pads, message)
        }
        if (message != null) {
            sp.display(message)
        } else {
            sp.display(event.channel)
        }
    }

    /**
     * Add notes to the NotePad.
     *
     * @param notes The [String] notes to add
     */
    fun addNotes(user: User, vararg notes: String, creation: OffsetDateTime) {
        notes.forEach { this.notes.add(Note(it, user.idLong, creation)) }
    }

    /**
     * Add notes to the NotePad.
     *
     * @param notes The [Note]s to add
     */
    fun addNotes(user: User, creation: OffsetDateTime, notes: Collection<String>) {
        notes.forEach { this.notes.add(Note(it, user.idLong, creation)) }
    }

    /**
     * Inserts notes at the given index, pushing the element at that index down.
     *
     * @param index The index to insert the notes at
     * @param notes The notes to insert
     */
    fun insertNotes(author: User, creation: OffsetDateTime, index: Int, notes: List<String>) {
        try {
            this.notes.addAll(if (index < 0) 0 else index, notes.toNotes(author.idLong, creation))
        } catch (e: IndexOutOfBoundsException) {
            this.addNotes(author, creation, notes)
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
        /** User's Viewable notepads */
        val cv = pads.filter { event canRead it }

        if (args.isEmpty()) {
            event.delete(30)
            if (pads.isEmpty()) {
                event.respondThenDelete(strdEmbedBuilder
                    .setTitle("${guild.name} has no NotePads")
                    .setDescription("Use ``make [Notepad Name]`` to make a new NotePad")
                    .build(), 30)
                return
            } else if (cv.isEmpty()) {
                event.respondThenDelete(strdEmbedBuilder
                    .setTitle("${guild.name} has no NotePads you can view")
                    .setDescription("Use ``make [Notepad Name]`` to make a new NotePad")
                    .build(), 30)
                return
            }
            sendNotePads(event, cv, event.textChannel)
            return
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
        : ButtonPaginator {
    val emojiCounter = EmojiCounter()
    val mem = event.member
    return ButtonPaginator(setOf(mem.user), title = "${mem.guild.name} NotePads",
            description = "${mem.guild.name} NotePads available to ${mem.effectiveName}",
            itemsPerPage = 10,
            thumbnail = "https://47eaps32orgm24ec5k1dcrn1" + "-wpengine.netdna-ssl.com/wp-content/uploads/2016/08/" + "notepad-pen-sponsor.png",
            items = List<Triple<Emoji, String, (Emoji, Message) -> Unit>>(pads.size) {
                Triple(emojiCounter.next(), "${pads[it].name} (${pads[it].id})")
                { e, m ->
                    try { m.clearReactions().queue() } catch (e: PermissionException) {}
                    pads[OrderedEmoji.indexOf(e)].send(event, m, pads)
                }
            }) { m ->
        try { m.clearReactions().queue() } catch (ignored: Exception) { }
    }
}
