package nl.juiced.guhs.feature.doolhof;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Het Guhdoolhof (2.9, De Grote Guhspelen; structure guhdoolhof in the Guhvelden): a big hedge maze with guh-ear
 * topiary and a guh-shaped lookout tower in the middle, reached over a bridge. Meneer Vadskronkel (DOOLHOFGUH,
 * {@link DoolhofRole}) grows a new random maze for every game ({@link DoolhofKaart}, {@link DoolhofVeld}): find all the
 * kaasknabbels the Mika's stole and run out of the exit; Heg-Mika's ({@link DoolhofMikaEntity}) pinch knabbels back.
 * Time is the score ({@link DoolhofGame}); doolhofknabbels buy the explorer's outfit. The little lanterns on the hedges
 * light up at night. Resources: tools/features/doolhof.py (+ doolhof_bouw.py, doolhof_tex.py).
 */
public final class DoolhofFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks --------------------------------------------------------------------------------------------------------
    /** The maze hedge: dense, trimmed, green with little pink guh-flowers (you can't see through it). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> HEG = BLOCKS.registerSimpleBlock("doolhofheg",
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.6f, 3f).sound(SoundType.AZALEA_LEAVES)
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false));
    /** The hedge with a trimmed guh face (topiary). */
    public static final DeferredBlock<DoolhofBlocks.HegGezicht> HEG_GEZICHT = BLOCKS.registerBlock("doolhofheg_gezicht", DoolhofBlocks.HegGezicht::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.6f, 3f).sound(SoundType.AZALEA_LEAVES)
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false));
    /** A little guh-ear lantern that lights up by itself at night. */
    public static final DeferredBlock<DoolhofBlocks.Lantaarn> LANTAARN = BLOCKS.registerBlock("doolhof_lantaarn", DoolhofBlocks.Lantaarn::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).mapColor(MapColor.COLOR_PINK).noOcclusion()
                    .lightLevel(s -> s.getValue(DoolhofBlocks.Lantaarn.LIT) ? 13 : 2));
    /** The invisible anchor under Meneer Vadskronkel. */
    public static final DeferredBlock<DoolhofBlocks.AnkerBlock> ANKER = BLOCKS.registerBlock("doolhof_anker", DoolhofBlocks.AnkerBlock::new,
            BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1f, 3600000f).noOcclusion().isValidSpawn((s, l, p, e) -> false));

    // --- items ---------------------------------------------------------------------------------------------------------
    /** The coin of the maze: a little kaasknabbel wrapped in a hedge leaf. */
    public static final DeferredItem<Item> DOOLHOFKNABBEL = ITEMS.registerSimpleItem("doolhofknabbel", new Item.Properties());
    /** A kaasknabbel the Mika's stole (found in the maze; Meneer Vadskronkel takes them back afterwards). */
    public static final DeferredItem<Item> GESTOLEN_KNABBEL = ITEMS.registerItem("gestolen_knabbel", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("item.guhs.gestolen_knabbel.lore").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HEG_ITEM = ITEMS.registerSimpleBlockItem(HEG);
    public static final DeferredItem<BlockItem> HEG_GEZICHT_ITEM = ITEMS.registerSimpleBlockItem(HEG_GEZICHT);
    public static final DeferredItem<BlockItem> LANTAARN_ITEM = ITEMS.registerSimpleBlockItem(LANTAARN);

    // --- the Heg-Mika --------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<DoolhofMikaEntity>> MIKA = ENTITY_TYPES.register("doolhof_mika",
            () -> EntityType.Builder.of(DoolhofMikaEntity::new, MobCategory.MISC).sized(0.8f, 0.75f).eyeHeight(0.5f).clientTrackingRange(8)
                    .build("guhs:doolhof_mika"));

    // --- sounds --------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> GIECHEL = sound("doolhof.giechel");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNABBEL = sound("doolhof.knabbel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROEI = sound("doolhof.groei");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final NpcRole ROLE = new DoolhofRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(MIKA.get(), DoolhofMikaEntity.createAttributes().build()));
        Minigames.registerGame(Minigames.DOOLHOF, DoolhofGame::isPlaying);
        NeoForge.EVENT_BUS.addListener(DoolhofGame::onDamage);
        NeoForge.EVENT_BUS.addListener(DoolhofGame::onDeath);
        NeoForge.EVENT_BUS.addListener(DoolhofGame::onLogout);
        NeoForge.EVENT_BUS.addListener(DoolhofGame::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(DoolhofGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(DoolhofGame::onServerStopped);
        NeoForge.EVENT_BUS.register(DoolhofProtection.class);
        Protected.add((level, pos) -> level instanceof net.minecraft.server.level.ServerLevel server && server.dimension() == ModDimensions.GUHMENSION
                && DoolhofProtection.inDoolhof(server, pos));
        // the explorer's outfit: only from Meneer Vadskronkel's shop
        KledingBronnen.bron(GuhClothes.DOOLHOF_HOEDJE, "doolhof", DoolhofRole.HOEDJE + " doolhofknabbels");
        KledingBronnen.bron(GuhClothes.DOOLHOF_KOMPAS, "doolhof", DoolhofRole.KOMPAS + " doolhofknabbels");
        KledingBronnen.bron(GuhClothes.DOOLHOF_RUGZAKJE, "doolhof", DoolhofRole.RUGZAKJE + " doolhofknabbels");
    }

    public static void payloads(PayloadRegistrar registrar) {
        DoolhofPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(DOOLHOFKNABBEL.get()));
        output.accept(new ItemStack(HEG_ITEM.get()));
        output.accept(new ItemStack(HEG_GEZICHT_ITEM.get()));
        output.accept(new ItemStack(LANTAARN_ITEM.get()));
    }

    /** Meneer Vadskronkel (DOOLHOFGUH). */
    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    private DoolhofFeature() {
    }
}
