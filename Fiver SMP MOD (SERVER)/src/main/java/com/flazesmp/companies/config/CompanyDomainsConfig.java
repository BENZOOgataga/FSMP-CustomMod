// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is strictly prohibited.

package com.flazesmp.companies.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.flazesmp.companies.FlazeSMP;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for company domains and subdomains
 * This class manages the domain configuration that is sent to clients
 */
public class CompanyDomainsConfig {
    private static CompanyDomainsConfig INSTANCE;
    private static final String CONFIG_FILE = "company_domains.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean showBuffsGlobally = true;
    private boolean defaultLockedState = false;
    private List<DomainConfig> domains = new ArrayList<>();

    // Private constructor for singleton
    private CompanyDomainsConfig() {
        // Default is just a placeholder message
        addDomain(
                createDomain("Please Setup Config", "minecraft:paper", "Edit the config file at config/flazesmp/company_domains.json")
        );
    }

    /**
     * Get the singleton instance
     */
    public static CompanyDomainsConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadConfig();
        }
        return INSTANCE;
    }

    /**
     * Load configuration from file
     */
    public static CompanyDomainsConfig loadConfig() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                CompanyDomainsConfig config = new CompanyDomainsConfig();
                config.domains.clear();

                // Load options
                if (json.has("options")) {
                    JsonObject options = json.getAsJsonObject("options");
                    if (options.has("showBuffsGlobally")) {
                        config.showBuffsGlobally = options.get("showBuffsGlobally").getAsBoolean();
                    }
                    if (options.has("defaultLockedState")) {
                        config.defaultLockedState = options.get("defaultLockedState").getAsBoolean();
                    }
                }

                // Load domains
                if (json.has("domains")) {
                    JsonArray domainsArray = json.getAsJsonArray("domains");
                    for (int i = 0; i < domainsArray.size(); i++) {
                        JsonObject domainJson = domainsArray.get(i).getAsJsonObject();

                        DomainConfig domain = new DomainConfig();
                        domain.name = domainJson.get("name").getAsString();
                        domain.icon = domainJson.get("icon").getAsString();
                        domain.description = domainJson.get("description").getAsString();

                        // Load subdomains
                        if (domainJson.has("subdomains")) {
                            JsonArray subdomainsArray = domainJson.getAsJsonArray("subdomains");
                            for (int j = 0; j < subdomainsArray.size(); j++) {
                                JsonObject subdomainJson = subdomainsArray.get(j).getAsJsonObject();

                                SubdomainConfig subdomain = new SubdomainConfig();
                                subdomain.name = subdomainJson.get("name").getAsString();
                                subdomain.icon = subdomainJson.get("icon").getAsString();
                                subdomain.buff = subdomainJson.get("buff").getAsString();
                                subdomain.locked = subdomainJson.has("locked") && subdomainJson.get("locked").getAsBoolean();
                                subdomain.showBuff = !subdomainJson.has("showBuff") || subdomainJson.get("showBuff").getAsBoolean();

                                domain.subdomains.add(subdomain);
                            }
                        }

                        config.domains.add(domain);
                    }
                }

                FlazeSMP.LOGGER.info("Loaded domain configuration with " + config.domains.size() + " domains");
                return config;
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load company domains config", e);
            }
        }

        // Create default config
        CompanyDomainsConfig config = new CompanyDomainsConfig();
        config.createDefaultConfigFile();
        FlazeSMP.LOGGER.info("Created default domain configuration template");

        return config;
    }

    /**
     * Create a default config file template
     */
    private void createDefaultConfigFile() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();

            // Create domains array
            JsonArray domainsArray = new JsonArray();

            // Add example domain
            JsonObject domain = new JsonObject();
            domain.addProperty("name", "Mining");
            domain.addProperty("icon", "minecraft:diamond_pickaxe");
            domain.addProperty("description", "Specializes in resource extraction");

            JsonArray subdomains = new JsonArray();

            JsonObject subdomain = new JsonObject();
            subdomain.addProperty("name", "Ore Extraction");
            subdomain.addProperty("icon", "minecraft:iron_ore");
            subdomain.addProperty("buff", "10% faster mining speed");
            subdomain.addProperty("locked", false);
            subdomain.addProperty("showBuff", true);

            subdomains.add(subdomain);
            domain.add("subdomains", subdomains);

            domainsArray.add(domain);

            // Add domains array to main JSON
            json.add("domains", domainsArray);

            // Add options
            JsonObject options = new JsonObject();
            options.addProperty("showBuffsGlobally", true);
            options.addProperty("defaultLockedState", false);
            json.add("options", options);

            // Write the JSON to file with pretty printing
            writer.write(GSON.toJson(json));

            FlazeSMP.LOGGER.info("Created domain configuration template");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to create default config file", e);
        }
    }

    /**
     * Save configuration to file
     */
    public void saveConfig() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();

            // Create domains array
            JsonArray domainsArray = new JsonArray();

            // Add all domains
            for (DomainConfig domain : domains) {
                JsonObject domainJson = new JsonObject();
                domainJson.addProperty("name", domain.name);
                domainJson.addProperty("icon", domain.icon);
                domainJson.addProperty("description", domain.description);

                JsonArray subdomainsArray = new JsonArray();

                // Add all subdomains
                for (SubdomainConfig subdomain : domain.subdomains) {
                    JsonObject subdomainJson = new JsonObject();
                    subdomainJson.addProperty("name", subdomain.name);
                    subdomainJson.addProperty("icon", subdomain.icon);
                    subdomainJson.addProperty("buff", subdomain.buff);
                    subdomainJson.addProperty("locked", subdomain.locked);
                    subdomainJson.addProperty("showBuff", subdomain.showBuff);

                    subdomainsArray.add(subdomainJson);
                }

                domainJson.add("subdomains", subdomainsArray);
                domainsArray.add(domainJson);
            }

            // Add domains array to main JSON
            json.add("domains", domainsArray);

            // Add options
            JsonObject options = new JsonObject();
            options.addProperty("showBuffsGlobally", showBuffsGlobally);
            options.addProperty("defaultLockedState", defaultLockedState);
            json.add("options", options);

            // Write the JSON to file with pretty printing
            writer.write(GSON.toJson(json));

            FlazeSMP.LOGGER.info("Saved domain configuration with " + domains.size() + " domains");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save company domains config", e);
        }
    }

    /**
     * Get all domains
     */
    public List<DomainConfig> getDomains() {
        return domains;
    }

    /**
     * Get a domain by name
     */
    public DomainConfig getDomain(String name) {
        for (DomainConfig domain : domains) {
            if (domain.name.equalsIgnoreCase(name)) {
                return domain;
            }
        }
        return null;
    }

    /**
     * Add a domain
     */
    public void addDomain(DomainConfig domain) {
        // Check if domain already exists
        for (int i = 0; i < domains.size(); i++) {
            if (domains.get(i).name.equalsIgnoreCase(domain.name)) {
                domains.set(i, domain); // Replace existing domain
                return;
            }
        }

        // Add new domain
        domains.add(domain);
    }

    /**
     * Remove a domain by name
     */
    public boolean removeDomain(String name) {
        for (int i = 0; i < domains.size(); i++) {
            if (domains.get(i).name.equalsIgnoreCase(name)) {
                domains.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new domain builder
     */
    public static DomainConfig createDomain(String name, String icon, String description) {
        DomainConfig domain = new DomainConfig();
        domain.name = name;
        domain.icon = icon;
        domain.description = description;
        return domain;
    }

    /**
     * Check if buffs should be shown globally
     */
    public boolean shouldShowBuffsGlobally() {
        return showBuffsGlobally;
    }

    /**
     * Get default locked state for new subdomains
     */
    public boolean getDefaultLockedState() {
        return defaultLockedState;
    }

    /**
     * Set whether to show buffs globally
     */
    public void setShowBuffsGlobally(boolean showBuffs) {
        this.showBuffsGlobally = showBuffs;
    }

    /**
     * Set default locked state for new subdomains
     */
    public void setDefaultLockedState(boolean locked) {
        this.defaultLockedState = locked;
    }

    /**
     * Domain configuration
     */
    public static class DomainConfig {
        public String name;
        public String icon;
        public String description;
        public List<SubdomainConfig> subdomains = new ArrayList<>();

        /**
         * Add a subdomain to this domain
         */
        public DomainConfig addSubdomain(String name, String icon, String buff, boolean locked) {
            return addSubdomain(name, icon, buff, locked, true);
        }

        /**
         * Add a subdomain to this domain with control over buff visibility
         */
        public DomainConfig addSubdomain(String name, String icon, String buff, boolean locked, boolean showBuff) {
            SubdomainConfig subdomain = new SubdomainConfig();
            subdomain.name = name;
            subdomain.icon = icon;
            subdomain.buff = buff;
            subdomain.locked = locked;
            subdomain.showBuff = showBuff;

            // Check if subdomain already exists
            for (int i = 0; i < subdomains.size(); i++) {
                if (subdomains.get(i).name.equalsIgnoreCase(name)) {
                    subdomains.set(i, subdomain); // Replace existing subdomain
                    return this;
                }
            }

            // Add new subdomain
            subdomains.add(subdomain);
            return this;
        }

        /**
         * Remove a subdomain by name
         */
        public boolean removeSubdomain(String name) {
            for (int i = 0; i < subdomains.size(); i++) {
                if (subdomains.get(i).name.equalsIgnoreCase(name)) {
                    subdomains.remove(i);
                    return true;
                }
            }
            return false;
        }

        /**
         * Get a subdomain by name
         */
        public SubdomainConfig getSubdomain(String name) {
            for (SubdomainConfig subdomain : subdomains) {
                if (subdomain.name.equalsIgnoreCase(name)) {
                    return subdomain;
                }
            }
            return null;
        }

        /**
         * Lock or unlock a subdomain
         */
        public boolean setSubdomainLocked(String name, boolean locked) {
            SubdomainConfig subdomain = getSubdomain(name);
            if (subdomain != null) {
                subdomain.locked = locked;
                return true;
            }
            return false;
        }

        /**
         * Update subdomain buff text
         */
        public boolean setSubdomainBuff(String name, String buff) {
            SubdomainConfig subdomain = getSubdomain(name);
            if (subdomain != null) {
                subdomain.buff = buff;
                return true;
            }
            return false;
        }

        /**
         * Update subdomain icon
         */
        public boolean setSubdomainIcon(String name, String icon) {
            SubdomainConfig subdomain = getSubdomain(name);
            if (subdomain != null) {
                subdomain.icon = icon;
                return true;
            }
            return false;
        }

        /**
         * Set whether to show buff for a subdomain
         */
        public boolean setSubdomainShowBuff(String name, boolean showBuff) {
            SubdomainConfig subdomain = getSubdomain(name);
            if (subdomain != null) {
                subdomain.showBuff = showBuff;
                return true;
            }
            return false;
        }
    }
    /**
     * Force reload the configuration from disk
     */
    public static void reloadConfig() {
        // Reset the instance to null so it will be reloaded from disk next time getInstance() is called
        INSTANCE = null;
        // Load the config immediately
        getInstance();
    }

    /**
     * Subdomain configuration
     */
    public static class SubdomainConfig {
        public String name;
        public String icon;
        public String buff;
        public boolean locked;
        public boolean showBuff = true;
    }
}
