plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.recepiesuggestor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.recepiesuggestor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    packagingOptions {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE")
        }
    }
}


dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")



    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("androidx.camera:camera-extensions:1.3.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

//    implementation 'com.google.android.material:material:1.12.0'

    val camerax_version = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version") // PreviewView

    // Recycleriew
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // google ml kit
    // implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mlkit:genai-image-description:1.0.0-beta1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    //tflite
    implementation("org.tensorflow:tensorflow-lite:2.15.0") // Use latest stable version
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") // Utilities for image/tensor handling

    // POS tagging - use a Java 8 / Android friendly OpenNLP release
    // Newer 2.x releases target newer Java versions and pull in dependencies
    // that can break D8/desugaring on Android. Downgrade to 1.9.4 which
    // is known to work well on Android or shade the library.
    implementation("org.apache.opennlp:opennlp-tools:1.9.4")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4") // Or the latest version

}