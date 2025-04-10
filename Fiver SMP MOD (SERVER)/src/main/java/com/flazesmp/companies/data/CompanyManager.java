// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is strictly prohibited.

package com.flazesmp.companies.data;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CompanyManager extends SavedData {
    private static final String DATA_NAME = FlazeSMP.MOD_ID + "_companies";
    private static CompanyManager INSTANCE;
    private static final String COMPANIES_DIR = "companies";
    private static final String COMPANIES_FILE = "companies.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Map<Integer, Company> companies = new HashMap<>();
    private final Map<UUID, Integer> playerCompanies = new HashMap<>();
    private final AtomicInteger nextCompanyId = new AtomicInteger(1);
    private final Set<Integer> usedCompanyIds = new HashSet<>();
    private final Random random = new Random();

    // Map of player UUID to company ID they're invited to
    private final Map<UUID, Integer> pendingInvitations = new HashMap<>();
    // Map of player UUID to their pending role in the company
    private final Map<UUID, CompanyRole> pendingInvitationRoles = new HashMap<>();

    // Map to store merge proposals (source company ID -> target company ID)
    private final Map<Integer, Integer> mergeProposals = new HashMap<>();

    public CompanyManager() {
        loadFromFile();
    }

    /**
     * Get the singleton instance
     */
    public static CompanyManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CompanyManager();
        }
        return INSTANCE;
    }

    /**
     * Get the manager from the server level
     */
    public static CompanyManager get(ServerLevel level) {
        if (INSTANCE == null) {
            INSTANCE = level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(CompanyManager::load, CompanyManager::new, DATA_NAME);
        }
        return INSTANCE;
    }

    /**
     * Load companies from file
     */
    private void loadFromFile() {
        File dataDir = FMLPaths.GAMEDIR.get().resolve("data").resolve(FlazeSMP.MOD_ID).toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            return;
        }

        File companiesDir = new File(dataDir, COMPANIES_DIR);
        if (!companiesDir.exists()) {
            companiesDir.mkdirs();
            return;
        }

        File companiesFile = new File(companiesDir, COMPANIES_FILE);

        if (companiesFile.exists()) {
            try (FileReader reader = new FileReader(companiesFile)) {
                Type listType = new TypeToken<ArrayList<Company>>() {
                }.getType();
                List<Company> loadedCompanies = GSON.fromJson(reader, listType);

                if (loadedCompanies != null) {
                    companies.clear();
                    playerCompanies.clear();
                    usedCompanyIds.clear();

                    int maxId = 0;

                    for (Company company : loadedCompanies) {
                        companies.put(company.getId(), company);
                        usedCompanyIds.add(company.getId());

                        // Update player to company mapping
                        for (UUID memberId : company.getMembers().keySet()) {
                            playerCompanies.put(memberId, company.getId());
                        }

                        // Track highest company ID
                        if (company.getId() > maxId) {
                            maxId = company.getId();
                        }
                    }

                    // Set next ID to be one higher than the highest existing ID
                    nextCompanyId.set(maxId + 1);

                    FlazeSMP.LOGGER.info("Loaded " + companies.size() + " companies from file");
                }
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load companies from file", e);
            }
        }
    }

    /**
     * Save companies to file
     */
    public void saveCompanies() {
        File dataDir = FMLPaths.GAMEDIR.get().resolve("data").resolve(FlazeSMP.MOD_ID).toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File companiesDir = new File(dataDir, COMPANIES_DIR);
        if (!companiesDir.exists()) {
            companiesDir.mkdirs();
        }

        File companiesFile = new File(companiesDir, COMPANIES_FILE);

        try (FileWriter writer = new FileWriter(companiesFile)) {
            GSON.toJson(new ArrayList<>(companies.values()), writer);
            FlazeSMP.LOGGER.info("Saved " + companies.size() + " companies to file");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save companies to file", e);
        }

        // Save individual company files for easier access
        for (Company company : companies.values()) {
            saveCompanyToFile(company, companiesDir);
        }
    }

    /**
     * Save a single company to its own file
     */
    private void saveCompanyToFile(Company company, File companiesDir) {
        File companyFile = new File(companiesDir, "company_" + company.getId() + ".json");

        try (FileWriter writer = new FileWriter(companyFile)) {
            GSON.toJson(company, writer);
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save company " + company.getId() + " to file", e);
        }
    }

    public Company createCompany(String name, UUID ownerId) {
        // Check if player already has a company
        if (playerCompanies.containsKey(ownerId)) {
            return null;
        }

        // Generate a random unique ID between 1-9999
        int id;
        do {
            id = 1 + random.nextInt(9999);
        } while (usedCompanyIds.contains(id));

        usedCompanyIds.add(id);

        Company company = new Company(id, name, ownerId);
        companies.put(id, company);
        playerCompanies.put(ownerId, id);
        setDirty();
        saveCompanies(); // Save to file
        return company;
    }

    public boolean disbandCompany(int companyId) {
        Company company = companies.get(companyId);
        if (company != null) {
            // Remove all player associations
            for (UUID memberId : company.getMembers().keySet()) {
                playerCompanies.remove(memberId);
            }

            companies.remove(companyId);
            usedCompanyIds.remove(companyId);

            // Remove any pending invitations to this company
            pendingInvitations.entrySet().removeIf(entry -> entry.getValue() == companyId);
            pendingInvitationRoles.keySet().removeIf(playerId -> !pendingInvitations.containsKey(playerId));

            setDirty();
            saveCompanies(); // Save to file
            return true;
        }
        return false;
    }

    public static Company getCompany(int companyId) {
        return companies.get(companyId);
    }

    public Company getPlayerCompany(UUID playerId) {
        Integer companyId = playerCompanies.get(playerId);
        if (companyId != null) {
            return companies.get(companyId);
        }
        return null;
    }

    public Collection<Company> getAllCompanies() {
        return Collections.unmodifiableCollection(companies.values());
    }

    public boolean invitePlayer(int companyId, UUID inviterId, UUID inviteeId, CompanyRole role) {
        Company company = companies.get(companyId);
        if (company == null) {
            return false;
        }

        // Check if inviter has permission
        if (!company.hasPermission(inviterId, CompanyPermission.INVITE_PLAYERS)) {
            return false;
        }

        // Check if invitee is already in a company
        if (playerCompanies.containsKey(inviteeId)) {
            return false;
        }

        // Add invitation with role information
        pendingInvitations.put(inviteeId, companyId);
        pendingInvitationRoles.put(inviteeId, role);
        setDirty();
        saveCompanies(); // Save to file
        return true;
    }

    public boolean hasInvitation(UUID playerId, int companyId) {
        Integer invitedCompanyId = pendingInvitations.get(playerId);
        return invitedCompanyId != null && invitedCompanyId == companyId;
    }

    public boolean acceptInvitation(UUID playerId, int companyId) {
        if (!hasInvitation(playerId, companyId)) {
            return false;
        }

        Company company = companies.get(companyId);
        if (company == null) {
            return false;
        }

        // Get the role the player was invited with
        CompanyRole role = pendingInvitationRoles.getOrDefault(playerId, CompanyRole.EMPLOYEE);

        // Add player with the specified role
        if (company.addMember(playerId, role)) {
            playerCompanies.put(playerId, companyId);
            pendingInvitations.remove(playerId);
            pendingInvitationRoles.remove(playerId);
            setDirty();
            saveCompanies(); // Save to file
            return true;
        }

        return false;
    }

    public boolean leaveCompany(UUID playerId) {
        Integer companyId = playerCompanies.get(playerId);
        if (companyId == null) {
            return false;
        }

        Company company = companies.get(companyId);
        if (company == null) {
            return false;
        }

        // CEO can't leave
        if (playerId.equals(company.getCeoId())) {
            return false;
        }

        if (company.removeMember(playerId)) {
            playerCompanies.remove(playerId);
            setDirty();
            saveCompanies(); // Save to file
            return true;
        }

        return false;
    }

    public boolean revokeInvitation(UUID playerId) {
        if (pendingInvitations.containsKey(playerId)) {
            pendingInvitations.remove(playerId);
            pendingInvitationRoles.remove(playerId);
            setDirty();
            saveCompanies(); // Save to file
            return true;
        }
        return false;
    }

    public void removePlayerFromCompany(UUID playerId) {
        playerCompanies.remove(playerId);
        setDirty();
        saveCompanies(); // Save to file
    }

    public void proposeMerge(int sourceCompanyId, int targetCompanyId) {
        mergeProposals.put(sourceCompanyId, targetCompanyId);
        setDirty();
        saveCompanies(); // Save to file
    }

    public boolean hasMergeProposal(int sourceCompanyId, int targetCompanyId) {
        Integer proposedTarget = mergeProposals.get(sourceCompanyId);
        return proposedTarget != null && proposedTarget == targetCompanyId;
    }

    public boolean executeMerge(int sourceCompanyId, int targetCompanyId) {
        if (!hasMergeProposal(sourceCompanyId, targetCompanyId)) {
            return false;
        }

        Company sourceCompany = companies.get(sourceCompanyId);
        Company targetCompany = companies.get(targetCompanyId);

        if (sourceCompany == null || targetCompany == null) {
            return false;
        }

        // Transfer funds
        targetCompany.addFunds(sourceCompany.getFunds());

        // Transfer CEO to associate role in target company
        targetCompany.addMember(sourceCompany.getCeoId(), CompanyRole.ASSOCIATE);

        // Transfer all members to target company
        for (Map.Entry<UUID, CompanyRole> entry : sourceCompany.getMembers().entrySet()) {
            UUID memberId = entry.getKey();

            // Skip CEO as they're already added
            if (memberId.equals(sourceCompany.getCeoId())) {
                continue;
            }

            // Add as employee
            targetCompany.addMember(memberId, CompanyRole.EMPLOYEE);

            // Update player company mapping
            playerCompanies.put(memberId, targetCompanyId);
        }

        // Remove the source company
        disbandCompany(sourceCompanyId);

        // Remove the merge proposal
        mergeProposals.remove(sourceCompanyId);
        setDirty();
        saveCompanies(); // Save to file

        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save to file first
        saveCompanies();

        tag.putInt("NextId", nextCompanyId.get());

        // Save used company IDs
        IntArrayTag usedIdsTag = new IntArrayTag(usedCompanyIds.stream().mapToInt(Integer::intValue).toArray());
        tag.put("UsedIds", usedIdsTag);

        // Save companies
        ListTag companiesTag = new ListTag();

        for (Company company : companies.values()) {
            CompoundTag companyTag = new CompoundTag();

            companyTag.putInt("Id", company.getId());
            companyTag.putString("Name", company.getName());
            companyTag.putString("CEO", company.getCeoId().toString());
            companyTag.putInt("Tier", company.getTier());
            companyTag.putString("Domain", company.getDomain());
            companyTag.putDouble("Funds", company.getFunds());

            // Save members
            ListTag membersTag = new ListTag();

            for (Map.Entry<UUID, CompanyRole> entry : company.getMembers().entrySet()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putString("UUID", entry.getKey().toString());
                memberTag.putString("Role", entry.getValue().name());
                membersTag.add(memberTag);
            }

            companyTag.put("Members", membersTag);

            // Save permissions
            ListTag permissionsTag = new ListTag();

            for (Map.Entry<UUID, CompanyRole> entry : company.getMembers().entrySet()) {
                UUID memberId = entry.getKey();

                // Skip CEO as they have all permissions by default
                if (memberId.equals(company.getCeoId())) {
                    continue;
                }

                CompoundTag permTag = new CompoundTag();
                permTag.putString("UUID", memberId.toString());

                ListTag permsListTag = new ListTag();

                for (CompanyPermission permission : company.getMemberPermissions(memberId)) {
                    permsListTag.add(StringTag.valueOf(permission.name()));
                }

                permTag.put("Perms", permsListTag);
                permissionsTag.add(permTag);
            }

            companyTag.put("Permissions", permissionsTag);

            // Save unlocked subdomains
            ListTag subdomainsTag = new ListTag();
            for (String subdomain : company.getUnlockedSubdomains()) {
                CompoundTag subdomainTag = new CompoundTag();
                subdomainTag.putString("Name", subdomain);
                subdomainsTag.add(subdomainTag);
            }
            companyTag.put("UnlockedSubdomains", subdomainsTag);

            // Save parent company ID
            if (company.hasParentCompany()) {
                companyTag.putInt("ParentCompanyId", company.getParentCompanyId());
            }

            // Save subsidiaries
            if (!company.getSubsidiaries().isEmpty()) {
                ListTag subsidiariesTag = new ListTag();
                for (Map.Entry<Integer, Integer> entry : company.getSubsidiaries().entrySet()) {
                    CompoundTag subsidiaryTag = new CompoundTag();
                    subsidiaryTag.putInt("Id", entry.getKey());
                    subsidiaryTag.putInt("SharePercentage", entry.getValue());
                    subsidiariesTag.add(subsidiaryTag);
                }
                companyTag.put("Subsidiaries", subsidiariesTag);
            }

            // Save maintenance data
            companyTag.putBoolean("MaintenanceWarningIssued", company.isMaintenanceWarningIssued());
            companyTag.putLong("MaintenanceWarningTime", company.getMaintenanceWarningTime());

            companiesTag.add(companyTag);
        }

        tag.put("Companies", companiesTag);

        // Save pending invitations
        ListTag invitationsTag = new ListTag();

        for (Map.Entry<UUID, Integer> entry : pendingInvitations.entrySet()) {
            CompoundTag inviteTag = new CompoundTag();
            inviteTag.putString("Player", entry.getKey().toString());
            inviteTag.putInt("Company", entry.getValue());

            // Save the role for this invitation
            CompanyRole role = pendingInvitationRoles.getOrDefault(entry.getKey(), CompanyRole.EMPLOYEE);
            inviteTag.putString("Role", role.name());

            invitationsTag.add(inviteTag);
        }

        tag.put("Invitations", invitationsTag);

        // Save merge proposals
        if (!mergeProposals.isEmpty()) {
            ListTag mergeProposalsTag = new ListTag();
            for (Map.Entry<Integer, Integer> entry : mergeProposals.entrySet()) {
                CompoundTag proposalTag = new CompoundTag();
                proposalTag.putInt("SourceCompany", entry.getKey());
                proposalTag.putInt("TargetCompany", entry.getValue());
                mergeProposalsTag.add(proposalTag);
            }
            tag.put("MergeProposals", mergeProposalsTag);
        }

        return tag;
    }

    public static CompanyManager load(CompoundTag tag) {
        CompanyManager manager = new CompanyManager();

        // Try to load from file first
        manager.loadFromFile();

        // If no companies were loaded from file, load from NBT
        if (manager.companies.isEmpty()) {
            if (tag.contains("NextId")) {
                manager.nextCompanyId.set(tag.getInt("NextId"));
            }

            // Load used company IDs
            if (tag.contains("UsedIds")) {
                int[] usedIds = tag.getIntArray("UsedIds");
                for (int id : usedIds) {
                    manager.usedCompanyIds.add(id);
                }
            }

            if (tag.contains("Companies")) {
                ListTag companiesTag = tag.getList("Companies", Tag.TAG_COMPOUND);

                for (int i = 0; i < companiesTag.size(); i++) {
                    CompoundTag companyTag = companiesTag.getCompound(i);
                    int id = companyTag.getInt("Id");
                    String name = companyTag.getString("Name");
                    UUID ceoId = UUID.fromString(companyTag.getString("CEO"));

                    Company company = new Company(id, name, ceoId);
                    company.setTier(companyTag.getInt("Tier"));
                    company.setDomain(companyTag.getString("Domain"));
                    company.addFunds(companyTag.getDouble("Funds"));

                    // Load members
                    if (companyTag.contains("Members")) {
                        ListTag membersTag = companyTag.getList("Members", Tag.TAG_COMPOUND);

                        for (int j = 0; j < membersTag.size(); j++) {
                            CompoundTag memberTag = membersTag.getCompound(j);
                            UUID memberId = UUID.fromString(memberTag.getString("UUID"));
                            CompanyRole role = CompanyRole.valueOf(memberTag.getString("Role"));

                            if (!memberId.equals(ceoId)) { // CEO is already added in constructor
                                company.addMember(memberId, role);
                            }

                            manager.playerCompanies.put(memberId, id);
                        }
                    }

                    // Load permissions
                    if (companyTag.contains("Permissions")) {
                        ListTag permissionsTag = companyTag.getList("Permissions", Tag.TAG_COMPOUND);

                        for (int j = 0; j < permissionsTag.size(); j++) {
                            CompoundTag permTag = permissionsTag.getCompound(j);
                            UUID memberId = UUID.fromString(permTag.getString("UUID"));

                            if (!memberId.equals(ceoId)) { // CEO has all permissions by default
                                ListTag permsListTag = permTag.getList("Perms", Tag.TAG_STRING);

                                for (int k = 0; k < permsListTag.size(); k++) {
                                    String permName = permsListTag.getString(k);
                                    CompanyPermission permission = CompanyPermission.valueOf(permName);
                                    company.setPermission(memberId, permission, true);
                                }
                            }
                        }
                    }

                    // Load unlocked subdomains
                    if (companyTag.contains("UnlockedSubdomains")) {
                        ListTag subdomainsTag = companyTag.getList("UnlockedSubdomains", Tag.TAG_COMPOUND);
                        for (int j = 0; j < subdomainsTag.size(); j++) {
                            CompoundTag subdomainTag = subdomainsTag.getCompound(j);
                            company.getUnlockedSubdomains().add(subdomainTag.getString("Name"));
                        }
                    } else if (companyTag.contains("Subdomain")) {
                        // For backward compatibility
                        String subdomain = companyTag.getString("Subdomain");
                        if (!subdomain.isEmpty()) {
                            company.setSubdomain(subdomain);
                        }
                    }

                    // Load parent company ID
                    if (companyTag.contains("ParentCompanyId")) {
                        company.setParentCompanyId(companyTag.getInt("ParentCompanyId"));
                    }

                    // Load subsidiaries
                    if (companyTag.contains("Subsidiaries")) {
                        ListTag subsidiariesTag = companyTag.getList("Subsidiaries", Tag.TAG_COMPOUND);
                        for (int j = 0; j < subsidiariesTag.size(); j++) {
                            CompoundTag subsidiaryTag = subsidiariesTag.getCompound(j);
                            int subsidiaryId = subsidiaryTag.getInt("Id");
                            int sharePercentage = subsidiaryTag.getInt("SharePercentage");
                            company.getSubsidiaries().put(subsidiaryId, sharePercentage);
                        }
                    }

                    // Load maintenance data
                    if (companyTag.contains("MaintenanceWarningIssued")) {
                        company.setMaintenanceWarningIssued(companyTag.getBoolean("MaintenanceWarningIssued"));
                    }
                    if (companyTag.contains("MaintenanceWarningTime")) {
                        company.setMaintenanceWarningTime(companyTag.getLong("MaintenanceWarningTime"));
                    }

                    manager.companies.put(id, company);
                }
            }

            // Load pending invitations
            if (tag.contains("Invitations")) {
                ListTag invitationsTag = tag.getList("Invitations", Tag.TAG_COMPOUND);

                for (int i = 0; i < invitationsTag.size(); i++) {
                    CompoundTag inviteTag = invitationsTag.getCompound(i);
                    UUID playerId = UUID.fromString(inviteTag.getString("Player"));
                    int companyId = inviteTag.getInt("Company");

                    manager.pendingInvitations.put(playerId, companyId);

                    // Load the role for this invitation
                    if (inviteTag.contains("Role")) {
                        CompanyRole role = CompanyRole.valueOf(inviteTag.getString("Role"));
                        manager.pendingInvitationRoles.put(playerId, role);
                    } else {
                        // Default to EMPLOYEE if no role is specified (for backward compatibility)
                        manager.pendingInvitationRoles.put(playerId, CompanyRole.EMPLOYEE);
                    }
                }
            }

            // Load merge proposals
            if (tag.contains("MergeProposals")) {
                ListTag mergeProposalsTag = tag.getList("MergeProposals", Tag.TAG_COMPOUND);
                for (int i = 0; i < mergeProposalsTag.size(); i++) {
                    CompoundTag proposalTag = mergeProposalsTag.getCompound(i);
                    int sourceCompanyId = proposalTag.getInt("SourceCompany");
                    int targetCompanyId = proposalTag.getInt("TargetCompany");
                    manager.mergeProposals.put(sourceCompanyId, targetCompanyId);
                }
            }

            // Save to file for future use
            manager.saveCompanies();
        }

        return manager;
    }

    /**
     * Mark data as dirty and save to file
     */
    @Override
    public void setDirty() {
        super.setDirty();
        saveCompanies();
    }
}