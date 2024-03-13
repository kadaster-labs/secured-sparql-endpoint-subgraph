package nl.kadaster.labs.unlocked.securedsparqlendpoint.endpoints;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.logging.LogEvent;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.repositories.DatasetRepository;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.IOUtil;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
public class SparqlEndpoint {
    private static final String ACCESS_RULES_QUERY = """
            PREFIX auth: <https://data.federatief.datastelsel.nl/lock-unlock/authorisation/model/def/>
                        
            SELECT DISTINCT ?rule ?subject ?condition WHERE {
              ?rule a auth:AccessRule .
              <https://labs.kadaster.nl/unlocked/data/$dataset.auth#$persona> auth:extends*/auth:has_rule ?rule
            }
            """;
    private static final String DEFAULT_USER = "h_de_vries";

    @Autowired
    private DatasetRepository datasets;

    @GetMapping(value = "/{dataset}/sparql")
    public String queryGet(@PathVariable("dataset") String datasetName, @RequestParam("query") String queryString, @RequestParam(required = false) String persona) {
        return this.query(datasetName, queryString, persona);
    }

    @PostMapping(value = "/{dataset}/sparql",
            consumes = "application/x-www-form-urlencoded"
    )
    public String queryInBody(@PathVariable("dataset") String datasetName, @RequestParam("query") String queryString, @RequestParam(required = false) String persona) {
        return this.query(datasetName, queryString, persona);
    }

    @PostMapping(value = "/{dataset}/sparql",
            consumes = "application/sparql-query"
    )
    public String queryPlain(@PathVariable("dataset") String datasetName, @RequestBody String queryString, @RequestParam(required = false) String persona) {
        return this.query(datasetName, queryString, persona);
    }

    private String query(String datasetName, String queryString, String persona) {
        var dataset = this.datasets.get(this.extractDatasetName(datasetName))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));

        LogEvent event = new LogEvent(dataset);
        event.addDetail("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/endpoint", datasetName);
        event.addDetail("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/sparqlquery", queryString);

        if (persona == null) {
            persona = DEFAULT_USER;
        } else {
            event.addDetail("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/by_user", persona);
        }

        // TODO log amount of results
        // TODO log has results
        // TODO log is filtered

        try {
            var query = QueryFactory.create(queryString);
            event.addDetail(query.queryType());
            if (query.isUnknownType()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown query type");

            var modelName = this.extractModelName(datasetName);
            if (modelName == null) {
                var accessRulesQuery = QueryFactory.create(ACCESS_RULES_QUERY
                        .replace("$persona", persona)
                        .replace("$dataset", dataset.name)
                );
                try (QueryExecution accessRules = QueryExecutionFactory.create(accessRulesQuery, dataset.metaOntology)) {
                    var subset = ModelFactory.createDefaultModel();
                    accessRules.execSelect().forEachRemaining(result -> {
                        var rule = result.getResource("rule").getURI();
                        var model = dataset.getSubset(rule);
                        subset.add(model);
                        event.addDetail("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/filtered_by", rule);
                        log.info("Access rule {} yields {} triples", rule, model.size());
                    });
                    try (QueryExecution execution = QueryExecutionFactory.create(query, subset)) {
                        return this.buildResponse(execution);
                    }
                }
            } else if (modelName.equals("auth")) {
                try (QueryExecution execution = QueryExecutionFactory.create(query, dataset.metaOntology)) {
                    return this.buildResponse(execution);
                }
            } else if (modelName.equals("log")) {
                try (QueryExecution execution = QueryExecutionFactory.create(query, dataset.logOntology)) {
                    return this.buildResponse(execution);
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Model does not exist");
            }
        } catch (QueryParseException exception) {
            log.info("Query cannot be parsed");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid query");
        } finally {
            event.conclude();
        }
    }

    private String buildResponse(QueryExecution execution) {
        var query = execution.getQuery();
        if (query.isAskType()) {
            return """
                    <?xml version="1.0"?>
                    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                    <head></head>
                    <boolean>$result</boolean>
                    </sparql>""".replace("$result", execution.execAsk() ? "true" : "false");
        } else if (query.isSelectType()) {
            return IOUtil.capture(output -> ResultSetFormatter.outputAsJSON(output, execution.execSelect()));
        }

        var writer = RDFWriter.create();

        // TODO negotiate language
        writer.lang(Lang.TRIG);

        if (query.isConstructType()) {
            writer.source(execution.execConstruct());
        } else if (query.isDescribeType()) {
            writer.source(execution.execDescribe());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Query method isn't implemented yet");
        }

        return IOUtil.capture(writer::output);
    }

    private String extractDatasetName(String name) {
        var idx = name.indexOf('.');
        return idx == -1 ? name : name.substring(0, idx);
    }

    private String extractModelName(String name) {
        var idx = name.indexOf('.');
        return idx == -1 ? null : name.substring(idx + 1);
    }
}
