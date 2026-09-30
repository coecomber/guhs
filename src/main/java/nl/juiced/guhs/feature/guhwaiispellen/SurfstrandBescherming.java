package nl.juiced.guhs.feature.guhwaiispellen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The surf beach of Guhwai'i can't be broken (like the other guh buildings): no breaking, building, emptying buckets or
 * blowing it up, and mobs don't grief it. Players in creative mode may change it. (The sea around it is free.)
 */
public final class SurfstrandBescherming {
    /** Is this spot part of a surf beach? */
    public static boolean inSurfstrand(ServerLevel level, BlockPos pos) {
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(Surfplek.SURFSTRAND);
        return structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }

    static boolean beschermd(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION && inSurfstrand(server, pos);
    }

    public static boolean nee(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !beschermd(player.level(), pos)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.guhwaiispellen.niet_bouwen").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (nee(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? nee(player, event.getPos()) : entity != null && beschermd(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getItemStack().isEmpty()) {
            return;
        }
        if (nee(event.getEntity(), event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION) {
            event.getAffectedBlocks().removeIf(pos -> inSurfstrand(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && beschermd(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private SurfstrandBescherming() {
    }
}
