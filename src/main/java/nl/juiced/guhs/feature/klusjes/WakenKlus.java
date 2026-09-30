package nl.juiced.guhs.feature.klusjes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * Wachten & waarschuwen: when a Mika or a monster comes near the huisje, the resident peeps (a pink "!" over its head),
 * runs to its owner when they are near (and waves: "pas op!") or else sends them a little message, and then gently
 * pushes Mika's out of the home base: a soft shove, knockback only. It NEVER fights and never hurts anything; other
 * monsters it only warns about. One warning per huisje per {@link #RUST} ticks. Guhs and pieppiepmuisjes (muisjes peep
 * and run, but are too small to push).
 */
public class WakenKlus extends BasisKlus {
    /** How far outside the home base a threat is noticed. */
    public static final int EXTRA = 6;
    /** Minimum ticks between two warnings of one huisje. */
    public static final int RUST = 400;
    /** The owner is fetched when within this many blocks of the huisje, else messaged. */
    public static final int BAAS_BIJ = 40;
    public static final int DUW_KEER = 3;
    private static final Map<String, Long> GEWAARSCHUWD = new ConcurrentHashMap<>();

    WakenKlus() {
        super("waken", () -> new ItemStack(Items.BELL), 60);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner) || isMuisje(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        LivingEntity dreiging = dreiging(level, huisje, bewoner);
        if (dreiging == null) {
            return null;
        }
        String key = level.dimension().identifier() + "|" + huisje.pos().asLong();
        long nu = level.getGameTime();
        boolean waarschuwen = GEWAARSCHUWD.getOrDefault(key, Long.MIN_VALUE / 2) + RUST <= nu;
        boolean duwen = isMika(dreiging) && isGuh(bewoner) && huisje.inGebied(dreiging.blockPosition());
        if (!waarschuwen && !duwen) {
            return null;
        }
        if (waarschuwen) {
            GEWAARSCHUWD.put(key, nu);
        }
        return new Taak(level, huisje, bewoner, dreiging, waarschuwen, duwen);
    }

    /** The nearest Mika or monster near the huisje (a tame thing never counts), or null. */
    @Nullable
    static LivingEntity dreiging(ServerLevel level, Huisje huisje, Mob wie) {
        Vec3 m = huisje.midden();
        double r = Huisjes.BEREIK + EXTRA;
        List<Mob> lijst = level.getEntitiesOfClass(Mob.class, huisje.gebied().inflate(EXTRA), e -> e.isAlive() && e != wie
                && (e instanceof Enemy || isMika(e)) && !isSpelMika(e) && !(e instanceof OwnableEntity o && o.getOwnerUUID() != null)
                && !e.isPassenger() && e.position().distanceToSqr(m.x, e.getY(), m.z) <= r * r);
        return lijst.stream().min(Comparator.comparingDouble(e -> e.position().distanceToSqr(m))).orElse(null);
    }

    /** A soft shove away from the huisje: knockback only, never damage. */
    public static void duw(ServerLevel level, Mob guh, LivingEntity mika, Vec3 wegVan) {
        double dx = wegVan.x - mika.getX(), dz = wegVan.z - mika.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            dx = guh.getX() - mika.getX();
            dz = guh.getZ() - mika.getZ();
        }
        mika.knockback(0.9, dx, dz);
        mika.hurtMarked = true;
        if (mika instanceof Mob m) {
            m.getNavigation().stop();
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, mika.getX(), mika.getY() + mika.getBbHeight() * 0.5, mika.getZ(),
                6, 0.25, 0.2, 0.25, 0.02);
        level.playSound(null, mika.blockPosition(), KlusjesFeature.DUW.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    private class Taak extends StappenTaak {
        private final LivingEntity dreiging;
        private final boolean waarschuwen, duwen;
        private int geduwd;

        Taak(ServerLevel level, Huisje huisje, Mob mob, LivingEntity dreiging, boolean waarschuwen, boolean duwen) {
            super(level, huisje, mob, WakenKlus.this);
            this.dreiging = dreiging;
            this.waarschuwen = waarschuwen;
            this.duwen = duwen;
        }

        @Override
        protected void begin() {
            erbij(doe(this::piep));
            if (waarschuwen) {
                ServerPlayer baas = Band.eigenaarOnline(mob);
                if (baas != null && baas.level() == level && baas.position().distanceToSqr(huisje.midden()) <= BAAS_BIJ * BAAS_BIJ) {
                    erbij(loopNaar(() -> baas.isAlive() ? baas : null, 2.6));
                    erbij(doe(() -> waarschuw(baas, true)));
                } else if (baas != null) {
                    erbij(doe(() -> waarschuw(baas, false)));
                } else {
                    erbij(doe(this::gelukt));
                }
            }
            if (duwen) {
                naarMika();
            }
        }

        private void naarMika() {
            erbij(loopNaar(() -> dreiging.isAlive() ? dreiging : null, 1.9));
            erbij(werk(6, dreiging.position().add(0, dreiging.getBbHeight() * 0.6, 0), null));
            erbij(doe(() -> {
                if (!dreiging.isAlive()) {
                    return;
                }
                duw(level, mob, dreiging, huisje.midden());
                geduwd++;
                aantal = geduwd;
                gelukt();
                if (geduwd < DUW_KEER && huisje.inGebied(dreiging.blockPosition())) {
                    naarMika();
                }
            }));
        }

        private void piep() {
            level.playSound(null, mob.blockPosition(), KlusjesFeature.PIEP.get(), SoundSource.NEUTRAL, 1f, 1.3f + mob.getRandom().nextFloat() * 0.2f);
            level.sendParticles(KlusjesFeature.UITROEP.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.5, mob.getZ(), 1, 0, 0, 0, 0);
            mob.getLookControl().setLookAt(dreiging, 30f, 30f);
            if (mob.onGround()) {
                mob.getJumpControl().jump();
            }
        }

        private void waarschuw(ServerPlayer baas, boolean erbij) {
            Component wat = isMika(dreiging) ? Component.translatable("gui.guhs.klusjes.waken.mika")
                    : Component.translatable("gui.guhs.klusjes.waken.monster", dreiging.getName());
            if (erbij) {
                mob.getLookControl().setLookAt(baas, 30f, 30f);
                level.playSound(null, mob.blockPosition(), KlusjesFeature.PIEP.get(), SoundSource.NEUTRAL, 1f, 1.5f);
                level.sendParticles(KlusjesFeature.UITROEP.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.5, mob.getZ(), 1, 0, 0, 0, 0);
                baas.sendOverlayMessage(Component.translatable("gui.guhs.klusjes.waken.hier", mob.getName(), wat, huisje.naam())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                if (mob instanceof GuhEntity g) {
                    g.emotes.start(Emote.ZWAAIEN, false, GuhEmotes.Source.SELF);
                }
            } else {
                baas.sendSystemMessage(Component.translatable("gui.guhs.klusjes.waken.ver", mob.getName(), wat, huisje.naam())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            gelukt();
        }
    }
}
