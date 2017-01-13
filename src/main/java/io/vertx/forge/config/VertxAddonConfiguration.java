package io.vertx.forge.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.forge.ForgeUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxAddonConfiguration {

    public static final String CONF_LOCATION_PROPERTY = "vertx-forge-addon-config";

    private static final VertxAddonConfiguration INSTANCE;
    private static final Properties VERSIONS;

    static {
        INSTANCE = load();
        VERSIONS = loadVersions();
    }

    private static VertxAddonConfiguration load() {
        File conf = new File("vertx-forge-addon-configuration.json");
        if (System.getProperty(CONF_LOCATION_PROPERTY) != null) {
            conf = new File(System.getProperty(CONF_LOCATION_PROPERTY));
        }

        InputStream stream;
        if (conf.isFile()) {
            // Loading the configuration file
            try {
                stream = new FileInputStream(conf);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Unable to load the vert.x addon configuration from the file "
                    + conf.getAbsolutePath(), e);
            }
        } else {
            stream = VertxAddonConfiguration.class.getClassLoader()
                .getResourceAsStream("vertx-addon-configuration.json");
        }

        ObjectMapper mapper = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);

        try {
            return mapper.readValue(stream, VertxAddonConfiguration.class)
                .verify()
                .initializeDependencyVersions();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the vert.x addon configuration", e);
        }

    }

    private VertxAddonConfiguration verify() {
        Objects.requireNonNull(version, "Vert.x version must be set in the configuration file");
        if (availableVersions.isEmpty()) {
            availableVersions = new ArrayList<>();
            availableVersions.add(version);
        }
        if (!availableVersions.contains(version)) {
            availableVersions.add(version);
        }
        return this;
    }

    private VertxAddonConfiguration initializeDependencyVersions() {
        getDependencies().forEach(dep -> {
            if (dep.getVersion().equalsIgnoreCase("VERTX_VERSION")) {
                dep.setVersion(version);
                return;
            }

            if (dep.getVersion().equalsIgnoreCase("VERTX_COMMUNITY_VERSION")) {
                if (communityVersion != null) {
                    dep.setVersion(communityVersion);
                } else {
                    dep.setVersion(version);
                }
            }
        });
        return this;
    }

    public static VertxAddonConfiguration config() {
        return Objects.requireNonNull(INSTANCE, "The vertx addon configuration has not been read");
    }

    private static Properties loadVersions() {
        URL url = ForgeUtils.class.getClassLoader().getResource("dependencies-version.properties");
        Objects.requireNonNull(url);

        Properties properties = new Properties();
        try (InputStream stream = url.openStream()) {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the 'dependencies-version.properties' file", e);
        }

        return properties;
    }


    String version;

    String communityVersion;

    List<String> availableVersions = new ArrayList<>();

    List<VertxDependency> dependencies = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public String getVersion(String key) {
        return VERSIONS.getProperty(key);
    }

    public VertxAddonConfiguration setVersion(String version) {
        this.version = version;
        return this;
    }

    public List<String> getAvailableVersions() {
        return availableVersions;
    }

    public VertxAddonConfiguration setAvailableVersions(List<String> availableVersions) {
        this.availableVersions = availableVersions;
        return this;
    }

    public List<VertxDependency> getDependencies() {
        return dependencies;
    }

    public VertxAddonConfiguration setDependencies(List<VertxDependency> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public String getCommunityVersion() {
        return communityVersion;
    }

    public VertxAddonConfiguration setCommunityVersion(String communityVersion) {
        this.communityVersion = communityVersion;
        return this;
    }
}
