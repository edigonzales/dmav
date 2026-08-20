package ch.so.agi.dmav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.interlis.ili2c.metamodel.TransferDescription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Dmav11FixtureCharacterizationTest {

    private static final Path MODEL = Path.of("src/test/data/dmav11/DMAVTYM_Alles_V1_1.ili");
    private static final Path FIXTURE = Path.of("src/test/data/dmav11/DMAVTYM_Alles_V1_1.reduced.xtf");
    private static final Path TWO_BASKET_FIXTURE =
            Path.of("src/test/data/dmav11/DMAV_FixpunkteAVKategorie3_V1_1.two-baskets.xtf");

    private static final String BASKET_TYPE =
            "DMAV_FixpunkteAVKategorie3_V1_1.FixpunkteAVKategorie3";
    private static final String HFP3_NF_TAG = BASKET_TYPE + ".HFP3Nachfuehrung";
    private static final String HFP3_TAG = BASKET_TYPE + ".HFP3";

    private static final String BID = "1331ee4d-9b33-466e-98a1-82eb52ed9c82";
    private static final String NF_TID = "34776b74-5962-43db-aa7d-d91e32009943";
    private static final String HFP3_TID = "891fbd78-c4c4-4f18-b222-4ab8e43ed9a5";

    private static TransferDescription dmav11Model;

    @BeforeAll
    static void compileDmav11Model() throws Exception {
        dmav11Model = IoxTestSupport.compileModel(
                "DMAVTYM_Alles_V1_1",
                "src/test/data/dmav11",
                "https://models.geo.admin.ch",
                "https://models.interlis.ch");
    }

    @Test
    void usesCurrentDmav11UmbrellaModel() throws IOException {
        String model = Files.readString(MODEL);

        assertTrue(model.contains("MODEL DMAVTYM_Alles_V1_1"));
        assertTrue(model.contains("VERSION \"2026-01-31\""));
        assertTrue(model.contains("IMPORTS DMAV_FixpunkteAVKategorie3_V1_1;"));
        assertTrue(model.contains("IMPORTS DMAV_Bodenbedeckung_V1_1;"));
        assertTrue(model.contains("IMPORTS DMAV_Grundstuecke_V1_1;"));
    }

    @Test
    void fixturePinsBasketObjectAndReferenceIdentity() throws Exception {
        IoxTestSupport.TransferSnapshot transfer = IoxTestSupport.read(FIXTURE, dmav11Model);

        assertEquals(1, transfer.getBaskets().size());

        IoxTestSupport.BasketSnapshot basket = transfer.basket(BASKET_TYPE);
        assertNotNull(basket);
        assertEquals(BID, basket.getBid());
        assertEquals(Set.of(NF_TID, HFP3_TID), basket.tids());

        IoxTestSupport.ObjectSnapshot nf = basket.object(NF_TID);
        IoxTestSupport.ObjectSnapshot hfp3 = basket.object(HFP3_TID);
        assertNotNull(nf);
        assertNotNull(hfp3);
        assertEquals(HFP3_NF_TAG, nf.getTag());
        assertEquals(HFP3_TAG, hfp3.getTag());
        assertEquals(List.of(NF_TID), hfp3.getReferences().get("Entstehung"));
    }

    @Test
    void allFixtureReferencesResolveToObjectsInTheFixture() throws Exception {
        IoxTestSupport.TransferSnapshot transfer = IoxTestSupport.read(FIXTURE, dmav11Model);

        assertTrue(transfer.allTids().containsAll(transfer.allReferenceTargets()));
        assertEquals(Set.of(NF_TID), transfer.allReferenceTargets());
    }

    @Test
    void ioxSplitterPreservesDmav11BasketTidAndReferences(@TempDir Path tempDir) throws Exception {
        Splitter splitter = new Splitter();
        assertTrue(splitter.run(FIXTURE, "449", tempDir));

        Path output = tempDir.resolve("DMAV_FixpunkteAVKategorie3_V1_1.449.xtf");
        assertTrue(Files.exists(output));
        assertFalse(Files.exists(tempDir.resolve("DMAV_split_logging.xtf")));

        IoxTestSupport.BasketSnapshot source = IoxTestSupport.read(FIXTURE, dmav11Model).basket(BASKET_TYPE);
        IoxTestSupport.BasketSnapshot split = IoxTestSupport.read(output, dmav11Model).basket(BASKET_TYPE);

        assertNotNull(source);
        assertNotNull(split);
        assertEquals(source.getBid(), split.getBid());
        assertEquals(source.tidsByTag(), split.tidsByTag());
        assertEquals(source.referencesByTid(), split.referencesByTid());
    }

    @Test
    void ioxSplitterKeepsMultipleBasketsOfSameModelInOneFile(@TempDir Path tempDir) throws Exception {
        Splitter splitter = new Splitter();
        assertTrue(splitter.run(TWO_BASKET_FIXTURE, "449", tempDir));

        Path output = tempDir.resolve("DMAV_FixpunkteAVKategorie3_V1_1.449.xtf");
        IoxTestSupport.TransferSnapshot transfer = IoxTestSupport.read(output, dmav11Model);

        assertEquals(2, transfer.getBaskets().size());
        assertEquals(
                List.of(
                        "11111111-1111-4111-8111-111111111111",
                        "22222222-2222-4222-8222-222222222222"),
                transfer.getBaskets().stream()
                        .map(IoxTestSupport.BasketSnapshot::getBid)
                        .collect(Collectors.toList()));
    }
}
