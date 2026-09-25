package pl.fadedpearl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.world.FadedPearlSavedData;
import pl.fadedpearl.world.AnchorRecallService;

import java.util.UUID;

public final class ResonatingAnchorBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ResonatingAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player) {
            FadedPearlSavedData data = FadedPearlSavedData.get(server);
            UUID id = AnchorRecallService.companionIdFor(player, data).orElse(null);
            boolean active = id != null && data.isHealed(id) && !data.isDead(id);
            level.setBlock(pos, state.setValue(ACTIVE, active), 3);
            if (active) {
                data.setHome(id, server.dimension(), pos);
                FadedEnderman companion = findCompanion(server, data, id);
                if (companion != null && companion.isFriend(player)) companion.setHome(server.dimension(), pos);
                player.sendSystemMessage(Component.translatable("message.faded_pearl.anchor.home_set", pos.getX(), pos.getY(), pos.getZ()));
            } else player.sendSystemMessage(Component.translatable("message.faded_pearl.anchor.silent"));
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        AnchorRecallService.request(server, pos, (ServerPlayer) player);
        return InteractionResult.CONSUME;
    }

    private static FadedEnderman findCompanion(ServerLevel from, FadedPearlSavedData data, UUID id) {
        for (ServerLevel level : from.getServer().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof FadedEnderman companion) return companion;
        }
        if (data.lastDimension(id).isPresent() && data.lastPos(id).isPresent()) {
            ServerLevel level = from.getServer().getLevel(data.lastDimension(id).get());
            if (level != null) {
                level.getChunk(data.lastPos(id).get());
                Entity entity = level.getEntity(id);
                if (entity instanceof FadedEnderman companion) return companion;
            }
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(2) == 0) level.addParticle(ParticleTypes.PORTAL,
                pos.getX() + .5D + (random.nextDouble() - .5D) * .45D,
                pos.getY() + .25D + random.nextDouble() * .8D,
                pos.getZ() + .5D + (random.nextDouble() - .5D) * .45D, 0, .015D, 0);
    }
}
