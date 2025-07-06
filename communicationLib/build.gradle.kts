plugins {
    id("java-library")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jupiter.junit.jupiter)
    implementation(libs.okhttp.core)
    implementation(libs.jackson.databind)
    implementation(libs.bouncyCastle.prov)
}
