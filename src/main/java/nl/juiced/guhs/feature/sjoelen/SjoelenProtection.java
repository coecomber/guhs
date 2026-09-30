package nl.juiced.guhs.feature.sjoelen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.world.ModDimensions;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * The Sjoelhuisje can't be broken or built in: no breaking, no placing blocks, no buckets, no explosions, no griefing
 * mobs. Doors, chests and chairs still work. Creative players may change it.
 */
public final class SjoelenProtection {
    /** Extra protected boxes, only for the GameTests (the test world has no generated Sjoelhuisje). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();

    /** Is this spot part of a Sjoelhuisje (server side)? */
    public static boolean inHuisje(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        if (server.dimension() != ModDimensions.GUHMENSION) {
            return false;
        }
        Structure structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(SjoelenFeature.SJOELHUISJE);
        return structure != null && server.structureManager().getStructureAt(pos, structure).isValid();
    }

    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild || !inHuisje(player.level(), pos)) {
            return false;
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.sjoelen.no_build").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && inHuisje(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL))) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos(), true) || denied(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem && denied(event.getEntity(), event.getEntity().blockPosition(), false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inHuisje(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inHuisje(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private SjoelenProtection() {
    }
}
