package nl.juiced.guhs.feature.band;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModItems;

/**
 * "Waar is mijn guh?" (2.10): the last known place of every band guh and maatje, kept in {@link BandData} so the
 * Guhdex can tell you even when the guh is far away, an item in a chest, running in a Guh Wheel or asleep in its
 * huisje. Fundament keeps it current (loaded guhs every ~100 ticks and on leave/unload/death/dimension change,
 * picked-up items, the wheel, huisjes, riding, maatjes on the shoulder or as items); speelgoed sets GUHKAMER.
 */
public final class GuhVolger {
    /** A place that didn't change is only rewritten after this many ticks (its time stamp). */
    private static final long VERVERS = 1200;

    private GuhVolger() {
    }

    /** Sets where a guh (or maatje) of this owner is. Needs the running server ({@link Band#server}). */
    public static void zet(UUID eigenaar, UUID bandId, Plek plek) {
        MinecraftServer s = Band.server();
        if (s != null) {
            zet(s, eigenaar, bandId, plek, null, null);
        }
    }

    private static void zet(MinecraftServer s, UUID eigenaar, UUID bandId, Plek plek, @Nullable Boolean guh, @Nullable String soort) {
        BandData data = BandData.get(s);
        BandData.Rec r = data.rec(eigenaar, bandId);
        if (guh != null) {
            r.guh = guh;
        }
        if (soort != null) {
            r.soort = soort;
        }
        if (!r.plek.zelfde(plek) || plek.tijd() - r.plek.tijd() >= VERVERS) {
            r.plek = plek;
            data.setDirty();
        }
    }

    /** Convenience from a loaded guh or maatje (owner, id, dimension and position from the entity). */
    public static void zet(Entity guhOfMaatje, PlekSoort soort, String detail) {
        MinecraftServer s = guhOfMaatje.getServer();
        UUID eigenaar = Band.eigenaar(guhOfMaatje);
        if (s == null || eigenaar == null || !(Band.isBandGuh(guhOfMaatje) || guhOfMaatje instanceof PiepMaatje)) {
            return;
        }
        boolean guh = guhOfMaatje instanceof GuhEntity;
        zet(s, eigenaar, Band.id(guhOfMaatje), new Plek(soort, guhOfMaatje.level().dimension(), guhOfMaatje.blockPosition(), detail,
                guhOfMaatje.level().getGameTime()), guh, guh ? "guh" : ((PiepMaatje) guhOfMaatje).soort());
        if (guh && guhOfMaatje instanceof GuhEntity g) {
            BandData.Rec r = BandData.get(s).vind(eigenaar, g.getUUID());
            if (r != null) {
                r.naam = g.getName().getString();
            }
        }
    }

    /**
     * An item holding a guh ({@code picked_up_guh}) or a maatje (muisje/Schilly/Poepschilly item): where it is now.
     * Does nothing for other items or items without an owner.
     */
    public static void item(ItemStack stack, PlekSoort soort, ResourceKey<Level> dim, BlockPos pos, String detail, long tijd) {
        MinecraftServer s = Band.server();
        Wie wie = wie(stack);
        if (s == null || wie == null) {
            return;
        }
        zet(s, wie.eigenaar, wie.id, new Plek(soort, dim, pos, detail, tijd), wie.guh, wie.guh ? "guh" : null);
        if (!wie.naam.isEmpty()) {
            BandData.Rec r = BandData.get(s).vind(wie.eigenaar, wie.id);
            if (r != null && r.naam.isEmpty()) {
                r.naam = wie.naam;
            }
        }
    }

    /** An item in a player's pockets. */
    public static void inZakken(ItemStack stack, Player holder) {
        item(stack, PlekSoort.ITEM_SPELER, holder.level().dimension(), holder.blockPosition(), holder.getGameProfile().getName(),
                holder.level().getGameTime());
    }

    /** Who is in this item: owner + band id (+ name), or null. */
    @Nullable
    public static Wie wie(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.PICKED_UP_GUH.get())) {
            CompoundTag tag = PickedUpGuhItem.guhData(stack);
            if (!"guhs:guh".equals(tag.getString("id")) || !tag.hasUUID("UUID") || !tag.hasUUID("Owner")) {
                return null;   // (a Reisguh, or a wild one)
            }
            return new Wie(tag.getUUID("Owner"), tag.getUUID("UUID"), true, tag.getString("GuhDisplayName"));
        }
        if (stack.getItem() instanceof nl.juiced.guhs.feature.piep.PiepDierItem) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                return null;
            }
            CompoundTag tag = data.copyTag();
            CompoundTag forge = tag.getCompound("NeoForgeData");
            if (!tag.hasUUID("Owner") || !forge.hasUUID(Band.BAND_ID)) {
                return null;
            }
            return new Wie(tag.getUUID("Owner"), forge.getUUID(Band.BAND_ID), false,
                    stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "");
        }
        return null;
    }

    public record Wie(UUID eigenaar, UUID id, boolean guh, String naam) {
    }

    @Nullable
    public static Plek plek(MinecraftServer s, UUID eigenaar, UUID bandId) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, bandId);
        return r == null ? null : r.plek;
    }

    /** "In het Guhhuisje Knabbelkasteeltje (Bovenwereld, 12 64 -30)" and so on. */
    public static Component tekst(Plek plek) {
        return Component.translatable("gui.guhs.band.plek." + plek.soort().id(), plek.detail(), dimensie(plek.dim()),
                plek.pos().getX(), plek.pos().getY(), plek.pos().getZ());
    }

    /** A dimension's name (lang gui.guhs.band.dim.&lt;namespace&gt;.&lt;path&gt;). */
    public static Component dimensie(ResourceKey<Level> dim) {
        return Component.translatable("gui.guhs.band.dim." + dim.location().getNamespace() + "." + dim.location().getPath());
    }
}
