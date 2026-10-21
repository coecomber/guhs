package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.vormen.Mix;

/**
 * The level tools (design 2.3): the egaliseerder (Zoutkeet), the verfmachine and the tunnelboor (Sauskeet). A click puts
 * down a ghost of the change with its panel (look first, then Bouw!). Sneak-click a block with the tunnelboor: that block
 * lines the tunnel; with the others: the settings.
 */
public class Terreinwerk extends GereedschapItem {
    public Terreinwerk(Properties properties, Gereedschap soort) {
        super(properties, soort);
    }

    @Override
    protected void opBlok(ServerPlayer p, ItemStack stack, BouwNiveau n, BlockPos pos, Direction kant, boolean sluip) {
        if (sluip) {
            if (soort == Gereedschap.TUNNELBOOR) {
                pipet(p, "bekleding", pos);
            } else {
                GereedschapPayloads.openInstellingen(p, soort, n);
            }
            return;
        }
        CompoundTag t = Opties.van(p, soort);
        Map<BlockPos, BlockState> uit = switch (soort) {
            case EGALISEERDER -> Terrein.egaliseer(p.level(), pos, Terrein.Stand.values()[Math.min(3, Opties.waarde(soort, t, "stand"))],
                    Math.min(Math.max(2, n.maxVorm / 2), Opties.waarde(soort, t, "straal")), Opties.waarde(soort, t, "hoogte"),
                    Opties.waarde(soort, t, "omlaag") != 0, Opties.waarde(soort, t, "water") != 0, Opties.waarde(soort, t, "ruwheid"), p.getDirection());
            case VERFMACHINE -> verf(p, pos, t);
            default -> {
                Direction richting = kant.getOpposite();
                if (p.getXRot() > 60) {
                    richting = Direction.DOWN;
                } else if (p.getXRot() < -60) {
                    richting = Direction.UP;
                } else if (richting.getAxis().isVertical()) {
                    richting = p.getDirection();
                }
                yield Terrein.tunnel(p.level(), pos, richting, Terrein.Doorsnede.values()[Math.min(2, Opties.waarde(soort, t, "vorm"))],
                        Opties.waarde(soort, t, "breedte"), Opties.waarde(soort, t, "hoogte"), Opties.waarde(soort, t, "lengte"),
                        Mix.laad(t.getListOrEmpty("bekleding")).eerste(Blocks.STONE_BRICKS), Opties.waarde(soort, t, "lampjes") != 0);
            }
        };
        Plaatsen.opWereld(p, uit, Component.translatable("item.guhs." + soort.id()).getString());
    }

    /** (the verfmachine's settings into Kwasten.verf) */
    static Map<BlockPos, BlockState> verf(ServerPlayer p, BlockPos pos, CompoundTag t) {
        Gereedschap g = Gereedschap.VERFMACHINE;
        DyeColor a = DyeColor.byId(Opties.waarde(g, t, "kleur"));
        DyeColor b = Opties.waarde(g, t, "verloop") != 0 ? DyeColor.byId(Opties.waarde(g, t, "kleur2")) : null;
        return Kwasten.verf(p.level(), pos, Opties.waarde(g, t, "straal"), a, b, Opties.waarde(g, t, "as") == 0, p.getDirection());
    }
}
