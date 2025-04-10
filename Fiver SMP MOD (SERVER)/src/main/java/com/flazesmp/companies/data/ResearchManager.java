// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is strictly prohibited.

package com.flazesmp.companies.data;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class ResearchManager {
    private static ResearchManager INSTANCE;
    private static final String CONFIG_FILE = "company_research.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<ResearchProject> availableProjects = new ArrayList<>();
    private Map<Integer, ActiveResearch> activeResearch = new HashMap<>();
    private Map<Integer, List<CompletedResearch>> researchHistory = new HashMap<>();

    // Private constructor for singleton
    private ResearchManager() {
        loadProjects();
    }

    /**
     * Get the singleton instance
     */
    public static ResearchManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResearchManager();
        }
        return INSTANCE;
    }

    /**
     * Load research projects from config file
     */
    private void loadProjects() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Type listType = new TypeToken<ArrayList<ResearchProject>>(){}.getType();
                availableProjects = GSON.fromJson(reader, listType);
                FlazeSMP.LOGGER.info("Loaded " + availableProjects.size() + " research projects");
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load research projects", e);
                initializeDefaultProjects();
            }
        } else {
            initializeDefaultProjects();
            saveProjects();
        }
    }

    /**
     * Initialize default research projects
     */
    private void initializeDefaultProjects() {
        availableProjects = new ArrayList<>();

        // Add some default research projects
        availableProjects.add(new ResearchProject(
                "Efficient Mining",
                "minecraft:diamond_pickaxe",
                "Increases mining efficiency by 15%",
                5000.0,
                "mining",
                new HashMap<String, String>() {{
                    put("mining_efficiency", "15");
                }}
        ));

        availableProjects.add(new ResearchProject(
                "Advanced Farming",
                "minecraft:wheat",
                "Increases crop yield by 20%",
                5000.0,
                "agriculture",
                new HashMap<String, String>() {{
                    put("crop_yield", "20");
                }}
        ));

        availableProjects.add(new ResearchProject(
                "Market Connections",
                "minecraft:emerald",
                "Unlocks better prices at the market",
                7500.0,
                "any",
                new HashMap<String, String>() {{
                    put("market_discount", "10");
                }}
        ));

        availableProjects.add(new ResearchProject(
                "Resource Extraction",
                "minecraft:iron_ore",
                "Unlocks a new mining subdomain",
                10000.0,
                "mining",
                new HashMap<String, String>() {{
                    put("unlock_subdomain", "Deep Mining");
                }}
        ));

        FlazeSMP.LOGGER.info("Created " + availableProjects.size() + " default research projects");
    }

    /**
     * Save research projects to config file
     */
    public void saveProjects() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(availableProjects, writer);
            FlazeSMP.LOGGER.info("Saved " + availableProjects.size() + " research projects");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save research projects", e);
        }
    }

    /**
     * Get all available research projects
     */
    public List<ResearchProject> getAvailableProjects() {
        return new ArrayList<>(availableProjects);
    }

    /**
     * Get available projects for a specific company
     */
    public List<ResearchProject> getAvailableProjectsForCompany(Company company) {
        List<ResearchProject> result = new ArrayList<>();

        for (ResearchProject project : availableProjects) {
            // Filter by domain if specified
            if (!project.getDomain().equals("any") && !project.getDomain().equals(company.getDomain().toLowerCase())) {
                continue;
            }

            // Add project if it passes filters
            result.add(project);
        }

        return result;
    }

    /**
     * Add a new research project
     */
    public void addResearchProject(ResearchProject project) {
        availableProjects.add(project);
        saveProjects();
    }

    /**
     * Start research for a company
     */
    public boolean startResearch(int companyId, String projectName, UUID researcherId) {
        // Check if company already has active research
        if (activeResearch.containsKey(companyId)) {
            return false;
        }

        // Find the project
        ResearchProject project = null;
        for (ResearchProject p : availableProjects) {
            if (p.getName().equalsIgnoreCase(projectName)) {
                project = p;
                break;
            }
        }

        if (project == null) {
            return false;
        }

        // Create active research
        ActiveResearch research = new ActiveResearch(
                projectName,
                researcherId,
                System.currentTimeMillis(),
                project.getBaseCost()
        );

        activeResearch.put(companyId, research);
        return true;
    }

    /**
     * Complete research for a company
     */
    public CompletedResearch completeResearch(int companyId) {
        ActiveResearch active = activeResearch.get(companyId);
        if (active == null) {
            return null;
        }

        // Find the project
        ResearchProject project = null;
        for (ResearchProject p : availableProjects) {
            if (p.getName().equalsIgnoreCase(active.getProjectName())) {
                project = p;
                break;
            }
        }

        if (project == null) {
            return null;
        }

        // Create completed research
        CompletedResearch completed = new CompletedResearch(
                active.getProjectName(),
                active.getResearcherId(),
                active.getStartTime(),
                System.currentTimeMillis(),
                active.getCost(),
                new HashMap<>(project.getOutcomes())
        );

        // Add to history
        if (!researchHistory.containsKey(companyId)) {
            researchHistory.put(companyId, new ArrayList<>());
        }
        researchHistory.get(companyId).add(completed);

        // Remove active research
        activeResearch.remove(companyId);

        return completed;
    }

    /**
     * Get active research for a company
     */
    public ActiveResearch getActiveResearch(int companyId) {
        return activeResearch.get(companyId);
    }

    /**
     * Get research history for a company
     */
    public List<CompletedResearch> getResearchHistory(int companyId) {
        return researchHistory.getOrDefault(companyId, new ArrayList<>());
    }

    /**
     * Calculate research cost for a company based on previous research
     */
    public double calculateResearchCost(int companyId, ResearchProject project) {
        double baseCost = project.getBaseCost();
        int completedCount = researchHistory.getOrDefault(companyId, new ArrayList<>()).size();

        // Increase cost by 20% for each completed research
        return baseCost * (1 + (completedCount * 0.2));
    }

    /**
     * Research project definition
     */
    public static class ResearchProject {
        private String name;
        private String icon;
        private String description;
        private double baseCost;
        private String domain; // "any" or specific domain
        private Map<String, String> outcomes;

        public ResearchProject(String name, String icon, String description, double baseCost, String domain, Map<String, String> outcomes) {
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.baseCost = baseCost;
            this.domain = domain;
            this.outcomes = outcomes;
        }

        public String getName() { return name; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public double getBaseCost() { return baseCost; }
        public String getDomain() { return domain; }
        public Map<String, String> getOutcomes() { return outcomes; }
    }

    /**
     * Active research data
     */
    public static class ActiveResearch {
        private String projectName;
        private UUID researcherId;
        private long startTime;
        private double cost;

        public ActiveResearch(String projectName, UUID researcherId, long startTime, double cost) {
            this.projectName = projectName;
            this.researcherId = researcherId;
            this.startTime = startTime;
            this.cost = cost;
        }

        public String getProjectName() { return projectName; }
        public UUID getResearcherId() { return researcherId; }
        public long getStartTime() { return startTime; }
        public double getCost() { return cost; }
    }

    /**
     * Completed research data
     */
    public static class CompletedResearch {
        private String projectName;
        private UUID researcherId;
        private long startTime;
        private long completionTime;
        private double cost;
        private Map<String, String> outcomes;

        public CompletedResearch(String projectName, UUID researcherId, long startTime, long completionTime, double cost, Map<String, String> outcomes) {
            this.projectName = projectName;
            this.researcherId = researcherId;
            this.startTime = startTime;
            this.completionTime = completionTime;
            this.cost = cost;
            this.outcomes = outcomes;
        }

        public String getProjectName() { return projectName; }
        public UUID getResearcherId() { return researcherId; }
        public long getStartTime() { return startTime; }
        public long getCompletionTime() { return completionTime; }
        public double getCost() { return cost; }
        public Map<String, String> getOutcomes() { return outcomes; }
    }
}
