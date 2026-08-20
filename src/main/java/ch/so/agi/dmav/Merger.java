package ch.so.agi.dmav;

import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.xtf.XtfStartTransferEvent;
import ch.interlis.iom_j.xtf.XtfWriter;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox.StartTransferEvent;
import ch.interlis.iox_j.IoxIliReader;
import ch.interlis.iox_j.utility.ReaderFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Merger {

    private static final String FIXPUNKTE_LV_LFP = "FixpunkteLV_V1_0_LFP";
    private static final String FIXPUNKTE_LV_HFP = "FixpunkteLV_V1_0_HFP";
    private static final String FIXPUNKTE_LV_BASKET = "FixpunkteLV_V1_0.FixpunkteLV";
    private static final String FIXPUNKTE_LV_BID = "bb3b5f52-707b-4ac6-9cf8-4d03ef37bb3a";

    private static final List<String> LEGACY_SOURCE_ORDER = List.of(
            FIXPUNKTE_LV_LFP,
            FIXPUNKTE_LV_HFP,
            "KGKCGC_FPDS2_V1_1",
            "DMAV_FixpunkteAVKategorie3_V1_0",
            "DMAV_Bodenbedeckung_V1_0",
            "DMAV_Einzelobjekte_V1_0",
            "DMAV_Nomenklatur_V1_0",
            "DMAV_Grundstuecke_V1_0",
            "DMAV_Rohrleitungen_V1_0",
            "DMAV_HoheitsgrenzenLV_V1_0",
            "DMAV_HoheitsgrenzenAV_V1_0",
            "DMAV_Toleranzstufen_V1_0",
            "DMAV_DauerndeBodenverschiebungen_V1_0",
            "OfficialIndexOfLocalities_V1_0",
            "DMAV_Gebaeudeadressen_V1_0",
            "DMAVSUP_UntereinheitGrundbuch_V1_0",
            "DMAV_Dienstbarkeitsgrenzen_V1_0");

    private final InputResolver inputResolver;
    private final ModelLoader modelLoader;

    public Merger() {
        this(new InputResolver(), new ModelLoader());
    }

    Merger(InputResolver inputResolver, ModelLoader modelLoader) {
        this.inputResolver = inputResolver;
        this.modelLoader = modelLoader;
    }

    public boolean run(Path configFile, String fosnr, Path outputDir) {
        Path workDir = null;
        try {
            Files.createDirectories(outputDir);
            workDir = Files.createTempDirectory("dmav_");
            merge(configFile, fosnr, outputDir, workDir);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (workDir != null) {
                try {
                    Utils.deleteDirectory(workDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void merge(Path configFile, String fosnr, Path outputDir, Path workDir) throws Exception {
        Map<String, Path> configured = inputResolver.resolve(configFile, fosnr, workDir);
        Map<String, Path> sources = prepareLegacySources(configured, fosnr, workDir);

        Set<String> modelNames = new LinkedHashSet<>();
        for (Path source : sources.values()) {
            modelNames.addAll(readModelNames(source));
        }
        // Keep the umbrella model as the final top-level model. This guarantees an
        // XTF 2.4 writer while still compiling additional non-DMAV source models.
        modelNames.remove(ModelLoader.LEGACY_DMAV_MODEL);
        modelNames.add(ModelLoader.LEGACY_DMAV_MODEL);

        TransferDescription td = modelLoader.compileModels(modelNames);
        Path outputFile = outputDir.resolve("DMAV." + fosnr + ".xtf");

        try (IoxWriterResource output = new IoxWriterResource(new XtfWriter(outputFile.toFile(), td))) {
            XtfWriter writer = output.writer;
            writer.write(new ch.interlis.iox_j.StartTransferEvent("DMAVMerger", null));

            writeFixpunkteLv(writer, sources.get(FIXPUNKTE_LV_LFP), sources.get(FIXPUNKTE_LV_HFP), td);

            for (Map.Entry<String, Path> source : sources.entrySet()) {
                if (FIXPUNKTE_LV_LFP.equals(source.getKey()) || FIXPUNKTE_LV_HFP.equals(source.getKey())) {
                    continue;
                }
                copyBaskets(source.getValue(), writer, td);
            }

            writer.write(new ch.interlis.iox_j.EndTransferEvent());
        }
    }

    private Map<String, Path> prepareLegacySources(
            Map<String, Path> configured, String fosnr, Path workDir) throws IOException {
        Map<String, Path> sources = new LinkedHashMap<>();
        for (String key : LEGACY_SOURCE_ORDER) {
            Path configuredSource = configured.get(key);
            if (configuredSource != null) {
                sources.put(key, configuredSource);
            } else {
                sources.put(key, materializeEmptyResource(key, fosnr, workDir));
            }
        }

        // Preserve configured extension models as well. The old XSLT ignored unknown
        // keys; the IOX merger can safely append them once their model is available.
        for (Map.Entry<String, Path> entry : configured.entrySet()) {
            sources.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return sources;
    }

    private static Path materializeEmptyResource(String key, String fosnr, Path workDir) throws IOException {
        String resourceName = key + ".empty.xtf";
        Path target = workDir.resolve(key + "." + fosnr + ".empty.xtf");
        try (InputStream in = Merger.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new FileNotFoundException("Resource file not found: " + resourceName);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static List<String> readModelNames(Path source) throws IoxException {
        IoxReader reader = new ReaderFactory().createReader(source.toFile(), null, new Settings());
        try {
            IoxEvent event = reader.read();
            if (!(event instanceof XtfStartTransferEvent)) {
                throw new IoxException("XTF header does not expose INTERLIS models: " + source);
            }

            Map<String, IomObject> headerObjects = ((XtfStartTransferEvent) event).getHeaderObjects();
            List<String> modelNames = new ArrayList<>();
            if (headerObjects != null) {
                for (IomObject headerObject : headerObjects.values()) {
                    String modelName = headerObject.getattrvalue("model");
                    if (modelName != null && !modelName.isBlank()) {
                        modelNames.add(modelName);
                    }
                }
            }
            if (modelNames.isEmpty()) {
                throw new IoxException("No INTERLIS models found in XTF header: " + source);
            }
            return modelNames;
        } finally {
            reader.close();
        }
    }

    private static void copyBaskets(Path source, XtfWriter writer, TransferDescription td) throws IoxException {
        IoxReader reader = openReader(source, td);
        try {
            IoxEvent event;
            while ((event = reader.read()) != null) {
                if (event instanceof StartBasketEvent
                        || event instanceof ObjectEvent
                        || event instanceof EndBasketEvent) {
                    writer.write(event);
                } else if (event instanceof EndTransferEvent) {
                    break;
                }
            }
        } finally {
            reader.close();
        }
    }

    private static void writeFixpunkteLv(
            XtfWriter writer, Path lfpSource, Path hfpSource, TransferDescription td) throws IoxException {
        writer.write(new ch.interlis.iox_j.StartBasketEvent(FIXPUNKTE_LV_BASKET, FIXPUNKTE_LV_BID));
        copyObjectsByClass(lfpSource, writer, td, ".LFP1");
        copyObjectsByClass(hfpSource, writer, td, ".HFP1");
        writer.write(new ch.interlis.iox_j.EndBasketEvent());
    }

    private static void copyObjectsByClass(
            Path source, XtfWriter writer, TransferDescription td, String classSuffix) throws IoxException {
        IoxReader reader = openReader(source, td);
        try {
            IoxEvent event;
            while ((event = reader.read()) != null) {
                if (event instanceof ObjectEvent) {
                    IomObject object = ((ObjectEvent) event).getIomObject();
                    if (object.getobjecttag().endsWith(classSuffix)) {
                        writer.write(event);
                    }
                } else if (event instanceof EndTransferEvent) {
                    break;
                }
            }
        } finally {
            reader.close();
        }
    }

    private static IoxReader openReader(Path source, TransferDescription td) throws IoxException {
        IoxReader reader = new ReaderFactory().createReader(source.toFile(), null, new Settings());
        IoxEvent firstEvent = reader.read();
        if (!(firstEvent instanceof StartTransferEvent)) {
            reader.close();
            throw new IoxException("Expected StartTransferEvent in " + source);
        }
        if (!(reader instanceof IoxIliReader)) {
            reader.close();
            throw new IoxException("Input is not an INTERLIS transfer file: " + source);
        }
        ((IoxIliReader) reader).setModel(td);
        return reader;
    }

    private static final class IoxWriterResource implements AutoCloseable {
        private final XtfWriter writer;

        private IoxWriterResource(XtfWriter writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IoxException {
            writer.close();
        }
    }
}
