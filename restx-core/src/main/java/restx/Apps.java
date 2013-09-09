package restx;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import restx.classloader.CompilationManager;
import restx.common.MoreFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static com.google.common.collect.Iterables.transform;

/**
 * User: xavierhanin
 * Date: 7/28/13
 * Time: 8:16 AM
 */
public class Apps {
    public static CompilationManager newAppCompilationManager(EventBus eventBus) {
        return new CompilationManager(eventBus, getSourceRoots(), getTargetClasses());
    }

    public static Path getTargetClasses() {
        return FileSystems.getDefault().getPath(System.getProperty("restx.targetClasses", "tmp/classes"));
    }

    public static Iterable<Path> getSourceRoots() {
        return transform(Splitter.on(',').trimResults().split(
                    System.getProperty("restx.sourceRoots",
                            "src/main/java, src/main/resources")),
                    MoreFiles.strToPath);
    }

    public static Optional<String> guessAppBasePackage(Path fromDir) {
        for (Path sourceRoot : getSourceRoots()) {
            Path sourceRootDir = fromDir.resolve(sourceRoot);

            try {
                final Path[] appServer = new Path[1];
                Files.walkFileTree(sourceRootDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().equals("AppServer.java")) {
                            appServer[0] = file;
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                if (appServer[0] != null) {
                    return Optional.of(sourceRootDir.relativize(appServer[0]).getParent().toString().replace("/", "."));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.absent();
    }


    public static Process run(File workingDirectory, Path targetClasses, Path dependenciesDir, List<String> vmOptions,
                              String mainClassName, List<String> args) throws IOException {
        return new ProcessBuilder(
                ImmutableList.<String>builder()
                        .add("java",
                             "-cp",
                             targetClasses.toString() + ":" + dependenciesDir.toString() + "/*")
                        .addAll(vmOptions)
                        .add(mainClassName)
                        .addAll(args)
                        .build()
        )
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .directory(workingDirectory.getAbsoluteFile())
                .start();
    }
}