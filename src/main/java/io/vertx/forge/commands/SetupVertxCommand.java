package io.vertx.forge.commands;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import javax.inject.Inject;

import static java.util.Arrays.asList;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SetupVertxCommand extends AbstractVertxCommand {

  @Inject
  @WithAttributes(shortName = 'v', label = "Vert.x version", type = InputType.DROPDOWN)
  private UISelectOne<String> version;

  @Override
  public void initializeUI(UIBuilder uiBuilder) throws Exception {
    uiBuilder.add(version);

    //Version default and values
    version.setDefaultValue(() -> {
      return facet.getDefaultVertxVersion();
    });
    version.setValueChoices(() -> {
      return asList("3.0.0", "3.1.0", "3.2.0", "3.2.1");
    });
  }

  @Override
  public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
    facet.setVertxVersion(version.getValue());
    facet.setFaceted(getSelectedProject(uiExecutionContext.getUIContext()));
    facet.install();
    return Results.success("vert.x project created successfully");
  }

  @Override
  public String name() {
    return "vertx-setup";
  }
}
