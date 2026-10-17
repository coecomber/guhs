package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Rustpunten;

/**
 * The sneak through the base under Meer Laogai (DESIGN 5 h3: "Sluipen naar de basis onder Meer Laogai"; DESIGN 0: seen =
 * pushed back to the last rest point). Six Dai Li-Mika's stand guard ({@code sluip_wacht_1..6}), only for you: each one
 * slowly turns his head to and fro ({@link #kijkt}), and what he sees is a cone in front of him: {@link #ZICHT} blocks when
 * you walk, {@link #ZICHT_GEBUKT} when you crouch, never through a wall. Seen: "Halt!", a puff, and you are back in the
 * hall ({@code rustpunt_3}), nothing lost. Reach the bars of Appa's chamber ({@code appa_poort}) and the step ends with Long
 * Feng-Mika and the reunion ({@link H3Verloop#tralies}).
 */
public final class H3Sluipen {
    public static final int WACHTERS = 6;
    /**
     * fix3-h3 (playtest 2 item 5: "ik kom niet voorbij de derde Dai Li-Mika"): crouched they see you only up to
     * {@link #ZICHT_GEBUKT} = 1.8 blocks. The corridor is three wide and every guard stands against one of its walls, so
     * crouching along the other wall (two blocks from him) always gets you past; walking past still needs his head turned
     * away. It was 3.2: then a guard saw a crouching player at the far wall, and the third one (and the one at Appa's bars)
     * could only be passed in a short gap of his sweep.
     */
    public static final double ZICHT = 7.5, ZICHT_GEBUKT = 1.8, HOEK = 38, ZWAAI = 75, DOEL = 1.9;
    /** After being sent back the guards give you this many ticks to breathe (and to crouch). */
    public static final int RUST = 80;
    /** The way you face on the rest point: south, down the corridor towards Appa (yaw 0), not at the stair you came down. */
    public static final float RUST_YAW = 0f;
    public static final String SOORT = "einde2_h3_aarde";

    private static final Map<UUID, Integer> RUST_TOT = new ConcurrentHashMap<>();

    private H3Sluipen() {
    }

    /** Which way guard nr (1..6) looks at this tick: his marker's yaw, swinging {@link #ZWAAI} degrees each way. */
    public static float kijkt(float basis, int nr, long tick) {
        int periode = 110 + nr * 17;
        return basis + (float) (ZWAAI * Math.sin(2 * Math.PI * ((tick + nr * 37L) % periode) / periode));
    }

    /**
     * Does a guard at {@code oog} looking along {@code yaw} see a player at {@code doel} (feet) who crouches or not? Pure: the
     * cone and the distance; the wall is asked by the caller.
     */
    public static boolean ziet(Vec3 oog, float yaw, Vec3 doel, boolean gebukt) {
        Vec3 d = doel.subtract(oog);
        double ver = Math.sqrt(d.x * d.x + d.z * d.z);
        if (ver > (gebukt ? ZICHT_GEBUKT : ZICHT) || Math.abs(d.y) > 3) {
            return false;
        }
        if (ver < 1.2) {
            return true;
        }
        float naar = Cutscene.yawVan(d);
        return Math.abs(Mth.wrapDegrees(naar - yaw)) <= HOEK;
    }

    /** (every 10 ticks from H3Verloop, step LAOGAI) in the base: the rest point, the guards, the goal. */
    static void tik(ServerPlayer p, E2H3Aarde.Stad stad) {
        if (!stad.in(p, "basis", 1)) {
            H3Acteurs.wegAlle(p, "wacht");
            return;
        }
        Vec3 rust = stad.voet("rustpunt_3");
        Rustpunten.Punt punt = Rustpunten.van(p, SOORT);
        if (rust != null && punt != null && punt.yaw() != RUST_YAW) {
            Rustpunten.zet(p, SOORT, GuheindeFeature.GUHEINDE, rust, RUST_YAW);   // (a rest point of before fix3-h3 faced the stair)
        }
        if (rust != null && punt == null) {
            Rustpunten.zet(p, SOORT, GuheindeFeature.GUHEINDE, rust, RUST_YAW);
            p.sendSystemMessage(Component.translatable("quest.guhs.einde2_h3_aarde.sluip.begin").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            H3Hints.titel(p, Component.translatable("gui.guhs.einde2_h3_aarde.sluip.titel").withStyle(ChatFormatting.GREEN),
                    Component.translatable("gui.guhs.einde2_h3_aarde.sluip.onder").withStyle(ChatFormatting.YELLOW), 70);
        }
        for (int nr = 1; nr <= WACHTERS; nr++) {
            if (H3Acteurs.van(p, "wacht_" + nr) == null) {
                Vec3 plek = stad.voet("sluip_wacht_" + nr);
                if (plek != null) {
                    H3Acteurs.npc(p, "wacht_" + nr, GuhNpcEntity.Kind.DAI_LI_MIKA, stad.level(), plek, stad.plek("sluip_wacht_" + nr).yaw(),
                            E2H3Aarde.STRUCTUUR + "/wacht", true);
                }
            }
        }
        if (stad.bij(p, "appa_poort", DOEL) && H3Verloop.vrij(p)) {
            H3Acteurs.wegAlle(p, "wacht");
            Rustpunten.wis(p, SOORT);
            H3Verloop.tralies(p, stad);
        }
    }

    /** Every tick for a player among the guards: they turn, they look. */
    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || E2H3Aarde.stap(p) != E2H3Aarde.LAOGAI) {
            return;
        }
        Entity eerste = H3Acteurs.van(p, "wacht_1");
        if (eerste == null) {
            return;
        }
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        if (stad == null) {
            return;
        }
        long tick = p.level().getGameTime();
        int rust = RUST_TOT.getOrDefault(p.getUUID(), 0);
        for (int nr = 1; nr <= WACHTERS; nr++) {
            Entity w = H3Acteurs.van(p, "wacht_" + nr);
            if (w == null) {
                continue;
            }
            float yaw = kijkt(stad.plek("sluip_wacht_" + nr).yaw(), nr, tick);
            w.setYRot(yaw);
            w.setYHeadRot(yaw);
            if (w instanceof GuhNpcEntity npc) {
                npc.setYBodyRot(yaw);
            }
            if (p.tickCount % BUNDEL_TIK == nr % BUNDEL_TIK && w.distanceToSqr(p) < 24 * 24) {
                bundel(p, w, yaw);
            }
            if (p.tickCount < rust || !nl.juiced.guhs.feature.verhaal.Duwtje.mag(p)) {
                continue;
            }
            Vec3 oog = w.getEyePosition();
            if (ziet(w.position(), yaw, p.position(), p.isShiftKeyDown()) && vrijeLijn(p, oog)) {
                betrapt(p, w);
                return;
            }
        }
    }

    /** (fix2-h3) How often (ticks) a guard's beam of dust is drawn. */
    static final int BUNDEL_TIK = 4;
    private static final DustParticleOptions BUNDEL_VER = new DustParticleOptions(0xE8D890, 0.7f);
    private static final DustParticleOptions BUNDEL_DICHT = new DustParticleOptions(0xF08A30, 0.9f);

    /**
     * (fix2-h3, playtest 1: "hidden triggers must be visible") what a guard sees, drawn for the sneaking player: a beam of
     * dust on the floor along the middle and both edges of his cone, orange as far as he sees you when you crouch, pale
     * yellow up to where he sees you walking. It stops at a wall, as his sight does.
     */
    static void bundel(ServerPlayer p, Entity wacht, float yaw) {
        Vec3 voet = wacht.position().add(0, 0.25, 0);
        for (float hoek : new float[] {-(float) HOEK, 0f, (float) HOEK}) {
            Vec3 r = Vec3.directionFromRotation(0, yaw + hoek);
            for (double d = 1.2; d <= ZICHT; d += hoek == 0 ? 0.9 : 1.3) {
                Vec3 punt = voet.add(r.scale(d));
                net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(punt.x, punt.y + 0.5, punt.z);
                if (!p.level().getBlockState(b).getCollisionShape(p.level(), b).isEmpty()) {
                    break;
                }
                Schijn.deeltjes(p, d <= ZICHT_GEBUKT ? BUNDEL_DICHT : BUNDEL_VER, punt, 1, 0.02, 0);
            }
        }
    }

    private static boolean vrijeLijn(ServerPlayer p, Vec3 oog) {
        return p.level().clip(new ClipContext(oog, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    /** Seen: "Halt!", and back to the hall. */
    static void betrapt(ServerPlayer p, Entity wacht) {
        RUST_TOT.put(p.getUUID(), p.tickCount + RUST);
        Schijn.deeltjes(p, ParticleTypes.ANGRY_VILLAGER, wacht.position().add(0, 2.3, 0), 3, 0.2, 0);
        Schijn.geluid(p, SoundEvents.VILLAGER_NO, wacht.blockPosition(), 1f, 0.6f);
        E2H3Aarde.zeg(p, wacht, "sluip.halt." + p.getRandom().nextInt(4));
        if (wacht instanceof GuhNpcEntity npc) {
            npc.speel("stenen_hand");
        }
        Rustpunten.terug(p, SOORT);
    }

    /** Not sneaking any more (another step, left the base): the guards go. */
    static void stop(ServerPlayer p) {
        if (H3Acteurs.van(p, "wacht_1") != null) {
            H3Acteurs.wegAlle(p, "wacht");
        }
    }
}
