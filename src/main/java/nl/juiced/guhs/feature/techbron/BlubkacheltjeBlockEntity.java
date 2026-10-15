package nl.juiced.guhs.feature.techbron;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The Blubkacheltje's source: {@link VadsGetallen#BLUBKACHELTJE} VK while there is a Sausblubje in the jar and it is warm.
 * One knabbel keeps it warm for {@link TechbronGetallen#BLUB_SECONDEN} seconds; the bakje holds up to
 * {@link TechbronGetallen#BLUB_VOER_MAX} knabbels and the blubje takes the next one by itself. Knabbelbuizen and hoppers
 * may put knabbels in ({@link #handler}), nothing comes out that way.
 * <p>
 * The Sausblubje itself belongs to the sausdieren slice: this block only keeps the jar item that was put in (with all its
 * components) and gives exactly that back.
 */
public class BlubkacheltjeBlockEntity extends BronBlockEntity {
    private final Bakje bakje = new Bakje();
    private ItemStack blubje = ItemStack.EMPTY;
    /** Ticks the blubje stays warm on the knabbel it ate last. */
    private int warm;

    public BlubkacheltjeBlockEntity(BlockPos pos, BlockState state) {
        super(TechbronFeature.BLUBKACHELTJE_BE.get(), pos, state);
    }

    // --- the blubje ---

    public boolean heeftBlubje() {
        return !blubje.isEmpty();
    }

    /** The jar item that is in the stove (empty: none). */
    public ItemStack blubje() {
        return blubje;
    }

    /** Is there a blubje and is it warm (does the stove give vadskracht)? */
    public boolean warm() {
        return heeftBlubje() && warm > 0;
    }

    /** How many ticks the blubje stays warm without another knabbel. */
    public int warmTicks() {
        return warm;
    }

    /** (tests, commands) Sets how long the blubje stays warm on what it ate. */
    public void zetWarm(int ticks) {
        warm = Math.max(0, ticks);
        sync();
    }

    /** Puts this jar in (empty: takes the blubje out); returns the jar that was in it. A new blubje starts on a knabbel at once. */
    public ItemStack zetBlubje(ItemStack nieuw) {
        ItemStack oud = blubje;
        blubje = nieuw.isEmpty() ? ItemStack.EMPTY : nieuw.copyWithCount(1);
        if (blubje.isEmpty()) {
            warm = 0;   // (the knabbel it was busy with leaves with it)
        } else {
            eet();
        }
        sync();
        return oud;
    }

    // --- the knabbels ---

    /** How many knabbels lie in the bakje. */
    public int voorraad() {
        return bakje.getAmountAsInt(0);
    }

    /** Puts knabbels of this stack into the bakje (the stack shrinks unless {@code gratis}); returns how many went in. */
    public int voer(ItemStack stack, boolean gratis) {
        if (!stack.is(TechbronFeature.BLUBVOER)) {
            return 0;
        }
        ItemStack rest = Kisten.stop(bakje, stack);
        int erin = stack.getCount() - rest.getCount();
        if (erin > 0 && !gratis) {
            stack.shrink(erin);
        }
        return erin;
    }

    /** A hungry blubje takes the next knabbel from the bakje. */
    private boolean eet() {
        if (!heeftBlubje() || warm > 0 || Kisten.neem(bakje, s -> true, 1).isEmpty()) {
            return false;
        }
        warm = TechbronGetallen.BLUB_SECONDEN * 20;
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0);
        }
        return true;
    }

    /** What pipes and hoppers get: knabbels in (only the tag guhs:techbron/blubvoer), nothing out. */
    public ResourceHandler<ItemResource> handler(@Nullable Direction kant) {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return bakje.size();
            }

            @Override
            public ItemResource getResource(int index) {
                return bakje.getResource(index);
            }

            @Override
            public long getAmountAsLong(int index) {
                return bakje.getAmountAsLong(index);
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return bakje.getCapacityAsLong(index, resource);
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return bakje.isValid(index, resource);
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return bakje.insert(index, resource, amount, transaction);
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }

    private final class Bakje extends ItemStacksResourceHandler {
        Bakje() {
            super(1);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.test(stack -> stack.is(TechbronFeature.BLUBVOER));
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return TechbronGetallen.BLUB_VOER_MAX;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    }

    // --- vadskracht ---

    @Override
    public BronSoort vadsSoort() {
        return BronSoort.BLUBKACHELTJE;
    }

    @Override
    public int vadsAanbod() {
        return warm() ? VadsGetallen.BLUBKACHELTJE : 0;
    }

    @Override
    protected Snoet gezicht() {
        return !warm() ? Snoet.SLAAPT : voorraad() >= TechbronGetallen.BLUB_VOER_MAX ? Snoet.VOL : Snoet.WERKT;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (!heeftBlubje()) {
            regels.accept(Component.translatable("gui.guhs.techbron.blub.leeg").withStyle(ChatFormatting.GRAY));
        } else if (warm <= 0) {
            regels.accept(Component.translatable("gui.guhs.techbron.blub.trek").withStyle(ChatFormatting.GOLD));
        } else {
            regels.accept(Component.translatable("gui.guhs.techbron.blub.warm", voorraad(), TechbronGetallen.BLUB_VOER_MAX).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- ticking ---

    @Override
    protected void tik(ServerLevel level) {
        if (!heeftBlubje()) {
            return;
        }
        if (warm > 0) {
            if (--warm == 0 && !eet()) {
                sync();   // cold: the client sees a hungry blubje
                return;
            }
            if (warm % 20 == 0) {
                setChanged();
            }
            beloon("blub");
        } else if (eet()) {
            sync();       // a knabbel came (a pipe, a hopper): warm again
        }
    }

    /** Client: a warm blubje blubs (a soft pop now and then) and a wisp of smoke leaves the stove pipe. */
    @Override
    protected void clientTick() {
        if (level == null || !warm()) {
            return;
        }
        if (level.getRandom().nextInt(50) == 0) {
            level.playLocalSound(worldPosition, net.minecraft.sounds.SoundEvents.LAVA_POP, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.25f, 1.3f + level.getRandom().nextFloat() * 0.5f, false);
        }
        if (level.getRandom().nextInt(12) == 0) {
            // the pipe stands behind the jar, a little to the left as you look at the face (the model: x 9..12, z 13..15)
            Direction voor = getBlockState().getValue(BronBlock.FACING), achter = voor.getOpposite(), opzij = voor.getClockWise();
            double x = worldPosition.getX() + 0.5 + achter.getStepX() * 0.375 + opzij.getStepX() * 0.16;
            double z = worldPosition.getZ() + 0.5 + achter.getStepZ() * 0.375 + opzij.getStepZ() * 0.16;
            level.addParticle(ParticleTypes.SMOKE, x, worldPosition.getY() + 0.9, z, 0, 0.02, 0);
        }
    }

    // --- removal, saving ---

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            if (!blubje.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, blubje);
                blubje = ItemStack.EMPTY;
            }
            for (ItemStack stack : bakje.copyToList()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        blubje = in.read("Blubje", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        warm = Math.max(0, in.getIntOr("Warm", 0));
        bakje.deserialize(in.childOrEmpty("Bakje"));
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        if (!blubje.isEmpty()) {
            uit.store("Blubje", ItemStack.CODEC, blubje);
        }
        uit.putInt("Warm", warm);
        bakje.serialize(uit.child("Bakje"));
    }
}
