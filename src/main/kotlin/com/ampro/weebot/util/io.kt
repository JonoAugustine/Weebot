package com.ampro.weebot.util

import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.main.MLOG
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.apache.commons.io.FileUtils
import java.io.*
import java.nio.file.FileAlreadyExistsException


val DIR_HOME = File("wbot")

val DIR_LOGS = File(DIR_HOME, "logs")

val DIR_TEMP = File(DIR_HOME, "temp")
val TEMP_OUT = File(DIR_TEMP, "out")
val TEMP_IN  = File(DIR_TEMP, "in")

val DIR_DAO = File(DIR_HOME, "dao")
val DAO_SAVE = File(DIR_DAO, "database.wbot")
val DAO_BKUP = File(DIR_DAO, "dbsBK.wbot")
val DAO_BKBK = File(DIR_TEMP, "dbstemp.wbot")

val GSON = GsonBuilder().enableComplexMapKeySerialization()
    .setExclusionStrategies().setPrettyPrinting()
    .registerTypeAdapter(IPassive::class.java, InterfaceAdapter<Any>())
    .registerTypeAdapter(Class::class.java,CommandClassAdapter())
    .create()


fun BufferedWriter.writeLn(line: Any) {
    this.write(line.toString())
    this.newLine()
}

/**
 * Print the List to file, each index its own line.
 *
 * @param name The name of the file.
 * @return The [File] made or `null` if unable to create.
 * @throws FileAlreadyExistsException
 */
@Throws(FileAlreadyExistsException::class)
fun List<Any>.toFile(name: String = "file") : File {
    val file = File(DIR_HOME, name)
    //Leave if the file already exists
    if (file.exists()) {
        throw FileAlreadyExistsException("File '$name' already exists.")
    } else {
        file.createNewFile()
        val bw = file.bufferedWriter()
        this.forEach { bw.writeLn(it) }
        bw.close()
    }
    return file
}

/**
 * Save an object to a json file
 *
 * @param file The file to save to
 * @return 0 if the file was saved.
 *        -1 if an IO error occurred.
 *        -2 if a Gson exception was thrown
 */
@Synchronized
fun Any.saveJson(file: File): Int {
    try {
        val fw = FileWriter(file)
        GSON.toJson(this, fw)
        fw.close()
    } catch (e: FileNotFoundException) {
        System.err.println("File not found while writing gson to file.")
        return -1
    } catch (e: IOException) {
        System.err.println("IOException while writing gson to file.")
        return -1
    }
    return 0
}

/**
 * Load the given file form JSON.
 *
 * @param file The file to loadDao from
 * @return The parsed object or null if it was not found or an exception was thrown.
 */
@Synchronized
inline fun <reified T> loadJson(file: File): T? {
    try {
        FileReader(file).use { reader -> return GSON.fromJson(reader, T::class.java) }
    } catch (e: FileNotFoundException) {
        System.err.println("Unable to locate file '${file.name}'")
        return null
    } catch (e: IOException) {
        System.err.println("IOException while reading json '${file.name}'.")
        return null
    }
}

/**
 * @return `false` if gson fails to read the file.
 */
@Synchronized
internal fun corruptJsonTest(file: File, objcClass: Class<*>): Boolean {
    try {
        FileReader(file).use { reader -> GSON.fromJson(reader, objcClass) }
    } catch (e: JsonSyntaxException) {
        e.printStackTrace()
        return false
    } catch (e: IOException) {
        return true
    }
    return true
}

/**
* Build the all file directories used by Weebot.
* @return `false` if any directory is not created (and did not already exist)
*/
fun buildDirs(): Boolean {
    if (!DIR_DAO.exists()) DIR_DAO.mkdirs()
    if (!DIR_LOGS.exists()) DIR_LOGS.mkdirs()
    if (!TEMP_OUT.exists()) TEMP_OUT.mkdirs()
    if (!TEMP_IN.exists()) TEMP_IN.mkdirs()
    if (!DIR_LOGS.exists()) DIR_LOGS.mkdirs()
    return DIR_HOME.exists() && TEMP_OUT.exists() && TEMP_IN.exists() && DIR_DAO
        .exists() && DIR_LOGS.exists()
}

/** Clears the temp folders.  */
@Synchronized
fun clearTempDirs() {
    try {
        FileUtils.cleanDirectory(DIR_TEMP)
    } catch (e: IOException) {
        MLOG.elog("Failed clear temp dir.")
        e.printStackTrace()
    }

}
