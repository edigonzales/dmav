package ch.so.agi.dmav;

import static org.junit.jupiter.api.Assertions.assertSame;

import ch.interlis.ili2c.metamodel.TransferDescription;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelLoaderCacheTest {

    @Test
    void reusesCompilationAcrossLoaderInstances(@TempDir Path tempDir) throws Exception {
        Files.writeString(
                tempDir.resolve("CacheTestModel.ili"),
                "INTERLIS 2.4;\n"
                        + "MODEL CacheTestModel AT \"https://example.invalid/\" VERSION \"2026-08-20\" =\n"
                        + "  TOPIC Data =\n"
                        + "    CLASS Item =\n"
                        + "      name : TEXT*20;\n"
                        + "    END Item;\n"
                        + "  END Data;\n"
                        + "END CacheTestModel.\n");

        ModelLoader firstLoader = new ModelLoader(tempDir.toString());
        ModelLoader secondLoader = new ModelLoader(tempDir.toString());

        TransferDescription first = firstLoader.compileModels(List.of("CacheTestModel"));
        TransferDescription second =
                secondLoader.compileModels(List.of(" CacheTestModel ", "CacheTestModel"));

        assertSame(first, second);
    }
}
