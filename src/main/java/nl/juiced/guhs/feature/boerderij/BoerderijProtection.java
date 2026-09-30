package nl.juiced.guhs.feature.boerderij;

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
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * The Guhboerderij can't be broken or built in: no breaking, no placing blocks, no buckets or bone meal on it, no
 * explosions, no griefing mobs. Everything of the farm still works: doors, gates, the chest, the voerbakken and
 * kippennestjes, and the moestuinbakken and bloempotten (plant, water, harvest). Creative players may change it.
 * (Fire and fluids from outside: feature.Protected asks {@link #inBoerderij}.)
 */
public final class BoerderijProtection {
    /** Extra protected boxes, only for the GameTests (the headless test world has no generated farm). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(BoerderijProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(BoerderijProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(BoerderijProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(BoerderijProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(BoerderijProtection::onMobGriefing);
    }

    /** Is this spot part of a Guhboerderij (server side)? */
    public static boolean inBoerderij(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        if (server.dimension() != nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
            return false;                                      // (it only generates in the Guhmensie)
        }
        Structure structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(BoerderijFeature.GUHBOERDERIJ);
        return structure != null && server.structureManager().getStructureAt(pos, structure).isValid();
    }

    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild || !inBoerderij(player.level(), pos)) {
            return false;
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.boerderij.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
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
        if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && inBoerderij(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Blocks, buckets and bone meal on a block of the farm: the item isn't used (the block itself still reacts). */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty()) {
            return;
        }
        boolean building = stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL);
        if (!building) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos(), true) || denied(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inBoerderij(server, pos));
        }
    }

    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inBoerderij(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private BoerderijProtection() {
    }
}
