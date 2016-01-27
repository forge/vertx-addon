package io.vertx.forge.commands;

import io.vertx.forge.VertxMavenFacet;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public abstract class AbstractVertxCommand extends AbstractProjectCommand {

  @Inject
  protected FacetFactory factory;

  @Inject
  protected ProjectFactory projectFactory;

  @Inject
  protected VertxMavenFacet facet;


  public abstract String name();

  public String description() {
    return "";
  }


  @Override
  public UICommandMetadata getMetadata(UIContext context) {
    return Metadata.forCommand(SetupVertxCommand.class)
        .name(name())
        .description(description())
        .category(Categories.create(category()));
  }

  public String category() {
    return "vert.x";
  }

  @Override
  protected boolean isProjectRequired() {
    return true;
  }

  @Override
  protected ProjectFactory getProjectFactory() {
    return projectFactory;
  }



}
