package nl.juiced.guhs.feature.vissen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * The guhvis pond can't be broken (like the verstopguh house): no breaking, building, scooping up or pouring water,
 * lighting fires or blowing it up, and mobs don't grief it. Opening barrels and sitting on the benches is fine.
 * Players in creative mode may change it.
 */
public final class VissenProtection {
    public static final ResourceKey<Structure> VIJVER = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guhvis_vijver"));

    /** Is this spot in a guhvis pond (the structure)? */
    public static boolean inVijver(ServerLevel world, BlockPos pos) {
        var structure = world.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(VIJVER);
        return structure != null && world.structureManager().getStructureAt(pos, structure).isValid();
    }

    static boolean protectedAt(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION && inVijver(server, pos);
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !protectedAt(player.level(), pos)) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.vissen.no_build").withStyle(ChatFormatting.AQUA));
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos()) : entity != null && protectedAt(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Using an item on a block (buckets, flint and steel, axes stripping logs...): not here. Opening things is fine. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        var stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty() || stack.getItem() instanceof net.minecraft.world.item.FishingRodItem
                || stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
            return;         // (casting a line or eating a fish while looking at the pier is fine)
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(face))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are used "in the air" too (scooping the pond empty: njeg!). */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION) {
            event.getAffectedBlocks().removeIf(pos -> inVijver(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private VissenProtection() {
    }
}
