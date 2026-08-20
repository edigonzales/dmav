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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Splitter {

    private final ModelLoader modelLoader;

    public Splitter() {
        this(new ModelLoader());
    }

    Splitter(ModelLoader modelLoader) {
        this.modelLoader = modelLoader;
    }

    public boolean run(Path inputFile, String fosnr, Path outputDir) {
        try {
            split(inputFile, fosnr, outputDir);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void split(Path inputFile, String fosnr, Path outputDir) throws Exception {
        Files.createDirectories(outputDir);

        IoxReader reader = new ReaderFactory().createReader(inputFile.toFile(), null, new Settings());
        Map<String, XtfWriter> writers = new LinkedHashMap<>();

        try {
            IoxEvent firstEvent = reader.read();
            if (!(firstEvent instanceof StartTransferEvent)) {
                throw new IoxException("Expected StartTransferEvent in " + inputFile);
            }
            if (!(reader instanceof IoxIliReader)) {
                throw new IoxException("Input is not an INTERLIS transfer file: " + inputFile);
            }

            StartTransferEvent sourceTransfer = (StartTransferEvent) firstEvent;
            List<String> modelNames = extractModelNames(firstEvent);
            TransferDescription td = modelLoader.compileModels(modelNames);
            ((IoxIliReader) reader).setModel(td);

            XtfWriter currentWriter = null;
            boolean endTransferSeen = false;

            IoxEvent event;
            while ((event = reader.read()) != null) {
                if (event instanceof StartBasketEvent) {
                    StartBasketEvent startBasket = (StartBasketEvent) event;
                    String modelName = modelName(startBasket.getType());
                    currentWriter = writers.get(modelName);
                    if (currentWriter == null) {
                        Path outputFile = outputDir.resolve(modelName + "." + fosnr + ".xtf");
                        currentWriter = new XtfWriter(outputFile.toFile(), td);
                        currentWriter.write(new ch.interlis.iox_j.StartTransferEvent(
                                sourceTransfer.getSender(), sourceTransfer.getComment()));
                        writers.put(modelName, currentWriter);
                    }
                    currentWriter.write(event);
                } else if (event instanceof ObjectEvent) {
                    requireBasketWriter(currentWriter, inputFile);
                    currentWriter.write(event);
                } else if (event instanceof EndBasketEvent) {
                    requireBasketWriter(currentWriter, inputFile);
                    currentWriter.write(event);
                    currentWriter = null;
                } else if (event instanceof EndTransferEvent) {
                    if (currentWriter != null) {
                        throw new IoxException("Transfer ended inside a basket in " + inputFile);
                    }
                    endTransferSeen = true;
                    break;
                }
            }

            if (!endTransferSeen) {
                throw new IoxException("Missing EndTransferEvent in " + inputFile);
            }

            for (XtfWriter writer : writers.values()) {
                writer.write(new ch.interlis.iox_j.EndTransferEvent());
            }
        } finally {
            closeWriters(writers);
            reader.close();
        }
    }

    private static List<String> extractModelNames(IoxEvent event) throws IoxException {
        if (!(event instanceof XtfStartTransferEvent)) {
            throw new IoxException("XTF header does not expose its INTERLIS models");
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
            throw new IoxException("No INTERLIS models found in XTF header");
        }
        return modelNames;
    }

    private static String modelName(String basketType) throws IoxException {
        int separator = basketType.indexOf('.');
        if (separator <= 0) {
            throw new IoxException("Invalid basket type: " + basketType);
        }
        return basketType.substring(0, separator);
    }

    private static void requireBasketWriter(XtfWriter writer, Path inputFile) throws IoxException {
        if (writer == null) {
            throw new IoxException("Object or basket end outside a basket in " + inputFile);
        }
    }

    private static void closeWriters(Map<String, XtfWriter> writers) throws IoxException {
        IoxException firstFailure = null;
        for (XtfWriter writer : writers.values()) {
            try {
                writer.close();
            } catch (IoxException e) {
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }
}
