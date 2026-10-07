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
     * tree gives its blossom twig and the player gets the Guhstation. Safe to call again: nothing is given twice. True the
     * first time.
     */
    public static boolean rondAf(ServerPlayer p) {
        boolean eerste = !SnuffelFeature.LIJN.klaar(p);
        SnuffelFeature.LIJN.begin(p);
        SnuffelFeature.LIJN.zet(p, SnuffelFeature.STAPPEN);
        Boom.geefCadeau(p);
        geefGuhstation(p, false);
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
     * What the Guhdex shows under the story: the two things the island gives, then the player's rank ("Snuffelpup (rang 1
     * (laagste) van 5 (hoogste))") and the list of all five ranks, each ticked when reached.
     */
    static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        List<VerhaalStand.Beloning> uit = new ArrayList<>();
        uit.add(new VerhaalStand.Beloning("guhs:guhstation", Component.translatable("block.guhs.guhstation"), heeftGuhstationGehad(p)));
        uit.add(new VerhaalStand.Beloning("guhs:snuffel_bloesemtakje", Component.translatable("item.guhs.snuffel_bloesemtakje"), Boom.cadeauGehad(p)));
        Rang rang = Rang.van(p);
        int geuren = Geuren.aantal(p);
        uit.add(new VerhaalStand.Beloning("minecraft:name_tag", Component.translatable("gui.guhs.snuffel.rang.jouw", rang.regel(), geuren), true));
        for (Rang r : Rang.values()) {
            uit.add(new VerhaalStand.Beloning(r.ordinal() == 0 ? "minecraft:bone" : "minecraft:paper", r.lijstRegel(), r.ordinal() <= rang.ordinal()));
        }
        return uit;
    }
}
