package io.vertx.forge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.forge.dependencies.VertxDependency;
import io.vertx.forge.verticles.Verticles;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.facets.constraints.FacetConstraints;
import org.jboss.forge.addon.maven.plugins.*;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.resource.ResourceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FacetConstraints(
    @FacetConstraint(JavaSourceFacet.class)
)
public class VertxMavenFacet extends AbstractFacet<Project> implements ProjectFacet {

    private static final Properties VERSIONS;
    private final static Coordinate VERTX_MAVEN_PLUGIN;
    private final static Coordinate JUNIT_DEPENDENCY;
    private static final String VERTX_VERSION;

    public static final String VERTX_VERSION_PROPERTY = "vertx.version";

    static {
        VERSIONS = ForgeUtils.loadVersions();
        VERTX_MAVEN_PLUGIN = CoordinateBuilder
            .create("io.fabric8:vertx-maven-plugin:" + VERSIONS.getProperty("vertx-maven-plugin"));

        JUNIT_DEPENDENCY = CoordinateBuilder.create("junit:junit:" + VERSIONS.getProperty("junit"));
        VERTX_VERSION = VERSIONS.getProperty("vertx");
    }

    @Inject
    FacetFactory factory;

    private String vertxVersion = VERTX_VERSION;

    @Inject
    ResourceFactory resourceFactory;

    @Inject
    Verticles verticles;

    //TODO vertx-add-service itf --generate-js-client
    //TODO remove vertx-remove-verticle, vertx-remove-service

    @Override
    public boolean install() {
        // Create vertx version property
        declareVertxVersionProperty();

        // Add java compiler
        addMavenCompilerPlugin();

        // Add Vert.x plugin
        addVertxPlugin();

        // Add BOM
        addVertxBom();

        // Add dependencies
        addVertxDependencies();

        // Create verticle
        verticles.createNewVerticle(getFaceted(), "MainVerticle", null, "java", true);

        return isInstalled();
    }

    private void addVertxPlugin() {
        MavenPluginFacet pluginFacet = getFaceted().getFacet(MavenPluginFacet.class);
        MavenPluginBuilder builder = MavenPluginBuilder.create().setCoordinate(VERTX_MAVEN_PLUGIN);
        if (pluginFacet.hasPlugin(VERTX_MAVEN_PLUGIN)) {
            report("vertx-maven-plugin is already configured in the `pom.xml` file - skipping its configuration");
        } else {
            report("Configuring the vertx-maven-plugin...");

            builder.addExecution(ExecutionBuilder.create().addGoal("initialize").addGoal("package").setId("vertx"));
            builder.createConfiguration().addConfigurationElement(
                ConfigurationElementBuilder.create().setName("redeploy").setText("true")
            );

            pluginFacet.addPlugin(builder);

            JavaSourceFacet facet = getJavaSourceFacet();
            String topLevelPackage = facet.getBasePackage();
            ForgeUtils.addPropertyToProject(this.getFaceted(), "vertx.verticle",
                topLevelPackage + ".MainVerticle");
        }
    }

    private void report(String message) {
        System.out.println(message);
    }

    public JavaSourceFacet getJavaSourceFacet() {
        return getFaceted().getFacet(JavaSourceFacet.class);
    }

    @Override
    public boolean isInstalled() {
        MavenFacet mavenFacet = getMavenFacet();
        Model pom = mavenFacet.getModel();
        return pom.getProperties().getProperty(VERTX_VERSION_PROPERTY) != null;
    }

    public MavenFacet getMavenFacet() {
        return getFaceted().getFacet(MavenFacet.class);
    }

    private void declareVertxVersionProperty() {
        ForgeUtils.addPropertyToProject(this.getFaceted(), VERTX_VERSION_PROPERTY, vertxVersion);
    }

    private void addMavenCompilerPlugin() {
        Coordinate coordinate = ForgeUtils.coordinate("org.apache.maven.plugins", "maven-compiler-plugin");
        MavenPlugin plugin = ForgeUtils.findDirectPlugin(getFaceted(), coordinate.getArtifactId());

        MavenPluginBuilder builder;
        ConfigurationBuilder configurationBuilder;
        if (plugin != null) {
            builder = MavenPluginBuilder.create(plugin);
            configurationBuilder = ConfigurationBuilder.create(plugin.getConfig(), builder);
        } else {
            builder = MavenPluginBuilder.create().setCoordinate(coordinate);
            configurationBuilder = ConfigurationBuilder.create();
        }
        builder.setConfiguration(configurationBuilder);


        if (plugin != null) {
            // Update it.
            report("maven-compiler-plugin already configured in the `pom.xml`, updating configuration...");
            // Removing existing configuration.
            configurationBuilder.removeConfigurationElement("source");
            configurationBuilder.removeConfigurationElement("target");
            // Set the version
            configurationBuilder.createConfigurationElement("source").setText("1.8");
            configurationBuilder.createConfigurationElement("target").setText("1.8");
            getFaceted().getFacet(MavenPluginFacet.class).updatePlugin(builder);
        } else {
            configurationBuilder.createConfigurationElement("source").setText("1.8");
            configurationBuilder.createConfigurationElement("target").setText("1.8");
            getFaceted().getFacet(MavenPluginFacet.class).addPlugin(builder);
        }

        // Remove properties is there
        MavenFacet maven = getMavenFacet();
        Model pom = maven.getModel();
        Properties properties = pom.getProperties();
        properties.remove("maven.compiler.source");
        properties.remove("maven.compiler.target");
        maven.setModel(pom);
    }

    private void save() {
        MavenFacet maven = getMavenFacet();
        Model pom = maven.getModel();
        maven.setModel(pom);
    }

    private void addVertxBom() {
        DependencyBuilder dependency = DependencyBuilder.create()
            .setGroupId("io.vertx")
            .setArtifactId("vertx-dependencies")
            .setVersion("${" + VERTX_VERSION_PROPERTY + "}")
            .setScopeType("import")
            .setPackaging("pom");
        DependencyFacet facet = getDependencyFacet();
        facet.addManagedDependency(dependency);
    }

    private void addVertxDependencies() {
        ForgeUtils.getOrAddDependency(getFaceted(), "io.vertx", "vertx-core");
        ForgeUtils.getOrAddDependency(getFaceted(), "io.vertx", "vertx-unit", null, null, "test");
        ForgeUtils.getOrAddDependency(getFaceted(), JUNIT_DEPENDENCY.getGroupId(), JUNIT_DEPENDENCY.getArtifactId(),
            JUNIT_DEPENDENCY.getVersion(), null, "test");
    }

    private DependencyFacet getDependencyFacet() {
        return getFaceted().getFacet(DependencyFacet.class);
    }

    public String getDefaultVertxVersion() {
        return vertxVersion;
    }

    public void setVertxVersion(String value) {
        vertxVersion = value;
    }


    public void addDependencies(List<VertxDependency> deps) {
        deps.forEach(dep ->
            ForgeUtils.getOrAddDependency(getFaceted(),
                dep.getGroupId(), dep.getArtifactId(), null, dep.getClassifier(),
                dep.getScope()));
        save();
    }

    public static Collection<VertxDependency> getAllDependencies() {
        ObjectMapper mapper = new ObjectMapper();
        URL url = VertxMavenFacet.class.getResource("/dependencies/dependencies.json");
        try {
            String content = IOUtils.toString(url);
            VertxDependency[] value = mapper.readValue(content, VertxDependency[].class);
            return Arrays.asList(value);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read the dependencies.json file", e);
        }
    }

    public Collection<VertxDependency> getNotUsedDependencies() {
        List<Dependency> dependencies = getDependencyFacet().getDependencies();
        List<String> artifactIds = dependencies.stream().map(d -> d.getCoordinate().getArtifactId()).collect(Collectors
            .toList());

        return getAllDependencies().stream()
            .filter(dep -> !artifactIds.contains(dep.getArtifactId()))
            .collect(Collectors.toList());
    }

    public void removeDependencies(List<VertxDependency> deps) {
        List<Dependency> dependencies = new ArrayList<>(getDependencyFacet().getDependencies());
        deps.forEach(dep -> {
            for (Dependency dependency : dependencies) {
                if (dependency.getCoordinate().getArtifactId().equalsIgnoreCase(dep.getArtifactId())) {
                    getDependencyFacet().removeDependency(dependency);
                }
            }
        });
        save();
    }


    public Collection<VertxDependency> getUsedDependencies() {
        List<Dependency> dependencies = getDependencyFacet().getDependencies();
        List<String> artifactIds = dependencies.stream().map(d -> d.getCoordinate().getArtifactId()).collect(Collectors
            .toList());

        return getAllDependencies().stream()
            .filter(dep -> artifactIds.contains(dep.getArtifactId()))
            .collect(Collectors.toList());
    }
}
