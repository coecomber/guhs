package nl.juiced.guhs.feature.mewtwo;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The kloon-eiland's game events: your questline to your client at login, Mieuwguh around the island (MewSpawner). */
public final class MewtwoEvents {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MewtwoVoortgang.sync(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MewtwoVoortgang.sync(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MewtwoVoortgang.sync(p);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % MewSpawner.ELKE == 0 && !p.isSpectator()) {
            MewSpawner.kijk(p);
        }
    }

    /**
     * Op commands (tests, AutoCheck scripts): {@code /guhs mewtwo stap <0-4>} puts your questline on that step (with the notes /
     * parts of the steps before it), {@code /guhs mewtwo wis} forgets it (and the tamed Guhtwo), {@code /guhs mewtwo mew}
     * spawns a Mieuwguh at you.
     */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("mewtwo").requires(s -> s.hasPermission(2))
                .then(Commands.literal("stap").then(Commands.argument("stap", IntegerArgumentType.integer(0, MewtwoVoortgang.KLAAR))
                        .executes(ctx -> stap(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stap")))))
                .then(Commands.literal("wis").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MewtwoVoortgang.wis(p);
                    nl.juiced.guhs.feature.verhaal.VerhaalGuhs.vergeet(p, nl.juiced.guhs.feature.verhaal.VerhaalGuh.MEWTWO);
                    ctx.getSource().sendSuccess(() -> Component.literal("mewtwo: questline forgotten"), false);
                    return 1;
                }))
                .then(Commands.literal("mew").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MewSpawner.spawn(p.serverLevel(), p.blockPosition().above(2), p.blockPosition(), false);
                    return 1;
                }))));
    }

    private static int stap(CommandSourceStack source, int stap) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        MewtwoVoortgang.wis(p);
        if (stap >= MewtwoVoortgang.ONDERDELEN) {
            for (int n = 1; n <= MewtwoFeature.NOTITIES; n++) {
                MewtwoVoortgang.vondNotitie(p, n);
            }
        }
        if (stap >= MewtwoVoortgang.MAALTIJD) {
            for (int n = 1; n <= MewtwoFeature.ONDERDELEN; n++) {
                MewtwoVoortgang.vondOnderdeel(p, n);
                MewtwoVoortgang.bouwIn(p, n);
            }
        }
        MewtwoVoortgang.zetStap(p, stap);
        if (stap >= MewtwoVoortgang.KLAAR) {
            nl.juiced.guhs.feature.verhaal.VerhaalGuhs.geefVrij(p, nl.juiced.guhs.feature.verhaal.VerhaalGuh.MEWTWO);
        }
        source.sendSuccess(() -> Component.literal("mewtwo: step " + stap), false);
        return 1;
    }

    private MewtwoEvents() {
    }
}
