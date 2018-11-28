/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import jdk.nashorn.internal.ir.annotations.Ignore
import kotlinx.coroutines.newSingleThreadContext

import java.io.*

val threadName: String get() = Thread.currentThread().name

fun slog(any: Any = "", inline: Boolean = false)
        = print("${if(inline) "" else "\n"}[$threadName] $any")
fun elog(any: Any = "", inline: Boolean = false)
        = System.err.print("${if(inline) "" else "\n"}[$threadName] $any")

/** A simple logger that will show it's name each time it prints */
class Slogger(val name: String = "") {
    fun slog(any: Any = "") = println("[$name] [$threadName] $any")
    fun elog(any: Any = "") = System.err.println("[$name] [$threadName] $any")
}

/** File and Console logger */
class FileLogger(val name: String, private val timeStamp: Boolean = true) {

    companion object {
        val HEADDER = "## Aquatic Mastery Productions ##\n\t\t # Weebot Log #\n"
    }

    @Transient
    private val log: File = if (name.isNotEmpty()) {
        File(DIR_LOGS, "$name.flog")
    } else {
        File(DIR_LOGS, "log_$NOW_FILE.flog")
    }

    /**
     * Initializes the session's flog file.
     *
     * @return false if the logger fails to initialize
     */
    init {
        if (log.exists()) {
            throw FileAlreadyExistsException(log)
        }
        try {
            log.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        flog("$HEADDER\t $NOW\n\n\n")
    }

    /**
     * Print and flog to file.
     *
     * @param any The object to flog
     */
    fun slog(any: Any = "") {
        val out = if (any == "") {
            ""
        } else {
            "${if (timeStamp) "[$NOW]" else ""} [$name] [info] $any"
        }
        println(out)
        flog(out)
    }

    /**
     * Print err and flog to file.
     *
     * @param any The object to flog
     */
    fun elog(any: Any = "") {
        val out = if (any == "") {
            ""
        } else {
            "${if (timeStamp) "[$NOW]" else ""} [$name] [err] $any"
        }
        System.err.println(out)
        flog(out)
    }

    /**
     * Log to file.
     *
     * @param any The object to flog
     */
    @Synchronized
    fun flog(any: Any) {
        try {
            if (!log.exists()) {
                log.createNewFile()
            }
            if (log.canWrite()) {
                val oWriter = BufferedWriter(FileWriter(log, true))
                oWriter.newLine()
                oWriter.write(any.toString())
                oWriter.close()
            }
        } catch (oException: IOException) {
            oException.printStackTrace()
        }
    }

}

/**
 * A Logger that keeps all messages sent to it in an internal list.
 * The logger does not print a message passed to it unless the
 * InternalLog#logAndPrint() function is used.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class InternalLog(val name: String = "flog", initSize: Int = 100_000,
                  var showName: Boolean = false, var showThread: Boolean = false) {

    /** A flog message that can be an err or a normal message */
    data class Message(val any: Any, val err: Boolean = false,
                       val logName: String = "", val thread: String = "") {
        override fun toString(): String
                = "${if (logName.isBlank()) "" else "[$logName]"} " +
                "${if (thread.isBlank()) "" else "[$thread]"} " +
                "[${if (err) "err" else "info"}] $any"
    }

    @Ignore
    //val logThread = newSingleThreadContext("LOG")
    val log = ArrayList<Message>(initSize)

    /** Add a Message to the flog */
    fun log(any: Any, err: Boolean = false) = synchronized(log) {
        val logname = if (showName) name else ""
        val thread = if (showThread) Thread.currentThread().name else ""
        val message = Message(any, err, logname, thread)
        log.add(message)
        return@synchronized message
    }

    fun logAndPrint(any: Any, err: Boolean = false) {
        if (!err) println(log(any, err))
        else System.err.println(log(any, err))
    }

    /**
     * Print each Message on it's own line.
     *
     * @param printStream The PrintStream for non-err messages (default = System.out)
     * @param errStream The PrintStream for err messages (default = System.err)
     */
    fun print(printStream: PrintStream = System.out,
              errStream: PrintStream = System.err) {
        log.forEach {
            if (!it.err) errStream.println("[$name] $it")
            else printStream.println("[$name] $it")
        }
    }

    /**
     * Saves the flog to a file named after the flog
     *
     * @return The created File
     */
    fun toFile() = log.toFile("${DIR_LOGS.name}/${name}_$NOW_FILE.flog")

    /** Add all lines from an InternalLog into this flog */
    fun ingest(src: InternalLog) = synchronized(log) { this.log.addAll(src.log) }

    /** Add all lines from an InternalLog into this flog and print them */
    fun ingestAndPrint(src: InternalLog) = synchronized(log) {
        this.log.addAll(src.log)
        src.log.forEach {
            if (!it.err) println(it)
            else System.err.println(it)
        }
    }
}
