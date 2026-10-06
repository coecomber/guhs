package nl.juiced.guhs.feature.ringsausuman;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-sausuman): the extra stop of "In de ban van de Knabbelring": de Toren van Sausuman (structure
 * {@code guhs:sausuman_toren}, {@link Toren}), a black tower full of sputtering machines, and Sausuman van de Vele Sauzen,
 * the wizard-Mika who also wants a bite ({@link SausumanRol}). It opens for a player once chapter 4 is done.
 * <ul>
 *   <li>The questline {@link #LIJN} (4 steps, per player): he does not get a bite, so he bakes a ring of his own in his
 *       Ringenbakker ({@link RingenbakkerBlock}). The player fetches the three ingredients from the stations upstairs
 *       ({@link VoorraadBlock}: dough, hot sauce and "cheese", which is an onion), pulls the lever, and in a cutscene
 *       ({@link Bakkerij#BAKKEN}) the machine bakes... an onion ring. He sulks in his Mokhoek, and goes on sulking for good.
 *       Reward: onion rings, the Pannantir ({@link PannantirBlock}), and one onion ring a day from the lever.</li>
 *   <li>The nod to Guh-technologie: the tower's machines are REAL vadskracht machines on real Guhdraad, all on one Mika-rad
 *       ({@link MikaradBlock}, a real source of 10 vadskracht). That is far too heavy, so the net stands still and the
 *       hover readout says so. Upstairs the wire goes nowhere at all. Nothing is faked and nothing needs a script.</li>
 *   <li>The sputterpijp ({@link SputterpijpBlock}): the exhaust pipes that make the tower smoke and pop; a building block.</li>
 * </ul>
 * Nothing in the tower changes for good and every quest item comes from a per-player click, so any number of players do
 * the questline at the same time. Nothing here can hurt a player. Resources: tools/features/ring_sausuman.py (the tower:
 * ring_sausuman_bouw.py, Sausuman's model: ring_sausuman_modellen.py).
 */
public final class RingSausumanFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks -----------------------------------------------------------------------------------------------------------
    /** The face and lever of the great machine (only in the tower: no item). */
    public static final DeferredBlock<RingenbakkerBlock> RINGENBAKKER = BLOCKS.registerBlock("ringsausuman_ringenbakker", RingenbakkerBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0f, 1200f).sound(SoundType.COPPER).lightLevel(s -> 7)
                    .pushReaction(PushReaction.BLOCK).noLootTable());
    /** The three stations: Deegkneder, Sauskraan, Kaaskast (only in the tower: no item). */
    public static final DeferredBlock<VoorraadBlock> VOORRAAD = BLOCKS.registerBlock("ringsausuman_voorraad", VoorraadBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(4.0f, 1200f).sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    /** The Mika-rad: a real source of a little vadskracht (only in the tower: no item). */
    public static final DeferredBlock<MikaradBlock> MIKARAD = BLOCKS.registerBlock("ringsausuman_mikarad", MikaradBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 1200f).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)
                    .noLootTable());
    /** An exhaust pipe that puffs and sputters. */
    public static final DeferredBlock<SputterpijpBlock> SPUTTERPIJP = BLOCKS.registerBlock("ringsausuman_sputterpijp", SputterpijpBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).sound(SoundType.COPPER).noOcclusion()
                    .requiresCorrectToolForDrops());
    /** The seeing pan. */
    public static final DeferredBlock<PannantirBlock> PANNANTIR = BLOCKS.registerBlock("ringsausuman_pannantir", PannantirBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(s -> 9));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MikaradBlock.Kern>> MIKARAD_BE = BLOCK_ENTITY_TYPES.register(
            "ringsausuman_mikarad", () -> new BlockEntityType<>(MikaradBlock.Kern::new, MIKARAD.get()));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> SPUTTERPIJP_ITEM = ITEMS.registerSimpleBlockItem(SPUTTERPIJP);
    public static final DeferredItem<BlockItem> PANNANTIR_ITEM = ITEMS.registerSimpleBlockItem(PANNANTIR, () -> new Item.Properties().stacksTo(1));
    /** The three quest items (item tag guhs:loaned: they never go into storage). */
    public static final DeferredItem<LoreItem> RINGDEEG = ITEMS.registerItem("ringsausuman_ringdeeg", LoreItem::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<LoreItem> FRITUURSAUS = ITEMS.registerItem("ringsausuman_frituursaus", LoreItem::new,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<LoreItem> UI = ITEMS.registerItem("ringsausuman_ui", LoreItem::new, () -> new Item.Properties().stacksTo(1));
    /** What the Ringenbakker really bakes: crunchy, round, and no Knabbelring. */
    public static final DeferredItem<LoreItem> UIENRING = ITEMS.registerItem("ringsausuman_uienring", LoreItem::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));

    // --- sounds (vanilla sounds, pitched, in sounds.json) -----------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SPUTTER = sound("ringsausuman.sputter");
    public static final DeferredHolder<SoundEvent, SoundEvent> RONK = sound("ringsausuman.ronk");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLING = sound("ringsausuman.pling");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOK = sound("ringsausuman.mok");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /**
     * The questline of the tower (the halte "ring_sausuman" of the travel map; ring-kern refers to this field): 0 talk to
     * Sausuman, 1 fetch the three ingredients, 2 pull the lever of the Ringenbakker (the baking scene), 3 offer the sulking
     * wizard a bite; 4 = done. It opens when chapter 4 is done.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_sausuman", "knabbelring").stappen(4).na("ring_h4")
            .icoon("guhs:ringsausuman_uienring")
            .nodig((p, stap) -> stap == 1 ? List.of(
                    Verhaallijn.nodig("guhs:ringsausuman_ringdeeg", Bakkerij.heeft(p, Ingredient.DEEG) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:ringsausuman_frituursaus", Bakkerij.heeft(p, Ingredient.SAUS) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:ringsausuman_ui", "gui.guhs.ringsausuman.nodig.kaas", Bakkerij.heeft(p, Ingredient.KAAS) ? 1 : 0, 1))
                    : stap == 3 ? List.of(Verhaallijn.nodig("guhs:ringsausuman_uienring", Math.min(1, GuhQuests.count(p, UIENRING.get())), 1))
                    : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:ringsausuman_uienring", stap(p) >= 3),
                    Verhaallijn.beloning("guhs:ringsausuman_pannantir", stap(p) >= 4),
                    Verhaallijn.beloning("guhs:ringsausuman_uienring", "gui.guhs.ringsausuman.beloning.dagelijks", stap(p) >= 4)))
            .doel((p, stap) -> Ring.doel(Ring.SAUSUMAN))
            .registreer();

    private static int stap(ServerPlayer p) {
        return LIJN.stap(p);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> VadskrachtFeature.knoopCapability(event, MIKARAD_BE.get()));
        // the tower: hidden and protected until a player's story has passed chapter 4, then in their Superkompas
        Ring.sluier(Toren.STRUCTUUR, 3, Ring.SAUSUMAN_NR);
        SuperkompasItem.voegToe("barbecue", Toren.STRUCTUUR);
        NpcRollen.zet(GuhNpcEntity.Kind.SAUSUMAN, new SausumanRol());
        Bezetting.npc(Toren.SAUSUMAN, Toren.STRUCTUUR, null, Toren.NPC, GuhNpcEntity.Kind.SAUSUMAN, null, Toren.NPC_YAW);
        Bakkerij.registreer();
        // chapter 4 done: Sam-guh smells something frying (the tower is optional: the story's own pointer goes on to chapter 5)
        Verhaallijn h4 = Ring.lijn(4);
        h4.opStap((p, oud, nieuw) -> {
            if (oud < h4.stappen() && nieuw >= h4.stappen()) {
                wenk(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(RingSausumanFeature::commando);
    }

    /** "There is a black tower nearby": said by the player's Sam-guh when he walks along, else as a hint. */
    static void wenk(ServerPlayer p) {
        GuhEntity sam = Sam.van(p);
        if (sam != null) {
            GuhQuests.say(p, sam, "quest.guhs.ringsausuman.wenk");
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringsausuman.wenk_alleen").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(UIENRING, PANNANTIR_ITEM, SPUTTERPIJP_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /**
     * {@code /guhs ringsausuman stand} (ops; for the AutoCheck script and dev checks): where the caller is in the questline
     * and what they carry; in dev runs only {@code toren}: the tower's template around you, your feet on the spot just
     * outside its door. The steps themselves: {@code /guhs verhaal stap ring_sausuman <speler> <n>}.
     */
    private static void commando(RegisterCommandsEvent event) {
        var wortel = Commands.literal("ringsausuman").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.literal("ring_sausuman: stap " + LIJN.stap(p) + " van " + LIJN.stappen()
                            + (LIJN.aanDeBeurt(p) ? "" : " (hoofdstuk 4 is nog niet klaar)") + ", ingredienten " + Bakkerij.aantal(p) + "/3"
                            + (LIJN.vlag(p, Bakkerij.GEVULD) ? ", de machine is gevuld" : "")), false);
                    return 1;
                }));
        if (!FMLEnvironment.isProduction()) {
            wortel.then(Commands.literal("toren").executes(c -> {
                BlockPos hoek = BlockPos.containing(c.getSource().getPosition()).subtract(Toren.DEUR);
                boolean gelukt = Toren.plaats(c.getSource().getLevel(), hoek);
                c.getSource().sendSuccess(() -> Component.literal(gelukt ? "guhs:sausuman_toren staat op " + hoek.toShortString() : "Geen template"), false);
                return gelukt ? 1 : 0;
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    private RingSausumanFeature() {
    }
}
