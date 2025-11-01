package net.ccbluex.liquidbounce.file

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.Lizz.CLIENT_NAME
import net.ccbluex.liquidbounce.Lizz.MINECRAFT_VERSION
import net.ccbluex.liquidbounce.Lizz.background
import net.ccbluex.liquidbounce.Lizz.isStarting
import net.ccbluex.liquidbounce.file.configs.*
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.zipFilesTo
import net.ccbluex.liquidbounce.utils.render.shader.Background
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.io.File

private val FILE_CONFIGS = ArrayList<FileConfig>()

@SideOnly(Side.CLIENT)
object FileManager : MinecraftInstance, Iterable<FileConfig> by FILE_CONFIGS {

    val dir = File(mc.mcDataDir, "$CLIENT_NAME-$MINECRAFT_VERSION")
    val fontsDir = File(dir, "fonts")
    val settingsDir = File(dir, "settings")
    val themesDir = File(dir, "themes")

    val modulesConfig = +ModulesConfig(File(dir, "modules.json"))
    val valuesConfig = +ValuesConfig(File(dir, "values.json"))
    val clickGuiConfig = +ClickGuiConfig(File(dir, "clickgui.json"))
    val accountsConfig = +AccountsConfig(File(dir, "accounts.json"))
    val friendsConfig = +FriendsConfig(File(dir, "friends.json"))
    val xrayConfig = +XRayConfig(File(dir, "xray-blocks.json"))
    val hudConfig = +HudConfig(File(dir, "hud.json"))
    val shortcutsConfig = +ShortcutsConfig(File(dir, "shortcuts.json"))

    val backgroundImageFile = File(dir, "userbackground.png")
    val backgroundShaderFile = File(dir, "userbackground.frag")

    var firstStart = false
        private set

    var backedup = false
        private set

    val PRETTY_GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    init {
        setupFolder()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun <T : FileConfig> T.unaryPlus(): T = apply {
        FILE_CONFIGS.add(this)
    }

    private fun setupFolder() {
        if (!dir.exists()) {
            dir.mkdir()
            firstStart = true
        }
        if (!fontsDir.exists()) fontsDir.mkdir()
        if (!settingsDir.exists()) settingsDir.mkdir()
        if (!themesDir.exists()) themesDir.mkdir()
    }

    fun backupAllConfigs(previousVersion: String, currentVersion: String) {
        try {
            FILE_CONFIGS.mapNotNull { it.file.takeIf(File::isFile) }.zipFilesTo(File(dir, "backup_${previousVersion}_${currentVersion}.zip"))
            backedup = true
            LOGGER.info("[FileManager] Successfully backed up all configs.")
        } catch (e: Exception) {
            LOGGER.error("[FileManager] Failed backup configs!", e)
        }
    }

    fun loadAllConfigs() {
        FILE_CONFIGS.forEach {
            try {
                loadConfig(it)
            } catch (e: Exception) {
                LOGGER.error("[FileManager] Failed to load config file of ${it.file.name}.", e)
            }
        }
    }

    fun loadConfigs(vararg configs: FileConfig) {
        for (fileConfig in configs) loadConfig(fileConfig)
    }

    fun loadConfig(config: FileConfig) {
        if (!config.hasConfig()) {
            LOGGER.info("[FileManager] Skipped loading config: ${config.file.name}.")
            config.loadDefault()
            saveConfig(config, false)
            return
        }

        try {
            config.loadConfig()
            LOGGER.info("[FileManager] Loaded config: ${config.file.name}.")
        } catch (t: Throwable) {
            LOGGER.error("[FileManager] Failed to load config file: ${config.file.name}.", t)
        }
    }

    fun saveAllConfigs() {
        FILE_CONFIGS.forEach {
            try {
                saveConfig(it)
            } catch (e: Exception) {
                LOGGER.error("[FileManager] Failed to save config file of ${it.file.name}.", e)
            }
        }
        // no automatic exported_settings file created anymore
    }

    fun saveConfigs(vararg configs: FileConfig) {
        for (fileConfig in configs) saveConfig(fileConfig)
    }

    fun saveConfig(config: FileConfig, ignoreStarting: Boolean = true) {
        if (ignoreStarting && isStarting) return

        try {
            if (!config.hasConfig()) config.createConfig()
            config.saveConfig()
            LOGGER.info("[FileManager] Saved config: ${config.file.name}.")
        } catch (t: Throwable) {
            LOGGER.error("[FileManager] Failed to save config file: ${config.file.name}.", t)
        }
    }

    /**
     * Save a local settings script file (values + states + binds) into settings/<name>.txt
     * Use this from your `.localsettings save <name>` command handler.
     */
    fun saveLocalSettings(name: String) {
        try {
            if (!settingsDir.exists()) settingsDir.mkdirs()
            val safeName = name.trim().ifEmpty { "exported_settings" }
            val outFile = File(settingsDir, "$safeName.txt")
            val script = net.ccbluex.liquidbounce.config.SettingsUtils.generateScript(values = true, binds = true, states = true)
            outFile.writeText(script)
            LOGGER.info("[FileManager] Saved local settings script to ${outFile.absolutePath}")
        } catch (t: Throwable) {
            LOGGER.error("[FileManager] Failed to save local settings script.", t)
        }
    }

    /**
     * Load local settings script from settings/<name>.txt and apply it (values + states + binds).
     * Returns true on success, false if file missing or exception.
     */
    fun loadLocalSettings(name: String): Boolean {
        try {
            val safeName = name.trim().ifEmpty { "exported_settings" }
            val inFile = File(settingsDir, "$safeName.txt")
            if (!inFile.exists()) {
                LOGGER.warn("[FileManager] Local settings file not found: ${inFile.absolutePath}")
                return false
            }
            val script = inFile.readText()
            net.ccbluex.liquidbounce.config.SettingsUtils.applyScript(script)
            LOGGER.info("[FileManager] Loaded local settings script from ${inFile.absolutePath}")
            return true
        } catch (t: Throwable) {
            LOGGER.error("[FileManager] Failed to load local settings script.", t)
            return false
        }
    }

    fun loadBackground() {
        val backgroundFile = when {
            backgroundImageFile.exists() -> backgroundImageFile
            backgroundShaderFile.exists() -> backgroundShaderFile
            else -> null
        }

        if (backgroundFile != null) {
            background = Background.fromFile(backgroundFile)
        }
    }
}
