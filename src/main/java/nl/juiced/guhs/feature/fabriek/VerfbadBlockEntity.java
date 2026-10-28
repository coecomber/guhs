package nl.juiced.guhs.feature.fabriek;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;

/**
 * Het Verfbad (1.8.0): a tub of paint with a guh face. Slot 0: wool (or anything else the game dyes: knuffelblokken, glass,
 * terracotta...), slot 1: the dye, slot 2: the dyed blocks ({@link Verven}: one block per dye, or eight around one).
 */
public class VerfbadBlockEntity extends TechBlockEntity {
    public static final int BLOK = 0, VERF = 1, UIT = 2;

    public VerfbadBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.VERFBAD_BE.get(), pos, state, FabriekGetallen.VERFBAD, 3);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.VERFBAD;
    }

    @Override
    public int duur() {
        return FabriekGetallen.VERF_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return vak == VERF ? FabriekFeature.isVerf(wat.toStack()) : vak == BLOK && !FabriekFeature.isVerf(wat.toStack());
    }

    @Nullable
    private Verven.Uitkomst uitkomst() {
        if (!(level instanceof ServerLevel server) || vakken().getAmountAsInt(BLOK) == 0 || vakken().getAmountAsInt(VERF) == 0) {
            return null;
        }
        Verven.Uitkomst u = Verven.van(server, vakken().getResource(BLOK).toStack(), vakken().getResource(VERF).toStack());
        return u != null && vakken().getAmountAsInt(BLOK) >= u.blokken() ? u : null;
    }

    private boolean ruimte(ItemStack komt) {
        ItemResource ligt = vakken().getResource(UIT);
        return ligt.isEmpty() || ligt.matches(komt) && vakken().getAmountAsInt(UIT) + komt.getCount() <= vakken().getCapacityAsInt(UIT, ligt);
    }

    @Override
    protected boolean kanWerken() {
        Verven.Uitkomst u = uitkomst();
        if (u == null || !ruimte(u.resultaat())) {
            voortgang = 0;
            return false;
        }
        return true;
    }

    @Override
    protected boolean isVol() {
        Verven.Uitkomst u = uitkomst();
        return u != null && !ruimte(u.resultaat());
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        Verven.Uitkomst u = uitkomst();
        if (u == null || !ruimte(u.resultaat())) {
            return;
        }
        vakken().set(BLOK, vakken().getResource(BLOK), vakken().getAmountAsInt(BLOK) - u.blokken());
        vakken().set(VERF, vakken().getResource(VERF), vakken().getAmountAsInt(VERF) - 1);
        vakken().set(UIT, ItemResource.of(u.resultaat()), vakken().getAmountAsInt(UIT) + u.resultaat().getCount());
        server.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                6, 0.25, 0.05, 0.25, 0.02);
        server.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.3f, 1.5f);
        FabriekFeature.beloon(this, "fabriek_verf");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (vakken().getAmountAsInt(VERF) == 0) {
            regels.accept(Component.translatable("gui.guhs.fabriek.verfbad.geen_verf").withStyle(ChatFormatting.GRAY));
        }
    }
}
