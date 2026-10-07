package nl.juiced.guhs.feature.oudescenes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import nl.juiced.guhs.entity.GuhVariant;

/**
 * bbq2 (oude-scenes): the entity data of the props and extras of the scenes (the verhaal engine reads it like a saved
 * entity: {@code Cutscene.Builder.acteur(..., nbt)}). They only ever exist on the viewer's own client.
 */
final class Rekwisieten {
    /** 626-guh's picture book: a book lying open-side-up on the sand (an item display, laid flat). */
    static void prentenboek(CompoundTag tag) {
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:book");
        item.putInt("count", 1);
        tag.put("item", item);
        tag.putString("item_display", "fixed");
        CompoundTag t = new CompoundTag();
        // (a quarter turn round the x axis: flat on the ground)
        t.put("left_rotation", floats(-0.70710677f, 0f, 0f, 0.70710677f));
        t.put("right_rotation", floats(0f, 0f, 0f, 1f));
        t.put("translation", floats(0f, 0f, 0f));
        t.put("scale", floats(0.9f, 0.9f, 0.9f));
        tag.put("transformation", t);
    }

    /** The first little resident of the Timmerguh's huisje: a small guh. */
    static void bewonertje(CompoundTag tag, GuhVariant variant) {
        tag.putString("Variant", variant.id());
        CompoundTag schaal = new CompoundTag();
        schaal.putString("id", "minecraft:scale");
        schaal.putDouble("base", 0.7);
        ListTag attributen = new ListTag();
        attributen.add(schaal);
        tag.put("attributes", attributen);
    }

    private static ListTag floats(float... v) {
        ListTag lijst = new ListTag();
        for (float f : v) {
            lijst.add(FloatTag.valueOf(f));
        }
        return lijst;
    }

    private Rekwisieten() {
    }
}
