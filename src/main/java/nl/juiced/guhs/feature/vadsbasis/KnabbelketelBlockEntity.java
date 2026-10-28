package nl.juiced.guhs.feature.vadsbasis;

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
import nl.juiced.guhs.feature.techbron.BronBlockEntity;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.8.0 (vadskracht): the Knabbelketel's source: {@link VadsGetallen#KNABBELKETEL} VK while it burns. It burns kaasknabbels
 * (and blocks of kaasknabbels): one knabbel for {@link VadsGetallen#KETEL_KNABBEL_TIKKEN} ticks, a block of kaasknabbels
 * for nine times as long, so a ketel that runs day and night eats {@link VadsGetallen#KETEL_KNABBELS_PER_UUR} knabbels an hour: a
 * real, lasting use for a big knabbel farm. Knabbelbuizen and hoppers fill its bakje ({@link #handler}); nothing comes out.
 * It is a source of its own kind ({@link BronSoort#KNABBELKETEL}: at most four count per net) and not part of the Guhraden's
 * afnemende opbrengst.
 */
public class KnabbelketelBlockEntity extends BronBlockEntity {
    private final Bakje bakje = new Bakje();
    /** Ticks it still burns on what it took last. */
    private int brandt;

    public KnabbelketelBlockEntity(BlockPos pos, BlockState state) {
        super(VadsbasisFeature.KNABBELKETEL_BE.get(), pos, state);
    }

    /** How many knabbels this stack is worth per item (a block of kaasknabbels: {@link VadsGetallen#KETEL_BLOK}); 0: no fuel. */
    public static int waarde(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get()) ? 1 : stack.is(ModBlocks.BLOCK_OF_KAASKNABBELS.get().asItem()) ? VadsGetallen.KETEL_BLOK : 0;
    }

    public boolean brandt() {
        return brandt > 0;
    }

    public int brandTicks() {
        return brandt;
    }

    /** (tests, commands) */
    public void zetBrandt(int ticks) {
        brandt = Math.max(0, ticks);
        sync();
    }

    /** How many knabbels (blocks count as nine) lie in the bakje. */
    public int voorraad() {
        int n = 0;
        for (int i = 0; i < bakje.size(); i++) {
            ItemStack s = bakje.getResource(i).toStack(bakje.getAmountAsInt(i));
            n += s.getCount() * waarde(s);
        }
        return n;
    }

    /** Puts what fits of this stack in the bakje (the stack shrinks unless {@code gratis}); returns how many went in. */
    public int voer(ItemStack stack, boolean gratis) {
        if (waarde(stack) <= 0) {
            return 0;
        }
        ItemStack rest = Kisten.stop(bakje, stack.copy());
        int erin = stack.getCount() - rest.getCount();
        if (erin > 0 && !gratis) {
            stack.shrink(erin);
        }
        if (erin > 0) {
            sync();
        }
        return erin;
    }

    /** Takes the next knabbel (or block) out of the bakje and burns it. */
    private boolean stook() {
        ItemStack s = Kisten.neem(bakje, x -> waarde(x) > 0, 1);
        if (s.isEmpty()) {
            return false;
        }
        brandt = waarde(s) * VadsGetallen.KETEL_KNABBEL_TIKKEN;
        return true;
    }

    // --- vadskracht ---

    @Override
    public BronSoort vadsSoort() {
        return BronSoort.KNABBELKETEL;
    }

    @Override
    public int vadsAanbod() {
        return brandt() ? VadsGetallen.KNABBELKETEL : 0;
    }

    @Override
    protected Snoet gezicht() {
        return !brandt() ? Snoet.SLAAPT : bakje.getAmountAsInt(0) >= VadsGetallen.KETEL_BAKJE ? Snoet.VOL : Snoet.WERKT;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (!brandt()) {
            regels.accept(Component.translatable("block.guhs.knabbelketel.koud").withStyle(ChatFormatting.GOLD));
        } else {
            regels.accept(Component.translatable("block.guhs.knabbelketel.brandt", voorraad(), brandt / 20).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    protected void tik(ServerLevel level) {
        if (brandt > 0) {
            if (--brandt == 0 && !stook()) {
                sync();
                return;
            }
            if (brandt % 20 == 0) {
                setChanged();
            }
            beloon("ketel");
        } else if (stook()) {
            sync();
        }
    }

    @Override
    protected void clientTick() {
        if (level == null || !brandt()) {
            return;
        }
        if (level.getRandom().nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 0, 0.04, 0);
        }
        if (level.getRandom().nextInt(40) == 0) {
            level.playLocalSound(worldPosition, net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.5f, 1.2f, false);
        }
    }

    /** What pipes and hoppers get: knabbels and blocks of knabbels in, nothing out. */
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
            super(2);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.test(s -> waarde(s) > 0);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return VadsGetallen.KETEL_BAKJE;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    }

    // --- removal, saving ---

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            for (ItemStack s : bakje.copyToList()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        bakje.deserialize(in.childOrEmpty("Bakje"));
        brandt = in.getIntOr("Brandt", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        bakje.serialize(uit.child("Bakje"));
        uit.putInt("Brandt", brandt);
    }
}
