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

/**
 * Job advancement (ronde 2/8): at lv 10 and lv 20 each class gets a new title in a ceremony in the ordetempel (a non-click
 * RijkScene: the camera circles you, light beams rise, a big title card with the new title). Each step unlocks something real:
 * <ul>
 *   <li>Knuffelridder: 1 Kussenkampioen = Dubbelklap (Kussenklap swings a second time at 60 %); 2 Vahoege Paladijn =
 *   Paladijnsaura (you and your party within 10 blocks heal 1.5 % every 2 s).</li>
 *   <li>Pluisjager: 1 Sterrenschutter = Saltosalvo (Rolsalto fires 3 arrows at the nearest enemy mid-flip); 2 Vahoege
 *   Hemelvalk = Valkenblik (arrows curve towards enemies, +8 % crit, +15 % crit damage).</li>
 *   <li>Vadsmagier: 1 Sterrenwever = the Roze Vuurbal leaves an element puddle for 3 s; 2 Vahoege Aartsmagier = Vadsoverloop
 *   (every 3rd skill costs no energy, +10 % cooldown reduction, +8 % element damage).</li>
 * </ul>
 * z2-systemen: a 3rd step at lv 30 (Kristalkampioen / Poolstervalk / Kristalorakel, see {@code zone2.systemen.Advancement3}); its
 * hidden node {@code <x>_adv3} is added to the trees by that class. {@link #MAX_STAP} = 3.
 * Each step is a hidden tree node ({@code <x>_adv1/2/3}, cost 0) that a respec never removes and that carries a stat bonus.
 * API for the npc slice's advancement quests: {@link #kanDoen} / {@link #doe}.
 */
public final class Advancement {
    /** True until the npc slice's advancement quests are live: the ordetempel desk then shows a "Ceremonie" button. */
    public static boolean VIA_BALIE = true;

    private Advancement() {
    }

    /** z2-systemen: the number of steps (was 2); z3-systemen: 4 (the 4th at class lv 50, see zone3.systemen.Advancement4). */
    public static final int MAX_STAP = 4;
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

    /** The level the next step needs (10, 20, 30, 50) or 0 when all are done. */
    public static int nodigLevel(int stap) {
        return stap == 0 ? 10 : stap == 1 ? 20 : stap == 2 ? 30 : stap == 3 ? 50 : 0;
    }

    /** The level of step {@code s} (1..MAX_STAP). */
    public static int levelVan(int s) {
        return nodigLevel(s - 1);
    }

    /** Can this player do the next advancement of their active class now? */
    public static boolean kanDoen(ServerPlayer p) {
        return kanDoen(p, Spelers.klasse(p).id());
    }

    /** g-klassen: can this player do the next advancement of this class (its CLASS level counts)? */
    public static boolean kanDoen(ServerPlayer p, String klasse) {
        if (Klassen.van(klasse) == null) {
            return false;
        }
        int s = stap(p, klasse);
        return s < MAX_STAP && Spelers.level(p, klasse) >= nodigLevel(s);
    }

    /** Does the next advancement of the active class (with the ceremony). Returns false when it can't. */
    public static boolean doe(ServerPlayer p) {
        return doe(p, Spelers.klasse(p).id());
    }

    /** Does the next advancement of this class (with the ceremony). Returns false when it can't. */
    public static boolean doe(ServerPlayer p, String klasse) {
        if (!kanDoen(p, klasse)) {
            return false;
        }
        Klasse k = Klassen.van(klasse);
        int s = stap(p, k.id()) + 1;
        SpelerData d = Spelers.data(p);
        Boom.knopen(d, k.id()).add(Bomen.advKnoop(k.id(), s));
        Spelers.sync(p);
        ceremonie(p, k.id(), s);
        return true;
    }

    /** The title lang key of a class at an advancement step. */
    public static String titelKey(String klasse, int stap) {
        if (stap <= 0) {
            Klasse k = Klassen.van(klasse);
            return k != null ? k.naamKey() : "vadsrijk.klassen.klasse." + klasse;
        }
        return "vadsrijk.klassen.titel." + klasse + "." + stap;
    }

    /** The current title of this player (class + advancement). */
    public static String titelKey(Player p) {
        String k = Spelers.klasse(p).id();
        return titelKey(k, stap(p, k));
    }

    private static void ceremonie(ServerPlayer p, String klasse, int stap) {
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
                .titel(titelKey(klasse, stap), "vadsrijk.klassen.adv.onder." + klasse + "." + stap, 60, 80, Fx.ROZE)
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
        Later.na(150, () -> p.sendSystemMessage(Component.translatable("vadsrijk.klassen.adv.chat", Component.translatable(titelKey(klasse, stap)),
                Component.translatable("vadsrijk.klassen.adv.onder." + klasse + "." + stap)).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}
