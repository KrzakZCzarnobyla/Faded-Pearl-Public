package pl.fadedpearl.entity.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

public final class NameLearningMemory {
    public static final String NBT_KEY = "faded_pearl:name_learning";
    public static final int MAX_NAME_LENGTH = 64;
    public static final int MAX_PETS = 32;
    public static final int MIN_REPEAT_COOLDOWN = 2400;
    public static final int MAX_REPEAT_COOLDOWN = 4800;
    public static final int MIN_PET_REPEAT_COOLDOWN = 3600;
    public static final int MAX_PET_REPEAT_COOLDOWN = 7200;

    private static final String OWN_NAME = "OwnName";
    private static final String REPEAT_COOLDOWN = "RepeatCooldown";
    private static final String PETS = "Pets";
    private static final String PET_REPEAT_COOLDOWN = "PetRepeatCooldown";
    private static final String PET_ID = "Id";
    private static final String PET_NAME = "Name";

    private String ownName = "";
    private int repeatCooldown;
    private int petRepeatCooldown;
    private final LinkedHashMap<UUID, String> pets = new LinkedHashMap<>();

    public static String sanitizeName(String value) {
        if (value == null || value.isEmpty()) return "";
        int end = value.offsetByCodePoints(0, Math.min(MAX_NAME_LENGTH, value.codePointCount(0, value.length())));
        return value.substring(0, end);
    }

    public static boolean canObserve(boolean healed, boolean downed, boolean friendPresent,
                                     boolean friendNearby, boolean visible, boolean higherPriorityAction,
                                     boolean socialActionActive) {
        return healed && !downed && friendPresent && friendNearby && visible
                && !higherPriorityAction && !socialActionActive;
    }

    public static boolean isCalmForRepeat(boolean onGround, boolean unsafeEnvironment,
                                         boolean moving, boolean navigationActive) {
        return onGround && !unsafeEnvironment && !moving && !navigationActive;
    }

    public boolean isNewOwnName(String visibleName) {
        String safe = sanitizeName(visibleName);
        return !safe.isEmpty() && !safe.equals(ownName);
    }

    public void forgetOwnNameIfAbsent(String visibleName) {
        if (sanitizeName(visibleName).isEmpty()) {
            ownName = "";
            repeatCooldown = 0;
        }
    }

    public void rememberOwnName(String visibleName, IntUnaryOperator randomNextInt) {
        ownName = sanitizeName(visibleName);
        resetRepeatCooldown(randomNextInt);
    }

    public boolean canRepeatOwnName(String visibleName) {
        String safe = sanitizeName(visibleName);
        return repeatCooldown <= 0 && !safe.isEmpty() && safe.equals(ownName);
    }

    public void tickRepeatCooldown() {
        if (repeatCooldown > 0) repeatCooldown--;
    }

    public void ensureRepeatCooldown(IntUnaryOperator randomNextInt) {
        if (!ownName.isEmpty() && repeatCooldown <= 0) resetRepeatCooldown(randomNextInt);
    }

    public void resetRepeatCooldown(IntUnaryOperator randomNextInt) {
        if (randomNextInt == null) throw new IllegalArgumentException("Random source cannot be null");
        repeatCooldown = MIN_REPEAT_COOLDOWN
                + randomNextInt.applyAsInt(MAX_REPEAT_COOLDOWN - MIN_REPEAT_COOLDOWN + 1);
    }

    public boolean isNewPetName(UUID id, String visibleName) {
        String safe = sanitizeName(visibleName);
        return id != null && !safe.isEmpty() && !safe.equals(pets.get(id));
    }

    public void rememberPet(UUID id, String visibleName) {
        if (id == null) return;
        String safe = sanitizeName(visibleName);
        if (safe.isEmpty()) return;
        if (!pets.containsKey(id) && pets.size() >= MAX_PETS) {
            UUID oldest = pets.keySet().iterator().next();
            pets.remove(oldest);
        }
        pets.put(id, safe);
    }

    public void rememberPet(UUID id, String visibleName, IntUnaryOperator randomNextInt) {
        if (id == null || sanitizeName(visibleName).isEmpty()) return;
        if (randomNextInt == null) throw new IllegalArgumentException("Random source cannot be null");
        rememberPet(id, visibleName);
        resetPetRepeatCooldown(randomNextInt);
    }

    public boolean canRepeatPetName(UUID id, String visibleName) {
        String safe = sanitizeName(visibleName);
        return petRepeatCooldown <= 0 && id != null && !safe.isEmpty() && safe.equals(pets.get(id));
    }

    public void tickPetRepeatCooldown() {
        if (petRepeatCooldown > 0) petRepeatCooldown--;
    }

    public void ensurePetRepeatCooldown(IntUnaryOperator randomNextInt) {
        if (!pets.isEmpty() && petRepeatCooldown <= 0) resetPetRepeatCooldown(randomNextInt);
    }

    public void resetPetRepeatCooldown(IntUnaryOperator randomNextInt) {
        if (randomNextInt == null) throw new IllegalArgumentException("Random source cannot be null");
        petRepeatCooldown = MIN_PET_REPEAT_COOLDOWN
                + randomNextInt.applyAsInt(MAX_PET_REPEAT_COOLDOWN - MIN_PET_REPEAT_COOLDOWN + 1);
    }

    public Optional<String> ownName() {
        return ownName.isEmpty() ? Optional.empty() : Optional.of(ownName);
    }

    public int repeatCooldown() {
        return repeatCooldown;
    }

    public int petRepeatCooldown() {
        return petRepeatCooldown;
    }

    public Map<UUID, String> petSnapshot() {
        return Map.copyOf(pets);
    }

    public void write(CompoundTag parent) {
        if (ownName.isEmpty() && pets.isEmpty()) {
            parent.remove(NBT_KEY);
            return;
        }
        CompoundTag memory = new CompoundTag();
        if (!ownName.isEmpty()) {
            memory.putString(OWN_NAME, ownName);
            memory.putInt(REPEAT_COOLDOWN, repeatCooldown);
        }
        if (!pets.isEmpty()) {
            ListTag entries = new ListTag();
            pets.forEach((id, name) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(PET_ID, id);
                entry.putString(PET_NAME, name);
                entries.add(entry);
            });
            memory.put(PETS, entries);
            memory.putInt(PET_REPEAT_COOLDOWN, petRepeatCooldown);
        }
        parent.put(NBT_KEY, memory);
    }

    public void read(CompoundTag parent) {
        ownName = "";
        repeatCooldown = 0;
        petRepeatCooldown = 0;
        pets.clear();
        if (!parent.contains(NBT_KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag memory = parent.getCompound(NBT_KEY);
        if (memory.contains(OWN_NAME, Tag.TAG_STRING)) ownName = sanitizeName(memory.getString(OWN_NAME));
        if (!ownName.isEmpty() && memory.contains(REPEAT_COOLDOWN, Tag.TAG_INT))
            repeatCooldown = Math.max(0, memory.getInt(REPEAT_COOLDOWN));
        if (!memory.contains(PETS, Tag.TAG_LIST)) return;
        ListTag entries = memory.getList(PETS, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size() && pets.size() < MAX_PETS; index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.hasUUID(PET_ID) || !entry.contains(PET_NAME, Tag.TAG_STRING)) continue;
            String name = sanitizeName(entry.getString(PET_NAME));
            if (!name.isEmpty()) pets.put(entry.getUUID(PET_ID), name);
        }
        if (!pets.isEmpty() && memory.contains(PET_REPEAT_COOLDOWN, Tag.TAG_INT)) {
            petRepeatCooldown = Math.max(0,
                    Math.min(MAX_PET_REPEAT_COOLDOWN, memory.getInt(PET_REPEAT_COOLDOWN)));
        }
    }
}
