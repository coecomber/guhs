package nl.juiced.guhs.feature.speelgoed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A guh (or a muisje) runs through the pluizige tunnel: walks to an entrance, scurries through (inside the fur, no
 * physics), hides in the middle for a while (verstoppertje!) and pops out at another entrance. While a guh hides, a tame
 * muisje of the same owner nearby comes looking for it, and a player can knock on the tunnel ({@link #klop}): found!
 */
class TunnelSpel extends SpeelTaak {
    /** The guhs hiding in a tunnel right now (entity id -> its session), for knocking. */
    private static final Map<Integer, TunnelSpel> VERSTOPT = new ConcurrentHashMap<>();

    enum Fase { LOPEN, BINNEN, VERSTOPT, KLAAR }

    private final TunnelBlock.Ingang in, uit;
    private final List<Vec3> punten = new ArrayList<>();
    private final Set<BlockPos> netwerk;
    private final int verstopBij;
    private int punt, verstopTicks;
    private Fase fase = Fase.LOPEN;
    private boolean gevonden, binnenGeweest;

    TunnelSpel(Mob mob, ServerLevel level, TunnelBlock.Ingang in, TunnelBlock.Ingang uit, List<BlockPos> route, Set<BlockPos> netwerk) {
        super(mob, level);
        this.in = in;
        this.uit = uit;
        this.netwerk = netwerk;
        punten.add(in.buiten());
        for (BlockPos p : route) {
            punten.add(Vec3.atBottomCenterOf(p).add(0, 0.05, 0));
        }
        punten.add(uit.buiten());
        this.verstopBij = Math.max(1, punten.size() / 2);
        this.verstopTicks = mob instanceof GuhEntity ? 60 + level.random.nextInt(100) : 20 + level.random.nextInt(30);
    }

    Fase fase() {
        return fase;
    }

    @Override
    public int maxTicks() {
        return 900;
    }

    @Override
    protected boolean speel() {
        switch (fase) {
            case LOPEN -> {
                if (loopNaar(punten.get(0), 1.15, 0.6)) {
                    fase = Fase.BINNEN;
                    binnenGeweest = true;
                    mob.noPhysics = true;
                    mob.setNoGravity(true);
                    punt = 1;
                    level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.WOOL_STEP, SoundSource.NEUTRAL, 0.8f, 1.3f);
                } else if (vast()) {
                    return false;
                }
                return true;
            }
            case BINNEN -> {
                if (punt >= punten.size()) {
                    return klaar();
                }
                Vec3 doel = punten.get(punt);
                Vec3 nu = mob.position();
                Vec3 r = doel.subtract(nu);
                double stap = mob instanceof GuhEntity ? 0.16 : 0.2;
                mob.getNavigation().stop();
                mob.setDeltaMovement(Vec3.ZERO);
                if (r.length() <= stap) {
                    mob.setPos(doel.x, doel.y, doel.z);
                    if (punt == verstopBij && !gevonden && verstopTicks > 0) {
                        fase = Fase.VERSTOPT;
                        VERSTOPT.put(mob.getId(), this);
                        level.playSound(null, mob.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.5f, 1.5f);
                    }
                    punt++;
                } else {
                    Vec3 s = r.normalize().scale(stap);
                    mob.setPos(nu.x + s.x, nu.y + s.y, nu.z + s.z);
                    mob.setYRot((float) Math.toDegrees(Math.atan2(-s.x, s.z)));
                    mob.setYBodyRot(mob.getYRot());
                    mob.setYHeadRot(mob.getYRot());
                }
                if (ticks % 5 == 0) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, mob.getX(), mob.getY() + 0.8, mob.getZ(), 2, 0.2, 0.05, 0.2, 0);
                }
                return true;
            }
            case VERSTOPT -> {
                mob.setDeltaMovement(Vec3.ZERO);
                if (--verstopTicks <= 0) {
                    VERSTOPT.remove(mob.getId());
                    fase = Fase.BINNEN;
                    return true;
                }
                if (ticks % 30 == 0) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, mob.getX(), mob.getY() + 1.0, mob.getZ(), 3, 0.2, 0.05, 0.2, 0);
                    if (level.random.nextInt(3) == 0) {   // a giggle from inside the tunnel
                        level.playSound(null, mob.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.35f, 1.7f);
                    }
                }
                if (mob instanceof GuhEntity g && ticks % 10 == 0) {
                    zoekMuisje(g);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** A tame muisje of the same owner nearby comes looking; next to the tunnel it finds the guh. */
    private void zoekMuisje(GuhEntity g) {
        if (g.getOwnerUUID() == null) {
            return;
        }
        List<PieppiepmuisjeEntity> muisjes = level.getEntitiesOfClass(PieppiepmuisjeEntity.class, g.getBoundingBox().inflate(12),
                m -> m.isTame() && g.getOwnerUUID().equals(m.getOwnerUUID()) && !m.isOrderedToSit() && !m.isBezig() && !m.isVerstopt());
        if (muisjes.isEmpty()) {
            return;
        }
        PieppiepmuisjeEntity m = muisjes.get(0);
        Vec3 bij = in.buiten().distanceToSqr(m.position()) < uit.buiten().distanceToSqr(m.position()) ? in.buiten() : uit.buiten();
        if (m.distanceToSqr(bij) < 2.5 || m.distanceToSqr(g) < 2.5) {
            gevonden(m);
        } else {
            m.getNavigation().moveTo(bij.x, bij.y, bij.z, 1.2);
        }
    }

    /** Found! A giggle, hearts, and out it comes. */
    void gevonden(Entity vinder) {
        if (gevonden) {
            return;
        }
        gevonden = true;
        VERSTOPT.remove(mob.getId());
        fase = Fase.BINNEN;
        level.playSound(null, mob.blockPosition(), SpeelgoedFeature.GEVONDEN.get(), SoundSource.NEUTRAL, 1f, 1f);
        level.sendParticles(BandFeature.HARTJE.get(), mob.getX(), mob.getY() + 1.1, mob.getZ(), 6, 0.4, 0.2, 0.4, 0);
        if (vinder instanceof PieppiepmuisjeEntity m) {
            level.sendParticles(BandFeature.HARTJE.get(), m.getX(), m.getY() + 0.6, m.getZ(), 3, 0.2, 0.1, 0.2, 0);
        }
        if (vinder instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable("gui.guhs.speelgoed.tunnel.gevonden", mob.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            if (p.getUUID().equals(Band.eigenaar(mob))) {
                Band.geefHartjes(mob, p, 2, Reden.SPEELGOED);
            }
            nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "lieve_vadsjes/speelgoed_verstop");
        } else if (vinder instanceof PieppiepmuisjeEntity) {
            ServerPlayer baas = Band.eigenaarOnline(mob);
            if (baas != null && baas.distanceToSqr(mob) < 32 * 32) {
                baas.displayClientMessage(Component.translatable("gui.guhs.speelgoed.tunnel.muisje", vinder.getName(), mob.getName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }

    boolean isGevonden() {
        return gevonden;
    }

    private boolean klaar() {
        fase = Fase.KLAAR;
        uitDeTunnel();
        Spelen.gespeeld(mob, "tunnel", null);
        return false;
    }

    private void uitDeTunnel() {
        VERSTOPT.remove(mob.getId());
        if (binnenGeweest) {
            mob.noPhysics = false;
            mob.setNoGravity(false);
            if (fase != Fase.KLAAR) {   // (stopped halfway: out of the nearest hole, not stuck in the fur)
                Vec3 a = in.buiten(), b = uit.buiten();
                Vec3 p = mob.distanceToSqr(a) < mob.distanceToSqr(b) ? a : b;
                mob.teleportTo(p.x, p.y, p.z);
            }
            binnenGeweest = false;
        }
    }

    @Override
    public void stop() {
        super.stop();
        uitDeTunnel();
    }

    /** A player knocks on a tunnel piece: every guh hiding in that tunnel is found. */
    static boolean klop(ServerLevel level, BlockPos pos, ServerPlayer player) {
        boolean raak = false;
        for (TunnelSpel s : List.copyOf(VERSTOPT.values())) {
            if (s.level == level && s.fase == Fase.VERSTOPT && (s.netwerk.contains(pos) || s.mob.blockPosition().closerThan(pos, 3))) {
                s.gevonden(player);
                raak = true;
            }
        }
        return raak;
    }

    /** (tests) the session of a guh hiding in a tunnel right now, or null. */
    @Nullable
    static TunnelSpel verstopt(Mob mob) {
        return VERSTOPT.get(mob.getId());
    }

    static void wis() {
        VERSTOPT.clear();
    }
}
