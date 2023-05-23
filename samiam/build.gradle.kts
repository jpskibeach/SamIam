import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

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

tasks.create("jpackageAppImage", JPackageTask::class) {
    group = "distribution"
    dependsOn("shadowJar")
    input = "build/libs"
    destination = "$buildDir/dist"
    appName = "samiam-$version"
    vendor = "ucla"
    mainJar = "${project.name}-${project.version}-all.jar"
    mainClass = main
    javaOptions = listOf("-Dfile.encoding=UTF-8")
    linux {
        appName = "samiam-$version.AppImage"
        type = ImageType.APP_IMAGE
    }
}

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
        appName = "samiam-$version.exe"
    }
    linux {
        appName = "samiam-$version.AppImage"
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