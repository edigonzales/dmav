package ch.so.agi.dmav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class Dmav11FixtureCharacterizationTest {

    private static final Path MODEL = Path.of("src/test/data/dmav11/DMAVTYM_Alles_V1_1.ili");
    private static final Path FIXTURE = Path.of("src/test/data/dmav11/DMAVTYM_Alles_V1_1.reduced.xtf");

    private static final String BASKET_TYPE =
            "DMAV_FixpunkteAVKategorie3_V1_1.FixpunkteAVKategorie3";
    private static final String HFP3_NF_TAG = BASKET_TYPE + ".HFP3Nachfuehrung";
    private static final String HFP3_TAG = BASKET_TYPE + ".HFP3";

    private static final String BID = "1331ee4d-9b33-466e-98a1-82eb52ed9c82";
    private static final String NF_TID = "34776b74-5962-43db-aa7d-d91e32009943";
    private static final String HFP3_TID = "891fbd78-c4c4-4f18-b222-4ab8e43ed9a5";

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
        IoxTestSupport.TransferSnapshot transfer = IoxTestSupport.read(FIXTURE);

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
        IoxTestSupport.TransferSnapshot transfer = IoxTestSupport.read(FIXTURE);

        assertTrue(transfer.allTids().containsAll(transfer.allReferenceTargets()));
        assertEquals(Set.of(NF_TID), transfer.allReferenceTargets());
    }
}
