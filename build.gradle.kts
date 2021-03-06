/*
 * Copyright 2019 Google LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Project Kotlin version.
 */
val kotlinVersion: String = "1.4.10"

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version("1.4.10")
    kotlin("plugin.serialization").version("1.4.10")
    distribution
    id("maven-publish")
    id("com.github.jk1.dependency-license-report").version("1.14")
    id("org.jetbrains.dokka").version("0.10.1")
}

repositories {
    jcenter()
}

subprojects {
    val versionBase = rootProject.property("prefab.version") as String
    require(versionBase.matches("""^\d+\.\d+\.\d+$""".toRegex())) {
        "prefab.version is not in major.minor.path format"
    }
    val qualifier = rootProject.property("prefab.qualifier") as String
    require(qualifier.matches("""^(-(alpha|beta|rc)\d+)?$""".toRegex())) {
        "prefab.qualifier did not match the expected format"
    }
    group = "com.google.prefab"
    version = versionBase + if (!rootProject.hasProperty("prefab.release")) {
        "-SNAPSHOT"
    } else {
        qualifier
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "maven-publish")

    repositories {
        jcenter()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))

        // Use JUnit 5.
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    }

    publishing {
        repositories {
            maven {
                url = uri("${rootProject.buildDir}/repository")
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
    }
}

tasks {
    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
        subProjects = subprojects.map { it.name }
        configuration {
            reportUndocumented = true
        }
    }
}

licenseReport {
    allowedLicensesFile = projectDir.resolve("config/allowed_licenses.json")
    excludes = listOf(
        // This isn't a real dependency, it's only used to inject the real
        // dependency based on the platform. The real dependency
        // (kotlinx-serialization-json-jvm) passes the license check, but the
        // no-op dependency has no license information.
        "org.jetbrains.kotlinx:kotlinx-serialization-json"
    ).toTypedArray()
}

tasks.named("check") {
    dependsOn(":checkLicense")
}

distributions {
    create("repository") {
        contents {
            from(buildDir.resolve("repository"))
        }
    }
}

tasks.named("distTar") {
    dependsOn(":repositoryDistTar")
}

tasks.named("distZip") {
    dependsOn(":repositoryDistZip")
}

tasks.named("repositoryDistTar") {
    subprojects.map { dependsOn(":${it.name}:publish") }
}

tasks.named("repositoryDistZip") {
    subprojects.map { dependsOn(":${it.name}:publish") }
}

tasks.register("release") {
    dependsOn(":build")
    dependsOn(":dokka")
}
