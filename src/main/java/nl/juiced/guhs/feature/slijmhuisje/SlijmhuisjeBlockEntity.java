package nl.juiced.guhs.feature.slijmhuisje;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * The inside of a Slijmhuisje: one guh slime lives here as data (the {@link OpgepakteGuhSlimeItem} it came in with, kept
 * whole, so it comes out exactly as it went in), and a little bowl (slot 0, out only: pipes and hoppers may take from it).
 * <p>
 * On vadskracht ({@link VadsGetallen#SLIJMHUISJE}) a slime that lives here makes one guh slimeball every {@link #TIJD}
 * ticks (45 seconds, the same for every size), until the bowl holds {@link #SCHAAL_MAX}: then it waits, content (the snoet
 * shows "vol"). Without vadskracht it sleeps and nothing is made. No food, no treats: a sweet little house, that's all.
 * <p>
 * Clients get the slime and how full the bowl is (for the renderer), not the slot itself.
 */
public class SlijmhuisjeBlockEntity extends MachineBlockEntity {
    /** Ticks of work per slimeball: 45 seconds. */
    public static final int TIJD = 45 * 20;
    /** What the bowl holds; full = production pauses. */
    public static final int SCHAAL_MAX = 16;

    /** The slime that lives here (an {@link OpgepakteGuhSlimeItem} stack), or empty. */
    private ItemStack slijm = ItemStack.EMPTY;
    /** Ticks of work towards the next slimeball. */
    int voortgang;
    /** (client) how many slimeballs lie in the bowl (the slot is not synced). */
    private int schaalClient;

    public SlijmhuisjeBlockEntity(BlockPos pos, BlockState state) {
        super(SlijmhuisjeFeature.SLIJMHUISJE_BE.get(), pos, state, VadsGetallen.SLIJMHUISJE, 1);
    }

    // =====================================================================================================================
    // the slime
    // =====================================================================================================================

    public boolean heeftSlijm() {
        return !slijm.isEmpty();
    }

    /** A copy of the slime's item (empty: nobody lives here). */
    public ItemStack slijm() {
        return slijm.copy();
    }

    /** The slime's size (1, 2, 4), 0 when nobody lives here. */
    public int grootte() {
        return heeftSlijm() ? OpgepakteGuhSlimeItem.grootte(slijm) : 0;
    }

    /** Lets this slime move in (one item of the stack). False: somebody lives here already, or it is no slime. */
    public boolean trekIn(ItemStack stack) {
        if (heeftSlijm() || !(stack.getItem() instanceof OpgepakteGuhSlimeItem)) {
            return false;
        }
        slijm = stack.copyWithCount(1);
        voortgang = 0;
        sync();
        return true;
    }

    /** The slime moves out: its item (empty when nobody lived here). */
    public ItemStack trekUit() {
        ItemStack uit = slijm;
        slijm = ItemStack.EMPTY;
        voortgang = 0;
        sync();
        return uit;
    }

    // =====================================================================================================================
    // the bowl
    // =====================================================================================================================

    /** How many slimeballs lie in the bowl (server: the slot; client: what was synced). */
    public int schaal() {
        if (level != null && level.isClientSide()) {
            return schaalClient;
        }
        return vakken().getAmountAsInt(0);
    }

    /** Empties the bowl: the slimeballs (an empty stack when there were none). */
    public ItemStack leegSchaal() {
        int n = vakken().getAmountAsInt(0);
        if (n <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack uit = vakken().getResource(0).toStack(n);
        vakken().set(0, ItemResource.EMPTY, 0);
        return uit;
    }

    public int voortgang() {
        return voortgang;
    }

    @Override
    protected boolean isInvoer(int vak) {
        return false;   // (nothing goes into the bowl from outside)
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return true;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return wat.is(ModItems.GUH_SLIMEBALL.get());
    }

    @Override
    protected void vakkenVeranderd() {
        sync();   // (the renderer shows the slimeballs in the bowl)
    }

    // =====================================================================================================================
    // the work
    // =====================================================================================================================

    @Override
    protected boolean kanWerken() {
        return heeftSlijm() && schaal() < SCHAAL_MAX;
    }

    /** Content: the bowl is full, the slime waits until somebody empties it. */
    @Override
    protected boolean isVol() {
        return heeftSlijm() && schaal() >= SCHAAL_MAX;
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (++voortgang < TIJD) {
            if (voortgang % 100 == 0) {
                BlockPos k = kussen();
                server.sendParticles(ParticleTypes.HEART, k.getX() + 0.5, k.getY() + 0.9, k.getZ() + 0.5, 1, 0.3, 0.2, 0.3, 0.0);
            }
            return;
        }
        voortgang = 0;
        int nu = vakken().getAmountAsInt(0);
        vakken().set(0, ItemResource.of(ModItems.GUH_SLIMEBALL.get()), nu + 1);
        BlockPos k = kussen();
        server.playSound(null, k, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.7f, 1.3f);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, k.getX() + 0.5, k.getY() + 0.5, k.getZ() + 0.5, 4, 0.4, 0.3, 0.4, 0.0);
        beloon();
        setChanged();
    }

    /** The side the door and the snoet look at. */
    public Direction voor() {
        return getBlockState().hasProperty(MachineBlock.FACING) ? getBlockState().getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    /** The cushion the slime bounces on: the middle of the house, on top of the base. */
    public BlockPos kussen() {
        return worldPosition.relative(voor().getOpposite()).above();
    }

    /** The first slimeball: the one who placed the house gets guhs:techniek/tech_machines_slijmhuisje (+ the hidden quest twin). */
    private void beloon() {
        if (level != null && level.getServer() != null && eigenaar() != null) {
            ServerPlayer speler = level.getServer().getPlayerList().getPlayer(eigenaar());
            if (speler != null) {
                GuhAdvancements.grant(speler, "tech_machines_slijmhuisje");
                GidsFeature.grant(speler, "techniek/tech_machines_slijmhuisje");
            }
        }
    }

    // =====================================================================================================================
    // hover readout, removal, saving
    // =====================================================================================================================

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        String k = "gui.guhs.slijmhuisje.";
        if (!heeftSlijm()) {
            regels.accept(Component.translatable(k + "leeg").withStyle(ChatFormatting.GRAY));
            return;
        }
        regels.accept(Component.translatable(k + "bewoner", slijm.getHoverName(), grootte()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (schaal() >= SCHAAL_MAX) {
            regels.accept(Component.translatable(k + "schaal_vol", SCHAAL_MAX).withStyle(ChatFormatting.AQUA));
        } else if (!heeftKracht()) {
            regels.accept(Component.translatable(k + "slaapt", schaal(), SCHAAL_MAX).withStyle(ChatFormatting.GOLD));
        } else {
            regels.accept(Component.translatable(k + "maakt", schaal(), SCHAAL_MAX, Math.max(1, (TIJD - voortgang + 19) / 20))
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /** Removed: the slime falls out in its item too (the bowl's slimeballs are dropped by the machine). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide() && heeftSlijm()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slijm);
            slijm = ItemStack.EMPTY;
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Voortgang", voortgang);
        if (!slijm.isEmpty()) {
            uit.store("Slijm", ItemStack.CODEC, slijm);
        }
        uit.putInt("Schaal", vakken().getAmountAsInt(0));
    }

    @Override
    protected void laden(ValueInput in) {
        voortgang = in.getIntOr("Voortgang", 0);
        slijm = in.read("Slijm", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        schaalClient = in.getIntOr("Schaal", 0);
    }
}
