package nl.juiced.guhs.feature.favorietjes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Favorieten;

/**
 * The colour of every piece of guh clothing, for the colour favourite ({@link Favorieten#kleur}). The table is made by
 * tools/features/favorietjes.py from the clothes textures themselves (the colour most of the piece is), with the name
 * winning where it says it ("gouden", "roze", "black"...). Hairstyles have no colour of their own (the kapper tints them).
 */
public final class KledingKleuren {
    private KledingKleuren() {
    }

    /** Fills {@link Favorieten}'s clothes -&gt; colour table from the generated data. */
    static void laad() {
        for (Map.Entry<String, JsonElement> e : FavorietjesData.deel("kleuren").entrySet()) {
            GuhClothes stuk = GuhClothes.byId(e.getKey());
            String kleur = e.getValue().getAsString();
            if (stuk != null && Favorieten.KLEUREN.contains(kleur)) {
                Favorieten.kleur(stuk, kleur);
            }
        }
    }

    /** Every piece of this colour. */
    public static List<GuhClothes> van(String kleur) {
        List<GuhClothes> out = new ArrayList<>();
        for (GuhClothes c : GuhClothes.values()) {
            if (kleur.equals(Favorieten.kleur(c))) {
                out.add(c);
            }
        }
        return out;
    }

    /** Does this guh wear something of this colour right now? */
    public static boolean draagt(GuhEntity guh, @Nullable String kleur) {
        if (kleur == null) {
            return false;
        }
        for (GuhClothes.Slot slot : GuhClothes.Slot.kleding()) {
            GuhClothes c = guh.getClothes(slot);
            if (c != null && kleur.equals(Favorieten.kleur(c))) {
                return true;
            }
        }
        return false;
    }
}
