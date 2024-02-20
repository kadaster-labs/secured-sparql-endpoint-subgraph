package nl.kadaster.labs.unlocked.securedsparqlendpoint;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.GlobUtil;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Dataset {
    public static final String AUTHORISATION_ONTOLOGY_INFIX = ".auth";
    public static final Model EMPTY_MODEL = ModelFactory.createDefaultModel();
    public static final DatasetGraph OPEN_AUTHORISATION_POLICY = Dataset.load(Path.of("data/open.auth.ttl"));
    public static final Query RULES_QUERY = QueryFactory.create("""
            PREFIX auth: <https://data.federatief.datastelsel.nl/lock-unlock/authorisation/model/def/>
            SELECT DISTINCT ?rule ?subject ?condition WHERE {
                ?rule a auth:AccessRule .
                ?rule auth:condition ?condition.
                ?rule auth:subject ?subject.
            }
            """);

    private static DatasetGraph load(Path path) {
        var dataset = TDBFactory.createDataset().asDatasetGraph();
        dataset.addAll(RDFDataMgr.loadDataset(path.toString()).asDatasetGraph());
        return dataset;
    }

    public final String name;
    private final Map<String, Model> accessRules = new HashMap<>();
    public final DatasetGraph accessOntology;
    public final Model logOntology = ModelFactory.createDefaultModel();

    public Dataset(Path path) throws IOException {
        var filename = path.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        this.name = filename;

        log.info("Loading ontology [{}]", this.name);
        var dataOntology = load(path);

        var authPath = GlobUtil.findFirst(path.getParent(), this.name + AUTHORISATION_ONTOLOGY_INFIX + ".*");
        if (authPath.isPresent()) {
            this.accessOntology = Dataset.load(authPath.get());
        } else {
            log.warn("Ontology [{}] has no authorisation policy, using open authorisation policy", this.name);
            this.accessOntology = OPEN_AUTHORISATION_POLICY;
        }
        log.info("Loaded ontology [{}] containing {} triples", this.name, dataOntology.getUnionGraph().size());

        try (QueryExecution rules = QueryExecutionFactory.create(RULES_QUERY, this.accessOntology)) {
            rules.execSelect().forEachRemaining(result -> {
                var rule = result.getResource("rule").getURI();
                log.info("Evaluating access rule {}", rule);
                var constructQuery = QueryFactory.create("CONSTRUCT {$subject} WHERE {GRAPH ?g {$subject $condition}}"
                        .replace("$subject", result.getLiteral("subject").getString())
                        .replace("$condition", result.getLiteral("condition").getString())
                );

                try (QueryExecution execution = QueryExecutionFactory.create(constructQuery, dataOntology)) {
                    var model = execution.execConstruct();
                    log.info("Access rule {} yields {} triples", rule, model.size());
                    accessRules.put(rule, model);
                }
            });
        }
        log.info("Evaluated all access rules for [{}]", this.name);
    }

    public Model getSubset(String ruleURI) {
        return this.accessRules.getOrDefault(ruleURI, EMPTY_MODEL);
    }
}
