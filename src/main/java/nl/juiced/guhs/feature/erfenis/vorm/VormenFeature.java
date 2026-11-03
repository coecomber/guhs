package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 1.9 "Een opknapper!": the building shapes ({@link GebouwVormen}), their creatures ({@link DecoWezens}) and the dev
 * command {@code /guhs erfenis vorm ...} (op) to look at them where you stand:
 * <ul>
 *   <li>{@code vorm <id> <level> <palet> <rot>}: one building (south-east of your feet, on the block you stand on)</li>
 *   <li>{@code vorm alle <level> <palet>}: every building in a row to the east</li>
 *   <li>{@code vorm plukje <soort>}, {@code vorm deco <id> <rot>}, {@code vorm decoalle}, {@code vorm pad <soort>} (a plus with corners)</li>
 *   <li>{@code vorm wis <breedte> <diepte>}: back to grass</li>
 * </ul>
 */
public final class VormenFeature {
    private VormenFeature() {
    }

    public static void register() {
        DecoWezens.register();
        NeoForge.EVENT_BUS.addListener(VormenFeature::commando);
    }

    private static BlockPos hoek(CommandSourceStack src) {
        BlockPos voet = BlockPos.containing(src.getPosition());
        return voet.offset(2, -1, 2);
    }

    private static int klaar(CommandSourceStack src, String tekst) {
        GebouwVormen.klaarNu();
        src.sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("erfenis").then(Commands.literal("vorm")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("alle").then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .then(Commands.argument("palet", IntegerArgumentType.integer(0, 5)).executes(VormenFeature::alle))))
                .then(Commands.literal("plukje").then(Commands.argument("soort", IntegerArgumentType.integer(0, 5)).executes(c -> {
                    GebouwVormen.stempelPlukje(c.getSource().getLevel(), hoek(c.getSource()), IntegerArgumentType.getInteger(c, "soort"));
                    return klaar(c.getSource(), "Plukje " + IntegerArgumentType.getInteger(c, "soort") + " neergezet. Njeg, wat een rommel!");
                })))
                .then(Commands.literal("deco").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(GebouwVormen.decoIds(), b))
                        .then(Commands.argument("rot", IntegerArgumentType.integer(0, 3)).executes(c -> {
                            String id = StringArgumentType.getString(c, "id");
                            GebouwVormen.stempelDeco(c.getSource().getLevel(), id, hoek(c.getSource()), IntegerArgumentType.getInteger(c, "rot"));
                            return klaar(c.getSource(), "Deco " + id + " neergezet.");
                        }))))
                .then(Commands.literal("overhang").then(Commands.literal("aan").executes(c -> {
                    GebouwVormen.OVERHANG = true;
                    return klaar(c.getSource(), "Overhang aan: dakranden en molenwieken steken weer uit (alleen in lucht).");
                })).then(Commands.literal("uit").executes(c -> {
                    GebouwVormen.OVERHANG = false;
                    return klaar(c.getSource(), "Overhang uit: alles blijft binnen het perceel.");
                })))
                .then(Commands.literal("decoalle").executes(VormenFeature::decoAlle))
                .then(Commands.literal("pad").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(GebouwVormen.padSoorten(), b)).executes(VormenFeature::pad)))
                .then(Commands.literal("wis").then(Commands.argument("breedte", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("diepte", IntegerArgumentType.integer(1, 64)).executes(c -> {
                            GebouwVormen.wis(c.getSource().getLevel(), hoek(c.getSource()), IntegerArgumentType.getInteger(c, "breedte"),
                                    IntegerArgumentType.getInteger(c, "diepte"));
                            return klaar(c.getSource(), "Weer gras.");
                        }))))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(GebouwVormen.gebouwen(), b))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                .then(Commands.argument("palet", IntegerArgumentType.integer(0, 5))
                                        .then(Commands.argument("rot", IntegerArgumentType.integer(0, 3)).executes(VormenFeature::een))))))));
    }

    private static int een(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "id");
        if (GebouwVormen.levels(id) == 0) {
            c.getSource().sendFailure(Component.literal("Onbekend gebouw: " + id));
            return 0;
        }
        int level = Math.min(IntegerArgumentType.getInteger(c, "level"), GebouwVormen.levels(id));
        int palet = IntegerArgumentType.getInteger(c, "palet");
        if (palet == 0) {
            palet = GebouwVormen.vormTier(id, level);
        }
        GebouwVormen.stempel(c.getSource().getLevel(), id, level, palet, hoek(c.getSource()), IntegerArgumentType.getInteger(c, "rot"));
        int[] m = GebouwVormen.maat(id);
        return klaar(c.getSource(), id + " L" + level + " (vorm T" + GebouwVormen.vormTier(id, level) + ", palet T" + palet + ", " + m[0] + "x"
                + m[1] + ") neergezet. Vads!");
    }

    private static int alle(CommandContext<CommandSourceStack> c) {
        ServerLevel lvl = c.getSource().getLevel();
        BlockPos o = hoek(c.getSource());
        int level = IntegerArgumentType.getInteger(c, "level");
        int palet = IntegerArgumentType.getInteger(c, "palet");
        int x = 0;
        for (String id : GebouwVormen.gebouwen()) {
            int l = Math.min(level, GebouwVormen.levels(id));
            GebouwVormen.stempel(lvl, id, l, palet == 0 ? GebouwVormen.vormTier(id, l) : palet, o.offset(x, 0, 0), 0);
            x += GebouwVormen.maat(id)[0] + 2;
        }
        return klaar(c.getSource(), "Alle gebouwen op L" + level + " neergezet (" + x + " blokken naar het oosten).");
    }

    private static int decoAlle(CommandContext<CommandSourceStack> c) {
        ServerLevel lvl = c.getSource().getLevel();
        BlockPos o = hoek(c.getSource());
        List<String> ids = GebouwVormen.decoIds();
        int x = 0, z = 0;
        for (String id : ids) {
            int[] m = GebouwVormen.decoMaat(id);
            if (x > 40) {
                x = 0;
                z += 5;
            }
            GebouwVormen.stempelDeco(lvl, id, o.offset(x, 0, z), 0);
            x += m[0] + 1;
        }
        return klaar(c.getSource(), ids.size() + " decostukken neergezet.");
    }

    private static int pad(CommandContext<CommandSourceStack> c) {
        ServerLevel lvl = c.getSource().getLevel();
        BlockPos o = hoek(c.getSource());
        String soort = StringArgumentType.getString(c, "soort");
        // a plus (crossing, straights, ends) and a square ring (corners)
        boolean[][] p = new boolean[9][9];
        for (int i = 0; i < 5; i++) {
            p[2][i] = true;
            p[i][2] = true;
        }
        for (int i = 5; i < 9; i++) {
            p[i][5] = p[i][8] = p[5][i] = p[8][i] = true;
        }
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                if (!p[x][z]) {
                    continue;
                }
                int m = (z > 0 && p[x][z - 1] ? 1 : 0) | (x < 8 && p[x + 1][z] ? 2 : 0) | (z < 8 && p[x][z + 1] ? 4 : 0) | (x > 0 && p[x - 1][z] ? 8 : 0);
                GebouwVormen.pad(lvl, o.offset(x, 0, z), soort, m);
            }
        }
        return klaar(c.getSource(), "Pad " + soort + " neergezet.");
    }
}
