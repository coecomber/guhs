package nl.juiced.guhs.feature.park.plek;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;

/**
 * 1.4.2 (Het Knabbelpark, plekjes A): the dev command {@code /guhs parkplek zet <naam>} (op): puts one of the five plekjes
 * with its south edge (the entrance) at your feet, its ground one block under you, as worldgen would (the inhabitants of
 * the template come along; the eendjes and the rowing boat are made right away, as Bezetting and the upkeep would). For
 * AutoCheck looks (tools/autocheck/park_plekjes_a.txt) and quick checks.
 */
final class PlekACommando {
    static final String[] NAMEN = {"eendenvijver", "speeltuin", "picknickweide", "muziektuin", "japanse_tuin"};

    private PlekACommando() {
    }

    static void registreer(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("parkplek").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("zet").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(NAMEN, b)).executes(PlekACommando::zet)))));
    }

    private static int zet(CommandContext<CommandSourceStack> c) {
        String naam = StringArgumentType.getString(c, "naam");
        ServerLevel level = c.getSource().getLevel();
        StructureTemplate t = level.getStructureManager().get(Guhs.id("knabbelpark/" + naam)).orElse(null);
        if (t == null) {
            c.getSource().sendFailure(Component.literal("geen plekje " + naam));
            return 0;
        }
        BlockPos voet = BlockPos.containing(c.getSource().getPosition());
        BlockPos hoek = voet.offset(-t.getSize().getX() / 2, -2, -t.getSize().getZ() + 1);
        t.placeInWorld(level, hoek, hoek, new StructurePlaceSettings().setRotation(Rotation.NONE), level.getRandom(), Block.UPDATE_CLIENTS);
        if (naam.equals("eendenvijver")) {
            for (BlockPos e : PlekAPlekken.EENDEN) {
                var mama = PlekA.eendje(level, Vec3.atBottomCenterOf(hoek.offset(e)));
                if (mama != null) {
                    level.addFreshEntity(mama);
                }
            }
            PlekAEvents.boot(level, hoek.offset(PlekAPlekken.LIGPLAATS), Rotation.NONE);
        }
        c.getSource().sendSuccess(() -> Component.literal("parkplek " + naam + " op " + hoek.toShortString()), false);
        return 1;
    }
}
