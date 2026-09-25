package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent encounter sites, independent of where a healed companion later travels. */
final class CaveEncounterLedger {
    static final int MIN_HORIZONTAL_SPACING = 100;
    private final List<Site> sites = new ArrayList<>();

    record Site(UUID companion, String dimension, BlockPos pos) {}

    List<Site> sitesIn(String dimension) {
        return sites.stream().filter(site -> site.dimension.equals(dimension)).toList();
    }

    boolean contains(UUID companion) { return sites.stream().anyMatch(site -> site.companion.equals(companion)); }

    boolean canPlace(String dimension, BlockPos pos) {
        return canPlace(dimension, pos, MIN_HORIZONTAL_SPACING);
    }

    boolean canPlace(String dimension, BlockPos pos, int minimumHorizontalSpacing) {
        long minDistanceSq = (long) minimumHorizontalSpacing * minimumHorizontalSpacing;
        return sitesIn(dimension).stream().noneMatch(site -> {
            long dx = (long) site.pos.getX() - pos.getX();
            long dz = (long) site.pos.getZ() - pos.getZ();
            return dx * dx + dz * dz < minDistanceSq;
        });
    }

    boolean register(UUID companion, String dimension, BlockPos pos) {
        return register(companion, dimension, pos, MIN_HORIZONTAL_SPACING);
    }

    boolean register(UUID companion, String dimension, BlockPos pos, int minimumHorizontalSpacing) {
        if (contains(companion) || !canPlace(dimension, pos, minimumHorizontalSpacing)) return false;
        sites.add(new Site(companion, dimension, pos.immutable()));
        return true;
    }

    /** The exact cave origin is unavailable in pre-1.7 worlds; keep the best known site. */
    boolean importLegacy(UUID companion, String dimension, BlockPos pos) {
        if (contains(companion)) return false;
        sites.add(new Site(companion, dimension, pos.immutable()));
        return true;
    }

    boolean remove(UUID companion) { return sites.removeIf(site -> site.companion.equals(companion)); }

    static CaveEncounterLedger load(ListTag list) {
        CaveEncounterLedger ledger = new CaveEncounterLedger();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag record = list.getCompound(i);
            if (!record.hasUUID("Companion") || !record.contains("Dimension", Tag.TAG_STRING)
                    || !record.contains("Pos", Tag.TAG_LONG)) continue;
            ledger.importLegacy(record.getUUID("Companion"), record.getString("Dimension"),
                    BlockPos.of(record.getLong("Pos")));
        }
        return ledger;
    }

    ListTag save() {
        ListTag list = new ListTag();
        sites.stream().sorted(java.util.Comparator.comparing(Site::companion)).forEach(site -> {
            CompoundTag record = new CompoundTag();
            record.putUUID("Companion", site.companion);
            record.putString("Dimension", site.dimension);
            record.putLong("Pos", site.pos.asLong());
            list.add(record);
        });
        return list;
    }
}
