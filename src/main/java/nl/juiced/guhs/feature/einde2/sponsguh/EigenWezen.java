package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.EnumSet;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * einde2, slice sponsguh_wezens: a character that can also be a player's OWN: the Sponsguh buddy ({@link SponsguhEntity})
 * and Gerrit the pet ({@link GerritEntity}). Without an owner it is the plain story character; with one (only
 * {@link Maatjes#geef} makes those) it follows its owner, can be told to stay where it stands (sneak-click, saved) and is
 * never lost: the player's own record names the ONE that is theirs ({@link Maatjes}), and any other copy that still thinks
 * it is tidies itself up as soon as it meets its owner again.
 */
public abstract class EigenWezen extends SponsguhWezen {
    @Nullable
    private UUID eigenaar;
    private boolean thuis;

    protected EigenWezen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** "sponsguh" or "gerrit": the name of this kind in the player keys and the lang keys. */
    protected abstract String soortNaam();

    /** How fast it follows (1.0 = its walking speed) and from how far it starts to. */
    protected double volgSnelheid() {
        return 1.3;
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public boolean heeftEigenaar() {
        return eigenaar != null;
    }

    public boolean isVan(ServerPlayer player) {
        return player.getUUID().equals(eigenaar);
    }

    void zetEigenaar(@Nullable UUID eigenaar) {
        this.eigenaar = eigenaar;
    }

    /** It stays at home where it stands (true) or follows its owner (false). */
    public boolean thuis() {
        return thuis;
    }

    public void zetThuis(boolean thuis) {
        this.thuis = thuis;
        if (thuis) {
            getNavigation().stop();
            setHomeTo(blockPosition(), 6);
        } else {
            clearHome();
        }
    }

    @Nullable
    protected ServerPlayer baas() {
        return eigenaar != null && level() instanceof ServerLevel level && level.getPlayerByUUID(eigenaar) instanceof ServerPlayer p ? p : null;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (eigenaar != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        tag.putBoolean("Thuis", thuis);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        eigenaar = tag.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        thuis = tag.getBooleanOr("Thuis", false);
    }

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(3, new VolgBaasGoal());
    }

    @Override
    protected boolean magLopen() {
        return super.magLopen();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || eigenaar == null || (tickCount + getId()) % 20 != 0) {
            return;
        }
        ServerPlayer baas = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(eigenaar);
        if (baas != null && !Maatjes.isDeEchte(baas, soortNaam(), getUUID())) {
            discard();                                                       // (a copy that was left behind: its owner has another one now)
        }
    }

    /** The owner's clicks: sneak = stay here / come along again, a plain click = petting ({@link #geaaid}). */
    @Override
    protected boolean eigenKlik(ServerPlayer player) {
        if (eigenaar == null) {
            return false;
        }
        if (!isVan(player)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens." + soortNaam() + ".van_ander", getDisplayName())
                    .withStyle(ChatFormatting.GRAY));
            return true;
        }
        if (player.isShiftKeyDown()) {
            Maatjes.zetThuis(player, soortNaam(), !thuis);
            return true;
        }
        geaaid(player);
        return true;
    }

    /** Petted by its owner. */
    protected void geaaid(ServerPlayer player) {
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 2, 0.2, 0.1, 0.2, 0);
    }

    /** Trots after its owner; when the owner got far away it pops up next to them (as a tamed animal does). */
    private class VolgBaasGoal extends Goal {
        @Nullable
        private ServerPlayer baas;
        private int wacht;

        VolgBaasGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (thuis || stil() || isPassenger() || !EigenWezen.super.magLopen()) {
                return false;
            }
            baas = baas();
            return baas != null && !baas.isSpectator() && distanceToSqr(baas) > 5.0 * 5.0;
        }

        @Override
        public boolean canContinueToUse() {
            return baas != null && baas.isAlive() && !thuis && !stil() && baas.level() == level() && distanceToSqr(baas) > 2.5 * 2.5;
        }

        @Override
        public void start() {
            wacht = 0;
        }

        @Override
        public void stop() {
            baas = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (baas == null) {
                return;
            }
            getLookControl().setLookAt(baas, 10f, getMaxHeadXRot());
            if (--wacht > 0) {
                return;
            }
            wacht = adjustedTickDelay(10);
            if (distanceToSqr(baas) > 24.0 * 24.0) {
                BlockPos bij = Maatjes.vastePlekBij(baas);
                if (bij != null) {                                           // (an owner in the air: it waits here until they land)
                    snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5, getYRot(), getXRot());
                }
                getNavigation().stop();
            } else {
                getNavigation().moveTo(baas, volgSnelheid());
            }
        }
    }
}
