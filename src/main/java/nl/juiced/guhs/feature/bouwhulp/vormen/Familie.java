package nl.juiced.guhs.feature.bouwhulp.vormen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The family of a block: its full block, stair, slab and wall (oak_planks / oak_stairs / oak_slab; bricks / brick_stairs;
 * guhs pastelbaksteen_roze / _trap / _plaat / _muur), found by name. The roofs use it for their stairs, the kwasten to
 * keep a block's shape (design 2.4: "een trap blijft een trap"), the verweerkwast for the mossy and cracked kin.
 */
public record Familie(Block vol, @Nullable Block trap, @Nullable Block plaat, @Nullable Block muur) {
    public enum Rol { VOL, TRAP, PLAAT, MUUR, ANDERS }

    private static final Map<Block, Familie> CACHE = new ConcurrentHashMap<>();

    public static Familie van(Block b) {
        return CACHE.computeIfAbsent(b, Familie::zoek);
    }

    public static Rol rol(Block b) {
        if (b instanceof StairBlock) {
            return Rol.TRAP;
        }
        if (b instanceof SlabBlock) {
            return Rol.PLAAT;
        }
        if (b instanceof WallBlock) {
            return Rol.MUUR;
        }
        BlockState s = b.defaultBlockState();
        return s.isSolidRender() ? Rol.VOL : Rol.ANDERS;
    }

    /** The member with this role (the full block when the family has none). */
    public Block lid(Rol rol) {
        Block b = switch (rol) {
            case TRAP -> trap;
            case PLAAT -> plaat;
            case MUUR -> muur;
            default -> vol;
        };
        return b != null ? b : vol;
    }

    /**
     * {@code oud} made of {@code nieuw}'s family in the same shape, with the same facing, half, waterlogging... (null:
     * leave it, e.g. a door, a fence or the same block already). A stair of a material without stairs becomes its full
     * block.
     */
    @Nullable
    public static BlockState inVorm(BlockState oud, Block nieuw) {
        Rol rol = rol(oud.getBlock());
        if (rol == Rol.ANDERS) {
            return null;
        }
        Block doel = van(nieuw).lid(rol);
        if (doel == oud.getBlock()) {
            return null;
        }
        return doel.withPropertiesOf(oud);
    }

    private static Familie zoek(Block b) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(b);
        String ns = id.getNamespace(), p = id.getPath();
        String basis = p;
        for (String eind : new String[] {"_stairs", "_slab", "_wall", "_trap", "_plaat", "_muur"}) {
            if (p.endsWith(eind)) {
                basis = p.substring(0, p.length() - eind.length());
                break;
            }
        }
        Block vol = b instanceof StairBlock || b instanceof SlabBlock || b instanceof WallBlock
                ? eerste(ns, basis, basis + "s", basis + "_planks", basis + "_block", basis + "_bricks")
                : b;
        if (vol == null) {
            vol = b;
        }
        // the stem of the members: oak_planks -> oak, bricks -> brick, quartz_block -> quartz, stone_bricks -> stone_brick
        String volPad = BuiltInRegistries.BLOCK.getKey(vol).getPath();
        String[] stammen = {volPad, strip(volPad, "_planks"), strip(volPad, "_block"), strip(volPad, "s"), basis};
        Block trap = null, plaat = null, muur = null;
        for (String st : stammen) {
            if (st == null) {
                continue;
            }
            trap = trap != null ? trap : eerste(ns, st + "_stairs", st + "_trap");
            plaat = plaat != null ? plaat : eerste(ns, st + "_slab", st + "_plaat");
            muur = muur != null ? muur : eerste(ns, st + "_wall", st + "_muur");
        }
        return new Familie(vol, trap instanceof StairBlock ? trap : null, plaat instanceof SlabBlock ? plaat : null,
                muur instanceof WallBlock ? muur : null);
    }

    @Nullable
    private static String strip(String p, String eind) {
        return p.endsWith(eind) ? p.substring(0, p.length() - eind.length()) : null;
    }

    @Nullable
    private static Block eerste(String ns, String... paden) {
        for (String p : paden) {
            for (String n : new String[] {ns, "minecraft"}) {
                Identifier i = Identifier.tryParse(n + ":" + p);
                if (i != null) {
                    var b = BuiltInRegistries.BLOCK.getOptional(i);
                    if (b.isPresent() && b.get() != Blocks.AIR) {
                        return b.get();
                    }
                }
            }
        }
        return null;
    }

    /** A block by id (null when there is none). */
    @Nullable
    public static Block blok(String id) {
        return Mix.blok(id);
    }

    // =====================================================================================================================
    // colours (the patroonkwast's animal patterns, the verfmachine): wool, concrete, terracotta and their kin
    // =====================================================================================================================

    /** The colour families: what a coloured block is called after its colour ("wool" -> white_wool...). */
    public static final String[] KLEURSOORTEN = {"wool", "concrete", "terracotta", "stained_glass", "stained_glass_pane", "carpet",
            "concrete_powder", "glazed_terracotta"};

    /** The colour family of a block ("wool", ...; plain terracotta and glass count too), or null. */
    @Nullable
    public static String kleurSoort(Block b) {
        if (b == Blocks.TERRACOTTA) {
            return "terracotta";
        }
        if (b == Blocks.GLASS) {
            return "stained_glass";
        }
        if (b == Blocks.GLASS_PANE) {
            return "stained_glass_pane";
        }
        String p = BuiltInRegistries.BLOCK.getKey(b).getPath();
        if (!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals("minecraft")) {
            return null;
        }
        for (DyeColor c : DyeColor.values()) {
            String voor = c.getSerializedName() + "_";
            if (p.startsWith(voor)) {
                String rest = p.substring(voor.length());
                for (String s : KLEURSOORTEN) {
                    if (s.equals(rest)) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    /** The block of this colour family in this colour (white_wool...). */
    public static Block gekleurd(String soort, DyeColor kleur) {
        Block b = eerste("minecraft", kleur.getSerializedName() + "_" + soort);
        return b != null ? b : Blocks.WHITE_WOOL;
    }
}
