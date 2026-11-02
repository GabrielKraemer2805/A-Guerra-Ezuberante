package io.github.omarflk.guerraezuberante.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 0.029: the Foice e Martelo is one item, drawn as a sickle in the hand that holds it and a hammer in the other, as long
 * as that other hand is free. The hammer exists only as a model: nothing is added to the inventory.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class HeldItemsClient {
    private static final ResourceLocation HAMMER = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "item/mao/martelo");

    private static boolean carriesHammer(LivingEntity entity) {
        return entity.getMainHandItem().is(ModItems.FOICE_E_MARTELO.get()) && entity.getOffhandItem().isEmpty();
    }

    private static BakedModel hammer() { return Minecraft.getInstance().getModelManager().getModel(HAMMER); }

    /** First person: vanilla draws nothing for an empty off hand, so the hammer goes where that hand would be. */
    @SubscribeEvent
    public static void firstPerson(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (event.getHand() != InteractionHand.OFF_HAND || player == null || !carriesHammer(player)) { return; }
        boolean left = player.getMainArm() == HumanoidArm.RIGHT;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate((left ? -1 : 1) * .56F, -.52F + event.getEquipProgress() * -.6F, -.72F);
        Minecraft.getInstance().getItemRenderer().render(player.getMainHandItem(),
                left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, left, pose,
                event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, hammer());
        pose.popPose();
    }

    /** Third person: the arm with the hammer is raised like one holding an item. */
    @SubscribeEvent
    public static void armPose(RenderPlayerEvent.Pre event) {
        if (!carriesHammer(event.getEntity())) { return; }
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        if (event.getEntity().getMainArm() == HumanoidArm.RIGHT) {
            if (model.leftArmPose == HumanoidModel.ArmPose.EMPTY) { model.leftArmPose = HumanoidModel.ArmPose.ITEM; }
        } else if (model.rightArmPose == HumanoidModel.ArmPose.EMPTY) {
            model.rightArmPose = HumanoidModel.ArmPose.ITEM;
        }
    }

    /** Same placement as the vanilla layer for items in hand. */
    private static final class HammerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        HammerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbAmount,
                float partial, float age, float headYaw, float headPitch) {
            if (!carriesHammer(player)) { return; }
            HumanoidArm arm = player.getMainArm().getOpposite();
            boolean left = arm == HumanoidArm.LEFT;
            pose.pushPose();
            getParentModel().translateToHand(arm, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate((left ? -1 : 1) / 16F, .125F, -.625F);
            Minecraft.getInstance().getItemRenderer().render(player.getMainHandItem(),
                    left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, pose,
                    buffers, light, OverlayTexture.NO_OVERLAY, hammer());
            pose.popPose();
        }
    }

    @Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void models(ModelEvent.RegisterAdditional event) { event.register(HAMMER); }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) { renderer.addLayer(new HammerLayer(renderer)); }
            }
        }
    }

    private HeldItemsClient() {}
}
