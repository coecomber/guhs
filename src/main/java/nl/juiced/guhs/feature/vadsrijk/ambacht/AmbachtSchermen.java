package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten.Ingredient;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten.Recept;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * The server side of the Ambachten screens: the Ambachten book ({@code boek}, tabs overzicht / recepten / bestel / rang / plekken;
 * the key and the NPCs open it) and a workshop ({@code werkplaats}: only at its guh, or by the dev command). The client draws
 * what comes in the data; every action comes back through {@link #doe}.
 */
public final class AmbachtSchermen {
    public static final List<String> TABS = List.of("overzicht", "recepten", "bestel", "rang", "plekken");
    /** The workshop a player may use now: ambacht + where its guh stands (null pos = the dev command: anywhere). */
    private record Open(Ambacht ambacht, @Nullable BlockPos bij) {
    }

    private static final Map<UUID, Open> WERK = new ConcurrentHashMap<>();

    private AmbachtSchermen() {
    }

    static void vergeet(UUID id) {
        WERK.remove(id);
    }

    // ---- actions ---------------------------------------------------------------------------------------------------------------

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        switch (actie) {
            case "oogst" -> Verzamelen.oogst(p, a.getStringOr("spot", ""), a.getIntOr("score", 0), a.getBooleanOr("over", false));
            case "boek" -> boek(p, a.getStringOr("tab", "overzicht"), Ambacht.van(a.getStringOr("ambacht", "mijnen")), a.getStringOr("lijst", ""), "");
            case "claim" -> boek(p, "bestel", null, "", Bestelbord.claim(p, a.getIntOr("slot", -1)));
            case "ruil" -> boek(p, "bestel", null, "", Bestelbord.ruil(p, a.getIntOr("slot", -1)));
            case "maak" -> {
                Open o = WERK.get(p.getUUID());
                Recept r = Recepten.van(a.getStringOr("recept", ""));
                String m;
                if (o == null || r == null || r.ambacht() != o.ambacht()) {
                    m = "vadsrijk.ambacht.werk.niet_hier";
                } else if (o.bij() != null && o.bij().distSqr(p.blockPosition()) > 12 * 12) {
                    m = "vadsrijk.ambacht.werk.te_ver";
                } else {
                    List<Stat> keuzes = new ArrayList<>();
                    for (String s : a.getStringOr("keuzes", "").split(",")) {
                        try {
                            if (!s.isBlank()) {
                                keuzes.add(Stat.van(s));
                            }
                        } catch (IllegalArgumentException e) {
                            // (ignored)
                        }
                    }
                    m = Werkplaats.maak(p, r.id(), keuzes, a.getStringOr("eigenschap", ""), a.getStringOr("kristal", ""), a.getIntOr("score", 0),
                            a.getIntOr("aantal", 1));
                }
                if (o != null) {
                    stuurWerk(p, o.ambacht(), m, r == null ? "" : r.id());
                }
            }
            case "dicht" -> {
            }
            default -> {
            }
        }
    }

    // ---- the book --------------------------------------------------------------------------------------------------------------

    public static void boek(ServerPlayer p, String tab, @Nullable Ambacht a) {
        boek(p, tab, a, "", "");
    }

    static void boek(ServerPlayer p, String tab, @Nullable Ambacht a, String lijst, String melding) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        if (a == null) {
            a = Ambacht.MIJNEN;
        }
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        d.putString("scherm", "boek");
        d.putString("tab", TABS.contains(tab) ? tab : "overzicht");
        d.putString("ambacht", a.id);
        d.putString("melding", melding);
        d.putLong("munten", Spelers.data(p).munten);
        d.putLong("uitgerust", AmbachtData.uitgerust(p));
        d.putInt("cap", AmbachtXp.CAP);
        ListTag tegels = new ListTag();
        for (Ambacht x : Ambacht.values()) {
            CompoundTag t = new CompoundTag();
            long xp = AmbachtData.xp(p, x);
            int lv = AmbachtXp.levelVan(xp);
            t.putString("id", x.id);
            t.putInt("level", lv);
            t.putLong("in", AmbachtXp.inLevel(xp));
            t.putLong("nodig", lv >= AmbachtXp.CAP ? 0 : AmbachtXp.xpVoor(lv));
            t.putInt("verhaal", AmbachtData.verhaal(p, x));
            t.putString("icoon", x.icoon);
            t.putInt("kleur", x.kleur);
            tegels.add(t);
        }
        d.put("tegels", tegels);
        Eten.hud(p, d);
        switch (d.getStringOr("tab", "")) {
            case "recepten" -> d.put("recepten", recepten(p, a, items, false));
            case "bestel" -> bestel(p, d);
            case "rang" -> rang(p, d, lijst.isEmpty() ? a.id : lijst);
            case "plekken" -> plekken(p, d);
            default -> {
            }
        }
        AmbachtPayloads.stuur(p, d, items);
    }

    private static ListTag recepten(ServerPlayer p, Ambacht a, List<ItemStack> items, boolean werk) {
        ListTag l = new ListTag();
        for (Recept r : Recepten.van(a)) {
            CompoundTag e = new CompoundTag();
            e.putString("id", r.id());
            e.putString("naam", r.naamKey());
            e.putInt("level", r.level());
            e.putInt("tier", r.tier());
            e.putBoolean("kent", Werkplaats.kent(p, r));
            e.putString("soort", r.soort());
            e.putInt("keuzes", r.keuzes());
            e.putInt("item", items.size());
            items.add(Recepten.toon(r));
            ListTag in = new ListTag();
            for (Ingredient i : r.in()) {
                CompoundTag c = new CompoundTag();
                c.putString("naam", "item.guhs.vr_amb_" + kort(i.opties().get(0)));
                c.putBoolean("of", i.opties().size() > 1);
                c.putInt("nodig", i.aantal());
                c.putInt("heeft", MateriaalTas.tel(p, i.past()));
                c.putInt("item", items.size());
                items.add(Materialen.stack(i.opties().get(0), 0, i.aantal()));
                in.add(c);
            }
            if (r.soort().equals("slijp")) {
                CompoundTag c = new CompoundTag();
                c.putString("naam", "vadsrijk.ambacht.werk.kristal_in." + r.doel());
                c.putBoolean("of", true);
                c.putInt("nodig", 3);
                c.putInt("heeft", -1);
                c.putInt("item", items.size());
                items.add(Kristallen.stack("rozekwarts", Integer.parseInt(r.doel()), 3));
                in.add(c);
            }
            e.put("in", in);
            if (werk) {
                e.putInt("kan", Werkplaats.kan(p, r, null));
            }
            l.add(e);
        }
        return l;
    }

    private static String kort(String id) {
        return Materialen.KERN.containsKey(id) ? "kern_" + id : id;
    }

    private static void bestel(ServerPlayer p, CompoundTag d) {
        Bestelbord.vandaag(p);
        ListTag l = new ListTag();
        List<String> ids = Bestelbord.ids(p);
        for (int i = 0; i < ids.size(); i++) {
            Bestelbord.Bestelling b = Bestelbord.POOL.get(ids.get(i));
            if (b == null) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            String[] t = Bestelbord.tekstDelen(p, i);
            e.putString("key", t[0]);
            e.putString("gever", t[1]);
            e.putString("ding", t[2]);
            e.putString("ambacht", b.ambacht().id);
            e.putInt("aantal", b.aantal());
            e.putInt("voortgang", b.soort().equals("breng") ? Math.min(b.aantal(), MateriaalTas.tel(p, Bestelbord.past(b))) : Bestelbord.voortgang(p, i));
            e.putBoolean("klaar", Bestelbord.klaar(p, i));
            e.putBoolean("geclaimd", Bestelbord.geclaimd(p, i));
            l.add(e);
        }
        d.put("bestel", l);
        CompoundTag t = Bestelbord.vandaag(p);
        d.putBoolean("geruild", t.getBooleanOr("geruild", false));
        d.putInt("weekN", t.getIntOr("weekN", 0));
        d.putInt("weekDoel", Bestelbord.WEEKDOEL);
        d.putBoolean("weekKist", t.getBooleanOr("weekKist", false));
        d.putLong("reset", nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Data.secondenTotReset());
    }

    private static void rang(ServerPlayer p, CompoundTag d, String lijst) {
        Ranglijst.bij(p);
        List<Ranglijst.Plek> l = Ranglijst.get(p.level().getServer()).lijst(lijst);
        ListTag top = new ListTag();
        int eigen = -1;
        for (int i = 0; i < l.size(); i++) {
            Ranglijst.Plek x = l.get(i);
            if (x.uuid().equals(p.getUUID())) {
                eigen = i + 1;
            }
            if (i < 10) {
                CompoundTag e = new CompoundTag();
                e.putString("naam", x.naam());
                e.putInt("level", x.level());
                e.putLong("xp", x.xp());
                e.putBoolean("jij", x.uuid().equals(p.getUUID()));
                top.add(e);
            }
        }
        d.putString("lijst", lijst);
        d.put("top", top);
        d.putInt("eigen", eigen);
        d.putInt("aantal", l.size());
    }

    private static void plekken(ServerPlayer p, CompoundTag d) {
        ListTag l = new ListTag();
        for (int zone = 1; zone <= 3; zone++) {
            for (Ambacht a : Ambacht.values()) {
                if (!a.verzamel) {
                    continue;
                }
                int tot = 0, gev = 0;
                for (AmbachtPlekken.Groep g : AmbachtPlekken.GROEPEN) {
                    if (g.zone() == zone && g.ambacht() == a) {
                        tot++;
                        if (AmbachtData.heeft(p, "plekken", g.id())) {
                            gev++;
                        }
                    }
                }
                if (tot > 0) {
                    CompoundTag e = new CompoundTag();
                    e.putInt("zone", zone);
                    e.putString("ambacht", a.id);
                    e.putInt("gevonden", gev);
                    e.putInt("totaal", tot);
                    l.add(e);
                }
            }
        }
        d.put("plekken", l);
        int vis = 0, visTot = 0;
        for (Materialen.Materiaal m : Materialen.ALLE.values()) {
            if (m.ambacht() == Ambacht.VISSEN && m.tier() > 0) {
                visTot++;
                if (AmbachtData.heeft(p, "vissen", m.id())) {
                    vis++;
                }
            }
        }
        d.putInt("vissen", vis);
        d.putInt("vissenTot", visTot);
        d.putInt("honing", AmbachtData.lijst(p, "honing").size());
        d.putInt("honingTot", Recepten.HONING.size());
        d.putInt("recepten", AmbachtData.lijst(p, "gemaakt").size());
        d.putInt("receptenTot", Recepten.ALLE.size());
    }

    // ---- a workshop ---------------------------------------------------------------------------------------------------------------

    /** Opens the workshop of this ambacht (from its guh: {@code npc} is its id, the player must stay near; null = dev, anywhere). */
    public static void werkplaats(ServerPlayer p, Ambacht a, @Nullable String npc) {
        BlockPos bij = npc == null ? null : AmbachtNpcs.waar(npc);
        WERK.put(p.getUUID(), new Open(a, npc == null ? null : bij != null ? bij : p.blockPosition()));
        stuurWerk(p, a, "", "");
    }

    static void stuurWerk(ServerPlayer p, Ambacht a, String melding, String gekozen) {
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        d.putString("scherm", "werkplaats");
        d.putString("ambacht", a.id);
        d.putString("melding", melding);
        d.putString("gekozen", gekozen);
        d.putLong("munten", Spelers.data(p).munten);
        d.putInt("level", AmbachtData.level(p, a));
        d.put("recepten", recepten(p, a, items, true));
        ListTag stats = new ListTag();
        for (Stat s : Maatwerk.keuzes()) {
            stats.add(StringTag.valueOf(s.name().toLowerCase(java.util.Locale.ROOT)));
        }
        d.put("stats", stats);
        ListTag eig = new ListTag();
        for (Maatwerk.Eigenschap e : Maatwerk.Eigenschap.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.id);
            c.putString("key", e.key());
            c.putInt("procent", e.procent);
            eig.add(c);
        }
        d.put("eigenschappen", eig);
        ListTag kr = new ListTag();
        for (Kristallen.Soort s : Kristallen.SOORTEN.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", s.id());
            c.putInt("ruw", MateriaalTas.tel(p, Werkplaats.kristalVan(s.id(), 0)));
            c.putInt("geslepen", MateriaalTas.tel(p, Werkplaats.kristalVan(s.id(), 1)));
            c.putInt("item", items.size());
            items.add(Kristallen.stack(s.id(), 0, 1));
            kr.add(c);
        }
        d.put("kristallen", kr);
        AmbachtPayloads.stuur(p, d, items);
    }
}
