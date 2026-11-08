package nl.juiced.guhs.feature.vadsrijk.questlopen;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-QUESTLOPEN: {@code /guhs vadsrijk questlopen} (OP: opens the admin screen that clicks through the main line) and
 * {@code /guhs vadsrijk questlopen verder} (opens it again after a scene / boss fight). Ticks the sessions, hooks the scene
 * recorder/mute into {@link KernPayloads#VOOR_STUUR} and zone 1's scene runner into {@link QlHaken#SCENE}.
 */
public final class QuestLopenRegistry {
    private QuestLopenRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> commando(e));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> QuestLoper.tick(e.getServer()));
        KernPayloads.VOOR_STUUR = QuestLoper::voorStuur;
        QlHaken.SCENE.add(nl.juiced.guhs.feature.vadsrijk.npc.Verhaal::bezig);
    }

    public static void payloads(PayloadRegistrar r) {
        QlNet.payloads(r);
    }

    private static void commando(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("questlopen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> {
                    QuestLoper.open(c.getSource().getPlayerOrException());
                    return 1;
                });
        b.then(Commands.literal("verder").executes(c -> {
            QuestLoper.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        e.getDispatcher().register(VadsrijkCommando.wortel(b));
    }
}
