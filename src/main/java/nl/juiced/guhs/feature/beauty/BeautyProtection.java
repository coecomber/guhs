package nl.juiced.guhs.feature.beauty;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Keeps the beauty theatre and its shows safe (the verstopguh approach): nobody breaks, builds, spills or blows up
 * anything in the theatre (creative players may); the model and the jury can't be fed, leashed, named or dressed by
 * hand; the performer can't get hurt; a show ends when its performer leaves (logout, death, other dimension); leftovers of
 * a show that ended without them (a crash) clean themselves up; and no wild guhs wander in to steal the show.
 */
public final class BeautyProtection {
    public static final ResourceKey<Structure> THEATRE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_beauty_theater"));

    /** Is this spot inside a beauty theatre? */
    public static boolean inTheatre(ServerLevel level, BlockPos pos) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return false;
        }
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(THEATRE);
        return structure != null && level.structureManager().getStructureAt(pos, structure).isValid();
    }

    static boolean protectedAt(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && inTheatre(server, pos);
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !protectedAt(player.level(), pos)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.beauty.no_build").withStyle(ChatFormatting.LIGHT_PURPLE), true);
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

    /**
     * Using an item on a block (buckets, flint and steel, axes...): not in the theatre, and the flowers stay in their pots.
     * Sitting on the benches and opening the loaner wardrobes is fine.
     */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock
                && denied(event.getEntity(), event.getPos())) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            return;
        }
        if (event.getItemStack().isEmpty()) {
            return;
        }
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(event.getFace() == null
                ? net.minecraft.core.Direction.UP : event.getFace()))) {
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

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server && server.dimension() == ModDimensions.GUHMENSION) {
            event.getAffectedBlocks().removeIf(pos -> inTheatre(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    // --- the model and the jury ----------------------------------------------------------------------------------------------

    /** Right-clicking the model opens the wardrobe (for the performer); the jury introduce themselves. Nothing else happens to them. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof GuhEntity guh) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (guh.getTags().contains(BeautyShow.MODEL_TAG) || BeautyShow.isModel(guh)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            BeautyShow show = BeautyShow.of(player);
            if (show != null && show.model() == guh && show.isDressing()) {
                show.openWardrobe(player);
            } else {
                player.displayClientMessage(Component.translatable("gui.guhs.beauty.model_hands_off").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        } else if (guh.getTags().contains(BeautyShow.AUDIENCE_TAG)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            GuhQuests.say(player, guh, "quest.guhs.beauty.publiek");
        } else if (guh.getTags().contains(BeautyShow.JURY_TAG)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            for (int i = 0; i < BeautyJury.JUDGES.size(); i++) {
                if (guh.getTags().contains(BeautyShow.JURY_TAG + (i + 1))) {
                    GuhQuests.say(player, guh, "quest.guhs.beauty.jury." + BeautyJury.JUDGES.get(i) + ".hello");
                }
            }
        }
    }

    // --- performers ---------------------------------------------------------------------------------------------------------

    /** No hurting the performer in the theatre (a fall off the stage, a stray arrow...). */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && BeautyShow.inShowArea(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BeautyShow.leave(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        BeautyShow.leave(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            BeautyShow.leave(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            BeautyShow.checkStale(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BeautyShow.endAll();
    }

    /**
     * Leftovers of a show that ended without them (a crash): a model goes home, a borrowed guh gets its own clothes and
     * owner back, a score card disappears.
     */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof GuhEntity guh) {
            if (guh.getTags().contains(BeautyShow.MODEL_TAG) && !BeautyShow.isModel(guh)) {
                event.setCanceled(true);
            } else if (guh.getPersistentData().contains(BeautyShow.LOAN) && !BeautyShow.isModel(guh)) {
                BeautyShow.giveBack(guh);
            }
        } else if (entity instanceof Display.TextDisplay && entity.getTags().contains(BeautyShow.SCORE_TAG) && !BeautyShow.isScoreCard(entity)) {
            event.setCanceled(true);
        }
    }

    /**
     * No wild guhs wander into the theatre by themselves (natural spawns; the guhmension spawner skips the theatre too).
     * A guh from a spawn egg, a command or breeding is welcome.
     */
    @SubscribeEvent
    public static void onSpawnCheck(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        if (event.getEntity() instanceof GuhEntity && (event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.NATURAL
                || event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION)
                && inTheatre(event.getLevel().getLevel(), event.getEntity().blockPosition())) {
            event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private BeautyProtection() {
    }
}
