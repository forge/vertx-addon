package io.vertx.forge.commands;

import io.vertx.forge.VertxMavenFacet;
import io.vertx.forge.config.VertxDependency;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.furnace.util.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FacetConstraint({VertxMavenFacet.class})
public class AddDependencyCommand extends AbstractVertxCommand {

    private UISelectMany<VertxDependency> dependencies;

    @Override
    public String name() {
        return "vertx-add-dependency";
    }

    @Override
    public String description() {
        return "Add one or more dependency. Already used dependencies have been filtered out.";
    }


    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        InputComponentFactory factory = builder.getInputComponentFactory();
        dependencies = factory.createSelectMany("dependencies", VertxDependency.class)
            .setLabel("Dependency List")
            .setDescription("Dependency list");

        UIContext uiContext = builder.getUIContext();
        if (uiContext.getProvider().isGUI()) {
            dependencies.setItemLabelConverter(VertxDependency::getName);
        } else {
            dependencies.setItemLabelConverter(VertxDependency::getArtifactId);
        }
        Project project = Projects.getSelectedProject(getProjectFactory(), uiContext);
        final Collection<VertxDependency> deps;
        if (project != null && project.hasFacet(VertxMavenFacet.class)) {
            deps = project.getFacet(VertxMavenFacet.class).getNotUsedDependencies();
        } else {
            deps = VertxMavenFacet.getAllDependencies();
        }

        dependencies.setValueChoices(deps);

        builder.add(dependencies);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        Optional<VertxMavenFacet> maybeFacet = project.getFacetAsOptional(VertxMavenFacet.class);

        VertxMavenFacet facet;

        if (!maybeFacet.isPresent()) {
            super.facet.setFaceted(project);
            super.facet.install();
            facet = super.facet;
        } else {
            facet = maybeFacet.get();
        }

        if (dependencies.hasValue()) {
            List<VertxDependency> deps = Lists.toList(dependencies.getValue());
            facet.addDependencies(deps);
            List<String> artifactIds = deps.stream().map(VertxDependency::getArtifactId)
                .collect(Collectors.toList());
            return Results.success("Vert.x Dependency '"
                + artifactIds
                + "' were successfully added to the project descriptor");
        }
        return Results.success();

    }

    /**
     * @return the dependencies
     */
    public UISelectMany<VertxDependency> getDependencies() {
        return dependencies;
    }
}
