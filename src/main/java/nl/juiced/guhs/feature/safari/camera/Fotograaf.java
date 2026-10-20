package nl.juiced.guhs.feature.safari.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * The server side of a photo (S1, design 1.7): which safari animal is in the picture ({@link #kijk}), how many stars, and
 * what that pays.
 * <ul>
 *   <li>The picture is a cone around where the player looks: narrow when zoomed in (the camera held a moment), wide for a
 *   quick snap. An animal counts when one of three points of it (middle, top, bottom) can be seen: glass, panes,
 *   one-way glass, water, plants and other see-through blocks don't block the view, leaves only count against it.</li>
 *   <li>The animal closest to the middle is the subject; the others in the picture give the extra point (family, a second
 *   species) and the photo missions (two species, a family of three).</li>
 *   <li>Pays once per species per star: 10 for the first star, 20 for the second, 40 for the third (design 1.8: 70 per
 *   species); twice that for the birthday species of the day ({@link #JARIG}, set by the moments slice) and for the first
 *   photo after a night in the lodge (flag {@code vroege_vogel}, set by the lobby slice). The day's missions pay
 *   {@link Missies#MUNTEN} once each.</li>
 * </ul>
 */
public final class Fotograaf {
    /** How far a picture reaches (blocks): zoomed in and a quick snap. */
    static final double VER_ZOOM = 64, VER_SNEL = 24;
    /** Half the opening angle of the picture (radians): zoomed in (the spyglass view is about 7 degrees high) and a snap. */
    static final double HALF_ZOOM = 0.075, HALF_SNEL = 0.55;
    static final int[] STER_MUNTEN = {0, 10, 20, 40};
    /** The birthday species of the day (the moments slice sets this): photos of it pay double. */
    public static volatile Predicate<String> JARIG = s -> false;
    /** Set by the lobby slice when a player wakes up in the lodge: the first photo after that pays double. */
    public static final String VROEGE_VOGEL = "vroege_vogel";

    /** One click: look, score, keep, pay and tell the client. Returns the photo (null: no animal in the picture). */
    @Nullable
    public static Foto klik(ServerPlayer p, boolean zoom) {
        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(), SoundEvents.SPYGLASS_STOP_USING, SoundSource.PLAYERS, 0.8f, 1.8f);
        Foto f = kijk(level, p.getEyePosition(), p.getLookAngle(), zoom);
        if (f == null) {
            ModNetworking.sendTo(p, new CameraPayloads.Uitslag(new CompoundTag(), 0, 0, 0));
            return null;
        }
        long dag = Klok.dag();
        CompoundTag tag = f.tag(dag, p.getGameProfile().name());
        int was = SafariSpeler.registreerFoto(p, f.soort(), f.sterren(), tag);
        boolean vroeg = SafariSpeler.vlag(p, VROEGE_VOGEL);
        if (vroeg) {
            SafariSpeler.zetVlag(p, VROEGE_VOGEL, false);
        }
        int keer = JARIG.test(f.soort()) || vroeg ? 2 : 1;
        int munten = 0;
        for (int k = was + 1; k <= f.sterren(); k++) {
            int n = STER_MUNTEN[k] * keer;
            if (SafariMunten.verdienEens(level.getServer(), p.getUUID(), "foto." + f.soort() + "." + k, n, "foto")) {
                munten += n;
            }
        }
        if (vroeg && munten == 0) {
            munten = 10;   // (the first photo of the morning pays a little even when the species had all its stars)
            SafariMunten.geef(p, munten, "vroege vogel");
        }
        int missies = 0;
        List<Missies.Missie> vandaag = Missies.van(dag);
        for (int i = 0; i < vandaag.size(); i++) {
            if (vandaag.get(i).klopt(f) && SafariSpeler.telDag(level.getServer(), p.getUUID(), "missie" + i, 1, 1) > 0) {
                missies++;
            }
        }
        if (missies > 0) {
            SafariMunten.geef(p, missies * Missies.MUNTEN, "missie");
        }
        ModNetworking.sendTo(p, new CameraPayloads.Uitslag(tag, was, munten, missies));
        return f;
    }

    /** What a camera at {@code oog} looking along {@code richting} sees (null: no safari animal in the picture). */
    @Nullable
    public static Foto kijk(ServerLevel level, Vec3 oog, Vec3 richting, boolean zoom) {
        double ver = zoom ? VER_ZOOM : VER_SNEL, half = zoom ? HALF_ZOOM : HALF_SNEL;
        Vec3 kijk = richting.normalize();
        AABB doos = new AABB(oog, oog).inflate(ver);
        List<SafariDier> inBeeld = new ArrayList<>();
        SafariDier beste = null;
        double besteHoek = Double.MAX_VALUE;
        int[] besteZicht = null;
        for (SafariDier d : level.getEntitiesOfClass(SafariDier.class, doos, d -> !d.soort().isEmpty() && d.isAlive())) {
            Vec3 m = d.getBoundingBox().getCenter();
            double afstand = m.distanceTo(oog);
            if (afstand > ver || afstand < 0.5) {
                continue;
            }
            double hoek = Math.acos(Math.max(-1, Math.min(1, m.subtract(oog).normalize().dot(kijk))));
            // (the animal's own size widens what still counts as "in the picture")
            double rand = half * 1.35 + Math.atan(Math.max(d.getBbWidth(), d.getBbHeight()) / 2 / afstand);
            if (hoek > rand) {
                continue;
            }
            int[] zicht = zicht(level, oog, d);
            if (zicht[0] == 0) {
                continue;
            }
            inBeeld.add(d);
            if (hoek < besteHoek) {
                besteHoek = hoek;
                beste = d;
                besteZicht = zicht;
            }
        }
        if (beste == null) {
            return null;
        }
        final SafariDier onderwerp = beste;
        Vec3 m = onderwerp.getBoundingBox().getCenter();
        double afstand = m.distanceTo(oog);
        double grootte = Math.atan(Math.max(onderwerp.getBbWidth(), onderwerp.getBbHeight()) / afstand) / (2 * half);
        int afstandPunt = grootte >= 0.35 ? 2 : grootte >= 0.15 ? 1 : 0;
        if (besteZicht[0] < 3 || besteZicht[1] >= 2) {
            afstandPunt = Math.max(0, afstandPunt - 1);   // half hidden or behind leaves
        }
        // in the middle and looking at the camera
        Vec3 blik = Vec3.directionFromRotation(0, onderwerp.yBodyRot);
        Vec3 naarCamera = oog.subtract(m).multiply(1, 0, 1);
        boolean kijktJe = naarCamera.lengthSqr() > 1e-6 && blik.dot(naarCamera.normalize()) > 0.25;
        int middenPunt = besteHoek <= half * 0.4 && kijktJe ? 1 : 0;
        String actie = onderwerp.actie();
        boolean bijzonder = onderwerp.slaapt() || onderwerp.hangt() || (actie != null && !actie.isEmpty() && !actie.equals("idle") && !actie.equals("walk"));
        int zelfde = (int) inBeeld.stream().filter(d -> d.soort().equals(onderwerp.soort())).count();
        int soorten = (int) inBeeld.stream().map(SafariDier::soort).distinct().count();
        boolean extra = onderwerp.isBaby() || zelfde >= 2 || soorten >= 2;
        ParkData.Soort info = ParkData.get().soort(onderwerp.soort());
        String zone = info == null || info.zone() == null ? "" : info.zone();
        boolean water = onderwerp.isInWater();
        return new Foto(onderwerp.soort(), onderwerp.variant(), zone, onderwerp.slaapt() ? "slaap" : actie, onderwerp.slaapt(), onderwerp.isBaby(),
                water, zelfde, soorten, afstandPunt, middenPunt, bijzonder ? 1 : 0, extra ? 1 : 0);
    }

    /**
     * How well the animal can be seen: {visible points 0..3, leaf blocks in the way}. Three points (middle, top, bottom
     * quarter) are traced from the eye in small steps; a solid, non see-through block whose shape holds the step blocks
     * that point.
     */
    static int[] zicht(ServerLevel level, Vec3 oog, SafariDier d) {
        AABB b = d.getBoundingBox();
        Vec3[] punten = {b.getCenter(), new Vec3(b.getCenter().x, b.maxY - b.getYsize() * 0.15, b.getCenter().z),
                new Vec3(b.getCenter().x, b.minY + b.getYsize() * 0.25, b.getCenter().z)};
        int zichtbaar = 0, blad = 0;
        for (Vec3 doel : punten) {
            int[] r = straal(level, oog, doel, b);
            if (r[0] == 1) {
                zichtbaar++;
            }
            blad = Math.max(blad, r[1]);
        }
        return new int[] {zichtbaar, blad};
    }

    /** {1 = free, leaf blocks passed}. */
    private static int[] straal(ServerLevel level, Vec3 van, Vec3 naar, AABB dier) {
        Vec3 stap = naar.subtract(van);
        double lengte = stap.length();
        int n = (int) Math.ceil(lengte / 0.2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long vorige = Long.MIN_VALUE;
        int blad = 0;
        for (int i = 1; i < n; i++) {
            Vec3 p = van.add(stap.scale(i / (double) n));
            if (dier.contains(p)) {
                break;
            }
            pos.set(p.x, p.y, p.z);
            long sleutel = pos.asLong();
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) {
                continue;
            }
            if (s.is(BlockTags.LEAVES)) {
                if (sleutel != vorige) {
                    blad++;
                }
                vorige = sleutel;
                continue;
            }
            vorige = sleutel;
            if (doorzichtig(s)) {
                continue;
            }
            VoxelShape vorm = s.getShape(level, pos);
            if (!vorm.isEmpty() && vorm.bounds().move(pos).contains(p) && raakt(vorm, pos, p)) {
                return new int[] {0, blad};
            }
        }
        return new int[] {1, blad};
    }

    private static boolean raakt(VoxelShape vorm, BlockPos pos, Vec3 p) {
        double x = p.x - pos.getX(), y = p.y - pos.getY(), z = p.z - pos.getZ();
        for (AABB a : vorm.toAabbs()) {
            if (a.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** Blocks you can look through for a photo: glass (also the one-way glass of the hides), panes, bars, fluids, plants. */
    static boolean doorzichtig(BlockState s) {
        if (!s.getFluidState().isEmpty() && s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty()) {
            return true;
        }
        if (s.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || s.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)
                || s.is(net.minecraft.world.level.block.Blocks.IRON_BARS)) {
            return true;
        }
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (id.contains("glass") || id.equals("eenrichtingsglas")) {
            return true;
        }
        // plants, flowers, carpets of moss, snow layers, torches: no occlusion and nothing to bump into
        return !s.canOcclude() && s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    private Fotograaf() {
    }
}
