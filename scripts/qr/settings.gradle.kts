// A separate small build for the QR tool of scripts/make-card.ps1 (tooling only, never part of the app;
// docs/DECISIONS.md D35). Run with the project's wrapper: gradlew.bat -p scripts\qr installDist
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "qrtool"
