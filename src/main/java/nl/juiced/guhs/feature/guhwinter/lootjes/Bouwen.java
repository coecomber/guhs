package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;

/**
 * Building on your lootjes plot (design R5, R10: open from the draw on 2 Dec until 5 Dec 18:00).
 * <ul>
 *   <li>Walk into your own plot: creative, your survival things stashed ({@link Bouwkist}). Walk out (through the gate, on the
 *       ground): everything back. Flying is kept to the plot (+1 block around it, +1 above).</li>
 *   <li>You place and break only inside your own 7x7x7 room ({@link GuhwinterZone#uitzondering}), only blocks that may be in a
 *       gift ({@link BouwRegels#magBlok}); items on blocks only the decorating tools; no other items used at all (no spawn
 *       eggs, buckets, pearls...); no menus or containers opened; no beds slept in; nothing hit; nothing thrown away, and
 *       no item or other loose entity may appear in a plot.</li>
 *   <li>Everybody else (also the builder after the deadline) can look but never step in: whoever stands in a plot that
 *       isn't theirs (or not now) is put back on the street. Ops in creative may (to help or fix).</li>
 * </ul>
 */
public final class Bouwen {
    private static final String T = Lootjes.T;
    private static final Map<UUID, Long> GEZEGD = new HashMap<>();

    private Bouwen() {
    }

    /** (GuhwinterFeature) the plaza lets a builder change their own room. */
    static boolean uitzondering(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer p) || !Bouwkist.bouwt(p)) {
            return false;
        }
        Bouwvelden.Veld v = Bouwvelden.veld(Bouwkist.veld(p));
        return v != null && v.bevat(pos) && Guhwinter.bouwenOpen(p.level().getServer());
    }

    // =====================================================================================================================
    // the tick: in, out, keep flying inside, others out
    // =====================================================================================================================

    static void tick(MinecraftServer server) {
        ServerLevel plein = server.getLevel(GuhwinterZone.DIMENSIE);
        AABB straat = Bouwvelden.straat();
        boolean bezig = Guhwinter.bezig(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Bouwkist.bouwt(p)) {
                bouwer(server, p);
            } else if (bezig && straat != null && p.level() == plein && straat.contains(p.position())) {
                bezoeker(server, p);
            }
        }
    }

    private static void bouwer(MinecraftServer server, ServerPlayer p) {
        int i = Bouwkist.veld(p);
        Bouwvelden.Veld v = Bouwvelden.veld(i);
        LootjesData d = LootjesData.get(server);
        boolean mag = v != null && p.level().dimension() == GuhwinterZone.DIMENSIE && Guhwinter.bouwenOpen(server) && d.veldVan(p.getUUID()) == i
                && p.isAlive() && !p.isSpectator();
        if (!mag) {
            Bouwkist.herstel(p);
            p.sendSystemMessage(Component.translatable(T + "bouwen.tijd_om").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        AABB r = v.ruimte();
        Vec3 pos = p.position();
        double minX = r.minX - 1, maxX = r.maxX + 1, minZ = r.minZ - 1, maxZ = r.maxZ + 1, maxY = r.maxY + 1 - p.getBbHeight();
        boolean buiten = pos.x < minX || pos.x > maxX || pos.z < minZ || pos.z > maxZ;
        double ver = Math.max(Math.max(minX - pos.x, pos.x - maxX), Math.max(minZ - pos.z, pos.z - maxZ));
        if (buiten && (pos.y < r.minY + 1.0 || ver > 4 || pos.y < r.minY - 2)) {
            // walked out (through the gate), or taken away (a teleport): everything back
            Bouwkist.herstel(p);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.7f, 1.1f);
            p.sendSystemMessage(Component.translatable(T + "bouwen.uit").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        double x = Math.max(minX + 0.3, Math.min(maxX - 0.3, pos.x));
        double z = Math.max(minZ + 0.3, Math.min(maxZ - 0.3, pos.z));
        double y = Math.min(maxY, pos.y);
        if (x != pos.x || y != pos.y || z != pos.z) {
            p.connection.teleport(x, y, z, p.getYRot(), p.getXRot());
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
            zegZelden(p, "bouwen.grens");
        }
    }

    private static void bezoeker(MinecraftServer server, ServerPlayer p) {
        Bouwvelden.Veld v = Bouwvelden.voeten(p.position());
        if (v == null || p.isSpectator()) {
            return;
        }
        if (p.isCreative() && p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;   // (an op helping out)
        }
        LootjesData d = LootjesData.get(server);
        UUID eigenaar = d.eigenaarVanVeld(v.i());
        GameType m = p.gameMode.getGameModeForPlayer();
        if (p.getUUID().equals(eigenaar) && Guhwinter.bouwenOpen(server) && (m == GameType.SURVIVAL || m == GameType.ADVENTURE)) {
            Bouwkist.begin(p, v.i());
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
            p.sendSystemMessage(Component.translatable(T + "bouwen.in", d.naam(d.lootjeVan(p.getUUID()))).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.sendSystemMessage(Component.translatable(T + "bouwen.regels").withStyle(ChatFormatting.GRAY));
            return;
        }
        // not yours (or not now): back to the street
        p.connection.teleport(v.uitgang().x, v.uitgang().y, v.uitgang().z, v.uitgangYaw(), p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        String waarom = p.getUUID().equals(eigenaar) ? (Guhwinter.bouwenOpen(server) ? "bouwen.modus" : "bouwen.dicht")
                : eigenaar == null ? "bouwen.vrij" : "bouwen.niet_jouw";
        zegZelden(p, waarom);
    }

    private static void zegZelden(ServerPlayer p, String sleutel) {
        long nu = p.level().getGameTime();
        Long laatst = GEZEGD.get(p.getUUID());
        if (laatst == null || nu - laatst > 40) {
            GEZEGD.put(p.getUUID(), nu);
            p.sendOverlayMessage(Component.translatable(T + sleutel).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Everyone who builds back to normal (before a square is stamped, at a reset...). */
    static int iedereenTerug(MinecraftServer server) {
        int n = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Bouwkist.herstel(p)) {
                n++;
            }
        }
        return n;
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Bouwkist.herstel(p);
            GEZEGD.remove(p.getUUID());
        }
    }

    static void opReis(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Bouwkist.herstel(p);
        }
    }

    static void opDood(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Bouwkist.herstel(p);
        }
    }

    /** An op switching a builder's game mode: first everything back (then the new mode applies as usual). */
    static void opModus(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!Bouwkist.bezig && event.getEntity() instanceof ServerPlayer p && Bouwkist.bouwt(p) && event.getNewGameMode() != GameType.CREATIVE) {
            GameType nieuw = event.getNewGameMode();
            Bouwkist.herstel(p);
            event.setNewGameMode(nieuw);
        }
    }

    static void opGooien(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && Bouwkist.bouwt(p)) {
            event.setCanceled(true);
        }
    }

    /** No loose things in a plot: items, XP, mobs, projectiles, carts, boats, armour stands, frames, TNT. */
    static void opEntiteit(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != GuhwinterZone.DIMENSIE) {
            return;
        }
        if (!(e instanceof ItemEntity || e instanceof ExperienceOrb || e instanceof Mob || e instanceof Projectile || e instanceof VehicleEntity
                || e instanceof ArmorStand || e instanceof HangingEntity || e instanceof PrimedTnt)) {
            return;
        }
        if (nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcs.isNpc(e)) {
            return;
        }
        Vec3 p = e.position();
        for (Bouwvelden.Veld v : Bouwvelden.alle()) {
            if (v.ruimte().inflate(1.5).contains(p)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** A builder places only allowed blocks, only inside their own room (multi-blocks: every part). */
    static void opPlaats(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Bouwkist.bouwt(p)) {
            return;
        }
        Bouwvelden.Veld v = Bouwvelden.veld(Bouwkist.veld(p));
        boolean goed = v != null && v.bevat(event.getPos()) && BouwRegels.magBlok(event.getPlacedBlock());
        if (goed && event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot s : multi.getReplacedBlockSnapshots()) {
                goed &= v.bevat(s.getPos()) && BouwRegels.magBlok(s.getLevel().getBlockState(s.getPos()));
            }
        }
        if (!goed) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable(T + (v != null && v.bevat(event.getPos()) ? "bouwen.mag_niet" : "bouwen.buiten_veld"),
                    event.getPlacedBlock().getBlock().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Right-click on a block while building: only the decorating tools; no menus, containers or beds. */
    static void opKlikBlok(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Bouwkist.bouwt(p)) {
            return;
        }
        if (!BouwRegels.magItemOpBlok(event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
            zegZelden(p, "bouwen.item");
        }
        BlockState s = event.getLevel().getBlockState(event.getPos());
        if (s.getBlock() instanceof BedBlock || s.getMenuProvider(event.getLevel(), event.getPos()) != null
                || event.getLevel().getBlockEntity(event.getPos()) instanceof Container) {
            event.setUseBlock(TriState.FALSE);
            if (!(event.getItemStack().getItem() instanceof BlockItem)) {
                zegZelden(p, "bouwen.geen_kist");
            }
        }
    }

    /** No item used in the air while building (pearls, eggs, potions, spawn eggs, buckets...). */
    static void opKlikItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && Bouwkist.bouwt(p) && !(event.getItemStack().getItem() instanceof BlockItem)) {
            event.setCanceled(true);
            zegZelden(p, "bouwen.item");
        }
    }

    static void opEntiteitKlik(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer p && Bouwkist.bouwt(p)) {
            event.setCanceled(true);
        }
    }

    static void opEntiteitKlikPrecies(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer p && Bouwkist.bouwt(p)) {
            event.setCanceled(true);
        }
    }

    static void opSlaan(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Bouwkist.bouwt(p)) {
            event.setCanceled(true);
        }
    }

    static void vergeet() {
        GEZEGD.clear();
    }
}
