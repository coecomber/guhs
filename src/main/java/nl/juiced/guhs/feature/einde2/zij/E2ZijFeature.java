package nl.juiced.guhs.feature.einde2.zij;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.vesting.VestingCellen;
import nl.juiced.guhs.feature.titels.Titels;

/**
 * einde2 phase B, slice zij (CONTRACT_E2_B 7.6): the side places of the Guheinde and their quests, the four lasting powers
 * on one key ({@link Krachten}, {@link ZijKrachten}, the client's wheel and icon), the Opper-Mika's entrance and the poem
 * at the end of the Guheinde ({@link ZijScenes}), wink 10.
 * <ul>
 *   <li>the eight side lines ({@link ZijLijnen}): {@link Omashguh}, {@link Luchttempel}, {@link Vuurtempel},
 *       {@link KyoshiDorp}, {@link Bibliotheek}, {@link Leeuwschildpad}, {@link MikaKok}, {@link VetbootjeOpdracht};</li>
 *   <li>the Leegtehaven's market and ferry ({@link Leegtehaven}), the ring race ({@link Ringenrace});</li>
 *   <li>the titles {@code vestingbevrijder} and {@code ringenracer};</li>
 *   <li>the dev commands ({@link ZijCommando}).</li>
 * </ul>
 * Resources: tools/features/einde2_zij.py (+ _tekst, _wiki). Chunk c96_e2_zij.
 */
public final class E2ZijFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The small earth pillar of the lasting earth power (no item, no drops, crumbles by itself). */
    public static final DeferredBlock<AardzuilBlock> AARDZUIL = BLOCKS.registerBlock("einde2_zij_aardzuil", AardzuilBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6f).sound(SoundType.PACKED_MUD).noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<ZijItems.KristalsnoepRing> KRISTALSNOEP_RING = ITEMS.registerItem("einde2_zij_kristalsnoep_ring",
            ZijItems.KristalsnoepRing::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ZijItems.Zegenschub> ZEGENSCHUB = ITEMS.registerItem("einde2_zij_zegenschub", ZijItems.Zegenschub::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<ZijItems.Lore> SPIJTSOEP = ITEMS.registerItem("einde2_zij_spijtsoep", ZijItems.Lore::new,
            () -> new Item.Properties().stacksTo(16).usingConvertsTo(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build()));

    public static final String TITEL_VESTING = "vestingbevrijder", TITEL_RACE = "ringenracer";

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        ZijLijnen.lijnen();
        ZijKrachten.register();
        Omashguh.register();
        Luchttempel.register();
        Vuurtempel.register();
        KyoshiDorp.register();
        Bibliotheek.register();
        Leeuwschildpad.register();
        MikaKok.register();
        VetbootjeOpdracht.register();
        Leegtehaven.register();
        Ringenrace.register();
        ZijScenes.register();
        ZijEvents.register();
        Titels.registreer(new Titels.Titel(TITEL_VESTING, "gui.guhs.titel." + TITEL_VESTING, ChatFormatting.GOLD, "minecraft:iron_bars",
                p -> VestingCellen.bevrijd(p) >= VestingCellen.BEVRIJDER));
        Titels.registreer(new Titels.Titel(TITEL_RACE, "gui.guhs.titel." + TITEL_RACE, ChatFormatting.AQUA, "guhs:guhvleugels", Ringenrace::titel));
        NeoForge.EVENT_BUS.addListener(ZijCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KrachtPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KRISTALSNOEP_RING.get()));
        output.accept(new ItemStack(ZEGENSCHUB.get()));
        output.accept(new ItemStack(SPIJTSOEP.get()));
    }

    private E2ZijFeature() {
    }
}
