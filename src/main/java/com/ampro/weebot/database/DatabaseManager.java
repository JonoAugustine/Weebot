package com.ampro.weebot.database;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.util.Logger;
import com.ampro.weebot.util.io.CommandClassAdapter;
import com.ampro.weebot.util.io.FileManager;
import com.ampro.weebot.util.io.InterfaceAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;

/**
 * Holds a database and reads & writes {@code Database} it from/to file
 * using GSON (JSON).
 * @author Jonathan Augustine
 */
public class DatabaseManager extends FileManager {

    /** Database Directory */
    public static final File DIR_DBS = new File(FileManager.DIR_HOME, "databases");

    private static final File SAVE = new File(DIR_DBS, "database.wbot");
    private static final File BKUP = new File(DIR_DBS, "dbsBK.wbot");
    private static final File BKBK = new File(TEMP, "dbstemp.wbot");

    private static final Gson GSON
            = new GsonBuilder().enableComplexMapKeySerialization()
                               .setExclusionStrategies().setPrettyPrinting()
                               .registerTypeAdapter(IPassive.class,
                                                    new InterfaceAdapter<>())
                               .registerTypeAdapter(Class.class,
                                                    new CommandClassAdapter())
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
        if (!corruptBackupReadCheck()) return -2;
        try {
            if (!DIR_DBS.mkdir()) {
                //System.err.println("\tDirectory not created.");
            }
            if (!SAVE.createNewFile()) {
                //System.err.println("\tFile not created");
            }
        } catch (IOException e) {
            System.err.println("IOException while creating Database file.");
            e.printStackTrace();
            return -1;
        }
        try (Writer writer = new FileWriter(SAVE)) {
            GSON.toJson(database, writer);
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

    /**
     * Save a backup of the database.
     * @param database
     * @return -1 if any error prevented the database to be backed up.
     */
    public static synchronized int backUp(Database database) {
        try {
            if (!DIR_DBS.exists() && DIR_DBS.mkdirs()) {
                Logger.derr("[Database Manager] Failed to generate database dir!");
                return -1;
            }
            else if (!BKUP.exists() && !BKUP.createNewFile()) {
                Logger.derr("[Database Manager] Failed to generate database backup!");
                return -1;
            }
        } catch (IOException e) {
            Logger.derr("IOException while creating Database backup file.");
            e.printStackTrace();
            return -1;
        }
        if (!corruptBackupWriteCheck(database)) return -1;
        try (Writer writer = new FileWriter(BKUP)) {
            GSON.toJson(database, writer);
            return 1;
        } catch (FileNotFoundException e) {
            Logger.derr("File not found while writing gson backup to file.");
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            Logger.derr("IOException while writing gson backup to file.");
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
        String f = "[Database Manager]";
        Database out = null;
        Database bk = null;
        try (Reader reader = new FileReader(SAVE)) {
            out = GSON.fromJson(reader, Database.class);
        } catch (FileNotFoundException e) {
            System.err.println( f +
                    "Unable to locate database.wbot.\n\tAttempting to load backup file..."
            );
        } catch (IOException e) {
            System.err.println(f + "IOException while reading gson from file.");
            e.printStackTrace();
        }
        try (Reader bKreader = new FileReader(BKUP)) {
             bk = GSON.fromJson(bKreader, Database.class);
        } catch (FileNotFoundException e) {
            System.err.println(f + "\t\tUnable to locate databseBK.wbot.");
            //e.printStackTrace();
            //e2.printStackTrace();
        } catch (Exception e) {
            System.err.println( f + "\tException while reading gson from backup file.");
            e.printStackTrace();
        }
        return out == bk ? out : (bk == null ? out : bk);

    }

    /**
     * Attempt to load the backup file. If any {@link Gson} exceptions are thrown
     * while the file is found, return false.
     *
     * @return {@code false} if gson fails to load the backup.
     */
    private static synchronized boolean corruptBackupReadCheck() {
        try (Reader reader = new FileReader(BKBK)) {
            GSON.fromJson(reader, Database.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    /**
     * Attempt to load the backup file. If any {@link Gson} exceptions are thrown
     * while the file is found, return false.
     *
     * @return {@code false} if gson fails to load the backup.
     */
    private static synchronized boolean corruptBackupWriteCheck(Database db) {
        try (Writer writer = new FileWriter(BKBK)) {
            GSON.toJson(db, writer);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            BKBK.delete();
        }
        return true;
    }

}
