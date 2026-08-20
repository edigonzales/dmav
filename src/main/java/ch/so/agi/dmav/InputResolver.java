package ch.so.agi.dmav;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class InputResolver {

    Map<String, Path> resolve(Path configFile, String fosnr, Path workDir) throws IOException {
        Files.createDirectories(workDir);

        Map<String, String> configured = ConfigParser.read(configFile);
        Map<String, Path> resolved = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : configured.entrySet()) {
            String key = entry.getKey().trim();
            String value = entry.getValue().trim().replace("${fosnr}", fosnr);
            resolved.put(key, resolveValue(key, value, fosnr, workDir));
        }
        return resolved;
    }

    private Path resolveValue(String key, String value, String fosnr, Path workDir) throws IOException {
        Path target = workDir.resolve(key + "." + fosnr + ".xtf");

        if (value.startsWith("http://") || value.startsWith("https://")) {
            try (InputStream in = URI.create(value).toURL().openStream()) {
                if (isZip(value)) {
                    extractFirstXtf(in, target, value);
                } else {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return target;
        }

        Path source = Paths.get(value);
        if (isZip(value)) {
            try (InputStream in = Files.newInputStream(source)) {
                extractFirstXtf(in, target, source.toString());
            }
        } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static boolean isZip(String value) {
        return value.toLowerCase().endsWith(".zip");
    }

    private static void extractFirstXtf(InputStream zipInput, Path target, String sourceName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInput)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xtf")) {
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
                zis.closeEntry();
            }
        }
        throw new IOException("No XTF file found in ZIP file: " + sourceName);
    }
}
