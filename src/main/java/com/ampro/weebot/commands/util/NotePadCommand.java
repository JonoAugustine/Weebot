package com.ampro.weebot.commands.util;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.properties.Restriction;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
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

import static com.ampro.weebot.util.io.FileManager.TEMP_OUT;

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
     *     A {@link Weebot} can have up to {@link NotePadCommand#MAX_NOTEPADS}
     *     concurrent NotePads at once.
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
            /** TODO Restrictions used only for user ids */
            final Restriction writeRestrictions;

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
                this.writeRestrictions = new Restriction();
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

        /** Restrictions on seeing the NotePad */
        final Restriction readRestrictions;
        /** Restrictions on writing to the NotePad */
        final Restriction writeRestrictions;

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
         * Check if the event can edit the NotePad.
         * @param event
         * @return True if Author or Admin <br>
         *         False if the channel is not allowed. <br>
         *         False if the Member is not allowed. <br>
         *         False if the Role is not allowed.
         */
        public boolean allowedWrite(BetterMessageEvent event) {
            Member member = event.getMember();

            //The Author can always edit
            if (member.getUser().getIdLong() == this.authorID) {
                return true;
            }

            //Admins are admins so ya know...they win
            if (member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                return true;
            }

            //Check Channel
            if (!writeRestrictions.isAllowed(event.getTextChannel())) {
                return false;
            }

            //Check Role
            for (Role r : event.getMember().getRoles()) {
                if (writeRestrictions.isAllowed(r)) {
                    return true;
                }
            }
             return writeRestrictions.isAllowed(member.getUser());

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
            writeRestrictions = new Restriction();
            readRestrictions = new Restriction();
        }

        /**
         * Make a new NotePad with the given name and empty notes.
         *
         * @param name
         *         The name of the NotePad. (Will have spaces replaced with '_' char)
         * @param authorID
         */
        public NotePad(String name, long authorID) throws InvalidNameException {
            this.name = String.join("_", name.split("\\s+", -1));
            this.authorID = authorID;
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.creationTime = OffsetDateTime.now();
            writeRestrictions = new Restriction();
            readRestrictions = new Restriction();
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

    /** The maximum number of note pads a Weebot will hold for a single guild */
    private static final int MAX_NOTEPADS = 20;

    private enum Action {MAKE, WRITE, INSERT, EDIT, GET, LOCKTO, LOCKOUT, DELETE, CLEAR,
                            TRASH, FILE}

    public NotePadCommand() {
        super(
                "NotePad",
                new String[]{"notepads", "notes", "jotter", "todo", "note"},
                "Write a note for me to keep track of for you",
                        "notes\n"   +
                        "notes make [the name]\n"   +
                        "notes #\n" +
                        "notes # file\n"    +
                        "notes # clear\n"   +
                        "notes # toss/trash/bin\n"  +
                        "notes # delete/remove #\n" +
                        "notes # write/add <new message>\n"    +
                        "notes # insert # <new message>\n"    +
                        "notes # edit # <new message>\n"   +
                        "notes # lockto <roles, members, or channels>\n"    +
                        "notes # lockout <roles, members, or channels>\n",
                true,
                false,
                0,
                false,
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
     * notes # lockto [some roles, members, or channels]
     * notes # lockout [some roles, members, or channels]
     * </code>
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected synchronized void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = cleanArgs(bot, event);
        Message m = event.getMessage();
        ArrayList<NotePad> notes = bot.getNotePads();
        NotePad pad;
        String out;
        int initLeng;

        //If 'notes'
        if (args.length == 1) {
            if (notes.size() == 0) {
                event.reply("There are no NotePads. " +
                        "Use this command to make a new NotePad:```" +
                        "notes make [The Name]``` or ```help notes``` for more help.");
                return;
            }
            out = "Here are the available NotePads:```";
            initLeng = 1;
            for (NotePad n : notes) {
                if (n.allowedWrite(event)) {
                    out = out.concat(initLeng + ".) " + n.name + "\n");
                }
                initLeng++;
            }
            event.reply(out.concat(
                    "```Use ``notes <notepad_number>`` to see the content of a NotePad"
                    + "or ``help notes`` for more help."
            ));
            new Thread(event::deleteMessage).start();
            return;
        }

        //Check for the make command, since it is a special abnormality.
        if (parseAction(args[1]) == Action.MAKE) {
            if (notes.size() >= MAX_NOTEPADS) {
                event.reply("*There are already " + MAX_NOTEPADS + " in this server. "
                            + "Please remove a NotePad before" + "creating a new one.*");
                return;
            }
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
        } else if (!pad.allowedWrite(event)) {
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
            initLeng = 1;
            for (NotePad.Note n : pad) {
                out = out.concat(n.lastEditTime.format(
                                DateTimeFormatter.ofPattern(
                                        "d-M-y hh:mm:ss"))
                                + "\n" + initLeng++ + ".) " + n + "\n\n"
                );
            }
            out += "```";
            event.reply(out);
            new Thread(() -> event.deleteMessage()).start();
            return;
        }

        //Any other command requires and action to be specified next
        Action action = this.parseAction(args[2]);
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
                StringBuilder sb = new StringBuilder();
                m.getMentionedChannels().forEach(c -> {
                    pad.writeRestrictions.allow(c);
                    pad.readRestrictions.allow(c);
                    sb.append("#" + c.getName() + ", ");
                });
                m.getMentionedMembers().forEach( u -> {
                    pad.writeRestrictions.allow(u.getUser());
                    pad.readRestrictions.allow(u.getUser());
                    sb.append("@" + u.getEffectiveName() + ", ");
                });
                m.getMentionedRoles().forEach( r -> {
                    pad.writeRestrictions.allow(r);
                    pad.readRestrictions.allow(r);
                    sb.append("@" + r.getName() + ", ");
                });

                if (sb.length() == 0) {
                    event.reply("A TextChannel, Member, or Role must be mentioned using" +
                            "``#channel, @member, or @role``.");
                } else {
                    sb.insert(0, pad.name + " NotePad has been locked to ");
                    sb.setLength(sb.length() - 2);
                    sb.append(".");
                    event.reply(sb.toString());
                }
                return;
            case LOCKOUT:
                sb = new StringBuilder();
                m = event.getMessage();
                m.getMentionedChannels().forEach(c -> {
                    pad.writeRestrictions.block(c);
                    pad.readRestrictions.block(c);
                    sb.append(c.getAsMention() + ", ");
                });
                m.getMentionedMembers().forEach( u -> {
                    pad.writeRestrictions.block(u.getUser());
                    pad.readRestrictions.block(u.getUser());
                    sb.append(u.getAsMention() + ", ");
                });
                m.getMentionedRoles().forEach( r -> {
                    pad.writeRestrictions.block(r);
                    pad.readRestrictions.block(r);
                    sb.append(r.getAsMention() + ", ");
                });
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

    /**
     * Make a file out of a notepad.
     * @param pad The notepad
     * @return A file of the notepad. Null if an err occurrs.
     */
    private static File makeNotePadFile(NotePad pad) {
        File file = new File(TEMP_OUT, pad.name + " NotePad.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
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
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Try and parse an action from arguments.
     * @param args The args to parse.
     * @return The {@link Action} parsed or null if no action was found
     */
    private Action parseAction(String...args) {
        for (String s : args) {
            switch (s.toLowerCase()) {
                case "make":
                    return Action.MAKE;
                case "write":
                case "add":
                    return Action.WRITE;
                case "insert":
                    return Action.INSERT;
                case "edit":
                    return Action.EDIT;
                case "get":
                case "see":
                    return Action.GET;
                case "delete":
                case "remove":
                    return Action.DELETE;
                case "clear":
                    return Action.CLEAR;
                case "toss":
                case "trash":
                case "bin":
                case "garbo":
                    return Action.TRASH;
                case "file":
                    return Action.FILE;
                case "lockto":
                    return Action.LOCKTO;
                case "lockout":
                    return Action.LOCKOUT;
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
    public final MessageEmbed getEmbedHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Write and edit server NotePads\n")
          .append("Note Pads can be locked to specific")
          .append("roles, members, and text channels.\n");

        EmbedBuilder eb = Launcher.makeEmbedBuilder("Note Pad",
                                                    null, sb.toString());
        sb.setLength(0);

        eb.addField("See all available NotePads", "notes", false)
          .addField("Make a NotePad",
                  "notes make [The_Name]\n*If no name is given, the date and time will " +
                          "be used as the name of the NotePad*", false)
          .addField("See the contents of a Note Pad",
        "notes <notepad_number>\n*To find the NotePad's number, use 'notes'*",
                  false)
          .addField("Write to a NotePad",
                  "notes write <notepad_number> <The Message>\n*Alias:* add",
                  false)
          .addField("Insert a Note into the NotePad",
                    "notes <notepad_number> insert <The Message>", false)
          .addField("Edit (replace) a Note",
                  "notes <notepad_number> edit <note_number> <New Message>", false)
          .addField("Delete a Note from a NotePad",
                    "notes <notepad_number> delete <note_number>\n*Alias*: remove\n",
                    false)
          .addField("Clear a NotePad of all Notes", "notes <notepad_number> clear",
                   false)
          .addField("Get a NotePad as a text file", "notes <notepad_number> file",
                  false)
          .addField("Lock access to a NotePad",
                  "notes <notepad_number> lockto [roles, members, or channels]", false)
          .addField("Lock a NotePad's access from Roles, Members, or Channels",
                  "notes <notepad_number> lockout [roles, members, or channels]", false)
          .addField("Delete a NotePad",
                  "notes <notepad_number> trash\n*Aliases*:toss, bin, garbo", false);

        sb.append("Any usagage of '*notes*' can be replaced with any of the following:\n")
          .append(this.aliases.toString() + ".\n\n")
          .append("<required>   [optional]   /situationally_required/");
        eb.addField("Extra", sb.toString(), false);

        return eb.build();

    }

}
