package com.ampro.main.database;

import java.io.*;

/**
 * Holds a database and reads & writes {@code Database} it from/to file.
 * @author Jonathan Augustine
 */
public class DatabaseManager {

    /**
     * Save the Database to file in the format:
     * <code>database_name.wbot</code>
     * @return 1 if the file was saved.
     *          -1 if the en error occurred.
     */
    public static synchronized int save(Database databse) {
        synchronized (databse) {
            File file = new File(databse.getName() + ".wbot");
            try {
                file.createNewFile();
                ObjectOutputStream oos = new ObjectOutputStream(
                        new FileOutputStream(file)
                );
                oos.writeObject(databse);
                return 1;
            } catch (FileNotFoundException e) {
                System.err.println("FileNotFoundException while writing Database to file.");
                e.printStackTrace();
                return -1;
            } catch (IOException e) {
                System.err.println("IOException while loading Database.");
                e.printStackTrace();
                return -1;
            }
        }
    }

    /**
     * Saves an object to the {@code Database}.
     *
     * @param o The object to save
     */
    public static void save(Object o) {
        //TODO
    }

    /**
     * Loads database.
     * @return Database in format database.wbot
     *             null if database not found.
     */
    public static Database load() {
        File in = new File("database.wbot");
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(in)
            );
            return (Database) ois.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found while loading Database.");
            e.printStackTrace();
            return null;
        } catch (ClassCastException e) {
            System.err.println("Cast to Database failed while loading Database.");
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            System.err.println("IOException while loading Database.");
            return null;
        }
    }

}
