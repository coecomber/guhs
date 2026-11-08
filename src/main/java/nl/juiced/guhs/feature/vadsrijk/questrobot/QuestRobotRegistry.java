package nl.juiced.guhs.feature.vadsrijk.questrobot;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-QUESTROBOT: the dev command {@code /guhs vadsrijk questrobot zone <n> | alles} (op 2). It plays the quests with a robot player
 * (a FakePlayer "guhs_questrobot" in Het Vadsrijk; YOUR quests, level and inventory are not touched) and writes
 * {@code <game dir>/questrobot/robot.json + robot.md}. {@code zone <n>} plays zones 1..n (the earlier zones are needed for the flags).
 */
public final class QuestRobotRegistry {
    public static final GameProfile ROBOT = new GameProfile(UUID.fromString("9a7e5b0b-0b07-4c0b-8e57-9e5700b07a11"), "guhs_questrobot");

    private QuestRobotRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> commando(e));
    }

    private static void commando(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("questrobot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        b.then(Commands.literal("alles").executes(c -> draai(c, 6)));
        b.then(Commands.literal("zone").then(Commands.argument("n", IntegerArgumentType.integer(1, 6))
                .executes(c -> draai(c, IntegerArgumentType.getInteger(c, "n")))));
        e.getDispatcher().register(VadsrijkCommando.wortel(b));
    }

    private static int draai(CommandContext<CommandSourceStack> c, int totZone) {
        ServerLevel v = Vadsrijk.level(c.getSource().getServer());
        ServerLevel lvl = v != null ? v : c.getSource().getServer().overworld();
        ServerPlayer robot = FakePlayerFactory.get(lvl, ROBOT);
        QuestRobot.Rapport r = QuestRobot.speel(robot, totZone);
        String pad;
        try {
            pad = RobotRapport.schrijf(r).toString();
        } catch (java.io.IOException ex) {
            pad = "(niet geschreven: " + ex + ")";
        }
        String sam = RobotRapport.samenvatting(r);
        String pf = pad;
        c.getSource().sendSuccess(() -> Component.literal("Questrobot klaar in " + r.ms + " ms (zones 1-" + totZone + "):\n" + sam + "rapport: " + pf), false);
        for (String s : r.algemeen) {
            if (s.contains("FOUT")) {
                c.getSource().sendSuccess(() -> Component.literal(s), false);
            }
        }
        return 1;
    }
}
