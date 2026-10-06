package nl.juiced.guhs.feature.techsaus;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The words of the sauce machines (lang {@code gui.guhs.techsaus.*}, tools/features/tech_vloeistof.py): the names of the
 * four sauces, amounts in buckets with one decimal, and the line about a tank. Everything is a translatable Component: the
 * server never resolves text (a player may read Dutch or English).
 */
public final class SausTekst {
    static final String K = "gui.guhs.techsaus.";

    /** "kaassaus" / "kaasfrituursaus" / "water" / "melk"; any other fluid by its own name. */
    public static Component naam(FluidResource saus) {
        String id = id(saus);
        return id == null ? saus.getHoverName() : Component.translatable(K + "saus." + id);
    }

    /** The short id of one of the four sauces of the Guh-technologie (also the value of the Sausvat's block property). */
    @javax.annotation.Nullable
    public static String id(FluidResource saus) {
        if (saus.equals(Sauzen.kaassaus())) {
            return "kaassaus";
        }
        if (saus.equals(Sauzen.frituursaus())) {
            return "frituursaus";
        }
        if (saus.equals(Sauzen.water())) {
            return "water";
        }
        if (saus.equals(Sauzen.melk())) {
            return "melk";
        }
        return null;
    }

    /** An amount in buckets with one decimal ("2,5" in Dutch): mB rounded down to a tenth of a bucket. */
    public static Component emmers(int mb) {
        return Component.translatable(K + "getal", mb / Sauzen.EMMER, mb % Sauzen.EMMER / 100);
    }

    /** "Kaassaus: 2,5/4,0 emmers", or "Kaassaus: leeg (er past 4,0 emmers in)"; {@code hoort}: what an empty tank is for (EMPTY: anything). */
    public static MutableComponent tank(SausTank tank, FluidResource hoort) {
        if (tank.isLeeg()) {
            return hoort.isEmpty() ? Component.translatable(K + "tank.leeg", emmers(tank.max()))
                    : Component.translatable(K + "tank.leeg_van", naam(hoort), emmers(tank.max()));
        }
        return Component.translatable(K + "tank", naam(tank.saus()), emmers(tank.inhoud()), emmers(tank.max()));
    }

    private SausTekst() {
    }
}
