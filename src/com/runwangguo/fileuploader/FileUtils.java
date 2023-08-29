package com.runwangguo.fileuploader;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {
	public static void unzipFile(File file, File destinationDir) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(destinationDir, entry.getName());

            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                entryDestination.getParentFile().mkdirs();
                try (InputStream in = zipFile.getInputStream(entry);
                     OutputStream out = new FileOutputStream(entryDestination)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        }

        zipFile.close();
    }
	public static File findIndexHtmlFile(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File indexHtmlFile = findIndexHtmlFile(file);
                    if (indexHtmlFile != null) {
                        return indexHtmlFile;
                    }
                } else if (file.getName().equalsIgnoreCase("index.html")) {
                    return file;
                }
            }
        }
        return null;
    }

	public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
