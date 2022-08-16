package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

public abstract class FilesChangeWatcher {

    protected final WatchService watchService;

    public FilesChangeWatcher() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void watch(String... excludedFileExtensions) {
        try {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    String fileName = event.context().toString();
                    if (excludeFile(fileName, excludedFileExtensions))
                        continue;

                    try {
                        processFile(event.kind(), new File(key.watchable().toString(), fileName));
                    } catch (Exception e) {
                        ServerLoggers.systemLogger.error("FilesChangeWatcher processFile error. " + e.getMessage() + "  File: '" + fileName + "'");
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean excludeFile(String fileName, String... excludedFileExtensions) {
        boolean exclude = fileName.endsWith("~");
        if (!exclude) {
            for (String extension : excludedFileExtensions) {
                if (FilenameUtils.getExtension(fileName).equals(extension)) {
                    exclude = true;
                    break;
                }
            }
        }
        return exclude;
    }

    protected abstract void processFile(WatchEvent.Kind<?> kind, File file);

    public void walkAndRegisterDirectories(List<Path> paths) {
        // register directory and subdirectories
        try {
            for (Path path : paths) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
