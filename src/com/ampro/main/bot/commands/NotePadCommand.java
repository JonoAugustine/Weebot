package com.ampro.main.bot.commands;

import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * View and modify {@link NotePad Note Pads}.
 *
 * @author Jonathan Augustine
 */
public class NotePadCommand extends Command {

    /**
     * A way for members to keep a notepad of ideas and whatnot.\
     * Each notepad has a {@link String} name and {@link ArrayList} of
     * strings as notes.
     *
     * @author Jonathan Augustine
     */
    public static final class NotePad {
        final String name;
        final ArrayList<String> notes;

        /**
         * Make a new empty NotePad, using {@link OffsetDateTime#now()} as the
         * name.
         */
        NotePad() {
            this.name = OffsetDateTime.now().toString();
            this.notes = new ArrayList<>();
        }

        /**
         * Make a new NotePad with the given name and empty notes.
         * @param name Th ename of the NotePad.
         */
        NotePad(String name) {
            this.name = name;
            this.notes = new ArrayList<>();
        }

        /**
         * Make a new NotePad with an initial list of {@link String} notes.
         * @param name The name of the NotePad.
         * @param notes Initial notes to add to the NotePad.
         */
        NotePad(String name, String...notes) {
            this.name = name;
            this.notes = new ArrayList<>(Arrays.asList(notes));
        }

        /**
         * Add notes to the NotePad.
         * @param notes The {@link String} notes to add
         */
        public void addNotes(String...notes) {
            this.notes.addAll(Arrays.asList(notes));
        }

        /**
         * Add notes to the NotePad.
         * @param notes The notes to add
         */
        public void addNotes(Collection<String> notes) {
            this.notes.addAll(notes);
        }

        /**
         * Remove a note from the NotePad.
         * @param note The note to remove.
         * @return The removed note.
         */
        public String deleteNote(String note) {
            this.notes.remove(note);
            return note;
        }

        /**
         * Remove notes from the NotePad.
         * @param notes The notes to remove.
         * @return The removed notes.
         */
        public String[] deleteNotes(String...notes) {
            for (String n : notes) {
                this.deleteNote(n);
            }
            return notes;
        }

        /**
         * Remove a note from the NotePad.
         * @param index The index of the note.
         * @return The note removed.
         */
        public String deleteNote(int index) {
            return this.notes.remove(index);
        }

        /**
         * Remove notes from the NotePad.
         * @param indecies The indeces of the notes to remove.
         * @return The removed notes.
         */
        public String[] deleteNotes(int...indecies) {
            String[] out = new String[indecies.length];
            for (Integer i : indecies) {
                out[i - indecies[0]] = this.notes.get(i);
                this.notes.remove(i);
            }
            return out;
        }

    }

    public NotePadCommand() {
        super(
                "NotePad",
                new ArrayList<>(Arrays.asList(
                        "addnote", "selfnote", "jot", "jotdown",
                        "notetoself", "rememberthis", "remember"
                )),
                "Write a note for me to keep track of for you",
                "<commandname> [message to save]",
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

    }

    /**
     * Performs the action of the command.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        synchronized (bot.getNotePads()) {
            //TODO
        }
    }
}
