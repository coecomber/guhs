package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;

/**
 * The zone-2 rows of the three skill trees (lv cap 35: 15 more points). Row 4 opens at lv {@link #RIJ4_LEVEL} (2 points per
 * node), row 5 at lv {@link #RIJ5_LEVEL} (3 points). Per class 6 nodes: two stat nodes and four that change how you fight,
 * most of them around the new status Bevroren. Row-4 nodes need any capstone (row 3), a row-5 node the row-4 node above it.
 * Everything costs 15; with 34 points at lv 35 and the 22 of zone 1 you still choose.
 * <p>Added to each class's tree list at registration ({@link #voegToe}; the lists are the klassen slice's ArrayLists, so the tree
 * screen, the stats and the respec see them like any other node). Effects run on kern hooks.
 */
public final class Boomrijen {
    public static final int RIJ4_LEVEL = 20, RIJ5_LEVEL = 26;
    private static final Map<UUID, Long> IJZER = new HashMap<>();

    private Boomrijen() {
    }

    public static List<BoomKnoop> ridder() {
        String k = Klassen.RIDDER, cap = "r_reuzenkussen|r_kussenstorm|r_bolwerk";
        List<BoomKnoop> l = new ArrayList<>();
        l.add(var(k, "r_ijsklap", 4, 0, List.of(cap), "minecraft:blue_ice"));
        l.add(stat(k, "r_ijspantser", 4, 1, List.of(cap), Stats.van(Stat.VADS, 250, Stat.VERDEDIGING, 12, Stat.ELEMENTWEERSTAND, 8), "minecraft:packed_ice"));
        l.add(var(k, "r_onverzettelijk", 4, 2, List.of(cap), "minecraft:anvil"));
        l.add(var(k, "r_galmstoot", 5, 0, List.of("r_ijsklap"), "minecraft:bell"));
        l.add(stat(k, "r_kristalhart", 5, 1, List.of("r_ijspantser"), Stats.van(Stat.KRACHT, 10, Stat.KRITIEKSCHADE, 20, Stat.VADS, 150),
                "minecraft:amethyst_cluster"));
        l.add(var(k, "r_ijzeren_knuffel", 5, 2, List.of("r_onverzettelijk"), "minecraft:iron_chestplate"));
        return l;
    }

    public static List<BoomKnoop> jager() {
        String k = Klassen.JAGER, cap = "j_vallende_ster|j_pluizenmantel|j_jachtroes";
        List<BoomKnoop> l = new ArrayList<>();
        l.add(var(k, "j_ijspijl", 4, 0, List.of(cap), "minecraft:arrow"));
        l.add(stat(k, "j_poolvacht", 4, 1, List.of(cap), Stats.van(Stat.BEHENDIGHEID, 8, Stat.VADS, 150, Stat.LOOPSNELHEID, 5), "minecraft:snowball"));
        l.add(var(k, "j_jachtinstinct", 4, 2, List.of(cap), "minecraft:bone"));
        l.add(var(k, "j_splinterregen", 5, 0, List.of("j_ijspijl"), "minecraft:pointed_dripstone"));
        l.add(stat(k, "j_sterrenoog", 5, 1, List.of("j_poolvacht"), Stats.van(Stat.KRITIEK, 8, Stat.KRITIEKSCHADE, 25), "minecraft:glow_berries"));
        l.add(var(k, "j_windloper", 5, 2, List.of("j_jachtinstinct"), "minecraft:breeze_rod"));
        return l;
    }

    public static List<BoomKnoop> magier() {
        String k = Klassen.MAGIER, cap = "m_kettingbolt|m_meteoorregen|m_tijdsprong";
        List<BoomKnoop> l = new ArrayList<>();
        l.add(var(k, "m_ijsbollen", 4, 0, List.of(cap), "minecraft:snow_block"));
        l.add(stat(k, "m_kristalgeest", 4, 1, List.of(cap), Stats.van(Stat.WIJSHEID, 8, Stat.VADS, 120, Stat.ELEMENTWEERSTAND, 10), "minecraft:echo_shard"));
        l.add(var(k, "m_heldere_bron", 4, 2, List.of(cap), "minecraft:milk_bucket"));
        l.add(var(k, "m_ijsnova", 5, 0, List.of("m_ijsbollen"), "minecraft:powder_snow_bucket"));
        l.add(stat(k, "m_sterrenwijsheid", 5, 1, List.of("m_kristalgeest"), Stats.van(Stat.ELEMENTSCHADE, 15, Stat.COOLDOWNREDUCTIE, 8),
                "minecraft:enchanted_book"));
        l.add(var(k, "m_manastroom", 5, 2, List.of("m_heldere_bron"), "minecraft:experience_bottle"));
        return l;
    }

    private static BoomKnoop stat(String klasse, String id, int rij, int kolom, List<String> vereist, Stats bonus, String icoon) {
        Bomen.ICOON.put(id, icoon);
        Boom.MIN_LEVEL.put(id, rij == 4 ? RIJ4_LEVEL : RIJ5_LEVEL);
        return new BoomKnoop(id, klasse, rij == 4 ? 2 : 3, vereist, rij, kolom, bonus, null);
    }

    private static BoomKnoop var(String klasse, String id, int rij, int kolom, List<String> vereist, String icoon) {
        Bomen.ICOON.put(id, icoon);
        Boom.MIN_LEVEL.put(id, rij == 4 ? RIJ4_LEVEL : RIJ5_LEVEL);
        return new BoomKnoop(id, klasse, rij == 4 ? 2 : 3, vereist, rij, kolom, null, id);
    }

    /** Adds the nodes to a registered class's tree (once). */
    static void voegToe(String klasse, List<BoomKnoop> knopen) {
        Klasse k = Klassen.van(klasse);
        if (k == null) {
            return;
        }
        List<BoomKnoop> boom = k.boom();
        for (BoomKnoop b : knopen) {
            if (Boom.knoop(k, b.id()) == null) {
                boom.add(b);   // (the klassen slice's trees are ArrayLists)
            }
        }
    }

    static void init() {
        voegToe(Klassen.RIDDER, ridder());
        voegToe(Klassen.JAGER, jager());
        voegToe(Klassen.MAGIER, magier());
        Schade.VOOR_KLAP.add(Boomrijen::voorKlap);
        Schade.NA_KLAP.add(Boomrijen::naKlap);
        Schade.NA_HEAL.add(Boomrijen::naHeal);
        Haken.STATUS.add(Boomrijen::status);
        Haken.VERSLAGEN.add(Boomrijen::verslagen);
        Vaardigheden.COOLDOWN_MOD.add((p, v) -> {
            int min = switch (v.id()) {
                case "stormsprong" -> Spelers.heeftKnoop(p, "r_galmstoot") ? 40 : 0;
                case "pluizenregen" -> Spelers.heeftKnoop(p, "j_splinterregen") ? 30 : 0;
                default -> 0;
            };
            return min <= 0 || v.cooldownTicks() <= 0 ? null : (float) Math.max(0, v.cooldownTicks() - min) / v.cooldownTicks();
        });
    }

    static boolean heeft(LivingEntity e, String knoop) {
        return e instanceof ServerPlayer p && Spelers.heeftKnoop(p, knoop);
    }

    static double voorKlap(Schade.Klap k, double n) {
        String id = k.skillId() == null ? "" : k.skillId();
        LivingEntity bron = k.bron(), doel = k.doel();
        if (bron instanceof ServerPlayer p && k.soort() != Schade.Soort.WAAR) {
            if (id.equals("stormsprong") && Bevroren.heeft(doel) && Spelers.heeftKnoop(p, "r_galmstoot")) {
                n *= 2;
            }
            if (id.equals("pluizenregen") && Bevroren.heeft(doel) && Spelers.heeftKnoop(p, "j_splinterregen")) {
                n *= 1.6;
            }
            if (doel instanceof VadsrijkMob m && m.maxVads() > 0 && m.vads() / m.maxVads() < 0.35 && Spelers.heeftKnoop(p, "j_jachtinstinct")) {
                n *= 1.2;
            }
        }
        return n;
    }

    static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        String id = k.skillId() == null ? "" : k.skillId();
        if (u.schade() > 0 && k.bron() instanceof ServerPlayer p && k.doel() instanceof VadsrijkMob m) {
            if (id.equals("kussenklap") && Spelers.heeftKnoop(p, "r_ijsklap")) {
                Bevroren.geef(m, 50, p);
            }
            if (id.equals("basis") && k.soort() == Schade.Soort.AFSTAND && Spelers.heeftKnoop(p, "j_ijspijl") && p.getRandom().nextDouble() < 0.15) {
                Bevroren.geef(m, 40, p);
            }
            if (id.equals("basis") && k.soort() == Schade.Soort.MAGIE && Spelers.heeftKnoop(p, "m_ijsbollen") && p.getRandom().nextDouble() < 0.20) {
                Bevroren.geef(m, 40, p);
            }
            if (id.equals("meteoorguh") && Spelers.heeftKnoop(p, "m_ijsnova")) {
                Bevroren.geef(m, 80, p);
                long nu = p.level().getGameTime();
                if (!Long.valueOf(nu).equals(NOVA.get(p.getUUID()))) {
                    NOVA.put(p.getUUID(), nu);
                    var d = Spelers.data(p);
                    d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + 30);
                    Spelers.sync(p);
                }
            }
        }
        // Ijzeren knuffel: you dropped below 30 %
        if (u.schade() > 0 && k.doel() instanceof ServerPlayer p && Spelers.heeftKnoop(p, "r_ijzeren_knuffel")
                && Spelers.vads(p) < Spelers.maxVads(p) * 0.30 && Spelers.vads(p) > 0) {
            long nu = p.level().getGameTime();
            Long klaar = IJZER.get(p.getUUID());
            if (klaar == null || klaar <= nu) {
                IJZER.put(p.getUUID(), nu + 20 * 60);
                Statussen.geef(p, Status.KNUFFELSCHILD, 100, Spelers.maxVads(p) * 0.30, p);
                if (p.level() instanceof ServerLevel sl) {
                    sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.6f);
                    sl.sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.7, 0.5, 0.05);
                }
            }
        }
    }

    private static final Map<UUID, Long> NOVA = new HashMap<>();

    /** Heldere bron: the magier's heals wash the debuffs away. */
    static void naHeal(LivingEntity bron, LivingEntity doel, double n) {
        if (bron != null && heeft(bron, "m_heldere_bron") && doel instanceof ServerPlayer) {
            for (Status s : new Status[] {Status.BRANDEND, Status.GESPOREN, Status.PLAKKERIG, Status.BEVROREN}) {
                Statussen.wis(doel, s);
            }
        }
    }

    /** Onverzettelijk: a debuff on you lasts at most half as long. */
    static void status(LivingEntity e, Status s) {
        if (!s.buff && s != Status.DUIZELIG && heeft(e, "r_onverzettelijk")) {
            Statussen.Actief a = Statussen.actief(e, s);
            if (a != null) {
                a.ticks = Math.max(1, a.ticks / 2);
            }
        }
    }

    /** Windloper (Rolsalto ready again) and Manastroom (+8 energy) on every defeat you helped with. */
    static void verslagen(Haken.Verslagen v) {
        for (ServerPlayer p : v.helpers()) {
            boolean sync = false;
            if (Spelers.heeftKnoop(p, "j_windloper") && Spelers.data(p).klaarOp.remove("rolsalto") != null) {
                sync = true;
            }
            if (Spelers.heeftKnoop(p, "m_manastroom")) {
                var d = Spelers.data(p);
                d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + 8);
                sync = true;
            }
            if (sync) {
                Spelers.sync(p);
            }
        }
    }
}
