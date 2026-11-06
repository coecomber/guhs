package nl.juiced.guhs.feature.vadsrijk.zone4.wereld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.wereld.Uitkijken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Het Vadsrijk zone 4 "De Lampionkust", slice "t-z4wereld": the map (tiles from tools/vadsrijk/zone4 via
 * {@code maak_kaart.py --zone 4}, placed by kern's KaartFeature like zones 1-3) and its living edges:
 * <ul>
 *   <li>areas: the islands as kern {@link Gebieden} (flat; bounding boxes of the island ellipses of
 *       {@link Z4WereldPlekken#GEBIEDEN}, then the whole zone {@code z4_zee}); level ranges lv 50-56, Lampionhaven = town;</li>
 *   <li>the Kringstroom: swimmers near the edge of the zone are pushed back gently towards the middle (the sailboat slice
 *       does the same for its boat with {@link Z4WereldPlekken#stroming});</li>
 *   <li>the zone-4 viewpoints (zone 1's panorama scene);</li>
 *   <li>client: the grey fog of the Grauwe Mistbank around the Mysterie-eiland ({@code zone4.wereld.client.Z4WereldClient}).</li>
 * </ul>
 * Biomes "Het Vadsrijk - &lt;island&gt;": {@code biomen.VadsrijkBiomen} asks {@link #biome(int, int)} (one marked hook).
 * Called from {@code wereld.WereldRegistry} (one marked line). Dev: {@code /guhs vadsrijk z4wereld ...}.
 */
public final class Z4WereldRegistry {
    /** Push per tick at full current (blocks/tick); a swimmer's own speed is ~0.1. */
    static final double DUW = 0.045;
    private static final Map<UUID, Long> GEMELD = new ConcurrentHashMap<>();

    private Z4WereldRegistry() {
    }

    public static boolean inZone4(double x, double z) {
        return Z4WereldPlekken.inZone4(x, z);
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> gebieden());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null) {
                return;
            }
            long t = sl.getGameTime();
            for (ServerPlayer p : sl.players()) {
                if (!inZone4(p.getX(), p.getZ()) || p.isSpectator()) {
                    continue;
                }
                stroming(p, t);
                if (t % 10 == 0 && !RijkScenes.bezig(p)) {
                    for (WereldPlekken.Uitkijk u : Z4WereldPlekken.UITKIJKEN) {
                        if (p.position().distanceToSqr(Vec3.atBottomCenterOf(u.top())) < 36) {
                            SpelerData d = Spelers.data(p);
                            if (d.gezien.add("vr_w_uitkijk_" + u.id())) {
                                Uitkijken.panorama(p, u);
                            }
                        }
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> GEMELD.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(Z4WereldRegistry::commandos);
    }

    /** The flat kern areas (once, when the server starts; skipped when somebody already registered zone 4). */
    static void gebieden() {
        if (Gebieden.van("z4_zee") != null) {
            return;
        }
        for (Gebieden.Gebied g : vlakkeGebieden().reversed()) {
            Gebieden.voegVooraan(g);
        }
    }

    /** The zone-4 areas as kern boxes, in lookup order (islands first, the whole zone last). */
    public static List<Gebieden.Gebied> vlakkeGebieden() {
        List<Gebieden.Gebied> uit = new ArrayList<>();
        for (Z4WereldPlekken.Gebied4 g : Z4WereldPlekken.GEBIEDEN) {
            if (g.id().equals("z4_zee")) {
                uit.add(new Gebieden.Gebied(g.id(), Z4WereldPlekken.TEGEL_X0 * 64, Z4WereldPlekken.TEGEL_Z0 * 64,
                        Z4WereldPlekken.TEGEL_X1 * 64 + 63, Z4WereldPlekken.TEGEL_Z1 * 64 + 63, g.minLevel(), g.maxLevel(), false));
            } else {
                // the box inside the ellipse edge (0.85): neighbouring islands never overlap
                int rx = (int) Math.round(g.rx() * 0.85), rz = (int) Math.round(g.rz() * 0.85);
                uit.add(new Gebieden.Gebied(g.id(), g.cx() - rx, g.cz() - rz, g.cx() + rx, g.cz() + rz, g.minLevel(), g.maxLevel(), g.stad()));
            }
        }
        return uit;
    }

    /** The biome id (without "vadsrijk_") for a zone-4 column, or null outside zone 4 (VadsrijkBiomen hook). */
    public static String biome(int x, int z) {
        Z4WereldPlekken.Gebied4 g = Z4WereldPlekken.gebied(x + 0.5, z + 0.5);
        if (g == null) {
            return inZone4(x, z) ? "lampionkust" : null;
        }
        return switch (g.id()) {
            case "z4_lampionhaven" -> "lampionhaven";
            case "z4_knuffeltij" -> "knuffeltij";
            case "z4_strandfeest" -> "strandfeest";
            case "z4_bloemeneiland" -> "bloemeneiland";
            case "z4_koraalbaai" -> "koraalbaai";
            case "z4_vliegereiland" -> "vliegereiland";
            case "z4_vuurtoreneiland" -> "vuurtoreneiland";
            case "z4_pierkop" -> "pierkop";
            case "z4_mysterie" -> "mysterie";
            case "z4_schildpad" -> "schildpadeiland";
            case "z4_wrak" -> "wrak";
            default -> "lampionkust";
        };
    }

    /** The Kringstroom: in the water (or in a boat/mount on it) near the zone edge you drift back towards the middle. */
    static void stroming(ServerPlayer p, long t) {
        double s = Z4WereldPlekken.stroming(p.getX(), p.getZ());
        if (s <= 0 || p.isCreative()) {
            return;
        }
        boolean opWater = p.isInWater() || (p.getVehicle() != null && p.getVehicle().isInWater());
        if (!opWater && p.getY() > Z4WereldPlekken.ZEE + 4) {
            return;    // flying / on a jetty high above the water: no current
        }
        double dx = Z4WereldPlekken.STROOM_X - p.getX(), dz = Z4WereldPlekken.STROOM_Z - p.getZ();
        double l = Math.max(1, Math.sqrt(dx * dx + dz * dz));
        double k = DUW * s;
        Vec3 duw = new Vec3(dx / l * k, 0, dz / l * k);
        if (p.getVehicle() != null) {
            // the sailboat slice may push its own boat; a vehicle without its own current handling drifts here
            if (!Z4Stroming.eigenStroming(p.getVehicle())) {
                p.getVehicle().setDeltaMovement(p.getVehicle().getDeltaMovement().add(duw));
                p.getVehicle().hurtMarked = true;
            }
        } else {
            p.setDeltaMovement(p.getDeltaMovement().add(duw));
            p.hurtMarked = true;
        }
        if (s > 0.3 && t - GEMELD.getOrDefault(p.getUUID(), -10000L) > 600) {
            GEMELD.put(p.getUUID(), t);
            p.sendOverlayMessage(Component.translatable("vadsrijk.z4wereld.kringstroom").withStyle(ChatFormatting.AQUA));
        }
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static boolean naar(ServerPlayer p, BlockPos b) {
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (b == null || sl == null) {
            return false;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
        return true;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("z4wereld");
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z4WereldPlekken.ALLE.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            if (!naar(c.getSource().getPlayerOrException(), Z4WereldPlekken.plek(StringArgumentType.getString(c, "id")))) {
                c.getSource().sendFailure(Component.literal("Onbekende plek (zie POI_Z4.md)"));
                return 0;
            }
            return 1;
        })));
        w.then(dev("eiland").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z4WereldPlekken.ALLE.keySet().stream().filter(k -> k.startsWith("eiland_")).forEach(k -> b.suggest(k.substring(7)));
            b.suggest("lampionhaven");
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            BlockPos b = id.equals("lampionhaven") ? Z4WereldPlekken.plek("respawn") : Z4WereldPlekken.plek("eiland_" + id);
            return naar(c.getSource().getPlayerOrException(), b) ? 1 : 0;
        })));
        w.then(dev("dok").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z4WereldPlekken.DOKKEN.forEach(d -> b.suggest(d.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (Z4WereldPlekken.Dok d : Z4WereldPlekken.DOKKEN) {
                if (d.id().equals(id)) {
                    return naar(c.getSource().getPlayerOrException(), d.steiger()) ? 1 : 0;
                }
            }
            return 0;
        })));
        w.then(dev("gebied").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z4WereldPlekken.Gebied4 g = Z4WereldPlekken.gebied(p.getX(), p.getZ());
            Z4WereldPlekken.Gebied4 m = Z4WereldPlekken.mobzone(p.getX(), p.getZ());
            Gebieden.Gebied k = Gebieden.op(p.getX(), p.getY(), p.getZ());
            String bi = biome(p.getBlockX(), p.getBlockZ());
            c.getSource().sendSuccess(() -> Component.literal("z4 gebied " + (g == null ? "-" : g.id()) + ", kern " + (k == null ? "-" : k.id())
                    + ", mobzone " + (m == null ? "-" : m.id()) + ", biome " + (bi == null ? "-" : bi)
                    + String.format(", stroming %.2f, mist %.2f", Z4WereldPlekken.stroming(p.getX(), p.getZ()), Z4WereldPlekken.mist(p.getX(), p.getZ()))), false);
            return 1;
        }));
        w.then(dev("uitkijk").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z4WereldPlekken.UITKIJKEN.forEach(u -> b.suggest(u.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (WereldPlekken.Uitkijk u : Z4WereldPlekken.UITKIJKEN) {
                if (u.id().equals(id)) {
                    Uitkijken.panorama(c.getSource().getPlayerOrException(), u);
                    return 1;
                }
            }
            return 0;
        })));
        w.then(dev("stroming").executes(c -> {
            // to the edge of the zone, in the water south of the Strandfeesteiland: you should drift back north
            return naar(c.getSource().getPlayerOrException(), new BlockPos(1995, Z4WereldPlekken.ZEE + 1, -748)) ? 1 : 0;
        }));
        w.then(dev("mist").executes(c -> naar(c.getSource().getPlayerOrException(), Z4WereldPlekken.plek("mysterie_strand")) ? 1 : 0));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
