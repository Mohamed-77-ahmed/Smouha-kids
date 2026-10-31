import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Release signing reads keystore.properties next to settings.gradle.kts. That file and the
// keystore itself are never committed (see .gitignore and docs/RELEASE.md).
val keystorePropertiesFile = rootProject.layout.projectDirectory.file("keystore.properties")
val keystorePropertiesText = providers.fileContents(keystorePropertiesFile).asText
val keystoreProperties = Properties().apply {
    if (keystorePropertiesText.isPresent) load(StringReader(keystorePropertiesText.get()))
}

android {
    namespace = "com.example.tracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Permanent after the first real release (docs/DECISIONS.md D31). The Kotlin package and
        // the namespace stay com.example.tracker; only the id Android and the phone see changed.
        applicationId = "com.smouha.kidsreminder"
        minSdk = 28
        targetSdk = 37
        // Every APK shared through Drive needs a higher versionCode than the one installed.
        // 1 / 1.0.0: the first release signed with the real key (batch 4, docs/DECISIONS.md D34).
        // 4 / 1.1.0: Koko Dawa (batch 6), above every build in docs/RELEASE.md (release 1, the update
        // test's 1.0.1 = 2, the debug pilot 1.2.0 = 3). scripts/update-test.ps1 builds the next version
        // with the same key through -PupdateTestVersionCode / -PupdateTestVersionName; a real release
        // never passes them.
        versionCode = providers.gradleProperty("updateTestVersionCode").orNull?.toInt() ?: 4
        versionName = providers.gradleProperty("updateTestVersionName").orNull ?: "1.1.0"

        // Runs the app with an in-memory DB during instrumented tests (see androidTest TestRunner).
        testInstrumentationRunner = "com.example.tracker.TestRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesText.isPresent) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    sourceSets {
        // Exported Room schemas, needed by MigrationTestHelper in instrumented tests.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
    lint {
        error += listOf("MissingTranslation", "HardcodedText", "RtlHardcoded", "RtlSymmetry")
        abortOnError = true
    }
    // The language is switched inside the app, so an app bundle must keep every language.
    bundle {
        language {
            enableSplit = false
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// A release build without keystore.properties must stop with a clear message instead of
// failing later or, worse, being signed with something else.
val checkReleaseKeystore by tasks.registering {
    val present = keystorePropertiesText.isPresent
    val path = keystorePropertiesFile.asFile.path
    doLast {
        if (!present) {
            throw GradleException(
                "Release signing is not set up: $path was not found. " +
                    "Create the keystore and keystore.properties as described in docs/RELEASE.md."
            )
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseKeystore) }

// Some unit tests read the resource files themselves (ContrastTest, MascotTest, IconContrastTest,
// BrandTextTest). Without this input a change to a color or a string alone left the test task
// "up to date", and it reported the old result.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/res").withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("appResources")
}

// Instrumented tests and uninstall tasks can wipe the app's data on a device. Automated runs go
// through scripts/run-tests.cmd, which only allows the test emulator (CLAUDE.md 0b). Runs started
// from Android Studio are not affected.
val startedFromIde = providers.gradleProperty("android.injected.invoked.from.ide").isPresent
val startedByTestRunner = providers.environmentVariable("PILLPALS_TEST_RUNNER").isPresent
tasks.matching { it.name.startsWith("connected") || it.name.startsWith("uninstall") }.configureEach {
    // Plain local copies: the configuration cache cannot store references to the build script.
    val fromIde = startedFromIde
    val fromTestRunner = startedByTestRunner
    doFirst {
        if (!fromIde && !fromTestRunner) {
            throw GradleException(
                "This task can uninstall the app and delete its data. Run it through scripts/run-tests.cmd " +
                    "with ANDROID_SERIAL set to the test emulator Pixel_API_34_test."
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}
