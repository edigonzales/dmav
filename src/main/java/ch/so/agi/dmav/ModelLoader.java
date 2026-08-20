package ch.so.agi.dmav;

import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ilirepository.IliManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ModelLoader {

    static final String CURRENT_DMAV_MODEL = "DMAVTYM_Alles_V1_1";
    static final String LEGACY_DMAV_MODEL = "DMAVTYM_Alles_V1_0";

    private static final String[] DEFAULT_REPOSITORIES = {
        "https://models.geo.admin.ch",
        "https://models.interlis.ch"
    };

    private final String[] repositories;

    ModelLoader() {
        this(DEFAULT_REPOSITORIES);
    }

    ModelLoader(String... repositories) {
        this.repositories = repositories.clone();
    }

    TransferDescription compileCurrentDmav() throws Ili2cFailure {
        return compileModels(List.of(CURRENT_DMAV_MODEL));
    }

    TransferDescription compileForTransferModels(Collection<String> modelNames) throws Ili2cFailure {
        if (containsDmavVersion(modelNames, "_V1_1")) {
            return compileCurrentDmav();
        }
        if (containsDmavVersion(modelNames, "_V1_0")) {
            return compileModels(List.of(LEGACY_DMAV_MODEL));
        }
        return compileModels(modelNames);
    }

    TransferDescription compileModels(Collection<String> modelNames) throws Ili2cFailure {
        Set<String> uniqueNames = new LinkedHashSet<>();
        for (String modelName : modelNames) {
            if (modelName != null && !modelName.isBlank()) {
                uniqueNames.add(modelName.trim());
            }
        }
        if (uniqueNames.isEmpty()) {
            throw new Ili2cFailure("No INTERLIS models specified");
        }

        IliManager manager = new IliManager();
        manager.setRepositories(repositories);

        ArrayList<String> entries = new ArrayList<>(uniqueNames);
        Configuration config;
        try {
            config = manager.getConfigWithFiles(entries, null, 0.0);
        } catch (Ili2cException e) {
            throw new Ili2cFailure(e);
        }

        if (config == null) {
            throw new Ili2cFailure("Failed to create compiler configuration for models: " + uniqueNames);
        }

        Ili2cSettings settings = new Ili2cSettings();
        ch.interlis.ili2c.Main.setDefaultIli2cPathMap(settings);
        settings.setIlidirs(String.join(";", repositories));

        TransferDescription td = ch.interlis.ili2c.Main.runCompiler(config, settings);
        if (td == null) {
            throw new Ili2cFailure("Failed to compile models: " + uniqueNames);
        }
        return td;
    }

    private static boolean containsDmavVersion(Collection<String> modelNames, String suffix) {
        for (String modelName : modelNames) {
            if (modelName != null && modelName.startsWith("DMAV") && modelName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
