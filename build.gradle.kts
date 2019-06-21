import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.31"
    kotlin("kapt") version "1.3.31"
    id("kotlinx-serialization") version "1.3.30"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    //id("com.sedmelluq.jdaction") version "1.0.2" Incompat w/ Gradle 4.10
    //id("org.springframework.boot")
}

group = "com.ampro"
version = "2.3.0"

application {
    mainClassName = "com.ampro.weebot.LauncherKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
    maven(url = "http://jcenter.bintray.com")
    maven(url = "https://oss.jfrog.org/artifactory/libs-release")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.30")
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    //Utils
    //implementation("com.gitlab.JonoAugustine:KPack:a098e9b2")
    implementation("org.slf4j:slf4j-simple:1.6.1")
    implementation("joda-time:joda-time:2.2")
    implementation("commons-io:commons-io:2.6")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.github.ben-manes.caffeine:caffeine:2.6.2")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:4.3.0")
    //Web
    implementation("com.github.kittinunf.fuel:fuel:1.16.0")
    implementation("com.github.kittinunf.fuel:fuel-gson:1.16.0")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:1.16.0")
    implementation("io.javalin:javalin:3.0.0") // Javalin
    //Database
    implementation("org.litote.kmongo:kmongo-coroutine:3.10.2")
    //implementation("org.mongodb:mongodb-driver-sync:3.9.1")
    //implementation("org.mongodb:mongodb-driver-async:3.9.1")


    //JDA
    implementation("net.dv8tion:JDA:3.8.3_463")
    implementation("com.github.JDA-Applications:JDA-Utilities:-SNAPSHOT")
    //Bot List Sites
    implementation("org.discordbots:DBL-Java-Library:2.0.1")

    //APIs
    implementation("me.sargunvohra.lib:pokekotlin:2.3.0")
    implementation("com.twilio.sdk:twilio:7.9.0")
    implementation("com.github.twitch4j:twitch4j:1.0.0-alpha.13")
    implementation("org.twitter4j:twitter4j-stream:4.0.7")
    //implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.1.1")
    //implementation("ca.pjer:chatter-bot-api:1.4.7")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
