package io.vertx.forge.dependencies;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxDependency extends Dependency {

    List<String> keywords = new ArrayList<>();
    String name;
    String description;

    public List<String> getKeywords() {
        return keywords;
    }

    public VertxDependency setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public String getName() {
        return name;
    }

    public VertxDependency setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VertxDependency setDescription(String description) {
        this.description = description;
        return this;
    }
}
