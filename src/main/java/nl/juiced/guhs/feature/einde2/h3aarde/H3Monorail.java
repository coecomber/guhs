package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.storage.Nbt;

/**
 * The monorail of Ba Sing Guh (DESIGN 5 h3: "monorail tussen de ringen die je met aardesturen voortduwt"): a little car on
 * the beam from the outer wall to the upper ring ({@code monorail_route}), there for you alone. Step into it at the outer
 * wall ({@code monorail_wagen}); every jump is a push of earth bending (a thud, dust off the beam), the car glides and slowly
 * loses speed, so you keep pushing. In the story it stops at station Middenring (the tea house is down the stairs); once the
 * chapter is done you may ride it all the way up, as often as you like. Crouching gets you out: you are put back on the
 * platform you came from (never a fall off the viaduct).
 * <p>
 * The car is six block displays (a floor that you ride, four posts and a green roof) that move together every tick: no
 * entity type of our own, nothing saved ({@link H3Acteurs} cleans up after a restart).
 */
public final class H3Monorail {
    /** Speed (blocks per tick) a push adds, the top speed, and what a tick takes off. */
    public static final double DUW = 0.11, MAX = 0.55, WRIJVING = 0.985, REM = 0.0015;
    /** How near the waiting car you must be to step in (blocks). */
    public static final double INSTAP = 4.5;

    private static final class Rit {
        final ServerLevel level;
        final List<Vec3> punten;
        final double[] tot;
        final List<Entity> delen;
        final double eind;
        final String eindStation, beginStation;
        double s, v;
        boolean sprong;
        int duwen;

        Rit(ServerLevel level, List<Vec3> punten, List<Entity> delen, double eind, String beginStation, String eindStation) {
            this.level = level;
            this.punten = punten;
            this.delen = delen;
            this.eind = eind;
            this.beginStation = beginStation;
            this.eindStation = eindStation;
            this.tot = new double[punten.size()];
            for (int i = 1; i < punten.size(); i++) {
                tot[i] = tot[i - 1] + punten.get(i).distanceTo(punten.get(i - 1));
            }
        }

        Vec3 op(double afstand) {
            for (int i = 1; i < punten.size(); i++) {
                if (afstand <= tot[i] || i == punten.size() - 1) {
                    double f = (afstand - tot[i - 1]) / Math.max(1e-6, tot[i] - tot[i - 1]);
                    return punten.get(i - 1).lerp(punten.get(i), Math.max(0, Math.min(1, f)));
                }
            }
            return punten.get(0);
        }
    }

    private static final Map<UUID, Rit> RITTEN = new ConcurrentHashMap<>();

    private H3Monorail() {
    }

    /** Is this player riding the monorail now? */
    public static boolean rijdt(ServerPlayer p) {
        return RITTEN.containsKey(p.getUUID());
    }

    /** (every 10 ticks from H3Verloop) the waiting car; step in when you come near it. */
    static void tik(ServerPlayer p, E2H3Aarde.Stad stad, int stap) {
        if (rijdt(p)) {
            return;
        }
        Vec3 wagen = stad.voet("monorail_wagen");
        if (wagen == null || !H3Verloop.vrij(p)) {
            return;
        }
        Vec3 instap = wagen.add(0, 1, 0);
        double d = p.position().distanceTo(instap);
        if (d <= INSTAP) {
            start(p, stad, stap >= E2H3Aarde.STAPPEN ? "station_boven" : "station_midden");
        } else if (d <= 12 && p.tickCount % 40 < 10) {
            Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, instap.add(0, 0.6, 0), 4, 0.6, 0);
        }
    }

    /** Puts the player in a car at the outer wall that rides to the station {@code eindStation}. */
    static boolean start(ServerPlayer p, E2H3Aarde.Stad stad, String eindStation) {
        List<Vec3> punten = stad.pad("monorail_route");
        Vec3 eindPunt = stad.voet(eindStation);
        if (punten.size() < 2 || eindPunt == null) {
            return false;
        }
        List<Entity> delen = new ArrayList<>();
        Entity vloer = deel(p, "monorail_vloer", "minecraft:stripped_dark_oak_wood", -1.3f, -0.35f, -0.85f, 2.6f, 0.35f, 1.7f);
        if (vloer == null) {
            return false;
        }
        delen.add(vloer);
        float[][] palen = {{-1.25f, -0.8f}, {1.05f, -0.8f}, {-1.25f, 0.6f}, {1.05f, 0.6f}};
        for (int i = 0; i < palen.length; i++) {
            Entity paal = deel(p, "monorail_paal_" + i, "minecraft:dark_oak_log", palen[i][0], 0f, palen[i][1], 0.2f, 2.2f, 0.2f);
            if (paal != null) {
                delen.add(paal);
            }
        }
        Entity dak = deel(p, "monorail_dak", "minecraft:green_terracotta", -1.45f, 2.2f, -1.0f, 2.9f, 0.25f, 2.0f);
        if (dak != null) {
            delen.add(dak);
        }
        // the car rides to where the route comes nearest the platform of the end station
        Rit rit = new Rit(stad.level(), punten, delen, 0, "station_buiten", eindStation);
        double eind = 0, best = Double.MAX_VALUE;
        for (double a = 0; a <= rit.tot[rit.tot.length - 1]; a += 0.5) {
            double dd = rit.op(a).distanceToSqr(eindPunt.x, rit.op(a).y, eindPunt.z);
            if (dd < best) {
                best = dd;
                eind = a;
            }
        }
        Rit echt = new Rit(stad.level(), punten, delen, eind, "station_buiten", eindStation);
        echt.s = naast(echt, stad.voet("monorail_wagen"));
        Vec3 begin = echt.op(echt.s);
        for (Entity e : delen) {
            e.teleportTo(begin.x, begin.y, begin.z);
        }
        p.stopRiding();
        p.teleportTo(begin.x, begin.y + 0.05, begin.z);
        if (!p.startRiding(vloer, true, false)) {
            delen.forEach(Entity::discard);
            return false;
        }
        RITTEN.put(p.getUUID(), echt);
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h3_aarde.monorail.uitleg").withStyle(ChatFormatting.GREEN));
        E2Audio.stinger(p, "deuntje.monorail");
        return true;
    }

    /** The route distance of the point nearest to v. */
    private static double naast(Rit rit, Vec3 v) {
        double beste = 0, d = Double.MAX_VALUE;
        for (double a = 0; a <= rit.tot[rit.tot.length - 1]; a += 0.25) {
            double dd = rit.op(a).distanceToSqr(v);
            if (dd < d) {
                d = dd;
                beste = a;
            }
        }
        return beste;
    }

    @Nullable
    private static Entity deel(ServerPlayer p, String naam, String blok, float tx, float ty, float tz, float sx, float sy, float sz) {
        Entity e = EntityType.BLOCK_DISPLAY.create(p.level(), EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        CompoundTag state = new CompoundTag();
        state.putString("Name", blok);
        if (blok.endsWith("_log") || blok.endsWith("_wood")) {
            CompoundTag props = new CompoundTag();
            props.putString("axis", "y");
            state.put("Properties", props);
        }
        tag.put("block_state", state);
        CompoundTag vorm = new CompoundTag();
        vorm.put("translation", floats(tx, ty, tz));
        vorm.put("scale", floats(sx, sy, sz));
        vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
        vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", vorm);
        tag.putInt("teleport_duration", 2);
        Nbt.load(e, tag);
        e.snapTo(p.getX(), p.getY(), p.getZ(), 0f, 0f);
        return H3Acteurs.zet(p, naam, e);
    }

    private static ListTag floats(float... waarden) {
        ListTag lijst = new ListTag();
        for (float f : waarden) {
            lijst.add(FloatTag.valueOf(f));
        }
        return lijst;
    }

    /** Every tick of a rider: the push, the glide, the arrival; out of the car = back on the platform. */
    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Rit rit = RITTEN.get(p.getUUID());
        if (rit == null) {
            return;
        }
        Entity vloer = rit.delen.get(0);
        if (vloer.isRemoved() || p.getVehicle() != vloer || p.level() != rit.level) {
            stap(p, rit, rit.beginStation, false);
            return;
        }
        Input in = p.getLastClientInput();
        boolean sprong = in.jump();
        if (sprong && !rit.sprong) {
            rit.v = Math.min(MAX, rit.v + DUW);
            rit.duwen++;
            Vec3 waar = vloer.position();
            Schijn.geluid(p, SoundEvents.ROOTED_DIRT_PLACE, BlockPos.containing(waar), 1f, 0.6f + (float) rit.v);
            Schijn.deeltjes(p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD_BRICKS.defaultBlockState()), waar.add(-1.4, -0.4, 0), 12, 0.3, 0.05);
        }
        rit.sprong = sprong;
        rit.v = Math.max(0, rit.v * WRIJVING - REM);
        rit.s = Math.min(rit.eind, rit.s + rit.v);
        Vec3 nu = rit.op(rit.s);
        for (Entity e : rit.delen) {
            if (!e.isRemoved()) {
                e.teleportTo(nu.x, nu.y, nu.z);
            }
        }
        if (p.tickCount % 10 == 0) {
            int pct = (int) Math.round(100 * rit.s / Math.max(1, rit.eind));
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h3_aarde.monorail.stand", pct, snelheid(rit.v)).withStyle(ChatFormatting.GREEN));
        }
        if (rit.s >= rit.eind - 0.05) {
            stap(p, rit, rit.eindStation, true);
        }
    }

    private static String snelheid(double v) {
        int n = (int) Math.round(v / MAX * 8);
        return "■".repeat(n) + "□".repeat(8 - n);
    }

    /** Out of the car onto the platform of that station; arrived = the ride is over (the story moves on). */
    private static void stap(ServerPlayer p, Rit rit, String station, boolean aangekomen) {
        RITTEN.remove(p.getUUID());
        if (p.getVehicle() != null) {
            p.stopRiding();
        }
        H3Acteurs.wegAlle(p, "monorail");
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(rit.level);
        Vec3 perron = stad == null ? null : stad.voet(station);
        if (perron != null) {
            Duwtje.terug(p, rit.level.dimension(), perron, p.getYRot());
        }
        if (aangekomen && stad != null) {
            aangekomen(p, stad, rit.duwen);
        }
    }

    /** Arrived: in the story the wink with Sjokkel plays on the platform, then the step moves on. */
    private static void aangekomen(ServerPlayer p, E2H3Aarde.Stad stad, int duwen) {
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h3_aarde.monorail.aan", duwen).withStyle(ChatFormatting.GOLD));
        if (E2H3Aarde.stap(p) == E2H3Aarde.MONORAIL) {
            E2H3Aarde.LIJN.verder(p, E2H3Aarde.MONORAIL);
            H3Knipogen.sjokkel(p, stad);
        }
    }

    /** (leaving the city, logging out) a car that is still there goes. */
    static void uit(ServerPlayer p) {
        Rit rit = RITTEN.remove(p.getUUID());
        if (rit != null) {
            if (p.getVehicle() != null) {
                p.stopRiding();
            }
            H3Acteurs.wegAlle(p, "monorail");
        }
    }
}
