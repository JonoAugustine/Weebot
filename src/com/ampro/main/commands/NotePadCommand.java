package com.ampro.main.commands;

import com.ampro.main.Launcher;
import com.ampro.main.entities.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * View and modify {@link NotePad Note Pads}.
 * TODO: Add separate view and edit permissions.
 * @author Jonathan Augustine
 */
public class NotePadCommand extends Command {

    /**
     * A way for members to keep a notepad of ideas and whatnot. <br>
     * Each notepad has a {@link String} name and {@link ArrayList} of
     * {@link Note Notes}.
     * <br>
     *     A {@link Weebot} can have up to //TODO concurrent NotePads at once.
     * <br>
     *     A NotePad can have access restricted by {@link TextChannel},
     *     {@link User}/{@link Member}, or {@link Role}.
     *
     * @author Jonathan Augustine
     */
    public static final class NotePad implements Iterable<NotePad.Note> {

        /**
         * A note has a {@link String} note and a creation time & date.
         */
        static final class Note {
            String note;
            private final OffsetDateTime creationTime;
            private OffsetDateTime lastEditTime;
            private long edits;
            private final long authorID;
            private final ArrayList<Long> editorIDs;


            /**
             * Make a Note.
             * @param note The note.
             */
            Note(String note, User author) {
                this.creationTime = OffsetDateTime.now();
                this.lastEditTime = this.creationTime;
                this.note = note;
                this.edits = 0;
                this.authorID = author.getIdLong();
                this.editorIDs = new ArrayList<>();
            }

            /**
             * Edit the note content and update the date-time.
             * @param note The new note.
             */
            void edit(String note, User editor) {
                this.note = note;
                this.lastEditTime = OffsetDateTime.now();
                this.edits++;
                if (!this.editorIDs.contains(editor.getIdLong()))
                    this.editorIDs.add(0, editor.getIdLong());
            }

            long lastEditor() {
                if (this.editorIDs.isEmpty())
                    return this.authorID;
                return this.editorIDs.get(0);
            }

            @Override
            public String toString() {
                return this.note;
            }
        }

        final String name;
        final ArrayList<Note> notes;
        final OffsetDateTime creationTime;
        final long authorID;

        /**
         * Long IDs of {@link User Users} allowed to edit this NotePad.
         */
        final ArrayList<Long> allowedUserIDs;

        /**
         * Long IDs of {@link TextChannel Channels} allowed to show this NotePad.
         */
        final ArrayList<Long> allowedChannelIDs;

        /**
         * Long IDs of {@link Role Roles} allowed to edit this NotePad.
         */
        final ArrayList<Long> allowedRoleIDs;

        /**
         * Long IDs of {@link User Users} not allowed to edit this NotePad.
         */
        final ArrayList<Long> blockedUserIDs;

        /**
         * Long IDs of {@link TextChannel Channels} not allowed to show this NotePad.
         */
        final ArrayList<Long> blockedChannelIDs;

        /**
         * List of words that cannot be used as names, to avoid parsing err.
         */
        final static String[] keyWords = new String[] {"make", "write", "add", "insert",
                                                       "edit", "delete", "remove",
                                                       "lockto", "lockout", "clear",
                                                       "toss", "trash", "bin"};

        /**
         * Check if a string is a reserved {@link NotePad#keyWords}.
         * @param test The {@link String} to test.
         * @return {@code true} if test does not match any keyWord.
         */
        private boolean isOk(String test) {
            for (String s: keyWords)
                if (s.equalsIgnoreCase(test))
                    return false;
            return true;
        }

        /**
         * Check if the event can edit the NotePad. TODO separate lists for each entity.
         * @param event
         * @return False if the channel is not allowed. <br>
         *         False if the Member is not allowed. <br>
         *         False if the Role is not allowed.
         */
        public boolean allowedEdit(BetterMessageEvent event) {
            //Check Channel
            long id = event.getTextChannel().getIdLong();
            if (!allowedChannelIDs.isEmpty()) {
                if (!allowedChannelIDs.contains(id)) {
                    return false;
                }
            }

            //The Author can always edit
            if (event.getAuthor().getIdLong() == this.authorID) {
                return true;
            }
            //Admins are admins, Managers are Managers
            List<Permission> perms = event.getMember().getPermissions();
            if (perms.contains(Permission.ADMINISTRATOR)
                || perms.contains(Permission.MANAGE_SERVER)
                || perms.contains(Permission.MANAGE_CHANNEL)
                || perms.contains(Permission.MANAGE_PERMISSIONS)) {
                return true;
            }


            if (!!blockedChannelIDs.isEmpty()) {
                if (blockedChannelIDs.contains(id)) {
                    return false;
                }
            }

            //Check Role
            if (!allowedRoleIDs.isEmpty()) {
                for (Role r : event.getMember().getRoles()) {
                    if (allowedRoleIDs.contains(r.getIdLong()))
                        return true;
                }
            }

            //Check User
            id = event.getAuthor().getIdLong();
            if (!allowedUserIDs.isEmpty()) {
                if (allowedUserIDs.contains(id))
                    return true;
            }

            if (!blockedUserIDs.isEmpty()) {
                if (blockedUserIDs.contains(id)) {
                    return false;
                }
            }

            return true;

        }

        /**
         * Make a new empty NotePad, using {@link OffsetDateTime#now()} as the name.
         * @param authorID
         */
        public NotePad(long authorID) {
            this.authorID = authorID;
            this.creationTime = OffsetDateTime.now();
            this.name = this.creationTime
                      .format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"));
            this.notes = new ArrayList<>();
            allowedUserIDs = new ArrayList<>();
            allowedChannelIDs = new ArrayList<>();
            allowedRoleIDs = new ArrayList<>();
            blockedUserIDs = new ArrayList<>();
            blockedChannelIDs = new ArrayList<>();
        }

        /**
         * Make a NotePad named after a {@link Weebot Weebot's} {@link Guild} name.
         * @param bot The bot whence the Guild name comes.
         * @param authorID
         */
        public NotePad(Weebot bot, long authorID) {
            this.authorID = authorID;
            this.creationTime = OffsetDateTime.now();
            if (bot.getGuildName() == null) {
                this.name = "Private NotePad";
            } else if (isOk(bot.getGuildName())) {
                this.name = bot.getGuildName();
            } else {
                this.name = this.creationTime
                      .format(DateTimeFormatter.ofPattern("m-d-y HH:mm:ss"));
            }
            this.notes = new ArrayList<>();
            allowedUserIDs = new ArrayList<>();
            allowedChannelIDs = new ArrayList<>();
            allowedRoleIDs = new ArrayList<>();
            blockedUserIDs = new ArrayList<>();
            blockedChannelIDs = new ArrayList<>();
        }

        /**
         * Make an empty NotePad named after a {@link Guild}.
         * @param guild The guild to name after.
         * @param authorID
         */
        public NotePad(Guild guild, long authorID) {
            this.authorID = authorID;
            this.creationTime = OffsetDateTime.now();
            if (isOk(guild.getName())) {
                this.name = guild.getName();
            } else {
                this.name = this.creationTime
                      .format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"));
            }
            this.notes = new ArrayList<>();
            allowedUserIDs = new ArrayList<>();
            allowedChannelIDs = new ArrayList<>();
            allowedRoleIDs = new ArrayList<>();
            blockedUserIDs = new ArrayList<>();
            blockedChannelIDs = new ArrayList<>();
        }

        /**
         * Make a new NotePad with the given name and empty notes.
         *
         * @param name
         *         The name of the NotePad. (Will have spaces replaced with '_' char)
         * @param authorID
         */
        public NotePad(String name, long authorID) throws InvalidNameException {
            this.name = String.join("_", name.split(" "));
            this.authorID = authorID;
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.creationTime = OffsetDateTime.now();
            allowedUserIDs = new ArrayList<>();
            allowedChannelIDs = new ArrayList<>();
            allowedRoleIDs = new ArrayList<>();
            blockedUserIDs = new ArrayList<>();
            blockedChannelIDs = new ArrayList<>();
        }

        /**
         * Make a new NotePad with an initial list of {@link String} notes.
         *  @param name
         *         The name of the NotePad.
         * @param authorID
         * @param notes
         */
        public NotePad(User author, String name, long authorID, String... notes) throws
                InvalidNameException {
            this.creationTime = OffsetDateTime.now();
            this.name = String.join("_", name.split(" "));
            this.authorID = authorID;
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.addNotes(author, notes);
            allowedUserIDs = new ArrayList<>();
            blockedUserIDs = new ArrayList<>();
            allowedRoleIDs = new ArrayList<>();
            allowedChannelIDs = new ArrayList<>();
            blockedChannelIDs = new ArrayList<>();
        }

        /**
         * Make a new NotePad with an initial list of {@link String} notes.
         *  @param name
         *         The name of the NotePad.
         * @param notes
         * @param authorID
         */
        public NotePad(String name, Collection<String> notes, User author, long authorID) throws
                InvalidNameException {
            this.name = String.join("_", name.split(" "));
            this.authorID = authorID;
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.addNotes(author, notes);
            this.creationTime = OffsetDateTime.now();
            allowedUserIDs = new ArrayList<>();
            blockedChannelIDs = new ArrayList<>();
            allowedChannelIDs = new ArrayList<>();
            allowedRoleIDs = new ArrayList<>();
            blockedUserIDs = new ArrayList<>();
        }

        /**
         * Add notes to the NotePad.
         *
         * @param notes
         *         The {@link String} notes to add
         */
        public void addNotes(User user, String... notes) {
            for (String s : notes) {
                this.notes.add(new Note(s, user));
            }
        }

        /**
         * Add notes to the NotePad.
         *
         * @param notes
         *         The notes to add
         */
        public void addNotes(User user, Collection<String> notes) {
            for (String s : notes) {
                this.notes.add(new Note(s, user));
            }
        }

        /**
         * Inserts notes at the given index, pushing the element at that index down.
         * @param index The index to insert the notes at
         * @param notes The notes to insert
         */
        public void insertNotes(User author, int index, String...notes) {
            ArrayList<Note> add = new ArrayList<>();
            for (String s : notes) {
                add.add(new Note(s, author));
            }
            try {
                this.notes.addAll((index < 0 ? 0 : index), add);
            } catch (IndexOutOfBoundsException e) {
                this.addNotes(author, notes);
            }
        }

        /**
         * Edit a note.
         * @param index The index of the note.
         * @param edit The new note.
         * @return The message previously held by the note.
         */
        public String editNote(int index, String edit, User editor) throws
                IndexOutOfBoundsException {
            String old = this.notes.get(index).note;
            this.notes.get(index).edit(edit, editor);
            return old;
        }

        /**
         * Remove a note from the NotePad.
         *
         * @param index
         *         The index of the note.
         *
         * @return The note removed.
         */
        public Note deleteNote(int index) throws IndexOutOfBoundsException {
            return this.notes.remove(index);
        }

        /**
         * Remove notes from the NotePad.
         *
         * @param indecies
         *         The indeces of the notes to remove.
         *
         * @return The removed notes.
         */
        public Note[] deleteNotes(int... indecies) {
            Note[] out = new Note[indecies.length];
            for (Integer i : indecies) {
                out[i - indecies[0]] = this.notes.get(i);
                this.notes.remove(i);
            }
            return out;
        }

        /**
         * Clear the NotePad's notes
         * @return The removed notes
         */
        public Note[] clear() {
            Note[] out = new Note[this.notes.size()];
            this.notes.toArray(out);
            this.notes.clear();
            return out;
        }

        /**
         * Locate a NotePad from a lsit
         * @param name The name of the NotePad
         * @param bucket The List to search in
         * @return The NotePad or {@code null} if not found.
         */
        public static NotePad find(String name, List<NotePad> bucket) {
            for (NotePad n : bucket) {
                if (n.name.equalsIgnoreCase(name)) {
                    return n;
                }
            }
            return null;
        }

        @Override
        public Iterator<Note> iterator() {
            return this.notes.iterator();
        }

    }

    private enum ACTION {MAKE, WRITE, INSERT, EDIT, GET, LOCKTO, LOCKOUT, DELETE, CLEAR,
                            TRASH, FILE}

    public NotePadCommand() {
        super(
                "NotePad",
                new ArrayList<>(Arrays.asList(
                        "notepads", "notes", "jotter", "todo", "note"
                )),
                "Write a note for me to keep track of for you",
                "notes\n"   +
                "notes #\n" +
                "notes # file\n"    +
                "notes # clear\n"   +
                "notes # toss/trash/bin\n"  +
                "notes # delete/remove #\n" +
                "notes make [the name]\n"   +
                "notes # write/add <new message>\n"    +
                "notes # insert # <new message>\n"    +
                "notes # edit # <new message>\n"   +
                "notes # lockto [roles, members, or channels]\n"    +
                "notes # lockout [roles, members,, or channels]\n",
                true,
                false,
                0,
                false
        );
    }

    /**
     * Performs a check then runs the command.
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : NotePadCommand");
            thread.start();
        }
    }

    /**
     * <code>
     * Possible Command Argument Formats <br>
     * notes <br>
     * notes # <br>
     * notes # file <br>
     * notes # clear <br>
     * notes # toss/trash/bin <br>
     * notes # delete/remove # <br>
     * notes make [the name] <br>
     * notes # write/add <\the message> <br>
     * notes # insert # <\here the message> <br>
     * notes # edit # <\new message> <br>
     * notes # lockto [some roles, memebers, or channels] TODO
     * notes # lockout [some roles, memebers, or channels] TODO
     * </code>
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected synchronized void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        ArrayList<NotePad> notes = bot.getNotePads();
        NotePad pad;
        String out;
        int i;

        //If 'notes'
        if (args.length == 1) {
            if (notes.size() == 0) {
                event.reply("There are no NotePads. " +
                        "Use this command to make a new NotePad:```" +
                        "notes make [The Name]``` or ```help notes``` for more help.");
                return;
            }
            out = "Here are the available NotePads:```";
            i = 1;
            for (NotePad n : notes) {
                //TODO add permission check here
                if (n.allowedEdit(event)) {
                    out = out.concat(i + ".) " + n.name + "\n");
                }
                i++;
            }
            event.reply(out.concat(
                    "```Use ``notes <notepad_number>`` to see the content of a NotePad"
                    + "or ``help notes`` for more help."
            ));
            new Thread(() -> event.deleteMessage()).start();
            return;
        }

        //Check for the make command, since it is a special abnormality.
        if (parseAction(args[1]) == ACTION.MAKE) {
            //If no name was given
            NotePad nPad;
            if (args.length == 2) {
                nPad = new NotePad(event.getAuthor().getIdLong());
                notes.add(nPad);
            } else {
                String name =
                        String.join(" ",
                                    Arrays.copyOfRange(args, 2,args.length)
                        ).trim();
                try {
                    nPad = new NotePad(name, event.getAuthor().getIdLong());
                    notes.add(nPad);
                } catch (NamingException e) {
                    event.reply("Sorry, '" + name + "' can't be used as a NotePad" +
                                        " name. Make sure you name isn't any of" +
                                        " these ``\"make\", \"write\", \"add\"," +
                                        " \"insert\", \"edit\", \"delete\", " +
                                        "\"remove\", \"clear\", \"toss\", " +
                                        "\"trash\", \"bin\", \"lockto\", \"lockout\"``"
                    );
                    return;
                }
            }
            event.reply(nPad.name + " has been created. It's number "
                                + (notes.indexOf(nPad) + 1) + "."
            );
            new Thread(() -> event.deleteMessage()).start();
            return;
        }

        //Any other command should start as 'notes #' , so get the notepad.
        pad = this.parseNotePad(notes, args[1], event);

        if (pad == null) {
            //If the parser had an err, exit the command.
            return;
        } else if (!pad.allowedEdit(event)) {
            event.reply(
                    "You do not have permission to view or edit this NotePad."
                    + " Use ``notes`` to view all NotePads you have access to."
            );
            return;
        }

        //Should be 'notes #' , so display NotePad
        if (args.length == 2) {
            if (pad.notes.isEmpty()) {
                event.reply(pad.name + " is empty.");
                return;
            }
            out = "```(" + (notes.indexOf(pad) + 1) + ") " + pad.name + "\n\n";
            i = 1;
            for (NotePad.Note n : pad) {
                out = out.concat(n.lastEditTime.format(
                                DateTimeFormatter.ofPattern(
                                        "d-M-y hh:mm:ss"))
                                + "\n" + i++ + ".) " + n + "\n\n"
                );
            }
            out += "```";
            event.reply(out);
            new Thread(() -> event.deleteMessage()).start();
            return;
        }

        //Any other command requires and action to be specified next
        ACTION action = this.parseAction(args[2]);
        if (action == null) {
            event.reply("Sorry, I couldn't understand '" +
                                String.join(" ", args) + "'. Please ``help notes`` for " +
                                "available commands."
            );
            return;
        }

        //This is where the serious divergance happens, since some actions
        //can take an infinite number of args (when taking in new notes).
        switch (action) {
            case WRITE:
                String nNote =
                        String.join(" ",Arrays.copyOfRange(args,3,args.length)).trim();
                if (nNote.isEmpty()) {
                    event.reply("The note's message cannot be blank.");
                    return;
                }
                pad.addNotes(event.getAuthor(), nNote);
                event.reply("``" + nNote + "`` was added to " + pad.name + ".");
                new Thread(() -> event.deleteMessage()).start();
                return;
            case INSERT:
                nNote =
                    String.join(" ", Arrays.copyOfRange(args, 4,args.length)).trim();
                if (nNote.isEmpty()) {
                    event.reply("The note's message cannot be blank.");
                    return;
                }
                try {
                    pad.insertNotes(event.getAuthor(), Integer.parseInt(args[4]), nNote);
                    event.reply("``" + nNote + "`` was added to " + pad.name + ".");
                    new Thread(() -> event.deleteMessage()).start();
                } catch (NumberFormatException e) {
                    event.reply("Sorry, I couldn't understand '" +
                                    args[3] + "'. Please use an integer " +
                                    "between 1 and 2,147,483,647.");
                }
                return;
            case EDIT:
                try {
                    nNote = String.join(" ", Arrays.copyOfRange(args,4, args.length))
                                  .trim();
                    if (nNote.isEmpty()) {
                        event.reply("The note's message cannot be blank.");
                        return;
                    }
                    //Index read starting at 1
                    String old = pad.editNote(Integer.parseInt(args[3]) - 1, nNote,
                                              event.getAuthor());
                    event.reply("``" + old + "`` was replaced with ``" + nNote + "`` in" +
                                        " ``" + pad.name + "``.");
                    new Thread(() -> event.deleteMessage()).start();
                } catch (NumberFormatException e) {
                    event.reply("Sorry, I couldn't understand '" +
                                        args[3] + "'. Please use an integer " +
                                        "between 1 and 2,147,483,647.");
                } catch (IndexOutOfBoundsException e) {
                    noteIndexOutOfBoundsMessage(args[3], event);
                }
                return;
            case GET:
                try {
                    NotePad.Note note = pad.notes.get(Integer.parseInt(args[3]) - 1);
                    out = "```" + note.note + "```\nCreated ``"
                        + note.creationTime
                               .format(DateTimeFormatter.ofPattern("M-d-y HH:mm:ss"))
                        +"`` by " + Launcher.getJda().getUserById(note.authorID).getName()
                        + ".\nEdited " + note.edits + (note.edits != 1 ? " times."
                                                                      : " time.")
                        + (note.creationTime != note.lastEditTime
                           ? " Last edited ``" + note.lastEditTime.format(
                                   DateTimeFormatter.ofPattern("M-d-y HH:mm:ss")) +
                                   "`` by " + Launcher.getJda()
                                                      .getUserById(note.editorIDs.get(0))
                                                      .getName()
                           : ".");
                    event.reply(out);
                    new Thread(() -> event.deleteMessage()).start();
                } catch (NumberFormatException e) {
                    event.reply("Sorry, I couldn't understand '" +
                                        args[3] + "'. Please use an integer " +
                                        "between 1 and 2,147,483,647.");
                } catch (IndexOutOfBoundsException e) {
                    noteIndexOutOfBoundsMessage(args[3], event);
                }
                return;
            case DELETE:
                try {
                    NotePad.Note oNote = pad.deleteNote(Integer.parseInt(args[3]) - 1);
                    event.reply("``" + oNote + "`` was removed from " + pad.name + ".");
                    new Thread(() -> event.deleteMessage()).start();
                } catch (NumberFormatException e) {
                    event.reply("Sorry, I couldn't understand '" +
                                       args[3] + "'. Please use an integer " +
                                       "between 1 and 2,147,483,647.");
                } catch (IndexOutOfBoundsException e) {
                    noteIndexOutOfBoundsMessage(args[3], event);
                }
                return;
            case LOCKTO:
                StringBuilder sb = new StringBuilder(
                                            pad.name + " NotePad has been locked to ");
                i  = 0;
                Message m = event.getMessage();
                for (TextChannel c : m.getMentionedChannels()) {
                    pad.allowedChannelIDs.add(c.getIdLong());
                    pad.blockedChannelIDs.remove(c.getIdLong());
                    sb.append("#" + c.getName() + ", ");
                    i++;
                }
                for (Member u : m.getMentionedMembers()) {
                    pad.allowedUserIDs.add(u.getUser().getIdLong());
                    pad.blockedUserIDs.remove(u.getUser().getIdLong());
                    sb.append("@" + u.getEffectiveName() + ", ");
                    i++;
                }
                for (Role r : m.getMentionedRoles()) {
                    pad.allowedRoleIDs.add(r.getIdLong());
                    sb.append("@" + r.getName() + ", ");
                    i++;
                }
                if (i == 0) {
                    event.reply("A TextChannel, Member, or Role must be mentioned using" +
                            "``#channel, @member, or @role``.");
                } else {
                    sb.setLength(sb.length() - 2);
                    sb.append(".");
                    event.reply(sb.toString());
                }
                return;
            case LOCKOUT:
                sb = new StringBuilder();
                m = event.getMessage();
                for (TextChannel c : m.getMentionedChannels()) {
                    pad.blockedChannelIDs.add(c.getIdLong());
                    pad.allowedChannelIDs.remove(c.getIdLong());
                    sb.append("#" + c.getName() + ", ");
                }
                for (Member u : m.getMentionedMembers()) {
                    pad.blockedUserIDs.add(u.getUser().getIdLong());
                    pad.allowedUserIDs.remove(u.getUser().getIdLong());
                    sb.append("@" + u.getEffectiveName() + ", ");
                }
                for (Role r : m.getMentionedRoles()) {
                    pad.allowedRoleIDs.remove(r.getIdLong());
                    sb.append("@" + r.getName() + ", ");
                }
                if (sb.length() == 0) {
                    event.reply("A TextChannel, Member, or Role must be mentioned using" +
                                        "``#channel, @member, or @role``.");
                } else {
                    sb.setLength(sb.length() - 2);
                    sb.append("have been blocked from viewing or editing "+pad.name+ ".");
                    event.reply(sb.toString());
                }
                return;
            case CLEAR:
                pad.clear();
                event.reply(pad.name + " NotePad was cleared.");
                new Thread(() -> event.deleteMessage()).start();
                return;
            case TRASH:
                notes.remove(pad);
                event.reply(pad.name + " NotePad was deleted by " + event.getAuthor()
                        .getName() + ".");
                new Thread(() -> event.deleteMessage()).start();
                return;
            case FILE:
                if (pad.notes.isEmpty())
                    event.reply(pad.name + " NotePad is empty.");
                else
                   event.reply(makeNotePadFile(pad), pad.name + " NotePad.txt",
                                 file -> file.deleteOnExit());
                return;
            default:
                return;
        }

    }

    private static File makeNotePadFile(NotePad pad) {
        String name = "/temp/out/" + pad.name + " NotePad.txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(name))) {
            String out = pad.name + "\n\n";
            int i = 1;
            for (NotePad.Note n : pad) {
                out = out.concat(n.lastEditTime.format(
                        DateTimeFormatter.ofPattern("d-M-y hh:mm:ss"))
                    + "| Author: " + Launcher.getJda().getUserById(n.authorID).getName()
                    + (n.lastEditTime == n.creationTime ?  "" : " | Last Editor: " +
                        Launcher.getJda().getUserById(n.lastEditor()).getName())
                    + "\n" + i++ + ".) " + n + "\n\n"
                );
            }

            bw.write(out);
            return new File(name);
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }
    }

    /**
     * Try and parse an action from arguments.
     * @param args The args to parse.
     * @return The {@link ACTION} parsed or null if no action was found
     */
    private ACTION parseAction(String...args) {
        for (String s : args) {
            switch (s.toLowerCase()) {
                case "make":
                    return ACTION.MAKE;
                case "write":
                case "add":
                    return ACTION.WRITE;
                case "insert":
                    return ACTION.INSERT;
                case "edit":
                    return ACTION.EDIT;
                case "get":
                case "see":
                    return ACTION.GET;
                case "delete":
                case "remove":
                    return ACTION.DELETE;
                case "clear":
                    return ACTION.CLEAR;
                case "toss":
                case "trash":
                case "bin":
                    return ACTION.TRASH;
                case "file":
                    return ACTION.FILE;
                case "lockto":
                    return ACTION.LOCKTO;
                case "lockout":
                    return ACTION.LOCKOUT;
            }
        }
        return null;
    }

    /**
     * Parse a NotePad from arguments.
     * @param notes The list of NotePads to check against.
     * @param args The arguments to parse.
     * @return The parsed NotePad or null if not found.
     */
    private NotePad parseNotePad(List<NotePad> notes, String...args) {
        for (String s : args) {
            NotePad pad = NotePad.find(s, notes);
            if (pad != null) {
                return pad;
            }
        }
        return null;
    }

    /**
     * Attempts to parse a NotePad from a string. If it fails, an err message is
     * sent by the {@link BetterMessageEvent event}.
     * @param notes The NotePad {@link List} to retrieve from.
     * @param index The {@link String} to parse for an {@link Integer}.
     *                 (Based on 1 based counting array [1,2,3] not [0,1,2])
     * @param event The event to reply from if an err occurs.
     * @return The NotePad at the passed index.<br>Null if and err occurred.
     */
    private NotePad parseNotePad
            (List<NotePad> notes, String index, BetterMessageEvent event) {
        NotePad pad;
        try {
            return notes.get(Integer.parseInt(index) - 1);
        } catch (NumberFormatException e) {
            event.reply("Sorry, I couldn't understand '" +
                    index + "'. Please use an integer " +
                    "between 1 and 2,147,483,647.");
            return null;
        } catch (IndexOutOfBoundsException e) {
            this.notePadindexOutOfBoundsMessage(index, event, notes);
            return null;
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
    private void notePadindexOutOfBoundsMessage
            (String arg, BetterMessageEvent event, List<NotePad> notes) {
        if (Integer.parseInt(arg) > notes.size()) {
            int k = notes.size();
            event.reply("There " + (k != 1 ? "are" : "is") + " only " + notes.size()
                                 + " Note Pad" + (k != 1 ? "s" : "") + "."
                                 + "\nUse ```" + this.getArgFormat() + "``` to "
                                 + "write or edit a specific notepad.");
            return;
        }
        String out = "I couldn't find NotePad " + arg + ". I will send a " +
                "private message of all the NotePads available to you.";
        event.reply(out);
        out = "Here are the available NotePads "
                + (event.getGuild() != null ? "on " + event.getGuild().getName()
                                            : "")
                + "\n```";
        int i = 1;
        for (NotePad n : notes) {
            out = out.concat(i++ + ".) " + n.name + "\n");
        }
        event.privateReply(out.concat(
                "```\nUse ```" + this.getArgFormat() + "``` to " +
                        "make or edit a specific notepad."
        ));
    }

    /**
     * "I couldn't find a note " + arg + ". Please use a number" +
     *                             " listed in the NotePad."
     * @param arg
     * @param event
     */
    private static void noteIndexOutOfBoundsMessage
            (String arg, BetterMessageEvent event) {
        event.reply("I couldn't find a note " + arg + ". Please use a number" +
                            " listed in the NotePad.");
    }

    @Override
    public String getHelp() {
        String out = "";

        out += "```";

        out += "NotePadCommand Help:\n\n"
            +  "<required> , [optional]\n\n";

        out += this.argFormat
            +  "\nNote: any instance of 'notes' can be replaced with any of the following"
            + this.aliases.toString() + ".\n"
            + "Note: X # Y # = X <notepad_number> Y <note_number>.";
        out += "```";

        return out;
    }

}
