plugins {
    `java-library`
}

// Bootstrap API has no dependencies - it must be minimal
// and loadable on the bootstrap classpath
dependencies {
    // No dependencies - this is critical for bootstrap loading
}

// Configure Java compatibility
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
