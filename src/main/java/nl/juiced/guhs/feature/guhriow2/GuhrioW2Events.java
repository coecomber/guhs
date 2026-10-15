package nl.juiced.guhs.feature.guhriow2;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * World 2's hooks into the game: the player's tick (the egg gate, the vadsmunten of the cellars) and the dev command
 * (op 2, literal texts):
 * <pre>
 * /guhs guhriow2 stand     what world 2 remembers of you
 * /guhs guhriow2 uit       Guhshi is hatched for you (without the scene)
 * /guhs guhriow2 wis       forget the hatching, the warp room and the scene (the egg itself is the engine's: /guhs guhrio wis)
 * </pre>
 */
public final class GuhrioW2Events {
    private GuhrioW2Events() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuhrioW2.tick(player);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var w2 = Commands.literal("guhriow2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.literal("Guhrio world 2: egg " + GuhrioKasteel.heeftEi(p) + ", hatched " + GuhrioW2.uitgebroed(p)
                            + ", warp room " + GuhrioW2.warpGevonden(p) + ", vadsmunten 2-1 " + Integer.toBinaryString(GuhrioKasteel.vadsmunten(p, GuhrioW2.LEVEL_1))
                            + " 2-2 " + Integer.toBinaryString(GuhrioKasteel.vadsmunten(p, GuhrioW2.LEVEL_2))), false);
                    return 1;
                }))
                .then(Commands.literal("uit").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean was = GuhrioW2.uitgebroed(p);
                    GuhrioW2.uit(p);
                    return was ? 0 : 1;
                }))
                .then(Commands.literal("wis").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (String key : new String[]{GuhrioW2.UIT, GuhrioW2.WARP_GEVONDEN, GuhrioW2.VADS}) {
                        GuhQuests.saved(p).remove(key);
                    }
                    Cutscenes.vergeet(p, GuhrioW2.SCENE.id());
                    return 1;
                }));
        event.getDispatcher().register(Commands.literal("guhs").then(w2));
    }
}
