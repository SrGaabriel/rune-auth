@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version "1.9.22"
    alias(plugin.plugins.kotlinx.serialization)
    alias(plugin.plugins.shadow)
    alias(plugin.plugins.plugin.yml)
    alias(plugin.plugins.paperweight.userdev)
}

group = "com.runerealms"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${plugin.versions.kotlin.get()}")
    "paperweightDevelopmentBundle"(plugin.purpur.dev.bundle)
    compileOnly(files("libs/core-0.0.1.jar"))
    implementation(files("libs/database-0.0.1.jar"))
    implementation(files("libs/menus-0.0.1.jar"))
    implementation(files("libs/commands-0.0.1.jar"))
    implementation(plugin.bcrypt)
    implementation(plugin.kotlinx.serialization.json)
    compileOnly(plugin.protocol)
    runtimeOnly(plugin.postgresql)
    runtimeOnly(plugin.kotlin.reflection)
}

bukkit {
    name = "RuneAuth"
    version = "1.0-SNAPSHOT"
    main = "com.runerealms.auth.RuneAuth"
    author = "SrGaabriel"
    depend = listOf("RuneCore", "ProtocolLib")
    apiVersion = "1.19"
}

tasks {
    shadowJar {
        exclude {
            // kotlin but not kotlin/reflect
            it.path.startsWith("kotlin/") && !it.path.startsWith("kotlin/reflect/full")
        }
    }
}