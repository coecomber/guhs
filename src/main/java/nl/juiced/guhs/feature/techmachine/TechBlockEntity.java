package nl.juiced.guhs.feature.techmachine;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.RangedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The block entity of a machine of this slice: a {@link MachineBlockEntity} (vadskracht, inventory, owner, face) with what
 * all of them share: a job that takes {@link #duur} ticks of work ({@link #voortgang}, saved and shown as the arrow on the
 * screen), the screen itself ({@link MachineMenu}, layout {@link #soort}), which slots take what ({@link #magErin}), and
 * putting a whole harvest into the out slots or nothing at all ({@link #pastAlles}, {@link #stopAlles}).
 */
public abstract class TechBlockEntity extends MachineBlockEntity implements MenuProvider {
    /** Ticks of work done on the current job. */
    protected int voortgang;

    /** What the screen shows besides the slots: progress, its maximum, vadskracht (0 / 1), and the machine's own state code. */
    private final ContainerData gegevens = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> voortgang;
                case 1 -> duur();
                case 2 -> heeftKracht() ? 1 : 0;
                case 3 -> standCode();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return MachineMenu.GEGEVENS;
        }
    };

    protected TechBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int vraag, int vakken) {
        super(type, pos, state, vraag, vakken);
    }

    /** Which machine this is (its slots and its screen). */
    public abstract MachineSoort soort();

    /** How many ticks of work one job takes (cutting one plant, gnawing one block...). */
    public abstract int duur();

    public int voortgang() {
        return voortgang;
    }

    // --- (client) the moving part ----------------------------------------------------------------------------------------

    /** (client) How many ticks the machine's moving part has moved, now and a tick ago. */
    private float beweging, oBeweging;

    /** (client) How fast the moving part goes this tick: 1 while the machine works, 0 while it does not. */
    protected float tempo() {
        return bezig() ? 1f : 0f;
    }

    @Override
    protected void clientTick() {
        oBeweging = beweging;
        beweging += tempo();
    }

    /** (client) The moving part's time in ticks of movement (it stands still while the machine does). */
    public float beweging(float partialTick) {
        return oBeweging + (beweging - oBeweging) * partialTick;
    }

    /** A number of the machine's own for the screen (what it is waiting for). */
    protected int standCode() {
        return 0;
    }

    /** The side the snoet looks at. */
    public Direction voor() {
        return getBlockState().hasProperty(MachineBlock.FACING) ? getBlockState().getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    // --- slots -------------------------------------------------------------------------------------------------------

    @Override
    protected boolean isInvoer(int vak) {
        return soort().invoer(vak);
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return soort().uitvoer(vak);
    }

    /** May this go into this slot (by hand on the screen, by a pipe, by a chore guh)? */
    public final boolean magErin(int vak, ItemResource wat) {
        return isInvoer(vak) && past(vak, wat);
    }

    /** The slots {@code van} up to and including {@code tot} as a handler of their own. */
    protected ResourceHandler<ItemResource> vakken(int van, int tot) {
        return RangedResourceHandler.of(vakken(), van, tot + 1);
    }

    /** Do ALL these stacks fit into the slots van..tot together? */
    protected boolean pastAlles(List<ItemStack> stacks, int van, int tot) {
        return stopAlles(stacks, van, tot, true);
    }

    /** Puts ALL these stacks into the slots van..tot, or none of them (false). */
    protected boolean stopAlles(List<ItemStack> stacks, int van, int tot) {
        return stopAlles(stacks, van, tot, false);
    }

    private boolean stopAlles(List<ItemStack> stacks, int van, int tot, boolean simuleer) {
        ResourceHandler<ItemResource> uit = vakken(van, tot);
        try (Transaction tx = Transaction.openRoot()) {
            for (ItemStack stack : stacks) {
                if (!Kisten.stop(uit, stack).isEmpty()) {
                    return false;   // (closing the transaction without a commit takes everything out again)
                }
            }
            if (!simuleer) {
                tx.commit();
            }
            return true;
        }
    }

    // --- the screen --------------------------------------------------------------------------------------------------

    public void open(ServerPlayer speler) {
        speler.openMenu(this, buf -> {
            buf.writeEnum(soort());
            buf.writeBlockPos(worldPosition);
        });
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this, gegevens);
    }

    public boolean dichtbij(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    // --- may it change the world there? ------------------------------------------------------------------------------

    /** How long (in ticks) an answer of {@link #magHier} is kept: protection hardly ever changes, the question is asked every tick. */
    private static final int MAG_ONTHOUDEN = 20;
    private long magPlek = Long.MIN_VALUE, magTot = Long.MIN_VALUE;
    private boolean magAntwoord;

    /**
     * May this machine change the block there on behalf of whoever placed it ({@link Bescherming#magWijzigen}: not in a
     * protected building, a protected area, the spawn protection or the huisje area of somebody else)? The answer for the
     * spot asked last is kept for a second.
     */
    protected boolean magHier(ServerLevel server, BlockPos plek) {
        long nu = server.getGameTime(), sleutel = plek.asLong();
        if (sleutel != magPlek || nu >= magTot || nu < magTot - MAG_ONTHOUDEN) {
            magPlek = sleutel;
            magTot = nu + MAG_ONTHOUDEN;
            magAntwoord = Bescherming.magWijzigen(server, plek, eigenaar());
        }
        return magAntwoord;
    }

    @Override
    public void zetEigenaar(@Nullable java.util.UUID wie) {
        super.zetEigenaar(wie);
        magTot = Long.MIN_VALUE;   // (another owner: ask again)
    }

    // --- little helpers ----------------------------------------------------------------------------------------------

    /**
     * This machine did its thing: the one who placed it (when they are online) gets the hidden advancement
     * guhs:quest/tech_machines_&lt;name&gt; (for the FTB quests) and the visible guhs:techniek/tech_machines_&lt;name&gt;.
     */
    protected void beloon(String name) {
        if (level != null && level.getServer() != null && eigenaar() != null) {
            ServerPlayer speler = level.getServer().getPlayerList().getPlayer(eigenaar());
            if (speler != null) {
                beloon(speler, name);
            }
        }
    }

    static void beloon(ServerPlayer speler, String name) {
        GuhAdvancements.grant(speler, "tech_machines_" + name);
        GidsFeature.grant(speler, "techniek/tech_machines_" + name);
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Voortgang", voortgang);
    }

    @Override
    protected void laden(ValueInput in) {
        voortgang = in.getIntOr("Voortgang", 0);
    }
}
