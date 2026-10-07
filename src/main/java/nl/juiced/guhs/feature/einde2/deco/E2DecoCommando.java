package nl.juiced.guhs.feature.einde2.deco;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice deco: {@code /guhs einde2 deco toon [bouwsel]}, for looking at the decoration buildings without hunting
 * for them: it puts one template (or all twenty in a row to the east, eight blocks apart) down where the caller stands,
 * its ground layer at the caller's feet. It builds in the world, so it only exists in a dev run (never on the official
 * world); the visual check of the merge step uses it (tools/autocheck/einde2_deco.txt).
 */
public final class E2DecoCommando {
    /** The template y of the ground layer of every decoration building (tools/features/einde2_deco_bouw.py G). */
    public static final int GROND = 4;
    /** Room between two buildings of the row. */
    private static final int TUSSEN = 8;

    private E2DecoCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("deco")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toon")
                        .executes(c -> toon(c.getSource(), null))
                        .then(Commands.argument("bouwsel", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(E2DecoFeature.BOUWSELS, b))
                                .executes(c -> toon(c.getSource(), StringArgumentType.getString(c, "bouwsel"))))))));
    }

    /** The template of a decoration building (null: no such building). */
    public static StructureTemplate template(ServerLevel level, String bouwsel) {
        return E2DecoFeature.BOUWSELS.contains(bouwsel) ? level.getStructureManager().get(Guhs.id("einde2_deco/" + bouwsel)).orElse(null) : null;
    }

    /** Puts a building down with its ground layer one block under {@code voeten} and its north-west corner there; returns its size. */
    public static Vec3i zet(ServerLevel level, String bouwsel, BlockPos voeten) {
        StructureTemplate template = template(level, bouwsel);
        if (template == null) {
            return null;
        }
        BlockPos at = voeten.below(GROND + 1);
        template.placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), 2);
        return template.getSize();
    }

    private static int toon(CommandSourceStack source, String bouwsel) {
        ServerLevel level = source.getLevel();
        BlockPos voeten = BlockPos.containing(source.getPosition());
        int n = 0;
        StringBuilder waar = new StringBuilder();
        for (String id : E2DecoFeature.BOUWSELS) {
            if (bouwsel != null && !bouwsel.equals(id)) {
                continue;
            }
            Vec3i size = zet(level, id, voeten);
            if (size == null) {
                continue;
            }
            waar.append(n == 0 ? "" : ", ").append(id).append(" @ ").append(voeten.getX()).append(' ').append(voeten.getY()).append(' ').append(voeten.getZ());
            voeten = voeten.east(size.getX() + TUSSEN);
            n++;
        }
        if (n == 0) {
            source.sendFailure(Component.literal("geen bouwsel " + bouwsel));
            return 0;
        }
        String tekst = waar.toString();
        source.sendSuccess(() -> Component.literal(tekst), false);
        return n;
    }
}
