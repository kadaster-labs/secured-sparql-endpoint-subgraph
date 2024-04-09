package nl.kadaster.labs.unlocked.securedsparqlendpoint.repositories;

import lombok.extern.slf4j.Slf4j;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.Dataset;
import nl.kadaster.labs.unlocked.securedsparqlendpoint.util.GlobUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
@Slf4j
public class DatasetRepository extends Repository<Dataset> {
    private static final Path DATA_FOLDER = Path.of("./data/");

    public DatasetRepository(ApplicationArguments appArgs) throws IOException {
        var args = appArgs.getSourceArgs();
        var file = args.length == 0 ? "*" : args[0];
        var paths = GlobUtil.findAll(DATA_FOLDER, file);

        log.info("Sourcing datasets from {}/{}, found {} files", DATA_FOLDER, file, paths.size());

        for (Path path : paths) {
            if (path.toString().contains(Dataset.AUTHORISATION_ONTOLOGY_INFIX)) continue;
            var dataset = new Dataset(path);
            this.entries.put(dataset.name, dataset);
        }
    }
}

