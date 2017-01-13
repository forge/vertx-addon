package io.vertx.forge;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
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
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@RunWith(Arquillian.class)
public class AddonConfigurationOverrideTest {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private MavenBuildSystem projectProvider;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private ShellTest shellTest;
    private File root;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() throws IOException {
        File destFile = new File("vertx-forge-addon-configuration.json");
        FileUtils.copyFile(new File("src/test/resources/config/vertx-forge-addon-configuration.json"),
            destFile);

        return ShrinkWrap
            .create(AddonArchive.class)
            .addPackages(true, "org.assertj.core")
            .addBeansXML();
    }


    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File("vertx-forge-addon-configuration.json"));
    }

    @Before
    public void setUp() throws IOException {
        root = prepareRoot("target/tests/config-override");
    }

    @Test
    public void testOverridingTheConfiguration() throws TimeoutException, IOException {
        Project project = projectFactory.createProject(resourceFactory.create(root), projectProvider);
        VertxMavenFacet facet = facetFactory.install(project, VertxMavenFacet.class);
        assertThat(facet.getDefaultVertxVersion()).isEqualTo("3.3.1-product");
        assertThat(new File(root, "pom.xml")).isFile();

        // Not a valid dependency
        shellTest.getShell().setCurrentResource(project.getRoot());
        Result result = shellTest.execute("vertx-add-dependency --dependencies vertx-dropwizard-metrics", 10,
            TimeUnit.SECONDS);
        assertThat(result).isNotInstanceOf(Failed.class);
        Dependency dependency = findDependencyByName(getDependencies(project), "vertx-dropwizard-metrics");
        assertThat(dependency).isNull();


        // Product version
        shellTest.getShell().setCurrentResource(project.getRoot());
        result = shellTest.execute("vertx-add-dependency --dependencies vertx-web", 10,
            TimeUnit.SECONDS);
        assertThat(result).isNotInstanceOf(Failed.class);
        dependency = findDependencyByName(getDependencies(project), "vertx-web");
        assertThat(dependency).isNotNull();
        assertThat(dependency.getCoordinate().getVersion()).isNull();

        // Community version
        shellTest.getShell().setCurrentResource(project.getRoot());
        result = shellTest.execute("vertx-add-dependency --dependencies vertx-mongo-client", 10,
            TimeUnit.SECONDS);
        assertThat(result).isNotInstanceOf(Failed.class);
        dependency = findDependencyByName(getDependencies(project), "vertx-mongo-client");
        assertThat(dependency).isNotNull();
        assertThat(dependency.getCoordinate().getVersion()).isEqualTo("3.3.2");
    }

    public static File prepareRoot(String root) {
        File r = new File(root);
        if (r.exists()) {
            FileUtils.deleteQuietly(r);
        }
        r.mkdirs();
        return r;
    }

    public Dependency findDependencyByName(List<Dependency> dependencies, String artifactId) {
        for (Dependency dependency : dependencies) {
            if (dependency.getCoordinate().getArtifactId().equalsIgnoreCase(artifactId)) {
                return dependency;
            }
        }
        return null;
    }

    public List<Dependency> getDependencies(Project project) {
        return project.getFacet(DependencyFacet.class).getDependencies();
    }

}
