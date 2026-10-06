package nl.juiced.guhs.feature.techmachine;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * The machines that share one menu and one screen ({@link MachineMenu}, {@code client.MachineScreen}): how many slots each
 * has, where they sit on the screen (the usual panel of 176 x 166 with the player's inventory at the bottom), and which of
 * them a player may put something in. The server asks the machine itself ({@link TechBlockEntity#magErin}); {@link #lijkt}
 * is what the client can tell without it.
 */
public enum MachineSoort {
    /** Nine slots of harvest (out). */
    OOGSTER(9),
    /** Nine slots of what it gnawed loose (out). */
    KNABBELAAR(9),
    /** Nine slots of blocks to put down (in). */
    NEERZETTER(9),
    /** One slot to grind (in), one of what came out. */
    VADSMOLEN(2),
    /** The Bouwtekening, nine slots of supplies (in), three of what it made (out). */
    KNUTSELMACHINE(13),
    /** The saplings (in). */
    PLANTAGEBAK(1);

    public final int vakken;

    MachineSoort(int vakken) {
        this.vakken = vakken;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The slot's left edge on the panel. */
    public int x(int vak) {
        return switch (this) {
            case OOGSTER, KNABBELAAR, NEERZETTER -> 62 + (vak % 3) * 18;
            case VADSMOLEN -> vak == 0 ? 53 : 107;
            case KNUTSELMACHINE -> vak == 0 ? 17 : vak <= 9 ? 53 + ((vak - 1) % 3) * 18 : 143;
            case PLANTAGEBAK -> 80;
        };
    }

    /** The slot's top edge on the panel. */
    public int y(int vak) {
        return switch (this) {
            case OOGSTER, KNABBELAAR, NEERZETTER -> 17 + (vak / 3) * 18;
            case VADSMOLEN -> 35;
            case KNUTSELMACHINE -> vak == 0 ? 35 : vak <= 9 ? 17 + ((vak - 1) / 3) * 18 : 17 + (vak - 10) * 18;
            case PLANTAGEBAK -> 35;
        };
    }

    /** May a player (or a pipe) put something into this slot at all? */
    public boolean invoer(int vak) {
        return switch (this) {
            case OOGSTER, KNABBELAAR -> false;
            case NEERZETTER, PLANTAGEBAK -> true;
            case VADSMOLEN -> vak == 0;
            case KNUTSELMACHINE -> vak <= 9;
        };
    }

    /** May a pipe take from this slot? */
    public boolean uitvoer(int vak) {
        return switch (this) {
            case OOGSTER, KNABBELAAR -> true;
            case NEERZETTER, PLANTAGEBAK -> false;
            case VADSMOLEN -> vak == 1;
            case KNUTSELMACHINE -> vak >= 10;
        };
    }

    /** The screen has an arrow that fills while the machine works (and where its left edge is). */
    public int pijlX() {
        return switch (this) {
            case VADSMOLEN -> 77;
            case KNUTSELMACHINE -> 113;
            default -> -1;
        };
    }

    /**
     * (client) Does this stack look like something for this slot? What the client can know by itself; the server decides
     * ({@link TechBlockEntity#magErin}) and puts a wrong guess right.
     */
    public boolean lijkt(int vak, ItemStack stack, ItemStack tekening) {
        if (!invoer(vak)) {
            return false;
        }
        return switch (this) {
            case NEERZETTER -> stack.getItem() instanceof BlockItem;
            case PLANTAGEBAK -> Plantagebakken.isZaailing(stack);
            case KNUTSELMACHINE -> vak == 0 ? Bouwtekeningen.lees(stack) != null : KnutselmachineBlockEntity.hoortBij(tekening, stack);
            default -> true;
        };
    }
}
