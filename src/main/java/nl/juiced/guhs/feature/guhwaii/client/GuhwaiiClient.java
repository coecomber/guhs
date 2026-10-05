package nl.juiced.guhs.feature.guhwaii.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
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

/**
 * Client side of Guhwai'i: the looks of Lilo-guh (a red dress with white leaves, long black hair, a pink hibiscus behind her
 * ear) and Nani-guh (a flowery top, her hair in a bun with a plumeria), the 626-guh's moves (little extra arms that wiggle,
 * his big ears, tilted up a wall, upside down under a ceiling, and his ukelele while he strums), and the scanner's screen.
 */
public final class GuhwaiiClient {
    private static ItemStack ukelele;

    public static void init(IEventBus modBus) {
        // the palm sign: its wood type's sprite (guhs:entity/signs/guhwaii_palm) joins the sign sheet's table
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(
                () -> net.minecraft.client.renderer.Sheets.addWoodType(GuhwaiiFeature.PALM_WOOD)));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.LILO_GUH, Guhs.id("entity/guh_npc_lilo_guh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.NANI_GUH, Guhs.id("entity/guh_npc_nani_guh"));
        // Lilo's hair and her skirt sway a little in the sea breeze; Nani's flower nods
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.LILO_GUH, (npc, tick) -> {
            float t = (float) tick * 0.07f;
            return bones -> {
                bones.ifPresent("lilo_haar", b -> b.setRotX((float) Math.sin(t) * 0.05f));
                bones.ifPresent("lilo_rokje", b -> b.setRotZ((float) Math.sin(t * 1.3f) * 0.04f));
            };
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.NANI_GUH, (npc, tick) -> {
            float t = (float) tick * 0.05f;
            return bones -> bones.ifPresent("nani_bloem", b -> b.setRotZ((float) Math.sin(t) * 0.06f));
        });
        VariantUiterlijk.zet(GuhVariant.STITCH626, new Uiterlijk626());
    }

    /** (GuhwaiiPayloads.Scan) open the scanner's big screen. */
    public static void scan(GuhwaiiPayloads.Scan scan) {
        Minecraft.getInstance().setScreen(new ScannerScherm(scan));
    }

    /** The 626-guh's own moves (1.1.0: values computed at extract time, bone moves / the ukelele handed to the frame). */
    static class Uiterlijk626 implements VariantUiterlijk.Uiterlijk {
        @Override
        public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
            float t = guh.tickCount + pt;
            boolean uke = guh.emotes.current() == Emote.UKELELE;
            if (!uke) {
                // the extra arms: a lazy little wiggle (the ukelele emote animates them itself)
                frame.bones(bones -> {
                    bones.ifPresent("stitch_arm_links", b -> b.setRotX((float) Math.sin(t * 0.15f) * 0.25f));
                    bones.ifPresent("stitch_arm_rechts", b -> b.setRotX((float) Math.sin(t * 0.15f + 2f) * 0.25f));
                });
            }
            if (!GuhHooks.heeft(guh, VerhaalVlaggen.KLIMT)) {
                return;
            }
            if (plafond(guh)) {
                // upside down under the ceiling: flipped, feet against it
                float hoog = guh.getBbHeight() / Math.max(0.1f, guh.getScale()) * 16f;
                frame.bones(bones -> bones.ifPresent("root", root -> {
                    root.setRotZ(Mth.PI);
                    root.setTranslateY(hoog);
                }));
            } else {
                // up the wall: nose up, belly against it
                frame.bones(bones -> bones.ifPresent("root", root -> {
                    root.setRotX(1.25f);
                    root.setTranslateY(4f);
                    root.setTranslateZ(-3f);
                }));
            }
        }

        /** Is there a ceiling right above his head (else he's on a wall)? */
        private static boolean plafond(GuhEntity guh) {
            Level level = guh.level();
            BlockPos boven = BlockPos.containing(guh.getX(), guh.getBoundingBox().maxY + 0.1, guh.getZ());
            return !level.getBlockState(boven).getCollisionShape(level, boven).isEmpty();
        }

        @Override
        public void extra(GuhEntity guh, GuhRenderFrame frame, float pt) {
            if (guh.emotes.current() != Emote.UKELELE || guh.isInvisible()) {
                return;
            }
            if (ukelele == null) {
                ukelele = new ItemStack(GuhwaiiFeature.UKELELE.get());
            }
            float s = guh.getScale();
            float t = guh.tickCount + pt;
            float yaw = Mth.rotLerp(pt, guh.yBodyRotO, guh.yBodyRot);
            ItemStackRenderState item = GuhRenderer.itemState(ukelele, ItemDisplayContext.FIXED, guh);
            frame.extra((pose, collector, light) -> {
                pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
                pose.scale(s, s, s);
                // held in front of the belly, neck to the right and a little up, strummed
                pose.translate(0.05, 0.36, -0.62);
                pose.mulPose(Axis.ZP.rotationDegrees(-28f + (float) Math.sin(t * 0.9f) * 3f));
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                pose.scale(0.62f, 0.62f, 0.62f);
                item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            });
        }
    }

    private GuhwaiiClient() {
    }
}
