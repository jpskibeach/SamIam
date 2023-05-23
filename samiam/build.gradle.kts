import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.panteleyev.jpackage.ImageType

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow")
    id("org.panteleyev.jpackageplugin") version "1.5.2"

}
val main = "edu.ucla.belief.ui.UI"
application { mainClass.set(main) }
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
    isZip64 = true
}
group = parent!!.group
version = parent!!.version
tasks.jpackage {
    dependsOn("shadowJar")

    input = "build/libs"
    destination = "$buildDir/dist"

    appName = "samiam-$version"
    vendor = "ucla"

    mainJar = "${project.name}-${project.version}-all.jar"
    mainClass = main

    javaOptions = listOf("-Dfile.encoding=UTF-8")

    windows {
        winConsole = true
    }
    linux {
        appName = "samiam-$version.AppImage"
        type = ImageType.APP_IMAGE
        winConsole = true
    }
}
repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":inflib"))
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