package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.faction.CastPose;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arm poses of players using a power ({@link CastPose}). The renderer sets the arm poses before posting
 * {@link RenderPlayerEvent.Pre}, so the custom Forge arm pose set here is the one the model animates with.
 * Only the arms move, blended in and out over a few ticks from where the walk animation put them; first person
 * keeps the vanilla hands. The arm pose is created at client setup: vanilla switches over the arm poses build their
 * lookup tables on first use, so a pose added after the first player render would fall outside them.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class PlayerPoseClient {
    private record Active(CastPose pose, long start, int ticks) {}

    private static final Map<Integer, Active> ACTIVE = new ConcurrentHashMap<>();
    private static HumanoidModel.ArmPose armPose;

    /** Mod bus: creates the arm pose before any player model is drawn. */
    @Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> armPose = HumanoidModel.ArmPose.create("GUERRA_EZUBERANTE_CAST", false, PlayerPoseClient::transform));
        }

        private Setup() {}
    }

    public static void start(FactionNetwork.CastPoseSync packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) { return; }
        CastPose.byOrdinal(packet.pose()).ifPresent(pose ->
                ACTIVE.put(packet.entity(), new Active(pose, level.getGameTime(), Math.max(1, packet.ticks()))));
    }

    /** Pose currently shown for the entity, for checks; empty when none. */
    public static java.util.Optional<CastPose> current(int entityId) {
        var active = ACTIVE.get(entityId);
        return active == null ? java.util.Optional.empty() : java.util.Optional.of(active.pose());
    }

    private static float age(Active active) {
        var minecraft = Minecraft.getInstance();
        return minecraft.level == null ? Float.MAX_VALUE : minecraft.level.getGameTime() - active.start() + minecraft.getFrameTime();
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        Active active = ACTIVE.get(event.getEntity().getId());
        if (active == null || armPose == null) { return; }
        if (age(active) >= active.ticks() || !event.getEntity().isAlive()) {
            ACTIVE.remove(event.getEntity().getId());
            return;
        }
        var model = event.getRenderer().getModel();
        model.rightArmPose = armPose;
        model.leftArmPose = armPose;
    }

    /** Bends one arm towards the pose's target, weighted by a smooth fade in and out. */
    private static void transform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        Active active = ACTIVE.get(entity.getId());
        if (active == null) { return; }
        float age = age(active), length = active.ticks();
        float t = Mth.clamp(age / length, 0, 1);
        float ramp = Math.min(4, length * .2F);
        float weight = Mth.clamp(Math.min(age / ramp, (length - age) / ramp), 0, 1);
        weight = weight * weight * (3 - 2 * weight);
        boolean right = arm == HumanoidArm.RIGHT;
        float side = right ? 1 : -1;
        ModelPart part = right ? model.rightArm : model.leftArm;
        float headX = model.head.xRot, headY = model.head.yRot;
        float[] target = switch (active.pose()) {
            case MANIFESTANDO -> new float[]{-2.7F + .35F * Mth.sin(age * .9F + (right ? 0 : 1.6F)), 0, side * .35F};
            case VIRAR_O_VOTO -> right ? new float[]{headX - 1.57F, headY - .7F * Mth.sin(Mth.PI * t), 0} : null;
            case VOTO_DE_CONSCIENCIA -> right ? new float[]{-3.0F, 0, .1F} : new float[]{-1.0F, .6F, 0};
            // Hands low over the ground grabbing the roots, then pulled up over the head.
            case RAIZES -> t < .45F ? new float[]{-.6F, 0, side * .2F}
                    : new float[]{Mth.lerp(Math.min(1, (t - .45F) / .25F), -.6F, -2.9F), 0, side * .55F};
            case SUMICO -> new float[]{-1.75F, right ? -.8F : .8F, 0};
            case PAVIO_CURTO -> new float[]{-.4F, 0, side * (1.3F + .12F * Mth.sin(age * 2.5F))};
            case PARIS -> right ? new float[]{-2.7F, 0, .3F + .45F * Mth.sin(age * 1.2F)} : null;
            case LIVE -> right ? new float[]{-1.3F + headX * .5F, headY - .45F, 0}
                    : new float[]{-.7F - .35F * (Mth.sin(age * .45F) + 1), headY + .3F, 0};
            case INVERNO -> new float[]{headX - 1.57F, headY + (right ? -.12F : .12F), 0};
            case COMITE -> right ? new float[]{-3.05F, 0, .05F} : null;
            case FIM_DA_ESCALA -> new float[]{-2.85F, 0, side * .22F};
            case ESTOCAR_VENTO -> new float[]{headX - 1.5F + .25F * Mth.sin(age * .8F * side), headY,
                    side * .25F * (Mth.cos(age * .8F) - 1)};
            case TAPAS -> right ? new float[]{-1.5F, .9F * Mth.sin(age * .95F), .2F} : null;
            case OCUPACAO -> new float[]{-.35F, 0, side * .75F};
            case ARREMESSO -> right ? new float[]{t < .4F ? -2.9F : -1.1F + headX, headY, .1F} : null;
            case LIQUIDACAO -> new float[]{-2.55F + .2F * Mth.sin(age * .8F + (right ? 0 : 1.6F)), 0, side * .6F};
            case LACO -> right ? t < .6F ? new float[]{-2.9F, 0, .3F + .35F * Mth.sin(age * 1.6F)} : new float[]{headX - 1.4F, headY, 0} : null;
            case BOIADA -> right ? new float[]{headX - 1.57F - .3F * Mth.sin(Mth.PI * t), headY, 0} : new float[]{-2.7F, .5F, -.3F};
        };
        if (active.pose() == CastPose.FIM_DA_ESCALA && right) { stamp(model.rightLeg, age, weight); }
        if (target == null) { return; }
        part.xRot = Mth.lerp(weight, part.xRot, target[0]);
        part.yRot = Mth.lerp(weight, part.yRot, target[1]);
        part.zRot = Mth.lerp(weight, part.zRot, target[2]);
    }

    /** Fim da Escala: the right leg rises with the knee forward and comes down hard on the stomp tick. */
    private static void stamp(ModelPart leg, float age, float weight) {
        float stomp = io.github.omarflk.guerraezuberante.faction.ConvictionPowers.STOMP_TICK;
        float lift = age < stomp - 1 ? Mth.clamp(age / (stomp - 3), 0, 1) : Mth.clamp(stomp - age, 0, 1);
        leg.xRot = Mth.lerp(weight * lift, leg.xRot, -1.35F);
    }

    /** Fim da Escala: the "6x1" sign between the raised hands, readable from the front and from the back. */
    @SubscribeEvent
    public static void renderSign(RenderPlayerEvent.Post event) {
        Active active = ACTIVE.get(event.getEntity().getId());
        if (active == null || active.pose() != CastPose.FIM_DA_ESCALA || age(active) >= active.ticks()) { return; }
        float age = age(active), ramp = 3;
        if (age < ramp) { return; }
        var player = event.getEntity();
        float yaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        var font = Minecraft.getInstance().font;
        var text = net.minecraft.network.chat.Component.literal("6x1").withStyle(net.minecraft.ChatFormatting.BOLD);
        float width = font.width(text);
        var pose = event.getPoseStack();
        for (int face = 0; face < 2; face++) {
            pose.pushPose();
            pose.translate(0, player.getBbHeight() + .55, 0);
            // Like a name tag, but turned with the body instead of the camera: the front faces where the player looks.
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-(yaw + 180) + face * 180));
            pose.scale(-.045F, -.045F, .045F);
            font.drawInBatch(text, -width / 2, -4, 0xFFFFFFFF, false, pose.last().pose(), event.getMultiBufferSource(),
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0xFF8B5A2B, event.getPackedLight());
            pose.popPose();
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    private PlayerPoseClient() {}
}
