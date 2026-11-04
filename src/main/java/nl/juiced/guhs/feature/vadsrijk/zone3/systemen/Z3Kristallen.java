package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * Zone-3 socket crystals (ZONE3_PLAN §9.4), on top of the zone-2 gem system ({@link Kristallen}: same qualities, same juwelier,
 * never lost). Four kinds, items {@code guhs:vr_z3_kristal_<soort>}:
 * <ul>
 *   <li>Amberroze: Levensroof +1 % (the pink resin of the trunk);</li>
 *   <li>Honingkristal: Heal-kracht +4 %;</li>
 *   <li>Wortelkristal: Verdediging +7;</li>
 *   <li>Schemerkristal: Cooldownreductie +1.5 %.</li>
 * </ul>
 * Sockets: zone-3 gear (lv 30+) rolls 0-2 like zone 2, and from lv 45 a rare 3rd ({@link #sokjes}). Zone-3 foes (lv 30+, in the
 * zone box) drop them: normal 8 % ruw, elite 35 % geslepen, boss always stralend (per helper), one of the 4 zone-3 kinds.
 */
public final class Z3Kristallen {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final List<String> SOORTEN = List.of("amberroze", "honingkristal", "wortelkristal", "schemerkristal");
    public static final int ZONE3_LEVEL = 30, DRIE_LEVEL = 45;
    private static Rollen.SokjesRol oud;

    static {
        soort(new Kristallen.Soort("amberroze", Stat.LEVENSROOF, 1.0, 0xFF9A6A, "vr_z3_kristal_amberroze"));
        soort(new Kristallen.Soort("honingkristal", Stat.HEALKRACHT, 4, 0xFFC830, "vr_z3_kristal_honingkristal"));
        soort(new Kristallen.Soort("wortelkristal", Stat.VERDEDIGING, 7, 0x6AC8A0, "vr_z3_kristal_wortelkristal"));
        soort(new Kristallen.Soort("schemerkristal", Stat.COOLDOWNREDUCTIE, 1.5, 0x9A7AE8, "vr_z3_kristal_schemerkristal"));
    }

    private Z3Kristallen() {
    }

    private static void soort(Kristallen.Soort s) {
        Kristallen.SOORTEN.put(s.id(), s);
        Kristallen.ITEM.put(s.id(), ITEMS.registerItem(s.itemId(), p -> new Kristallen.KristalItem(p, s.id()), () -> new Item.Properties().stacksTo(64)));
    }

    /** Wraps the zone-2 socket roll (call after Kristallen.init). */
    static void init() {
        oud = Rollen.SOKJES;
        Rollen.SOKJES = Z3Kristallen::sokjes;
        Haken.VERSLAGEN.add(Z3Kristallen::drop);
    }

    /** Lv < 30: the zone-1/2 rules. Lv 30+: 0-2 (rarer = more); lv 45+: also a 3rd (rare; Guhddelijk always 3). */
    public static int sokjes(BasisType t, int level, Zeldzaamheid z, RandomSource r) {
        if (level < ZONE3_LEVEL) {
            return oud == null ? 0 : oud.aantal(t, level, z, r);
        }
        double[] een = {0.35, 0.50, 0.65, 0.85, 1.0, 1.0};
        double[] twee = {0.0, 0.10, 0.22, 0.40, 0.60, 1.0};
        double[] drie = {0.0, 0.0, 0.04, 0.10, 0.20, 1.0};
        int i = z.ordinal();
        if (r.nextDouble() >= een[i]) {
            return 0;
        }
        if (r.nextDouble() >= twee[i]) {
            return 1;
        }
        return level >= DRIE_LEVEL && r.nextDouble() < drie[i] ? 3 : 2;
    }

    static void drop(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        if (m.mobLevel() < ZONE3_LEVEL || !Z3Lagen.inZone3(m.getX(), m.getZ())) {
            return;
        }
        boolean baas = m instanceof VadsrijkBaas;
        int q = Kristallen.dropKwaliteit(m.elite(), baas);
        double kans = baas ? 1.0 : m.elite() ? 0.35 : 0.08;
        for (ServerPlayer p : v.helpers()) {
            if (p.getRandom().nextDouble() >= kans) {
                continue;
            }
            ItemStack s = Kristallen.stack(SOORTEN.get(p.getRandom().nextInt(SOORTEN.size())), q, 1);
            LootBundel.laatVallen(p, m.position().add(0, 0.5, 0), new LootTabel.Resultaat(List.of(s), 0), q);
        }
    }
}
