package io.vertx.forge.config;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxDependency {

    private String artifactId;
    private String groupId;
    private String version;
    private String scope;

    private String type;
    private String classifier;

    private String name;
    private String description;


    public String getArtifactId() {
        return artifactId;
    }

    public VertxDependency setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public VertxDependency setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public VertxDependency setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public VertxDependency setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getType() {
        return type;
    }

    public VertxDependency setType(String type) {
        this.type = type;
        return this;
    }

    public String getClassifier() {
        return classifier;
    }

    public VertxDependency setClassifier(String classifier) {
        this.classifier = classifier;
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
