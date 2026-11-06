package nl.juiced.guhs.feature.vadsrijk.zone4.dieren;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken;

/**
 * Het Vadsrijk zone 4 "De Lampionkust", slice "t-z4dieren": the moving things of the sea.
 * <ul>
 *   <li>{@link OuweDobber}, the giant turtle (guhs:vr_z4_ouwe_dobber): one alive while a player is within
 *       {@value #DOBBER_BEREIK} blocks of his loop; swims {@link DobberRoute}, waits at the Schildpadeiland jetty, carries 8.</li>
 *   <li>{@link GuhVlieger}, the guh kites (guhs:vr_z4_vlieger): put up near players at {@link Z4DierenPlekken#VLIEGERS}, taken
 *       down when nobody is around; client-side wind animation with LOD.</li>
 * </ul>
 * Neither is ever saved in a chunk: this manager (every second) makes them. Called from {@code wereld.WereldRegistry} (one
 * marked line). Dev: {@code /guhs vadsrijk z4dieren schildpad tp|rijd|route|halte}, {@code ... vliegers aan|uit|tp}.
 */
public final class Z4DierenRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<OuweDobber>> OUWE_DOBBER = ENTITIES.register("vr_z4_ouwe_dobber",
            () -> EntityType.Builder.<OuweDobber>of(OuweDobber::new, MobCategory.MISC).sized(10f, 3.6f).clientTrackingRange(16)
                    .updateInterval(20).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_z4_ouwe_dobber"))));
    public static final DeferredHolder<EntityType<?>, EntityType<GuhVlieger>> VLIEGER = ENTITIES.register("vr_z4_vlieger",
            () -> EntityType.Builder.<GuhVlieger>of(GuhVlieger::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10)
                    .updateInterval(200).noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_z4_vlieger"))));

    /** A player within this many blocks (horizontal) of the loop centre keeps Ouwe Dobber around. */
    static final int DOBBER_BEREIK = 192;
    /** A player within this many blocks of a kite keeps it up. */
    static final int VLIEGER_BEREIK = 128;

    @Nullable
    private static UUID officieel;
    private static final Map<Integer, UUID> VLIEGERS = new HashMap<>();
    static boolean vliegersAan = true;
    /** Dev time shift for Ouwe Dobber ({@code schildpad halte}). */
    static int verschuif;
    /** Dev: players waiting to be seated on him (ticks left). */
    private static final Map<UUID, Integer> RIJDEN = new HashMap<>();
    /** Dev: players who see the route (ticks left). */
    private static final Map<UUID, Integer> ROUTE = new HashMap<>();

    private Z4DierenRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl != null) {
                tick(sl);
            }
        });
        // logging out on his back: off onto the jetty first (so the player is never saved riding a turtle that isn't there)
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.getVehicle() instanceof OuweDobber) {
                p.stopRiding();
                p.teleportTo(Z4DierenPlekken.DOK_X + 0.5, Z4DierenPlekken.DOK_Y, Z4DierenPlekken.DOK_Z + 0.5);
            }
            RIJDEN.remove(e.getEntity().getUUID());
            ROUTE.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener(Z4DierenRegistry::commandos);
    }

    /** True for the one Ouwe Dobber that may live (the first one that asks when there is none). */
    static boolean isOfficieel(OuweDobber d) {
        if (officieel == null || officieel.equals(d.getUUID())) {
            officieel = d.getUUID();
            return true;
        }
        if (d.level() instanceof ServerLevel sl) {
            Entity o = sl.getEntity(officieel);
            if (o == null || o.isRemoved()) {
                officieel = d.getUUID();
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static OuweDobber dobber(ServerLevel sl) {
        if (officieel == null) {
            return null;
        }
        return sl.getEntity(officieel) instanceof OuweDobber d && !d.isRemoved() ? d : null;
    }

    /**
     * For the story/tracker (t-z4verhaal): where Ouwe Dobber is now (also when he is not loaded: his place is a function of the
     * time). His halte next to the Schildpadeiland jetty is {@link #dobberHalte()}; he waits there 30 s every round.
     */
    public static Vec3 dobberPlek(ServerLevel sl) {
        return OuweDobber.plek(klok(sl));
    }

    /** His halte (water surface) next to the end of the Schildpadeiland jetty. */
    public static Vec3 dobberHalte() {
        Vec3 h = DobberRoute.get().halte();
        return new Vec3(h.x, OuweDobber.OPPERVLAK, h.z);
    }

    /** True while he waits at the halte (good moment to hop on / feed him). */
    public static boolean dobberWacht(ServerLevel sl) {
        return DobberRoute.get().wacht(klok(sl));
    }

    static double klok(ServerLevel sl) {
        return sl.getGameTime() + verschuif;
    }

    static void tick(ServerLevel sl) {
        long t = sl.getGameTime();
        dev(sl, t);
        if (t % 20 != 0) {
            return;
        }
        // --- Ouwe Dobber
        DobberRoute r = DobberRoute.get();
        Vec3 nu = OuweDobber.plek(klok(sl));
        boolean nodig = iemandBij(sl, nu.x, nu.z, DOBBER_BEREIK);
        OuweDobber d = dobber(sl);
        if (d != null) {
            if (!nodig) {
                for (Entity e : List.copyOf(d.getPassengers())) {
                    e.stopRiding();
                }
                d.discard();
                officieel = null;
            } else {
                d.setVerschuif(verschuif);
                if (!d.vast()) {
                    d.setPos(nu.x, nu.y, nu.z);       // also when his chunk does not tick: he keeps swimming
                }
            }
        } else if (nodig && sl.isPositionEntityTicking(BlockPos.containing(nu)) && r.lengte() > 0) {
            OuweDobber nieuw = OUWE_DOBBER.get().create(sl, EntitySpawnReason.TRIGGERED);
            if (nieuw != null) {
                nieuw.setVerschuif(verschuif);
                nieuw.setPos(nu.x, nu.y, nu.z);
                nieuw.setYRot(OuweDobber.yaw(klok(sl)));
                officieel = nieuw.getUUID();
                sl.addFreshEntity(nieuw);
            }
        }
        // --- the kites
        List<Z4DierenPlekken.Vlieger> lijst = Z4DierenPlekken.VLIEGERS;
        for (int i = 0; i < lijst.size(); i++) {
            Z4DierenPlekken.Vlieger v = lijst.get(i);
            UUID id = VLIEGERS.get(i);
            Entity e = id == null ? null : sl.getEntity(id);
            if (e != null && e.isRemoved()) {
                e = null;
            }
            boolean bij = vliegersAan && iemandBij(sl, v.x(), v.z(), VLIEGER_BEREIK);
            if (e != null && !bij) {
                e.discard();
                VLIEGERS.remove(i);
            } else if (e == null && bij && sl.isPositionEntityTicking(BlockPos.containing(v.x(), v.y(), v.z()))) {
                GuhVlieger k = VLIEGER.get().create(sl, EntitySpawnReason.TRIGGERED);
                if (k != null) {
                    k.zet(v);
                    k.index = i;
                    VLIEGERS.put(i, k.getUUID());
                    sl.addFreshEntity(k);
                }
            }
        }
    }

    static boolean iemandBij(ServerLevel sl, double x, double z, int bereik) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && Math.abs(p.getX() - x) < bereik && Math.abs(p.getZ() - z) < bereik) {
                return true;
            }
        }
        return false;
    }

    /** Dev helpers that run every tick: seat waiting players, show the route. */
    private static void dev(ServerLevel sl, long t) {
        if (!RIJDEN.isEmpty()) {
            OuweDobber d = dobber(sl);
            RIJDEN.entrySet().removeIf(en -> {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(en.getKey());
                if (p == null || en.getValue() <= 0) {
                    return true;
                }
                en.setValue(en.getValue() - 1);
                if (d != null && p.level() == sl) {
                    if (p.getVehicle() == d || p.startRiding(d, true, true)) {
                        p.sendOverlayMessage(Component.translatable("vadsrijk.z4dieren.dobber.op").withStyle(ChatFormatting.AQUA));
                        return true;
                    }
                }
                return false;
            });
        }
        if (!ROUTE.isEmpty() && t % 10 == 0) {
            DobberRoute r = DobberRoute.get();
            ROUTE.entrySet().removeIf(en -> {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(en.getKey());
                if (p == null || en.getValue() <= 0 || p.level() != sl) {
                    return true;
                }
                en.setValue(en.getValue() - 10);
                for (double a = 0; a < r.lengte(); a += 3) {
                    Vec3 q = r.punt(a);
                    sl.sendParticles(p, a == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD, true, false, q.x, OuweDobber.OPPERVLAK + 1.5, q.z,
                            1, 0, 0, 0, 0);
                }
                return false;
            });
        }
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static boolean naar(ServerPlayer p, Vec3 v, float yaw) {
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return false;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.stopRiding();
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
        return true;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("z4dieren");
        LiteralArgumentBuilder<CommandSourceStack> s = dev("schildpad");
        s.then(Commands.literal("tp").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Vec3 h = DobberRoute.get().halte();
            Vec3 dok = new Vec3(Z4DierenPlekken.DOK_X + 0.5, Z4DierenPlekken.DOK_Y, Z4DierenPlekken.DOK_Z + 0.5);
            float yaw = (float) Math.toDegrees(Math.atan2(-(h.x - dok.x), h.z - dok.z));
            return naar(p, dok, yaw) ? 1 : 0;
        }));
        s.then(Commands.literal("rijd").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            if (sl == null) {
                return 0;
            }
            Vec3 nu = OuweDobber.plek(klok(sl));
            naar(p, nu.add(0, 4.5, 0), OuweDobber.yaw(klok(sl)));
            RIJDEN.put(p.getUUID(), 200);
            return 1;
        }));
        s.then(Commands.literal("route").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            DobberRoute r = DobberRoute.get();
            ROUTE.put(p.getUUID(), 400);
            double k = sl == null ? 0 : klok(sl);
            OuweDobber d = sl == null ? null : dobber(sl);
            c.getSource().sendSuccess(() -> Component.literal(String.format(
                    "Ouwe Dobber: route %.0f blokken, rondje %d s (+%d s wachten), nu %s (%.0f / %.0f), %s, botsingen in de check: %d",
                    r.lengte(), r.zwemTicks() / 20, DobberRoute.PAUZE / 20, r.wacht(k) ? "wacht bij de steiger" : "zwemt", r.afstand(k), r.lengte(),
                    d == null ? "niet geladen" : String.format("op %.0f %.1f %.0f met %d passagiers", d.getX(), d.getY(), d.getZ(), d.getPassengers().size()),
                    Z4DierenPlekken.CHECK_FOUTEN)), false);
            return 1;
        }));
        s.then(Commands.literal("halte").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl == null) {
                return 0;
            }
            // shift his clock so a new cycle starts now (he jumps to the halte and waits 30 s)
            int cyc = DobberRoute.get().cyclus();
            verschuif = (int) Math.floorMod(-sl.getGameTime(), (long) cyc);
            OuweDobber d = dobber(sl);
            if (d != null) {
                d.setVerschuif(verschuif);
            }
            c.getSource().sendSuccess(() -> Component.literal("Ouwe Dobber staat nu bij de steiger (30 s)."), false);
            return 1;
        }));
        w.then(s);
        LiteralArgumentBuilder<CommandSourceStack> v = dev("vliegers");
        v.then(Commands.literal("aan").executes(c -> {
            vliegersAan = true;
            c.getSource().sendSuccess(() -> Component.literal("Guh-vliegers: aan (" + Z4DierenPlekken.VLIEGERS.size() + " plekken)"), false);
            return 1;
        }));
        v.then(Commands.literal("uit").executes(c -> {
            vliegersAan = false;
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl != null) {
                for (UUID id : VLIEGERS.values()) {
                    Entity e = sl.getEntity(id);
                    if (e != null) {
                        e.discard();
                    }
                }
            }
            VLIEGERS.clear();
            c.getSource().sendSuccess(() -> Component.literal("Guh-vliegers: uit"), false);
            return 1;
        }));
        v.then(Commands.literal("tp").executes(c -> {
            BlockPos b = Z4WereldPlekken.plek("eiland_vliegereiland");
            return b != null && naar(c.getSource().getPlayerOrException(), Vec3.atBottomCenterOf(b), 0) ? 1 : 0;
        }));
        w.then(v);
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
