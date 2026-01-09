plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "1.7.7" // Optional: for NMS access
}

group = "com.example"
version = "1.0.0"
description = "My Paper Plugin"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

dependencies {
    // Paper API
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    
    // PlaceholderAPI (optional)
    compileOnly("me.clip:placeholderapi:2.11.5")
    
    // Database dependencies (optional)
    implementation("com.zaxxer:HikariCP:5.1.0") // MySQL connection pooling
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.0") // MySQL driver
    // implementation("org.mongodb:mongodb-driver-sync:4.11.1") // MongoDB
    
    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    
    shadowJar {
        // Relocate dependencies to avoid conflicts
        relocate("com.zaxxer.hikari", "com.example.myplugin.libs.hikari")
        relocate("org.mariadb", "com.example.myplugin.libs.mariadb")
        
        archiveClassifier.set("")
        archiveFileName.set("${project.name}-${project.version}.jar")
    }
    
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    
    javadoc {
        options.encoding = "UTF-8"
    }
    
    processResources {
        filteringCharset = "UTF-8"
        
        val props = mapOf(
            "version" to project.version,
            "description" to project.description
        )
        
        inputs.properties(props)
        
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
    
    test {
        useJUnitPlatform()
    }
}
