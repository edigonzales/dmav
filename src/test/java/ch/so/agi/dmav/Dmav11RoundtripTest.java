package ch.so.agi.dmav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.interlis.ili2c.metamodel.TransferDescription;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Dmav11RoundtripTest {

    private static final Path FIXTURE =
            Path.of("src/test/data/dmav11/DMAVTYM_Alles_V1_1.reduced.xtf");
    private static final String SOURCE_MODEL = "DMAV_FixpunkteAVKategorie3_V1_1";
    private static final String SOURCE_TOPIC = SOURCE_MODEL + ".FixpunkteAVKategorie3";

    private static TransferDescription dmav11Model;

    @BeforeAll
    static void compileDmav11Model() throws Exception {
        dmav11Model = IoxTestSupport.compileModel(
                ModelLoader.CURRENT_DMAV_MODEL,
                "src/test/data/dmav11",
                "https://models.geo.admin.ch",
                "https://models.interlis.ch");
    }

    @Test
    void splitThenMergePreservesDataAndAddsModelDrivenEmptyBaskets(@TempDir Path tempDir)
            throws Exception {
        Path splitDir = tempDir.resolve("split");
        Path mergeDir = tempDir.resolve("merge");

        Splitter splitter = new Splitter();
        assertTrue(splitter.run(FIXTURE, "449", splitDir));

        Path splitFile = splitDir.resolve(SOURCE_MODEL + ".449.xtf");
        assertTrue(Files.exists(splitFile));

        Path configFile = tempDir.resolve("roundtrip.ini");
        Files.writeString(configFile, SOURCE_MODEL + "=" + splitFile.toAbsolutePath() + System.lineSeparator());

        Merger merger = new Merger();
        assertTrue(merger.run(configFile, "449", mergeDir));

        Path mergedFile = mergeDir.resolve("DMAV.449.xtf");
        IoxTestSupport.TransferSnapshot original = IoxTestSupport.read(FIXTURE, dmav11Model);
        IoxTestSupport.TransferSnapshot merged = IoxTestSupport.read(mergedFile, dmav11Model);

        IoxTestSupport.BasketSnapshot originalBasket = original.basket(SOURCE_TOPIC);
        IoxTestSupport.BasketSnapshot mergedBasket = merged.basket(SOURCE_TOPIC);
        assertNotNull(originalBasket);
        assertNotNull(mergedBasket);
        assertEquals(originalBasket.getBid(), mergedBasket.getBid());
        assertEquals(originalBasket.tidsByTag(), mergedBasket.tidsByTag());
        assertEquals(originalBasket.referencesByTid(), mergedBasket.referencesByTid());

        Set<String> expectedTopics = new LinkedHashSet<>();
        for (Map.Entry<String, java.util.List<String>> entry :
                ModelLoader.directTransferTopics(dmav11Model, ModelLoader.CURRENT_DMAV_MODEL).entrySet()) {
            expectedTopics.addAll(entry.getValue());
        }

        Set<String> actualTopics = new LinkedHashSet<>();
        for (IoxTestSupport.BasketSnapshot basket : merged.getBaskets()) {
            actualTopics.add(basket.getType());
        }
        assertEquals(expectedTopics, actualTopics);

        for (String topic : expectedTopics) {
            IoxTestSupport.BasketSnapshot basket = merged.basket(topic);
            assertNotNull(basket);
            if (!SOURCE_TOPIC.equals(topic)) {
                assertTrue(basket.getObjects().isEmpty(), "Expected empty basket for " + topic);
            }
        }

        assertTrue(merged.allTids().containsAll(merged.allReferenceTargets()));
    }
}
