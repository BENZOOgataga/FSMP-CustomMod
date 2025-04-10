package com.flazesmp.companies.data;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CompanyLog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final UUID actorId;
    private final long timestamp;
    private final LogType type;
    private final String details;
    private final int companyId;

    public CompanyLog(int companyId, UUID actorId, LogType type, String details) {
        this.companyId = companyId;
        this.actorId = actorId;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.details = details;
    }

    public CompanyLog(CompoundTag tag) {
        this.companyId = tag.getInt("CompanyId");
        this.actorId = tag.getUUID("ActorId");
        this.timestamp = tag.getLong("Timestamp");
        this.type = LogType.valueOf(tag.getString("Type"));
        this.details = tag.getString("Details");
    }

    public UUID getActorId() {
        return actorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LogType getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public int getCompanyId() {
        return companyId;
    }

    public String getFormattedTimestamp() {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CompanyId", companyId);
        tag.putUUID("ActorId", actorId);
        tag.putLong("Timestamp", timestamp);
        tag.putString("Type", type.name());
        tag.putString("Details", details);
        return tag;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("companyId", companyId);
        json.addProperty("actorId", actorId.toString());
        json.addProperty("timestamp", timestamp);
        json.addProperty("formattedTime", getFormattedTimestamp());
        json.addProperty("type", type.name());
        json.addProperty("typeDescription", type.getDescription());
        json.addProperty("details", details);
        return json;
    }

    public enum LogType {
        MEMBER_JOIN("joined the company"),
        MEMBER_LEAVE("left the company"),
        MEMBER_FIRED("was fired from the company"),
        PROMOTION("was promoted"),
        DEMOTION("was demoted"),
        COMPANY_CREATED("created the company"),
        COMPANY_RENAMED("renamed the company"),
        OWNERSHIP_TRANSFER("transferred ownership"),
        COMPANY_DISBANDED("disbanded the company"),
        FINANCIAL("performed a financial action"),
        RESEARCH("invested in research"),
        PERMISSION_CHANGE("changed permissions"),
        DOMAIN_SELECTED("selected domain"),
        SUBDOMAIN_SELECTED("selected subdomain"),
        SUBDOMAIN_UNLOCKED("unlocked a subdomain"),
        UPGRADE_REQUESTED("requested a tier upgrade"),
        UPGRADE_APPROVED("approved a tier upgrade"),
        UPGRADE_DENIED("denied a tier upgrade"),
        SUBSIDIARY_CONTRACT("modified a subsidiary contract"),
        SUBSIDIARY_CONTRACT_CHANGED("changed a subsidiary contract"),
        MAINTENANCE_WARNING("received a maintenance warning");



        private final String description;

        LogType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
