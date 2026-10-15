package nl.juiced.guhs.feature.vadskracht;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import org.slf4j.Logger;

/**
 * What a guh gives in a Guhrad, in VK per second: a table per guh variant, as DATA ({@code data/guhs/vadskracht/guhrad.json},
 * written by tools/features/vadskracht.py, reloaded with the datapacks):
 * <pre>{"standaard": {"kracht": 10, "blij": 15}, "varianten": {"baltoguh": {"kracht": 20, "blij": 25}, ...}}</pre>
 * {@code kracht} = an ordinary day, {@code blij} = a happy guh (it was "blij" when it was put in the wheel, see
 * {@link #isBlij}). A variant that is not in the table gives the {@code standaard} numbers; without the file those are
 * {@link VadsGetallen#GUHRAD} and {@link VadsGetallen#GUHRAD_BLIJ}.
 */
public final class GuhradKracht {
    private static final Logger LOG = LogUtils.getLogger();
    public static final Identifier BESTAND = Guhs.id("vadskracht/guhrad.json");

    /** What one variant gives: on an ordinary day, and when it is happy. */
    public record Kracht(int kracht, int blij) {
        public int van(boolean isBlij) {
            return isBlij ? blij : kracht;
        }
    }

    private static final Kracht INGEBOUWD = new Kracht(VadsGetallen.GUHRAD, VadsGetallen.GUHRAD_BLIJ);
    private static volatile Kracht standaard = INGEBOUWD;
    private static volatile Map<String, Kracht> varianten = Map.of();

    /** The numbers of this variant id ("normal", "baltoguh"...). */
    public static Kracht van(String variant) {
        Kracht k = varianten.get(variant.toLowerCase(Locale.ROOT));
        return k != null ? k : standaard;
    }

    public static Kracht van(GuhVariant variant) {
        return van(variant.id());
    }

    /** What the guh with this saved data (a picked-up guh) gives. */
    public static int van(CompoundTag guh, boolean blij) {
        return van(guh.getStringOr("Variant", GuhVariant.NORMAL.id())).van(blij);
    }

    /** Is the guh with this saved data happy right now (the hartjesmeter's "blij", {@link Band#BLIJ_TOT})? */
    public static boolean isBlij(CompoundTag guh, long gameTime) {
        return guh.getCompoundOrEmpty("NeoForgeData").getLongOr(Band.BLIJ_TOT, 0L) > gameTime;
    }

    /** The reload listener ({@link VadskrachtFeature} adds it to the server's data). */
    static final ResourceManagerReloadListener LADER = GuhradKracht::laad;

    private static void laad(ResourceManager bron) {
        Kracht basis = INGEBOUWD;
        Map<String, Kracht> tabel = new HashMap<>();
        var resource = bron.getResource(BESTAND);
        if (resource.isPresent()) {
            try (Reader in = resource.get().openAsReader()) {
                JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
                if (json.has("standaard")) {
                    basis = lees(json.getAsJsonObject("standaard"), INGEBOUWD);
                }
                if (json.has("varianten")) {
                    for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("varianten").entrySet()) {
                        tabel.put(e.getKey().toLowerCase(Locale.ROOT), lees(e.getValue().getAsJsonObject(), basis));
                    }
                }
            } catch (Exception e) {
                LOG.error("Guhs vadskracht: {} is not readable, every guh gives the standard {} / {} VK", BESTAND, INGEBOUWD.kracht(), INGEBOUWD.blij(), e);
                basis = INGEBOUWD;
                tabel.clear();
            }
        }
        standaard = basis;
        varianten = Map.copyOf(tabel);
    }

    private static Kracht lees(JsonObject o, Kracht anders) {
        int kracht = o.has("kracht") ? Math.max(0, o.get("kracht").getAsInt()) : anders.kracht();
        int blij = o.has("blij") ? Math.max(0, o.get("blij").getAsInt()) : Math.max(kracht, anders.blij());
        return new Kracht(kracht, blij);
    }

    private GuhradKracht() {
    }
}
