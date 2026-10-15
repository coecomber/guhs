package nl.juiced.guhs.feature.einde2.geestenwereld;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.Protected;

/**
 * einde2, slice geestenwereld: nothing changes in the Geestenwereld and nobody gets hurt there (DESIGN 5: "Beschermd").
 * In the whole dimension (and a test box, {@link Geestenwereld#testGebied}): no breaking, no placing, no buckets or bone meal,
 * no explosions, no griefing mobs, no fire or fluids from outside ({@link Protected}); doors, the stones and the puzzle tiles
 * still work. Creative players may change it (to fix something). No damage (except /kill), no natural monster spawns, and
 * whoever falls into the void is put back at the arrival ({@link Geestenwereld#vangOp}).
 */
public final class GeestBescherming {
    private GeestBescherming() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opBreken);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opPlaatsen);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opBlokKlik);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opItemKlik);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opExplosie);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opGrief);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opSchade);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opSpawn);
        NeoForge.EVENT_BUS.addListener(GeestBescherming::opTick);
        Protected.add(Geestenwereld::in);
    }

    /** May this player not change this spot? (says why, unless quiet) */
    public static boolean nee(Player player, BlockPos pos, boolean stil) {
        if (player.getAbilities().instabuild || !Geestenwereld.in(player.level(), pos)) {
            return false;
        }
        if (!stil) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_geestenwereld.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    private static void opBreken(BreakBlockEvent event) {
        if (nee(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    private static void opPlaatsen(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? nee(player, event.getPos(), false)
                : entity != null && Geestenwereld.in(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void opBlokKlik(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL))) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (nee(event.getEntity(), event.getPos(), true) || nee(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    private static void opItemKlik(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem
                && nee(event.getEntity(), event.getEntity().blockPosition(), false)) {
            event.setCanceled(true);
        }
    }

    private static void opExplosie(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().removeIf(pos -> Geestenwereld.in(level, pos));
        }
    }

    private static void opGrief(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && Geestenwereld.in(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    private static void opSchade(LivingIncomingDamageEvent event) {
        if (Geestenwereld.doetGeenPijn(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** No monster comes by itself (the shadow spirits are residents: they are put there, not spawned). */
    private static void opSpawn(FinalizeSpawnEvent event) {
        EntitySpawnReason r = event.getSpawnType();
        if (event.getEntity() instanceof Enemy && event.getLevel().getLevel().dimension() == Geestenwereld.DIM
                && (r == EntitySpawnReason.NATURAL || r == EntitySpawnReason.CHUNK_GENERATION || r == EntitySpawnReason.PATROL
                || r == EntitySpawnReason.REINFORCEMENT)) {
            event.setSpawnCancelled(true);
        }
    }

    private static void opTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Geestenwereld.vangOp(p);
        }
    }
}
