// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.4" apply false
}

buildscript {
    repositories {
        google()      // ✅ Required here too
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.0") // if you use Firebase/ML Kit
    }
}