package pl.fadedpearl.entity.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import pl.fadedpearl.entity.dialogue.FadedDialogue;
import pl.fadedpearl.entity.dialogue.NameLearningMemory;
import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;
import pl.fadedpearl.entity.journal.JournalMemory;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

public final class FadedPersistenceCodec {
    public static final int TRUST_SYSTEM_VERSION = 2;
    public static final String PEARL_GIVEN = "PearlGiven";
    public static final String HEALED = "Healed";
    public static final String HEALING_COLOR = "HealingColor";
    public static final String FLOWER_COLOR = "FlowerColor";
    public static final String FRIEND = "Friend";
    public static final String COMMAND = "Command";
    public static final String DOWNED = "Downed";
    public static final String DOWNED_TICKS = "DownedTicks";
    public static final String CRY_COOLDOWN = "CryCooldown";
    public static final String CRYING = "Crying";
    public static final String CRYING_TICKS = "CryingTicks";
    public static final String SEATED_YAW = "SeatedYaw";
    public static final String SEATED_YAW_SET = "SeatedYawSet";
    public static final String SOCIAL_ACTION = "SocialAction";
    public static final String SOCIAL_ACTION_TICKS = "SocialActionTicks";
    public static final String SOCIAL_ACTION_COOLDOWN = "SocialActionCooldown";
    public static final String CARRY_ACTION = "CarryAction";
    public static final String CARRY_ACTION_TICKS = "CarryActionTicks";
    public static final String CARRIED_BLINK_COOLDOWN = "CarriedBlinkCooldown";
    public static final String TRUST = "Trust";
    public static final String TRUST_LEVEL = "faded_pearl:trust_level";
    public static final String CROUCH_TIMER = "faded_pearl:crouch_timer";
    public static final String STARE_TIMER = "faded_pearl:stare_timer";
    public static final String JUMP_COUNT = "faded_pearl:jump_count";
    public static final String JUMP_WINDOW_TIMER = "faded_pearl:jump_window_timer";
    public static final String RAIN_SHELTERED = "faded_pearl:rain_sheltered";
    public static final String IS_DOWNED = "faded_pearl:is_downed";
    public static final String LAST_INTERACTION_TICK = "faded_pearl:last_interaction_tick";
    public static final String TRUST_INTERACTION_COOLDOWN = "faded_pearl:trust_interaction_cooldown";
    public static final String FLOWER_COOLDOWN = "faded_pearl:flower_cooldown";
    public static final String TRUST_SYSTEM_VERSION_KEY = "TrustSystemVersion";
    public static final String LAST_FRIEND_SEEN_TIME = "LastFriendSeenTime";
    public static final String HOME_POS = "HomePos";
    public static final String HOME_DIMENSION = "HomeDimension";
    public static final String CARRIED_FLOWER = "CarriedFlower";
    public static final String PROTECTOR_COOLDOWN = "ProtectorCooldown";
    public static final String WORLD_REACTION_COOLDOWN = "WorldReactionCooldown";
    public static final String LAST_WORLD_REACTION = "LastWorldReaction";
    public static final String RECOVERY_EPOCH = "RecoveryEpoch";
    public static final String CURIOSITY_STACK = "faded_pearl:curiosity_stack";
    public static final String CURIOSITY_SEEN = "faded_pearl:curiosity_seen";
    public static final String CURIOSITY_COOLDOWN = "faded_pearl:curiosity_cooldown";
    public static final String CURIOSITY_RETURN_POS = "faded_pearl:curiosity_return_pos";
    public static final String CURIOSITY_RETURN_DIMENSION = "faded_pearl:curiosity_return_dimension";
    public static final String ANIMAL_CARRY_COOLDOWN = "faded_pearl:animal_carry_cooldown";
    public static final String ANIMAL_SAME_COOLDOWN = "faded_pearl:animal_same_cooldown";
    public static final String ANIMAL_LAST_ID = "faded_pearl:animal_last_id";
    public static final String ESCAPE_PEARL_ARMED = "faded_pearl:escape_pearl_armed";
    public static final String RECOVERY_HEALTH = "RecoveryHealth";
    public static final String RECOVERY_ABSORPTION = "RecoveryAbsorption";
    public static final String RECOVERY_EFFECTS = "RecoveryEffects";
    public static final List<String> RECOVERY_KEYS = List.of(
            PEARL_GIVEN, HEALED, HEALING_COLOR, FLOWER_COLOR, FRIEND, COMMAND, DOWNED, DOWNED_TICKS,
            CRY_COOLDOWN, CRYING, CRYING_TICKS, SEATED_YAW, SEATED_YAW_SET, SOCIAL_ACTION,
            SOCIAL_ACTION_TICKS, SOCIAL_ACTION_COOLDOWN, CARRY_ACTION, CARRY_ACTION_TICKS,
            CARRIED_BLINK_COOLDOWN, TRUST, TRUST_LEVEL, CROUCH_TIMER, STARE_TIMER, JUMP_COUNT,
            JUMP_WINDOW_TIMER, RAIN_SHELTERED, IS_DOWNED, LAST_INTERACTION_TICK,
            TRUST_INTERACTION_COOLDOWN, FLOWER_COOLDOWN, TRUST_SYSTEM_VERSION_KEY, LAST_FRIEND_SEEN_TIME,
            HOME_POS, HOME_DIMENSION, CARRIED_FLOWER, PROTECTOR_COOLDOWN, WORLD_REACTION_COOLDOWN,
            LAST_WORLD_REACTION, RECOVERY_EPOCH, CURIOSITY_STACK, CURIOSITY_SEEN, CURIOSITY_COOLDOWN,
            CURIOSITY_RETURN_POS, CURIOSITY_RETURN_DIMENSION, ANIMAL_CARRY_COOLDOWN,
            ANIMAL_SAME_COOLDOWN, ANIMAL_LAST_ID, ESCAPE_PEARL_ARMED, FadedDialogue.MEMORY_NBT_KEY,
            WorldAwarenessMemory.NBT_KEY, NameLearningMemory.NBT_KEY, JournalMemory.NBT_KEY);

    private FadedPersistenceCodec() {}

    public static void write(CompoundTag tag, PersistentState state) {
        tag.putBoolean(PEARL_GIVEN, state.pearlGiven());
        tag.putBoolean(HEALED, state.healed());
        tag.putInt(HEALING_COLOR, state.healingColor());
        tag.putInt(FLOWER_COLOR, state.flowerColor());
        state.friendId().ifPresent(value -> tag.putUUID(FRIEND, value));
        tag.putInt(COMMAND, state.command());
        tag.putBoolean(DOWNED, state.downed());
        tag.putInt(DOWNED_TICKS, state.downedTicks());
        tag.putInt(CRY_COOLDOWN, state.cryCooldown());
        tag.putBoolean(CRYING, state.crying());
        tag.putInt(CRYING_TICKS, state.cryingTicks());
        tag.putFloat(SEATED_YAW, state.seatedYaw());
        tag.putBoolean(SEATED_YAW_SET, state.seatedYawSet());
        tag.putInt(SOCIAL_ACTION, state.socialAction());
        tag.putInt(SOCIAL_ACTION_TICKS, state.socialActionTicks());
        tag.putInt(SOCIAL_ACTION_COOLDOWN, state.socialActionCooldown());
        tag.putInt(CARRY_ACTION, state.carryAction());
        tag.putInt(CARRY_ACTION_TICKS, state.carryActionTicks());
        tag.putInt(CARRIED_BLINK_COOLDOWN, state.carriedBlinkCooldown());
        tag.putInt(TRUST, state.trust());
        tag.putInt(TRUST_LEVEL, state.trust());
        tag.putInt(CROUCH_TIMER, state.crouchTimer());
        tag.putInt(STARE_TIMER, state.stareTimer());
        tag.putInt(JUMP_COUNT, state.jumpCount());
        tag.putInt(JUMP_WINDOW_TIMER, state.jumpWindowTimer());
        tag.putBoolean(RAIN_SHELTERED, state.rainSheltered());
        tag.putBoolean(IS_DOWNED, state.downed());
        tag.putLong(LAST_INTERACTION_TICK, state.lastInteractionTick());
        tag.putInt(TRUST_INTERACTION_COOLDOWN, state.trustInteractionCooldown());
        tag.putInt(FLOWER_COOLDOWN, state.flowerCooldown());
        tag.putInt(TRUST_SYSTEM_VERSION_KEY, TRUST_SYSTEM_VERSION);
        tag.putLong(LAST_FRIEND_SEEN_TIME, state.lastFriendSeenTime());
        state.home().ifPresent(home -> {
            tag.putLong(HOME_POS, home.pos());
            tag.putString(HOME_DIMENSION, home.dimension());
        });
        state.carriedFlower().ifPresent(value -> tag.put(CARRIED_FLOWER, value.copy()));
        tag.putInt(PROTECTOR_COOLDOWN, state.protectorCooldown());
        tag.putInt(WORLD_REACTION_COOLDOWN, state.worldReactionCooldown());
        tag.putString(LAST_WORLD_REACTION, state.lastWorldReaction());
        tag.putInt(RECOVERY_EPOCH, state.recoveryEpoch());
    }

    public static DecodedState read(CompoundTag tag, IntUnaryOperator randomNextInt) {
        OptionalInt healingColor = tag.contains(FLOWER_COLOR) ? OptionalInt.of(tag.getInt(FLOWER_COLOR))
                : tag.contains(HEALING_COLOR) ? OptionalInt.of(tag.getInt(HEALING_COLOR)) : OptionalInt.empty();
        int cryCooldown = tag.contains(CRY_COOLDOWN) ? tag.getInt(CRY_COOLDOWN) : 40 + randomNextInt.applyAsInt(81);
        int trust = readTrust(tag);
        Optional<Home> home = tag.contains(HOME_POS) && tag.contains(HOME_DIMENSION)
                ? Optional.of(new Home(tag.getLong(HOME_POS), tag.getString(HOME_DIMENSION))) : Optional.empty();
        Optional<CompoundTag> carriedFlower = tag.contains(CARRIED_FLOWER)
                ? Optional.of(tag.getCompound(CARRIED_FLOWER)) : Optional.empty();
        return new DecodedState(tag.getBoolean(PEARL_GIVEN), tag.getBoolean(HEALED), healingColor,
                tag.hasUUID(FRIEND) ? Optional.of(tag.getUUID(FRIEND)) : Optional.empty(),
                optionalInt(tag, COMMAND), tag.getBoolean(DOWNED), tag.getInt(DOWNED_TICKS), cryCooldown,
                tag.getBoolean(CRYING), tag.getInt(CRYING_TICKS), tag.getBoolean(SEATED_YAW_SET),
                tag.getFloat(SEATED_YAW), optionalInt(tag, SOCIAL_ACTION), tag.getInt(SOCIAL_ACTION_TICKS),
                tag.contains(SOCIAL_ACTION_COOLDOWN) ? tag.getInt(SOCIAL_ACTION_COOLDOWN) : 80,
                optionalInt(tag, CARRY_ACTION), tag.getInt(CARRY_ACTION_TICKS), tag.getInt(CARRIED_BLINK_COOLDOWN),
                trust, tag.getInt(CROUCH_TIMER), tag.getInt(STARE_TIMER), tag.getInt(JUMP_COUNT),
                tag.getInt(JUMP_WINDOW_TIMER), tag.getBoolean(RAIN_SHELTERED),
                tag.contains(IS_DOWNED) ? Optional.of(tag.getBoolean(IS_DOWNED)) : Optional.empty(),
                tag.getLong(LAST_INTERACTION_TICK), Math.max(0, tag.getInt(TRUST_INTERACTION_COOLDOWN)),
                Math.max(0, tag.getInt(FLOWER_COOLDOWN)), tag.getLong(LAST_FRIEND_SEEN_TIME), home,
                carriedFlower, tag.getInt(PROTECTOR_COOLDOWN),
                tag.contains(WORLD_REACTION_COOLDOWN) ? tag.getInt(WORLD_REACTION_COOLDOWN) : 400,
                tag.getString(LAST_WORLD_REACTION), Math.max(0, tag.getInt(RECOVERY_EPOCH)));
    }

    /** Mirrors the save migration rule for read-only journal snapshots. */
    public static int readTrust(CompoundTag tag) {
        return tag.contains(TRUST_LEVEL) ? tag.getInt(TRUST_LEVEL)
                : tag.getInt(TRUST_SYSTEM_VERSION_KEY) >= TRUST_SYSTEM_VERSION ? tag.getInt(TRUST) : 0;
    }

    public static CompoundTag copyRecoveryKeys(CompoundTag raw) {
        CompoundTag safe = new CompoundTag();
        for (String key : RECOVERY_KEYS) {
            Tag value = raw.get(key);
            if (value != null) safe.put(key, value.copy());
        }
        return safe;
    }

    private static OptionalInt optionalInt(CompoundTag tag, String key) {
        return tag.contains(key) ? OptionalInt.of(tag.getInt(key)) : OptionalInt.empty();
    }

    public record Home(long pos, String dimension) {}

    public record PersistentState(boolean pearlGiven, boolean healed, int healingColor, int flowerColor,
            Optional<UUID> friendId, int command, boolean downed, int downedTicks, int cryCooldown,
            boolean crying, int cryingTicks, float seatedYaw, boolean seatedYawSet, int socialAction,
            int socialActionTicks, int socialActionCooldown, int carryAction, int carryActionTicks,
            int carriedBlinkCooldown, int trust, int crouchTimer, int stareTimer, int jumpCount,
            int jumpWindowTimer, boolean rainSheltered, long lastInteractionTick, int trustInteractionCooldown,
            int flowerCooldown, long lastFriendSeenTime, Optional<Home> home, Optional<CompoundTag> carriedFlower,
            int protectorCooldown, int worldReactionCooldown, String lastWorldReaction, int recoveryEpoch) {}

    public record DecodedState(boolean pearlGiven, boolean healed, OptionalInt healingColor,
            Optional<UUID> friendId, OptionalInt command, boolean downed, int downedTicks, int cryCooldown,
            boolean crying, int cryingTicks, boolean seatedYawSet, float seatedYaw, OptionalInt socialAction,
            int socialActionTicks, int socialActionCooldown, OptionalInt carryAction, int carryActionTicks,
            int carriedBlinkCooldown, int trust, int crouchTimer, int stareTimer, int jumpCount,
            int jumpWindowTimer, boolean rainSheltered, Optional<Boolean> downedOverride, long lastInteractionTick,
            int trustInteractionCooldown, int flowerCooldown, long lastFriendSeenTime, Optional<Home> home,
            Optional<CompoundTag> carriedFlower, int protectorCooldown, int worldReactionCooldown,
            String lastWorldReaction, int recoveryEpoch) {}
}
