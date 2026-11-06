package nl.juiced.guhs.feature.vadsrijk.hud.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.hud.Emote;
import nl.juiced.guhs.feature.vadsrijk.hud.UHudPayloads;

/**
 * U-HUD: emote poses on the client. The player model is not GeckoLib, so an emote is a small pose: arm/head/leg rotations (and
 * a whole-body offset for zit/slaap) set after vanilla's pose ({@code mixin.client.UHudEmotePoseMixin}), eased in and out.
 * Which player plays what comes from the server ({@link UHudPayloads.EmoteSpeel}); it is copied into the player's render
 * state ({@link #STAAT}) at extract time.
 */
public final class EmotePose {
    /** In a player's render state while they play an emote: the emote and how far it is (ticks, with partial tick). */
    public static final ContextKey<Speelt> STAAT = new ContextKey<>(Guhs.id("vr_u_hud_emote"));

    public record Speelt(Emote emote, float t) {
    }

    private record Lokaal(Emote emote, long start, @Nullable Vec3 waar) {
    }

    private static final Map<Integer, Lokaal> SPEELT = new ConcurrentHashMap<>();
    private static long klok;

    private EmotePose() {
    }

    static void ontvang(UHudPayloads.EmoteSpeel p) {
        Emote e = Emote.van(p.emote());
        if (e == null) {
            SPEELT.remove(p.entity());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity ent = mc.level == null ? null : mc.level.getEntity(p.entity());
        SPEELT.put(p.entity(), new Lokaal(e, klok - p.verstreken(), ent == null ? null : ent.position()));
    }

    /** Does this entity play an emote right now (for the HUD/tests)? */
    @Nullable
    public static Emote speelt(int entityId) {
        Lokaal l = SPEELT.get(entityId);
        return l == null ? null : l.emote();
    }

    static void tick() {
        klok++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            SPEELT.clear();
            return;
        }
        SPEELT.entrySet().removeIf(en -> {
            Lokaal l = en.getValue();
            Entity ent = mc.level.getEntity(en.getKey());
            if (ent == null) {
                return true;
            }
            if (!l.emote().loop) {
                return klok - l.start() > l.emote().ticks + 2;
            }
            // a loop emote ends as soon as the player walks away (the server says so too, a moment later)
            return l.waar() != null && ent.position().distanceToSqr(l.waar()) > 0.35 * 0.35;
        });
    }

    static void wis() {
        SPEELT.clear();
    }

    /** Extract time (render state modifier): copy the emote into the player's render state. */
    static void extract(Player p, AvatarRenderState state) {
        Lokaal l = SPEELT.get(p.getId());
        if (l == null) {
            state.setRenderData(STAAT, null);
            return;
        }
        float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        state.setRenderData(STAAT, new Speelt(l.emote(), klok - l.start() + pt));
    }

    // ---- the poses ----------------------------------------------------------------------------------------------------------------

    private static final float PI = (float) Math.PI;

    /** Called after vanilla's setupAnim of a player (or its armour) model. */
    public static void pas(HumanoidModel<?> m, AvatarRenderState state) {
        Speelt s = state.getRenderData(STAAT);
        if (s == null) {
            return;
        }
        Emote e = s.emote();
        float t = s.t();
        float w = Mth.clamp(t / 4f, 0, 1);
        if (!e.loop) {
            w *= Mth.clamp((e.ticks - t) / 6f, 0, 1);
        }
        if (w <= 0) {
            return;
        }
        ModelPart ra = m.rightArm, la = m.leftArm, rl = m.rightLeg, ll = m.leftLeg, hoofd = m.head, root = m.root();
        switch (e) {
            case ZWAAI -> {
                zet(ra, -PI + 0.25f, 0, 0.45f + Mth.sin(t * 0.7f) * 0.45f, w);
                zet(hoofd, hoofd.xRot, hoofd.yRot, Mth.sin(t * 0.35f) * 0.08f, w);
            }
            case KNUFFEL -> {
                float knijp = Mth.sin(t * 0.25f) * 0.08f;
                zet(ra, -1.35f, -0.55f - knijp, 0.05f, w);
                zet(la, -1.35f, 0.55f + knijp, -0.05f, w);
                zet(hoofd, 0.15f, hoofd.yRot, Mth.sin(t * 0.2f) * 0.12f, w);
            }
            case DANS -> {
                float a = t * 0.4f;
                zet(ra, -PI * 0.75f + Mth.sin(a) * 0.6f, 0, 0.3f, w);
                zet(la, -PI * 0.75f - Mth.sin(a) * 0.6f, 0, -0.3f, w);
                zet(rl, Mth.sin(a) * 0.45f, 0, 0.05f, w);
                zet(ll, -Mth.sin(a) * 0.45f, 0, -0.05f, w);
                zet(hoofd, Mth.sin(a * 2) * 0.18f, hoofd.yRot, Mth.cos(a) * 0.15f, w);
                root.yRot += Mth.sin(t * 0.2f) * 0.45f * w;
                root.y -= Math.abs(Mth.sin(a)) * 1.5f * w;
            }
            case JUICH -> {
                float a = t * 0.8f;
                zet(ra, -PI + 0.2f + Mth.sin(a) * 0.15f, 0, 0.45f, w);
                zet(la, -PI + 0.2f + Mth.cos(a) * 0.15f, 0, -0.45f, w);
                zet(hoofd, -0.3f, hoofd.yRot, 0, w);
                root.y -= Math.abs(Mth.sin(t * 0.45f)) * 2.5f * w;
            }
            case ZIT -> {
                zet(rl, -PI / 2, 0.15f, 0, w);
                zet(ll, -PI / 2, -0.15f, 0, w);
                zet(ra, -0.55f, 0, 0.05f, w);
                zet(la, -0.55f, 0, -0.05f, w);
                root.y += 10f * w;
            }
            case SLAAP -> {
                zet(rl, 0, 0, 0.05f, w);
                zet(ll, 0, 0, -0.05f, w);
                zet(ra, -0.15f, 0, 0.35f, w);
                zet(la, -0.15f, 0, -0.35f, w);
                zet(hoofd, 0, 0.25f * Mth.sin(t * 0.02f), 0, w);
                // lie on your back: the whole body turns 90 degrees backwards and goes down to the ground
                root.xRot += -PI / 2 * w;
                root.y += 21f * w;
                root.z += 10f * w;
                root.y += Mth.sin(t * 0.08f) * 0.3f * w;   // (breathing)
            }
            case LACH -> {
                float schud = Mth.sin(t * 1.6f);
                zet(ra, -0.7f, -0.6f, 0.1f, w);
                zet(la, -0.7f, 0.6f, -0.1f, w);
                zet(hoofd, -0.45f + schud * 0.08f, hoofd.yRot, 0, w);
                root.y += schud * 0.4f * w;
            }
            case WIJS -> zet(ra, -PI / 2 + hoofd.xRot, hoofd.yRot - 0.08f, 0, w);
            case HARTJE -> {
                zet(ra, -PI + 0.55f, 0, -0.45f, w);
                zet(la, -PI + 0.55f, 0, 0.45f, w);
                zet(hoofd, -0.15f, hoofd.yRot, Mth.sin(t * 0.3f) * 0.1f, w);
            }
        }
        // the overlay layers (hat/jacket/sleeves/pants) are children of these parts, so they follow
    }

    private static void zet(ModelPart p, float x, float y, float z, float w) {
        p.xRot = Mth.lerp(w, p.xRot, x);
        p.yRot = Mth.lerp(w, p.yRot, y);
        p.zRot = Mth.lerp(w, p.zRot, z);
    }
}
