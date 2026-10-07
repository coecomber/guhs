package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Aandenken;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * The lobby NPC of a joke game (Skyblok-guh, Bedwars-guh, Vadsnite-guh): a click opens the talking screen with "Spelen!",
 * "Wat is dit?" and, after the first time, "Ik ben mijn aandenken kwijt, njeg" (only for a keepsake that is an item; an
 * outfit is an unlock and cannot be lost). The first talk is step 0 of the questline, the game itself counts the steps.
 * Any number of players can talk and play at the same time: every player gets an arena of their own.
 */
final class GrapRol implements NpcRole {
    static final int SPELEN = 1, UITLEG = 2, KWIJT = 3, DOEI = 4;
    private final String id;
    private final SpelSoort spel;
    @Nullable
    private final Supplier<? extends Item> aandenken;

    GrapRol(String id, SpelSoort spel, @Nullable Supplier<? extends Item> aandenken) {
        this.id = id;
        this.spel = spel;
        this.aandenken = aandenken;
    }

    private String key(String wat) {
        return "quest.guhs." + id + "." + wat;
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        if (Sessies.van(player) != null) {
            return;
        }
        boolean klaar = Grappen.isKlaar(player, id);
        List<Praat.Optie> opties = new ArrayList<>();
        opties.add(new Praat.Optie(SPELEN, key(klaar ? "optie.opnieuw" : "optie.spelen")));
        opties.add(new Praat.Optie(UITLEG, key("optie.uitleg")));
        if (klaar && aandenken != null) {
            opties.add(new Praat.Optie(KWIJT, key("optie.kwijt")));
        }
        opties.add(new Praat.Optie(DOEI, key("optie.doei")));
        Praat.open(player, npc, null, key(klaar ? "hallo.opnieuw" : "hallo"), new Object[] {player.getDisplayName()}, opties.toArray(Praat.Optie[]::new));
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        if (npc.distanceToSqr(player) > 12 * 12 || Sessies.van(player) != null) {
            return;
        }
        switch (optie) {
            case SPELEN -> {
                Praat.sluit(player);
                Sessies.start(spel, List.of(player), new CompoundTag());
            }
            case UITLEG -> Praat.open(player, npc, null, key("uitleg"), new Object[0],
                    new Praat.Optie(SPELEN, key(Grappen.isKlaar(player, id) ? "optie.opnieuw" : "optie.spelen")), new Praat.Optie(DOEI, key("optie.doei")));
            case KWIJT -> {
                if (aandenken != null && Grappen.isKlaar(player, id)) {
                    boolean gegeven = Aandenken.opnieuw(player, aandenken.get());
                    Praat.open(player, npc, null, key(gegeven ? "kwijt.hier" : "kwijt.nee"), new Object[0], new Praat.Optie(DOEI, key("optie.doei")));
                }
            }
            case DOEI -> Praat.sluit(player);
            default -> {
            }
        }
    }
}
