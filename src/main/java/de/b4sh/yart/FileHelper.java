package de.b4sh.yart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * FileHelper collects all file operation functions that may help templating all relevant components.
 */
public class FileHelper {

    private final static Logger log = Logger.getLogger(FileHelper.class.getName());

    /**
     * The function copyDirectory walks over all files and subdirectories starting from given sourceLocation
     * @param sourceLocation source location from where to start the copy process
     * @param targetLocation target location to copy files towards
     * @throws IOException thrown when IO operation fails
     */
    public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
        log.log(Level.INFO, String.format("Copying files from %s to %s",sourceLocation.getPath(), targetLocation.getPath()));
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        log.log(Level.INFO, "Done copying files.");
    }

    /**
     * Deletes folder with content in reverse order.
     * @param element path to start deleting in
     */
    public static void deleteFolderWithContent(Path element) {
        try (Stream<Path> pathStream = Files.walk(element)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
