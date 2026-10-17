package nl.juiced.guhs.feature.einde2.spd1start;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.storage.Nbt;

/**
 * einde2 "Sponsguh", slice sp_d1_start: the things this slice puts in the world for ONE player (the bubble letter, the crates,
 * the order ticket and the burger on the grill). They are vanilla display and interaction entities, seen by that player only
 * ({@link Zicht#alleenVoor}) and live only while their moment lasts: a copy that was saved with its chunk anyway (a server
 * stop in the middle) is not let back in when the chunk loads ({@link #opJoin}).
 */
final class D1Tijdelijk {
    /** The tag of every such entity. */
    static final String TAG = "guhs_einde2_sp_d1_start_tijdelijk";

    private D1Tijdelijk() {
    }

    static boolean is(Entity e) {
        return e.entityTags().contains(TAG);
    }

    static void opJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Puts the entity in the world for this player only. */
    static <T extends Entity> T zet(ServerLevel level, T e, ServerPlayer voor) {
        e.addTag(TAG);
        Zicht.alleenVoor(e, voor.getUUID());
        level.addFreshEntity(e);
        return e;
    }

    /** Takes an entity away (null: nothing). */
    static void weg(@Nullable Entity e) {
        if (e != null && !e.isRemoved()) {
            e.discard();
        }
    }

    // =====================================================================================================================
    // makers (not yet in the world: zet does that)
    // =====================================================================================================================

    /** An item floating at a spot; {@code plat}: lying flat (a layer of the burger), else upright; billboard "fixed" / "vertical". */
    @Nullable
    static Display.ItemDisplay item(ServerLevel level, Vec3 plek, String itemId, float schaal, boolean plat, String billboard) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = basis(schaal, plat ? new float[] {0.70710677f, 0, 0, 0.70710677f} : new float[] {0, 0, 0, 1}, 0, 0, 0, billboard);
        CompoundTag item = new CompoundTag();
        item.putString("id", itemId);
        item.putInt("count", 1);
        tag.put("item", item);
        tag.putString("item_display", "fixed");
        Nbt.load(d, tag);
        d.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        return d;
    }

    /** A block (a crate) standing on a spot, centred on it. */
    @Nullable
    static Display.BlockDisplay blok(ServerLevel level, Vec3 plek, String blockId, @Nullable CompoundTag props, float schaal) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = basis(schaal, new float[] {0, 0, 0, 1}, -schaal / 2f, 0, -schaal / 2f, "fixed");
        CompoundTag state = new CompoundTag();
        state.putString("Name", blockId);
        if (props != null) {
            state.put("Properties", props);
        }
        tag.put("block_state", state);
        Nbt.load(d, tag);
        d.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        return d;
    }

    /** A line of text that turns to the viewer. */
    @Nullable
    static Display.TextDisplay tekst(ServerLevel level, Vec3 plek, Component tekst, float schaal, int achtergrond, int breedte) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = basis(schaal, new float[] {0, 0, 0, 1}, 0, 0, 0, "center");
        tag.put("text", ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), tekst).getOrThrow());
        tag.putInt("background", achtergrond);
        tag.putInt("line_width", breedte);
        Nbt.load(d, tag);
        d.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        return d;
    }

    /** An invisible box a player can click (right or left), standing on a spot. */
    @Nullable
    static Interaction knop(ServerLevel level, Vec3 plek, float breed, float hoog) {
        Interaction i = EntityType.INTERACTION.create(level, EntitySpawnReason.TRIGGERED);
        if (i == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.putFloat("width", breed);
        tag.putFloat("height", hoog);
        tag.putBoolean("response", true);
        Nbt.load(i, tag);
        i.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        return i;
    }

    private static CompoundTag basis(float schaal, float[] draai, float tx, float ty, float tz, String billboard) {
        CompoundTag tag = new CompoundTag();
        CompoundTag t = new CompoundTag();
        t.put("translation", floats(tx, ty, tz));
        t.put("left_rotation", floats(draai));
        t.put("scale", floats(schaal, schaal, schaal));
        t.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", t);
        tag.putString("billboard", billboard);
        tag.putInt("teleport_duration", 3);
        tag.putFloat("view_range", 1.5f);
        return tag;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) {
            list.add(FloatTag.valueOf(v));
        }
        return list;
    }
}
