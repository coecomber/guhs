package nl.juiced.guhs.feature.vadsrijk.kern;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * X-LABELS: no raw key may ever show in Het Vadsrijk. The user met a guh called "vadsrijk.npc.naam.kk_poort_zwamzoet": the
 * Kwartiermeester of Zwamzoet, an NPC that is only added at run time (u-huizen's {@code Poorten}), so neither the static
 * taalcheck nor {@link VadsrijkTaalGameTests} (which only reads {@code Npcs.alle()} at start) saw it.
 *
 * <p>This test enumerates every id that can really occur at run time and resolves every key BUILT from it, in nl_nl and en_us
 * (read from the jar):
 * <ul>
 *   <li>{@link #npcNamen}: every NPC the world can place, incl. the ones added late (Kwartiermeesters, Bondenbord guhs, ambacht
 *   board guhs, Meubelmeester Mollie, the Werver). A real {@link RijkGuh} is made per NPC, dressed with {@code stelIn} and its
 *   display name rendered in both languages: it may never look like a key;</li>
 *   <li>{@link #npcRegels}: their idle lines ({@code praat.<id>.1..3});</li>
 *   <li>{@link #registers}: every registry/enum/table of the Vadsrijk (classes, skills, tree nodes, specs, loot bases, pets,
 *   mounts, plume colours, shops, house sizes and looks, modes, emotes, airship lines and ports, towns, guild ranks, crafts,
 *   recipes, cosmetics, puzzles, boats, quays, fake-player roles, statuses, stats, affixes...). Every key-returning method of
 *   every entry is called;</li>
 *   <li>{@link #familiesPerId}: the families that are built in code from an id ({@code "prefix." + id}): caves, cave dungeons,
 *   lookouts, world doors, Wolkwal views, guild hubs, travel map, room tips, class/spec/skill/node texts, craft roles...;</li>
 *   <li>{@link #wezens}: every Vadsrijk entity type is created; its name (custom or type name) is rendered in both languages and
 *   its key methods are checked;</li>
 *   <li>{@link #gebiedenEnQuests}: areas, quests (title, text, objectives, giver's name).</li>
 * </ul>
 * The static half is tools/vadsrijk/taalcheck.py (its DYNAMISCH table). The client safety net is {@link Labels#terugval}.
 */
public class VadsrijkAlleLabelsGameTests {
    private static final String BATCH = "vadsrijk_alle_labels";
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final String V = "nl.juiced.guhs.feature.vadsrijk.";

    // ---- the two languages ---------------------------------------------------------------------------------------------------

    private static Map<String, String> taalNl, taalEn;

    static synchronized Map<String, String> taal(String naam) {
        if ("nl_nl".equals(naam) && taalNl != null) {
            return taalNl;
        }
        if ("en_us".equals(naam) && taalEn != null) {
            return taalEn;
        }
        Map<String, String> m = new HashMap<>();
        try (InputStream in = VadsrijkAlleLabelsGameTests.class.getResourceAsStream("/assets/guhs/lang/" + naam + ".json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String k : o.keySet()) {
                    m.put(k, o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "");
                }
            }
        } catch (Exception ignored) {
            // (an empty map fails every check, which is what we want)
        }
        if ("nl_nl".equals(naam)) {
            taalNl = m;
        } else {
            taalEn = m;
        }
        return m;
    }

    /** Collects the missing keys per family; fails with the full list. */
    static final class Eisen {
        final Map<String, Set<String>> keys = new LinkedHashMap<>();
        final TreeMap<String, TreeSet<String>> fout = new TreeMap<>();

        void key(String familie, String k) {
            if (k != null && !k.isEmpty()) {
                keys.computeIfAbsent(familie, f -> new LinkedHashSet<>()).add(k);
            }
        }

        void fout(String familie, String wat) {
            fout.computeIfAbsent(familie, f -> new TreeSet<>()).add(wat);
        }

        int aantal() {
            return keys.values().stream().mapToInt(Set::size).sum();
        }

        void controleer(GameTestHelper helper, String wat, int minimaal) {
            Map<String, String> n = taal("nl_nl"), e = taal("en_us");
            for (var f : keys.entrySet()) {
                for (String k : f.getValue()) {
                    boolean inNl = n.containsKey(k), inEn = e.containsKey(k);
                    if (!inNl || !inEn) {
                        fout(f.getKey(), k + (inNl ? " (en)" : inEn ? " (nl)" : " (nl+en)"));
                    }
                }
            }
            helper.assertTrue(aantal() >= minimaal, wat + ": only " + aantal() + " keys found (expected " + minimaal + "+): a source was not read");
            if (!fout.isEmpty()) {
                StringBuilder sb = new StringBuilder(wat + ": " + fout.values().stream().mapToInt(Set::size).sum() + " ontbreken\n");
                fout.forEach((f, l) -> sb.append("  [").append(f).append("] ").append(l.size()).append(": ").append(l).append('\n'));
                LOG.error("X-LABELS {}", sb);
                helper.fail(sb.toString());
            }
        }
    }

    // ---- reflection helpers (many tables are package-private; the test reads them as they are) --------------------------------

    /** A static field or no-arg static method "Class#member" (class relative to the vadsrijk package); null when not there. */
    static Object statisch(String klasse, String lid) {
        try {
            Class<?> c = Class.forName(klasse.startsWith("nl.") ? klasse : V + klasse);
            try {
                Field f = c.getDeclaredField(lid);
                f.setAccessible(true);
                return f.get(null);
            } catch (NoSuchFieldException ignored) {
                Method m = c.getDeclaredMethod(lid);
                m.setAccessible(true);
                return m.invoke(null);
            }
        } catch (Throwable t) {
            LOG.warn("X-LABELS: cannot read {}#{}: {}", klasse, lid, t.toString());
            return null;
        }
    }

    /** The entries of a collection / map (values) / array / enum class / single object. */
    static List<Object> items(Object o) {
        List<Object> l = new ArrayList<>();
        if (o == null) {
            return l;
        }
        if (o instanceof Class<?> c && c.isEnum()) {
            l.addAll(List.of(c.getEnumConstants()));
        } else if (o instanceof Map<?, ?> m) {
            l.addAll(m.values());
        } else if (o instanceof Collection<?> c) {
            l.addAll(c);
        } else if (o.getClass().isArray()) {
            for (int i = 0; i < java.lang.reflect.Array.getLength(o); i++) {
                l.add(java.lang.reflect.Array.get(o, i));
            }
        } else {
            l.add(o);
        }
        return l;
    }

    static Object roep(Object o, String methode) {
        try {
            Method m = o.getClass().getMethod(methode);
            m.setAccessible(true);
            return m.invoke(o);
        } catch (Throwable t) {
            try {
                Method m = o.getClass().getDeclaredMethod(methode);
                m.setAccessible(true);
                return m.invoke(o);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    static String str(Object o, String methode) {
        Object r = roep(o, methode);
        return r == null ? null : r.toString();
    }

    /** Every key a registry entry hands out: all its public no-arg methods that return a String or Component with a vadsrijk key. */
    static void sleutelsVan(Object o, Consumer<String> uit) {
        if (o == null) {
            return;
        }
        for (Method m : o.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() == Object.class
                    || m.getDeclaringClass() == Enum.class || m.getDeclaringClass() == Record.class || m.getName().equals("toString")) {
                continue;
            }
            Class<?> r = m.getReturnType();
            if (r != String.class && !Component.class.isAssignableFrom(r)) {
                continue;
            }
            try {
                m.setAccessible(true);
                Object v = m.invoke(o);
                if (v instanceof String s && s.startsWith("vadsrijk.") && Labels.isSleutel(s)) {
                    uit.accept(s);
                } else if (v instanceof Component c) {
                    translatables(c, uit);
                }
            } catch (Throwable ignored) {
                // (a method that needs a running world: not a label)
            }
        }
    }

    static void translatables(Component c, Consumer<String> uit) {
        if (c.getContents() instanceof TranslatableContents t) {
            if (t.getKey().startsWith("vadsrijk.")) {
                uit.accept(t.getKey());
            }
            for (Object a : t.getArgs()) {
                if (a instanceof Component ac) {
                    translatables(ac, uit);
                }
            }
        }
        for (Component s : c.getSiblings()) {
            translatables(s, uit);
        }
    }

    // ---- rendering a component in one language ----------------------------------------------------------------------------------

    /** A Language that answers from one of our lang files only (missing: the key itself, like Minecraft). */
    static final class EenTaal extends Language {
        final Map<String, String> m;

        EenTaal(Map<String, String> m) {
            this.m = m;
        }

        @Override
        public String getOrDefault(String key, String fallback) {
            return m.getOrDefault(key, fallback);
        }

        @Override
        public boolean has(String key) {
            return m.containsKey(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return false;
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return FormattedCharSequence.EMPTY;
        }
    }

    /** The text a player sees for this component in this language (raw keys stay raw, like in game). */
    static synchronized String toon(Component c, String taal) {
        Language oud = Language.getInstance();
        try {
            Language.inject(new EenTaal(taal(taal)));
            return c.copy().getString();
        } finally {
            Language.inject(oud);
        }
    }

    /** "Not a raw key": shown text is not empty and does not look like a.b.c. */
    static void eisNaam(Eisen e, String familie, String wie, Component naam) {
        if (naam == null) {
            e.fout(familie, wie + ": no name at all");
            return;
        }
        for (String t : List.of("nl_nl", "en_us")) {
            String s = toon(naam, t);
            if (s.isBlank() || Labels.isSleutel(s.strip()) || s.contains("vadsrijk.")) {
                e.fout(familie, wie + " (" + t + ") shows \"" + s + "\"");
            }
        }
    }

    // ---- all NPCs the world can place -----------------------------------------------------------------------------------------

    /** Id -> definition of every NPC: the keeper's own + the ones other slices add only when a player comes near. */
    static Map<String, NpcDef> alleNpcs() {
        Map<String, NpcDef> l = new TreeMap<>();
        for (NpcDef d : Npcs.alle()) {
            l.put(d.id(), d);
        }
        Consumer<String> extra = id -> l.computeIfAbsent(id, i -> new NpcDef(i, BlockPos.ZERO, 0, GuhVariant.NORMAL, List.of(), NpcDef.Gedrag.STAAN, null, 1f));
        // u-huizen: a Kwartiermeester per hub town (Poorten.HUBS), Meubelmeester Mollie
        for (Object h : items(statisch("huizen.Poorten", "HUBS"))) {
            extra.accept(str(h, "npc"));
        }
        extra.accept((String) statisch("huizen.Figuranten", "MOLLIE"));
        // u-gilden: the bond NPCs + a Bondenbord guh per hub (+ the valley)
        for (String f : List.of("KLERK", "POORT", "PLEIN")) {
            extra.accept((String) statisch("gilden.BondNpcs", f));
        }
        extra.accept("bond_bord_vallei");
        Object borden = statisch("gilden.BondNpcs", "BORDEN");
        if (borden instanceof Map<?, ?> m) {
            m.keySet().forEach(k -> extra.accept(k.toString()));
        }
        // t-ambacht: a board guh per workshop
        for (Object w : items(statisch("ambacht.AmbachtPlekken", "WERKPLAATSEN"))) {
            extra.accept(str(w, "npc"));
        }
        // the Werver at the portal temple
        extra.accept((String) statisch("npc.Werver", "ID"));
        l.remove(null);
        return l;
    }

    /** Every NPC, dressed like in the world: its name tag must be a real name in both languages. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void npcNamen(GameTestHelper helper) {
        Eisen e = new Eisen();
        Map<String, NpcDef> npcs = alleNpcs();
        for (NpcDef d : npcs.values()) {
            e.key("npc.naam", d.naamKey());
            RijkGuh g = NpcRegistry.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            if (g == null) {
                e.fout("npc.spawn", d.id() + ": no guh");
                continue;
            }
            g.stelIn(d);
            eisNaam(e, "npc.naamkaartje", d.id(), g.getCustomName());
            g.discard();
        }
        // the hubs must all be there (the bug: only 2 of the 7 Kwartiermeesters had a name)
        for (String hub : List.of("zwamzoet", "kandijkom", "honingtak", "lampionhaven", "hoogvads")) {
            helper.assertTrue(npcs.containsKey("kk_poort_" + hub), "Kwartiermeester of " + hub + " is enumerated");
        }
        e.controleer(helper, "NPC-namen", 150);
        helper.succeed();
    }

    /** What a NPC says when you click it without quest business: praat.<id>.1..3 (..4 for a few). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcRegels(GameTestHelper helper) {
        Eisen e = new Eisen();
        for (NpcDef d : alleNpcs().values()) {
            if (heeftEigenKlik(d.id())) {
                continue;
            }
            int n = NpcDef.VIER_REGELS.contains(d.id()) ? 4 : 3;
            for (int i = 1; i <= n; i++) {
                e.key("npc.praat", "vadsrijk.npc.praat." + d.id() + "." + i);
            }
        }
        e.controleer(helper, "NPC-regels", 300);
        helper.succeed();
    }

    /** NPCs whose click never reaches the chatter line (their own menu answers every click). */
    static boolean heeftEigenKlik(String id) {
        return id.startsWith("bond_") || id.equals("kk_mollie") || id.equals("werver")
                || items(statisch("ambacht.AmbachtPlekken", "WERKPLAATSEN")).stream().anyMatch(w -> id.equals(str(w, "npc")));
    }

    // ---- registries -----------------------------------------------------------------------------------------------------------

    /** Class#member sources: every entry's key methods are called. */
    static final List<String[]> BRONNEN = List.of(
            new String[] {"kern.Stat", "values"}, new String[] {"kern.Status", "values"}, new String[] {"kern.Element", "values"},
            new String[] {"kern.loot.Slot", "values"}, new String[] {"kern.loot.Zeldzaamheid", "values"}, new String[] {"kern.mob.Affix", "values"},
            new String[] {"kern.loot.BasisTypes", "alle"}, new String[] {"kern.klasse.Klassen", "alle"},
            new String[] {"klassen.spec.Spec", "values"}, new String[] {"huisdieren.Huisdieren", "alle"},
            new String[] {"economie.RijdierSoort", "values"}, new String[] {"economie.RijdierSoort$PluimKleur", "values"},
            new String[] {"economie.Winkels", "WINKELS"}, new String[] {"huizen.HuisSoort", "values"}, new String[] {"huizen.Ontwerp", "PALETTEN"},
            new String[] {"huizen.HuizenData$Deur", "values"},
            new String[] {"karakter.Modus", "values"}, new String[] {"hud.Emote", "values"}, new String[] {"luchtschip.Lijnen", "ALLE"},
            new String[] {"stadsleven.Steden", "ALLE"}, new String[] {"gilden.Rang", "values"}, new String[] {"ambacht.Ambacht", "values"},
            new String[] {"ambacht.Recepten", "ALLE"}, new String[] {"ambacht.Maatwerk$Eigenschap", "values"}, new String[] {"kleding.Cosmetica", "alle"},
            new String[] {"kleding.Cosmetica$Soort", "values"}, new String[] {"kleding.Cosmetica$Kracht", "values"},
            new String[] {"puzzels.Puzzels", "DEFS"}, new String[] {"zone4.boot.BootVariant", "values"}, new String[] {"zone4.boot.BootVariant$As", "values"},
            new String[] {"zone4.boot.BootZee", "steigers"}, new String[] {"party.nep.NepRol", "values"}, new String[] {"rijdier.Maatjes$Rol", "values"},
            new String[] {"rijdier.Maatjes", "KRACHTEN"}, new String[] {"vfx.BaasProfielen", "alle"}, new String[] {"dungeon.DungeonType", "values"},
            new String[] {"zone3.systemen.Premies", "POOL"}, new String[] {"zone3.systemen.Premies", "WINKEL"});

    static Object bron(String klasse, String lid) {
        if (lid.equals("values")) {
            try {
                return Class.forName(V + klasse);
            } catch (Throwable t) {
                return null;
            }
        }
        return statisch(klasse, lid);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void registers(GameTestHelper helper) {
        Eisen e = new Eisen();
        for (String[] b : BRONNEN) {
            List<Object> l = items(bron(b[0], b[1]));
            if (l.isEmpty()) {
                e.fout("bronnen", b[0] + "#" + b[1] + " is empty or gone");
            }
            List<Object> plat = new ArrayList<>();
            for (Object o : l) {
                plat.addAll(o instanceof Collection<?> c ? c : List.of(o));
            }
            for (Object o : plat) {
                sleutelsVan(o, k -> e.key(b[0], k));
                if (o.getClass().getSimpleName().equals("Lijn")) {   // (an airship line's two ports)
                    sleutelsVan(roep(o, "a"), k -> e.key(b[0], k));
                    sleutelsVan(roep(o, "b"), k -> e.key(b[0], k));
                }
            }
        }
        // the classes' skills and tree nodes
        for (var k : nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.alle()) {
            k.vaardigheden().forEach(v -> sleutelsVan(v, s -> e.key("klassen.skill", s)));
            k.boom().forEach(n -> sleutelsVan(n, s -> e.key("klassen.knoop", s)));
        }
        e.controleer(helper, "registers", 400);
        helper.succeed();
    }

    // ---- families built from an id in code --------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void familiesPerId(GameTestHelper helper) {
        Eisen e = new Eisen();
        // caves (grotten/Grotten): name, subtitle, elite, the four exploration-book lines
        for (Object g : items(statisch("grotten.GrotData", "ALLE"))) {
            String id = str(g, "id");
            for (String k : List.of("naam.", "onder.", "elite.naam.", "boek.ontdekt.", "boek.elite.", "boek.puzzel.", "boek.schat.")) {
                e.key("grotten", "vadsrijk.grotten." + k + id);
            }
        }
        // cave dungeons (grotkerker): name, boss, boss subtitle
        for (Object k : items(statisch("grotkerker.KerkerData", "ALLE"))) {
            String id = str(k, "id");
            e.key("grotkerker", "vadsrijk.grotkerker.grot." + id);
            e.key("grotkerker", "vadsrijk.grotkerker.baas." + id);
            e.key("grotkerker", "vadsrijk.grotkerker.baas." + id + ".onder");
            e.key("grotkerker", "vadsrijk.grotten.boek.baas." + id);
        }
        // lookouts + world doors (zone 1) and the Wolkwal views (zone 5)
        for (Object u : items(statisch("wereld.WereldPlekken", "UITKIJKEN"))) {
            e.key("uitkijk", "vadsrijk.wereld.uitkijk." + str(u, "id"));
        }
        for (Object d : items(statisch("wereld.WereldDeuren", "alle"))) {
            String id = str(d, "id");
            for (String k : List.of("titel.", "onder.", "dicht.")) {
                e.key("wereld.deur", "vadsrijk.wereld.deur." + k + id);
            }
        }
        for (Object u : items(statisch("zone5.wolkwal.WolkwalUitzicht", "UITZICHTEN"))) {
            e.key("wolkwal.uitzicht", "vadsrijk.wolkwal.uitzicht." + str(u, "id"));
            e.key("wolkwal.uitzicht", "vadsrijk.wolkwal.uitzicht." + str(u, "id") + ".onder");
        }
        // u-gilden: the teleport hubs; npc: the travel map, the room tips
        for (Object h : items(statisch("gilden.BondPaneel", "HUBS"))) {
            e.key("gilden.hub", "vadsrijk.gilden.hub." + str(h, "id"));
        }
        for (Object p : items(statisch("npc.Reiskaart", "PLEKKEN"))) {
            e.key("reiskaart", "vadsrijk.npc.reiskaart.plek." + p);
        }
        for (Object k : items(statisch("npc.KamerTips", "KAMERS"))) {
            String id = str(k, "id");
            if (!id.startsWith("z2_")) {
                e.key("kamertips", "vadsrijk.npc.kamer." + id);
            }
        }
        // classes: the choice cards; specs: role, short text, titles, advancement lines
        for (var k : nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.alle()) {
            e.key("klassen", "vadsrijk.klassen.kies.rol." + k.id());
            e.key("klassen", "vadsrijk.klassen.kort." + k.id());
            for (int s = 1; s <= 3; s++) {
                e.key("klassen", "vadsrijk.klassen.titel." + k.id() + "." + s);
            }
            for (var v : k.vaardigheden()) {
                e.key("klassen.skilluitleg", "vadsrijk.klassen.skilluitleg." + v.id());
            }
            for (var n : k.boom()) {
                e.key("klassen.knoop.uitleg", "vadsrijk.klassen.knoop." + n.id() + ".uitleg");
            }
        }
        for (Object s : items(bron("klassen.spec.Spec", "values"))) {
            String id = String.valueOf(veld(s, "id"));
            e.key("spec", "vadsrijk.klassen.spec." + id + ".rol");
            e.key("spec", "vadsrijk.klassen.spec." + id + ".kort");
            for (int st = 1; st <= 3; st++) {
                e.key("spec", "vadsrijk.klassen.titel." + id + "." + st);
            }
        }
        // crafts: the board guhs' greeting per role, the craft group per workshop hub
        for (Object a : items(bron("ambacht.Ambacht", "values"))) {
            String rol = nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtNpcs.rol((nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht) a);
            e.key("ambacht.npc", "vadsrijk.ambacht.npc." + rol + ".hallo");
            e.key("ambacht.npc", "vadsrijk.ambacht.npc." + rol + ".uitleg");
        }
        e.key("ambacht.npc", "vadsrijk.ambacht.npc." + nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtNpcs.rol(null) + ".hallo");
        // modes (karakter): the line, the new/make buttons
        for (Object m : items(bron("karakter.Modus", "values"))) {
            String id = str(m, "id");
            if (id == null) {
                Object f = veld(m, "id");
                id = f == null ? null : f.toString();
            }
            e.key("karakter.modus", "vadsrijk.karakter.modus." + id + ".regel");
            e.key("karakter.modus", "vadsrijk.karakter.scherm.nieuw." + id);
            e.key("karakter.modus", "vadsrijk.karakter.scherm.maak." + id);
        }
        // towns: the stadsleven name
        for (Object s : items(statisch("stadsleven.Steden", "ALLE"))) {
            e.key("stadsleven", "vadsrijk.stadsleven.stad." + str(s, "id"));
        }
        // u-huizen: the Kwartiermeester's "where" line, like zone 6's two
        for (Object h : items(statisch("huizen.Poorten", "HUBS"))) {
            e.key("kk_poort.plek", "vadsrijk.npc.plek.npc." + str(h, "npc"));
        }
        e.controleer(helper, "families per id", 300);
        helper.succeed();
    }

    static Object veld(Object o, String naam) {
        try {
            Field f = o.getClass().getDeclaredField(naam);
            f.setAccessible(true);
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- creatures ----------------------------------------------------------------------------------------------------------------

    /** Every Vadsrijk entity: its name in both languages (custom name or type name) and its key methods. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void wezens(GameTestHelper helper) {
        Eisen e = new Eisen();
        int n = 0;
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            if (!id.getNamespace().equals("guhs") || !id.getPath().startsWith("vr_")) {
                continue;
            }
            Entity ent;
            try {
                ent = t.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            } catch (Throwable th) {
                continue;
            }
            if (ent == null) {
                continue;
            }
            n++;
            e.key("entity.type", t.getDescriptionId());
            if (!(ent instanceof RijkGuh)) {
                eisNaam(e, "entity.naam", id.getPath(), ent.getName());
            }
            for (Method m : ent.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == String.class && m.getName().toLowerCase(java.util.Locale.ROOT).endsWith("key")
                        && m.getDeclaringClass().getName().startsWith(V)) {
                    try {
                        Object v = m.invoke(ent);
                        if (v instanceof String s && s.startsWith("vadsrijk.")) {
                            e.key("entity." + id.getPath(), s);
                        }
                    } catch (Throwable ignored) {
                        // (needs a world state)
                    }
                }
            }
            ent.discard();
        }
        helper.assertTrue(n > 50, "Vadsrijk entity types found: " + n);
        e.controleer(helper, "wezens", 50);
        helper.succeed();
    }

    // ---- areas + quests ------------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void gebiedenEnQuests(GameTestHelper helper) {
        Eisen e = new Eisen();
        for (Gebieden.Gebied g : Gebieden.alle()) {
            e.key("gebied", g.naamKey());
        }
        for (Quest q : Quests.alle().values()) {
            e.key("quest", q.titelKey());
            e.key("quest", q.tekstKey());
            for (int i = 0; i < q.doelen().size(); i++) {
                e.key("quest.doel", q.doelKey(i));
            }
            String gever = str(q, "gever");
            if (gever != null && !gever.isEmpty()) {
                e.key("quest.gever", "vadsrijk.npc.naam." + gever);
            }
        }
        e.controleer(helper, "gebieden + quests", 300);
        helper.succeed();
    }

    // ---- the safety net itself ---------------------------------------------------------------------------------------------------

    /** Labels.terugval: a missing per-id key falls back on the generic one, and never shows a key. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void terugval(GameTestHelper helper) {
        Map<String, String> n = taal("nl_nl");
        helper.assertTrue("Kwartiermeester".equals(Labels.terugval("vadsrijk.npc.naam.kk_poort_nergenshuizen", n::get)),
                "kk_poort_<onbekend> -> the generic Kwartiermeester: " + Labels.terugval("vadsrijk.npc.naam.kk_poort_nergenshuizen", n::get));
        String r = Labels.terugval("vadsrijk.foo.bar.iets_heel_raars", k -> null);
        helper.assertTrue(!Labels.isSleutel(r) && !r.isBlank() && !r.contains("vadsrijk"), "humanised: " + r);
        helper.succeed();
    }
}
