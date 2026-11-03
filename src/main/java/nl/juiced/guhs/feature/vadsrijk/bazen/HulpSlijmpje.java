package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The mini-slijmpje of the Koninklijke Klodderknots: a pink little slime that hops onto its owner's target for 8 s and deals
 * damage in the owner's name (skill id {@code bazen_hulpslijmpje}). It can't be hurt and doesn't save (it's gone after a reload).
 */
public class HulpSlijmpje extends PathfinderMob {
    public static final int LEVEN = 160;
    @Nullable
    private UUID eigenaar, doel;
    private double schade = 4;

    public HulpSlijmpje(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributen() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.34);
    }

    public void stel(ServerPlayer eigenaar, VadsrijkMob doel, double schade) {
        this.eigenaar = eigenaar.getUUID();
        this.doel = doel.getUUID();
        this.schade = schade;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        ServerPlayer p = eigenaar == null ? null : sl.getEntity(eigenaar) instanceof ServerPlayer sp ? sp : null;
        VadsrijkMob m = doel == null ? null : sl.getEntity(doel) instanceof VadsrijkMob vm && !vm.geplofd() ? vm : null;
        if (m == null && p != null) {
            m = sl.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(8), x -> !x.geplofd()).stream().findFirst().orElse(null);
            doel = m == null ? null : m.getUUID();
        }
        if (tickCount > LEVEN || p == null || m == null) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.3, getZ(), 12, 0.2, 0.2, 0.2, 0.02);
            discard();
            return;
        }
        getNavigation().moveTo(m, 1.3);
        if (onGround() && tickCount % 10 == 0) {
            Vec3 d = m.position().subtract(position()).multiply(1, 0, 1);
            d = d.lengthSqr() > 0.01 ? d.normalize().scale(0.3) : Vec3.ZERO;
            setDeltaMovement(d.x, 0.42, d.z);
        }
        if (tickCount % 15 == 0 && distanceTo(m) < 2.2) {
            Schade.doe(new Schade.Klap(p, m, schade, Element.ROZE, Schade.Soort.MELEE, false, "bazen_hulpslijmpje"));
            sl.sendParticles(ParticleTypes.ITEM_SLIME, m.getX(), m.getY() + 0.6, m.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.6f, 1.8f);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
    }
}
