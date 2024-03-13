package nl.kadaster.labs.unlocked.securedsparqlendpoint;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.ZipUtil;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb.TDBFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipInputStream;

@Slf4j
public class Dataset {
    private static final Set<String> IGNORED_ENTRIES = Set.of("auth.trig", "data_examplequeries.trig", "users.trig");
    public static final String AUTHORISATION_ONTOLOGY_INFIX = ".auth";
    public static final Query DATAFILE_QUERY = QueryFactory.create("""
            PREFIX sse: <https://data.federatief.datastelsel.nl/lock-unlock/authorisation/model/def/>
            SELECT ?url WHERE { ?node sse:has_data_file ?url. }
            """);
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
    public final Model logOntology = ModelFactory.createDefaultModel();
    public final DatasetGraph metaOntology;

    public Dataset(Path path) throws IOException {
        var filename = path.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        this.name = filename;

        log.info("Loading ontology [{}]", this.name);
        var dataOntology = TDBFactory.createDataset().asDatasetGraph();
        this.metaOntology = load(path);
        try (QueryExecution exec = QueryExecutionFactory.create(DATAFILE_QUERY, this.metaOntology)) {
            var select = exec.execSelect();
            if (!select.hasNext()) {
                log.error("Dataset [{}] has no data file specified", this.name);
                throw new Error("Dataset has no data file specified");
            }

            var datafileUrl = new URL(select.next().getLiteral("url").toString());
            if (select.hasNext()) {
                log.error("Dataset [{}] has multiple data files specified", this.name);
                throw new Error("Dataset has multiple data files specified");
            }

            log.info("Downloading data for [{}] from [{}]", this.name, datafileUrl);
            HttpURLConnection connection = (HttpURLConnection) datafileUrl.openConnection();
            connection.setRequestMethod("GET");

            ZipInputStream archive = new ZipInputStream(connection.getInputStream());
            Map<String, String> files = ZipUtil.readFiles(archive);
            IGNORED_ENTRIES.forEach(files::remove);
            log.info("Data for [{}] consists of {} file(s), excluding ignored files", this.name, files.size());

            files.forEach((name, content) -> {
                log.info("Parsing [{}] into [{}]", name, this.name);
                RDFDataMgr.read(dataOntology, new ByteArrayInputStream(content.getBytes()), Lang.TRIG);
            });
        }

        log.info("Loaded ontology [{}] containing {} triples", this.name, dataOntology.getUnionGraph().size());

        try (QueryExecution rules = QueryExecutionFactory.create(RULES_QUERY, this.metaOntology)) {
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
        return Optional
                .ofNullable(this.accessRules.get(ruleURI))
                .orElseGet(ModelFactory::createDefaultModel);
    }
}
