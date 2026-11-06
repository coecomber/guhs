package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.time.Instant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.vadsrijk.web.WebExport;

/**
 * U-GILDEN: the bond ranking for guhs.nl/vadsrijk, written with s-site's hiscores ({@code WebExport.EXTRA_HISCORES}: same folder,
 * timer and on/off switch) as {@code hiscores/bonden.json} in the same format as the other lists: {lijst, naam, tijd, cap,
 * rijen[plek, naam, waarde = level, xp]} + per row {@code tag}, {@code leden}, {@code kasteel}. Only public bond facts: names and
 * tags are limited to letters, digits, spaces and '-' when they are made ({@link BondActies#NAAM}), so they are safe on a page.
 */
public final class BondExport {
    public static final String LIJST = "bonden";

    private BondExport() {
    }

    public static JsonObject json(MinecraftServer s) {
        JsonObject o = new JsonObject();
        o.addProperty("lijst", LIJST);
        o.addProperty("naam", "Knuffelbonden");
        o.addProperty("tijd", Instant.ofEpochMilli(BondXp.KLOK.getAsLong()).toString());
        o.addProperty("cap", BondXp.MAX);
        JsonArray l = new JsonArray();
        int plek = 1;
        for (Bond b : Bonden.get(s).ranglijst()) {
            if (!BondActies.NAAM.matcher(b.naam).matches() || !BondActies.TAG.matcher(b.tag).matches()) {
                continue;
            }
            JsonObject r = new JsonObject();
            r.addProperty("plek", plek++);
            r.addProperty("naam", b.naam);
            r.addProperty("waarde", b.level());
            r.addProperty("xp", b.xp);
            r.addProperty("tag", b.tag);
            r.addProperty("leden", b.leden.size());
            r.addProperty("kasteel", b.kasteel);
            l.add(r);
        }
        o.add("rijen", l);
        return o;
    }

    /** Writes hiscores/bonden.json through WebExport (nothing while the live export is off, like the other lists). */
    public static String exporteer(MinecraftServer s) {
        WebExport.schrijf("hiscores/" + LIJST + ".json", json(s));
        return WebExport.map().resolve("hiscores").resolve(LIJST + ".json").toString();
    }
}
