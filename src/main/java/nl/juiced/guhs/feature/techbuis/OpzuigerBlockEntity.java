package nl.juiced.guhs.feature.techbuis;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The block entity of the Opzuiger: with vadskracht ({@link VadsGetallen#OPZUIGER}) it looks for loose items within
 * {@link #BEREIK} blocks every {@link #KIJK} ticks, pulls them towards its snoet and slurps up what comes close, into its
 * nine slots (all of them "out" slots: pipes and hoppers only take). It leaves alone: items that were only just dropped
 * (their pick-up delay), loaned items, items marked {@code PreventRemoteMovement}, and whatever lies in a protected
 * building or in the area of somebody else's Guhhuisje ({@link Bescherming#magWijzigen}). The face is surprised when
 * something it wants does not fit any more.
 */
public class OpzuigerBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int VAKKEN = 9;
    /** How far it reaches (a cube around the block), how often it looks, and how many items it pulls at once. */
    public static final int BEREIK = 4, KIJK = 5, MAX_BUIT = 24;
    /** From this close an item is slurped up. */
    private static final double HAP_AFSTAND = 1.35;

    private final List<ItemEntity> buit = new ArrayList<>();
    /** The game time of its last look around (-1: not looked yet). */
    private long gekeken = -1;
    private boolean vol;

    public OpzuigerBlockEntity(BlockPos pos, BlockState state) {
        super(TechbuisFeature.OPZUIGER_BE.get(), pos, state, VadsGetallen.OPZUIGER, VAKKEN);
    }

    @Override
    protected boolean isInvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return true;
    }

    /** The middle of its snoet: just in front of the block. */
    private Vec3 mond() {
        Direction voor = getBlockState().getValue(MachineBlock.FACING);
        return Vec3.atCenterOf(worldPosition).add(voor.getStepX() * 0.45, -0.1, voor.getStepZ() * 0.45);
    }

    private boolean wilHebben(ServerLevel server, ItemEntity item) {
        ItemStack stack = item.getItem();
        return item.isAlive() && !stack.isEmpty() && !item.hasPickUpDelay() && !Features.isLoaned(stack)
                && !item.getPersistentData().getBooleanOr("PreventRemoteMovement", false)
                && Bescherming.magWijzigen(server, item.blockPosition(), eigenaar());
    }

    /** Looks around (not every tick) and says whether there is something to slurp. */
    @Override
    protected boolean kanWerken() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        long nu = server.getGameTime();
        if (gekeken < 0 || nu - gekeken >= KIJK || nu < gekeken) {
            gekeken = nu;
            buit.clear();
            for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(BEREIK), i -> wilHebben(server, i))) {
                buit.add(item);
                if (buit.size() >= MAX_BUIT) {
                    break;
                }
            }
            if (buit.isEmpty()) {
                vol = false;
            }
        }
        return !buit.isEmpty();
    }

    @Override
    protected boolean isVol() {
        return vol;
    }

    @Override
    protected void vakkenVeranderd() {
        vol = false;
    }

    /** Pulls what it saw towards its snoet and slurps up what is close. */
    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 mond = mond();
        for (ItemEntity item : buit) {
            if (!item.isAlive()) {
                continue;
            }
            Vec3 weg = mond.subtract(item.position());
            double afstand = weg.length();
            if (afstand < HAP_AFSTAND) {
                slurp(server, item);
            } else {
                // (a little more than gravity, so things also come up from below)
                item.setDeltaMovement(item.getDeltaMovement().scale(0.8).add(weg.scale(0.09 / afstand)));
                item.needsSync = true;
            }
        }
    }

    private void slurp(ServerLevel server, ItemEntity item) {
        ItemStack stack = item.getItem();
        ItemStack rest = Kisten.stop(vakken(), stack);
        if (rest.getCount() == stack.getCount()) {
            vol = true;   // (vakkenVeranderd makes it false again as soon as something is taken out)
            return;
        }
        boolean wasVol = !rest.isEmpty();
        if (rest.isEmpty()) {
            item.discard();
        } else {
            item.setItem(rest);
        }
        vol = wasVol;
        Vec3 mond = mond();
        server.playSound(null, mond.x, mond.y, mond.z, SoundEvents.GENERIC_DRINK.value(), SoundSource.BLOCKS, 0.35f, 1.5f + server.getRandom().nextFloat() * 0.4f);
        server.sendParticles(ParticleTypes.POOF, mond.x, mond.y, mond.z, 3, 0.1, 0.1, 0.1, 0.01);
    }

    /** While it slurps: little clouds that fly into its snoet. */
    @Override
    protected void clientTick() {
        if (level == null || !bezig() || level.getGameTime() % 3 != 0) {
            return;
        }
        Vec3 mond = mond();
        var random = level.getRandom();
        double x = mond.x + (random.nextDouble() - 0.5) * 3, y = mond.y + (random.nextDouble() - 0.3) * 1.5, z = mond.z + (random.nextDouble() - 0.5) * 3;
        level.addParticle(ParticleTypes.CLOUD, x, y, z, (mond.x - x) * 0.12, (mond.y - y) * 0.12, (mond.z - z) * 0.12);
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (vol) {
            regels.accept(Component.translatable("gui.guhs.techbuis.opzuiger.vol").withStyle(ChatFormatting.GOLD));
        } else if (heeftKracht()) {
            regels.accept(Component.translatable("gui.guhs.techbuis.opzuiger.bereik", BEREIK).withStyle(ChatFormatting.GRAY));
        }
    }

    // --- the screen ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OpzuigerMenu(id, inventory, vakken(), ContainerLevelAccess.create(level, worldPosition));
    }
}
