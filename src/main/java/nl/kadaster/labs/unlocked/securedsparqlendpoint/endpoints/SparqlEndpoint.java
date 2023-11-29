package nl.kadaster.labs.unlocked.securedsparqlendpoint.endpoints;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.repositories.DatasetRepository;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;

@Slf4j
@RestController
public class SparqlEndpoint {
    @Autowired
    private DatasetRepository datasets;

    @PostMapping(value = "/{dataset}/sparql",
            consumes = "application/x-www-form-urlencoded"
    )
    public String queryInBody(@PathVariable("dataset") String datasetName, @RequestParam("query") String queryString) {
        return this.query(datasetName, queryString);
    }

    @PostMapping(value = "/{dataset}/sparql",
            consumes = "application/sparql-query"
    )
    public String queryPlain(@PathVariable("dataset") String datasetName, @RequestBody String queryString) {
        return this.query(datasetName, queryString);
    }

    private String query(String datasetName, String queryString) {
        log.info("Query submitted [{}]", queryString.replace("\n", ""));

        var dataset = this.datasets.get(datasetName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));
        var access = dataset.accessOntology
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dataset has no authorisation policy defined"));
        var query = QueryFactory.create(queryString);
        if (query.isUnknownType()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown query type");

        var subgraph = ModelFactory.createDefaultModel();

        access.find(
                Node.ANY,
                Node.ANY,
                NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NodeFactory.createURI("https://labs.kadaster.nl/unlocked/securedsparqlendpoint/AccessRule")
        ).forEachRemaining(quad -> {
            var rule = quad.getSubject();
            var subject = access.find(Node.ANY, rule, NodeFactory.createURI("https://labs.kadaster.nl/unlocked/securedsparqlendpoint/subject"), Node.ANY).next().getObject().getLiteralValue().toString();
            var condition = access.find(Node.ANY, rule, NodeFactory.createURI("https://labs.kadaster.nl/unlocked/securedsparqlendpoint/condition"), Node.ANY).next().getObject().getLiteralValue().toString();
            var accessQuery = QueryFactory.create("CONSTRUCT {$subject} WHERE {$subject $condition}"
                    .replace("$subject", subject)
                    .replace("$condition", condition));
            try (QueryExecution execution = QueryExecutionFactory.create(accessQuery, dataset.dataOntology)) {
                var model = execution.execConstruct();
                log.info("Access rule {} yields {} triples", rule, model.size());
                subgraph.add(model);
            }
        });

        log.info("Accessible graph has {} triples", subgraph.size());

        try (QueryExecution execution = QueryExecutionFactory.create(query, subgraph)) {
            return this.buildResponse(execution);
        }
    }

    private String buildResponse(QueryExecution execution) {
        var query = execution.getQuery();
        if (query.isAskType()) {
            var result = execution.execAsk();
            return """
                    <?xml version="1.0"?>
                    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                    <head></head>
                    <boolean>$result</boolean>
                    </sparql>""".replace("$result", result ? "true" : "false");
        } else if (query.isConstructType()) {
            var result = execution.execConstruct();
            var output = new ByteArrayOutputStream();
            RDFWriter.create()
                    .source(result)
                    .lang(Lang.TRIG)
                    .output(output);

            return output.toString();
        } else if (query.isDescribeType()) {
            var result = execution.execDescribe();
            var output = new ByteArrayOutputStream();
            RDFWriter.create()
                    .source(result)
                    .lang(Lang.TRIG)
                    .output(output);

            return output.toString();
        } else if (query.isSelectType()) {
            var result = execution.execSelect();
            var output = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(output, result);
            return output.toString();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Query method isn't implemented yet");
        }
    }
}
