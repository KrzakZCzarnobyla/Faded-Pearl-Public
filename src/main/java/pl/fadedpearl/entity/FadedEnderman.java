package pl.fadedpearl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.util.Mth;
import net.minecraftforge.common.util.ITeleporter;
import pl.fadedpearl.item.PulsatingPearlItem;
import pl.fadedpearl.entity.dialogue.FadedDialogue;
import pl.fadedpearl.entity.dialogue.NameLearningMemory;
import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;
import pl.fadedpearl.entity.journal.EndermanJournalPolicy;
import pl.fadedpearl.entity.journal.EndermanJournalSnapshot;
import pl.fadedpearl.entity.journal.JournalMemory;
import pl.fadedpearl.entity.journal.JournalNotificationPolicy;
import pl.fadedpearl.entity.behavior.FadedGestureDetector;
import pl.fadedpearl.entity.behavior.FadedWeatherDetector;
import pl.fadedpearl.entity.behavior.CompanionLivelinessPolicy;
import pl.fadedpearl.entity.animation.FadedAnimationState;
import pl.fadedpearl.entity.interaction.FadedInteractionHandler;
import pl.fadedpearl.entity.movement.FadedMovementCoordinator;
import pl.fadedpearl.entity.persistence.FadedPersistenceCodec;
import pl.fadedpearl.entity.protection.FadedProtectionController;
import pl.fadedpearl.entity.protection.EscapePearlPolicy;
import pl.fadedpearl.entity.sound.FadedVoiceController;
import pl.fadedpearl.entity.social.FadedSocialController;
import pl.fadedpearl.entity.social.SmallAnimalCarryPolicy;
import pl.fadedpearl.entity.trust.FadedTrustManager;
import pl.fadedpearl.entity.trust.TrustLossNotice;
import pl.fadedpearl.entity.behavior.CombatBlinkPolicy;
import pl.fadedpearl.entity.behavior.CompanionRecoveryStatus;
import pl.fadedpearl.entity.behavior.CompanionPerformancePolicy;
import pl.fadedpearl.entity.curiosity.ItemCuriosityPolicy;
import pl.fadedpearl.entity.curiosity.ItemCuriosityStateMachine;
import pl.fadedpearl.registry.ModItems;
import pl.fadedpearl.registry.ModSounds;
import pl.fadedpearl.world.FadedPearlSavedData;
import pl.fadedpearl.network.ModNetwork;
import pl.fadedpearl.block.ResonatingAnchorBlock;
import pl.fadedpearl.registry.ModBlocks;
import pl.fadedpearl.item.EndermanJournalItem;
import pl.fadedpearl.config.FadedServerConfig;
import pl.fadedpearl.world.AnchorRecallRewardPolicy;

import java.util.Optional;
import java.util.ArrayList;
import java.util.UUID;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.joml.Vector3f;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.custom.NearbyItemsSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;
import net.tslat.smartbrainlib.registry.SBLMemoryTypes;
import net.tslat.smartbrainlib.util.BrainUtils;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class FadedEnderman extends PathfinderMob implements GeoEntity, SmartBrainOwner<FadedEnderman> {
    private static final int WATER_BARRIER_SCAN_INTERVAL_TICKS = 10;
    private static final int WATER_BARRIER_MAX_CORRIDOR_SAMPLES = 32;
    private static final int WATER_BARRIER_LANDING_RADIUS = 5;
    private static final int WATER_BARRIER_LANDING_VERTICAL_RANGE = 6;
    private static final double FOLLOW_PROGRESS_DISTANCE_SQR = 0.04D;
    private static final double FOLLOW_DISTANCE_IMPROVEMENT_SQR = 0.25D;
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.faded_enderman.sit");
    private static final RawAnimation CRY = RawAnimation.begin().thenLoop("animation.faded_enderman.cry");
    private static final RawAnimation QUIET_CRY = RawAnimation.begin().thenLoop("animation.faded_enderman.cry_quiet");
    private static final RawAnimation NOTICE = RawAnimation.begin().thenLoop("animation.faded_enderman.notice");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.faded_enderman.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.faded_enderman.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.faded_enderman.run");
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("animation.faded_enderman.rest");
    private static final RawAnimation CURIOUS = RawAnimation.begin().thenLoop("animation.faded_enderman.curious");
    private static final RawAnimation LOOK_AROUND = RawAnimation.begin().thenLoop("animation.faded_enderman.look_around");
    private static final RawAnimation FLOWER_INTEREST = RawAnimation.begin().thenLoop("animation.faded_enderman.flower_interest");
    private static final RawAnimation HELD_ITEM = RawAnimation.begin().thenLoop("animation.faded_enderman.held_item");
    private static final RawAnimation PLAYER_GESTURE = RawAnimation.begin().thenLoop("animation.faded_enderman.player_gesture");
    private static final RawAnimation HEALING = RawAnimation.begin().thenPlay("animation.faded_enderman.healing");
    private static final RawAnimation TOUCH_WOUND = RawAnimation.begin().thenLoop("animation.faded_enderman.touch_wound");
    private static final RawAnimation HUG = RawAnimation.begin().thenLoop("animation.faded_enderman.hug");
    private static final RawAnimation PEEK = RawAnimation.begin().thenLoop("animation.faded_enderman.peek_corner");
    private static final RawAnimation AFFECTION = RawAnimation.begin().thenLoop("animation.faded_enderman.affection");
    private static final RawAnimation RAIN_NERVOUS = RawAnimation.begin().thenLoop("animation.faded_enderman.rain_nervous");
    private static final RawAnimation WORRIED = RawAnimation.begin().thenLoop("animation.faded_enderman.worried");
    private static final RawAnimation HOSTILE_WARNING = RawAnimation.begin().thenLoop("animation.faded_enderman.hostile_warning");
    private static final RawAnimation GROUND_FLOWER = RawAnimation.begin().thenLoop("animation.faded_enderman.ground_flower");
    private static final RawAnimation PICK_UP_PLAYER = RawAnimation.begin().thenPlay("animation.faded_enderman.pick_up_player");
    private static final RawAnimation CARRY_IDLE = RawAnimation.begin().thenLoop("animation.faded_enderman.carry_idle");
    private static final RawAnimation CARRY_WALK = RawAnimation.begin().thenLoop("animation.faded_enderman.carry_walk");
    private static final RawAnimation PUT_DOWN_PLAYER = RawAnimation.begin().thenPlay("animation.faded_enderman.put_down_player");
    private static final RawAnimation JUMP_REACT = RawAnimation.begin().thenPlay("animation.faded_enderman.jump_react");
    private static final RawAnimation CROUCH = RawAnimation.begin().thenPlay("animation.faded_enderman.crouch_down").thenLoop("animation.faded_enderman.crouch_idle");
    private static final RawAnimation STARE_FREEZE = RawAnimation.begin().thenPlay("animation.faded_enderman.stare_freeze");
    private static final RawAnimation STARE_TILT = RawAnimation.begin().thenPlay("animation.faded_enderman.stare_tilt");
    private static final RawAnimation TOUCH_RECOIL = RawAnimation.begin().thenPlay("animation.faded_enderman.touch_recoil");
    private static final RawAnimation TOUCH_HESITATE = RawAnimation.begin().thenPlay("animation.faded_enderman.touch_hesitate");
    private static final RawAnimation RAIN_SHELTER = RawAnimation.begin().thenLoop("animation.faded_enderman.rain_shelter");
    private static final RawAnimation RAIN_SHIVER = RawAnimation.begin().thenLoop("animation.faded_enderman.rain_shiver");
    private static final RawAnimation DOWNED_IDLE = RawAnimation.begin().thenLoop("animation.faded_enderman.downed_idle");
    private static final RawAnimation DOWNED_RECOVER = RawAnimation.begin().thenPlay("animation.faded_enderman.downed_recover");
    private static final RawAnimation CHEST_EXPOSE = RawAnimation.begin().thenPlay("animation.faded_enderman.chest_expose");
    private static final RawAnimation EMBRACE_READY = RawAnimation.begin().thenPlay("animation.faded_enderman.embrace_ready");
    private static final RawAnimation NIGHT_GAZE = RawAnimation.begin().thenPlay("animation.faded_enderman.night_gaze");
    private static final RawAnimation SNOW_CATCH = RawAnimation.begin().thenPlay("animation.faded_enderman.snow_catch");
    private static final RawAnimation WATCH_SLEEPING = RawAnimation.begin().thenLoop("animation.faded_enderman.watch_sleeping");
    private static final RawAnimation ITEM_POINT = RawAnimation.begin().thenLoop("animation.faded_enderman.item_point");
    private static final RawAnimation ITEM_INSPECT = RawAnimation.begin().thenLoop("animation.faded_enderman.item_inspect");
    private static final EntityDataAccessor<Boolean> HEALED = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HEALING_COLOR = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DOWNED = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DOWNED_SECONDS = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CRYING = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PLAYER_NEARBY = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SEATED_YAW = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SEATED_YAW_SET = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SOCIAL_ACTION = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CARRY_ACTION = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CARRY_MOVING = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HEALING_TICKS = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING_TO_FRIEND = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CARRIED_BLINK_COOLDOWN = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CURIOSITY_DISPLAY = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> SYNCED_TRUST = SynchedEntityData.defineId(FadedEnderman.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final FadedDialogue.Memory dialogueMemory = new FadedDialogue.Memory();
    private final WorldAwarenessMemory worldAwarenessMemory = new WorldAwarenessMemory();
    private final NameLearningMemory nameLearningMemory = new NameLearningMemory();
    private final JournalMemory journalMemory = new JournalMemory();
    private EndermanJournalSnapshot journalNotificationBaseline;
    private boolean pearlGiven;
    private UUID friendId;
    private int downedTicks;
    private int cryCooldown;
    private int cryingTicks;
    private int socialActionTicks;
    private int socialActionCooldown = 80;
    private float lastFriendHealth = Float.NaN;
    private Monster protectorScanAttacker;
    private int carryActionTicks;
    private boolean hadCarriedPassenger;
    private UUID animalCarryTarget;
    private int animalCarryApproachTicks;
    private int animalCarryTicks;
    private int animalCarryElapsedTicks;
    private int animalCarryCooldown;
    private UUID lastCarriedAnimalId;
    private int sameAnimalCooldown;
    private float animalCarryInitialHealth;
    private boolean animalCarryBoarding;
    private long lastCarriedBlinkRequestTick = Long.MIN_VALUE;
    private int trust;
    private long lastFriendSeenTime;
    private long lastAffectionClick;
    private BlockPos homePos;
    private ResourceKey<Level> homeDimension;
    private ItemStack carriedFlower = ItemStack.EMPTY;
    private int protectorCooldown;
    private int combatBlinkCooldown;
    private int trustInteractionCooldown;
    private int strangerReactionCooldown;
    private UUID strangerReactionTarget;
    private int fadeMeetingCooldown = 600;
    private int fadeMeetingTicks;
    private UUID fadeMeetingPartner;
    private SocialAction fadeMeetingAction = SocialAction.NONE;
    private int warningCooldown;
    private int growlCooldown;
    private LivingEntity hostileReactionTarget;
    private LivingEntity selfDefenseTarget;
    private int selfDefenseTicks;
    private int worldReactionCooldown = 400;
    private String lastWorldReaction = "";
    private int recoveryEpoch;
    private boolean escapePearlArmed;
    private Vec3 dangerSoundPos;
    private int dangerSoundTicks;
    private int crouchTimer, stareTimer, jumpCount, jumpWindowTimer;
    private boolean rainSheltered;
    private long lastInteractionTick;
    private int rainExposureTicks, shelteredTogetherTicks;
    private boolean friendWasAirborne, wasInWater, waterFollowPaused;
    private int nearbyLightCount;
    private boolean nearbyLightBaselineInitialized;
    private int flowerCooldown;
    private int mobReactionCooldown;
    private int ambientVoiceCooldown = FadedVoiceController.UNINITIALIZED_COOLDOWN;
    private ItemStack curiosityStack = ItemStack.EMPTY;
    private final Set<String> curiosityTrustGranted = new LinkedHashSet<>();
    private int curiosityCooldown;
    private boolean curiosityCooldownStartedThisTick;
    private int curiosityTicks;
    private CuriosityPhase curiosityPhase = CuriosityPhase.NONE;
    private UUID curiosityGroundTarget;
    private BlockPos curiosityGroundDestination;
    private Path curiosityGroundPath;
    private boolean curiosityGroundPathStarted;
    private String curiosityExpectedItem = "";
    private BlockPos curiosityReturnPos;
    private ResourceKey<Level> curiosityReturnDimension;
    private boolean awarenessBaselineInitialized;
    private boolean friendHadDiamond;
    private boolean friendHadEnderPearl;
    private int friendArmorValue;
    private int curiosityHintDelay;
    private boolean curiosityHintPending;
    private long lastDialogueTick = Long.MIN_VALUE;
    private long lastThoughtTick = Long.MIN_VALUE;
    private final List<PendingDialogue> deferredDialogues = new ArrayList<>();
    private int socialRepositionCooldown = 600;
    private int socialRepositionTicks;
    private BlockPos socialRepositionTarget;
    private Vec3 socialRepositionFriendOrigin;
    private Path socialRepositionPath;
    private boolean socialRepositionStarted;
    private boolean socialRepositionWatchFriend;

    private enum CuriosityPhase {
        NONE, POINT_HAND, APPROACH_GROUND, POINT_GROUND, INSPECT_GROUND, INSPECT, RETURN_PENDING
    }
    private final FadedMovementCoordinator movementCoordinator = new FadedMovementCoordinator();
    private FadedMovementCoordinator.State movementState = FadedMovementCoordinator.State.initial();
    private CompanionCommand movementCommand = CompanionCommand.FOLLOW;
    private final List<PendingLocomotion> pendingLocomotion = new ArrayList<>();
    private final List<PendingLook> pendingLook = new ArrayList<>();
    private final List<PendingTarget> pendingTarget = new ArrayList<>();
    private boolean interactionMovementStopLatched;
    private Vec3 lastFollowObservationPosition;
    private Vec3 lastFollowFriendPosition;
    private UUID lastObservedFriendId;
    private double lastFollowDistanceSqr = Double.NaN;
    private int waterBarrierScanTicks;
    private boolean cachedWaterBarrierEvidence;
    private Path cachedDryFollowPath;
    private UUID cachedDryFollowTarget;
    private boolean selfRescueSucceededSinceLastDecision;
    private FadedMovementCoordinator.RecoveryOutcome pendingBarrierRecoveryOutcome =
            FadedMovementCoordinator.RecoveryOutcome.NONE;

    private record PendingLocomotion(FadedMovementCoordinator.LocomotionCandidate candidate, Runnable apply) {}
    private record PendingLook(FadedMovementCoordinator.LookCandidate candidate, Runnable apply) {}
    private record PendingTarget(FadedMovementCoordinator.TargetCandidate candidate, Runnable apply) {}
    private record FollowMovementObservation(
            boolean measurableProgress,
            boolean friendMoved,
            boolean targetChanged,
            boolean pathReachable,
            boolean movementRequired,
            boolean evidence,
            boolean evidenceFresh,
            BlockPos recoveryLanding,
            FadedMovementCoordinator.RecoveryOutcome recoveryOutcome) {}
    private record DryRoute(BlockPos destination, Path path) {}
    private record PendingDialogue(UUID playerId, FadedDialogue.Descriptor dialogue, String visibleName) {}
    private enum ImmediateStopIntent { INTERACTION_LATCH, COMMAND_TRANSITION }

    public enum CompanionCommand {
        FOLLOW, STAY, REST, HOME;

        public CompanionCommand next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum SocialAction {
        NONE, CURIOUS, LOOK_AROUND, FLOWER_INTEREST, HELD_ITEM, PLAYER_GESTURE,
        RAIN_NERVOUS, WORRIED, GROUND_FLOWER, REST_NEAR, TOUCH_WOUND, PEEK_CORNER,
        PLAYFUL_TELEPORT, NIGHT_CLOSE, SOUND_ALERT, BRING_FLOWER, AFFECTION, HUG, GUARD,
        JUMP_REACT, CROUCH, STARE_FREEZE, STARE_TILT, TOUCH_RECOIL, TOUCH_HESITATE,
        RAIN_SHELTER, RAIN_SHIVER, DOWNED_RECOVER, CHEST_EXPOSE, EMBRACE_READY,
        NIGHT_GAZE, SNOW_CATCH, WATCH_SLEEPING, ITEM_POINT, ITEM_INSPECT
    }

    public enum TrustStage {
        TIER_0_FEAR, TIER_1_CAUTION, TIER_2_CURIOSITY, TIER_3_ACCEPTANCE,
        TIER_4_BOND, TIER_5_TRUST, TIER_6_PARTNER, TIER_7_DEVOTION;
        public static TrustStage from(int trust) {
            return values()[FadedTrustManager.stageIndex(trust)];
        }
    }

    public enum CarryAction {
        NONE, PICKING_UP, CARRYING, PUTTING_DOWN
    }

    public FadedEnderman(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    private void queueStop(FadedMovementCoordinator.Locomotion type, String reason) {
        queueLocomotion(type, null, 0.0D, reason, this::applyNavigationStop);
    }

    private void queueMoveTo(Entity target, double speed, String reason) {
        queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE, target.getStringUUID(), speed, reason,
                () -> applyMoveTo(target, speed));
    }

    private void queueMoveTo(Vec3 target, double speed, String reason) {
        queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE, target.toString(), speed, reason,
                () -> applyMoveTo(target, speed));
    }

    private void queueLocomotion(FadedMovementCoordinator.Locomotion type, String targetKey,
                                 double speed, String reason, Runnable apply) {
        if (level().isClientSide) return;
        pendingLocomotion.add(new PendingLocomotion(
                new FadedMovementCoordinator.LocomotionCandidate(type, targetKey, speed, reason), apply));
    }

    private void queueLookAt(Entity target, float yaw, float pitch, String reason) {
        FadedMovementCoordinator.Look type = target == getFriendPlayer()
                ? FadedMovementCoordinator.Look.LOOK_FRIEND
                : target instanceof Monster || target instanceof Enemy
                ? FadedMovementCoordinator.Look.LOOK_HOSTILE
                : FadedMovementCoordinator.Look.LOOK_SOCIAL_TARGET;
        queueLook(type, target.getStringUUID(), reason, () -> applyLookAt(target, yaw, pitch));
    }

    private void queueLookAt(Vec3 target, float yaw, float pitch, String reason) {
        queueLook(FadedMovementCoordinator.Look.LOOK_SOCIAL_TARGET, target.toString(), reason,
                () -> applyLookAt(target, yaw, pitch));
    }

    private void queueLook(FadedMovementCoordinator.Look type, String targetKey, String reason, Runnable apply) {
        if (level().isClientSide) return;
        pendingLook.add(new PendingLook(new FadedMovementCoordinator.LookCandidate(type, targetKey, reason), apply));
    }

    private void queueTarget(LivingEntity target, String reason) {
        if (level().isClientSide) return;
        pendingTarget.add(new PendingTarget(
                new FadedMovementCoordinator.TargetCandidate(FadedMovementCoordinator.TargetPolicy.SET_HOSTILE,
                        target.getStringUUID(), reason), () -> applyTarget(target)));
    }

    private void queueClearTarget(String reason) {
        if (level().isClientSide) return;
        pendingTarget.add(new PendingTarget(
                new FadedMovementCoordinator.TargetCandidate(FadedMovementCoordinator.TargetPolicy.CLEAR,
                        null, reason), () -> applyTarget(null)));
    }

    private boolean applyMoveTo(Entity target, double speed) {
        return getNavigation().moveTo(target, speed);
    }

    private boolean applyMoveTo(Path path, double speed) {
        return getNavigation().moveTo(path, speed);
    }

    private boolean applyMoveTo(Vec3 target, double speed) {
        return getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    private boolean applyMoveTo(BlockPos target, double yOffset, double speed) {
        return getNavigation().moveTo(target.getX() + .5D, target.getY() + yOffset, target.getZ() + .5D, speed);
    }

    private void applyLookAt(Entity target, float yaw, float pitch) {
        getLookControl().setLookAt(target, yaw, pitch);
    }

    private void applyLookAt(Vec3 target, float yaw, float pitch) {
        getLookControl().setLookAt(target.x, target.y, target.z, yaw, pitch);
    }

    private void applyLookAt(Vec3 target) { getLookControl().setLookAt(target); }

    private void applyTarget(LivingEntity target) { setTarget(target); }

    private void applyNavigationStop() {
        getNavigation().stop();
        getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private void applyImmediateStop(ImmediateStopIntent intent, String reason) {
        if (level().isClientSide) return;
        applyNavigationStop();
        if (intent == ImmediateStopIntent.INTERACTION_LATCH) {
            interactionMovementStopLatched = true;
            queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP, reason);
        }
    }

    public void requestInteractionMovementStop(String reason) {
        applyImmediateStop(ImmediateStopIntent.INTERACTION_LATCH, reason);
    }

    public void requestCommandTransitionStop(String reason) {
        applyImmediateStop(ImmediateStopIntent.COMMAND_TRANSITION, reason);
    }

    private void applyMovementDecision() {
        queueBrainMemoryCandidates();
        Player friend = getFriendPlayer();
        CompanionCommand command = getCommand();
        boolean commandChanged = command != movementCommand;
        FollowMovementObservation followObservation = observeFollowMovement(friend, command, commandChanged);
        movementState = new FadedMovementCoordinator.State(waterFollowPaused,
                movementState.noProgressTicks(), movementState.waterEvidenceTicks(),
                movementState.recoveryCooldownTicks(), movementState.selfRescuedInFollowEpisode());
        FadedMovementCoordinator.Snapshot snapshot = new FadedMovementCoordinator.Snapshot(
                FadedMovementCoordinator.Command.valueOf(command.name()), commandChanged,
                friend != null, friend != null && friend.isInWaterOrBubble(),
                friend != null && canFollowTeleportTo(friend), isInWaterOrBubble(),
                getControllingPassenger() instanceof Player, followObservation.measurableProgress(),
                followObservation.friendMoved(), followObservation.targetChanged(),
                followObservation.pathReachable(), followObservation.movementRequired(),
                followObservation.evidence(), followObservation.evidenceFresh(),
                followObservation.recoveryLanding() != null, selfRescueSucceededSinceLastDecision,
                followObservation.recoveryOutcome());
        FadedMovementCoordinator.Candidates candidates = new FadedMovementCoordinator.Candidates(
                pendingLocomotion.stream().map(PendingLocomotion::candidate).toList(),
                pendingLook.stream().map(PendingLook::candidate).toList(),
                pendingTarget.stream().map(PendingTarget::candidate).toList());
        FadedMovementCoordinator.Decision decision = movementCoordinator.resolve(snapshot, movementState, candidates);
        movementState = decision.nextState();
        waterFollowPaused = movementState.waterFollowPaused();
        movementCommand = command;
        selfRescueSucceededSinceLastDecision = false;

        pendingLocomotion.stream().filter(pending -> pending.candidate().equals(decision.locomotion()))
                .findFirst().ifPresent(pending -> pending.apply().run());
        pendingLook.stream().filter(pending -> pending.candidate().equals(decision.look()))
                .findFirst().ifPresent(pending -> pending.apply().run());
        pendingTarget.stream().filter(pending -> pending.candidate().equals(decision.targetPolicy()))
                .findFirst().ifPresent(pending -> pending.apply().run());
        interactionMovementStopLatched = false;
        pendingLocomotion.clear();
        pendingLook.clear();
        pendingTarget.clear();
    }

    private FollowMovementObservation observeFollowMovement(
            Player friend, CompanionCommand command, boolean commandChanged) {
        FadedMovementCoordinator.RecoveryOutcome outcome = pendingBarrierRecoveryOutcome;
        pendingBarrierRecoveryOutcome = FadedMovementCoordinator.RecoveryOutcome.NONE;
        boolean activeFollow = command == CompanionCommand.FOLLOW && friend != null && isHealed() && !isDowned()
                && !isVehicle() && !interactionMovementStopLatched;
        if (!activeFollow || commandChanged || friend.isInWaterOrBubble() || isInWaterOrBubble()) {
            resetFollowBarrierObservation();
            if (activeFollow && !commandChanged) lastObservedFriendId = friend.getUUID();
            return new FollowMovementObservation(false, false, false, false,
                    false, false, false, null, outcome);
        }

        double distanceSqr = distanceToSqr(friend);
        boolean movementRequired = distanceSqr > 36.0D;
        Vec3 currentPosition = position();
        Vec3 friendPosition = friend.position();
        boolean targetChanged = lastObservedFriendId != null && !lastObservedFriendId.equals(friend.getUUID());
        boolean firstObservation = targetChanged || lastFollowObservationPosition == null || lastFollowFriendPosition == null
                || !Double.isFinite(lastFollowDistanceSqr);
        boolean friendMoved = !firstObservation
                && friendPosition.distanceToSqr(lastFollowFriendPosition) >= FOLLOW_PROGRESS_DISTANCE_SQR;
        boolean selfRescueLatched = movementState.selfRescuedInFollowEpisode();
        boolean measurableProgress = firstObservation && !targetChanged
                || !selfRescueLatched && (currentPosition.distanceToSqr(lastFollowObservationPosition)
                >= FOLLOW_PROGRESS_DISTANCE_SQR
                || lastFollowDistanceSqr - distanceSqr >= FOLLOW_DISTANCE_IMPROVEMENT_SQR);
        if (targetChanged || friendMoved) clearFollowBarrierEpisodeCache();
        lastFollowObservationPosition = currentPosition;
        lastFollowFriendPosition = friendPosition;
        lastObservedFriendId = friend.getUUID();
        lastFollowDistanceSqr = distanceSqr;

        Path path = getNavigation().getPath();
        boolean pathUsesWater = pathUsesWater(path);
        boolean pathReachable = path != null && path.canReach() && !pathUsesWater;
        boolean evidenceFresh = false;
        if (movementRequired && distanceSqr <= 900.0D && !measurableProgress && !pathReachable) {
            if (++waterBarrierScanTicks >= WATER_BARRIER_SCAN_INTERVAL_TICKS) {
                waterBarrierScanTicks = 0;
                if (selfRescueLatched) {
                    Path dryPath = createDryFollowPath(friend);
                    if (dryPath != null) {
                        cachedDryFollowPath = dryPath;
                        cachedDryFollowTarget = friend.getUUID();
                        pathReachable = true;
                        cachedWaterBarrierEvidence = false;
                    }
                }
                if (!pathReachable) {
                    cachedWaterBarrierEvidence = scanWaterBarrier(path, friend);
                    evidenceFresh = true;
                }
            }
        } else {
            waterBarrierScanTicks = 0;
            cachedWaterBarrierEvidence = false;
        }

        BlockPos recoveryLanding = null;
        boolean recoveryReady = outcome == FadedMovementCoordinator.RecoveryOutcome.NONE
                && movementRequired && distanceSqr <= 900.0D && !measurableProgress && !pathReachable
                && cachedWaterBarrierEvidence
                && movementState.noProgressTicks() >= FadedMovementCoordinator.WATER_BARRIER_STUCK_TICKS - 1
                && movementState.waterEvidenceTicks() >= FadedMovementCoordinator.REQUIRED_WATER_EVIDENCE_TICKS
                && movementState.recoveryCooldownTicks() == 0;
        if (recoveryReady) {
            recoveryLanding = findVisibleFollowRecoveryLanding(friend);
            if (recoveryLanding == null) {
                outcome = FadedMovementCoordinator.RecoveryOutcome.NO_TARGET;
            } else {
                BlockPos landing = recoveryLanding;
                queueLocomotion(FadedMovementCoordinator.Locomotion.FOLLOW_WATER_BARRIER_RECOVERY,
                        landing.toShortString(), 0.0D, "follow water barrier recovery", () -> {
                            applyNavigationStop();
                            boolean success = trySafeTeleport(landing.getX() + .5D,
                                    landing.getY(), landing.getZ() + .5D);
                            pendingBarrierRecoveryOutcome = success
                                    ? FadedMovementCoordinator.RecoveryOutcome.SUCCESS
                                    : FadedMovementCoordinator.RecoveryOutcome.ROLLBACK;
                        });
            }
        }
        return new FollowMovementObservation(measurableProgress, friendMoved, targetChanged,
                pathReachable, movementRequired,
                cachedWaterBarrierEvidence, evidenceFresh, recoveryLanding, outcome);
    }

    private void resetFollowBarrierObservation() {
        lastFollowObservationPosition = null;
        lastFollowFriendPosition = null;
        lastObservedFriendId = null;
        lastFollowDistanceSqr = Double.NaN;
        clearFollowBarrierEpisodeCache();
    }

    private void clearFollowBarrierEpisodeCache() {
        waterBarrierScanTicks = 0;
        cachedWaterBarrierEvidence = false;
        cachedDryFollowPath = null;
        cachedDryFollowTarget = null;
    }

    private boolean pathUsesWater(Path path) {
        if (path == null) return false;
        for (int index = path.getNextNodeIndex(); index < path.getNodeCount(); index++) {
            var node = path.getNode(index);
            if (node.type == BlockPathTypes.WATER
                    || level().getFluidState(node.asBlockPos()).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    private Path createDryFollowPath(Player friend) {
        float previousWaterMalus = getPathfindingMalus(BlockPathTypes.WATER);
        try {
            setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
            Path dryPath = getNavigation().createPath(friend, 0);
            return dryPath != null && dryPath.canReach() && !pathUsesWater(dryPath) ? dryPath : null;
        } finally {
            setPathfindingMalus(BlockPathTypes.WATER, previousWaterMalus);
        }
    }

    private Path consumeDryFollowPath(Player friend) {
        if (cachedDryFollowPath == null || !friend.getUUID().equals(cachedDryFollowTarget)) return null;
        Path path = cachedDryFollowPath;
        cachedDryFollowPath = null;
        cachedDryFollowTarget = null;
        return path;
    }

    private boolean scanWaterBarrier(Path path, Player friend) {
        BlockPos corridorStart = blockPosition();
        if (path != null) {
            for (int index = path.getNextNodeIndex(); index < path.getNodeCount(); index++) {
                BlockPos node = path.getNodePos(index);
                if (level().getFluidState(node).is(FluidTags.WATER)) return true;
            }
        }
        if (path != null && path.getEndNode() != null) {
            corridorStart = path.getEndNode().asBlockPos();
            if (hasWaterNear(corridorStart)) return true;
        }
        Vec3 start = Vec3.atCenterOf(corridorStart);
        Vec3 end = friend.position().add(0.0D, 0.5D, 0.0D);
        int samples = Math.min(WATER_BARRIER_MAX_CORRIDOR_SAMPLES,
                Math.max(2, Mth.ceil(start.distanceTo(end) * 2.0D)));
        for (int sample = 1; sample < samples; sample++) {
            double progress = (double) sample / samples;
            BlockPos pos = BlockPos.containing(start.lerp(end, progress));
            if (isWaterColumn(pos)) return true;
        }
        return false;
    }

    private boolean hasWaterNear(BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (isWaterColumn(center.offset(dx, 0, dz))) return true;
        }
        return false;
    }

    private boolean isWaterColumn(BlockPos pos) {
        return level().getFluidState(pos).is(FluidTags.WATER)
                || level().getFluidState(pos.below()).is(FluidTags.WATER)
                || level().getFluidState(pos.above()).is(FluidTags.WATER);
    }

    private BlockPos findVisibleFollowRecoveryLanding(Player friend) {
        if (!(level() instanceof ServerLevel serverLevel) || !canFollowTeleportTo(friend)) return null;
        BlockPos origin = friend.blockPosition();
        for (int radius = 0; radius <= WATER_BARRIER_LANDING_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                for (int dy = 2; dy >= -WATER_BARRIER_LANDING_VERTICAL_RANGE; dy--) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    if (isSafeHomeLanding(serverLevel, feet) && isVisibleRecoveryLanding(feet)) {
                        return feet.immutable();
                    }
                }
            }
        }
        return null;
    }

    private boolean isVisibleRecoveryLanding(BlockPos feet) {
        Vec3 target = Vec3.atBottomCenterOf(feet).add(0.0D, getBbHeight() * 0.5D, 0.0D);
        BlockHitResult hit = level().clip(new ClipContext(getEyePosition(), target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    private BlockPos findNearestVisibleFollowSelfRescueLanding(BlockPos origin, int horizontalRadius,
                                                                int verticalRange) {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                for (int dy = 2; dy >= -verticalRange; dy--) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    if (isSafeHomeLanding(serverLevel, feet) && isVisibleRecoveryLanding(feet)) {
                        return feet.immutable();
                    }
                }
            }
        }
        return null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D).add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D).add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new EscapeFireGoal());
        goalSelector.addGoal(1, new ArbitratedMeleeAttackGoal());
        goalSelector.addGoal(2, new FollowFriendGoal());
        goalSelector.addGoal(2, new ReturnHomeDimensionGoal());
        goalSelector.addGoal(2, new ReturnHomeGoal());
        goalSelector.addGoal(5, new WanderNearAnchorGoal());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                target -> shouldProtectFriendFrom(target)));
    }

    @Override
    protected Brain.Provider<FadedEnderman> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    protected void customServerAiStep() {
        tickBrain(this);
        tickSelfDefense();
        maintainHostileReactionLook();
        if (isHealed() && !isDowned()) tickExpansionDetectors();
        super.customServerAiStep();
    }

    @Override
    public List<? extends ExtendedSensor<? extends FadedEnderman>> getSensors() {
        return List.of(
                new NearbyPlayersSensor<FadedEnderman>().setRadius(28),
                new NearbyLivingEntitySensor<FadedEnderman>().setRadius(32),
                new NearbyItemsSensor<FadedEnderman>().setRadius(12)
                        .setPredicate((item, enderman) -> item.isAlive() && item.getItem().is(ItemTags.SMALL_FLOWERS))
                        .setScanRate(enderman -> 5)
        );
    }

    @Override
    public BrainActivityGroup<? extends FadedEnderman> getCoreTasks() {
        return BrainActivityGroup.coreTasks();
    }

    @Override
    public BrainActivityGroup<? extends FadedEnderman> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<>(
                        new SetPlayerLookTarget<FadedEnderman>()
                                .lookPredicate((enderman, player) ->
                                        (!enderman.isHealed() && !player.isSpectator() && enderman.distanceToSqr(player) <= 64.0D)
                                                || (enderman.isHealed() && enderman.isFriend(player))),
                        new SetRandomLookTarget<FadedEnderman>()
                ),
                new OneRandomBehaviour<>(
                        new SetRandomWalkTarget<FadedEnderman>()
                                .setRadius(8, 4)
                                .speedModifier(.68F)
                                .avoidWaterWhen(enderman -> true)
                                .walkTargetPredicate((enderman, pos) -> enderman.canExplorePosition(pos))
                                .startCondition(FadedEnderman::canBrainExplore),
                        new Idle<FadedEnderman>().runFor(enderman -> 50 + enderman.getRandom().nextInt(91))
                )
        );
    }

    private boolean canBrainExplore() {
        Player friend = getFriendPlayer();
        return isHealed() && !isDowned() && !isVehicle() && !interactionMovementStopLatched
                && getCommand() == CompanionCommand.FOLLOW
                && friend != null && distanceToSqr(friend) <= 64.0D && socialActionTicks <= 0
                && curiosityPhase == CuriosityPhase.NONE && socialRepositionTarget == null
                && !shouldPauseFollowForWater(friend) && !level().isRainingAt(blockPosition());
    }

    private void queueBrainMemoryCandidates() {
        Optional<PositionTracker> lookTarget = getBrain().getMemory(MemoryModuleType.LOOK_TARGET);
        lookTarget.ifPresent(target -> {
            Vec3 position = target.currentPosition();
            queueLook(FadedMovementCoordinator.Look.LOOK_WALK_TARGET, position.toString(), "SBL look memory",
                    () -> applyLookAt(position));
        });

        if (!canBrainExplore()) return;
        Optional<WalkTarget> walkTarget = getBrain().getMemory(MemoryModuleType.WALK_TARGET);
        walkTarget.ifPresent(target -> {
            BlockPos destination = target.getTarget().currentBlockPosition();
            if (destination.distManhattan(blockPosition()) <= target.getCloseEnoughDist()) {
                getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                getBrain().eraseMemory(MemoryModuleType.PATH);
                return;
            }
            double speed = target.getSpeedModifier();
            queueLocomotion(FadedMovementCoordinator.Locomotion.IDLE_EXPLORE, destination.toShortString(), speed,
                    "SBL walk memory", () -> applyMoveTo(destination, 0.0D, speed));
        });
    }

    private boolean canExplorePosition(Vec3 pos) {
        if (pos == null) return false;
        Player friend = getFriendPlayer();
        return friend != null && pos.distanceToSqr(friend.position()) <= 64.0D
                && level().getFluidState(BlockPos.containing(pos)).isEmpty();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HEALED, false);
        entityData.define(HEALING_COLOR, PulsatingPearlItem.DEFAULT_COLOR);
        entityData.define(COMMAND, CompanionCommand.FOLLOW.ordinal());
        entityData.define(DOWNED, false);
        entityData.define(DOWNED_SECONDS, 0);
        entityData.define(CRYING, false);
        entityData.define(PLAYER_NEARBY, false);
        entityData.define(SEATED_YAW, 0.0F);
        entityData.define(SEATED_YAW_SET, false);
        entityData.define(SOCIAL_ACTION, SocialAction.NONE.ordinal());
        entityData.define(CARRY_ACTION, CarryAction.NONE.ordinal());
        entityData.define(CARRY_MOVING, false);
        entityData.define(HEALING_TICKS, 0);
        entityData.define(RUNNING_TO_FRIEND, false);
        entityData.define(CARRIED_BLINK_COOLDOWN, 0);
        entityData.define(CURIOSITY_DISPLAY, ItemStack.EMPTY);
        entityData.define(SYNCED_TRUST, 0);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        boolean clientSide = level().isClientSide();
        boolean serverPlayerPresent = player instanceof ServerPlayer;
        if (mainHand && player.getItemInHand(hand).is(Items.BOOK)) {
            if (clientSide) return InteractionResult.SUCCESS;
            if (serverPlayerPresent && isHealed() && !isDowned() && isFriend(player))
                return issueEndermanJournal((ServerPlayer) player, player.getItemInHand(hand));
            return InteractionResult.PASS;
        }
        ItemStack held = mainHand && !clientSide && serverPlayerPresent
                ? player.getItemInHand(hand) : ItemStack.EMPTY;
        if (mainHand && !clientSide && serverPlayerPresent && isFriend(player)
                && curiosityPhase == CuriosityPhase.POINT_HAND && curiosityTicks > 0
                && ItemCuriosityPolicy.isEligible(held)
                && curiosityExpectedItem.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getKey(held.getItem()).toString())) {
            ItemStack offered = held.copyWithCount(1);
            held.shrink(1);
            curiosityStack = offered;
            curiosityReturnPos = player.blockPosition().immutable();
            curiosityReturnDimension = player.level().dimension();
            entityData.set(CURIOSITY_DISPLAY, offered.copy());
            curiosityPhase = CuriosityPhase.INSPECT;
            curiosityTicks = ItemCuriosityPolicy.INSPECT_TICKS;
            curiosityHintPending = false;
            curiosityHintDelay = 0;
            setSocialAction(SocialAction.ITEM_INSPECT);
            return InteractionResult.CONSUME;
        }
        FadedInteractionHandler.HeldItem heldItem = classifyHeldItem(held);
        FadedInteractionHandler.InteractionIntent intent = FadedInteractionHandler.classify(
                new FadedInteractionHandler.InteractionInput(mainHand, clientSide, serverPlayerPresent, isHealed(), pearlGiven,
                        isFriend(player), isDowned(), player.isShiftKeyDown(), heldItem));
        if (intent == FadedInteractionHandler.InteractionIntent.PASS) return InteractionResult.PASS;
        if (intent == FadedInteractionHandler.InteractionIntent.CLIENT_SUCCESS) return InteractionResult.SUCCESS;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (isFriend(player)) clearStrangerReaction();
        switch (intent) {
            case HEAL_WITH_PULSATING_PEARL -> {
                FadedPearlSavedData data = FadedPearlSavedData.get((ServerLevel) level());
                if (!data.claimForHealing(serverPlayer.getUUID(), getUUID())) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.faded_pearl.companion.already_registered"));
                    return InteractionResult.CONSUME;
                }
                setHealed(true);
                entityData.set(HEALING_TICKS, 60);
                setHealingColor(PulsatingPearlItem.getColor(held));
                friendId = player.getUUID();
                setTrustValue(0);
                setHealth(getMaxHealth());
                if (!player.getAbilities().instabuild) held.shrink(1);
                data.markHealed(getUUID());
                ((ServerLevel) level()).sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.5D, getZ(), 70,
                        .65D, 1.15D, .65D, .08D);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 2.1D, getZ(), 8,
                        .4D, .45D, .4D, .03D);
                playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0F, 0.72F);
                sayRandom(serverPlayer, FadedDialogue.Category.HEALED);
                return InteractionResult.CONSUME;
            }
            case EMPTY_PEARL_SILENCE -> {
                sayTo(serverPlayer, "dialogue.faded_pearl.silence");
                return InteractionResult.CONSUME;
            }
            case FIRST_MEETING_PEARL -> {
                pearlGiven = true;
                ItemStack pearl = new ItemStack(ModItems.EMPTY_PEARL.get());
                if (!player.addItem(pearl)) spawnAtLocation(pearl);
                sayTo(serverPlayer, "dialogue.faded_pearl.first_meeting");
                return InteractionResult.CONSUME;
            }
            case RECOVER_DOWNED -> {
                    setDowned(false);
                    downedTicks = 0;
                    setHealth(Math.max(8.0F, getMaxHealth() / 2.0F));
                    setSocialAction(SocialAction.DOWNED_RECOVER);
                    socialActionTicks = 50;
                    sayRandom(serverPlayer, FadedDialogue.Category.DOWNED_RECOVERED);
                    return InteractionResult.CONSUME;
            }
            case FLOWER_INTERACTION -> {
                    setSocialAction(SocialAction.FLOWER_INTEREST);
                    socialActionTicks = 100;
                    if (flowerCooldown <= 0) {
                        if (!player.getAbilities().instabuild) held.shrink(1);
                        modifyTrust(FadedTrustManager.FLOWER_ACCEPTED);
                        flowerCooldown = 6000;
                    }
                    if (level() instanceof ServerLevel serverLevel && flowerCooldown == 6000) {
                        serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.4D, getZ(), 5,
                                .25D, .2D, .25D, .02D);
                    }
                    sayRandom(serverPlayer, FadedDialogue.Category.FLOWER);
                    return InteractionResult.CONSUME;
            }
            case EQUIP_ESCAPE_PEARL -> {
                    if (escapePearlArmed) {
                        serverPlayer.displayClientMessage(
                                Component.translatable("message.faded_pearl.escape_pearl.already_armed"), true);
                        return InteractionResult.CONSUME;
                    }
                    escapePearlArmed = true;
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.faded_pearl.escape_pearl.armed"), true);
                    if (level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.7D, getZ(),
                                18, .3D, .6D, .3D, .04D);
                    }
                    return InteractionResult.CONSUME;
            }
            case OPEN_COMMAND_MENU -> {
                    requestInteractionMovementStop("open command menu");
                    ModNetwork.openCommandMenu(serverPlayer, getId(), getCommand().ordinal());
                    return InteractionResult.CONSUME;
            }
            case EMPTY_HAND_INTERACTION -> {
                    long now = level().getGameTime();
                    if (FadedTrustManager.canCarry(trust) && now - lastAffectionClick <= 60 && !isVehicle()) {
                        requestInteractionMovementStop("start carrying interaction");
                        if (player.startRiding(this, true)) {
                            journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_CARRY);
                            setCarryAction(CarryAction.PICKING_UP);
                            carryActionTicks = 16;
                            hadCarriedPassenger = true;
                            modifyTrust(FadedTrustManager.CARRY_STARTED);
                            sayRandom(serverPlayer, FadedDialogue.Category.CARRY);
                            return InteractionResult.CONSUME;
                        }
                    }
                    lastAffectionClick = now;
                    performAffection(serverPlayer);
                    return InteractionResult.CONSUME;
            }
            case STRANGER_NEUTRAL_REACTION -> {
                    if (strangerReactionCooldown <= 0) {
                        clearFadeMeeting();
                        requestInteractionMovementStop("neutral stranger interaction");
                        setSocialAction(SocialAction.CURIOUS);
                        socialActionTicks = 45;
                        strangerReactionCooldown = 100;
                        strangerReactionTarget = serverPlayer.getUUID();
                        queueLookAt(serverPlayer, 25.0F, 25.0F, "neutral stranger interaction");
                        sayRandom(serverPlayer, FadedDialogue.Category.STRANGER_NEUTRAL);
                    }
                    return InteractionResult.CONSUME;
            }
            default -> throw new IllegalStateException("Unhandled interaction intent: " + intent);
        }
    }

    private InteractionResult issueEndermanJournal(ServerPlayer player, ItemStack book) {
        ItemStack journal = EndermanJournalItem.create(ModItems.ENDERMAN_JOURNAL.get(), getUUID());
        boolean delivered = player.getInventory().add(journal);
        if (!delivered) {
            Vec3 drop = player.position();
            ItemEntity droppedJournal = new ItemEntity(player.serverLevel(), drop.x, drop.y + 0.5D, drop.z, journal);
            delivered = player.serverLevel().addFreshEntity(droppedJournal);
        }
        if (!delivered) return InteractionResult.PASS;
        if (journalNotificationBaseline == null) journalNotificationBaseline = journalSnapshot();
        journalMemory.discover(JournalMemory.Discovery.DAY_MEETING);
        journalMemory.discover(JournalMemory.Discovery.DAY_HEALING);
        if (!player.getAbilities().instabuild) book.shrink(1);
        return InteractionResult.CONSUME;
    }

    private FadedInteractionHandler.HeldItem classifyHeldItem(ItemStack held) {
        if (held.is(ModItems.PULSATING_PEARL.get())) return FadedInteractionHandler.HeldItem.PULSATING_PEARL;
        if (held.is(ModItems.EMPTY_PEARL.get())) return FadedInteractionHandler.HeldItem.EMPTY_PEARL;
        if (held.is(ModItems.ESCAPE_PEARL.get())) return FadedInteractionHandler.HeldItem.ESCAPE_PEARL;
        if (held.is(ItemTags.SMALL_FLOWERS)) return FadedInteractionHandler.HeldItem.SMALL_FLOWER;
        return held.isEmpty() ? FadedInteractionHandler.HeldItem.EMPTY_HAND
                : FadedInteractionHandler.HeldItem.OTHER;
    }

    private void sayTo(ServerPlayer player, String key) {
        sendDialogue(player, FadedDialogue.normal(key));
    }

    private void sayRandom(ServerPlayer player, FadedDialogue.Category category) {
        sendDialogue(player, dialogueMemory.next(random, category));
    }

    private void sendDialogue(Player player, FadedDialogue.Descriptor dialogue) {
        sendDialogue(player, dialogue, null);
    }

    private void sendDialogue(Player player, FadedDialogue.Descriptor dialogue, String visibleName) {
        if (player instanceof ServerPlayer && lastThoughtTick == level().getGameTime()) {
            deferredDialogues.add(new PendingDialogue(player.getUUID(), dialogue, visibleName));
            return;
        }
        deliverDialogue(player, dialogue, visibleName);
    }

    private void deliverDialogue(Player player, FadedDialogue.Descriptor dialogue, String visibleName) {
        lastDialogueTick = level().getGameTime();
        if (visibleName == null) FadedDialogue.send(player, dialogue);
        else FadedDialogue.send(player, dialogue, visibleName);
    }

    private void flushDeferredDialogues() {
        if (deferredDialogues.isEmpty() || lastThoughtTick == level().getGameTime()
                || !(level() instanceof ServerLevel serverLevel)) return;
        List<PendingDialogue> pending = List.copyOf(deferredDialogues);
        deferredDialogues.clear();
        for (PendingDialogue deferred : pending) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(deferred.playerId());
            if (player != null) deliverDialogue(player, deferred.dialogue(), deferred.visibleName());
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            if (!isHealed()) spawnAtLocation(ModItems.ENDERMAN_TEAR.get());
            FadedPearlSavedData data = FadedPearlSavedData.get(serverLevel);
            boolean isCanonical = data.ownerOf(getUUID()).isPresent()
                    || data.isUnclaimedCompanion(getUUID())
                    || data.endermanId().filter(getUUID()::equals).isPresent();
            if (isCanonical) {
                data.markDead(getUUID());
                if (data.homeDimension(getUUID()).filter(serverLevel.dimension()::equals).isPresent()
                        && data.homePos(getUUID()).isPresent()) {
                    BlockPos anchor = data.homePos(getUUID()).get();
                    if (serverLevel.getBlockState(anchor).is(ModBlocks.RESONATING_ANCHOR.get()))
                        serverLevel.setBlock(anchor, serverLevel.getBlockState(anchor).setValue(ResonatingAnchorBlock.ACTIVE, false), 3);
                }
            }
        }
        super.die(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof Player player) {
            boolean friendAttack = isHealed() && isFriend(player);
            if (friendAttack)
                journalMemory.discover(JournalMemory.Discovery.ESCAPE_PEARL_PLAYER_HIT);
            if (EscapePearlPolicy.decide(escapePearlArmed, isHealed(), friendAttack,
                    source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))
                    == EscapePearlPolicy.Decision.PROTECT_AND_EVADE) {
                tryEscapePearlTeleport(player);
                Player friend = getFriendPlayer();
                if (friend != null) friend.displayClientMessage(
                        Component.translatable("message.faded_pearl.escape_pearl.triggered"), true);
                return false;
            }
            if (friendAttack)
                modifyTrust(FadedTrustManager.PLAYER_ATTACK);
        }
        if (!level().isClientSide && isHealed() && !isDowned()
                && source.getEntity() instanceof Monster monster && monster.isAlive()) {
            selfDefenseTarget = monster;
            selfDefenseTicks = 200;
            hostileReactionTarget = monster;
            setSocialAction(SocialAction.SOUND_ALERT);
            socialActionTicks = Math.max(socialActionTicks, 50);
            queueLookAt(monster, 35.0F, 30.0F, "direct attacker reaction");
            if (getCommand() != CompanionCommand.REST && !isVehicle())
                queueTarget(monster, "direct attacker self defense");
        }
        if (isHealed() && amount >= getHealth() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            setHealth(1.0F);
            setDowned(true);
            setCommand(CompanionCommand.REST);
            queueStop(FadedMovementCoordinator.Locomotion.IMMOBILE_DOWNED, "lethal damage downed");
            return true;
        }
        return super.hurt(source, amount);
    }

    private void tickExpansionDetectors() {
        Player friend = getFriendPlayer();
        if (friend == null) {
            applyGestureDetection(FadedGestureDetector.tick(
                    new FadedGestureDetector.Input(false, 0.0D, false, false, false, 0),
                    gestureState()), null);
            return;
        }
        double distanceSq = distanceToSqr(friend);
        boolean airborne = !friend.onGround() && friend.getDeltaMovement().y > 0.08D;
        Vec3 eyePosition = friend.getEyePosition();
        Vec3 lookEnd = eyePosition.add(friend.getLookAngle().scale(5.0D));
        boolean lookHitsTarget = getBoundingBox().inflate(0.1D).clip(eyePosition, lookEnd).isPresent();
        applyGestureDetection(FadedGestureDetector.tick(
                new FadedGestureDetector.Input(true, distanceSq, airborne, friend.isCrouching(),
                        lookHitsTarget, trust), gestureState()), friend);
        net.minecraft.world.level.biome.Biome.Precipitation precipitation = level().getBiome(blockPosition())
                .value().getPrecipitationAt(blockPosition());
        BlockPos weatherPos = blockPosition();
        boolean snowingAtSelf = level().isRaining()
                && precipitation == net.minecraft.world.level.biome.Biome.Precipitation.SNOW
                && level().canSeeSky(weatherPos)
                && FadedWeatherDetector.isAtOrAboveMotionBlockingHeight(
                        level().getHeight(Heightmap.Types.MOTION_BLOCKING, weatherPos.getX(), weatherPos.getZ()),
                        weatherPos.getY());
        FadedWeatherDetector.Result weather = FadedWeatherDetector.tick(
                new FadedWeatherDetector.Input(level().isRaining(),
                        precipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN,
                        precipitation == net.minecraft.world.level.biome.Biome.Precipitation.SNOW,
                        level().isRainingAt(weatherPos), snowingAtSelf, level().canSeeSky(weatherPos),
                        FadedWeatherDetector.isNearSurfaceShelter(
                                level().getHeight(Heightmap.Types.MOTION_BLOCKING, weatherPos.getX(), weatherPos.getZ()),
                                weatherPos.getY()), distanceSq,
                        level().isRainingAt(friend.blockPosition()), tickCount % 200 == 0),
                new FadedWeatherDetector.State(rainSheltered, rainExposureTicks, shelteredTogetherTicks));
        applyWeatherState(weather.state());
        applyWeatherIntents(weather.beforeSleepAndNight());
        if (isInWaterOrBubble() && !wasInWater) modifyTrust(FadedTrustManager.ENTERED_WATER);
        wasInWater = isInWaterOrBubble();
        if (friend.isSleeping() && distanceSq <= 64.0D) {
            setSocialAction(SocialAction.WATCH_SLEEPING); socialActionTicks = 30;
        } else if (tickCount % 600 == 0 && !level().isRaining() && level().isNight() && level().canSeeSky(blockPosition())) {
            setSocialAction(SocialAction.NIGHT_GAZE); socialActionTicks = 100;
        }
        applyWeatherIntents(weather.afterSleepAndNight());
        if (tickCount % 20 == 0) tickWorldAwareness(friend);
        if (CompanionPerformancePolicy.isCadenceTick(tickCount, getId(),
                CompanionPerformancePolicy.LIGHT_SCAN_TICKS)
                && level().isNight() && distanceSq <= 100.0D) {
            int lights = countNearbyPlacedLights();
            if (nearbyLightBaselineInitialized && lights > nearbyLightCount) modifyTrust(FadedTrustManager.NEW_LIGHT);
            nearbyLightCount = lights;
            nearbyLightBaselineInitialized = true;
        }
    }

    private void tickWorldAwareness(Player friend) {
        boolean hasDiamond = inventoryContains(friend, Items.DIAMOND);
        boolean hasEnderPearl = inventoryContains(friend, Items.ENDER_PEARL);
        if (hasEnderPearl)
            journalMemory.discover(JournalMemory.Discovery.ESCAPE_PEARL_ENDER_PEARL_HELD);
        int armor = friend.getArmorValue();
        if (!awarenessBaselineInitialized) {
            friendHadDiamond = hasDiamond;
            friendHadEnderPearl = hasEnderPearl;
            friendArmorValue = armor;
            awarenessBaselineInitialized = true;
        }
        else if (distanceToSqr(friend) <= 400.0D && hasLineOfSight(friend)) {
            boolean reacted = false;
            if (!friendHadDiamond && hasDiamond)
                reacted = observeAwareness(WorldAwarenessMemory.Milestone.PLAYER_DIAMOND,
                        FadedDialogue.Category.AWARE_PLAYER_DIAMOND, SocialAction.CURIOUS, 0);
            if (!reacted && !friendHadEnderPearl && hasEnderPearl)
                reacted = observeAwareness(WorldAwarenessMemory.Milestone.ENDER_PEARL,
                        FadedDialogue.Category.AWARE_ENDER_PEARL, SocialAction.WORRIED, 0);
            if (!reacted && friendArmorValue > 0 && armor > friendArmorValue)
                reacted = observeAwareness(WorldAwarenessMemory.Milestone.ARMOR_UPGRADE,
                        FadedDialogue.Category.AWARE_ARMOR_UPGRADE, SocialAction.CURIOUS, 0);
            if (reacted) {
                friendHadDiamond = hasDiamond;
                friendHadEnderPearl = hasEnderPearl;
                friendArmorValue = armor;
                return;
            }
        }
        friendHadDiamond = hasDiamond;
        friendHadEnderPearl = hasEnderPearl;
        friendArmorValue = armor;

        if (distanceToSqr(friend) > 400.0D || !hasLineOfSight(friend)) return;
        if (level() instanceof ServerLevel serverLevel && serverLevel.isVillage(friend.blockPosition())
                && observeAwareness(WorldAwarenessMemory.Milestone.VILLAGE,
                FadedDialogue.Category.AWARE_VILLAGE, SocialAction.LOOK_AROUND, 0)) return;
        if (tickCount % 100 == 0
                && !worldAwarenessMemory.hasSeen(WorldAwarenessMemory.Milestone.ENDERMAN_DIAMOND)
                && findVisibleDiamondOre() != null
                && observeAwareness(WorldAwarenessMemory.Milestone.ENDERMAN_DIAMOND,
                FadedDialogue.Category.AWARE_ENDERMAN_DIAMOND, SocialAction.ITEM_POINT, 0)) return;
        if (tickCount % 100 == 0 && !worldAwarenessMemory.hasSeen(WorldAwarenessMemory.Milestone.PET_SMALL_ANIMAL)) {
            Animal small = level().getEntitiesOfClass(Animal.class, getBoundingBox().inflate(8.0D),
                    animal -> animal.isAlive() && (animal.isBaby()
                            || (animal.getBbWidth() <= 0.9F && animal.getBbHeight() <= 1.2F)))
                    .stream().filter(this::hasLineOfSight).findFirst().orElse(null);
            if (small != null) {
                queueLookAt(small, 25.0F, 25.0F, "world awareness small animal");
                observeAwareness(WorldAwarenessMemory.Milestone.PET_SMALL_ANIMAL,
                        FadedDialogue.Category.AWARE_PET_ANIMAL, SocialAction.AFFECTION, 0);
            }
        }
    }

    private boolean inventoryContains(Player player, net.minecraft.world.item.Item item) {
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(item)) return true;
        for (ItemStack stack : player.getInventory().offhand)
            if (stack.is(item)) return true;
        return false;
    }

    private BlockPos findVisibleDiamondOre() {
        BlockPos origin = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -6, -8), origin.offset(8, 6, 8))) {
            if (!level().getBlockState(pos).is(Blocks.DIAMOND_ORE)
                    && !level().getBlockState(pos).is(Blocks.DEEPSLATE_DIAMOND_ORE)) continue;
            BlockHitResult hit = level().clip(new ClipContext(getEyePosition(), Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                queueLookAt(Vec3.atCenterOf(pos), 30.0F, 30.0F, "world awareness diamond ore");
                return pos.immutable();
            }
        }
        return null;
    }

    public void observeTamedAnimal(Animal animal, Player owner) {
        if (level().isClientSide || !isHealed() || !isFriend(owner) || animal.level() != level()
                || distanceToSqr(animal) > 400.0D || !hasLineOfSight(animal)) return;
        WorldAwarenessMemory.Milestone milestone;
        FadedDialogue.Category category;
        if (animal instanceof Wolf) {
            milestone = WorldAwarenessMemory.Milestone.TAMED_WOLF;
            category = FadedDialogue.Category.AWARE_TAMED_WOLF;
        }
        else if (animal instanceof Cat) {
            milestone = WorldAwarenessMemory.Milestone.TAMED_CAT;
            category = FadedDialogue.Category.AWARE_TAMED_CAT;
        }
        else if (animal instanceof Parrot) {
            milestone = WorldAwarenessMemory.Milestone.TAMED_PARROT;
            category = FadedDialogue.Category.AWARE_TAMED_PARROT;
        }
        else {
            milestone = WorldAwarenessMemory.Milestone.TAMED_OTHER;
            category = FadedDialogue.Category.AWARE_TAMED_OTHER;
        }
        queueLookAt(animal, 25.0F, 25.0F, "world awareness tamed animal");
        int reward = worldAwarenessMemory.markFirst(WorldAwarenessMemory.Milestone.TAMED_ANY) ? 1 : 0;
        observeAwareness(milestone, category, SocialAction.AFFECTION, reward);
    }

    public void observeCompletedBuild(Player builder) {
        if (level().isClientSide || !isHealed() || !isFriend(builder) || builder.level() != level()
                || distanceToSqr(builder) > 576.0D || !hasLineOfSight(builder)) return;
        observeAwareness(WorldAwarenessMemory.Milestone.BUILD_COMPLETED,
                FadedDialogue.Category.AWARE_BUILD, SocialAction.LOOK_AROUND, 1);
    }

    private boolean observeAwareness(WorldAwarenessMemory.Milestone milestone, FadedDialogue.Category category,
                                     SocialAction action, int trustReward) {
        if (!worldAwarenessMemory.markFirst(milestone)) return false;
        Player friend = getFriendPlayer();
        if (!(friend instanceof ServerPlayer serverPlayer)) return false;
        setSocialAction(action);
        socialActionTicks = 80;
        socialActionCooldown = Math.max(socialActionCooldown, 200);
        sayRandom(serverPlayer, category);
        if (trustReward > 0) addTrust(trustReward);
        return true;
    }

    private void applyWeatherState(FadedWeatherDetector.State state) {
        rainSheltered = state.rainSheltered();
        rainExposureTicks = state.rainExposureTicks();
        shelteredTogetherTicks = state.shelteredTogetherTicks();
    }

    private void applyWeatherIntents(List<FadedWeatherDetector.Intent> intents) {
        for (FadedWeatherDetector.Intent intent : intents) {
            switch (intent) {
                case RAIN_SHIVER -> {
                    journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_WEATHER_REACTION);
                    setSocialAction(SocialAction.RAIN_SHIVER); socialActionTicks = 30;
                }
                case RAIN_PENALTY -> modifyTrust(FadedTrustManager.RAIN_EXPOSURE);
                case RAIN_SHELTER -> {
                    journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_SHARED_SHELTER);
                    setSocialAction(SocialAction.RAIN_SHELTER); socialActionTicks = 30;
                }
                case SHELTER_REWARD -> {
                    journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_SHARED_SHELTER);
                    modifyTrust(FadedTrustManager.SHARED_SHELTER);
                    triggerDialogue(dialogueMemory.next(random, FadedDialogue.Category.WEATHER_RAIN_ROOF));
                }
                case SNOW_CATCH -> {
                    journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_WEATHER_REACTION);
                    triggerExpansionAction(SocialAction.SNOW_CATCH, 70,
                            dialogueMemory.next(random, FadedDialogue.Category.WEATHER_SNOW));
                }
            }
        }
    }

    private FadedGestureDetector.State gestureState() {
        return new FadedGestureDetector.State(crouchTimer, stareTimer, jumpCount,
                jumpWindowTimer, friendWasAirborne);
    }

    private void applyGestureDetection(FadedGestureDetector.Result result, Player friend) {
        FadedGestureDetector.State state = result.state();
        crouchTimer = state.crouchTimer();
        stareTimer = state.stareTimer();
        jumpCount = state.jumpCount();
        jumpWindowTimer = state.jumpWindowTimer();
        friendWasAirborne = state.friendWasAirborne();
        for (FadedGestureDetector.Intent intent : result.intents()) {
            switch (intent) {
                case JUMP -> triggerExpansionAction(SocialAction.JUMP_REACT, 45,
                        dialogueMemory.next(random, FadedTrustManager.usesLowGestureResponse(trust)
                                ? FadedDialogue.Category.GESTURE_JUMP_LOW
                                : FadedDialogue.Category.GESTURE_JUMP_HIGH));
                case CROUCH -> triggerExpansionAction(SocialAction.CROUCH, 140,
                        dialogueMemory.next(random, FadedTrustManager.usesLowGestureResponse(trust)
                                ? FadedDialogue.Category.GESTURE_CROUCH_LOW
                                : FadedDialogue.Category.GESTURE_CROUCH_HIGH));
                case STARE_LOW -> {
                    modifyTrust(FadedTrustManager.PERSISTENT_STARE);
                    Vec3 away = position().subtract(friend.position()).normalize().scale(.8D);
                    queueMoveTo(new Vec3(getX() + away.x, getY(), getZ() + away.z),
                            1.0D, "persistent stare recoil");
                    triggerExpansionAction(SocialAction.STARE_FREEZE, 45,
                            dialogueMemory.next(random, FadedDialogue.Category.GESTURE_STARE_LOW));
                }
                case STARE_HIGH -> triggerExpansionAction(SocialAction.STARE_TILT, 50,
                        dialogueMemory.next(random, FadedDialogue.Category.GESTURE_STARE_HIGH));
            }
        }
    }

    private int countNearbyPlacedLights() {
        int count = 0; BlockPos center = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            var block = level().getBlockState(pos).getBlock();
            if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE) count++;
        }
        return count;
    }

    private void triggerExpansionAction(SocialAction action, int ticks, FadedDialogue.Descriptor dialogue) {
        setSocialAction(action); socialActionTicks = ticks; triggerDialogue(dialogue);
    }

    private void triggerDialogue(FadedDialogue.Descriptor dialogue) {
        Player friend = getFriendPlayer();
        if (friend instanceof ServerPlayer serverPlayer) sendDialogue(serverPlayer, dialogue);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            spawnEmotionalParticles();
        }
        if (!level().isClientSide && !isHealed()) {
            if (!hasSeatedYaw()) {
                setSeatedYaw(getYRot());
            }
            queueStop(FadedMovementCoordinator.Locomotion.IMMOBILE_WOUNDED, "wounded immobilization");
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (cryingTicks > 0 && --cryingTicks == 0) setCrying(false);
            Player nearby = level().getNearestPlayer(this, 8.0D);
            boolean wasPlayerNearby = isPlayerNearby();
            boolean playerNearby = nearby != null;
            entityData.set(PLAYER_NEARBY, playerNearby);
            if (FadedVoiceController.shouldPlayWoundedNotice(true, wasPlayerNearby, playerNearby)) {
                playWoundedNotice();
            }
            if (nearby != null) {
                queueLookAt(nearby, 20.0F, getMaxHeadXRot(), "wounded nearby player");
            }
            if (cryCooldown-- <= 0) {
                playDistortedCry(nearby != null);
                cryCooldown = nearby != null ? 150 + random.nextInt(111) : 100 + random.nextInt(141);
            }
        }
        if (isDowned()) {
            queueStop(FadedMovementCoordinator.Locomotion.IMMOBILE_DOWNED, "downed immobilization");
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (!level().isClientSide && downedTicks % 20 == 0)
                entityData.set(DOWNED_SECONDS, CompanionRecoveryStatus.secondsUntilStand(downedTicks));
            if (!level().isClientSide && ++downedTicks >= CompanionRecoveryStatus.DOWNED_TICKS) {
                setDowned(false);
                downedTicks = 0;
                setHealth(Math.max(8.0F, getMaxHealth() / 2.0F));
                setCommand(CompanionCommand.STAY);
            }
        }
        if (!level().isClientSide && getCommand() == CompanionCommand.REST) {
            queueStop(FadedMovementCoordinator.Locomotion.IMMOBILE_REST, "rest immobilization");
            queueClearTarget("rest clears protection target");
        }
        if (!level().isClientSide) tickSmallAnimalCarry();
        if (!level().isClientSide && isHealed() && isInWaterOrBubble()) escapeWaterToDryLand();
        if (!level().isClientSide && isHealed()) tickItemCuriosity();
        if (!level().isClientSide && isHealed()) {
            nameLearningMemory.tickRepeatCooldown();
            tickNameLearning();
        }
        if (!level().isClientSide && isHealed() && curiosityPhase == CuriosityPhase.NONE) tickSocialBehavior();
        if (!level().isClientSide && isHealed()) tickCarryingState();
        if (!level().isClientSide) syncCarryAnimationMovement();
        if (!level().isClientSide && isHealed()) {
            if (tickCount % 100 == 0 && level() instanceof ServerLevel serverLevel) {
                FadedPearlSavedData data = FadedPearlSavedData.get(serverLevel);
                if (data.endermanId().isEmpty()) data.claimEnderman(getUUID());
                data.updateLastKnown(getUUID(), serverLevel.dimension(), blockPosition());
                if (homePos == null || homeDimension == null) {
                    homePos = data.homePos(getUUID()).orElse(null);
                    homeDimension = data.homeDimension(getUUID()).orElse(null);
                }
            }
            if (protectorCooldown > 0) protectorCooldown--;
            if (combatBlinkCooldown > 0) combatBlinkCooldown--;
            if (trustInteractionCooldown > 0) trustInteractionCooldown--;
            if (strangerReactionCooldown > 0) strangerReactionCooldown--;
            if (fadeMeetingCooldown > 0) fadeMeetingCooldown--;
            if (flowerCooldown > 0) flowerCooldown--;
            if (mobReactionCooldown > 0) mobReactionCooldown--;
            if (warningCooldown > 0) warningCooldown--;
            if (growlCooldown > 0) growlCooldown--;
            if (worldReactionCooldown > 0) worldReactionCooldown--;
            curiosityCooldown = ItemCuriosityStateMachine.advanceCooldown(
                    curiosityCooldown, curiosityCooldownStartedThisTick);
            curiosityCooldownStartedThisTick = false;
            tickProtector();
            tickNearbyMobReactions();
            tickAmbientVoice();
            tickSocialReposition();
            flushDeferredDialogues();
            tickCuriosityHint();
            if (entityData.get(HEALING_TICKS) > 0) {
                entityData.set(HEALING_TICKS, entityData.get(HEALING_TICKS) - 1);
                queueStop(FadedMovementCoordinator.Locomotion.IMMOBILE_HEALING, "healing immobilization");
            }
            if (tickCount % CompanionRecoveryStatus.HEAL_INTERVAL_TICKS == 0
                    && getHealth() < getMaxHealth() && !isDowned()) heal(1.0F);
        }
        if (!level().isClientSide) applyMovementDecision();
        if (!level().isClientSide && isHealed()) tickJournalNotification();
        if (shouldLockSeatedBodyRotation()) {
            lockSeatedBodyRotation();
        }
    }

    private void tickCarryingState() {
        boolean carryingFriend = getPassengers().stream().anyMatch(passenger -> passenger instanceof Player player && isFriend(player));
        boolean carryingAnimal = getFirstPassenger() instanceof Animal;
        if (carryingFriend || carryingAnimal) {
            if (carryingFriend) queueStop(FadedMovementCoordinator.Locomotion.PASSENGER_CONTROLLED, "controlling passenger");
            if (getCarriedBlinkCooldown() > 0) setCarriedBlinkCooldown(getCarriedBlinkCooldown() - 1);
            hadCarriedPassenger = true;
            if (getCarryAction() == CarryAction.PICKING_UP && carryActionTicks > 0) {
                carryActionTicks--;
                if (carryActionTicks == 0) setCarryAction(CarryAction.CARRYING);
            } else if (getCarryAction() != CarryAction.PICKING_UP) {
                setCarryAction(CarryAction.CARRYING);
            }
            setSocialAction(SocialAction.NONE);
            socialActionTicks = 0;
        } else if (hadCarriedPassenger) {
            hadCarriedPassenger = false;
            setCarryAction(CarryAction.PUTTING_DOWN);
            carryActionTicks = 14;
        } else if (getCarryAction() == CarryAction.PICKING_UP || getCarryAction() == CarryAction.CARRYING) {
            setCarryAction(CarryAction.PUTTING_DOWN);
            carryActionTicks = 14;
        } else if (getCarryAction() == CarryAction.PUTTING_DOWN && carryActionTicks > 0) {
            if (--carryActionTicks == 0) setCarryAction(CarryAction.NONE);
        }
    }

    private void syncCarryAnimationMovement() {
        boolean carrying = isVehicle() || getCarryAction() == CarryAction.CARRYING;
        double movedX = getX() - xo;
        double movedZ = getZ() - zo;
        boolean moving = carrying && (movedX * movedX + movedZ * movedZ > 0.0004D
                || getDeltaMovement().horizontalDistanceSqr() > 0.0004D);
        if (entityData.get(CARRY_MOVING) != moving) entityData.set(CARRY_MOVING, moving);
    }

    private void tickSmallAnimalCarry() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (animalCarryCooldown > 0) animalCarryCooldown--;
        if (sameAnimalCooldown > 0 && --sameAnimalCooldown == 0) lastCarriedAnimalId = null;
        Animal passenger = getFirstPassenger() instanceof Animal animal ? animal : null;
        Player friend = getFriendPlayer();
        boolean friendNearby = friend != null && friend.isAlive() && !friend.isSpectator() && friend.level() == level()
                && distanceToSqr(friend) <= 144.0D;
        boolean threat = getTarget() != null || selfDefenseTarget != null
                || hostileReactionTarget != null || hasCuriosityThreat(friend);
        boolean unsafe = isInWaterOrBubble() || isOnFire() || level().isRainingAt(blockPosition());
        boolean rescue = isFriendRescueActive() || friend != null
                && (friend.isInLava() || friend.getRemainingFireTicks() > 0 || friend.fallDistance > 5.0F);

        if (passenger != null) {
            if (!isAnimalCarryPlacementClear(passenger)) {
                queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP,
                        "animal carry blocked by wall");
                releaseCarriedAnimal(passenger);
                return;
            }
            boolean sessionLost = animalCarryTarget == null || !animalCarryTarget.equals(passenger.getUUID());
            if (SmallAnimalCarryPolicy.shouldRelease(new SmallAnimalCarryPolicy.Interruption(
                    isDowned() || !isHealed(), getCommand() != CompanionCommand.FOLLOW,
                    threat, rescue, unsafe, passenger.getHealth() < animalCarryInitialHealth,
                    sessionLost || !passenger.isAlive() || passenger.isLeashed()
                            || passenger instanceof TamableAnimal pet && pet.isInSittingPose(), !friendNearby))
                    || animalCarryTicks <= 0
                    || SmallAnimalCarryPolicy.canTryEscape(animalCarryElapsedTicks)
                    && random.nextInt(8) == 0) {
                releaseCarriedAnimal(passenger);
                return;
            }
            animalCarryTicks--;
            animalCarryElapsedTicks++;
            if (animalCarryTicks % 50 == 0 && friend != null && !getNavigation().isInProgress()) {
                DryRoute route = findSocialRepositionRoute(friend);
                if (route != null) queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE,
                        route.destination().toShortString(), .68D, "stroll with animal",
                        () -> applyMoveTo(route.path(), .68D));
            }
            return;
        }

        if (animalCarryTicks > 0) {
            finishAnimalCarrySession(animalCarryTarget);
            return;
        }

        if (animalCarryTarget != null) {
            Entity candidate = serverLevel.getEntity(animalCarryTarget);
            Animal target = candidate instanceof Animal animal ? animal : null;
            if (target == null || !isEligibleSmallAnimal(target) || !friendNearby || threat || rescue || unsafe
                    || getCommand() != CompanionCommand.FOLLOW || isVehicle()
                    || ++animalCarryApproachTicks > SmallAnimalCarryPolicy.MAX_APPROACH_TICKS) {
                cancelAnimalCarryAttempt();
                return;
            }
            if (distanceToSqr(target) <= 5.0D) {
                animalCarryBoarding = true;
                boolean mounted;
                try {
                    mounted = target.startRiding(this);
                } finally {
                    animalCarryBoarding = false;
                }
                if (mounted) {
                    journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_ANIMAL_CARRY);
                    animalCarryInitialHealth = target.getHealth();
                    animalCarryTicks = SmallAnimalCarryPolicy.CARRY_MIN_TICKS
                            + random.nextInt(SmallAnimalCarryPolicy.CARRY_RANDOM_TICKS);
                    animalCarryElapsedTicks = 0;
                    setCarryAction(CarryAction.PICKING_UP);
                    carryActionTicks = 16;
                    hadCarriedPassenger = true;
                    queueLookAt(target, 25.0F, 25.0F, "lift small animal");
                } else cancelAnimalCarryAttempt();
                return;
            }
            if (animalCarryApproachTicks % 15 == 1) {
                Path route = createDryPathTo(target.blockPosition());
                if (route == null) {
                    cancelAnimalCarryAttempt();
                    return;
                }
                queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE,
                        target.getStringUUID(), .78D, "approach small animal",
                        () -> applyMoveTo(route, .78D));
            }
            queueLookAt(target, 25.0F, 25.0F, "watch small animal");
            return;
        }

        if (tickCount % SmallAnimalCarryPolicy.SEARCH_INTERVAL_TICKS != 0) return;
        boolean busy = isVehicle() || getCarryAction() != CarryAction.NONE || socialRepositionTarget != null
                || curiosityPhase != CuriosityPhase.NONE
                || !curiosityStack.isEmpty() || getSocialAction() != SocialAction.NONE
                || entityData.get(HEALING_TICKS) > 0 || interactionMovementStopLatched;
        if (!SmallAnimalCarryPolicy.canStart(new SmallAnimalCarryPolicy.Context(
                isHealed(), isDowned(), trust, getCommand() == CompanionCommand.FOLLOW,
                friendNearby, threat || rescue, busy, !unsafe, onGround(), animalCarryCooldown))) return;
        Animal target = level().getEntitiesOfClass(Animal.class,
                        getBoundingBox().inflate(SmallAnimalCarryPolicy.SEARCH_RADIUS),
                        animal -> isEligibleSmallAnimal(animal) && hasLineOfSight(animal)
                                && SmallAnimalCarryPolicy.canSelectAnimal(animal.getUUID(),
                                lastCarriedAnimalId, sameAnimalCooldown))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null || createDryPathTo(target.blockPosition()) == null) return;
        animalCarryTarget = target.getUUID();
        animalCarryApproachTicks = 0;
    }

    private boolean isEligibleSmallAnimal(Animal animal) {
        boolean landAnimal = animal.getType() != EntityType.BEE && animal.getType() != EntityType.PARROT
                && animal.getType() != EntityType.TURTLE && animal.getType() != EntityType.AXOLOTL
                && animal.getType() != EntityType.FROG && animal.getType() != EntityType.HORSE
                && animal.getType() != EntityType.DONKEY && animal.getType() != EntityType.MULE
                && animal.getType() != EntityType.LLAMA && animal.getType() != EntityType.TRADER_LLAMA;
        boolean ownerAllowed = !(animal instanceof TamableAnimal pet) || !pet.isTame()
                || friendId != null && friendId.equals(pet.getOwnerUUID());
        return SmallAnimalCarryPolicy.canCarry(new SmallAnimalCarryPolicy.Candidate(
                animal.isAlive(), landAnimal, animal.isBaby(), animal.getBbWidth(), animal.getBbHeight(),
                animal.getHealth() >= animal.getMaxHealth(),
                animal instanceof TamableAnimal pet && pet.isInSittingPose(),
                animal.isLeashed(), animal.isVehicle(), animal.isPassenger(), ownerAllowed))
                && !animal.isInWaterOrBubble() && !animal.isOnFire() && animal.getTarget() == null;
    }

    private void resetAnimalCarrySession() {
        animalCarryTarget = null;
        animalCarryApproachTicks = 0;
        animalCarryTicks = 0;
        animalCarryElapsedTicks = 0;
    }

    private void cancelAnimalCarryAttempt() {
        resetAnimalCarrySession();
        animalCarryCooldown = SmallAnimalCarryPolicy.FAILED_ATTEMPT_COOLDOWN_TICKS;
    }

    private void finishAnimalCarrySession(UUID animalId) {
        resetAnimalCarrySession();
        animalCarryCooldown = SmallAnimalCarryPolicy.nextGlobalCooldown(random::nextInt);
        if (animalId != null) {
            lastCarriedAnimalId = animalId;
            sameAnimalCooldown = SmallAnimalCarryPolicy.SAME_ANIMAL_COOLDOWN_TICKS;
        }
    }

    private boolean releaseCarriedAnimal(Animal animal) {
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        BlockPos origin = blockPosition();
        for (int radius = 1; radius <= 4; radius++) {
            for (int step = 0; step < 12; step++) {
                double angle = (step + random.nextDouble() * .25D) * Math.PI / 6.0D;
                int x = Mth.floor(getX() + Math.cos(angle) * radius);
                int z = Mth.floor(getZ() + Math.sin(angle) * radius);
                for (int dy = 2; dy >= -3; dy--) {
                    BlockPos feet = new BlockPos(x, origin.getY() + dy, z);
                    BlockPos support = feet.below();
                    if (!serverLevel.getBlockState(support).isFaceSturdy(serverLevel, support, Direction.UP)
                            || !serverLevel.getFluidState(support).isEmpty()
                            || !serverLevel.getFluidState(feet).isEmpty()) continue;
                    double halfWidth = animal.getBbWidth() * .5D;
                    AABB box = new AABB(x + .5D - halfWidth, feet.getY(), z + .5D - halfWidth,
                            x + .5D + halfWidth, feet.getY() + animal.getBbHeight(), z + .5D + halfWidth);
                    if (!serverLevel.noCollision(animal, box) || serverLevel.containsAnyLiquid(box)) continue;
                    animal.stopRiding();
                    animal.teleportTo(x + .5D, feet.getY(), z + .5D);
                    animal.setDeltaMovement(Vec3.ZERO);
                    animal.fallDistance = 0.0F;
                    finishAnimalCarrySession(animal.getUUID());
                    socialActionCooldown = Math.max(socialActionCooldown, 100);
                    queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP,
                            "finish carrying animal");
                    return true;
                }
            }
        }
        return false;
    }

    private void tickNameLearning() {
        String ownName = hasCustomName() ? NameLearningMemory.sanitizeName(getCustomName().getString()) : "";
        nameLearningMemory.forgetOwnNameIfAbsent(ownName);

        Player friend = getFriendPlayer();
        boolean friendPresent = friend instanceof ServerPlayer && friend.level() == level()
                && friend.isAlive() && !friend.isSpectator();
        boolean friendNearby = friendPresent && distanceToSqr(friend) <= 144.0D;
        boolean friendVisible = friendNearby && hasLineOfSight(friend);
        NearestVisibleLivingEntities visibleEntities = BrainUtils.getMemory(this,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        boolean visibleThreat = friendPresent && visibleEntities != null && visibleEntities
                .findClosest(entity -> entity instanceof Monster monster && monster.isAlive()
                        && monster.distanceToSqr(friend) <= 784.0D).isPresent();
        boolean higherPriorityAction = entityData.get(HEALING_TICKS) > 0
                || curiosityPhase != CuriosityPhase.NONE || !curiosityStack.isEmpty()
                || isVehicle() || getCarryAction() != CarryAction.NONE || hadCarriedPassenger
                || isFriendRescueActive() || getTarget() != null || isAggressive()
                || getActiveHostileReactionTarget() != null || visibleThreat
                || getCommand() == CompanionCommand.HOME;
        boolean canObserve = NameLearningMemory.canObserve(isHealed(), isDowned(), friendPresent,
                friendNearby, friendVisible, higherPriorityAction,
                getSocialAction() != SocialAction.NONE || socialActionTicks > 0);
        if (!canObserve) return;

        ServerPlayer serverFriend = (ServerPlayer) friend;
        if (nameLearningMemory.isNewOwnName(ownName)) {
            nameLearningMemory.rememberOwnName(ownName, random::nextInt);
            startNameReaction(serverFriend, friend, FadedDialogue.Category.NAME_LEARNED, ownName,
                    "learn own name");
            return;
        }

        if (CompanionPerformancePolicy.isCadenceTick(tickCount, getId(),
                CompanionPerformancePolicy.NAMED_PET_SCAN_TICKS)) {
            TamableAnimal namedPet = level().getEntitiesOfClass(TamableAnimal.class,
                            getBoundingBox().inflate(12.0D), animal -> animal.isAlive() && animal.hasCustomName()
                                    && serverFriend.getUUID().equals(animal.getOwnerUUID())
                                    && distanceToSqr(animal) <= 144.0D
                                    && hasLineOfSight(animal)
                                    && nameLearningMemory.isNewPetName(animal.getUUID(),
                                    animal.getCustomName().getString()))
                    .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (namedPet != null) {
                String petName = NameLearningMemory.sanitizeName(namedPet.getCustomName().getString());
                nameLearningMemory.rememberPet(namedPet.getUUID(), petName);
                startNameReaction(serverFriend, namedPet, FadedDialogue.Category.NAMED_PET, petName,
                        "learn named pet");
                return;
            }
        }

        boolean calmForRepeat = NameLearningMemory.isCalmForRepeat(onGround(),
                isInWaterOrBubble() || isOnFire() || level().isRainingAt(blockPosition()),
                shouldRun() || getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                        || !pendingLocomotion.isEmpty(), !getNavigation().isDone());
        if (calmForRepeat && socialActionCooldown <= 0 && nameLearningMemory.canRepeatOwnName(ownName)) {
            nameLearningMemory.resetRepeatCooldown(random::nextInt);
            startNameReaction(serverFriend, friend, FadedDialogue.Category.NAME_REPEAT, ownName,
                    "repeat own name");
        }
    }

    private void startNameReaction(ServerPlayer friend, Entity lookTarget, FadedDialogue.Category category,
                                   String visibleName, String lookReason) {
        setSocialAction(SocialAction.AFFECTION);
        socialActionTicks = 80;
        socialActionCooldown = Math.max(socialActionCooldown, 200);
        queueLookAt(lookTarget, 25.0F, 25.0F, lookReason);
        sendDialogue(friend, dialogueMemory.next(random, category), visibleName);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !isDowned() && !isVehicle() && (passenger instanceof Player player && isFriend(player)
                || animalCarryBoarding && passenger instanceof Animal);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof Player player && (level().isClientSide || isFriend(player)) ? player : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        setYRot(player.getYRot());
        yRotO = getYRot();
        setXRot(player.getXRot() * 0.3F);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza;
        if (forward <= 0.0F) forward *= 0.25F;
        return new Vec3(player.xxa * 0.42F, input.y, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * (player.isSprinting() ? 1.25F : 1.0F);
    }

    @Override
    public boolean canSprint() {
        return getControllingPassenger() instanceof Player;
    }

    public void requestPassengerBlink(ServerPlayer player) {
        if (level().isClientSide || !isHealed() || isDowned() || player.getVehicle() != this
                || getControllingPassenger() != player || !isFriend(player)
                || !player.getMainHandItem().isEmpty() || getCarriedBlinkCooldown() > 0) return;
        long now = level().getGameTime();
        if (lastCarriedBlinkRequestTick != Long.MIN_VALUE && now - lastCarriedBlinkRequestTick < 2L) return;
        lastCarriedBlinkRequestTick = now;
        if (blinkWithRider(player)) setCarriedBlinkCooldown(80);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) return;
        if (passenger instanceof Animal animal) {
            Vec3 position = animalCarryPosition(animal);
            moveFunction.accept(passenger, position.x, position.y, position.z);
            return;
        }
        double yaw = Math.toRadians(getYRot());
        double forward = 0.58D;
        double side = 0.05D;
        double x = getX() - Math.sin(yaw) * forward + Math.cos(yaw) * side;
        double z = getZ() + Math.cos(yaw) * forward + Math.sin(yaw) * side;
        moveFunction.accept(passenger, x, getY() + 2.05D, z);
    }

    private Vec3 animalCarryPosition(Animal animal) {
        double yaw = Math.toRadians(getYRot());
        double forward = 0.24D;
        return new Vec3(getX() - Math.sin(yaw) * forward,
                getY() + 1.58D - animal.getBbHeight() * .35D,
                getZ() + Math.cos(yaw) * forward);
    }

    private boolean isAnimalCarryPlacementClear(Animal animal) {
        Vec3 position = animalCarryPosition(animal);
        double halfWidth = animal.getBbWidth() * .5D;
        AABB box = new AABB(position.x - halfWidth, position.y, position.z - halfWidth,
                position.x + halfWidth, position.y + animal.getBbHeight(), position.z + halfWidth)
                .deflate(.01D);
        for (VoxelShape shape : level().getBlockCollisions(animal, box)) {
            if (!shape.isEmpty()) return false;
        }
        return !level().containsAnyLiquid(box);
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldLockSeatedBodyRotation()) lockSeatedBodyRotation();
    }

    private boolean shouldLockSeatedBodyRotation() {
        return hasSeatedYaw() && (!isHealed() || isDowned() || getCommand() == CompanionCommand.REST);
    }

    private void lockSeatedBodyRotation() {
        float yaw = entityData.get(SEATED_YAW);
        setYRot(yaw);
        yRotO = yaw;
        setYBodyRot(yaw);
        yBodyRotO = yaw;
    }

    private boolean hasSeatedYaw() { return entityData.get(SEATED_YAW_SET); }
    private void setSeatedYaw(float yaw) {
        entityData.set(SEATED_YAW, yaw);
        entityData.set(SEATED_YAW_SET, true);
    }

    private void tickSocialBehavior() {
        if (socialActionCooldown > 0) socialActionCooldown--;
        if (tickStrangerReaction()) return;
        if (tickFadeMeeting()) return;
        Player friend = getFriendPlayer();
        if (friend != null) {
            long absence = level().getGameTime() - lastFriendSeenTime;
            if (lastFriendSeenTime > 0 && absence > 24000L && tickCount < 80) {
                sendDialogue(friend, FadedDialogue.normal("dialogue.faded_pearl.returned"));
                modifyTrust(FadedTrustManager.RETURN_AFTER_ABSENCE);
            }
            lastFriendSeenTime = level().getGameTime();
            if (tickCount % 3600 == 0 && distanceToSqr(friend) <= 144.0D) addTrust(FadedTrustManager.PROXIMITY);
        }
        if (animalCarryTarget != null) return;
        boolean directThreatActive = selfDefenseTarget != null && selfDefenseTicks > 0;
        boolean reset = friend == null || distanceToSqr(friend) > 400.0D || isDowned()
                || !directThreatActive && (getCommand() == CompanionCommand.REST
                || getCommand() == CompanionCommand.HOME);
        boolean waterPause = !reset && getCommand() == CompanionCommand.FOLLOW && shouldPauseFollowForWater(friend);
        FadedSocialController.Decision gate = FadedSocialController.resolve(new FadedSocialController.Snapshot(
                friend == null, friend != null && distanceToSqr(friend) > 400.0D, isDowned(),
                getCommand() == CompanionCommand.REST && !directThreatActive,
                getCommand() == CompanionCommand.HOME && !directThreatActive,
                waterPause, false, false, false, false, false, false, false, false, false, false,
                false, false, false, false, false));
        if (gate.intent() == FadedSocialController.Intent.RESET) {
            setSocialAction(SocialAction.NONE);
            socialActionTicks = 0;
            return;
        }
        if (gate.intent() == FadedSocialController.Intent.WATER_PAUSE) {
            queueStop(FadedMovementCoordinator.Locomotion.FOLLOW_WATER_PAUSE, "friend water pause");
            queueLookAt(friend, 20.0F, getMaxHeadXRot(), "friend water pause");
            return;
        }

        if (Float.isNaN(lastFriendHealth)) lastFriendHealth = friend.getHealth();
        boolean friendHurt = friend.getHealth() + 0.1F < lastFriendHealth;
        lastFriendHealth = friend.getHealth();

        NearestVisibleLivingEntities visibleEntities = BrainUtils.getMemory(this,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        Monster danger = visibleEntities == null ? null : visibleEntities
                .findClosest(entity -> entity instanceof Monster monster && monster.isAlive()
                        && monster.distanceToSqr(friend) <= 784.0D)
                .filter(Monster.class::isInstance).map(Monster.class::cast).orElse(null);
        if (directThreatActive && selfDefenseTarget instanceof Monster directAttacker)
            danger = directAttacker;
        ItemEntity sensedFlower = null;
        if (carriedFlower.isEmpty() && getCommand() == CompanionCommand.FOLLOW
                && getSocialAction() != SocialAction.GROUND_FLOWER) {
            sensedFlower = BrainUtils.memoryOrDefault(this, SBLMemoryTypes.NEARBY_ITEMS.get(),
                            List::<ItemEntity>of).stream()
                    .filter(ItemEntity::isAlive)
                    .filter(item -> item.getItem().is(ItemTags.SMALL_FLOWERS))
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        boolean flowerHeld = friend.getMainHandItem().is(ItemTags.SMALL_FLOWERS)
                || friend.getOffhandItem().is(ItemTags.SMALL_FLOWERS);
        boolean playerGesture = friend.isCrouching()
                || (!friend.onGround() && friend.getDeltaMovement().y > 0.08D);
        boolean interestingItem = !friend.getMainHandItem().isEmpty() || !friend.getOffhandItem().isEmpty();
        boolean idleBlocked = socialActionCooldown > 0
                || getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
        FadedSocialController.Decision decision = FadedSocialController.resolve(new FadedSocialController.Snapshot(
                false, false, false, false, false, false, friendHurt, dangerSoundTicks > 0 && dangerSoundPos != null,
                !carriedFlower.isEmpty(), getSocialAction() == SocialAction.WORRIED && socialActionTicks > 0,
                level().isRainingAt(blockPosition()), danger != null && (getTarget() == null || selfDefenseTicks > 0),
                sensedFlower != null,
                friend.isSleeping(), !level().isDay() && distanceToSqr(friend) > 16.0D
                        && getCommand() == CompanionCommand.FOLLOW,
                flowerHeld && socialActionCooldown <= 0 && getSocialAction() != SocialAction.FLOWER_INTEREST,
                playerGesture && socialActionCooldown <= 0, interestingItem && socialActionCooldown <= 0,
                socialActionTicks > 0, worldReactionCooldown <= 0, idleBlocked));

        if (decision.friendHurt()) {
            setSocialAction(SocialAction.WORRIED);
            socialActionTicks = 120;
            socialActionCooldown = 100;
        }

        if (decision.intent() == FadedSocialController.Intent.SOUND_ALERT) {
            dangerSoundTicks--;
            setSocialAction(SocialAction.SOUND_ALERT);
            socialActionTicks = Math.max(socialActionTicks, 25);
            queueLookAt(dangerSoundPos, 40.0F, 35.0F, "danger sound");
            if (dangerSoundTicks == 0) dangerSoundPos = null;
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.DELIVER_FLOWER) {
            setSocialAction(SocialAction.BRING_FLOWER);
            queueLookAt(friend, 25.0F, 25.0F, "deliver flower");
            if (distanceToSqr(friend) > 6.25D) queueMoveTo(friend, 0.9D, "deliver flower");
            else {
                ItemStack gift = carriedFlower.copy();
                carriedFlower = ItemStack.EMPTY;
                if (!friend.addItem(gift)) spawnAtLocation(gift);
                if (friend instanceof ServerPlayer serverPlayer)
                    sendDialogue(serverPlayer, FadedDialogue.normal("dialogue.faded_pearl.flower_gift"));
                if (level() instanceof ServerLevel serverLevel)
                    serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.2D, getZ(), 4, .2D, .2D, .2D, .02D);
                modifyTrust(FadedTrustManager.FLOWER_GIFT);
                flowerCooldown = 6000;
                setSocialAction(SocialAction.NONE);
                socialActionCooldown = 300;
            }
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.CONTINUE_WORRIED) {
            queueLookAt(friend, 30.0F, 25.0F, "continue worried");
            if (distanceToSqr(friend) > 6.25D && getCommand() == CompanionCommand.FOLLOW)
                queueMoveTo(friend, 1.15D, "continue worried");
            socialActionTicks -= distanceToSqr(friend) < 9.0D ? 2 : 1;
            if (socialActionTicks <= 0) setSocialAction(SocialAction.NONE);
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.RAIN_NERVOUS) {
            setSocialAction(SocialAction.RAIN_NERVOUS);
            socialActionTicks = 40;
            queueLookAt(friend, 20.0F, 20.0F, "rain nervous");
            for (int attempt = 0; attempt < 4; attempt++) {
                Vec3 shelter = LandRandomPos.getPos(this, 8, 4);
                if (shelter != null && !level().isRainingAt(BlockPos.containing(shelter))) {
                    queueMoveTo(shelter, 1.05D, "rain shelter");
                    break;
                }
            }
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.HOSTILE_WARNING) {
            setSocialAction(SocialAction.SOUND_ALERT);
            socialActionTicks = 35;
            hostileReactionTarget = danger;
            queueLookAt(danger, 35.0F, 30.0F, "hostile warning");
            if (warningCooldown <= 0 && friend instanceof ServerPlayer serverPlayer) {
                sendDialogue(serverPlayer, FadedDialogue.overlay("dialogue.faded_pearl.warning"));
                warningCooldown = 400;
            }
            if (danger.distanceToSqr(friend) <= 64.0D && growlCooldown <= 0) {
                playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_STARE, .65F, 1.15F);
                growlCooldown = 120;
            }
            return;
        }

        // A dropped flower is a meaningful stimulus, not a random idle action.
        // Sensor memory makes it interrupt ordinary looking/wandering, while danger,
        // rain and concern for the player retain their higher priority above it.
        if (decision.intent() == FadedSocialController.Intent.GROUND_FLOWER) {
            setSocialAction(flowerCooldown > 0 ? SocialAction.FLOWER_INTEREST : SocialAction.GROUND_FLOWER);
            socialActionTicks = 120;
            socialActionCooldown = Math.max(socialActionCooldown, 80);
            queueLookAt(sensedFlower, 25.0F, 25.0F, "ground flower");
            if (flowerCooldown > 0) return;
            if (distanceToSqr(sensedFlower) <= 3.0D) {
                carriedFlower = sensedFlower.getItem().copyWithCount(1);
                flowerCooldown = 6000;
                sensedFlower.getItem().shrink(1);
                if (sensedFlower.getItem().isEmpty()) sensedFlower.discard();
                setSocialAction(SocialAction.BRING_FLOWER);
            } else {
                queueMoveTo(sensedFlower, .72D, "ground flower");
            }
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.WATCH_SLEEPING) {
            setSocialAction(SocialAction.GUARD);
            socialActionTicks = 60;
            if (distanceToSqr(friend) > 9.0D) queueMoveTo(friend, .72D, "watch sleeping");
            else queueStop(FadedMovementCoordinator.Locomotion.STOP, "watch sleeping nearby");
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.NIGHT_CLOSE) {
            setSocialAction(SocialAction.NIGHT_CLOSE);
            socialActionTicks = 50;
            queueMoveTo(friend, .78D, "night close");
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.FLOWER_HELD) {
            setSocialAction(SocialAction.FLOWER_INTEREST);
            socialActionTicks = 80;
            socialActionCooldown = 100;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.45D, getZ(), 2,
                        0.18D, 0.12D, 0.18D, 0.01D);
            }
            queueLookAt(friend, 20.0F, 20.0F, "flower held");
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.PLAYER_GESTURE) {
            setSocialAction(SocialAction.PLAYER_GESTURE);
            socialActionTicks = 50;
            socialActionCooldown = 100;
            queueLookAt(friend, 25.0F, 25.0F, "player gesture");
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.HELD_ITEM) {
            setSocialAction(SocialAction.HELD_ITEM);
            socialActionTicks = 70;
            socialActionCooldown = 140;
            queueLookAt(friend, 25.0F, 25.0F, "held item");
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.CONTINUE_ACTION) {
            socialActionTicks--;
            if (getSocialAction() == SocialAction.GROUND_FLOWER) {
                ItemEntity flower = BrainUtils.memoryOrDefault(this, SBLMemoryTypes.NEARBY_ITEMS.get(), List::<ItemEntity>of)
                        .stream().filter(ItemEntity::isAlive)
                        .filter(item -> item.getItem().is(ItemTags.SMALL_FLOWERS))
                        .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
                if (flower != null) {
                    if (distanceToSqr(flower) <= 3.0D) {
                        carriedFlower = flower.getItem().copyWithCount(1);
                        flowerCooldown = 6000;
                        flower.getItem().shrink(1);
                        if (flower.getItem().isEmpty()) flower.discard();
                        setSocialAction(SocialAction.BRING_FLOWER);
                        socialActionTicks = 100;
                    } else {
                        queueMoveTo(flower, .72D, "continue ground flower");
                        queueLookAt(flower, 25.0F, 25.0F, "continue ground flower");
                    }
                }
            }
            if (getSocialAction() == SocialAction.CURIOUS || getSocialAction() == SocialAction.FLOWER_INTEREST
                    || getSocialAction() == SocialAction.HELD_ITEM || getSocialAction() == SocialAction.PLAYER_GESTURE)
                queueLookAt(friend, 20.0F, 20.0F, "continue social action");
            if (getSocialAction() == SocialAction.LOOK_AROUND) {
                Vec3 viewed = friend.getEyePosition().add(friend.getLookAngle().scale(8.0D));
                queueLookAt(viewed, 25.0F, 25.0F, "continue look around");
            }
            if (socialActionTicks == 0) setSocialAction(SocialAction.NONE);
            return;
        }

        if (decision.intent() == FadedSocialController.Intent.TRY_WORLD_CURIOSITY
                && tickWorldCuriosity(friend)) return;

        FadedSocialController.IdleDecision idle = FadedSocialController.resolveIdle(
                idleBlocked, trust, getCommand() == CompanionCommand.FOLLOW, () -> random.nextInt(7));
        if (idle.intent() == FadedSocialController.Intent.IDLE_BLOCKED) return;
        SocialAction action = toSocialAction(idle.action());
        setSocialAction(action);
        if (action == SocialAction.REST_NEAR) {
            if (distanceToSqr(friend) > 9.0D) queueMoveTo(friend, .65D, "rest near");
            else queueStop(FadedMovementCoordinator.Locomotion.STOP, "rest near reached");
        } else if (action == SocialAction.PLAYFUL_TELEPORT) {
            Vec3 destination = LandRandomPos.getPos(this, 8, 3);
            if (destination != null)
                queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE,
                        destination.toString(), 0.0D, "playful teleport",
                        () -> trySafeTeleport(destination.x, destination.y, destination.z));
        }
        if (idle.consumeTimingRng()) {
            socialActionTicks = 50 + random.nextInt(51);
            socialActionCooldown = 120 + random.nextInt(161);
        }
    }

    private boolean tickStrangerReaction() {
        if (strangerReactionTarget == null || !(level() instanceof ServerLevel serverLevel)) return false;
        ServerPlayer stranger = serverLevel.getServer().getPlayerList().getPlayer(strangerReactionTarget);
        if (stranger == null || !stranger.isAlive() || stranger.level() != level()
                || distanceToSqr(stranger) > 100.0D || isDowned() || isVehicle()
                || isInWaterOrBubble() || isOnFire() || getTarget() != null
                || selfDefenseTarget != null || hostileReactionTarget != null
                || curiosityPhase != CuriosityPhase.NONE || getSocialAction() != SocialAction.CURIOUS) {
            clearStrangerReaction();
            return false;
        }
        if (socialActionTicks <= 0) {
            clearStrangerReaction();
            return false;
        }
        socialActionTicks--;
        queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP, "neutral stranger reaction");
        queueLookAt(stranger, 25.0F, 25.0F, "neutral stranger reaction");
        if (socialActionTicks == 0) clearStrangerReaction();
        return true;
    }

    private void clearStrangerReaction() {
        if (strangerReactionTarget != null) {
            strangerReactionTarget = null;
            socialActionTicks = 0;
            setSocialAction(SocialAction.NONE);
        }
    }

    private boolean tickFadeMeeting() {
        if (!(level() instanceof ServerLevel serverLevel) || !isHealed() || isDowned()
                || isVehicle() || isInWaterOrBubble() || isOnFire()
                || curiosityPhase != CuriosityPhase.NONE || getTarget() != null
                || selfDefenseTarget != null || hostileReactionTarget != null
                || getCommand() == CompanionCommand.REST || getCommand() == CompanionCommand.HOME) {
            clearFadeMeeting();
            return false;
        }

        if (fadeMeetingPartner != null && fadeMeetingTicks > 0) {
            Entity partnerEntity = serverLevel.getEntity(fadeMeetingPartner);
            if (partnerEntity instanceof FadedEnderman partner && partner.isAlive() && partner.isHealed()
                    && !partner.isDowned() && getUUID().equals(partner.fadeMeetingPartner)
                    && getSocialAction() == fadeMeetingAction
                    && distanceToSqr(partner) <= 100.0D) {
                fadeMeetingTicks--;
                socialActionTicks = fadeMeetingTicks;
                queueStop(FadedMovementCoordinator.Locomotion.STOP, "meeting another Fade");
                queueLookAt(partner, 30.0F, 25.0F, "meeting another Fade");
                if (fadeMeetingTicks == 0) clearFadeMeeting();
                return true;
            }
            clearFadeMeeting();
        }

        if (fadeMeetingCooldown > 0 || !CompanionPerformancePolicy.isCadenceTick(tickCount, getId(), 20)
                || socialActionTicks > 0
                || animalCarryTarget != null || selfDefenseTarget != null || hostileReactionTarget != null)
            return false;

        FadedEnderman partner = serverLevel.getEntitiesOfClass(FadedEnderman.class,
                        getBoundingBox().inflate(8.0D), candidate -> candidate != this
                                && candidate.isAlive() && candidate.isHealed() && !candidate.isDowned()
                                && !candidate.isVehicle() && !candidate.isInWaterOrBubble() && !candidate.isOnFire()
                                && candidate.curiosityPhase == CuriosityPhase.NONE
                                && candidate.getTarget() == null
                                && candidate.selfDefenseTarget == null
                                && candidate.hostileReactionTarget == null
                                && candidate.getCommand() != CompanionCommand.REST
                                && candidate.getCommand() != CompanionCommand.HOME
                                && candidate.fadeMeetingCooldown <= 0
                                && candidate.fadeMeetingPartner == null
                                && candidate.socialActionTicks <= 0)
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (partner == null) {
            fadeMeetingCooldown = CompanionPerformancePolicy.FADE_MEETING_MISS_COOLDOWN_TICKS;
            return false;
        }
        if (getUUID().compareTo(partner.getUUID()) > 0) return false;

        beginFadeMeeting(partner, SocialAction.CURIOUS);
        partner.beginFadeMeeting(this, SocialAction.LOOK_AROUND);
        notifyOwnerOfFadeMeeting();
        partner.notifyOwnerOfFadeMeeting();
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                (getX() + partner.getX()) * 0.5D, Math.min(getY(), partner.getY()) + 1.8D,
                (getZ() + partner.getZ()) * 0.5D, 8, .35D, .35D, .35D, .02D);
        return true;
    }

    private void notifyOwnerOfFadeMeeting() {
        Player friend = getFriendPlayer();
        if (friend instanceof ServerPlayer serverPlayer && distanceToSqr(friend) <= 256.0D)
            sayRandom(serverPlayer, FadedDialogue.Category.FADE_MEETING);
    }

    private void beginFadeMeeting(FadedEnderman partner, SocialAction action) {
        fadeMeetingPartner = partner.getUUID();
        fadeMeetingTicks = 80;
        fadeMeetingCooldown = 2400 + random.nextInt(1201);
        fadeMeetingAction = action;
        socialActionCooldown = Math.max(socialActionCooldown, fadeMeetingTicks);
        setSocialAction(action);
        socialActionTicks = fadeMeetingTicks;
    }

    private void clearFadeMeeting() {
        boolean active = fadeMeetingPartner != null;
        fadeMeetingPartner = null;
        fadeMeetingTicks = 0;
        fadeMeetingAction = SocialAction.NONE;
        if (active) {
            socialActionTicks = 0;
            setSocialAction(SocialAction.NONE);
        }
    }

    private static SocialAction toSocialAction(FadedSocialController.IdleAction action) {
        return switch (action) {
            case CURIOUS -> SocialAction.CURIOUS;
            case LOOK_AROUND -> SocialAction.LOOK_AROUND;
            case TOUCH_WOUND -> SocialAction.TOUCH_WOUND;
            case PEEK_CORNER -> SocialAction.PEEK_CORNER;
            case REST_NEAR -> SocialAction.REST_NEAR;
            case PLAYFUL_TELEPORT -> SocialAction.PLAYFUL_TELEPORT;
            case AFFECTION -> SocialAction.AFFECTION;
        };
    }

    private void tickSocialReposition() {
        if (socialRepositionCooldown > 0) socialRepositionCooldown--;
        Player friend = getFriendPlayer();
        boolean calm = canUseSocialReposition(friend);
        if (socialRepositionTarget != null) {
            if (!calm || socialRepositionFriendOrigin == null
                    || friend.position().distanceToSqr(socialRepositionFriendOrigin) >= .04D
                    || !(level() instanceof ServerLevel serverLevel)
                    || !isSafeHomeLanding(serverLevel, socialRepositionTarget)
                    || --socialRepositionTicks <= 0) {
                cancelSocialReposition("social reposition interrupted");
                return;
            }
            Vec3 destination = Vec3.atBottomCenterOf(socialRepositionTarget);
            if (position().distanceToSqr(destination) <= 1.0D) {
                queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE,
                        socialRepositionTarget.toShortString(), 0.0D, "social reposition reached",
                        this::applyNavigationStop);
                if (socialRepositionWatchFriend) queueLookAt(friend, 20.0F, 20.0F,
                        "social reposition watch friend");
                else queueLookAt(friend.getEyePosition().add(friend.getLookAngle().scale(6.0D)),
                        20.0F, 20.0F, "social reposition watch surroundings");
                setSocialAction(socialRepositionWatchFriend ? SocialAction.CURIOUS : SocialAction.LOOK_AROUND);
                journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_SOCIAL_REPOSITION);
                socialActionTicks = 60 + random.nextInt(41);
                socialActionCooldown = Math.max(socialActionCooldown, socialActionTicks);
                clearSocialRepositionState();
                return;
            }
            if (socialRepositionStarted && getNavigation().isDone()) {
                cancelSocialReposition("social reposition path ended");
                return;
            }
            Path route = socialRepositionPath;
            queueLocomotion(FadedMovementCoordinator.Locomotion.SOCIAL_MOVE,
                    socialRepositionTarget.toShortString(), .68D, "calm social reposition", () -> {
                        if (socialRepositionStarted) return;
                        socialRepositionStarted = true;
                        socialRepositionPath = null;
                        if (route == null || !applyMoveTo(route, .68D)) {
                            applyNavigationStop();
                            cancelSocialReposition("social reposition path rejected");
                        }
                    });
            if (socialRepositionWatchFriend) queueLookAt(friend, 16.0F, 18.0F,
                    "social reposition friend look");
            else queueLookAt(destination, 16.0F, 18.0F, "social reposition path look");
            return;
        }

        if (socialRepositionCooldown > 0 || !calm
                || random.nextInt(CompanionLivelinessPolicy.SOCIAL_START_ROLL) != 0) return;
        DryRoute route = findSocialRepositionRoute(friend);
        if (route == null) {
            socialRepositionCooldown = 600;
            return;
        }
        socialRepositionTarget = route.destination();
        socialRepositionPath = route.path();
        socialRepositionStarted = false;
        socialRepositionFriendOrigin = friend.position();
        socialRepositionWatchFriend = random.nextBoolean();
        socialRepositionTicks = CompanionLivelinessPolicy.SOCIAL_MOVE_TIMEOUT;
        socialRepositionCooldown = CompanionLivelinessPolicy.socialCooldown(
                random.nextInt(CompanionLivelinessPolicy.SOCIAL_COOLDOWN_VARIANCE));
    }

    private boolean canUseSocialReposition(Player friend) {
        boolean friendStationary = friend != null && friend.onGround() && !friend.isSprinting()
                && !friend.isFallFlying() && !friend.getAbilities().flying
                && friend.getDeltaMovement().horizontalDistanceSqr() < .0025D;
        boolean threat = selfDefenseTarget != null || hostileReactionTarget != null
                || getTarget() != null || isAggressive() || dangerSoundTicks > 0;
        return CompanionLivelinessPolicy.canSocialMove(new CompanionLivelinessPolicy.SocialMoveSnapshot(
                trust, getCommand() == CompanionCommand.FOLLOW,
                friend != null && friend.isAlive() && !friend.isSpectator() && distanceToSqr(friend) <= 64.0D,
                friendStationary, isHealed(), isDowned(), entityData.get(HEALING_TICKS) > 0,
                isVehicle() || getCarryAction() != CarryAction.NONE || hadCarriedPassenger,
                isFriendRescueActive(), threat,
                isInWaterOrBubble() || isOnFire() || friend != null && (friend.isInWaterOrBubble() || friend.isOnFire()),
                curiosityPhase != CuriosityPhase.NONE || !curiosityStack.isEmpty(),
                interactionMovementStopLatched,
                getSocialAction() != SocialAction.NONE || socialActionTicks > 0 || !carriedFlower.isEmpty()));
    }

    private DryRoute findSocialRepositionRoute(Player friend) {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = CompanionLivelinessPolicy.SOCIAL_MIN_DISTANCE
                    + random.nextDouble() * (CompanionLivelinessPolicy.SOCIAL_MAX_DISTANCE
                    - CompanionLivelinessPolicy.SOCIAL_MIN_DISTANCE);
            int x = Mth.floor(friend.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(friend.getZ() + Math.sin(angle) * distance);
            for (int dy = 2; dy >= -4; dy--) {
                BlockPos feet = new BlockPos(x, friend.blockPosition().getY() + dy, z);
                Vec3 destination = Vec3.atBottomCenterOf(feet);
                double dx = destination.x - friend.getX();
                double dz = destination.z - friend.getZ();
                if (!CompanionLivelinessPolicy.isInSocialRing(dx * dx + dz * dz)
                        || !isSafeHomeLanding(serverLevel, feet)) continue;
                Path path = createDryPathTo(feet);
                if (path != null) return new DryRoute(feet.immutable(), path);
            }
        }
        return null;
    }

    private void cancelSocialReposition(String reason) {
        queueStop(FadedMovementCoordinator.Locomotion.STOP, reason);
        clearSocialRepositionState();
    }

    private void clearSocialRepositionState() {
        socialRepositionTarget = null;
        socialRepositionFriendOrigin = null;
        socialRepositionPath = null;
        socialRepositionStarted = false;
        socialRepositionTicks = 0;
    }

    private void performAffection(ServerPlayer player) {
        // Keep ambient world observations from talking over a deliberate player interaction.
        worldReactionCooldown = Math.max(worldReactionCooldown, 200);
        if (trustInteractionCooldown <= 0) {
            modifyTrust(FadedTrustManager.TOUCH);
            trustInteractionCooldown = 1200;
        }
        FadedInteractionHandler.TouchIntent intent = FadedInteractionHandler.classifyTouch(trust, random::nextBoolean);
        switch (intent) {
            case TOUCH_RECOIL -> applyTouchReaction(player, SocialAction.TOUCH_RECOIL, 35,
                    FadedDialogue.Category.TOUCH_RECOIL);
            case TOUCH_HESITATE -> applyTouchReaction(player, SocialAction.TOUCH_HESITATE, 40,
                    FadedDialogue.Category.TOUCH_HESITATE);
            case LOOK_AROUND -> applyTouchReaction(player, SocialAction.LOOK_AROUND, 45,
                    FadedDialogue.Category.TOUCH_LEARNING);
            case EMBRACE_READY -> applyTouchReaction(player, SocialAction.EMBRACE_READY, 70,
                    FadedDialogue.Category.TOUCH_DEVOTION);
            case CHEST_EXPOSE -> applyTouchReaction(player, SocialAction.CHEST_EXPOSE, 75,
                    FadedDialogue.Category.TOUCH_TRUST);
            case AFFECTION -> applyTouchReaction(player, SocialAction.AFFECTION, 55,
                    FadedDialogue.Category.TOUCH_AFFECTION);
            case CURIOUS -> applyTouchReaction(player, SocialAction.CURIOUS, 45,
                    FadedDialogue.Category.TOUCH_GENTLE);
        }
        queueLookAt(player, 25.0F, 25.0F, "touch interaction");
    }

    private void applyTouchReaction(ServerPlayer player, SocialAction action, int ticks,
                                    FadedDialogue.Category dialogueCategory) {
        setSocialAction(action);
        socialActionTicks = ticks;
        sayRandom(player, dialogueCategory);
    }

    private boolean tickWorldCuriosity(Player friend) {
        if (!(friend instanceof ServerPlayer serverPlayer) || distanceToSqr(friend) > 144.0D || random.nextInt(3) != 0)
            return false;
        java.util.List<String> reactions = new java.util.ArrayList<>();
        BlockPos viewedPos = BlockPos.containing(friend.getEyePosition().add(friend.getLookAngle().scale(6.0D)));
        var viewedState = level().getBlockState(viewedPos);
        if (viewedState.getBlock() instanceof FlowerBlock) reactions.add("flower_seen");
        if (viewedState.is(Blocks.CHEST) || viewedState.is(Blocks.BARREL)) reactions.add("container");
        if (viewedState.is(Blocks.FIRE) || viewedState.is(Blocks.SOUL_FIRE) || viewedState.is(Blocks.CAMPFIRE)) reactions.add("fire");
        if (viewedState.is(Blocks.WATER) || !level().getFluidState(viewedPos).isEmpty()) reactions.add("water");
        if (viewedState.is(Blocks.DIAMOND_ORE) || viewedState.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || viewedState.is(Blocks.EMERALD_ORE) || viewedState.is(Blocks.DEEPSLATE_EMERALD_ORE)) reactions.add("ore");
        if (viewedState.is(Blocks.CRAFTING_TABLE) || viewedState.is(Blocks.FURNACE)) reactions.add("crafting");
        if (!level().canSeeSky(friend.blockPosition()) && friend.getY() < 35) reactions.add("cave");
        if (!level().isDay() && level().canSeeSky(friend.blockPosition())) reactions.add("stars");
        if (level().isRainingAt(friend.blockPosition())) reactions.add("rain");
        Animal animal = level().getEntitiesOfClass(Animal.class, friend.getBoundingBox().inflate(10.0D), Animal::isAlive)
                .stream().findFirst().orElse(null);
        if (animal != null) reactions.add("animal");
        Villager villager = level().getEntitiesOfClass(Villager.class, friend.getBoundingBox().inflate(12.0D), Villager::isAlive)
                .stream().findFirst().orElse(null);
        if (villager != null) reactions.add("villager");
        if (!friend.getMainHandItem().isEmpty()) reactions.add("held_item");
        reactions.removeIf(lastWorldReaction::equals);
        if (reactions.isEmpty()) return false;
        String reaction = reactions.get(random.nextInt(reactions.size()));
        lastWorldReaction = reaction;
        worldReactionCooldown = 700 + random.nextInt(701);
        setSocialAction(reaction.equals("held_item") ? SocialAction.HELD_ITEM : SocialAction.CURIOUS);
        socialActionTicks = 70;
        if (animal != null && reaction.equals("animal")) queueLookAt(animal, 25.0F, 25.0F, "world animal");
        else if (villager != null && reaction.equals("villager")) queueLookAt(villager, 25.0F, 25.0F, "world villager");
        else queueLookAt(Vec3.atCenterOf(viewedPos), 25.0F, 25.0F, "world block");
        if (FadedTrustManager.isWorldCautious(trust))
            sayRandom(serverPlayer, FadedDialogue.Category.WORLD_CAUTIOUS);
        else if (FadedTrustManager.isWorldLearning(trust))
            sayRandom(serverPlayer, FadedDialogue.Category.WORLD_LEARNING);
        else
            sayRandom(serverPlayer, FadedDialogue.worldReaction(reaction));
        return true;
    }

    private void tickProtector() {
        Player friend = getFriendPlayer();
        boolean downed = friend != null && isDowned();
        boolean carryingPassenger = friend != null && !downed && isVehicle();
        Monster attacker = findProtectorAttacker(friend, downed);
        FadedProtectionController.Decision decision = FadedProtectionController.resolve(
                new FadedProtectionController.Snapshot(
                        friend != null, downed, attacker != null, carryingPassenger,
                        friend == null ? 0.0F : friend.fallDistance, trust,
                        friend == null ? 0.0D : distanceToSqr(friend),
                        friend != null && friend.isInLava(),
                        friend == null ? 0 : friend.getRemainingFireTicks(), protectorCooldown,
                        friend == null ? 0.0F : friend.getHealth(),
                        friend == null ? 0.0F : friend.getMaxHealth()));
        if (friend == null || downed) return;
        if (decision.guardAttacker() && getCommand() != CompanionCommand.REST) {
            Vec3 shieldPoint = friend.position().add(attacker.position().subtract(friend.position()).normalize().scale(1.6D));
            if (distanceToSqr(shieldPoint) > 2.0D)
                queueLocomotion(FadedMovementCoordinator.Locomotion.GUARD_INTERPOSE,
                        shieldPoint.toString(), 1.2D, "guard interpose",
                        () -> applyMoveTo(shieldPoint, 1.2D));
            queueTarget(attacker, "guard attacker");
            setSocialAction(SocialAction.GUARD);
        }
        if (decision.rescue() == FadedProtectionController.RescueIntent.FALL) {
            BlockPos landing = findNearestSafeLanding(friend.blockPosition(), 6, 18);
            if (landing != null) {
                queueLocomotion(FadedMovementCoordinator.Locomotion.RESCUE_FALL, landing.toShortString(), 0.0D,
                        "fall rescue", () -> {
                            teleportTo(landing.getX() + 1.5D, landing.getY(), landing.getZ() + .5D);
                            friend.teleportTo(landing.getX() + .5D, landing.getY(), landing.getZ() + .5D);
                            friend.fallDistance = 0;
                            if (level() instanceof ServerLevel serverLevel)
                                serverLevel.sendParticles(ParticleTypes.PORTAL, friend.getX(), friend.getY() + 1.0D, friend.getZ(),
                                        35, .35D, .6D, .35D, .06D);
                            if (friend instanceof ServerPlayer serverPlayer)
                                sayRandom(serverPlayer, FadedDialogue.Category.RESCUE_FALL);
                            journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_RESCUE);
                            addTrust(FadedTrustManager.FALL_RESCUE);
                            protectorCooldown = 80;
                        });
            }
            return;
        }
        if (decision.rescue() == FadedProtectionController.RescueIntent.LAVA_OR_FIRE) {
            BlockPos landing = findNearestSafeLanding(friend.blockPosition(),
                    FadedProtectionController.FIRE_RESCUE_MAX_HORIZONTAL_RADIUS, 12, true);
            queueLocomotion(FadedMovementCoordinator.Locomotion.RESCUE_FIRE,
                    landing == null ? null : landing.toShortString(), 0.0D, "fire rescue", () -> {
                        if (landing != null) {
                            teleportTo(landing.getX() + 1.5D, landing.getY(), landing.getZ() + .5D);
                            friend.teleportTo(landing.getX() + .5D, landing.getY(), landing.getZ() + .5D);
                            friend.fallDistance = 0;
                        }
                        clearFire();
                        extinguishFire();
                        friend.clearFire();
                        friend.extinguishFire();
                        addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
                        friend.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
                        protectorCooldown = 240;
                        if (friend instanceof ServerPlayer serverPlayer)
                            sayRandom(serverPlayer, FadedDialogue.Category.RESCUE_LAVA);
                        journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_RESCUE);
                        modifyTrust(FadedTrustManager.LAVA_RESCUE);
                    });
            return;
        }
        if (decision.rescue() == FadedProtectionController.RescueIntent.LOW_HEALTH) {
            queueLocomotion(FadedMovementCoordinator.Locomotion.RESCUE_LOW_HEALTH,
                    friend.getStringUUID(), 0.0D, "low health rescue", () -> {
                        if (homePos != null && homeDimension != null && level().dimension().equals(homeDimension)) {
                            friend.teleportTo(homePos.getX() + .5D, homePos.getY() + 1.0D, homePos.getZ() + .5D);
                            teleportTo(homePos.getX() + 1.5D, homePos.getY() + 1.0D, homePos.getZ() + .5D);
                        } else {
                            teleportNearFriend(friend, 2.0D);
                            friend.teleportTo(getX(), getY(), getZ());
                        }
                        protectorCooldown = 600;
                        setSocialAction(SocialAction.GUARD);
                        if (friend instanceof ServerPlayer serverPlayer)
                            sayRandom(serverPlayer, FadedDialogue.Category.RESCUE_HEALTH);
                        journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_RESCUE);
                        modifyTrust(FadedTrustManager.LOW_HEALTH_RESCUE);
                    });
        }
    }

    private Monster findProtectorAttacker(Player friend, boolean downed) {
        if (friend == null || downed) {
            protectorScanAttacker = null;
            return null;
        }
        boolean cachedValid = protectorScanAttacker != null && protectorScanAttacker.isAlive()
                && !protectorScanAttacker.isRemoved() && protectorScanAttacker.level() == level()
                && protectorScanAttacker.getTarget() == friend
                && protectorScanAttacker.distanceToSqr(friend) <= 144.0D;
        if (!cachedValid) protectorScanAttacker = null;
        if (CompanionPerformancePolicy.isCadenceTick(tickCount, getId(),
                CompanionPerformancePolicy.PROTECTOR_HOSTILE_SCAN_TICKS)) {
            protectorScanAttacker = level().getEntitiesOfClass(Monster.class,
                    friend.getBoundingBox().inflate(12.0D),
                    mob -> mob.isAlive() && mob.getTarget() == friend).stream().findFirst().orElse(null);
        }
        return protectorScanAttacker;
    }

    private void tickNearbyMobReactions() {
        if (mobReactionCooldown > 0 || isDowned() || isVehicle()) return;
        if (!CompanionPerformancePolicy.isCadenceTick(tickCount, getId(),
                CompanionPerformancePolicy.AMBIENT_MOB_SCAN_TICKS)) return;
        LivingEntity enemy = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12.0D),
                entity -> entity.isAlive() && entity instanceof Enemy).stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (enemy != null) {
            setSocialAction(SocialAction.SOUND_ALERT);
            socialActionTicks = 50;
            hostileReactionTarget = enemy;
            queueLookAt(enemy, 35.0F, 30.0F, "nearby hostile reaction");
            mobReactionCooldown = 100;
            return;
        }
        if (socialActionTicks > 0 || getDeltaMovement().horizontalDistanceSqr() > 0.0025D) return;
        Animal animal = level().getEntitiesOfClass(Animal.class, getBoundingBox().inflate(6.0D), Animal::isAlive)
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (animal != null) {
            setSocialAction(SocialAction.CURIOUS);
            socialActionTicks = 60;
            queueLookAt(animal, 25.0F, 25.0F, "nearby animal reaction");
            mobReactionCooldown = 160;
        }
    }

    private BlockPos findNearestSafeLanding(BlockPos origin, int horizontalRadius, int verticalRange) {
        return findNearestSafeLanding(origin, horizontalRadius, verticalRange, false);
    }

    private BlockPos findNearestSafeLanding(BlockPos origin, int horizontalRadius, int verticalRange,
                                            boolean requireFireRescueDistance) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    if (requireFireRescueDistance
                            && !FadedProtectionController.isValidFireRescueLandingOffset(dx, dz)) continue;
                    for (int dy = 2; dy >= -verticalRange; dy--) {
                        BlockPos feet = origin.offset(dx, dy, dz);
                        BlockPos floor = feet.below();
                        var floorState = level().getBlockState(floor);
                        if (!floorState.isCollisionShapeFullBlock(level(), floor)
                                || floorState.is(Blocks.LAVA) || floorState.is(Blocks.WATER)) continue;
                        if (!level().isEmptyBlock(feet) || !level().isEmptyBlock(feet.above())
                                || !level().getFluidState(feet).isEmpty() || !level().getFluidState(feet.above()).isEmpty()) continue;
                        double distance = feet.distSqr(origin);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = feet.immutable();
                        }
                        break;
                    }
                }
            }
            if (best != null) break;
        }
        return best;
    }

    private void playDistortedCry(boolean quiet) {
        setCrying(true);
        cryingTicks = FadedVoiceController.CRY_PRESENTATION_TICKS;
        queueStop(FadedMovementCoordinator.Locomotion.STOP, "distorted cry");
        float pitch = 0.94F + random.nextFloat() * 0.12F;
        level().playSound(null, blockPosition(), ModSounds.FADED_ENDERMAN_CAVE_CRY.get(), SoundSource.HOSTILE,
                quiet ? 0.38F : 2.8F, quiet ? pitch + 0.05F : pitch);
    }

    private void playWoundedNotice() {
        setCrying(false);
        cryingTicks = 0;
        cryCooldown = Math.max(cryCooldown, FadedVoiceController.WOUNDED_NOTICE_TICKS);
        level().playSound(null, blockPosition(), ModSounds.FADED_ENDERMAN_WOUNDED_NOTICE.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void tickAmbientVoice() {
        Player friend = getFriendPlayer();
        boolean friendNearby = friend != null && friend.isAlive() && !friend.isSpectator()
                && distanceToSqr(friend) <= 64.0D;
        boolean socialActionActive = getSocialAction() != SocialAction.NONE || socialActionTicks > 0;
        boolean curiosityActive = curiosityPhase != CuriosityPhase.NONE || !curiosityStack.isEmpty();
        boolean carrying = isVehicle() || getCarryAction() != CarryAction.NONE || hadCarriedPassenger;
        boolean combatActive = getTarget() != null || isAggressive() || getActiveHostileReactionTarget() != null;
        boolean unsafeEnvironment = isInWaterOrBubble() || isOnFire() || !onGround();
        boolean moving = shouldRun() || getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                || !getNavigation().isDone() || !pendingLocomotion.isEmpty();
        FadedVoiceController.AmbientDecision decision = FadedVoiceController.tickAmbient(
                ambientVoiceCooldown,
                new FadedVoiceController.AmbientSnapshot(
                        isHealed(), friendNearby, isDowned(), entityData.get(HEALING_TICKS) > 0,
                        getCommand() == CompanionCommand.REST, socialActionActive, curiosityActive,
                        carrying, combatActive, isFriendRescueActive(), unsafeEnvironment, moving),
                trust, random::nextInt);
        ambientVoiceCooldown = decision.nextCooldown();
        if (!decision.play()) return;

        SocialAction action = switch (decision.pose()) {
            case CURIOUS -> SocialAction.CURIOUS;
            case LOOK_AROUND -> SocialAction.LOOK_AROUND;
            case AFFECTION -> SocialAction.AFFECTION;
        };
        setSocialAction(action);
        socialActionTicks = decision.variant().presentationTicks();
        socialActionCooldown = Math.max(socialActionCooldown, socialActionTicks);
        if (action == SocialAction.LOOK_AROUND) {
            Vec3 viewed = friend.getEyePosition().add(friend.getLookAngle().scale(8.0D));
            queueLookAt(viewed, 25.0F, 25.0F, "ambient voice look around");
        } else {
            queueLookAt(friend, 20.0F, 20.0F, "ambient voice friend");
        }

        var sound = switch (decision.variant()) {
            case ENDERMAN_ONE -> ModSounds.FADED_ENDERMAN_AMBIENT_1.get();
            case ENDERMAN_TWO -> ModSounds.FADED_ENDERMAN_AMBIENT_2.get();
            case VOICE_164238 -> ModSounds.FADED_ENDERMAN_AMBIENT_3.get();
        };
        level().playSound(null, blockPosition(), sound, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void spawnEmotionalParticles() {
        if (isHealed()) {
            if (random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.PORTAL, getRandomX(.7D), getY() + random.nextDouble() * 2.8D,
                        getRandomZ(.7D), (random.nextDouble() - .5D) * .08D, -.02D,
                        (random.nextDouble() - .5D) * .08D);
            }
            if (random.nextInt(4) == 0) {
                int color = getHealingColor();
                Vector3f rgb = new Vector3f(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                        (color & 255) / 255.0F);
                level().addParticle(new DustParticleOptions(rgb, 0.55F), getRandomX(0.45D), getY() + 1.45D,
                        getRandomZ(0.45D), 0.0D, 0.008D, 0.0D);
            }
        } else {
            if (random.nextInt(22) == 0)
                level().addParticle(ParticleTypes.DRIPPING_WATER, getX() + (random.nextDouble() - 0.5D) * 0.28D,
                        getY() + 1.55D, getZ() - 0.31D, 0.0D, -0.01D, 0.0D);
            if (random.nextInt(28) == 0)
                level().addParticle(ParticleTypes.PORTAL, getRandomX(.55D), getY() + random.nextDouble() * 2.5D,
                        getRandomZ(.55D), 0, -.015D, 0);
        }
    }

    private Player getFriendPlayer() {
        return friendId == null ? null : level().getPlayerByUUID(friendId);
    }

    private void tickSelfDefense() {
        if (selfDefenseTicks > 0) selfDefenseTicks--;
        if (selfDefenseTarget == null) return;
        if (selfDefenseTicks <= 0 || !selfDefenseTarget.isAlive() || selfDefenseTarget.isRemoved()
                || distanceToSqr(selfDefenseTarget) > 1024.0D || isDowned()) {
            if (getTarget() == selfDefenseTarget) queueClearTarget("self defense ended");
            selfDefenseTarget = null;
            selfDefenseTicks = 0;
            return;
        }
        hostileReactionTarget = selfDefenseTarget;
        if (getCommand() != CompanionCommand.REST && !isVehicle())
            queueTarget(selfDefenseTarget, "maintain direct attacker self defense");
    }

    private boolean isAcceptedCombatTarget(LivingEntity target) {
        return shouldProtectFriendFrom(target)
                || target == selfDefenseTarget && selfDefenseTicks > 0 && target.isAlive();
    }

    private LivingEntity getActiveHostileReactionTarget() {
        if (hostileReactionTarget == null) return null;
        if (getSocialAction() != SocialAction.SOUND_ALERT || socialActionTicks <= 0
                || !hostileReactionTarget.isAlive() || hostileReactionTarget.isRemoved()
                || distanceToSqr(hostileReactionTarget) > 144.0D) {
            hostileReactionTarget = null;
            return null;
        }
        return hostileReactionTarget;
    }

    private void maintainHostileReactionLook() {
        LivingEntity hostile = getActiveHostileReactionTarget();
        if (hostile == null || getCommand() == CompanionCommand.REST
                || entityData.get(RUNNING_TO_FRIEND)) return;
        queueLookAt(hostile, 35.0F, 30.0F, "maintain hostile reaction");
    }

    private boolean shouldProtectFriendFrom(LivingEntity target) {
        if (!isHealed() || isDowned() || getCommand() == CompanionCommand.REST || isVehicle()) return false;
        Player friend = getFriendPlayer();
        if (friend == null || target == friend) return false;
        return target instanceof Mob mob && mob.getTarget() == friend;
    }

    private boolean isFriendRescueActive() {
        Player friend = getFriendPlayer();
        if (friend == null) return false;
        FadedProtectionController.Decision decision = FadedProtectionController.resolve(
                new FadedProtectionController.Snapshot(true, isDowned(), false, isVehicle(), friend.fallDistance,
                        trust, distanceToSqr(friend), friend.isInLava(), friend.getRemainingFireTicks(),
                        protectorCooldown, friend.getHealth(), friend.getMaxHealth()));
        return decision.rescue() != FadedProtectionController.RescueIntent.NONE;
    }

    private boolean isCombatAllowed(boolean protectionTargetAccepted) {
        return FadedMovementCoordinator.isCombatAllowed(new FadedMovementCoordinator.CombatSnapshot(
                isHealed(), isDowned(), entityData.get(HEALING_TICKS) > 0,
                getCommand() == CompanionCommand.REST, isVehicle(), isInWaterOrBubble(), isOnFire(),
                isFriendRescueActive(), protectionTargetAccepted));
    }

    private boolean canCombatBlink(LivingEntity target) {
        return CombatBlinkPolicy.canAttempt(combatBlinkCooldown, isVehicle(),
                target.level() == level(), distanceToSqr(target));
    }

    /** Flank only within loaded, dry, collision-free space; the movement adapter owns the teleport. */
    private boolean tryCombatBlink(LivingEntity target) {
        if (!canCombatBlink(target) || !(level() instanceof ServerLevel serverLevel)) return false;
        combatBlinkCooldown = CombatBlinkPolicy.FAILED_COOLDOWN_TICKS;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.7D + random.nextDouble() * 1.0D;
            int x = net.minecraft.util.Mth.floor(target.getX() + Math.cos(angle) * radius);
            int z = net.minecraft.util.Mth.floor(target.getZ() + Math.sin(angle) * radius);
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos feet = new BlockPos(x, net.minecraft.util.Mth.floor(target.getY()) + dy, z);
                if (!serverLevel.hasChunkAt(feet) || !isSafeHomeLanding(serverLevel, feet)) continue;
                var floor = serverLevel.getBlockState(feet.below());
                if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CAMPFIRE)
                        || floor.is(Blocks.SOUL_CAMPFIRE) || floor.is(Blocks.POWDER_SNOW)) continue;
                if (serverLevel.getBlockState(feet).is(Blocks.FIRE)
                        || serverLevel.getBlockState(feet).is(Blocks.SOUL_FIRE)
                        || serverLevel.getBlockState(feet).is(Blocks.SWEET_BERRY_BUSH)
                        || serverLevel.getBlockState(feet).is(Blocks.WITHER_ROSE)
                        || serverLevel.getBlockState(feet.above()).is(Blocks.FIRE)
                        || serverLevel.getBlockState(feet.above()).is(Blocks.SOUL_FIRE)
                        || serverLevel.getBlockState(feet.above(2)).is(Blocks.FIRE)
                        || serverLevel.getBlockState(feet.above(2)).is(Blocks.SOUL_FIRE)) continue;
                if (trySafeTeleport(feet.getX() + .5D, feet.getY(), feet.getZ() + .5D)) {
                    combatBlinkCooldown = CombatBlinkPolicy.SUCCESS_COOLDOWN_TICKS;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean blinkWithRider(ServerPlayer rider) {
        Vec3 start = rider.getEyePosition();
        Vec3 direction = rider.getLookAngle().normalize();
        if (direction.lengthSqr() < 0.01D) return false;
        Vec3 end = start.add(direction.scale(16.0D));
        BlockHitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, rider));
        Set<BlockPos> candidates = new LinkedHashSet<>();
        if (hit.getType() == HitResult.Type.BLOCK) {
            addBlinkHitCandidates(candidates, hit);
        } else {
            addBlinkAirCandidates(candidates, start, direction);
        }
        int checked = 0;
        for (BlockPos candidate : candidates) {
            if (++checked > 384) break;
            if (isVisibleSafePassengerLanding(rider, start, candidate)
                    && trySafeTeleportWithPassenger(rider, candidate)) return true;
        }
        return false;
    }

    private void addBlinkHitCandidates(Set<BlockPos> candidates, BlockHitResult hit) {
        BlockPos visibleSide = hit.getBlockPos().relative(hit.getDirection());
        int[] verticalOffsets = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8, 9, -9, 10, -10};
        for (int dy : verticalOffsets) addBlinkCandidateRing(candidates, visibleSide.offset(0, dy, 0), 2);
        BlockPos aboveHit = hit.getBlockPos().above();
        for (int dy = 0; dy <= 10; dy++) addBlinkCandidateRing(candidates, aboveHit.above(dy), 2);
    }

    private void addBlinkAirCandidates(Set<BlockPos> candidates, Vec3 start, Vec3 direction) {
        for (int distance = 16; distance >= 2; distance--) {
            BlockPos rayPos = BlockPos.containing(start.add(direction.scale(distance)));
            for (int dy = 2; dy >= -12; dy--) addBlinkCandidateRing(candidates, rayPos.offset(0, dy, 0), 1);
        }
    }

    private void addBlinkCandidateRing(Set<BlockPos> candidates, BlockPos center, int radius) {
        candidates.add(center);
        for (int ring = 1; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                candidates.add(center.offset(dx, 0, -ring));
                candidates.add(center.offset(dx, 0, ring));
            }
            for (int dz = -ring + 1; dz < ring; dz++) {
                candidates.add(center.offset(-ring, 0, dz));
                candidates.add(center.offset(ring, 0, dz));
            }
        }
    }

    private boolean isVisibleSafePassengerLanding(ServerPlayer rider, Vec3 start, BlockPos feet) {
        double x = feet.getX() + 0.5D;
        double y = feet.getY();
        double z = feet.getZ() + 0.5D;
        Vec3 vehicleCenter = new Vec3(x, y + getBbHeight() * 0.5D, z);
        if (start.distanceToSqr(vehicleCenter) > 256.0D || !isSafePassengerLanding(rider, feet)) return false;
        BlockHitResult visibility = level().clip(new ClipContext(start, vehicleCenter, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, rider));
        return visibility.getType() == HitResult.Type.MISS;
    }

    private boolean isSafePassengerLanding(ServerPlayer rider, BlockPos feet) {
        double x = feet.getX() + 0.5D;
        double y = feet.getY();
        double z = feet.getZ() + 0.5D;
        if (!hasDrySolidSupport(x, y, z)) return false;
        double dx = x - getX(), dy = y - getY(), dz = z - getZ();
        AABB vehicleBox = getBoundingBox().move(dx, dy, dz);
        if (!level().noCollision(this, vehicleBox) || level().containsAnyLiquid(vehicleBox)) return false;
        double yaw = Math.toRadians(getYRot());
        double riderX = x - Math.sin(yaw) * 0.58D + Math.cos(yaw) * 0.05D;
        double riderY = y + 2.05D;
        double riderZ = z + Math.cos(yaw) * 0.58D + Math.sin(yaw) * 0.05D;
        AABB riderBox = rider.getBoundingBox().move(riderX - rider.getX(), riderY - rider.getY(), riderZ - rider.getZ());
        return level().noCollision(rider, riderBox) && !level().containsAnyLiquid(riderBox);
    }

    private BlockPos findNearestSafePassengerLanding(ServerPlayer rider, BlockPos origin,
                                                      int horizontalRadius, int verticalRange) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    for (int dy = 2; dy >= -verticalRange; dy--) {
                        BlockPos feet = origin.offset(dx, dy, dz);
                        if (!isSafePassengerLanding(rider, feet)) continue;
                        double distance = feet.distSqr(origin);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = feet.immutable();
                        }
                        break;
                    }
                }
            }
            if (best != null) break;
        }
        return best;
    }

    private boolean trySafeTeleportWithPassenger(ServerPlayer rider, BlockPos feet) {
        double oldX = getX(), oldY = getY(), oldZ = getZ();
        double x = feet.getX() + 0.5D, y = feet.getY(), z = feet.getZ() + 0.5D;
        teleportTo(x, y, z);
        if (!hasPassenger(rider) || !level().noCollision(this) || level().containsAnyLiquid(getBoundingBox())
                || !level().noCollision(rider) || level().containsAnyLiquid(rider.getBoundingBox())) {
            teleportTo(oldX, oldY, oldZ);
            return false;
        }
        rider.connection.send(new ClientboundMoveVehiclePacket(this));
        rider.connection.teleport(rider.getX(), rider.getY(), rider.getZ(), rider.getYRot(), rider.getXRot());
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, oldX, oldY + 1.5D, oldZ, 32, .35D, .8D, .35D, .08D);
            serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.5D, getZ(), 32, .35D, .8D, .35D, .08D);
        }
        playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.05F);
        return true;
    }

    private void teleportNearFriend(Player friend, double radius) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + random.nextDouble() * radius;
            if (trySafeTeleport(friend.getX() + Math.cos(angle) * distance, friend.getY(),
                    friend.getZ() + Math.sin(angle) * distance)) return;
        }
    }

    private boolean canFollowTeleportTo(Player friend) {
        return friend.onGround() && !friend.isFallFlying() && !friend.getAbilities().flying
                && hasDrySolidSupport(friend.getX(), friend.getY(), friend.getZ());
    }

    private boolean shouldPauseFollowForWater(Player friend) {
        if (friend.isInWaterOrBubble()) waterFollowPaused = true;
        else if (waterFollowPaused && canFollowTeleportTo(friend)) waterFollowPaused = false;
        return waterFollowPaused;
    }

    private void escapeWaterToDryLand() {
        queueLocomotion(FadedMovementCoordinator.Locomotion.ESCAPE_WATER, null, 0.0D,
                "water self rescue", () -> {
                    applyNavigationStop();
                    if (getControllingPassenger() instanceof ServerPlayer rider && isFriend(rider)) {
                        BlockPos passengerLanding = findNearestSafePassengerLanding(rider, blockPosition(), 16, 12);
                        if (passengerLanding != null && trySafeTeleportWithPassenger(rider, passengerLanding))
                            selfRescueSucceededSinceLastDecision = true;
                        return;
                    }
                    BlockPos landing = getCommand() == CompanionCommand.FOLLOW && getFriendPlayer() != null
                            ? findNearestVisibleFollowSelfRescueLanding(blockPosition(), 16, 12)
                            : findNearestSafeLanding(blockPosition(), 16, 12);
                    if (landing != null && trySafeTeleport(landing.getX() + .5D,
                            landing.getY(), landing.getZ() + .5D)) selfRescueSucceededSinceLastDecision = true;
                });
    }

    private boolean teleportNearGroundedFriend(Player friend, double radius) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + random.nextDouble() * radius;
            double x = friend.getX() + Math.cos(angle) * distance;
            double z = friend.getZ() + Math.sin(angle) * distance;
            if (hasDrySolidSupport(x, friend.getY(), z) && trySafeTeleport(x, friend.getY(), z)) return true;
        }
        return false;
    }

    private boolean hasDrySolidSupport(double x, double y, double z) {
        BlockPos feet = BlockPos.containing(x, y, z);
        BlockPos support = BlockPos.containing(x, y - 0.01D, z);
        return level().getFluidState(feet).isEmpty() && level().getFluidState(support).isEmpty()
                && level().getBlockState(support).isFaceSturdy(level(), support, Direction.UP);
    }

    private boolean trySafeTeleport(double x, double y, double z) {
        if (getFirstPassenger() instanceof Animal animal && !releaseCarriedAnimal(animal)) return false;
        double oldX = getX(), oldY = getY(), oldZ = getZ();
        teleportTo(x, y, z);
        if (!level().noCollision(this) || level().containsAnyLiquid(getBoundingBox())) {
            teleportTo(oldX, oldY, oldZ);
            return false;
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, oldX, oldY + 1.5D, oldZ, 32, .35D, .8D, .35D, .08D);
            serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.5D, getZ(), 32, .35D, .8D, .35D, .08D);
        }
        playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.05F);
        return true;
    }

    private boolean tryEscapePearlTeleport(Player attacker) {
        BlockPos origin = attacker.blockPosition();
        int angleOffset = random.nextInt(32);
        int radiusOffset = random.nextInt(9);
        for (int radiusStep = 0; radiusStep < 9; radiusStep++) {
            int radius = 8 + (radiusOffset + radiusStep) % 9;
            for (int step = 0; step < 32; step++) {
                double angle = (angleOffset + step) * (Math.PI * 2.0D / 32.0D);
                int x = Mth.floor(attacker.getX() + Math.cos(angle) * radius);
                int z = Mth.floor(attacker.getZ() + Math.sin(angle) * radius);
                for (int dy = 6; dy >= -8; dy--) {
                    BlockPos feet = new BlockPos(x, origin.getY() + dy, z);
                    if (!level().hasChunkAt(feet) || !isEscapePearlLanding(feet, attacker)) continue;
                    if (trySafeTeleport(feet.getX() + .5D, feet.getY(), feet.getZ() + .5D)) return true;
                }
            }
        }
        return false;
    }

    private boolean isEscapePearlLanding(BlockPos feet, Player attacker) {
        double dx = feet.getX() + .5D - attacker.getX();
        double dz = feet.getZ() + .5D - attacker.getZ();
        double horizontalDistanceSqr = dx * dx + dz * dz;
        if (horizontalDistanceSqr < 64.0D || horizontalDistanceSqr > 256.0D) return false;
        BlockPos floor = feet.below();
        var floorState = level().getBlockState(floor);
        if (!floorState.isCollisionShapeFullBlock(level(), floor)
                || floorState.is(Blocks.LAVA) || floorState.is(Blocks.WATER)
                || floorState.is(Blocks.MAGMA_BLOCK)) return false;
        for (int height = 0; height < 3; height++) {
            BlockPos body = feet.above(height);
            if (!level().isEmptyBlock(body) || !level().getFluidState(body).isEmpty()) return false;
        }
        return true;
    }

    public boolean isHealed() { return entityData.get(HEALED); }
    public int getDownedSeconds() { return entityData.get(DOWNED_SECONDS); }
    public void setHealed(boolean value) { entityData.set(HEALED, value); }
    public int getHealingColor() { return entityData.get(HEALING_COLOR); }
    public int getHeartColor() { return getHealingColor(); }
    public void setHealingColor(int color) { entityData.set(HEALING_COLOR, color); }
    public Optional<UUID> getFriendId() { return Optional.ofNullable(friendId); }
    public boolean isFriend(Player player) { return friendId != null && friendId.equals(player.getUUID()); }
    public Optional<EndermanJournalSnapshot> createJournalSnapshot(UUID requestingPlayer) {
        if (!isHealed() || friendId == null || !friendId.equals(requestingPlayer)) return Optional.empty();
        return Optional.of(journalSnapshot());
    }
    private EndermanJournalSnapshot journalSnapshot() {
        return EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                journalMemory.snapshot(), worldAwarenessMemory.snapshot(), !curiosityTrustGranted.isEmpty(),
                nameLearningMemory.ownName(), !nameLearningMemory.petSnapshot().isEmpty(), hasHome(),
                getHealingColor(), trust));
    }
    private void tickJournalNotification() {
        EndermanJournalSnapshot current = journalSnapshot();
        EndermanJournalSnapshot previous = journalNotificationBaseline;
        journalNotificationBaseline = current;
        if (!journalMemory.has(JournalMemory.Discovery.DAY_MEETING)
                || !JournalNotificationPolicy.hasNewEntry(previous, current)) return;
        Player friend = getFriendPlayer();
        if (friend instanceof ServerPlayer serverFriend) ModNetwork.notifyJournalDiscovery(serverFriend);
    }
    public CompanionCommand getCommand() { return CompanionCommand.values()[Math.min(entityData.get(COMMAND), CompanionCommand.values().length - 1)]; }
    public void setCommand(CompanionCommand command) {
        entityData.set(COMMAND, command.ordinal());
        if (command != CompanionCommand.FOLLOW) entityData.set(RUNNING_TO_FRIEND, false);
        if (!isHealed()) return;
        if (command == CompanionCommand.REST) setSeatedYaw(getYRot());
        else entityData.set(SEATED_YAW_SET, false);
    }
    public int getTrust() { return level().isClientSide ? entityData.get(SYNCED_TRUST) : trust; }
    public TrustStage getTrustStage() { return TrustStage.from(getTrust()); }
    public void addTrust(int amount) { modifyTrust(amount); }
    public void modifyTrust(int amount) {
        if (level().isClientSide) return;
        int appliedAmount = FadedTrustManager.scaleModifier(amount,
                FadedServerConfig.TRUST_GAIN_PERCENT.get(), FadedServerConfig.TRUST_LOSS_PERCENT.get());
        int previous = trust;
        setTrustValue(FadedTrustManager.applyModifier(trust, appliedAmount));
        if (FadedTrustManager.isPositiveModifier(appliedAmount)) lastInteractionTick = level().getGameTime();
        if (!level().isClientSide && trust < previous && getFriendPlayer() instanceof ServerPlayer serverFriend) {
            TrustLossNotice loss = TrustLossNotice.from(previous, amount, appliedAmount);
            if (loss.points() > 0) ModNetwork.notifyTrustLoss(serverFriend, loss.points(), loss.reason());
        }
    }

    private void setTrustValue(int value) {
        trust = Mth.clamp(value, FadedTrustManager.MIN_TRUST, FadedTrustManager.MAX_TRUST);
        entityData.set(SYNCED_TRUST, trust);
    }

    public void setHome(ResourceKey<Level> dimension, BlockPos pos) {
        homeDimension = dimension;
        homePos = pos.immutable();
        if (level() instanceof ServerLevel serverLevel) FadedPearlSavedData.get(serverLevel).setHome(getUUID(), dimension, pos);
    }
    public boolean hasHome() {
        if ((homePos == null || homeDimension == null) && level() instanceof ServerLevel serverLevel) {
            FadedPearlSavedData data = FadedPearlSavedData.get(serverLevel);
            homePos = data.homePos(getUUID()).orElse(null);
            homeDimension = data.homeDimension(getUUID()).orElse(null);
        }
        return homePos != null && homeDimension != null;
    }

    public void reactToDangerSound(Vec3 position) {
        if (!isHealed() || isDowned()) return;
        dangerSoundPos = position;
        dangerSoundTicks = 60;
    }

    public boolean recallTo(ServerLevel target, BlockPos pos) {
        if (level() != target) return false;
        double distanceSquared = distanceToSqr(Vec3.atCenterOf(pos));
        teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D);
        setCommand(CompanionCommand.FOLLOW);
        int reward = FadedServerConfig.ANCHOR_RECALL_TRUST_REWARD.get();
        if (AnchorRecallRewardPolicy.shouldReward(distanceSquared, reward)) addTrust(reward);
        target.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.4D, getZ(),
                45, .45D, .9D, .45D, .08D);
        return true;
    }
    public boolean isDowned() { return entityData.get(DOWNED); }
    public void setDowned(boolean value) {
        entityData.set(DOWNED, value);
        if (!level().isClientSide) entityData.set(DOWNED_SECONDS,
                value ? CompanionRecoveryStatus.secondsUntilStand(0) : 0);
    }
    public boolean isCrying() { return entityData.get(CRYING); }
    public void setCrying(boolean value) { entityData.set(CRYING, value); }
    public boolean isPlayerNearby() { return entityData.get(PLAYER_NEARBY); }
    public SocialAction getSocialAction() {
        return SocialAction.values()[Math.min(entityData.get(SOCIAL_ACTION), SocialAction.values().length - 1)];
    }
    private void setSocialAction(SocialAction action) { entityData.set(SOCIAL_ACTION, action.ordinal()); }
    public CarryAction getCarryAction() {
        return CarryAction.values()[Math.min(entityData.get(CARRY_ACTION), CarryAction.values().length - 1)];
    }
    private void setCarryAction(CarryAction action) { entityData.set(CARRY_ACTION, action.ordinal()); }
    public int getCarriedBlinkCooldown() { return entityData.get(CARRIED_BLINK_COOLDOWN); }
    private void setCarriedBlinkCooldown(int ticks) { entityData.set(CARRIED_BLINK_COOLDOWN, Math.max(0, ticks)); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            boolean carrying = isVehicle() || getCarryAction() == CarryAction.CARRYING;
            boolean moving = carrying ? entityData.get(CARRY_MOVING) : state.isMoving();
            FadedAnimationState.Snapshot snapshot = new FadedAnimationState.Snapshot(
                    isHealed(), isCrying(), isPlayerNearby(), entityData.get(HEALING_TICKS) > 0,
                    FadedAnimationState.CarryAction.valueOf(getCarryAction().name()), isVehicle(), isDowned(),
                    FadedAnimationState.CompanionCommand.valueOf(getCommand().name()),
                    FadedAnimationState.SocialAction.valueOf(getSocialAction().name()), shouldRun(), moving);
            RawAnimation animation = switch (FadedAnimationState.resolve(snapshot)) {
                case SIT -> SIT;
                case CRY -> CRY;
                case QUIET_CRY -> QUIET_CRY;
                case NOTICE -> NOTICE;
                case IDLE -> IDLE;
                case WALK -> WALK;
                case RUN -> RUN;
                case REST -> REST;
                case CURIOUS -> CURIOUS;
                case LOOK_AROUND -> LOOK_AROUND;
                case FLOWER_INTEREST -> FLOWER_INTEREST;
                case HELD_ITEM -> HELD_ITEM;
                case PLAYER_GESTURE -> PLAYER_GESTURE;
                case HEALING -> HEALING;
                case TOUCH_WOUND -> TOUCH_WOUND;
                case HUG -> HUG;
                case PEEK -> PEEK;
                case AFFECTION -> AFFECTION;
                case RAIN_NERVOUS -> RAIN_NERVOUS;
                case WORRIED -> WORRIED;
                case HOSTILE_WARNING -> HOSTILE_WARNING;
                case GROUND_FLOWER -> GROUND_FLOWER;
                case PICK_UP_PLAYER -> PICK_UP_PLAYER;
                case CARRY_IDLE -> CARRY_IDLE;
                case CARRY_WALK -> CARRY_WALK;
                case PUT_DOWN_PLAYER -> PUT_DOWN_PLAYER;
                case JUMP_REACT -> JUMP_REACT;
                case CROUCH -> CROUCH;
                case STARE_FREEZE -> STARE_FREEZE;
                case STARE_TILT -> STARE_TILT;
                case TOUCH_RECOIL -> TOUCH_RECOIL;
                case TOUCH_HESITATE -> TOUCH_HESITATE;
                case RAIN_SHELTER -> RAIN_SHELTER;
                case RAIN_SHIVER -> RAIN_SHIVER;
                case DOWNED_IDLE -> DOWNED_IDLE;
                case DOWNED_RECOVER -> DOWNED_RECOVER;
                case CHEST_EXPOSE -> CHEST_EXPOSE;
                case EMBRACE_READY -> EMBRACE_READY;
                case NIGHT_GAZE -> NIGHT_GAZE;
                case SNOW_CATCH -> SNOW_CATCH;
                case WATCH_SLEEPING -> WATCH_SLEEPING;
                case ITEM_POINT -> ITEM_POINT;
                case ITEM_INSPECT -> ITEM_INSPECT;
            };
            return state.setAndContinue(animation);
        }));
    }

    private boolean shouldRun() {
        return entityData.get(RUNNING_TO_FRIEND) || getDeltaMovement().horizontalDistanceSqr() > 0.07D;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public ItemStack getCuriosityDisplayStack() { return entityData.get(CURIOSITY_DISPLAY); }

    private void tickItemCuriosity() {
        Player friend = getFriendPlayer();
        ServerPlayer globalFriend = friendId == null || !(level() instanceof ServerLevel serverLevel) ? null
                : serverLevel.getServer().getPlayerList().getPlayer(friendId);
        if (curiosityPhase == CuriosityPhase.RETURN_PENDING) {
            if (globalFriend != null && returnCuriosityStack(globalFriend)) clearCuriosity(true);
            return;
        }
        if (curiosityStack.isEmpty() && curiosityPhase == CuriosityPhase.INSPECT) {
            clearCuriosity(true);
            return;
        }
        boolean curiosityThreat = hasCuriosityThreat(friend);
        boolean interrupted = curiosityPhase != CuriosityPhase.NONE
                && ItemCuriosityStateMachine.shouldInterrupt(new ItemCuriosityStateMachine.Interruption(
                entityData.get(HEALING_TICKS) > 0,
                getSocialAction() == SocialAction.GUARD || curiosityThreat, getTarget() != null,
                isVehicle() || getCarryAction() != CarryAction.NONE, isFriendRescueActive(), isDowned(), getCommand() == CompanionCommand.REST
                || getCommand() == CompanionCommand.HOME,
                friend == null, friend != null && friend.level() != level(),
                isInWaterOrBubble() || isOnFire() || interactionMovementStopLatched
                        || friend != null && (friend.isInWaterOrBubble() || friend.isOnFire())
                        || (friend != null && distanceToSqr(friend) > 400.0D)));
        if (interrupted) {
            if (curiosityStack.isEmpty()) clearCuriosity(true);
            else if (globalFriend != null && returnCuriosityStack(globalFriend)) clearCuriosity(true);
            else {
                curiosityPhase = CuriosityPhase.RETURN_PENDING;
                setSocialAction(SocialAction.NONE);
            }
            return;
        }
        if (curiosityPhase == CuriosityPhase.INSPECT) {
            queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP, "inspect borrowed item");
            queueLookAt(friend, 25.0F, 25.0F, "inspect borrowed item");
            setSocialAction(SocialAction.ITEM_INSPECT);
            if (--curiosityTicks <= 0) completeCuriosity(friend);
            return;
        }
        if (curiosityPhase == CuriosityPhase.APPROACH_GROUND
                || curiosityPhase == CuriosityPhase.POINT_GROUND
                || curiosityPhase == CuriosityPhase.INSPECT_GROUND) {
            tickGroundCuriosity();
            return;
        }
        if (curiosityPhase == CuriosityPhase.POINT_HAND) {
            ItemStack held = friend.getMainHandItem();
            if (!ItemCuriosityPolicy.isEligible(held)
                    || !curiosityExpectedItem.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(held.getItem()).toString())) { clearCuriosity(true); return; }
            queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP, "point at held item");
            queueLookAt(friend, 25.0F, 25.0F, "point at held item");
            setSocialAction(SocialAction.ITEM_POINT);
            if (--curiosityTicks <= 0) clearCuriosity(true);
            return;
        }
        if (curiosityCooldown > 0 || trust < ItemCuriosityPolicy.MIN_TRUST || friend == null
                || getCommand() == CompanionCommand.REST || getCommand() == CompanionCommand.HOME
                || isDowned() || isVehicle() || entityData.get(HEALING_TICKS) > 0
                || isInWaterOrBubble() || isOnFire() || isFriendRescueActive()
                || getTarget() != null || curiosityThreat) return;
        if (!CompanionPerformancePolicy.isCadenceTick(tickCount, getId(),
                CompanionPerformancePolicy.CURIOSITY_IDLE_SCAN_TICKS)) return;
        if (ItemCuriosityPolicy.isEligible(friend.getMainHandItem())) {
            curiosityPhase = CuriosityPhase.POINT_HAND;
            curiosityExpectedItem = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(friend.getMainHandItem().getItem()).toString();
            curiosityTicks = ItemCuriosityPolicy.HAND_OFFER_TICKS;
            curiosityHintPending = true;
            curiosityHintDelay = ItemCuriosityPolicy.HINT_DELAY_TICKS;
            return;
        }
        List<ItemEntity> groundCandidates = level().getEntitiesOfClass(ItemEntity.class,
                        getBoundingBox().inflate(ItemCuriosityPolicy.GROUND_NOTICE_RANGE),
                        item -> item.isAlive() && item.onGround() && !item.isInWaterOrBubble() && !item.isInLava()
                                && ItemCuriosityPolicy.isEligible(item.getItem())
                                && distanceToSqr(item) <= ItemCuriosityPolicy.GROUND_NOTICE_RANGE
                                * ItemCuriosityPolicy.GROUND_NOTICE_RANGE && hasLineOfSight(item)).stream()
                .sorted(java.util.Comparator.comparingDouble(this::distanceToSqr))
                .limit(6).toList();
        for (ItemEntity ground : groundCandidates) {
            DryRoute route = findGroundCuriosityRoute(ground);
            if (route == null) continue;
            curiosityPhase = CuriosityPhase.APPROACH_GROUND;
            curiosityGroundTarget = ground.getUUID();
            curiosityGroundDestination = route.destination();
            curiosityGroundPath = route.path();
            curiosityGroundPathStarted = false;
            curiosityTicks = ItemCuriosityPolicy.GROUND_APPROACH_TIMEOUT_TICKS;
            return;
        }
        if (!groundCandidates.isEmpty()) {
            startCuriosityCooldown();
        }
    }

    private void tickGroundCuriosity() {
        ItemEntity item = validGroundCuriosityTarget();
        if (item == null) {
            clearCuriosity(true);
            return;
        }
        if (curiosityPhase == CuriosityPhase.APPROACH_GROUND) {
            if (curiosityGroundDestination == null || --curiosityTicks <= 0
                    || !(level() instanceof ServerLevel serverLevel)
                    || !isSafeHomeLanding(serverLevel, curiosityGroundDestination)) {
                clearCuriosity(true);
                return;
            }
            if (position().distanceToSqr(Vec3.atBottomCenterOf(curiosityGroundDestination)) <= 1.0D) {
                queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP,
                        "ground curiosity reached");
                queueLookAt(item, 25.0F, 25.0F, "ground curiosity reached");
                curiosityPhase = CuriosityPhase.POINT_GROUND;
                curiosityTicks = ItemCuriosityPolicy.POINT_TICKS;
                setSocialAction(SocialAction.ITEM_POINT);
                return;
            }
            if (curiosityGroundPathStarted && getNavigation().isDone()) {
                clearCuriosity(true);
                return;
            }
            Path route = curiosityGroundPath;
            queueLocomotion(FadedMovementCoordinator.Locomotion.CURIOSITY_APPROACH,
                    curiosityGroundDestination.toShortString(), .72D, "approach ground curiosity", () -> {
                        if (curiosityGroundPathStarted) return;
                        curiosityGroundPathStarted = true;
                        curiosityGroundPath = null;
                        if (route == null || !applyMoveTo(route, .72D)) {
                            applyNavigationStop();
                            clearCuriosity(true);
                        }
                    });
            queueLookAt(item, 25.0F, 25.0F, "approach ground curiosity");
            setSocialAction(SocialAction.NONE);
            return;
        }

        queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP,
                curiosityPhase == CuriosityPhase.POINT_GROUND
                        ? "point at ground item" : "inspect ground item");
        if (distanceToSqr(item) > ItemCuriosityPolicy.GROUND_STOP_DISTANCE
                * ItemCuriosityPolicy.GROUND_STOP_DISTANCE) {
            clearCuriosity(true);
            return;
        }
        queueLookAt(item, 25.0F, 25.0F,
                curiosityPhase == CuriosityPhase.POINT_GROUND
                        ? "point at ground item" : "inspect ground item");
        if (curiosityPhase == CuriosityPhase.POINT_GROUND) {
            setSocialAction(SocialAction.ITEM_POINT);
            if (--curiosityTicks <= 0) {
                curiosityPhase = CuriosityPhase.INSPECT_GROUND;
                curiosityTicks = ItemCuriosityPolicy.GROUND_INSPECT_TICKS;
                setSocialAction(SocialAction.ITEM_INSPECT);
            }
        } else {
            setSocialAction(SocialAction.ITEM_INSPECT);
            if (--curiosityTicks <= 0) clearCuriosity(true);
        }
    }

    private ItemEntity validGroundCuriosityTarget() {
        if (!(level() instanceof ServerLevel serverLevel) || curiosityGroundTarget == null) return null;
        Entity target = serverLevel.getEntity(curiosityGroundTarget);
        if (!(target instanceof ItemEntity item) || !item.isAlive() || !item.onGround()
                || item.isInWaterOrBubble() || item.isInLava()
                || !ItemCuriosityPolicy.isEligible(item.getItem()) || !hasLineOfSight(item)) return null;
        return item;
    }

    private boolean hasCuriosityThreat(Player friend) {
        if (hostileReactionTarget != null || selfDefenseTarget != null || dangerSoundTicks > 0) return true;
        NearestVisibleLivingEntities visible = BrainUtils.getMemory(this,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        return friend != null && visible != null && visible.findClosest(entity -> entity instanceof Monster monster
                && monster.isAlive() && monster.distanceToSqr(friend) <= 784.0D).isPresent();
    }

    private DryRoute findGroundCuriosityRoute(ItemEntity item) {
        if (!(level() instanceof ServerLevel serverLevel) || !hasLineOfSight(item)) return null;
        BlockPos itemPos = item.blockPosition();
        int angleOffset = random.nextInt(16);
        for (int sample = 0; sample < 16; sample++) {
            double angle = (angleOffset + sample) * Math.PI / 8.0D;
            double radius = 1.75D + (sample % 3) * .35D;
            int x = Mth.floor(item.getX() + Math.cos(angle) * radius);
            int z = Mth.floor(item.getZ() + Math.sin(angle) * radius);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos feet = new BlockPos(x, itemPos.getY() + dy, z);
                if (Vec3.atBottomCenterOf(feet).distanceToSqr(item.position())
                        > ItemCuriosityPolicy.GROUND_STOP_DISTANCE * ItemCuriosityPolicy.GROUND_STOP_DISTANCE
                        || !isSafeHomeLanding(serverLevel, feet) || !isGroundItemVisibleFrom(feet, item)) continue;
                Path path = createDryPathTo(feet);
                if (path != null) return new DryRoute(feet.immutable(), path);
            }
        }
        return null;
    }

    private boolean isGroundItemVisibleFrom(BlockPos feet, ItemEntity item) {
        Vec3 start = Vec3.atBottomCenterOf(feet).add(0.0D, getEyeHeight(), 0.0D);
        Vec3 end = item.position().add(0.0D, Math.max(.1D, item.getBbHeight() * .5D), 0.0D);
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private Path createDryPathTo(BlockPos destination) {
        float previousWaterMalus = getPathfindingMalus(BlockPathTypes.WATER);
        try {
            setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
            Path path = getNavigation().createPath(destination, 0);
            return path != null && path.canReach() && !pathUsesAnyFluid(path) ? path : null;
        } finally {
            setPathfindingMalus(BlockPathTypes.WATER, previousWaterMalus);
        }
    }

    private boolean pathUsesAnyFluid(Path path) {
        for (int index = path.getNextNodeIndex(); index < path.getNodeCount(); index++) {
            var node = path.getNode(index);
            if (!level().getFluidState(node.asBlockPos()).isEmpty()) return true;
        }
        return false;
    }

    private void tickCuriosityHint() {
        if (!curiosityHintPending || curiosityPhase != CuriosityPhase.POINT_HAND) return;
        if (curiosityHintDelay > 0) {
            curiosityHintDelay--;
            return;
        }
        if (lastDialogueTick == level().getGameTime()) return;
        Player friend = getFriendPlayer();
        if (!(friend instanceof ServerPlayer serverPlayer)) return;
        serverPlayer.sendSystemMessage(Component.translatable(
                "hint.faded_pearl.item_curiosity.offer"), true);
        lastThoughtTick = level().getGameTime();
        curiosityHintPending = false;
    }

    private void startCuriosityCooldown() {
        curiosityCooldown = ItemCuriosityPolicy.COOLDOWN_TICKS;
        curiosityCooldownStartedThisTick = true;
    }

    private void completeCuriosity(Player friend) {
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(curiosityStack.getItem()).toString();
        if (returnCuriosityStack(friend)) {
            if (ItemCuriosityStateMachine.grantsTrust(curiosityTrustGranted.contains(itemId), true)) {
                curiosityTrustGranted.add(itemId);
                addTrust(2);
            }
            if (friend instanceof ServerPlayer serverPlayer)
                sayRandom(serverPlayer, FadedDialogue.Category.ITEM_CURIOSITY);
            clearCuriosity(true);
        }
    }

    public void respondToCommand(ServerPlayer player, CompanionCommand command) {
        journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_COMMANDS);
        if (trust >= 60) journalMemory.discover(JournalMemory.Discovery.BASIC_FADE_ORIGIN);
        FadedDialogue.Category category = switch (command) {
            case FOLLOW -> FadedDialogue.Category.COMMAND_FOLLOW;
            case STAY -> FadedDialogue.Category.COMMAND_STAY;
            case REST -> FadedDialogue.Category.COMMAND_REST;
            case HOME -> FadedDialogue.Category.COMMAND_HOME;
        };
        sayRandom(player, category);
    }

    private boolean returnCuriosityStack(Player friend) {
        if (curiosityStack.isEmpty()) return true;
        ItemStack exact = curiosityStack.copy();
        if (friend != null && friend.getInventory().add(exact)) {
            curiosityStack = ItemStack.EMPTY;
            entityData.set(CURIOSITY_DISPLAY, ItemStack.EMPTY);
            return true;
        }
        if (friend == null) return false;
        Vec3 drop = friend.position();
        Level returnLevel = friend.level();
        if (!returnLevel.addFreshEntity(new ItemEntity(returnLevel, drop.x, drop.y + 0.5D, drop.z, exact))) return false;
        curiosityStack = ItemStack.EMPTY;
        entityData.set(CURIOSITY_DISPLAY, ItemStack.EMPTY);
        return true;
    }

    private void clearCuriosity(boolean cooldown) {
        curiosityPhase = CuriosityPhase.NONE;
        curiosityTicks = 0;
        curiosityGroundTarget = null;
        curiosityGroundDestination = null;
        curiosityGroundPath = null;
        curiosityGroundPathStarted = false;
        curiosityExpectedItem = "";
        curiosityHintPending = false;
        curiosityHintDelay = 0;
        setSocialAction(SocialAction.NONE);
        if (cooldown) startCuriosityCooldown();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && !curiosityStack.isEmpty() && reason == Entity.RemovalReason.KILLED) {
            ServerLevel current = (ServerLevel) level();
            FadedPearlSavedData data = FadedPearlSavedData.get(current);
            if (isHealed() && friendId != null && data.ownerOf(getUUID()).isEmpty())
                data.reconcileLegacyOwner(getUUID(), friendId);
            ItemCuriosityStateMachine.OwnershipAction action = ItemCuriosityStateMachine.removalAction(
                    ItemCuriosityStateMachine.Removal.KILLED, recoveryEpoch, data.recoveryEpoch(getUUID()), true);
            if (action == ItemCuriosityStateMachine.OwnershipAction.DROP) {
                CompoundTag escrow = createCuriosityEscrow();
                if (escrow != null && data.escrowCuriosityReturn(getUUID(), escrow, recoveryEpoch)) {
                    curiosityStack = ItemStack.EMPTY;
                    entityData.set(CURIOSITY_DISPLAY, ItemStack.EMPTY);
                }
            }
        }
        super.remove(reason);
    }

    private CompoundTag createCuriosityEscrow() {
        if (curiosityStack.isEmpty() || friendId == null) return null;
        CompoundTag escrow = new CompoundTag();
        escrow.putUUID("Friend", friendId);
        escrow.putUUID("Owner", getUUID());
        escrow.putInt("RecoveryEpoch", recoveryEpoch);
        escrow.put("Stack", curiosityStack.save(new CompoundTag()));
        if (curiosityReturnPos != null && curiosityReturnDimension != null) {
            escrow.putLong("ReturnPos", curiosityReturnPos.asLong());
            escrow.putString("ReturnDimension", curiosityReturnDimension.location().toString());
        }
        return escrow;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        FadedPersistenceCodec.write(tag, new FadedPersistenceCodec.PersistentState(pearlGiven, isHealed(),
                getHealingColor(), getHeartColor(), Optional.ofNullable(friendId), getCommand().ordinal(), isDowned(),
                downedTicks, cryCooldown, isCrying(), cryingTicks, entityData.get(SEATED_YAW), hasSeatedYaw(),
                getSocialAction().ordinal(), socialActionTicks, socialActionCooldown, getCarryAction().ordinal(),
                carryActionTicks, getCarriedBlinkCooldown(), trust, crouchTimer, stareTimer, jumpCount, jumpWindowTimer,
                rainSheltered, lastInteractionTick, trustInteractionCooldown, flowerCooldown, lastFriendSeenTime,
                homePos != null && homeDimension != null
                        ? Optional.of(new FadedPersistenceCodec.Home(homePos.asLong(), homeDimension.location().toString()))
                        : Optional.empty(),
                carriedFlower.isEmpty() ? Optional.empty() : Optional.of(carriedFlower.save(new CompoundTag())),
                protectorCooldown, worldReactionCooldown, lastWorldReaction, recoveryEpoch));
        if (!curiosityStack.isEmpty()) tag.put(FadedPersistenceCodec.CURIOSITY_STACK, curiosityStack.save(new CompoundTag()));
        ListTag seen = new ListTag();
        for (String id : curiosityTrustGranted) seen.add(net.minecraft.nbt.StringTag.valueOf(id));
        tag.put(FadedPersistenceCodec.CURIOSITY_SEEN, seen);
        tag.putInt(FadedPersistenceCodec.CURIOSITY_COOLDOWN, curiosityCooldown);
        tag.putInt(FadedPersistenceCodec.ANIMAL_CARRY_COOLDOWN, animalCarryCooldown);
        if (sameAnimalCooldown > 0 && lastCarriedAnimalId != null) {
            tag.putInt(FadedPersistenceCodec.ANIMAL_SAME_COOLDOWN, sameAnimalCooldown);
            tag.putUUID(FadedPersistenceCodec.ANIMAL_LAST_ID, lastCarriedAnimalId);
        }
        if (curiosityReturnPos != null && curiosityReturnDimension != null) {
            tag.putLong(FadedPersistenceCodec.CURIOSITY_RETURN_POS, curiosityReturnPos.asLong());
            tag.putString(FadedPersistenceCodec.CURIOSITY_RETURN_DIMENSION,
                    curiosityReturnDimension.location().toString());
        }
        tag.putBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED, escapePearlArmed);
        dialogueMemory.write(tag);
        worldAwarenessMemory.write(tag);
        nameLearningMemory.write(tag);
        journalMemory.write(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        FadedPersistenceCodec.DecodedState state = FadedPersistenceCodec.read(tag, random::nextInt);
        pearlGiven = state.pearlGiven();
        setHealed(state.healed());
        state.healingColor().ifPresent(this::setHealingColor);
        friendId = state.friendId().orElse(null);
        state.command().ifPresent(value -> entityData.set(COMMAND, value));
        setDowned(state.downed());
        downedTicks = state.downedTicks();
        cryCooldown = state.cryCooldown();
        setCrying(state.crying());
        cryingTicks = state.cryingTicks();
        if (state.seatedYawSet()) setSeatedYaw(state.seatedYaw());
        if (isHealed() && (isDowned() || getCommand() == CompanionCommand.REST)) setSeatedYaw(getYRot());
        state.socialAction().ifPresent(value -> entityData.set(SOCIAL_ACTION, value));
        socialActionTicks = state.socialActionTicks();
        socialActionCooldown = state.socialActionCooldown();
        state.carryAction().ifPresent(value -> entityData.set(CARRY_ACTION, value));
        carryActionTicks = state.carryActionTicks();
        setCarriedBlinkCooldown(state.carriedBlinkCooldown());
        setTrustValue(state.trust());
        crouchTimer = state.crouchTimer();
        stareTimer = state.stareTimer();
        jumpCount = state.jumpCount();
        jumpWindowTimer = state.jumpWindowTimer();
        rainSheltered = state.rainSheltered();
        state.downedOverride().ifPresent(this::setDowned);
        lastInteractionTick = state.lastInteractionTick();
        trustInteractionCooldown = state.trustInteractionCooldown();
        flowerCooldown = state.flowerCooldown();
        lastFriendSeenTime = state.lastFriendSeenTime();
        state.home().ifPresent(home -> {
            homePos = BlockPos.of(home.pos());
            homeDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(home.dimension()));
        });
        state.carriedFlower().ifPresent(value -> carriedFlower = ItemStack.of(value));
        protectorCooldown = state.protectorCooldown();
        worldReactionCooldown = state.worldReactionCooldown();
        lastWorldReaction = state.lastWorldReaction();
        recoveryEpoch = state.recoveryEpoch();
        escapePearlArmed = tag.getBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED);
        dialogueMemory.read(tag);
        worldAwarenessMemory.read(tag);
        nameLearningMemory.read(tag);
        journalMemory.read(tag);
        nameLearningMemory.ensureRepeatCooldown(random::nextInt);
        awarenessBaselineInitialized = false;
        curiosityStack = tag.contains(FadedPersistenceCodec.CURIOSITY_STACK)
                ? ItemStack.of(tag.getCompound(FadedPersistenceCodec.CURIOSITY_STACK)) : ItemStack.EMPTY;
        entityData.set(CURIOSITY_DISPLAY, curiosityStack.copy());
        curiosityTrustGranted.clear();
        ListTag seen = tag.getList(FadedPersistenceCodec.CURIOSITY_SEEN, net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < seen.size(); i++) curiosityTrustGranted.add(seen.getString(i));
        curiosityCooldown = Math.max(0, tag.getInt(FadedPersistenceCodec.CURIOSITY_COOLDOWN));
        animalCarryCooldown = Mth.clamp(tag.getInt(FadedPersistenceCodec.ANIMAL_CARRY_COOLDOWN), 0,
                SmallAnimalCarryPolicy.GLOBAL_COOLDOWN_MIN_TICKS
                        + SmallAnimalCarryPolicy.GLOBAL_COOLDOWN_RANDOM_TICKS - 1);
        sameAnimalCooldown = tag.hasUUID(FadedPersistenceCodec.ANIMAL_LAST_ID)
                ? Mth.clamp(tag.getInt(FadedPersistenceCodec.ANIMAL_SAME_COOLDOWN), 0,
                SmallAnimalCarryPolicy.SAME_ANIMAL_COOLDOWN_TICKS) : 0;
        lastCarriedAnimalId = sameAnimalCooldown > 0
                ? tag.getUUID(FadedPersistenceCodec.ANIMAL_LAST_ID) : null;
        if (tag.contains(FadedPersistenceCodec.CURIOSITY_RETURN_POS)
                && tag.contains(FadedPersistenceCodec.CURIOSITY_RETURN_DIMENSION)) {
            curiosityReturnPos = BlockPos.of(tag.getLong(FadedPersistenceCodec.CURIOSITY_RETURN_POS));
            curiosityReturnDimension = ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(tag.getString(FadedPersistenceCodec.CURIOSITY_RETURN_DIMENSION)));
        }
        if (!curiosityStack.isEmpty()) { curiosityPhase = CuriosityPhase.RETURN_PENDING; curiosityTicks = 0; }
        journalNotificationBaseline = journalSnapshot();
    }

    public CompoundTag createRecoverySnapshot() {
        CompoundTag raw = new CompoundTag();
        addAdditionalSaveData(raw);
        CompoundTag safe = FadedPersistenceCodec.copyRecoveryKeys(raw);
        safe.putFloat(FadedPersistenceCodec.RECOVERY_HEALTH, getHealth());
        safe.putFloat(FadedPersistenceCodec.RECOVERY_ABSORPTION, getAbsorptionAmount());
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : getActiveEffects()) effects.add(effect.save(new CompoundTag()));
        safe.put(FadedPersistenceCodec.RECOVERY_EFFECTS, effects);
        return safe;
    }

    public void applyRecoverySnapshot(CompoundTag snapshot, int epoch) {
        readAdditionalSaveData(snapshot.copy());
        if (snapshot.contains(FadedPersistenceCodec.RECOVERY_HEALTH))
            setHealth(Mth.clamp(snapshot.getFloat(FadedPersistenceCodec.RECOVERY_HEALTH), 0.0F, getMaxHealth()));
        if (snapshot.contains(FadedPersistenceCodec.RECOVERY_ABSORPTION))
            setAbsorptionAmount(Math.max(0.0F, snapshot.getFloat(FadedPersistenceCodec.RECOVERY_ABSORPTION)));
        removeAllEffects();
        ListTag effects = snapshot.getList(FadedPersistenceCodec.RECOVERY_EFFECTS, 10);
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = MobEffectInstance.load(effects.getCompound(i));
            if (effect != null) addEffect(effect);
        }
        recoveryEpoch = Math.max(0, epoch);
    }

    public int getRecoveryEpoch() { return recoveryEpoch; }
    public void setRecoveryEpoch(int value) { recoveryEpoch = Math.max(0, value); }

    /** Vanilla 1.20.1 melee cadence with chase/look execution deferred to the movement adapter. */
    private final class ArbitratedMeleeAttackGoal extends Goal {
        private static final double SPEED = 1.3D;
        private LivingEntity target;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private long lastCanUseCheck;

        private ArbitratedMeleeAttackGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            long gameTime = level().getGameTime();
            if (gameTime - lastCanUseCheck < 20L) return false;
            lastCanUseCheck = gameTime;
            target = getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!isCombatAllowed(isAcceptedCombatTarget(target))) return false;
            var path = getNavigation().createPath(target, 0);
            return path != null || getAttackReachSqr(target) >= distanceToSqr(target)
                    || canCombatBlink(target);
        }

        @Override
        public boolean canContinueToUse() {
            target = getTarget();
            return target != null && target.isAlive() && isCombatAllowed(isAcceptedCombatTarget(target))
                    && isWithinRestriction(target.blockPosition())
                    && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative());
        }

        @Override
        public void start() {
            setAggressive(true);
            ticksUntilNextPathRecalculation = 0;
            ticksUntilNextAttack = 0;
        }

        @Override
        public void stop() {
            LivingEntity current = getTarget();
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(current)) {
                queueClearTarget("melee target became invalid");
            }
            setAggressive(false);
            queueStop(FadedMovementCoordinator.Locomotion.STOP, "melee ended");
            target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            target = getTarget();
            if (target == null || !isCombatAllowed(isAcceptedCombatTarget(target))) return;
            queueLookAt(target, 30.0F, 30.0F, "melee target");
            double distance = getPerceivedTargetDistanceSquareForMeleeAttack(target);
            ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);
            boolean recalculatePath = ticksUntilNextPathRecalculation <= 0
                    && (pathedTargetX == 0.0D && pathedTargetY == 0.0D && pathedTargetZ == 0.0D
                    || target.distanceToSqr(pathedTargetX, pathedTargetY, pathedTargetZ) >= 1.0D
                    || random.nextFloat() < 0.05F);
            LivingEntity chaseTarget = target;
            int recalculationDelay = -1;
            if (recalculatePath) {
                pathedTargetX = target.getX();
                pathedTargetY = target.getY();
                pathedTargetZ = target.getZ();
                ticksUntilNextPathRecalculation = 4 + random.nextInt(7);
                if (distance > 1024.0D) ticksUntilNextPathRecalculation += 10;
                else if (distance > 256.0D) ticksUntilNextPathRecalculation += 5;
                recalculationDelay = ticksUntilNextPathRecalculation;
                ticksUntilNextPathRecalculation = adjustedTickDelay(recalculationDelay);
            }
            int baselineRecalculationDelay = recalculationDelay;
            queueLocomotion(FadedMovementCoordinator.Locomotion.COMBAT_CHASE, target.getStringUUID(), SPEED,
                    "melee chase", () -> {
                        if (recalculatePath) {
                            boolean pathStarted = applyMoveTo(chaseTarget, SPEED);
                            if (!pathStarted)
                                ticksUntilNextPathRecalculation = adjustedTickDelay(baselineRecalculationDelay + 15);
                            if ((!pathStarted || distance >= 25.0D && random.nextFloat() < 0.10F)
                                    && tryCombatBlink(chaseTarget)) {
                                applyNavigationStop();
                                ticksUntilNextPathRecalculation = 0;
                            }
                        }
                    });
            ticksUntilNextAttack = Math.max(ticksUntilNextAttack - 1, 0);
            if (distance <= getAttackReachSqr(target) && ticksUntilNextAttack <= 0) {
                ticksUntilNextAttack = adjustedTickDelay(20);
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(target);
            }
        }

        private double getAttackReachSqr(LivingEntity target) {
            return getBbWidth() * 2.0F * getBbWidth() * 2.0F + target.getBbWidth();
        }
    }

    private final class FollowFriendGoal extends Goal {
        private Player friend;

        private FollowFriendGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (interactionMovementStopLatched || socialRepositionTarget != null
                    || !isHealed() || isDowned() || isVehicle()
                    || getCommand() != CompanionCommand.FOLLOW || friendId == null) return false;
            friend = getFriendPlayer();
            return friend != null && friend.isAlive() && !friend.isSpectator() && !shouldPauseFollowForWater(friend)
                    && distanceToSqr(friend) > 64.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return friend != null && friend.isAlive() && !friend.isSpectator()
                    && !interactionMovementStopLatched && socialRepositionTarget == null
                    && getCommand() == CompanionCommand.FOLLOW && !isDowned() && !isVehicle()
                    && !shouldPauseFollowForWater(friend) && distanceToSqr(friend) > 36.0D;
        }

        @Override
        public void tick() {
            if (shouldPauseFollowForWater(friend)) {
                entityData.set(RUNNING_TO_FRIEND, false);
                queueStop(FadedMovementCoordinator.Locomotion.FOLLOW_WATER_PAUSE, "friend in water");
                return;
            }
            double distance = distanceToSqr(friend);
            if (distance > 900.0D) {
                entityData.set(RUNNING_TO_FRIEND, false);
                if (canFollowTeleportTo(friend)) {
                    Player followTarget = friend;
                    queueLocomotion(FadedMovementCoordinator.Locomotion.FOLLOW_DISTANCE_TELEPORT,
                            friend.getStringUUID(), 0.0D, "follow distance teleport", () -> {
                                teleportNearGroundedFriend(followTarget, 5.5D);
                                applyNavigationStop();
                            });
                } else queueStop(FadedMovementCoordinator.Locomotion.FOLLOW_DISTANCE_STOP,
                        "follow teleport landing unavailable");
                return;
            }
            boolean runningDistance = distance > 64.0D;
            LivingEntity hostile = runningDistance ? null : getActiveHostileReactionTarget();
            if (hostile != null) queueLookAt(hostile, 35.0F, 30.0F, "follow hostile reaction");
            else queueLookAt(friend, 10.0F, getMaxHeadXRot(), "follow friend");
            Player followTarget = friend;
            double speed = runningDistance ? 1.25D : 0.85D;
            queueLocomotion(FadedMovementCoordinator.Locomotion.FOLLOW_PATH, friend.getStringUUID(), speed,
                    "follow path", () -> {
                        Path dryPath = consumeDryFollowPath(followTarget);
                        boolean pursuing = dryPath != null
                                ? applyMoveTo(dryPath, speed) : applyMoveTo(followTarget, speed);
                        entityData.set(RUNNING_TO_FRIEND,
                                runningDistance && pursuing && !getNavigation().isDone());
                    });
        }

        @Override
        public void stop() {
            entityData.set(RUNNING_TO_FRIEND, false);
            friend = null;
            queueStop(FadedMovementCoordinator.Locomotion.STOP, "follow ended");
        }
    }

    private final class EscapeFireGoal extends Goal {
        private BlockPos safeDestination;

        private EscapeFireGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (!isHealed() || isDowned() || !isOnFire()) return false;
            Optional<BlockPos> water = BlockPos.findClosestMatch(blockPosition(), 16, 6,
                    pos -> level().getBlockState(pos).is(Blocks.WATER));
            safeDestination = water.map(pos -> findNearestSafeLanding(pos, 3, 6)).orElse(null);
            return safeDestination != null;
        }

        @Override
        public void start() {
            if (safeDestination == null) return;
            BlockPos destination = safeDestination;
            queueLocomotion(FadedMovementCoordinator.Locomotion.ESCAPE_FIRE, destination.toShortString(), 1.35D,
                    "escape fire", () -> {
                        if (distanceToSqr(Vec3.atCenterOf(destination)) > 64.0D)
                            trySafeTeleport(destination.getX() + .5D, destination.getY(), destination.getZ() + .5D);
                        else applyMoveTo(destination, 0.0D, 1.35D);
                    });
        }

        @Override
        public boolean canContinueToUse() {
            return isOnFire() && safeDestination != null && !getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (safeDestination != null) queueLocomotion(FadedMovementCoordinator.Locomotion.ESCAPE_FIRE,
                    safeDestination.toShortString(), 1.35D, "continue escape fire", () -> {});
            if (safeDestination != null && distanceToSqr(Vec3.atCenterOf(safeDestination)) < 4.0D) {
                clearFire();
                addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
            }
        }

        @Override
        public void stop() { safeDestination = null; }
    }

    private void refreshHomeFromSavedData() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        FadedPearlSavedData data = FadedPearlSavedData.get(serverLevel);
        Optional<BlockPos> savedHomePos = data.homePos(getUUID());
        Optional<ResourceKey<Level>> savedHomeDimension = data.homeDimension(getUUID());
        if (savedHomePos.isPresent() && savedHomeDimension.isPresent()) {
            homePos = savedHomePos.get();
            homeDimension = savedHomeDimension.get();
        }
    }

    private final class ReturnHomeDimensionGoal extends Goal {
        private int nextAttemptTick;

        private ReturnHomeDimensionGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (interactionMovementStopLatched || !isHealed() || isDowned() || getCommand() != CompanionCommand.HOME
                    || tickCount < nextAttemptTick || !(level() instanceof ServerLevel)) return false;
            refreshHomeFromSavedData();
            return homePos != null && homeDimension != null && !level().dimension().equals(homeDimension);
        }

        @Override
        public void start() {
            nextAttemptTick = tickCount + 20;
            if (!(level() instanceof ServerLevel current) || homePos == null || homeDimension == null) return;
            ServerLevel destination = current.getServer().getLevel(homeDimension);
            if (destination == null) return;
            destination.getChunk(homePos);
            BlockPos landing = findSafeHomeLanding(destination, homePos.above(), 8, 12);
            if (landing != null) queueLocomotion(FadedMovementCoordinator.Locomotion.HOME_DIMENSION_TRANSFER,
                    destination.dimension().location().toString(), 0.0D, "home dimension transfer",
                    () -> transferHomeDimension(destination, landing));
        }

        @Override
        public boolean canContinueToUse() { return false; }
    }

    private final class ReturnHomeGoal extends Goal {
        private ReturnHomeGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (interactionMovementStopLatched || !isHealed() || isDowned()
                    || getCommand() != CompanionCommand.HOME) return false;
            refreshHomeFromSavedData();
            if (homePos == null || homeDimension == null) {
                if (level() instanceof ServerLevel serverLevel) {
                    FadedPearlSavedData data = FadedPearlSavedData.get(serverLevel);
                    homePos = data.homePos(getUUID()).orElse(null);
                    homeDimension = data.homeDimension(getUUID()).orElse(null);
                }
            }
            if (homePos == null || homeDimension == null) return false;
            if (!level().dimension().equals(homeDimension)) return false;
            return distanceToSqr(Vec3.atCenterOf(homePos)) > 1225.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return !interactionMovementStopLatched && getCommand() == CompanionCommand.HOME && homePos != null
                    && distanceToSqr(Vec3.atCenterOf(homePos)) > 1225.0D;
        }

        @Override
        public void start() {
            if (homePos == null) return;
            queueHomeReturn(homePos);
        }

        @Override
        public void tick() {
            if (homePos != null && distanceToSqr(Vec3.atCenterOf(homePos)) > 1225.0D) {
                if (distanceToSqr(Vec3.atCenterOf(homePos)) > 2500.0D) queueHomeReturn(homePos);
                else if (getNavigation().isDone()) queueHomePath(homePos);
                else queueLocomotion(FadedMovementCoordinator.Locomotion.HOME_RETURN, homePos.toShortString(), .9D,
                        "continue home path", () -> {});
            }
        }

        private void queueHomeReturn(BlockPos destination) {
            if (distanceToSqr(Vec3.atCenterOf(destination)) > 2500.0D && level() instanceof ServerLevel serverLevel) {
                BlockPos landing = findSafeHomeLanding(serverLevel, destination.above(), 8, 12);
                if (landing != null) queueLocomotion(FadedMovementCoordinator.Locomotion.HOME_RETURN,
                        landing.toShortString(), .9D, "home long return",
                        () -> trySafeTeleport(landing.getX() + .5D, landing.getY(), landing.getZ() + .5D));
                else queueHomePath(destination);
            } else queueHomePath(destination);
        }

        private void queueHomePath(BlockPos destination) {
            queueLocomotion(FadedMovementCoordinator.Locomotion.HOME_RETURN, destination.toShortString(), .9D,
                    "home path", () -> applyMoveTo(destination, 1.0D, .9D));
        }
    }

    private BlockPos findSafeHomeLanding(ServerLevel destination, BlockPos origin, int horizontalRadius, int verticalRange) {
        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                for (int dy = 2; dy >= -verticalRange; dy--) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    if (isSafeHomeLanding(destination, feet)) return feet.immutable();
                }
            }
        }
        return null;
    }

    private boolean isSafeHomeLanding(ServerLevel destination, BlockPos feet) {
        BlockPos support = feet.below();
        var supportState = destination.getBlockState(support);
        if (!supportState.isFaceSturdy(destination, support, Direction.UP)
                || !destination.getFluidState(support).isEmpty()) return false;
        if (!destination.getFluidState(feet).isEmpty() || !destination.getFluidState(feet.above()).isEmpty()
                || !destination.getFluidState(feet.above(2)).isEmpty()) return false;
        double halfWidth = getBbWidth() / 2.0D;
        AABB box = new AABB(feet.getX() + .5D - halfWidth, feet.getY(), feet.getZ() + .5D - halfWidth,
                feet.getX() + .5D + halfWidth, feet.getY() + getBbHeight(), feet.getZ() + .5D + halfWidth);
        return destination.noCollision(this, box) && !destination.containsAnyLiquid(box);
    }

    private Entity transferHomeDimension(ServerLevel destination, BlockPos landing) {
        if (getFirstPassenger() instanceof Animal animal && !releaseCarriedAnimal(animal)) return null;
        Vec3 target = Vec3.atBottomCenterOf(landing);
        return changeDimension(destination, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                            Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(target, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }

            @Override
            public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw,
                                      Function<Boolean, Entity> repositionEntity) {
                Entity moved = repositionEntity.apply(false);
                if (moved != null) moved.moveTo(target.x, target.y, target.z, yaw, entity.getXRot());
                return moved;
            }
        });
    }

    private final class WanderNearAnchorGoal extends Goal {
        private Vec3 destination;

        private WanderNearAnchorGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (interactionMovementStopLatched || !isHealed() || isDowned() || getCommand() != CompanionCommand.HOME
                    || homePos == null || homeDimension == null || !level().dimension().equals(homeDimension)
                    || distanceToSqr(Vec3.atCenterOf(homePos)) > 1225.0D || random.nextInt(80) != 0) return false;
            for (int attempt = 0; attempt < 12; attempt++) {
                Vec3 candidate = LandRandomPos.getPos(FadedEnderman.this, 10, 5);
                if (candidate == null || candidate.distanceToSqr(Vec3.atCenterOf(homePos)) > 1024.0D) continue;
                BlockPos feet = BlockPos.containing(candidate);
                if (level() instanceof ServerLevel serverLevel && isSafeHomeLanding(serverLevel, feet)) {
                    destination = Vec3.atBottomCenterOf(feet);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            if (destination != null) {
                Vec3 target = destination;
                queueLocomotion(FadedMovementCoordinator.Locomotion.HOME_WANDER, target.toString(), .72D,
                        "home wander", () -> applyMoveTo(target, .72D));
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !interactionMovementStopLatched && destination != null && !getNavigation().isDone()
                    && getCommand() == CompanionCommand.HOME
                    && homePos != null && position().distanceToSqr(Vec3.atCenterOf(homePos)) <= 1156.0D;
        }

        @Override
        public void tick() {
            if (destination != null) queueLocomotion(FadedMovementCoordinator.Locomotion.HOME_WANDER,
                    destination.toString(), .72D, "continue home wander", () -> {});
        }

        @Override
        public void stop() { destination = null; }
    }

    private final class WanderNearFriendGoal extends Goal {
        private Vec3 destination;

        private WanderNearFriendGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (interactionMovementStopLatched || !isHealed() || isDowned()
                    || getCommand() != CompanionCommand.FOLLOW || friendId == null
                    || random.nextInt(70) != 0) return false;
            Player friend = level().getPlayerByUUID(friendId);
            if (friend == null || shouldPauseFollowForWater(friend) || distanceToSqr(friend) > 64.0D) return false;
            for (int attempt = 0; attempt < 8; attempt++) {
                Vec3 candidate = LandRandomPos.getPos(FadedEnderman.this, 8, 4);
                if (candidate != null && candidate.distanceToSqr(friend.position()) <= 64.0D) {
                    destination = candidate;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            if (destination != null) {
                Vec3 target = destination;
                queueLocomotion(FadedMovementCoordinator.Locomotion.IDLE_EXPLORE, target.toString(), .72D,
                        "inactive friend wander", () -> applyMoveTo(target, .72D));
            }
        }

        @Override
        public boolean canContinueToUse() {
            Player friend = getFriendPlayer();
            return !interactionMovementStopLatched && !getNavigation().isDone()
                    && getCommand() == CompanionCommand.FOLLOW
                    && friend != null && !shouldPauseFollowForWater(friend);
        }

        @Override
        public void stop() {
            destination = null;
        }
    }
}
