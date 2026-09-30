package nl.juiced.guhs.feature.race;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.world.ModDimensions;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * The guh racebaan can't be broken (like the verstopguh house): no breaking, building, buckets, fire or explosions, and
 * mobs don't grief it. Opening things is fine; players in creative mode may change it. Protected is the racebaan
 * structure in the Guhmension, and every track a Raceguh has found (so older tracks and the test track count too).
 */
public final class RaceProtection {
    private record Area(ResourceKey<Level> dimension, AABB box) {
    }

    private static final List<Area> TRACKS = new CopyOnWriteArrayList<>();
    /** The protected track structures (2.9: the Guh-Circuit joins the racebaan) and what a player is told there. */
    private static final java.util.Map<ResourceKey<Structure>, String> STRUCTURES = new java.util.concurrent.ConcurrentHashMap<>(
            java.util.Map.of(RaceTrack.STRUCTURE, "gui.guhs.race.no_build"));

    /** Protects another track structure (the circuit), with its own "no building here" message. */
    public static void protect(ResourceKey<Structure> structure, String message) {
        STRUCTURES.put(structure, message);
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(RaceProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(RaceProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(RaceProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(RaceProtection::onUseItem);
        NeoForge.EVENT_BUS.addListener(RaceProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(RaceProtection::onMobGriefing);
        nl.juiced.guhs.feature.Protected.add(RaceProtection::protectedAt);
    }

    /** A track found by a Raceguh (its area is protected from now on). */
    static void remember(Level level, AABB box) {
        Area area = new Area(level.dimension(), box);
        if (!TRACKS.contains(area)) {
            TRACKS.add(area);
        }
    }

    static void forgetAll() {
        TRACKS.clear();
    }

    public static boolean protectedAt(Level level, BlockPos pos) {
        return why(level, pos) != null;
    }

    /** The "no building" message if pos is protected, else null. */
    @javax.annotation.Nullable
    private static String why(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        for (Area area : TRACKS) {
            if (area.dimension() == level.dimension() && area.box().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return "gui.guhs.race.no_build";
            }
        }
        if (server.dimension() != ModDimensions.GUHMENSION) {
            return null;
        }
        for (var entry : STRUCTURES.entrySet()) {
            Structure structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(entry.getKey());
            if (structure != null && server.structureManager().getStructureAt(pos, structure).isValid()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Can this player change this block? (If not, they are told why.) */
    public static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || player.isCreative()) {
            return false;
        }
        String why = why(player.level(), pos);
        if (why == null) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable(why).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos()) : entity != null && protectedAt(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty()) {
            return;
        }
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(),
                event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    private static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide()) {
            event.getAffectedBlocks().removeIf(pos -> protectedAt(event.getLevel(), pos));
        }
    }

    private static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private RaceProtection() {
    }
}
