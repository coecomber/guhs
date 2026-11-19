package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterPleinData;

/**
 * Our blocks on plein's square: Het Grote Boek on {@code grote_boek} (plein put an empty vanilla lectern there) and a
 * {@code guhwinter_bouwbord} on each {@code bouwveld_bord_<i>} (plein put a blank spruce sign there). They go there after
 * the winter square is stamped (plein's {@code naStempel}), with {@code /guhs winter lootjes plaats}, and by themselves
 * during the event when someone comes near a square that still has plein's plain ones (a world set up before this slice).
 * <p>The plot signs say "Bouwplekje N" and, once the Sint has drawn, "Gemaakt door &lt;maker&gt;": never the receiver. Waxed
 * (nobody edits them); a click is ours ({@link LootjesFeature}: the maker's drawing, or who builds here).
 */
public final class LootjesPlaatsing {
    private static final String T = Lootjes.T;

    private LootjesPlaatsing() {
    }

    /** Puts the book and the eight signs in place (and writes the signs). Returns a summary line. */
    static String plaats(ServerLevel level) {
        int boek = 0, borden = 0;
        GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek("grote_boek");
        if (a != null) {
            BlockPos p = a.pos();
            BlockState s = LootjesFeature.GROTE_BOEK.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.fromYRot(a.yaw()));
            if (!level.getBlockState(p).is(LootjesFeature.GROTE_BOEK.get())) {
                level.setBlock(p, s, Block.UPDATE_ALL);
            }
            boek = 1;
        }
        for (Bouwvelden.Veld v : Bouwvelden.alle()) {
            BlockPos p = v.bord();
            if (p == null) {
                continue;
            }
            BlockState oud = level.getBlockState(p);
            if (!oud.is(LootjesFeature.BOUWBORD.get())) {
                GuhwinterAnkers.Anker ba = GuhwinterAnkers.zoek("bouwveld_bord_" + v.i());
                int rot = oud.hasProperty(StandingSignBlock.ROTATION) ? oud.getValue(StandingSignBlock.ROTATION)
                        : RotationSegment.convertToSegment(ba == null ? 0 : ba.yaw());
                level.setBlock(p, LootjesFeature.BOUWBORD.get().defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), Block.UPDATE_ALL);
            }
            borden++;
        }
        schrijfBorden(level);
        return "lootjes: Grote Boek " + boek + ", bordjes " + borden;
    }

    /** Writes every plot sign: "Bouwplekje N" + the maker (or "nog vrij"). */
    static void schrijfBorden(ServerLevel level) {
        LootjesData d = LootjesData.get(level.getServer());
        for (Bouwvelden.Veld v : Bouwvelden.alle()) {
            BlockPos p = v.bord();
            if (p == null || !(level.getBlockEntity(p) instanceof SignBlockEntity s)) {
                continue;
            }
            UUID eigenaar = d.eigenaarVanVeld(v.i());
            SignText t = new SignText().setColor(DyeColor.BROWN)
                    .setMessage(0, Component.translatable(T + "bord.nummer", v.i() + 1))
                    .setMessage(1, eigenaar == null ? Component.empty() : Component.translatable(T + "bord.gemaakt"))
                    .setMessage(2, eigenaar == null ? Component.translatable(T + "bord.vrij") : Component.literal(d.naam(eigenaar)))
                    .setMessage(3, Component.empty());
            s.setText(t, true);
            s.setText(t, false);
            s.setWaxed(true);
            s.setChanged();
            level.sendBlockUpdated(p, s.getBlockState(), s.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** (every 10 s during the event) plein's plain lectern still there while someone is near: swap in ours. */
    static void controleer(ServerLevel level) {
        GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek("grote_boek");
        if (a == null || !"winter".equals(GuhwinterPleinData.van(level).plein())) {
            return;
        }
        BlockPos p = a.pos();
        if (!level.isLoaded(p)) {
            return;
        }
        boolean dichtbij = false;
        for (ServerPlayer sp : level.players()) {
            dichtbij |= sp.blockPosition().closerThan(p, 64);
        }
        if (!dichtbij) {
            return;
        }
        boolean nodig = level.getBlockState(p).is(Blocks.LECTERN);
        for (Bouwvelden.Veld v : Bouwvelden.alle()) {
            nodig |= v.bord() != null && level.isLoaded(v.bord()) && !level.getBlockState(v.bord()).is(LootjesFeature.BOUWBORD.get())
                    && level.getBlockState(v.bord()).getBlock() instanceof StandingSignBlock;
        }
        if (nodig) {
            plaats(level);
        }
    }
}
