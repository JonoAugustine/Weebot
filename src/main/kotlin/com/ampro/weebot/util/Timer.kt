package com.ampro.weebot.util

/**
 * Timer and Time-related functions, classes, and variables.
 *
 * @author Jonathan Augustine
 * @since 3.0
 */

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

val DD_MM_YYYY_HH_MM_SS = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
val DD_MM_YYYY_HH_MM    = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

val NOW = OffsetDateTime.now()

/** @return The current local date and time. dd-MM-yyyy HH:mm:ss */
val NOW_STR: String get() = OffsetDateTime.now().format(
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))

/** @return The current local date and time. dd-MM-yyyy HH-mm-ss */
val NOW_STR_FILE: String get() = NOW_STR.replace(":", "-")

/**
 * A Timer object contains a start-time (millisec) that is defined upon creation.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class Timer(private var startTime: Long = System.currentTimeMillis()) {

    /** If the timer is running or not */
    private var running: Boolean = true

    /** The time elapsed since the Timer started (-1 the timer is running) */
    private var elapsedTime: Long = -1

    /** @return The time since the Timer started or was stopped */
    fun getElapsedTime() : Long {
        return if (running) {
            System.currentTimeMillis() - startTime
        } else {
            elapsedTime
        }
    }

    companion object {
        fun format(millis: Long) : String {
            var seconds = (millis / 1_000).toDouble()//Math.pow(10.0, 9.0)
            var hours = 0
            var min = 0
            if (seconds > 60) {
                var i = 0
                while (i < seconds) {
                    if (i == 60) {
                        min++
                        seconds -= 60.0
                        i = 0
                        if (min == 60) {
                            hours++
                            min -= 60
                        }
                    }
                    i++
                }
            }
            return "$hours:$min:$seconds"
        }
    }

    /** @return The current duration of the timer's run time in HH:MMM:SS */
    override fun toString(): String = format(this.getElapsedTime())

    /** Start the Timer if it is not running */
    fun start() {
        this.running = true
        this.elapsedTime -1L
    }

    /**
     * Stop the timer (set the end time)
     *
     * @return The formatted duration
     */
    fun stop() : String {
        this.elapsedTime = System.currentTimeMillis()
        this.running = false
        return this.toString()
    }

    /**
     * Resets the starting time and returns the previous duration
     *
     * @return the last duration formatted
     */
    fun reset() : String {
        val lastTime = this.toString()
        this.startTime = System.currentTimeMillis()
        return lastTime
    }

}

/**
 * @param timers Array of Timers
 * @return The average duration of the provided Timers, formatted HH:MMM:SS
 */
fun timerAvg(timers: List<Timer>): String {

    var averageNanoTime: Long = 0

    timers.forEach { averageNanoTime += it.getElapsedTime() }

    averageNanoTime /= timers.size.toLong()

    var seconds = averageNanoTime / Math.pow(10.0, 9.0)
    var hours = 0
    var min = 0
    if (seconds > 60) {
        var i = 0
        while (i < seconds) {
            if (i == 60) {
                min++
                seconds -= 60.0
                i = 0
                if (min == 60) {
                    hours++
                    min -= 60
                }
            }
            i++
        }
    }
    return "$hours:$min:$seconds"
}
