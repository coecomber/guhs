package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.vormen.Daken;
import nl.juiced.guhs.feature.bouwhulp.vormen.VormInstelling;
import nl.juiced.guhs.feature.bouwhulp.vormen.Vormen;

/**
 * The vormstaf (design 2.4): click a block and the chosen shape or roof stands there as a ghost (facing you), with its
 * control panel (Bouw!, turn, move...). Sneak-click a block: that block becomes the wall material. In capture mode a click
 * sets the corners of a new blueprint (sneak-click: the frame finds the building itself). The radial menu (hold the
 * Vormstaf key) and the settings screen (sneak-click in the air) choose shape, size, hollow/filled and materials.
 */
public class Vormstaf extends GereedschapItem {
    public Vormstaf(Properties properties) {
        super(properties, Gereedschap.VORMSTAF);
    }

    @Override
    protected void opBlok(ServerPlayer p, ItemStack stack, BouwNiveau n, BlockPos pos, Direction kant, boolean sluip) {
        CompoundTag t = Opties.van(p, soort);
        int modus = Gereedschap.modus(t);
        if (modus == Gereedschap.VASTLEG) {
            VastlegDienst.klik(p, n, pos, sluip);
            return;
        }
        if (sluip) {
            pipet(p, "wand", pos);
            return;
        }
        VormInstelling v = VormInstelling.van(t);
        Ontwerp o = ontwerp(v, n, modus);
        if (o.isLeeg()) {
            p.sendOverlayMessage(Component.translatable(v.vorm() == Vormen.Vorm.TEKST ? "gui.guhs.bouwhulp.dicht.bloktekst" : "gui.guhs.bouwhulp.dicht.natuur"));
            return;
        }
        String naam = Component.translatable(modus == Gereedschap.DAK ? "gui.guhs.bouwhulp.keuze.dak." + v.dak().id()
                : "gui.guhs.bouwhulp.keuze.vorm." + v.vorm().id()).getString();
        Plaatsen.spook(p, o, Plaatsen.oorsprong(o, pos, kant), Plaatsen.naarSpeler(p), naam);
    }

    /** The shape or roof the settings make (also the keet's tabs and the client's preview). */
    public static Ontwerp ontwerp(VormInstelling v, BouwNiveau n, int modus) {
        return modus == Gereedschap.DAK ? Daken.maak(v, n) : Vormen.maak(v, n);
    }
}
