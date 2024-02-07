package nl.kadaster.labs.unlocked.securedsparqlendpoint;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.GlobUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class Dataset {
    public static final String AUTHORISATION_ONTOLOGY_INFIX = ".auth";
    public static final DatasetGraph OPEN_AUTHORISATION_POLICY = Dataset.load(Path.of("data/open.auth.ttl"));

    private static DatasetGraph load(Path path) {
        var dataset = TDBFactory.createDataset().asDatasetGraph();
        dataset.addAll(RDFDataMgr.loadDataset(path.toString()).asDatasetGraph());
        return dataset;
    }

    public final String name;
    public final DatasetGraph accessOntology;
    public final DatasetGraph dataOntology;
    public final Model logOntology = ModelFactory.createOntologyModel();

    public Dataset(Path path) throws IOException {
        this.dataOntology = load(path);

        var filename = path.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        this.name = filename;

        var authPath = GlobUtil.findFirst(path.getParent(), this.name + AUTHORISATION_ONTOLOGY_INFIX + ".*");
        if (authPath.isPresent()) {
            this.accessOntology = Dataset.load(authPath.get());
        } else {
            log.warn("Ontology [{}] has no authorisation policy, using open authorisation policy", this.name);
            this.accessOntology = OPEN_AUTHORISATION_POLICY;
        }
    }
}
