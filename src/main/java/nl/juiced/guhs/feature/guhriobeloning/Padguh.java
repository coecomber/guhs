package nl.juiced.guhs.feature.guhriobeloning;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Pad-guh, the toadstool guh on the forecourt of the castle. He sits on the counter of his paddenstoelenkraam.
 * <ul>
 *     <li>The first click: the intro (three balloons): the Grote Nether-Mika took Prinses Perzikguh "for a piece of
 *     cake", how a level works, what the coins are for.</li>
 *     <li>Every later click: his main menu. Its balloon is the running gag when you finished a world since you last
 *     spoke ("Bedankt! Maar de prinses is in een ander kasteeldeel, njeg." - once per world, also called after you at
 *     the flagpole, {@link #levelKlaar}), the golden cap when you have all eighteen big vadsmunten, else a greeting with
 *     your coins.</li>
 *     <li>The menu: outfits, building blocks ({@link Winkel}; a shelf stays open so you can buy more), and your big
 *     vadsmunten level by level.</li>
 * </ul>
 * The talking screen has room for three buttons, so each shelf holds three things. Everything is per player.
 */
public final class Padguh implements NpcRole {
    /** The key of his talking screens (Praat). */
    public static final String SLEUTEL = "guhriobeloning_padguh";
    /** The answers: the three of the main menu; buying thing i of the shop is {@link #KOOP} + its ordinal. */
    public static final int OUTFITS = 1, BLOKKEN = 2, VADSMUNTEN = 3, KOOP = 10;
    /** Questline flags: met him, got the golden cap. */
    public static final String ONTMOET = "ontmoet", GOUDEN_PET = "gouden_pet";
    private static final String T = "quest.guhs.guhriobeloning.padguh.";
    private static final Praat.Optie[] HOOFDMENU = {new Praat.Optie(OUTFITS, "gui.guhs.guhriobeloning.optie.outfits"),
            new Praat.Optie(BLOKKEN, "gui.guhs.guhriobeloning.optie.blokken"), new Praat.Optie(VADSMUNTEN, "gui.guhs.guhriobeloning.optie.vadsmunten")};

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        Verhaallijn lijn = GuhrioBeloningFeature.LIJN;
        if (!lijn.vlag(p, ONTMOET)) {
            lijn.vlag(p, ONTMOET, true);
            GuhrioBeloningFeature.bijwerken(p);
            GuhAdvancements.grant(p, "guhrio_beloning_padguh");
            Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(npc, "", T + "intro.1"), new Praat.Regel(npc, "", T + "intro.2"),
                    new Praat.Regel(npc, "", T + "intro.3")), HOOFDMENU);
            return;
        }
        hoofdmenu(npc, p);
    }

    /** The main menu, with whatever he has to say first. */
    private static void hoofdmenu(Entity npc, ServerPlayer p) {
        if (goudenPet(p)) {
            Praat.open(p, npc, SLEUTEL, T + "gouden_pet", new Object[0], HOOFDMENU);
            return;
        }
        int wereld = gag(p);
        if (wereld > 0) {
            Praat.open(p, npc, SLEUTEL, T + "gag", new Object[]{Component.translatable("gui.guhs.guhriobeloning.wereld." + wereld)}, HOOFDMENU);
        } else if (GuhrioKasteel.duelGewonnen(p) && GuhrioBeloningFeature.LIJN.eenmalig(p, "na_duel")) {
            Praat.open(p, npc, SLEUTEL, T + "na_duel", new Object[0], HOOFDMENU);
        } else {
            Praat.open(p, npc, SLEUTEL, T + "hallo", new Object[]{GuhrioSpel.munten(p)}, HOOFDMENU);
        }
    }

    /** The world (1..3) whose "the princess is in another part of the castle" he has not said to this player yet, or 0. */
    private static int gag(ServerPlayer p) {
        for (int w = 1; w <= 3; w++) {
            if (wereldGehaald(p, w) && GuhrioBeloningFeature.LIJN.eenmalig(p, "gag_" + w)) {
                return w;
            }
        }
        return 0;
    }

    private static boolean wereldGehaald(ServerPlayer p, int wereld) {
        return GuhrioKasteel.gehaald(p, "kasteel_" + wereld + "_1") && GuhrioKasteel.gehaald(p, "kasteel_" + wereld + "_2");
    }

    /**
     * (GuhrioSpel.BIJ_KLAAR) the flagpole of the second level of a world: Pad-guh calls his thanks after you, the first
     * time you finish that world. (When he says it here he does not say it again on the forecourt.)
     */
    static void levelKlaar(ServerPlayer p, GuhrioSpel.Sessie sessie, int ticks, boolean record) {
        String id = sessie.level().level().id();
        for (int w = 1; w <= 3; w++) {
            if (id.equals("kasteel_" + w + "_2") && wereldGehaald(p, w) && GuhrioBeloningFeature.LIJN.eenmalig(p, "gag_" + w)) {
                p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc.padguh")).append("> ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.translatable(T + "gag",
                                Component.translatable("gui.guhs.guhriobeloning.wereld." + w)).withStyle(ChatFormatting.WHITE)));
            }
        }
    }

    /**
     * All eighteen big vadsmunten: the golden cap, once (and again when you lost the item before you ever put it on).
     * True when he just gave it.
     */
    static boolean goudenPet(ServerPlayer p) {
        if (GuhrioKasteel.alleVadsmunten(p) < GuhrioKasteel.VADSMUNTEN) {
            return false;
        }
        Verhaallijn lijn = GuhrioBeloningFeature.LIJN;
        GuhClothes pet = GuhClothes.GUHRIOBELONING_GOUDEN_PET;
        boolean kwijt = !KledingUnlocks.heeft(p, pet) && GuhQuests.count(p, ModItems.clothingItem(pet)) == 0;
        if (lijn.vlag(p, GOUDEN_PET) && !kwijt) {
            return false;
        }
        lijn.vlag(p, GOUDEN_PET, true);
        Minigames.give(p, new ItemStack(ModItems.clothingItem(pet)));
        GuhAdvancements.grant(p, "guhrio_beloning_gouden_pet");
        nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "guhrio/guhrio_beloning_gouden_pet");
        GuhrioBeloningFeature.bijwerken(p);
        return true;
    }

    /** (Praat) an answer in one of his screens. */
    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie < 0 || spreker == null) {
            return;                                             // (read to the end, closed, or walked away)
        }
        if (optie == OUTFITS || optie == BLOKKEN) {
            plank(spreker, p, optie == OUTFITS, T + (optie == OUTFITS ? "outfits" : "blokken"), GuhrioSpel.munten(p));
        } else if (optie == VADSMUNTEN) {
            vadsmunten(spreker, p);
        } else if (optie >= KOOP && optie < KOOP + Winkel.Waar.values().length) {
            Winkel.Waar waar = Winkel.Waar.values()[optie - KOOP];
            int tekort = Winkel.tekort(p, waar);
            if (Winkel.koop(p, waar)) {
                plank(spreker, p, waar.outfit, T + "gekocht", waar.stack().getHoverName(), GuhrioSpel.munten(p));
            } else {
                plank(spreker, p, waar.outfit, T + "tekort", tekort, GuhrioSpel.munten(p));
            }
        }
    }

    /** A shelf: its three things as buttons, under a balloon. */
    private static void plank(Entity npc, ServerPlayer p, boolean outfits, String tekst, Object... args) {
        List<Praat.Optie> opties = new ArrayList<>();
        for (Winkel.Waar waar : Winkel.Waar.plank(outfits)) {
            opties.add(new Praat.Optie(KOOP + waar.ordinal(), waar.knop()));
        }
        Praat.open(p, npc, SLEUTEL, tekst, args, opties.toArray(Praat.Optie[]::new));
    }

    /** Your big vadsmunten, level by level (a filled dot for each one you have), with the two shelves under it. */
    private static void vadsmunten(Entity npc, ServerPlayer p) {
        Object[] args = new Object[GuhrioKasteel.LEVELS.size() + 1];
        for (int i = 0; i < GuhrioKasteel.LEVELS.size(); i++) {
            args[i] = stippen(GuhrioKasteel.vadsmunten(p, GuhrioKasteel.LEVELS.get(i)));
        }
        args[GuhrioKasteel.LEVELS.size()] = GuhrioKasteel.alleVadsmunten(p);
        Praat.open(p, npc, SLEUTEL, T + "vadsmunten", args, HOOFDMENU[0], HOOFDMENU[1]);
    }

    /** The three big vadsmunten of a level as dots: filled = yours. */
    static String stippen(int bits) {
        StringBuilder s = new StringBuilder();
        for (int n = 0; n < 3; n++) {
            s.append((bits >> n & 1) != 0 ? '●' : '○');
        }
        return s.toString();
    }
}
