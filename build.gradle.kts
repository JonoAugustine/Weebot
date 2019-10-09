import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.50"
    kotlin("kapt") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

group = "com.ampro"
version = "4.0.0"

application {
    mainClassName = "com.ampro.weebot.LauncherKt"
}

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "http://jcenter.bintray.com")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/serebit/snapshot")
    maven(url = "https://dl.bintray.com/michaelbull/maven")
}

dependencies {
    implementation(kotlin("stdlib"))
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.30")
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    implementation("com.serebit.strife:strife-client-jvm:master-f00b3932")

    //Utils
    implementation("org.slf4j:slf4j-simple:1.6.1")
    implementation("com.michael-bull.kotlin-retry:kotlin-retry:1.0.1")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:4.3.0")
    //implementation("joda-time:joda-time:2.2")
    //implementation("com.google.code.gson:gson:2.8.5")
    //implementation("com.gitlab.JonoAugustine:KPack:a098e9b2")

    //Web
    implementation("io.javalin:javalin:3.5.0") // Javalin
    implementation("io.github.rybalkinsd:kohttp:0.11.0")
    //implementation("com.github.kwebio:kweb-core:0.5.5") // kweb

    //Database
    //implementation("com.github.kwebio:shoebox:0.2.30")
    implementation("com.github.ben-manes.caffeine:caffeine:2.6.2")
    implementation("org.litote.kmongo:kmongo-coroutine:3.10.2")
    //implementation("org.mongodb:mongodb-driver-sync:3.9.1")
    //implementation("org.mongodb:mongodb-driver-async:3.9.1")

    //APIs
    implementation("com.twilio.sdk:twilio:7.9.0")
    implementation("me.sargunvohra.lib:pokekotlin:2.3.0")
    //implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.1.1")
    //implementation("ca.pjer:chatter-bot-api:1.4.7")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
