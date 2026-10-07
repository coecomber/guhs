package nl.juiced.guhs.feature.einde2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * einde2: the stand-in for a creature whose slice isn't built yet. Every entity id the slices share exists from the
 * skeleton on ({@link Einde2#plaatshouder}), so a building may already place {@code guhs:schuilguh} or
 * {@code guhs:otter_pinguinguh}: until the owner slice puts its real entity under that id, it is this one: no AI, can't be
 * hurt or pushed, never despawns, drawn as nothing.
 */
public class Einde2Plaatshouder extends PathfinderMob {
    public Einde2Plaatshouder(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
