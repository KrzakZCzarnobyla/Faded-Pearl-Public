package pl.fadedpearl.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.block.ResonatingAnchorBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FadedPearl.MOD_ID);

    public static final RegistryObject<Block> RESONATING_ANCHOR = BLOCKS.register("resonating_anchor",
            () -> new ResonatingAnchorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(2.5F, 8.0F).lightLevel(state -> state.getValue(ResonatingAnchorBlock.ACTIVE) ? 8 : 0).noOcclusion()));

    private ModBlocks() {}
}
