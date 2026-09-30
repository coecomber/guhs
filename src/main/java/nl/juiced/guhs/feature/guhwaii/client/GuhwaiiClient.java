package nl.juiced.guhs.feature.guhwaii.client;

import java.util.Optional;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.guhwaii.GuhwaiiFeature;
import nl.juiced.guhs.feature.guhwaii.GuhwaiiPayloads;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Client side of Guhwai'i: the looks of Lilo-guh (a red dress with white leaves, long black hair, a pink hibiscus behind her
 * ear) and Nani-guh (a flowery top, her hair in a bun with a plumeria), the 626-guh's moves (little extra arms that wiggle,
 * his big ears, tilted up a wall, upside down under a ceiling, and his ukelele while he strums), and the scanner's screen.
 */
public final class GuhwaiiClient {
    private static ItemStack ukelele;

    public static void init(IEventBus modBus) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.LILO_GUH, Guhs.id("geo/entity/guh_npc_lilo_guh.geo.json"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.NANI_GUH, Guhs.id("geo/entity/guh_npc_nani_guh.geo.json"));
        // Lilo's hair and her skirt sway a little in the sea breeze; Nani's flower nods
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.LILO_GUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.07f;
            bot.apply("lilo_haar").ifPresent(b -> b.setRotX((float) Math.sin(t) * 0.05f));
            bot.apply("lilo_rokje").ifPresent(b -> b.setRotZ((float) Math.sin(t * 1.3f) * 0.04f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.NANI_GUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.05f;
            bot.apply("nani_bloem").ifPresent(b -> b.setRotZ((float) Math.sin(t) * 0.06f));
        });
        VariantUiterlijk.zet(GuhVariant.STITCH626, new Uiterlijk626());
    }

    /** (GuhwaiiPayloads.Scan) open the scanner's big screen. */
    public static void scan(GuhwaiiPayloads.Scan scan) {
        Minecraft.getInstance().setScreen(new ScannerScherm(scan));
    }

    /** The 626-guh's own moves. */
    static class Uiterlijk626 implements VariantUiterlijk.Uiterlijk {
        @Override
        public void botten(GuhEntity guh, Function<String, Optional<GeoBone>> bot, float pt) {
            float t = guh.tickCount + pt;
            boolean uke = guh.emotes.current() == Emote.UKELELE;
            if (!uke) {
                // the extra arms: a lazy little wiggle (the ukelele emote animates them itself)
                bot.apply("stitch_arm_links").ifPresent(b -> b.setRotX((float) Math.sin(t * 0.15f) * 0.25f));
                bot.apply("stitch_arm_rechts").ifPresent(b -> b.setRotX((float) Math.sin(t * 0.15f + 2f) * 0.25f));
            }
            if (!GuhHooks.heeft(guh, VerhaalVlaggen.KLIMT)) {
                return;
            }
            bot.apply("root").ifPresent(root -> {
                if (plafond(guh)) {
                    // upside down under the ceiling: flipped, feet against it
                    root.setRotZ(Mth.PI);
                    root.setPosY(guh.getBbHeight() / Math.max(0.1f, guh.getScale()) * 16f);
                } else {
                    // up the wall: nose up, belly against it
                    root.setRotX(1.25f);
                    root.setPosY(4f);
                    root.setPosZ(-3f);
                }
            });
        }

        /** Is there a ceiling right above his head (else he's on a wall)? */
        private static boolean plafond(GuhEntity guh) {
            Level level = guh.level();
            BlockPos boven = BlockPos.containing(guh.getX(), guh.getBoundingBox().maxY + 0.1, guh.getZ());
            return !level.getBlockState(boven).getCollisionShape(level, boven).isEmpty();
        }

        @Override
        public void extra(GuhEntity guh, PoseStack pose, MultiBufferSource buffers, int light, float pt) {
            if (guh.emotes.current() != Emote.UKELELE || guh.isInvisible()) {
                return;
            }
            if (ukelele == null) {
                ukelele = new ItemStack(GuhwaiiFeature.UKELELE.get());
            }
            float s = guh.getScale();
            float t = guh.tickCount + pt;
            float yaw = Mth.rotLerp(pt, guh.yBodyRotO, guh.yBodyRot);
            pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            pose.scale(s, s, s);
            // held in front of the belly, neck to the right and a little up, strummed
            pose.translate(0.05, 0.36, -0.62);
            pose.mulPose(Axis.ZP.rotationDegrees(-28f + (float) Math.sin(t * 0.9f) * 3f));
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.scale(0.62f, 0.62f, 0.62f);
            Minecraft.getInstance().getItemRenderer().renderStatic(ukelele, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose,
                    buffers, guh.level(), guh.getId());
        }
    }

    private GuhwaiiClient() {
    }
}
