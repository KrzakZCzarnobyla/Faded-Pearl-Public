package pl.fadedpearl.entity.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.entity.dialogue.NameLearningMemory;
import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;
import pl.fadedpearl.entity.persistence.FadedPersistenceCodec;
import pl.fadedpearl.item.EndermanJournalItem;
import pl.fadedpearl.item.PulsatingPearlItem;
import pl.fadedpearl.network.ModNetwork;
import pl.fadedpearl.registry.ModItems;
import pl.fadedpearl.world.FadedPearlSavedData;

import java.util.Optional;
import java.util.UUID;

/** Server-authoritative lookup and bounded snapshot creation for journal opening. */
public final class EndermanJournalAccess {
    public static boolean open(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.ENDERMAN_JOURNAL.get())) return false;
        UUID companionId = EndermanJournalItem.companionId(stack).orElse(null);
        if (companionId == null) return false;
        FadedPearlSavedData data = FadedPearlSavedData.get(player.serverLevel());
        FadedEnderman loaded = findLoaded(player, companionId);
        if (loaded != null && loaded.isHealed() && loaded.isFriend(player)
                && data.ownerOf(companionId).isEmpty())
            data.reconcileLegacyOwner(companionId, player.getUUID());
        if (data.isDead(companionId) || !data.isHealed(companionId)
                || !data.companionOf(player.getUUID()).filter(companionId::equals).isPresent()) return false;

        EndermanJournalSnapshot snapshot;
        if (loaded != null) {
            if (loaded.getRecoveryEpoch() < data.recoveryEpoch(companionId)) return false;
            snapshot = loaded.createJournalSnapshot(player.getUUID()).orElse(null);
        }
        else snapshot = fromRecovery(player, data.companionSnapshot(companionId).orElse(null));
        if (snapshot == null) return false;
        ModNetwork.openEndermanJournal(player, snapshot);
        return true;
    }

    private static FadedEnderman findLoaded(ServerPlayer player, UUID companionId) {
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(companionId);
            if (entity instanceof FadedEnderman companion) return companion;
        }
        return null;
    }

    static EndermanJournalSnapshot fromRecovery(ServerPlayer player, CompoundTag tag) {
        if (tag == null || !tag.getBoolean(FadedPersistenceCodec.HEALED)
                || !tag.hasUUID(FadedPersistenceCodec.FRIEND)
                || !player.getUUID().equals(tag.getUUID(FadedPersistenceCodec.FRIEND))) return null;
        JournalMemory journal = new JournalMemory();
        journal.read(tag);
        WorldAwarenessMemory awareness = new WorldAwarenessMemory();
        awareness.read(tag);
        NameLearningMemory names = new NameLearningMemory();
        names.read(tag);
        boolean curiosityObserved = false;
        if (tag.contains(FadedPersistenceCodec.CURIOSITY_SEEN, Tag.TAG_LIST)) {
            ListTag values = tag.getList(FadedPersistenceCodec.CURIOSITY_SEEN, Tag.TAG_STRING);
            curiosityObserved = !values.isEmpty();
        }
        int color = tag.contains(FadedPersistenceCodec.FLOWER_COLOR, Tag.TAG_INT)
                ? tag.getInt(FadedPersistenceCodec.FLOWER_COLOR)
                : tag.contains(FadedPersistenceCodec.HEALING_COLOR, Tag.TAG_INT)
                ? tag.getInt(FadedPersistenceCodec.HEALING_COLOR) : PulsatingPearlItem.DEFAULT_COLOR;
        return EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                journal.snapshot(), awareness.snapshot(), curiosityObserved, names.ownName(),
                !names.petSnapshot().isEmpty(), tag.contains(FadedPersistenceCodec.HOME_POS, Tag.TAG_LONG)
                && tag.contains(FadedPersistenceCodec.HOME_DIMENSION, Tag.TAG_STRING), color,
                FadedPersistenceCodec.readTrust(tag)));
    }

    private EndermanJournalAccess() {}
}
