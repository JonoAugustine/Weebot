import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.11"
    kotlin("kapt") version "1.3.11"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    //id("com.sedmelluq.jdaction") version "1.0.2" Incompat w/ Gradle 4.10
    //id("org.springframework.boot")
}

group = "com.ampro"
version = "2.1"

application {
    mainClassName = "com.ampro.weebot.main.LauncherKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven ( url = "https://jitpack.io" )
    maven ( url = "http://jcenter.bintray.com" )
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.0")
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    //Utils
    implementation("com.github.AquaticMasteryProductions:AMPro-Java-library:master-SNAPSHOT")
    implementation("org.slf4j:slf4j-simple:1.6.1")
    implementation("joda-time:joda-time:2.2")
    implementation("commons-io:commons-io:2.6")
    implementation("com.google.code.gson:gson:2.8.5")
        //Web
    implementation("com.github.kittinunf.fuel:fuel:1.16.0")
    implementation("com.github.kittinunf.fuel:fuel-gson:1.16.0")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:1.16.0")
        //Database
    //implementation("org.jetbrains.exposed:exposed:0.11.2")
    //implementation("mysql:mysql-connector-java:8.0.13")
    //implementation("io.requery:requery:1.5.1")
    //implementation("io.requery:requery-kotlin:1.5.1")
    //kapt("io.requery:requery-processor:1.5.1")
   // implementation("io.reactivex.rxjava2:rxjava:2.2.4")

    //JDA
    implementation("net.dv8tion:JDA:3.8.1_437")
    implementation("com.github.JDA-Applications:JDA-Utilities:-SNAPSHOT")
    implementation("org.discordbots:DBL-Java-Library:2.0.1")

    //Tools & APIs
    //implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.1.1")
    implementation("me.sargunvohra.lib:pokekotlin:2.3.0")
    //implementation("net.dean.jraw:JRAW:1.1.0")
    //implementation("ca.pjer:chatter-bot-api:1.4.7")
    //implementation("org.springframework.boot:spring-boot-gradle-plugin:1.4.3.RELEASE")
    //implementation("org.springframework.boot:spring-boot-starter-web:1.4.3.RELEASE")
    implementation("com.twilio.sdk:twilio:7.9.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
