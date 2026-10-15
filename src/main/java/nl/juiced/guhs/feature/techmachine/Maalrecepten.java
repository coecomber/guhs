package nl.juiced.guhs.feature.techmachine;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import org.slf4j.Logger;

/**
 * What the Vadsmolen grinds, as DATA ({@code data/guhs/techmachine/malen.json}, written by tools/features/tech_machines.py,
 * reloaded with the datapacks; other slices and datapacks may add to it by shipping their own file of that name):
 * <pre>{"recepten": [{"in": "#guhs:knus/knabbelgraan", "uit": "guhs:knabbelmeel", "aantal": 1, "ticks": 20}, ...]}</pre>
 * {@code in} is an item or (with #) an item tag; one of it becomes {@code aantal} of {@code uit} after {@code ticks} ticks
 * of work. The first recipe that fits wins.
 */
public final class Maalrecepten {
    private static final Logger LOG = LogUtils.getLogger();
    public static final Identifier BESTAND = Guhs.id("techmachine/malen.json");
    public static final int STANDAARD_TICKS = 20;

    /** One thing the molen grinds. */
    public record Recept(@Nullable Item item, @Nullable TagKey<Item> tag, Item uit, int aantal, int ticks) {
        public boolean past(ItemStack stack) {
            return !stack.isEmpty() && (item != null ? stack.is(item) : tag != null && stack.is(tag));
        }

        public ItemStack resultaat() {
            return new ItemStack(uit, aantal);
        }
    }

    private static volatile List<Recept> recepten = List.of();

    /** The recipe for this stack, or null when the molen does not grind it. */
    @Nullable
    public static Recept van(ItemStack stack) {
        for (Recept r : recepten) {
            if (r.past(stack)) {
                return r;
            }
        }
        return null;
    }

    public static List<Recept> alle() {
        return recepten;
    }

    /** The reload listener ({@link TechmachineFeature} adds it to the server's data). */
    static final ResourceManagerReloadListener LADER = Maalrecepten::laad;

    private static void laad(ResourceManager bron) {
        List<Recept> lijst = new ArrayList<>();
        var resource = bron.getResource(BESTAND);
        if (resource.isPresent()) {
            try (Reader in = resource.get().openAsReader()) {
                JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
                for (JsonElement e : json.getAsJsonArray("recepten")) {
                    Recept r = lees(e.getAsJsonObject());
                    if (r != null) {
                        lijst.add(r);
                    }
                }
            } catch (Exception e) {
                LOG.error("Guhs techmachine: {} is not readable, the Vadsmolen grinds nothing", BESTAND, e);
                lijst.clear();
            }
        }
        recepten = List.copyOf(lijst);
    }

    @Nullable
    private static Recept lees(JsonObject o) {
        String in = o.get("in").getAsString();
        Item uit = BuiltInRegistries.ITEM.getValue(Identifier.parse(o.get("uit").getAsString()));
        int aantal = o.has("aantal") ? Math.max(1, o.get("aantal").getAsInt()) : 1;
        int ticks = o.has("ticks") ? Math.max(1, o.get("ticks").getAsInt()) : STANDAARD_TICKS;
        if (uit == Items.AIR) {
            LOG.warn("Guhs techmachine: malen.json: unknown item {}", o.get("uit"));
            return null;
        }
        if (in.startsWith("#")) {
            return new Recept(null, TagKey.create(Registries.ITEM, Identifier.parse(in.substring(1))), uit, aantal, ticks);
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(in));
        if (item == Items.AIR) {
            LOG.warn("Guhs techmachine: malen.json: unknown item {}", in);
            return null;
        }
        return new Recept(item, null, uit, aantal, ticks);
    }

    private Maalrecepten() {
    }
}
