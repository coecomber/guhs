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
    private static final Map<String, BiConsumer<ServerPlayer, String>> KOPPELS = new ConcurrentHashMap<>();
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
            Map.entry("rijdier", List.of(V + "economie.Rijdieren#geefEerste")),
            Map.entry("mika_spion", List.of(V + "bazen.mika.MikaSpion#ontmasker")),
            Map.entry("advancement", List.of(V + "klassen.Advancement#doe")));

    private NpcRollen() {
    }

    /** Bind a role to an action of your slice (call from your registry class). */
    public static void koppel(String rol, Consumer<ServerPlayer> actie) {
        KOPPELS.put(rol, (p, a) -> actie.accept(p));
    }

    /** Bind a role that takes an argument (a destination, "klasse:tier", a mount id...). */
    public static void koppelMet(String rol, BiConsumer<ServerPlayer, String> actie) {
        KOPPELS.put(rol, actie);
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
        BiConsumer<ServerPlayer, String> k = KOPPELS.get(rol);
        if (k != null) {
            k.accept(p, arg);
            return true;
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
        List<Vec3> pad = new ArrayList<>(List.of(a, a.lerp(b, 0.35).add(0, -25, 0), a.lerp(b, 0.65).add(0, -20, 0), b));
        return roep(c, "vlieg", new Class<?>[] {ServerLevel.class, List.class, int.class, boolean.class}, sl, pad, 170, true) != NIETS;
    }

    /** Where the Kampklodder lives (wezens' WezensPlekken.KAMPKLODDER when it is in this tree). */
    static BlockPos kampklodderPlek() {
        Class<?> c = laad(V + "wezens.WezensPlekken");
        try {
            if (c != null && c.getField("KAMPKLODDER").get(null) instanceof BlockPos b) {
                return b;
            }
        } catch (ReflectiveOperationException e) {
            // (fall through)
        }
        return NpcPlekken.KAMPVIJVER.offset(16, 0, -17);
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
