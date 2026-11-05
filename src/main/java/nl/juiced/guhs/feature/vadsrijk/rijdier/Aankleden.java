package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Bron;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Variant;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;

/**
 * T-RIJDIER: who owns which mount look, what each mount wears, and the sources that hand them out (plus the sources of the three new
 * mounts and the two new Pluimguh colours).
 * <p>Storage (account-wide later: it lives next to the mounts in {@code SpelerData.extra("economie")}):
 * {@code garderobe} = "vacht:knuffelhert:sneeuw,patroon:hartjes,tuig:hoedje,..." (owned looks); per mount
 * {@code rijdieren.<soort>.uiterlijk} = "vacht|patroon|tuig" (what it wears).
 * <p>APIs for other slices: {@link #geef(ServerPlayer, String)} (a look by key), {@link #SOORT_BRON} / {@link #PLUIM_BRON} (where a
 * mount / Pluimguh colour comes from), {@link #NIEUW} (fires on a new look).
 */
public final class Aankleden {
    /** Where the new mounts come from (checked like the look sources; only after your first mount, the zone-3 starter). */
    public static final Map<RijdierSoort, Bron> SOORT_BRON = new EnumMap<>(RijdierSoort.class);
    /** Where the new Pluimguh colours come from. */
    public static final Map<PluimKleur, Bron> PLUIM_BRON = new EnumMap<>(PluimKleur.class);
    /** Fires when a player gets a new look (key, e.g. "tuig:hoedje"). */
    public static final List<BiConsumer<ServerPlayer, String>> NIEUW = new CopyOnWriteArrayList<>();

    static {
        SOORT_BRON.put(RijdierSoort.DUINHUPPER, Bron.geheim("knuffels_10"));
        SOORT_BRON.put(RijdierSoort.WOLKWALKALFJE, Bron.veren(20));
        SOORT_BRON.put(RijdierSoort.ZWAMZWIJNTJE, Bron.baas("guhs:vr_baas_opperzwam"));
        PLUIM_BRON.put(PluimKleur.KRISTAL, Bron.baas("guhs:vr_z2_baas_spiegelzucht"));
        PLUIM_BRON.put(PluimKleur.LAMPION, Bron.veren(12));
    }

    private Aankleden() {
    }

    private static CompoundTag eco(ServerPlayer p) {
        return Spelers.data(p).extra("economie");
    }

    private static Set<String> garderobe(ServerPlayer p) {
        Set<String> s = new LinkedHashSet<>();
        for (String k : eco(p).getStringOr("garderobe", "").split(",")) {
            if (!k.isEmpty()) {
                s.add(k);
            }
        }
        return s;
    }

    // ---- owning -------------------------------------------------------------------------------------------------------------------

    public static boolean heeft(ServerPlayer p, Variant v) {
        if (v.bron().soort().equals("start")) {
            return v.soort() == null || Rijdieren.heeft(p, v.soort());
        }
        return garderobe(p).contains(v.sleutel());
    }

    /** Gives a look (with a chat line + chime when {@code melden}). False when unknown or already owned. */
    public static boolean geef(ServerPlayer p, Variant v, boolean melden) {
        if (heeft(p, v)) {
            return false;
        }
        Set<String> g = garderobe(p);
        g.add(v.sleutel());
        eco(p).putString("garderobe", String.join(",", g));
        if (melden) {
            Component naam = Component.translatable(v.naamKey()).withStyle(ChatFormatting.GOLD);
            p.sendSystemMessage(Component.translatable(v.soort() == null ? "vadsrijk.rijdier.nieuw." + v.as().id() : "vadsrijk.rijdier.nieuw.vacht_van",
                    naam, v.soort() == null ? Component.empty() : Component.translatable(v.soort().naamKey()),
                    Component.keybind("key.guhs.vr_rijdieren").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.5f);
        }
        for (BiConsumer<ServerPlayer, String> h : NIEUW) {
            h.accept(p, v.sleutel());
        }
        return true;
    }

    /** API: gives a look by its key ("tuig:hoedje", "patroon:glitter", "vacht:guhwolf:zon"). False when unknown/owned. */
    public static boolean geef(ServerPlayer p, String sleutel) {
        Variant v = Uiterlijk.vanSleutel(sleutel);
        return v != null && geef(p, v, true);
    }

    /** Dev: every look. */
    public static void alles(ServerPlayer p) {
        for (Variant v : Uiterlijk.alle()) {
            geef(p, v, false);
        }
    }

    /** Dev: forget the wardrobe and what every mount wears. */
    public static void reset(ServerPlayer p) {
        eco(p).remove("garderobe");
        CompoundTag r = eco(p).getCompoundOrEmpty("rijdieren");
        for (String k : r.keySet()) {
            CompoundTag m = r.getCompoundOrEmpty(k);
            m.remove("uiterlijk");
            r.put(k, m);
        }
    }

    // ---- wearing ------------------------------------------------------------------------------------------------------------------

    /** What this mount wears: "vacht|patroon|tuig" (only owned parts; "" = plain). */
    public static String uiterlijk(ServerPlayer p, RijdierSoort s) {
        String[] d = Uiterlijk.delen(eco(p).getCompoundOrEmpty("rijdieren").getCompoundOrEmpty(s.id()).getStringOr("uiterlijk", ""));
        return Uiterlijk.maak(eigen(p, s, As.VACHT, d[0]), eigen(p, s, As.PATROON, d[1]), eigen(p, s, As.TUIG, d[2]));
    }

    private static String eigen(ServerPlayer p, RijdierSoort s, As as, String id) {
        if (id.isEmpty()) {
            return "";
        }
        Variant v = Uiterlijk.van(s, as, id);
        return v != null && heeft(p, v) ? id : "";
    }

    /**
     * Puts on a look (id "" = take that part off; for the coat "" = naturel). False when the mount or the look isn't yours. The
     * mount you ride changes right away.
     */
    public static boolean kies(ServerPlayer p, RijdierSoort s, As as, String id) {
        if (!Rijdieren.heeft(p, s)) {
            return false;
        }
        if (!id.isEmpty()) {
            Variant v = Uiterlijk.van(s, as, id);
            if (v == null || !heeft(p, v)) {
                return false;
            }
        }
        String[] d = Uiterlijk.delen(uiterlijk(p, s));
        d[as.ordinal()] = as == As.VACHT && id.equals(Uiterlijk.NATUREL) ? "" : id;
        CompoundTag r = eco(p).getCompoundOrEmpty("rijdieren");
        CompoundTag m = r.getCompoundOrEmpty(s.id());
        m.putString("uiterlijk", Uiterlijk.maak(d[0], d[1], d[2]));
        r.put(s.id(), m);
        eco(p).put("rijdieren", r);
        if (p.getVehicle() instanceof RijdierEntity re && re.soort() == s) {
            re.zetUiterlijk(uiterlijk(p, s));
        }
        return true;
    }

    // ---- the sources --------------------------------------------------------------------------------------------------------------

    /** Is this (automatic) source met: an achievement, a boss's first kill, enough secrets. */
    public static boolean gehaald(ServerPlayer p, Bron b) {
        return switch (b.soort()) {
            case "prestatie", "geheim" -> Prestaties.gehaald(p, b.sleutel());
            case "baas" -> Prestaties.baasVerslagen(p, b.sleutel());
            default -> false;
        };
    }

    /**
     * Hands out everything whose automatic source is met (every 10 s in the Vadsrijk, when the mount screen opens, and from the
     * dev command). Only once you own a mount (the zone-3 starter comes first). Returns how many new things.
     */
    public static int inhalen(ServerPlayer p) {
        if (!Rijdieren.heeftEen(p)) {
            return 0;
        }
        int n = 0;
        for (Variant v : Uiterlijk.alle()) {
            if (v.bron().vanzelf() && (v.soort() == null || Rijdieren.heeft(p, v.soort())) && !heeft(p, v) && gehaald(p, v.bron())
                    && geef(p, v, true)) {
                n++;
            }
        }
        for (Map.Entry<RijdierSoort, Bron> e : SOORT_BRON.entrySet()) {
            if (e.getValue().vanzelf() && !Rijdieren.heeft(p, e.getKey()) && gehaald(p, e.getValue()) && Rijdieren.geef(p, e.getKey())) {
                n++;
            }
        }
        for (Map.Entry<PluimKleur, Bron> e : PLUIM_BRON.entrySet()) {
            if (e.getValue().vanzelf() && Rijdieren.heeft(p, RijdierSoort.PLUIMGUH) && !Rijdieren.heeftKleur(p, e.getKey())
                    && gehaald(p, e.getValue()) && Rijdieren.geefKleur(p, e.getKey())) {
                n++;
            }
        }
        return n;
    }

    /** At the stal: buys a look whose source is "stal". Returns the message key. */
    static String koop(ServerPlayer p, Variant v) {
        if (heeft(p, v)) {
            return "vadsrijk.economie.rijdier.al";
        }
        if (!v.bron().soort().equals("stal") || (v.soort() != null && !Rijdieren.heeft(p, v.soort()))) {
            return "vadsrijk.rijdier.niet_te_koop";
        }
        if (!Spelers.neemMunten(p, v.bron().waarde())) {
            return "vadsrijk.economie.smid.te_weinig";
        }
        geef(p, v, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.4f);
        return "vadsrijk.rijdier.gekocht";
    }

    /** Anywhere in the mount screen: trades Gouden Veren for a look / a mount / a Pluimguh colour whose source is "veren". */
    static String ruil(ServerPlayer p, Bron b, Runnable geven) {
        if (!b.soort().equals("veren")) {
            return "vadsrijk.rijdier.niet_te_koop";
        }
        if (!GoudenVeren.neem(p, (int) b.waarde())) {
            return "vadsrijk.rijdier.te_weinig_veren";
        }
        geven.run();
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.7f);
        return "vadsrijk.rijdier.geruild";
    }

    // ---- the mount screen (economie Stal hooks) ------------------------------------------------------------------------------------

    /**
     * Extra data for one mount row of the mount screen: {@code uiterlijk} (what it wears), {@code looks} = a list of {as, id, heeft,
     * bron, waarde, sleutel, tekst?}, {@code soortBron} (where this mount comes from, for the new ones).
     */
    public static void schermData(ServerPlayer p, RijdierSoort s, CompoundTag t) {
        t.putString("uiterlijk", Rijdieren.heeft(p, s) ? uiterlijk(p, s) : "");
        ListTag l = new ListTag();
        for (As as : As.values()) {
            for (Variant v : Uiterlijk.van(s, as)) {
                CompoundTag c = new CompoundTag();
                c.putString("as", as.id());
                c.putString("id", v.id());
                c.putBoolean("heeft", heeft(p, v));
                bron(p, v.bron(), c);
                l.add(c);
            }
        }
        t.put("looks", l);
        Bron sb = SOORT_BRON.get(s);
        if (sb != null) {
            CompoundTag c = new CompoundTag();
            bron(p, sb, c);
            t.put("soortBron", c);
        }
        if (s.kleuren()) {
            CompoundTag kb = new CompoundTag();
            for (Map.Entry<PluimKleur, Bron> e : PLUIM_BRON.entrySet()) {
                CompoundTag c = new CompoundTag();
                bron(p, e.getValue(), c);
                kb.put(e.getKey().id(), c);
            }
            t.put("kleurBron", kb);
        }
    }

    private static void bron(ServerPlayer p, Bron b, CompoundTag c) {
        c.putString("bron", b.soort());
        c.putLong("waarde", b.waarde());
        c.putString("sleutel", b.sleutel());
        c.putString("tekst", b.tekstKey());
        int[] v = Prestaties.voortgang(p, b.sleutel());
        if (v[1] > 0) {
            c.putInt("heb", v[0]);
            c.putInt("nodig", v[1]);
        }
    }

    /** The screen-wide numbers: Gouden Veren. */
    public static void schermData(ServerPlayer p, CompoundTag d) {
        d.putInt("veren", nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Data.veren(p));
    }

    /**
     * The mount screen's look actions (economie {@code Stal.doe} hands unknown actions here): "kleed" {soort, as, id} = put on,
     * "koop_look" {soort, as, id} (stal only), "ruil" {soort, as, id} (as "soort" = the mount itself, "pluimkleur" = a colour).
     * Returns the message key ("" = nothing to say), or null when the action isn't ours.
     */
    @Nullable
    public static String doe(ServerPlayer p, boolean stal, String actie, CompoundTag a) {
        RijdierSoort s = RijdierSoort.van(a.getStringOr("soort", ""));
        String asId = a.getStringOr("as", ""), id = a.getStringOr("id", "");
        As as = null;
        for (As x : As.values()) {
            if (x.id().equals(asId)) {
                as = x;
            }
        }
        switch (actie) {
            case "kleed" -> {
                return s != null && as != null && kies(p, s, as, id) ? "vadsrijk.rijdier.aangekleed" : "";
            }
            case "koop_look" -> {
                if (!stal) {
                    return "vadsrijk.rijdier.alleen_stal";
                }
                Variant v = s == null || as == null ? null : Uiterlijk.van(s, as, id);
                return v == null ? "" : koop(p, v);
            }
            case "ruil" -> {
                if (asId.equals("soort") && s != null) {
                    Bron b = SOORT_BRON.get(s);
                    if (b == null || Rijdieren.heeft(p, s)) {
                        return "vadsrijk.economie.rijdier.al";
                    }
                    return ruil(p, b, () -> Rijdieren.geef(p, s));
                }
                if (asId.equals("pluimkleur")) {
                    PluimKleur k = PluimKleur.vanOfNull(id);
                    Bron b = k == null ? null : PLUIM_BRON.get(k);
                    if (b == null || Rijdieren.heeftKleur(p, k)) {
                        return "vadsrijk.economie.rijdier.al";
                    }
                    return ruil(p, b, () -> Rijdieren.geefKleur(p, k));
                }
                Variant v = s == null || as == null ? null : Uiterlijk.van(s, as, id);
                if (v == null || heeft(p, v)) {
                    return "vadsrijk.economie.rijdier.al";
                }
                return ruil(p, v.bron(), () -> geef(p, v, true));
            }
            default -> {
                return null;
            }
        }
    }
}
