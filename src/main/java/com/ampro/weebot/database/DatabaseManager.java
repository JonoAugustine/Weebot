package com.ampro.weebot.database;

import com.ampro.weebot.commands.IPassive;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.*;

/**
 * Holds a database and reads & writes {@code Database} it from/to file
 * using GSON (JSON).
 * //TODO Add another redundancy to the databases
 * @author Jonathan Augustine
 */
public class DatabaseManager {

    public static final File DIR = new File("databases");

    private static final Gson GSON
            = new GsonBuilder().enableComplexMapKeySerialization()
                               .setExclusionStrategies().setPrettyPrinting()
                               .registerTypeAdapter(IPassive.class,
                                                    new InterfaceAdapter<>())
                               .create();

    /**
     * Save the Database to file in the format:
     * <code>database.wbot</code>
     * @return 1 if the file was saved.
     *        -1 if an IO error occurred.
     *        -2 if a Gson exception was thrown
     *
     */
    public static synchronized int save(Database database) {
        if (!corruptBackupCheck()) return -2;
        File file = new File(DIR, "database.wbot");
        try {
            if (!DIR.mkdir()) {
                //System.err.println("\tDirectory not created.");
            }
            if (!file.createNewFile()) {
                //System.err.println("\tFile not created");
            }
        } catch (IOException e) {
            System.err.println("IOException while creating Database file.");
            e.printStackTrace();
            return -1;
        }
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(database, writer);
            System.out.println("Database saved.");
        } catch (FileNotFoundException e) {
            System.err.println("File not found while writing gson to file.");
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            System.err.println("IOException while writing gson to file.");
            e.printStackTrace();
            return -1;
        }
        return 1;
    }


    public static synchronized int backUp(Database database) {
        File file = new File(DIR, "databaseBK.wbot");
        try {
            if (!DIR.mkdir()) {
                //System.err.println("\tDirectory not created.");
            }
            if (!file.createNewFile()) {
                //System.err.println("\tFile not created");
            }
        } catch (IOException e) {
            System.err.println("IOException while creating Database backup file.");
            e.printStackTrace();
            return -1;
        }
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(database, writer);
            return 1;
        } catch (FileNotFoundException e) {
            System.err.println("File not found while writing gson backup to file.");
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            System.err.println("IOException while writing gson backup to file.");
            e.printStackTrace();
            return -1;
        }

    }

    /**
     * Loads database. If main database does not match the backup, return the backup.
     * @return Database in format database.wbot
     *             null if database not found.
     */
    public static synchronized Database load() {
        Database out = null;
        Database bk = null;
        try (Reader reader = new FileReader(new File(DIR, "database.wbot"))) {
            out = GSON.fromJson(reader, Database.class);
        } catch (FileNotFoundException e) {
            System.err.println(
                    "Unable to locate database.wbot."
                    + "\n\tAttempting to load backup file..."
            );
        } catch (IOException e) {
            System.err.println("IOException while reading gson from file.");
            e.printStackTrace();
            return null;
        }
        try (Reader bKreader = new FileReader(new File(DIR, "databaseBK.wbot"))) {
             bk = GSON.fromJson(bKreader, Database.class);
        } catch (FileNotFoundException e) {
            System.err.println("\tUnable to locate databseBK.wbot.");
            //e.printStackTrace();
            //e2.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("IOException while reading gson from backup file.");
            e.printStackTrace();
            e.printStackTrace();
            return null;
        }
        out = out != null ? out : bk;
        return out == bk ? out : bk;
    }

    /**
     * Attempt to load the backup file. If any {@link Gson} exceptions are thrown
     * while the file is found, return false.
     *
     * @return {@code false} if gson fails to load the backup.
     */
    private static synchronized boolean corruptBackupCheck() {
        try (Reader reader = new FileReader(new File(DIR, "databaseBK.wbot"))) {
            Database test = GSON.fromJson(reader, Database.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            return true;
        }
        return true;
    }

}
