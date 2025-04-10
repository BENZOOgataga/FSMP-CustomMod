// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CompanyTransaction {
    private final UUID actorId;
    private final long timestamp;
    private final TransactionType type;
    private final double amount;
    private final String details;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public CompanyTransaction(UUID actorId, TransactionType type, double amount, String details) {
        this.actorId = actorId;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.amount = amount;
        this.details = details;
    }

    public UUID getActorId() {
        return actorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getDetails() {
        return details;
    }

    public String getFormattedTimestamp() {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public enum TransactionType {
        DEPOSIT("deposited to company"),
        WITHDRAWAL("withdrew from company"),
        PAYMENT("paid from company funds"),
        INVESTMENT("invested in R&D"),
        SALARY("paid as salary"),
        REVENUE_SHARE("received as revenue share"),
        RESEARCH_INVESTMENT("invested in research");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
