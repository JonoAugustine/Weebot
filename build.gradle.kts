import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.0"
    application
    "com.github.johnrengelman.shadow version 1.2.4"
    "com.sedmelluq.jdaction version 1.0.2"
}

group = "com.ampro"
version = "2.0"

repositories {
    mavenCentral()
    jcenter()
    maven ( url = "https://jitpack.io" )
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    implementation("com.github.AquaticMasteryProductions:AMPro-Java-library:master-SNAPSHOT")
    implementation("org.slf4j:slf4j-simple:1.6.1")
    implementation("joda-time:joda-time:2.2")
    implementation("commons-io:commons-io:2.6")
    implementation("com.google.code.gson:gson:2.8.1")

    implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.1.1")
    implementation("com.vdurmont:emoji-java:4.0.0")

    implementation("com.github.DV8FromTheWorld:JDA:v3.8.1")
    implementation("com.jagrosh:jda-utilities:2.1")
    implementation("ca.pjer:chatter-bot-api:1.4.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
