package nl.juiced.guhs.feature.bio;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.bio.blokkendal.BlokkenDalSlice;
import nl.juiced.guhs.feature.bio.blokkenwolk.BlokkenWolkSlice;
import nl.juiced.guhs.feature.bio.bouwdal.BouwDalSlice;
import nl.juiced.guhs.feature.bio.bouwmeer.BouwMeerSlice;
import nl.juiced.guhs.feature.bio.bouwwolk1.BouwWolk1Slice;
import nl.juiced.guhs.feature.bio.bouwwolk2.BouwWolk2Slice;
import nl.juiced.guhs.feature.bio.dieren.DierenSlice;
import nl.juiced.guhs.feature.bio.kompas.KompasSlice;
import nl.juiced.guhs.feature.bio.systemen.SystemenSlice;
import nl.juiced.guhs.feature.bio.wereld.WereldSlice;

/**
 * biomes3: three new Guhmensie biomes (Bloesemmeertje, Klaterdal, Wolkenweide) with their blocks, animals and structures,
 * and the Superkompas tab "Biomes". This class is the kern: it registers the shared things and then calls every slice's
 * entry class in table order, so a slice never edits a shared registry: it owns its DeferredRegisters inside
 * {@code <Pkg>Slice}. Resources: tools/features/bio.py (kern) and tools/features/bio_&lt;slice&gt;.py.
 * <p>
 * API for the slices (all in this package): {@link Bio} (the biome keys, things of other slices by id), {@link Bezocht}
 * (has this player ever been in a dimension / a biome), {@link BiomeLijst} (the sections and biomes of the Superkompas
 * tab), {@link BioZelftest} (checks for the dev server: {@code /guhs bio zelftest}).
 */
public final class BioFeature {
    public static void register(IEventBus modBus) {
        Bezocht.register();
        NeoForge.EVENT_BUS.addListener(BioZelftest::commando);
        BioZelftest.kern();
        // the slices, in table order (the blocks first: the others use them)
        BlokkenDalSlice.register(modBus);
        BlokkenWolkSlice.register(modBus);
        WereldSlice.register(modBus);
        DierenSlice.register(modBus);
        KompasSlice.register(modBus);
        BouwDalSlice.register(modBus);
        BouwMeerSlice.register(modBus);
        BouwWolk1Slice.register(modBus);
        BouwWolk2Slice.register(modBus);
        SystemenSlice.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
        BlokkenDalSlice.payloads(registrar);
        BlokkenWolkSlice.payloads(registrar);
        WereldSlice.payloads(registrar);
        DierenSlice.payloads(registrar);
        KompasSlice.payloads(registrar);
        BouwDalSlice.payloads(registrar);
        BouwMeerSlice.payloads(registrar);
        BouwWolk1Slice.payloads(registrar);
        BouwWolk2Slice.payloads(registrar);
        SystemenSlice.payloads(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        BlokkenDalSlice.creative(output);
        BlokkenWolkSlice.creative(output);
        WereldSlice.creative(output);
        DierenSlice.creative(output);
        KompasSlice.creative(output);
        BouwDalSlice.creative(output);
        BouwMeerSlice.creative(output);
        BouwWolk1Slice.creative(output);
        BouwWolk2Slice.creative(output);
        SystemenSlice.creative(output);
    }

    private BioFeature() {
    }
}
