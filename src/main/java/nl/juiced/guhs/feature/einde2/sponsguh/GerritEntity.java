package nl.juiced.guhs.feature.einde2.sponsguh;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice sponsguh_wezens: slak Gerrit, the snail of Sponsguh, who says "njeg" where a cat would say miauw; and (with
 * an owner, see {@link Gerrit}) a player's little pet: it follows slowly, leaves a short trail of slime, says "njeg" when
 * petted and can be told to stay (sneak-click).
 */
public class GerritEntity extends EigenWezen {
    public GerritEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String soortNaam() {
        return "gerrit";
    }

    @Override
    protected double volgSnelheid() {
        return 1.0;                                                          // (a snail: it takes its time)
    }

    @Nullable
    @Override
    protected SoundEvent geluid() {
        return E2SponsguhFeature.GERRIT_NJEG.get();
    }

    @Override
    protected float stem() {
        return 1.25f + random.nextFloat() * 0.2f;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return heeftEigenaar() ? E2SponsguhFeature.GERRIT_NJEG.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 900;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    /** Says "njeg": the sound, the little stretch of its eye stalks, and the word for this player. */
    public void njeg(@Nullable ServerPlayer voor) {
        zetAnimatie("njeg");
        playSound(E2SponsguhFeature.GERRIT_NJEG.get(), 0.9f, stem());
        if (voor != null) {
            GuhQuests.say(voor, this, "gui.guhs.einde2_sponsguh_wezens.gerrit.njeg");
        }
    }

    @Override
    protected void praatje(ServerPlayer player) {
        getLookControl().setLookAt(player);
        njeg(player);
    }

    @Override
    protected void geaaid(ServerPlayer player) {
        super.geaaid(player);
        njeg(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && onGround() && tickCount % 8 == 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0e-4) {
            level.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.05, getZ(), 1, 0.05, 0, 0.05, 0);   // (the trail)
        }
    }
}
