package nl.juiced.guhs.feature.vadsrijk.indeling;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * T-WERELD: the connected world layout (W-3). Registers the barrier push-back ({@link Barrieres}) and the dev commands
 * {@code /guhs vadsrijk indeling ...}: teleport to every transition, harbour, viewpoint and barrier edge, show the zone you are
 * in, switch the barriers off for testing. The zone offsets themselves: {@link VadsrijkZones}; the sea: {@link ZeeVulling}.
 */
public final class IndelingRegistry {
    private IndelingRegistry() {
    }

    /** Fixed test spots (stand positions, world): the barrier edges and the transitions, for the cheat sheet. */
    public static final Map<String, Vec3> RANDEN = new LinkedHashMap<>();
    static {
        RANDEN.put("splinterwand", new Vec3(300, 70, -650));       // in the sea under the north cliffs, the spires ahead
        RANDEN.put("splinterwand_west", new Vec3(-60, 70, -660));
        RANDEN.put("wolkenkloof", new Vec3(1066, 64, -720));       // in the open channel between the cloud banks
        RANDEN.put("wolkenkloof_noord", new Vec3(1060, 64, -1000));
        RANDEN.put("wolkenval", new Vec3(1712, 64, -500));         // between the Boom and Lampionhaven
        RANDEN.put("zoetezee", new Vec3(-300, 64, 760));            // the strait Knuffelkamp <-> zone 1
        RANDEN.put("kust_oost_z1", new Vec3(700, 64, 100));         // the inlet east of the Klodderweide
        RANDEN.put("kust_zw_z1", new Vec3(-640, 64, 640));          // the south-west peninsula
        RANDEN.put("z2_oostkust", new Vec3(1000, 64, -1150));       // zone 2's crumbling east coast
        RANDEN.put("z2_zuidkliffen", new Vec3(700, 64, -780));      // under zone 2's south ice cliffs
        RANDEN.put("mistbank", new Vec3(2470, 64, -300));           // zone 4 south-east (zone 4's own map)
        RANDEN.put("hoog_wind", new Vec3(560, 150, -720));          // high over the Splinterwand (the wind wall)
    }

    /** The shared harbour ids and their planned spots (the generated POIs win when the map has them). */
    public static final Map<String, Vec3> HAVENS = new LinkedHashMap<>();
    static {
        // W-SCHEPEN: the pier tips (the generated POIs win anyway)
        HAVENS.put("haven_z1_z2_vertrek", new Vec3(150, 127, -657));
        HAVENS.put("haven_z2_z1_vertrek", new Vec3(704, 91, -880));
        HAVENS.put("haven_z2_z3_vertrek", new Vec3(984, 117, -856));
        HAVENS.put("haven_z3_z2_vertrek", new Vec3(1186, 167, -602));
        HAVENS.put("haven_z3_z4_vertrek", new Vec3(1622, 167, -485));
        HAVENS.put("haven_z4_z3_vertrek", new Vec3(1791, 71, -470));
    }

    public static void register(IEventBus modBus) {
        nl.juiced.guhs.world.TikWacht.post("IndelingRegistry:63", e -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl != null) {
                Barrieres.tik(e.getServer(), sl.getGameTime());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Barrieres.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(IndelingRegistry::commandos);
    }

    /** A POI of any zone by id (zone 1, 2, 3 generated plekken), else the planned harbour spot. */
    public static BlockPos plek(String id) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.plek(id);
        if (b == null) {
            b = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek(id);
        }
        if (b == null) {
            b = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.plek(id);
        }
        if (b == null) {
            b = nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.plek(id);   // U-OVERGANG: zone 4/5 POIs too
        }
        if (b == null) {
            b = nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.plek(id);
        }
        if (b == null && HAVENS.containsKey(id)) {
            b = BlockPos.containing(HAVENS.get(id));
        }
        return b;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int tp(CommandSourceStack src, Vec3 v, String naam) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            return 0;
        }
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return 0;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
        src.sendSuccess(() -> Component.translatable("vadsrijk.indeling.dev.tp", naam,
                (int) v.x + " " + (int) v.y + " " + (int) v.z), false);
        return 1;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("indeling");
        // /guhs vadsrijk indeling plek <id>   (any POI of zone 1-3, incl. haven_* and uitkijk_*)
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            HAVENS.keySet().forEach(b::suggest);
            for (String s : new String[] {"uitkijk_hoedtop", "uitkijk_galmtop", "uitkijk_nest", "uitkijk_kroon",   // U-OVERGANG: epic viewpoints
                    "uitkijk_vuurtoren", "aankomst", "steiger_eind", "dok_landing", "haven_z1_z2_plein", "haven_z2_z3_plein"}) {
                b.suggest(s);
            }
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            BlockPos b = plek(id);
            if (b == null) {
                c.getSource().sendFailure(Component.translatable("vadsrijk.indeling.dev.onbekend", id));
                return 0;
            }
            return tp(c.getSource(), Vec3.atBottomCenterOf(b), id);
        })));
        // /guhs vadsrijk indeling rand <naam>   (the barrier edges)
        w.then(dev("rand").then(Commands.argument("naam", StringArgumentType.word()).suggests((c, b) -> {
            RANDEN.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            String n = StringArgumentType.getString(c, "naam");
            Vec3 v = RANDEN.get(n);
            if (v == null) {
                c.getSource().sendFailure(Component.translatable("vadsrijk.indeling.dev.onbekend", n));
                return 0;
            }
            return tp(c.getSource(), v, n);
        })));
        // /guhs vadsrijk indeling zone   (where am I: zone, free area, barrier kind)
        w.then(dev("zone").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var z = VadsrijkZones.zoneVan(p.getX(), p.getY(), p.getZ());
            boolean vrij = Barrieres.vrij(p.getX(), p.getY(), p.getZ());
            String txt = "zone " + (z == null ? "-" : z.id) + ", vrij gebied: " + (vrij ? "ja" : "nee (" + Barrieres.soort(p.getX(), p.getY(),
                    p.getZ(), p.isInWater()) + ", duwt naar " + Barrieres.dichtste(p.getX(), p.getY(), p.getZ()).zone() + ")")
                    + ", barrieres " + (Barrieres.aan ? "aan" : "uit");
            c.getSource().sendSuccess(() -> Component.literal(txt), false);
            return 1;
        }));
        // /guhs vadsrijk indeling barriere aan|uit
        w.then(dev("barriere").then(Commands.literal("aan").executes(c -> zet(c.getSource(), true)))
                .then(Commands.literal("uit").executes(c -> zet(c.getSource(), false))));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }

    private static int zet(CommandSourceStack src, boolean aan) {
        Barrieres.aan = aan;
        src.sendSuccess(() -> Component.translatable("vadsrijk.indeling.dev.barriere", aan ? "aan" : "uit"), true);
        return 1;
    }
}
