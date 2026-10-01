package nl.juiced.guhs.feature.favorietjes;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.Favorieten;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the favorietjes (2.10): every kind found once through its moment (and loved again after), the hints (warm /
 * cold, resting, only with the owner around), the happy buff and hearts that only go up, the clothes colour table, being
 * at a favourite (colour, friend), the favourite song in minigames, and the dagboekje stories (eerste keren, wist-je-datjes
 * once a day). (Template favorietjes_test_wei: grass at y 0.)
 */
public class FavorietjesGameTests {
    private static final String WEI = "favorietjes_test_wei";
    private static final String BATCH = "favorietjes";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        return guh;
    }

    /** Something of this kind that is NOT the favourite (the first other candidate). */
    static String nietFavoriet(MinecraftServer s, GuhEntity guh, FavorietSoort soort) {
        String fav = Favorieten.waarde(guh, soort);
        for (String k : Favorieten.kandidaten(s, soort)) {
            if (!k.equals(fav)) {
                return k;
            }
        }
        throw new IllegalStateException("only one candidate for " + soort);
    }

    /** Tries a value through the real moment of its kind (as the rest of the mod would). */
    static void moment(GuhEntity guh, ServerPlayer p, FavorietSoort soort, String waarde) {
        switch (soort) {
            case ETEN -> Band.moment(guh, p, Moment.GEGETEN, waarde);
            case PLEK -> Band.moment(guh, p, Moment.PLEK, waarde);
            case KNUFFEL -> Band.moment(guh, p, Moment.KNUFFEL, waarde);
            case LIEDJE -> Band.moment(guh, p, Moment.LIEDJE, waarde);
            case SPEELTJE -> Band.moment(guh, p, Moment.SPEELTJE, waarde);
            case EMOTE -> Band.moment(guh, p, Moment.EMOTE, waarde);
            case KLEUR -> Band.moment(guh, p, Moment.KLEDING, KledingKleuren.van(waarde).get(0).id());
            case VRIEND -> Band.moment(guh, p, Moment.VRIENDJE, waarde);
        }
    }

    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void favorietjesElkeSoortEenKeerOntdekt(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity a = guh(helper, p, new BlockPos(4, 1, 4));
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 8));
        MinecraftServer s = p.level().getServer();
        UUID id = Band.id(a);
        helper.assertTrue(Favorieten.waarde(a, FavorietSoort.VRIEND) != null, "two guhs: a friend favourite");
        int hartjes = Band.hartjes(a);
        for (FavorietSoort soort : FavorietSoort.values()) {
            String fav = Favorieten.waarde(a, soort);
            helper.assertTrue(!Favorieten.ontdekt(a, soort), "not found yet: " + soort);
            moment(a, p, soort, fav);
            helper.assertTrue(Favorieten.ontdekt(a, soort), "found through its moment: " + soort + " = " + fav);
            helper.assertTrue(Band.hartjes(a) >= hartjes, "hearts never go down");
            hartjes = Band.hartjes(a);
            helper.assertTrue(Dagboek.heeftEersteKeer(s, p.getUUID(), id, "favorietjes_fav_" + soort.id()), "the dagboek knows: " + soort);
            helper.assertTrue(GidsFeature.heeft(p, "lieve_vadsjes/favorietjes_" + soort.id()), "the advancement of " + soort);
            helper.assertTrue(Favorietjes.probeer(a, p, soort, fav) == Favorietjes.Uitkomst.WEER, "found once: then 'again': " + soort);
        }
        helper.assertTrue(Favorieten.ontdekt(s, p.getUUID(), id).equals(EnumSet.allOf(FavorietSoort.class)), "all eight stored");
        helper.assertTrue(hartjes >= 400, "a big bonus: " + hartjes);
        helper.assertTrue(Dagboek.heeftEersteKeer(s, p.getUUID(), id, "eerste_favoriet") && Dagboek.heeftEersteKeer(s, p.getUUID(), id, "favorietjes_alle"),
                "eerste_favoriet + all of them in the dagboek");
        helper.assertTrue(GidsFeature.heeft(p, "lieve_vadsjes/favorietjes_eerste") && GidsFeature.heeft(p, "lieve_vadsjes/favorietjes_alle"),
                "the first and the all-eight advancements");
        helper.assertTrue(!Favorieten.ontdekt(b, FavorietSoort.ETEN), "the other guh has its own secrets");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void favorietjesHintsWarmEnKoud(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        MinecraftServer s = p.level().getServer();
        // the families
        helper.assertTrue(Hints.warm(s, null, FavorietSoort.ETEN, "guhs:macaron_roze", "guhs:macaron_mint")
                && Hints.warm(s, null, FavorietSoort.ETEN, "guhs:kaasijsje_roze", "guhs:kaasknabbel_milkshake")
                && !Hints.warm(s, null, FavorietSoort.ETEN, "guhs:kaasijsje_roze", "minecraft:sweet_berries"), "snack families");
        helper.assertTrue(Hints.warm(s, null, FavorietSoort.KLEUR, "roze", "rood") && !Hints.warm(s, null, FavorietSoort.KLEUR, "roze", "groen"),
                "the colour wheel");
        helper.assertTrue(Hints.warm(s, null, FavorietSoort.LIEDJE, "disco:mika_mambo", "disco:vadsige_tango")
                && !Hints.warm(s, null, FavorietSoort.LIEDJE, "disco:mika_mambo", "koortje:toonladder"), "songs of the same kind");
        helper.assertTrue(Hints.warm(s, null, FavorietSoort.PLEK, "guhs:pink_puffs", "guhs:guh_fields")
                && !Hints.warm(s, null, FavorietSoort.PLEK, "guhs:pink_puffs", "minecraft:desert"), "places of the same family");
        helper.assertTrue(!Hints.warm(s, null, FavorietSoort.ETEN, "guhs:guh_cupcake", "guhs:guh_cupcake"), "the favourite itself is never a hint");
        // real hints for the owner, resting in between, never a discovery
        String fout = nietFavoriet(s, guh, FavorietSoort.SPEELTJE);
        Favorietjes.Uitkomst u = Favorietjes.probeer(guh, p, FavorietSoort.SPEELTJE, fout);
        boolean warm = Hints.warm(s, Band.id(guh), FavorietSoort.SPEELTJE, Favorieten.waarde(guh, FavorietSoort.SPEELTJE), fout);
        helper.assertTrue(u == (warm ? Favorietjes.Uitkomst.WARM : Favorietjes.Uitkomst.KOUD), "a hint: " + u);
        helper.assertTrue(Favorietjes.probeer(guh, p, FavorietSoort.SPEELTJE, fout) == Favorietjes.Uitkomst.NIETS, "then it rests a moment");
        Favorietjes.vergeetRust(guh);
        helper.assertTrue(Favorietjes.probeer(guh, p, FavorietSoort.SPEELTJE, fout) != Favorietjes.Uitkomst.NIETS, "and hints again later");
        helper.assertTrue(!Favorieten.ontdekt(guh, FavorietSoort.SPEELTJE), "a hint is not a discovery");
        // the texts are there (all variants of every kind and temperature)
        net.minecraft.locale.Language taal = net.minecraft.locale.Language.getInstance();
        for (FavorietSoort soort : FavorietSoort.values()) {
            for (int i = 0; i < Favorietjes.HINT_VARIANTEN; i++) {
                helper.assertTrue(taal.has("gui.guhs.favorietjes.hint.warm." + soort.id() + "." + i)
                        && taal.has("gui.guhs.favorietjes.hint.koud." + soort.id() + "." + i), "hint texts of " + soort);
            }
            helper.assertTrue(taal.has("gui.guhs.favorietjes.ontdekt." + soort.id()) && taal.has("gui.guhs.dagboek.eerste.favorietjes_fav_" + soort.id()),
                    "discovery texts of " + soort);
        }
        // without the owner nearby: no hint and no discovery (only a happy guh)
        p.snapTo(p.getX() + 200, p.getY(), p.getZ());
        Favorietjes.vergeetRust(guh);
        helper.assertTrue(Favorietjes.probeer(guh, p, FavorietSoort.SPEELTJE, fout) == Favorietjes.Uitkomst.NIETS, "no hint far away");
        String fav = Favorieten.waarde(guh, FavorietSoort.SPEELTJE);
        helper.assertTrue(Favorietjes.probeer(guh, p, FavorietSoort.SPEELTJE, fav) == Favorietjes.Uitkomst.BLIJ
                && !Favorieten.ontdekt(guh, FavorietSoort.SPEELTJE) && Band.isBlij(guh), "unseen: blij, but not found");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void favorietjesBlijBuffEnHartjesNooitOmlaag(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.assertTrue(!Band.isBlij(guh) && Band.klusSnelheid(guh) == 1f, "not blij to start with");
        int voor = Band.hartjes(guh);
        helper.assertTrue(Favorietjes.probeer(guh, p, FavorietSoort.ETEN, Favorieten.waarde(guh, FavorietSoort.ETEN)) == Favorietjes.Uitkomst.ONTDEKT,
                "the favourite snack");
        int na = Band.hartjes(guh);
        helper.assertTrue(na >= voor + 50, "FAVORIET_ONTDEKT hearts: " + voor + " -> " + na);
        helper.assertTrue(Band.isBlij(guh) && Band.klusSnelheid(guh) == 1.5f, "blij: faster chores");
        helper.assertTrue(FavorietLiedje.volume(guh) > 1f, "blij: cheers louder");
        // lots of hints, again-moments, wrong things: hearts never go down
        MinecraftServer s = p.level().getServer();
        for (int i = 0; i < 30; i++) {
            Favorietjes.vergeetRust(guh);
            FavorietSoort soort = FavorietSoort.values()[i % 7];
            Favorietjes.probeer(guh, p, soort, i % 2 == 0 ? nietFavoriet(s, guh, soort) : Favorieten.waarde(guh, soort));
            int nu = Band.hartjes(guh);
            helper.assertTrue(nu >= na, "hearts never go down: " + na + " -> " + nu);
            na = nu;
        }
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void favorietjesKleurTabel(GameTestHelper helper) {
        for (String kleur : Favorieten.KLEUREN) {
            helper.assertTrue(!KledingKleuren.van(kleur).isEmpty(), "clothes in every colour: " + kleur);
        }
        helper.assertTrue("rood".equals(Favorieten.kleur(GuhClothes.RED_BOWTIE)) && "roze".equals(Favorieten.kleur(GuhClothes.PINK_ONESIE))
                && "zwart".equals(Favorieten.kleur(GuhClothes.BLACK_BOWTIE)) && "goud".equals(Favorieten.kleur(GuhClothes.ROYAL_CROWN)),
                "names and crowns");
        for (GuhClothes c : GuhClothes.values()) {
            helper.assertTrue(c.slot != GuhClothes.Slot.HAAR || Favorieten.kleur(c) == null, "hairstyles have no colour: " + c);
        }
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void favorietjesBijEenFavoriet(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity a = guh(helper, p, new BlockPos(4, 1, 4));
        GuhEntity b = guh(helper, p, new BlockPos(6, 1, 4));
        // wearing its favourite colour: blij, and found (the owner is near)
        String kleur = Favorieten.waarde(a, FavorietSoort.KLEUR);
        a.wear(KledingKleuren.van(kleur).get(0));
        Favorietjes.kijk(a);
        helper.assertTrue(Favorieten.ontdekt(a, FavorietSoort.KLEUR) && Band.isBlij(a), "wearing its colour: found and blij");
        // next to its favourite friend
        String vriend = Favorieten.waarde(a, FavorietSoort.VRIEND);
        helper.assertTrue(Band.id(b).toString().equals(vriend), "the only other guh is its friend favourite");
        Favorietjes.kijk(a);
        helper.assertTrue(Favorieten.ontdekt(a, FavorietSoort.VRIEND), "next to its friend: found");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void favorietjesLiedjeJuichtHarder(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        for (String liedje : Favorieten.kandidaten(p.level().getServer(), FavorietSoort.LIEDJE)) {
            helper.assertTrue(FavorietLiedje.noten(liedje) != null, "every song can be sung: " + liedje);
        }
        helper.assertTrue(FavorietLiedje.juich(guh) == 1f && !FavorietLiedje.zingt(guh), "not blij: a normal cheer, no song");
        // it knows its song: a minigame starts, it hums and is blij; a record: it sings loud
        Favorieten.ontdek(guh, p, FavorietSoort.LIEDJE);
        Band.moment(guh, p, Moment.MINIGAME_START, "sjoelen");
        helper.assertTrue(Band.isBlij(guh) && FavorietLiedje.zingt(guh), "hums its song at the start, blij");
        helper.assertTrue(FavorietLiedje.juich(guh) == FavorietLiedje.BLIJ_VOLUME, "blij: louder");
        Band.moment(guh, p, Moment.RECORD, "sjoelen");
        helper.assertTrue(FavorietLiedje.zingt(guh), "sings at a record");
        helper.succeedWhen(() -> {
            helper.assertTrue(!FavorietLiedje.zingt(guh), "the song ends");
            helper.assertTrue(GidsFeature.heeft(p, "quest/favorietjes_juichen"), "the cheering quest");
            weg(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void favorietjesDagboekVerhaaltjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity a = guh(helper, p, new BlockPos(4, 1, 4));
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 8));
        MinecraftServer s = p.level().getServer();
        UUID id = Band.id(a);
        // the generated texts are loaded
        for (String sleutel : List.of("droom", "aai", "eten_ijskoud", "plek_guhmensie", "spel_sjoelen", "klus_opgraven", "speeltje_tunnel",
                "emote_vahoeg", "dim_nether", "besties")) {
            helper.assertTrue(Verhaaltjes.varianten(sleutel) > 0, "texts of " + sleutel);
        }
        // eerste keren from moments
        Band.moment(a, p, Moment.GEGETEN, "guhs:kaasijsje_roze");
        Band.moment(a, p, Moment.DIMENSIE, "minecraft:the_nether");
        Band.moment(a, p, Moment.LIEDJE, "disco:njeg_njeg_boogie");
        for (String e : List.of("favorietjes_ijsje", "favorietjes_nether", "favorietjes_disco")) {
            helper.assertTrue(Dagboek.heeftEersteKeer(s, p.getUUID(), id, e), "eerste keer " + e);
        }
        // a wist-je-datje at most once a day per subject, and at most PER_DAG a day
        BandData.Rec r = BandData.get(s).vind(p.getUUID(), id);
        int n0 = r.wist.size();
        a.getPersistentData().remove(Verhaaltjes.DAG_KEY);
        helper.assertTrue(Verhaaltjes.wist(a, "aai", 1, net.minecraft.network.chat.Component.empty()), "a wist-je-datje");
        helper.assertTrue(!Verhaaltjes.wist(a, "aai", 1, net.minecraft.network.chat.Component.empty()), "not twice a day about the same");
        helper.assertTrue(r.wist.size() == n0 + 1 && r.wist.get(0).key().startsWith("gui.guhs.wistjedat.favorietjes.aai."), "newest first");
        int geschreven = 1;
        for (String sleutel : List.of("droom", "wakker", "regen", "nacht", "rit", "record", "spel", "klus", "guhkamer", "onweer")) {
            if (Verhaaltjes.wist(a, sleutel, 1, net.minecraft.network.chat.Component.empty())) {
                geschreven++;
            }
        }
        helper.assertTrue(geschreven == Verhaaltjes.PER_DAG, "at most " + Verhaaltjes.PER_DAG + " a day: " + geschreven);
        // besties write about each other
        Vriendjes.samen(a, b, Vriendjes.BESTIES);
        helper.assertTrue(Dagboek.heeftEersteKeer(s, p.getUUID(), id, "favorietjes_besties")
                && Dagboek.heeftEersteKeer(s, p.getUUID(), Band.id(b), "favorietjes_besties"), "besties: both");
        weg(helper, p);
        helper.succeed();
    }
}
