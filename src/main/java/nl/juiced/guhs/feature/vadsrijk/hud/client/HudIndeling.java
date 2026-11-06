package nl.juiced.guhs.feature.vadsrijk.hud.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;

/**
 * U-HUD: the player's own HUD layout. Every HUD part (a "deel") has a default box on screen; the player can move it
 * (dx, dy in GUI pixels) and scale it (0.5 - 2). Drawing a part = its normal drawing code under a pose transform
 * (translate to the new spot, scale, translate back from the default spot), so no part needs to know about the editor.
 * <p>
 * Parts come in three kinds, all automatic:
 * <ul>
 *   <li>kern's own parts in {@link RijkHud} (balken, xp, skills, info, baasbalk, tip): wrapped by kern's {@code deel(...)} hook;</li>
 *   <li>every {@link RijkHud#plek} spot: a plek() call inside a {@link RijkHud#voegToe} element moves that element along. A new
 *   plek id is registered by itself the first time it is asked (so new elements are editable without code here);</li>
 *   <li>other slices' own GUI layers ({@link #registreerLaag}): the whole layer is moved (RenderGuiLayerEvent Pre/Post).</li>
 * </ul>
 * Saved per client in {@code config/guhs-vadsrijk-hud.json}.
 */
public final class HudIndeling implements RijkHud.Plaatser {
    public static final float MIN_SCHAAL = 0.5f, MAX_SCHAAL = 2f;

    /** A part's default box on a gui of w x h: {x, y, breed, hoog}. */
    public interface Box {
        int[] box(int w, int h);
    }

    /** One editable part. {@code laag} = a GUI layer drawn as a whole under this part's transform (or null). */
    public record Deel(String id, String naamKey, Box box, @Nullable Identifier laag) {
        public Component naam() {
            return I18n.exists(naamKey) ? Component.translatable(naamKey) : Component.literal(id);
        }
    }

    /** The player's change of one part. */
    public record Stand(int dx, int dy, float schaal) {
        static final Stand STANDAARD = new Stand(0, 0, 1f);

        boolean standaard() {
            return dx == 0 && dy == 0 && Math.abs(schaal - 1f) < 0.001f;
        }
    }

    private static final Map<String, Deel> DELEN = new LinkedHashMap<>();
    private static final Map<Identifier, Deel> LAGEN = new LinkedHashMap<>();
    private static final Map<String, Stand> STAND = new LinkedHashMap<>();
    static final HudIndeling INSTANTIE = new HudIndeling();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // the transform stack of this frame: how many pushes are open per kind
    private int kernOpen;
    @Nullable
    private GuiGraphicsExtractor extraG;
    private int extraW, extraH;
    private boolean inExtra, extraGeduwd, inLaag, laagGeduwd;
    @Nullable
    private String extraId;

    private HudIndeling() {
    }

    // ---- registration -------------------------------------------------------------------------------------------------------------

    public static synchronized void registreer(String id, String naamKey, Box box) {
        DELEN.put(id, new Deel(id, naamKey, box, null));
    }

    /** A whole GUI layer as one part. */
    public static synchronized void registreerLaag(Identifier laag, String id, String naamKey, Box box) {
        Deel d = new Deel(id, naamKey, box, laag);
        DELEN.put(id, d);
        LAGEN.put(laag, d);
    }

    public static synchronized List<Deel> delen() {
        return new ArrayList<>(DELEN.values());
    }

    @Nullable
    public static Deel deel(String id) {
        return DELEN.get(id);
    }

    /** The default {@link RijkHud#plek} spot of id as a box. */
    static int[] plekBox(String id, int w, int h) {
        int[] d = RijkHud.standaardPlek(id, w, h);
        return switch (id) {
            case "buffs" -> new int[] {d[0], d[1] - 12, Math.max(16, d[2] - d[0]), d[3] + 12};   // (+ the status name flash above)
            case "drankjesriem" -> new int[] {d[0] - 1, d[1] - 10, 2 * d[3] + d[2] + 2, d[2] + 11};   // (+ the key labels above)
            case "doelframe" -> new int[] {d[0] - 1, d[1] - 1, d[2] + 2, d[3] + 6};
            case "gietbalk", "baasgiet" -> new int[] {d[0] - 2, d[1] - 12, d[2] + 4, d[3] + 14};   // (+ the spell name above)
            default -> new int[] {d[0], d[1], Math.max(16, d[2]), Math.max(8, d[3])};
        };
    }

    // ---- the layout ---------------------------------------------------------------------------------------------------------------

    public static Stand stand(String id) {
        return STAND.getOrDefault(id, Stand.STANDAARD);
    }

    public static void zet(String id, int dx, int dy, float schaal) {
        Stand s = new Stand(dx, dy, Mth.clamp(schaal, MIN_SCHAAL, MAX_SCHAAL));
        if (s.standaard()) {
            STAND.remove(id);
        } else {
            STAND.put(id, s);
        }
    }

    public static void reset(String id) {
        STAND.remove(id);
    }

    public static void resetAlles() {
        STAND.clear();
    }

    /** The default box of a part. */
    public static int[] standaardBox(String id, int w, int h) {
        Deel d = DELEN.get(id);
        return d == null ? plekBox(id, w, h) : d.box().box(w, h);
    }

    /** Where a part is now: {x, y, breed, hoog} on screen (scaled size). Kept on screen when moved. */
    public static float[] huidig(String id, int w, int h) {
        int[] b = standaardBox(id, w, h);
        Stand s = stand(id);
        if (s.standaard()) {
            return new float[] {b[0], b[1], b[2], b[3]};
        }
        float bw = b[2] * s.schaal(), bh = b[3] * s.schaal();
        float x = Mth.clamp(b[0] + s.dx(), 0, Math.max(0, w - bw)), y = Mth.clamp(b[1] + s.dy(), 0, Math.max(0, h - bh));
        return new float[] {x, y, bw, bh};
    }

    /** Pushes the part's transform (always; identity when not moved). */
    private static void duw(GuiGraphicsExtractor g, String id, int w, int h) {
        g.pose().pushMatrix();
        Stand s = stand(id);
        if (s.standaard()) {
            return;
        }
        int[] b = standaardBox(id, w, h);
        float[] n = huidig(id, w, h);
        g.pose().translate(n[0], n[1]);
        g.pose().scale(s.schaal(), s.schaal());
        g.pose().translate(-b[0], -b[1]);
    }

    // ---- RijkHud.Plaatser --------------------------------------------------------------------------------------------------------

    @Override
    public int[] plek(String id, int[] standaard, int w, int h) {
        if (!DELEN.containsKey(id)) {
            registreer(id, "vadsrijk.uhud.deel." + id, (ww, hh) -> plekBox(id, ww, hh));   // (a new spot: editable at once)
        }
        if (inExtra && extraG != null) {
            if (!extraGeduwd || !id.equals(extraId)) {
                if (extraGeduwd) {
                    extraG.pose().popMatrix();
                }
                duw(extraG, id, w, h);
                extraGeduwd = true;
                extraId = id;
            }
            return standaard;
        }
        if (inLaag || kernOpen > 0) {
            return standaard;   // (already moved as a whole)
        }
        Stand s = stand(id);
        if (s.standaard()) {
            return standaard;
        }
        // asked from outside any drawing (e.g. a hover test): just the moved position
        int[] b = standaardBox(id, w, h);
        float[] n = huidig(id, w, h);
        int ddx = Math.round(n[0] - b[0]), ddy = Math.round(n[1] - b[1]);
        int[] r = standaard.clone();
        r[0] += ddx;
        r[1] += ddy;
        if ("buffs".equals(id)) {
            r[2] += ddx;
        }
        return r;
    }

    @Override
    public void begin(GuiGraphicsExtractor g, String id, int w, int h) {
        duw(g, id, w, h);
        kernOpen++;
    }

    @Override
    public void eind(GuiGraphicsExtractor g) {
        if (kernOpen > 0) {
            kernOpen--;
            g.pose().popMatrix();
        }
    }

    @Override
    public void extraBegin(GuiGraphicsExtractor g, String id, int w, int h) {
        inExtra = true;
        extraG = g;
        extraW = w;
        extraH = h;
        extraGeduwd = false;
        extraId = null;
        Deel d = DELEN.get(id);   // (an element registered under its own voegToe id: moved as a whole)
        if (d != null && d.laag() == null) {
            duw(g, id, w, h);
            extraGeduwd = true;
            extraId = id;
        }
    }

    @Override
    public void extraEind(GuiGraphicsExtractor g) {
        if (extraGeduwd) {
            g.pose().popMatrix();
        }
        inExtra = false;
        extraGeduwd = false;
        extraG = null;
        extraId = null;
    }

    @Override
    public int[] vak(String id, int[] xyz, int w, int h) {
        Stand s = stand(id);
        if (s.standaard()) {
            return xyz;
        }
        int[] b = standaardBox(id, w, h);
        float[] n = huidig(id, w, h);
        return new int[] {Math.round(n[0] + (xyz[0] - b[0]) * s.schaal()), Math.round(n[1] + (xyz[1] - b[1]) * s.schaal()),
                Math.round(xyz[2] * s.schaal())};
    }

    /** A point drawn at (x, y) in part id's own coordinates, on screen. */
    public static float[] naarScherm(String id, float x, float y, int w, int h) {
        Stand s = stand(id);
        if (s.standaard()) {
            return new float[] {x, y};
        }
        int[] b = standaardBox(id, w, h);
        float[] n = huidig(id, w, h);
        return new float[] {n[0] + (x - b[0]) * s.schaal(), n[1] + (y - b[1]) * s.schaal()};
    }

    // ---- whole layers -------------------------------------------------------------------------------------------------------------

    static void init() {
        RijkHud.PLAATSER = INSTANTIE;
        laad();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderGuiLayerEvent.Pre e) -> {
            Deel d = LAGEN.get(e.getName());
            if (d != null) {   // (LOWEST: a cancelled layer never reaches us, so Post always follows)
                GuiGraphicsExtractor g = e.getGuiGraphics();
                duw(g, d.id(), g.guiWidth(), g.guiHeight());
                INSTANTIE.inLaag = true;
                INSTANTIE.laagGeduwd = true;
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderGuiLayerEvent.Post e) -> {
            if (INSTANTIE.laagGeduwd && LAGEN.containsKey(e.getName())) {
                e.getGuiGraphics().pose().popMatrix();
                INSTANTIE.inLaag = false;
                INSTANTIE.laagGeduwd = false;
            }
        });
    }

    // ---- saving -------------------------------------------------------------------------------------------------------------------

    static Path bestand() {
        return FMLPaths.CONFIGDIR.get().resolve("guhs-vadsrijk-hud.json");
    }

    static void laad() {
        Path p = bestand();
        STAND.clear();
        if (!Files.exists(p)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            JsonObject o = GSON.fromJson(r, JsonObject.class);
            JsonObject d = o == null ? null : o.getAsJsonObject("delen");
            if (d == null) {
                return;
            }
            for (String id : d.keySet()) {
                JsonObject s = d.getAsJsonObject(id);
                zet(id, s.get("dx").getAsInt(), s.get("dy").getAsInt(), s.has("schaal") ? s.get("schaal").getAsFloat() : 1f);
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[guhs] HUD layout {} unreadable, using the defaults: {}", p, ex.toString());
            STAND.clear();
        }
    }

    static void bewaar() {
        JsonObject o = new JsonObject();
        o.addProperty("versie", 1);
        JsonObject d = new JsonObject();
        for (Map.Entry<String, Stand> en : STAND.entrySet()) {
            JsonObject s = new JsonObject();
            s.addProperty("dx", en.getValue().dx());
            s.addProperty("dy", en.getValue().dy());
            s.addProperty("schaal", Math.round(en.getValue().schaal() * 100) / 100f);
            d.add(en.getKey(), s);
        }
        o.add("delen", d);
        try {
            Files.createDirectories(bestand().getParent());
            try (Writer w = Files.newBufferedWriter(bestand(), StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[guhs] could not save the HUD layout: {}", ex.toString());
        }
    }
}
