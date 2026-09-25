package pl.fadedpearl.entity.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

public final class FadedDialogue {
    public static final String MEMORY_NBT_KEY = "faded_pearl:dialogue_memory";

    public enum Delivery {
        NORMAL(false), OVERLAY(true);

        private final boolean overlay;

        Delivery(boolean overlay) {
            this.overlay = overlay;
        }

        public boolean overlay() {
            return overlay;
        }
    }

    public record Descriptor(String key, Delivery delivery) {
        public Descriptor {
            if (key == null || key.isEmpty()) throw new IllegalArgumentException("Dialogue key cannot be empty");
            if (delivery == null) throw new IllegalArgumentException("Dialogue delivery cannot be null");
        }
    }

    public enum Category {
        HEALED("healed", "faded_pearl.dialogue.healed.", 3, 0, Delivery.NORMAL),
        DOWNED_RECOVERED("downed.recovered", "faded_pearl.dialogue.downed.recovered.", 3, 0, Delivery.NORMAL),
        FLOWER("flower", "faded_pearl.dialogue.flower.", 4, 0, Delivery.NORMAL),
        CARRY("carry", "faded_pearl.dialogue.carry.", 3, 0, Delivery.NORMAL),
        WEATHER_RAIN_ROOF("weather.rain_roof", "faded_pearl.dialogue.weather.rain_roof.", 3, 0, Delivery.NORMAL),
        WEATHER_SNOW("weather.snow", "faded_pearl.dialogue.weather.snow.", 3, 0, Delivery.NORMAL),
        GESTURE_JUMP_LOW("gesture.jump_low", "faded_pearl.dialogue.gesture.jump_low.", 3, 0, Delivery.NORMAL),
        GESTURE_JUMP_HIGH("gesture.jump_high", "faded_pearl.dialogue.gesture.jump_high.", 3, 0, Delivery.NORMAL),
        GESTURE_CROUCH_LOW("gesture.crouch_low", "faded_pearl.dialogue.gesture.crouch_low.", 3, 0, Delivery.NORMAL),
        GESTURE_CROUCH_HIGH("gesture.crouch_high", "faded_pearl.dialogue.gesture.crouch_high.", 3, 0, Delivery.NORMAL),
        GESTURE_STARE_LOW("gesture.stare_low", "faded_pearl.dialogue.gesture.stare_low.", 3, 0, Delivery.NORMAL),
        GESTURE_STARE_HIGH("gesture.stare_high", "faded_pearl.dialogue.gesture.stare_high.", 3, 0, Delivery.NORMAL),
        TOUCH_RECOIL("touch.recoil", "faded_pearl.dialogue.touch.recoil.", 3, 0, Delivery.NORMAL),
        TOUCH_HESITATE("touch.hesitate", "faded_pearl.dialogue.touch.hesitate.", 3, 0, Delivery.NORMAL),
        TOUCH_LEARNING("touch.learning", "faded_pearl.dialogue.touch.learning.", 3, 0, Delivery.NORMAL),
        TOUCH_DEVOTION("touch.devotion", "faded_pearl.dialogue.touch.devotion.", 3, 0, Delivery.NORMAL),
        TOUCH_TRUST("touch.trust", "faded_pearl.dialogue.touch.trust.", 3, 0, Delivery.NORMAL),
        TOUCH_AFFECTION("touch.affection", "faded_pearl.dialogue.touch.affection.", 3, 0, Delivery.NORMAL),
        TOUCH_GENTLE("touch.gentle", "faded_pearl.dialogue.touch.gentle.", 3, 0, Delivery.NORMAL),
        STRANGER_NEUTRAL("stranger.neutral", "dialogue.faded_pearl.stranger.neutral.", 3, 0, Delivery.NORMAL),
        FADE_MEETING("fade.meeting", "dialogue.faded_pearl.fade.meeting.", 3, 0, Delivery.NORMAL),
        WORLD_CAUTIOUS("world.cautious", "dialogue.faded_pearl.world.cautious_", 3, 1, Delivery.NORMAL),
        WORLD_LEARNING("world.learning", "dialogue.faded_pearl.world.learning_", 3, 1, Delivery.NORMAL),
        WORLD_FLOWER_SEEN("world.flower_seen", "dialogue.faded_pearl.world.flower_seen.", 5, 0, Delivery.NORMAL),
        WORLD_CONTAINER("world.container", "dialogue.faded_pearl.world.container.", 5, 0, Delivery.NORMAL),
        WORLD_FIRE("world.fire", "dialogue.faded_pearl.world.fire.", 5, 0, Delivery.NORMAL),
        WORLD_WATER("world.water", "dialogue.faded_pearl.world.water.", 5, 0, Delivery.NORMAL),
        WORLD_ORE("world.ore", "dialogue.faded_pearl.world.ore.", 5, 0, Delivery.NORMAL),
        WORLD_CRAFTING("world.crafting", "dialogue.faded_pearl.world.crafting.", 5, 0, Delivery.NORMAL),
        WORLD_CAVE("world.cave", "dialogue.faded_pearl.world.cave.", 5, 0, Delivery.NORMAL),
        WORLD_STARS("world.stars", "dialogue.faded_pearl.world.stars.", 5, 0, Delivery.NORMAL),
        WORLD_RAIN("world.rain", "dialogue.faded_pearl.world.rain.", 5, 0, Delivery.NORMAL),
        WORLD_ANIMAL("world.animal", "dialogue.faded_pearl.world.animal.", 5, 0, Delivery.NORMAL),
        WORLD_VILLAGER("world.villager", "dialogue.faded_pearl.world.villager.", 5, 0, Delivery.NORMAL),
        WORLD_HELD_ITEM("world.held_item", "dialogue.faded_pearl.world.held_item.", 5, 0, Delivery.NORMAL),
        ITEM_CURIOSITY("item.curiosity", "dialogue.faded_pearl.item_curiosity.", 5, 0, Delivery.NORMAL),
        COMMAND_FOLLOW("command.follow", "dialogue.faded_pearl.command.follow.", 3, 0, Delivery.OVERLAY),
        COMMAND_STAY("command.stay", "dialogue.faded_pearl.command.stay.", 3, 0, Delivery.OVERLAY),
        COMMAND_REST("command.rest", "dialogue.faded_pearl.command.rest.", 3, 0, Delivery.OVERLAY),
        COMMAND_HOME("command.home", "dialogue.faded_pearl.command.home.", 3, 0, Delivery.OVERLAY),
        AWARE_VILLAGE("awareness.village", "dialogue.faded_pearl.awareness.village.", 3, 0, Delivery.NORMAL),
        AWARE_PLAYER_DIAMOND("awareness.player_diamond", "dialogue.faded_pearl.awareness.player_diamond.", 3, 0, Delivery.NORMAL),
        AWARE_ENDERMAN_DIAMOND("awareness.enderman_diamond", "dialogue.faded_pearl.awareness.enderman_diamond.", 3, 0, Delivery.NORMAL),
        AWARE_ENDER_PEARL("awareness.ender_pearl", "dialogue.faded_pearl.awareness.ender_pearl.", 3, 0, Delivery.NORMAL),
        AWARE_ARMOR_UPGRADE("awareness.armor_upgrade", "dialogue.faded_pearl.awareness.armor_upgrade.", 3, 0, Delivery.NORMAL),
        AWARE_TAMED_WOLF("awareness.tamed_wolf", "dialogue.faded_pearl.awareness.tamed_wolf.", 3, 0, Delivery.NORMAL),
        AWARE_TAMED_CAT("awareness.tamed_cat", "dialogue.faded_pearl.awareness.tamed_cat.", 3, 0, Delivery.NORMAL),
        AWARE_TAMED_PARROT("awareness.tamed_parrot", "dialogue.faded_pearl.awareness.tamed_parrot.", 3, 0, Delivery.NORMAL),
        AWARE_TAMED_OTHER("awareness.tamed_other", "dialogue.faded_pearl.awareness.tamed_other.", 3, 0, Delivery.NORMAL),
        AWARE_PET_ANIMAL("awareness.pet_animal", "dialogue.faded_pearl.awareness.pet_animal.", 3, 0, Delivery.NORMAL),
        AWARE_BUILD("awareness.build", "dialogue.faded_pearl.awareness.build.", 3, 0, Delivery.NORMAL),
        NAME_LEARNED("name.learned", "dialogue.faded_pearl.name.learned.", 3, 0, Delivery.NORMAL),
        NAME_REPEAT("name.repeat", "dialogue.faded_pearl.name.repeat.", 3, 0, Delivery.NORMAL),
        NAMED_PET("name.pet", "dialogue.faded_pearl.name.pet.", 3, 0, Delivery.NORMAL),
        RESCUE_FALL("rescue.fall", "faded_pearl.dialogue.rescue.fall.", 3, 0, Delivery.NORMAL),
        RESCUE_LAVA("rescue.lava", "faded_pearl.dialogue.rescue.lava.", 3, 0, Delivery.NORMAL),
        RESCUE_HEALTH("rescue.health", "faded_pearl.dialogue.rescue.health.", 3, 0, Delivery.NORMAL);

        private final String id;
        private final String baseKey;
        private final int variants;
        private final int firstIndex;
        private final Delivery delivery;

        Category(String id, String baseKey, int variants, int firstIndex, Delivery delivery) {
            this.id = id;
            this.baseKey = baseKey;
            this.variants = variants;
            this.firstIndex = firstIndex;
            this.delivery = delivery;
        }

        public String id() {
            return id;
        }

        public String baseKey() {
            return baseKey;
        }

        public int variants() {
            return variants;
        }

        public int firstIndex() {
            return firstIndex;
        }

        public Delivery delivery() {
            return delivery;
        }
    }

    public static final class Memory {
        private final EnumMap<Category, Integer> lastVariants = new EnumMap<>(Category.class);

        public Descriptor next(RandomSource random, Category category) {
            if (category == null) throw new IllegalArgumentException("Dialogue category cannot be null");
            int previous = lastVariants.getOrDefault(category, -1);
            int selected = chooseVariantIndex(random, category.variants(), previous);
            lastVariants.put(category, selected);
            return variant(category.baseKey(), selected, category.variants(), category.firstIndex(), category.delivery());
        }

        public OptionalInt lastIndex(Category category) {
            Integer index = lastVariants.get(category);
            return index == null ? OptionalInt.empty() : OptionalInt.of(index);
        }

        public Map<Category, Integer> snapshot() {
            return Map.copyOf(lastVariants);
        }

        public void write(CompoundTag parent) {
            if (lastVariants.isEmpty()) {
                parent.remove(MEMORY_NBT_KEY);
                return;
            }
            CompoundTag memory = new CompoundTag();
            lastVariants.forEach((category, index) -> memory.putInt(category.id(), index));
            parent.put(MEMORY_NBT_KEY, memory);
        }

        public void read(CompoundTag parent) {
            lastVariants.clear();
            if (!parent.contains(MEMORY_NBT_KEY, Tag.TAG_COMPOUND)) return;
            CompoundTag memory = parent.getCompound(MEMORY_NBT_KEY);
            for (Category category : Category.values()) {
                if (!memory.contains(category.id(), Tag.TAG_INT)) continue;
                int index = memory.getInt(category.id());
                if (index >= 0 && index < category.variants()) lastVariants.put(category, index);
            }
        }
    }

    public static Descriptor normal(String key) {
        return new Descriptor(key, Delivery.NORMAL);
    }

    public static Descriptor overlay(String key) {
        return new Descriptor(key, Delivery.OVERLAY);
    }

    public static Descriptor variant(String baseKey, int index, int variants, int firstIndex, Delivery delivery) {
        if (variants <= 0) throw new IllegalArgumentException("Dialogue variants must be positive");
        if (index < 0 || index >= variants) throw new IndexOutOfBoundsException("Dialogue variant index out of range");
        return new Descriptor(baseKey + (firstIndex + index), delivery);
    }

    public static int chooseVariantIndex(RandomSource random, int variants, int previousIndex) {
        if (random == null) throw new IllegalArgumentException("Dialogue random source cannot be null");
        if (variants <= 0) throw new IllegalArgumentException("Dialogue variants must be positive");
        if (previousIndex < -1 || previousIndex >= variants)
            throw new IndexOutOfBoundsException("Previous dialogue variant index out of range");
        if (variants == 1) return 0;
        if (previousIndex < 0) return random.nextInt(variants);
        int selected = random.nextInt(variants - 1);
        return selected >= previousIndex ? selected + 1 : selected;
    }

    public static String variantKey(String baseKey, int index, int variants) {
        return variant(baseKey, index, variants, 0, Delivery.NORMAL).key();
    }

    public static void send(Player player, Descriptor dialogue) {
        sendComponent(player, dialogue, Component.translatable(dialogue.key()));
    }

    public static void send(Player player, Descriptor dialogue, String visibleName) {
        sendComponent(player, dialogue, withName(dialogue, visibleName));
    }

    public static Component withName(Descriptor dialogue, String visibleName) {
        Component safeName = Component.literal(NameLearningMemory.sanitizeName(visibleName));
        return Component.translatable(dialogue.key(), safeName);
    }

    private static void sendComponent(Player player, Descriptor dialogue, Component component) {
        if (dialogue.delivery().overlay() && player instanceof ServerPlayer serverPlayer)
            serverPlayer.sendSystemMessage(component, true);
        else
            player.sendSystemMessage(component);
    }

    public static void sendNormal(Player player, String key) {
        send(player, normal(key));
    }

    public static void sendOverlay(Player player, String key) {
        send(player, overlay(key));
    }

    public static Category worldReaction(String reaction) {
        return switch (reaction) {
            case "flower_seen" -> Category.WORLD_FLOWER_SEEN;
            case "container" -> Category.WORLD_CONTAINER;
            case "fire" -> Category.WORLD_FIRE;
            case "water" -> Category.WORLD_WATER;
            case "ore" -> Category.WORLD_ORE;
            case "crafting" -> Category.WORLD_CRAFTING;
            case "cave" -> Category.WORLD_CAVE;
            case "stars" -> Category.WORLD_STARS;
            case "rain" -> Category.WORLD_RAIN;
            case "animal" -> Category.WORLD_ANIMAL;
            case "villager" -> Category.WORLD_VILLAGER;
            case "held_item" -> Category.WORLD_HELD_ITEM;
            default -> throw new IllegalArgumentException("Unknown world reaction: " + reaction);
        };
    }

    private FadedDialogue() {}
}
