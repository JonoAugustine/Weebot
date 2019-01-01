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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.Permission.MESSAGE_ATTACH_FILES
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import java.io.*
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
        !member.roles.has { notePad.writeRestriction.isAllowed(it) } -> false
        //Check User
        notePad.writeRestriction.isAllowed(member.user) -> true
        //else
        else -> false
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
        !member.roles.has { notePad.readRestriction.isAllowed(it) } -> false
        //Check User
        notePad.readRestriction.isAllowed(member.user) -> true
        //else
        else -> false
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

        var locked = false

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
            .setTitle("Note $id ${if (locked) Lock else Unlock}").setDescription(note)
            .addField("Author", guild.getMemberById(authorID)?.effectiveName, true)
            .addField("Created at ${initTime.format(DD_MM_YYYY_HH_MM)}", "", true)
            .addField("Edits: $edits", "Time since last edit: ${ChronoUnit
                .SECONDS.between(lastEditTime, NOW()).formatTime()}", true)
            .addField("Guide", "To Edit: $Pencil\nTo Lock: $Lock\nTo Unlock: $Unlock" +
                    "\nTo delete: $X_Red", true)

        fun toSelectableEmbed(userSet: Set<User>, guild: Guild, notePad: NotePad,
                              finalAction: (Message) -> Unit) : SelectableEmbed {
            return SelectableEmbed(userSet, messageEmbed = toEmbed(guild).build(),
                options = listOf(Pencil to { m1, u ->
                    m1.textChannel.sendMessage("To Edit, send the new note:").queue {
                        WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                            { e -> e.isValidUser(users = userSet, guild = e.guild) },
                            { e ->
                                if (this.locked) {
                                    e.channel.sendMessage("*This Note is locked.*")
                                        .queue()
                                    return@waitForEvent
                                }
                                notePad.editNote(this, e.message.contentDisplay, e.author,
                                    e.message.creationTime)
                                val nm = toSelectableEmbed(userSet, guild, notePad,
                                    finalAction)
                                try {
                                    nm.display(m1)
                                } catch (ex: Exception) {
                                    nm.display(e.channel)
                                } finally {
                                    it.delete().queueAfter(250, MILLISECONDS) {
                                        e.message.delete().queueAfter(250, MILLISECONDS)
                                    }
                                }
                            }, 2, MINUTES, {
                                it.editMessage("~~${it.contentDisplay}~~ *timed out*")
                                    .queue()
                            })
                    }
                }, Lock to { m, u ->
                    if (u.idLong == authorID || guild.getMember(u)?.hasPerm(ADMINISTRATOR)!!) {
                        this.locked = true
                        val nm = toSelectableEmbed(userSet, guild, notePad, finalAction)
                        try {
                            nm.display(m)
                        } catch (ex: Exception) {
                            nm.display(m.channel)
                        }
                    } else m.channel.sendMessage("*You do not have permission.*")
                }, Unlock to { m, u ->
                    if (u.idLong == authorID || guild.getMember(u)?.hasPerm(ADMINISTRATOR)!!) {
                        this.locked = true
                        val nm = toSelectableEmbed(userSet, guild, notePad, finalAction)
                        try {
                            nm.display(m)
                        } catch (ex: Exception) {
                            nm.display(m.channel)
                        }
                    } else m.channel.sendMessage("*You do not have permission.*")
                }, X_Red to { m, u ->
                        if (!this.locked || guild.getMember(u)?.hasPerm(ADMINISTRATOR)!!) {
                            strdButtonMenu.setUsers(userSet.first())
                                .setText("Confirmation").setDescription("""
                                *Are you sure you want to delete note: ${this.id}?*
                                ${Fire.unicode} = YES,* ***ignore this to cancel***
                                """.trimIndent()).addChoice(Fire.unicode)
                                .setTimeout(30, SECONDS).setAction {
                                    if (it.toEmoji() == Fire) {
                                        val index = notePad.indexOf(this)
                                        if (index == -1) {
                                            m.channel.sendMessage(GENERIC_ERR_MESG)
                                                .queue()
                                            return@setAction
                                        } else {
                                            notePad.deleteNote(this, userSet.first(),
                                                m.creationTime)
                                            try {
                                                m.delete().queueAfter(250, MILLISECONDS)
                                                m.channel.sendMessage("*Note deleted.*")
                                                    .queueAfter(250, MILLISECONDS)
                                            } catch (ex: Exception) {
                                                m.clearReactions()
                                                    .queueAfter(250, MILLISECONDS)
                                            }
                                        }
                                    }
                                }
                                .setFinalAction {}.build().display(m.channel)
                        } else m.channel.sendMessage("*This Note is locked.*").queue()
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
        val nameOrUnkown: (Long) -> String = {
            guild.getMemberById(it)?.effectiveName ?: guild.getRoleById(it)?.name
            ?: guild.getTextChannelById(it)?.name ?: "Unknown"
        }
        fun historyField() : Field {
            val title = "Created: ${initTime.format(DD_MM_YYYY_HH_MM)}"
            val sb = StringBuilder("Edits: ${editHistory.size}\n")
            if (editHistory.isNotEmpty()) {
                val last = editHistory[0]
                val user = guild.getMemberById(editHistory[0].editorID)?.effectiveName
                        ?: getUser(editHistory[0].editorID)?.name ?: "Unknown User"
                sb.append("Last Edit: ${last.type.name} by $user")
                val time = last.time.format(WKDAY_MONTH_YEAR)
                val ids = last.noteIDs.joinToString(", ")
                sb.append(" on $time on notes: $ids")
            }
            return Field(title, sb.toString(), true)
        }
        /** @return Read Restriction, Write Restriction */
        fun restrictionFields() : Pair<Field?, Field?> {
            fun restrictionField(name: String, restriction: Restriction) : Field? {
                val sb = StringBuilder()
                val tList = mutableListOf<String>()
                if (restriction.allowedUsers.isNotEmpty()) {
                    sb.append("Allowed Users: ``")
                    restriction.allowedUsers.forEach { tList.add(nameOrUnkown(it)) }
                    sb.append(tList.joinToString(", ")).append("``\n")
                    tList.clear()
                }
                if (restriction.blockedUsers.isNotEmpty()) {
                    sb.append("Blocked Users: ``")
                    restriction.blockedUsers.forEach { tList.add(nameOrUnkown(it)) }
                    sb.append(tList.joinToString(", ")).append("``\n")
                    tList.clear()
                }
                if (restriction.allowedRoles.isNotEmpty()) {
                    sb.append("Allowed Roles: ``")
                    restriction.allowedRoles.forEach { tList.add(nameOrUnkown(it)) }
                    sb.append(tList.joinToString(", ")).append("``\n")
                    tList.clear()
                }
                if (restriction.blockedRoles.isNotEmpty()) {
                    sb.append("Blocked Roles: ``")
                    restriction.blockedRoles.forEach { tList.add(nameOrUnkown(it)) }
                    sb.append(tList.joinToString(", ")).append("``\n")
                    tList.clear()
                }
                if (restriction.allowedTextChannels.isNotEmpty()) {
                    sb.append("Allowed TextChannels: ``")
                    restriction.allowedTextChannels.forEach { tList.add(nameOrUnkown(it)) }
                    sb.append(tList.joinToString(", ")).append("``\n")
                    tList.clear()
                }
                if (restriction.blockedTextChannels.isNotEmpty()) {
                    sb.append("Blocked TextChannels: ``")
                    restriction.blockedTextChannels.forEach { tList.add(nameOrUnkown(it)) }
                    sb.append(tList.joinToString(", ")).append("``\n")
                    tList.clear()
                }
                return if (sb.isNotEmpty()) Field(name, sb.toString(), true) else null
            }
            return restrictionField("Read Restriction", readRestriction) to
                    restrictionField("Write Restriction", writeRestriction)
        }
        val fList = mutableListOf<MessageEmbed.Field>().apply {
            val rFields = restrictionFields()
            if (rFields.first != null) add(rFields.first as Field)
            if (rFields.second != null) add(rFields.second as Field)
            add(historyField())
        }
        val items = notes.map { "(${it.id}) ${it.note}" to { i: Int, m: Message ->
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
                }.display(m.channel)
            }
        }
        val notePad = SelectablePaginator(users = userSet, title = this.name,
                itemsPerPage = 10, items = items, fieldList = fList,
            exitAction = { it.delete().queueAfter(250, MILLISECONDS) }
        )

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
     * @param note The note.
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
            notes.removeAt(index)
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

    /**
     * Make a file out of a notepad.
     *
     * {NotePadName EditionTime.txt}
     * {NotePad Name (ID: NotePadID)}
     * {Author Name (ID: AuthorID)}
     * {Creation Date}
     * {EditionDate}
     * {Last Edit}
     * {Read Restrictions}
     * {Write Restrictions}
     * #.) {ID: NoteID}
     * {Note Content}
     *{
     *       Author (ID: AuthorID)
     *      CreationDate
     *      EditNum
     *      LastEdit
     * }
     *
     * @return A file of the notepad. Null if an err occurs.
     *
     */
    fun toFile(guild: Guild, dateTime: OffsetDateTime): File? {
        val nameOrUnknown: (Long) -> String = {
            guild.getMemberById(it)?.effectiveName ?: guild.getRoleById(it)?.name
            ?: guild.getTextChannelById(it)?.name ?: "Unknown"
        }

        val file = File(TEMP_OUT, "$name ${dateTime.format(DD_MM_YYYY_HH_MM)
            .replace(":", "-")}.txt")
        try {
            BufferedWriter(FileWriter(file)).use { bw ->
                val sb = StringBuilder("""
                    $name (ID: $id)

                    Author: ${nameOrUnknown(authorID)} (ID: $authorID)
                    Creation Date: ${initTime.format(WKDAY_MONTH_YEAR_TIME)}
                    File Edition: ${dateTime.format(WKDAY_MONTH_YEAR_TIME)}
                    """.trimIndent())

                if (editHistory.isNotEmpty()) {
                    val last = editHistory[0]
                    sb.append("\n\n").append("""
                        Last Edit:
                            Editor: ${nameOrUnknown(last.editorID)} (ID: ${last.editorID})
                            Type: ${last.type}
                            Notes: ${last.noteIDs.joinToString(", ")}
                            Date: ${last.time.format(WKDAY_MONTH_YEAR_TIME)}
                            ${if(last.info.isNotBlank()) "Info: ${last.info}" else ""}
                    """.trimIndent())
                }

                fun restriction(name: String, restriction: Restriction) : String {
                    val stringBuilder = StringBuilder("$name\n")
                    val tList = mutableListOf<String>()
                    if (restriction.allowedUsers.isNotEmpty()) {
                        stringBuilder.append("Allowed Users: ``")
                        restriction.allowedUsers.forEach {
                            tList.add(nameOrUnknown(it))
                        }
                        stringBuilder.append(tList.joinToString(", ")).append("``\n")
                        tList.clear()
                    }
                    if (restriction.blockedUsers.isNotEmpty()) {
                        stringBuilder.append("Blocked Users: ``")
                        restriction.blockedUsers.forEach {
                            tList.add(nameOrUnknown(it))
                        }
                        stringBuilder.append(tList.joinToString(", ")).append("``\n")
                        tList.clear()
                    }
                    if (restriction.allowedRoles.isNotEmpty()) {
                        stringBuilder.append("Allowed Roles: ``")
                        restriction.allowedRoles.forEach {
                            tList.add(nameOrUnknown(it))
                        }
                        stringBuilder.append(tList.joinToString(", ")).append("``\n")
                        tList.clear()
                    }
                    if (restriction.blockedRoles.isNotEmpty()) {
                        stringBuilder.append("Blocked Roles: ``")
                        restriction.blockedRoles.forEach {
                            tList.add(nameOrUnknown(it))
                        }
                        stringBuilder.append(tList.joinToString(", ")).append("``\n")
                        tList.clear()
                    }
                    if (restriction.allowedTextChannels.isNotEmpty()) {
                        stringBuilder.append("Allowed TextChannels: ``")
                        restriction.allowedTextChannels.forEach {
                            tList.add(nameOrUnknown(it))
                        }
                        stringBuilder.append(tList.joinToString(", ")).append("``\n")
                        tList.clear()
                    }
                    if (restriction.blockedTextChannels.isNotEmpty()) {
                        stringBuilder.append("Blocked TextChannels: ``")
                        restriction.blockedTextChannels.forEach {
                            tList.add(nameOrUnknown(it))
                        }
                        stringBuilder.append(tList.joinToString(", ")).append("``\n")
                        tList.clear()
                    }
                    return stringBuilder.toString()
                }

                if (readRestriction.restricted()) {
                    sb.append("\n\n")
                        .append(restriction("Read Restrictions", readRestriction))
                }
                if (writeRestriction.restricted()) {
                    sb.append("\n\n")
                        .append(restriction("Write Restrictions", writeRestriction))
                }

                notes.forEachIndexed { i, it ->
                    sb.append("\n\n").append("""
                        ${i + 1}.) ${it.note}
                        ID: ${it.id}
                        Author: ${nameOrUnknown(it.authorID)} (ID: ${it.authorID})
                        Creation Date: ${it.initTime.format(WKDAY_MONTH_YEAR_TIME)}
                        Edits: ${it.edits}
                        Last Edit: ${it.lastEditTime.format(WKDAY_MONTH_YEAR_TIME)}
                    """.trimIndent())
                }

                bw.write(sb.toString())
                return file
            }
        } catch (e: IOException) {
            MLOG.elog("IOException Making NotePad Note ${e.message ?: ""} ")
            return null
        }
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
        CmdInsert(), CmdEdit(), CmdDeleteNote(), CmdLockTo(),CmdLockFrom(),
        CmdClear(), CmdDeletePad(), CmdToFile() )
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
        "<notepad_id> [read/write] [@/roles] [@/members] [#/channels]",
        "Lock access to a NotePad.", cooldown = 10, cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            if (args.size < 2) return
            /** User's Viewable notepads */
            val cv = getWeebotOrNew(event.guild).notePads.filter { event canRead it }

            //<notepad_id> [read/write] [@/roles] [@/members] [#/channels]
            val notePad = cv.find { it.id.equals(args[0], true) } ?: run{
                event.reply("No NotePad found with ID ${args[0]}")
                return
            }
            val write = args[1].matches(Regex("(?i)-*(w+r*i*t*e*)"))

            if (event.message.mentionedMembers.isEmpty()
                    && event.message.mentionedChannels.isEmpty()
                    && event.message.mentionedRoles.isEmpty()) {
                event.reply("*Please mention at least 1 Role, Channel, or Member.*")
                return
            }

            if (event canWriteTo notePad) {
                val r = if (write) notePad.writeRestriction else notePad.readRestriction
                val sb = mutableListOf<String>()
                event.message.mentionedMembers.forEach {
                    sb.add(it.effectiveName); r.allow(it.user)
                }
                event.message.mentionedChannels.forEach { sb.add(it.name); r.allow(it) }
                event.message.mentionedRoles.forEach { sb.add(it.name); r.allow(it) }
                event.reply("*NotePad ${notePad.name} locked to* ${sb.joinToString(", ")}")
            } else {
                event.reply("*You do not have permissions to edit this NotePad.*")
            }
        }
    }

    class CmdLockFrom: WeebotCommand("lockfrom", arrayOf("block", "lockout"), CAT_UTIL,
        "<notepad_id> [read/write] [@/roles] [@/members] [#/channels]",
        "Lock access to a NotePad.", cooldownScope = USER_CHANNEL, cooldown = 10) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            if (args.size < 2) return
            /** User's Viewable notepads */
            val cv = getWeebotOrNew(event.guild).notePads.filter { event canRead it }

            //<notepad_id> [read/write] [@/roles] [@/members] [#/channels]
            val notePad = cv.find { it.id.equals(args[0], true) } ?: run{
                event.reply("No NotePad found with ID ${args[0]}")
                return
            }
            val write = args[1].matches(Regex("(?i)-*(w+r*i*t*e*)"))

            if (event.message.mentionedMembers.isEmpty()
                    && event.message.mentionedChannels.isEmpty()
                    && event.message.mentionedRoles.isEmpty()) {
                event.reply("*Please mention at least 1 Role, Channel, or Member.*")
                return
            }

            if (event canWriteTo notePad) {
                val r = if (write) notePad.writeRestriction else notePad.readRestriction
                val sb = mutableListOf<String>()
                event.message.mentionedMembers.forEach {
                    sb.add(it.effectiveName); r.block(it.user)
                }
                event.message.mentionedChannels.forEach { sb.add(it.name); r.block(it) }
                event.message.mentionedRoles.forEach { sb.add(it.name); r.block(it) }
                event.reply("${sb.joinToString(", ")} blocked from ${notePad.name} ")
            } else {
                event.reply("*You do not have permissions to edit this NotePad.*")
            }
        }
    }

    class CmdClear : WeebotCommand("clear", arrayOf("wipe"), CAT_UTIL, "<notepad_id>",
        "Clear a NotePad of all Notes", cooldown = 30, cooldownScope = USER_CHANNEL) {
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            val pads = getWeebotOrNew(event.guild).notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }
            val cw = pads.filter { event canWriteTo it }

            val notePads = cw.filter { args.contains(it.id.toUpperCase()) }
            when {
                notePads.isNotEmpty() -> {
                    event.reply(
                        "Are you sure you want to clear NotePad(s): ${
                        notePads.joinToString(", ") { "${it.name} (${it.id})"
                        }}? *(say ``yes`` or ``no``)*")
                    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                        { it.isValidUser(event.guild, setOf(event.author)) }, {
                            if (it.message.contentDisplay.matches(REG_YES)) {
                                notePads.forEach {
                                    it.clear(event.author, event.creationTime)
                                }
                                event.reply("*NotePad(s) cleared.*")
                                val unUsed = args.filterNot {
                                    cw.has { np -> np.id.equals(it, true) }
                                }
                                if (unUsed.isNotEmpty()) {
                                    event.reply(
                                        "I couldn't find any NotePads matching: ${
                                        unUsed.joinToString(", ")}")
                                }
                            } else if (it.message.contentDisplay.matches(REG_NO)) {
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
        public override fun execute(event: CommandEvent) {
            STAT.track(this, getWeebotOrNew(event.guild), event.author)
            val args = event.splitArgs()
            val pads = getWeebotOrNew(event.guild).notePads
            /** User's Viewable notepads */
            val cv = pads.filter { event canRead it }

            val notePad = cv.find { it.id.equals(args[0], true) } ?: run{
                event.reply("No NotePad found with ID ${args[0]}")
                return
            }

            val file: File = notePad.toFile(event.guild, event.creationTime) ?: run {
                event.reply(
                    "*Uhh I tripped and lost the file. I'll look for it, so try again later*"
                )
                return
            }

            if (event.guild.selfMember hasPerm MESSAGE_ATTACH_FILES) {
                event.reply(file, file.name)
            } else event.author.openPrivateChannel().queue {
                it.sendFile(file, file.name).queueAfter(250, MILLISECONDS)
            }
        }
    }

    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
        val args = event.splitArgs()
        val auth = event.author
        val pads = getWeebotOrNew(event.guild).notePads
        /** User's Viewable notepads */
        val cv = pads.filter { event canRead it }.toMutableList()

        if (args.isEmpty()) {
            SelectableEmbed(auth, mainMenuEmbed.apply {
                if (pads.isNotEmpty() && cv.contains(pads[0])) addEmptyField(
                    "Default NotePad: ${pads[0].name}")
            }.build(), listOf(Eyes to seeNotePads(event, pads, cv),
                Notebook to addNotePad(event), Pencil to writeToDefault(event, pads),
                EightSpokedAsterisk to setDefaultById(event, pads),
                FileFolder to notePadToFileById(event, cv),
                C to clearNotePadById(event, cv),
                X_Red to deleteNotePadById(event, cv))) {
                try {
                    it.delete().queue()
                    event.delete()
                } catch (e: Exception) {
                    try {
                        it.clearReactions().queue()
                    } catch (e: Exception) {
                    }
                }
            }.display(event.textChannel)
        } else {
            when (args[0].toLowerCase()) {
                "see", "view", "list" -> seeNotePads(event, pads, cv)(event.message, auth)
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
    fun seeNotePads(event: CommandEvent, allPads: MutableList<NotePad>,
                    viewable: MutableList<NotePad>) : (Message, User) -> Unit
            = { it, u ->
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

    /**
     * Send dialogue for adding a new [NotePad]
     *
     * @param event The invoking event
     */
    fun addNotePad(event: CommandEvent) : (Message, User) -> Unit = { _, _ ->
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

    fun writeToDefault(event: CommandEvent, pads: List<NotePad>)
            : (Message, User) -> Unit = { _, _ ->
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

    fun setDefaultById(event: CommandEvent, pads: MutableList<NotePad>)
            : (Message, User) -> Unit = { _, _ ->
        if (event.member hasPerm ADMINISTRATOR) {
            getNotePadByIdDialogue(event, pads, {
                when {
                    it.size == 1 -> {
                        pads.remove(it[0])
                        pads.add(0, it[0])
                        event.reply("*Default NotePad set to ${it[0].name}*")
                    }
                    it.size > 1 ->  event.reply("*Only noe ID.*")
                }
            }, { event.reply("*Timed Out*") })
        } else event.reply("*You must be an Admin to make this change.*")
    }

    fun notePadToFileById(event: CommandEvent, viewable: MutableList<NotePad>)
            : (Message, User) -> Unit = { _, _ ->
        getNotePadByIdDialogue(event, viewable, { nps -> runBlocking {
            nps.forEach { notePad ->
                val file: File = notePad.toFile(event.guild, event.creationTime) ?: run {
                    event.reply(
                        "*Uhh I tripped and lost the file. I'll look for it, so try again later*")
                    return@runBlocking
                }
                if (event.guild.selfMember hasPerm MESSAGE_ATTACH_FILES) {
                    event.reply(file, file.name)
                } else event.author.openPrivateChannel().queue {
                    it.sendFile(file, file.name).queueAfter(250, MILLISECONDS)
                }
                delay(250)
            }
        }})
    }

    fun clearNotePadById(event: CommandEvent, viewable: List<NotePad>)
            : (Message, User) -> Unit = { _, _ ->
        getNotePadByIdDialogue(event, viewable, { nps ->
            val cw = nps.filter { event canWriteTo it }
            when {
                cw.isNotEmpty() -> {
                    event.reply(
                        "Are you sure you want to clear NotePad(s): ${
                        cw.joinToString(", ") { "${it.name} (${it.id})"
                        }}? *(say ``yes`` or ``no``)*")
                    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                        { it.isValidUser(event.guild, setOf(event.author)) }, {
                            if (it.message.contentDisplay.matches(REG_YES)) {
                                cw.forEach { it.clear(event.author, event.creationTime) }
                                event.reply("*NotePad(s) cleared.*")
                                val unUsed = nps.filterNot { event canWriteTo it }
                                if (unUsed.isNotEmpty()) {
                                    event.reply("I couldn't find any NotePads matching: ${
                                        unUsed.joinToString(", ")}")
                                }
                            } else if (it.message.contentDisplay.matches(REG_NO)) {
                                event.reply("Ok, deletion cancelled.")
                            }
                        }, 1L, MINUTES) {
                        event.reply("Deletion cancelled (timed out).")
                    }
                }
                cw.isEmpty() -> {
                    if (nps.has { cw.contains(it) })
                        event.reply("*You do not have permission to edit these NotePad(s).*")
                    else {
                        event.reply("No NotePad(s) could be found with the given ID(s).")
                    }
                }
            }
        })
    }

    fun deleteNotePadById(event: CommandEvent, viewable: List<NotePad>)
            : (Message, User) -> Unit = { _, _ ->
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
                "``allow <notepad_id> [read/write] [@/roles] [@/members] [#/channels]``")
            .addField("Block a NotePad's access from Roles, Members, or Channels",
                "``block <notepad_id> [read/write] [@/roles] [@/members] [#/channels]``")
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

    /**
     *
     */
    fun getNotePadByIdDialogue(event: CommandEvent, viewable: List<NotePad>,
                               action: (List<NotePad>) -> Unit, timeout: () -> Unit = {
                event.message.editMessage("~~${event.message.contentDisplay}~~ *Timed out*")
                    .queue()
            }) {
        event.reply("*Please provide the ID(s) of the NotePad(s):*")
        WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
            { e -> e.isValidUser(event.guild, setOf(event.author)) }, { e ->
                val IDs = e.message.contentDisplay.toUpperCase().split(Regex("\\s+"))
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
    private fun getNoteByIdDialogue(event: CommandEvent, notes: List<Note>,
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
    private fun sendNotePads(event: CommandEvent, pads: MutableList<NotePad>,
                             textChannel:TextChannel) {
        buildNotePadPaginator(event, pads).display(textChannel)
    }

    /**
     * Sends a [NotePad] [ButtonPaginator] as an edit of [message]
     *
     * @param message The message to replace
     */
    private fun sendNotePads(event: CommandEvent, pads: MutableList<NotePad>,
                             message:Message) {
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
    private fun buildNotePadPaginator(event: CommandEvent, pads: MutableList<NotePad>)
            : SelectablePaginator {
        val mem = event.member
        return SelectablePaginator(setOf(mem.user), title = "${mem.guild.name} NotePads",
            description = "${mem.guild.name} NotePads available to ${mem.effectiveName}",
            itemsPerPage = 10, thumbnail = "https://47eaps32orgm24ec5k1dcrn1" +
                    "-wpengine.netdna-ssl.com/wp-content/uploads/2016/08/notepad-pen-sponsor.png",
            items = pads.map { " ${it.name} (${it.id})" to { i: Int, m: Message ->
                pads[i].send(event, null, pads)
            }}, exitAction = {
                val nn = SelectableEmbed(event.author, mainMenuEmbed.apply {
                    if (pads.isNotEmpty() && pads.contains(pads[0])) addEmptyField(
                        "Default NotePad: ${pads[0].name}")
                }.build(), listOf(Eyes to seeNotePads(event, pads, pads),
                    Notebook to addNotePad(event), Pencil to writeToDefault(event, pads),
                    EightSpokedAsterisk to setDefaultById(event, pads),
                    FileFolder to notePadToFileById(event, pads),
                    C to clearNotePadById(event, pads),
                    X_Red to deleteNotePadById(event, pads))) {
                    try {
                        it.delete().queue()
                        event.delete()
                    } catch (e: Exception) {
                        try {
                            it.clearReactions().queue()
                        } catch (e: Exception) {
                        }
                    }
                }
                it.delete().queue({

                    nn.display(event.message)
                }, { nn.display(event.textChannel) })
            }
        )
    }

}


