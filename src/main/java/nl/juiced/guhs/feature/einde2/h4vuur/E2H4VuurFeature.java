package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.titels.Titels;

/**
 * einde2 phase B/C, slice h4_vuur: chapter 4 and the finale of the Guhvatar story (DESIGN 5, CONTRACT_E2_B 7.5): the
 * capital of the Vuurnatie in disguise, the school, the secret dance party, the turtle-ducks, the Dag van de Zwarte Zon and
 * Zuka's speech, the prison, the camp and the making-up, Het Kokend Rotsblok, the Drakendans, the Sintelstrand theatre, the
 * White Lotus tent; then the Top der Voorgangers, the Leeuwschildpad-guh, the airship fleet, the Agni Kai, the duel, the
 * coronation and the credits.
 * <ul>
 *   <li>the story: {@link E2H4Vuur} (lines, direction), {@link Finale}, the minigames {@link Dansfeest}, {@link Goedmaken},
 *       {@link Rotsblok}, {@link Drakendans}, {@link Luchtvloot}, {@link Duel}; the scenes {@link Scenes};</li>
 *   <li>{@link #BEELD}: the Guhvatar statue (reward; the Hall's pedestal shows yours: {@link Beeld});</li>
 *   <li>{@link #LUCHTSCHIP}, {@link #VUURBAL}: the airships and the fire of the finale (never saved, never hurting);</li>
 *   <li>the title {@value #TITEL}, the clothes {@code VUURNATIE_VERMOMMING} and {@code ZUKA_LITTEKEN};</li>
 *   <li>the look of a player in disguise / in the Guhvatar state ({@link Look}, client.LookLaag).</li>
 * </ul>
 * Dev run only: {@code /guhs einde2 h4 ga|scene|wis|ronde} ({@link E2H4VuurCommando}). Resources: tools/features/einde2_h4_vuur.py
 * (texts: einde2_h4_vuur_tekst.py).
 */
public final class E2H4VuurFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    /** The title of the finale (CONTRACT_E2_B 7.1). */
    public static final String TITEL = "guhvatar";

    public static final DeferredBlock<GuhvatarBeeldBlock> BEELD = BLOCKS.registerBlock("guhvatar_beeld", GuhvatarBeeldBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6.0f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BeeldItem> BEELD_ITEM = ITEMS.registerItem("guhvatar_beeld", p -> new BeeldItem(BEELD.get(), p),
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhvatarBeeldBlockEntity>> BEELD_BE = BLOCK_ENTITIES.register(
            "guhvatar_beeld", () -> new BlockEntityType<>(GuhvatarBeeldBlockEntity::new, BEELD.get()));

    public static final DeferredHolder<EntityType<?>, EntityType<LuchtschipEntity>> LUCHTSCHIP = ENTITY_TYPES.register("einde2_h4_vuur_luchtschip",
            () -> EntityType.Builder.<LuchtschipEntity>of(LuchtschipEntity::new, MobCategory.MISC).sized(4.0f, 3.0f).fireImmune().noSave().noSummon()
                    .clientTrackingRange(16).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_h4_vuur_luchtschip"))));
    public static final DeferredHolder<EntityType<?>, EntityType<VuurbalEntity>> VUURBAL = ENTITY_TYPES.register("einde2_h4_vuur_vuurbal",
            () -> EntityType.Builder.<VuurbalEntity>of(VuurbalEntity::new, MobCategory.MISC).sized(0.8f, 0.8f).fireImmune().noSave().noSummon()
                    .clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_h4_vuur_vuurbal"))));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        E2H4Vuur.register();
        Schijn.registreer(Beeld::vul);
        Titels.registreer(new Titels.Titel(TITEL, "gui.guhs.titel." + TITEL, ChatFormatting.GOLD, "guhs:guhvatar_beeld", E2Verhaal::finaleKlaar));
        KledingBronnen.bron(GuhClothes.VUURNATIE_VERMOMMING, "einde2_h4_vuur");
        KledingBronnen.bron(GuhClothes.ZUKA_LITTEKEN, "einde2_h4_vuur");
    }

    public static void payloads(PayloadRegistrar registrar) {
        Look.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BEELD_ITEM.get()));
    }

    /** The statue as an item: whose statue it is, and a line of lore. */
    public static class BeeldItem extends BlockItem {
        public BeeldItem(GuhvatarBeeldBlock block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            var profiel = stack.get(net.minecraft.core.component.DataComponents.PROFILE);
            String naam = profiel == null ? null : profiel.name().orElse(null);
            if (naam != null) {
                tooltip.accept(Component.translatable("item.guhs.guhvatar_beeld.van", naam).withStyle(ChatFormatting.GOLD));
            }
            tooltip.accept(Component.translatable("item.guhs.guhvatar_beeld.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private E2H4VuurFeature() {
    }
}
