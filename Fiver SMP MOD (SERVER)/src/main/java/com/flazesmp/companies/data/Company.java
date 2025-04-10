// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.data;

import com.flazesmp.companies.config.CompanyConfig;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class Company {
    @Expose
    private final int id;
    @Expose
    private String name;
    @Expose
    private UUID ceoId;
    @Expose
    private int tier;
    @Expose
    private String domain;
    @Expose
    private String subdomain = "";
    @Expose
    private double funds;
    @Expose
    private final Map<UUID, CompanyRole> members;
    @Expose
    private final Map<UUID, Set<CompanyPermission>> memberPermissions;
    @Expose
    private boolean pendingUpgrade = false;
    @Expose
    private Set<String> unlockedSubdomains = new HashSet<>();
    @Expose
    private Map<String, String> perks = new HashMap<>();
    @Expose
    private int parentCompanyId = -1; // ID of the parent company (if this is a subsidiary)
    @Expose
    private Map<Integer, Integer> subsidiaries = new HashMap<>(); // Map of subsidiary company IDs to revenue share percentages
    @Expose
    private int subsidiaryProposalCompanyId = -1; // ID of the company proposing to make this a subsidiary
    @Expose
    private int subsidiaryProposalPercentage = 0; // Proposed revenue share percentage
    @Expose
    private long subsidiaryProposalExpiration = 0; // When the proposal expires
    @Expose
    private int shareEditProposalCompanyId = -1; // ID of the company proposing to edit share percentage
    @Expose
    private int shareEditProposalPercentage = 0; // Proposed new share percentage
    @Expose
    private boolean maintenanceWarningIssued = false;
    @Expose
    private long maintenanceWarningTime = 0;

    // Transient fields (not saved to file)
    private transient List<CompanyTransaction> transactions = new ArrayList<>();
    private transient Map<Integer, Long> subsidiaryProposals = new HashMap<>(); // Map of proposed subsidiary company IDs to expiration times
    private transient Map<Integer, Integer> shareEditProposals = new HashMap<>(); // Map of subsidiary company IDs to proposed new share percentages

    public Company(int id, String name, UUID ceoId) {
        this.id = id;
        this.name = name;
        this.ceoId = ceoId;
        this.tier = 1;
        this.domain = "Primary";
        this.funds = 0.0;
        this.members = new HashMap<>();
        this.memberPermissions = new HashMap<>();

        // Add CEO to members
        this.members.put(ceoId, CompanyRole.CEO);

        // Set default permissions for CEO (all permissions)
        Set<CompanyPermission> ceoPermissions = new HashSet<>(Arrays.asList(CompanyPermission.values()));
        this.memberPermissions.put(ceoId, ceoPermissions);
    }

    // Required for GSON deserialization
    private Company() {
        this.id = -1;
        this.name = "";
        this.ceoId = UUID.randomUUID();
        this.members = new HashMap<>();
        this.memberPermissions = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getCeoId() {
        return ceoId;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public double getFunds() {
        return funds;
    }

    public void addFunds(double amount) {
        this.funds += amount;
    }

    public boolean withdrawFunds(double amount) {
        if (amount <= this.funds) {
            this.funds -= amount;
            return true;
        }
        return false;
    }

    public Map<UUID, CompanyRole> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public boolean addMember(UUID playerId, CompanyRole role) {
        if (!members.containsKey(playerId)) {
            members.put(playerId, role);

            // Set default permissions based on role
            Set<CompanyPermission> permissions = new HashSet<>();
            if (role == CompanyRole.ASSOCIATE) {
                permissions.add(CompanyPermission.INVITE_PLAYERS);
                permissions.add(CompanyPermission.DEPOSIT_FUNDS);
            } else if (role == CompanyRole.EMPLOYEE) {
                permissions.add(CompanyPermission.DEPOSIT_FUNDS);
            }

            memberPermissions.put(playerId, permissions);
            return true;
        }
        return false;
    }

    public boolean removeMember(UUID playerId) {
        if (members.containsKey(playerId) && !playerId.equals(ceoId)) {
            members.remove(playerId);
            memberPermissions.remove(playerId);
            return true;
        }
        return false;
    }

    public CompanyRole getMemberRole(UUID playerId) {
        return members.getOrDefault(playerId, null);
    }

    public boolean hasPermission(UUID playerId, CompanyPermission permission) {
        if (!members.containsKey(playerId)) {
            return false;
        }

        // CEO always has all permissions
        if (playerId.equals(ceoId)) {
            return true;
        }

        Set<CompanyPermission> permissions = memberPermissions.getOrDefault(playerId, new HashSet<>());
        return permissions.contains(permission);
    }

    public void setPermission(UUID playerId, CompanyPermission permission, boolean enabled) {
        if (!members.containsKey(playerId) || playerId.equals(ceoId)) {
            return; // Can't modify CEO permissions or non-members
        }

        Set<CompanyPermission> permissions = memberPermissions.computeIfAbsent(playerId, k -> new HashSet<>());

        if (enabled) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }

    public Set<CompanyPermission> getMemberPermissions(UUID playerId) {
        if (playerId.equals(ceoId)) {
            return new HashSet<>(Arrays.asList(CompanyPermission.values()));
        }
        return new HashSet<>(memberPermissions.getOrDefault(playerId, new HashSet<>()));
    }

    public void transferOwnership(UUID newCeoId) {
        // Store old CEO's role
        CompanyRole oldCeoRole = CompanyRole.ASSOCIATE;

        // Set the old CEO to Associate
        if (members.containsKey(ceoId)) {
            members.put(ceoId, oldCeoRole);
        }

        // Set the new player as CEO
        members.put(newCeoId, CompanyRole.CEO);

        // Update the CEO ID
        this.ceoId = newCeoId;
    }

    public void setMemberRole(UUID memberId, CompanyRole role) {
        // Can't change CEO's role this way
        if (memberId.equals(ceoId) && role != CompanyRole.CEO) {
            return;
        }

        // Update the member's role
        if (members.containsKey(memberId)) {
            members.put(memberId, role);
        }
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
        if (subdomain != null && !subdomain.isEmpty()) {
            this.unlockedSubdomains.add(subdomain);
        }
    }

    // Add these methods to your Company class
    public void addTransaction(UUID actorId, CompanyTransaction.TransactionType type, double amount, String details) {
        transactions.add(new CompanyTransaction(actorId, type, amount, details));
    }

    public List<CompanyTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<CompanyTransaction> getTransactions(int limit) {
        if (limit <= 0 || limit > transactions.size()) {
            limit = transactions.size();
        }

        // Return the most recent transactions
        return transactions.subList(Math.max(0, transactions.size() - limit), transactions.size());
    }

    // Add this method to your Company class
    public void addLog(ServerLevel level, UUID actorId, CompanyLog.LogType type, String details) {
        CompanyLogger logger = CompanyLogger.get(level);
        logger.addLog(this.id, actorId, type, details);
    }

    public boolean isPendingUpgrade() {
        return pendingUpgrade;
    }

    public void setPendingUpgrade(boolean pendingUpgrade) {
        this.pendingUpgrade = pendingUpgrade;
    }

    public Set<String> getUnlockedSubdomains() {
        return unlockedSubdomains;
    }

    public Map<String, String> getPerks() {
        return perks;
    }

    public String getPerk(String key) {
        return perks.getOrDefault(key, null);
    }

    public void setPerk(String key, String value) {
        perks.put(key, value);
    }

    public boolean hasPerk(String key) {
        return perks.containsKey(key);
    }

    // Update the getter and setter
    public int getParentCompanyId() {
        return parentCompanyId;
    }

    public void setParentCompanyId(int parentId) {
        this.parentCompanyId = parentId;
    }

    // Add a helper method to check if company has a parent
    public boolean hasParentCompany() {
        return parentCompanyId != -1;
    }

    // Update the clearParentCompany method if it exists
    public void clearParentCompany() {
        this.parentCompanyId = -1;
    }

    public Map<Integer, Integer> getSubsidiaries() {
        return subsidiaries;
    }

    public boolean hasSubsidiaryProposal() {
        return subsidiaryProposalCompanyId != -1;
    }

    public int getSubsidiaryProposalCompanyId() {
        return subsidiaryProposalCompanyId;
    }

    public int getSubsidiaryProposalPercentage() {
        return subsidiaryProposalPercentage;
    }

    public boolean isSubsidiaryProposalExpired() {
        return System.currentTimeMillis() > subsidiaryProposalExpiration;
    }

    public void setSubsidiaryProposal(int companyId, int percentage, long expiration) {
        this.subsidiaryProposalCompanyId = companyId;
        this.subsidiaryProposalPercentage = percentage;
        this.subsidiaryProposalExpiration = expiration;
    }

    public void clearSubsidiaryProposal() {
        this.subsidiaryProposalCompanyId = -1;
        this.subsidiaryProposalPercentage = 0;
        this.subsidiaryProposalExpiration = 0;
    }

    public boolean hasShareEditProposal() {
        return shareEditProposalCompanyId != -1;
    }

    public int getShareEditProposalCompanyId() {
        return shareEditProposalCompanyId;
    }

    public int getShareEditProposalPercentage() {
        return shareEditProposalPercentage;
    }

    public void setShareEditProposal(int companyId, int percentage) {
        this.shareEditProposalCompanyId = companyId;
        this.shareEditProposalPercentage = percentage;
    }

    public void clearShareEditProposal() {
        this.shareEditProposalCompanyId = -1;
        this.shareEditProposalPercentage = 0;
    }

    public boolean isMaintenanceWarningIssued() {
        return maintenanceWarningIssued;
    }

    public void setMaintenanceWarningIssued(boolean maintenanceWarningIssued) {
        this.maintenanceWarningIssued = maintenanceWarningIssued;
    }

    public long getMaintenanceWarningTime() {
        return maintenanceWarningTime;
    }

    public void setMaintenanceWarningTime(long maintenanceWarningTime) {
        this.maintenanceWarningTime = maintenanceWarningTime;
    }

    public boolean meetsMaintenanceRequirements() {
        double minimumBalance = CompanyConfig.getInstance().calculateMinimumBalance(tier);
        return funds >= minimumBalance;
    }

    public double getMinimumRequiredBalance() {
        return CompanyConfig.getInstance().calculateMinimumBalance(tier);
    }

    // Helper method to initialize transactions list if it's null (after deserialization)
    private void ensureTransactionsInitialized() {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
    }

    // Helper method to initialize maps if they're null (after deserialization)
    private void ensureMapsInitialized() {
        if (unlockedSubdomains == null) {
            unlockedSubdomains = new HashSet<>();
        }
        if (perks == null) {
            perks = new HashMap<>();
        }
        if (subsidiaries == null) {
            subsidiaries = new HashMap<>();
        }
        if (subsidiaryProposals == null) {
            subsidiaryProposals = new HashMap<>();
        }
        if (shareEditProposals == null) {
            shareEditProposals = new HashMap<>();
        }
    }

    // This method is called after deserialization to initialize transient fields
    public void afterDeserialization() {
        ensureTransactionsInitialized();
        ensureMapsInitialized();

        // If subdomain is set but not in unlockedSubdomains, add it
        if (subdomain != null && !subdomain.isEmpty() && !unlockedSubdomains.contains(subdomain)) {
            unlockedSubdomains.add(subdomain);
        }
    }
}
