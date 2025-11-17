plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.8"
}

group = "com.vaultpack"
version = "1.0.0"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        name = "codemc-creatorfromhell"
        url = uri("https://repo.codemc.io/repository/creatorfromhell/")
    }
}

dependencies {
    // Paper API - Updated to 1.21.8
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // VaultUnlocked API - Modern fork of Vault with active maintenance
    // Compatible with both Vault and VaultUnlocked plugins
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.5")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set("VaultPack")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    // Don't shade dependencies - they're all provided
    dependencies {
        exclude(dependency("io.papermc.paper:paper-api"))
        exclude(dependency("net.milkbowl.vault:VaultUnlockedAPI"))
        exclude(dependency("me.clip:placeholderapi"))
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version
        )
    }
}

// Make sure shadowJar is the default build task
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Default task
defaultTasks("clean", "shadowJar")
