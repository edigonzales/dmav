package ch.so.agi.dmav;

import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Merger {

    private static final String FIXPUNKTE_LV_MODEL = "FixpunkteLV_V1_0";
    private static final String FIXPUNKTE_LV_LFP = "FixpunkteLV_V1_0_LFP";
    private static final String FIXPUNKTE_LV_HFP = "FixpunkteLV_V1_0_HFP";
    private static final String FIXPUNKTE_LV_BASKET = "FixpunkteLV_V1_0.FixpunkteLV";
    private static final String FIXPUNKTE_LV_BID = "bb3b5f52-707b-4ac6-9cf8-4d03ef37bb3a";

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
        String umbrellaModel = modelLoader.selectDmavModelForSources(configured.keySet());
        TransferDescription td = modelLoader.compileModels(List.of(umbrellaModel));
        Map<String, List<String>> topicsByModel = ModelLoader.directTransferTopics(td, umbrellaModel);

        Path outputFile = outputDir.resolve("DMAV." + fosnr + ".xtf");
        Set<String> writtenTopics = new LinkedHashSet<>();

        try (IoxWriterResource output = new IoxWriterResource(new XtfWriter(outputFile.toFile(), td))) {
            XtfWriter writer = output.writer;
            writer.write(new ch.interlis.iox_j.StartTransferEvent("DMAVMerger", null));

            for (Map.Entry<String, List<String>> modelTopics : topicsByModel.entrySet()) {
                String modelName = modelTopics.getKey();
                List<String> expectedTopics = modelTopics.getValue();

                if (FIXPUNKTE_LV_MODEL.equals(modelName)) {
                    writeFixpunkteLv(configured, writer, td, writtenTopics);
                } else {
                    Path source = configured.get(modelName);
                    if (source != null) {
                        copyBaskets(source, writer, td, new LinkedHashSet<>(expectedTopics), writtenTopics);
                    }
                }

                for (String topic : expectedTopics) {
                    if (!writtenTopics.contains(topic)) {
                        writeEmptyBasket(writer, topic, fosnr);
                        writtenTopics.add(topic);
                    }
                }
            }

            writer.write(new ch.interlis.iox_j.EndTransferEvent());
        }
    }

    private static void writeFixpunkteLv(
            Map<String, Path> configured,
            XtfWriter writer,
            TransferDescription td,
            Set<String> writtenTopics) throws IoxException {
        Path directSource = configured.get(FIXPUNKTE_LV_MODEL);
        Path lfpSource = configured.get(FIXPUNKTE_LV_LFP);
        Path hfpSource = configured.get(FIXPUNKTE_LV_HFP);

        if (directSource != null && (lfpSource != null || hfpSource != null)) {
            throw new IoxException(
                    "Use either " + FIXPUNKTE_LV_MODEL + " or the LFP/HFP source keys, not both");
        }

        if (directSource != null) {
            copyBaskets(
                    directSource,
                    writer,
                    td,
                    Set.of(FIXPUNKTE_LV_BASKET),
                    writtenTopics);
            return;
        }

        if (lfpSource == null && hfpSource == null) {
            return;
        }

        writer.write(new ch.interlis.iox_j.StartBasketEvent(FIXPUNKTE_LV_BASKET, FIXPUNKTE_LV_BID));
        if (lfpSource != null) {
            copyObjectsByClass(lfpSource, writer, td, ".LFP1");
        }
        if (hfpSource != null) {
            copyObjectsByClass(hfpSource, writer, td, ".HFP1");
        }
        writer.write(new ch.interlis.iox_j.EndBasketEvent());
        writtenTopics.add(FIXPUNKTE_LV_BASKET);
    }

    private static void copyBaskets(
            Path source,
            XtfWriter writer,
            TransferDescription td,
            Set<String> allowedTopics,
            Set<String> writtenTopics) throws IoxException {
        IoxReader reader = openReader(source, td);
        boolean copyCurrentBasket = false;
        try {
            IoxEvent event;
            while ((event = reader.read()) != null) {
                if (event instanceof StartBasketEvent) {
                    StartBasketEvent startBasket = (StartBasketEvent) event;
                    copyCurrentBasket = allowedTopics.contains(startBasket.getType());
                    if (copyCurrentBasket) {
                        writer.write(event);
                        writtenTopics.add(startBasket.getType());
                    }
                } else if (event instanceof ObjectEvent) {
                    if (copyCurrentBasket) {
                        writer.write(event);
                    }
                } else if (event instanceof EndBasketEvent) {
                    if (copyCurrentBasket) {
                        writer.write(event);
                    }
                    copyCurrentBasket = false;
                } else if (event instanceof EndTransferEvent) {
                    break;
                }
            }
        } finally {
            reader.close();
        }
    }

    private static void writeEmptyBasket(XtfWriter writer, String topic, String fosnr) throws IoxException {
        String bidSeed = "dmav-empty:" + fosnr + ":" + topic;
        String bid = UUID.nameUUIDFromBytes(bidSeed.getBytes(StandardCharsets.UTF_8)).toString();
        writer.write(new ch.interlis.iox_j.StartBasketEvent(topic, bid));
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
