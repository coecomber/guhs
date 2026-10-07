package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * einde2, slice vetbootje: what the vetbootje and the heteluchtballon share: your own guhs hopping aboard and off again
 * (a click with a kaasknabbel), a soft landing for whoever leaves high in the air, and the chest's door.
 */
final class Meevaren {
    /** A guh wider than this does not fit on a cushion ("te vads voor het bootje"). */
    static final float TE_VADS = 1.5f;
    /** How far from the vehicle your guhs hear the knabbel rustle. */
    static final double BEREIK = 8;

    /** Is this one of the player's own guhs, free to hop aboard? */
    static boolean eigenGuh(GuhEntity guh, Player player) {
        return guh.isAlive() && guh.isTame() && player.getUUID().equals(guh.getOwnerUUID());
    }

    /**
     * A click with a kaasknabbel: the player's own guhs close by hop aboard (as many as there are free seats); when none
     * can, the ones aboard hop off again. Returns how many got on (negative: how many got off).
     */
    static int wissel(Entity vaartuig, ServerPlayer p, int vrij) {
        ServerLevel level = p.level();
        List<GuhEntity> dichtbij = new ArrayList<>(level.getEntitiesOfClass(GuhEntity.class, vaartuig.getBoundingBox().inflate(BEREIK),
                g -> eigenGuh(g, p) && !g.isPassenger() && !g.isVehicle()));
        dichtbij.sort(Comparator.comparingDouble(g -> g.distanceToSqr(vaartuig)));
        int erin = 0;
        boolean teVads = false;
        for (GuhEntity guh : dichtbij) {
            if (erin >= vrij) {
                break;
            }
            if (guh.getBbWidth() > TE_VADS) {
                teVads = true;
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.guh.te_vads", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                continue;
            }
            guh.getNavigation().stop();
            if (guh.startRiding(vaartuig)) {
                erin++;
                level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 3, 0.2, 0.1, 0.2, 0);
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.guh.aan_boord", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (erin > 0) {
            level.playSound(null, vaartuig.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.8f, 1.3f);
            return erin;
        }
        if (teVads) {
            return 0;
        }
        int eruit = 0;
        for (Entity e : List.copyOf(vaartuig.getPassengers())) {
            if (e instanceof GuhEntity guh && eigenGuh(guh, p)) {
                guh.stopRiding();
                if (p.onGround() && !p.isPassenger()) {
                    guh.teleportTo(p.getX(), p.getY(), p.getZ());    // (next to you, not under a hovering boat)
                }
                eruit++;
            }
        }
        if (eruit > 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.guh.van_boord").withStyle(ChatFormatting.LIGHT_PURPLE));
            return -eruit;
        }
        p.sendOverlayMessage(Component.translatable(vrij <= 0 ? "gui.guhs.einde2_vetbootje.vol" : "gui.guhs.einde2_vetbootje.guh.geen")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return 0;
    }

    /** Somebody left the vehicle: with nothing to stand on within a few blocks below they float down (no fall, ever). */
    static void zachtLanden(Entity vaartuig, Entity passenger) {
        if (vaartuig.level().isClientSide() || !(passenger instanceof LivingEntity living)) {
            return;
        }
        BlockPos onder = vaartuig.blockPosition();
        for (int i = 0; i <= 3; i++) {
            if (!vaartuig.level().getBlockState(onder.below(i)).getCollisionShape(vaartuig.level(), onder.below(i)).isEmpty()) {
                return;
            }
        }
        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
        living.resetFallDistance();
    }

    /** The chest of a vetbootje as its owner opens it: the 27 slots of the fleet book, for as long as they stand at the boat. */
    static final class Kist implements Container {
        private final Container kist;
        private final VetbootjeEntity boot;

        Kist(Container kist, VetbootjeEntity boot) {
            this.kist = kist;
            this.boot = boot;
        }

        @Override
        public int getContainerSize() {
            return kist.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return kist.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return kist.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return kist.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return kist.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            kist.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            kist.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return boot.isAlive() && boot.isVan(player) && player.distanceToSqr(boot) < 64;
        }

        @Override
        public void clearContent() {
            kist.clearContent();
        }
    }

    private Meevaren() {
    }
}
