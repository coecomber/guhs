package nl.juiced.guhs.feature.guhpixel;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.guhpixel.among.AmongSlice;
import nl.juiced.guhs.feature.guhpixel.bioscoop.BioscoopSlice;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhpixel.blok.PoortBlock;
import nl.juiced.guhs.feature.guhpixel.blok.PortaalBlock;
import nl.juiced.guhs.feature.guhpixel.grap1.Grap1Slice;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Slice;
import nl.juiced.guhs.feature.guhpixel.guhkade.GuhkadeSlice;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorSlice;
import nl.juiced.guhs.feature.guhpixel.lobby.LobbySlice;
import nl.juiced.guhs.feature.guhpixel.parkour.ParkourSlice;
import nl.juiced.guhs.feature.guhpixel.reisbureau.ReisbureauSlice;

/**
 * Guhpixel: the foundation ("kern") of the minigame-server parody and its nine slices. This class registers the kern's
 * own things (the portal block, the home gate, the Netwerkkabeltje, sounds, the rules, the commands) and then calls every
 * slice's entry class in table order, so a slice never edits a shared registry: it owns its DeferredRegisters inside
 * {@code <Pkg>Slice}. Resources: tools/features/guhpixel.py (kern) and tools/features/guhpixel_&lt;slice&gt;.py.
 * <p>
 * API for the slices (all in this package): {@link Guhpixel}, {@link Toegang}, {@link PxData}, {@link Klok},
 * {@link Muntjes}, {@link Rang}, {@link Winkel}, {@link Arenas}, {@link Sessies} / {@link Sessie}, {@link Kluis},
 * {@link Regels}, {@link Grappen}, {@link LobbyNpcs}, {@link Aandenken}, {@link GuhpixelTitels}, {@link GidsBlad},
 * {@link Films}, {@link GuhKiezer}, {@link GuhOpslag}, {@link PxVlaggen}, {@link PxZelftest}, {@link PxTest}.
 */
public final class GuhpixelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<TicketType> TICKET_TYPES = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);

    /** The portal: in the café's CRT monitor (soort=in) and the exit on the plaza (soort=uit). No item. */
    public static final DeferredBlock<PortaalBlock> PORTAAL = BLOCKS.registerBlock("guhpixel_portaal", PortaalBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1.0f, 3600000.0f).noLootTable().noCollision()
                    .lightLevel(s -> 11).sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK));
    /** The craftable Guhpixel-poort for home. */
    public static final DeferredBlock<PoortBlock> POORT = BLOCKS.registerBlock("guhpixel_poort", PoortBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f).noOcclusion().lightLevel(s -> 7).sound(SoundType.METAL));
    public static final DeferredItem<LoreBlockItem> POORT_ITEM = ITEMS.registerItem("guhpixel_poort", p -> new LoreBlockItem(POORT.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    /** The Netwerkkabeltje: from the lobby greeter, once per player (and again when lost); the gate's recipe uses one. */
    public static final DeferredItem<Item> NETWERKKABELTJE = ITEMS.registerItem("guhpixel_netwerkkabeltje", LoreItem::new, () -> new Item.Properties());

    public static final DeferredHolder<SoundEvent, SoundEvent> MUNTJE = geluid("guhpixel.muntje");
    public static final DeferredHolder<SoundEvent, SoundEvent> ONTGRENDELD = geluid("guhpixel.ontgrendeld");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAAL_GELUID = geluid("guhpixel.portaal");

    /** Keeps an arena in use (and the lobby while it is rebuilt) loaded and ticking; no timeout, not saved. */
    public static final DeferredHolder<TicketType, TicketType> TICKET = TICKET_TYPES.register("guhpixel",
            () -> new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    /** An item with one grey tooltip line (lang {@code <description id>.lore}). */
    public static class LoreItem extends Item {
        public LoreItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        TICKET_TYPES.register(modBus);
        Minigames.registerGame(Sessies.SPEL_ID, p -> Sessies.van(p) != null);
        Protected.add(Guhpixel::in);   // (fire and fluids: the whole dimension, and a test's own box)
        NeoForge.EVENT_BUS.register(Regels.class);
        NeoForge.EVENT_BUS.register(Rangen.class);
        NeoForge.EVENT_BUS.addListener(PxCommando::register);
        PxZelftest.kern();
        // the nine slices, in table order (CONTRACT_PX 1.1)
        LobbySlice.register(modBus);
        Grap1Slice.register(modBus);
        Grap2Slice.register(modBus);
        AmongSlice.register(modBus);
        GuhkadeSlice.register(modBus);
        KantoorSlice.register(modBus);
        BioscoopSlice.register(modBus);
        ReisbureauSlice.register(modBus);
        ParkourSlice.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhpixelPayloads.register(registrar);
        LobbySlice.payloads(registrar);
        Grap1Slice.payloads(registrar);
        Grap2Slice.payloads(registrar);
        AmongSlice.payloads(registrar);
        GuhkadeSlice.payloads(registrar);
        KantoorSlice.payloads(registrar);
        BioscoopSlice.payloads(registrar);
        ReisbureauSlice.payloads(registrar);
        ParkourSlice.payloads(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(NETWERKKABELTJE.get()));
        output.accept(new ItemStack(POORT_ITEM.get()));
        LobbySlice.creative(output);
        Grap1Slice.creative(output);
        Grap2Slice.creative(output);
        AmongSlice.creative(output);
        GuhkadeSlice.creative(output);
        KantoorSlice.creative(output);
        BioscoopSlice.creative(output);
        ReisbureauSlice.creative(output);
        ParkourSlice.creative(output);
    }

    private GuhpixelFeature() {
    }
}
