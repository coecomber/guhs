package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * U-HUIZEN: everything of Het Knuffelkwartier that must survive a restart, SavedData {@code guhs:vr_huizen} (with the overworld).
 * <ul>
 *   <li>{@link #wijken}: how many wards exist (built once, then the blocks live in the dimension);</li>
 *   <li>{@link #huizen}: plot key ({@code wijk * 24 + nr}) -> {@link Huis} (only sold plots);</li>
 *   <li>one house per ACCOUNT (the Minecraft player, shared by your characters): {@link #huisVan};</li>
 *   <li>{@link #terug}: where each player came from (a Vadsrijk hub), {@link #thuisUit}: logged out inside the own house at,
 *   {@link #postvak}: deco that came out of a house (move/sell/reset) waiting at the mailbox.</li>
 * </ul>
 * Only the server thread changes it; every change calls {@link #setDirty()}.
 */
public final class HuizenData extends SavedData {
    public static final SavedDataType<HuizenData> TYPE = GuhSavedData.tagType("vr_huizen", HuizenData::new, HuizenData::load, HuizenData::save);

    public enum Deur {
        /** Everybody may come in (and look). */
        OPEN,
        /** Only friends (and the owner). */
        VRIENDEN,
        /** Only the owner. */
        SLOT;

        public String key() {
            return "vadsrijk.huizen.deur." + name().toLowerCase(java.util.Locale.ROOT);
        }

        public Deur volgende() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** One sold plot. */
    public static final class Huis {
        public UUID eigenaar;
        public String naam = "?";
        public HuisSoort soort = HuisSoort.HUISJE;
        public int variant;
        public Deur deur = Deur.VRIENDEN;
        public boolean vriendenInrichten;
        /** Friend uuid -> name. */
        public final Map<UUID, String> vrienden = new LinkedHashMap<>();
        /** Blocks the owner (or a friend) placed: the only ones that may be broken (BlockPos.asLong). */
        public final Set<Long> geplaatst = new LinkedHashSet<>();
        /** Boss trophies ("baaskill:&lt;entity type&gt;"), collected from every character that visits. */
        public final Set<String> trofeeen = new LinkedHashSet<>();
        /** The mount that idles in the stable (soort id, colour, looks) and the pets that roam the garden. */
        public String rijdier = "", rijdierKleur = "", rijdierUiterlijk = "";
        public final List<String> huisdieren = new ArrayList<>();
        /** What was paid in total (for selling back). */
        public long betaald;

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
            t.putString("Naam", naam);
            t.putString("Soort", soort.id());
            t.putInt("Variant", variant);
            t.putString("Deur", deur.name());
            t.putBoolean("VriendenInrichten", vriendenInrichten);
            ListTag v = new ListTag();
            vrienden.forEach((id, n) -> {
                CompoundTag c = new CompoundTag();
                c.store("Id", UUIDUtil.CODEC, id);
                c.putString("Naam", n);
                v.add(c);
            });
            t.put("Vrienden", v);
            long[] g = new long[geplaatst.size()];
            int i = 0;
            for (long l : geplaatst) {
                g[i++] = l;
            }
            t.putLongArray("Geplaatst", g);
            ListTag tr = new ListTag();
            trofeeen.forEach(s -> tr.add(StringTag.valueOf(s)));
            t.put("Trofeeen", tr);
            t.putString("Rijdier", rijdier);
            t.putString("RijdierKleur", rijdierKleur);
            t.putString("RijdierUiterlijk", rijdierUiterlijk);
            ListTag h = new ListTag();
            huisdieren.forEach(s -> h.add(StringTag.valueOf(s)));
            t.put("Huisdieren", h);
            t.putLong("Betaald", betaald);
            return t;
        }

        @Nullable
        public static Huis load(CompoundTag t) {
            UUID e = t.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
            if (e == null) {
                return null;
            }
            Huis h = new Huis();
            h.eigenaar = e;
            h.naam = t.getStringOr("Naam", "?");
            HuisSoort s = HuisSoort.van(t.getStringOr("Soort", "huisje"));
            h.soort = s == null ? HuisSoort.HUISJE : s;
            h.variant = t.getIntOr("Variant", 0);
            try {
                h.deur = Deur.valueOf(t.getStringOr("Deur", "VRIENDEN"));
            } catch (IllegalArgumentException x) {
                h.deur = Deur.VRIENDEN;
            }
            h.vriendenInrichten = t.getBooleanOr("VriendenInrichten", false);
            ListTag v = t.getListOrEmpty("Vrienden");
            for (int i = 0; i < v.size(); i++) {
                CompoundTag c = v.getCompoundOrEmpty(i);
                c.read("Id", UUIDUtil.CODEC).ifPresent(id -> h.vrienden.put(id, c.getStringOr("Naam", "?")));
            }
            for (long l : t.getLongArray("Geplaatst").orElse(new long[0])) {
                h.geplaatst.add(l);
            }
            ListTag tr = t.getListOrEmpty("Trofeeen");
            for (int i = 0; i < tr.size(); i++) {
                tr.getString(i).ifPresent(h.trofeeen::add);
            }
            h.rijdier = t.getStringOr("Rijdier", "");
            h.rijdierKleur = t.getStringOr("RijdierKleur", "");
            h.rijdierUiterlijk = t.getStringOr("RijdierUiterlijk", "");
            ListTag hd = t.getListOrEmpty("Huisdieren");
            for (int i = 0; i < hd.size(); i++) {
                hd.getString(i).ifPresent(h.huisdieren::add);
            }
            h.betaald = t.getLongOr("Betaald", 0);
            return h;
        }
    }

    public int wijken;
    public final Map<Integer, Huis> huizen = new HashMap<>();
    public final Map<UUID, Integer> huisVan = new HashMap<>();
    public final Map<UUID, CompoundTag> terug = new HashMap<>();
    public final Map<UUID, Long> thuisUit = new HashMap<>();
    public final Map<UUID, ListTag> postvak = new HashMap<>();

    public static HuizenData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public Huis huis(int sleutel) {
        return huizen.get(sleutel);
    }

    /** The house of this account (null: none). */
    @Nullable
    public Integer sleutelVan(UUID speler) {
        return huisVan.get(speler);
    }

    @Nullable
    public Huis huisVan(UUID speler) {
        Integer k = huisVan.get(speler);
        return k == null ? null : huizen.get(k);
    }

    public boolean vrij(int sleutel) {
        return !huizen.containsKey(sleutel);
    }

    /** The first free plot of the lowest ward that has one; -1 when every existing ward is full (a new ward is needed). */
    public int eersteVrije() {
        for (int w = 0; w < wijken; w++) {
            for (int i = 0; i < WijkPlan.KAVELS; i++) {
                if (vrij(w * WijkPlan.KAVELS + i)) {
                    return w * WijkPlan.KAVELS + i;
                }
            }
        }
        return -1;
    }

    /** The ward a newcomer should go to: the first with a free plot (or the next new ward number). */
    public int wijkMetPlek() {
        int k = eersteVrije();
        return k < 0 ? wijken : Math.floorDiv(k, WijkPlan.KAVELS);
    }

    public int bezet(int wijk) {
        int n = 0;
        for (int i = 0; i < WijkPlan.KAVELS; i++) {
            if (!vrij(wijk * WijkPlan.KAVELS + i)) {
                n++;
            }
        }
        return n;
    }

    /** Claims a plot for an account (no blocks: {@link Kwartier#koop} builds). False: taken, or the account already has one. */
    public boolean claim(int sleutel, UUID speler, String naam, HuisSoort s, int variant, long betaald) {
        if (!vrij(sleutel) || huisVan.containsKey(speler)) {
            return false;
        }
        Huis h = new Huis();
        h.eigenaar = speler;
        h.naam = naam;
        h.soort = s;
        h.variant = variant;
        h.betaald = betaald;
        huizen.put(sleutel, h);
        huisVan.put(speler, sleutel);
        setDirty();
        return true;
    }

    public void geefOp(int sleutel) {
        Huis h = huizen.remove(sleutel);
        if (h != null) {
            huisVan.remove(h.eigenaar);
        }
        setDirty();
    }

    /** Houses where this player is a friend. */
    public List<Integer> vriendVan(UUID speler) {
        List<Integer> l = new ArrayList<>();
        huizen.forEach((k, h) -> {
            if (h.vrienden.containsKey(speler)) {
                l.add(k);
            }
        });
        l.sort(Integer::compare);
        return l;
    }

    public void bewaarVoorPost(UUID speler, CompoundTag item) {
        postvak.computeIfAbsent(speler, x -> new ListTag()).add(item);
        setDirty();
    }

    // ---- save / load --------------------------------------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Wijken", wijken);
        ListTag hz = new ListTag();
        huizen.forEach((k, h) -> {
            CompoundTag c = h.save();
            c.putInt("Kavel", k);
            hz.add(c);
        });
        t.put("Huizen", hz);
        ListTag tr = new ListTag();
        terug.forEach((id, c) -> {
            CompoundTag x = c.copy();
            x.store("Speler", UUIDUtil.CODEC, id);
            tr.add(x);
        });
        t.put("Terug", tr);
        ListTag tu = new ListTag();
        thuisUit.forEach((id, n) -> {
            CompoundTag x = new CompoundTag();
            x.store("Speler", UUIDUtil.CODEC, id);
            x.put("Tijd", LongTag.valueOf(n));
            tu.add(x);
        });
        t.put("ThuisUit", tu);
        ListTag pv = new ListTag();
        postvak.forEach((id, l) -> {
            CompoundTag x = new CompoundTag();
            x.store("Speler", UUIDUtil.CODEC, id);
            x.put("Items", l.copy());
            pv.add(x);
        });
        t.put("Postvak", pv);
        return t;
    }

    public static HuizenData load(CompoundTag t) {
        HuizenData d = new HuizenData();
        d.wijken = t.getIntOr("Wijken", 0);
        ListTag hz = t.getListOrEmpty("Huizen");
        Set<UUID> gezien = new HashSet<>();
        for (int i = 0; i < hz.size(); i++) {
            CompoundTag c = hz.getCompoundOrEmpty(i);
            Huis h = Huis.load(c);
            int k = c.getIntOr("Kavel", -1);
            if (h != null && k >= 0 && gezien.add(h.eigenaar)) {   // (one house per account, also after a bad edit)
                d.huizen.put(k, h);
                d.huisVan.put(h.eigenaar, k);
                d.wijken = Math.max(d.wijken, k / WijkPlan.KAVELS + 1);
            }
        }
        ListTag tr = t.getListOrEmpty("Terug");
        for (int i = 0; i < tr.size(); i++) {
            CompoundTag c = tr.getCompoundOrEmpty(i);
            c.read("Speler", UUIDUtil.CODEC).ifPresent(id -> {
                CompoundTag x = c.copy();
                x.remove("Speler");
                d.terug.put(id, x);
            });
        }
        ListTag tu = t.getListOrEmpty("ThuisUit");
        for (int i = 0; i < tu.size(); i++) {
            CompoundTag c = tu.getCompoundOrEmpty(i);
            c.read("Speler", UUIDUtil.CODEC).ifPresent(id -> d.thuisUit.put(id, c.getLongOr("Tijd", 0)));
        }
        ListTag pv = t.getListOrEmpty("Postvak");
        for (int i = 0; i < pv.size(); i++) {
            CompoundTag c = pv.getCompoundOrEmpty(i);
            c.read("Speler", UUIDUtil.CODEC).ifPresent(id -> d.postvak.put(id, c.getListOrEmpty("Items").copy()));
        }
        return d;
    }
}
