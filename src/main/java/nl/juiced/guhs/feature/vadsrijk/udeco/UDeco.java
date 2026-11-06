package nl.juiced.guhs.feature.vadsrijk.udeco;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-DECO (round U, feedback round 4): the runtime half of the decoration fixes; the map half is in the generators
 * (tools/vadsrijk/kamp/kamp.py, zone3/bouw3.py, zone4/kit/eilanden.py + bouw4.py).
 * <ul>
 * <li>{@link Aankomstvrij}: no mob spawns within 24 blocks of gondola stations, harbours, docks, campfires and town edges;</li>
 * <li>{@link HoningtakBijen}: vanilla bees + guhbijtjes on the Honingtak deck (tame, never sting, never in a hive);</li>
 * <li>{@link Verdiepingborden}: name boards over the landings of the spiral stair in the tree.</li>
 * </ul>
 * Dev command {@code /guhs vadsrijk udeco vrij|bijen|borden}.
 */
public final class UDeco {
    private UDeco() {
    }

    public static void register(IEventBus modBus) {
        Aankomstvrij.registreer();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel sl = e.getServer().getLevel(Vadsrijk.DIM);
            if (sl != null) {
                HoningtakBijen.tick(sl);
                Verdiepingborden.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener(HoningtakBijen::geenSteek);
        NeoForge.EVENT_BUS.addListener(UDeco::commando);
    }

    static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("udeco").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("vrij").executes(UDeco::vrij))
                .then(Commands.literal("bijen").executes(UDeco::bijen))
                .then(Commands.literal("borden").executes(c -> {
                    Verdiepingborden.weg();
                    c.getSource().sendSuccess(() -> Component.literal("Verdiepingborden opnieuw: ze komen terug zodra je in de buurt bent ("
                            + Verdiepingborden.AANTAL + " stuks, trap_bordes_1..6)."), false);
                    return 1;
                }))));
    }

    /** Is a spawn allowed here, and the nearest no-spawn spots. */
    private static int vrij(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BlockPos b = p.blockPosition();
        List<Aankomstvrij.Punt> dicht = Aankomstvrij.punten().stream()
                .sorted(Comparator.comparingDouble(q -> q.pos().distSqr(b))).limit(4).toList();
        c.getSource().sendSuccess(() -> Component.literal("Aankomstvrij (" + Aankomstvrij.STRAAL + " blokken): "
                + Aankomstvrij.punten().size() + " plekken; stadsrand hier: " + Aankomstvrij.stadsrand(b)), false);
        for (Aankomstvrij.Punt q : dicht) {
            long d = Math.round(Math.sqrt(q.pos().distSqr(b)));
            c.getSource().sendSuccess(() -> Component.literal(" - " + q.id() + (q.strikt() ? " (aankomst)" : " (kampvuur)") + ": " + d
                    + " blokken" + (Aankomstvrij.binnen(b, q.pos(), Aankomstvrij.STRAAL) ? "  -> hier spawnt niets" : "")), false);
        }
        return 1;
    }

    private static int bijen(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<Bee> l = p.level().getEntitiesOfClass(Bee.class, new AABB(p.blockPosition()).inflate(40), b -> b.entityTags().contains(HoningtakBijen.TAG));
        long guh = l.stream().filter(b -> b.getType() == nl.juiced.guhs.registry.ModEntities.GUH_BEE.get()).count();
        c.getSource().sendSuccess(() -> Component.literal("Honingtak-bijen binnen 40 blokken: " + (l.size() - guh) + " gewone bijen, " + guh
                + " guhbijtjes (op het dek: " + HoningtakBijen.opHoningtak(p.getX(), p.getY(), p.getZ()) + "; alleen overdag, niet bij regen)."), false);
        return 1;
    }
}
