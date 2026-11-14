package nl.juiced.guhs.feature.vadsrijk.dubbel;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-DUBBEL: hooks {@link Dubbel} in (one line in VadsrijkFeature). Dev: {@code /guhs vadsrijk dubbel scan} (lists doubles of
 * unique entities in the loaded chunks of the dimension you are in, or the Vadsrijk), {@code dubbel opruimen} (removes the
 * extras, also the heuristic groups), {@code dubbel stats}.
 */
public final class DubbelRegistry {
    private DubbelRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, EntityJoinLevelEvent.class, Dubbel::binnen);
        NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, Dubbel::verborgen);
        nl.juiced.guhs.world.TikWacht.post("DubbelRegistry:39", e -> Dubbel.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> Dubbel.wis());
        NeoForge.EVENT_BUS.addListener(DubbelRegistry::commandos);
    }

    private static ServerLevel level(CommandContext<CommandSourceStack> c) {
        ServerLevel hier = c.getSource().getLevel();
        ServerLevel vr = Vadsrijk.level(c.getSource().getServer());
        return hier.dimension() == Vadsrijk.DIM || vr == null ? hier : vr;
    }

    static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> d = Commands.literal("dubbel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        d.then(Commands.literal("scan").executes(c -> {
            ServerLevel sl = level(c);
            List<Dubbel.Groep> l = Dubbel.scan(sl);
            if (l.isEmpty()) {
                c.getSource().sendSuccess(() -> Component.literal("Geen dubbele unieke wezens in de geladen chunks van "
                        + sl.dimension().identifier() + ".").withStyle(ChatFormatting.GREEN), false);
                return 0;
            }
            c.getSource().sendSuccess(() -> Component.literal(l.size() + " groep(en) dubbel in " + sl.dimension().identifier() + ":")
                    .withStyle(ChatFormatting.GOLD), false);
            for (Dubbel.Groep g : l) {
                Entity e = g.kopieen().get(0);
                MutableComponent r = Component.literal((g.streng() ? "  ● " : "  ○ ") + g.wat() + " ×" + g.kopieen().size() + "  ")
                        .withStyle(g.streng() ? ChatFormatting.RED : ChatFormatting.YELLOW);
                for (Entity k : g.kopieen()) {
                    String pos = k.getBlockX() + " " + k.getBlockY() + " " + k.getBlockZ();
                    r.append(Component.literal("[" + pos + "] ").withStyle(s -> s.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + pos))));
                }
                r.append(Component.literal(e.getName().getString()).withStyle(ChatFormatting.GRAY));
                c.getSource().sendSuccess(() -> r, false);
            }
            c.getSource().sendSuccess(() -> Component.literal("● = zelfde unieke sleutel (wordt elke 10 s vanzelf opgeruimd), "
                    + "○ = zelfde NPC/naam op dezelfde plek (alleen met 'opruimen').").withStyle(ChatFormatting.DARK_GRAY), false);
            return l.size();
        }));
        d.then(Commands.literal("opruimen").executes(c -> {
            ServerLevel sl = level(c);
            int n = Dubbel.opruimen(sl, true);
            c.getSource().sendSuccess(() -> Component.literal(n + " dubbele kopie(en) weggehaald."), false);
            return n;
        }));
        d.then(Commands.literal("stats").executes(c -> {
            ServerLevel sl = level(c);
            c.getSource().sendSuccess(() -> Component.literal("Sinds de start: " + Dubbel.geweigerd + " oude kopie(en) van schijf geweigerd, "
                    + Dubbel.vervangen + " vervangen, " + Dubbel.geestenWeg + " spook-wezens (niet-opgeslagen, verborgen) weggehaald, "
                    + Dubbel.opgeruimd + " opgeruimd. Bekende unieke wezens hier: " + Dubbel.bekend(sl) + "."), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(d));
    }
}
