plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "com.vaultpack"
version = "3.0.0"

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

    // Phase 1: ACF (Aikar's Command Framework)
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
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

    // Phase 1: ACF - Modern command framework
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")

    // Phase 1: Lombok - Reduce boilerplate code
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Phase 1: bStats - Plugin metrics
    implementation("org.bstats:bstats-bukkit:3.0.2")
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

    // Phase 1: Relocate ACF and bStats to avoid conflicts
    relocate("co.aikar.commands", "com.vaultpack.libs.acf")
    relocate("co.aikar.locales", "com.vaultpack.libs.locales")
    relocate("org.bstats", "com.vaultpack.libs.bstats")

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

// Run-Paper configuration for testing
tasks.runServer {
    minecraftVersion("1.21.4")

    // Download soft dependencies for testing
    downloadPlugins {
        // Vault (dependency)
        github("MilkBowl", "Vault", "1.7.3", "Vault.jar")

        // PlaceholderAPI (dependency)
        hangar("PlaceholderAPI", "2.11.6")

        // LuckPerms (for testing permissions)
        modrinth("luckperms", "5.4.139")
    }

    // Use latest Paper build
    runDirectory.set(file("run"))
}
