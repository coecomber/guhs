package nl.juiced.guhs.feature.techsaus;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * Op commands for dev checks and the AutoCheck script tools/autocheck/bbq2_tech-vloeistof.txt (literal texts: they are not
 * for players):
 * <ul>
 *   <li>{@code /guhs techsaus vul <x y z> <kaassaus|frituursaus|water|melk|leeg> [mB]}: sets the tank of the Sausvat or the
 *       machine there (a machine: the tank that is for that sauce);</li>
 *   <li>{@code /guhs techsaus stand <x y z>}: says what the block there holds and what is joined to it by hoses.</li>
 * </ul>
 */
final class SausCommando {
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("techsaus")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("vul").then(Commands.argument("plek", BlockPosArgument.blockPos())
                        .then(Commands.argument("saus", StringArgumentType.word())
                                .executes(c -> vul(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "plek"), StringArgumentType.getString(c, "saus"), Integer.MAX_VALUE))
                                .then(Commands.argument("mb", IntegerArgumentType.integer(0))
                                        .executes(c -> vul(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "plek"), StringArgumentType.getString(c, "saus"),
                                                IntegerArgumentType.getInteger(c, "mb")))))))
                .then(Commands.literal("stand").then(Commands.argument("plek", BlockPosArgument.blockPos())
                        .executes(c -> stand(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "plek")))))));
    }

    private static List<SausTank> tanks(BlockEntity be) {
        if (be instanceof SausvatBlockEntity vat) {
            return List.of(vat.tank());
        }
        return be instanceof SausMachineBlockEntity machine ? machine.tanks() : List.of();
    }

    private static FluidResource saus(String naam) {
        return switch (naam) {
            case "kaassaus" -> Sauzen.kaassaus();
            case "frituursaus" -> Sauzen.frituursaus();
            case "water" -> Sauzen.water();
            case "melk" -> Sauzen.melk();
            default -> FluidResource.EMPTY;
        };
    }

    private static int vul(CommandSourceStack source, BlockPos pos, String naam, int mb) {
        List<SausTank> tanks = tanks(source.getLevel().getBlockEntity(pos));
        FluidResource wat = saus(naam);
        if (tanks.isEmpty() || wat.isEmpty() && !naam.equals("leeg")) {
            source.sendFailure(Component.literal(tanks.isEmpty() ? "Geen Sausvat of sausmachine op " + pos.toShortString() : "Onbekende saus: " + naam));
            return 0;
        }
        int gedaan = 0;
        for (SausTank tank : tanks) {
            if (wat.isEmpty()) {
                tank.zet(FluidResource.EMPTY, 0);
                gedaan++;
            } else if (tank.isValid(0, wat)) {
                tank.zet(wat, Math.min(mb, tank.max()));
                gedaan++;
            }
        }
        int n = gedaan;
        source.sendSuccess(() -> Component.literal(n + " tank(s) op " + pos.toShortString() + " gezet: " + naam), false);
        return gedaan;
    }

    private static int stand(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        List<Component> regels = new ArrayList<>();
        if (be instanceof SausvatBlockEntity vat) {
            regels.add(SausTekst.tank(vat.tank(), FluidResource.EMPTY));
        } else if (be instanceof SausMachineBlockEntity machine) {
            machine.vadsRegels(regels::add);
        }
        for (Component regel : regels) {
            source.sendSuccess(() -> regel, false);
        }
        List<Slangen.Aansluiting> lijst = Slangen.opnieuw(level, pos);
        int echt = 0;
        for (Slangen.Aansluiting a : lijst) {
            echt += a.handler(level) != null ? 1 : 0;
        }
        int n = echt;
        source.sendSuccess(() -> Component.literal("Aansluitingen via slangen: " + n), false);
        return regels.size() + echt;
    }

    private SausCommando() {
    }
}
