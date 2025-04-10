package com.flazesmp.companies.data;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class CompanyFileStorage {
    private static final String COMPANIES_DIR = "companies";
    private static final String COMPANIES_FILE = "companies.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Save all companies to file
     */
    public static void saveCompanies(Collection<Company> companies) {
        // Create directories if they don't exist
        File dataDir = FMLPaths.GAMEDIR.get().resolve("data").resolve(FlazeSMP.MOD_ID).toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File companiesDir = new File(dataDir, COMPANIES_DIR);
        if (!companiesDir.exists()) {
            companiesDir.mkdirs();
        }

        // Save main companies file
        File companiesFile = new File(companiesDir, COMPANIES_FILE);

        try (FileWriter writer = new FileWriter(companiesFile)) {
            GSON.toJson(companies, writer);
            FlazeSMP.LOGGER.info("Saved " + companies.size() + " companies to file");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save companies to file", e);
        }

        // Save individual company files for easier access
        for (Company company : companies) {
            saveCompanyToFile(company, companiesDir);
        }
    }

    /**
     * Save a single company to its own file
     */
    private static void saveCompanyToFile(Company company, File companiesDir) {
        File companyFile = new File(companiesDir, "company_" + company.getId() + ".json");

        try (FileWriter writer = new FileWriter(companyFile)) {
            GSON.toJson(company, writer);
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save company " + company.getId() + " to file", e);
        }
    }

    /**
     * Load all companies from file
     */
    public static List<Company> loadCompanies() {
        List<Company> companies = new ArrayList<>();

        File dataDir = FMLPaths.GAMEDIR.get().resolve("data").resolve(FlazeSMP.MOD_ID).toFile();
        if (!dataDir.exists()) {
            return companies;
        }

        File companiesDir = new File(dataDir, COMPANIES_DIR);
        if (!companiesDir.exists()) {
            return companies;
        }

        File companiesFile = new File(companiesDir, COMPANIES_FILE);

        if (companiesFile.exists()) {
            try (FileReader reader = new FileReader(companiesFile)) {
                Type listType = new TypeToken<ArrayList<Company>>(){}.getType();
                companies = GSON.fromJson(reader, listType);
                FlazeSMP.LOGGER.info("Loaded " + companies.size() + " companies from file");
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load companies from file", e);
            }
        }

        return companies;
    }
}
