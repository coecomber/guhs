package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;

/**
 * U-GILDEN: the simple banner designer (V1 "een banier/kleuren"): a base colour + at most {@link #MAX_LAGEN} layers, each one of
 * the {@link #PATRONEN} (a few friendly vanilla patterns) in a colour. The bond's colour (chat tag, name badge) is the base colour.
 * The castle's banners are recoloured from this ({@link #zet}).
 */
public final class Banier {
    public static final int MAX_LAGEN = 3;
    /** Dutch id -> vanilla pattern path. */
    public static final List<String[]> PATRONEN = List.of(
            new String[] {"rand", "border"}, new String[] {"kruis", "straight_cross"}, new String[] {"schuinkruis", "cross"},
            new String[] {"bloem", "flower"}, new String[] {"cirkel", "circle"}, new String[] {"ruit", "rhombus"},
            new String[] {"paal", "stripe_center"}, new String[] {"balk", "stripe_middle"}, new String[] {"bovenhelft", "half_horizontal"},
            new String[] {"verloop", "gradient"}, new String[] {"schuin", "diagonal_left"}, new String[] {"punt", "triangle_top"},
            new String[] {"tanden", "triangles_bottom"}, new String[] {"wereldbol", "globe"});

    private Banier() {
    }

    @Nullable
    public static String vanilla(String nl) {
        for (String[] p : PATRONEN) {
            if (p[0].equals(nl)) {
                return p[1];
            }
        }
        return null;
    }

    public static int index(String nl) {
        for (int i = 0; i < PATRONEN.size(); i++) {
            if (PATRONEN.get(i)[0].equals(nl)) {
                return i;
            }
        }
        return 0;
    }

    /** The text colour of a dye (a bit lighter for the dark ones so a badge stays readable). */
    public static int rgb(DyeColor c) {
        return switch (c) {
            case BLACK -> 0x6A6A7A;
            case BLUE -> 0x6A8AFF;
            case BROWN -> 0xC8905A;
            case GRAY -> 0xA0A0A8;
            case PURPLE -> 0xC07AF0;
            case GREEN -> 0x7AC85A;
            default -> c.getTextColor();
        };
    }

    public static DyeColor kleur(String naam) {
        DyeColor c = DyeColor.byName(naam.toLowerCase(Locale.ROOT), null);
        return c == null ? DyeColor.PINK : c;
    }

    public static BannerPatternLayers lagen(HolderLookup.Provider reg, Bond b) {
        HolderLookup.RegistryLookup<BannerPattern> lk = reg.lookupOrThrow(Registries.BANNER_PATTERN);
        BannerPatternLayers.Builder bl = new BannerPatternLayers.Builder();
        for (Bond.Laag l : b.lagen) {
            String v = vanilla(l.patroon());
            if (v == null) {
                continue;
            }
            java.util.Optional<Holder.Reference<BannerPattern>> h = lk.get(ResourceKey.create(Registries.BANNER_PATTERN, Identifier.withDefaultNamespace(v)));
            h.ifPresent(x -> bl.add(x, l.kleur()));
        }
        return bl.build();
    }

    /** The bond's banner as an item (for the panel's hover and the castle). */
    public static ItemStack item(HolderLookup.Provider reg, Bond b) {
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(b.basis.getName() + "_banner"));
        ItemStack s = new ItemStack(it);
        s.set(DataComponents.BANNER_PATTERNS, lagen(reg, b));
        return s;
    }

    /** Makes the banner at pos this bond's banner (keeps its facing / rotation). False: no banner there. */
    public static boolean zet(ServerLevel sl, BlockPos pos, Bond b) {
        BlockState st = sl.getBlockState(pos);
        Block nieuw;
        BlockState ns;
        if (st.getBlock() instanceof WallBannerBlock) {
            nieuw = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(b.basis.getName() + "_wall_banner"));
            ns = nieuw.defaultBlockState().setValue(WallBannerBlock.FACING, st.getValue(WallBannerBlock.FACING));
        } else if (st.getBlock() instanceof BannerBlock) {
            nieuw = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(b.basis.getName() + "_banner"));
            ns = nieuw.defaultBlockState().setValue(BannerBlock.ROTATION, st.getValue(BannerBlock.ROTATION));
        } else {
            return false;
        }
        if (ns != st) {
            sl.setBlock(pos, ns, 2);
        }
        if (sl.getBlockEntity(pos) instanceof BannerBlockEntity be) {
            be.applyComponentsFromItemStack(item(sl.registryAccess(), b));
            be.setChanged();
            sl.sendBlockUpdated(pos, ns, ns, 3);
        }
        return true;
    }
}
