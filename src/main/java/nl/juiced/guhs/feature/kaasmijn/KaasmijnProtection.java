package nl.juiced.guhs.feature.kaasmijn;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The kaasmijn can't be broken (like the verstopguh house): no breaking, building, buckets or explosions, and mobs
 * don't grief it. The one thing you may break is a cheese vein, with a pickaxe: that's what the mine is for. A mined
 * vein grows back ({@link KaasaderBlock.MinedOut}), so the mine stays whole. Players in creative mode may change it.
 */
public final class KaasmijnProtection {
    public static final ResourceKey<Structure> MINE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("kaasmijn"));
    /** For game tests: areas (in any dimension) that count as a kaasmijn. */
    /** The tag of the sitting guh miners in the template. */
    public static final String MINER_TAG = "guhs_kaasmijn_mijnwerker";
    public static final List<AABB> TEST_AREAS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Is this spot inside a kaasmijn (anywhere in its box: the head, the yard and all of the mine below)? */
    public static boolean inMine(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (!TEST_AREAS.isEmpty()) {
            Vec3 centre = Vec3.atCenterOf(pos);
            for (AABB area : TEST_AREAS) {
                if (area.contains(centre)) {
                    return true;
                }
            }
        }
        if (server.dimension() != ModDimensions.GUHMENSION) {
            return false;
        }
        Structure mine = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(MINE);
        return mine != null && server.structureManager().getStructureWithPieceAt(pos, mine).isValid();
    }

    private static boolean denied(Player player, BlockPos pos, String key) {
        return denied(player, pos, key, false);
    }

    /** Is this not allowed here? Tells the player why, unless {@code quiet}. */
    private static boolean denied(Player player, BlockPos pos, String key, boolean quiet) {
        if (player.getAbilities().instabuild || !inMine(player.level(), pos)) {
            return false;
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /** Only cheese veins, and only with the right tool (a pickaxe), so no vein is ever wasted. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        BlockState state = event.getState();
        if (state.getBlock() instanceof KaasaderBlock) {
            if (!player.getMainHandItem().isCorrectToolForDrops(state) && denied(player, event.getPos(), "quest.guhs.kaasmijn.need_pickaxe")) {
                event.setCanceled(true);
            }
            return;
        }
        String why = state.getBlock() instanceof KaasaderBlock.MinedOut ? "quest.guhs.kaasmijn.growing" : "quest.guhs.kaasmijn.no_build";
        if (denied(player, event.getPos(), why)) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos(), "quest.guhs.kaasmijn.no_build")
                : entity != null && inMine(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Using an item on a block (buckets, flint and steel, axes stripping logs...): not in the mine. Opening things is fine, and so is putting a cart on the rails. */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty() || event.getItemStack().getItem() instanceof MinecartItem) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        // (no scolding for a pickaxe or a snack in your paw: you're just clicking a dispenser or the vault with it)
        ItemStack stack = event.getItemStack();
        boolean quiet = stack.getItem() instanceof PickaxeItem || stack.has(DataComponents.FOOD);
        if (denied(event.getEntity(), event.getPos(), "quest.guhs.kaasmijn.no_build", quiet)
                || denied(event.getEntity(), event.getPos().relative(face), "quest.guhs.kaasmijn.no_build", quiet)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are used "in the air" too. */
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition(), "quest.guhs.kaasmijn.no_build")) {
            event.setCanceled(true);
        }
    }

    /** The sitting guh miners are part of the mine: no taming, leashing, name tags or undressing them. */
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (isMiner(event.getTarget()) && !event.getEntity().getAbilities().instabuild) {
            if (!event.getLevel().isClientSide()) {
                event.getEntity().sendOverlayMessage(Component.translatable("quest.guhs.kaasmijn.miner_busy").withStyle(ChatFormatting.GOLD));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onInteractEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isMiner(event.getTarget()) && !event.getEntity().getAbilities().instabuild) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** A decorative guh miner of the mine (tagged in the template). */
    public static boolean isMiner(Entity entity) {
        return entity.entityTags().contains(MINER_TAG);
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inMine(server, pos));
        }
    }

    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inMine(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private KaasmijnProtection() {
    }
}
