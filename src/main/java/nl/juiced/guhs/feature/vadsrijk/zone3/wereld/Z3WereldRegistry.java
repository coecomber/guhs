package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.wereld.Uitkijken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Het Vadsrijk zone 3 "De Schemerwortel", slice "wereld" (Z3_BRIEF): the map (tiles from tools/vadsrijk/zone3 via
 * {@code maak_kaart.py --zone 3}, placed by kern's KaartFeature like zones 1 and 2) and its living parts:
 * <ul>
 *   <li>areas: zone 3 is layered, so the real areas carry a y-range ({@link Z3WereldPlekken#gebied(double, double, double)});
 *       kern {@link Gebieden} is still flat (x/z), so only three flat boxes go in front of it here (Honingtak town, the
 *       Kaaszeppelinwerf, the whole zone as {@code z3_ring}) for the 2D callers; z3-systemen adds the y-support to kern;</li>
 *   <li>{@link Z3Reizen}: the airship line to the Honingtak dock (gate zone2_klaar + lv 30, opening film), quest action
 *       {@code z3_vlieg} for the captains / travel map;</li>
 *   <li>{@link Z3Liften}: honey lifts, the Honinglift, the Schorsstad lifts (ride scenes);</li>
 *   <li>the wind columns are only PLACED here (map + {@link Z3WereldPlekken#WINDKOLOMMEN}); their physics is z3-systemen's Z3Wind;</li>
 *   <li>the zone-3 viewpoints (zone 1's panorama scene);</li>
 *   <li>client: the zone-3 sky, fog per layer and ambient ({@code zone3.wereld.client}).</li>
 * </ul>
 * Called from {@code wereld.WereldRegistry} (one marked line).
 */
public final class Z3WereldRegistry {
    private Z3WereldRegistry() {
    }

    public static boolean inZone3(double x, double z) {
        return Z3WereldPlekken.inZone3(x, z);
    }

    public static void register(IEventBus modBus) {
        // flat areas for kern's 2D lookups, ONLY when nobody registered the layered zone-3 areas (z3-systemen's Z3Lagen puts the
        // y-aware areas into kern; then these flat boxes would override them): checked once the server starts
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent e) -> vlakkeGebieden());
        QuestMotor.ACTIES.put("z3_vlieg", (p, naar) -> Z3Reizen.naarHoningtak(p));
        npcBindingen();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null) {
                return;
            }
            long t = sl.getGameTime();
            for (ServerPlayer p : sl.players()) {
                if (!inZone3(p.getX(), p.getZ()) || p.isSpectator()) {
                    continue;
                }
                if (t % 10 == 0 && !RijkScenes.bezig(p)) {
                    for (WereldPlekken.Uitkijk u : Z3WereldPlekken.UITKIJKEN) {
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
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p) || e.getHand() != InteractionHand.MAIN_HAND
                    || !inZone3(p.getX(), p.getZ())) {
                return;
            }
            if (Z3Reizen.klik(p, e.getPos()) || Z3Liften.klik(p, e.getPos())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener(Z3WereldRegistry::commandos);
    }

    static void vlakkeGebieden() {
        if (Gebieden.van("z3_kruinzee") != null) {
            return;
        }
        Gebieden.voegVooraan(new Gebieden.Gebied("z3_ring", Z3WereldPlekken.ZONE_MIN_X, Z3WereldPlekken.TEGEL_Z0 * 64,
                Z3WereldPlekken.TEGEL_X1 * 64 + 63, Z3WereldPlekken.TEGEL_Z1 * 64 + 63, 30, 50, false));
        for (Z3WereldPlekken.Gebied3 g : Z3WereldPlekken.GEBIEDEN) {
            if (!g.cilinder() && (g.id().equals("z3_zeppelinwerf") || g.id().equals("z3_honingtak"))) {
                Gebieden.voegVooraan(new Gebieden.Gebied(g.id(), g.a(), g.b(), g.c(), g.d(), g.minLevel(), g.maxLevel(), g.stad()));
            }
        }
    }

    /** The npc slice's optional bindings (POI_VERZOEKEN_Z3): the Honinglift opens / rides, a cave lantern lights. */
    static void npcBindingen() {
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppel("z3_honinglift_open",
                p -> nl.juiced.guhs.feature.vadsrijk.npc.Questlog.zetVlag(p, "z3_honinglift", true));
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppelMet("z3_honinglift", (p, arg) -> {
            Z3WereldPlekken.Lift l = Z3Liften.lift("honinglift");
            if (l != null) {
                Z3Liften.rijd(p, l, "op".equals(arg));
            }
        });
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppelMet("z3_grotlantaarn", (p, arg) -> {
            BlockPos b = Z3WereldPlekken.plek("hart_grotlantaarn_" + arg);
            if (b != null && p.level() instanceof ServerLevel sl) {
                BlockPos lamp = b.above(2);
                if (sl.getBlockState(lamp).is(net.minecraft.world.level.block.Blocks.SOUL_LANTERN)) {
                    sl.setBlock(lamp, net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState(), 3);
                }
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, lamp.getX() + 0.5, lamp.getY() + 0.5, lamp.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.01);
            }
        });
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("z3wereld");
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z3WereldPlekken.ALLE.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BlockPos b = Z3WereldPlekken.plek(StringArgumentType.getString(c, "id"));
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            if (b == null || sl == null) {
                c.getSource().sendFailure(Component.literal("Onbekende plek (zie POI_Z3.md)"));
                return 0;
            }
            if (!Vadsrijk.in(p)) {
                VadsrijkCommando.naarBinnen(p, false);
            }
            p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
            return 1;
        })));
        w.then(dev("gebied").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z3WereldPlekken.Gebied3 g = Z3WereldPlekken.gebied(p.getX(), p.getY(), p.getZ());
            Z3WereldPlekken.Gebied3 m = Z3WereldPlekken.mobzone(p.getX(), p.getY(), p.getZ());
            c.getSource().sendSuccess(() -> Component.literal("gebied " + (g == null ? "-" : g.id()) + ", mobzone " + (m == null ? "-" : m.id())), false);
            return 1;
        }));
        w.then(dev("vlieg").then(Commands.literal("honingtak").executes(c -> Z3Reizen.naarHoningtak(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("opnieuw").executes(c -> {
                    Spelers.data(c.getSource().getPlayerOrException()).gezien.remove(Z3Reizen.GEZIEN_EERSTE);
                    return 1;
                })));
        w.then(dev("lift").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z3WereldPlekken.LIFTEN.forEach(l -> b.suggest(l.id()));
            return b.buildFuture();
        }).then(Commands.literal("op").executes(c -> {
            Z3WereldPlekken.Lift l = Z3Liften.lift(StringArgumentType.getString(c, "id"));
            return l != null && Z3Liften.rijd(c.getSource().getPlayerOrException(), l, true) ? 1 : 0;
        })).then(Commands.literal("neer").executes(c -> {
            Z3WereldPlekken.Lift l = Z3Liften.lift(StringArgumentType.getString(c, "id"));
            return l != null && Z3Liften.rijd(c.getSource().getPlayerOrException(), l, false) ? 1 : 0;
        }))));
        w.then(dev("uitkijk").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z3WereldPlekken.UITKIJKEN.forEach(u -> b.suggest(u.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (WereldPlekken.Uitkijk u : Z3WereldPlekken.UITKIJKEN) {
                if (u.id().equals(id)) {
                    Uitkijken.panorama(c.getSource().getPlayerOrException(), u);
                    return 1;
                }
            }
            return 0;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
