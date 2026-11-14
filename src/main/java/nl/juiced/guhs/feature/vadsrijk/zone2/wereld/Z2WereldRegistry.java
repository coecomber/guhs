package nl.juiced.guhs.feature.vadsrijk.zone2.wereld;

import java.util.List;

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
import nl.juiced.guhs.feature.vadsrijk.wereld.Uitkijken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Het Vadsrijk zone 2 "De Splinterpieken", slice "wereld" (Z2_BRIEF): the map (tiles from tools/vadsrijk/zone2 via
 * maak_kaart.py, placed by kern's KaartFeature like zone 1) and its living parts:
 * <ul>
 *   <li>the zone-2 areas in {@link Gebieden} ({@link Z2WereldPlekken#GEBIEDEN}, added in front of zone 1's boxes);</li>
 *   <li>the Spiegelburcht door (a {@link WereldDeuren} door, id {@code spiegelburcht});</li>
 *   <li>{@link Z2Reizen}: the airship line De Haven &lt;-&gt; Kandijkom (gate zone1_klaar + lv 20, opening scene);</li>
 *   <li>{@link Z2Gondel}: the gondola Kandijkom &lt;-&gt; Galmtop;</li>
 *   <li>the three zone-2 viewpoints (zone 1's panorama scene);</li>
 *   <li>client: the zone-2 sky and ambient ({@code zone2.wereld.client}).</li>
 * </ul>
 * Called from {@code wereld.WereldRegistry} (one marked line), so the shared kern files stay untouched.
 */
public final class Z2WereldRegistry {
    private Z2WereldRegistry() {
    }

    /** Is this x/z in zone 2 (the tile range of its map)? */
    public static boolean inZone2(double x, double z) {
        return nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.inZone2(x, z);   // T-WERELD: the zone-2 tile box (it no longer owns a whole z band)
    }

    public static void register(IEventBus modBus) {
        // areas: in front of zone 1's boxes, in the published order (voegVooraan puts each at index 0, so add them reversed)
        List<Gebieden.Gebied> g = Z2WereldPlekken.GEBIEDEN;
        for (int i = g.size() - 1; i >= 0; i--) {
            Gebieden.voegVooraan(g.get(i));
        }
        WereldDeuren.EXTRA.addAll(Z2WereldPlekken.DEUREN);
        nl.juiced.guhs.world.TikWacht.post("Z2WereldRegistry:62", e -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null || sl.getGameTime() % 10 != 0) {
                return;
            }
            for (ServerPlayer p : sl.players()) {
                if (RijkScenes.bezig(p) || !inZone2(p.getX(), p.getZ())) {
                    continue;
                }
                for (WereldPlekken.Uitkijk u : Z2WereldPlekken.UITKIJKEN) {
                    if (p.position().distanceToSqr(Vec3.atBottomCenterOf(u.top())) < 36) {
                        SpelerData d = Spelers.data(p);
                        if (d.gezien.add("vr_w_uitkijk_" + u.id())) {
                            Uitkijken.panorama(p, u);
                        }
                    }
                }
            }
        });
        // before the zone-1 handler (sneak-click on the Haven bell = Kandijkom)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p) || e.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            if (Z2Reizen.klik(p, e.getPos()) || Z2Gondel.klik(p, e.getPos())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener(Z2WereldRegistry::commandos);
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("z2wereld");
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z2WereldPlekken.ALLE.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BlockPos b = Z2WereldPlekken.plek(StringArgumentType.getString(c, "id"));
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            if (b == null || sl == null) {
                c.getSource().sendFailure(Component.literal("Onbekende plek (zie POI_Z2.md)"));
                return 0;
            }
            if (!Vadsrijk.in(p)) {
                VadsrijkCommando.naarBinnen(p, false);
            }
            p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
            return 1;
        })));
        w.then(dev("vlieg").then(Commands.literal("kandijkom").executes(c -> Z2Reizen.naarKandijkom(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("haven").executes(c -> Z2Reizen.naarHaven(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("opnieuw").executes(c -> {
                    Spelers.data(c.getSource().getPlayerOrException()).gezien.remove(Z2Reizen.GEZIEN_EERSTE);
                    return 1;
                })));
        w.then(dev("gondel").then(Commands.literal("op").executes(c -> Z2Gondel.rijd(c.getSource().getPlayerOrException(), true) ? 1 : 0))
                .then(Commands.literal("neer").executes(c -> Z2Gondel.rijd(c.getSource().getPlayerOrException(), false) ? 1 : 0)));
        w.then(dev("uitkijk").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z2WereldPlekken.UITKIJKEN.forEach(u -> b.suggest(u.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (WereldPlekken.Uitkijk u : Z2WereldPlekken.UITKIJKEN) {
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
