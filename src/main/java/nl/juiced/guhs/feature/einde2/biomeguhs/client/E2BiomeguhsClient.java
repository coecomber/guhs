package nl.juiced.guhs.feature.einde2.biomeguhs.client;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.math.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.biomeguhs.Korstbal;
import nl.juiced.guhs.feature.einde2.biomeguhs.Kruimelzweef;
import nl.juiced.guhs.feature.einde2.biomeguhs.Trucs;
import nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk;

/**
 * einde2, slice biomeguhs, client side: how the six biome guhs look while they do their trick. Everything hangs on
 * {@link VariantUiterlijk} (the GuhRenderer is never edited) and reads the synced flags only.
 * <ul>
 *   <li>Korstguh: rolled up it is a ball of crust (paws, ears and tail tucked away, the shield wrapped round) that turns
 *       by the distance it rolls;</li>
 *   <li>Duinguh: dug in, the model sinks until only its tall ears stick out, and they wiggle;</li>
 *   <li>Kruimelguh: floating, it spreads its paws and flaps its little cloud wings;</li>
 *   <li>Schimmelguh: its mushrooms always glow softly; as a lamp its blue veins light up too;</li>
 *   <li>Kristalguh: its kaaskristallen glow, the outer ones lean outwards, now and then a sparkle;</li>
 *   <li>Koorguh: singing, it throws its head back and sways.</li>
 * </ul>
 * Glow masks: tools/features/einde2_biomeguhs_tex.py ({@code guh_<id>_gloed.png}, {@code guh_schimmelguh_lamp.png}).
 */
public final class E2BiomeguhsClient {
    private static final Identifier SCHIMMEL_GLOED = Guhs.id("textures/entity/guh_schimmelguh_gloed.png");
    private static final Identifier SCHIMMEL_LAMP = Guhs.id("textures/entity/guh_schimmelguh_lamp.png");
    private static final Identifier KRISTAL_GLOED = Guhs.id("textures/entity/guh_kristalguh_gloed.png");
    private static final String[] POTEN = {"leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"};

    /** Per guh {value, the time it was last asked}: a trick eases in and out instead of snapping. */
    private static final Map<GuhEntity, float[]> ZACHT = Collections.synchronizedMap(new WeakHashMap<>());

    public static void init(IEventBus modBus) {
        VariantUiterlijk.zet(GuhVariant.KORSTGUH, new Korst());
        VariantUiterlijk.zet(GuhVariant.DUINGUH, new Duin());
        VariantUiterlijk.zet(GuhVariant.KRUIMELGUH, new Kruimel());
        VariantUiterlijk.zet(GuhVariant.SCHIMMELGUH, new Schimmel());
        VariantUiterlijk.zet(GuhVariant.KRISTALGUH, new Kristal());
        VariantUiterlijk.zet(GuhVariant.KOORGUH, new Koor());
        NeoForge.EVENT_BUS.addListener(E2BiomeguhsClient::sprankels);
    }

    /** 0..1, moving towards 1 while {@code aan} and back to 0 otherwise, {@code stap} per tick. */
    private static float zacht(GuhEntity guh, float partialTick, boolean aan, float stap) {
        float nu = guh.tickCount + partialTick;
        float[] z = ZACHT.computeIfAbsent(guh, g -> new float[] {aan ? 1f : 0f, nu});
        float dt = Mth.clamp(nu - z[1], 0f, 20f);
        z[0] = Mth.approach(z[0], aan ? 1f : 0f, stap * dt);
        z[1] = nu;
        return z[0];
    }

    private static float maat(GuhEntity guh) {
        return guh.getScale() * guh.getAgeScale();
    }

    // -----------------------------------------------------------------------------------------------------------------
    private static final class Korst implements VariantUiterlijk.Uiterlijk {
        @Override
        public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
            float k = zacht(guh, pt, Korstbal.opgerold(guh), 0.2f);
            if (k <= 0.01f) {
                return;
            }
            float hoek = Korstbal.rolHoek(guh, pt), midden = 0.42f * maat(guh);
            frame.pose(pose -> {   // the ball turns about its middle (forwards: the top goes the way it faces)
                pose.translate(0f, midden, 0f);
                pose.mulPose(Axis.XP.rotation(-hoek));
                pose.translate(0f, -midden, 0f);
            });
            boolean dicht = k > 0.6f;
            frame.bones(bones -> {
                for (String poot : POTEN) {
                    bones.ifPresent(poot, b -> b.setScale(1f - k, 1f - k, 1f - k).skipRender(dicht));
                }
                bones.ifPresent("tail", b -> b.setScale(1f - k, 1f - k, 1f - k).skipRender(dicht).skipChildrenRender(dicht));
                bones.ifPresent("ear_left", b -> b.setScale(1f - 0.8f * k, 1f - 0.8f * k, 1f - 0.8f * k));
                bones.ifPresent("ear_right", b -> b.setScale(1f - 0.8f * k, 1f - 0.8f * k, 1f - 0.8f * k));
                // the head tucks in under the shield, nose down
                bones.ifPresent("head", b -> b.setRotation(0.6f * k, 0f, 0f).setTranslation(0f, -1.2f * k, 4.6f * k).setScale(1f - 0.16f * k, 1f - 0.16f * k, 1f - 0.16f * k));
                bones.ifPresent("body", b -> b.setScale(1f, 1f + 0.06f * k, 1f - 0.08f * k));
                bones.ifPresent("korstguh_schild", b -> b.setScale(1f + 0.14f * k, 1f + 0.2f * k, 1f + 0.1f * k));
                bones.ifPresent("korstguh_kap", b -> b.setScale(1f + 0.25f * k, 1f + 0.25f * k, 1f + 0.25f * k));
            });
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    private static final class Duin implements VariantUiterlijk.Uiterlijk {
        /** How deep it digs in (model pixels): just over the top of its head, so only the ears are left. */
        private static final float DIEP = 15.7f;

        @Override
        public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
            float k = zacht(guh, pt, Trucs.doet(guh), 0.07f);
            float t = guh.tickCount + pt + guh.getId() * 7;
            if (k > 0.01f) {
                float omlaag = DIEP / 16f * k * maat(guh);
                frame.pose(pose -> pose.translate(0f, -omlaag, 0f));
            }
            // its big ears are never still; dug in they listen: a slow turn and a quick twitch now and then
            float wiebel = Mth.sin(t * 0.09f) * 0.07f + (k > 0.5f && (int) (t / 9) % 7 == 0 ? Mth.sin(t * 1.9f) * 0.16f : 0f);
            frame.bones(bones -> {
                bones.ifPresent("ear_left", b -> b.setRotZ(b.getRotZ() + wiebel));
                bones.ifPresent("ear_right", b -> b.setRotZ(b.getRotZ() + wiebel * 0.8f));
            });
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    private static final class Kruimel implements VariantUiterlijk.Uiterlijk {
        @Override
        public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
            float k = zacht(guh, pt, Kruimelzweef.zweeft(guh), 0.15f);
            float t = guh.tickCount + pt + guh.getId() * 5;
            float flap = 0.12f + Mth.sin(t * 0.12f) * 0.06f + k * (0.75f + Mth.sin(t * 0.9f) * 0.45f);
            float wieg = k * Mth.sin(t * 0.16f) * 0.07f;
            frame.bones(bones -> {
                for (String vleugel : new String[] {"kruimelguh_vleugel_links", "kruimelguh_vleugel_rechts"}) {
                    // (outward is towards the side the wing sits on, whatever way the model is mirrored)
                    bones.ifPresent(vleugel, b -> b.setRotZ(Math.signum(b.getBone().pivotX()) * flap));
                }
                if (k > 0.01f) {   // paws spread like a little parachutist, ears up in the wind
                    bones.ifPresent("leg_front_left", b -> b.setRotX(Mth.lerp(k, b.getRotX(), 1.0f)));
                    bones.ifPresent("leg_front_right", b -> b.setRotX(Mth.lerp(k, b.getRotX(), 1.0f)));
                    bones.ifPresent("leg_back_left", b -> b.setRotX(Mth.lerp(k, b.getRotX(), -1.0f)));
                    bones.ifPresent("leg_back_right", b -> b.setRotX(Mth.lerp(k, b.getRotX(), -1.0f)));
                    bones.ifPresent("body", b -> b.setRotZ(wieg));
                    bones.ifPresent("kruimelguh_staart", b -> b.setRotX(-0.5f * k));
                }
            });
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    private static final class Schimmel implements VariantUiterlijk.Uiterlijk {
        @Override
        public Identifier glow(GuhEntity guh) {
            return Trucs.doet(guh) ? SCHIMMEL_LAMP : SCHIMMEL_GLOED;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    private static final class Kristal implements VariantUiterlijk.Uiterlijk {
        @Override
        public Identifier glow(GuhEntity guh) {
            return KRISTAL_GLOED;
        }

        @Override
        public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
            frame.bones(bones -> {
                for (String kristal : new String[] {"kristalguh_kristal_links", "kristalguh_kristal_rechts"}) {
                    // (the outer crystals lean away from the middle; their pivot is at their foot)
                    bones.ifPresent(kristal, b -> b.setRotation(0.12f, 0f, -Math.signum(b.getBone().pivotX()) * 0.3f));
                }
                bones.ifPresent("kristalguh_kristal", b -> b.setRotX(-0.1f));
            });
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    private static final class Koor implements VariantUiterlijk.Uiterlijk {
        @Override
        public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
            float k = zacht(guh, pt, Trucs.doet(guh), 0.12f);
            float t = guh.tickCount + pt + guh.getId() * 3;
            float bloem = Mth.sin(t * 0.1f) * 0.05f + k * Mth.sin(t * 0.5f) * 0.14f;
            float kop = -0.42f * k + k * Mth.sin(t * 0.5f) * 0.06f, wieg = k * Mth.sin(t * 0.25f) * 0.12f;
            frame.bones(bones -> {
                bones.ifPresent("koorguh_bloem", b -> b.setRotZ(bloem));
                bones.ifPresent("koorguh_bloem_knop", b -> b.setRotZ(bloem));
                if (k > 0.01f) {   // head back, mouth to the sky, swaying on the beat
                    bones.ifPresent("head", b -> b.setRotX(b.getRotX() + kop).setRotZ(b.getRotZ() + wieg));
                    bones.ifPresent("koorguh_staart", b -> b.setRotY(wieg * 2f));
                }
            });
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    /** Now and then a sparkle off a Kristalguh's crystals and a glowing spore off a Schimmelguh that is a lamp. */
    private static void sprankels(ClientTickEvent.Post event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.level.getGameTime() % 4 != 0) {
            return;
        }
        for (GuhEntity guh : mc.level.getEntitiesOfClass(GuhEntity.class, mc.player.getBoundingBox().inflate(24), g -> g.getVariant().isBuitenGuh())) {
            GuhVariant v = guh.getVariant();
            boolean kristal = v == GuhVariant.KRISTALGUH, lamp = v == GuhVariant.SCHIMMELGUH && Trucs.doet(guh);
            if ((!kristal && !lamp) || guh.isInvisible() || guh.getRandom().nextInt(kristal ? 12 : 8) != 0) {
                continue;
            }
            double r = guh.getBbWidth() * 0.45;
            double x = guh.getX() + (guh.getRandom().nextDouble() - 0.5) * 2 * r, z = guh.getZ() + (guh.getRandom().nextDouble() - 0.5) * 2 * r;
            double y = guh.getY() + guh.getBbHeight() * (0.8 + guh.getRandom().nextDouble() * 0.4);
            if (kristal) {
                mc.level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0, 0.02, 0);
            } else {
                mc.level.addParticle(ParticleTypes.GLOW, x, y, z, 0, 0.01, 0);
            }
        }
    }

    private E2BiomeguhsClient() {
    }
}
