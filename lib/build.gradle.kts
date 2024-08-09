plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

// https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.logging.TestLogging.html
tasks.named<Test>("test") {
     testLogging {
       outputs.upToDateWhen {false}
       showStandardStreams = true
    }

    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
