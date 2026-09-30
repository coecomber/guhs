package nl.juiced.guhs.feature.knabbelspelen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * De Knabbelspelen can't be broken (the tent, the fields, the tins and bottles: Juf Vahoegsakee sets them up herself): no breaking, building, emptying buckets, lighting fires or
 * blowing it up, and mobs don't grief it either. Players in creative mode may change it.
 */
public final class KnabbelspelenProtection {
    public static final ResourceKey<Structure> SPELEN = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knabbelspelen"));

    /** Is this spot part of a Knabbelspelen? */
    public static boolean inSpelen(ServerLevel world, BlockPos pos) {
        var structure = world.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SPELEN);
        return structure != null && world.structureManager().getStructureAt(pos, structure).isValid();
    }

    static boolean protectedAt(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION && inSpelen(server, pos);
    }

    /** May this player change the block at pos? (No, inside De Knabbelspelen: unless in creative.) */
    public static boolean denied(Player player, BlockPos pos) {
        return denied(player, !player.getAbilities().instabuild && protectedAt(player.level(), pos));
    }

    /** (Also for the tests) says no, with a message, if the spot is protected for this player. */
    static boolean denied(Player player, boolean protectedSpot) {
        if (!protectedSpot || player.getAbilities().instabuild) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.knabbelspelen.no_build").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
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

    /** Using an item on a block (buckets, flint and steel, axes...): not at the Knabbelspelen. Eating is fine. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getItemStack().isEmpty() || KnabbelspelenFeature.geleend(event.getItemStack())) {
            return;                                           // (the staartje goes on the guh board: that's the game)
        }
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are used "in the air" too. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    /** No knocking things out of frames either. */
    @SubscribeEvent
    public static void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getTarget() instanceof net.minecraft.world.entity.decoration.HangingEntity && denied(event.getEntity(), event.getTarget().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION) {
            event.getAffectedBlocks().removeIf(pos -> inSpelen(server, pos));
            event.getAffectedEntities().removeIf(e -> e instanceof net.minecraft.world.entity.decoration.HangingEntity && inSpelen(server, e.blockPosition()));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private KnabbelspelenProtection() {
    }
}
