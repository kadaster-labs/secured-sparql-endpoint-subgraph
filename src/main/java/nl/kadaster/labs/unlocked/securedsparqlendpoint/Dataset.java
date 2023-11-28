package nl.kadaster.labs.unlocked.securedsparqlendpoint;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.GlobUtil;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class Dataset {
    public static final String AUTHORISATION_ONTOLOGY_INFIX = ".auth";

    private static DatasetGraph load(Path path) {
        var dataset = TDBFactory.createDataset().asDatasetGraph();
        dataset.addAll(RDFDataMgr.loadDataset(path.toString()).asDatasetGraph());
        return dataset;
    }

    public final String name;
    public final Optional<DatasetGraph> accessOntology;
    public final DatasetGraph dataOntology;

    public Dataset(Path path) throws IOException {
        this.dataOntology = load(path);

        var filename = path.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        this.name = filename;

        this.accessOntology = GlobUtil
                .findFirst(path.getParent(), this.name + AUTHORISATION_ONTOLOGY_INFIX + ".*")
                .map(Dataset::load);

        if (this.accessOntology.isEmpty()) {
            log.warn("Ontology [{}] has no authorisation policy", filename);
        }
    }
}
