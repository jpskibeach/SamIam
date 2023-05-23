import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    id("java")
}

group = parent!!.group
version = parent!!.version

repositories {
    mavenCentral()
}

dependencies {
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

tasks.javadoc  {
    options {
        this as StandardJavadocDocletOptions
        tags(
            "from",
            "changed",
            "decision",
            "precondition",
            "postcondition",
            "pq",
            "param-missing", //parameters in the javadoc that aren't present in the code
        )
        addBooleanOption("Xdoclint:none", true)
        addStringOption("Xmaxwarns", "1")
    }
}