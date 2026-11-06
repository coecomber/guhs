package nl.juiced.guhs.feature.vadsrijk.ruiters;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-RUITERS (round U, the user's wish: thematic, iconic mounts that bring the world to life): ambient guhs riding Pluimguhs along
 * routes in zone 3 (the riding spiral, the inner stair, Honingtak, branch paths, the Pluimstal, the ground) and zone 1 (a courier,
 * a rider on the forest road). Entities {@code vr_ruiter_pluim} + {@code vr_ruiter_guh}, the manager {@link Ruiters}, the routes
 * {@link RuiterRoutes}. Dev: {@code /guhs vadsrijk ruiters lijst | tp <route> [k] | aan | uit | spawn}. Texts:
 * tools/features/vadsrijk_u_ruiters.py.
 */
public final class RuiterRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RuiterPluim>> PLUIM = ENTITIES.register("vr_ruiter_pluim",
            () -> EntityType.Builder.<RuiterPluim>of(RuiterPluim::new, MobCategory.MISC).sized(1.4f, 2.6f).eyeHeight(2.1f).noSave().fireImmune()
                    .clientTrackingRange(8).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_ruiter_pluim"))));
    public static final DeferredHolder<EntityType<?>, EntityType<RuiterGuh>> GUH = ENTITIES.register("vr_ruiter_guh",
            () -> EntityType.Builder.<RuiterGuh>of(RuiterGuh::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).noSave().fireImmune()
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_ruiter_guh"))));

    private RuiterRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(PLUIM.get(), RijdierEntity.attributen().build());
            e.put(GUH.get(), GuhEntity.createAttributes().build());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Ruiters.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && !Ruiters.magErin(e.getEntity())) {
                e.setCanceled(true);   // a rider this manager didn't spawn (never from disk: the types are noSave)
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (!e.getLevel().isClientSide()) {
                Ruiters.verlaten(e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> Ruiters.wis());
        NeoForge.EVENT_BUS.addListener(RuiterRegistry::commandos);
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("ruiters").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("lijst").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            StringBuilder sb = new StringBuilder("Rijdende guhs: " + (Ruiters.aan ? "AAN" : "UIT") + ", nu zichtbaar " + Ruiters.WERELD.aantal()
                    + " / budget " + Ruiters.BUDGET + " (binnen " + (int) Ruiters.ZICHT + " blokken)");
            for (RuiterRoute r : RuiterRoutes.alle()) {
                int zicht = 0;
                for (int k = 0; k < r.ruiters; k++) {
                    if (Ruiters.WERELD.paar(r.id + "#" + k) != null) {
                        zicht++;
                    }
                }
                sb.append(String.format("\n %s (zone %d, %s): %d ruiter(s), %d blokken, %s, zichtbaar %d", r.id, r.zone,
                        r.modus.name().toLowerCase(), r.ruiters, (int) r.lengte(),
                        r.modus == RuiterRoute.Modus.STAL ? "staan stil" : String.format("%.1f min per rondje", r.periode() / 1200.0), zicht));
            }
            if (sl == null) {
                sb.append("\n (de Vadsrijk-dimensie is niet geladen)");
            }
            c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return RuiterRoutes.alle().size();
        }));
        m.then(Commands.literal("tp").then(Commands.argument("route", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(RuiterRoutes.alle().stream().map(r -> r.id), b))
                .executes(c -> tp(c.getSource(), StringArgumentType.getString(c, "route"), 0))
                .then(Commands.argument("ruiter", IntegerArgumentType.integer(0, 9))
                        .executes(c -> tp(c.getSource(), StringArgumentType.getString(c, "route"), IntegerArgumentType.getInteger(c, "ruiter"))))));
        m.then(Commands.literal("aan").executes(c -> {
            Ruiters.aan = true;
            c.getSource().sendSuccess(() -> Component.literal("Rijdende guhs: AAN"), false);
            return 1;
        }));
        m.then(Commands.literal("uit").executes(c -> {
            Ruiters.aan = false;
            Ruiters.WERELD.allesWeg();
            c.getSource().sendSuccess(() -> Component.literal("Rijdende guhs: UIT (allemaal weg)"), false);
            return 1;
        }));
        m.then(Commands.literal("spawn").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (!Vadsrijk.in(p)) {
                c.getSource().sendFailure(Component.literal("Alleen in Het Vadsrijk."));
                return 0;
            }
            String id = Ruiters.demo(p);
            c.getSource().sendSuccess(() -> Component.literal("Demo-ruiter " + id + ": rondjes (straal 5) om deze plek, 2 minuten."), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }

    private static int tp(CommandSourceStack src, String id, int k) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = Vadsrijk.level(src.getServer());
        RuiterRoute r = RuiterRoutes.van(id);
        if (sl == null || r == null) {
            src.sendFailure(Component.literal("Onbekende route: " + id));
            return 0;
        }
        long t = sl.getGameTime();
        int kk = Math.min(k, r.ruiters - 1);
        RuiterRoute.Stand nu = r.stand(t, kk);
        // stand a bit behind it on its own (walkable) route, looking at it; at the stable: a few blocks in front
        Vec3 naar = r.modus == RuiterRoute.Modus.STAL ? nu.pos().add(nu.richting().scale(5)) : r.stand(t - 60, kk).pos();
        if (naar.distanceToSqr(nu.pos()) < 4) {
            naar = nu.pos().add(nu.richting().scale(-6));
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(nu.pos().x - naar.x), nu.pos().z - naar.z));
        p.teleport(new TeleportTransition(sl, naar.add(0, 0.6, 0), Vec3.ZERO, yaw, 10f, TeleportTransition.DO_NOTHING));
        src.sendSuccess(() -> Component.literal("Bij " + id + " ruiter " + kk), false);
        return 1;
    }
}
