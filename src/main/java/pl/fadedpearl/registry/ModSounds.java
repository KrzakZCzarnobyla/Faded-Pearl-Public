package pl.fadedpearl.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.fadedpearl.FadedPearl;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FadedPearl.MOD_ID);

    public static final RegistryObject<SoundEvent> FADED_ENDERMAN_CRY = SOUNDS.register("faded_enderman_cry",
            () -> SoundEvent.createVariableRangeEvent(FadedPearl.id("faded_enderman_cry")));
    public static final RegistryObject<SoundEvent> FADED_ENDERMAN_WOUNDED_NOTICE =
            register("faded_enderman_wounded_notice");
    public static final RegistryObject<SoundEvent> FADED_ENDERMAN_CAVE_CRY =
            register("faded_enderman_cave_cry");
    public static final RegistryObject<SoundEvent> FADED_ENDERMAN_AMBIENT_1 =
            register("faded_enderman_ambient_1");
    public static final RegistryObject<SoundEvent> FADED_ENDERMAN_AMBIENT_2 =
            register("faded_enderman_ambient_2");
    public static final RegistryObject<SoundEvent> FADED_ENDERMAN_AMBIENT_3 =
            register("faded_enderman_ambient_3");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(FadedPearl.id(name)));
    }

    private ModSounds() {}
}
