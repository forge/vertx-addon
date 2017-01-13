package io.vertx.forge;

import io.vertx.forge.verticles.Verticles;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.vertx.forge.config.VertxAddonConfiguration.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;


/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@RunWith(Arquillian.class)
public class VertxFacetTest {

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap
            .create(AddonArchive.class)
            .addPackages(true, "org.assertj.core")
            .addBeansXML();
    }

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private MavenBuildSystem projectProvider;

    @Inject
    private Verticles verticles;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private ShellTest shellTest;


    @Test
    public void testOnEmptyProject() throws Exception {
        Project project = projectFactory.createTempProject(projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
        MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
        MavenFacet maven = project.getFacet(MavenFacet.class);

        // Check vertx.version
        assertThat(maven.getProperties()).contains(entry(VertxMavenFacet.VERTX_VERSION_PROPERTY, config().getVersion()));

        // Check managed dependency
        checkVertxBom(dependencies);
        assertThat(dependencies.getManagedDependencies()).hasSize(1);

        // Check dependencies
        checkDependency(dependencies, "io.vertx", "vertx-core", null, null);
        checkDependency(dependencies, "io.vertx", "vertx-unit", null, "test");
        checkDependency(dependencies, "junit", "junit", "4.12", "test");
        assertThat(dependencies.getDependencies()).hasSize(3);

        // Check maven compiler
        hasPlugin(plugins, "maven-compiler-plugin");
        hasPlugin(plugins, "vertx-maven-plugin");
    }

    @Test
    public void testOnProjectThatHasAlreadyDependencies() throws Exception {
        File root = prepareRoot("target/tests/project-with-dependencies");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);

        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
        dependencies.addDirectDependency(DependencyBuilder.create("commons-io:commons-io:2.4"));
        checkDependency(dependencies, "commons-io", "commons-io", "2.4", null);

        facetFactory.install(project, VertxMavenFacet.class);
        MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
        MavenFacet maven = project.getFacet(MavenFacet.class);

        // Check vertx.version
        assertThat(maven.getProperties()).contains(entry(VertxMavenFacet.VERTX_VERSION_PROPERTY, config().getVersion()));

        // Check managed dependency
        checkVertxBom(dependencies);
        assertThat(dependencies.getManagedDependencies()).hasSize(1);

        // Check dependencies
        checkDependency(dependencies, "commons-io", "commons-io", "2.4", null);
        checkDependency(dependencies, "io.vertx", "vertx-core", null, null);
        checkDependency(dependencies, "io.vertx", "vertx-unit", null, "test");
        checkDependency(dependencies, "junit", "junit", "4.12", "test");
        assertThat(dependencies.getDependencies()).hasSize(4);

        // Check maven compiler
        hasPlugin(plugins, "maven-compiler-plugin");
        hasPlugin(plugins, "vertx-maven-plugin");
    }

    @Test
    public void testOnProjectThatHasAlreadyACoreDependency() throws Exception {
        File root = prepareRoot("target/tests/project-with-core-dependency");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);

        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
        dependencies.addDirectDependency(DependencyBuilder.create("io.vertx:vertx-core:3.1.0"));
        checkDependency(dependencies, "io.vertx", "vertx-core", "3.1.0", null);

        facetFactory.install(project, VertxMavenFacet.class);
        MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
        MavenFacet maven = project.getFacet(MavenFacet.class);

        // Check vertx.version
        assertThat(maven.getProperties()).contains(entry(VertxMavenFacet.VERTX_VERSION_PROPERTY, config().getVersion()));

        // Check managed dependency
        checkVertxBom(dependencies);
        assertThat(dependencies.getManagedDependencies()).hasSize(1);

        // Check dependencies
        checkDependency(dependencies, "io.vertx", "vertx-core", "3.1.0", null);
        checkDependency(dependencies, "io.vertx", "vertx-unit", null, "test");
        checkDependency(dependencies, "junit", "junit", "4.12", "test");
        assertThat(dependencies.getDependencies()).hasSize(3);

        // Check maven compiler
        hasPlugin(plugins, "maven-compiler-plugin");
        hasPlugin(plugins, "vertx-maven-plugin");
    }

    @Test
    public void testOnProjectThatHasAlreadyABomDependency() throws Exception {
        File root = prepareRoot("target/tests/project-with-managed-dependency");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);

        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
        dependencies.addManagedDependency(DependencyBuilder.create("org.jboss.forge:forge-bom:2.20.1.Final")
            .setScopeType("import").setPackaging("pom"));

        facetFactory.install(project, VertxMavenFacet.class);
        MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
        MavenFacet maven = project.getFacet(MavenFacet.class);

        // Check vertx.version
        assertThat(maven.getProperties()).contains(entry(VertxMavenFacet.VERTX_VERSION_PROPERTY, config().getVersion()));

        // Check managed dependency
        checkVertxBom(dependencies);
        assertThat(dependencies.getManagedDependencies()).hasSize(2);

        // Check dependencies
        checkDependency(dependencies, "io.vertx", "vertx-core", null, null);
        checkDependency(dependencies, "io.vertx", "vertx-unit", null, "test");
        checkDependency(dependencies, "junit", "junit", "4.12", "test");
        assertThat(dependencies.getDependencies()).hasSize(3);

        // Check maven compiler
        hasPlugin(plugins, "maven-compiler-plugin");
        hasPlugin(plugins, "vertx-maven-plugin");
    }

    @Test
    public void testOnProjectThatHasTheBomAlready() throws Exception {
        File root = prepareRoot("target/tests/project-with-managed-dependency");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);

        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
        dependencies.addManagedDependency(DependencyBuilder.create("io.vertx:vertx-dependencies:3.3.0")
            .setScopeType("import").setPackaging("pom"));
        dependencies.addDirectDependency(DependencyBuilder.create("io.vertx:vertx-core"));

        facetFactory.install(project, VertxMavenFacet.class);
        MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
        MavenFacet maven = project.getFacet(MavenFacet.class);

        // Check vertx.version
        assertThat(maven.getProperties()).contains(entry(VertxMavenFacet.VERTX_VERSION_PROPERTY, config().getVersion()));

        // Check managed dependency
        checkVertxBom(dependencies);
        assertThat(dependencies.getManagedDependencies()).hasSize(1);

        // Check dependencies
        checkDependency(dependencies, "io.vertx", "vertx-core", null, null);
        checkDependency(dependencies, "io.vertx", "vertx-unit", null, "test");
        checkDependency(dependencies, "junit", "junit", "4.12", "test");
        assertThat(dependencies.getDependencies()).hasSize(3);

        // Check maven compiler
        hasPlugin(plugins, "maven-compiler-plugin");
        hasPlugin(plugins, "vertx-maven-plugin");
    }

    /**
     * If the project define vertx.version, the facet is considered as already installed.
     *
     * @throws Exception something failed
     */
    @Test
    public void testOnProjectDefiningVertxVersion() throws Exception {
        File root = prepareRoot("target/tests/project-defining-vertx-version");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);

        MavenFacet maven = project.getFacet(MavenFacet.class);
        ForgeUtils.addPropertyToProject(project, VertxMavenFacet.VERTX_VERSION_PROPERTY, "3.1.0");
        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);

        facetFactory.install(project, VertxMavenFacet.class);
        assertThat(project.hasFacet(VertxMavenFacet.class)).isTrue();
        assertThat(maven.getProperties()).contains(entry(VertxMavenFacet.VERTX_VERSION_PROPERTY, "3.1.0"));
        assertThat(dependencies.getManagedDependencies()).hasSize(0);
        assertThat(dependencies.getDependencies()).hasSize(0);
    }

    @Test
    public void testAddJavaVerticle() throws Exception {
        File root = prepareRoot("target/tests/add-java-verticle-0");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        verticles.createNewVerticle(project, "MyVerticle", "io.acme", "java", false);
        assertThat(new File(root, "src/main/java/io/acme/MyVerticle.java")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "unknown.MainVerticle"));

        verticles.createNewVerticle(project, "MyMainVerticle.java", "io.acme", "java", true);
        assertThat(new File(root, "src/main/java/io/acme/MyMainVerticle.java")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "io.acme.MyMainVerticle"));
    }


    @Test
    public void testAddGroovyVerticle() throws Exception {
        File root = prepareRoot("target/tests/add-groovy-verticle-0");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        String groovy1 = verticles.createNewVerticle(project, "my-verticle.groovy", null, "groovy", false);
        assertThat(groovy1).endsWith("src/main/groovy/my-verticle.groovy");
        assertThat(new File(root, "src/main/groovy/my-verticle.groovy")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "unknown.MainVerticle"));

        String groovy2 = verticles.createNewVerticle(project, "my-main-verticle.groovy", null, "groovy", true);
        assertThat(groovy2).endsWith("src/main/groovy/my-main-verticle.groovy");
        assertThat(new File(root, "src/main/groovy/my-main-verticle.groovy")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "my-main-verticle.groovy"));
    }

    @Test
    public void testAddJavaScript() throws Exception {
        File root = prepareRoot("target/tests/add-javascript-verticle-0");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        String js1 = verticles.createNewVerticle(project, "my-verticle", null, "js", false);
        assertThat(js1).endsWith("src/main/javascript/my-verticle.js");
        assertThat(new File(root, "src/main/javascript/my-verticle.js")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "unknown.MainVerticle"));

        String js2 = verticles.createNewVerticle(project, "my-main-verticle.js", null, "javascript", true);
        assertThat(js2).endsWith("src/main/javascript/my-main-verticle.js");
        assertThat(new File(root, "src/main/javascript/my-main-verticle.js")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "my-main-verticle.js"));
    }


    @Test
    public void testAddRubyVerticle() throws Exception {
        File root = prepareRoot("target/tests/add-ruby-verticle-0");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        String ruby = verticles.createNewVerticle(project, "my-ruby-verticle.rb", null, "ruby", false);
        assertThat(ruby).endsWith("src/main/ruby/my-ruby-verticle.rb");
        assertThat(new File(root, "src/main/ruby/my-ruby-verticle.rb")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "unknown.MainVerticle"));
    }

    @Test
    public void testAddRubyVerticleAsMain() throws Exception {
        File root = prepareRoot("target/tests/add-ruby-verticle-1");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        String ruby = verticles.createNewVerticle(project, "my-ruby-verticle.rb", null, "ruby", true);
        assertThat(ruby).endsWith("src/main/ruby/my-ruby-verticle.rb");
        assertThat(new File(root, "src/main/ruby/my-ruby-verticle.rb")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "my-ruby-verticle.rb"));
    }

    @Test
    public void testAddingAndRemoveADependency() throws TimeoutException {
        File root = prepareRoot("target/tests/add-dropwizard-dep");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);


        shellTest.getShell().setCurrentResource(project.getRoot());
        Result result = shellTest.execute("vertx-add-dependency --dependencies vertx-dropwizard-metrics", 10,
            TimeUnit.SECONDS);

        assertThat(result).isNotInstanceOf(Failed.class);
        assertThat(new File(root, "pom.xml")).isFile();
        assertThat(getDependencies(project)).isNotNull();
        Dependency dependency = findDependencyByName(getDependencies(project), "vertx-dropwizard-metrics");
        assertThat(dependency).isNotNull();
        assertThat(dependency.getCoordinate().getVersion()).isNull();

        // Remove the deps

        result = shellTest.execute(" vertx-remove-dependency --dependencies vertx-dropwizard-metrics",
            10, TimeUnit.SECONDS);
        assertThat(result).isNotInstanceOf(Failed.class);
        assertThat(getDependencies(project)).isNotNull();
        assertThat(findDependencyByName(getDependencies(project), "vertx-dropwizard-metrics")).isNull();

        // List

        result = shellTest.execute(" vertx-list-dependencies", 10, TimeUnit.SECONDS);
        assertThat(result).isNotInstanceOf(Failed.class);
    }



    public static Dependency findDependencyByName(List<Dependency> dependencies, String artifactId) {
        for (Dependency dependency : dependencies) {
            if (dependency.getCoordinate().getArtifactId().equalsIgnoreCase(artifactId)) {
                return dependency;
            }
        }
        return null;
    }

    public static List<Dependency> getDependencies(Project project) {
        return project.getFacet(DependencyFacet.class).getDependencies();
    }

    @Test
    public void addOneVerticleOfEachType() throws Exception {
        File root = prepareRoot("target/tests/add-all-verticles");
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        facetFactory.install(project, VertxMavenFacet.class);

        String ruby = verticles.createNewVerticle(project, "my-ruby-verticle.rb", null, "ruby", true);
        assertThat(ruby).endsWith("src/main/ruby/my-ruby-verticle.rb");
        assertThat(new File(root, "src/main/ruby/my-ruby-verticle.rb")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "my-ruby-verticle.rb"));

        String js2 = verticles.createNewVerticle(project, "my-main-verticle.js", null, "javascript", true);
        assertThat(js2).endsWith("src/main/javascript/my-main-verticle.js");
        assertThat(new File(root, "src/main/javascript/my-main-verticle.js")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "my-main-verticle.js"));

        String groovy2 = verticles.createNewVerticle(project, "my-main-verticle.groovy", null, "groovy", true);
        assertThat(groovy2).endsWith("src/main/groovy/my-main-verticle.groovy");
        assertThat(new File(root, "src/main/groovy/my-main-verticle.groovy")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "my-main-verticle.groovy"));

        verticles.createNewVerticle(project, "MyMainVerticle.java", "io.acme", "java", true);
        assertThat(new File(root, "src/main/java/io/acme/MyMainVerticle.java")).isFile();
        assertThat(project.getFacet(MavenFacet.class).getProperties())
            .contains(entry("vertx.verticle", "io.acme.MyMainVerticle"));
    }

    private void hasPlugin(MavenPluginFacet plugins, String artifactId) {
        Optional<MavenPlugin> p = plugins.listConfiguredEffectivePlugins().stream().filter((plugin) -> plugin
            .getCoordinate().getArtifactId().equals(artifactId)).findFirst();
        assertThat(p.get()).isNotNull();
    }

    public static File prepareRoot(String root) {
        File r = new File(root);
        if (r.exists()) {
            FileUtils.deleteQuietly(r);
        }
        r.mkdirs();
        return r;
    }

    public void checkVertxBom(DependencyFacet dependencies) {
        Optional<Dependency> dep = dependencies.getManagedDependencies().stream().filter((dependency) -> dependency.getCoordinate().getArtifactId()
            .equals("vertx-dependencies")).findFirst();
        assertThat(dep.isPresent()).isTrue();
        assertThat(dep.get().getCoordinate().getGroupId()).isEqualTo("io.vertx");
        assertThat(dep.get().getCoordinate().getVersion()).isEqualTo(config().getVersion());
        assertThat(dep.get().getCoordinate().getPackaging()).isEqualTo("pom");
        assertThat(dep.get().getScopeType()).isEqualTo("import");
    }

    public void checkDependency(DependencyFacet dependencies, String groupId, String artifactId, String version, String
        scope) {
        Optional<Dependency> dep = dependencies.getDependencies().stream().filter((dependency) ->
            dependency.getCoordinate().getArtifactId().equals(artifactId))
            .findFirst();
        assertThat(dep.isPresent()).isTrue();
        assertThat(dep.get().getCoordinate().getGroupId()).isEqualTo(groupId);
        if (version == null) {
            assertThat(dep.get().getCoordinate().getVersion()).isNull();
        } else {
            assertThat(dep.get().getCoordinate().getVersion()).isEqualTo(version);
        }
        if (scope != null) {
            assertThat(dep.get().getScopeType()).isEqualTo(scope);
        }
    }
}
