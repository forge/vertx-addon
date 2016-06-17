package io.vertx.forge;

import org.jboss.forge.addon.maven.plugins.Configuration;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MavenConfigurationTest {

  @Test
  public void testMavenShadePluginConfiguration() {
    Configuration configuration = VertxMavenFacet.getShadeConfiguration();
    System.out.println(configuration);
    assertThat(configuration.toString())
        .contains("<outputFile>${project.build.directory}/${project.artifactId}-${project.version}-fat.jar</outputFile>")
        .contains("implementation=\"org.apache.maven.plugins.shade.resource.ManifestResourceTransformer\"")
        .contains("implementation=\"org.apache.maven.plugins.shade.resource.AppendingTransformer\"");
  }

}
