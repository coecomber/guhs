package nl.juiced.guhs.feature.golf;

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
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The guh golf course can't be broken (like the verstopguh house): no breaking, building, buckets, fire or explosions,
 * and mobs don't grief it. Opening things is fine. Players in creative mode may change it.
 */
public final class GolfProtection {
    public static final ResourceKey<Structure> COURSE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_golfbaan"));

    /** Is this spot part of a guh golf course? */
    public static boolean inCourse(ServerLevel world, BlockPos pos) {
        Structure structure = world.registryAccess().registryOrThrow(Registries.STRUCTURE).get(COURSE);
        return structure != null && world.structureManager().getStructureAt(pos, structure).isValid();
    }

    static boolean protectedAt(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION && inCourse(server, pos);
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !protectedAt(player.level(), pos)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.golf.no_build").withStyle(ChatFormatting.LIGHT_PURPLE), true);
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

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getItemStack().isEmpty() || event.getItemStack().is(GolfFeature.GOLFCLUB.get())) {
            return;                                                    // (swinging the club at the ball on the ground is fine)
        }
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getItemStack().getItem() instanceof BucketItem && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION) {
            event.getAffectedBlocks().removeIf(pos -> inCourse(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private GolfProtection() {
    }
}
