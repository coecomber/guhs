package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Guhpixel slice "guhkade": the Guhkade cabinets for home, bought with muntjes in the Guhpixel shop.
 * <ul>
 *   <li>two cabinets, {@link #KAST_FLAPPY} (Flappy Guh) and {@link #KAST_PONG} (Mika-Pong): {@link KastBlock},
 *   {@link KastBlockEntity} (the scores of that cabinet, the top 5 on its screen);</li>
 *   <li>the games themselves: package {@code spel} (exact little simulations; the server replays what a client played);</li>
 *   <li>{@link Guhkade}: a player's game from right-click to score, and what beating a guh does;</li>
 *   <li>{@link KastGoal} + {@link GuhKunde}: your guhs walk up and play, getting better up to a ceiling per kind of guh;</li>
 *   <li>the shop: the first cabinet (either one) costs 250 muntjes, every next one 150;</li>
 *   <li>the Guhdex section, and {@code /guhs px guhkade ...} for testing.</li>
 * </ul>
 * Resources: tools/features/guhpixel_guhkade.py. Client: client.GuhkadeClient (the game screen, the block's own screen).
 */
public final class GuhkadeSlice {
    /** This slice's namespace (ids, lang keys, the claim on a guh). */
    public static final String NS = "guhkade";
    public static final int PRIJS_EERSTE = 250, PRIJS_VOLGENDE = 150;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    private static BlockBehaviour.Properties props(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(1.5f).noOcclusion().lightLevel(s -> 9).sound(SoundType.WOOD)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<KastBlock> KAST_FLAPPY = BLOCKS.registerBlock("guhkade_kast_flappy", p -> new KastBlock(p, Spel.FLAPPY),
            () -> props(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<KastBlock> KAST_PONG = BLOCKS.registerBlock("guhkade_kast_pong", p -> new KastBlock(p, Spel.PONG),
            () -> props(MapColor.COLOR_PURPLE));
    public static final DeferredItem<KastBlock.KastItem> KAST_FLAPPY_ITEM = ITEMS.registerItem("guhkade_kast_flappy",
            p -> new KastBlock.KastItem(KAST_FLAPPY.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<KastBlock.KastItem> KAST_PONG_ITEM = ITEMS.registerItem("guhkade_kast_pong",
            p -> new KastBlock.KastItem(KAST_PONG.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KastBlockEntity>> KAST_BE = BLOCK_ENTITIES.register("guhkade_kast",
            () -> new BlockEntityType<>(KastBlockEntity::new, KAST_FLAPPY.get(), KAST_PONG.get()));

    /** A coin drops in (a game starts), a beep, a point, game over, a new number one, and a guh that just lost its place. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUNTJE = geluid("guhkade.muntje");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP = geluid("guhkade.piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUNT = geluid("guhkade.punt");
    public static final DeferredHolder<SoundEvent, SoundEvent> AF = geluid("guhkade.af");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD = geluid("guhkade.record");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIP = geluid("guhkade.sip");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    public static DeferredBlock<KastBlock> kast(Spel spel) {
        return spel == Spel.PONG ? KAST_PONG : KAST_FLAPPY;
    }

    public static DeferredItem<KastBlock.KastItem> item(Spel spel) {
        return spel == Spel.PONG ? KAST_PONG_ITEM : KAST_FLAPPY_ITEM;
    }

    /** The shop id of a cabinet. */
    public static String aanbod(Spel spel) {
        return "guhkade_kast_" + spel.id;
    }

    /** What the next cabinet costs this player: the first one (whichever) 250, after that 150. */
    public static int prijs(ServerPlayer p) {
        int al = 0;
        for (Spel s : Spel.values()) {
            al += Winkel.gekocht(p, aanbod(s));
        }
        return al == 0 ? PRIJS_EERSTE : PRIJS_VOLGENDE;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        for (Spel spel : Spel.values()) {
            Winkel.aanbod(aanbod(spel)).groep("guhkade").icoon(() -> new ItemStack(item(spel).get()))
                    .naam(Component.translatable("block.guhs.guhkade_kast_" + spel.id))
                    .uitleg(Component.translatable("gui.guhs.guhkade.winkel." + spel.id))
                    .prijs((speler, alGekocht) -> prijs(speler))
                    .max(0)
                    .lever((speler, aanbod) -> {
                        if (!Winkel.geef(speler, new ItemStack(item(spel).get()))) {
                            return false;
                        }
                        GuhAdvancements.grant(speler, "guhkade_kast");
                        return true;
                    })
                    .registreer();
        }
        GuhHooks.doelen((guh, goals) -> {
            if (guh.getType() == ModEntities.GUH.get()) {
                goals.addGoal(4, new KastGoal(guh));
            }
        });
        GuhHooks.tick(KastGoal::ruimOp);
        GidsBlad.registreer(new Gids());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> Guhkade.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            Guhkade.vergeet();
            KastGoal.vergeet();
        });
        NeoForge.EVENT_BUS.addListener(GuhkadeSlice::commandos);
        PxZelftest.registreer(NS, GuhkadeSlice::zelftest);
    }

    /** The dev-server self test (/guhs px zelftest): the shop, the advancements, and a cabinet put down and taken away again. */
    private static void zelftest(net.minecraft.server.MinecraftServer server, ServerLevel guhpixel, PxZelftest.Melder meld) {
        for (Spel spel : Spel.values()) {
            meld.check(Winkel.van(aanbod(spel)) != null, "in the shop: " + aanbod(spel));
            Sim.Uitkomst a = Sim.voorspel(spel, 123L, 3), b = Sim.voorspel(spel, 123L, 3);
            meld.check(a.equals(b) && a.score() >= 3, spel.id + ": the same game twice, " + a);
        }
        for (String adv : List.of("guhkade_kast", "guhkade_gespeeld", "guhkade_flappy_mijlpaal", "guhkade_pong_mijlpaal", "guhkade_guh_verslagen")) {
            meld.check(server.getAdvancements().get(Guhs.id("quest/" + adv)) != null, "advancement quest/" + adv);
        }
        // a cabinet at home (the overworld, high in the air above 0,0): both halves, its block entity, its list; then gone again
        ServerLevel thuis = server.overworld();
        BlockPos pos = new BlockPos(8, thuis.getMaxY() - 8, 8);
        thuis.getChunk(pos);
        if (!thuis.getBlockState(pos).isAir() || !thuis.getBlockState(pos.above()).isAir()) {
            meld.fout("no room for the test cabinet at " + pos.toShortString());
            return;
        }
        KastBlock.bouw(thuis, pos, KAST_PONG.get(), net.minecraft.core.Direction.NORTH);
        KastBlockEntity be = Guhkade.kast(thuis, pos);
        meld.check(be != null && be.spel() == Spel.PONG && be.top().size() == 3, "a cabinet stands with its three house names");
        meld.check(Guhkade.Kasten.rond(thuis, pos, 4).contains(be), "and it is known as a loaded cabinet");
        thuis.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        meld.check(thuis.getBlockState(pos).isAir() && Guhkade.Kasten.rond(thuis, pos, 4).isEmpty(), "taking the upper half away takes the whole cabinet");
        for (net.minecraft.world.entity.item.ItemEntity drop : thuis.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3), e -> e.getItem().is(KAST_PONG_ITEM.get()))) {
            drop.discard();
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhkadePayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KAST_FLAPPY_ITEM.get()));
        output.accept(new ItemStack(KAST_PONG_ITEM.get()));
    }

    /** The Guhdex section (tab "Guhpixel & uitjes"): your best scores, how often you played, the guhs you beat. */
    private static final class Gids implements GidsSectie {
        @Override
        public String id() {
            return NS;
        }

        @Override
        public int volgorde() {
            return 50;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            b.kop(Component.translatable("gui.guhs.guhkade.gids.kop"));
            b.regel(Component.translatable("gui.guhs.guhkade.gids.uitleg"));
            for (Spel spel : Spel.values()) {
                int best = Guhkade.best(p, spel), potjes = Guhkade.potjes(p, spel);
                b.stat(Component.translatable("gui.guhs.guhkade.gids.best", Component.translatable("gui.guhs.guhkade.spel." + spel.id)),
                        potjes == 0 ? Component.translatable("gui.guhs.guhkade.gids.nog_niet")
                                : Component.translatable("gui.guhs.guhkade.gids.best.waarde", best, potjes));
            }
            b.stat(Component.translatable("gui.guhs.guhkade.gids.verslagen"), Component.literal(String.valueOf(Guhkade.verslagen(p))));
            b.regel(Component.translatable("gui.guhs.guhkade.gids.tip"));
        }
    }

    // =====================================================================================================================
    // /guhs px guhkade ... (gamemasters; for trying things out and for the AutoCheck script)
    // =====================================================================================================================

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal(NS);
        k.then(Commands.literal("geef").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            for (Spel spel : Spel.values()) {
                Winkel.geef(p, new ItemStack(item(spel).get()));
            }
            return 1;
        }));
        // two cabinets side by side, three blocks in front of you, facing you
        k.then(Commands.literal("zet").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            var kijkt = p.getDirection();
            BlockPos basis = p.blockPosition().relative(kijkt, 3);
            KastBlock.bouw(p.level(), basis, KAST_FLAPPY.get(), kijkt.getOpposite());
            KastBlock.bouw(p.level(), basis.relative(kijkt.getClockWise(), 2), KAST_PONG.get(), kijkt.getOpposite());
            return zeg(ctx.getSource(), "twee kasten bij " + basis.toShortString());
        }));
        k.then(Commands.literal("open").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KastBlockEntity be = dichtbij(p);
            if (be == null) {
                return zeg(ctx.getSource(), "geen kast binnen 8 blokken");
            }
            Guhkade.open(p, be.getBlockPos());
            return 1;
        }).then(Commands.literal("pong").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            for (KastBlockEntity be : Guhkade.Kasten.rond(p.level(), p.blockPosition(), 8)) {
                if (be.spel() == Spel.PONG) {
                    Guhkade.open(p, be.getBlockPos());
                    return 1;
                }
            }
            return zeg(ctx.getSource(), "geen Mika-Pong-kast binnen 8 blokken");
        })));
        // a finished game with this score for yourself on the nearest cabinet (what the server does after a real game)
        k.then(Commands.literal("score").then(Commands.argument("n", IntegerArgumentType.integer(0, 9999)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KastBlockEntity be = dichtbij(p);
            if (be == null) {
                return zeg(ctx.getSource(), "geen kast binnen 8 blokken");
            }
            Guhkade.Uitslag u = Guhkade.verwerk(p, be, IntegerArgumentType.getInteger(ctx, "n"));
            return zeg(ctx.getSource(), be.spel().id + ": score " + u.score() + ", plaats " + u.plaats() + ", guhs verslagen " + u.verslagen().size());
        })));
        // your nearest guh goes and plays as soon as it can (no rest, no dice)
        k.then(Commands.literal("guh").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            GuhEntity guh = guh(p);
            if (guh == null) {
                return zeg(ctx.getSource(), "geen eigen guh binnen 16 blokken");
            }
            guh.getPersistentData().remove(GuhKunde.RUST);
            guh.getPersistentData().putInt(GuhKunde.OEFEN, Math.max(1, GuhKunde.oefen(guh)));
            return zeg(ctx.getSource(), guh.getName().getString() + " gaat zo spelen: potjes flappy " + GuhKunde.keren(guh, Spel.FLAPPY) + ", pong "
                    + GuhKunde.keren(guh, Spel.PONG) + ", wil oefenen " + GuhKunde.oefen(guh));
        }).then(Commands.literal("keren").then(Commands.argument("n", IntegerArgumentType.integer(0, 10000)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            GuhEntity guh = guh(p);
            if (guh == null) {
                return zeg(ctx.getSource(), "geen eigen guh binnen 16 blokken");
            }
            int n = IntegerArgumentType.getInteger(ctx, "n");
            for (Spel spel : Spel.values()) {
                guh.getPersistentData().putInt(GuhKunde.KEREN + spel.id, n);
            }
            guh.getPersistentData().remove(GuhKunde.RUST);
            return zeg(ctx.getSource(), guh.getName().getString() + ": " + n + " potjes; plafond flappy "
                    + GuhKunde.plafond(Spel.FLAPPY, guh.getVariant(), guh.getPersonality()) + ", pong "
                    + GuhKunde.plafond(Spel.PONG, guh.getVariant(), guh.getPersonality()));
        }))));
        k.then(Commands.literal("wis").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KastBlockEntity be = dichtbij(p);
            if (be == null) {
                return zeg(ctx.getSource(), "geen kast binnen 8 blokken");
            }
            be.wis();
            return zeg(ctx.getSource(), "lijst van de kast gewist");
        }));
        k.then(Commands.literal("lijst").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KastBlockEntity be = dichtbij(p);
            if (be == null) {
                return zeg(ctx.getSource(), "geen kast binnen 8 blokken");
            }
            StringBuilder sb = new StringBuilder(be.spel().id + ":");
            for (KastBlockEntity.Regel r : be.regels()) {
                sb.append(' ').append(r.naam().getString()).append(r.guh() ? "(guh)" : "").append(' ').append(r.score()).append(';');
            }
            return zeg(ctx.getSource(), sb.toString());
        }));
        k.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px").then(k)));
    }

    @javax.annotation.Nullable
    private static KastBlockEntity dichtbij(ServerPlayer p) {
        List<KastBlockEntity> kasten = Guhkade.Kasten.rond(p.level(), p.blockPosition(), 8);
        return kasten.isEmpty() ? null : kasten.get(0);
    }

    @javax.annotation.Nullable
    private static GuhEntity guh(ServerPlayer p) {
        ServerLevel level = p.level();
        GuhEntity best = null;
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(16),
                x -> x.getType() == ModEntities.GUH.get() && p.getUUID().equals(x.getOwnerUUID()))) {
            if (best == null || g.distanceToSqr(p) < best.distanceToSqr(p)) {
                best = g;
            }
        }
        return best;
    }

    private static int zeg(CommandSourceStack bron, String tekst) {
        bron.sendSuccess(() -> Component.literal("[guhkade] " + tekst), false);
        return 1;
    }

    private GuhkadeSlice() {
    }
}
