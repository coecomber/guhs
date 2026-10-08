package nl.juiced.guhs.feature.bio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.Guhs;

/**
 * The fixed names of biomes3 that every slice may use: the three biome keys, "is this spot in that biome", and the
 * blocks and items of ANOTHER slice by id.
 * <p>
 * A slice never names another slice's class. It asks for the other's block or item by its contract id with a stand-in:
 * {@code Bio.blok("wolkenblok_wit", Blocks.WHITE_WOOL)}. While the owner's slice is not merged the stand-in comes back;
 * once it is, the real thing does, without any change in the asking code. Ask at use time (not in a static field: the
 * registries are still empty then).
 */
public final class Bio {
    public static final ResourceKey<Biome> BLOESEMMEERTJE = ResourceKey.create(Registries.BIOME, Guhs.id("bloesemmeertje"));
    public static final ResourceKey<Biome> KLATERDAL = ResourceKey.create(Registries.BIOME, Guhs.id("klaterdal"));
    public static final ResourceKey<Biome> WOLKENWEIDE = ResourceKey.create(Registries.BIOME, Guhs.id("wolkenweide"));

    /** Is this spot in that biome (both sides)? */
    public static boolean in(Level level, BlockPos pos, ResourceKey<Biome> biome) {
        return level.getBiome(pos).is(biome);
    }

    /** Is this spot in one of the three new biomes (both sides)? */
    public static boolean inNieuw(Level level, BlockPos pos) {
        var b = level.getBiome(pos);
        return b.is(BLOESEMMEERTJE) || b.is(KLATERDAL) || b.is(WOLKENWEIDE);
    }

    /** The guhs block with this id, or the stand-in while nobody registered it. */
    public static Block blok(String id, Block standIn) {
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return b == Blocks.AIR ? standIn : b;
    }

    /** The guhs item with this id, or the stand-in while nobody registered it. */
    public static Item item(String id, Item standIn) {
        Item i = BuiltInRegistries.ITEM.getValue(Guhs.id(id));
        return i == Items.AIR ? standIn : i;
    }

    /** Does a guhs block with this id exist yet? */
    public static boolean heeftBlok(String id) {
        return BuiltInRegistries.BLOCK.getValue(Guhs.id(id)) != Blocks.AIR;
    }

    private Bio() {
    }
}
