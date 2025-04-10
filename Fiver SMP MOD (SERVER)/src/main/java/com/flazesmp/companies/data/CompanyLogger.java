package com.flazesmp.companies.data;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CompanyLogger extends SavedData {
    private static final String DATA_NAME = FlazeSMP.MOD_ID + "_company_logs";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final List<CompanyLog> logs = new ArrayList<>();

    public CompanyLogger() {
    }

    public static CompanyLogger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(CompanyLogger::load, CompanyLogger::new, DATA_NAME);
    }

    public void addLog(int companyId, UUID actorId, CompanyLog.LogType type, String details) {
        CompanyLog log = new CompanyLog(companyId, actorId, type, details);
        logs.add(log);
        setDirty();

        // Also write to JSON file for easier external access
        writeLogToFile(log);
    }

    public List<CompanyLog> getCompanyLogs(int companyId, int limit) {
        // Get logs for a specific company, most recent first
        return logs.stream()
                .filter(log -> log.getCompanyId() == companyId)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
    }

    public List<CompanyLog> getCompanyLogsByType(int companyId, CompanyLog.LogType type, int limit) {
        // Get logs for a specific company and type, most recent first
        return logs.stream()
                .filter(log -> log.getCompanyId() == companyId && log.getType() == type)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
    }

    private void writeLogToFile(CompanyLog log) {
        try {
            // Create logs directory if it doesn't exist
            Path logsDir = FMLPaths.GAMEDIR.get().resolve("logs").resolve(FlazeSMP.MOD_ID).resolve("companies");
            Files.createDirectories(logsDir);

            // Create company-specific directory
            Path companyDir = logsDir.resolve("company_" + log.getCompanyId());
            Files.createDirectories(companyDir);

            // Create daily log file
            String today = LocalDate.now().format(DATE_FORMAT);
            File logFile = companyDir.resolve("log_" + today + ".json").toFile();

            // Read existing logs or create new array
            JsonArray logsArray;
            if (logFile.exists()) {
                String content = new String(Files.readAllBytes(logFile.toPath()));
                logsArray = GSON.fromJson(content, JsonArray.class);
            } else {
                logsArray = new JsonArray();
            }

            // Add new log
            logsArray.add(log.toJson());

            // Write back to file
            try (FileWriter writer = new FileWriter(logFile)) {
                GSON.toJson(logsArray, writer);
            }

            // Also update the consolidated log file
            updateConsolidatedLog(companyDir, log);

        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to write company log to file", e);
        }
    }

    private void updateConsolidatedLog(Path companyDir, CompanyLog log) {
        try {
            File consolidatedFile = companyDir.resolve("consolidated_log.json").toFile();

            // Read existing data or create new object
            JsonObject data;
            if (consolidatedFile.exists()) {
                String content = new String(Files.readAllBytes(consolidatedFile.toPath()));
                data = GSON.fromJson(content, JsonObject.class);
            } else {
                data = new JsonObject();
                data.addProperty("companyId", log.getCompanyId());
                data.add("logs", new JsonArray());
            }

            // Add new log to array
            JsonArray logsArray = data.getAsJsonArray("logs");
            logsArray.add(log.toJson());

            // Keep only the last 1000 logs to prevent the file from growing too large
            if (logsArray.size() > 1000) {
                JsonArray trimmedArray = new JsonArray();
                for (int i = logsArray.size() - 1000; i < logsArray.size(); i++) {
                    trimmedArray.add(logsArray.get(i));
                }
                data.add("logs", trimmedArray);
            }

            // Write back to file
            try (FileWriter writer = new FileWriter(consolidatedFile)) {
                GSON.toJson(data, writer);
            }

        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to update consolidated log file", e);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag logsTag = new ListTag();

        for (CompanyLog log : logs) {
            logsTag.add(log.save());
        }

        tag.put("Logs", logsTag);
        return tag;
    }

    public static CompanyLogger load(CompoundTag tag) {
        CompanyLogger logger = new CompanyLogger();

        if (tag.contains("Logs")) {
            ListTag logsTag = tag.getList("Logs", Tag.TAG_COMPOUND);

            for (int i = 0; i < logsTag.size(); i++) {
                CompoundTag logTag = logsTag.getCompound(i);
                logger.logs.add(new CompanyLog(logTag));
            }
        }

        return logger;
    }
}
