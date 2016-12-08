/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.vertx.forge.project;

import java.util.Arrays;
import java.util.Collections;

import org.jboss.forge.addon.projects.AbstractProjectType;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import io.vertx.forge.VertxMavenFacet;
import io.vertx.forge.commands.AddDependencyCommand;
import io.vertx.forge.commands.SetupVertxCommand;

/**
 * Project type for Vert.x projects
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class VertxProjectType extends AbstractProjectType {

    @Override
    public Iterable<Class<? extends ProjectFacet>> getRequiredFacets() {
	return Collections.singleton(VertxMavenFacet.class);
    }

    @Override
    public NavigationResult next(UINavigationContext context) {
	NavigationResultBuilder builder = NavigationResultBuilder.create();
	builder.add(
		Metadata.forCommand(SetupVertxCommand.class).name("Vert.x: Setup").description("Setup Vert.x")
			.category(Categories.create("vert.x")),
		Arrays.asList(SetupVertxCommand.class, AddDependencyCommand.class));
	return builder.build();
    }

    @Override
    public String getType() {
	return "Vert.x";
    }

    @Override
    public int priority() {
	return 100;
    }

    @Override
    public String toString() {
	return "vert.x";
    }

}
