package com.ampro.weebot.util.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    public static final File DIR_HOME = new File("wbot");
    public static final File TEMP     = new File(DIR_HOME, "temp");
    public static final File TEMP_OUT = new File(TEMP, "out");
    public static final File TEMP_IN = new File(TEMP,"in");

    private static final Gson GSON
            = new GsonBuilder().enableComplexMapKeySerialization()
                               .setExclusionStrategies().setPrettyPrinting().create();

    /**
     * Read a file's text.
     * @param in The file to read
     * @return A String Array, each index representing a line in the file
     * @throws IOException
     */
    public static String[] readFile(File in) throws IOException {
        List<String> out = new ArrayList<>();

        return out.toArray(new String[out.size()]);
    }

    /**
     * Print the given String Array to file, each index its own line.
     * @param name The name of the file.
     * @param in The Strings to print.
     * @return The {@link File} made or {@code null} if unable to create.
     * @throws FileAlreadyExistsException
     */
    public static File toFile(String name, String[] in)
    throws FileAlreadyExistsException {
        File file = new File(DIR_HOME, name);
        //Leave if the file already exists
        if (file.exists())
            throw new FileAlreadyExistsException("File" + " '" + "name"
                                                         + "' already exists.");
        return file;
    }

    /**
     * Save the Config to file in the format:
     * <code>database.wbot</code>
     * @return 0 if the file was saved.
     *        -1 if an IO error occurred.
     *        -2 if a Gson exception was thrown
     *
     */
    public static synchronized int saveJson(File file, Object obj) {
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(obj, writer);
            System.out.println("Config saved.");
        } catch (FileNotFoundException e) {
            System.err.println("File not found while writing gson to file.");
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            System.err.println("IOException while writing gson to file.");
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    /**
     * Load the given file form JSON.
     * @return The parsed object
     *          or null if it was not found or an exception was thrown.
     */
    public static synchronized Object loadJson(File file, Class<?> objClass) {
        try (Reader reader = new FileReader(file)) {
            return GSON.fromJson(reader, objClass);
        } catch (FileNotFoundException e) {
            System.err.println( "Unable to locate" + file.getName());
            return null;
        } catch (IOException e) {
            System.err.println("IOException while reading gson from file.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return {@code false} if gson fails to read the file.
     */
    protected static synchronized boolean corruptJsonTest(File file, Class<?> objcClass) {
        try (Reader reader = new FileReader(file)) {
            GSON.fromJson(reader, objcClass);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    /** Clears the temp folders. */
    public static synchronized void clearTempDirs() {
        try {
            FileUtils.cleanDirectory(TEMP);
        } catch (IOException e) {
            System.err.println("Failed clear temp dir.");
            e.printStackTrace();
        }
    }

}
