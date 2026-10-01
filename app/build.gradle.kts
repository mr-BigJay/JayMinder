plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.offlinejournal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.offlinejournal"
        minSdk = 26
        targetSdk = 34
        versionCode = 18
        versionName = "1.7.0"

        val arvanBaseUrl = (
            project.findProperty("ARVAN_BASE_URL") as String?
                ?: System.getenv("ARVAN_BASE_URL")
                ?: "https://api.arvancloudai.ir/v1"
            ).trim()
        val arvanModel = (
            project.findProperty("ARVAN_MODEL") as String?
                ?: System.getenv("ARVAN_MODEL")
                ?: ""
            ).trim()
        val arvanBackendCleanupUrl = (
            project.findProperty("ARVAN_BACKEND_CLEANUP_URL") as String?
                ?: System.getenv("ARVAN_BACKEND_CLEANUP_URL")
                ?: ""
            ).trim()

        buildConfigField("String", "ARVAN_BASE_URL", quoteBuildConfig(arvanBaseUrl))
        buildConfigField("String", "ARVAN_MODEL", quoteBuildConfig(arvanModel))
        buildConfigField(
            "String",
            "ARVAN_BACKEND_CLEANUP_URL",
            quoteBuildConfig(arvanBackendCleanupUrl)
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("signing/jayminder-release.jks")
            storePassword = "jayminder-release"
            keyAlias = "jayminder"
            keyPassword = "jayminder-release"
        }
        create("legacyGithub") {
            storeFile = file("signing/github-legacy-debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("legacyUpdate") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release", "debug")
            signingConfig = signingConfigs.getByName("legacyGithub")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("net.java.dev.jna:jna:5.18.1@aar")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

afterEvaluate {
    tasks.named("assembleRelease") {
        doLast {
            renameBuiltApk(
                layout.buildDirectory.dir("outputs/apk/release").get().asFile,
                "JayMinder-v${android.defaultConfig.versionName}.apk"
            )
        }
    }
    tasks.named("assembleLegacyUpdate") {
        doLast {
            renameBuiltApk(
                layout.buildDirectory.dir("outputs/apk/legacyUpdate").get().asFile,
                "JayMinder-v${android.defaultConfig.versionName}-legacy-update.apk"
            )
        }
    }
}

private fun quoteBuildConfig(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun renameBuiltApk(outDir: java.io.File, targetName: String) {
    val built = outDir.listFiles()?.firstOrNull { it.extension == "apk" && !it.name.startsWith("JayMinder") }
        ?: outDir.resolve("app-release.apk")
    val named = outDir.resolve(targetName)
    if (built.exists()) {
        built.copyTo(named, overwrite = true)
        if (built.absolutePath != named.absolutePath) {
            built.delete()
        }
    }
}
