package nl.juiced.guhs.feature.einde2.spd2heen;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 "Sponsguh", slice sp_d2_heen: the dev commands (CONTRACT_E2_C 8.2; a dev run only, operators), under
 * {@code /guhs einde2 sponsguh d2heen ...}. Jumping to a step is the skelet's {@code /guhs einde2 sponsguh ga sponsguh_kroon
 * <0-4>} (it runs this slice's start of that step).
 * <ul>
 *   <li>{@code scene <naam>}: forget and play one scene of this slice at its own spot (no step changes);</li>
 *   <li>{@code kaart}: the narrator card of part 2 again;</li>
 *   <li>{@code spel kroeg}: part 2 at step 4, at the swing gates of De Ruige Kroeg, the stand-still game at once;</li>
 *   <li>{@code rit}: part 2 at step 3, behind the wheel of the Knabbelburgermobiel with Sponsguh and Patguh (no scene);</li>
 *   <li>{@code wis}: this slice's own state and seen scenes (the steps stay; {@code /guhs einde2 sponsguh wis} for those).</li>
 * </ul>
 */
final class E2SpD2HeenCommando {
    private E2SpD2HeenCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("d2heen")
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SpD2Heen.ANKERS.keySet(), b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("kaart").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Verteller.vergeet(p, SpD2Heen.KAART);
                    return Verteller.toon(p, SpD2Heen.KAART, null) ? 1 : 0;
                }))
                .then(Commands.literal("spel").then(Commands.literal("kroeg").executes(c -> kroeg(c.getSource()))))
                .then(Commands.literal("rit").executes(c -> rit(c.getSource())))
                .then(Commands.literal("wis").executes(c -> {
                    SpD2Heen.wisEigen(c.getSource().getPlayerOrException(), true);
                    c.getSource().sendSuccess(() -> Component.literal("sp_d2_heen: own state and seen scenes forgotten"), false);
                    return 1;
                })));
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String[] anker = SpD2Heen.ANKERS.get(naam);
        Cutscene s = D2Scenes.van(naam);
        BlockPos a = anker == null ? null : D2Plek.blok(p, anker[0], anker[1]);
        if (s == null || a == null) {
            src.sendFailure(Component.literal("sp_d2_heen: no scene " + naam + " here (" + SpD2Heen.ANKERS.keySet() + "; be in the Guheinde)"));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        boolean ok = Cutscenes.speel(p, s, a, Rotation.NONE, null);
        src.sendSuccess(() -> Component.literal("sp_d2_heen: scene " + naam + (ok ? " plays (" + s.duur() / 20.0 + " s)" : " could not start")), false);
        return ok ? 1 : 0;
    }

    private static int kroeg(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_SLEUTEL);
        SpD2Heen.wisEigen(p, false);
        boolean ok = Kroeg.start(p, p.level().getServer().getTickCount());
        src.sendSuccess(() -> Component.literal(ok ? "sp_d2_heen: the stand-still game runs" : "sp_d2_heen: De Ruige Kroeg is not known here"), false);
        return ok ? 1 : 0;
    }

    private static int rit(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_RIT);
        SpD2Heen.wisEigen(p, false);
        Vec3 start = D2Plek.staan(p, D2Plek.BB, "mobiel_start");
        if (start == null || Rit.zetAuto(p) == null) {
            src.sendFailure(Component.literal("sp_d2_heen: Bikinibodem is not known here"));
            return 0;
        }
        Rit.instappen(p);
        src.sendSuccess(() -> Component.literal("sp_d2_heen: behind the wheel, drive east to De Ruige Kroeg"), false);
        return 1;
    }
}
