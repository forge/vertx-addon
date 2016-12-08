package io.vertx.forge.dependencies;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Dependency {

    private String artifactId;
    private String groupId;
    private String version;
    private String scope;

    private String type;
    private String classifier;


    public String getArtifactId() {
        return artifactId;
    }

    public Dependency setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public Dependency setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Dependency setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Dependency setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getType() {
        return type;
    }

    public Dependency setType(String type) {
        this.type = type;
        return this;
    }

    public String getClassifier() {
        return classifier;
    }

    public Dependency setClassifier(String classifier) {
        this.classifier = classifier;
        return this;
    }
}
