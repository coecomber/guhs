package nl.juiced.guhs.feature.vadsrijk.grootte;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;

/** U-GROOTTE: dev command {@code /guhs vadsrijk grootte status}: where the map tiles come from (compact pack or the .nbt). */
public final class GrootteRegistry {
    private GrootteRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(GrootteRegistry::commando);
    }

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("grootte").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("status").executes(c -> {
            MinecraftServer s = c.getSource().getServer();
            var sleutels = KaartPakLader.sleutels(s);
            if (sleutels.isEmpty()) {
                c.getSource().sendSuccess(() -> Component.literal("Kaartpak: NIET gevonden, de tegels komen uit de losse .nbt-bestanden.")
                        .withStyle(ChatFormatting.GOLD), false);
                return 0;
            }
            long t0 = System.nanoTime();
            boolean ok = KaartFeature.tegel(c.getSource().getLevel(), 0, 0).isPresent();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            c.getSource().sendSuccess(() -> Component.literal("Kaartpak gevonden: " + sleutels.size() + " tegels, " + KaartPakLader.uniek(s)
                    + " verschillende. Tegel 0,0 " + (ok ? "geladen" : "ONTBREEKT") + " (" + ms + " ms).").withStyle(ChatFormatting.GREEN), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }
}
