package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * Op commands of einde2 (testing, the autocheck; Brigadier merges them into the /guhs tree; the slices hang their own
 * {@code /guhs einde2 <slice> ...} next to these):
 * <ul>
 *   <li>{@code /guhs einde2 waar <structuur>}: where the guaranteed copy of a big place lies in this world (start chunk,
 *       origin, box);</li>
 *   <li>{@code /guhs einde2 plek <structuur> <plek>}: takes you to a marker of the guaranteed copy;</li>
 *   <li>{@code /guhs einde2 plekken}: lists the markers of the structure you stand in (or next to).</li>
 * </ul>
 */
public final class Einde2Commando {
    private Einde2Commando() {
    }

    private static List<String> structuren() {
        return new ArrayList<>(Einde2Ids.LEEGTE_STRUCTUREN);
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("waar").then(Commands.argument("structuur", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(structuren(), b))
                        .executes(c -> waar(c.getSource(), StringArgumentType.getString(c, "structuur")))))
                .then(Commands.literal("plek").then(Commands.argument("structuur", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(structuren(), b))
                        .then(Commands.argument("plek", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Plekken.van(StringArgumentType.getString(c, "structuur")).keySet(), b))
                                .executes(c -> plek(c.getSource(), StringArgumentType.getString(c, "structuur"), StringArgumentType.getString(c, "plek"))))))
                .then(Commands.literal("plekken").executes(c -> plekken(c.getSource())))));
    }

    private static ServerLevel guheinde(CommandSourceStack source) {
        return source.getServer().getLevel(GuheindeFeature.GUHEINDE);
    }

    private static int waar(CommandSourceStack source, String structuur) {
        ServerLevel level = guheinde(source);
        ChunkPos start = level == null ? null : Plekken.startChunk(level, structuur);
        BoundingBox doos = start == null ? null : Plekken.doos(level, structuur);
        if (doos == null) {
            source.sendFailure(Component.translatable("gui.guhs.einde2.commando.geen", structuur));
            return 0;
        }
        BlockPos o = Plekken.oorsprong(level, structuur);
        source.sendSuccess(() -> Component.translatable("gui.guhs.einde2.commando.waar", structuur, start.x() + ", " + start.z(), o.toShortString(),
                doos.minX() + " " + doos.minY() + " " + doos.minZ() + " .. " + doos.maxX() + " " + doos.maxY() + " " + doos.maxZ()), false);
        return 1;
    }

    private static int plek(CommandSourceStack source, String structuur, String plek) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = guheinde(source);
        Plekken.Plek p = Plekken.plek(structuur, plek);
        BlockPos at = level == null ? null : Plekken.wereld(level, structuur, plek);
        if (p == null || at == null) {
            source.sendFailure(Component.translatable("gui.guhs.einde2.commando.geen_plek", structuur, plek));
            return 0;
        }
        player.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.of(), p.yaw(), 0f, false);
        source.sendSuccess(() -> Component.translatable("gui.guhs.einde2.commando.plek", structuur, plek, p.soort(), at.toShortString()), false);
        return 1;
    }

    private static int plekken(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        List<String> namen = structuren();
        namen.add(Einde2Ids.MIKA_VESTING);
        for (String structuur : namen) {
            StructureStart start = Plekken.bij(level, structuur, player.blockPosition(), 16);
            if (start == null) {
                continue;
            }
            var plekken = Plekken.van(structuur).values();
            source.sendSuccess(() -> Component.translatable("gui.guhs.einde2.commando.plekken", structuur, plekken.size()), false);
            for (Plekken.Plek p : plekken) {
                BlockPos at = Plekken.wereld(start, p);
                source.sendSuccess(() -> Component.literal("  " + p.id() + " (" + p.soort() + ") " + (at == null ? "-" : at.toShortString())), false);
            }
            return plekken.size();
        }
        source.sendFailure(Component.translatable("gui.guhs.einde2.commando.geen_structuur"));
        return 0;
    }
}
