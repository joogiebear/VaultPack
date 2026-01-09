plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "com.vaultpack"
version = "2.0.0"

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

    // Auxilor ecosystem (eco suite)
    maven {
        name = "auxilor"
        url = uri("https://repo.auxilor.io/repository/maven-public/")
    }
}

dependencies {
    // Paper API - Updated to 1.21.8
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // Vault API (exclude transitive Bukkit dependency to avoid conflicts)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.5")

    // Auxilor eco suite integration
    // eco - Core library for Spigot development
    compileOnly("com.willfp:eco:6.74.1")

    // EcoItems - Custom items support (already in plugin.yml softdepend)
    compileOnly("com.willfp:EcoItems:5.59.2")

    // Phase 2: Database support
    // HikariCP - High-performance JDBC connection pool
    implementation("com.zaxxer:HikariCP:5.1.0")

    // MySQL Connector - JDBC driver for MySQL
    implementation("com.mysql:mysql-connector-j:8.3.0")

    // Phase 3: Adventure API (modern text components)
    // Paper includes Adventure, but we add MiniMessage for advanced formatting
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
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

    // Phase 2: Relocate HikariCP and MySQL to avoid conflicts
    relocate("com.zaxxer.hikari", "com.vaultpack.libs.hikari")
    relocate("com.mysql", "com.vaultpack.libs.mysql")

    // Phase 3: Relocate MiniMessage to avoid conflicts
    relocate("net.kyori.adventure.text.minimessage", "com.vaultpack.libs.minimessage")

    // Exclude provided dependencies
    dependencies {
        exclude(dependency("io.papermc.paper:paper-api"))
        exclude(dependency("com.github.MilkBowl:VaultAPI"))
        exclude(dependency("me.clip:placeholderapi"))
        exclude(dependency("com.willfp:eco"))
        exclude(dependency("com.willfp:EcoItems"))
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
