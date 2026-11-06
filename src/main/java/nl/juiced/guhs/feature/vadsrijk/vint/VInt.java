package nl.juiced.guhs.feature.vadsrijk.vint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs;

/**
 * V-INT (round U integration): the server-side glue between round-U slices that were built apart.
 * <ul>
 * <li>u-specs -> u-tabs: the specialisation line in the Karakter tab ({@code MenuServer.KARAKTER_EXTRA});</li>
 * <li>u-specs -> u-hud: the spec name under a party member's name ({@code PartyFrames.SPEC_BRON});</li>
 * <li>u-dubbel: u-ruiters' riders and the Knuffelkwartier are left out of the heuristic double scan.</li>
 * </ul>
 */
public final class VInt {
    private VInt() {
    }

    public static void register(IEventBus modBus) {
        RijdierBuit.register();   // the user's mount-drop rule: 10 % per player per kill/clear, sure after 15 misses
        // u-dubbel's heuristic scan: u-ruiters (one pair per route#k, own join guard + sweep) and everything in the Knuffelkwartier
        // (u-huizen's figures + one Mollie per ward share an npc id on purpose) are not doubles.
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.NIET_HEURISTISCH.add(e -> e instanceof nl.juiced.guhs.feature.vadsrijk.ruiters.RuiterGuh
                || e instanceof nl.juiced.guhs.feature.vadsrijk.ruiters.RuiterPluim);
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.NIET_HEURISTISCH.add(e -> "knuffelkwartier".equals(e.level().dimension().identifier().getPath()));
        nl.juiced.guhs.feature.vadsrijk.menu.MenuServer.KARAKTER_EXTRA.add((p, data) -> {
            ListTag extra = data.getListOrEmpty("extra");
            CompoundTag regel = new CompoundTag();
            Spec s = specVan(p);
            if (s != null) {
                regel.putString("k", "vadsrijk.vint.karakter.spec");
                regel.putString("a", s.naamKey());
            } else {
                regel.putString("k", "vadsrijk.vint.karakter.spec.geen");
            }
            extra.add(regel);
            data.put("extra", extra);
        });
        nl.juiced.guhs.feature.vadsrijk.hud.PartyFrames.SPEC_BRON.add(p -> {
            Spec s = specVan(p);
            return s == null ? null : s.naamKey();
        });
    }

    /** The spec of the player's current class (null: none chosen yet, or no class). */
    static Spec specVan(net.minecraft.world.entity.player.Player p) {
        try {
            if (Spelers.klasse(p) == null) {
                return null;
            }
            return Specs.spec(p);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
