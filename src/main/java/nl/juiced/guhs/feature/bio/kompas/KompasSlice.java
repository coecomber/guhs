package nl.juiced.guhs.feature.bio.kompas;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bezocht;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.feature.bio.BiomeLijst;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * biomes3 slice "kompas": the Superkompas tab Biomes. Sections per dimension ({@link BiomeLijst}), locked until the
 * player has been in the dimension ({@link Bezocht}); choosing a biome makes the compass point at the nearest spot of it
 * ({@link BiomeKompas}, {@link BiomeZoeker}); a tick for every biome the player ever stood in. What the tab shows is
 * {@link BiomesMenu}; the client draws it in {@code client.BiomesTab}. Resources: tools/features/bio_kompas.py.
 * <p>
 * Dev commands (gamemasters and the console): {@code /guhs bio kompas zoek <biome>} (the compass' own search, from where
 * the command runs), {@code /guhs bio kompas bezocht dimensie|biome <id>}, {@code ... bezocht alles}, {@code ... bezocht wis}.
 */
public final class KompasSlice {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The menu's choice of a biome (client -> server). */
    public record BiomeKeuze(boolean mainHand, String biome) implements CustomPacketPayload {
        public static final Type<BiomeKeuze> TYPE = new Type<>(Guhs.id("biokompas_keuze"));
        public static final StreamCodec<FriendlyByteBuf, BiomeKeuze> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, BiomeKeuze::mainHand, ByteBufCodecs.stringUtf8(64), BiomeKeuze::biome, BiomeKeuze::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BiomeKeuze p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                BiomeKompas.kiesVoor(player, p.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, p.biome());
            }
        }
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(KompasSlice::commando);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                BiomeKompas.stopAlles(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> BiomeKompas.stopAlles(null));
        BioZelftest.registreer("kompas", KompasSlice::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToServer(BiomeKeuze.TYPE, BiomeKeuze.STREAM_CODEC, BiomeKeuze::handle);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    // --- dev commands ---------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        var kompas = Commands.literal("kompas");
        kompas.then(Commands.literal("zoek").then(Commands.argument("biome", StringArgumentType.word()).executes(ctx -> {
            String regel = zoek(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()), StringArgumentType.getString(ctx, "biome"));
            zeg(ctx.getSource(), regel);
            return regel.contains(" gevonden op ") ? 1 : 0;
        })));
        var bezocht = Commands.literal("bezocht");
        bezocht.then(Commands.literal("dimensie").then(Commands.argument("sectie", StringArgumentType.word()).executes(ctx -> {
            String id = StringArgumentType.getString(ctx, "sectie");
            boolean nieuw = Bezocht.zetDimensie(ctx.getSource().getPlayerOrException(), id);
            zeg(ctx.getSource(), "[bio-kompas] dimensie " + id + (nieuw ? ": nu bezocht" : ": niets veranderd (al bezocht, onbekend, of kan nooit open)"));
            return nieuw ? 1 : 0;
        })));
        bezocht.then(Commands.literal("biome").then(Commands.argument("biome", StringArgumentType.word()).executes(ctx -> {
            String id = StringArgumentType.getString(ctx, "biome");
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            BiomeLijst.Sectie s = BiomesMenu.sectieVan(id);
            boolean nieuw = s != null && Bezocht.zetBiome(p, id);
            zeg(ctx.getSource(), "[bio-kompas] biome " + id + (nieuw ? ": nu bezocht" : ": niets veranderd (al bezocht of onbekend)"));
            return nieuw ? 1 : 0;
        })));
        bezocht.then(Commands.literal("alles").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            int n = 0;
            for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
                n += Bezocht.zetDimensie(p, s.id()) ? 1 : 0;
                for (String b : BiomeLijst.biomes(s.id())) {
                    n += Bezocht.zetBiome(p, b) ? 1 : 0;
                }
            }
            zeg(ctx.getSource(), "[bio-kompas] alles bezocht: " + n + " nieuw");
            return n;
        }));
        bezocht.then(Commands.literal("wis").executes(ctx -> {
            int n = wis(ctx.getSource().getPlayerOrException());
            zeg(ctx.getSource(), "[bio-kompas] " + n + " dimensies en biomes vergeten (waar je nu staat komt vanzelf terug)");
            return n;
        }));
        kompas.then(bezocht);
        event.getDispatcher().register(Commands.literal("guhs")
                .then(Commands.literal("bio").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(kompas)));
    }

    private static void zeg(CommandSourceStack bron, String regel) {
        LOGGER.info(regel);
        bron.sendSuccess(() -> Component.literal(regel), false);
    }

    /** (Dev command, self test) the compass' own search, all at once: one report line. */
    static String zoek(ServerLevel level, BlockPos van, String biome) {
        long t = System.nanoTime();
        BiomeZoeker z = BiomeZoeker.voor(level, BiomeLijst.sleutel(biome), van, BiomeKompas.STRAAL, BiomeKompas.STAP).helemaal();
        long ms = (System.nanoTime() - t) / 1_000_000L;
        BlockPos p = z.gevonden();
        String kop = "[bio-kompas] zoek " + biome + " in " + level.dimension().identifier() + " vanaf " + van.getX() + " " + van.getZ() + ": ";
        if (p == null) {
            return kop + "niets binnen " + BiomeKompas.STRAAL + " blokken (" + z.monsters() + " plekken bekeken, " + z.afgekeurd()
                    + " in oudere chunks overgeslagen, " + ms + " ms)";
        }
        return kop + "gevonden op " + p.getX() + " " + p.getY() + " " + p.getZ() + ", " + (int) Math.hypot(p.getX() - van.getX(), p.getZ() - van.getZ())
                + " blokken ver (" + z.monsters() + " plekken bekeken, " + z.afgekeurd() + " in oudere chunks overgeslagen, " + ms + " ms)";
    }

    /** (Dev command, tests) forgets every dimension and biome of this player; how many went. */
    static int wis(ServerPlayer player) {
        ListTag oud = GuhQuests.saved(player).getListOrEmpty(SpelGroepen.KEY);
        ListTag nieuw = new ListTag();
        int weg = 0;
        for (int i = 0; i < oud.size(); i++) {
            String id = oud.getStringOr(i, "");
            if (id.startsWith(Bezocht.DIMENSIE) || id.startsWith(Bezocht.BIOME)) {
                weg++;
            } else {
                nieuw.add(StringTag.valueOf(id));
            }
        }
        GuhQuests.saved(player).put(SpelGroepen.KEY, nieuw);
        SpelGroepen.sync(player);
        return weg;
    }

    // --- the self test (dev server: the real dimensions) -----------------------------------------------------------------

    private static void zelftest(net.minecraft.server.MinecraftServer server, ServerLevel guhmensie, BioZelftest.Melder meld) {
        BlockPos van = new BlockPos(0, 80, 0);
        int gevonden = 0, totaal = 0;
        long langst = 0;
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            if (s.teaser()) {
                meld.check(BiomeLijst.biomes(s.id()).isEmpty() && s.dimensie() == null, "the teaser lists nothing and has no dimension");
                continue;
            }
            ServerLevel level = server.getLevel(s.dimensie());
            if (level == null) {
                meld.fout("the dimension of section " + s.id() + " does not exist");
                continue;
            }
            var bron = level.getChunkSource().getGenerator().getBiomeSource();
            var sampler = level.getChunkSource().randomState().sampler();
            StringBuilder niet = new StringBuilder();
            int voor = gevonden;
            for (String b : BiomeLijst.biomes(s.id())) {
                ResourceKey<Biome> key = BiomeLijst.sleutel(b);
                long t = System.nanoTime();
                BiomeZoeker z = BiomeZoeker.voor(level, key, van, BiomeKompas.STRAAL, BiomeKompas.STAP).helemaal();
                langst = Math.max(langst, (System.nanoTime() - t) / 1_000_000L);
                totaal++;
                BlockPos p = z.gevonden();
                if (p == null) {
                    niet.append(' ').append(b);
                    continue;
                }
                gevonden++;
                // the spot really is that biome, and vanilla's own search finds none clearly nearer
                boolean echt = bron.getNoiseBiome(p.getX() >> 2, p.getY() >> 2, p.getZ() >> 2, sampler).is(key);
                if (!echt) {
                    meld.fout("the spot found for " + b + " is another biome: " + p);
                }
                BlockPos v = BioZelftest.vind(level, key, van, 2048);
                if (v != null) {
                    double mijn = Math.hypot(p.getX() - van.getX(), p.getZ() - van.getZ()), hun = Math.hypot(v.getX() - van.getX(), v.getZ() - van.getZ());
                    if (mijn > hun + 2 * BiomeKompas.STAP) {
                        meld.fout(b + ": found at " + (int) mijn + " blocks, vanilla's search at " + (int) hun);
                    }
                }
            }
            meld.ok("section " + s.id() + ": " + (gevonden - voor) + " of " + BiomeLijst.biomes(s.id()).size() + " biomes found within " + BiomeKompas.STRAAL + " blocks of 0,0"
                    + (niet.isEmpty() ? "" : "; not found:" + niet));
        }
        meld.check(gevonden > 0, "the search finds biomes (" + gevonden + " of " + totaal + "), the slowest took " + langst + " ms");
        // a whole sweep that finds nothing: how long the search thread is busy at worst
        long t = System.nanoTime();
        BiomeZoeker leeg = new BiomeZoeker(guhmensie.getChunkSource().getGenerator().getBiomeSource(), guhmensie.getChunkSource().randomState().sampler(),
                h -> false, van, BiomeKompas.STRAAL, BiomeKompas.STAP, guhmensie.getMinY(), guhmensie.getMaxY()).helemaal();
        long ms = (System.nanoTime() - t) / 1_000_000L;
        meld.check(leeg.gevonden() == null && ms < 30_000, "a whole sweep of the Guhmensie that finds nothing: " + leeg.monsters() + " spots in " + ms + " ms");
        // the saved form of a real chunk of the Guhmensie: it holds its own biome and not another
        var chunk = guhmensie.getChunk(0, 0);
        var eigen = guhmensie.getBiome(new BlockPos(8, 80, 8)).unwrapKey().orElseThrow().identifier().toString();
        var nbt = net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(guhmensie, chunk).write();
        meld.check(Opgeslagen.kanZijn(nbt, eigen) && !Opgeslagen.kanZijn(nbt, "guhs:bestaat_niet"), "a saved chunk is read: chunk 0,0 holds " + eigen);
        for (String regel : List.of(zoek(guhmensie, van, "bleekwoud"), zoek(guhmensie, new BlockPos(3000, 80, -3000), "guhwaii"))) {
            meld.check(regel.contains(" gevonden op "), regel);
        }
    }

    private KompasSlice() {
    }
}
