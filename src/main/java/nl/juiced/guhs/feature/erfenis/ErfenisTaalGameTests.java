package nl.juiced.guhs.feature.erfenis;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import nl.juiced.guhs.feature.erfenis.kaart.BestelGoed;
import nl.juiced.guhs.feature.erfenis.kaart.Bestelling;
import nl.juiced.guhs.feature.erfenis.kaart.Handje;
import nl.juiced.guhs.feature.erfenis.kaart.KaartGebouwType;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.13 "random English in the Erfenis": the players play Dutch on a dedicated server, whose own language is en_us.
 * <ul>
 *   <li>{@link #opgehaaldInSpelersTaal}: the collect toast ("12 eieren, 3 veren") was flattened to a String on the server,
 *   so it arrived in English; now it travels as a component and the reader's client resolves it;</li>
 *   <li>{@link #erfenisSleutelsBestaan}: every lang key that the Erfenis BUILDS from a runtime id ({@code "erfenis.deco." + id},
 *   goods, buildings, recipes, goals, finds, milestones, tutorial steps, palettes, map and order families...) exists in
 *   nl_nl AND en_us (read from the jar). A missing key shows as a raw key, or in English when only en_us has it.</li>
 * </ul>
 */
public class ErfenisTaalGameTests {
    private static final String BATCH = "erfenis_taal";

    // ---- the two languages (from the jar, not the server's Language: that one is en_us only) ------------------------------

    private static Map<String, String> taalNl, taalEn;

    private static synchronized Map<String, String> taal(String naam) {
        if ("nl_nl".equals(naam) && taalNl != null) {
            return taalNl;
        }
        if ("en_us".equals(naam) && taalEn != null) {
            return taalEn;
        }
        Map<String, String> m = new HashMap<>();
        try (InputStream in = ErfenisTaalGameTests.class.getResourceAsStream("/assets/guhs/lang/" + naam + ".json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String k : o.keySet()) {
                    m.put(k, o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "");
                }
            }
        } catch (Exception ignored) {
            // (an empty map fails every check)
        }
        if ("nl_nl".equals(naam)) {
            taalNl = m;
        } else {
            taalEn = m;
        }
        return m;
    }

    /** A static field of a class (package-private tables), or null. */
    private static Object veld(Class<?> c, String naam) {
        try {
            Field f = c.getDeclaredField(naam);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // ---- the collect toast ------------------------------------------------------------------------------------------------

    /** The collect toast carries its goods as a component (lang keys), never as text the server resolved in en_us. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opgehaaldInSpelersTaal(GameTestHelper helper) {
        HolderLookup.Provider reg = helper.getLevel().registryAccess();
        Map<Goed, Integer> erin = new LinkedHashMap<>(), weg = new LinkedHashMap<>();
        erin.put(Goed.EI, 12);
        erin.put(Goed.VEER, 3);
        weg.put(Goed.STAM, 30);
        String arg = ErfenisPayloads.Melding.tekst(Erfenis.ophaalRegel(erin, weg), reg);
        helper.assertTrue(arg.startsWith(ErfenisPayloads.Melding.TEKST), "the goods go as a whole text (component JSON): " + arg);
        // through the network, as the client gets it
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ErfenisPayloads.Melding.STREAM_CODEC.encode(buf, new ErfenisPayloads.Melding("erfenis.ok.opgehaald", List.of(arg, "7"), true));
        ErfenisPayloads.Melding m = ErfenisPayloads.Melding.STREAM_CODEC.decode(buf);
        Component c = ErfenisPayloads.Melding.leesTekst(m.args().get(0), reg);
        helper.assertTrue(c != null, "the client reads the text back");
        Set<String> keys = new LinkedHashSet<>();
        StringBuilder letterlijk = new StringBuilder();
        loop(c, keys, letterlijk);
        for (String k : List.of("guhs.erfenis.goed.ei", "guhs.erfenis.goed.veer", "guhs.erfenis.goed.stam", "erfenis.ok.overloop")) {
            helper.assertTrue(keys.contains(k), "the client resolves " + k + " itself: " + keys);
        }
        for (Goed g : List.of(Goed.EI, Goed.VEER, Goed.STAM)) {
            String engels = Language.getInstance().getOrDefault(g.naamKey());
            helper.assertTrue(engels.equals(g.naamKey()) || !letterlijk.toString().contains(engels),
                    "no text resolved on the server (\"" + engels + "\") in the toast: " + letterlijk);
        }
        helper.assertTrue(letterlijk.toString().contains("12") && letterlijk.toString().contains("30"), "the numbers stay: " + letterlijk);
        helper.assertTrue(ErfenisPayloads.Melding.leesTekst(m.args().get(1), reg) == null
                && ErfenisPayloads.Melding.leesTekst("guhs.erfenis.goed.ei", reg) == null, "numbers and lang keys stay plain arguments");
        helper.succeed();
    }

    private static void loop(Component c, Set<String> keys, StringBuilder letterlijk) {
        if (c.getContents() instanceof TranslatableContents t) {
            keys.add(t.getKey());
            for (Object a : t.getArgs()) {
                if (a instanceof Component ac) {
                    loop(ac, keys, letterlijk);
                } else {
                    letterlijk.append(a).append('|');
                }
            }
        } else if (c.getContents() instanceof PlainTextContents p) {
            letterlijk.append(p.text()).append('|');
        }
        for (Component s : c.getSiblings()) {
            loop(s, keys, letterlijk);
        }
    }

    // ---- every built key ---------------------------------------------------------------------------------------------------

    /** Every key the Erfenis builds from a runtime id exists in nl_nl and en_us. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void erfenisSleutelsBestaan(GameTestHelper helper) {
        Map<String, Set<String>> fam = new LinkedHashMap<>();
        java.util.function.BiConsumer<String, String> key = (f, k) -> fam.computeIfAbsent(f, x -> new LinkedHashSet<>()).add(k);

        // buildings, families, goods, recipes, the Hoeve goals
        for (GebouwType t : GebouwType.values()) {
            key.accept("gebouw", t.naamKey());
            key.accept("gebouw.uitleg", t.uitlegKey());
            for (Recept r : Productie.recepten(t)) {
                key.accept("recept", r.naamKey());
            }
        }
        for (GebouwType.Familie f : GebouwType.Familie.values()) {
            key.accept("familie", f.naamKey());
        }
        for (Goed g : Goed.values()) {
            key.accept("goed", g.naamKey());
        }
        for (int naar = 2; naar <= 10; naar++) {
            for (Prijzen.Doel d : Prijzen.doelen(naar)) {
                key.accept("doel", "erfenis.doel." + d.soort());
            }
        }
        // deco: the shop, every vormen deco, and the ids other slices hand to ErfenisDeco
        for (DecoWinkel.Categorie c : DecoWinkel.Categorie.values()) {
            key.accept("decowinkel.cat", c.naamKey());
        }
        for (DecoWinkel.Stuk s : DecoWinkel.alle()) {
            key.accept("decowinkel.stuk", s.naamKey());
        }
        for (String id : GebouwVormen.decoIds()) {
            key.accept("deco", "erfenis.deco." + id);
        }
        List<String> anderen = new ArrayList<>(List.of(Handje.BURENSTUKKEN));
        anderen.add("trekguh");
        Object besteldeco = veld(nl.juiced.guhs.feature.erfenis.kaart.Bestellingen.class, "DECO");
        if (besteldeco instanceof List<?> l) {
            for (Object d : l) {
                try {
                    java.lang.reflect.Method id = d.getClass().getDeclaredMethod("id");
                    id.setAccessible(true);
                    anderen.add((String) id.invoke(d));
                } catch (ReflectiveOperationException e) {
                    helper.fail("Bestellingen.DECO: " + e);
                }
            }
        } else {
            helper.fail("Bestellingen.DECO not readable");
        }
        Object sterDeco = veld(nl.juiced.guhs.feature.erfenis.kaart.Bestellingen.class, "STER_DECO");
        helper.assertTrue(sterDeco instanceof String, "Bestellingen.STER_DECO readable");
        anderen.add((String) sterDeco);
        for (String id : anderen) {
            key.accept("deco (andere slices)", "erfenis.deco." + ErfenisDeco.vorm(id));
            if (!id.equals("trekguh")) {
                key.accept("kaart.deco", "erfenis.kaart.deco." + id);
            }
        }
        for (String id : nl.juiced.guhs.feature.erfenis.sherlock.Zaken.DECO) {
            key.accept("deco (sherlock)", "erfenis.deco." + ErfenisDeco.vorm(id));
        }
        // finds: the melding (erfenis.vondst.<id>) and the vondstenkistje (Vensters.Opslag.sleutel)
        List<String> vondsten = new ArrayList<>(List.of("roestig_sleuteltje"));
        for (int n = 0; n < Plot.PLUKJES; n++) {
            String v = Base.vasteVondst(n);
            if (v != null) {
                vondsten.add(v);
            }
        }
        if (veld(Base.class, "DECO_VONDST") instanceof String[][] dv) {
            for (String[] ring : dv) {
                for (String d : ring) {
                    vondsten.add("deco_" + d);
                    key.accept("deco (vondst)", "erfenis.deco." + ErfenisDeco.vorm(d));
                }
            }
        } else {
            helper.fail("Base.DECO_VONDST not readable");
        }
        if (veld(Base.class, "ZELDZAAM") instanceof String[] zz) {
            vondsten.addAll(List.of(zz));
        } else {
            helper.fail("Base.ZELDZAAM not readable");
        }
        for (int nr = Afwerking.EERSTE_POST; nr <= Afwerking.LAATSTE_POST; nr++) {
            vondsten.add("ansichtkaart_" + nr);
            key.accept("ansicht", "erfenis.afwerking.ansicht." + nr + ".naam");
            for (int i = 0; i < 4; i++) {
                key.accept("ansicht", "erfenis.afwerking.ansicht." + nr + ".tekst." + i);
            }
        }
        for (String v : vondsten) {
            key.accept("vondst", "erfenis.vondst." + v);
            // (the same choice as client/Vensters.Opslag.sleutel)
            key.accept("vondstenkistje", v.startsWith("deco_") || !v.contains("_") && !v.startsWith("ansicht")
                    ? "erfenis.deco." + v.replace("deco_", "") : "erfenis.vondst." + v);
        }
        for (String boek : List.of("testament", "dagboek")) {
            key.accept("boek", "erfenis.afwerking.boek." + boek + ".titel");
            for (int i = 0; i < 3; i++) {
                key.accept("boek", "erfenis.afwerking.boek." + boek + "." + i);
            }
        }
        // numbered families
        for (int m : Knusheid.MIJLPALEN) {
            key.accept("knus.mijlpaal", "erfenis.knus.mijlpaal." + m);
        }
        for (String t : List.of(Knusheid.TITEL_250, Knusheid.TITEL_1000, Knusheid.TITEL_2500)) {
            key.accept("titel", "gui.guhs.titels.naam." + t);
        }
        for (int s = 1; s < Makelaarstour.KLAAR; s++) {
            key.accept("tut", "erfenis.tut." + s);
        }
        for (int p = 1; p <= 5; p++) {   // Base.maxPalet: (Hoeve 10 + 1) / 2
            key.accept("palet", "erfenis.palet." + p);
        }
        for (int s = 0; s < 6; s++) {   // Base.plukjeSoort: floorMod(.., 6)
            key.accept("plukje.soort", "erfenis.plukje.soort." + s);
        }
        for (int s = 1; s <= Bouwkeet.sets().size(); s++) {
            key.accept("bouwkeet.set", "erfenis.bouwkeet.set." + s);
        }
        for (String n : List.of("al", "niet_jouw", "dood", "weg", "kwijt", "bezig")) {   // knabbelvelden.Pensioen.nietNu + "bezig"
            key.accept("pension.niet", "erfenis.pension.niet." + n);
        }
        for (String rol : List.of("ander", "iemand", "morris", "pim", "oehoe", "knabbelhamer")) {   // Sherlock.klik / Sherlock.rol (SherlockFeature)
            key.accept("sherlock.klik", "sherlock.klik." + rol);
        }
        // the world map and the orders
        for (BestelGoed g : BestelGoed.values()) {
            key.accept("kaart.goed", g.naamKey());
        }
        for (KaartGebouwType t : KaartGebouwType.values()) {
            key.accept("kaart.gebouw", t.naamKey());
        }
        for (int r = 0; r < Bestelling.REDENEN; r++) {
            key.accept("kaart.reden", "erfenis.kaart.bestel.reden." + r);
        }
        for (int r = 0; r < Bestelling.STER_REDENEN; r++) {
            key.accept("kaart.ster_reden", "erfenis.kaart.bestel.ster_reden." + r);
        }
        for (int r = -1; r <= 1; r++) {
            key.accept("kaart.relatie", "erfenis.kaart.bestel.relatie." + (r + 1));
        }
        for (int lvl = 1; lvl <= 10; lvl++) {   // (KaartScherm.grootte)
            key.accept("kaart.grootte", "erfenis.kaart.grootte." + (lvl + 1) / 2);
        }
        Object gezellig = veld(nl.juiced.guhs.feature.erfenis.kaart.KaartLeven.class, "GEZELLIG");
        helper.assertTrue(gezellig instanceof Integer, "KaartLeven.GEZELLIG readable");
        for (int i = 0; i < (Integer) gezellig; i++) {
            key.accept("kaart.nieuws.gezellig", "erfenis.kaart.nieuws.gezellig." + i);
        }

        // check
        Map<String, String> nl = taal("nl_nl"), en = taal("en_us");
        TreeMap<String, TreeSet<String>> fout = new TreeMap<>();
        int n = 0;
        for (var f : fam.entrySet()) {
            for (String k : f.getValue()) {
                n++;
                boolean inNl = nl.containsKey(k), inEn = en.containsKey(k);
                if (!inNl || !inEn) {
                    fout.computeIfAbsent(f.getKey(), x -> new TreeSet<>()).add(k + (inNl ? " (en)" : inEn ? " (nl)" : " (nl+en)"));
                }
            }
        }
        helper.assertTrue(n > 400, "only " + n + " keys found: a source was not read");
        helper.assertTrue(fout.isEmpty(), "missing Erfenis texts: " + fout);
        helper.succeed();
    }
}
