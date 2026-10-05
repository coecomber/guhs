package nl.juiced.guhs.feature.bank;

import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.storage.BankStorage;

/**
 * bbq2 (bank): the Bank Guh's cap and upgrade, the Hapluikje and its link key. Resources: tools/features/bank.py.
 * <ul>
 * <li><b>The cap.</b> A Bank Guh holds at most {@link BankStorage#CAP} of one kind of item (item + components); the rules
 * live in {@link BankStorage} (the single way in, which hands back what does not fit) and every caller keeps that
 * remainder. Banks that held more before the cap existed keep everything.</li>
 * <li><b>The upgrade</b> ({@link #BANK_UPGRADE}, "Bodemloos Buikje"): used on a placed Bank Guh it takes the cap away for
 * good. It stays with the bank when the bank is picked up (block entity + the item component {@link #BANK_OPGEVOERD} +
 * the loot table). Only the uitvinder-guh gives one (tech-quests): there is no recipe.</li>
 * <li><b>The item capability of the Bank Guh</b> ({@link nl.juiced.guhs.storage.BankHandler}): pipes, hoppers and chore
 * guhs can always put in (up to the cap), taking out only works on an upgraded bank.</li>
 * <li><b>The Hapluikje</b> ({@link #HAPLUIKJE}): a little guh machine on vadskracht that swallows whatever is put in (by
 * hand, hopper, Knabbelbuis, chore guh, Bezorgguhtje) and it lands in the ONE bank it is linked to, however far away, in
 * any dimension. Deposit only. Linked with the {@link #BANK_SLEUTEL}: click the bank, then the luikje (one bank per
 * luikje, as many luikjes per bank as you like). It refuses, and the giver keeps the items, when it has no vadskracht,
 * when its bank stands nowhere or when the bank is full for that item.</li>
 * <li>Every bank has an id ({@link #BANK_ID}) and {@link BankAdressen} knows where each placed one stands.</li>
 * </ul>
 * Other slices never touch {@code BankStorage}: they move items through the item capability
 * ({@code Kisten.van(level, pos, side)} on a Bank Guh or a Hapluikje).
 */
public final class BankFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);

    // --- the fixed ids of CONTRACT_130 7 ---

    public static final DeferredBlock<HapluikjeBlock> HAPLUIKJE = BLOCKS.registerBlock("hapluikje", HapluikjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(2.0f, 6.0f));
    public static final DeferredItem<BlockItem> HAPLUIKJE_ITEM = ITEMS.registerItem("hapluikje",
            p -> new BlockItem(HAPLUIKJE.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs.hapluikje.lore").withStyle(ChatFormatting.GRAY));
                    tooltip.accept(Component.translatable("block.guhs.hapluikje.lore.kracht", VadsGetallen.HAPLUIKJE).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }, () -> new Item.Properties().useBlockDescriptionPrefix());
    /** The link key: click a Bank Guh (the key remembers it), then every Hapluikje that must feed that bank. */
    public static final DeferredItem<Item> BANK_SLEUTEL = ITEMS.<Item>registerItem("bank_sleutel", BankSleutelItem::new,
            () -> new Item.Properties().stacksTo(1));
    /** The one upgrade ("Bodemloos Buikje"): use it on a placed Bank Guh. Given by tech-quests (no recipe). */
    public static final DeferredItem<Item> BANK_UPGRADE = ITEMS.<Item>registerItem("bank_upgrade", BankUpgradeItem::new,
            () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HapluikjeBlockEntity>> HAPLUIKJE_BE = BLOCK_ENTITIES.register("hapluikje",
            () -> new BlockEntityType<>(HapluikjeBlockEntity::new, HAPLUIKJE.get()));

    // --- item components ---

    /** On a Bank Guh item: this bank's own id. On a Banksleutel: the bank it is bound to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BANK_ID = COMPONENTS.registerComponentType("bank_id",
            b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    /** On a Bank Guh item: this bank has the upgrade (no cap). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BANK_OPGEVOERD = COMPONENTS.registerComponentType(
            "bank_opgevoerd", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** On a Banksleutel: where the bank stood when the key was bound (only for the tooltip; the id is what counts). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> BANK_PLEK = COMPONENTS.registerComponentType("bank_plek",
            b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /**
     * What keeps the chunk of a bank loaded for a little while after a Hapluikje reached into it from far away (20 s, it
     * only loads the chunk, nothing ticks there, and it is renewed on every use): a busy luikje does not load the chunk
     * again for every item, an idle one keeps nothing loaded.
     */
    public static final DeferredHolder<TicketType, TicketType> HAPLUIKJE_TICKET = TICKETS.register("bank_hapluikje",
            () -> new TicketType(400L, TicketType.FLAG_LOADING | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        TICKETS.register(modBus);
        modBus.addListener(BankFeature::capabilities);
        NeoForge.EVENT_BUS.addListener(BankFeature::commands);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        // the Bank Guh itself: in always (to the cap), out only when upgraded
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BANK_GUH.get(), (bank, kant) -> bank.handler());
        // the Hapluikje: a vadskracht consumer whose item handler is the mouth (MachineBlockEntity#handler, overridden)
        VadskrachtFeature.machineCapabilities(event, HAPLUIKJE_BE.get());
    }

    // =====================================================================================================================
    // op commands (dev checks and the AutoCheck script tools/autocheck/bbq2_bank.txt; literal texts: not for players)
    //   /guhs bank koppel <luikje> <bank>   link the Hapluikje there to the Bank Guh there (what the Banksleutel does)
    //   /guhs bank opvoeren <bank>          give the Bank Guh there the upgrade
    //   /guhs bank adres <bank>             say the bank's id and what the address book knows
    // =====================================================================================================================

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bank")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("koppel").then(Commands.argument("luikje", BlockPosArgument.blockPos())
                        .then(Commands.argument("bank", BlockPosArgument.blockPos()).executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            if (!(level.getBlockEntity(BlockPosArgument.getLoadedBlockPos(ctx, "luikje")) instanceof HapluikjeBlockEntity luikje)
                                    || !(level.getBlockEntity(BlockPosArgument.getLoadedBlockPos(ctx, "bank")) instanceof BankGuhBlockEntity bank)) {
                                ctx.getSource().sendFailure(Component.literal("Daar staat geen Hapluikje / geen Bank Guh"));
                                return 0;
                            }
                            bank.meld();
                            luikje.koppel(bank.bankId());
                            ctx.getSource().sendSuccess(() -> Component.literal("Hapluikje gekoppeld aan bank " + bank.bankId()), false);
                            return 1;
                        }))))
                .then(Commands.literal("opvoeren").then(Commands.argument("bank", BlockPosArgument.blockPos()).executes(ctx -> {
                    if (!(ctx.getSource().getLevel().getBlockEntity(BlockPosArgument.getLoadedBlockPos(ctx, "bank")) instanceof BankGuhBlockEntity bank)) {
                        ctx.getSource().sendFailure(Component.literal("Daar staat geen Bank Guh"));
                        return 0;
                    }
                    bank.getStorage().setUpgraded(true);
                    ctx.getSource().sendSuccess(() -> Component.literal("Bank Guh opgevoerd: geen grens meer"), false);
                    return 1;
                })))
                .then(Commands.literal("adres").then(Commands.argument("bank", BlockPosArgument.blockPos()).executes(ctx -> {
                    if (!(ctx.getSource().getLevel().getBlockEntity(BlockPosArgument.getLoadedBlockPos(ctx, "bank")) instanceof BankGuhBlockEntity bank)) {
                        ctx.getSource().sendFailure(Component.literal("Daar staat geen Bank Guh"));
                        return 0;
                    }
                    bank.meld();
                    BankAdressen boek = BankAdressen.van(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Bank " + bank.bankId() + " staat volgens het adresboek op "
                            + boek.plek(bank.bankId()) + " (" + boek.aantal() + " banken bekend); opgevoerd: " + bank.isUpgraded()), false);
                    return 1;
                })))));
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(BankVolPayload.TYPE, BankVolPayload.STREAM_CODEC, BankVolPayload::handle);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(HAPLUIKJE_ITEM.get()));
        output.accept(new ItemStack(BANK_SLEUTEL.get()));
        output.accept(new ItemStack(BANK_UPGRADE.get()));
    }

    // --- the hidden advancements of the FTB quests, and the two visible ones (tab Guh-technologie) ---

    /** A kind of item reached the cap in a bank this player filled. */
    public static void vol(ServerPlayer player) {
        GuhAdvancements.grant(player, "bank_vol");
    }

    static void opgevoerd(ServerPlayer player) {
        GuhAdvancements.grant(player, "bank_opgevoerd");
        GidsFeature.grant(player, "techniek/bank_opgevoerd");
    }

    static void gekoppeld(ServerPlayer player) {
        GuhAdvancements.grant(player, "bank_gekoppeld");
    }

    static void gehapt(ServerPlayer player) {
        GuhAdvancements.grant(player, "bank_gehapt");
        GidsFeature.grant(player, "techniek/bank_gehapt");
    }

    private BankFeature() {
    }
}
