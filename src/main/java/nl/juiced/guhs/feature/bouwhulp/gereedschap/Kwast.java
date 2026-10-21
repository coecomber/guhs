package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.vormen.Mix;

/**
 * The four kwasten (design 2.4): vervangkwast, mengkwast, patroonkwast and verweerkwast. A click paints a ball around the
 * clicked block (its radius in the settings) and builds it at once, from your own blocks (missing: it stays a ghost and
 * says what is missing); the undo list takes it back. Sneak-click a block: its block becomes the kwast's material
 * (the mengkwast and patroonkwast add it to their mix).
 */
public class Kwast extends GereedschapItem {
    public Kwast(Properties properties, Gereedschap soort) {
        super(properties, soort);
    }

    @Override
    protected void opBlok(ServerPlayer p, ItemStack stack, BouwNiveau n, BlockPos pos, Direction kant, boolean sluip) {
        if (sluip) {
            switch (soort) {
                case VERVANGKWAST -> pipet(p, "blok", pos);
                case MENGKWAST, PATROONKWAST -> pipet(p, "mix", pos);
                default -> GereedschapPayloads.openInstellingen(p, soort, n);
            }
            return;
        }
        CompoundTag t = Opties.van(p, soort);
        List<BlockPos> gebied = Kwasten.gebied(p.level(), pos, Opties.waarde(soort, t, "straal"), Opties.waarde(soort, t, "zelfde") != 0,
                Opties.waarde(soort, t, "buitenkant") != 0);
        Map<BlockPos, BlockState> uit;
        switch (soort) {
            case VERVANGKWAST -> {
                Mix m = Mix.laad(t.getListOrEmpty("blok"));
                if (m.isLeeg()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.kwast.kies_blok"));
                    return;
                }
                uit = Kwasten.vervang(p.level(), gebied, m.eerste(null));
            }
            case MENGKWAST -> {
                Mix m = Mix.laad(t.getListOrEmpty("mix"));
                if (m.isLeeg()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.kwast.kies_mix"));
                    return;
                }
                uit = Kwasten.meng(p.level(), gebied, m);
            }
            case PATROONKWAST -> {
                Patronen.Patroon pat = Patronen.Patroon.van(t.getIntOr("patroon", 0));
                if (!pat.open(p)) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.dicht.dex"));
                    return;
                }
                Mix m = Mix.laad(t.getListOrEmpty("mix"));
                if (!pat.dier() && m.aantal() < 2) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.kwast.kies_twee"));
                    return;
                }
                uit = Kwasten.patroon(p.level(), gebied, pat, m);
            }
            default -> uit = Kwasten.verweer(p.level(), gebied, Opties.waarde(soort, t, "mos") != 0, Opties.waarde(soort, t, "barsten") != 0,
                    Opties.waarde(soort, t, "klimop") != 0, Opties.waarde(soort, t, "sterkte"));
        }
        Plaatsen.meteen(p, uit, Component.translatable("item.guhs." + soort.id()).getString());
    }
}
