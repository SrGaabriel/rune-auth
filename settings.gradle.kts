rootProject.name = "rune-auth"

dependencyResolutionManagement {
    versionCatalogs {
        create("plugin") {
            from(files("plugin.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}