package nl.juiced.guhs.feature.bio.systemen;

import java.util.function.Consumer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.feature.bio.bouwdal.Cadeaus;
import nl.juiced.guhs.feature.bio.bouwmeer.Visserguh;
import nl.juiced.guhs.feature.bio.wereld.BioModel;
import nl.juiced.guhs.feature.bio.wereld.WolkTerrein;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.reisbureau.Bestemming;
import nl.juiced.guhs.feature.guhpixel.reisbureau.Reizen;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * biomes3 slice "systemen" (closing): the FTB quests, the Reisbureau destinations, the titles and the Guhdex of the three
 * new biomes. Most of it is data (tools/features/bio_systemen.py, tools/make_ftbquests.py and the Reisbureau tables in
 * feature/guhpixel/reisbureau); this package holds the proofs no building slice made ({@link Bewijzen}), the four titles
 * ({@link BioTitels}), the Guhdex section of the weebhuisje ({@link BioGids}) and the dev commands
 * {@code /guhs bio systemen titels alles|wis}, {@code japan alles|wis}, {@code top} (where the nearest summit is).
 */
public final class SystemenSlice {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(Bewijzen::onTick);
        NeoForge.EVENT_BUS.addListener(SystemenSlice::commando);
        GidsBlad.registreer(new BioGids());
        BioZelftest.registreer("systemen", SystemenSlice::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    // --- dev commands ---------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        var systemen = Commands.literal("systemen");
        systemen.then(Commands.literal("titels")
                .then(Commands.literal("alles").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    zetJapan(p, true);
                    Bewijzen.top(p);
                    GuhQuests.saved(p).putInt(Bewijzen.NIES_KEY, Bewijzen.NIEZEN);
                    Visserguh.zetStap(p, Visserguh.KLAAR);
                    Titels.kijk(p);
                    zeg(ctx.getSource(), "[bio-systemen] de vier titels verdiend: " + BioTitels.ALLE.stream().filter(t -> Titels.heeft(p, t)).count());
                    return 1;
                }))
                .then(Commands.literal("wis").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    zetJapan(p, false);
                    GuhQuests.saved(p).remove(Bewijzen.TOP_KEY);
                    GuhQuests.saved(p).remove(Bewijzen.NIES_KEY);
                    Visserguh.zetStap(p, Visserguh.NIET);
                    Titels.kijk(p);
                    zeg(ctx.getSource(), "[bio-systemen] de vier titels weer op slot (de bewijs-advancements blijven staan)");
                    return 1;
                })));
        systemen.then(Commands.literal("japan")
                .then(Commands.literal("alles").executes(ctx -> {
                    zetJapan(ctx.getSource().getPlayerOrException(), true);
                    zeg(ctx.getSource(), "[bio-systemen] de hele Japan-verzameling staat op je naam (geen blokken gegeven)");
                    return 1;
                }))
                .then(Commands.literal("half").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, 0b010101010101);
                    GuhQuests.saved(p).putInt(Cadeaus.AANTAL_KEY, 7);
                    zeg(ctx.getSource(), "[bio-systemen] zes van de twaalf stukken staan op je naam");
                    return 1;
                }))
                .then(Commands.literal("wis").executes(ctx -> {
                    zetJapan(ctx.getSource().getPlayerOrException(), false);
                    zeg(ctx.getSource(), "[bio-systemen] de Japan-verzameling is vergeten");
                    return 1;
                })));
        systemen.then(Commands.literal("top").executes(ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            BlockPos hier = BlockPos.containing(ctx.getSource().getPosition());
            String regel = dichtsteTop(level, hier, 400);
            zeg(ctx.getSource(), "[bio-systemen] " + regel);
            return regel.startsWith("top") ? 1 : 0;
        }));
        event.getDispatcher().register(Commands.literal("guhs")
                .then(Commands.literal("bio").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(systemen)));
    }

    private static void zetJapan(ServerPlayer p, boolean alles) {
        if (alles) {
            GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, Cadeaus.REEKS_VOL);
            GuhQuests.saved(p).putInt(Cadeaus.AANTAL_KEY, Math.max(Cadeaus.REEKS.length, Cadeaus.aantal(p)));
        } else {
            GuhQuests.saved(p).remove(Cadeaus.REEKS_KEY);
            GuhQuests.saved(p).remove(Cadeaus.AANTAL_KEY);
        }
    }

    /** "top x y z (n islands, h above the meadow)" of the nearest summit within {@code straal} blocks, or why there is none. */
    static String dichtsteTop(ServerLevel level, BlockPos hier, int straal) {
        BioModel m = BioModel.van(level.getChunkSource().randomState());
        WolkTerrein.Eiland beste = null;
        int stapels = 0, tellen = 0;
        for (WolkTerrein.Stapel s : WolkTerrein.stapels(m, hier.getX() - straal, hier.getZ() - straal, hier.getX() + straal, hier.getZ() + straal)) {
            stapels += s.los ? 0 : 1;
            WolkTerrein.Eiland top = Bewijzen.top(m, s);
            if (top == null) {
                continue;
            }
            tellen++;
            if (beste == null || hier.distToCenterSqr(top.x, hier.getY(), top.z) < hier.distToCenterSqr(beste.x, hier.getY(), beste.z)) {
                beste = top;
            }
        }
        if (beste == null) {
            return "geen top binnen " + straal + " blokken (" + stapels + " stapels, geen hoog genoeg)";
        }
        return "top " + beste.x + " " + (beste.top + 1) + " " + beste.z + " (" + beste.stapel.eilanden.size() + " eilanden, " + (beste.top - WolkTerrein.grond(m, beste.stapel.x, beste.stapel.z))
                + " boven de weide; " + tellen + " van de " + stapels + " stapels hier tellen als top)";
    }

    private static void zeg(CommandSourceStack bron, String regel) {
        bron.sendSuccess(() -> Component.literal(regel), false);
        LOGGER.info(regel);
    }

    // --- the dev server's self test ------------------------------------------------------------------------------------------

    private static void zelftest(MinecraftServer server, ServerLevel level, BioZelftest.Melder meld) {
        // the Reisbureau: twenty destinations, five per duration, and today's offer still one per duration
        boolean vijf = Bestemming.ECHT.size() == 20;
        for (int duur : Bestemming.DUREN) {
            vijf &= Bestemming.metDuur(duur).size() == 5;
        }
        meld.check(vijf, "the Reisbureau has twenty destinations, five per duration");
        var aanbod = Reizen.aanbod(server);
        boolean vier = aanbod.size() == 4;
        for (int i = 0; vier && i < 4; i++) {
            vier = aanbod.get(i).minuten() == Bestemming.DUREN[i];
        }
        meld.check(vier, "today's offer: one trip per duration: " + aanbod);
        // every proof of this slice is a loaded advancement, every title has an icon item
        for (String a : new String[] {Bewijzen.KOMPAS_BIOME, Bewijzen.KOMPAS_GEVONDEN, Bewijzen.KIKKER_OP_BLAD, Bewijzen.HOOGSTE_EILAND, Bewijzen.REUS_DRIE}) {
            meld.check(server.getAdvancements().get(Guhs.id("quest/" + a)) != null, "the proof guhs:quest/" + a + " is loaded");
        }
        for (Titels.Titel t : BioTitels.ALLE) {
            meld.check(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.parse(t.icoon())),
                    "title " + t.id() + " has its icon " + t.icoon());
        }
        // a summit exists in the real Wolkenweide (asked of the model: no chunk is loaded)
        var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        meld.check(biomes.get(Bio.WOLKENWEIDE).isPresent(), "the Wolkenweide is a biome");
        BlockPos weide = BioZelftest.vind(level, Bio.WOLKENWEIDE, BlockPos.ZERO, 6400);
        if (weide == null) {
            meld.fout("no Wolkenweide within 6400 blocks: the summit rule was not looked at");
        } else {
            String regel = dichtsteTop(level, weide, 600);
            meld.check(regel.startsWith("top"), "a summit near the Wolkenweide at " + weide.getX() + " " + weide.getZ() + ": " + regel);
        }
    }

    private SystemenSlice() {
    }
}
