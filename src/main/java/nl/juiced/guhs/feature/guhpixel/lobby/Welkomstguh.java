package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.crafting.Recipe;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.Toegang;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Welkomstguh (NPC kind LOBBY_WELKOMSTGUH) next to the spawn point of the lobby. The first time a player talks to it
 * they get the Netwerkkabeltje (once per player; again on request when they lost it) and the recipe of the Guhpixel-poort
 * in their recipe book: the gate for home needs that cable, so it can only be made after the first visit. After that it
 * explains the lobby (and the way home) and the ranks.
 */
public final class Welkomstguh implements NpcRole {
    static final Welkomstguh ROL = new Welkomstguh();
    /** Answer ids. */
    public static final int WAT = 1, KABEL = 2, RANGEN = 3;
    static final String BEGROET = "Begroet";
    private static final ResourceKey<Recipe<?>> POORT_RECEPT = ResourceKey.create(Registries.RECIPE, Guhs.id("guhpixel_poort"));

    private static Praat.Optie[] opties() {
        // (three at most: the answers eat into the speech balloon, and the screen has its own "Doei!")
        return new Praat.Optie[] {new Praat.Optie(WAT, "gui.guhs.lobby.welkom.optie.wat"), new Praat.Optie(RANGEN, "gui.guhs.lobby.welkom.optie.rangen"),
                new Praat.Optie(KABEL, "gui.guhs.lobby.welkom.optie.kabel")};
    }

    public static boolean begroet(ServerPlayer p) {
        return LobbySlice.data(p).getBooleanOr(BEGROET, false);
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 1.3f);
        if (!Toegang.heeft(p)) {
            // (only somebody who was put here by hand: everybody else came through the big screen)
            Praat.open(p, npc, null, "gui.guhs.lobby.welkom.op_slot", new Object[0]);
            return;
        }
        if (!begroet(p)) {
            LobbySlice.data(p).putBoolean(BEGROET, true);
            PxData.vuil(p.level().getServer());
            if (!Toegang.kabeltjeGehad(p)) {
                Toegang.geefKabeltje(p);
            }
            p.awardRecipesByKey(List.of(POORT_RECEPT));
            GuhAdvancements.grant(p, "lobby_welkom");
            Praat.open(p, npc, null, "gui.guhs.lobby.welkom.eerste", new Object[] {p.getName()}, opties());
            return;
        }
        Praat.open(p, npc, null, "gui.guhs.lobby.welkom.terug", new Object[] {p.getName()}, opties());
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        switch (optie) {
            case WAT -> Praat.open(p, npc, null, "gui.guhs.lobby.welkom.wat", new Object[0], opties());
            case RANGEN -> Praat.open(p, npc, null, "gui.guhs.lobby.welkom.rangen", new Object[0], opties());
            case KABEL -> {
                if (!Toegang.heeft(p)) {
                    Praat.open(p, npc, null, "gui.guhs.lobby.welkom.op_slot", new Object[0]);
                } else if (Toegang.geefKabeltje(p)) {
                    p.awardRecipesByKey(List.of(POORT_RECEPT));
                    Praat.open(p, npc, null, "gui.guhs.lobby.welkom.kabel.nieuw", new Object[0], opties());
                } else {
                    Praat.open(p, npc, null, "gui.guhs.lobby.welkom.kabel.heb_je", new Object[0], opties());
                }
            }
            default -> {
            }
        }
    }

    private Welkomstguh() {
    }
}
