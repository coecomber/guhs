package nl.juiced.guhs.feature.knabbelspelen;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SoundType;
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
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.doolhof.DoolhofBlocks;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.world.ModDimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.TooltipDisplay;
/**
 * De Knabbelspelen (2.9, De Grote Guhspelen; structure knabbelspelen in the Guhweides and Roze pluisjes): a big striped
 * circus tent with a guh face and two guh ears as tent tops, and six play fields around it. Juf Vahoegsakee
 * (SPELLEIDERGUH, {@link KnabbelspelenRole}) runs the six events ({@link Onderdeel}: knabbelhappen, zaklopen,
 * Mika-blikgooien, eierlopen, spijkerpoepen, guhguhtje prik), each on its own or all six as the Grote Zeskamp
 * ({@link Wedstrijd}); up to four friends play at once, each in their own lane. Spelenlintjes buy the sports outfit.
 * Resources: tools/features/knabbelspelen.py (+ knabbelspelen_bouw.py, knabbelspelen_tex.py).
 */
public final class KnabbelspelenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks --------------------------------------------------------------------------------------------------------
    /** A tin with a Mika face (Mika-blikgooien; also a fun deco block). */
    public static final DeferredBlock<KnabbelspelenBlocks.Blik> BLIK = BLOCKS.registerBlock("knabbelspelen_blik", KnabbelspelenBlocks.Blik::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.4f, 2f).sound(SoundType.LANTERN).noOcclusion());
    /** A big kaasmelk bottle (Spijkerpoepen). */
    public static final DeferredBlock<KnabbelspelenBlocks.KaasmelkFles> KAASMELKFLES = BLOCKS.registerBlock("knabbelspelen_kaasmelkfles",
            KnabbelspelenBlocks.KaasmelkFles::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f, 2f).sound(SoundType.GLASS).noOcclusion());
    /** The invisible anchor under Juf Vahoegsakee. */
    public static final DeferredBlock<DoolhofBlocks.AnkerBlock> ANKER = BLOCKS.registerBlock("knabbelspelen_anker", DoolhofBlocks.AnkerBlock::new,
            () -> BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion().isValidSpawn((s, l, p, e) -> false));

    // --- items ---------------------------------------------------------------------------------------------------------
    /** The coin of the Knabbelspelen: a pink ribbon with a golden guh medal. */
    public static final DeferredItem<Item> SPELENLINTJE = ITEMS.registerSimpleItem("spelenlintje", () -> new Item.Properties());
    /** The loaned things (tag guhs:loaned; Juf Vahoegsakee takes them back after every event). */
    public static final DeferredItem<Item> GUH_ZAK = ITEMS.registerItem("guh_zak", p -> new Geleend(p, "item.guhs.guh_zak.lore"),
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> KNABBELEI_LEPEL = ITEMS.registerItem("knabbelei_lepel", p -> new Geleend(p, "item.guhs.knabbelei_lepel.lore"),
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> KNABBELSPIJKER = ITEMS.registerItem("knabbelspijker", p -> new Geleend(p, "item.guhs.knabbelspijker.lore"),
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<KnabbelspelenBlocks.Staartje> GUHGUHTJE_STAARTJE = ITEMS.registerItem("guhguhtje_staartje",
            KnabbelspelenBlocks.Staartje::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<KnabbelspelenBlocks.Pluisbal> BLIK_PLUISBAL = ITEMS.registerItem("blik_pluisbal", KnabbelspelenBlocks.Pluisbal::new,
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<BlockItem> BLIK_ITEM = ITEMS.registerSimpleBlockItem(BLIK);
    public static final DeferredItem<BlockItem> KAASMELKFLES_ITEM = ITEMS.registerSimpleBlockItem(KAASMELKFLES);

    /** A loaned item with a line of lore. */
    static class Geleend extends Item {
        private final String lore;

        Geleend(Properties properties, String lore) {
            super(properties);
            this.lore = lore;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable(lore).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Is this one of the Knabbelspelen's loaned things? */
    public static boolean geleend(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(GUH_ZAK.get()) || stack.is(KNABBELEI_LEPEL.get()) || stack.is(KNABBELSPIJKER.get())
                || stack.is(GUHGUHTJE_STAARTJE.get()) || stack.is(BLIK_PLUISBAL.get()));
    }

    static boolean heeftGeleend(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (geleend(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    // --- the moving things -----------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<SpelDing>> DING = ENTITY_TYPES.register("knabbelspelen_ding",
            () -> EntityType.Builder.<SpelDing>of(SpelDing::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(6).updateInterval(1)
                    .noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("guhs:knabbelspelen_ding"))));

    // --- sounds --------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUIT = sound("knabbelspelen.fluit");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUICH = sound("knabbelspelen.juich");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAP = sound("knabbelspelen.hap");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOP = sound("knabbelspelen.hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLIK_KLANG = sound("knabbelspelen.blik");
    public static final DeferredHolder<SoundEvent, SoundEvent> EI_KAPOT = sound("knabbelspelen.ei_kapot");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLONK = sound("knabbelspelen.plonk");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final NpcRole ROLE = new KnabbelspelenRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        Minigames.registerGame(Minigames.KNABBELSPELEN, Wedstrijd::isPlaying);
        NeoForge.EVENT_BUS.addListener(Wedstrijd::onDamage);
        NeoForge.EVENT_BUS.addListener(Wedstrijd::onDeath);
        NeoForge.EVENT_BUS.addListener(Wedstrijd::onLogout);
        NeoForge.EVENT_BUS.addListener(Wedstrijd::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(Wedstrijd::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wedstrijd::onServerStopped);
        NeoForge.EVENT_BUS.addListener(Zaklopen::onJump);
        NeoForge.EVENT_BUS.addListener(Blikgooien::onImpact);
        NeoForge.EVENT_BUS.register(KnabbelspelenProtection.class);
        Protected.add((level, pos) -> level instanceof net.minecraft.server.level.ServerLevel server && server.dimension() == ModDimensions.GUHMENSION
                && KnabbelspelenProtection.inSpelen(server, pos));
        // the sports outfit: only from Juf Vahoegsakee's shop
        KledingBronnen.bron(GuhClothes.SPELEN_ZWEETBANDJE, "knabbelspelen", KnabbelspelenRole.ZWEETBANDJE + " spelenlintjes");
        KledingBronnen.bron(GuhClothes.SPELEN_FLUITJE, "knabbelspelen", KnabbelspelenRole.FLUITJE + " spelenlintjes");
        KledingBronnen.bron(GuhClothes.SPELEN_SPORTSHIRTJE, "knabbelspelen", KnabbelspelenRole.SPORTSHIRTJE + " spelenlintjes");
    }

    public static void payloads(PayloadRegistrar registrar) {
        KnabbelspelenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SPELENLINTJE.get()));
        output.accept(new ItemStack(BLIK_ITEM.get()));
        output.accept(new ItemStack(KAASMELKFLES_ITEM.get()));
    }

    /** Juf Vahoegsakee (SPELLEIDERGUH). */
    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    private KnabbelspelenFeature() {
    }
}
