package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Deur;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;

/**
 * U-HUIZEN: who may do what (pure, no level: the gametests check it directly).
 * <ul>
 *   <li><b>Eigenaar</b> (the account that bought it): everything inside the own fence.</li>
 *   <li><b>Vriend</b>: comes in unless the door is "op slot"; decorates only when the owner allowed it ("Vrienden mogen inrichten").</li>
 *   <li><b>Bezoeker</b>: looks only; comes in only when the door is "open voor iedereen".</li>
 * </ul>
 * Building: only inside your own (or a friend's, with permission) fence, between the floor and {@link WijkPlan#HOOGTE} above it,
 * up to the house's deco limit; only blocks that were placed (never the house itself) can be broken again. Never: containers or
 * other block entities with an inventory, pistons, TNT, falling blocks, liquids (no dupes, no griefing, no lost items).
 */
public final class Rechten {
    public enum Rol {
        EIGENAAR, VRIEND, BEZOEKER
    }

    private Rechten() {
    }

    public static Rol rol(Huis h, UUID speler) {
        if (h.eigenaar.equals(speler)) {
            return Rol.EIGENAAR;
        }
        return h.vrienden.containsKey(speler) ? Rol.VRIEND : Rol.BEZOEKER;
    }

    public static boolean magBinnen(Huis h, UUID speler) {
        Rol r = rol(h, speler);
        return r == Rol.EIGENAAR || h.deur == Deur.OPEN || h.deur == Deur.VRIENDEN && r == Rol.VRIEND;
    }

    public static boolean magInrichten(Huis h, UUID speler) {
        Rol r = rol(h, speler);
        return r == Rol.EIGENAAR || r == Rol.VRIEND && h.vriendenInrichten;
    }

    /** May this block be placed (as decoration) at all? */
    public static boolean decoBlok(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof PistonBaseBlock || b instanceof TntBlock || b instanceof FallingBlock || b instanceof LiquidBlock) {
            return false;
        }
        if (b instanceof EntityBlock eb) {
            // block entities are fine (signs, banners, beds, skulls...), but nothing that holds items
            var be = eb.newBlockEntity(BlockPos.ZERO, s);
            return !(be instanceof Container);
        }
        return true;
    }

    public static boolean decoItem(ItemStack s) {
        return s.getItem() instanceof BlockItem bi && decoBlok(bi.getBlock().defaultBlockState());
    }

    /** Why a placement is refused (a lang key), or null when it may. */
    @Nullable
    public static String plaatsen(@Nullable Huis h, Kavel k, BlockPos pos, UUID speler, BlockState s) {
        if (h == null) {
            return "vadsrijk.huizen.nee.niet_van_jou";
        }
        if (!magInrichten(h, speler)) {
            return rol(h, speler) == Rol.VRIEND ? "vadsrijk.huizen.nee.vriend_mag_niet" : "vadsrijk.huizen.nee.niet_van_jou";
        }
        if (!k.binnenHek(pos) || pos.getY() < WijkPlan.VLOER || pos.getY() > WijkPlan.VLOER + WijkPlan.HOOGTE) {
            return "vadsrijk.huizen.nee.buiten_kavel";
        }
        if (!decoBlok(s)) {
            return "vadsrijk.huizen.nee.blok";
        }
        if (h.geplaatst.size() >= h.soort.maxDeco) {
            return "vadsrijk.huizen.nee.vol";
        }
        return null;
    }

    /** Why breaking is refused (a lang key), or null when it may. */
    @Nullable
    public static String breken(@Nullable Huis h, BlockPos pos, UUID speler) {
        if (h == null || !magInrichten(h, speler)) {
            return "vadsrijk.huizen.nee.niet_van_jou";
        }
        if (!h.geplaatst.contains(pos.asLong())) {
            return "vadsrijk.huizen.nee.huis_zelf";
        }
        return null;
    }
}
