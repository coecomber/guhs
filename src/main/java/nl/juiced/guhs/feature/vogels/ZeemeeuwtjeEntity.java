package nl.juiced.guhs.feature.vogels;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The zeemeeuwtje: a white gull with a cheeky look. It sits on the beach or bobs on the sea. Hold a fish (or bread) near it
 * and it comes circling over your head with its friends, shouting "Mijn! Mijn!"; fish lying on the ground it snatches
 * up (only fish and bread: your other things are safe). Give it one and it's the happiest gull of the Guhzee.
 */
public class ZeemeeuwtjeEntity extends Vogeltje {
    /** How far it sees food in a hand or on the ground. */
    public static final double VOER_ZICHT = 12.0;

    private int mijnTijd = 40;
    @Nullable
    private ItemEntity hapje;

    public ZeemeeuwtjeEntity(EntityType<? extends Vogeltje> type, Level level) {
        super(type, level);
    }

    @Override
    public String naam() {
        return "zeemeeuwtje";
    }

    @Override
    protected double vliegSnelheid() {
        return 0.36;
    }

    @Override
    public boolean lekker(ItemStack stack) {
        return stack.is(VogelTags.VISJES);
    }

    @Override
    protected SoundEvent roep() {
        return VogelsFeature.KLIEW.get();
    }

    @Override
    public double schrikAfstand() {
        return 3.0;
    }

    @Override
    protected boolean magZwemmen() {
        return true;
    }

    /** Sand, stones, a pier... and the sea itself (it bobs on the water). */
    @Override
    public boolean magLanden(BlockPos plek) {
        if (super.magLanden(plek)) {
            return true;
        }
        BlockState onder = level().getBlockState(plek.below());
        return level().isLoaded(plek) && level().getBlockState(plek).isAir() && onder.getFluidState().isSource()
                && onder.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
    }

    /** The nearest player within {@link #VOER_ZICHT} with fish or bread in a hand, or null. */
    @Nullable
    public Player voerder() {
        Player best = null;
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(VOER_ZICHT), p -> !p.isSpectator() && p.isAlive())) {
            if ((lekker(p.getMainHandItem()) || lekker(p.getOffhandItem())) && (best == null || p.distanceToSqr(this) < best.distanceToSqr(this))) {
                best = p;
            }
        }
        return best;
    }

    /** Fish or bread lying on the ground nearby, or null. */
    @Nullable
    public ItemEntity hapjeOpDeGrond() {
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10, 6, 10),
                i -> i.isAlive() && lekker(i.getItem()));
        ItemEntity best = null;
        for (ItemEntity i : items) {
            if (best == null || i.distanceToSqr(this) < best.distanceToSqr(this)) {
                best = i;
            }
        }
        return best;
    }

    @Override
    protected void zitStap() {
        if (isInWater()) {         // bob on the waves
            setDeltaMovement(getDeltaMovement().multiply(0.8, 0.5, 0.8).add(0, 0.045, 0));
        }
        if (tickCount % 10 == 0 && (voerder() != null || hapjeOpDeGrond() != null)) {
            startVliegen(null, 200);
        }
    }

    @Override
    protected boolean vliegStap() {
        if (tickCount % 10 == 0 && (hapje == null || !hapje.isAlive())) {
            hapje = hapjeOpDeGrond();
        }
        if (hapje != null && hapje.isAlive()) {
            landplek = null;
            vliegTijd = Math.max(vliegTijd, 60);
            stuur(hapje.position().add(0, 0.2, 0), vliegSnelheid());
            if (distanceToSqr(hapje) < 0.9) {
                hap(hapje);
                hapje = null;
            }
            return true;
        }
        Player p = voerder();
        if (p != null) {
            landplek = null;
            vliegTijd = Math.max(vliegTijd, 80);
            double a = tickCount * 0.06 + getId() * 1.7;
            double r = 2.2 + (getId() % 3) * 0.6;
            stuur(p.position().add(Math.cos(a) * r, 3.2 + 0.6 * Math.sin(getId() + tickCount * 0.1), Math.sin(a) * r), vliegSnelheid());
            if (--mijnTijd <= 0 && p instanceof ServerPlayer sp) {
                mijnMijn(sp);
            }
            return true;
        }
        return false;
    }

    /** "Mijn! Mijn!": the call, the beak animation, a line above the hotbar for that player. */
    public void mijnMijn(@Nullable ServerPlayer tegen) {
        mijnTijd = 60 + random.nextInt(80);
        triggerAnim("actie", "roep");
        level().playSound(null, getX(), getY(), getZ(), VogelsFeature.MIJN_MIJN.get(), SoundSource.NEUTRAL, 1.0f, 0.9f + random.nextFloat() * 0.25f);
        if (tegen != null) {
            tegen.displayClientMessage(Component.translatable("gui.guhs.vogels.mijn_mijn").withStyle(ChatFormatting.WHITE), true);
            GidsFeature.grant(tegen, "diertjes/vogels_mijn");
        }
    }

    /** Snatches one fish/bread from a stack lying on the ground. */
    public void hap(ItemEntity item) {
        item.getItem().shrink(1);
        if (item.getItem().isEmpty()) {
            item.discard();
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5f, 1.6f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
        mijnMijn(null);
    }

    @Override
    protected void gevoerd(ServerPlayer player, ItemStack stack) {
        mijnMijn(player);
    }

    @Override
    protected String[] extraActies() {
        return new String[]{"roep"};
    }

    /** Gliding now and then while it flies (both sides agree: it only depends on the tick count and the id). */
    public boolean zweeft() {
        return vliegt() && ((tickCount + getId() * 13) / 50) % 3 == 0;
    }

    @Override
    protected RawAnimation beweging(AnimationState<Vogeltje> state) {
        if (zweeft()) {
            return anim("glide", true);
        }
        if (!vliegt() && isInWater()) {
            return anim("idle", true);
        }
        return super.beweging(state);
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    static boolean strand(BlockState s) {
        return s.is(BlockTags.SAND);
    }
}
