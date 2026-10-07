package nl.juiced.guhs.feature.guhpixel;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The safe rules everywhere {@link Guhpixel#in} (1.3.2: and in the extra {@link #zone zones}): no breaking or placing blocks, no explosions or mob griefing, players
 * take no damage (only /kill gets through), food, health and air stay what they were, items cannot be tossed (they come
 * straight back) or picked up by others, nobody attacks anything, and a block can only be right-clicked when it is in the
 * block tag {@code guhs:guhpixel_bruikbaar} (lobby things) or the player's game says so. A game may allow a specific thing
 * ({@link Sessie#magBreken} and friends). Creative players (instabuild) pass everything.
 * <p>
 * Also here: falling into the void puts you back (the arena start, else the plaza), a player found in guhpixel outside
 * the lobby without a game is put on the spawn point, a bed never sets the spawn, dying costs nothing (the inventory and
 * XP come back at respawn), and the life cycle hooks of {@link Sessies}, {@link Arenas}, {@link Lobby} and {@link Klok}.
 */
public final class Regels {
    public static final TagKey<Block> BRUIKBAAR = TagKey.create(Registries.BLOCK, Guhs.id("guhpixel_bruikbaar"));
    private static final String DOOD = "guhs_guhpixel_dood";

    /**
     * 1.3.2: other places where the same safe rules count (the rooms inside the Guhhuisjes, feature/huisje/Binnen). Such a
     * zone has no games: nothing is broken, placed, hurt, tossed or lost there. Guhpixel itself is unchanged.
     */
    private static final java.util.List<java.util.function.BiPredicate<net.minecraft.world.level.Level, BlockPos>> ZONES =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void zone(java.util.function.BiPredicate<net.minecraft.world.level.Level, BlockPos> zone) {
        ZONES.add(zone);
    }

    /** Guhpixel ({@link Guhpixel#in}) or one of the extra {@link #zone zones}. */
    private static boolean veilig(net.minecraft.world.level.Level level, BlockPos pos) {
        if (Guhpixel.in(level, pos)) {
            return true;
        }
        for (var zone : ZONES) {
            if (zone.test(level, pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean veilig(@javax.annotation.Nullable Entity e) {
        return e != null && veilig(e.level(), e.blockPosition());
    }

    private static boolean vrij(Player p) {
        return p.getAbilities().instabuild;
    }

    private static void nee(Player p) {
        if (p instanceof ServerPlayer sp && (sp.tickCount & 7) == 0) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.regels.niet_bouwen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- blocks ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        Player p = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || !veilig(level, event.getPos()) || vrij(p)) {
            return;
        }
        Sessie s = p instanceof ServerPlayer sp ? Sessies.van(sp) : null;
        if (s == null || !s.magBreken((ServerPlayer) p, event.getPos(), event.getState())) {
            event.setCanceled(true);
            nee(p);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !veilig(level, event.getPos())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (vrij(sp)) {
                return;
            }
            Sessie s = Sessies.van(sp);
            if (s != null && s.magPlaatsen(sp, event.getPos(), event.getPlacedBlock())) {
                return;
            }
            nee(sp);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        // (server only: the client cannot know what a game allows; the server answers with the real blocks)
        if (!(event.getEntity() instanceof ServerPlayer sp) || !veilig(event.getLevel(), event.getPos()) || vrij(sp)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Sessie s = Sessies.van(sp);
        if (!state.is(BRUIKBAAR) && (s == null || !s.magGebruiken(sp, pos, state))) {
            event.setUseBlock(TriState.FALSE);
        }
        if (s == null) {
            event.setUseItem(TriState.FALSE);   // (no buckets, boats, spawn eggs, picked-up guhs... on the plaza)
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer sp && !vrij(sp) && Guhpixel.in(sp)) {
            Sessie s = Sessies.van(sp);
            if (s != null && !s.magEntiteit(sp, event.getTarget())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> veilig(event.getLevel(), pos));
        event.getAffectedEntities().removeIf(Regels::veilig);
    }

    @SubscribeEvent
    public static void onGriefing(EntityMobGriefingEvent event) {
        if (veilig(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    // --- players -----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!veilig(event.getEntity())) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            if (!event.getSource().is(DamageTypes.GENERIC_KILL)) {
                event.setCanceled(true);
            }
        } else if (event.getSource().getEntity() instanceof Player p && !vrij(p)) {
            event.setCanceled(true);   // (nobody hurts anything here)
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (veilig(event.getEntity()) && !vrij(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        Player p = event.getPlayer();
        if (!(p instanceof ServerPlayer) || !veilig(p) || vrij(p)) {
            return;
        }
        ItemStack stack = event.getEntity().getItem().copy();
        if (p.getInventory().add(stack) && stack.isEmpty()) {
            event.setCanceled(true);   // (straight back into the pockets)
            p.inventoryMenu.broadcastChanges();
        } else {
            // the pockets are full (it came off the cursor): it lies at the player's feet and only they can take it
            event.getEntity().setItem(stack);
            event.getEntity().setTarget(p.getUUID());
            event.getEntity().setDeltaMovement(Vec3.ZERO);
            event.getEntity().setPos(p.getX(), p.getY() + 0.2, p.getZ());
            event.getEntity().setNoPickUpDelay();
            event.getEntity().setUnlimitedLifetime();
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player p = event.getPlayer();
        if (veilig(p) && !vrij(p)) {
            Entity eigenaar = event.getItemEntity().getOwner();
            if (eigenaar != null && eigenaar != p) {
                event.setCanPickup(TriState.FALSE);
            }
        }
    }

    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (!event.isForced() && event.getSpawnLevel() == Guhpixel.DIM) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if ((p.tickCount + p.getId()) % 20 == 0 && Sessies.van(p) == null) {
            Kluis.geefRest(p);   // (wherever the player is: what did not fit when the inventory came back)
        }
        if (!veilig(p)) {
            return;
        }
        if (!p.isAlive()) {
            // dead (only /kill gets through): NO health floor, or the player is alive again one tick later and vanilla ignores
            // the respawn that gives the inventory back
            Minigames.forget(p);
            return;
        }
        if (!p.isCreative() && !p.isSpectator()) {
            Minigames.keep(p);   // (food, health and air never drop below what they were on arrival: no hunger, no free healing)
        }
        if (!Guhpixel.echt(p.level()) || p.isSpectator() || !p.isAlive()) {
            return;
        }
        Sessie s = Sessies.van(p);
        if (p.getY() < Guhpixel.VOID_Y) {
            Vec3 naar = s != null ? s.terugzetPlek(p) : LobbyPlek.SPAWN.pos();
            float yaw = s != null ? s.terugzetYaw(p) : LobbyPlek.SPAWN.yaw();
            terug(p, naar, yaw);
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.regels.void").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (s == null && !p.isCreative() && (p.tickCount + p.getId()) % 20 == 0 && !Guhpixel.inLobby(p) && !Lobby.herbouwBezig()) {
            // "nowhere": outside the lobby without a game
            if (Lobby.zorg((ServerLevel) p.level())) {
                terug(p, LobbyPlek.SPAWN.pos(), LobbyPlek.SPAWN.yaw());
            }
        }
    }

    private static void terug(ServerPlayer p, Vec3 naar, float yaw) {
        p.stopRiding();
        p.teleportTo((ServerLevel) p.level(), naar.x, naar.y, naar.z, Set.of(), yaw, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
    }

    // --- dying costs nothing -------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && (veilig(p) || Sessies.van(p) != null)) {
            Minigames.forget(p);   // (no health floor for the dead: see onPlayerTick)
            Sessies.verlaat(p, Vertrek.DOOD);
            if (Kluis.bewaar(p)) {   // (nothing drops: the whole inventory waits in the safe until the respawn)
                GuhQuests.saved(p).putBoolean(DOOD, true);
            }
        }
    }

    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && GuhQuests.saved(p).getBooleanOr(DOOD, false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer nieuw && event.getOriginal() instanceof ServerPlayer oud
                && GuhQuests.saved(oud).getBooleanOr(DOOD, false)) {
            nieuw.experienceLevel = oud.experienceLevel;
            nieuw.totalExperience = oud.totalExperience;
            nieuw.experienceProgress = oud.experienceProgress;
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && GuhQuests.saved(p).getBooleanOr(DOOD, false)) {
            GuhQuests.saved(p).remove(DOOD);
            Kluis.herstel(p);
        }
    }

    // --- life cycle ----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            if (p.isDeadOrDying()) {
                // logged out (or the server stopped) on the death screen: nothing goes onto the dead body, a respawn copies no
                // inventory. The safe stays shut and opens at the respawn.
                if (Kluis.heeft(p)) {
                    GuhQuests.saved(p).putBoolean(DOOD, true);
                }
                return;
            }
            GuhQuests.saved(p).remove(DOOD);
            Sessies.opLogin(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Sessies.verlaat(p, Vertrek.UITGELOGD);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            if (event.getFrom() == Guhpixel.DIM) {
                Sessies.verlaat(p, Vertrek.DIMENSIE);
                Toegang.verlaten(p);
            }
            if (event.getTo() == Guhpixel.DIM) {
                Minigames.forget(p);
                GuhpixelPayloads.hud(p);
            }
        }
    }

    /** No game survives a restart, so whatever loads from disk in the arena region is left over from a crash. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && !(event.getEntity() instanceof Player) && event.getLevel().dimension() == Guhpixel.DIM
                && event.getEntity().getX() >= Guhpixel.ARENA_X0 - 64) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Klok.laad(server);
        Arenas.opStart(server);
        Lobby.opStart(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Sessies.stopAlles(Vertrek.SERVER_STOP);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Arenas.opStop();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Sessies.tick(server);
        Lobby.tick(server);
        LobbyNpcs.tick(server);
    }

    private Regels() {
    }
}
