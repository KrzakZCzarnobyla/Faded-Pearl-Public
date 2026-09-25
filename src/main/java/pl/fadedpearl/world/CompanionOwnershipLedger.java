package pl.fadedpearl.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Server-owned identity index. Recovery state remains separate until the 1.7 migration is complete. */
final class CompanionOwnershipLedger {
    private static final String OWNED = "Owned";
    private static final String UNCLAIMED = "Unclaimed";
    private static final String OWNER = "Owner";
    private static final String COMPANION = "Companion";

    enum ClaimResult { CLAIMED, ALREADY_OWNED, OWNER_HAS_COMPANION, COMPANION_OWNED, UNKNOWN_COMPANION }

    private final Map<UUID, UUID> ownerToCompanion = new HashMap<>();
    private final Map<UUID, UUID> companionToOwner = new HashMap<>();
    private final Set<UUID> unclaimed = new HashSet<>();

    Optional<UUID> companionOf(UUID owner) { return Optional.ofNullable(ownerToCompanion.get(owner)); }
    Optional<UUID> ownerOf(UUID companion) { return Optional.ofNullable(companionToOwner.get(companion)); }
    Set<UUID> ownedCompanionIds() { return Set.copyOf(companionToOwner.keySet()); }
    boolean isUnclaimed(UUID companion) { return unclaimed.contains(companion); }
    boolean hasUnclaimed() { return !unclaimed.isEmpty(); }
    int unclaimedCount() { return unclaimed.size(); }
    boolean removeUnclaimed(UUID companion) { return unclaimed.remove(companion); }

    boolean registerUnclaimed(UUID companion) {
        if (companionToOwner.containsKey(companion)) return false;
        return unclaimed.add(companion);
    }

    ClaimResult claim(UUID owner, UUID companion) {
        UUID existing = ownerToCompanion.get(owner);
        if (companion.equals(existing)) return ClaimResult.ALREADY_OWNED;
        if (existing != null) return ClaimResult.OWNER_HAS_COMPANION;
        if (companionToOwner.containsKey(companion)) return ClaimResult.COMPANION_OWNED;
        if (!unclaimed.remove(companion)) return ClaimResult.UNKNOWN_COMPANION;
        ownerToCompanion.put(owner, companion);
        companionToOwner.put(companion, owner);
        return ClaimResult.CLAIMED;
    }

    /** Imports only a verified pre-1.7 Friend; never overwrites either side of a relation. */
    boolean importLegacyOwner(UUID owner, UUID companion) {
        if (ownerToCompanion.containsKey(owner) || companionToOwner.containsKey(companion))
            return companion.equals(ownerToCompanion.get(owner)) && owner.equals(companionToOwner.get(companion));
        unclaimed.remove(companion);
        ownerToCompanion.put(owner, companion);
        companionToOwner.put(companion, owner);
        return true;
    }

    static CompanionOwnershipLedger load(CompoundTag tag) {
        CompanionOwnershipLedger ledger = new CompanionOwnershipLedger();
        ListTag owned = tag.getList(OWNED, Tag.TAG_COMPOUND);
        for (int i = 0; i < owned.size(); i++) {
            CompoundTag entry = owned.getCompound(i);
            if (entry.hasUUID(OWNER) && entry.hasUUID(COMPANION))
                ledger.importLegacyOwner(entry.getUUID(OWNER), entry.getUUID(COMPANION));
        }
        ListTag unclaimed = tag.getList(UNCLAIMED, Tag.TAG_COMPOUND);
        for (int i = 0; i < unclaimed.size(); i++) {
            CompoundTag entry = unclaimed.getCompound(i);
            if (entry.hasUUID(COMPANION)) ledger.registerUnclaimed(entry.getUUID(COMPANION));
        }
        return ledger;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag owned = new ListTag();
        ownerToCompanion.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag record = new CompoundTag();
            record.putUUID(OWNER, entry.getKey());
            record.putUUID(COMPANION, entry.getValue());
            owned.add(record);
        });
        tag.put(OWNED, owned);
        ListTag unclaimedEntries = new ListTag();
        unclaimed.stream().sorted().forEach(companion -> {
            CompoundTag record = new CompoundTag();
            record.putUUID(COMPANION, companion);
            unclaimedEntries.add(record);
        });
        tag.put(UNCLAIMED, unclaimedEntries);
        return tag;
    }
}
