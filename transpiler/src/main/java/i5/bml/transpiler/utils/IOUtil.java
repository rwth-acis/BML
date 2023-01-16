package i5.bml.transpiler.utils;

import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    private static final Pattern IMPORT_PATTERN = Pattern.compile("^(package |import )i5\\.bml\\.transpiler\\.bot(.*)");

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final ClassLoader CLASS_LOADER = IOUtil.class.getClassLoader();

    public static final String BOT_DIR = "transpiler/src/main/java/i5/bml/transpiler/";

    private static final IOFileFilter COMPONENT_DIR_FILTER;

    private IOUtil() {}

    static {
        var componentPackageNames = new String[]{"slack", "telegram", "openapi", "rasa", "dialogue", "openai"};
        var componentNameFilter = FileFilterUtils.or(Arrays.stream(componentPackageNames).map(FileFilterUtils::nameFileFilter).toArray(IOFileFilter[]::new));
        COMPONENT_DIR_FILTER = FileFilterUtils.notFileFilter(FileFilterUtils.and(componentNameFilter, FileFilterUtils.directoryFileFilter()));
    }

    public static void copyFiles(String resourceName, File destDir, String outputPackage, Predicate<String> dirFilter) {
        var resource = IOUtil.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            LOGGER.error("Resource {} does not exist", resourceName);
            return;
        }

        URI uri;
        try {
            uri = resource.toURI();
        } catch (URISyntaxException e) {
            LOGGER.error("Resource {} could not be converted to URI", resourceName, e);
            return;
        }

        if (uri.getScheme().equals("jar")) {
            try (var fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                final boolean[] seenRoot = {false};
                Files.walkFileTree(fileSystem.getPath(resourceName), new FileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!seenRoot[0]) {
                            seenRoot[0] = true;
                            return FileVisitResult.CONTINUE;
                        }

                        var srcFile = dir.toString().substring(resourceName.length() + 1);

                        if (dirFilter.test(srcFile)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        new File(destDir.getAbsolutePath() + "/" + srcFile).mkdir();

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        var srcFile = file.toString().substring(resourceName.length() + 1);

                        var stream = CLASS_LOADER.getResourceAsStream(resourceName + "/" + srcFile);
                        if (stream == null) {
                            LOGGER.error("Couldn't find resource {}", srcFile);
                            return FileVisitResult.CONTINUE;
                        }

                        copyFileAndRenameImports(stream, new File(destDir.getAbsolutePath() + "/" + srcFile), outputPackage);

                        stream.close();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Failed to copy resources in directory {} to {}", resourceName, destDir, e);
            }
        } else {
            copyFiles(new File(BOT_DIR + resourceName), destDir, outputPackage, COMPONENT_DIR_FILTER);
        }
    }

    public static InputStream getResourceAsStream(String resourceName) {
        var resource = IOUtil.class.getClassLoader().getResourceAsStream(resourceName);
        if (resource == null) {
            LOGGER.error("Resource {} does not exist", resourceName);
            return InputStream.nullInputStream();
        }

        return resource;
    }

    public static String getResourceAsString(String resourceName) {
        try (var resourceStream = IOUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (resourceStream == null) {
                LOGGER.error("Resource {} does not exist", resourceName);
                return "";
            }

            return new String(resourceStream.readAllBytes(), Charset.defaultCharset());
        } catch (Exception e) {
            LOGGER.error("Failed to read data from resource ({}) input stream", resourceName, e);
            return "";
        }
    }

    public static void forceDeleteFile(String botOutputPath, Class<?> clazz) {
        var packageName = clazz.getPackageName()
                .replace("i5.bml.transpiler.bot", "")
                .replaceFirst("\\.", "")
                .replace("\\.", "/");
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
            var destDir = new File(visitor.botOutputPath() + visitor.outputPackage() + packageName);
            destDir.mkdir();
            IOUtil.copyFiles("bot/" + packageName, destDir, visitor.outputPackage(), s -> true);
        } catch (Exception e) {
            LOGGER.error("Failed to copy from package {}", packageName, e);
        }
    }

    private static void copyFileAndRenameImports(File srcFile, File destFile, String outputPackage) {
        try (var stream = new FileInputStream(srcFile)) {
            copyFileAndRenameImports(stream, destFile, outputPackage);
        } catch (IOException e) {
            LOGGER.error("Failed to copy files from {} to {}", srcFile, destFile, e);
        }
    }

    private static void copyFileAndRenameImports(InputStream inputStream, File destFile, String outputPackage) {
        try {
            destFile.createNewFile();
        } catch (IOException e) {
            LOGGER.error("Failed to create file {}", destFile, e);
        }

        try (var destFileStream = new FileOutputStream(destFile)) {
            var destChannel = destFileStream.getChannel();
            destChannel.position(0);
            var inputBytes = inputStream.readAllBytes();
            var src = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(inputBytes)));

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

            var byteArrayInputStream = new ByteArrayInputStream(inputBytes);
            byteArrayInputStream.skip(position);
            destChannel.transferFrom(Channels.newChannel(byteArrayInputStream), destChannel.position(), Long.MAX_VALUE);
        } catch (Exception e) {
            LOGGER.error("Failed to copy file {}", inputStream, e);
        }
    }

    public static void copyFiles(File srcDir, File destDir, String outputPackage, FileFilter filter) {
        var files = Objects.requireNonNullElse(srcDir.listFiles(filter), new File[]{});
        for (var srcFile : files) {
            var destFile = new File(destDir, srcFile.getName());
            if (srcFile.isDirectory()) {
                destFile.mkdir();
                copyFiles(srcFile, destFile, outputPackage, filter);
            } else {
                IOUtil.copyFileAndRenameImports(srcFile, destFile, outputPackage);
            }
        }
    }
}
