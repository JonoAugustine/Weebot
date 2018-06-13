package com.ampro.weebot.util;

import com.ampro.weebot.util.io.FileManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File & Console logger.
 */
public class Logger {

    public static final transient File LOGS
            = new File(FileManager.DIR_HOME, "logs");

    private static final transient String HEADDER
            = "## Aquatic Mastery Productions ##\n\t\t # Weebot Log #\n";

    private static transient File log;

    /**
     * Initiallizes the session's log file.
     * @return false if the logger fails to initialize
     */
    public static boolean init() {
        OffsetDateTime now = OffsetDateTime.now();
        String name = "log_" + now.format(DateTimeFormatter
                                                .ofPattern("dd-MM-yyyy__hh-mm-ss"));
        log = new File(LOGS, name + ".txt");
        try {
            if (!log.createNewFile())
                return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        String head = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm:ss"));
        log(HEADDER + "\t " + head + "\n\n\n", true);
        return log.exists();
    }

    /**
     * Print to {@link System#err} and log to file.
     * @param obj The object to log
     */
    public static final void dout(Object obj) {
        System.out.println(obj);
        if (log != null) log(obj, true);
    }

    /**
     * Print to {@link System#out} and log to file.
     * @param obj The object to log
     */
    public static final void derr(Object obj) {
        System.err.println(obj);
        if (log != null) log(obj, true);
    }

    /**
     * Log to file.
     * @param obj The object to log
     */
    public static final synchronized void log(Object obj, boolean newLine) {
        try {
            if (!log.exists()) {
                log.createNewFile();
            }
            if (log.canWrite()) {
                BufferedWriter oWriter = new BufferedWriter(new FileWriter(log, true));
                if (newLine) oWriter.newLine();
                oWriter.write(obj.toString());
                oWriter.close();
            }
        }
        catch (IOException oException) {
            oException.printStackTrace();
        }
    }

}
