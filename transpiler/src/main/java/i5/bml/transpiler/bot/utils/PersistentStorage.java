package i5.bml.transpiler.bot.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import i5.bml.transpiler.bot.config.BotSettings;
import i5.bml.transpiler.bot.threads.rasa.RasaComponent;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;

public class PersistentStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStorage.class);

    private static final String SETTINGS_FILE_NAME = "bot_properties.json";

    private static File settingsFile;

    static {
        try {
            var jarExecutionFile = RasaComponent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            settingsFile = new File("/" + FilenameUtils.getPath(jarExecutionFile) + SETTINGS_FILE_NAME);
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to retrieve jar execution uri", e);
        }

        if (!settingsFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                settingsFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                settingsFile.createNewFile();
            } catch (IOException e) {
                LOGGER.error("Failed to create settings file at '{}'", settingsFile.getPath(), e);
            }
        }
    }

    public static BotSettings getBotSettings() {
        var settingsFileContents = IOUtil.readString(settingsFile);
        try {
            return Objects.requireNonNullElse(new Gson().fromJson(settingsFileContents, BotSettings.class), new BotSettings());
        } catch (JsonSyntaxException e) {
            LOGGER.error("Failed to parse JSON from settings file:\n{}", settingsFileContents, e);
        }
        return new BotSettings();
    }

    public static void writeBotSettings(BotSettings settings) {
        var settingsFileContents = new Gson().toJson(settings);
        try {
            Files.write(settingsFile.toPath(), settingsFileContents.getBytes());
        } catch (Exception e) {
            LOGGER.error("Failed to write JSON back to settings file:\n{}", settingsFileContents, e);
        }
    }
}
