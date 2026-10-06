package nl.juiced.guhs.feature.torenpeper;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The three peppers of one plant. Which one a plant gives is decided by the ground it stands on: hot ground (block tag
 * guhs:torenpeper/hete_grond, gloeikool) makes the red Vahoegpeper, sweet ground (guhs:torenpeper/zoete_grond,
 * pindasaus-nylium) the pink Snoeppeper, any other pepper ground (as-aarde) the green Njegpeper.
 */
public enum PeperSoort implements StringRepresentable {
    GROEN("groen"), ROOD("rood"), ROZE("roze");

    private final String id;

    PeperSoort(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** The pepper this kind gives. */
    public Item peper() {
        return switch (this) {
            case GROEN -> TorenpeperFeature.NJEGPEPER.get();
            case ROOD -> TorenpeperFeature.VAHOEGPEPER.get();
            case ROZE -> TorenpeperFeature.SNOEPPEPER.get();
        };
    }

    /** What a plant on this ground becomes. */
    public static PeperSoort vanGrond(BlockState grond) {
        return grond.is(TorenpeperFeature.HETE_GROND) ? ROOD : grond.is(TorenpeperFeature.ZOETE_GROND) ? ROZE : GROEN;
    }
}
