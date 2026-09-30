package nl.juiced.guhs.feature.vogels;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The pluisvinkje: a round pink-white finch that lives in little flocks (the one with the lowest id near it leads: the
 * others take off with it, fly in a loose cloud around it and land near it). Now and then it drops a
 * {@link VogelsFeature#PLUISVEERTJE}: by itself every 5 to 10 minutes, when you give it seeds (at most every 2 minutes per
 * vinkje), and sometimes while it eats at a {@link VoerhuisjeBlock}.
 */
public class PluisvinkjeEntity extends Vogeltje {
    /** How far a flock holds together. */
    public static final double ZWERM = 16.0;
    /** Ticks between feathers it drops by itself (a random part added). */
    public static final int VEERTJE_TIJD = 6000;
    /** Ticks after a feather for seeds before the next one. */
    public static final int VOER_VEERTJE_TIJD = 2400;

    private int veertjeTijd = VEERTJE_TIJD + 3000;
    private int voerVeertje;
    @Nullable
    private PluisvinkjeEntity leiderCache;
    private int leiderTick;

    public PluisvinkjeEntity(EntityType<? extends Vogeltje> type, Level level) {
        super(type, level);
        veertjeTijd = VEERTJE_TIJD + random.nextInt(VEERTJE_TIJD);
    }

    @Override
    public String naam() {
        return "pluisvinkje";
    }

    @Override
    protected double vliegSnelheid() {
        return 0.32;
    }

    @Override
    public boolean lekker(ItemStack stack) {
        return stack.is(VogelTags.ZAADJES);
    }

    @Override
    protected SoundEvent roep() {
        return VogelsFeature.TJIEP.get();
    }

    @Override
    protected String[] extraActies() {
        return new String[]{"blij"};
    }

    // --- the flock -------------------------------------------------------------------------------------------------------
    /** The leader of its flock: the pluisvinkje with the lowest id within {@link #ZWERM} blocks (maybe itself). */
    public PluisvinkjeEntity leider() {
        if (leiderCache != null && leiderCache.isAlive() && tickCount - leiderTick < 20 && leiderCache.distanceTo(this) < ZWERM) {
            return leiderCache;
        }
        leiderTick = tickCount;
        PluisvinkjeEntity best = this;
        for (PluisvinkjeEntity v : level().getEntitiesOfClass(PluisvinkjeEntity.class, getBoundingBox().inflate(ZWERM), e -> e.isAlive())) {
            if (v.getId() < best.getId()) {
                best = v;
            }
        }
        leiderCache = best;
        return best;
    }

    /** Its own place in the flock's little cloud. */
    private Vec3 plekInZwerm() {
        double a = (getId() * 2.399963) % (Math.PI * 2);
        double r = 1.2 + (getId() % 3) * 0.7;
        return new Vec3(Math.cos(a) * r, 0.6 * Math.sin(getId() + tickCount * 0.05), Math.sin(a) * r);
    }

    @Override
    protected boolean vliegStap() {
        PluisvinkjeEntity l = leider();
        if (l != this && landplek == null) {
            if (l.vliegt()) {
                if (l.thuis() != null) {
                    zetThuis(l.thuis());
                }
                stuur(l.position().add(plekInZwerm()), vliegSnelheid() * 1.15);
                vliegTijd = Math.max(vliegTijd, 10);
                return true;
            }
            vliegTijd = Math.min(vliegTijd, 1 + (getId() % 7));   // the leader landed: land near it too
        }
        return false;
    }

    @Override
    protected void zitStap() {
        PluisvinkjeEntity l = leider();
        if (l != this && l.vliegt() && random.nextInt(6) == 0 && vertrouwen <= 0) {
            startVliegen(null, 200);
        }
        if (--veertjeTijd <= 0) {
            veertjeTijd = VEERTJE_TIJD + random.nextInt(VEERTJE_TIJD);
            laatVeertjeVallen();
        }
        if (voerVeertje > 0) {
            voerVeertje--;
        }
    }

    @Override
    public void schrik(Vec3 van) {
        super.schrik(van);
        // the whole flock flies up
        List<PluisvinkjeEntity> zwerm = level().getEntitiesOfClass(PluisvinkjeEntity.class, getBoundingBox().inflate(8),
                v -> v != this && !v.vliegt() && v.vertrouwen() <= 0);
        for (PluisvinkjeEntity v : zwerm) {
            v.startVliegen(van, 80 + v.getRandom().nextInt(120));
        }
        if (random.nextInt(10) == 0) {
            laatVeertjeVallen();       // a fright: a feather comes loose
        }
    }

    // --- feathers --------------------------------------------------------------------------------------------------------
    /** Drops one pluisveertje (it floats down a bit). Returns the item entity (tests). */
    @Nullable
    public ItemEntity laatVeertjeVallen() {
        if (!(level() instanceof ServerLevel server)) {
            return null;
        }
        ItemEntity item = spawnAtLocation(new ItemStack(VogelsFeature.PLUISVEERTJE.get()), 0.2f);
        if (item != null) {
            item.setDeltaMovement((random.nextDouble() - 0.5) * 0.05, 0.1, (random.nextDouble() - 0.5) * 0.05);
        }
        server.sendParticles(VogelsFeature.VEERTJE.get(), getX(), getY() + 0.3, getZ(), 5, 0.2, 0.2, 0.2, 0.01);
        level().playSound(null, getX(), getY(), getZ(), VogelsFeature.TJIEP.get(), SoundSource.NEUTRAL, 0.5f, 1.4f);
        return item;
    }

    @Override
    protected void gevoerd(ServerPlayer player, ItemStack stack) {
        triggerAnim("actie", "blij");
        if (voerVeertje <= 0) {
            voerVeertje = VOER_VEERTJE_TIJD;
            laatVeertjeVallen();
        }
    }

    @Override
    protected void smult(ServerLevel level, BlockPos voerhuisje) {
        if (voerVeertje <= 0 && random.nextInt(8) == 0) {
            voerVeertje = VOER_VEERTJE_TIJD / 2;
            laatVeertjeVallen();
        }
    }

    /** Test helper: the feather drops next tick by itself. */
    public void veertjeNu() {
        veertjeTijd = 1;
    }

    public boolean magVoerVeertje() {
        return voerVeertje <= 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VeertjeTijd", veertjeTijd);
        tag.putInt("VoerVeertje", voerVeertje);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("VeertjeTijd")) {
            veertjeTijd = tag.getInt("VeertjeTijd");
        }
        voerVeertje = tag.getInt("VoerVeertje");
    }
}
