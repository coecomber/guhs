package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.function.Consumer;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcs;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterPlein;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.elftocht.WarmDrankjeItem;
import nl.juiced.guhs.feature.guhwinter.spellen.race.RaceSim;

/**
 * Guhwinter 1.10.9, slice "spellen": the games of the winter plaza and the pepernoten shop.
 * <ul>
 *   <li>De Pepernotenrace: a phone-style 3-lane runner from the arcade cabinet ({@link Pepernotenrace}, race.RaceSim);</li>
 *   <li>the highscore boards (top 10; {@link HighscorebordBlock}): one for the race, one for the schaatsrondje;</li>
 *   <li>het schaatsrondje on the pond ({@link Schaatsrondje}, skates {@link WinterSchaatsenItem});</li>
 *   <li>oliebollen bakken ({@link Oliebollen}: the story's game, replayable at the stall during Oud &amp; Nieuw);</li>
 *   <li>hidden pepernoten ({@link VerstopteNoten});</li>
 *   <li>the pepernoten shop ({@link GuhwinterWinkel}).</li>
 * </ul>
 * The blocks stand at plein's anchors: {@link SpellenPlaatsing} puts them there ({@code /guhs winter spellen plaats}).
 * Kern and plein are reached without a compile dependency: {@link SpelBrug}. Resources: tools/features/guhwinter_spellen.py.
 */
public final class SpellenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** Items that melt on 3 January (kern removes them; the shop's temporary fun). */
    public static final TagKey<Item> SMELT = TagKey.create(Registries.ITEM, Guhs.id("guhwinter_smelt"));
    public static final TagKey<Block> SMELT_BLOK = TagKey.create(Registries.BLOCK, Guhs.id("guhwinter_smelt_blok"));

    /** The plaza's fixtures: only creative breaks them (the plaza is protected besides). */
    private static BlockBehaviour.Properties vast(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(-1.0f, 3600000.0f).noOcclusion().sound(SoundType.WOOD)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<SpelBlokken.Arcadekast> ARCADEKAST = BLOCKS.registerBlock("guhwinter_arcadekast", SpelBlokken.Arcadekast::new,
            () -> vast(MapColor.COLOR_RED).lightLevel(s -> 9));
    public static final DeferredBlock<HighscorebordBlock> HIGHSCOREBORD = BLOCKS.registerBlock("guhwinter_highscorebord", HighscorebordBlock::new,
            () -> vast(MapColor.WOOD));
    public static final DeferredBlock<SpelBlokken.Kraam> WINKELKRAAM = BLOCKS.registerBlock("guhwinter_winkelkraam",
            p -> new SpelBlokken.Kraam(p, SpelBlokken.KraamSoort.WINKEL), () -> vast(MapColor.COLOR_RED));
    public static final DeferredBlock<SpelBlokken.Kraam> BAKKRAAM = BLOCKS.registerBlock("guhwinter_bakkraam",
            p -> new SpelBlokken.Kraam(p, SpelBlokken.KraamSoort.BAKKRAAM), () -> vast(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<SpelBlokken.Schaatsbord> SCHAATSBORD = BLOCKS.registerBlock("guhwinter_schaatsbord", SpelBlokken.Schaatsbord::new,
            () -> vast(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<SpelBlokken.Zakje> ZAKJE = BLOCKS.registerBlock("guhwinter_pepernootzakje", SpelBlokken.Zakje::new,
            () -> vast(MapColor.COLOR_BROWN).sound(SoundType.WOOL));
    public static final DeferredBlock<SpelBlokken.Lampjes> KERSTLAMPJES = BLOCKS.registerBlock("guhwinter_kerstlampjes", SpelBlokken.Lampjes::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.2f).noCollision().noOcclusion().sound(SoundType.WOOL)
                    .lightLevel(s -> 10).pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<DoubleHighBlockItem> ARCADEKAST_ITEM = ITEMS.registerItem("guhwinter_arcadekast",
            p -> new DoubleHighBlockItem(ARCADEKAST.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> HIGHSCOREBORD_ITEM = ITEMS.registerSimpleBlockItem(HIGHSCOREBORD);
    public static final DeferredItem<BlockItem> WINKELKRAAM_ITEM = ITEMS.registerSimpleBlockItem(WINKELKRAAM);
    public static final DeferredItem<BlockItem> BAKKRAAM_ITEM = ITEMS.registerSimpleBlockItem(BAKKRAAM);
    public static final DeferredItem<BlockItem> SCHAATSBORD_ITEM = ITEMS.registerSimpleBlockItem(SCHAATSBORD);
    public static final DeferredItem<BlockItem> ZAKJE_ITEM = ITEMS.registerSimpleBlockItem(ZAKJE);
    public static final DeferredItem<BlockItem> KERSTLAMPJES_ITEM = ITEMS.registerSimpleBlockItem(KERSTLAMPJES);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HighscorebordBlock.Entity>> HIGHSCOREBORD_BE =
            BLOCK_ENTITIES.register("guhwinter_highscorebord", () -> new BlockEntityType<>(HighscorebordBlock.Entity::new, HIGHSCOREBORD.get()));

    /** Warme chocomel: a little food, and the warmth: a short speed boost and a bit of regeneration. */
    public static final DeferredItem<WarmDrankjeItem> CHOCOMEL = ITEMS.registerItem("guhwinter_chocomel", WarmDrankjeItem::new,
            () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).alwaysEdible().build(),
                    Consumables.defaultDrink().consumeSeconds(0.8f).sound(SoundEvents.HONEY_DRINK)
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(MobEffects.SPEED, WarmDrankjeItem.BOOST_TICKS, 0),
                                    new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0)), 1f))
                            .build()));
    public static final DeferredItem<ChocoladeletterItem> CHOCOLADELETTER = ITEMS.registerItem("guhwinter_chocoladeletter", ChocoladeletterItem::new,
            () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build()));
    public static final DeferredItem<WinterSchaatsenItem> SCHAATSEN = ITEMS.registerItem("guhwinter_schaatsen", WinterSchaatsenItem::new,
            () -> new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.FEET).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setDamageOnHurt(false).build()));
    /** The kerstmuts and the Sint-mijter: worn on the head, shown as their item model (like a carved pumpkin). */
    public static final DeferredItem<Item> KERSTMUTS = ITEMS.registerSimpleItem("guhwinter_kerstmuts", () -> hoofd(Rarity.UNCOMMON));
    public static final DeferredItem<Item> MIJTER = ITEMS.registerSimpleItem("guhwinter_mijter", () -> hoofd(Rarity.RARE));

    private static Item.Properties hoofd(Rarity r) {
        return new Item.Properties().stacksTo(1).rarity(r).component(DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setDamageOnHurt(false).build());
    }

    private SpellenFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        Pepernotenrace.register();
        Schaatsrondje.register();
        Oliebollen.register();
        NeoForge.EVENT_BUS.addListener(WinterSchaatsenItem::tick);
        NeoForge.EVENT_BUS.addListener(WinterSchaatsenItem::dichtKist);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                Pepernotenrace.vergeet(p.getUUID());
                Schaatsrondje.stop(p, null);
                Oliebollen.vergeet(p);
                GuhwinterWinkel.vergeet(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                Schaatsrondje.stop(p, "gui.guhs.guhwinter_spellen.schaats.te_ver");
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            Pepernotenrace.vergeetAlles();
            Schaatsrondje.vergeetAlles();
            Oliebollen.vergeetAlles();
            SpelBrug.vergeet();
        });
        nl.juiced.guhs.world.TikWacht.post("GuhwinterSpellen", event -> {
            Schaatsrondje.tick(event.getServer());
            VerstopteNoten.tick(event.getServer());
        });
        nl.juiced.guhs.feature.guhwinter.GuhwinterCommando.voegToe(ctx -> commandos());
        // (1.10.9 integration) the blocks go with the square: away before any square is stamped (also the frame at the
        // end), back on the winter square; and the winkelguh opens the shop
        GuhwinterPlein.voorStempel((level, welke) -> "spellen: " + SpellenPlaatsing.weg(level));
        GuhwinterPlein.naStempel((level, welke) -> welke.equals(GuhwinterPlein.WINTER) ? "spellen: " + SpellenPlaatsing.plaats(level) : null);
        GuhwinterNpcs.klikHook((speler, naam, npc) -> {
            if (!"winkelguh".equals(naam)) {
                return false;
            }
            GuhwinterWinkel.open(speler);
            return true;
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        SpellenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ARCADEKAST_ITEM.get()));
        output.accept(new ItemStack(HIGHSCOREBORD_ITEM.get()));
        output.accept(new ItemStack(WINKELKRAAM_ITEM.get()));
        output.accept(new ItemStack(BAKKRAAM_ITEM.get()));
        output.accept(new ItemStack(SCHAATSBORD_ITEM.get()));
        output.accept(new ItemStack(ZAKJE_ITEM.get()));
        output.accept(new ItemStack(KERSTLAMPJES_ITEM.get()));
        output.accept(new ItemStack(CHOCOMEL.get()));
        output.accept(ChocoladeletterItem.maak("Guh"));
        output.accept(new ItemStack(SCHAATSEN.get()));
        output.accept(new ItemStack(KERSTMUTS.get()));
        output.accept(new ItemStack(MIJTER.get()));
    }

    // --- /guhs winter spellen ... (testing) --------------------------------------------------------------------------------

    private static int zeg(CommandSourceStack bron, String tekst) {
        bron.sendSuccess(() -> Component.literal("[winter spellen] " + tekst).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandos() {
        LiteralArgumentBuilder<CommandSourceStack> k = nl.juiced.guhs.feature.guhwinter.GuhwinterCommando.dev("spellen");
        k.then(Commands.literal("plaats").executes(ctx -> zeg(ctx.getSource(), SpellenPlaatsing.plaats(SpelBrug.plein(ctx.getSource().getServer())))));
        k.then(Commands.literal("weg").executes(ctx -> zeg(ctx.getSource(), SpellenPlaatsing.weg(SpelBrug.plein(ctx.getSource().getServer())))));
        k.then(Commands.literal("ankers").executes(ctx -> {
            SpelBrug.vergeet();
            var server = ctx.getSource().getServer();
            StringBuilder sb = new StringBuilder(SpelBrug.heeftAnkers(server) ? "ankers.json gelezen" : "geen ankers.json: terugvalplekken");
            sb.append("; fase ").append(SpelBrug.fase(server));
            sb.append("; route ").append(Schaatsrondje.route(server).size()).append(" poortjes");
            sb.append("; zakjes ").append(VerstopteNoten.plekken(server).size());
            sb.append("; pepernoot-item ").append(SpelBrug.pepernoot() != null ? "ja" : "NEE (kern ontbreekt)");
            return zeg(ctx.getSource(), sb.toString());
        }));
        k.then(Commands.literal("race").then(Commands.literal("open").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Pepernotenrace.open(p, p.blockPosition());
            return 1;
        })).then(Commands.literal("score").then(Commands.argument("score", IntegerArgumentType.integer(0, 1_000_000))
                .then(Commands.argument("noten", IntegerArgumentType.integer(0, 10_000)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int noten = IntegerArgumentType.getInteger(ctx, "noten");
                    var u = new RaceSim.Uitkomst(IntegerArgumentType.getInteger(ctx, "score"), 60 * 30, noten, 0);
                    var v = Pepernotenrace.verwerk(p, u);
                    return zeg(ctx.getSource(), "race: gekregen " + v.gekregen() + ", plaats " + v.plaats() + (v.record() ? ", RECORD" : ""));
                })))));
        k.then(Commands.literal("schaats").then(Commands.literal("start").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Schaatsrondje.start(p, p.blockPosition());
            return 1;
        })).then(Commands.literal("tijd").then(Commands.argument("ticks", IntegerArgumentType.integer(1, 100_000)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            int n = Schaatsrondje.verwerk(p, IntegerArgumentType.getInteger(ctx, "ticks"));
            return zeg(ctx.getSource(), "schaats: " + n + " pepernoten");
        }))));
        k.then(Commands.literal("olie").executes(ctx -> olie(ctx.getSource(), false))
                .then(Commands.argument("airfryer", BoolArgumentType.bool()).executes(ctx -> olie(ctx.getSource(), BoolArgumentType.getBool(ctx, "airfryer")))));
        k.then(Commands.literal("kraam").executes(ctx -> {
            Oliebollen.bijKraam(ctx.getSource().getPlayerOrException());
            return 1;
        }));
        k.then(Commands.literal("winkel").executes(ctx -> {
            GuhwinterWinkel.open(ctx.getSource().getPlayerOrException());
            return 1;
        }));
        k.then(Commands.literal("letter").then(Commands.argument("naam", StringArgumentType.word()).executes(ctx -> {
            ctx.getSource().getPlayerOrException().getInventory().add(ChocoladeletterItem.maak(StringArgumentType.getString(ctx, "naam")));
            return 1;
        })));
        k.then(Commands.literal("vergeet").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            SpellenData.get(ctx.getSource().getServer()).vergeet(p.getUUID());
            return zeg(ctx.getSource(), "alles van " + p.getGameProfile().name() + " vergeten (eerste keren, gevonden zakjes, eigen records)");
        }));
        k.then(Commands.literal("wis").then(Commands.argument("bord", StringArgumentType.word()).executes(ctx -> {
            String bord = StringArgumentType.getString(ctx, "bord");
            SpellenData.get(ctx.getSource().getServer()).wis(bord);
            HighscorebordBlock.verversAlles(ctx.getSource().getServer());
            return zeg(ctx.getSource(), "bord " + bord + " gewist");
        })));
        return k;
    }

    private static int olie(CommandSourceStack bron, boolean airfryer) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = bron.getPlayerOrException();
        Oliebollen.start(p, airfryer, score -> p.sendSystemMessage(Component.literal("[winter spellen] oliebollen-score: " + score).withStyle(ChatFormatting.AQUA)));
        return 1;
    }
}
