package nl.juiced.guhs.feature.guhwinter.plein;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.guhwinter.GuhwinterCommando;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 1.10.10 Guhwinter, slice plein: see {@link GuhwinterPlein}. Commands (op / console; Brigadier merges them into kern's
 * {@code /guhs winter} tree):
 * <ul>
 *   <li>{@code /guhs winter inrichten}: the one-time setup (portal move, Reisguh "Spawn", old peak, square, arrival);</li>
 *   <li>{@code /guhs winter plein winter|raam}: (dev) place the winter square or the empty frame again;</li>
 *   <li>{@code /guhs winter anker <naam>}: (dev) where an anchor is.</li>
 * </ul>
 */
public final class GuhwinterPleinFeature {
    private GuhwinterPleinFeature() {
    }

    public static void register(IEventBus modBus) {
        nl.juiced.guhs.feature.guhwinter.Guhwinter.bijEinde(GuhwinterPlein::plaatsRaam);   // 3 Jan: the empty frame
        commands();
        nl.juiced.guhs.world.TikWacht.post("GuhwinterPlein:1", e -> GuhwinterPlein.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> GuhwinterPlein.stop());
    }

    /** Under kern's {@code /guhs winter} ({@link GuhwinterCommando#voegToe}), op only. */
    private static void commands() {
        GuhwinterCommando.voegToe(ctx -> GuhwinterCommando.dev("inrichten").executes(GuhwinterPleinFeature::inrichten));
        GuhwinterCommando.voegToe(ctx -> GuhwinterCommando.dev("plein")
                .then(Commands.literal("winter").executes(c -> plein(c, GuhwinterPlein.WINTER)))
                .then(Commands.literal("raam").executes(c -> plein(c, GuhwinterPlein.RAAM))));
        GuhwinterCommando.voegToe(ctx -> GuhwinterCommando.dev("anker")
                .then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(GuhwinterAnkers.alle().keySet(), b))
                        .executes(GuhwinterPleinFeature::anker)));
    }

    private static int inrichten(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = GuhwinterPlein.guhmension(src.getServer());
        if (level == null) {
            src.sendFailure(Component.translatable("commands.guhs.guhwinter.inrichten.geen_dim"));
            return 0;
        }
        if (!GuhwinterPlein.inrichten(level, regel -> src.sendSuccess(() -> Component.translatable("commands.guhs.guhwinter.inrichten.klaar", regel), true))) {
            src.sendFailure(Component.translatable("commands.guhs.guhwinter.inrichten.bezig", String.valueOf(GuhwinterPlein.stand())));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("commands.guhs.guhwinter.inrichten.start"), true);
        return 1;
    }

    private static int plein(CommandContext<CommandSourceStack> c, Identifier welke) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = GuhwinterPlein.guhmension(src.getServer());
        if (level == null) {
            src.sendFailure(Component.translatable("commands.guhs.guhwinter.inrichten.geen_dim"));
            return 0;
        }
        if (!GuhwinterPlein.plaats(level, welke, regel -> src.sendSuccess(() -> Component.translatable("commands.guhs.guhwinter.inrichten.klaar", regel), true))) {
            src.sendFailure(Component.translatable("commands.guhs.guhwinter.inrichten.bezig", String.valueOf(GuhwinterPlein.stand())));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("commands.guhs.guhwinter.plein.start", welke.getPath()), true);
        return 1;
    }

    private static int anker(CommandContext<CommandSourceStack> c) {
        String naam = StringArgumentType.getString(c, "naam");
        GuhwinterAnkers.Anker a = GuhwinterAnkers.alle().get(naam);
        if (a == null) {
            c.getSource().sendFailure(Component.translatable("commands.guhs.guhwinter.anker.onbekend", naam));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.guhs.guhwinter.anker", naam, a.x(), a.y(), a.z(), a.yaw()), false);
        return 1;
    }
}
