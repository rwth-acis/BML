package i5.bml.transpiler.utils;

import i5.bml.transpiler.InputParser;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.regex.Pattern;

public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    private static final Pattern IMPORT_PATTERN = Pattern.compile("^(package |import )i5\\.bml\\.transpiler\\.bot(.*)");

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static void forceDeleteFile(String botOutputPath, Class<?> clazz) {
        var packageName = clazz.getPackageName()
                .replace("i5.bml.transpiler.bot", "")
                .replaceFirst("\\.", "")
                .replaceAll("\\.", "/");
        File file = null;
        try {
            file = new File("%s%s/%s.java".formatted(botOutputPath, packageName, clazz.getSimpleName()));
            FileUtils.forceDelete(file);
        } catch (Exception e) {
            LOGGER.error("Failed to delete file {}", file, e);
        }
    }

    public static void copyDirAndRenameImports(String packageName, JavaTreeGenerator visitor) {
        try {
            var srcDir = new File("%s/%s".formatted(InputParser.BOT_DIR, packageName));
            var destDir = new File(visitor.botOutputPath() + visitor.outputPackage() + packageName);
            destDir.mkdirs();
            IOUtil.copyFiles(srcDir, destDir, visitor.outputPackage(), FileFilterUtils.trueFileFilter());
        } catch (Exception e) {
            LOGGER.error("Failed to copy from package {}", packageName, e);
        }
    }

    public static void copyFileAndRenameImports(File srcFile, File destFile, String outputPackage) {
        try {
            destFile.createNewFile();
        } catch (IOException e) {
            LOGGER.error("Failed to create file {}", destFile, e);
        }

        try (var srcFileStream = new FileInputStream(srcFile); var destFileStream = new FileOutputStream(destFile)) {
            var destChannel = destFileStream.getChannel();
            var src = new BufferedReader(new FileReader(srcFile));

            String line = src.readLine();
            int position = 0;
            do {
                // We add +1 for the line separator that is discarded by readLine()
                position += line.length() + 1;
                var matcher = IMPORT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    var trailingPackageName = matcher.group(2);
                    if (trailingPackageName.startsWith(".") && outputPackage.isEmpty()) {
                        destChannel.write(ByteBuffer.wrap((matcher.group(1) + outputPackage + trailingPackageName.substring(1) + LINE_SEPARATOR).getBytes()));
                    } else if (!trailingPackageName.startsWith(";")) {
                        destChannel.write(ByteBuffer.wrap((matcher.group(1) + outputPackage + trailingPackageName + LINE_SEPARATOR).getBytes()));
                    }
                } else {
                    destChannel.write(ByteBuffer.wrap((line + LINE_SEPARATOR).getBytes()));
                }

                line = src.readLine();
            } while (!line.startsWith("pu"));

            destChannel.transferFrom(srcFileStream.getChannel().position(position), destChannel.position(), Long.MAX_VALUE);
        } catch (Exception e) {
            LOGGER.error("Failed to copy file {}", srcFile, e);
        }
    }

    public static void copyFiles(File srcDir, File destDir, String outputPackage, FileFilter filter) {
        var files = Objects.requireNonNullElse(srcDir.listFiles(filter), new File[]{});
        for (var srcFile : files) {
            final File destFile = new File(destDir, srcFile.getName());
            if (srcFile.isDirectory()) {
                destFile.mkdir();
                copyFiles(srcFile, destFile, outputPackage, filter);
            } else {
                IOUtil.copyFileAndRenameImports(srcFile, destFile, outputPackage);
            }
        }
    }
}
