package nl.juiced.guhs.feature.snuffel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The front door of Het Snuffeleiland for the slices that build on the kern (the dock, the village): one place with the
 * things a story step wants to do. Everything is per player. The pieces themselves: {@link Reis} (travel), {@link Keuze}
 * (the chosen dog and companion), {@link Bewoners} (residents and their roles), {@link Geuren} / {@link Geurbronnen}
 * (scents and where they are), {@link Daden} (good deeds), {@link Boom} (the tree and its growth scene), {@link Maatjes}
 * (the companion), {@link Examen}, {@link Rang}, {@link SnuffelFeature#LIJN} (the questline).
 */
public final class Snuffel {
    private record DoelBron(int van, int tot, BiFunction<ServerPlayer, Integer, Doel> bron) {
    }

    private static final List<DoelBron> DOELEN = new CopyOnWriteArrayList<>();

    private record SleutelBron(int van, int tot, BiFunction<ServerPlayer, Integer, String> bron) {
    }

    private static final List<SleutelBron> SLEUTELS_VAN = new CopyOnWriteArrayList<>();
    /**
     * Every text variant of a step of the questline besides the step numbers ("4_bot": the Guhdex and the objective line
     * then show {@code gui.guhs.verhalen.snuffeleiland.nu/waar/kort.4_bot}). A slice that gives a step variants names them
     * here (the texts: tools/features/snuffel.py EXTRA) and registers who picks one with {@link #sleutel}.
     */
    static final String[] SLEUTELS = {
            // snuffel-dorp (the steps 2-8)
            "thuis", "4_bot", "4_fluit", "4_bij", "4_terug", "6_zoek", "6_breng", "7_bezig", "7_diploma", "8_snuffel",
    };

    private Snuffel() {
    }

    // --- travel ---------------------------------------------------------------------------------------------------------------

    /** The dock's captain sails the player out for the first time: they wash ashore on the beach, a dog. */
    public static boolean spoelAan(ServerPlayer p) {
        return Reis.naarEiland(p, Reis.Aankomst.STRAND);
    }

    /** A later crossing with the dock's captain: the boat lands in the island's harbour. */
    public static boolean vaarNaarEiland(ServerPlayer p) {
        return Reis.naarEiland(p, Reis.bezocht(p) ? Reis.Aankomst.HAVEN : Reis.Aankomst.STRAND);
    }

    /** Home, exactly where the player left from (the memory card and the harbour captain do this). */
    public static boolean naarHuis(ServerPlayer p) {
        return Reis.naarHuis(p);
    }

    // --- the story ------------------------------------------------------------------------------------------------------------

    /**
     * A good deed for a resident: it counts once, teaches its scent ({@code geur} may be null) and makes the tree grow a
     * step with the growth scene. {@code daarna}: see {@link Boom#groei}. False when the player had done it already.
     */
    public static boolean goedeDaad(ServerPlayer p, String daad, @Nullable String geur, @Nullable Consumer<ServerPlayer> daarna) {
        return Daden.geef(p, daad, geur, daarna);
    }

    /** The trainer's exam begins (registered with {@link Examen#registreer}). */
    public static boolean startExamen(ServerPlayer p, String examen) {
        return Examen.start(p, examen);
    }

    /** The diploma: the player passed as a Snuffelpup (shown in the snuffelboekje and the Guhstation). */
    public static void geefDiploma(ServerPlayer p) {
        SnuffelData.van(p).putBoolean("Diploma", true);
        GuhAdvancements.grant(p, "snuffel_diploma");
        Stand.stuur(p);
    }

    public static boolean heeftDiploma(ServerPlayer p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr("Diploma", false);
    }

    /**
     * The end of the first series: the questline {@code snuffeleiland} is finished (this is what the Guhpad asks for), the
     * tree gives its blossom twig and the player gets the Guhstation and the island's music disc. Safe to call again: nothing
     * is given twice. True the first time.
     */
    public static boolean rondAf(ServerPlayer p) {
        boolean eerste = !SnuffelFeature.LIJN.klaar(p);
        SnuffelFeature.LIJN.begin(p);
        SnuffelFeature.LIJN.zet(p, SnuffelFeature.STAPPEN);
        Boom.geefCadeau(p);
        geefGuhstation(p, false);
        if (eerste) {
            // (whoever had finished before the disc existed gets it from the captain, with his line: DorpRollen.kapitein)
            geefPlaat(p);
        }
        Stand.stuur(p);
        return eerste;
    }

    /** Is the first series finished for this player (the story counts for the Guhpad)? */
    public static boolean klaar(ServerPlayer p) {
        return SnuffelFeature.LIJN.klaar(p);
    }

    /** Gives the Guhstation, once per player; {@code opnieuw}: again (a resident replaces a lost one). */
    public static boolean geefGuhstation(ServerPlayer p, boolean opnieuw) {
        if (!opnieuw && heeftGuhstationGehad(p)) {
            return false;
        }
        SnuffelData.van(p).putBoolean("Guhstation", true);
        geef(p, new ItemStack(SnuffelFeature.GUHSTATION_ITEM.get()));
        GuhAdvancements.grant(p, "snuffel_guhstation");
        return true;
    }

    public static boolean heeftGuhstationGehad(ServerPlayer p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr("Guhstation", false);
    }

    /**
     * Gives the island's music disc, once per player and never again (true when it was given now). At the end of the first
     * series with the Guhstation; whoever finished the story before the disc existed gets it from Kapitein Zoutsnoet.
     */
    public static boolean geefPlaat(ServerPlayer p) {
        if (heeftPlaatGehad(p)) {
            return false;
        }
        SnuffelData.van(p).putBoolean("Muziekplaat", true);
        geef(p, new ItemStack(SnuffelFeature.MUZIEKPLAAT.get()));
        return true;
    }

    public static boolean heeftPlaatGehad(ServerPlayer p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr("Muziekplaat", false);
    }

    /**
     * Gives the player a thing. A dog has no pockets of its own: the thing travels home with the post and is there when
     * the player is a player again ({@link SnuffelKluis#post}); a player gets it at once.
     */
    public static void geef(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (Hondvorm.actief(p)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffel.post_mee", stack.getCount(), stack.getHoverName()).withStyle(ChatFormatting.GOLD));
            SnuffelKluis.post(p, stack.copy());
        } else {
            Minigames.give(p, stack.copy());
        }
    }

    // --- the Guhdex -----------------------------------------------------------------------------------------------------------

    /**
     * Where "Mijn verhaal" (Superkompas, objective line) points during the steps van..tot (inclusive) of the questline.
     * The dock registers the steigerhuisje for its steps, the village its spots on the island.
     */
    public static void doel(int vanStap, int totStap, BiFunction<ServerPlayer, Integer, Doel> bron) {
        DOELEN.add(new DoelBron(vanStap, totStap, bron));
    }

    /**
     * Which text variant the steps van..tot (inclusive) show for a player: a name of {@link #SLEUTELS}, or null for the
     * step's own text.
     */
    public static void sleutel(int vanStap, int totStap, BiFunction<ServerPlayer, Integer, String> bron) {
        SLEUTELS_VAN.add(new SleutelBron(vanStap, totStap, bron));
    }

    static String sleutelVan(ServerPlayer p, int stap) {
        for (SleutelBron s : SLEUTELS_VAN) {
            if (stap >= s.van() && stap <= s.tot()) {
                String sleutel = s.bron().apply(p, stap);
                if (sleutel != null) {
                    return sleutel;
                }
            }
        }
        return String.valueOf(stap);
    }

    @Nullable
    static Doel doelVan(ServerPlayer p, int stap) {
        for (DoelBron d : DOELEN) {
            if (stap >= d.van() && stap <= d.tot()) {
                Doel doel = d.bron().apply(p, stap);
                if (doel != null) {
                    return doel;
                }
            }
        }
        return null;
    }

    /**
     * What the Guhdex shows under the story: the three things the island gives, then the player's rank ("Snuffelpup (rang 1
     * (laagste) van 5 (hoogste))") and the list of all five ranks, each ticked when reached (nothing is ticked before the
     * player's first visit to the island).
     */
    static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        List<VerhaalStand.Beloning> uit = new ArrayList<>();
        uit.add(new VerhaalStand.Beloning("guhs:guhstation", Component.translatable("block.guhs.guhstation"), heeftGuhstationGehad(p)));
        uit.add(new VerhaalStand.Beloning("guhs:music_disc_snuffeleiland", Component.translatable("item.guhs.music_disc_snuffeleiland"), heeftPlaatGehad(p)));
        uit.add(new VerhaalStand.Beloning("guhs:snuffel_bloesemtakje", Component.translatable("item.guhs.snuffel_bloesemtakje"), Boom.cadeauGehad(p)));
        Rang rang = Rang.van(p);
        int geuren = Geuren.aantal(p);
        // (a rank is something of a dog: whoever was never on the island has none yet, so nothing is ticked for them)
        boolean hond = Reis.bezocht(p);
        uit.add(new VerhaalStand.Beloning("minecraft:name_tag", Component.translatable("gui.guhs.snuffel.rang.jouw", rang.regel(), geuren), hond));
        for (Rang r : Rang.values()) {
            uit.add(new VerhaalStand.Beloning(r.ordinal() == 0 ? "minecraft:bone" : "minecraft:paper", r.lijstRegel(), hond && r.ordinal() <= rang.ordinal()));
        }
        return uit;
    }
}
