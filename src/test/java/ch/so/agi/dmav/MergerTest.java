package ch.so.agi.dmav;

import org.interlis2.validator.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import ch.ehi.basics.settings.Settings;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MergerTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8181);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // Lokale Zip-Datei
    @Test
    public void mergeLocalZipFileOk(@TempDir Path tempDir) throws IOException {
        // Run test
        Merger merger = new Merger();
        boolean ret = merger.run(Paths.get("src/test/data/merger/myconfig_local_zip.ini"), "449", tempDir);

        // Validate result
        assertTrue(ret);

        Path xtfFile = tempDir.resolve("DMAV.449.xtf");
        Settings settings = new Settings();
        Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
        settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());

        assertNoLocalNamespaceDeclarations(xtfFile);

        boolean valid = Validator.runValidation(xtfFile.toString(), settings);
        assertFalse(valid);

        String content = Files.readString(logFile);
        if (content.contains("model(s) not found")) {
            Assumptions.assumeTrue(false, "INTERLIS models not available for validation.");
            return;
        }
        assertTrue(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
        assertTrue(content.contains("1 objects in CLASS OfficialIndexOfLocalities_V1_0.OfficialIndexOfLocalities.Locality"));
        assertTrue(content.contains("OfficialIndexOfLocalities_V1_0.OfficialIndexOfLocalities.Locality: tid BB74A1B6-3A4F-4A76-9B6C-466F10630ABA: ZIP should associate 1 to * target objects (instead of 0)"));
    }
    
    // Externe Zip-Datei
    @Test
    public void mergeExternalZipFileOk(@TempDir Path tempDir) throws IOException {
        // Prepare
        byte[] fileContent = Files.readAllBytes(Paths.get("src/test/data/merger/fixpunkte_v1_1_SO_lv95.zip"));

        mockWebServer.enqueue(new MockResponse()
                .setBody(new Buffer().write(fileContent)) 
                .addHeader("Content-Type", "application/zip")
                .setResponseCode(200));

        // Run test
        Merger merger = new Merger();
        boolean ret = merger.run(Paths.get("src/test/data/merger/myconfig_web_zip.ini"), "449", tempDir);

        // Validate result
        assertTrue(ret);

        Path xtfFile = tempDir.resolve("DMAV.449.xtf");
        Settings settings = new Settings();
        Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
        settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());

        assertNoLocalNamespaceDeclarations(xtfFile);
        boolean valid = Validator.runValidation(xtfFile.toString(), settings);
        String content = Files.readString(logFile);
        if (!valid) {
            if (content.contains("model(s) not found")) {
                Assumptions.assumeTrue(false, "INTERLIS models not available for validation.");
                return;
            } else {
                fail("Validation failed: " + content);
            }
        }

        assertTrue(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
        assertTrue(content.contains("3890 objects in CLASS KGKCGC_FPDS2_V1_1.FPDS2.FixpunktVersion"));
        assertTrue(content.contains("548 objects in CLASS KGKCGC_FPDS2_V1_1.FPDS2.Fixpunkt"));
    }

    
    // Multiple: verschiedene Dateien zum gleichen Modell (z.B. FixpunkteLV HFP1 und LFP1)
    @Test
    public void mergeMultipleLocalFilesOk(@TempDir Path tempDir) throws IOException {
        // Run test
        Merger merger = new Merger();
        boolean ret = merger.run(Paths.get("src/test/data/merger/myconfig_local_multiple.ini"), "449", tempDir);

        // Validate result
        assertTrue(ret);

        Path xtfFile = tempDir.resolve("DMAV.449.xtf");
        Settings settings = new Settings();
        Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
        settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());

        assertNoLocalNamespaceDeclarations(xtfFile);
        boolean valid = Validator.runValidation(xtfFile.toString(), settings);
        String content = Files.readString(logFile);
        if (!valid) {
            if (content.contains("model(s) not found")) {
                Assumptions.assumeTrue(false, "INTERLIS models not available for validation.");
                return;
            } else {
                fail("Validation failed: " + content);
            }
        }

        assertTrue(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
        assertTrue(content.contains("3 objects in CLASS FixpunkteLV_V1_0.FixpunkteLV.HFP1"));
        assertTrue(content.contains("3 objects in CLASS FixpunkteLV_V1_0.FixpunkteLV.LFP1"));
    }

    @Test
    public void mergeLocalFilesOk(@TempDir Path tempDir) throws IOException {
        // Run test
        Merger merger = new Merger();
        //Path myTempDir = Path.of("/Users/stefan/tmp/");
        boolean ret = merger.run(Paths.get("src/test/data/merger/myconfig_local.ini"), "449", tempDir);
    
        // Validate result
        assertTrue(ret);
        
        Path xtfFile = tempDir.resolve("DMAV.449.xtf");
        Settings settings = new Settings();
        Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
        settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());

        assertNoLocalNamespaceDeclarations(xtfFile);
        boolean valid = Validator.runValidation(xtfFile.toString(), settings);
        String content = Files.readString(logFile);
        if (!valid) {
            if (content.contains("model(s) not found")) {
                Assumptions.assumeTrue(false, "INTERLIS models not available for validation.");
                return;
            } else {
                fail("Validation failed: " + content);
            }
        }

        assertTrue(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
        assertTrue(content.contains("1 objects in CLASS DMAV_HoheitsgrenzenAV_V1_0.HoheitsgrenzenAV.Gemeindegrenze"));
    }
    
    @Test
    public void mergeExternalFilesOk(@TempDir Path tempDir) throws IOException {
        // Prepare
        byte[] fileContent = Files.readAllBytes(Paths.get("src/test/data/merger/DMAV_FixpunkteAVKategorie3_V1_0.449.xtf"));

        mockWebServer.enqueue(new MockResponse()
                .setBody(new String(fileContent)) 
                .addHeader("Content-Type", "application/xml")
                .setResponseCode(200));
        
        // Run test
        Merger merger = new Merger();
        boolean ret = merger.run(Paths.get("src/test/data/merger/myconfig_web.ini"), "449", tempDir);

        // Validate result
        assertTrue(ret);
        
        Path xtfFile = tempDir.resolve("DMAV.449.xtf");
        Settings settings = new Settings();
        Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
        settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());

        assertNoLocalNamespaceDeclarations(xtfFile);
        boolean valid = Validator.runValidation(xtfFile.toString(), settings);
        String content = Files.readString(logFile);
        if (!valid) {
            if (content.contains("model(s) not found")) {
                Assumptions.assumeTrue(false, "INTERLIS models not available for validation.");
                return;
            } else {
                fail("Validation failed: " + content);
            }
        }

        assertTrue(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
        assertTrue(content.contains("1 objects in CLASS DMAV_HoheitsgrenzenAV_V1_0.HoheitsgrenzenAV.Gemeindegrenze"));
    }

    private void assertNoLocalNamespaceDeclarations(Path xtfFile) {
        try (InputStream inputStream = Files.newInputStream(xtfFile)) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            Element root = document.getDocumentElement();
            NodeList elements = document.getElementsByTagName("*");

            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if (element == root) {
                    continue;
                }

                NamedNodeMap attributes = element.getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    Attr attribute = (Attr) attributes.item(j);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                        fail("Element " + element.getTagName() + " contains namespace declaration " + attribute.getName());
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            fail("Failed to inspect namespaces: " + e.getMessage());
        }
    }
}
