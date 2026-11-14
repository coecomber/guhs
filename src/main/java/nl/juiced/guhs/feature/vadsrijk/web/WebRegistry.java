package nl.juiced.guhs.feature.vadsrijk.web;

import java.nio.file.Path;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * (S-SITE) guhs.nl/vadsrijk: the live export ({@link WebExport}, off by default) and the wiki dump ({@link WikiExport}).
 * Commands (op): {@code /guhs vadsrijk web nu} (write every live file now, also when the config is off: to see the output),
 * {@code /guhs vadsrijk web wikiexport} (the static game-content dump to {@code <server>/vadsrijk_runtime.json}).
 */
public final class WebRegistry {
    private WebRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> WebExport.gestart(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> WebExport.stopt(e.getServer()));
        nl.juiced.guhs.world.TikWacht.post("WebRegistry:31", e -> WebExport.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (WebExport.aan && e.getEntity() instanceof ServerPlayer p) {
                WebExport.bewaar(p.level().getServer(), p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (WebExport.aan && e.getEntity() instanceof ServerPlayer p) {
                WebExport.bewaar(p.level().getServer(), p);
            }
        });
        Haken.VERSLAGEN.add(v -> {
            if (WebExport.aan && v.mob() instanceof VadsrijkBaas b && !b.arenaId.isEmpty()) {
                for (ServerPlayer p : v.helpers()) {
                    WebStats.get(p.level().getServer()).baas(p.getUUID(), p.getName().getString(), b.arenaId);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(WebRegistry::commando);
        NeoForge.EVENT_BUS.addListener(WebRegistry::kaartCommando);   // U-SITE
        // U-SITE: zone 6 hidden places for the live map: the raid tower (De Sterrendovende Toren) and the underground dungeon
        // De Verzonken Sterrenwacht (z6wezens' own rooms: x 3940..4061, z -983..-959, floor y 30); status only, never a position
        WebPosities.VERBORGEN_PLEKKEN.add(p -> nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6Raid.inToren(p) ? WebPosities.RAID : null);
        WebPosities.VERBORGEN_PLEKKEN.add(p -> WebPosities.inZ6Kerker(p.getX(), p.getY(), p.getZ()) ? WebPosities.KERKER : null);
    }

    /** U-SITE: every player can hide from (and come back on) the live map of guhs.nl/vadsrijk. */
    private static void kaartCommando(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("kaart")
                .then(Commands.literal("verberg").executes(c -> kaart(c.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("toon").executes(c -> kaart(c.getSource().getPlayerOrException(), false)))));
    }

    private static int kaart(ServerPlayer p, boolean verberg) {
        WebStats.get(p.level().getServer()).zetVerborgen(p.getUUID(), verberg);
        p.sendSystemMessage(Component.translatable(verberg ? "vadsrijk.web.kaart.verborgen" : "vadsrijk.web.kaart.getoond")
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        return 1;
    }

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("web").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("nu").executes(c -> {
                    Path m = WebExport.nu(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Vadsrijk web: alles geschreven naar " + m.toAbsolutePath()), true);
                    return 1;
                }))
                .then(Commands.literal("wikiexport").executes(c -> {
                    try {
                        Path p = WikiExport.schrijf(c.getSource().getServer().getServerDirectory().resolve("vadsrijk_runtime.json"));
                        c.getSource().sendSuccess(() -> Component.literal("Wiki-export: " + p.toAbsolutePath()), true);
                        return 1;
                    } catch (java.io.IOException e) {
                        c.getSource().sendFailure(Component.literal("Wiki-export mislukt: " + e));
                        return 0;
                    }
                }))));
    }
}
