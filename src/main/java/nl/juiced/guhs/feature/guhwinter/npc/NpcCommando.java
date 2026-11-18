package nl.juiced.guhs.feature.guhwinter.npc;

import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /guhs winter npc ... (dev, op 2; brigadier merges it into kern's /guhs winter):
 * <ul>
 *   <li>(nothing): the phase and who stands where;</li>
 *   <li>{@code zorgvoor}: put everyone this phase needs on the plein now; {@code weg}: everyone away (they come back);</li>
 *   <li>{@code fase sint|kerst|oudjaar|weg|echt}: this slice's own phase override (kern's date override comes on top at integration);</li>
 *   <li>{@code hier <naam>}: that guh right here (held), {@code tp <naam>}: you to that guh;</li>
 *   <li>{@code loop <naam>}: that guh walks to you, {@code los <naam>}: back to idling, {@code zeg <naam> <tekst>}: a line in chat.</li>
 * </ul>
 */
public final class NpcCommando {
    private NpcCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> npc = Commands.literal("npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        npc.executes(c -> {
            ServerLevel level = GuhwinterNpcs.level(c.getSource().getServer());
            NpcFase f = GuhwinterNpcs.fase(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal("Guhwinter NPC-fase: " + f + (GuhwinterNpcs.devFase != null ? " (dev)" : "")), false);
            if (level == null) {
                return 0;
            }
            for (String naam : GuhwinterNpcs.namen()) {
                Entity e = GuhwinterNpcs.get(level, naam);
                String waar = e == null ? (GuhwinterNpcs.hoortHier(level, naam) ? "-" : "(niet deze fase)")
                        : String.format(Locale.ROOT, "%.0f %.0f %.0f", e.getX(), e.getY(), e.getZ());
                c.getSource().sendSuccess(() -> Component.literal("  " + naam + ": " + waar), false);
            }
            return 1;
        });
        npc.then(Commands.literal("zorgvoor").executes(c -> {
            ServerLevel level = GuhwinterNpcs.level(c.getSource().getServer());
            if (level != null) {
                GuhwinterNpcs.zorgVoor(level);
            }
            return 1;
        }));
        npc.then(Commands.literal("weg").executes(c -> {
            ServerLevel level = GuhwinterNpcs.level(c.getSource().getServer());
            int n = level == null ? 0 : GuhwinterNpcs.wisAlles(level);
            c.getSource().sendSuccess(() -> Component.literal(n + " weg"), false);
            return n;
        }));
        npc.then(Commands.literal("fase").then(Commands.argument("fase", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"sint", "kerst", "oudjaar", "weg", "echt"}, b))
                .executes(c -> {
                    String s = StringArgumentType.getString(c, "fase");
                    GuhwinterNpcs.devFase = s.equals("echt") ? null : NpcFase.valueOf(s.toUpperCase(Locale.ROOT));
                    ServerLevel level = GuhwinterNpcs.level(c.getSource().getServer());
                    if (level != null) {
                        GuhwinterNpcs.zorgVoor(level);
                    }
                    c.getSource().sendSuccess(() -> Component.literal("NPC-fase: " + GuhwinterNpcs.fase(c.getSource().getServer())), false);
                    return 1;
                })));
        npc.then(Commands.literal("hier").then(naam().executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String naam = StringArgumentType.getString(c, "naam");
            Entity e = GuhwinterNpcs.zet(p.level(), naam, p.position(), p.getYRot() + 180);
            if (e == null) {
                return 0;
            }
            GuhwinterNpcs.hold(p.level(), naam, true);
            return 1;
        })));
        npc.then(Commands.literal("tp").then(naam().executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel level = GuhwinterNpcs.level(c.getSource().getServer());
            Entity e = level == null ? null : GuhwinterNpcs.get(level, StringArgumentType.getString(c, "naam"));
            if (e == null) {
                c.getSource().sendFailure(Component.literal("Niet op het plein (nu)."));
                return 0;
            }
            p.teleportTo(level, e.getX(), e.getY(), e.getZ(), java.util.Set.of(), p.getYRot(), p.getXRot(), true);
            return 1;
        })));
        npc.then(Commands.literal("loop").then(naam().executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return GuhwinterNpcs.walkTo(p.level(), StringArgumentType.getString(c, "naam"), p.position(), 1.0) ? 1 : 0;
        })));
        npc.then(Commands.literal("los").then(naam().executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            GuhwinterNpcs.release(p.level(), StringArgumentType.getString(c, "naam"));
            return 1;
        })));
        npc.then(Commands.literal("zeg").then(naam().then(Commands.argument("tekst", StringArgumentType.greedyString()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            GuhwinterNpcs.say(p.level(), StringArgumentType.getString(c, "naam"), Component.literal(StringArgumentType.getString(c, "tekst")));
            return 1;
        }))));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("winter").then(npc)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> naam() {
        return Commands.argument("naam", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(GuhwinterNpcs.namen(), b));
    }
}
