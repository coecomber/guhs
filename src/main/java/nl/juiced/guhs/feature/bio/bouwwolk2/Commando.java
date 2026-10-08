package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * DEV (gamemasters): {@code /guhs bio bouw-wolk2 ...}
 * <ul>
 *   <li>{@code zet <regenboogbrug|wolkenkasteeltje|bliksemsmidse>}: places the building's template so that its deck is at
 *       your feet (the lift columns hang down under it as far as the world goes), to look at without looking for one;</li>
 *   <li>{@code dag}: forgets that you took the treats of the pot and the hoard today;</li>
 *   <li>{@code inspecteer zoek|kijk|los}: {@link Inspectie}.</li>
 * </ul>
 */
final class Commando {
    /** The y of each template's deck (its anchor): tools/features/bio_bouw_wolk2_bouw.py STRUCTUREN "hoogte". */
    private static final Map<String, Integer> DEK = Map.of("regenboogbrug", 36, "wolkenkasteeltje", 102, "bliksemsmidse", 24);

    static void registreer(RegisterCommandsEvent event) {
        var zet = Commands.literal("zet").then(Commands.argument("naam", StringArgumentType.word()).executes(ctx -> {
            CommandSourceStack bron = ctx.getSource();
            String naam = StringArgumentType.getString(ctx, "naam");
            ServerLevel level = bron.getLevel();
            StructureTemplate t = DEK.containsKey(naam) ? level.getStructureManager().get(Guhs.id(naam)).orElse(null) : null;
            if (t == null) {
                bron.sendFailure(Component.literal("[bouw-wolk2] zet: regenboogbrug, wolkenkasteeltje of bliksemsmidse"));
                return 0;
            }
            Vec3i maat = t.getSize();
            BlockPos hier = BlockPos.containing(bron.getPosition());
            BlockPos hoek = new BlockPos(hier.getX() - maat.getX() / 2, hier.getY() - 1 - DEK.get(naam), hier.getZ() + 3);
            t.placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
            bron.sendSuccess(() -> Component.literal("[bouw-wolk2] " + naam + " gezet, hoek " + hoek.toShortString() + " (ten zuiden van je, dek op jouw hoogte)"), true);
            return 1;
        }));
        var dag = Commands.literal("dag").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Dagtraktatie.zet(p, Dagtraktatie.POT, -1);
            Dagtraktatie.zet(p, Dagtraktatie.SCHAT, -1);
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-wolk2] de pot en de schat zijn weer vol voor jou"), false);
            return 1;
        });
        var inspecteer = Commands.literal("inspecteer")
                .then(Commands.literal("zoek").executes(ctx -> meld(ctx.getSource(), Inspectie.zoek(ctx.getSource().getServer(), guhmensie(ctx.getSource()), 2))))
                .then(Commands.literal("kijk").executes(ctx -> meld(ctx.getSource(), Inspectie.kijk(guhmensie(ctx.getSource())))))
                .then(Commands.literal("los").executes(ctx -> {
                    int n = Inspectie.los(guhmensie(ctx.getSource()));
                    ctx.getSource().sendSuccess(() -> Component.literal("[bouw-wolk2] " + n + " chunks losgelaten"), false);
                    return 1;
                }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("bouw-wolk2").then(zet).then(dag).then(inspecteer))));
    }

    private static ServerLevel guhmensie(CommandSourceStack bron) {
        ServerLevel level = bron.getServer().getLevel(ModDimensions.GUHMENSION);
        return level == null ? bron.getLevel() : level;
    }

    private static int meld(CommandSourceStack bron, List<String> regels) {
        regels.forEach(r -> bron.sendSuccess(() -> Component.literal(r), false));
        return regels.stream().noneMatch(r -> r.contains(" FOUT ")) ? 1 : 0;
    }

    private Commando() {
    }
}
