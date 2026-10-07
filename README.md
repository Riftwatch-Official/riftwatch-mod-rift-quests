# Riftwatch Quests

Custom NeoForge mod `rift_quests` of the Riftwatch Minecraft server (Minecraft 1.21.1, NeoForge 21.1.255, Create 6.0.10). Public repository `Riftwatch-Official/riftwatch-mod-rift-quests`; all rights reserved, see `LICENSE` for the permitted use.

The mod is the quest book of the Riftwatch modpack: the engine (data model, loader, validator, tasks, rewards, progress, screens and the Riftwatch Ticket) lives here, the quest content is a data pack built from the modpack repository.

The development guide for all Riftwatch mods (environment, Test and Deploy mode, deploy flow, rollback) is `custom-mods/README.md` in the repository `Riftwatch-Official/riftwatch-server`. New mods are created with `scripts/new-mod.sh` there, never by copying a mod by hand.

## Build and Run

| Command | Purpose |
|---|---|
| `./gradlew build` | builds `build/libs/rift_quests-<version>.jar` |
| `./gradlew runServer` | dev server in `run/` (once: `eula=true` in `run/eula.txt`, `online-mode=false` in `run/server.properties`) |
| `./gradlew runClient` | dev client, join `localhost` |
| `./gradlew runData` | data generators into `src/generated/resources/` |
| `./gradlew runGameTestServer` | game tests of the mod |

JDK 21 is selected by the Gradle toolchain. Create, Ponder, Flywheel and Registrate are on the dev classpath; a mod that uses Create also declares it as `required` in `src/main/templates/META-INF/neoforge.mods.toml`.

## Layout

| Path | Content |
|---|---|
| `gradle.properties` | versions (Minecraft, NeoForge, Parchment, Create and its libraries) and mod metadata (`mod_id`, `mod_name`, `mod_version`) |
| `build.gradle` | ModDevGradle setup, repositories, dependencies, run configurations |
| `src/main/java/net/riftwatch/rift_quests/` | mod sources, entry point `RiftQuestsMod` |
| `src/main/templates/META-INF/neoforge.mods.toml` | mod metadata, expanded from `gradle.properties` |
| `.github/workflows/deploy.yml` | build on every push; with `DEPLOY_ENABLED=true` deploy to the server and GitHub Release `v<version>` |

## Deploy

Every push to `main` builds the mod. When the repository variable `DEPLOY_ENABLED` is `true`, the workflow refuses a version that was released before, uploads the jar to the Riftwatch host, runs `riftwatch-mod-deploy` (backup, restart with countdown, automatic rollback) and publishes the GitHub Release `v<version>` with the jar. A push therefore changes the live server: work happens in Test mode until the explicit deploy GO.
