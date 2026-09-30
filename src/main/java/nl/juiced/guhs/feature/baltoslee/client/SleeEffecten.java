package nl.juiced.guhs.feature.baltoslee.client;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.juiced.guhs.feature.balto.client.Sneeuwstorm;
import nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature;
import nl.juiced.guhs.feature.baltoslee.RitRoute;
import nl.juiced.guhs.feature.baltoslee.SleeBaan;
import nl.juiced.guhs.feature.baltoslee.SleeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeRijden;
import nl.juiced.guhs.feature.baltoslee.SneeuwsleeEntity;

/**
 * What the rider sees and hears on the sled (client): the storm (balto's {@link Sneeuwstorm}, source "tocht": dense guh-snow,
 * fog, the wind of the gusts) plus a few snow streaks right around the sled, Baltoguh's nose (glowing sparkles along the
 * middle of the track ahead: in a white-out you steer by them), the avalanche rolling down its slope as you come near, the
 * whoosh of a gust before it hits, the runners on the snow and the bells; a shake when you're buried or plof off a bridge.
 */
public final class SleeEffecten {
    /** Shake (ticks left), and how hard. */
    static int schud;
    static float schudKracht;
    /** A white-out after being buried (ticks left). */
    static int wit;
    private static boolean stormAan;
    private static final Set<Integer> LAWINE_GEHOORD = new HashSet<>();
    private static int laatsteVlaag = Integer.MIN_VALUE, laatsteBeen = -1;

    private SleeEffecten() {
    }

    /** The sled the local player rides (and drives), or null. */
    @Nullable
    public static SleeEntity eigenSlee() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.getVehicle() instanceof SleeEntity s ? s : null;
    }

    /** Every client tick of every sled. */
    static void tick(SleeEntity sled) {
        ClientLevel level = (ClientLevel) sled.level();
        RandomSource rnd = level.getRandom();
        RitRoute r = sled.route();
        if (r == null) {
            return;
        }
        boolean rijdt = sled.fase() == SleeEntity.RIJDT && sled.v > 0.03;
        if (rijdt && sled.tickCount % 22 == 0) {
            level.playLocalSound(sled.getX(), sled.getY(), sled.getZ(), BaltoSleeFeature.BELLEN.get(), SoundSource.NEUTRAL,
                    sled.eigen ? 0.35f : 0.5f, 0.95f + rnd.nextFloat() * 0.1f, false);
        }
        if (rijdt && sled.tickCount % 16 == 0) {
            level.playLocalSound(sled.getX(), sled.getY(), sled.getZ(), BaltoSleeFeature.GLIJDEN.get(), SoundSource.NEUTRAL,
                    (float) Math.min(0.8, sled.v * 1.6), 0.9f + (float) sled.v * 0.4f, false);
        }
        if (rijdt && rnd.nextFloat() < sled.v * 1.4) {                   // snow sprays from the runners
            Vec3 t = sled.richting();
            level.addParticle(ParticleTypes.SNOWFLAKE, sled.getX() - t.x * 1.1 + (rnd.nextFloat() - 0.5) * 0.8, sled.getY() + 0.1,
                    sled.getZ() - t.z * 1.1 + (rnd.nextFloat() - 0.5) * 0.8, -t.x * 0.05, 0.06, -t.z * 0.05);
        }
        if (!sled.eigen) {
            return;
        }
        SleeBaan b = r.baan(sled.been);
        double s = sled.s;
        if (sled.been != laatsteBeen) {
            laatsteBeen = sled.been;
            LAWINE_GEHOORD.clear();
            laatsteVlaag = Integer.MIN_VALUE;
        }
        // --- the storm ---
        float storm = sled.storm();
        double g = SleeRijden.windvlaag(sled.seed(), sled.been, s, b.lengte);
        Vec3 t = b.richting(s), rechts = b.rechts(s);
        Vec3 wind = rechts.scale(g * 1.2).add(t.scale(-0.35 - storm * 0.3));
        Sneeuwstorm.zet("tocht", storm, (float) wind.x, (float) wind.z, Mth.clamp(1f - storm * 0.82f, 0.08f, 1f));
        stormAan = true;
        LocalPlayer me = Minecraft.getInstance().player;
        if (me != null) {
            int n = (int) (storm * 5 + Math.abs(g) * 6);
            for (int i = 0; i < n; i++) {
                Vec3 at = me.getEyePosition().add((rnd.nextFloat() - 0.5) * 9, (rnd.nextFloat() - 0.4) * 4, (rnd.nextFloat() - 0.5) * 9);
                level.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, wind.x * 0.6 + t.x * -0.2, -0.05, wind.z * 0.6 + t.z * -0.2);
            }
        }
        // --- a gust coming: whoosh ---
        int komt = SleeRijden.windKomt(sled.seed(), sled.been, s, b.lengte);
        int cel = (int) Math.floor(s / SleeRijden.VLAAG_CEL);
        if (komt != 0 && cel != laatsteVlaag) {
            laatsteVlaag = cel;
            Vec3 bron = sled.position().add(rechts.scale(-komt * 6));
            level.playLocalSound(bron.x, bron.y + 1, bron.z, BaltoSleeFeature.WINDVLAAG.get(), SoundSource.WEATHER, 0.6f + storm * 0.5f, 1f, false);
        }
        // --- Baltoguh's nose: sparkles along the middle ahead ---
        if (sled.fase() == SleeEntity.RIJDT && sled.tickCount % 2 == 0) {
            double ahead = 4 + rnd.nextFloat() * (6 + 10 * storm);
            if (s + ahead < b.lengte) {
                Vec3 p = b.op(s + ahead, (rnd.nextFloat() - 0.5) * 0.3);
                level.addParticle(BaltoSleeFeature.SNUFFEL.get(), p.x, p.y + 0.12, p.z, 0, 0.01, 0);
            }
        }
        // --- avalanches: the snow comes down as you come near ---
        for (RitRoute.Zone z : r.zones(sled.been, RitRoute.Soort.LAWINE)) {
            double tot = z.midden() - s;
            if (tot > 20 || tot < -6) {
                continue;
            }
            int key = (int) (z.midden() * 10);
            if (tot < 18 && LAWINE_GEHOORD.add(key)) {
                Vec3 bron = b.op(z.midden(), z.kant() * 10);
                level.playLocalSound(bron.x, bron.y + 3, bron.z, BaltoSleeFeature.LAWINE.get(), SoundSource.WEATHER, 1.5f, 1f, false);
                schud = Math.max(schud, 8);
                schudKracht = 0.6f;
            }
            double w = b.breedte(z.midden());
            // the wave: where it is now (from far up the slope to over the track), a white rolling cloud
            double f = Mth.clamp(1 - tot / 18, 0, 1.2);
            double golfLat = z.kant() * (w + 12 - f * (12 + w * 0.6));
            for (int i = 0; i < 6; i++) {
                double ss = z.midden() + (rnd.nextFloat() - 0.5) * (z.s1() - z.s0() + 4);
                Vec3 p = b.op(Mth.clamp(ss, 0, b.lengte), golfLat + (rnd.nextFloat() - 0.5) * 3);
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y + 0.4 + rnd.nextFloat() * 1.8, p.z, -z.kant() * rechts.x * 0.15, 0.02,
                        -z.kant() * rechts.z * 0.15);
                level.addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.5 + rnd.nextFloat() * 2.5, p.z, -z.kant() * rechts.x * 0.3, -0.02,
                        -z.kant() * rechts.z * 0.3);
            }
        }
        // --- the ice bridge sparkles ---
        if (r.zone(sled.been, RitRoute.Soort.IJSBRUG, s) != null && rnd.nextFloat() < 0.4f) {
            Vec3 p = b.op(s + rnd.nextFloat() * 4, (rnd.nextFloat() - 0.5) * 2 * b.breedte(s));
            level.addParticle(ParticleTypes.END_ROD, p.x, p.y + 0.05, p.z, 0, 0.01, 0);
        }
    }

    /** Entity events of a sled (server: buried, dodged, plof, a rest, around the berghut). */
    static void event(SleeEntity sled) {
        ClientLevel level = (ClientLevel) sled.level();
        switch (sled.laatsteEvent) {
            case SleeEntity.EV_BEDOLVEN -> {
                if (sled.eigen || sled == eigenSlee()) {
                    schud = 18;
                    schudKracht = 2.2f;
                    wit = 40;
                }
                for (int i = 0; i < 40; i++) {
                    level.addParticle(ParticleTypes.SNOWFLAKE, sled.getX() + (level.getRandom().nextFloat() - 0.5) * 3, sled.getY() + level.getRandom().nextFloat() * 2,
                            sled.getZ() + (level.getRandom().nextFloat() - 0.5) * 3, 0, 0.1, 0);
                }
            }
            case SleeEntity.EV_PLOF -> {
                if (sled == eigenSlee()) {
                    schud = 10;
                    schudKracht = 1.4f;
                    wit = 20;
                }
            }
            case SleeEntity.EV_ONTWEKEN -> {
                if (sled == eigenSlee()) {
                    schud = Math.max(schud, 10);
                    schudKracht = 0.9f;
                }
            }
            case SleeEntity.EV_RUST -> {
                for (int i = 0; i < 8; i++) {
                    level.addParticle(ParticleTypes.HEART, sled.getX() + (level.getRandom().nextFloat() - 0.5) * 3, sled.getY() + 1, sled.getZ()
                            + (level.getRandom().nextFloat() - 0.5) * 3, 0, 0.05, 0);
                }
            }
            default -> {
            }
        }
    }

    /** Every client tick: the storm stops when you're off the sled; the shake fades. */
    static void clientTick() {
        if (schud > 0) {
            schud--;
        }
        if (wit > 0) {
            wit--;
        }
        if (stormAan && eigenSlee() == null) {
            stormAan = false;
            Sneeuwstorm.uit("tocht");
            laatsteBeen = -1;
        }
    }

    static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (schud > 0) {
            float t = (float) (schud + event.getPartialTick());
            float k = schudKracht * Math.min(1, schud / 6f);
            event.setRoll(event.getRoll() + Mth.sin(t * 2.3f) * k);
            event.setPitch(event.getPitch() + Mth.sin(t * 3.1f) * k * 0.6f);
        }
    }

    /** Your own sneeuwslee: the bells and the runners (every client tick of every sneeuwslee). */
    static void eigenTick(SneeuwsleeEntity sled) {
        ClientLevel level = (ClientLevel) sled.level();
        float v = sled.getoondeSnelheid;
        if (v > 0.04f && sled.tickCount % 24 == 0) {
            level.playLocalSound(sled.getX(), sled.getY(), sled.getZ(), BaltoSleeFeature.BELLEN.get(), SoundSource.NEUTRAL, 0.35f, 1f, false);
        }
        if (v > 0.04f && sled.tickCount % 16 == 0 && sled.opSneeuwGezien()) {
            level.playLocalSound(sled.getX(), sled.getY(), sled.getZ(), BaltoSleeFeature.GLIJDEN.get(), SoundSource.NEUTRAL, Math.min(0.7f, v * 1.5f),
                    0.9f + v * 0.4f, false);
        }
        if (v > 0.1f && level.getRandom().nextFloat() < v) {
            Vec3 f = Vec3.directionFromRotation(0, sled.getYRot());
            level.addParticle(ParticleTypes.SNOWFLAKE, sled.getX() - f.x * 1.1, sled.getY() + 0.1, sled.getZ() - f.z * 1.1, -f.x * 0.05, 0.06, -f.z * 0.05);
        }
    }
}
