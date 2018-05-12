package com.ampro.main.bot.commands;

import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;

import javax.naming.InvalidNameException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * View and modify {@link NotePad Note Pads}.
 *
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
            private OffsetDateTime creationTime;
            String note;

            /**
             * Make a Note.
             * @param note The note.
             */
            Note(String note) {
                this.creationTime = OffsetDateTime.now();
                this.note = note;
            }

            /**
             * Edit the note content and update the date-time.
             * @param note The new note.
             */
            void edit(String note) {
                this.note = note;
                this.creationTime = OffsetDateTime.now();
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
         * as to avoid parsing err.
         */
        final static String[] keyWords = new String[]
                {"write", "add", "delete", "insert", "default", "edit"};
        final ArrayList<Long> allowedRoles;

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
            this.name = "default";
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
                this.name = bot.getGuildName() + " NotePad";
            } else {
                this.name = this.creationTime.toString();
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
                this.name = guild.getName() + " NotePad";
            } else {
                this.name = this.creationTime.toString();
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
        public NotePad(String name, String... notes) throws InvalidNameException {
            this.name = String.join("_", name.split(" "));
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.addNotes(notes);
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
        public NotePad(String name, Collection<String> notes) throws InvalidNameException {
            this.name = String.join("_", name.split(" "));
            if (!isOk(this.name)) throw new InvalidNameException();
            this.notes = new ArrayList<>();
            this.addNotes(notes);
            this.creationTime = OffsetDateTime.now();
        }

        /**
         * Add notes to the NotePad.
         *
         * @param notes
         *         The {@link String} notes to add
         */
        public void addNotes(String... notes) {
            for (String s : notes) {
                this.notes.add(new Note(s));
            }
        }

        /**
         * Add notes to the NotePad.
         *
         * @param notes
         *         The notes to add
         */
        public void addNotes(Collection<String> notes) {
            for (String s : notes) {
                this.notes.add(new Note(s));
            }
        }

        /**
         * Inserts notes at the given index, pushing the element.
         * If the index is below 0, it is not inserted
         * @param index The index to insert the notes at
         * @param notes The notes to insert
         * @return 1 if the notes were added, -1 if the index was too low
         */
        public int insertNotes(int index, String...notes) {
            if (index < 0) return -1;
            ArrayList<Note> add = new ArrayList<>();
            for (String s : notes) {
                add.add(new Note(s));
            }
            try {
                this.notes.addAll(index, add);
            } catch (IndexOutOfBoundsException e) {
                this.addNotes(notes);
            }
            return 1;
        }

        /**
         * Edit a note.
         * @param note The index of the note.
         * @param edit The new note.
         */
        public void editNote(int note, String edit) {
            this.notes.get(note).edit(edit);
        }

        /**
         * Remove a note from the NotePad.
         *
         * @param index
         *         The index of the note.
         *
         * @return The note removed.
         */
        public Note deleteNote(int index) {
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
         * Locate a NotePad from a lsit
         * @param name The name of the NotePad
         * @param bucket The List to search in
         * @return The NotePad or {@code null} if not found.
         */
        public static NotePad find(String name, List<NotePad> bucket) {
            for (NotePad n : bucket) {
                if (n.name == name) {
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

    private enum ACTION {WRITE, INSERT, EDIT, DELETE}

    public NotePadCommand() {
        super(
                "NotePad",
                new ArrayList<>(Arrays.asList(
                        "notes", "jotter", "todo"
                )),
                "Write a note for me to keep track of for you",
                "<notepad/notes/jotter/todo> [notepad_name] " +
                        "[action] [index] [message]",
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
     * Reads, writes, or edits a {@link NotePad}.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            //TODO
            String[] args = this.cleanArgs(bot, event);
            ArrayList<NotePad> notes = bot.getNotePads();
            NotePad pad;
            String out;
            switch (args.length) {
                case 1:
                    //List notepads
                    //Show help
                    break;
                case 2: //Should be '<command> [notepad]'
                    //Try Find the notepad by int then by name
                    try {
                        pad = notes.get(Integer.parseInt(args[1]) - 1);
                    } catch (NumberFormatException e) {
                        pad = NotePad.find(args[1], notes);
                    }
                    if (pad != null) {

                        out = "```"
                            + pad.name == "default" ?  bot.getGuildName() : pad.name
                            + "\n\n";
                        int i = 1;
                        for (NotePad.Note n : pad) {
                            out = out.concat(i++ + ".) " + n + " | "
                                    + n.creationTime.format(
                                            DateTimeFormatter.ofPattern(
                                                    "yy-MM-dd HH:mm:ss"))
                                    + "\n");
                        }
                        event.reply(out.concat("```"));
                    } else {
                        out = "I couldn't find '" + args[1]
                                + ", here are the available NotePads:```";
                        int i = 1;
                        for (NotePad n : notes) {
                            out = out.concat(i++ + ".) " + n.name + "\n");
                        }
                        event.reply(out.concat(
                                "```\nUse ``" + this.getArgFormat() + "`` to " +
                                        "write or edit a specific notepad."
                        ));
                    }
                    return;
                default:
                    //Could be a. '<command> [action] [message]' or
                    //         c. '<command> [notepad] [action] [message]'
                    //         d. '<command> [notepad] [action] [message_int] [message]'

                    //Parse action, notepad and Index
                    ACTION action = this.parseAction(args[1], args[2]);
                    pad = this.parseNotePad(notes, args[1], args[2]);
                    //Index of the start of the message in case 2
                    int messageIndex = 3;

                    if (action == null) {
                        //If an action was note parsed, There is a problem
                        return;
                    } else if (pad == null) {
                        pad = NotePad.find("default", notes);
                        if (pad == null) return;
                        //If the notepad was null then the message index is 2 (case a)
                        messageIndex = 2;
                    }

                    switch (action) {
                        case WRITE:
                            pad.addNotes(
                                    String.join(" ",
                                    Arrays.copyOfRange(args,messageIndex,args.length))
                                            .trim()
                            );
                            return;
                        case INSERT:
                            try {
                                //Index read starting at 1
                                int index = Integer.parseInt(args[3]);
                                pad.insertNotes(index - 1,
                                        String.join(" ",
                                        Arrays.copyOfRange(args,messageIndex+1,
                                                            args.length)
                                        ).trim()
                                );
                            } catch (NumberFormatException e) {
                                event.reply("Sorry, I couldn't understand '" +
                                        args[3] + "'. Please use an integer " +
                                        "between 1 and 2,147,483,647.");
                            }
                            return;
                        case EDIT:
                            try {
                                //Index read starting at 1
                                int index = Integer.parseInt(args[3]);
                                pad.editNote(index - 1,
                                        String.join(" ",
                                                Arrays.copyOfRange(args,messageIndex+1,
                                                        args.length)
                                        ).trim()
                                );
                            } catch (NumberFormatException e) {
                                event.reply("Sorry, I couldn't understand '" +
                                        args[3] + "'. Please use an integer " +
                                        "between 1 and 2,147,483,647.");
                            }
                            return;
                        case DELETE:
                            try {
                                int index = Integer.parseInt(args[])
                            } catch (NumberFormatException e) {

                            }
                            return;
                        default:
                            //This case is REALLY bad and probably impossible
                            return;
                    }
            }
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
                case "write":
                case "add":
                    return ACTION.WRITE;
                case "insert":
                    return ACTION.INSERT;
                case "edit":
                    return ACTION.EDIT;
                case "delete":
                case "remove":
                    return ACTION.DELETE;
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

}
