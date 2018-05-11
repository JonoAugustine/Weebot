package com.ampro.main.bot.commands;

import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;

import javax.naming.InvalidNameException;
import java.time.OffsetDateTime;
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
    public static final class NotePad implements Iterable<String> {

        final String name;
        final ArrayList<String> notes;
        /** List of words that cannot be used as names,
         * as to avoid parsing err.
         */
        final String[] keyWords = new String[]{"write", "add", "delete", "default"};

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
            this.name = OffsetDateTime.now().toString();
            this.notes = new ArrayList<>();
        }

        /**
         * Make a NotePad named after a {@link Weebot Weebot's} {@link Guild} name.
         * @param bot The bot whence the Guild name comes.
         */
        public NotePad(Weebot bot) {
            if (bot.getGuildName() == null) {
                this.name = "Private NotePad";
            } else if (isOk(bot.getGuildName())) {
                this.name = bot.getGuildName() + " NotePad";
            } else {
                this.name = OffsetDateTime.now().toString();
            }
            this.notes = new ArrayList<>();
        }

        /**
         * Make an empty NotePad named after a {@link Guild}.
         * @param guild The guild to name after.
         */
        public NotePad(Guild guild) {
            if (isOk(guild.getName())) {
                this.name = guild.getName() + " NotePad";
            } else {
                this.name = OffsetDateTime.now().toString();
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
            this.notes = new ArrayList<>(Arrays.asList(notes));
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
            this.notes = new ArrayList<>(notes);
        }

        /**
         * Add notes to the NotePad.
         *
         * @param notes
         *         The {@link String} notes to add
         */
        public void addNotes(String... notes) {
            this.notes.addAll(Arrays.asList(notes));
        }

        /**
         * Add notes to the NotePad.
         *
         * @param notes
         *         The notes to add
         */
        public void addNotes(Collection<String> notes) {
            this.notes.addAll(notes);
        }

        /**
         * Remove a note from the NotePad.
         *
         * @param note
         *         The note to remove.
         *
         * @return The removed note.
         */
        public String deleteNote(String note) {
            this.notes.remove(note);
            return note;
        }

        /**
         * Remove notes from the NotePad.
         *
         * @param notes
         *         The notes to remove.
         *
         * @return The removed notes.
         */
        public String[] deleteNotes(String... notes) {
            for (String n : notes) {
                this.deleteNote(n);
            }
            return notes;
        }

        /**
         * Remove a note from the NotePad.
         *
         * @param index
         *         The index of the note.
         *
         * @return The note removed.
         */
        public String deleteNote(int index) {
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
        public String[] deleteNotes(int... indecies) {
            String[] out = new String[indecies.length];
            for (Integer i : indecies) {
                out[i - indecies[0]] = this.notes.get(i);
                this.notes.remove(i);
            }
            return out;
        }

        @Override
        public Iterator<String> iterator() {
            return this.notes.iterator();
        }
    }

    public NotePadCommand() {
        super(
                "NotePad",
                new ArrayList<>(Arrays.asList(
                        "notes", "jotter", "todo"
                )),
                "Write a note for me to keep track of for you",
                "<commandname> [notepad_name] [action] [message]",
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
            TreeMap<String, NotePad> notes = bot.getNotePads();
            NotePad pad;
            switch (args.length) {
                case 1:
                    //Show help
                    break;
                case 2: //Should be <command> [notepad]
                    //Try Find the notepad
                    pad = notes.get(args[1]);
                    if (pad == null) {
                        pad = notes.get("default");
                    }
                        String out = "```" + pad.name + "\n\n";
                        for (String n : pad) {
                            out.concat(n + "\n");
                        }
                        event.reply(out.concat("```"));
                    //Else show help
                    return;
                default:
                    //Could be '<command> [action] [message]' or
                    //         '<command> [action] [notepad] [message]' or
                    //         '<command> [notepad] [action] [message]'
                    //Try read action
                    if (args[1].equalsIgnoreCase("write")
                            || args[1].equalsIgnoreCase("add")) {
                        //Try get notepad by name
                        pad = notes.get(args[2]);
                        if (pad != null) { //If the 2nd arg is a notepad name
                            pad.addNotes(
                                    String.join(
                                            " ",
                                            Arrays.copyOfRange(args, 3,args.length)
                                    )
                            );
                        } else {
                            //If a notepad was not found by that name
                            //write to the default
                            notes.get("default").addNotes(
                                    String.join(
                                            " ",
                                            Arrays.copyOfRange(args, 2,args.length)
                                    )
                            );
                        }

                    } else if (args[2].equalsIgnoreCase("write")
                                    || args[2].equalsIgnoreCase("add")) {

                    }
                    switch (args[1].toLowerCase()) {
                        case "write":
                        case "add":


                    }
                    //  Get default notepad
                    //Try to get the notepad
                    //  Try to read the action
                    break;

            }
        }
    }

}
