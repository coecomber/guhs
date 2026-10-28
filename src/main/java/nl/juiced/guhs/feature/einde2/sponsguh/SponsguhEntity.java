package nl.juiced.guhs.feature.einde2.sponsguh;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.registry.ModFluids;
import nl.juiced.guhs.world.GuhTime;

/**
 * einde2, slice sponsguh_wezens: Sponsguh Kaasbroek, a square block of gatenkaas with guh ears, and (with an owner, see
 * {@link Sponsguh}) a player's buddy. What the buddy does by itself:
 * <ul>
 *   <li>now and then (at most once a minute) it soaks up ONE block of spilled water or kaassaus within {@value #ZUIG_BEREIK}
 *       blocks ({@link #magOpzuigen}: a flowing block, or a single source without another source next to it; never a pond,
 *       never in a protected place, never in somebody else's Guhhuisje area);</li>
 *   <li>once per Minecraft day it bakes a Knabbelburger for its owner (it does not need the secret recipe: it IS the fry
 *       cook);</li>
 *   <li>it laughs, and its laugh is contagious: guhs within {@value #LACH_BEREIK} blocks hop along.</li>
 * </ul>
 */
public class SponsguhEntity extends EigenWezen {
    public static final int ZUIG_BEREIK = 4, LACH_BEREIK = 8, ZUIG_RUST = 1200;

    private long volgendeZuig, volgendeLach;
    /** "It never baked": the first burger comes the day after it was given. */
    private static final long NOOIT = Long.MIN_VALUE;
    /** The last Minecraft day it baked (or {@link #NOOIT}). */
    private long burgerDag = NOOIT;
    /** Server: ticks until the burger that is baking now is handed over (0: none). */
    private int baktNog;

    public SponsguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String soortNaam() {
        return "sponsguh";
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("BurgerDag", burgerDag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        burgerDag = tag.getLongOr("BurgerDag", NOOIT);
    }

    @Nullable
    @Override
    protected SoundEvent geluid() {
        return E2SponsguhFeature.SPONSGUH_LACH.get();
    }

    @Override
    protected float stem() {
        return 1.0f + random.nextFloat() * 0.15f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !heeftEigenaar()) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        long nu = level.getGameTime();
        if (baktNog > 0 && --baktNog == 0) {
            geefBurger();
        }
        if ((tickCount + getId()) % 40 != 0) {
            return;
        }
        ServerPlayer baas = baas();
        if (baas == null || baas.isSpectator()) {
            return;
        }
        if (burgerDag == NOOIT) {
            burgerDag = GuhTime.day(level);
        } else if (baktNog == 0 && GuhTime.day(level) > burgerDag && distanceToSqr(baas) < 8 * 8) {
            bak();
        }
        if (nu >= volgendeZuig && zuig(baas)) {
            volgendeZuig = nu + ZUIG_RUST;
        }
        if (nu >= volgendeLach && distanceToSqr(baas) < 6 * 6) {
            volgendeLach = nu + 1800 + random.nextInt(1800);
            if (random.nextInt(3) == 0) {
                lach();
            }
        }
    }

    // --- soaking up a spill ------------------------------------------------------------------------------------------------

    /**
     * May the buddy of this owner soak up the fluid at this spot? Only plain water or kaassaus (a fluid block, nothing
     * waterlogged), and only a spill: a flowing block, or a single source block with no other source of it on any of its
     * six sides. Never in a protected einde2 place, never inside the area of a Guhhuisje its owner may not change.
     */
    public static boolean magOpzuigen(ServerLevel level, BlockPos pos, @Nullable ServerPlayer baas) {
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (!(state.getBlock() instanceof LiquidBlock) || fluid.isEmpty() || !isSpilbaar(fluid)) {
            return false;
        }
        if (fluid.isSource()) {
            for (Direction d : Direction.values()) {
                FluidState buur = level.getFluidState(pos.relative(d));
                if (buur.isSource() && buur.getType().isSame(fluid.getType())) {
                    return false;                                            // (a pond, a canal, a sauce lake: not a spill)
                }
            }
        }
        if (Einde2Bescherming.beschermd(level, pos)) {
            return false;
        }
        for (Huisje huisje : Huisjes.rond(level, pos, Huisjes.MAX_BEREIK)) {
            if (baas == null || !Huisjes.magBewerken(baas, huisje)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSpilbaar(FluidState fluid) {
        return fluid.getType().isSame(Fluids.WATER) || fluid.getType().isSame(ModFluids.KAAS_SAUS.get());
    }

    /** Looks for a spill within reach and soaks up one block (true: it did). A single source goes before a flowing block. */
    public boolean zuig(@Nullable ServerPlayer baas) {
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos hier = blockPosition(), beste = null;
        boolean besteBron = false;
        double besteAfstand = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(hier.offset(-ZUIG_BEREIK, -1, -ZUIG_BEREIK), hier.offset(ZUIG_BEREIK, 2, ZUIG_BEREIK))) {
            if (level.getFluidState(p).isEmpty() || !magOpzuigen(level, p, baas)) {
                continue;
            }
            boolean bron = level.getFluidState(p).isSource();
            double d = p.distSqr(hier);
            if (beste == null || (bron && !besteBron) || (bron == besteBron && d < besteAfstand)) {
                beste = p.immutable();
                besteBron = bron;
                besteAfstand = d;
            }
        }
        if (beste == null) {
            return false;
        }
        getLookControl().setLookAt(beste.getX() + 0.5, beste.getY(), beste.getZ() + 0.5);
        level.setBlockAndUpdate(beste, Blocks.AIR.defaultBlockState());
        zetAnimatie("zuig");
        level.playSound(null, blockPosition(), SoundEvents.SPONGE_ABSORB, SoundSource.NEUTRAL, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.SPLASH, beste.getX() + 0.5, beste.getY() + 0.4, beste.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.1);
        return true;
    }

    // --- the daily burger ----------------------------------------------------------------------------------------------------

    /** Starts baking today's Knabbelburger (the owner gets it a moment later). */
    public void bak() {
        if (level().isClientSide() || baktNog > 0) {
            return;
        }
        burgerDag = GuhTime.day(level());
        baktNog = 34;
        zetAnimatie("bak");
        level().playSound(null, blockPosition(), E2SponsguhFeature.GRILL_SIS.get(), SoundSource.NEUTRAL, 0.7f, 1.2f);
        ((ServerLevel) level()).sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.1, getZ(), 6, 0.2, 0.1, 0.2, 0.01);
    }

    private void geefBurger() {
        ServerPlayer baas = baas();
        ItemStack burger = new ItemStack(E2SponsguhFeature.KNABBELBURGER.get());
        if (baas != null) {
            Minigames.give(baas, burger);
            baas.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens.sponsguh.burger", getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            spawnAtLocation((ServerLevel) level(), burger);
        }
        playSound(SoundEvents.ITEM_PICKUP, 0.6f, 1.3f);
    }

    /** The day it last baked (tests). */
    public long burgerDag() {
        return burgerDag;
    }

    /** (Tests) as if it last baked on this day. */
    public void zetBurgerDag(long dag) {
        burgerDag = dag;
    }

    public boolean bakt() {
        return baktNog > 0;
    }

    // --- the contagious laugh ------------------------------------------------------------------------------------------------

    /** Laughs: the laugh animation and sound, and every guh within {@value #LACH_BEREIK} blocks hops along. Returns how many did. */
    public int lach() {
        if (!(level() instanceof ServerLevel level)) {
            return 0;
        }
        zetAnimatie("lach");
        playSound(E2SponsguhFeature.SPONSGUH_LACH.get(), 1.0f, 0.95f + random.nextFloat() * 0.15f);
        int n = 0;
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(LACH_BEREIK), g -> g.isAlive() && !g.isPassenger())) {
            if (guh.onGround() && !guh.isOrderedToSit()) {
                guh.getJumpControl().jump();
            }
            level.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 1, 0.1, 0, 0.1, 0);
            n++;
        }
        return n;
    }

    @Override
    protected void geaaid(ServerPlayer player) {
        super.geaaid(player);
        lach();
    }
}
