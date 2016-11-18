package io.vertx.forge.commands;

import static java.util.Arrays.asList;

import javax.inject.Inject;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SetupVertxCommand extends AbstractVertxCommand {

    @Inject
    @WithAttributes(shortName = 'v', label = "Vert.x version", type = InputType.DROPDOWN)
    private UISelectOne<String> vertxVersion;

    @Override
    public void initializeUI(UIBuilder uiBuilder) throws Exception {
	uiBuilder.add(vertxVersion);

	// Version default and values
	vertxVersion.setDefaultValue(() -> facet.getDefaultVertxVersion());
	vertxVersion.setValueChoices(() -> asList("3.0.0", "3.1.0", "3.2.1", "3.3.2", "3.3.3"));
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
	facet.setVertxVersion(vertxVersion.getValue());
	facet.setFaceted(getSelectedProject(uiExecutionContext.getUIContext()));
	facet.install();
	return Results.success("Vert.x project created successfully");
    }

    @Override
    public String name() {
	return "vertx-setup";
    }
}
