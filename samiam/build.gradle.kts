import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
val shadowJarName = "${project.name}-${version}-all.jar"

println("${parent!!.version} parent!!.version")
println("${version} version")

task("packageNetworkSamples", Copy::class) {
    group = "distribution"
    from("${rootDir}/network_samples").into("$buildDir/package/network_samples")
}
task("packageShadowJar", Copy::class) {
    group = "distribution"
    dependsOn("shadowJar")
    from("${buildDir}/libs/$shadowJarName").into("$buildDir/package")
}

tasks.jpackage {
    dependsOn("packageNetworkSamples", "packageShadowJar")
    group = "distribution"

    input = "build/package"
    destination = "$buildDir/dist"

    appName = project.name
    vendor = "ucla"

    mainJar = shadowJarName
    mainClass = main

    javaOptions = listOf("-Dfile.encoding=UTF-8")

    windows {
        type = ImageType.MSI
        winMenu = true
        winDirChooser = true
        appName = project.name
        winShortcutPrompt = true
    }
    linux {
        type = ImageType.DEFAULT
    }
    mac {
        type = ImageType.DMG
    }
}

tasks.create("jpackageAppImage", JPackageTask::class) {
    group = "distribution"
    dependsOn("shadowJar")
    input = "build/libs"
    destination = "$buildDir/dist"
    appName = "samiam-$version"
    vendor = "ucla"
    mainJar = shadowJarName
    mainClass = main
    javaOptions = listOf("-Dfile.encoding=UTF-8")
    linux {
        appName = "${project.name}-$version.AppImage"
        type = ImageType.APP_IMAGE
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