package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Guhpixel slice "kantoor": the Guhkantoor for home (DESIGN_PX section 4). A Prikklok and up to four Bureautjes, bought
 * from the Verkoper-guh; you put 0-4 of your guhs "to work" (they sleep on the keyboard, stored as data: {@link Kantoor},
 * {@link KantoorData}); every 8 real hours a readable loonstrookje, with every third one a kwartaalrapport with flat
 * graphs, and each month an oorkonde for the "Werknemer van de maand": the longest sleeper ({@link Papier},
 * {@link KantoorTeksten}). The papers can be read, collected and hung on a wall ({@link PapierBlock}). No muntjes, no
 * useful output. Resources: tools/features/guhpixel_kantoor.py.
 */
public final class KantoorSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredBlock<PrikklokBlock> PRIKKLOK = BLOCKS.registerBlock("guhkantoor_prikklok", PrikklokBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).noOcclusion().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<BureautjeBlock> BUREAUTJE = BLOCKS.registerBlock("guhkantoor_bureautje", BureautjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).noOcclusion().sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<PapierBlock> LOONSTROOKJE = BLOCKS.registerBlock("guhkantoor_loonstrookje",
            p -> new PapierBlock(p, PapierBlock.STROOKJE), () -> MuurDecoBlock.props().sound(SoundType.WOOL));
    public static final DeferredBlock<PapierBlock> KWARTAALRAPPORT = BLOCKS.registerBlock("guhkantoor_kwartaalrapport",
            p -> new PapierBlock(p, PapierBlock.RAPPORT), () -> MuurDecoBlock.props().sound(SoundType.WOOL));
    public static final DeferredBlock<PapierBlock> OORKONDE = BLOCKS.registerBlock("guhkantoor_oorkonde",
            p -> new PapierBlock(p, PapierBlock.OORKONDE), () -> MuurDecoBlock.props().sound(SoundType.WOOL));

    public static final DeferredItem<LoreBlockItem> PRIKKLOK_ITEM = ITEMS.registerItem("guhkantoor_prikklok", p -> new LoreBlockItem(PRIKKLOK.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<LoreBlockItem> BUREAUTJE_ITEM = ITEMS.registerItem("guhkantoor_bureautje", p -> new LoreBlockItem(BUREAUTJE.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<PapierItem> LOONSTROOKJE_ITEM = ITEMS.registerItem("guhkantoor_loonstrookje", p -> new PapierItem(LOONSTROOKJE.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));
    public static final DeferredItem<PapierItem> KWARTAALRAPPORT_ITEM = ITEMS.registerItem("guhkantoor_kwartaalrapport",
            p -> new PapierItem(KWARTAALRAPPORT.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));
    public static final DeferredItem<PapierItem> OORKONDE_ITEM = ITEMS.registerItem("guhkantoor_oorkonde", p -> new PapierItem(OORKONDE.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrikklokBlockEntity>> PRIKKLOK_BE = BLOCK_ENTITIES.register("guhkantoor_prikklok",
            () -> new BlockEntityType<>(PrikklokBlockEntity::new, PRIKKLOK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BureautjeBlockEntity>> BUREAUTJE_BE = BLOCK_ENTITIES.register("guhkantoor_bureautje",
            () -> new BlockEntityType<>(BureautjeBlockEntity::new, BUREAUTJE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PapierBlockEntity>> PAPIER_BE = BLOCK_ENTITIES.register("guhkantoor_papier",
            () -> new BlockEntityType<>(PapierBlockEntity::new, LOONSTROOKJE.get(), KWARTAALRAPPORT.get(), OORKONDE.get()));

    public static final DeferredHolder<SoundEvent, SoundEvent> INKLOKKEN = geluid("guhkantoor.inklokken");
    public static final DeferredHolder<SoundEvent, SoundEvent> UITKLOKKEN = geluid("guhkantoor.uitklokken");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRINTER = geluid("guhkantoor.printer");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOETSENBORD = geluid("guhkantoor.toetsenbord");

    /** Client: opens the reading screen of a paper (its tag); set by client.KantoorClient, a no-op on a server. */
    public static volatile Consumer<CompoundTag> papierLezer = t -> { };

    public static final int PRIJS_SET = 200, PRIJS_BUREAUTJE = 40;
    private static final String G = "gui.guhs.guhkantoor.";

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        winkel();
        GidsBlad.registreer(new Gids());
        NeoForge.EVENT_BUS.addListener(KantoorSlice::commando);
        NeoForge.EVENT_BUS.addListener(KantoorSlice::spelerTick);
        NeoForge.EVENT_BUS.addListener(KantoorSlice::login);
        PxZelftest.registreer("kantoor", (server, level, meld) -> {
            int ontbreekt = 0;
            for (KantoorTeksten.Pool pool : KantoorTeksten.Pool.values()) {
                for (int i = 0; i < pool.aantal(); i++) {
                    if (!Language.getInstance().has(pool.sleutel(i))) {
                        ontbreekt++;
                    }
                }
            }
            meld.check(ontbreekt == 0, "every line of the paper pools has a text (" + ontbreekt + " missing)");
            for (Papier.Soort soort : Papier.Soort.values()) {
                meld.check(KantoorTeksten.blad(Papier.tag(Papier.voorbeeld(soort, 7L))).size() >= 10, "a sample " + soort.id() + " can be written");
            }
            meld.check(Winkel.van("guhkantoor_set") != null && Winkel.van("guhkantoor_bureautje") != null, "the two shop offers are registered");
            meld.ok(KantoorData.get(server).alle().size() + " guhs at work in this world");
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        KantoorPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PRIKKLOK_ITEM.get()));
        output.accept(new ItemStack(BUREAUTJE_ITEM.get()));
        for (Papier.Soort soort : Papier.Soort.values()) {
            output.accept(Papier.voorbeeld(soort, 86L));
        }
    }

    /** The shop: the set (a Prikklok and one Bureautje) and extra desks; real items, buy them again whenever you like. */
    private static void winkel() {
        Winkel.aanbod("guhkantoor_set").groep("guhkantoor").icoon(() -> new ItemStack(PRIKKLOK_ITEM.get()))
                .naam(Component.translatable(G + "winkel.set.naam")).uitleg(Component.translatable(G + "winkel.set"))
                .prijs(PRIJS_SET).max(0)
                .lever((speler, aanbod) -> Winkel.geef(speler, new ItemStack(PRIKKLOK_ITEM.get())) & Winkel.geef(speler, new ItemStack(BUREAUTJE_ITEM.get())))
                .registreer();
        Winkel.aanbod("guhkantoor_bureautje").groep("guhkantoor").icoon(() -> new ItemStack(BUREAUTJE_ITEM.get()))
                .naam(Component.translatable("block.guhs.guhkantoor_bureautje")).uitleg(Component.translatable(G + "winkel.bureautje"))
                .prijs(PRIJS_BUREAUTJE).max(0)
                .eis(speler -> Winkel.gekocht(speler, "guhkantoor_set") > 0, Component.translatable(G + "winkel.eis"))
                .lever((speler, aanbod) -> Winkel.geef(speler, new ItemStack(BUREAUTJE_ITEM.get())))
                .registreer();
    }

    /** The Guhdex section: who works now, the papers collected, the Werknemer van de maand. */
    private static final class Gids implements GidsSectie {
        @Override
        public String id() {
            return "kantoor";
        }

        @Override
        public int volgorde() {
            return 60;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            var server = p.level().getServer();
            Kantoor.bijwerken(server, p.getUUID());
            b.kop(Component.translatable(G + "gids.kop"));
            b.regel(Component.translatable(G + "gids.uitleg"));
            b.voortgang(Component.translatable(G + "gids.werkt"), KantoorData.get(server).vanEigenaar(p.getUUID()).size(), Kantoor.MAX_BUREAUS);
            b.stat(Component.translatable(G + "gids.loon"), Component.literal(String.valueOf(Kantoor.loonstrookjes(p))));
            b.stat(Component.translatable(G + "gids.rapport"), Component.literal(String.valueOf(Kantoor.rapporten(p))));
            b.stat(Component.translatable(G + "gids.oorkonde"), Component.literal(String.valueOf(Kantoor.oorkondes(p))));
            Kantoor.Topper top = Kantoor.topper(server, p.getUUID());
            b.stat(Component.translatable(G + "gids.maand"), top == null ? Component.translatable(G + "gids.maand.geen")
                    : Component.translatable(G + "gids.maand.waarde", top.naam(), top.uren()));
        }
    }

    /** Now and then for every online player: the postvak, and guhs whose desk vanished without telling. */
    private static void spelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 200 == 137) {
            Kantoor.controleer(p);
            Kantoor.bezorg(p);
        }
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Kantoor.bijwerken(p.level().getServer(), p.getUUID());
        }
    }

    // =====================================================================================================================
    // dev commands: /guhs px kantoor set | papier | demo | stand | vrij
    // =====================================================================================================================

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px").then(Commands.literal("kantoor")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Winkel.geef(p, new ItemStack(PRIKKLOK_ITEM.get()));
                    Winkel.geef(p, new ItemStack(BUREAUTJE_ITEM.get(), Kantoor.MAX_BUREAUS));
                    return zeg(ctx.getSource(), "een Prikklok en 4 Bureautjes");
                }))
                .then(Commands.literal("papier").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    long zaad = p.getRandom().nextLong();
                    for (Papier.Soort soort : Papier.Soort.values()) {
                        Winkel.geef(p, Papier.voorbeeld(soort, zaad));
                    }
                    return zeg(ctx.getSource(), "drie papieren met zaad " + zaad);
                }))
                .then(Commands.literal("demo").executes(ctx -> demo(ctx.getSource())))
                .then(Commands.literal("stand").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    List<KantoorData.Werk> werk = KantoorData.get(p.level().getServer()).vanEigenaar(p.getUUID());
                    StringBuilder sb = new StringBuilder(werk.size() + " guhs aan het werk; loonstrookjes " + Kantoor.loonstrookjes(p) + ", rapporten "
                            + Kantoor.rapporten(p) + ", oorkondes " + Kantoor.oorkondes(p));
                    for (KantoorData.Werk w : werk) {
                        sb.append("\n ").append(w.pos.toShortString()).append(": ").append((Klok.nu() - w.sinds) / 60000L).append(" min, wachtend ")
                                .append(Kantoor.wachtend(w));
                    }
                    return zeg(ctx.getSource(), sb.toString());
                }))
                .then(Commands.literal("vrij").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int n = 0;
                    for (KantoorData.Werk w : KantoorData.get(p.level().getServer()).vanEigenaar(p.getUUID())) {
                        ServerLevel level = p.level().getServer().getLevel(w.dim);
                        if (level != null && Kantoor.klokUit(level, w.pos, level.getBlockState(w.pos), p, false)) {
                            n++;
                        }
                    }
                    Kantoor.controleer(p);
                    return zeg(ctx.getSource(), n + " guhs uitgeklokt");
                })))));
    }

    /** A little office three blocks south of the player: a Prikklok, two desks, two fresh tame guhs asleep at them. */
    private static int demo(CommandSourceStack bron) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = bron.getPlayerOrException();
        if (!(p.level() instanceof ServerLevel level)) {
            return 0;
        }
        BlockPos klok = p.blockPosition().offset(0, 0, 3);
        zetNeer(level, klok, PRIKKLOK.get().defaultBlockState());
        String[] namen = {"Vadsje", "Njegje"};
        for (int i = 0; i < 2; i++) {
            BlockPos bureau = klok.offset(i == 0 ? -2 : 2, 0, 0);
            zetNeer(level, bureau, BUREAUTJE.get().defaultBlockState());
            GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
            if (guh != null) {
                guh.snapTo(p.getX(), p.getY(), p.getZ() + 1);
                level.addFreshEntity(guh);
                guh.tame(p);
                guh.setCustomName(Component.literal(namen[i]));
                Kantoor.klokIn(p, level, bureau, guh.getUUID());
            }
        }
        return zeg(bron, "kantoor gebouwd bij " + klok.toShortString() + " (spoel de klok met /guhs px klok 8.1)");
    }

    /** Sets a Prikklok or Bureautje and connects it, as placing by hand does (the dev command and the game tests). */
    public static void zetNeer(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
        if (state.is(PRIKKLOK.get())) {
            Kantoor.klokGeplaatst(level, pos, null);
        } else if (state.is(BUREAUTJE.get())) {
            Kantoor.koppel(level, pos, null);
        }
    }

    private static int zeg(CommandSourceStack bron, String tekst) {
        bron.sendSuccess(() -> Component.literal("[px kantoor] " + tekst), false);
        return 1;
    }

    private KantoorSlice() {
    }
}
