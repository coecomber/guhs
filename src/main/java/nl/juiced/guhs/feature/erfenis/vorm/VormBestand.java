package nl.juiced.guhs.feature.erfenis.vorm;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * One exported shape (a building level, a plukje, a deco piece) from {@code data/guhs/erfenis/vormen/**}, made by
 * {@code renders-erfenis/_bron/export_vormen.py} from the approved render scripts. Local coordinates: x 0..w-1 (east),
 * z 0..d-1 (south), y = 0 the ground layer, y &gt;= 1 above it (y = -1 only when the shape digs in). A palette entry is a
 * literal block state, or {@code @name}: a palette role (a variant with one block state per palette 1..5, paletten.json).
 */
final class VormBestand {
    static final String MAP = "/data/guhs/erfenis/vormen/";
    private static final Map<String, Optional<VormBestand>> CACHE = new ConcurrentHashMap<>();
    private static volatile Map<String, String[]> varianten;
    /** Block states the game does not know (id or a property): logged once, listed for the gametest. */
    static final Map<String, String> ONBEKEND = new ConcurrentHashMap<>();
    private static final Map<String, BlockState> STATES = new ConcurrentHashMap<>();

    final int w, d, ymin, ymax;
    final String[] pal;
    /** Palette index per cell, order y (ymin..ymax), z, x. */
    final short[] vol;
    final List<Wezen> wezens;
    private final BlockState[][] perPalet = new BlockState[6][];

    /** A creature marker: t = entity soort ("knabbelkippetje", "npc", "guh" ...), position in local block units. */
    record Wezen(String t, String kind, String skin, double x, double y, double z, float yaw, float schaal, boolean zit) {
    }

    private VormBestand(JsonObject o) {
        w = o.get("w").getAsInt();
        d = o.get("d").getAsInt();
        ymin = o.get("ymin").getAsInt();
        ymax = o.get("ymax").getAsInt();
        JsonArray p = o.getAsJsonArray("pal");
        pal = new String[p.size()];
        for (int i = 0; i < pal.length; i++) {
            pal[i] = p.get(i).getAsString();
        }
        vol = new short[w * d * (ymax - ymin + 1)];
        JsonArray rle = o.getAsJsonArray("rle");
        int k = 0;
        for (int i = 0; i + 1 < rle.size(); i += 2) {
            short v = (short) rle.get(i).getAsInt();
            int n = rle.get(i + 1).getAsInt();
            for (int j = 0; j < n && k < vol.length; j++) {
                vol[k++] = v;
            }
        }
        List<Wezen> lijst = new ArrayList<>();
        if (o.has("wezens")) {
            for (JsonElement e : o.getAsJsonArray("wezens")) {
                JsonObject m = e.getAsJsonObject();
                lijst.add(new Wezen(m.get("t").getAsString(), str(m, "kind"), str(m, "skin"), m.get("x").getAsDouble(),
                        m.get("y").getAsDouble(), m.get("z").getAsDouble(), m.get("yaw").getAsFloat(),
                        m.has("schaal") ? m.get("schaal").getAsFloat() : 1f, m.has("sit") && m.get("sit").getAsBoolean()));
            }
        }
        wezens = List.copyOf(lijst);
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    int hoogte() {
        return ymax - ymin + 1;
    }

    /** Palette index at a local cell (0 = air). */
    int at(int x, int y, int z) {
        return vol[((y - ymin) * d + z) * w + x];
    }

    /** The block states of the palette entries for palette 1..5. */
    BlockState[] states(int palet) {
        int p = Math.max(1, Math.min(5, palet));
        BlockState[] s = perPalet[p];
        if (s == null) {
            s = new BlockState[pal.length];
            for (int i = 0; i < pal.length; i++) {
                String t = pal[i];
                if (t.startsWith("@")) {
                    String[] v = varianten().get(t.substring(1));
                    t = v == null ? "minecraft:air" : v[p - 1];
                    if (v == null) {
                        ONBEKEND.putIfAbsent(pal[i], "variant ontbreekt");
                    }
                }
                s[i] = state(t);
            }
            perPalet[p] = s;
        }
        return s;
    }

    // ------------------------------------------------------------------------------------------------- loading

    /** The shape at {@code pad} (relative to the vormen folder, without .json), or null when there is none. */
    static VormBestand laad(String pad) {
        return CACHE.computeIfAbsent(pad, k -> Optional.ofNullable(lees(k)).map(VormBestand::new)).orElse(null);
    }

    static JsonObject lees(String pad) {
        try (InputStream in = VormBestand.class.getResourceAsStream(MAP + pad + ".json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("guhs").error("Erfenis-vorm {} niet te lezen", pad, e);
            return null;
        }
    }

    static Map<String, String[]> varianten() {
        Map<String, String[]> v = varianten;
        if (v == null) {
            v = new HashMap<>();
            JsonObject o = lees("paletten");
            if (o != null) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("varianten").entrySet()) {
                    JsonArray a = e.getValue().getAsJsonArray();
                    String[] s = new String[5];
                    for (int i = 0; i < 5; i++) {
                        s[i] = a.get(i).getAsString();
                    }
                    v.put(e.getKey(), s);
                }
            }
            varianten = v;
        }
        return v;
    }

    /** Parses "ns:id[k=v,...]" into a block state; unknown ids become air, unknown properties are skipped (both noted). */
    static BlockState state(String tekst) {
        return STATES.computeIfAbsent(tekst, VormBestand::parse);
    }

    private static BlockState parse(String tekst) {
        int h = tekst.indexOf('[');
        String id = h < 0 ? tekst : tekst.substring(0, h);
        Identifier rl = Identifier.tryParse(id.contains(":") ? id : "guhs:" + id);
        Optional<Block> blok = rl == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(rl);
        if (blok.isEmpty()) {
            ONBEKEND.putIfAbsent(tekst, "blok bestaat niet");
            return Blocks.AIR.defaultBlockState();
        }
        BlockState s = blok.get().defaultBlockState();
        if (h >= 0) {
            for (String kv : tekst.substring(h + 1, tekst.length() - 1).split(",")) {
                int is = kv.indexOf('=');
                if (is < 0) {
                    continue;
                }
                s = zet(s, kv.substring(0, is), kv.substring(is + 1), tekst);
            }
        }
        if (s.hasProperty(LeavesBlock.PERSISTENT)) {
            s = s.setValue(LeavesBlock.PERSISTENT, true);   // decoration leaves never decay
        }
        return s;
    }

    private static <T extends Comparable<T>> BlockState zet(BlockState s, String naam, String waarde, String tekst) {
        @SuppressWarnings("unchecked")
        Property<T> p = (Property<T>) s.getBlock().getStateDefinition().getProperty(naam);
        if (p == null) {
            ONBEKEND.putIfAbsent(tekst, "eigenschap " + naam + " bestaat niet");
            return s;
        }
        Optional<T> v = p.getValue(waarde);
        if (v.isEmpty()) {
            ONBEKEND.putIfAbsent(tekst, "waarde " + naam + "=" + waarde + " bestaat niet");
            return s;
        }
        return s.setValue(p, v.get());
    }
}
