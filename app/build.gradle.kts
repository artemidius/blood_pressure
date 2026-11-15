import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.artemidius.bloodpressure"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.artemidius.bloodpressure"
        minSdk = 29
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

val scannerAssets = layout.projectDirectory.dir("src/main/assets/seven-digits")
val verifyScannerModel by tasks.registering {
    inputs.dir(scannerAssets)
    doLast {
        val directory = scannerAssets.asFile
        for (name in listOf("detector.tflite", "detector.json", "MODEL_LICENSE")) {
            check(directory.resolve(name).isFile) {
                "Missing scanner asset $name. Run python3 scripts/prepare_scanner_model.py /path/to/export; see docs/SCANNER.md."
            }
        }
        val expected = Regex("\"model_sha256\"\\s*:\\s*\"([a-f0-9]{64})\"")
            .find(directory.resolve("detector.json").readText())?.groupValues?.get(1)
        val actual = MessageDigest.getInstance("SHA-256")
            .digest(directory.resolve("detector.tflite").readBytes())
            .joinToString("") { "%02x".format(it) }
        check(expected == actual) { "Scanner model checksum mismatch; prepare matching model assets again." }
    }
}
tasks.named("preBuild") { dependsOn(verifyScannerModel) }

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.connect.client)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.compose.material.icons)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ui.auth)
    implementation(libs.hilt.android)
    implementation(libs.seven.segment.scanner)
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    ksp(libs.hilt.android.compiler)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
