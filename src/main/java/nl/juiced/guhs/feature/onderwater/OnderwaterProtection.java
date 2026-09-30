package nl.juiced.guhs.feature.onderwater;

import java.util.List;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The Guhbubbel can't be broken: one broken pane and the whole dome would fill with water. No breaking, building,
 * buckets or explosions anywhere in the structure (its piece of the sea, the island, the Duikpost), and mobs don't grief it.
 * Opening doors and chests, taking pearls from the shells and talking to the Zeemeerguh are fine, of course.
 * Players in creative mode may change it.
 */
public final class OnderwaterProtection {
    public static final ResourceKey<Structure> BUBBLE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("onderwater"));
    /** For game tests: areas (in any dimension) that count as the Guhbubbel. */
    public static final List<AABB> TEST_AREAS = new java.util.concurrent.CopyOnWriteArrayList<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(OnderwaterProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(OnderwaterProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(OnderwaterProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(OnderwaterProtection::onUseItem);
        NeoForge.EVENT_BUS.addListener(OnderwaterProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(OnderwaterProtection::onMobGriefing);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> TEST_AREAS.clear());
    }

    /** Is this spot part of a Guhbubbel (anywhere in its box: its piece of the sea, the dome, the island and the Duikpost)? */
    public static boolean inBubble(Level level, BlockPos pos) {
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
        Structure bubble = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(BUBBLE);
        return bubble != null && server.structureManager().getStructureWithPieceAt(pos, bubble).isValid();
    }

    /** Not allowed here? Tells the player why, unless quiet. */
    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild || !inBubble(player.level(), pos)) {
            return false;
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.onderwater.no_build").withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (denied(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && inBubble(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /**
     * Using an item on a block (buckets, flint and steel, bone meal, axes...): not here. The block itself still reacts
     * (doors, chests, the shells), so a full hand never stops you from taking a pearl.
     */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty()) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        ItemStack stack = event.getItemStack();
        boolean quiet = !(stack.getItem() instanceof net.minecraft.world.item.BlockItem) && !(stack.getItem() instanceof BucketItem);
        if (denied(event.getEntity(), event.getPos(), quiet) || denied(event.getEntity(), event.getPos().relative(face), quiet)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are also used "in the air" (scooping up the sea, or pouring into the dome). */
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition(), false)) {
            event.setCanceled(true);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inBubble(server, pos));
        }
    }

    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inBubble(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private OnderwaterProtection() {
    }
}
