package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.item.PickedUpGuhItem;

/**
 * 1.6.2: takes a picked-up guh (found by its UUID) out of wherever it is, for "Roep naar mij": pockets and ender chests of
 * online players, a chest / barrel / any container block (shulker boxes inside it too), a Bank Guh, a guh's backpack, the
 * ground, a Guh Wheel. Every method removes the item (or the wheel's guh) and returns the guh's entity data, or null when
 * it isn't there. The caller makes the guh appear: the item is gone first, so it never exists twice.
 */
final class UitItem {
    /** How far around the remembered spot we look (a chest moved, a backpack guh walked a bit, an item rolled). */
    private static final int OM = 2, OM_RUGZAK = 24, OM_GROND = 8;

    private UitItem() {
    }

    static boolean isDeze(ItemStack s, UUID id) {
        GuhVolger.Wie w = GuhVolger.wie(s);
        return w != null && w.guh() && w.id().equals(id);
    }

    /** From the pockets or ender chest of any online player (the owner first). */
    @Nullable
    static CompoundTag uitSpelers(MinecraftServer s, UUID eigenaar, UUID id) {
        List<ServerPlayer> spelers = new ArrayList<>(s.getPlayerList().getPlayers());
        spelers.sort((a, b) -> Boolean.compare(!a.getUUID().equals(eigenaar), !b.getUUID().equals(eigenaar)));
        for (ServerPlayer p : spelers) {
            CompoundTag d = uitSpeler(p, id);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    /** From this player's pockets or ender chest. */
    @Nullable
    static CompoundTag uitSpeler(ServerPlayer p, UUID id) {
        CompoundTag d = uit(p.getInventory(), id);
        if (d == null) {
            d = uit(p.getEnderChestInventory(), id);
        }
        if (d == null && isDeze(p.containerMenu.getCarried(), id)) {      // (held on the mouse in an open screen)
            d = PickedUpGuhItem.guhData(p.containerMenu.getCarried());
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        if (d != null) {
            p.containerMenu.broadcastChanges();
        }
        return d;
    }

    /** From the world around a remembered spot, by the kind of place "Waar is mijn guh?" last saw it. */
    @Nullable
    static CompoundTag uitWereld(ServerLevel level, BlockPos pos, PlekSoort soort, UUID id) {
        return switch (soort) {
            case GUHWIEL -> uitWiel(level, pos, id);
            case ITEM_BANK -> uitBank(level, pos, id);
            case ITEM_RUGZAK -> uitRugzak(level, pos, id);
            case ITEM_GROND -> uitGrond(level, pos, id);
            default -> {
                CompoundTag d = uitBlokken(level, pos, id);
                yield d != null ? d : uitGrond(level, pos, id);
            }
        };
    }

    @Nullable
    static CompoundTag uitWiel(ServerLevel level, BlockPos pos, UUID id) {
        if (level.getBlockEntity(pos) instanceof GuhWheelBlockEntity wiel && wiel.hasGuh()) {
            CompoundTag data = wiel.guhData();
            if (data != null && id.equals(data.read("UUID", net.minecraft.core.UUIDUtil.CODEC).orElse(null))) {
                CompoundTag uit = wiel.takeOut();
                if (level.getBlockState(pos).hasProperty(GuhWheelBlock.RUNNING)) {
                    level.setBlock(pos, level.getBlockState(pos).setValue(GuhWheelBlock.RUNNING, false), 3);
                }
                return uit;
            }
        }
        return null;
    }

    @Nullable
    static CompoundTag uitBank(ServerLevel level, BlockPos pos, UUID id) {
        if (!(level.getBlockEntity(pos) instanceof BankGuhBlockEntity bank)) {
            return null;
        }
        for (var entry : bank.getStorage().snapshot().entries()) {
            ItemStack stack = entry.item();
            if (isDeze(stack, id)) {
                ItemStack uit = bank.getStorage().extract(stack, 1);
                if (!uit.isEmpty()) {
                    bank.setChanged();
                    return PickedUpGuhItem.guhData(uit);
                }
            }
        }
        return null;
    }

    @Nullable
    static CompoundTag uitRugzak(ServerLevel level, BlockPos pos, UUID id) {
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(OM_RUGZAK))) {
            CompoundTag d = uit(g.getBackpack(), id);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    @Nullable
    static CompoundTag uitGrond(ServerLevel level, BlockPos pos, UUID id) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(OM_GROND), i -> isDeze(i.getItem(), id))) {
            CompoundTag d = PickedUpGuhItem.guhData(item.getItem());
            item.discard();
            return d;
        }
        return null;
    }

    /** Any container block at or right around the spot. */
    @Nullable
    static CompoundTag uitBlokken(ServerLevel level, BlockPos pos, UUID id) {
        for (BlockPos b : BlockPos.betweenClosed(pos.offset(-OM, -OM, -OM), pos.offset(OM, OM, OM))) {
            BlockEntity be = level.getBlockEntity(b);
            if (be instanceof Container c) {
                CompoundTag d = uit(c, id);
                if (d != null) {
                    return d;
                }
            } else if (be instanceof BankGuhBlockEntity) {
                CompoundTag d = uitBank(level, b.immutable(), id);
                if (d != null) {
                    return d;
                }
            }
        }
        return null;
    }

    /** From a container: a slot holding it, or a shulker box (or other item with a container) holding it. */
    @Nullable
    static CompoundTag uit(@Nullable Container c, UUID id) {
        if (c == null) {
            return null;
        }
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (isDeze(s, id)) {
                CompoundTag d = PickedUpGuhItem.guhData(s);
                c.removeItem(i, 1);
                c.setChanged();
                return d;
            }
            ItemContainerContents inhoud = s.get(DataComponents.CONTAINER);
            if (inhoud != null) {
                List<ItemStack> lijst = new ArrayList<>(inhoud.allItemsCopyStream().toList());
                for (int j = 0; j < lijst.size(); j++) {
                    if (isDeze(lijst.get(j), id)) {
                        CompoundTag d = PickedUpGuhItem.guhData(lijst.get(j));
                        lijst.set(j, ItemStack.EMPTY);
                        s.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(lijst));
                        c.setChanged();
                        return d;
                    }
                }
            }
        }
        return null;
    }
}
