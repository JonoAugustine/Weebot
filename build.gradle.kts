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
version = "2.2.1"

application {
    mainClassName = "com.ampro.weebot.LauncherKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven ( url = "https://jitpack.io" )
    maven ( url = "http://jcenter.bintray.com" )
    maven ( url =  "https://oss.jfrog.org/artifactory/libs-release" )
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
    implementation("com.github.ben-manes.caffeine:caffeine:2.6.2")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:4.3.0")
        //Web
    implementation("com.github.kittinunf.fuel:fuel:1.16.0")
    implementation("com.github.kittinunf.fuel:fuel-gson:1.16.0")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:1.16.0")
        //Database
    implementation("org.litote.kmongo:kmongo-id:3.9.2")
    implementation("org.litote.kmongo:kmongo-async:3.9.2")
    implementation("org.litote.kmongo:kmongo-id:3.9.2")
    implementation("org.mongodb:mongodb-driver-sync:3.9.1")
    implementation("org.mongodb:mongodb-driver-async:3.9.1")
    //implementation("org.jetbrains.exposed:exposed:0.12.1")
    //implementation("mysql:mysql-connector-java:8.0.14")
    //implementation("mysql:mysql-connector-java:8.0.13")
    //implementation("io.requery:requery:1.5.1")
    //implementation("io.requery:requery-kotlin:1.5.1")
    //kapt("io.requery:requery-processor:1.5.1")
   // implementation("io.reactivex.rxjava2:rxjava:2.2.4")

    //JDA
    implementation("net.dv8tion:JDA:3.8.1_437")
    implementation("com.github.JDA-Applications:JDA-Utilities:-SNAPSHOT")
        //Bot List Sites
    implementation("org.discordbots:DBL-Java-Library:2.0.1")

    //APIs
    implementation("me.sargunvohra.lib:pokekotlin:2.3.0")
    implementation("com.twilio.sdk:twilio:7.9.0")
    implementation("com.github.twitch4j:twitch4j:1.0.0-alpha.5")
    implementation("org.twitter4j:twitter4j-stream:4.0.7")
    //implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.1.1")
    //implementation("net.dean.jraw:JRAW:1.1.0")
    //implementation("ca.pjer:chatter-bot-api:1.4.7")
    //implementation("org.springframework.boot:spring-boot-gradle-plugin:1.4.3.RELEASE")
    //implementation("org.springframework.boot:spring-boot-starter-web:1.4.3.RELEASE")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
