package io.github.arcaneplugins.levelledmobs.misc

import java.io.File
import java.io.FileInputStream
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeStandardCodes
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.util.FileUtil
import org.yaml.snakeyaml.Yaml

/**
 * Used to load various configuration files and migrate if necessary
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
object FileLoader {
    const val SETTINGS_FILE_VERSION: Int = 35 // Last changed: v3.12.0 b770
    const val MESSAGES_FILE_VERSION: Int = 8 // Last changed: v3.4.0 b621
    const val CUSTOMDROPS_FILE_VERSION: Int = 10 // Last changed: v3.1.0 b474
    const val RULES_FILE_VERSION: Int = 4 // Last changed: v3.13.0 b789

    fun loadFile(
        plugin: Plugin,
        cfgName: String,
        compatibleVersion: Int
    ): YamlConfiguration? {
        var cfgName = cfgName
        cfgName += ".yml"

        Utils.logger.info("&fFile Loader: &7Loading file '&b$cfgName&7'...")

        val file = File(plugin.dataFolder, cfgName)

        saveResourceIfNotExists(plugin, file)
        try {
            FileInputStream(file).use { fs ->
                Yaml().load<Any>(fs)
            }
        } catch (e: Exception) {
            val parseErrorMessage =
                """
                            LevelledMobs was unable to read file &b%s&r due to a user-caused YAML syntax error.
                            Copy the contents of your file into a YAML Parser website, such as < https://tinyurl.com/yamlp >  to help locate the line of the mistake.
                            Failure to resolve this issue will cause LevelledMobs to function improperly, or likely not at all.
                            Below represents where LevelledMobs became confused while attempting to read your file:
                            &b---- START ERROR ----&r
                            &4%s&r
                            &b---- END ERROR ----&r
                            If an attempt to solve this error has come to no avail, you are welcome to ask for assistance in the ArcanePlugins Discord Guild.
                            &bhttps://discord.io/arcaneplugins
                            """.trimIndent()

            Utils.logger.error(String.format(parseErrorMessage, cfgName, e))
            return null
        }

        var cfg = YamlConfiguration.loadConfiguration(file)
        cfg.options().copyDefaults(true)
        val ymlHelper = YmlParsingHelper(cfg)
        val fileVersion: Int = ymlHelper.getInt( "file-version")
        val isCustomDrops = cfgName == "customdrops.yml"
        val isRules = cfgName == "rules.yml"

        // not migrating rules version 2 or newer
        if ((!isRules || fileVersion < 2) && fileVersion < compatibleVersion) {
            val backedupFile = File(
                plugin.dataFolder,
                "$cfgName.v$fileVersion.old"
            )

            // copy to old file
            FileUtil.copy(file, backedupFile)
            Utils.logger.info(
                "&fFile Loader: &8(Migration) &b" + cfgName + " backed up to "
                        + backedupFile.name
            )
            // overwrite the file from new version
            if (!isRules) {
                plugin.saveResource(file.name, true)
            }

            // copy supported values from old file to new
            Utils.logger.info(
                ("&fFile Loader: &8(Migration) &7Migrating &b" + cfgName
                        + "&7 from old version to new version.")
            )

            if (isCustomDrops) {
                FileMigrator.copyCustomDrops(backedupFile, file, fileVersion)
            } else if (!isRules) {
                FileMigrator.copyYmlValues(backedupFile, file, fileVersion)
            } else {
                FileMigrator.migrateRules(file)
            }

            // reload cfg from the updated values
            cfg = YamlConfiguration.loadConfiguration(file)
        } else if (!isRules) {
            checkFileVersion(file, compatibleVersion, ymlHelper.getInt( "file-version"))
        }

        return cfg
    }

    fun getFileLoadErrorMessage(): String {
        return colorizeStandardCodes(
            "&4An error occured&r whilst attempting to parse the file &brules.yml&r due to a user-caused YAML syntax error. Please see the console logs for more details."
        )
    }

    private fun saveResourceIfNotExists(instance: Plugin, file: File) {
        if (!file.exists()) {
            Utils.logger.info(
                "&fFile Loader: &7File '&b" + file.name
                        + "&7' doesn't exist, creating it now..."
            )
            instance.saveResource(file.name, false)
        }
    }

    private fun checkFileVersion(
        file: File, compatibleVersion: Int,
        installedVersion: Int
    ) {
        if (compatibleVersion == installedVersion) {
            return
        }

        val what = if (installedVersion < compatibleVersion) "outdated"
        else "ahead of the compatible version of this file for this version of the plugin"

        Utils.logger.error(
            "&fFile Loader: &7The version of &b" + file.name + "&7 you have installed is "
                    + what
                    + "! Fix this as soon as possible, else the plugin will most likely malfunction."
        )
        Utils.logger.error(
            ("&fFile Loader: &8(&7You have &bv" + installedVersion
                    + "&7 installed but you are meant to be running &bv" + compatibleVersion + "&8)")
        )
    }
}