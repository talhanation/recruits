plugins {
    // Keep in sync with moddev_version in gradle.properties.
    id("net.neoforged.moddev") version "2.0.74"
}

val modId = project.property("mod_id") as String
group = project.property("maven_group") as String
version = project.property("mod_version") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

neoForge {
    version = project.property("neoforge_version") as String
    parchment {
        mappingsVersion = project.property("parchment_version") as String
        minecraftVersion = project.property("minecraft_version") as String
    }

    // The access transformer still needs to be migrated from SRG to Mojang names,
    // so don't fail the build on validation yet.
    validateAccessTransformers = false

    runs {
        create("client") {
            client()
            gameDirectory = file("run/client")
        }
        create("server") {
            server()
            gameDirectory = file("run/server")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.maxhenkel.de/repository/public")
    exclusiveContent {
        forRepository {
            maven {
                name = "CurseForge"
                url = uri("https://cursemaven.com")
            }
        }
        filter { includeGroup("curse.maven") }
    }
}

dependencies {
    implementation("de.maxhenkel.corelib:corelib:${project.property("corelib_version")}")

    // Bundle corelib INSIDE the Recruits jar (NeoForge Jar-in-Jar) so no separate
    // corelib download is needed. It stays a real nested mod (modId "corelib"), which
    // satisfies the REQUIRED dependency; NeoForge deduplicates if a standalone copy exists.
    jarJar("de.maxhenkel.corelib:corelib") {
        version {
            strictly("[${project.property("corelib_version")},)")
            prefer(project.property("corelib_version") as String)
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val properties = mapOf(
        "mod_id" to modId,
        "mod_version" to (project.property("mod_version") as String),
        "mod_name" to (project.property("mod_name") as String),
        "mod_authors" to (project.property("mod_authors") as String),
        "mod_license" to (project.property("mod_license") as String),
        "mod_description" to (project.property("mod_description") as String),
        "minecraft_version_range" to (project.property("minecraft_version_range") as String),
        "loader_version_range" to (project.property("neoforge_version_range") as String)
    )
    filteringCharset = "UTF-8"
    inputs.properties(properties)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(properties) }
}
