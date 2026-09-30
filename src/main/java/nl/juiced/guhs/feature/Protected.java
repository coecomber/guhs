package nl.juiced.guhs.feature;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Fire and fluids against the unbreakable guh buildings. Each feature says which spots are its building
 * ({@link #add}); here nothing burns or floods them from the outside: no lighting a fire or pouring lava or water right
 * next to one ({@link #MARGIN} blocks), fire that appears in or against one goes out at once, lava doesn't set it
 * alight, and flowing water and lava from outside stop at its edge (see nl.juiced.guhs.mixin.FlowingFluidMixin).
 * What each feature already forbids inside its own building (breaking, building, buckets...) stays in its own class.
 */
public final class Protected {
    /** How close to a protected building you can't light a fire or empty a bucket. */
    public static final int MARGIN = 3;
    private static final List<BiPredicate<Level, BlockPos>> AREAS = new CopyOnWriteArrayList<>();

    /** A feature's "is this spot part of my building?" (server side; false elsewhere). */
    public static void add(BiPredicate<Level, BlockPos> area) {
        AREAS.add(area);
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Protected::onFluidPlace);
        NeoForge.EVENT_BUS.addListener(Protected::onNeighborNotify);
        NeoForge.EVENT_BUS.addListener(Protected::onUseBlock);
        NeoForge.EVENT_BUS.addListener(Protected::onUseItem);
    }

    /** Is this spot part of any protected guh building? */
    public static boolean at(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        for (BiPredicate<Level, BlockPos> area : AREAS) {
            if (area.test(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Is a protected building within {@code margin} blocks of this spot? */
    public static boolean near(Level level, BlockPos pos, int margin) {
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-margin, -margin, -margin), pos.offset(margin, margin, margin))) {
            if (at(level, p)) {
                return true;
            }
        }
        return false;
    }

    /** In or right against a protected building (where fire would burn it)? */
    private static boolean touching(Level level, BlockPos pos) {
        if (at(level, pos)) {
            return true;
        }
        for (Direction d : Direction.values()) {
            if (at(level, pos.relative(d))) {
                return true;
            }
        }
        return false;
    }

    /** May fluid flow from one spot into the other? Not from outside into a protected building (inside it flows as usual). */
    public static boolean keepsFluidOut(Level level, BlockPos from, BlockPos to) {
        return level instanceof ServerLevel && at(level, to) && !at(level, from);
    }

    /** Things that start a fire or a flood. */
    public static boolean firestarter(ItemStack stack) {
        return stack.getItem() instanceof FlintAndSteelItem || stack.getItem() instanceof FireChargeItem
                || stack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY;
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !near(player.level(), pos, MARGIN)) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.protected.no_fire").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Lava lighting a fire (or turning into stone) in or against a building: it doesn't. */
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && (event.getNewState().getBlock() instanceof BaseFireBlock ? touching(level, event.getPos()) : at(level, event.getPos()))) {
            event.setNewState(event.getOriginalState());
        }
    }

    /** Fire that appears in or against a building (spreading, lightning, a fireball...) goes out before it can burn anything. */
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getState().getBlock() instanceof BaseFireBlock && event.getLevel() instanceof ServerLevel level && touching(level, event.getPos())) {
            event.setCanceled(true);
            level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Flint and steel, a fire charge or a full bucket on (or next to) a building. */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !firestarter(event.getItemStack())) {
            return;
        }
        BlockPos target = event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace());
        if (denied(event.getEntity(), target)) {
            event.setUseItem(TriState.FALSE);
            event.setCanceled(true);
        }
    }

    /** Buckets are emptied where you look (also without clicking a block first). */
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !(event.getItemStack().getItem() instanceof BucketItem) || !firestarter(event.getItemStack())) {
            return;
        }
        Player player = event.getEntity();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = event.getLevel().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos target = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : player.blockPosition();
        if (denied(player, target)) {
            event.setCanceled(true);
        }
    }

    private Protected() {
    }
}
