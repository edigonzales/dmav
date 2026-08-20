package ch.so.agi.dmav;

import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.Model;
import ch.interlis.ili2c.metamodel.Topic;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ilirepository.IliManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class ModelLoader {

    static final String CURRENT_DMAV_MODEL = "DMAVTYM_Alles_V1_1";
    static final String LEGACY_DMAV_MODEL = "DMAVTYM_Alles_V1_0";

    private static final String CURRENT_SHARED_V1_0_MODEL = "DMAV_HoheitsgrenzenAV_V1_0";

    private static final String[] DEFAULT_REPOSITORIES = {
        "https://models.geo.admin.ch",
        "https://models.interlis.ch"
    };

    /*
     * Compiling the DMAV umbrella models is expensive. A TransferDescription is
     * treated as read-only after ili2c returns it, so successful compilations can
     * safely be reused by subsequent Splitter/Merger instances in the same JVM.
     *
     * The repository list and the ordered top-level model list are both part of
     * the key. Model order is significant because XtfWriter uses the last model
     * in the TransferDescription to determine transfer properties.
     */
    private static final ConcurrentMap<CacheKey, CompletableFuture<TransferDescription>> MODEL_CACHE =
            new ConcurrentHashMap<>();

    private final String[] repositories;
    private final List<String> repositoryKey;

    ModelLoader() {
        this(DEFAULT_REPOSITORIES);
    }

    ModelLoader(String... repositories) {
        this.repositories = repositories.clone();
        this.repositoryKey = List.of(this.repositories.clone());
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

    String selectDmavModelForSources(Collection<String> sourceKeys) {
        for (String key : sourceKeys) {
            if (isDmavModelVersion(key, "_V1_1")) {
                return CURRENT_DMAV_MODEL;
            }
        }
        for (String key : sourceKeys) {
            if (isDmavModelVersion(key, "_V1_0") && !CURRENT_SHARED_V1_0_MODEL.equals(key)) {
                return LEGACY_DMAV_MODEL;
            }
        }
        return CURRENT_DMAV_MODEL;
    }

    static Map<String, List<String>> directTransferTopics(
            TransferDescription td, String umbrellaModelName) {
        Model umbrella = findModel(td, umbrellaModelName);
        if (umbrella == null) {
            throw new IllegalArgumentException("Model not found: " + umbrellaModelName);
        }

        Map<String, List<String>> topicsByModel = new LinkedHashMap<>();
        for (Model importedModel : umbrella.getImporting()) {
            List<String> topics = new ArrayList<>();
            Iterator<Element> elements = importedModel.iterator();
            while (elements.hasNext()) {
                Element element = elements.next();
                if (element instanceof Topic) {
                    Topic topic = (Topic) element;
                    if (!topic.isAbstract() && !topic.isViewTopic()) {
                        topics.add(topic.getScopedName(null));
                    }
                }
            }
            if (!topics.isEmpty()) {
                topicsByModel.put(importedModel.getName(), topics);
            }
        }
        return topicsByModel;
    }

    TransferDescription compileModels(Collection<String> modelNames) throws Ili2cFailure {
        List<String> normalizedModelNames = normalizeModelNames(modelNames);
        if (normalizedModelNames.isEmpty()) {
            throw new Ili2cFailure("No INTERLIS models specified");
        }

        CacheKey cacheKey = new CacheKey(repositoryKey, normalizedModelNames);
        CompletableFuture<TransferDescription> newCompilation = new CompletableFuture<>();
        CompletableFuture<TransferDescription> compilation =
                MODEL_CACHE.putIfAbsent(cacheKey, newCompilation);

        if (compilation == null) {
            compilation = newCompilation;
            try {
                TransferDescription td = compileModelsUncached(normalizedModelNames);
                newCompilation.complete(td);
            } catch (Ili2cFailure e) {
                newCompilation.completeExceptionally(e);
                MODEL_CACHE.remove(cacheKey, newCompilation);
                throw e;
            } catch (RuntimeException e) {
                newCompilation.completeExceptionally(e);
                MODEL_CACHE.remove(cacheKey, newCompilation);
                throw e;
            }
        }

        try {
            return compilation.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Ili2cFailure) {
                throw (Ili2cFailure) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw e;
        }
    }

    private TransferDescription compileModelsUncached(List<String> modelNames) throws Ili2cFailure {
        IliManager manager = new IliManager();
        manager.setRepositories(repositories);

        ArrayList<String> entries = new ArrayList<>(modelNames);
        Configuration config;
        try {
            config = manager.getConfigWithFiles(entries, null, 0.0);
        } catch (Ili2cException e) {
            throw new Ili2cFailure(e);
        }

        if (config == null) {
            throw new Ili2cFailure("Failed to create compiler configuration for models: " + modelNames);
        }

        Ili2cSettings settings = new Ili2cSettings();
        ch.interlis.ili2c.Main.setDefaultIli2cPathMap(settings);
        settings.setIlidirs(String.join(";", repositories));

        TransferDescription td = ch.interlis.ili2c.Main.runCompiler(config, settings);
        if (td == null) {
            throw new Ili2cFailure("Failed to compile models: " + modelNames);
        }
        return td;
    }

    private static List<String> normalizeModelNames(Collection<String> modelNames) {
        Set<String> uniqueNames = new LinkedHashSet<>();
        for (String modelName : modelNames) {
            if (modelName != null && !modelName.isBlank()) {
                uniqueNames.add(modelName.trim());
            }
        }
        return List.copyOf(uniqueNames);
    }

    private static Model findModel(TransferDescription td, String modelName) {
        Iterator<Model> models = td.iterator();
        while (models.hasNext()) {
            Model model = models.next();
            if (modelName.equals(model.getName())) {
                return model;
            }
        }
        return null;
    }

    private static boolean containsDmavVersion(Collection<String> modelNames, String suffix) {
        for (String modelName : modelNames) {
            if (isDmavModelVersion(modelName, suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDmavModelVersion(String modelName, String suffix) {
        return modelName != null && modelName.startsWith("DMAV") && modelName.endsWith(suffix);
    }

    private static final class CacheKey {
        private final List<String> repositories;
        private final List<String> modelNames;

        private CacheKey(List<String> repositories, List<String> modelNames) {
            this.repositories = List.copyOf(repositories);
            this.modelNames = List.copyOf(modelNames);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheKey)) {
                return false;
            }
            CacheKey that = (CacheKey) other;
            return repositories.equals(that.repositories) && modelNames.equals(that.modelNames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repositories, modelNames);
        }
    }
}
