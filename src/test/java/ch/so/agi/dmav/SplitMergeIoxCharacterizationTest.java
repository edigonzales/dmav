package ch.so.agi.dmav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SplitMergeIoxCharacterizationTest {

    private static final String FIXPUNKTE_AV_TYPE =
            "DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3";
    private static final String HOHEITSGRENZEN_AV_TYPE =
            "DMAV_HoheitsgrenzenAV_V1_0.HoheitsgrenzenAV";
    private static final String FIXPUNKTE_LV_TYPE = "FixpunkteLV_V1_0.FixpunkteLV";

    @Test
    void splitterPreservesBasketBidTidsAndReferences(@TempDir Path tempDir) throws Exception {
        Path input = Path.of("src/test/data/splitter/DMAV.449.xtf");

        Splitter splitter = new Splitter();
        assertTrue(splitter.run(input, "449", tempDir));

        IoxTestSupport.TransferSnapshot source = IoxTestSupport.read(input);

        assertBasketIdentityEquals(
                source.basket(FIXPUNKTE_AV_TYPE),
                IoxTestSupport.read(tempDir.resolve("DMAV_FixpunkteAVKategorie3_V1_0.449.xtf"))
                        .basket(FIXPUNKTE_AV_TYPE));

        assertBasketIdentityEquals(
                source.basket(HOHEITSGRENZEN_AV_TYPE),
                IoxTestSupport.read(tempDir.resolve("DMAV_HoheitsgrenzenAV_V1_0.449.xtf"))
                        .basket(HOHEITSGRENZEN_AV_TYPE));
    }

    @Test
    void mergerPreservesUnmodifiedBasketBidTidsAndReferences(@TempDir Path tempDir) throws Exception {
        Path fixpunkteSource = Path.of("src/test/data/merger/DMAV_FixpunkteAVKategorie3_V1_0.449.xtf");
        Path hoheitsgrenzenSource = Path.of("src/test/data/merger/DMAV_HoheitsgrenzenAV_V1_0.449.xtf");

        Merger merger = new Merger();
        assertTrue(merger.run(Path.of("src/test/data/merger/myconfig_local.ini"), "449", tempDir));

        IoxTestSupport.TransferSnapshot merged = IoxTestSupport.read(tempDir.resolve("DMAV.449.xtf"));

        assertBasketIdentityEquals(
                IoxTestSupport.read(fixpunkteSource).basket(FIXPUNKTE_AV_TYPE),
                merged.basket(FIXPUNKTE_AV_TYPE));
        assertBasketIdentityEquals(
                IoxTestSupport.read(hoheitsgrenzenSource).basket(HOHEITSGRENZEN_AV_TYPE),
                merged.basket(HOHEITSGRENZEN_AV_TYPE));
    }

    @Test
    void mergerFixpunkteLvSpecialCaseCurrentlyCreatesOneBasketWithStaticBid(@TempDir Path tempDir)
            throws Exception {
        Path lfpSource = Path.of("src/test/data/merger/FixpunkteLV_V1_0_LFP.xtf");
        Path hfpSource = Path.of("src/test/data/merger/FixpunkteLV_V1_0_HFP.xtf");

        Merger merger = new Merger();
        assertTrue(merger.run(Path.of("src/test/data/merger/myconfig_local_multiple.ini"), "449", tempDir));

        IoxTestSupport.BasketSnapshot lfp = IoxTestSupport.read(lfpSource).basket(FIXPUNKTE_LV_TYPE);
        IoxTestSupport.BasketSnapshot hfp = IoxTestSupport.read(hfpSource).basket(FIXPUNKTE_LV_TYPE);
        IoxTestSupport.BasketSnapshot merged =
                IoxTestSupport.read(tempDir.resolve("DMAV.449.xtf")).basket(FIXPUNKTE_LV_TYPE);

        assertNotNull(lfp);
        assertNotNull(hfp);
        assertNotNull(merged);
        assertEquals("bb3b5f52-707b-4ac6-9cf8-4d03ef37bb3a", merged.getBid());

        Set<String> sourceTids = new LinkedHashSet<>();
        sourceTids.addAll(lfp.tids());
        sourceTids.addAll(hfp.tids());
        assertEquals(sourceTids, merged.tids());
        assertEquals(6, merged.getObjects().size());
    }

    private static void assertBasketIdentityEquals(
            IoxTestSupport.BasketSnapshot expected, IoxTestSupport.BasketSnapshot actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getBid(), actual.getBid());
        assertEquals(expected.tidsByTag(), actual.tidsByTag());
        assertEquals(expected.referencesByTid(), actual.referencesByTid());
    }
}
