package pl.fadedpearl.entity.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;
import java.util.Set;

public final class WorldAwarenessMemory {
    public static final String NBT_KEY = "faded_pearl:world_awareness";

    public enum Milestone {
        VILLAGE, PLAYER_DIAMOND, ENDERMAN_DIAMOND, ENDER_PEARL, ARMOR_UPGRADE,
        TAMED_ANY, TAMED_WOLF, TAMED_CAT, TAMED_PARROT, TAMED_OTHER, BUILD_COMPLETED, PET_SMALL_ANIMAL
    }

    private final EnumSet<Milestone> seen = EnumSet.noneOf(Milestone.class);

    public boolean markFirst(Milestone milestone) {
        return seen.add(milestone);
    }

    public boolean hasSeen(Milestone milestone) {
        return seen.contains(milestone);
    }

    public Set<Milestone> snapshot() {
        return Set.copyOf(seen);
    }

    public void write(CompoundTag parent) {
        if (seen.isEmpty()) {
            parent.remove(NBT_KEY);
            return;
        }
        ListTag values = new ListTag();
        seen.forEach(value -> values.add(StringTag.valueOf(value.name())));
        parent.put(NBT_KEY, values);
    }

    public void read(CompoundTag parent) {
        seen.clear();
        if (!parent.contains(NBT_KEY, Tag.TAG_LIST)) return;
        ListTag values = parent.getList(NBT_KEY, Tag.TAG_STRING);
        for (int index = 0; index < values.size(); index++) {
            try {
                seen.add(Milestone.valueOf(values.getString(index)));
            }
            catch (IllegalArgumentException ignored) {
                // Unknown values belong to a newer or damaged save and are ignored safely.
            }
        }
    }
}
