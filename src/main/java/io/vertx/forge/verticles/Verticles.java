package io.vertx.forge.verticles;

import io.vertx.forge.facets.CustomResourceFacet;
import io.vertx.forge.ForgeUtils;
import io.vertx.forge.VertxMavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;
import java.net.URL;
import java.util.Objects;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Verticles {

  @Inject
  private ResourceFactory resourceFactory;

  public String createNewVerticle(Project project, String verticleName, String packageName, String type,
                                         boolean isMain) {
    Objects.requireNonNull(project);
    Objects.requireNonNull(verticleName);
    Objects.requireNonNull(type);

    if (type.equalsIgnoreCase("java")) {
      return addJavaVerticle(project, packageName, verticleName, isMain);
    }

    if (type.equalsIgnoreCase("groovy")) {
      return addGroovyVerticle(project, verticleName, isMain);
    }

    if (type.equalsIgnoreCase("ruby")) {
      return addRubyVerticle(project, verticleName, isMain);
    }

    if (type.equalsIgnoreCase("javascript") || type.equalsIgnoreCase("js")) {
      return addJavascriptVerticle(project,verticleName, isMain);
    }

    throw new IllegalArgumentException("Unknown type " + type);

  }

  private String addGroovyVerticle(Project project, String verticleName, boolean isMain) {
    // Add groovy resource
    CustomResourceFacet groovy = new CustomResourceFacet(project, "groovy");
    if (!groovy.isInstalled()) {
      groovy.install();
    }
    Resource<URL> urlResource = resourceFactory.create(VertxMavenFacet.class.getResource("verticle.groovy"));
    if (!verticleName.endsWith(".groovy")) {
      verticleName += ".groovy";
    }
    FileResource<?> resource = groovy.createResource(urlResource.getContents().toCharArray(), verticleName);

    // Update pom if it's main
    if (isMain) {
      ForgeUtils.addPropertyToProject(project, "verticle.main", verticleName);
    }

    // Add groovy dependency if not there
    ForgeUtils.getOrAddDependency(project, "io.vertx", "vertx-lang-groovy");

    return resource.getFullyQualifiedName();
  }

  private String addJavascriptVerticle(Project project, String verticleName, boolean isMain) {
    // Add groovy resource
    CustomResourceFacet groovy = new CustomResourceFacet(project, "javascript");
    if (!groovy.isInstalled()) {
      groovy.install();
    }
    Resource<URL> urlResource = resourceFactory.create(VertxMavenFacet.class.getResource("verticle.js"));
    if (!verticleName.endsWith(".js")) {
      verticleName += ".js";
    }
    FileResource<?> resource = groovy.createResource(urlResource.getContents().toCharArray(), verticleName);

    // Update pom if it's main
    if (isMain) {
      ForgeUtils.addPropertyToProject(project, "verticle.main", verticleName);
    }

    // Add groovy dependency if not there
    ForgeUtils.getOrAddDependency(project, "io.vertx", "vertx-lang-js");

    return resource.getFullyQualifiedName();
  }

  private String addRubyVerticle(Project project, String verticleName, boolean isMain) {
    // Add groovy resource
    CustomResourceFacet ruby = new CustomResourceFacet(project, "ruby");
    if (!ruby.isInstalled()) {
      ruby.install();
    }
    Resource<URL> urlResource = resourceFactory.create(VertxMavenFacet.class.getResource("verticle.rb"));

    if (!verticleName.endsWith(".rb")) {
      verticleName += ".rb";
    }
    FileResource<?> resource = ruby.createResource(urlResource.getContents().toCharArray(), verticleName);

    // Update pom if it's main
    if (isMain) {
      ForgeUtils.addPropertyToProject(project, "verticle.main", verticleName);
    }

    // Add groovy dependency if not there
    ForgeUtils.getOrAddDependency(project, "io.vertx", "vertx-lang-ruby");

    return resource.getFullyQualifiedName();
  }

  private String addJavaVerticle(Project project, String packageName, String className, boolean isMain) {
    JavaSourceFacet source = project.getFacet(JavaSourceFacet.class);

    String topLevelPackage;
    if (packageName == null) {
      topLevelPackage = source.getBasePackage();
    } else {
      topLevelPackage = packageName;
    }

    if (className.endsWith(".java")) {
      className = className.substring(0, className.length() - ".java".length());
    }

    JavaClassSource mainVerticle = Roaster.create(JavaClassSource.class)
        .setPackage(topLevelPackage)
        .setAbstract(false)
        .setName(className)
        .setSuperType("io.vertx.core.AbstractVerticle");
    mainVerticle
        .addImport("io.vertx.core.*");
    mainVerticle.addMethod().setName("start").setBody(
        "vertx.createHttpServer().requestHandler(req -> req.response().end(\"Hello World!\")).listen(8080);")
        .setPublic()
        .addAnnotation(Override.class);

    JavaResource resource = source.saveJavaSource(mainVerticle.getEnclosingType());

    if (isMain) {
      ForgeUtils.addPropertyToProject(project, "verticle.main", topLevelPackage + "." + className);
    }

    return resource.toString();
  }
}
