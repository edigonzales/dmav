package ch.so.agi.dmav;

import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox_j.IoxIliReader;
import ch.interlis.iox_j.utility.ReaderFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class IoxTestSupport {

    private IoxTestSupport() {
    }

    static TransferDescription compileModel(String modelName, String... repositories) throws Ili2cFailure {
        return new ModelLoader(repositories).compileModels(List.of(modelName));
    }

    static TransferSnapshot read(Path path, TransferDescription td) throws IoxException {
        return read(path, td, new String[0]);
    }

    static TransferSnapshot read(Path path, TransferDescription td, String... topicFilter) throws IoxException {
        IoxReader reader = new ReaderFactory().createReader(path.toFile(), null);
        if (reader instanceof IoxIliReader) {
            IoxIliReader iliReader = (IoxIliReader) reader;
            iliReader.setModel(td);
            if (topicFilter != null && topicFilter.length > 0) {
                iliReader.setTopicFilter(topicFilter);
            }
        }

        TransferSnapshot transfer = new TransferSnapshot();
        BasketSnapshot currentBasket = null;

        try {
            IoxEvent event;
            while ((event = reader.read()) != null) {
                if (event instanceof StartBasketEvent) {
                    StartBasketEvent startBasket = (StartBasketEvent) event;
                    currentBasket = new BasketSnapshot(startBasket.getType(), startBasket.getBid());
                    transfer.baskets.add(currentBasket);
                } else if (event instanceof ObjectEvent) {
                    if (currentBasket == null) {
                        throw new IllegalStateException("Object event outside a basket in " + path);
                    }
                    ObjectEvent objectEvent = (ObjectEvent) event;
                    currentBasket.objects.add(snapshot(objectEvent.getIomObject()));
                } else if (event instanceof EndBasketEvent) {
                    currentBasket = null;
                } else if (event instanceof EndTransferEvent) {
                    break;
                }
            }
        } finally {
            reader.close();
        }

        return transfer;
    }

    private static ObjectSnapshot snapshot(IomObject object) {
        Map<String, List<String>> references = new LinkedHashMap<>();
        collectReferences(object, "", references);
        return new ObjectSnapshot(object.getobjecttag(), object.getobjectoid(), references);
    }

    private static void collectReferences(
            IomObject object, String path, Map<String, List<String>> references) {
        for (int attrIndex = 0; attrIndex < object.getattrcount(); attrIndex++) {
            String attrName = object.getattrname(attrIndex);
            if (attrName == null) {
                continue;
            }

            int valueCount;
            try {
                valueCount = object.getattrvaluecount(attrName);
            } catch (RuntimeException e) {
                valueCount = 0;
            }

            for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                IomObject child;
                try {
                    child = object.getattrobj(attrName, valueIndex);
                } catch (RuntimeException e) {
                    child = null;
                }

                if (child == null) {
                    continue;
                }

                String attrPath = path.isEmpty() ? attrName : path + "." + attrName;
                String referenceOid = child.getobjectrefoid();
                if (referenceOid != null) {
                    references.computeIfAbsent(attrPath, ignored -> new ArrayList<>()).add(referenceOid);
                } else {
                    collectReferences(child, attrPath, references);
                }
            }
        }
    }

    static final class TransferSnapshot {
        private final List<BasketSnapshot> baskets = new ArrayList<>();

        List<BasketSnapshot> getBaskets() {
            return Collections.unmodifiableList(baskets);
        }

        BasketSnapshot basket(String type) {
            for (BasketSnapshot basket : baskets) {
                if (type.equals(basket.type)) {
                    return basket;
                }
            }
            return null;
        }

        Set<String> allTids() {
            Set<String> tids = new LinkedHashSet<>();
            for (BasketSnapshot basket : baskets) {
                tids.addAll(basket.tids());
            }
            return tids;
        }

        Set<String> allReferenceTargets() {
            Set<String> refs = new LinkedHashSet<>();
            for (BasketSnapshot basket : baskets) {
                for (ObjectSnapshot object : basket.objects) {
                    for (List<String> values : object.references.values()) {
                        refs.addAll(values);
                    }
                }
            }
            return refs;
        }
    }

    static final class BasketSnapshot {
        private final String type;
        private final String bid;
        private final List<ObjectSnapshot> objects = new ArrayList<>();

        BasketSnapshot(String type, String bid) {
            this.type = type;
            this.bid = bid;
        }

        String getType() {
            return type;
        }

        String getBid() {
            return bid;
        }

        List<ObjectSnapshot> getObjects() {
            return Collections.unmodifiableList(objects);
        }

        ObjectSnapshot object(String tid) {
            for (ObjectSnapshot object : objects) {
                if (tid.equals(object.tid)) {
                    return object;
                }
            }
            return null;
        }

        Set<String> tids() {
            Set<String> tids = new LinkedHashSet<>();
            for (ObjectSnapshot object : objects) {
                if (object.tid != null) {
                    tids.add(object.tid);
                }
            }
            return tids;
        }

        Map<String, Set<String>> tidsByTag() {
            Map<String, Set<String>> result = new LinkedHashMap<>();
            for (ObjectSnapshot object : objects) {
                result.computeIfAbsent(object.tag, ignored -> new LinkedHashSet<>());
                if (object.tid != null) {
                    result.get(object.tag).add(object.tid);
                }
            }
            return result;
        }

        Map<String, Map<String, List<String>>> referencesByTid() {
            Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
            for (ObjectSnapshot object : objects) {
                if (object.tid != null && !object.references.isEmpty()) {
                    result.put(object.tid, object.references);
                }
            }
            return result;
        }
    }

    static final class ObjectSnapshot {
        private final String tag;
        private final String tid;
        private final Map<String, List<String>> references;

        ObjectSnapshot(String tag, String tid, Map<String, List<String>> references) {
            this.tag = tag;
            this.tid = tid;
            this.references = references;
        }

        String getTag() {
            return tag;
        }

        String getTid() {
            return tid;
        }

        Map<String, List<String>> getReferences() {
            return Collections.unmodifiableMap(references);
        }
    }
}
