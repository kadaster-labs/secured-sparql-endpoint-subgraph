package nl.kadaster.labs.unlocked.securedsparqlendpoint.repositories;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class Repository<T> {
    protected final Map<String, T> entries = new HashMap<>();

    public Optional<T> get(String name) {
        return Optional.ofNullable(this.entries.get(name));
    }

    public boolean has(String name) {
        return this.entries.containsKey(name);
    }

    public Collection<String> list() {
        return this.entries.keySet();
    }
}
