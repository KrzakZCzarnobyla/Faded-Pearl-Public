package pl.fadedpearl.entity.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;
import java.util.Set;

/** Optional, bounded journal discoveries owned by one companion. */
public final class JournalMemory {
    public static final String NBT_KEY = "faded_pearl:journal_memory";

    public enum Discovery {
        DAY_MEETING,
        DAY_HEALING,
        BEHAVIOR_COMMANDS,
        BEHAVIOR_CARRY,
        BEHAVIOR_RESCUE,
        BEHAVIOR_WEATHER_REACTION,
        BEHAVIOR_SHARED_SHELTER,
        BEHAVIOR_SOCIAL_REPOSITION,
        BASIC_FADE_ORIGIN,
        BEHAVIOR_ANIMAL_CARRY,
        ESCAPE_PEARL_PLAYER_HIT,
        ESCAPE_PEARL_ENDER_PEARL_HELD
    }

    private final EnumSet<Discovery> discoveries = EnumSet.noneOf(Discovery.class);

    public boolean discover(Discovery discovery) {
        return discovery != null && discoveries.add(discovery);
    }

    public boolean has(Discovery discovery) {
        return discoveries.contains(discovery);
    }

    public Set<Discovery> snapshot() {
        return Set.copyOf(discoveries);
    }

    public void write(CompoundTag parent) {
        if (discoveries.isEmpty()) {
            parent.remove(NBT_KEY);
            return;
        }
        ListTag values = new ListTag();
        discoveries.forEach(value -> values.add(StringTag.valueOf(value.name())));
        parent.put(NBT_KEY, values);
    }

    public void read(CompoundTag parent) {
        discoveries.clear();
        if (!parent.contains(NBT_KEY, Tag.TAG_LIST)) return;
        ListTag values = parent.getList(NBT_KEY, Tag.TAG_STRING);
        for (int index = 0; index < values.size() && discoveries.size() < Discovery.values().length; index++) {
            try {
                discoveries.add(Discovery.valueOf(values.getString(index)));
            }
            catch (IllegalArgumentException ignored) {
                // Unknown values from newer or damaged saves are ignored safely.
            }
        }
    }
}
