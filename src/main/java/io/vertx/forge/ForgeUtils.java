package io.vertx.forge;

import com.google.common.base.Strings;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.*;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ForgeUtils {

  public static Coordinate coordinate(String group, String artifact) {
    return CoordinateBuilder.create().setGroupId(group).setArtifactId(artifact);
  }

  public static Coordinate coordinate(String artifact) {
    return CoordinateBuilder.create().setArtifactId(artifact);
  }

  public static void addPropertyToProject(Project project, String key, String value) {
    MavenFacet maven = project.getFacet(MavenFacet.class);
    Model pom = maven.getModel();
    Properties properties = pom.getProperties();
    properties.setProperty(key, value);
    maven.setModel(pom);
  }

  public static ConfigurationElement createSimpleConfigurationElement(String name, String value) {
    return ConfigurationElementBuilder.create().createConfigurationElement(name).setText(value);
  }

  public static ConfigurationElement createComplexConfigurationElement(
      String name,
      String... values) {

    ConfigurationElementBuilder element = ConfigurationElementBuilder.create().setName(name);
    for (int i = 0; i < values.length;) {
      String key = values[i];
      // The value is the next element, if it's not there, the parameters are invalid.
      String value = values[i+1];
      if (value == null) {
        // null is used for "empty" element
        element.addChild(ConfigurationElementBuilder.create().setName(key).setText(""));
      } else {
        element.addChild(ConfigurationElementBuilder.create().setName(key).setText(value));
      }
      i = i + 2;
    }
    return element;
  }

  public static MavenPlugin findDirectPlugin(Project project, final String artifactId) {
    MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
    List<MavenPlugin> list = plugins.listConfiguredPlugins();
    Optional<MavenPlugin> maybePlugin = list.stream().filter(plugin -> plugin.getCoordinate().getArtifactId().equalsIgnoreCase(artifactId)).findFirst();
    if (maybePlugin.isPresent()) {
      return maybePlugin.get();
    }
    return null;
  }

  public static Dependency getOrAddDependency(Project project, String groupId, String artifactId) {
    return getOrAddDependency(project, groupId, artifactId, null, null);
  }

  public static Dependency getOrAddDependency(Project project, String groupId, String artifactId,
                                              String version, String scope) {
    DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
    Optional<Dependency> found = dependencies.getEffectiveDependencies().stream().filter(dep ->
        dep.getCoordinate().getGroupId().equalsIgnoreCase(groupId)
            && dep.getCoordinate().getArtifactId().equalsIgnoreCase(artifactId)
            && (version == null || version.equalsIgnoreCase(dep.getCoordinate().getVersion()))
            && Strings.isNullOrEmpty(dep.getCoordinate().getClassifier())
            && (Strings.isNullOrEmpty(dep.getCoordinate().getPackaging()) || dep.getCoordinate().getPackaging()
            .equalsIgnoreCase("jar"))
    ).findAny();
    if (found.isPresent()) {
      return found.get();
    }

    DependencyBuilder dependency = DependencyBuilder.create().setGroupId(groupId).setArtifactId(artifactId);
    if (version != null) {
      dependency.setVersion(version);
    }
    if (scope != null) {
      dependency.setScopeType(scope);
    }
    dependencies.addDirectDependency(dependency);

    return dependency;
  }
}
