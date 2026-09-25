package pl.fadedpearl.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.entity.FadedEnderman;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FadedPearl.MOD_ID);

    public static final RegistryObject<EntityType<FadedEnderman>> FADED_ENDERMAN = ENTITIES.register("faded_enderman",
            () -> EntityType.Builder.of(FadedEnderman::new, MobCategory.CREATURE)
                    .sized(0.6F, 2.9F).clientTrackingRange(8).build("faded_enderman"));

    private ModEntities() {}
}
