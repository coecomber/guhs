package nl.juiced.guhs.feature.vadsrijk.npc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The adapter between the npc slice and the screens/actions of the other slices (task: "a registry NpcRollen.koppel("smid",
 * Consumer&lt;ServerPlayer&gt;) that the other slice fills at integration").
 * <ul>
 *   <li>Another slice may bind a role from its registry class: {@code NpcRollen.koppel("smid", Smid::open)}, or with an
 *       argument {@code NpcRollen.koppelMet("luchtschip", (p, naar) -> ...)}. A binding always wins.</li>
 *   <li>Until then {@link #doe} calls the APIs the other slices published in their worktrees (2026-11-03), by reflection so this
 *       tree compiles without them: klassen {@code KlasseBalie.kiesEerste/open}, {@code Advancement.doe}; economie
 *       {@code Smid.open}, {@code Winkels.open(p, id)}, {@code Markt.open}, {@code Stal.open}, {@code Rijdieren.geefEerste};
 *       bazen {@code MikaSpion.ontmasker}; wereld {@code WereldReizen.vlieg(p, Dok, Dok)}; wezens {@code DraakVlucht.vlieg/cirkel},
 *       {@code Kampklodder.plaats}. It returns false when nothing is there: the caller shows its own fallback.</li>
 * </ul>
 * Role ids: klasse_kiezen, ordetempel, smid, winkel, wapenwinkel, kleermaker, markt, bank, stal, rijdier, mika_spion,
 * advancement, luchtschip (arg "zwamzoet"/"knuffelkamp"), draak (arg "overvlucht"/"ver"), kampklodder, bam_baas (binding only).
 */
public final class NpcRollen {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, java.util.function.BiPredicate<ServerPlayer, String>> KOPPELS = new ConcurrentHashMap<>();
    private static final String V = "nl.juiced.guhs.feature.vadsrijk.";
    /** role -> "class#method" or "class#method@fixed argument" (player first, then the argument when the method takes one). */
    private static final Map<String, List<String>> API = Map.ofEntries(
            Map.entry("klasse_kiezen", List.of(V + "klassen.KlasseBalie#kiesEerste", V + "klassen.KlasseBalie#open")),
            Map.entry("ordetempel", List.of(V + "klassen.KlasseBalie#open")),
            Map.entry("smid", List.of(V + "economie.Smid#open")),
            Map.entry("winkel", List.of(V + "economie.Winkels#open@drankjes")),
            Map.entry("wapenwinkel", List.of(V + "economie.Winkels#open@uitrusting")),
            Map.entry("kleermaker", List.of(V + "economie.Winkels#open@cosmetica")),
            Map.entry("markt", List.of(V + "economie.Markt#open")),
            Map.entry("stal", List.of(V + "economie.Stal#open")),
            Map.entry("rijdier", List.of(V + "economie.Rijdieren#questBeloning")),
            Map.entry("mika_spion", List.of(V + "bazen.mika.MikaSpion#ontmasker")),
            Map.entry("advancement", List.of(V + "klassen.Advancement#doe")));

    private NpcRollen() {
    }

    /** Bind a role to an action of your slice (call from your registry class). */
    public static void koppel(String rol, Consumer<ServerPlayer> actie) {
        KOPPELS.put(rol, (p, a) -> {
            actie.accept(p);
            return true;
        });
    }

    /** Bind a role whose action may answer "nothing happened" (false: the caller shows its own fallback). */
    public static void koppelAls(String rol, java.util.function.BiPredicate<ServerPlayer, String> actie) {
        KOPPELS.put(rol, actie);
    }

    /** Bind a role that takes an argument (a destination, "klasse:tier", a mount id...). */
    public static void koppelMet(String rol, BiConsumer<ServerPlayer, String> actie) {
        KOPPELS.put(rol, (p, a) -> {
            actie.accept(p, a);
            return true;
        });
    }

    /**
     * Integration 1.10.0: all slices are in one tree now, so the roles are bound to the real APIs directly (no reflection).
     * Only wereld's airship stays on reflection until the wereld slice is merged.
     */
    public static void koppelAlles() {
        String V0 = V;   // (keeps the reflection table for the wereld role)
        koppelAls("klasse_kiezen", (p, a) -> {
            if (!nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.kiesEerste(p)) {
                nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.open(p);
            }
            return true;
        });
        koppel("ordetempel", nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie::open);
        koppel("smid", nl.juiced.guhs.feature.vadsrijk.economie.Smid::open);
        koppel("winkel", p -> nl.juiced.guhs.feature.vadsrijk.economie.Winkels.open(p, "drankjes"));
        koppel("wapenwinkel", p -> nl.juiced.guhs.feature.vadsrijk.economie.Winkels.open(p, "uitrusting"));
        koppel("kleermaker", p -> nl.juiced.guhs.feature.vadsrijk.economie.Winkels.open(p, "cosmetica"));
        koppel("markt", nl.juiced.guhs.feature.vadsrijk.economie.Markt::open);
        koppel("stal", nl.juiced.guhs.feature.vadsrijk.economie.Stal::open);
        koppelAls("rijdier", (p, a) -> nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.questBeloning(p));   // (I-MOUNTS: the Wolkenschaapje)
        koppelAls("mika_spion", (p, a) -> nl.juiced.guhs.feature.vadsrijk.bazen.mika.MikaSpion.ontmasker(p));
        koppelAls("advancement", (p, a) -> a != null && a.contains(":")   // (g-klassen: the quest's own class, "klasse:stap")
                ? nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.doe(p, a.substring(0, a.indexOf(':')))
                : nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.doe(p));
        koppelAls("kampklodder", (p, a) -> p.level() instanceof ServerLevel sl
                && nl.juiced.guhs.feature.vadsrijk.wezens.Kampklodder.plaats(sl, kampklodderPlek()) != null);
        koppelAls("draak", NpcRollen::draakDirect);
        koppelAls("luchtschip", (p, naar) -> {
            boolean stad = !naar.equals("knuffelkamp");
            var kamp = nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen.Dok.KAMP;
            var haven = nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen.Dok.HAVEN;
            nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen.vlieg(p, stad ? kamp : haven, stad ? haven : kamp);
            return true;
        });
        // (integration, zone 2) the airship line + gondola are z2-wereld's (Z2Reizen / Z2Gondel: gate, landing, the opening scene)
        koppelAls("z2_luchtlijn", (p, naar) -> naar.equals("haven")
                ? nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2Reizen.naarHaven(p)
                : nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2Reizen.naarKandijkom(p));
        koppelAls("z2_gondelbaan", (p, a) -> nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2Gondel.rijd(p, a.equals("op")));
        koppelAls("z2_boormeester", (p, a) -> nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boormeester.ontgrendel(p));
        // g-int (round 2): the Knuffelkamp trainers hand out their class's starter weapon (first one free, then WAPEN_PRIJS brons)
        for (String[] t : new String[][] {{"trainer_ridder", nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.RIDDER},
                {"trainer_jager", nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.JAGER},
                {"trainer_magier", nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.MAGIER}}) {
            String klasse = t[1];
            QuestMotor.EXTRA.computeIfAbsent(t[0], k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(new QuestMotor.ExtraOptie(
                    "vadsrijk.npc.optie.startwapen." + klasse, p -> true,
                    p -> nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.geefStartwapen(p, klasse,
                            nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.WAPEN_PRIJS)));
        }
        nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.VIA_BALIE = false;
        LOG.debug("Vadsrijk npc: all {} roles bound directly ({})", KOPPELS.size(), V0);
    }

    /** The dragon through wezens' DraakVlucht (see {@link #draak}). */
    private static boolean draakDirect(ServerPlayer p, String soort) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return false;
        }
        if (soort.equals("ver")) {
            return nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.cirkel(sl, new Vec3(p.getX() + 90, 0, p.getZ() - 110), 70.0,
                    p.getY() + 60, 320) != null;
        }
        Vec3 a = NpcPlekken.DRAAK_VAN, b = NpcPlekken.DRAAK_NAAR;
        // (balans 2026-11) the dragon is 1.5x bigger: the dip is 11/9 blocks less deep so its wings clear the camp's roofs and trees
        List<Vec3> pad = new ArrayList<>(List.of(a, a.lerp(b, 0.35).add(0, -14, 0), a.lerp(b, 0.65).add(0, -11, 0), b));
        return nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, pad, 170, true) != null;
    }

    public static boolean gekoppeld(String rol) {
        return KOPPELS.containsKey(rol);
    }

    /** Dev/tests: forget a binding. */
    static void ontkoppel(String rol) {
        KOPPELS.remove(rol);
    }

    public static boolean doe(String rol, ServerPlayer p) {
        return doe(rol, p, "");
    }

    /** Does the role: the binding, else the other slice's API; false = nothing in this tree (show your own fallback). */
    public static boolean doe(String rol, ServerPlayer p, String arg) {
        java.util.function.BiPredicate<ServerPlayer, String> k = KOPPELS.get(rol);
        if (k != null) {
            return k.test(p, arg);
        }
        switch (rol) {
            case "luchtschip" -> {
                return luchtschip(p, arg);
            }
            case "draak" -> {
                return draak(p, arg);
            }
            case "kampklodder" -> {
                return p.level() instanceof ServerLevel sl
                        && roep(V + "wezens.Kampklodder", "plaats", new Class<?>[] {ServerLevel.class, BlockPos.class}, sl, kampklodderPlek()) != NIETS;
            }
            default -> {
            }
        }
        for (String naam : API.getOrDefault(rol, List.of())) {
            if (reflectie(naam, p, arg)) {
                return true;
            }
        }
        return false;
    }

    /** Is there anything (binding or the other slice's API) for this role? */
    public static boolean bestaat(String rol) {
        if (KOPPELS.containsKey(rol)) {
            return true;
        }
        String klasse = switch (rol) {
            case "luchtschip" -> V + "wereld.WereldReizen";
            case "draak" -> V + "wezens.draak.DraakVlucht";
            case "kampklodder" -> V + "wezens.Kampklodder";
            default -> null;
        };
        if (klasse != null) {
            return laad(klasse) != null;
        }
        for (String naam : API.getOrDefault(rol, List.of())) {
            if (methode(naam) != null) {
                return true;
            }
        }
        return false;
    }

    /** Set a public static field of another slice if it is in this tree (e.g. klassen's Advancement.VIA_BALIE). */
    static boolean zetVeld(String klasseVeld, Object waarde) {
        int h = klasseVeld.indexOf('#');
        Class<?> c = laad(klasseVeld.substring(0, h));
        try {
            if (c == null) {
                return false;
            }
            c.getField(klasseVeld.substring(h + 1)).set(null, waarde);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            return false;
        }
    }

    // ---- the special ones ---------------------------------------------------------------------------------------------------------

    /** wereld: WereldReizen.vlieg(p, Dok van, Dok naar), Dok = KAMP / HAVEN. */
    private static boolean luchtschip(ServerPlayer p, String naar) {
        Class<?> dok = laad(V + "wereld.WereldReizen$Dok");
        if (dok == null) {
            return false;
        }
        boolean stad = !naar.equals("knuffelkamp");
        Object van = dokVan(dok, stad ? "KAMP" : "HAVEN"), heen = dokVan(dok, stad ? "HAVEN" : "KAMP");
        return roep(V + "wereld.WereldReizen", "vlieg", new Class<?>[] {ServerPlayer.class, dok, dok}, p, van, heen) != NIETS;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object dokVan(Class<?> dok, String naam) {
        return Enum.valueOf((Class<? extends Enum>) dok, naam);
    }

    /**
     * wezens: the dragon. "overvlucht" = DraakVlucht.vlieg over the camp (with a roar; the entity is shared, so everyone near
     * the camp sees it too), "ver" = one slow circle far away on the horizon (the cliffhanger).
     */
    private static boolean draak(ServerPlayer p, String soort) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return false;
        }
        String c = V + "wezens.draak.DraakVlucht";
        if (soort.equals("ver")) {
            return roep(c, "cirkel", new Class<?>[] {ServerLevel.class, Vec3.class, double.class, double.class, int.class}, sl,
                    new Vec3(p.getX() + 90, 0, p.getZ() - 110), 70.0, p.getY() + 60, 320) != NIETS;
        }
        Vec3 a = NpcPlekken.DRAAK_VAN, b = NpcPlekken.DRAAK_NAAR;
        List<Vec3> pad = new ArrayList<>(List.of(a, a.lerp(b, 0.35).add(0, -14, 0), a.lerp(b, 0.65).add(0, -11, 0), b));
        return roep(c, "vlieg", new Class<?>[] {ServerLevel.class, List.class, int.class, boolean.class}, sl, pad, 170, true) != NIETS;
    }

    /** Where the Kampklodder lives (wezens' WezensPlekken.KAMPKLODDER when it is in this tree). */
    static BlockPos kampklodderPlek() {
        return nl.juiced.guhs.feature.vadsrijk.wezens.WezensPlekken.KAMPKLODDER;
    }

    // ---- reflection ---------------------------------------------------------------------------------------------------------------

    private static final Object NIETS = new Object();

    private static Class<?> laad(String naam) {
        try {
            return Class.forName(naam);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** Calls a static method with exactly these parameter types; NIETS when it is not there or failed. */
    private static Object roep(String klasse, String methode, Class<?>[] types, Object... args) {
        Class<?> c = laad(klasse);
        if (c == null) {
            return NIETS;
        }
        try {
            Method m = c.getMethod(methode, types);
            Object r = m.invoke(null, args);
            return r == null && m.getReturnType() != void.class ? NIETS : r;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Vadsrijk npc: adapter {}.{} failed", klasse, methode, e);
            return NIETS;
        }
    }

    private static Method methode(String naam) {
        int h = naam.indexOf('#'), at = naam.indexOf('@');
        String m = naam.substring(h + 1, at < 0 ? naam.length() : at);
        Class<?> c = laad(naam.substring(0, h));
        if (c == null) {
            return null;
        }
        for (Method x : c.getMethods()) {
            if (x.getName().equals(m) && Modifier.isStatic(x.getModifiers())) {
                Class<?>[] t = x.getParameterTypes();
                if ((t.length == 1 || t.length == 2 && t[1] == String.class) && t[0].isAssignableFrom(ServerPlayer.class)) {
                    return x;
                }
            }
        }
        return null;
    }

    private static boolean reflectie(String naam, ServerPlayer p, String arg) {
        Method m = methode(naam);
        if (m == null) {
            return false;
        }
        int at = naam.indexOf('@');
        String a = at < 0 ? arg : naam.substring(at + 1);
        try {
            Object r = m.getParameterCount() == 1 ? m.invoke(null, p) : m.invoke(null, p, a);
            return !(r instanceof Boolean b) || b;   // (a method that answers false did nothing: try the next one)
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Vadsrijk npc: role adapter {} failed", naam, e);
            return false;
        }
    }
}
