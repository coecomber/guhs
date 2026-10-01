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

import net.minecraft.core.UUIDUtil;
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
        zet(guhOfMaatje, soort, Component.literal(detail));
    }

    /** {@link #zet(Entity, PlekSoort, String)} with a detail every player reads in their own language (1.2.0). */
    public static void zet(Entity guhOfMaatje, PlekSoort soort, Component detail) {
        MinecraftServer s = guhOfMaatje.level().getServer();
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
                r.naam = g.hasCustomName() ? g.getCustomName().copy() : Component.empty();   // (1.2.0: see BandData.Rec.naam)
            }
        }
    }

    /**
     * An item holding a guh ({@code picked_up_guh}) or a maatje (muisje/Schilly/Poepschilly item): where it is now.
     * Does nothing for other items or items without an owner.
     */
    public static void item(ItemStack stack, PlekSoort soort, ResourceKey<Level> dim, BlockPos pos, String detail, long tijd) {
        item(stack, soort, dim, pos, Component.literal(detail), tijd);
    }

    /** {@link #item(ItemStack, PlekSoort, ResourceKey, BlockPos, String, long)} with a translatable detail (1.2.0). */
    public static void item(ItemStack stack, PlekSoort soort, ResourceKey<Level> dim, BlockPos pos, Component detail, long tijd) {
        MinecraftServer s = Band.server();
        Wie wie = wie(stack);
        if (s == null || wie == null) {
            return;
        }
        zet(s, wie.eigenaar, wie.id, new Plek(soort, dim, pos, detail, tijd), wie.guh, wie.guh ? "guh" : null);
        if (!nl.juiced.guhs.taal.Tekst.empty(wie.naam)) {
            BandData.Rec r = BandData.get(s).vind(wie.eigenaar, wie.id);
            if (r != null && nl.juiced.guhs.taal.Tekst.empty(r.naam)) {
                r.naam = wie.naam;
            }
        }
    }

    /** An item in a player's pockets. */
    public static void inZakken(ItemStack stack, Player holder) {
        item(stack, PlekSoort.ITEM_SPELER, holder.level().dimension(), holder.blockPosition(), holder.getGameProfile().name(),
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
            if (!"guhs:guh".equals(tag.getStringOr("id", "")) || !tag.read("UUID", UUIDUtil.CODEC).isPresent() || !tag.read("Owner", UUIDUtil.CODEC).isPresent()) {
                return null;   // (a Reisguh, or a wild one)
            }
            return new Wie(tag.read("Owner", UUIDUtil.CODEC).orElseThrow(), tag.read("UUID", UUIDUtil.CODEC).orElseThrow(), true, nl.juiced.guhs.taal.Tekst.get(tag, "GuhDisplayName"));
        }
        if (stack.getItem() instanceof nl.juiced.guhs.feature.piep.PiepDierItem) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                return null;
            }
            CompoundTag tag = data.copyTag();
            CompoundTag forge = tag.getCompoundOrEmpty("NeoForgeData");
            if (!tag.read("Owner", UUIDUtil.CODEC).isPresent() || !forge.read(Band.BAND_ID, UUIDUtil.CODEC).isPresent()) {
                return null;
            }
            return new Wie(tag.read("Owner", UUIDUtil.CODEC).orElseThrow(), forge.read(Band.BAND_ID, UUIDUtil.CODEC).orElseThrow(), false,
                    stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().copy() : Component.empty());
        }
        return null;
    }

    public record Wie(UUID eigenaar, UUID id, boolean guh, Component naam) {
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
        return Component.translatable("gui.guhs.band.dim." + dim.identifier().getNamespace() + "." + dim.identifier().getPath());
    }
}
