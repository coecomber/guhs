package nl.juiced.guhs.feature.knuffeldal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.feature.knus.PleinSlot;

/**
 * The Knuffeldal towns can't be broken or built in (every piece: the plein, the houses and the building slots of the
 * other 2.8 features): no breaking, no placing blocks, no buckets, no explosions, no griefing mobs. Doors, chests, the
 * seasonal flower boxes, the feestbuffet and the characters all still work. Players in creative mode may change it.
 * (Fire and fluids from outside: see feature.Protected, which asks {@link #inStadje}.)
 */
public final class KnuffeldalProtection {
    static void register() {
        NeoForge.EVENT_BUS.addListener(KnuffeldalProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(KnuffeldalProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(KnuffeldalProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(KnuffeldalProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(KnuffeldalProtection::onMobGriefing);
    }

    /** Is this spot in a piece of a Knuffeldal town (server side)? */
    public static boolean inStadje(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && PleinSlot.inStadje(server, pos);
    }

    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild || !inStadje(player.level(), pos)) {
            return false;
        }
        if (!quiet) {
            player.displayClientMessage(Component.translatable("gui.guhs.knuffeldal.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE), true);
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
        if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && inStadje(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /**
     * Blocks, buckets, bone meal and the like on a block of the town: the item isn't used (the block itself still reacts:
     * doors, chests, the flower boxes...).
     */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide || stack.isEmpty()) {
            return;
        }
        boolean building = stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem
                || stack.is(net.minecraft.world.item.Items.BONE_MEAL);
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
            event.getAffectedBlocks().removeIf(pos -> PleinSlot.inStadje(server, pos));
        }
    }

    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inStadje(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private KnuffeldalProtection() {
    }
}
