package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.WezensLoot;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;

/**
 * Zone-2 loot from the creatures (lv 20-35): 15 base types (items guhs:vr_z2_mob_&lt;id&gt;):
 * <ul>
 *   <li>4 loose armour pieces (sneeuwmuts, galmpantser, schotsbroek, ijzelslofjes);</li>
 *   <li>the <b>Kristalset</b> (4, Verdediging/Wijsheid) and the <b>Mika-buitset</b> (4, Kracht/Behendigheid), see {@link Z2Sets};</li>
 *   <li>3 pieces of jewellery (kern Slot RING/KETTING/OORBEL): kristalring, spiegelketting, galmoorbel.</li>
 * </ul>
 * A table per creature "z2wezens:&lt;naam&gt;" + a richer "_elite" one (the zone-1 weapons via {@link WezensLoot#extraWapens}, kern's
 * jewellery in the elite tables). Plus a raw crystal for gear sockets now and then ({@link #kristal}): those items are
 * z2-systemen's ({@code guhs:vr_z2_kristal_<soort>}); a crystal whose item does not exist (yet) is simply skipped.
 */
public final class Z2Loot {
    /** id -> slot (the generator tools/features/vadsrijk_z2_wezens.py has the same list: HARNAS). */
    public static final Map<String, Slot> STUKKEN = new LinkedHashMap<>();

    static {
        STUKKEN.put("sneeuwmuts", Slot.HOOFD);
        STUKKEN.put("galmpantser", Slot.BORST);
        STUKKEN.put("schotsbroek", Slot.BENEN);
        STUKKEN.put("ijzelslofjes", Slot.VOETEN);
        STUKKEN.put("kristalkroon", Slot.HOOFD);
        STUKKEN.put("kristalborstplaat", Slot.BORST);
        STUKKEN.put("kristalbeenstukken", Slot.BENEN);
        STUKKEN.put("kristalschoentjes", Slot.VOETEN);
        STUKKEN.put("mijnhelm", Slot.HOOFD);
        STUKKEN.put("buitvest", Slot.BORST);
        STUKKEN.put("boorbroek", Slot.BENEN);
        STUKKEN.put("kaaslaarzen", Slot.VOETEN);
        STUKKEN.put("kristalring", Slot.RING);
        STUKKEN.put("spiegelketting", Slot.KETTING);
        STUKKEN.put("galmoorbel", Slot.OORBEL);
    }

    /**
     * The raw crystals for sockets (z2-systemen's items guhs:vr_z2_kristal_&lt;soort&gt;; DEPENDENCY, see NOTITIES_Z2_WEZENS) and
     * which creatures drop which: crystal creatures their own colour, Mika's what they mined.
     */
    public static final List<String> KRISTALLEN = List.of("roze", "ijs", "galm", "grauw", "spiegel");

    private Z2Loot() {
    }

    public static String itemId(String basis) {
        return "guhs:vr_z2_mob_" + basis;
    }

    public static boolean sieraad(String id) {
        return STUKKEN.get(id) != null && STUKKEN.get(id).sieraad();
    }

    /** An armour piece; base stats ~ 2.6x zone 1 (level 20-35 gear), growth per level like zone 1. */
    private static BasisType stuk(String id, double vd, double vads, Object... extra) {
        Stats basis = Stats.van(Stat.VERDEDIGING, vd, Stat.VADS, vads);
        Stats groei = Stats.van(Stat.VERDEDIGING, vd * 0.25, Stat.VADS, vads * 0.2);
        voegToe(basis, groei, extra);
        return new BasisType(id, itemId(id), STUKKEN.get(id), "harnas", null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    /** A piece of jewellery: only side stats (growing a little per level). */
    private static BasisType sieraad(String id, Object... stats) {
        Stats basis = new Stats(), groei = new Stats();
        voegToe(basis, groei, stats);
        return BasisType.sieraad(id, itemId(id), STUKKEN.get(id), basis, groei, 3);
    }

    private static void voegToe(Stats basis, Stats groei, Object... extra) {
        for (int i = 0; i + 1 < extra.length; i += 2) {
            Stat s = (Stat) extra[i];
            double v = ((Number) extra[i + 1]).doubleValue();
            basis.plus(s, v);
            groei.plus(s, v * (s.hoofd() ? 0.25 : 0.06));
        }
    }

    /** From Z2WezensRegistry.register. */
    public static void basisTypes() {
        BasisTypes.registreer(stuk("sneeuwmuts", 8, 26, Stat.ELEMENTWEERSTAND, 6));
        BasisTypes.registreer(stuk("galmpantser", 14, 42, Stat.VERDEDIGING, 4));
        BasisTypes.registreer(stuk("schotsbroek", 12, 36, Stat.VADS, 10));
        BasisTypes.registreer(stuk("ijzelslofjes", 6, 22, Stat.LOOPSNELHEID, 6));
        // Kristalset: verdediging + wijsheid, cold-proof
        BasisTypes.registreer(stuk("kristalkroon", 9, 28, Stat.WIJSHEID, 6, Stat.ELEMENTWEERSTAND, 4));
        BasisTypes.registreer(stuk("kristalborstplaat", 15, 46, Stat.WIJSHEID, 7));
        BasisTypes.registreer(stuk("kristalbeenstukken", 12, 38, Stat.WIJSHEID, 6));
        BasisTypes.registreer(stuk("kristalschoentjes", 7, 24, Stat.WIJSHEID, 4, Stat.ELEMENTWEERSTAND, 3));
        // Mika-buitset: kracht + behendigheid, a little crit
        BasisTypes.registreer(stuk("mijnhelm", 8, 25, Stat.KRACHT, 5, Stat.KRITIEK, 2));
        BasisTypes.registreer(stuk("buitvest", 13, 40, Stat.KRACHT, 6, Stat.BEHENDIGHEID, 4));
        BasisTypes.registreer(stuk("boorbroek", 11, 34, Stat.BEHENDIGHEID, 6));
        BasisTypes.registreer(stuk("kaaslaarzen", 6, 22, Stat.BEHENDIGHEID, 4, Stat.LOOPSNELHEID, 4));
        // jewellery
        BasisTypes.registreer(sieraad("kristalring", Stat.ELEMENTSCHADE, 6, Stat.KRITIEK, 2));
        BasisTypes.registreer(sieraad("spiegelketting", Stat.ELEMENTWEERSTAND, 7, Stat.VADS, 20));
        BasisTypes.registreer(sieraad("galmoorbel", Stat.KRITIEKSCHADE, 8, Stat.AANVALSSNELHEID, 3));
    }

    private static List<LootTabel.Regel> regels(Object... paren) {
        List<LootTabel.Regel> l = new ArrayList<>();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            l.add(new LootTabel.Regel((String) paren[i], (Integer) paren[i + 1]));
        }
        l.addAll(WezensLoot.extraWapens);
        return l;
    }

    private static void tabel(String mob, List<LootTabel.Regel> r, double kans, int muntMin, int muntMax) {
        LootTabellen.registreer("z2wezens:" + mob, new LootTabel(r, 1, kans, 1.2, muntMin, muntMax));
        List<LootTabel.Regel> elite = new ArrayList<>(r);
        for (String s : WezensLoot.SIERADEN) {
            elite.add(new LootTabel.Regel(s, 1));
        }
        elite.add(new LootTabel.Regel("kristalring", 2));
        elite.add(new LootTabel.Regel("spiegelketting", 2));
        elite.add(new LootTabel.Regel("galmoorbel", 2));
        LootTabellen.registreer("z2wezens:" + mob + "_elite", new LootTabel(elite, 2, Math.min(1, kans * 3.5), 2.8, muntMin * 3, muntMax * 3));
    }

    /** One table per creature (after {@link #basisTypes}). */
    public static void tabellen() {
        List<LootTabel.Regel> galm = regels("sneeuwmuts", 4, "galmpantser", 3, "ijzelslofjes", 3, "schotsbroek", 1, "kristalkroon", 1, "kristalschoentjes", 1,
                "galmoorbel", 1);
        List<LootTabel.Regel> meer = regels("schotsbroek", 4, "ijzelslofjes", 2, "sneeuwmuts", 2, "kristalkroon", 2, "kristalborstplaat", 2,
                "kristalbeenstukken", 2, "kristalschoentjes", 2, "spiegelketting", 1);
        List<LootTabel.Regel> mika = regels("mijnhelm", 3, "buitvest", 3, "boorbroek", 3, "kaaslaarzen", 3, "galmpantser", 1, "kristalring", 1);
        List<LootTabel.Regel> kloof = regels("galmpantser", 2, "schotsbroek", 2, "mijnhelm", 1, "buitvest", 1, "kristalborstplaat", 2, "kristalbeenstukken", 2,
                "kristalring", 1);
        List<LootTabel.Regel> burcht = regels("kristalkroon", 3, "kristalborstplaat", 3, "kristalbeenstukken", 3, "kristalschoentjes", 3, "spiegelketting", 2,
                "kristalring", 1, "galmoorbel", 1);
        tabel("splinterling", galm, 0.2, 18, 40);
        tabel("galmvleermuis", galm, 0.18, 16, 36);
        tabel("sneeuwknabbelaar", galm, 0.14, 12, 28);
        tabel("kristalnaaldkruiper", galm, 0.2, 18, 40);
        tabel("ijzelgeest", meer, 0.2, 22, 46);
        tabel("schotsbijter", meer, 0.24, 26, 52);
        tabel("versteende_wacht", meer, 0.3, 32, 64);
        tabel("ijspuffer", meer, 0.18, 20, 44);
        tabel("mika_mijnwerker", mika, 0.22, 30, 60);
        tabel("mika_boorder", mika, 0.24, 30, 62);
        tabel("kloofgier", kloof, 0.22, 28, 58);
        tabel("kristalspin", kloof, 0.22, 28, 58);
        tabel("spiegelwacht", burcht, 0.32, 40, 80);
        tabel("scherfgeest", burcht, 0.28, 36, 72);
    }

    /** Which crystal (soort) a creature drops, or null for none at all. */
    public static String kristalSoort(Z2Wezen w, RandomSource r) {
        return switch (w.naam()) {
            case "splinterling", "kristalnaaldkruiper", "versteende_wacht", "kristalspin" -> r.nextInt(4) == 0 ? "roze" : "grauw";
            case "galmvleermuis" -> "galm";
            case "ijzelgeest", "ijspuffer", "schotsbijter", "sneeuwknabbelaar" -> r.nextInt(5) == 0 ? "galm" : "ijs";
            case "mika_mijnwerker", "mika_boorder" -> KRISTALLEN.get(r.nextInt(KRISTALLEN.size()));
            case "kloofgier" -> "grauw";
            case "spiegelwacht", "scherfgeest" -> r.nextInt(3) == 0 ? "roze" : "spiegel";
            default -> null;
        };
    }

    /** The chance of a crystal per helper: 8 % (Mika's: what they mined, 12 %), elites 35 %. */
    public static double kristalKans(Z2Wezen w) {
        double k = w.aard() == Z2Wezen.Aard.MIKA ? 0.12 : 0.08;
        return w.elite() ? 0.35 : k;
    }

    /** A raw crystal for this helper, or EMPTY. Integration: always EMPTY: z2-systemen drops the crystals itself for every
     * defeated lv 20+ creature in the zone (Haken.VERSLAGEN, its own soorten/qualities), so dropping here too would double them. */
    public static ItemStack kristal(Z2Wezen w, RandomSource r) {
        if (true) {
            return ItemStack.EMPTY;
        }
        if (w.klein() || r.nextDouble() >= kristalKans(w)) {
            return ItemStack.EMPTY;
        }
        String soort = kristalSoort(w, r);
        return soort == null ? ItemStack.EMPTY : kristalItem(soort);
    }

    public static ItemStack kristalItem(String soort) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse("guhs:vr_z2_kristal_" + soort)).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
