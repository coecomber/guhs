package nl.juiced.guhs.feature.safari.rit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.momenten.Momenten;
import nl.juiced.guhs.quest.Scorebord;

/**
 * Keeps the rides of the park running (S3): the trucks on their loop (one about every minute at every stop), the two
 * gondolas, the moored canoe at the jetty and the two race ostriches at the start, all only where the park is loaded and
 * only while somebody is in it (the rides are never saved, so after a restart they simply come back). Also the guide's
 * facts at the stops, renting a canoe, getting off (your guhs come with you) and the race's scoreboard.
 */
public final class Ritten {
    private static final Map<String, UUID> RITTEN = new HashMap<>();
    @Nullable
    private static List<Double> halteAfstanden;
    /** Facts per zone in the language file (gui.guhs.safari.feit.zone.0..FEITEN-1). */
    public static final int FEITEN = 4;

    /** The stop distances along the route, sorted (cached; the route is fixed). */
    public static List<Double> halteAfstanden() {
        List<Double> h = halteAfstanden;
        if (h == null) {
            List<Double> l = new ArrayList<>();
            for (RitData.Halte halte : RitData.get().haltes) {
                l.add(halte.afstand());
            }
            halteAfstanden = h = List.copyOf(l);
        }
        return h;
    }

    public static void vergeet() {
        halteAfstanden = null;
        RitData.vergeet();
    }

    // --- events -----------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty()) {
            RITTEN.clear();
            return;
        }
        onderhoud(level);
    }

    /** Getting off: the truck/canoe lets you go (your guhs come along); the gondola only in a station. */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || event.getLevel().isClientSide() || !(event.getEntityMounting() instanceof ServerPlayer p)) {
            return;
        }
        Entity rit = event.getEntityBeingMounted();
        if (rit instanceof GondelEntity g && g.station() < 0 && !g.isRemoved() && p.isAlive() && !p.isRemoved()) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.kabel.blijf_zitten").withStyle(ChatFormatting.AQUA));
            return;
        }
        if (rit instanceof SchemaRit s) {
            s.zetGuhsAf(p);
        } else if (rit instanceof KanoEntity k) {
            for (Entity e : List.copyOf(k.getPassengers())) {
                if (e instanceof GuhEntity guh && p.getUUID().equals(guh.getOwnerUUID())) {
                    guh.stopRiding();
                }
            }
        }
    }

    // --- keeping the rides in the park ---------------------------------------------------------------------------------------

    private static void onderhoud(ServerLevel level) {
        RitData d = RitData.get();
        long t = level.getGameTime();
        boolean nacht = Momenten.nachtsafari(level);
        if (d.route != null && !d.haltes.isEmpty()) {
            int n = RitSchema.trucks(d.route.lengte(), d.haltes.size());
            for (int k = 0; k < n; k++) {
                RitSchema.Stand s = RitSchema.truck(t, k, n, d.route.lengte(), halteAfstanden());
                TruckEntity truck = zorg(level, "truck" + k, d.route.op(s.afstand()), SafariRitten.TRUCK.get(), k, n);
                if (truck != null) {
                    truck.zetNacht(nacht);
                }
            }
        }
        if (d.kabelA != null && d.kabelB != null) {
            for (int k = 0; k < 2; k++) {
                double[] g = RitSchema.gondel(t, k, d.kabelA.distanceTo(d.kabelB));
                zorg(level, "gondel" + k, d.kabelA.lerp(d.kabelB, g[0]), SafariRitten.GONDEL.get(), k, 2);
            }
        }
        if (d.kanoStart != null) {
            KanoEntity lig = zorg(level, "kano", d.kanoStart, SafariRitten.KANO.get(), 0, 1);
            if (lig != null && d.kano != null) {
                lig.setYRot(d.kano.richting(0, 0));
            }
        }
        if (d.raceWacht != null) {
            for (int k = 0; k < 2; k++) {
                zorg(level, "struisvogel" + k, d.raceWacht.add(k * 2.5, 0, 0), SafariRitten.STRUISVOGEL.get(), k, 2);
            }
            if (d.raceBord != null && level.getGameTime() % 100 < 20
                    && level.hasNearbyAlivePlayer(d.raceBord.x, d.raceBord.y, d.raceBord.z, 48)) {
                Scorebord.show(level, d.raceBord, "safari_race", Scorebord.tekst(level.getServer(),
                        Component.translatable("gui.guhs.safari.race.bord"), List.of(RaceStruisvogelEntity.BORD), List.of(),
                        s -> Component.literal(RaceStruisvogelEntity.tijd(s))));
            }
        }
    }

    /**
     * The ride with this name: made when it is missing and its spot is loaded; dropped when its spot is not ticking any
     * more (a frozen ride far away; it comes back when somebody is near). Moving rides are put on the timetable when made.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends Entity> T zorg(ServerLevel level, String naam, Vec3 plek, EntityType<T> type, int k, int n) {
        UUID id = RITTEN.get(naam);
        Entity e = id == null ? null : level.getEntity(id);
        BlockPos b = BlockPos.containing(plek);
        boolean tikt = level.isPositionEntityTicking(b);
        if (e != null && !e.isRemoved()) {
            if (!level.isPositionEntityTicking(e.blockPosition()) && e.getPassengers().isEmpty()) {
                e.discard();
                RITTEN.remove(naam);
                return null;
            }
            return (T) e;
        }
        if (!tikt) {
            return null;
        }
        T nieuw = type.create(level, EntitySpawnReason.EVENT);
        if (nieuw == null) {
            return null;
        }
        nieuw.snapTo(plek.x, plek.y, plek.z, 0, 0);
        if (nieuw instanceof SchemaRit rit) {
            rit.zetSchema(k, n);
        }
        level.addFreshEntity(nieuw);
        RITTEN.put(naam, nieuw.getUUID());
        return nieuw;
    }

    // --- the guide ---------------------------------------------------------------------------------------------------------------

    /** The truck stands at a stop: the guide tells a fact (to its riders and whoever waits at the stop). */
    static void aanDeHalte(TruckEntity truck, RitData.Halte halte) {
        if (!(truck.level() instanceof ServerLevel level)) {
            return;
        }
        int feit = Math.floorMod(truck.rondes + truck.k(), FEITEN);
        Component tekst = Component.translatable("gui.guhs.safari.rit.gids", Component.translatable("gui.guhs.safari.zone." + halte.zone()),
                Component.translatable("gui.guhs.safari.feit." + halte.zone() + "." + feit)).withStyle(ChatFormatting.GREEN);
        for (ServerPlayer p : level.players()) {
            if (p.getVehicle() == truck || p.distanceToSqr(truck) < 12 * 12) {
                p.sendSystemMessage(tekst);
            }
        }
    }

    // --- the canoe ---------------------------------------------------------------------------------------------------------------

    /** p clicked the moored canoe: a guided trip (or, sneaking, a free canoe) of his own. */
    static void huurKano(ServerPlayer p, KanoEntity lig, boolean vrij) {
        ServerLevel level = (ServerLevel) lig.level();
        KanoEntity kano = SafariRitten.KANO.get().create(level, EntitySpawnReason.EVENT);
        if (kano == null) {
            return;
        }
        kano.snapTo(lig.getX(), lig.getY(), lig.getZ(), lig.getYRot(), 0);
        if (vrij) {
            kano.zetSoort(KanoEntity.VRIJ);
        } else {
            kano.begin(level.getGameTime());
        }
        level.addFreshEntity(kano);
        if (p.startRiding(kano)) {
            for (GuhEntity g : SchemaRit.volgers(p, 10)) {
                if (g.startRiding(kano, true, true)) {
                    break;
                }
            }
            p.sendOverlayMessage(Component.translatable(vrij ? "gui.guhs.safari.kano.vrij" : "gui.guhs.safari.kano.rit"));
        } else {
            kano.discard();
        }
    }

    private Ritten() {
    }
}
