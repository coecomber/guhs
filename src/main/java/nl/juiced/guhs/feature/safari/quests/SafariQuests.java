package nl.juiced.guhs.feature.safari.quests;

import java.util.function.Consumer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.mojang.brigadier.arguments.StringArgumentType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhpixel.GuhpixelFeature;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * Op Safari! slice S4: the quests and extra's of Safaripark Vadse Bergen (design ontwerp-safaripark-en-bouwen.md 1.7,
 * contract SAFARI_CONTRACT.md "Stap 2 API").
 * <ul>
 *   <li>{@link VermomdeGuh}: a guh in a homemade zebra suit hides at three spots in three suits; afterwards he goes home
 *       with you (with his zebrapakje: guh clothes for every guh);</li>
 *   <li>{@link VaheOlifant}: Dikkie no longer fits through his little gate: a diet (apple, carrot, melon) and a run on
 *       the olifantenloopband;</li>
 *   <li>{@link Geheimpjes}: one per zone (geheimpjes.json), 25 coins each, the trophy and the title Speurneus for all;</li>
 *   <li>{@link Beloningen}: the dex milestones (hidden advancements guhs:quest/safari_*, the FTB chapter guhs_safari), the
 *       full dex (golden camera, golden statue, voucher for the Gouden dikdik's board, title Safarikoning), the title
 *       Beverbaas ({@link #beverdamGebouwd});</li>
 *   <li>the pootafdrukken ({@link PootafdrukBlock}; they glow through a spyglass: client.SafariQuestsClient).</li>
 * </ul>
 * Hooks for the other slices: {@link #hoofdranger} (S2's head ranger), {@link #aaihoekKlusje} (S2's aaihoek),
 * {@link #beverdamGebouwd} (the bever helpers), {@link #VERREKIJKER} (S6's verrekijker counts as a spyglass).
 * Resources: tools/features/safari_quests.py (+ safari_park/zone_zz_s4.py for the park).
 */
public final class SafariQuests {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    // --- blocks (deco: DecoBlock + LoreBlockItem, models from tools/features/safari_quests.py) ---------------------------
    public static final DeferredBlock<DecoBlock> OLIFANTENLOOPBAND = BLOCKS.registerBlock("olifantenloopband",
            p -> new DecoBlock(p, Shapes.or(net.minecraft.world.level.block.Block.box(0, 0, 1, 16, 4, 15), net.minecraft.world.level.block.Block.box(1, 4, 0, 15, 15, 3))),
            () -> DecoBlock.props().mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL));
    public static final DeferredBlock<DecoBlock> FOTOLIJSTJE = BLOCKS.registerBlock("fotolijstje_slanke_olifant",
            p -> new DecoBlock(p, net.minecraft.world.level.block.Block.box(2, 0, 8, 14, 13, 14)), () -> DecoBlock.props().mapColor(MapColor.COLOR_PINK).sound(SoundType.WOOD));
    public static final DeferredBlock<DecoBlock> GEHEIMPJES_TROFEE = BLOCKS.registerBlock("geheimpjes_trofee",
            p -> new DecoBlock(p, net.minecraft.world.level.block.Block.box(1, 0, 3, 15, 14, 13)), () -> DecoBlock.props().mapColor(MapColor.GOLD).sound(SoundType.METAL)
                    .lightLevel(s -> 4));
    public static final DeferredBlock<DecoBlock> GOUDEN_DIERENBEELD = BLOCKS.registerBlock("gouden_dierenbeeld",
            p -> new DecoBlock(p, net.minecraft.world.level.block.Block.box(2, 0, 0, 14, 14, 14)), () -> DecoBlock.props().mapColor(MapColor.GOLD).sound(SoundType.METAL)
                    .lightLevel(s -> 6));
    /** The footprints on the trails (park only: no item, no drop). */
    public static final DeferredBlock<PootafdrukBlock> POOTAFDRUK = BLOCKS.registerBlock("pootafdruk", PootafdrukBlock::new,
            () -> BlockBehaviour.Properties.of().noCollision().noOcclusion().instabreak().noLootTable().replaceable()
                    .mapColor(MapColor.DIRT).sound(SoundType.MUD));

    public static final DeferredItem<LoreBlockItem> OLIFANTENLOOPBAND_ITEM = blokItem(OLIFANTENLOOPBAND);
    public static final DeferredItem<LoreBlockItem> FOTOLIJSTJE_ITEM = blokItem(FOTOLIJSTJE);
    public static final DeferredItem<LoreBlockItem> GEHEIMPJES_TROFEE_ITEM = blokItem(GEHEIMPJES_TROFEE);
    public static final DeferredItem<LoreBlockItem> GOUDEN_DIERENBEELD_ITEM = blokItem(GOUDEN_DIERENBEELD);

    // --- items ------------------------------------------------------------------------------------------------------------
    /** The golden safari camera: the cosmetic reward of the full dex (S1's camera can check the flag {@link Beloningen#GOUDEN_CAMERA}). */
    public static final DeferredItem<GuhpixelFeature.LoreItem> GOUDEN_SAFARICAMERA = ITEMS.registerItem("gouden_safaricamera",
            GuhpixelFeature.LoreItem::new, () -> new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    /** The voucher for the Gouden dikdik's board (S5's giftshop / board redeems it). */
    public static final DeferredItem<GuhpixelFeature.LoreItem> BORDBON_GOUDEN_DIKDIK = ITEMS.registerItem("bordbon_gouden_dikdik",
            GuhpixelFeature.LoreItem::new, () -> new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

    /**
     * S6 sets this to "is this player looking through the verrekijker?"; a vanilla spyglass always counts (both client
     * side: the footprints glow on the client).
     */
    public static volatile java.util.function.Predicate<net.minecraft.world.entity.player.Player> VERREKIJKER = p -> false;

    private static DeferredItem<LoreBlockItem> blokItem(DeferredBlock<DecoBlock> blok) {
        return ITEMS.registerItem(blok.getId().getPath(), p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(SafariQuests::onEntity);
        NeoForge.EVENT_BUS.addListener(SafariQuests::onEntitySpecifiek);
        NeoForge.EVENT_BUS.addListener(SafariQuests::onBlock);
        NeoForge.EVENT_BUS.addListener(SafariQuests::onTick);
        NeoForge.EVENT_BUS.addListener(SafariQuests::onLogin);
        NeoForge.EVENT_BUS.addListener(SafariQuests::commands);
        Beloningen.register();
        SafariDialoog.register();
        for (nl.juiced.guhs.entity.GuhClothes c : java.util.List.of(nl.juiced.guhs.entity.GuhClothes.ZEBRAPAKJE,
                nl.juiced.guhs.entity.GuhClothes.PANDAPAKJE, nl.juiced.guhs.entity.GuhClothes.PINGUINPAKJE)) {
            nl.juiced.guhs.feature.kleding.KledingBronnen.bron(c, "safari_quests",
                    nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.vind_de_vermomde_guh"));
        }
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(OLIFANTENLOOPBAND_ITEM.get()));
        output.accept(new ItemStack(FOTOLIJSTJE_ITEM.get()));
        output.accept(new ItemStack(GEHEIMPJES_TROFEE_ITEM.get()));
        output.accept(new ItemStack(GOUDEN_DIERENBEELD_ITEM.get()));
        output.accept(new ItemStack(GOUDEN_SAFARICAMERA.get()));
        output.accept(new ItemStack(BORDBON_GOUDEN_DIKDIK.get()));
    }

    // =====================================================================================================================
    // hooks for the other slices
    // =====================================================================================================================

    /** S2: the head ranger was clicked. True when he said something about a quest (then S2 shows nothing else). */
    public static boolean hoofdranger(ServerPlayer p) {
        return hoofdranger(p, null);
    }

    /** The same, with the ranger himself as the speaker of the talking screen. */
    public static boolean hoofdranger(ServerPlayer p, @javax.annotation.Nullable net.minecraft.world.entity.Entity ranger) {
        return VermomdeGuh.ranger(p, ranger);
    }

    /** S2: the player did an aaihoek chore (feeding, brushing, walking the alpaca): the FTB quest "Aaien maar!". */
    public static void aaihoekKlusje(ServerPlayer p) {
        Beloningen.adv(p, "safari_aaihoek");
    }

    /** The bever helpers (building help): a beaver dam was finished for this player; five give the title Beverbaas. */
    public static int beverdamGebouwd(ServerPlayer p) {
        return Beloningen.beverdam(p);
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    private static void onEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p)) {
            if (event.getTarget().entityTags().contains(VermomdeGuh.TAG) || event.getTarget().entityTags().contains(VaheOlifant.TAG)) {
                event.setCanceled(true);                 // (the off hand and the client: never tame or pick up a quest animal)
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (klik(p, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** (R5: the disguised guhs are guhs; a click can arrive as the specific interaction only, like the kijk-guhs had in
     *  round 4, so both paths handle it, once per click.) */
    private static void onEntitySpecifiek(PlayerInteractEvent.EntityInteractSpecific event) {
        boolean questDier = event.getTarget().entityTags().contains(VermomdeGuh.TAG) || event.getTarget().entityTags().contains(VaheOlifant.TAG);
        if (!questDier) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer p) {
            klik(p, event.getTarget());
        }
    }

    private static final java.util.Map<java.util.UUID, Long> LAATST = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean klik(ServerPlayer p, net.minecraft.world.entity.Entity doel) {
        long nu = p.level().getGameTime();
        boolean questDier = doel.entityTags().contains(VermomdeGuh.TAG) || doel.entityTags().contains(VaheOlifant.TAG);
        if (questDier && LAATST.getOrDefault(p.getUUID(), -100L) > nu - 5) {
            return true;                        // (one click can fire both interactions)
        }
        boolean raak = VermomdeGuh.klik(p, doel) || VaheOlifant.klik(p, doel);
        if (raak) {
            LAATST.put(p.getUUID(), nu);
        }
        return raak;
    }

    private static void onBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p) || !Safari.echt(p.level())) {
            return;
        }
        if (Geheimpjes.klik(p, event.getPos()) || VaheOlifant.loopband(p, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 == 37) {
            VermomdeGuh.bewaak(server);
            VaheOlifant.bewaak(server);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Beloningen.inhalen(p);
        }
    }

    // =====================================================================================================================
    // dev commands (ops): /guhs safari quest <vermomde_guh|vahe_olifant> [stand] [speler], /guhs safari quests [speler]
    // =====================================================================================================================

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("safari").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("quests").executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("quest").then(Commands.argument("naam", StringArgumentType.word())
                        .then(Commands.argument("stand", StringArgumentType.word())
                                .executes(ctx -> zet(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "naam"), StringArgumentType.getString(ctx, "stand")))
                                .then(Commands.argument("speler", EntityArgument.player())
                                        .executes(ctx -> zet(ctx.getSource(), EntityArgument.getPlayer(ctx, "speler"),
                                                StringArgumentType.getString(ctx, "naam"), StringArgumentType.getString(ctx, "stand")))))))));
    }

    private static int zet(CommandSourceStack src, ServerPlayer p, String naam, String stand) {
        String waarde = stand.equals("leeg") ? "" : stand;
        SafariSpeler.zetQuest(p.level().getServer(), p.getUUID(), naam, waarde);
        src.sendSuccess(() -> Component.translatable("commands.guhs.safari.quest", naam, stand), true);
        return 1;
    }

    private static int status(CommandSourceStack src, ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        src.sendSuccess(() -> Component.translatable("commands.guhs.safari.quests", leeg(SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST)),
                leeg(SafariSpeler.quest(server, p.getUUID(), VaheOlifant.QUEST)), Geheimpjes.gevonden(p), QuestData.get().geheimpjes.size(),
                Beloningen.beverdammen(p)), false);
        return 1;
    }

    private static String leeg(String s) {
        return s.isEmpty() ? "-" : s;
    }

    // =====================================================================================================================
    // small helpers
    // =====================================================================================================================

    /** (Test round 4) a page in the talking screen from "the park" (no speaker drawn); never the chat. */
    static void zeg(ServerPlayer p, String sleutel, Object... args) {
        SafariDialoog.zeg(p, null, SafariDialoog.PARK, sleutel, args);
    }

    /** A reward item: into the inventory, with a page "Je krijgt: ...!" in the same talking screen. */
    static void geef(ServerPlayer p, ItemStack stack) {
        SafariDialoog.beloning(p, stack);
        Minigames.give(p, stack);
    }

    static ItemStack stuk(BlockItem item) {
        return new ItemStack(item);
    }

    private SafariQuests() {
    }
}
