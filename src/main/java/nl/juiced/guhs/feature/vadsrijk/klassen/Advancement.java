package nl.juiced.guhs.feature.vadsrijk.klassen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs;

/**
 * Job advancement. U-SPECS (ronde 4, V2/V3): the NEW model replaces the old steps at class lv 10/20/30/50:
 * <ul>
 *   <li>lv 1: the first job right away (the weapon in your hand, free switching between the 3 classes, as before);</li>
 *   <li>step 1 at class lv {@value #LV1}, just before the Zwamtempel (zone 1's end dungeon): here, and only here, you pick a
 *   SPECIALISATION ({@link Spec}, two per class); the ceremony asks for the choice ({@link Specs#kies});</li>
 *   <li>step 2 at class lv {@value #LV2}: zone 3, after the first boss (the Kaaszeppelinwerf) and the Pluimguh;</li>
 *   <li>step 3 at class lv {@value #LV3}: the end of Hoogvads (after the zone-5 finale).</li>
 * </ul>
 * After step 1 you keep "speccing through": every step deepens your spec (a new effect or an ult upgrade, and a title per spec;
 * see {@code SpecEffecten} and the lang keys {@code vadsrijk.klassen.titel.<spec>.<step>}).
 * Each step is still a hidden tree node ({@code <x>_adv1/2/3}, cost 0) that a respec never removes and that carries a stat
 * bonus; the old {@code <x>_adv4} node is no longer granted (the migration in {@link Specs#migreer} maps old saves).
 * API for the quests (npc role "advancement"): {@link #kanDoen} / {@link #doe}.
 */
public final class Advancement {
    /** True until the npc slice's advancement quests are live: the ordetempel desk then shows a "Ceremonie" button. */
    public static boolean VIA_BALIE = true;
    /** U-SPECS: the class levels of the three steps. */
    public static final int LV1 = 17, LV2 = 35, LV3 = 58;

    private Advancement() {
    }

    /**
     * U-SPECS: story gates per step (2 = zone 3 after the Kaaszeppelinwerf, 3 = zone 5 after the finale; filled by
     * spec.SpecRegistry). {@link #POORTEN} = false (tests) ignores them.
     */
    public static final java.util.Map<Integer, java.util.function.Predicate<ServerPlayer>> POORT = new java.util.concurrent.ConcurrentHashMap<>();
    public static boolean POORTEN = true;

    /** U-SPECS: 3 steps (was 4). */
    public static final int MAX_STAP = 3;
    /** z2-systemen: extra ceremony effects per step (player, step), right after the ceremony starts. */
    public static final java.util.List<java.util.function.BiConsumer<ServerPlayer, Integer>> CEREMONIE = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 0..{@link #MAX_STAP}: how many advancements this player did in this class. */
    public static int stap(Player p, String klasse) {
        var k = Spelers.data(p).boom.get(klasse);
        if (k == null) {
            return 0;
        }
        for (int s = MAX_STAP; s >= 1; s--) {
            if (k.contains(Bomen.advKnoop(klasse, s))) {
                return s;
            }
        }
        return 0;
    }

    /** The level the next step needs (17, 35, 58) or 0 when all are done. */
    public static int nodigLevel(int stap) {
        return stap == 0 ? LV1 : stap == 1 ? LV2 : stap == 2 ? LV3 : 0;
    }

    /** The level of step {@code s} (1..MAX_STAP). */
    public static int levelVan(int s) {
        return nodigLevel(s - 1);
    }

    /** Can this player do the next advancement of their active class now? */
    public static boolean kanDoen(ServerPlayer p) {
        return kanDoen(p, Spelers.klasse(p).id());
    }

    /**
     * g-klassen: can this player do the next advancement of this class (its CLASS level counts)? U-SPECS: steps 2 and 3 need
     * the spec of step 1 (a migrated save without one first picks it).
     */
    public static boolean kanDoen(ServerPlayer p, String klasse) {
        if (Klassen.van(klasse) == null) {
            return false;
        }
        int s = stap(p, klasse);
        if (s >= 1 && Specs.spec(p, klasse) == null) {
            return false;
        }
        if (POORTEN && s < MAX_STAP && POORT.containsKey(s + 1) && !POORT.get(s + 1).test(p)) {
            return false;
        }
        return s < MAX_STAP && Spelers.level(p, klasse) >= nodigLevel(s);
    }

    /** Does the next advancement of the active class (with the ceremony). Returns false when it can't. */
    public static boolean doe(ServerPlayer p) {
        return doe(p, Spelers.klasse(p).id());
    }

    /**
     * Does the next advancement of this class (with the ceremony). Returns false when it can't. U-SPECS: step 1 first opens
     * the spec choice (the screen); the ceremony follows the choice ({@link Specs#kies}).
     */
    public static boolean doe(ServerPlayer p, String klasse) {
        if (Klassen.van(klasse) != null && stap(p, klasse) >= 1 && Specs.spec(p, klasse) == null) {
            Specs.zetOpen(Spelers.data(p), klasse, true);   // (a migrated save: pick the spec first)
            KlasseBalie.vraagSpec(p, klasse);
            return true;
        }
        if (!kanDoen(p, klasse)) {
            return false;
        }
        Klasse k = Klassen.van(klasse);
        int s = stap(p, k.id()) + 1;
        if (s == 1) {
            Specs.zetOpen(Spelers.data(p), k.id(), true);
            KlasseBalie.vraagSpec(p, k.id());
            return true;
        }
        voltooi(p, k.id(), s);
        return true;
    }

    /** Tests/dev: step 1 with this spec right away (no screen). */
    public static boolean doe(ServerPlayer p, Spec spec) {
        if (stap(p, spec.klasse) == 0 && !kanDoen(p, spec.klasse)) {
            return false;
        }
        Specs.zetOpen(Spelers.data(p), spec.klasse, true);
        return Specs.kies(p, spec) == null;
    }

    /** U-SPECS: grants step {@code s} (its hidden node and the ones before it) and plays the ceremony. */
    public static void voltooi(ServerPlayer p, String klasse, int s) {
        SpelerData d = Spelers.data(p);
        for (int i = 1; i <= s; i++) {
            Boom.knopen(d, klasse).add(Bomen.advKnoop(klasse, i));
        }
        Spelers.sync(p);
        ceremonie(p, klasse, s);
    }

    /** The title lang key of a class at an advancement step without a spec (the class name, or the old class title). */
    public static String titelKey(String klasse, int stap) {
        if (stap <= 0) {
            Klasse k = Klassen.van(klasse);
            return k != null ? k.naamKey() : "vadsrijk.klassen.klasse." + klasse;
        }
        return "vadsrijk.klassen.titel." + klasse + "." + stap;
    }

    /** U-SPECS: the title of a player's class at a step: the spec's title (e.g. "Schildridder"), else {@link #titelKey(String, int)}. */
    public static String titelKey(Player p, String klasse, int stap) {
        Spec sp = Specs.spec(p, klasse);
        return stap >= 1 && sp != null ? sp.titelKey(stap) : titelKey(klasse, stap);
    }

    /** The current title of this player (class + advancement). */
    public static String titelKey(Player p) {
        String k = Spelers.klasse(p).id();
        return titelKey(p, k, stap(p, k));
    }

    /** U-SPECS: the lang key of the line under the title card (per spec when there is one). */
    static String onderKey(ServerPlayer p, String klasse, int stap) {
        Spec sp = Specs.spec(p, klasse);
        return sp != null ? "vadsrijk.klassen.adv.onder." + sp.id + "." + stap : "vadsrijk.klassen.adv.onder." + klasse + "." + stap;
    }

    /** The ceremony scene of a step (the camera circles you, light beams, the title card). */
    public static void ceremonie(ServerPlayer p, String klasse, int stap) {
        ServerLevel sl = (ServerLevel) p.level();
        Vec3 c = p.position();
        Vec3 hoofd = c.add(0, 1.6, 0);
        double yaw = Math.toRadians(p.getYRot());
        Vec3 voor = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 zij = new Vec3(voor.z, 0, -voor.x);
        RijkScene scene = RijkScene.maak("vadsrijk.klassen.adv." + klasse + "." + stap, 150)
                .camera(0, hoofd.add(voor.scale(7)).add(zij.scale(4)).add(0, 1, 0), hoofd)
                .camera(45, hoofd.add(voor.scale(4)).add(zij.scale(-6)).add(0, 3, 0), hoofd)
                .camera(90, hoofd.add(voor.scale(-3)).add(zij.scale(-4)).add(0, 7, 0), hoofd.add(0, 2, 0))
                .camera(150, hoofd.add(voor.scale(3.2)).add(0, 0.3, 0), hoofd)
                .titel(titelKey(p, klasse, stap), onderKey(p, klasse, stap), 60, 80, Fx.ROZE)
                .bouw();
        RijkScenes.speel(p, scene);
        CEREMONIE.forEach(f -> f.accept(p, stap));   // z2-systemen
        Fx.geluid(sl, c, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.8f);
        for (int t = 0; t < 140; t += 4) {
            int tt = t;
            Later.na(t, () -> {
                // four rising light beams around you, coming closer
                double r = 3.2 - tt / 70.0;
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 2 * i + tt * 0.03;
                    Vec3 v = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    for (int h = 0; h < 14; h += 2) {
                        sl.sendParticles(ParticleTypes.END_ROD, v.x, v.y + h * 0.9, v.z, 1, 0.05, 0.3, 0.05, 0.01);
                    }
                    sl.sendParticles(Fx.stof(Fx.ROZE, 2f), v.x, v.y + 0.3, v.z, 2, 0.1, 0.2, 0.1, 0);
                }
                if (tt % 20 == 0) {
                    Fx.geluid(sl, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.6f + tt / 140f);
                }
            });
        }
        Later.na(60, () -> {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1, c.z, 120, 0.8, 1.2, 0.8, 0.4);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 4, c.z, 80, 2.5, 1, 2.5, 0.03);
            sl.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 2, c.z, 60, 0.5, 1.5, 0.5, 0.2);
            for (int h = 0; h < 30; h++) {
                sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + h, c.z, 3, 0.15, 0.2, 0.15, 0.01);
            }
            Fx.geluid(sl, c, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1f);
            Fx.geluid(sl, c, SoundEvents.PLAYER_LEVELUP, 1f, 0.7f);
        });
        Later.na(150, () -> p.sendSystemMessage(Component.translatable("vadsrijk.klassen.adv.chat", Component.translatable(titelKey(p, klasse, stap)),
                Component.translatable(onderKey(p, klasse, stap))).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}
