package nl.kadaster.labs.unlocked.securedsparqlendpoint.repositories;

import nl.kadaster.labs.unlocked.securedsparqlendpoint.Dataset;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.GlobUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class DatasetRepository extends Repository<Dataset> {
    public DatasetRepository() throws IOException {
        for (Path path : GlobUtil.findAll(Path.of("./data/"), "*")) {
            if (path.toString().contains(Dataset.AUTHORISATION_ONTOLOGY_INFIX)) continue;
            var dataset = new Dataset(path);
            this.entries.put(dataset.name, dataset);
        }
    }
}

