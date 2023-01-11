package i5.bml.transpiler.bot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Collectors;

public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    private IOUtil() {}

    public static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(classLoader.getResourceAsStream(fileName))))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static String readString(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            LOGGER.error("Failed to read from file {}", file.getPath(), e);
        }

        return "";
    }
}
