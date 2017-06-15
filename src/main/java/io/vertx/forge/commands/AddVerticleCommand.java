package io.vertx.forge.commands;

import static java.util.Arrays.asList;

import javax.inject.Inject;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import io.vertx.forge.VertxMavenFacet;
import io.vertx.forge.verticles.Verticles;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FacetConstraint({ VertxMavenFacet.class })
public class AddVerticleCommand extends AbstractVertxCommand {

    @Inject
    @WithAttributes(shortName = 'n', description = "The name of the verticle", required = true, name = "name",
        label = "Verticle Name", type = InputType.TEXTBOX)
    private UIInput<String> name;

    @Inject
    @WithAttributes(shortName = 't',
        name = "type",
        label = "Verticle type (language)",
        type = InputType.DROPDOWN,
        description = "The type of the verticle (language), deduced from the verticle name if not set.")
    private UISelectOne<String> type;

    @Inject
    @WithAttributes(shortName = 'm',
        name = "main",
        label = "Is the verticle the main verticle",
        defaultValue = "false",
        description = "Set it to `true` to make the created verticle the main verticle")
    private UIInput<Boolean> main;

    @Inject
    Verticles verticles;

    @Override
    public void initializeUI(UIBuilder uiBuilder) throws Exception {
        uiBuilder.add(name);
        uiBuilder.add(type);
        uiBuilder.add(main);

        type.setValueChoices(() -> asList("java", "javascript", "groovy", "ruby"));

    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        Project project = getSelectedProject(uiExecutionContext.getUIContext());
        VertxMavenFacet facet = factory.install(project, VertxMavenFacet.class);

        if (!facet.isInstalled()) {
            return Results.fail("The project is not a vert.x project, execute 'vertx-setup' before adding a verticle.");
        }

        String verticleType = type.getValue();
        if (verticleType == null || verticleType.isEmpty()) {
            verticleType = detectVerticleTypeFromVerticleName(name.getValue());
            System.out.println("Verticle Type (detected) : " + verticleType);
        }

        String packageName = null;
        String verticleName = name.getValue();
        if (verticleType.equalsIgnoreCase("java")) {
            packageName = extractPackageName(name.getValue());
            verticleName = extractJavaClassName(name.getValue());
        }

        String fileName = verticles.createNewVerticle(facet.getFaceted(), verticleName, packageName, verticleType,
            main.getValue());

        return Results.success("Verticle " + fileName + " created");
    }

    private String extractJavaClassName(String verticleName) {
        String tmp = verticleName;
        if (tmp.endsWith(".java")) {
            tmp = tmp.substring(0, ".java".length());
        }
        if (tmp.contains(".")) {
            return tmp.substring(tmp.indexOf(".") - 1);
        }
        return tmp;
    }

    private String extractPackageName(String verticleName) {
        String tmp = verticleName;
        if (tmp.endsWith(".java")) {
            tmp = tmp.substring(0, ".java".length());
        }
        if (tmp.contains(".")) {
            return tmp.substring(0, tmp.indexOf("."));
        }
        return null;
    }

    private static String detectVerticleTypeFromVerticleName(String verticleName) {
        if (verticleName.endsWith(".js")) {
            return "javascript";
        }
        if (verticleName.endsWith(".groovy")) {
            return "groovy";
        }
        if (verticleName.endsWith(".rb")) {
            return "ruby";
        }
        return "java";
    }

    @Override
    public String name() {
        return "vertx-add-verticle";
    }
}
