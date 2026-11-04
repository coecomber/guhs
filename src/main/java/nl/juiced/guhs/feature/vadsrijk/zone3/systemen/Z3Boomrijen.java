package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;
import nl.juiced.guhs.feature.vadsrijk.klassen.Later;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.ridder.Knuffelridder;

/**
 * Skill-tree row 6 (+ a capstone row 7) for zone 3 (ZONE3_PLAN §9.2, cap 50: 15 more points). Per class 4 nodes that all do
 * something real, 4 points each:
 * <ul>
 *   <li>row 6, kolom 0/1/2 at class lv {@link #LV_A}/{@link #LV_B}/{@link #LV_C}, each needs any row-5 node;</li>
 *   <li>row 7, the keystone in the middle at class lv {@link #LV_SLUIT}, needs any row-6 node.</li>
 * </ul>
 * 16 points for all four, with 15 new points (and the old rows still wanting points): you still choose. Effects run on kern hooks
 * ({@code Schade.VOOR_KLAP/NA_KLAP}, {@code Vaardigheden.NA_GEBRUIK / COOLDOWN_MOD}).
 */
public final class Z3Boomrijen {
    public static final int LV_A = 40, LV_B = 42, LV_C = 45, LV_SLUIT = 48, KOSTEN = 4;
    /** Lichtpijl: marked target -> game time the mark ends. */
    private static final Map<UUID, Long> GEMARKEERD = new ConcurrentHashMap<>();
    /** Zweefschot: player -> game time it is ready again. */
    private static final Map<UUID, Long> ZWEEF_KLAAR = new ConcurrentHashMap<>();

    private Z3Boomrijen() {
    }

    public static List<BoomKnoop> ridder() {
        String k = Klassen.RIDDER, rij5 = "r_galmstoot|r_kristalhart|r_ijzeren_knuffel";
        List<BoomKnoop> l = new ArrayList<>();
        l.add(knoop(k, "r_schorshuid", 6, 0, LV_A, rij5, "minecraft:spruce_log"));
        l.add(knoop(k, "r_afstijgstorm", 6, 1, LV_B, rij5, "minecraft:feather"));
        l.add(knoop(k, "r_lantaarnschild", 6, 2, LV_C, rij5, "minecraft:lantern"));
        l.add(knoop(k, "r_drakenkussen", 7, 1, LV_SLUIT, "r_schorshuid|r_afstijgstorm|r_lantaarnschild", "minecraft:pink_bed"));
        return l;
    }

    public static List<BoomKnoop> jager() {
        String k = Klassen.JAGER, rij5 = "j_splinterregen|j_sterrenoog|j_windloper";
        List<BoomKnoop> l = new ArrayList<>();
        l.add(knoop(k, "j_zweefschot", 6, 0, LV_A, rij5, "minecraft:phantom_membrane"));
        l.add(knoop(k, "j_lichtpijl", 6, 1, LV_B, rij5, "minecraft:spectral_arrow"));
        l.add(knoop(k, "j_sporenzoeker", 6, 2, LV_C, rij5, "minecraft:spyglass"));
        l.add(knoop(k, "j_sterrenzwerm", 7, 1, LV_SLUIT, "j_zweefschot|j_lichtpijl|j_sporenzoeker", "minecraft:nether_star"));
        return l;
    }

    public static List<BoomKnoop> magier() {
        String k = Klassen.MAGIER, rij5 = "m_ijsnova|m_sterrenwijsheid|m_manastroom";
        List<BoomKnoop> l = new ArrayList<>();
        l.add(knoop(k, "m_lantaarnvlam", 6, 0, LV_A, rij5, "minecraft:soul_lantern"));
        l.add(knoop(k, "m_wortelband", 6, 1, LV_B, rij5, "minecraft:hanging_roots"));
        l.add(knoop(k, "m_blinkspoor", 6, 2, LV_C, rij5, "minecraft:glow_berries"));
        l.add(knoop(k, "m_meteoorzwerm", 7, 1, LV_SLUIT, "m_lantaarnvlam|m_wortelband|m_blinkspoor", "minecraft:fire_charge"));
        return l;
    }

    private static BoomKnoop knoop(String klasse, String id, int rij, int kolom, int level, String vereist, String icoon) {
        Bomen.ICOON.put(id, icoon);
        Boom.MIN_LEVEL.put(id, level);
        return new BoomKnoop(id, klasse, KOSTEN, List.of(vereist), rij, kolom, null, id);
    }

    /** Adds the nodes to a registered class's tree (once). */
    static void voegToe(String klasse, List<BoomKnoop> knopen) {
        Klasse k = Klassen.van(klasse);
        if (k == null) {
            return;
        }
        for (BoomKnoop b : knopen) {
            if (Boom.knoop(k, b.id()) == null) {
                k.boom().add(b);   // (the klassen slice's trees are ArrayLists)
            }
        }
    }

    static void init() {
        voegToe(Klassen.RIDDER, ridder());
        voegToe(Klassen.JAGER, jager());
        voegToe(Klassen.MAGIER, magier());
        Schade.VOOR_KLAP.add(Z3Boomrijen::voorKlap);
        Schade.NA_KLAP.add(Z3Boomrijen::naKlap);
        Vaardigheden.NA_GEBRUIK.add(Z3Boomrijen::naGebruik);
        Vaardigheden.COOLDOWN_MOD.add((p, v) -> "stormsprong".equals(v.id()) && Spelers.heeftKnoop(p, "r_afstijgstorm") && v.cooldownTicks() > 0
                ? (float) Math.max(0, v.cooldownTicks() - 20) / v.cooldownTicks() : null);
    }

    private static boolean heeftStatus(LivingEntity e) {
        for (Status s : new Status[] {Status.BRANDEND, Status.GESPOREN, Status.PLAKKERIG, Status.VERDOOFD, Status.BEVROREN}) {
            if (Statussen.heeft(e, s)) {
                return true;
            }
        }
        return false;
    }

    static double voorKlap(Schade.Klap k, double n) {
        String id = k.skillId() == null ? "" : k.skillId();
        LivingEntity bron = k.bron(), doel = k.doel();
        // Schorshuid: below 40 % Vads, 15 % less damage taken
        if (doel instanceof ServerPlayer p && Spelers.heeftKnoop(p, "r_schorshuid") && Spelers.vads(p) < Spelers.maxVads(p) * 0.40) {
            n *= 0.85;
        }
        // Lantaarnschild: who hits your raised shield gets a quarter back
        if (doel instanceof ServerPlayer p && bron instanceof VadsrijkMob m && n > 0 && Spelers.heeftKnoop(p, "r_lantaarnschild") && Knuffelridder.blokt(p)) {
            double terug = n * 0.25;
            Later.na(1, () -> Schade.doe(new Schade.Klap(p, m, terug, nl.juiced.guhs.feature.vadsrijk.kern.Element.ROZE, Schade.Soort.WAAR, false, "z3_lantaarnschild")));
        }
        if (!(bron instanceof ServerPlayer p) || k.soort() == Schade.Soort.WAAR) {
            return n;
        }
        if (id.equals("stormsprong") && Spelers.heeftKnoop(p, "r_afstijgstorm")) {
            n *= 1.35;
        }
        // Zweefschot: a shot from the air (jumping, gliding, on a mount) hits 50 % harder, every 8 s
        if (k.soort() == Schade.Soort.AFSTAND && Spelers.heeftKnoop(p, "j_zweefschot") && (!p.onGround() || p.isPassenger())) {
            long nu = p.level().getGameTime();
            if (ZWEEF_KLAAR.getOrDefault(p.getUUID(), 0L) <= nu) {
                ZWEEF_KLAAR.put(p.getUUID(), nu + 160);
                n *= 1.5;
                if (p.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, doel.getX(), doel.getY() + doel.getBbHeight() * 0.6, doel.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
        // Lichtpijl: everything hits a marked target 15 % harder (anyone's hits, it is a mark)
        Long merk = GEMARKEERD.get(doel.getUUID());
        if (merk != null && merk > doel.level().getGameTime()) {
            n *= 1.15;
        }
        if (Spelers.heeftKnoop(p, "j_sporenzoeker") && heeftStatus(doel)) {
            n *= 1.20;
        }
        return n;
    }

    static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        String id = k.skillId() == null ? "" : k.skillId();
        if (u.schade() <= 0 || !(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob m) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        long nu = sl.getGameTime();
        if (id.equals("knuffelpijl") && Spelers.heeftKnoop(p, "j_lichtpijl")) {
            GEMARKEERD.put(m.getUUID(), nu + 120);
            sl.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        }
        if (id.equals("sterrenschot") && Spelers.heeftKnoop(p, "j_sterrenzwerm")) {
            double s = Spelers.wapenSchade(p) * 0.4;
            for (int i = 1; i <= 3; i++) {
                Later.na(5 * i, () -> {
                    if (!m.isRemoved() && m.isAlive()) {
                        Fx.lijn(sl, ParticleTypes.END_ROD, p.getEyePosition(), m.position().add(0, m.getBbHeight() * 0.5, 0), 0.7);
                        Schade.doe(new Schade.Klap(p, m, s, Spelers.wapenElement(p), Schade.Soort.AFSTAND, true, "z3_nalaser"));
                    }
                });
            }
        }
        if (id.equals("roze_vuurbal") && Spelers.heeftKnoop(p, "m_lantaarnvlam")) {
            Statussen.geef(m, Status.BRANDEND, 60, Math.max(1, Spelers.wapenSchade(p) * 0.15), p);
        }
        if (id.equals("meteoorguh") && Spelers.heeftKnoop(p, "m_meteoorzwerm") && !METEOOR.containsKey(p.getUUID() + ":" + nu)) {
            METEOOR.put(p.getUUID() + ":" + nu, nu);
            METEOOR.values().removeIf(t -> t < nu - 40);
            Vec3 c = m.position();
            double s = Spelers.wapenSchade(p) * 0.6;
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 / 3 * i + p.getRandom().nextDouble();
                Vec3 v = c.add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                Later.na(8 + 6 * i, () -> {
                    sl.sendParticles(ParticleTypes.FLAME, v.x, v.y + 0.5, v.z, 25, 1, 0.4, 1, 0.05);
                    sl.sendParticles(Fx.stof(Fx.ROZE, 2f), v.x, v.y + 1, v.z, 15, 1, 0.6, 1, 0);
                    Fx.geluid(sl, v, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f);
                    for (LivingEntity e : Fx.mobsBij(sl, v, 2.5)) {
                        Schade.doe(new Schade.Klap(p, e, s, Spelers.wapenElement(p), Schade.Soort.MAGIE, true, "z3_meteoorzwerm"));
                    }
                });
            }
        }
    }

    private static final Map<String, Long> METEOOR = new ConcurrentHashMap<>();

    /** Skills that were just used: Drakenkussen, Wortelband, Blinkspoor. */
    static void naGebruik(ServerPlayer p, Vaardigheid v) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        switch (v.id()) {
            case "vahoege_plof" -> {
                if (Spelers.heeftKnoop(p, "r_drakenkussen")) {
                    for (ServerPlayer lid : Fx.party(p, 10)) {
                        Statussen.geef(lid, Status.KNUFFELSCHILD, 80, Spelers.maxVads(lid) * 0.20, p);
                        sl.sendParticles(Fx.stof(Fx.ROZE, 1.8f), lid.getX(), lid.getY() + 1.2, lid.getZ(), 20, 0.8, 0.5, 0.8, 0);
                        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, lid.getX(), lid.getY() + 1.8, lid.getZ(), 12, 0.9, 0.3, 0.9, 0.02);
                    }
                }
            }
            case "vadsbron" -> {
                if (Spelers.heeftKnoop(p, "m_wortelband")) {
                    for (LivingEntity e : Fx.mobsBij(sl, p.position(), 5)) {
                        Statussen.geef(e, Status.PLAKKERIG, 40, 1, p);
                        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, e.getX(), e.getY() + 0.3, e.getZ(), 6, 0.3, 0.1, 0.3, 0);
                    }
                }
            }
            case "blink" -> {
                if (Spelers.heeftKnoop(p, "m_blinkspoor")) {
                    Vec3 van = p.position();
                    Later.na(2, () -> {
                        Fx.lijn(sl, ParticleTypes.HAPPY_VILLAGER, van.add(0, 0.3, 0), p.position().add(0, 0.3, 0), 0.6);
                        for (ServerPlayer lid : Fx.party(p, 6)) {
                            Schade.heal(p, lid, Spelers.maxVads(lid) * 0.08);
                        }
                    });
                }
            }
            default -> {
            }
        }
    }
}
