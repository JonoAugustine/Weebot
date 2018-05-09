package com.ampro.main.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

/**
 * Holds a database and reads & writes {@code Database} it from/to file
 * using GSON (JSON).
 * @author Jonathan Augustine
 */
public class DatabaseManager {

    /**
     * Save the Database to file in the format:
     * <code>database.wbot</code>
     * @return 1 if the file was saved.
     *          -1 if the en error occurred.
     */
    public static synchronized int save(Database database) {
        synchronized (database) {
            File file = new File("database.wbot");
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("IOException while creating Database file.");
                e.printStackTrace();
                return -1;
            }
            try (Writer writer = new FileWriter("database.wbot")) {
                Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
                        .setExclusionStrategies().setPrettyPrinting().create();
                gson.toJson(database, writer);
                System.out.println("Database saved.");
                return 1;
            } catch (FileNotFoundException e) {
                System.err.println("File not found while writing gson to file.");
                e.printStackTrace();
                return -1;
            } catch (IOException e) {
                System.err.println("IOException while writing gson to file.");
                e.printStackTrace();
                return -1;
            }
        }
    }

    public static synchronized int backUp(Database database) {
        synchronized (database) {
            File file = new File("databaseBK.wbot");
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("IOException while creating Database backup file.");
                e.printStackTrace();
                return -1;
            }
            try (Writer writer = new FileWriter("databaseBK.wbot")) {
                Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
                        .setExclusionStrategies().setPrettyPrinting().create();
                gson.toJson(database, writer);
                System.out.println("Database backed up.");
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
    }

    /**
     * Loads database.
     * @return Database in format database.wbot
     *             null if database not found.
     */
    public static synchronized Database load() {
        Gson gson = new GsonBuilder().create();
        try (Reader reader = new FileReader("database.wbot")) {
            return gson.fromJson(reader, Database.class);
        } catch (FileNotFoundException e) {
            System.err.println(
                    "Unable to locate database.wbot."
                    + "\n\tAttempting to load backup file..."
            );
            try (Reader bKreader = new FileReader("databaseBK.wbot")) {
                return gson.fromJson(bKreader, Database.class);
            } catch (FileNotFoundException e2) {
                System.err.println("\tUnable to locate databseBK.wbot.");
                //e.printStackTrace();
                //e2.printStackTrace();
            } catch (IOException e2) {
                System.err.println("IOException while reading gson from file.");
                e.printStackTrace();
                e2.printStackTrace();
            }
            return null;
        } catch (IOException e) {
            System.err.println("IOException while reading gson from file.");
            e.printStackTrace();
            return null;
        }
    }

}
