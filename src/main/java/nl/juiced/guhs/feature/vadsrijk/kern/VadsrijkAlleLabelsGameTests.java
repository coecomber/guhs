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
 *   <li>{@link #gebiedenEnQuests}: areas, quests (title, text, objectives, giver's name);</li>
 *   <li>1.10.13: {@link #ambachten} (every gathering group incl. the cave ones Grotten adds, spot kinds, fish hints, story steps,
 *   order board), {@link #npcKeuzes} (class names in the party panel, travel NPCs, role and extra choices), {@link #questTeksten}
 *   (quest dialogue + the tracker's "where" line), {@link #geheimenZones} (zone 3-6 secrets), {@link #itemsEnBazen} (item names,
 *   boss item effects, the Bazen tab, sealed dungeon doors).</li>
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
            LOG.info("X-LABELS {}: {} keys in {} families checked in nl_nl + en_us", wat, aantal(), keys.size());
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
    /** Sources that could not be read (a renamed field): the npc test fails on them, so a source never silently drops out. */
    static final Set<String> ONGELEZEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static Map<String, NpcDef> alleNpcs() {
        Map<String, NpcDef> l = new TreeMap<>();
        for (NpcDef d : Npcs.alle()) {
            l.put(d.id(), d);
        }
        Consumer<String> extra = id -> {
            if (id == null) {
                LOG.warn("X-LABELS: an NPC source gave no id");
                ONGELEZEN.add("npc-bron");
                return;
            }
            l.computeIfAbsent(id, i -> new NpcDef(i, BlockPos.ZERO, 0, GuhVariant.NORMAL, List.of(), NpcDef.Gedrag.STAAN, null, 1f));
        };
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
        return l;
    }

    /** Every NPC, dressed like in the world: its name tag must be a real name in both languages. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void npcNamen(GameTestHelper helper) {
        veilig(helper, () -> npcNamen0(helper));
    }

    /** A crash inside a test shows as "UnknownGameTestException": report the real exception instead. */
    static void veilig(GameTestHelper helper, Runnable r) {
        try {
            r.run();
        } catch (net.minecraft.gametest.framework.GameTestException a) {
            throw a;
        } catch (Throwable t) {
            LOG.error("X-LABELS crash", t);
            StringBuilder sb = new StringBuilder(t.toString());
            for (StackTraceElement el : t.getStackTrace()) {
                if (el.getClassName().startsWith("nl.juiced")) {
                    sb.append(" @ ").append(el.getClassName().substring(el.getClassName().lastIndexOf('.') + 1)).append(':').append(el.getLineNumber());
                }
            }
            helper.fail(sb.toString());
        }
    }

    static void npcNamen0(GameTestHelper helper) {
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
        if (!ONGELEZEN.isEmpty()) {
            e.fout("bronnen", "unreadable NPC sources: " + ONGELEZEN + " (see the log)");
        }
        e.controleer(helper, "NPC-namen", 150);
        helper.succeed();
    }

    /** What a NPC says when you click it without quest business: praat.<id>.1..3 (..4 for a few). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcRegels(GameTestHelper helper) {
        veilig(helper, () -> npcRegels0(helper));
    }

    static void npcRegels0(GameTestHelper helper) {
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
        return id.startsWith("bond_") || id.startsWith("kk_poort_") || id.equals("kk_mollie") || id.equals("werver")   // (Menus.klik: the Kwartiermeesters always open their menu)
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
            new String[] {"zone4.boot.BootZee", "PLACEHOLDERS"}, new String[] {"party.nep.NepRol", "values"}, new String[] {"rijdier.Maatjes$Rol", "values"},
            new String[] {"rijdier.Maatjes", "KRACHTEN"}, new String[] {"vfx.BaasProfielen", "alle"}, new String[] {"dungeon.DungeonType", "values"},
            new String[] {"zone3.systemen.Premies", "POOL"}, new String[] {"zone3.systemen.Premies", "WINKEL"},
            new String[] {"rijdier.Uiterlijk", "alle"}, new String[] {"rijdier.Uiterlijk$As", "values"},
            // 1.10.13: the gathering kinds (book hint, HUD), the cave dungeon defs and the mount/pet drop targets (pity lines)
            new String[] {"ambacht.NodeSoort", "values"}, new String[] {"grotkerker.GrotBazen", "alle"}, new String[] {"vint.RijdierBuit", "DOELEN"});

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
        for (var n : nl.juiced.guhs.feature.vadsrijk.npc.ReiskaartIndeling.NAMEN) {   // 1.10.3: the area labels on the travel map
            e.key("reiskaart.naam", "vadsrijk.kern.gebied." + n.gebied());
        }
        e.key("reiskaart", "vadsrijk.npc.reiskaart.kampvuur");
        for (Object k : items(statisch("npc.KamerTips", "KAMERS"))) {
            String id = str(k, "id");
            if (!id.startsWith("z2_")) {
                e.key("kamertips", "vadsrijk.npc.kamer." + id);
            }
        }
        // classes: the choice cards; specs: role, short text, titles, advancement lines
        for (var k : nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.alle()) {
            e.key("klassen", "vadsrijk.klassen.kies.rol." + k.id());
            for (int s = 1; s <= 3; s++) {
                e.key("klassen", "vadsrijk.klassen.titel." + k.id() + "." + s);
            }
            for (var v : k.vaardigheden()) {
                e.key("klassen.skilluitleg", "vadsrijk.klassen.skilluitleg." + v.id());
                e.key("klassen.kort", "vadsrijk.klassen.kort." + v.id());   // (the choice card: one line per skill)
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
            if (((nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht) a).verzamel) {   // ("where do I gather?": only the gathering masters)
                e.key("ambacht.npc", "vadsrijk.ambacht.npc." + rol + ".uitleg");
            }
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
        // secrets: zone 1's cuddly toys, clues and treasures (found message + questlog), zone 2's
        for (String[] f : new String[][] {{"KNUFFEL", "vadsrijk.npc.knuffel."}, {"AANWIJZING", "vadsrijk.npc.aanwijzing."},
                {"AANWIJZING", "vadsrijk.npc.aanwijzing.titel."}, {"SCHAT", "vadsrijk.npc.schat."}, {"SCHAT", "vadsrijk.npc.schat.naam."}}) {
            if (statisch("npc.NpcPlekken", f[0]) instanceof Map<?, ?> m) {
                m.keySet().forEach(k -> e.key("geheim.z1", f[1] + k));
            }
        }
        for (String[] f : new String[][] {{"KNUFFEL", "vadsrijk.z2npc.knuffel."}, {"AANWIJZING", "vadsrijk.z2npc.aanwijzing."},
                {"SCHAT", "vadsrijk.z2npc.schat."}}) {
            if (statisch("zone2.npc.Z2NpcPlekken", f[0]) instanceof Map<?, ?> m) {
                m.keySet().forEach(k -> e.key("geheim.z2", f[1] + k));
            }
        }
        // mounts: the stable card per species; guilds: week tasks and castle levels
        for (Object o : items(bron("economie.RijdierSoort", "values"))) {
            String id = str(o, "id");
            e.key("rijdier", "vadsrijk.economie.rijdier.eigen." + id);
            if (!Boolean.TRUE.equals(roep(o, "kleuren"))) {
                e.key("rijdier", "vadsrijk.economie.stal.nog_niet." + id);
            }
        }
        for (Object o : items(bron("economie.RijdierSoort$PluimKleur", "values"))) {
            e.key("rijdier", "vadsrijk.economie.pluimkleur.bron." + str(o, "id"));
        }
        for (Object t : items(statisch("gilden.BondXp", "TAKEN"))) {
            e.key("gilden.taak", "vadsrijk.gilden.taak." + str(t, "id"));
            e.key("gilden.taak", "vadsrijk.gilden.taak." + str(t, "id") + ".lang");
        }
        for (int i = 1; statisch("gilden.Kasteel", "MAX") instanceof Integer max && i <= max; i++) {
            e.key("gilden.kasteel", "vadsrijk.gilden.kasteel." + i);
            e.key("gilden.kasteel", "vadsrijk.gilden.kasteel.uitleg." + i);
        }
        // named creatures: the area leaders of zone 3/5/6 (their name tag), z3 contract targets (name tag + board), riders
        for (String z : List.of("zone3.wezens.Z3WezensPlekken:z3wezens", "zone5.wezens.Z5WezensPlekken:z5wezens", "zone6.wezens.Z6WezensPlekken:z6wezens")) {
            String[] d = z.split(":");
            Object m = statisch(d[0], "LEIDERS");
            if (m instanceof Map<?, ?> mm) {
                mm.keySet().forEach(k -> e.key("leider", "vadsrijk." + d[1] + ".leider." + k));
            } else {
                e.fout("bronnen", d[0] + "#LEIDERS is gone");
            }
        }
        if (statisch("zone3.wezens.Z3Contracten", "DOELWITTEN") instanceof Map<?, ?> m) {
            m.keySet().forEach(k -> e.key("z3.contract", "vadsrijk.z3wezens.contract." + k));
        }
        for (Object k : items(statisch("zone3.systemen.Contracten", "DOELWITTEN"))) {
            e.key("z3.contract", "vadsrijk.z3.systemen.contract." + k);
        }
        Object namen = statisch("ruiters.Ruiters", "NAMEN"), z1 = statisch("ruiters.Ruiters", "REGELS_Z1"), z3 = statisch("ruiters.Ruiters", "REGELS_Z3");
        for (int i = 0; namen instanceof Integer n && i < n; i++) {
            e.key("ruiters", "vadsrijk.ruiters.naam." + i);
        }
        for (int i = 0; z1 instanceof Integer n && i < n; i++) {
            e.key("ruiters", "vadsrijk.ruiters.z1." + i);
        }
        for (int i = 0; z3 instanceof Integer n && i < n; i++) {
            e.key("ruiters", "vadsrijk.ruiters.z3." + i);
        }
        // puzzles: name, book hint, overlay line, the riddle of a whisper stone, the torn page and the unlock of the reward
        for (Object o : items(statisch("puzzels.Puzzels", "DEFS"))) {
            String id = str(o, "id"), soort = str(o, "soort");
            e.key("puzzels", "vadsrijk.puzzels.p." + id + ".naam");
            e.key("puzzels", "vadsrijk.puzzels.p." + id + ".aanwijzing");
            e.key("puzzels", "vadsrijk.puzzels.p." + id + ".uitleg");
            e.key("puzzels", "vadsrijk.puzzels.soort." + soort);
            if ("fluistersteen".equals(soort)) {
                e.key("puzzels", "vadsrijk.puzzels.p." + id + ".vraag");
            }
            Object bel = roep(o, "beloning");
            if (bel != null) {
                Object bl = roep(bel, "bladzijde"), on = roep(bel, "ontgrendel");
                if (bl instanceof Integer n && n > 0) {
                    e.key("puzzels.bladzijde", "vadsrijk.puzzels.bladzijde." + n + ".titel");
                    e.key("puzzels.bladzijde", "vadsrijk.puzzels.bladzijde." + n + ".tekst");
                }
                if (on instanceof String os && !os.isEmpty()) {
                    e.key("puzzels.ontgrendel", "vadsrijk.puzzels.ontgrendel." + os);
                }
            }
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

    // ---- 1.10.13: the families the first version missed -------------------------------------------------------------------------

    /**
     * Ambachten: every gathering group's name (the Ambachtenboek, the board guhs' "where" line, the beekeeping messages; the cave
     * groups that Grotten adds at run time showed "vadsrijk.ambacht.groep.grot_zwamgrot_amb_1"), the spot kinds and minigame titles,
     * the fish hints, the hub teachers' and the Gilde's story steps, the order board's givers and goods.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void ambachten(GameTestHelper helper) {
        veilig(helper, () -> ambachten0(helper));
    }

    static void ambachten0(GameTestHelper helper) {
        Eisen e = new Eisen();
        int groepen = 0;
        for (var g : nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.GROEPEN) {
            groepen++;
            e.key("ambacht.groep", "vadsrijk.ambacht.groep." + g.id());
            e.key("ambacht.soort", g.soort().naamKey());
        }
        helper.assertTrue(groepen >= 60 && nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.GROEPEN.stream().anyMatch(g -> g.id().startsWith("grot_")),
                "all gathering groups are registered, the cave ones too: " + groepen);
        for (var s : nl.juiced.guhs.feature.vadsrijk.ambacht.NodeSoort.values()) {
            e.key("ambacht.soort", s.naamKey());
            e.key("ambacht.mini", "vadsrijk.ambacht.mini." + s.oud);
        }
        for (var m : nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen.ALLE.values()) {
            if (m.vis()) {
                e.key("ambacht.boek.vis", "vadsrijk.ambacht.boek.hint.vis." + (m.wanneer().isEmpty() ? "altijd" : m.wanneer()));
            }
            if (!m.wanneer().isEmpty()) {
                e.key("ambacht.vis", "vadsrijk.ambacht.vis." + m.wanneer());
            }
            if (m.tier() > 0) {
                e.key("ambacht.tier", "vadsrijk.ambacht.tier." + m.tier());
            }
        }
        int gilde = nl.juiced.guhs.feature.vadsrijk.ambacht.HoogvadsGilde.stappen();
        for (var a : nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht.values()) {
            for (int stap = 0; stap < nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtNpcs.VERHAAL_LEVELS.length; stap++) {
                for (int r = 0; r < 3; r++) {
                    e.key("ambacht.verhaal", "vadsrijk.ambacht.verhaal." + a.id + "." + stap + "." + r);
                }
            }
            for (int i = 0; i < gilde; i++) {
                for (int r = 0; r < 3; r++) {
                    e.key("ambacht.gilde", "vadsrijk.ambacht.gilde." + a.id + "." + i + "." + r);
                }
            }
        }
        for (Object g : items(statisch("ambacht.Bestelbord", "GEVERS"))) {
            e.key("ambacht.bestel.gever", "vadsrijk.npc.naam." + g);
        }
        for (var b : nl.juiced.guhs.feature.vadsrijk.ambacht.Bestelbord.POOL.values()) {
            if (b.soort().equals("breng")) {
                String w = b.wat();
                e.key("ambacht.bestel.ding", w.startsWith("eten:") ? "item.guhs.vr_amb_eten_" + w.substring(5)
                        : w.startsWith("drank:") ? "item.guhs.vr_amb_drank_" + w.substring(6) : "item.guhs.vr_amb_" + w);
            }
        }
        e.controleer(helper, "ambachten", 300);
        helper.succeed();
    }

    /**
     * What a click on an NPC offers: the class name in the party panel (built from the class id: the Knuffelridder's own naamKey is
     * another key), the travel NPCs' "where to?" line and destinations (the Zandloper of zone 6 had none), every role choice and
     * every extra choice.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcKeuzes(GameTestHelper helper) {
        veilig(helper, () -> npcKeuzes0(helper));
    }

    static void npcKeuzes0(GameTestHelper helper) {
        Eisen e = new Eisen();
        for (var k : nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.alle()) {
            e.key("klasse.party", "vadsrijk.klassen.klasse." + k.id());   // (PartyDienst, Advancement's fallback)
            e.key("klasse.naam", k.naamKey());
        }
        for (var r : nl.juiced.guhs.feature.vadsrijk.party.nep.NepRol.values()) {
            e.key("klasse.party", "vadsrijk.klassen.klasse." + r.klasse);
        }
        var reizen = nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.REIZEN;
        helper.assertTrue(reizen.size() >= 7, "the travel NPCs are registered: " + reizen.keySet());
        reizen.forEach((npc, doelen) -> {
            e.key("reis.kies", "vadsrijk.npc.reis.kies." + npc);
            e.key("reis.npc", "vadsrijk.npc.naam." + npc);
            for (String naar : doelen) {
                e.key("reis.vlieg", "vadsrijk.npc.optie.vlieg." + naar);
                e.key("reis.vlieg", "vadsrijk.npc.optie.vlieg." + naar + ".slot");
            }
        });
        Set<String> zelf = Set.of("training", "kampwacht", "marktguh", "klasse", "questbord", "versteend", "luchtschip", "luchtschip_kamp",
                "luchtschip_z2", "luchtschip_z3", "luchtschip_z5");   // (QuestMotor.rol handles these itself, Z2Haken the frozen guhs)
        for (NpcDef d : alleNpcs().values()) {
            if (d.rol() != null && !zelf.contains(d.rol()) && !heeftEigenKlik(d.id())) {
                e.key("npc.rol", "vadsrijk.npc.optie.rol." + d.rol());
            }
        }
        nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.EXTRA.forEach((npc, l) -> l.forEach(o -> e.key("npc.extra", o.tekstKey())));
        e.controleer(helper, "NPC-keuzes", 60);
        helper.succeed();
    }

    /**
     * Every quest's dialogue (offer, how is it going, hand-in, the "talk to" pages and who speaks them) and the tracker's "where"
     * line of every objective (QuestStap: npc/poi/area/campfire/boss/everywhere), like the quest robot but without playing.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void questTeksten(GameTestHelper helper) {
        veilig(helper, () -> questTeksten0(helper));
    }

    static void questTeksten0(GameTestHelper helper) {
        Eisen e = new Eisen();
        String plek = nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.PLEK;
        Consumer<String> spreker = s -> {
            String id = s.contains(":") ? s.substring(0, s.indexOf(':')) : s;
            if (!id.equals("npc") && !id.equals("jij") && Npcs.van(id) != null) {
                e.key("quest.spreker", "vadsrijk.npc.naam." + id);
            }
        };
        for (Quest q : Quests.alle().values()) {
            String s = q.sleutel();
            for (int i = 0; i < q.aanbod().size(); i++) {
                e.key("quest.aanbod", s + ".aanbod." + i);
                spreker.accept(q.aanbod().get(i));
            }
            for (int i = 0; i < q.klaar().size(); i++) {
                e.key("quest.klaar", s + ".klaar." + i);
                spreker.accept(q.klaar().get(i));
            }
            q.praat().forEach((npc, l) -> {
                for (int i = 0; i < l.size(); i++) {
                    e.key("quest.praat", s + ".praat." + npc + "." + i);
                    spreker.accept(l.get(i));
                }
            });
            if (!q.gever().isEmpty() || !q.inleveren().isEmpty()) {
                e.key("quest.bezig", s + ".bezig");
            }
            for (String n : List.of(q.gever(), q.inleveren())) {
                if (!n.isEmpty()) {
                    e.key("quest.npc", "vadsrijk.npc.naam." + n);
                    e.key("stap.npc", plek + "npc." + n);
                }
            }
            for (Quest.Doel d : q.doelen()) {
                if (d.plek() != null && !d.plek().isEmpty() && nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek.pos(null, d.plek()) != null) {
                    e.key("stap.plek", nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek.tekstKey(d.plek()));
                    continue;
                }
                String doel = d.doel();
                String k = switch (d.type()) {
                    case PRAAT -> plek + "npc." + doel;
                    case ESCORTE -> plek + "poi." + (d.bron().startsWith("poi:") ? d.bron().substring(4) : d.bron());
                    case GEBIED, GEHEIM -> doel.startsWith("poi:") ? plek + "poi." + doel.substring(4)
                            : d.type() == Quest.Doel.Type.GEBIED ? plek + "gebied." + doel.split("\\|")[0] : plek + "overal." + doel;
                    case KAMPVUUR -> doel.equals("*") ? plek + "overal" : plek + "kampvuur." + doel;
                    case OPVROLIJK, ELITE -> doel.startsWith("gebied:") ? plek + "gebied." + doel.substring(7) : plek + "overal";
                    case VERZAMEL -> d.bron().startsWith("gebied:") ? plek + "gebied." + d.bron().substring(7) : plek + "overal";
                    case BAAS -> plek + "baas." + (doel.startsWith("gebied:") ? doel.substring(7) : doel.substring(doel.indexOf(':') + 1));
                    default -> null;
                };
                e.key("stap." + d.type().name().toLowerCase(java.util.Locale.ROOT), k);
                if (d.type() == Quest.Doel.Type.PRAAT || d.type() == Quest.Doel.Type.ESCORTE) {
                    e.key("stap.npc", plek + "npc." + doel);
                }
            }
        }
        e.controleer(helper, "quest-teksten", 1000);
        helper.succeed();
    }

    /** The secrets of zones 3 to 6 (found message, title card, page), like zone 1 and 2's in {@link #familiesPerId}. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void geheimenZones(GameTestHelper helper) {
        veilig(helper, () -> geheimenZones0(helper));
    }

    static void geheimenZones0(GameTestHelper helper) {
        Eisen e = new Eisen();
        java.util.function.BiConsumer<String, String[]> kaart = (bron, sjablonen) -> {
            String[] d = bron.split("#");
            if (!(statisch(d[0], d[1]) instanceof Map<?, ?> m) || m.isEmpty()) {
                e.fout("bronnen", bron + " is empty or gone");
                return;
            }
            for (Object id : m.keySet()) {
                for (String s : sjablonen) {
                    String nr = id.toString().substring(id.toString().lastIndexOf('_') + 1);
                    e.key(bron, s.replace("{id}", id.toString()).replace("{nr}", nr));
                }
            }
        };
        String z3 = "vadsrijk.z3npc.", z4 = "vadsrijk.z4verhaal.", z5 = "vadsrijk.z5verhaal.", z6 = "vadsrijk.z6verhaal.";
        kaart.accept("zone3.npc.Z3NpcPlekken#KNUFFEL", new String[] {z3 + "knuffel.{id}"});
        kaart.accept("zone3.npc.Z3NpcPlekken#AANWIJZING", new String[] {z3 + "aanwijzing.{id}"});
        kaart.accept("zone3.npc.Z3NpcPlekken#SCHAT", new String[] {z3 + "schat.{id}"});
        kaart.accept("zone3.npc.Z3NpcPlekken#RELIEF", new String[] {z3 + "relief.{id}"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#KNUFFEL", new String[] {z4 + "knuffel.{id}"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#FLESSENPOST", new String[] {z4 + "fles.{id}", z4 + "fles.titel.{id}"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#SCHAT", new String[] {z4 + "schat.{id}"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#UITZICHT", new String[] {z4 + "uitzicht.{id}", z4 + "uitzicht.titel.{id}"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#TIJKNUFFEL", new String[] {z4 + "tijknuffel.{id}.naam", z4 + "tijknuffel.{id}.verhaal"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#WRAKKNUFFEL", new String[] {z4 + "tijknuffel.{id}.naam", z4 + "tijknuffel.{id}.verhaal"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#GROTE_KNUFFEL", new String[] {z4 + "tijknuffel.{id}.naam", z4 + "tijknuffel.{id}.verhaal.0",
                z4 + "tijknuffel.{id}.verhaal.1", z4 + "tijknuffel.{id}.verhaal.2"});
        kaart.accept("zone4.verhaal.Z4VerhaalPlekken#MISTBRIEVEN", new String[] {z4 + "mistbrief.mistbrief_{nr}"});
        kaart.accept("zone5.verhaal.Z5VerhaalPlekken#KNUFFEL", new String[] {z5 + "knuffel.{id}"});
        kaart.accept("zone5.verhaal.Z5VerhaalPlekken#SCHAT", new String[] {z5 + "schat.{id}"});
        kaart.accept("zone6.verhaal.Z6VerhaalPlekken#KNUFFEL", new String[] {z6 + "knuffel.{id}"});
        kaart.accept("zone6.verhaal.Z6VerhaalPlekken#SCHAT", new String[] {z6 + "schat.{id}"});
        kaart.accept("zone6.verhaal.Z6VerhaalPlekken#BAKEN", new String[] {z6 + "bakens.{id}"});
        kaart.accept("zone6.verhaal.Z6VerhaalPlekken#VONDST", new String[] {z6 + "vondst.{id}"});
        if (statisch("zone6.verhaal.Z6VerhaalPlekken", "VONDST") instanceof Map<?, ?> v && v.get("z6_tabletten") instanceof Map<?, ?> t) {
            t.keySet().forEach(id -> e.key("z6.tablet", z6 + "tablet." + id));
        }
        e.controleer(helper, "geheimen zone 3-6", 80);
        helper.succeed();
    }

    /**
     * Every Vadsrijk item's name (tooltips, chat, the order board) and a boss item's effect line; the Bazen tab's rows (name, area,
     * cave name, cave pet); the sealed dungeon doors.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void itemsEnBazen(GameTestHelper helper) {
        veilig(helper, () -> itemsEnBazen0(helper));
    }

    static void itemsEnBazen0(GameTestHelper helper) {
        Eisen e = new Eisen();
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("guhs") || !id.getPath().startsWith("vr_")) {
                continue;
            }
            e.key("item.naam", item.getDescriptionId());
            if (item instanceof nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot.BaasItem && veld(item, "type") == null) {
                e.fout("item.effect", id.getPath() + ": no type");
            } else if (item instanceof nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot.BaasItem) {
                e.key("item.effect", "vadsrijk.bazen.effect." + veld(item, "type"));
            }
        }
        for (var b : nl.juiced.guhs.feature.vadsrijk.bazenboek.Bazenboek.alle()) {
            e.key("bazenboek.naam", b.naamKey);
            Object gebied = veld(b, "gebiedKey"), grot = veld(b, "dungeon"), pet = veld(b, "grotPet");
            e.key("bazenboek.gebied", gebied != null ? gebied.toString() : "vadsrijk.zonepoort.zone." + b.zone);
            if (grot != null) {
                e.key("bazenboek.grot", "vadsrijk.grotten.naam." + grot);
            }
            if (pet != null) {
                e.key("bazenboek.huisdier", "vadsrijk.huisdier." + pet + ".naam");
            }
        }
        for (Object set : items(statisch("items.Sets", "SET_VAN"))) {   // (the gear tooltip's set line)
            e.key("item.set", "vadsrijk.items.set." + set);
        }
        for (var d : nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit.DOELEN.values()) {
            e.key("rijdierbuit.per", "vadsrijk.vint.pech.per." + d.per());
        }
        for (var d : nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.ALLE) {
            if (d.zegel()) {
                e.key("dungeon.zegel", "vadsrijk.dungeon.zegel.bord." + d.dungeon());
                e.key("dungeon.zegel", "vadsrijk.dungeon.zegel.msg." + d.dungeon());
            }
        }
        e.controleer(helper, "items + bazen", 500);
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
