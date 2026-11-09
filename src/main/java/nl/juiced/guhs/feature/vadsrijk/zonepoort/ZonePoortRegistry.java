package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * X-ZONE1: registers the zone gate ({@link ZonePoort}): the server config {@code guhs-vadsrijk-zones.toml}, the safety-net tick and
 * the op command {@code /guhs vadsrijk zones [open <n>]}.
 */
public final class ZonePoortRegistry {
    private ZonePoortRegistry() {
    }

    public static void register(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.SERVER, ZonePoortConfig.SPEC, "guhs-vadsrijk-zones.toml"));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ZonePoort.tik(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ZonePoort.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(ZonePoortRegistry::commando);
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("zones").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> status(c.getSource()));
        z.then(Commands.literal("open").then(Commands.argument("n", IntegerArgumentType.integer(1, 6)).executes(c -> {
            int n = IntegerArgumentType.getInteger(c, "n");
            ZonePoortConfig.zet(n);
            for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
                Spelers.sync(p);   // (the HUD bar: the soft cap moved; the quest tracker follows within a second by itself)
            }
            c.getSource().sendSuccess(() -> Component.literal("Het Vadsrijk: zones 1-" + n + " open"
                    + (n >= 6 ? " (alles open)." : "; zone " + (n + 1) + "-6 binnenkort. Opgeslagen in guhs-vadsrijk-zones.toml.")), true);
            return n;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(z));
    }

    private static int status(CommandSourceStack s) {
        int n = ZonePoort.open();
        s.sendSuccess(() -> Component.literal("Het Vadsrijk: zones 1-" + n + " open" + (n >= 6 ? " (alles)." : ", zone " + (n + 1) + "-6 binnenkort; level-plafond "
                + ZonePoort.PLAFOND[n] + ".") + " Ops/creative mogen overal in. Wijzig met /guhs vadsrijk zones open <1-6>."), false);
        return n;
    }
}
