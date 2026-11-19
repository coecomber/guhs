package nl.juiced.guhs.feature.guhwinter.kern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * 1.10.10 Guhwinter: from 3 Jan (AFGELOPEN) the event things melt.
 * <ul>
 *   <li>items with the tag {@code guhs:guhwinter_smelt}: out of the inventory (login, every 5 s, also the cursor), out of
 *       every container a player opens, and they never land on the ground (a message per kind of item);</li>
 *   <li>placed blocks with the tag {@code guhs:guhwinter_smelt_blok}: in the chunks around every player (every 5 s, 4
 *       chunks around) and in every chunk that loads (also force-loaded ones, where blocks tick). Sections whose
 *       palette doesn't hold such a block are skipped, so this costs next to nothing.</li>
 * </ul>
 * Pepernoten don't melt. What sits inside a shulker box or bundle melts when it comes out.
 */
public final class Smelten {
    /** Chunks around a player that are looked at. */
    public static final int STRAAL = 4;
    private static final int CHUNKS_PER_TICK = 8;
    private static final Queue<Wacht> WACHT = new ConcurrentLinkedQueue<>();
    /** (KernEvents: updated every second) is it AFGELOPEN? Read on chunk load, where the SavedData isn't asked. */
    static volatile boolean afgelopen;

    private record Wacht(ResourceKey<Level> dim, long chunk) {
    }

    private Smelten() {
    }

    static boolean smelt(ItemStack stack) {
        return !stack.isEmpty() && stack.is(KernRegistry.SMELT);
    }

    // --- items -----------------------------------------------------------------------------------------------------------

    /** The player's inventory (armour and off hand included) and cursor. Returns how many items melted. */
    public static int speler(ServerPlayer p) {
        Map<Item, ItemStack> weg = new LinkedHashMap<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (smelt(s)) {
                weg.putIfAbsent(s.getItem(), s.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack hand = p.containerMenu.getCarried();
        if (smelt(hand)) {
            weg.putIfAbsent(hand.getItem(), hand.copy());
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        if (!weg.isEmpty()) {
            inv.setChanged();
            p.containerMenu.broadcastChanges();
        }
        meld(p, weg);
        return weg.size();
    }

    /** Every slot of a menu the player opened (the container and their own inventory). */
    public static int menu(ServerPlayer p, AbstractContainerMenu menu) {
        Map<Item, ItemStack> weg = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            ItemStack s = slot.getItem();
            if (smelt(s)) {
                weg.putIfAbsent(s.getItem(), s.copy());
                slot.set(ItemStack.EMPTY);
            }
        }
        if (!weg.isEmpty()) {
            menu.broadcastChanges();
        }
        meld(p, weg);
        return weg.size();
    }

    private static void meld(ServerPlayer p, Map<Item, ItemStack> weg) {
        if (weg.isEmpty()) {
            return;
        }
        for (ItemStack s : weg.values()) {
            p.sendSystemMessage(Component.translatable("guhwinter.kern.smelt.item", s.getHoverName()).withStyle(ChatFormatting.AQUA));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.35f, 1.7f);
        p.level().sendParticles(ParticleTypes.FALLING_WATER, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.35, 0.4, 0.35, 0);
    }

    static void opContainer(PlayerContainerEvent.Open event) {
        if (afgelopen && event.getEntity() instanceof ServerPlayer p) {
            menu(p, event.getContainer());
        }
    }

    /** A melting item never lands on the ground (dropped, from a broken chest, from a mob...). */
    static void opEntiteit(EntityJoinLevelEvent event) {
        if (afgelopen && !event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item && smelt(item.getItem())) {
            event.setCanceled(true);
        }
    }

    // --- blocks ----------------------------------------------------------------------------------------------------------

    /** Is there any block with the tag at all (an empty tag: no block work)? */
    static boolean blokkenBestaan() {
        return BuiltInRegistries.BLOCK.getTagOrEmpty(KernRegistry.SMELT_BLOK).iterator().hasNext();
    }

    static void opChunk(ChunkEvent.Load event) {
        if (afgelopen && event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk) {
            WACHT.add(new Wacht(level.dimension(), event.getChunk().getPos().pack()));   // (never change blocks while a chunk loads)
        }
    }

    /** (server tick, AFGELOPEN) a few chunks that loaded. */
    static void wachtrij(MinecraftServer server) {
        if (!afgelopen) {
            WACHT.clear();
            return;
        }
        if (!blokkenBestaan()) {
            WACHT.clear();
            return;
        }
        for (int n = 0; n < CHUNKS_PER_TICK && !WACHT.isEmpty(); n++) {
            Wacht w = WACHT.poll();
            ServerLevel level = server.getLevel(w.dim());
            if (level != null) {
                ChunkPos c = ChunkPos.unpack(w.chunk());
                LevelChunk chunk = level.getChunkSource().getChunkNow(c.x(), c.z());
                if (chunk != null) {
                    chunk(level, chunk);
                }
            }
        }
    }

    /** (every 5 s, AFGELOPEN) the chunks around every player. Returns how many blocks melted. */
    public static int rondSpelers(MinecraftServer server) {
        if (!blokkenBestaan()) {
            return 0;
        }
        int n = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel level = p.level();
            ChunkPos c = p.chunkPosition();
            for (int dx = -STRAAL; dx <= STRAAL; dx++) {
                for (int dz = -STRAAL; dz <= STRAAL; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(c.x() + dx, c.z() + dz);
                    if (chunk != null) {
                        n += chunk(level, chunk);
                    }
                }
            }
        }
        return n;
    }

    /** Every melting block of this chunk goes (water stays where it was waterlogged). Returns how many. */
    public static int chunk(ServerLevel level, LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        List<BlockPos> weg = new ArrayList<>();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(Smelten::smeltBlok)) {
                continue;
            }
            int y0 = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (smeltBlok(section.getBlockState(x, y, z))) {
                            weg.add(new BlockPos(x0 + x, y0 + y, z0 + z));
                        }
                    }
                }
            }
        }
        for (BlockPos pos : weg) {
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0);
        }
        return weg.size();
    }

    static boolean smeltBlok(BlockState state) {
        return state.is(KernRegistry.SMELT_BLOK);
    }
}
