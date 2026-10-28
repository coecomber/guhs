package nl.juiced.guhs.feature.fossiel.client;

import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.fossiel.FossielFeature;

/**
 * Client side of the oerguhs: the Fossiel-broedmachine's turning egg ({@link BroedmachineRenderer}), the Oer-stokstaartje-guh
 * standing upright like a meerkat while it stands still (it drops on four paws to walk), and the Pteroguh's wings that
 * flap while it moves.
 */
public final class FossielClient {
    /** How far the Oer-stokstaartje-guh tips up (degrees) and where it turns: around its back paws (blocks, model space). */
    public static final float RECHTOP = 68f, ACHTER = 0.45f, OP = 0.1f;

    private FossielClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(FossielFeature.BROEDMACHINE_BE.get(), BroedmachineRenderer::new));
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (frame.variant == GuhVariant.STOKSTAARTJEGUH && !frame.sleeping && !guh.isPassenger() && !guh.isInSittingPose()) {
                double v = guh.getDeltaMovement().horizontalDistanceSqr();
                if (v < 0.0004) {
                    float t = guh.tickCount + partialTick;
                    float wieg = Mth.sin(t * 0.07f) * 3f;
                    frame.pose(pose -> {   // up on the back paws, about their bottom
                        pose.translate(0f, OP, ACHTER);
                        pose.mulPose(Axis.XP.rotationDegrees(RECHTOP + wieg));
                        pose.translate(0f, -OP, -ACHTER);
                    });
                    frame.bones(bones -> {   // the front paws tucked in, the head level again: a watchful look
                        bones.ifPresent("leg_front_left", b -> b.setRotX(b.getRotX() - 1.2f));
                        bones.ifPresent("leg_front_right", b -> b.setRotX(b.getRotX() - 1.2f));
                        bones.ifPresent("head", b -> b.setRotX(b.getRotX() - (float) Math.toRadians(RECHTOP) * 0.85f));
                    });
                }
            }
            if (frame.variant == GuhVariant.PTEROGUH) {
                float t = guh.tickCount + partialTick;
                boolean vliegt = !guh.onGround() || guh.getDeltaMovement().horizontalDistanceSqr() > 0.0004;
                float slag = vliegt ? Mth.sin(t * 0.6f) * 0.55f : Mth.sin(t * 0.05f) * 0.06f;
                frame.bones(bones -> {
                    bones.ifPresent("oer_ptero_vleugel_links", b -> b.setRotZ(b.getRotZ() + slag));
                    bones.ifPresent("oer_ptero_vleugel_rechts", b -> b.setRotZ(b.getRotZ() - slag));
                });
            }
        });
    }
}
