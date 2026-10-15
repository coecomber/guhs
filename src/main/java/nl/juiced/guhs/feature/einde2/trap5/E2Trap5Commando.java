package nl.juiced.guhs.feature.einde2.trap5;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The dev commands of tier 5 (CONTRACT_E2_B 8; dev run only, plain literals): {@code /guhs einde2 trap5 ...}
 * <ul>
 *   <li>{@code ga <stap>}: puts you at a step of the Werktuigguh's line, the Guhvatar story finished, every earlier project
 *       done, the kit of that step in your pockets, and teleports you where the step plays: {@code deur} (in front of the
 *       closed door: the scene plays), {@code praat}, {@code zender}, {@code terminal}, {@code bestel}, {@code poortbuis},
 *       {@code thuispoort}, {@code elementen}, {@code bronnen}, {@code komeet} (at the comet shard), {@code terug},
 *       {@code klaar};</li>
 *   <li>{@code scene werkplaats}: forget and play the door scene; {@code wis}: forget the whole line, the shard and the
 *       scene; {@code project <naam>}: a project done (as if your machine did it); {@code richt}: the practice setups now;
 *       {@code deur aan|uit}: the door open for everybody; {@code stand}.</li>
 * </ul>
 * Without the chapter slices (this slice alone) {@code ga} registers one-step stand-ins of the five Guhvatar lines, so
 * "finale done" can be set; in the merged mod the real lines are there and nothing is registered.
 */
public final class E2Trap5Commando {
    static final List<String> STAPPEN = List.of("deur", "praat", "zender", "terminal", "bestel", "poortbuis", "thuispoort", "elementen", "bronnen",
            "komeet", "terug", "klaar");
    static final List<String> PROJECTEN = List.of(E2Trap5.P_ZENDER, E2Trap5.P_TERMINAL, E2Trap5.P_BESTEL, E2Trap5.P_POORTBUIS, E2Trap5.P_THUISPOORT,
            E2Trap5.P_LUCHT, E2Trap5.P_WATER, E2Trap5.P_AARDE, E2Trap5.P_VUUR, E2Trap5.P_ENDERGUHRAD, E2Trap5.P_KOMEET);

    private E2Trap5Commando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        LiteralArgumentBuilder<CommandSourceStack> trap5 = Commands.literal("trap5").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(STAPPEN, b))
                        .executes(c -> ga(c, StringArgumentType.getString(c, "stap")))))
                .then(Commands.literal("scene").then(Commands.literal("werkplaats").executes(E2Trap5Commando::scene)))
                .then(Commands.literal("wis").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    wis(p);
                    c.getSource().sendSuccess(() -> Component.literal("trap5: alles vergeten"), false);
                    return 1;
                }))
                .then(Commands.literal("project").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(PROJECTEN, b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            E2Trap5.meld(p, StringArgumentType.getString(c, "naam"));
                            return stand(c);
                        })))
                .then(Commands.literal("richt").executes(c -> {
                    ServerLevel g = c.getSource().getServer().getLevel(GuheindeFeature.GUHEINDE);
                    if (g == null) {
                        return 0;
                    }
                    Werkplaats.vergeetInrichting();
                    Werkplaats.richtIn(g);
                    c.getSource().sendSuccess(() -> Component.literal("trap5: oefenopstellingen gezet (als de werkplaats geladen is)"), false);
                    return 1;
                }))
                .then(Commands.literal("deur")
                        .then(Commands.literal("aan").executes(c -> deur(c, true)))
                        .then(Commands.literal("uit").executes(c -> deur(c, false))))
                .then(Commands.literal("stand").executes(E2Trap5Commando::stand));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(trap5)));
    }

    /** One-step stand-ins of the five Guhvatar lines when their slices are not in this tree (dev only). */
    static void standIns() {
        for (String l : E2Verhaal.HOOFDSTUKKEN) {
            if (Verhaallijnen.van(l) == null) {
                Verhaallijn.maak(l, E2Verhaal.GROEP).stappen(1).registreer();
            }
        }
    }

    /** The player finished the Guhvatar story (all five lines). */
    static void finaleKlaar(ServerPlayer p) {
        standIns();
        Verhaallijn finale = E2Verhaal.lijn(E2Verhaal.FINALE);
        E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, finale == null ? 1 : finale.stappen());
    }

    static void wis(ServerPlayer p) {
        if (E2Trap5.lijn() != null) {
            E2Trap5.lijn().wis(p);
        }
        GuhQuests.saved(p).remove(E2Trap5.KOMEET_GEPAKT);
        Cutscenes.vergeet(p, Werkplaats.SCENE);
        Werkplaats.vergeet(p);
    }

    private static int ga(CommandContext<CommandSourceStack> c, String naam) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int nr = STAPPEN.indexOf(naam);
        if (nr < 0) {
            c.getSource().sendFailure(Component.literal("trap5: onbekende stap " + naam + " (" + String.join(", ", STAPPEN) + ")"));
            return 0;
        }
        finaleKlaar(p);
        wis(p);
        Verhaallijn lijn = E2Trap5.lijn();
        // the step this name stands for, and every earlier project done
        int stap = switch (naam) {
            case "deur" -> -1;
            case "praat" -> E2Trap5.S_PRAAT;
            case "zender" -> E2Trap5.S_ZENDER;
            case "terminal" -> E2Trap5.S_TERMINAL;
            case "bestel" -> E2Trap5.S_BESTEL;
            case "poortbuis" -> E2Trap5.S_POORTBUIS;
            case "thuispoort" -> E2Trap5.S_THUISPOORT;
            case "elementen" -> E2Trap5.S_ELEMENTEN;
            case "bronnen", "komeet" -> E2Trap5.S_BRONNEN;
            case "terug" -> E2Trap5.S_TERUG;
            default -> E2Trap5.STAPPEN;
        };
        if (stap >= 0) {
            Cutscenes.vergeet(p, Werkplaats.SCENE);
            lijn.begin(p);
            lijn.eenmalig(p, E2Trap5.E_PLAN);
            Minigames.give(p, new ItemStack(E2Trap5Feature.BOUWPLAN.get()));
            for (int s = E2Trap5.S_ZENDER; s < Math.min(stap, E2Trap5.S_TERUG); s++) {
                vlaggen(p, lijn, s);
            }
            lijn.zet(p, stap);
        }
        // where it plays
        ServerLevel g = c.getSource().getServer().getLevel(GuheindeFeature.GUHEINDE);
        String plek = switch (naam) {
            case "deur" -> null;
            case "praat", "terug", "klaar" -> "werktuigguh";
            case "komeet" -> "komeetscherf";
            default -> null;
        };
        if (g != null && naam.equals("deur") && Plekken.bekend(g, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL)) {
            BlockPos deur = Plekken.wereld(g, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, "werkplaatsdeur");
            if (deur != null) {
                tp(p, g, Vec3.atBottomCenterOf(deur.west(T5Getallen.DEUR_AFSTAND + 3)), -90f);
            }
        } else if (g != null && plek != null) {
            String s = plek.equals("komeetscherf") ? Einde2Ids.VUURNATIE_HOOFDSTAD : Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
            BlockPos pos = Plekken.bekend(g, s) ? Plekken.wereld(g, s, plek) : null;
            if (pos != null) {
                tp(p, g, Vec3.atBottomCenterOf(plek.equals("werktuigguh") ? pos.west(2) : pos.north(4)), plek.equals("werktuigguh") ? -90f : 0f);
            }
        }
        // the kit of the step (as the Werktuigguh would give it)
        switch (naam) {
            case "zender" -> {
                Minigames.give(p, new ItemStack(E2Trap5Feature.ZENDER_ITEM.get()));
                Minigames.give(p, new ItemStack(E2Trap5Feature.ONTVANGER_ITEM.get()));
            }
            case "terminal" -> Minigames.give(p, new ItemStack(E2Trap5Feature.TERMINAL_ITEM.get()));
            case "bestel" -> Minigames.give(p, new ItemStack(E2Trap5Feature.BESTELBALIE_ITEM.get()));
            case "poortbuis" -> Minigames.give(p, new ItemStack(E2Trap5Feature.POORTBUIS_ITEM.get(), 2));
            case "thuispoort" -> Minigames.give(p, new ItemStack(E2Trap5Feature.THUISPOORT_ITEM.get(), 2));
            case "elementen" -> {
                Minigames.give(p, new ItemStack(E2Trap5Feature.LUCHTBLAZER_ITEM.get()));
                Minigames.give(p, new ItemStack(E2Trap5Feature.WATERDRAGER_ITEM.get()));
                Minigames.give(p, new ItemStack(E2Trap5Feature.KORSTDUWER_ITEM.get()));
                Minigames.give(p, new ItemStack(E2Trap5Feature.VUURKORF_ITEM.get()));
            }
            case "bronnen" -> Minigames.give(p, new ItemStack(E2Trap5Feature.ENDERGUHRAD_ITEM.get()));
            case "terug", "klaar" -> GuhQuests.saved(p).putBoolean(E2Trap5.KOMEET_GEPAKT, true);
            default -> {
            }
        }
        if (naam.equals("klaar")) {
            lijn.eenmalig(p, E2Trap5.E_BELONING);
        }
        c.getSource().sendSuccess(() -> Component.literal("trap5: stap " + naam + " -> " + lijn.stap(p) + "/" + lijn.stappen()), false);
        return 1;
    }

    /** The flags of the project of this step (as if the player's machines did it). */
    private static void vlaggen(ServerPlayer p, Verhaallijn lijn, int stap) {
        if (stap == E2Trap5.S_ELEMENTEN) {
            E2Trap5.ELEMENTEN.forEach(f -> lijn.vlag(p, f, true));
        } else if (stap == E2Trap5.S_BRONNEN) {
            lijn.vlag(p, E2Trap5.P_ENDERGUHRAD, true);
            lijn.vlag(p, E2Trap5.P_KOMEET, true);
        } else {
            String f = E2Trap5.projectVan(stap);
            if (f != null) {
                lijn.vlag(p, f, true);
            }
        }
    }

    private static void tp(ServerPlayer p, ServerLevel g, Vec3 waar, float yaw) {
        g.getChunk(BlockPos.containing(waar));
        p.teleportTo(g, waar.x, waar.y, waar.z, java.util.Set.of(), yaw, 0f, true);
    }

    private static int scene(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel g = c.getSource().getServer().getLevel(GuheindeFeature.GUHEINDE);
        BlockPos deur = g != null && Plekken.bekend(g, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL)
                ? Plekken.wereld(g, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, "werkplaatsdeur") : null;
        if (deur == null) {
            c.getSource().sendFailure(Component.literal("trap5: de Zuidelijke Luchttempel is nog niet bekend"));
            return 0;
        }
        Cutscenes.vergeet(p, Werkplaats.SCENE);
        if (p.level() != g || p.position().distanceTo(Vec3.atCenterOf(deur)) > 48) {
            tp(p, g, Vec3.atBottomCenterOf(deur.west(5)), -90f);
        }
        Werkplaats.speelScene(p, deur);
        return 1;
    }

    private static int deur(CommandContext<CommandSourceStack> c, boolean aan) {
        E2Trap5.devOpen = aan;
        c.getSource().sendSuccess(() -> Component.literal("trap5: deur voor iedereen " + (aan ? "open" : "zoals het verhaal zegt")), false);
        return 1;
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Verhaallijn lijn = E2Trap5.lijn();
        StringBuilder s = new StringBuilder("trap5: werktuigguh " + lijn.stap(p) + "/" + lijn.stappen() + (lijn.begonnen(p) ? " (begonnen)" : "")
                + ", finale klaar " + E2Verhaal.finaleKlaar(p) + ", komeet gepakt " + GuhQuests.saved(p).getBooleanOr(E2Trap5.KOMEET_GEPAKT, false)
                + ", projecten:");
        for (String f : PROJECTEN) {
            if (lijn.vlag(p, f)) {
                s.append(' ').append(f);
            }
        }
        c.getSource().sendSuccess(() -> Component.literal(s.toString()), false);
        return 1;
    }
}
