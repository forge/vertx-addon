package io.vertx.forge.commands;

import io.vertx.forge.VertxMavenFacet;
import io.vertx.forge.dependencies.VertxDependency;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.ui.UIProvider;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.output.UIOutput;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import java.io.PrintStream;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FacetConstraint({VertxMavenFacet.class})
public class ListDependenciesCommand extends AbstractVertxCommand {
    @Override
    public String name() {
        return "vertx-list-dependencies";
    }

    @Override
    public String description() {
        return "List known Vert.x dependencies";
    }

    @Override
    public Result execute(UIExecutionContext executionContext) throws Exception {
        UIProvider provider = executionContext.getUIContext().getProvider();
        UIOutput output = provider.getOutput();
        PrintStream out = output.out();
        for (VertxDependency dep : VertxMavenFacet.getAllDependencies()) {
            String msg = String.format("%s: %s", dep.getArtifactId(), dep.getName());
            out.println(msg);
        }
        return Results.success();
    }

    @Override
    protected boolean isProjectRequired() {
        return false;
    }
}
