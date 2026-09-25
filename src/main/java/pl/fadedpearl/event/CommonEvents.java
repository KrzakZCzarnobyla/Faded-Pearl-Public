package pl.fadedpearl.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.item.PulsatingPearlItem;
import pl.fadedpearl.registry.ModEntities;
import pl.fadedpearl.registry.ModItems;
import pl.fadedpearl.world.FadedPearlSavedData;
import pl.fadedpearl.world.AnchorRecallService;
import pl.fadedpearl.world.CompanionRecoveryService;
import pl.fadedpearl.world.CaveConnectivity;
import pl.fadedpearl.config.FadedServerConfig;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID)
public final class CommonEvents {
    private static final Map<UUID, BuildObservation> BUILD_OBSERVATIONS = new HashMap<>();
    private static final Map<String, Integer> FLOWER_COLORS = Map.ofEntries(
            Map.entry("poppy", 0xE33A4E), Map.entry("blue_orchid", 0x4EBFFF),
            Map.entry("dandelion", 0xFFD83D), Map.entry("allium", 0xB870D6),
            Map.entry("azure_bluet", 0xE8EEF2), Map.entry("red_tulip", 0xD83B3B),
            Map.entry("orange_tulip", 0xF28C28), Map.entry("white_tulip", 0xF4F0EA),
            Map.entry("pink_tulip", 0xF28FB8), Map.entry("oxeye_daisy", 0xFFF4B0),
            Map.entry("cornflower", 0x466BDE), Map.entry("lily_of_the_valley", 0xF5F5F5),
            Map.entry("wither_rose", 0x4B3158), Map.entry("torchflower", 0xFF8A26)
    );

    @SubscribeEvent
    public static void onEntityTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        ServerLevel level = (ServerLevel) event.level;

        int encounterInterval = FadedServerConfig.CAVE_ENCOUNTER_CHECK_INTERVAL_TICKS.get();
        if (FadedServerConfig.CAVE_ENCOUNTERS_ENABLED.get() && level.dimension() == Level.OVERWORLD
                && level.getGameTime() % encounterInterval == 0) trySpawnCaveEncounter(level);

        if (level.getGameTime() % 10 == 0) {
            for (ServerPlayer player : level.players()) {
                for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(48),
                        item -> item.isInWater() && item.getItem().is(ModItems.EMPTY_PEARL.get()))) {
                    ItemStack result = new ItemStack(ModItems.WATER_FILLED_PEARL.get(), entity.getItem().getCount());
                    entity.setItem(result);
                }
            }
        }
        if (level.getGameTime() % 20 == 0) finishObservedBuilds(level);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AnchorRecallService.tick(event.getServer());
            CompanionRecoveryService.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onEntityTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof FadedEnderman companion && companion.isHealed()
                && event.getEntity().getServer() != null) {
            CompanionRecoveryService.noteTransition(event.getEntity().getServer(), companion);
        }
    }

    @SubscribeEvent
    public static void preventFadedEndermanFromBecomingPassenger(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof FadedEnderman) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void protectCarriedAnimalFromWall(LivingHurtEvent event) {
        if (event.getEntity() instanceof Animal animal
                && animal.getVehicle() instanceof FadedEnderman
                && event.getSource().is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AnchorRecallService.cancel(event.getEntity().getServer(), event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AnchorRecallService.clear(event.getServer());
        CompanionRecoveryService.clear(event.getServer());
        BUILD_OBSERVATIONS.clear();
    }

    private static void trySpawnCaveEncounter(ServerLevel level) {
        FadedPearlSavedData data = FadedPearlSavedData.get(level);
        if (data.hasUnlocatedLegacyEncounter() || data.hasUnresolvedLegacyOwner()) return;
        // A solo player with a still-wounded Fade must not get a second one on migration.
        if (level.getServer().getPlayerList().getPlayers().size() <= 1 && data.hasUnclaimedCompanion()) return;
        List<ServerPlayer> eligible = level.players().stream()
                .filter(player -> data.companionOf(player.getUUID()).isEmpty()).toList();
        if (eligible.isEmpty() || data.unclaimedCompanionCount() >= eligible.size()) return;
        ServerPlayer player = eligible.get(level.random.nextInt(eligible.size()));
        if (player.getY() <= FadedServerConfig.CAVE_ENCOUNTER_MAX_Y.get()
                && !level.canSeeSky(player.blockPosition())) {
            for (int attempt = 0; attempt < 24; attempt++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                int distance = 36 + level.random.nextInt(21);
                int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
                int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
                BlockPos column = new BlockPos(x, player.getBlockY() + 8, z);
                if (!level.hasChunkAt(column)) continue;
                BlockPos pos = findCaveFloor(level, column);
                if (pos == null) continue;
                int spacing = FadedServerConfig.CAVE_ENCOUNTER_MIN_HORIZONTAL_SPACING.get();
                if (!data.canPlaceCaveEncounter(level.dimension(), pos, spacing)
                        || sameKnownCave(level, data, pos)) continue;
                FadedEnderman enderman = ModEntities.FADED_ENDERMAN.get().create(level);
                if (enderman == null) return;
                enderman.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0);
                if (!level.noCollision(enderman)) continue;
                if (!data.reserveCaveEncounter(enderman.getUUID(), level.dimension(), pos, spacing)) continue;
                if (!level.addFreshEntity(enderman)) {
                    data.rollbackCaveEncounter(enderman.getUUID());
                    continue;
                }
                if (!data.hasSpawned()) data.markSpawned(enderman.getUUID());
                return;
            }
        }
    }

    private static boolean sameKnownCave(ServerLevel level, FadedPearlSavedData data, BlockPos candidate) {
        for (BlockPos previous : data.caveEncounterPositions(level.dimension())) {
            if (Math.abs(previous.getX() - candidate.getX()) > CaveConnectivity.LOCAL_HORIZONTAL_RANGE
                    || Math.abs(previous.getZ() - candidate.getZ()) > CaveConnectivity.LOCAL_HORIZONTAL_RANGE)
                continue;
            CaveConnectivity.Result result = CaveConnectivity.survey(candidate, previous,
                    level::hasChunkAt,
                    feet -> !level.canSeeSky(feet) && level.isEmptyBlock(feet)
                            && level.isEmptyBlock(feet.above()) && level.isEmptyBlock(feet.above(2)));
            if (result != CaveConnectivity.Result.DISTINCT) return true;
        }
        return false;
    }

    private static BlockPos findCaveFloor(ServerLevel level, BlockPos top) {
        for (int offset = 0; offset < 20; offset++) {
            BlockPos feet = top.below(offset);
            if (!level.getBlockState(feet.below()).isCollisionShapeFullBlock(level, feet.below())) continue;
            if (level.isEmptyBlock(feet) && level.isEmptyBlock(feet.above()) && level.isEmptyBlock(feet.above(2))
                    && !level.canSeeSky(feet)) return feet;
        }
        return null;
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.getCrafting().is(ModItems.PULSATING_PEARL.get())) return;
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack ingredient = event.getInventory().getItem(i);
            if (ingredient.is(ItemTags.SMALL_FLOWERS)) {
                Item item = ingredient.getItem();
                String path = ForgeRegistries.ITEMS.getKey(item).getPath();
                PulsatingPearlItem.setColor(event.getCrafting(), FLOWER_COLORS.getOrDefault(path, PulsatingPearlItem.DEFAULT_COLOR));
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onAnimalTamed(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player) || event.getAnimal().level().isClientSide()) return;
        for (FadedEnderman companion : player.serverLevel().getEntitiesOfClass(FadedEnderman.class,
                player.getBoundingBox().inflate(20.0D), candidate -> candidate.isFriend(player)))
            companion.observeTamedAnimal(event.getAnimal(), player);
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        BlockPos pos = event.getPos();
        BuildObservation current = BUILD_OBSERVATIONS.get(player.getUUID());
        if (current == null || current.level != level || now - current.lastPlacementTick > 2400L
                || current.anchor.distManhattan(pos) > 24) {
            BUILD_OBSERVATIONS.put(player.getUUID(), new BuildObservation(level, pos.immutable(), 1, now));
        }
        else {
            current.blocks++;
            current.lastPlacementTick = now;
        }
    }

    private static void finishObservedBuilds(ServerLevel level) {
        long now = level.getGameTime();
        var iterator = BUILD_OBSERVATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BuildObservation> entry = iterator.next();
            BuildObservation observation = entry.getValue();
            if (observation.level != level) continue;
            long idle = now - observation.lastPlacementTick;
            if (idle < 300L) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && observation.blocks >= 20) {
                for (FadedEnderman companion : level.getEntitiesOfClass(FadedEnderman.class,
                        player.getBoundingBox().inflate(24.0D), candidate -> candidate.isFriend(player)))
                    companion.observeCompletedBuild(player);
            }
            iterator.remove();
        }
    }

    private static final class BuildObservation {
        private final ServerLevel level;
        private final BlockPos anchor;
        private int blocks;
        private long lastPlacementTick;

        private BuildObservation(ServerLevel level, BlockPos anchor, int blocks, long lastPlacementTick) {
            this.level = level;
            this.anchor = anchor;
            this.blocks = blocks;
            this.lastPlacementTick = lastPlacementTick;
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        net.minecraft.world.phys.Vec3 sound = event.getExplosion().getPosition();
        for (FadedEnderman companion : event.getLevel().getEntitiesOfClass(FadedEnderman.class,
                new net.minecraft.world.phys.AABB(sound, sound).inflate(36.0D))) {
            companion.reactToDangerSound(sound);
        }
    }

    private CommonEvents() {}

    @Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.FADED_ENDERMAN.get(), FadedEnderman.createAttributes().build());
        }

        @SubscribeEvent
        public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
            event.accept(ModItems.EMPTY_PEARL);
            event.accept(ModItems.WATER_FILLED_PEARL);
            event.accept(ModItems.PULSATING_PEARL);
            event.accept(ModItems.ESCAPE_PEARL);
            event.accept(ModItems.ENDERMAN_TEAR);
            event.accept(ModItems.RESONATING_ANCHOR);
        }
    }
}
