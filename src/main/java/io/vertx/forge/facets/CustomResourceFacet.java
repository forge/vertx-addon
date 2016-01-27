package io.vertx.forge.facets;

import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.visit.ResourceVisit;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FacetConstraint(MavenFacet.class)
public class CustomResourceFacet extends AbstractFacet<Project> implements ResourcesFacet {

  private String name = "groovy";

  public CustomResourceFacet(Project faceted, String name) {
    setFaceted(faceted);
    setName(name);
  }

  public String getName() {
    return name;
  }

  public CustomResourceFacet setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public List<DirectoryResource> getResourceDirectories() {
    List<DirectoryResource> result = new ArrayList<>();
    result.add(getResourceDirectory());
    return result;
  }

  @Override
  public DirectoryResource getResourceDirectory() {
    String resFolderName = "src" + File.separator + "main" + File.separator + name;
    DirectoryResource projectRoot = getFaceted().getRoot().reify(DirectoryResource.class);
    return projectRoot.getChildDirectory(resFolderName);
  }

  @Override
  public DirectoryResource getTestResourceDirectory() {
    String resFolderName = "src" + File.separator + "test" + File.separator + name;
    DirectoryResource projectRoot = getFaceted().getRoot().reify(DirectoryResource.class);
    return projectRoot.getChildDirectory(resFolderName);
  }

  @Override
  public boolean isInstalled() {
    return getResourceDirectory().exists();
  }

  @Override
  public boolean install() {
    if (!this.isInstalled()) {
      getResourceDirectories().forEach(DirectoryResource::mkdirs);
    }

    // Update Maven model - main resource only
    MavenFacet maven = getFaceted().getFacet(MavenFacet.class);
    Model pom = maven.getModel();
    Resource main = new Resource();
    main.setDirectory("${project.basedir}/src/main/" + name);
    pom.getBuild().getResources().add(main);

    maven.setModel(pom);

    return true;
  }

  @Override
  public FileResource<?> getResource(final String relativePath) {
    return (FileResource<?>) getResourceDirectory().getChild(relativePath);
  }

  @Override
  public FileResource<?> getTestResource(final String relativePath) {
    return (FileResource<?>) getTestResourceDirectory().getChild(relativePath);
  }

  @Override
  public FileResource<?> createResource(final char[] bytes, final String relativeFilename) {
    FileResource<?> file = (FileResource<?>) getResourceDirectory().getChild(relativeFilename);
    file.setContents(bytes);
    return file;
  }

  @Override
  public FileResource<?> createTestResource(final char[] bytes, final String relativeFilename) {
    FileResource<?> file = (FileResource<?>) getTestResourceDirectory().getChild(relativeFilename);
    file.setContents(bytes);
    return file;
  }

  @Override
  public void visitResources(ResourceVisitor visitor) {
    new ResourceVisit(getResourceDirectory()).perform(visitor,
        resource -> resource instanceof DirectoryResource,
        type -> true);
  }

  @Override
  public void visitTestResources(ResourceVisitor visitor) {
    new ResourceVisit(getTestResourceDirectory()).perform(visitor,
        resource -> resource instanceof DirectoryResource,
        type -> true);
  }
}
