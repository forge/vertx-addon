package io.vertx.forge;

import io.vertx.forge.verticles.Verticles;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Coordinate;
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
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FacetConstraints(
    @FacetConstraint(JavaSourceFacet.class)
)
public class VertxMavenFacet extends AbstractFacet<Project> implements ProjectFacet {

  @Inject
  FacetFactory factory;

  public static final String VERTX_VERSION_PROPERTY = "vertx.version";

  private String vertxVersion = "3.2.0";

  private final static Coordinate MAVEN_SHADE_PLUGIN_COORDINATE = CoordinateBuilder
      .create("org.apache.maven.plugins:maven-shade-plugin:2.3");

  private final static Coordinate MAVEN_EXEC_PLUGIN_COORDINATE = CoordinateBuilder
      .create("org.codehaus.mojo:exec-maven-plugin:1.4.0");

  @Inject
  ResourceFactory resourceFactory;

  @Inject
  Verticles verticles;

  //TODO vertx-run  command (execute the fat-jar with parameters)
  //TODO vertx-watch command (execute the fat-jar with redeploy)
  //TODO vertx-add-service itf --generate-js-client
  //TODO remove vertx-remove-verticle, vertx-remove-service

  //TODO generate "service" descriptor

  @Override
  public boolean install() {

    // Create vertx version property
    declareVertxVersionProperty();
    // Add java compiler
    addMavenCompilerPlugin();
    // Add BOM
    addVertxBom();
    // Add dependencies
    addVertxDependencies();
    // Create verticle
    verticles.createNewVerticle(getFaceted(), "MainVerticle", null, "java", true);

    // Add shader
    addShaderPlugin();

    addExecPlugin();

    return isInstalled();
  }

  private void addExecPlugin() {
    MavenPluginFacet pluginFacet = getFaceted().getFacet(MavenPluginFacet.class);
    MavenPluginBuilder plugin = MavenPluginBuilder.create().setCoordinate(MAVEN_EXEC_PLUGIN_COORDINATE);
    if (pluginFacet.hasPlugin(MAVEN_EXEC_PLUGIN_COORDINATE)) {
      report("maven-exec-plugin plugin already configured in the `pom.xml` file " +
          "- skipping maven-exec-plugin configuration");
      return;
    } else {
      report("Configuring the maven-exec-plugin...");
      Configuration configuration = ConfigurationBuilder.create();
      configuration
          .addConfigurationElement(ForgeUtils.createSimpleConfigurationElement("executable", "java"));
      configuration.addConfigurationElement(ForgeUtils.createComplexConfigurationElement("arguments",
          "argument", "-classpath",
          "classpath", null,
          "argument", "io.vertx.core.Launcher",
          "argument", "run",
          "argument", "${verticle.main}",
          "argument", "-cp",
          "argument", "${project.basedir}/target/classes"
      ));

      plugin.addExecution(ExecutionBuilder.create().addGoal("exec").setId("run")
          .setConfig(configuration)
      );
    }

    pluginFacet.addPlugin(plugin);
  }


  private void addShaderPlugin() {
    MavenPluginFacet pluginFacet = getFaceted().getFacet(MavenPluginFacet.class);
    MavenPluginBuilder builder = MavenPluginBuilder.create().setCoordinate(MAVEN_SHADE_PLUGIN_COORDINATE);
    if (pluginFacet.hasPlugin(MAVEN_SHADE_PLUGIN_COORDINATE)) {
      report("maven-shade-plugin plugin already configured in the `pom.xml` file - skipping maven-shade-plugin " +
          "configuration");
    } else {
      report("Configuring the maven-shade-plugin...");

      Resource<URL> urlResource = resourceFactory.create(getClass()
          .getResource("maven-shade-plugin-configuration.xml"));

      Configuration configuration = ConfigurationBuilder.create();
      configuration.addConfigurationElement(ConfigurationElementBuilder.create().setName("outputFile").setText("${project" +
          ".build.directory}/${project.artifactId}-${project.version}-fat.jar"));
      configuration.addConfigurationElement(ConfigurationElementBuilder.create().setName("transformers").setText(urlResource.getContents()));

      builder.addExecution(ExecutionBuilder.create().addGoal("shade").setPhase("package").setId("package-fat-jar")
          .setConfig(configuration));

      pluginFacet.addPlugin(builder);

      JavaSourceFacet facet = getJavaSourceFacet();
      String topLevelPackage = facet.getBasePackage();
      ForgeUtils.addPropertyToProject(this.getFaceted(), "verticle.main", topLevelPackage + ".MainVerticle");
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
    ForgeUtils.getOrAddDependency(getFaceted(), "io.vertx", "vertx-hazelcast");
    ForgeUtils.getOrAddDependency(getFaceted(), "io.vertx", "vertx-unit", null, "test");
  }

  public DependencyFacet getDependencyFacet() {
    return getFaceted().getFacet(DependencyFacet.class);
  }

  public String getDefaultVertxVersion() {
    return vertxVersion;
  }

  public void setVertxVersion(String value) {
    vertxVersion = value;
  }


}
