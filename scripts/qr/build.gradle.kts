// QR tool for the printable card (scripts/make-card.ps1): encodes the Drive link into an SVG and decodes
// a QR from a PNG (to check the printed card). ZXing, tooling only: it is not an app dependency.
plugins {
    application
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
}

application {
    mainClass = "QrTool"
}
