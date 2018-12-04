import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.10"
    application
    "com.github.johnrengelman.shadow version 1.2.4"
    "com.sedmelluq.jdaction version 1.0.2"
}

group = "com.ampro"
version = "2.0"

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
    implementation("com.google.code.gson:gson:2.8.1")
    implementation("com.github.jkcclemens:khttp:-SNAPSHOT")

    //JDA
    implementation("net.dv8tion:JDA:3.8.1_437")
    implementation("com.github.JDA-Applications:JDA-Utilities:fcaf53dfb7")
    implementation("ca.pjer:chatter-bot-api:1.4.7")

    //Tools & APIs
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.1.1")
    implementation("me.sargunvohra.lib:pokekotlin:2.3.0")
    implementation("net.dean.jraw:JRAW:1.1.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
