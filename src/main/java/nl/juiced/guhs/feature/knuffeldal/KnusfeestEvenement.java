package nl.juiced.guhs.feature.knuffeldal;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.evenementen.Evenement;
import nl.juiced.guhs.feature.evenementen.EvenementType;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.Seizoen;

/**
 * The Knusfeest as a guh event (EvenementType KNUSFEEST, 2.8): the feast at the feestbuffet. The tables are laid, the
 * bell rings, every participant's tamed guhs come to the buffet (walking, or with a poof from far away) and eat and
 * dance there; you can put your own baked, grown and poured things on the tables. When the time is up the tables are
 * cleared and the player whose round it was gets the rewards (the finale of the Grote Knusfeest, or the seasonal feast).
 * No fireworks, no parade: just a cosy feast.
 */
public class KnusfeestEvenement extends Evenement {
    /** A feast lasts 75 seconds. */
    public static final int DUUR = 20 * 75;
    /** Tamed guhs further than this walk too long: they come with a poof. */
    public static final double LOPEN = 40;
    /** Guhs this far from the middle come to the feast. */
    public static final double BEREIK = 128;

    @Nullable
    private final UUID vierder;
    private final List<BlockPos> tafels;

    KnusfeestEvenement(ServerLevel level, Vec3 center, @Nullable UUID vierder, List<BlockPos> tafels) {
        super(EvenementType.KNUSFEEST, level, center, DUUR);
        this.vierder = vierder;
        this.tafels = tafels;
    }

    /** A feast around this player: at the nearest feestbuffet (else right here). */
    public static KnusfeestEvenement maak(ServerLevel level, ServerPlayer anchor) {
        List<BlockPos> tafels = Feestbuffet.tafels(level, anchor.blockPosition(), Feestbuffet.RADIUS);
        Vec3 center = anchor.position();
        if (!tafels.isEmpty()) {
            double x = 0, y = 0, z = 0;
            for (BlockPos p : tafels) {
                x += p.getX() + 0.5;
                y += p.getY() + 1;
                z += p.getZ() + 0.5;
            }
            center = new Vec3(x / tafels.size(), y / tafels.size(), z / tafels.size());
        }
        boolean zijnRonde = Knusfeest.rondeBezig(anchor) && Knusfeest.alleGebracht(anchor);
        return new KnusfeestEvenement(level, center, zijnRonde ? anchor.getUUID() : null, tafels);
    }

    /** The tables this feast laid. */
    public List<BlockPos> tafels() {
        return List.copyOf(tafels);
    }

    @Override
    protected void begin() {
        Feestbuffet.dek(level, tafels, true);
        level.playSound(null, center.x, center.y, center.z, KnuffeldalFeature.FEESTBEL.get(), SoundSource.AMBIENT, 1.5f, 1f);
        say(vierder != null && level.getPlayerByUUID(vierder) instanceof ServerPlayer p && Knusfeest.ronde(p) == 0
                ? "gui.guhs.knuffeldal.feest.groot" : "gui.guhs.knuffeldal.feest.seizoen");
    }

    @Override
    protected void tickEvent() {
        if (age % 20 == 0) {
            haalGuhs();
        }
        if (age % 8 == 0) {
            ParticleOptions seizoen = switch (Seizoen.huidig(level)) {
                case LENTE -> KnuffeldalFeature.BLOESEMBLAADJE.get();
                case WINTER -> KnuffeldalFeature.SNEEUWVLOKJE.get();
                default -> KnuffeldalFeature.PLUISJE.get();
            };
            level.sendParticles(seizoen, center.x, center.y + 4, center.z, 6, 6, 2, 6, 0.01);
            level.sendParticles(ParticleTypes.NOTE, center.x, center.y + 1.5, center.z, 1, 3, 0.5, 3, 1);
        }
        if (age % 200 == 100) {
            level.playSound(null, center.x, center.y, center.z, KnuffeldalFeature.FEESTBEL.get(), SoundSource.AMBIENT, 0.8f, 1.2f);
        }
    }

    /** Every participant's tamed guhs come to the buffet: walking, or (far away) with a poof; there they eat and dance. */
    private void haalGuhs() {
        for (ServerPlayer player : players()) {
            for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(BlockPos.containing(center)).inflate(BEREIK),
                    g -> g.isOwnedBy(player) && g.isAlive() && !g.isPassenger() && !g.isVehicle())) {
                Vec3 spot = plekBijTafel(guh);
                double d = guh.position().distanceTo(spot);
                GuhHooks.bezig(guh, 60);
                if (d > LOPEN) {
                    level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.5, guh.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                    guh.teleportTo(spot.x, spot.y, spot.z);
                    level.sendParticles(ParticleTypes.POOF, spot.x, spot.y + 0.5, spot.z, 10, 0.3, 0.3, 0.3, 0.02);
                } else if (d > 2.5) {
                    if (!guh.isOrderedToSit()) {
                        guh.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.1);
                    }
                } else if (guh.emotes.current() == null) {
                    guh.emotes.start(level.random.nextInt(3) == 0 ? Emote.DANSEN : Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
                    level.broadcastEntityEvent(guh, (byte) 7);
                }
            }
        }
    }

    /** A spot next to one of the tables (by the guh's id, so they spread out), or the middle. */
    private Vec3 plekBijTafel(GuhEntity guh) {
        if (tafels.isEmpty()) {
            return center;
        }
        BlockPos t = tafels.get(Math.floorMod(guh.getId(), tafels.size()));
        int side = Math.floorMod(guh.getId() / 7, 4);
        int dx = side == 0 ? 1 : side == 1 ? -1 : 0, dz = side == 2 ? 1 : side == 3 ? -1 : 0;
        return new Vec3(t.getX() + 0.5 + dx * 1.2, t.getY(), t.getZ() + 0.5 + dz * 1.2);
    }

    @Override
    protected void finish(boolean completed) {
        Feestbuffet.dek(level, tafels, false);
        if (!completed) {
            return;
        }
        say("gui.guhs.knuffeldal.feest.einde");
        if (vierder != null && level.getPlayerByUUID(vierder) instanceof ServerPlayer p) {
            Feestbuffet.gevierd(p, true);
        }
    }
}
