plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "n1ql.query.generator"
version = "1.1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf())
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("") // No upper limit - compatible with all future versions
    }

    buildSearchableOptions {
        enabled = false
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    
    // Ensure resources are properly included in JAR
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
