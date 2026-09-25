package pl.fadedpearl.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import pl.fadedpearl.entity.FadedEnderman;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.network.chat.Component;

public final class FadedEndermanGeoRenderer extends GeoEntityRenderer<FadedEnderman> {
    private static final double NAME_TAG_EXTRA_HEIGHT = 0.65D;

    public FadedEndermanGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new FadedEndermanGeoModel());
        this.shadowRadius = 0.45F;
        addRenderLayer(new BlockAndItemGeoLayer<FadedEnderman>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, FadedEnderman enderman) {
                ItemStack stack = enderman.getCuriosityDisplayStack();
                return "right_hand".equals(bone.getName()) && !stack.isEmpty() ? stack : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                                   FadedEnderman enderman) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                              FadedEnderman enderman, MultiBufferSource buffers,
                                              float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();
                poseStack.translate(0.0D, -0.22D, -0.03D);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(0.65F, 0.65F, 0.65F);
                super.renderStackForBone(poseStack, bone, stack, enderman, buffers,
                        partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FadedEnderman enderman, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green,
                                  float blue, float alpha) {
        if (isFlowerColoredBone(bone)) {
            packedLight = 0xF000F0;
            if (enderman.isHealed()) {
                int color = enderman.getHealingColor();
                red = FadedTrustVisuals.colorChannel((color >> 16) & 255, enderman.getTrust());
                green = FadedTrustVisuals.colorChannel((color >> 8) & 255, enderman.getTrust());
                blue = FadedTrustVisuals.colorChannel(color & 255, enderman.getTrust());
            }
            else {
                red = green = blue = 1.0F;
            }
        }
        else {
            red = green = blue = 1.0F;
        }

        super.renderRecursively(poseStack, enderman, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static boolean isFlowerColoredBone(GeoBone bone) {
        return "pearl".equals(bone.getName()) || "eyes".equals(bone.getName());
    }

    @Override
    public boolean shouldShowName(FadedEnderman enderman) {
        return enderman.isDowned() || super.shouldShowName(enderman);
    }

    @Override
    protected void renderNameTag(FadedEnderman enderman, Component name, PoseStack poseStack,
                                 MultiBufferSource buffers, int packedLight) {
        if (enderman.isDowned()) {
            Component status = Component.translatable("status.faded_pearl.downed",
                    enderman.getDownedSeconds());
            super.renderNameTag(enderman, status, poseStack, buffers, packedLight);
        } else {
            poseStack.pushPose();
            poseStack.translate(0.0D, NAME_TAG_EXTRA_HEIGHT, 0.0D);
            super.renderNameTag(enderman, name, poseStack, buffers, packedLight);
            poseStack.popPose();
        }
    }

}
