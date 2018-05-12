package com.ampro.main.bot.commands;

import com.ampro.main.Launcher;
import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * View and modify {@link NotePad Note Pads}.
 * TODO: add NotePad permissions/roles allowed to modify
 * TODO: Get more details about a single {@link NotePad.Note note}, e.g. creation time
 * @author Jonathan Augustine
 */
public class NotePadCommand extends Command {

    /**
     * A way for members to keep a notepad of ideas and whatnot. <br>
     * Each notepad has a {@link String} name and {@link ArrayList} of
     * strings as notes.
     * <p>
     *     A {@link Weebot} can have up to //TODO concurrent NotePads at once,
     *      including a single, persistant, Guild-wide "default" NotePad.
     * </p>
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
                    this.editorIDs.add(editor.getIdLong());
            }

            @Override
            public String toString() {
                return this.note;
            }
        }

        final String name;
        final ArrayList<Note> notes;
        final OffsetDateTime creationTime;
        /**
         * List of words that cannot be used as names,
         * to avoid parsing err.
         */
        final static String[] keyWords = new String[]
                {"make", "write", "add", "insert", "edit", "delete", "remove",
                 "clear", "toss", "trash", "bin"};

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
         * Make a new empty NotePad, using {@link OffsetDateTime#now()} as the name.
         */
        public NotePad() {
            this.creationTime = OffsetDateTime.now();
            this.name = this.creationTime
                      .format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"));
            this.notes = new ArrayList<>();
        }

        /**
         * Make a NotePad named after a {@link Weebot Weebot's} {@link Guild} name.
         * @param bot The bot whence the Guild name comes.
         */
        public NotePad(Weebot bot) {
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
        }

        /**
         * Make an empty NotePad named after a {@link Guild}.
         * @param guild The guild to name after.
         */
        public NotePad(Guild guild) {
            this.creationTime = OffsetDateTime.now();
            if (isOk(guild.getName())) {
                this.name = guild.getName();
            } else {
                this.name = this.creationTime
                      .format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"));
            }
            this.notes = new ArrayList<>();
        }

        /**
         * Make a new NotePad with the given name and empty notes.
         *
         * @param name
         *         The name of the NotePad. (Will have spaces replaced with '_' char)
         */
        public NotePad(String name) throws InvalidNameException {
            this.name = String.join("_", name.split(" "));
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.creationTime = OffsetDateTime.now();
        }

        /**
         * Make a new NotePad with an initial list of {@link String} notes.
         *
         * @param name
         *         The name of the NotePad.
         * @param notes
         *         Initial notes to add to the NotePad.
         */
        public NotePad(User author, String name, String... notes) throws
                InvalidNameException {
            this.name = String.join("_", name.split(" "));
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.addNotes(author, notes);
            this.creationTime = OffsetDateTime.now();
        }

        /**
         * Make a new NotePad with an initial list of {@link String} notes.
         *
         * @param name
         *         The name of the NotePad.
         * @param notes
         *         Initial notes to add to the NotePad.
         */
        public NotePad(String name, Collection<String> notes, User author) throws
                InvalidNameException {
            this.name = String.join("_", name.split(" "));
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.addNotes(author, notes);
            this.creationTime = OffsetDateTime.now();
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

    private enum ACTION {MAKE, WRITE, INSERT, EDIT, GET, DELETE, CLEAR, TRASH}

    public NotePadCommand() {
        super(
                "NotePad",
                new ArrayList<>(Arrays.asList(
                        "notepads", "notes", "jotter", "todo"
                )),
                "Write a note for me to keep track of for you",
                "<notepad/notes/jotter/todo> <notepad_number> " +
                        "[<action> [note_number] [message]]",
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
     * notes # clear <br>
     * notes # toss/trash/bin <br>
     * notes # delete/remove # <br>
     * notes make [the name] <br>
     * notes # write/add <\the message> <br>
     * notes # insert # <\here the message> <br>
     * notes # edit # <\new message> <br>
     * </code>
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected synchronized void execute(Weebot bot, BetterMessageEvent event) {
        //TODO
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
                        "<notepad/notes/jotter/todo> <make> [The Name]```");
                return;
            }
            out = "Here are the available NotePads:```";
            i = 1;
            for (NotePad n : notes) {
                //TODO add permission check here
                out = out.concat(i++ + ".) " + n.name + "\n");
            }
            event.reply(out.concat( //TODO more detailed help
                    "```\nUse ``" + this.getArgFormat() + "`` to " +
                            "write or edit a specific notepad."
            ));
            //TODO Show help
            return;
        }

        //Check for the make command, since it is a special abnormality.
        if (parseAction(args[1]) == ACTION.MAKE) {
            //If no name was given
            NotePad nPad;
            if (args.length == 2) {
                nPad = new NotePad();
                notes.add(nPad);
            } else {
                String name =
                        String.join(" ",
                                    Arrays.copyOfRange(args, 2,args.length)
                        ).trim();
                try {
                    nPad = new NotePad(name);
                    notes.add(nPad);
                } catch (NamingException e) {
                    event.reply("Sorry, '" + name + "' can't be used as a NotePad" +
                                        " name. Make sure you name isn't any of" +
                                        " these ``\"make\", \"write\", \"add\"," +
                                        " \"insert\", \"edit\", \"delete\", " +
                                        "\"remove\", \"clear\", \"toss\", " +
                                        "\"trash\", \"bin\"``"
                    );
                    return;
                }
            }
            event.reply(nPad.name + " has been created. It's number "
                                + (notes.indexOf(nPad) + 1) + "."
            );
            return;
        }

        //Any other command should start as 'notes #' , so get the notepad.
        pad = this.parseNotePad(notes, args[1], event);
        //If the parser had an err, exit the command.
        if (pad == null) {
            return;
        }

        //Should be 'notes #' , so display NotePad
        if (args.length == 2) {
            out = "```" + pad.name + "\n\n";
            i = 1;
            if (pad.notes.isEmpty()) {
                event.reply(pad.name + " is empty.");
                return;
            }
            for (NotePad.Note n : pad) {
                out = out.concat(i++ + ".) " + n + "\t|"
                                    + n.lastEditTime.format(
                                        DateTimeFormatter.ofPattern(
                                                "d-M-y hh:mm:ss"))
                                    + "\n"
                );
            }
            out += "```";
            event.reply(out);
            return;
        }

        //Any other command requires and action to be specified next
        ACTION action = this.parseAction(args[2]);
        if (action == null) {
            event.reply("Sorry, I couldn't understand '" +
                                String.join(" ", args) + "'. Please use " +
                                "'write', 'add', 'insert', 'delete', " +
                                "'remove', or 'edit' to modify the NotePad."
            );
            return;
        }

        //This is where the serious divergance happens, since some actions
        //can take an infinite number of args (when taking in new notes).
        switch (action) {
            case WRITE:
                String nNote =
                        String.join(" ",Arrays.copyOfRange(args,3,args.length)).trim();
                pad.addNotes(event.getAuthor(), nNote);
                event.reply("``" + nNote + "`` was added to " + pad.name + ".");
                return;
            case INSERT:
                nNote =
                    String.join(" ", Arrays.copyOfRange(args, 4,args.length)).trim();
                try {
                    pad.insertNotes(event.getAuthor(), Integer.parseInt(args[4]), nNote);
                    event.reply("``" + nNote + "`` was added to " + pad.name + ".");
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
                    //Index read starting at 1
                    String old = pad.editNote(Integer.parseInt(args[3]) - 1, nNote,
                                              event.getAuthor());
                    event.reply("``" + old + "`` was replaced with ``" + nNote + "`` in" +
                                        " ``" + pad.name + "``.");
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
                    pad.deleteNote(Integer.parseInt(args[3]));
                } catch (NumberFormatException e) {
                    event.reply("Sorry, I couldn't understand '" +
                                       args[3] + "'. Please use an integer " +
                                       "between 1 and 2,147,483,647.");
                } catch (IndexOutOfBoundsException e) {
                    noteIndexOutOfBoundsMessage(args[3], event);
                }
                return;
            case CLEAR:
                pad.clear();
                event.reply(pad.name + " NotePad was cleared.");
                return;
            case TRASH:
                notes.remove(pad);
                event.reply(pad.name + " NotePad was deleted.");
                return;
            default:
                return;
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
                                 + "Note Pad" + (k != 1 ? "s" : "") + "."
                                 + "\nUse ``" + this.getArgFormat() + "`` to " +
                                "write or edit a specific notepad.\"");
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
                "```\nUse ``" + this.getArgFormat() + "`` to " +
                        "write or edit a specific notepad."
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

}
