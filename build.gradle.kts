import org.gradle.api.Project.DEFAULT_VERSION
import org.gradle.api.internal.artifacts.transform.UnzipTransform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("kapt") version "1.7.0"
    id("com.vanniktech.maven.publish") version "0.25.1"
}

group = "io.johnsonlee"
version = project.findProperty("version")?.takeIf { it != DEFAULT_VERSION } ?: "1.0.0-SNAPSHOT"

val artifactType = Attribute.of("artifactType", String::class.java)

configurations {
    create("unzip") {
        attributes.attribute(artifactType, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

dependencies {
    registerTransform(UnzipTransform::class.java) {
        from.attribute(artifactType, ArtifactTypeDefinition.JAR_TYPE)
        to.attribute(artifactType, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    api(libs.android.tools.layoutlib)
    api(libs.layoutlib.native.jdk11)
    implementation(libs.booster.build)
    implementation(libs.kxml2)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.5"
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xskip-metadata-version-check")
        jvmTarget = "1.8"
    }
}

fun DependencyHandler.unzip(dependency: Any) {
    add("unzip", dependency)
}