import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow")
}
application { mainClass.set("edu.ucla.belief.ui.UI") }
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
    isZip64 = true
}
group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":inflib-lib"))
    implementation(project(":inflib-core"))
    implementation(project(":samiam-core"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(PASSED, FAILED, SKIPPED)
        showStandardStreams = true
    }
}