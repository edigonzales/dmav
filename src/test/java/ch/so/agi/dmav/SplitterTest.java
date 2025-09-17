package ch.so.agi.dmav;

import org.interlis2.validator.Validator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ehi.basics.settings.Settings;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SplitterTest {

    @Test
    public void splitOk(@TempDir Path tempDir) throws IOException {
        // Run test
        //tempDir = Paths.get("/Users/stefan/tmp/splitter/");
        Splitter splitter =  new Splitter();
        boolean ret = splitter.run(Paths.get("src/test/data/splitter/DMAV.449.xtf"), "449", tempDir);
        
        // Validate result
        assertTrue(ret);
        
        {
            Path xtfFile = tempDir.resolve("DMAV_FixpunkteAVKategorie3_V1_0.449.xtf");
            Settings settings = new Settings();
            Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
            settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());
            
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

            assertFalse(content.contains("1 objects in CLASS DMAV_HoheitsgrenzenAV_V1_0.HoheitsgrenzenAV.Gemeinde"));
            assertTrue(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
        }

        {
            Path xtfFile = tempDir.resolve("DMAV_HoheitsgrenzenAV_V1_0.449.xtf");
            Settings settings = new Settings();
            Path logFile = tempDir.resolve("ilivaliator.log").toAbsolutePath();
            settings.setValue(Validator.SETTING_LOGFILE, logFile.toString());
            
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

            assertFalse(content.contains("114 objects in CLASS DMAV_FixpunkteAVKategorie3_V1_0.FixpunkteAVKategorie3.LFP3"));
            assertTrue(content.contains("1 objects in CLASS DMAV_HoheitsgrenzenAV_V1_0.HoheitsgrenzenAV.Gemeinde"));
        }

    }
}
