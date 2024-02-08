package nl.kadaster.labs.unlocked.securedsparqlendpoint.logging;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.Dataset;
import org.apache.jena.query.QueryType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Slf4j
public class LogEvent {
    private static final Map<String, String> staticData;
    private static final Map<QueryType, String> queryTypes = Map.of(
            QueryType.CONSTRUCT, "https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/SparqlConstruct",
            QueryType.DESCRIBE, "https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/SparqlDescribeEvent",
            QueryType.SELECT, "https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/SparqlSelectEvent"
    );

    static {
        Map<String, String> data = new HashMap<>();
        data.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/LogEvent");
        data.put("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/processId", String.valueOf(ProcessHandle.current().pid()));

        try {
            data.put("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/serverID", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException exception) {
            log.warn("Failed to establish server ID", exception);
        }

        staticData = Collections.unmodifiableMap(data);
    }

    private final Model graph;
    public final Resource node;

    public LogEvent(Dataset dataset) {
        this.graph = dataset.logOntology;
        this.node = this.graph.createResource();
        LogEvent.staticData.forEach(this::addDetail);
        this.addDetail("https://data.federatief.datastelsel.nl/lock-unlock/logging/model/def/startDate", new Date());
    }

    public void addDetail(String predicate, String object) {
        this.graph.add(this.node, this.graph.createProperty(predicate), object);
    }

    public void addDetail(QueryType type) {
        String uri = LogEvent.queryTypes.get(type);
        if (uri != null) {
            this.addDetail("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", uri);
        }
    }

    public void addDetail(String predicate, Date object) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.addDetail(predicate, df.format(object));
    }
}
