package nl.juiced.guhs.feature.smul;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The Vadsig eetfestijn can't be broken (like the verstopguh house): no breaking, building, emptying buckets, lighting fires
 * or blowing it up, and mobs don't grief it either. Sitting on the benches and trading still work. Creative players may change it.
 */
public final class SmulProtection {
    public static final ResourceKey<Structure> FESTIJN = ResourceKey.create(Registries.STRUCTURE, Guhs.id("vadsig_eetfestijn"));

    /** Is this spot part of an eetfestijn? */
    public static boolean inFestijn(ServerLevel world, BlockPos pos) {
        var structure = world.registryAccess().registryOrThrow(Registries.STRUCTURE).get(FESTIJN);
        return structure != null && world.structureManager().getStructureAt(pos, structure).isValid();
    }

    static boolean protectedAt(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION && inFestijn(server, pos);
    }

    /** Would this player be stopped here (inside = the spot is part of an eetfestijn)? Creative players never are. */
    public static boolean denies(Player player, boolean inside) {
        return inside && !player.isCreative();
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (!denies(player, protectedAt(player.level(), pos))) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.smul.no_build").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos()) : entity != null && protectedAt(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /**
     * Using an item on a block (buckets, flint and steel, axes...): not here. Sitting on a bench with an empty hand is fine,
     * but the decoration cakes can't be eaten up and the flowers stay in their pots.
     */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getItemStack().isEmpty()) {
            Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
            if ((block instanceof CakeBlock || block instanceof CandleCakeBlock || block instanceof FlowerPotBlock || block instanceof CandleBlock)
                    && denied(event.getEntity(), event.getPos())) {
                event.setUseBlock(TriState.FALSE);
            }
            return;
        }
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are used "in the air" too. */
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION) {
            event.getAffectedBlocks().removeIf(pos -> inFestijn(server, pos));
        }
    }

    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private SmulProtection() {
    }
}
