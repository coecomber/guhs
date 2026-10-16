package nl.juiced.guhs.feature.einde2.ballon;

import java.util.List;
import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase C, slice trap5_ballon: the dev command (CONTRACT_E2_B 8; a dev run only, plain literals):
 * <ul>
 *   <li>{@code /guhs einde2 ballon ga <stap>}: the whole Guhvatar story and the Werktuigguh's line done (whatever of them is
 *       registered; else the gate is opened by a dev mark), the line wiped and set to the step (7: the rewards too), you
 *       stand in front of the board on the building site. Steps: 0 = the board starts it, 1 doek, 2 mand, 3 brander,
 *       4 knabbelvoorraad, 5 versiering, 6 klaar om op te stijgen, 7 af;</li>
 *   <li>{@code /guhs einde2 ballon vul}: everything the current stage still needs, in your bag;</li>
 *   <li>{@code /guhs einde2 ballon scene vredesballon}: forget + play the launch (at step 6 it counts);</li>
 *   <li>{@code /guhs einde2 ballon zakje}: today's bag of knabbels may be taken again;</li>
 *   <li>{@code /guhs einde2 ballon stand} and {@code wis}.</li>
 * </ul>
 */
public final class BallonCommando {
    private BallonCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ballon")
                        .then(Commands.literal("ga").then(Commands.argument("stap", IntegerArgumentType.integer(0, Vredesballon.STAPPEN))
                                .executes(c -> ga(c.getSource(), IntegerArgumentType.getInteger(c, "stap")))))
                        .then(Commands.literal("vul").executes(c -> vul(c.getSource())))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("vredesballon"), b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("zakje").executes(c -> zakje(c.getSource())))
                        .then(Commands.literal("stand").executes(c -> stand(c.getSource())))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource()))))));
    }

    /** The story before this line, as far as it is registered in this build. */
    static void ervoor(ServerPlayer p) {
        if (E2Verhaal.lijn(E2Verhaal.FINALE) != null) {
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 999);
        }
        if (Verhaallijnen.van(Vredesballon.VORIGE) != null) {
            E2VerhaalDev.klaar(p, Vredesballon.VORIGE);
        }
        GuhQuests.saved(p).putBoolean(Vredesballon.DEV_VRIJ, true);
    }

    /** Sets the line to this step (wiped first; 7 = the rewards too). */
    static void zet(ServerPlayer p, int stap) {
        ervoor(p);
        Vredesballon.LIJN.wis(p);
        GuhQuests.saved(p).remove(Vredesballon.DAG);
        Cutscenes.vergeet(p, BallonScene.VREDESBALLON.id());
        Vredesballon.OPGESTEGEN.remove(p.getUUID());
        if (stap > 0) {
            Vredesballon.LIJN.zet(p, stap);
        }
        if (stap >= Vredesballon.STAPPEN) {
            Vredesballon.beloon(p);
        }
    }

    private static int ga(CommandSourceStack source, int stap) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel g = source.getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (g == null) {
            source.sendFailure(Component.literal("No Guheinde dimension"));
            return 0;
        }
        if (!Plekken.bekend(g, Vredesballon.STRUCTUUR)) {
            Plekken.zoekVooruit(g);
            source.sendFailure(Component.literal("The Zuidelijke Luchttempel is still being looked up: try again in a few seconds"));
            return 0;
        }
        BlockPos teller = Plekken.wereld(g, Vredesballon.STRUCTUUR, Vredesballon.TELLER);
        if (teller == null) {
            source.sendFailure(Component.literal("This world has no Zuidelijke Luchttempel"));
            return 0;
        }
        zet(p, stap);
        g.getChunk(teller);
        // in front of the board, looking at it (the teller is a block above the yard's floor)
        p.teleportTo(g, teller.getX() + 0.5, teller.getY() - 1, teller.getZ() + 3.5, Set.of(), 180f, 0f, true);
        Schijn.straks(p);
        BallonBord.ververs(p);
        source.sendSuccess(() -> Component.literal("vredesballon at step " + Vredesballon.LIJN.stap(p) + "/" + Vredesballon.STAPPEN
                + (stap == 0 ? ": click the board" : stap <= Vredesballon.AANTAL_FASEN ? ": /guhs einde2 ballon vul, then click the board"
                : stap == Vredesballon.KLAAR ? ": click the board and launch" : ": done (click the board for the bag of the day)")), false);
        return 1;
    }

    private static int vul(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int fase = Vredesballon.fase(p);
        if (fase == 0) {
            source.sendFailure(Component.literal("Not at a stage (step " + Vredesballon.LIJN.stap(p) + ")"));
            return 0;
        }
        for (Vredesballon.Levering l : Vredesballon.leveringen(fase)) {
            int nog = l.aantal() - Vredesballon.geleverd(p, fase, l);
            ItemStack voorbeeld = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(l.icoon())));
            while (nog > 0 && !voorbeeld.isEmpty()) {
                int n = Math.min(nog, voorbeeld.getMaxStackSize());
                Minigames.give(p, voorbeeld.copyWithCount(n));
                nog -= n;
            }
        }
        source.sendSuccess(() -> Component.literal("The things of stage " + fase + " are in your bag"), false);
        return 1;
    }

    private static int scene(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!"vredesballon".equals(naam)) {
            source.sendFailure(Component.literal("Unknown scene " + naam));
            return 0;
        }
        Cutscenes.vergeet(p, BallonScene.VREDESBALLON.id());
        if (Vredesballon.LIJN.stap(p) == Vredesballon.KLAAR) {
            return Vredesballon.lanceer(p) ? 1 : 0;
        }
        BlockPos anker = Vredesballon.plek(p.level(), Vredesballon.ANKER);
        if (anker == null) {
            source.sendFailure(Component.literal("Stand in the Guheinde near the Zuidelijke Luchttempel (try /guhs einde2 ballon ga 6)"));
            return 0;
        }
        Vredesballon.OPGESTEGEN.add(p.getUUID());
        Schijn.ververs(p);
        E2Audio.moment(p, "appa");
        boolean ok = Cutscenes.speel(p, BallonScene.VREDESBALLON, anker, Rotation.NONE, q -> {
            Vredesballon.OPGESTEGEN.remove(q.getUUID());
            E2Audio.stopMoment(q, 40);
            Schijn.ververs(q);
        });
        if (!ok) {
            Vredesballon.OPGESTEGEN.remove(p.getUUID());
            Schijn.ververs(p);
        }
        return ok ? 1 : 0;
    }

    private static int zakje(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        GuhQuests.saved(p).remove(Vredesballon.DAG);
        BallonBord.ververs(p);
        source.sendSuccess(() -> Component.literal("Today's bag of knabbels is ready again"), false);
        return 1;
    }

    private static int stand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int stap = Vredesballon.LIJN.stap(p), fase = Vredesballon.fase(p);
        StringBuilder s = new StringBuilder("vredesballon: step " + stap + "/" + Vredesballon.STAPPEN + ", may begin " + Vredesballon.magBeginnen(p)
                + ", overlay " + Vredesballon.laag(p) + ", bag ready " + Vredesballon.zakjeKlaar(p));
        if (fase > 0) {
            for (Vredesballon.Levering l : Vredesballon.leveringen(fase)) {
                s.append(", ").append(l.naam()).append(" ").append(Vredesballon.geleverd(p, fase, l)).append("/").append(l.aantal());
            }
        }
        source.sendSuccess(() -> Component.literal(s.toString()), false);
        return 1;
    }

    private static int wis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Vredesballon.LIJN.wis(p);
        GuhQuests.saved(p).remove(Vredesballon.DAG);
        GuhQuests.saved(p).remove(Vredesballon.DEV_VRIJ);
        Cutscenes.vergeet(p, BallonScene.VREDESBALLON.id());
        Schijn.straks(p);
        BallonBord.ververs(p);
        source.sendSuccess(() -> Component.literal("vredesballon wiped"), false);
        return 1;
    }
}
