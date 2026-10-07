package nl.juiced.guhs.feature.snuffel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.snuffel.Geuren.Geur;
import nl.juiced.guhs.network.ModNetworking;

/**
 * A player's own island state as their client gets it ({@code guhs:snuffel_stand}; sent at login and whenever it
 * changes): the chosen dog, the rank and the number of scents, the snuffelboekje (every learned scent with its kind and
 * picture), the good deeds, the tree's stage, the companion, the exam, the diploma. The client keeps the last one
 * (client.SnuffelClient) and draws the screens, the HUD and the tree from it.
 * <pre>
 * {Hond, Ras, Kleur, Naam, Maatje, Gekozen, Rang (1..5), Aantal, Geuren: [{Id, Soort, Icoon}], Daden: [id],
 *  Boom (0..4), BoomOver (ticks until the new stage shows), HeeftMaatje, Diploma, Klaar, Bezocht,
 *  Examen: {Id, Gevonden, Totaal}}
 * </pre>
 */
public final class Stand {
    private Stand() {
    }

    public static CompoundTag van(ServerPlayer p) {
        return van(p, 0);
    }

    static CompoundTag van(ServerPlayer p, int boomOver) {
        CompoundTag t = new CompoundTag();
        Keuze k = Keuze.vanOfStandaard(p);
        t.putBoolean("Hond", Hondvorm.actief(p));
        t.putBoolean("Gekozen", Keuze.heeft(p));
        t.putString("Ras", k.ras());
        t.putString("Kleur", k.kleur());
        t.putString("Naam", k.naam());
        t.putString("Maatje", k.maatje());
        t.putInt("Rang", Rang.van(p).nummer());
        t.putInt("Aantal", Geuren.aantal(p));
        ListTag geuren = new ListTag();
        for (Geur g : Geuren.boekje(p)) {
            CompoundTag x = new CompoundTag();
            x.putString("Id", g.id());
            x.putInt("Soort", g.soort().ordinal());
            x.putString("Icoon", g.icoon());
            geuren.add(x);
        }
        t.put("Geuren", geuren);
        ListTag daden = new ListTag();
        for (String d : Daden.van(p)) {
            daden.add(StringTag.valueOf(d));
        }
        t.put("Daden", daden);
        t.putInt("Boom", Boom.stap(p));
        t.putInt("BoomOver", boomOver);
        t.putBoolean("HeeftMaatje", Maatjes.heeft(p));
        t.putBoolean("Diploma", Snuffel.heeftDiploma(p));
        t.putBoolean("Klaar", SnuffelFeature.LIJN.klaar(p));
        t.putBoolean("Bezocht", Reis.bezocht(p));
        Examen.Loop loop = Examen.bezig(p);
        if (loop != null) {
            CompoundTag e = new CompoundTag();
            e.putString("Id", loop.examen().id());
            e.putInt("Gevonden", loop.gevonden());
            e.putInt("Totaal", loop.examen().bronnen().size());
            t.put("Examen", e);
        }
        return t;
    }

    /** Sends the player their own state. */
    public static void stuur(ServerPlayer p) {
        stuur(p, 0);
    }

    /** The same; the tree shows its new stage only after this many ticks (the growth scene pops it at the right moment). */
    static void stuur(ServerPlayer p, int boomOver) {
        if (p.connection == null) {
            return;
        }
        ModNetworking.sendTo(p, new SnuffelPayloads.StandBericht(van(p, boomOver)));
    }
}
