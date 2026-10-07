package nl.juiced.guhs.feature.guhpixel.among;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * What a finished round of Among Guhs pays, and the personal numbers of every player.
 * <p>
 * Muntjes (the contract's values): {@link #VERLIES} for a lost round, {@link #WINST} for a won one, {@link #PER_TAAK} per
 * own finished task, everything times {@link #LASTIG} on Lastig, at most {@link #DAG_MAX} per real day (the daily pot
 * {@value #POT} of Muntjes). Only a round played to the end pays; leaving early pays nothing.
 * <p>
 * Numbers per player (PxData slice "among"): rounds, wins as crew and as Mika, times wrongly voted out, times caught as
 * the Mika, pushes, tasks, rounds and wins on Lastig. The titles and the hidden FTB advancements read these.
 */
public final class AmongBeloning {
    public static final int VERLIES = 25, WINST = 45, PER_TAAK = 2, DAG_MAX = 200;
    public static final double LASTIG = 1.5;
    public static final String POT = "among";
    public static final String RONDES = "Rondes", WINST_CREW = "WinstCrew", WINST_MIKA = "WinstMika", ONTERECHT = "Onterecht", BETRAPT = "Betrapt",
            GEDUWD = "Geduwd", TAKEN = "Taken", LASTIG_RONDES = "LastigRondes", LASTIG_WINST = "LastigWinst", WEGGESTEMD = "Weggestemd";

    /** The muntjes of one finished round, before the daily cap. */
    public static int bedrag(boolean gewonnen, int eigenTaken, boolean lastig) {
        int n = (gewonnen ? WINST : VERLIES) + PER_TAAK * Math.max(0, eigenTaken);
        return lastig ? (int) Math.round(n * LASTIG) : n;
    }

    public static CompoundTag cijfers(ServerPlayer p) {
        return PxData.deel(p, "among");
    }

    public static int cijfer(ServerPlayer p, String sleutel) {
        return cijfers(p).getIntOr(sleutel, 0);
    }

    /** Everything the personal stats board (client.CijfersScherm) shows: the numbers, today's pot, the titles. */
    public static CompoundTag cijfersTag(ServerPlayer p) {
        CompoundTag t = cijfers(p).copy();
        nl.juiced.guhs.taal.Tekst.put(t, "Naam", p.getName());
        t.putInt("Vandaag", Muntjes.vandaag(p, POT));
        t.putInt("DagMax", DAG_MAX);
        t.putInt("Oefenrondjes", nl.juiced.guhs.feature.guhpixel.Grappen.keren(p, OefenSessie.GRAP));
        net.minecraft.nbt.ListTag titels = new net.minecraft.nbt.ListTag();
        for (nl.juiced.guhs.feature.titels.Titels.Titel titel : nl.juiced.guhs.feature.guhpixel.GuhpixelTitels.ALLE) {
            if (titel.id().startsWith("among_")) {
                CompoundTag k = new CompoundTag();
                nl.juiced.guhs.taal.Tekst.put(k, "Naam", titel.naam());
                nl.juiced.guhs.taal.Tekst.put(k, "Hint", titel.hint());
                k.putBoolean("Heeft", titel.behaald().test(p));
                titels.add(k);
            }
        }
        t.put("Titels", titels);
        return t;
    }

    private static void plus(CompoundTag t, String sleutel, int n) {
        t.putInt(sleutel, t.getIntOr(sleutel, 0) + n);
    }

    /**
     * A player finished a round: the numbers go up, the muntjes are paid (as far as today's pot allows). Returns what was
     * really paid.
     */
    public static int rondeKlaar(ServerPlayer p, boolean mika, boolean gewonnen, boolean lastig, int eigenTaken, int duwen, boolean weggestemd) {
        CompoundTag t = cijfers(p);
        plus(t, RONDES, 1);
        plus(t, TAKEN, eigenTaken);
        plus(t, GEDUWD, duwen);
        if (gewonnen) {
            plus(t, mika ? WINST_MIKA : WINST_CREW, 1);
        }
        if (weggestemd) {
            plus(t, WEGGESTEMD, 1);
            plus(t, mika ? BETRAPT : ONTERECHT, 1);
        }
        if (lastig) {
            plus(t, LASTIG_RONDES, 1);
            if (gewonnen) {
                plus(t, LASTIG_WINST, 1);
            }
        }
        PxData.vuil(p.level().getServer());
        GuhAdvancements.grant(p, "among_ronde");
        if (gewonnen) {
            GuhAdvancements.grant(p, mika ? "among_mika_winst" : "among_crew_winst");
        }
        if (lastig) {
            GuhAdvancements.grant(p, "among_lastig");
        }
        return Muntjes.verdienDagelijks(p, POT, bedrag(gewonnen, mika ? 0 : eigenTaken, lastig), DAG_MAX);
    }

    private AmongBeloning() {
    }
}
