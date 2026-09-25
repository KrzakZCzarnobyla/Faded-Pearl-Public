package pl.fadedpearl.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.entity.trust.FadedTrustManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public final class FadedEndermanGeoModel extends GeoModel<FadedEnderman> {
    private static final ResourceLocation MODEL = FadedPearl.id("geo/faded_enderman.geo.json");
    private static final ResourceLocation ANIMATIONS = FadedPearl.id("animations/faded_enderman.animation.json");
    private static final ResourceLocation WOUNDED = FadedPearl.id("textures/entity/faded_enderman_final_wounded.png");
    private static final ResourceLocation[] HEALED_TRUST = {
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_0.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_1.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_2.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_3.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_4.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_5.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_6.png"),
            FadedPearl.id("textures/entity/faded_enderman_final_healed_trust_7.png")
    };

    @Override
    public ResourceLocation getModelResource(FadedEnderman entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FadedEnderman entity) {
        return entity.isHealed() ? HEALED_TRUST[FadedTrustManager.stageIndex(entity.getTrust())] : WOUNDED;
    }

    @Override
    public ResourceLocation getAnimationResource(FadedEnderman entity) {
        return ANIMATIONS;
    }

    @Override
    public void handleAnimations(FadedEnderman entity, long instanceId, AnimationState<FadedEnderman> state) {
        super.handleAnimations(entity, instanceId, state);

        if (entity.getFirstPassenger() instanceof Animal) {
            float loweredArms = (float)Math.toRadians(-30.0D);
            getBone("left_arm").ifPresent(arm -> arm.setRotX(arm.getRotX() + loweredArms));
            getBone("right_arm").ifPresent(arm -> arm.setRotX(arm.getRotX() + loweredArms));
        }

        EntityModelData modelData = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        getBone("head_tracking").ifPresent(headTracking -> {
            float trackedYaw = net.minecraft.util.Mth.clamp(modelData.netHeadYaw(), -60.0F, 60.0F);
            float trackedPitch = net.minecraft.util.Mth.clamp(modelData.headPitch(), -35.0F, 35.0F);
            headTracking.setRotY((float)Math.toRadians(trackedYaw));
            headTracking.setRotX((float)Math.toRadians(trackedPitch));
        });
    }
}
