package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * /guhs vadsrijk z2npc ... (dev, op 2): {@code opening}, {@code vlieg kandijkom|haven}, {@code gondel op|af}, {@code draak},
 * {@code flashback}, {@code cliffhanger}, {@code tp <npc|poi.x|knuffel.x|aanwijzing.x|schat.x>}, {@code rij} (every zone-2 NPC in rows
 * north of you), {@code proefvloer} (a plain pink-quartz floor under Kandijkom until the z2-wereld tiles exist), {@code reset}
 * (forget the zone-2 secrets and scenes; quests: /guhs vadsrijk npc quest ...).
 */
public final class Z2Commando {
    private Z2Commando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("z2npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        z.then(Commands.literal("opening").executes(c -> {
            Z2Verhaal.opening(c.getSource().getPlayerOrException());
            return 1;
        }));
        z.then(Commands.literal("vlieg").then(Commands.literal("kandijkom").executes(c -> {
            Z2Verhaal.vlieg(c.getSource().getPlayerOrException(), "kandijkom");
            return 1;
        })).then(Commands.literal("haven").executes(c -> {
            Z2Verhaal.vlieg(c.getSource().getPlayerOrException(), "haven");
            return 1;
        })));
        z.then(Commands.literal("gondel").then(Commands.literal("op").executes(c -> {
            Z2Verhaal.gondel(c.getSource().getPlayerOrException(), true);
            return 1;
        })).then(Commands.literal("af").executes(c -> {
            Z2Verhaal.gondel(c.getSource().getPlayerOrException(), false);
            return 1;
        })));
        z.then(Commands.literal("draak").executes(c -> {
            Z2Verhaal.draak(c.getSource().getPlayerOrException());
            return 1;
        }));
        z.then(Commands.literal("flashback").executes(c -> {
            Z2Verhaal.flashback(c.getSource().getPlayerOrException());
            return 1;
        }));
        z.then(Commands.literal("cliffhanger").executes(c -> {
            Z2Verhaal.cliffhanger(c.getSource().getPlayerOrException());
            return 1;
        }));
        z.then(Commands.literal("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z2Haken.vergeet(p);
            Z2Verhaal.vergeet(p);
            Spelers.data(p).gezien.removeIf(s -> s.startsWith("npc:z2_"));
            c.getSource().sendSuccess(() -> Component.literal("Zone-2 geheimen en verhaalmomenten vergeten."), false);
            return 1;
        }));
        z.then(Commands.literal("rij").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int i = 0;
            for (String id : Z2NpcPlekken.NPC.keySet()) {
                NpcDef d = Npcs.van(id);
                RijkGuh g = d == null ? null : NpcRegistry.GUH.get().create(p.level(), EntitySpawnReason.COMMAND);
                if (g == null) {
                    continue;
                }
                double x = p.getX() + (i % 8 - 3.5) * 2, zz = p.getZ() - 4 - (i / 8) * 2.5;
                BlockPos hier = BlockPos.containing(x, p.getY(), zz);
                g.snapTo(x, p.getY(), zz, 0, 0);
                g.setYHeadRot(0);
                g.setYBodyRot(0);
                g.stelIn(new NpcDef(d.id(), hier, 0, d.variant(), d.kleren(), NpcDef.Gedrag.STAAN, d.rol(), d.maat()));
                p.level().addFreshEntity(g);
                i++;
            }
            int n = i;
            c.getSource().sendSuccess(() -> Component.literal(n + " zone-2 NPC's op een rij."), false);
            return n;
        }));
        z.then(Commands.literal("proefvloer").executes(c -> {
            // dev/AutoCheck only: a plain floor under the Kandijkom box at the NPC height, so the NPCs stand somewhere
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl == null) {
                return 0;
            }
            BlockState roze = Blocks.PINK_CONCRETE.defaultBlockState(), sneeuw = Blocks.SNOW_BLOCK.defaultBlockState();
            int n = 0;
            for (int x = -150 + nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.Z2_DX; x <= 150 + nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.Z2_DX; x++) {
                for (int zz = -2550 + nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.Z2_DZ; zz <= -2236 + nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.Z2_DZ; zz++) {
                    sl.setBlock(new BlockPos(x, Z2NpcPlekken.KK - 1, zz), (Math.floorMod(x, 16) == 0 || Math.floorMod(zz, 16) == 0) ? sneeuw : roze, 2);
                    n++;
                }
            }
            int m = n;
            c.getSource().sendSuccess(() -> Component.literal("Proefvloer onder Kandijkom: " + m + " blokken."), false);
            return 1;
        }));
        z.then(Commands.literal("tp").then(Commands.argument("plek", StringArgumentType.word()).suggests((c, b) -> {
            List<String> l = new ArrayList<>(Z2NpcPlekken.NPC.keySet());
            Z2NpcPlekken.POI.keySet().forEach(k -> l.add("poi." + k));
            Z2NpcPlekken.KNUFFEL.keySet().forEach(k -> l.add("knuffel." + k));
            Z2NpcPlekken.AANWIJZING.keySet().forEach(k -> l.add("aanwijzing." + k));
            Z2NpcPlekken.SCHAT.keySet().forEach(k -> l.add("schat." + k));
            return SharedSuggestionProvider.suggest(l, b);
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String s = StringArgumentType.getString(c, "plek");
            BlockPos b = plek(s);
            if (b == null) {
                c.getSource().sendFailure(Component.literal("Onbekende plek: " + s));
                return 0;
            }
            Z2Verhaal.zet(p, b.offset(2, 0, 0), p.getYRot());
            return 1;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(z));
    }

    static BlockPos plek(String s) {
        int d = s.indexOf('.');
        if (d < 0) {
            return Z2NpcPlekken.NPC.get(s);
        }
        String id = s.substring(d + 1);
        return switch (s.substring(0, d)) {
            case "poi" -> Z2NpcPlekken.POI.get(id);
            case "knuffel" -> Z2NpcPlekken.KNUFFEL.get(id);
            case "aanwijzing" -> Z2NpcPlekken.AANWIJZING.get(id);
            case "schat" -> Z2NpcPlekken.SCHAT.get(id);
            default -> null;
        };
    }
}
