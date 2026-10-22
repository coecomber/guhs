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
 * 1.6.2: takes a picked-up guh or maatje (an item holding it, found by its band id) out of wherever it is, for "Roep naar
 * mij" and the Knabbelportaal: pockets and ender chests of online players, a chest / barrel / any container block (shulker
 * boxes inside it too), a Bank Guh, a guh's backpack, the ground, a Guh Wheel. Every method removes the item (or the
 * wheel's guh) and returns it (one item), or null when it isn't there. The caller uses it: the item is gone first, so the
 * creature never exists twice.
 */
public final class UitItem {
    /** How far around the remembered spot we look (a chest moved, a backpack guh walked a bit, an item rolled). */
    private static final int OM = 2, OM_RUGZAK = 24, OM_GROND = 8;

    private UitItem() {
    }

    public static boolean isDeze(ItemStack s, UUID id) {
        GuhVolger.Wie w = GuhVolger.wie(s);
        return w != null && w.id().equals(id);
    }

    /** One item out of a slot's stack (the rest stays). */
    private static ItemStack een(ItemStack s) {
        return s.copyWithCount(1);
    }

    /** From the pockets or ender chest of any online player (the owner first). */
    @Nullable
    public static ItemStack uitSpelers(MinecraftServer s, UUID eigenaar, UUID id) {
        List<ServerPlayer> spelers = new ArrayList<>(s.getPlayerList().getPlayers());
        spelers.sort((a, b) -> Boolean.compare(!a.getUUID().equals(eigenaar), !b.getUUID().equals(eigenaar)));
        for (ServerPlayer p : spelers) {
            ItemStack d = uitSpeler(p, id);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    /** From this player's pockets or ender chest. */
    @Nullable
    public static ItemStack uitSpeler(ServerPlayer p, UUID id) {
        ItemStack d = uit(p.getInventory(), id);
        if (d == null) {
            d = uit(p.getEnderChestInventory(), id);
        }
        if (d == null && isDeze(p.containerMenu.getCarried(), id)) {      // (held on the mouse in an open screen)
            d = een(p.containerMenu.getCarried());
            p.containerMenu.getCarried().shrink(1);
        }
        if (d != null) {
            p.containerMenu.broadcastChanges();
        }
        return d;
    }

    /** From the world around a remembered spot, by the kind of place "Waar is mijn guh?" last saw it. */
    @Nullable
    public static ItemStack uitWereld(ServerLevel level, BlockPos pos, PlekSoort soort, UUID id) {
        return switch (soort) {
            case GUHWIEL -> uitWiel(level, pos, id);
            case ITEM_BANK -> uitBank(level, pos, id);
            case ITEM_RUGZAK -> uitRugzak(level, pos, id);
            case ITEM_GROND -> uitGrond(level, pos, id);
            default -> {
                ItemStack d = uitBlokken(level, pos, id);
                yield d != null ? d : uitGrond(level, pos, id);
            }
        };
    }

    @Nullable
    public static ItemStack uitWiel(ServerLevel level, BlockPos pos, UUID id) {
        if (level.getBlockEntity(pos) instanceof GuhWheelBlockEntity wiel && wiel.hasGuh()) {
            CompoundTag data = wiel.guhData();
            if (data != null && id.equals(data.read("UUID", net.minecraft.core.UUIDUtil.CODEC).orElse(null))) {
                CompoundTag uit = wiel.takeOut();
                if (level.getBlockState(pos).hasProperty(GuhWheelBlock.RUNNING)) {
                    level.setBlock(pos, level.getBlockState(pos).setValue(GuhWheelBlock.RUNNING, false), 3);
                }
                return PickedUpGuhItem.of(uit);
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack uitBank(ServerLevel level, BlockPos pos, UUID id) {
        if (!(level.getBlockEntity(pos) instanceof BankGuhBlockEntity bank)) {
            return null;
        }
        for (var entry : bank.getStorage().snapshot().entries()) {
            ItemStack stack = entry.item();
            if (isDeze(stack, id)) {
                ItemStack uit = bank.getStorage().extract(stack, 1);
                if (!uit.isEmpty()) {
                    bank.setChanged();
                    return uit;
                }
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack uitRugzak(ServerLevel level, BlockPos pos, UUID id) {
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(OM_RUGZAK))) {
            ItemStack d = uit(g.getBackpack(), id);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack uitGrond(ServerLevel level, BlockPos pos, UUID id) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(OM_GROND), i -> isDeze(i.getItem(), id))) {
            ItemStack d = een(item.getItem());
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) {
                item.discard();
            }
            return d;
        }
        return null;
    }

    /** Any container block at or right around the spot. */
    @Nullable
    public static ItemStack uitBlokken(ServerLevel level, BlockPos pos, UUID id) {
        for (BlockPos b : BlockPos.betweenClosed(pos.offset(-OM, -OM, -OM), pos.offset(OM, OM, OM))) {
            BlockEntity be = level.getBlockEntity(b);
            if (be instanceof Container c) {
                ItemStack d = uit(c, id);
                if (d != null) {
                    return d;
                }
            } else if (be instanceof BankGuhBlockEntity) {
                ItemStack d = uitBank(level, b.immutable(), id);
                if (d != null) {
                    return d;
                }
            }
        }
        return null;
    }

    /** From a container: a slot holding it, or a shulker box (or other item with a container) holding it. */
    @Nullable
    public static ItemStack uit(@Nullable Container c, UUID id) {
        if (c == null) {
            return null;
        }
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (isDeze(s, id)) {
                ItemStack d = een(s);
                c.removeItem(i, 1);
                c.setChanged();
                return d;
            }
            ItemContainerContents inhoud = s.get(DataComponents.CONTAINER);
            if (inhoud != null) {
                List<ItemStack> lijst = new ArrayList<>(inhoud.allItemsCopyStream().toList());
                for (int j = 0; j < lijst.size(); j++) {
                    if (isDeze(lijst.get(j), id)) {
                        ItemStack d = een(lijst.get(j));
                        lijst.get(j).shrink(1);
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
