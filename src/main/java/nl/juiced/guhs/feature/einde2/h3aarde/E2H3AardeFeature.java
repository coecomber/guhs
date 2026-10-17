package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.balto.BaltoBlocks;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.zij.Krachten;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 phase B, slice h3_aarde: chapter 3 of the Guhvatar story (Ba Sing Guh, De Jasmijn Draak, Appa's lost days, Lu Ten,
 * the fall). The story itself: {@link E2H3Aarde} (the line and its steps), {@link H3Verloop} (what happens where),
 * {@link H3Rollen} (who says what), {@link H3Scenes} (the camera scenes); the games: {@link H3Monorail}, {@link H3Blinddoek},
 * {@link H3Thee}, {@link PaiSho} ({@link PaiShoSpel}, {@link PaiShoTafelBlockEntity}), {@link H3Sluipen}; per player what you
 * see: {@link H3Schijn}, {@link H3Kool}, {@link H3Personen}, {@link H3Acteurs}; at home: {@link H3Blokken} and
 * {@link H3Theebezoek}. Dev: {@link E2H3AardeCommando}. Resources: tools/features/einde2_h3_aarde.py (+ _tekst, _wiki) and
 * the story additions of the build in tools/features/einde2_bouw_aarde_verhaal.py.
 */
public final class E2H3AardeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    // --- what you take home ------------------------------------------------------------------------------------------------
    public static final DeferredBlock<H3Blokken.Tafel> PAI_SHO_TAFEL = BLOCKS.registerBlock("pai_sho_tafel", H3Blokken.Tafel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<H3Blokken.Theeset> THEESET = BLOCKS.registerBlock("theeset", H3Blokken.Theeset::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.8f).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredBlock<H3Blokken.Koolkraam> KOOLKRAAM = BLOCKS.registerBlock("einde2_h3_aarde_koolkraam", H3Blokken.Koolkraam::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> PAI_SHO_TAFEL_ITEM = ITEMS.registerItem("pai_sho_tafel",
            p -> new BaltoBlocks.LoreBlock(PAI_SHO_TAFEL.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> THEESET_ITEM = ITEMS.registerItem("theeset",
            p -> new BaltoBlocks.LoreBlock(THEESET.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> KOOLKRAAM_ITEM = ITEMS.registerItem("einde2_h3_aarde_koolkraam",
            p -> new BaltoBlocks.LoreBlock(KOOLKRAAM.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaiShoTafelBlockEntity>> PAI_SHO_TAFEL_BE = BLOCK_ENTITIES.register("pai_sho_tafel",
            () -> new BlockEntityType<>(PaiShoTafelBlockEntity::new, PAI_SHO_TAFEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<H3Blokken.TheesetBlockEntity>> THEESET_BE = BLOCK_ENTITIES.register("theeset",
            () -> new BlockEntityType<>(H3Blokken.TheesetBlockEntity::new, THEESET.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);

        E2H3Aarde.LIJN.opStap(E2H3Aarde::naStap);
        Verteller.registreer(E2H3Aarde.KAART, E2H3Aarde.KAART_REGELS, E2H3Aarde.LIJN.id());
        H3Scenes.registreer();
        H3Rollen.registreer();
        H3Schijn.registreer();
        H3Hints.registreer();
        for (GuhClothes c : E2H3Aarde.KLEDING) {
            KledingBronnen.bron(c, E2H3Aarde.KLEDINGBRON);
        }
        // the story power of the chapter: the tremor of Toph's lesson, only inside Ba Sing Guh (zij's key and wheel show it)
        Krachten.registreer(new Krachten.Kracht(H3Blinddoek.KRACHT, Einde2Ids.Element.AARDE, "guhs:einde2_bouw_aarde_aardemunt", 40,
                (p, voet) -> E2H3Aarde.stap(p) >= E2H3Aarde.BLINDDOEK && inStad(p),
                (p, kijk) -> {
                    E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
                    if (stad != null) {
                        H3Blinddoek.tril(p, stad, true);
                    }
                }));

        NeoForge.EVENT_BUS.addListener(H3Verloop::opTick);
        NeoForge.EVENT_BUS.addListener(H3Monorail::opTick);
        NeoForge.EVENT_BUS.addListener(H3Sluipen::opTick);
        NeoForge.EVENT_BUS.addListener(H3Thee::opTick);
        NeoForge.EVENT_BUS.addListener(H3Thee::opKlik);
        NeoForge.EVENT_BUS.addListener(PaiShoSpel::opTick);
        NeoForge.EVENT_BUS.addListener(H3Theebezoek::opTick);
        NeoForge.EVENT_BUS.addListener(H3Acteurs::opBinnenkomst);
        NeoForge.EVENT_BUS.addListener(H3Rollen::opBinnenkomst);
        NeoForge.EVENT_BUS.addListener(H3Acteurs::opUitloggen);
        NeoForge.EVENT_BUS.addListener(E2H3AardeCommando::register);
    }

    /** Is the player inside (the box of) Ba Sing Guh? */
    static boolean inStad(net.minecraft.server.level.ServerPlayer p) {
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        return stad != null && stad.doos(0).contains(p.position());
    }

    public static void payloads(PayloadRegistrar registrar) {
        H3Payloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PAI_SHO_TAFEL_ITEM.get()));
        output.accept(new ItemStack(THEESET_ITEM.get()));
        output.accept(new ItemStack(KOOLKRAAM_ITEM.get()));
    }

    private E2H3AardeFeature() {
    }
}
