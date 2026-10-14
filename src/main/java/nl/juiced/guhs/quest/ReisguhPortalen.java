package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.4.2 (fixes, bug 5): the guh portals that already had their Reisguh. A portal gets a Reisguh only ONCE ever: picking it
 * up and going through the portal again does not make a new one. Saved per dimension ({@code <dimension>/data/guhs/
 * reisguh_portalen.dat}): a list of portal spots; a spot within {@link #GEBIED} blocks counts as the same portal (the
 * builder and the arrival ask with different blocks of one portal).
 * <p>
 * Portals from before 1.4.2: one whose Reisguh still stands is remembered the first time it is asked about; one whose
 * Reisguh was picked up and put down elsewhere is recognised by her automatic name "Guhportaal (x, z)" (the spot she was
 * made at, see {@link #portaalNaamBij}).
 */
public final class ReisguhPortalen extends SavedData {
    /** Two portal spots this close (every axis) are the same portal. */
    public static final int GEBIED = 8;

    private final List<BlockPos> portalen = new ArrayList<>();

    static final SavedDataType<ReisguhPortalen> TYPE = GuhSavedData.tagType("reisguh_portalen", ReisguhPortalen::new,
            ReisguhPortalen::load, ReisguhPortalen::save);

    public static ReisguhPortalen van(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Did this portal (or one within {@link #GEBIED} blocks) get its Reisguh already? */
    public boolean gehad(BlockPos portaal) {
        for (BlockPos p : portalen) {
            if (Math.abs(p.getX() - portaal.getX()) <= GEBIED && Math.abs(p.getY() - portaal.getY()) <= GEBIED
                    && Math.abs(p.getZ() - portaal.getZ()) <= GEBIED) {
                return true;
            }
        }
        return false;
    }

    /** This portal has had its Reisguh: never another one here. */
    public void onthoud(BlockPos portaal) {
        if (!gehad(portaal)) {
            portalen.add(portaal.immutable());
            setDirty();
        }
    }

    /** (tests) forget a portal. */
    public void vergeet(BlockPos portaal) {
        if (portalen.removeIf(p -> p.closerThan(portaal, GEBIED + 1))) {
            setDirty();
        }
    }

    /**
     * Is this the automatic name of a portal Reisguh ("Guhportaal (x, z)", quest.guhs.reis.portal_name) made within
     * {@link #GEBIED} blocks (x and z) of this portal? Such a Reisguh came from this portal, wherever she stands now.
     */
    public static boolean portaalNaamBij(Component naam, BlockPos portaal) {
        if (naam.getContents() instanceof TranslatableContents t && t.getKey().equals("quest.guhs.reis.portal_name") && t.getArgs().length >= 2) {
            Integer x = getal(t.getArgs()[0]), z = getal(t.getArgs()[1]);
            return x != null && z != null && Math.abs(x - portaal.getX()) <= GEBIED && Math.abs(z - portaal.getZ()) <= GEBIED;
        }
        return false;
    }

    private static Integer getal(Object arg) {
        if (arg instanceof Number n) {
            return n.intValue();
        }
        String s = arg instanceof Component c ? c.getString() : String.valueOf(arg);
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ReisguhPortalen load(CompoundTag tag) {
        ReisguhPortalen data = new ReisguhPortalen();
        for (Tag t : tag.getListOrEmpty("Portalen")) {
            if (t instanceof LongTag l) {
                data.portalen.add(BlockPos.of(l.longValue()));
            }
        }
        return data;
    }

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (BlockPos p : portalen) {
            list.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Portalen", list);
        return tag;
    }
}
