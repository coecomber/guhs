package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the band (2.10, fundament): hearts never go down and respect the per-day caps, the level thresholds and
 * the level listener (once per level), the happy buff, the moments bus (feeding, the menu's big cuddle), favourites
 * (made once, deterministic, discovered once), friendships (thresholds, only up), the dagboekje, "waar is mijn guh"
 * (picked up, in the Guh Wheel, back in your pockets) and the Mijn guhs snapshot. (Template band_test_wei: grass at y 0.)
 */
public class BandGameTests {
    private static final String WEI = "band_test_wei";
    private static final String BATCH = "band";

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

    /** A tamed guh of this player (a band guh). */
    static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        return guh;
    }

    /** Forget today's caps of this guh (as if a new day started), without touching the world's clock. */
    static void nieuweDag(GuhEntity guh) {
        BandData.Rec r = BandData.get(guh.level().getServer()).vind(guh.getOwnerUUID(), guh.getUUID());
        if (r != null) {
            r.dag = -1;
        }
    }

    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandHartjesGaanNooitOmlaag(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.assertTrue(Band.isBandGuh(guh) && Band.hartjes(guh) == 0, "a fresh band guh without hearts");
        helper.assertTrue(Band.geefHartjes(guh, p, 0, Reden.OVERIG) == 0 && Band.geefHartjes(guh, p, -50, Reden.OVERIG) == 0,
                "nothing or less than nothing adds nothing (and takes nothing)");
        int vorige = 0;
        for (int i = 0; i < 40; i++) {
            Band.geefHartjes(guh, p, 1, Reden.AAIEN);
            int nu = Band.hartjes(guh);
            helper.assertTrue(nu >= vorige, "hearts never go down");
            vorige = nu;
        }
        helper.assertTrue(Band.hartjes(guh) == Reden.AAIEN.dagMax(), "petting is capped per day: " + Band.hartjes(guh));
        helper.assertTrue(Band.geefHartjes(guh, p, 5, Reden.AAIEN) == 0, "capped: 0 added");
        helper.assertTrue(Band.geefHartjes(guh, p, 5, Reden.KNUFFELEN) == 5, "another reason has its own cap");
        nieuweDag(guh);
        helper.assertTrue(Band.geefHartjes(guh, p, 1, Reden.AAIEN) == 1, "a new day, new pets");
        helper.assertTrue(Band.hartjes(guh) == Reden.AAIEN.dagMax() + 6, "total " + Band.hartjes(guh));
        // not a band guh: harmless no-ops
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(8, 1, 8));
        helper.assertTrue(!Band.isBandGuh(wild) && Band.geefHartjes(wild, p, 10, Reden.OVERIG) == 0 && Band.hartjes(wild) == 0,
                "a wild guh has no hearts");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandNiveausEnEenMeldingPerNiveau(GameTestHelper helper) {
        helper.assertTrue(BandNiveau.van(0) == BandNiveau.GEEN && BandNiveau.van(99) == BandNiveau.GEEN && BandNiveau.van(100) == BandNiveau.LIEF
                && BandNiveau.van(599) == BandNiveau.LIEF && BandNiveau.van(600) == BandNiveau.MEGA && BandNiveau.van(1999) == BandNiveau.MEGA
                && BandNiveau.van(2000) == BandNiveau.ZIELSGUH && BandNiveau.van(99999) == BandNiveau.ZIELSGUH, "the thresholds 100 / 600 / 2000");
        helper.assertTrue(BandNiveau.LIEF.volgende() == BandNiveau.MEGA && BandNiveau.ZIELSGUH.volgende() == null, "the next level");
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        UUID id = Band.id(guh);
        List<BandNiveau> gemeld = new CopyOnWriteArrayList<>();
        Band.opNiveau((eigenaar, g, bandId, niveau) -> {
            if (bandId.equals(id)) {
                gemeld.add(niveau);
            }
        });
        // fill up over a few "days" (the caps), with every reason
        for (int dag = 0; dag < 6 && Band.hartjes(guh) < 2100; dag++) {
            nieuweDag(guh);
            for (Reden r : Reden.values()) {
                Band.geefHartjes(guh, p, r.dagMax(), r);
                Band.geefHartjes(guh, p, r.dagMax(), r);   // (capped: nothing more)
            }
        }
        helper.assertTrue(Band.hartjes(guh) >= 2000 && Band.niveau(guh) == BandNiveau.ZIELSGUH, "zielsguh: " + Band.hartjes(guh));
        helper.assertTrue(gemeld.equals(List.of(BandNiveau.LIEF, BandNiveau.MEGA, BandNiveau.ZIELSGUH)), "each level once, in order: " + gemeld);
        helper.assertTrue(GuhHooks.heeft(guh, BandVlaggen.ZIELSGUH), "a zielsguh carries the flag");
        helper.assertTrue(Dagboek.heeftEersteKeer(p.level().getServer(), p.getUUID(), id, "eerste_lief")
                && Dagboek.heeftEersteKeer(p.level().getServer(), p.getUUID(), id, "eerste_zielsguh"), "the level-ups are in the dagboekje");
        helper.assertTrue(Band.aantal(p.level().getServer(), p.getUUID(), BandNiveau.ZIELSGUH) >= 1, "one zielsguh for the Guhkamer");
        helper.assertTrue(nl.juiced.guhs.feature.gids.GidsFeature.heeft(p, "lieve_vadsjes/band_zielsguh"), "the advancement");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandOfflineNiveauBijInloggen(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 1, 5));
        UUID owner = UUID.randomUUID();   // an owner who isn't online
        guh.setOwnerUUID(owner);
        guh.setTame(true, true);
        UUID id = Band.id(guh);
        List<BandNiveau> gemeld = new CopyOnWriteArrayList<>();
        Band.opNiveau((eigenaar, g, bandId, niveau) -> {
            if (bandId.equals(id)) {
                gemeld.add(niveau);
            }
        });
        Band.geefHartjes(guh, null, 400, Reden.FAVORIET_ONTDEKT);
        helper.assertTrue(Band.hartjes(guh) == 400 && gemeld.isEmpty(), "while offline: stored, not announced yet");
        BandData.Rec r = BandData.get(helper.getLevel().getServer()).vind(owner, id);
        helper.assertTrue(r != null && r.teMelden.equals(List.of(BandNiveau.LIEF.ordinal())), "waiting for the login");
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandBlijGeeftAnderhalfKeer(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.assertTrue(!Band.isBlij(guh) && Band.klusSnelheid(guh) == 1.0f, "not blij yet");
        Band.maakBlij(guh, 200);
        helper.assertTrue(Band.isBlij(guh) && Band.klusSnelheid(guh) == 1.5f && GuhHooks.heeft(guh, BandVlaggen.BLIJ), "blij: faster chores, the flag");
        helper.assertTrue(Band.geefHartjes(guh, p, 3, Reden.OVERIG) == 5, "x1.5 rounded up");
        weg(helper, p);
        helper.succeed();
    }

    /** 1.2.0: a tap is only petting: no sitting down or standing up, and always the squish, also after today's hearts cap. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void bandAaienIsAltijdEenMomentje(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.runAfterDelay(10, () -> {
            nl.juiced.guhs.quest.GuhQuests.saved(p).remove(BandEvents.AAI_TELLER);
            guh.emotes.stop();
            guh.onOwnerTap(p);
            helper.assertTrue(!guh.isOrderedToSit() && !guh.isInSittingPose(), "a tap doesn't sit any more");
            helper.assertTrue(guh.emotes.current() == nl.juiced.guhs.feature.emotes.Emote.AAIEN, "the squish: " + guh.emotes.current());
            helper.assertTrue(Band.hartjes(guh) == Reden.AAIEN.standaard(), "a heart for the pet: " + Band.hartjes(guh));
            helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.saved(p).getIntOr(BandEvents.AAI_TELLER, 0) == 1, "the first pet is counted (menu hint)");
            // today's petting hearts used up: still a sweet moment, no more hearts
            for (int i = 0; i < Reden.AAIEN.dagMax(); i++) {
                Band.geefHartjes(guh, p, 1, Reden.AAIEN);
            }
            int vol = Band.hartjes(guh);
            for (int i = 0; i < 4; i++) {
                guh.emotes.stop();
                guh.onOwnerTap(p);
                helper.assertTrue(guh.emotes.current() == nl.juiced.guhs.feature.emotes.Emote.AAIEN, "always the squish (" + i + ")");
            }
            helper.assertTrue(Band.hartjes(guh) == vol, "capped: no more hearts today: " + Band.hartjes(guh));
            helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.saved(p).getIntOr(BandEvents.AAI_TELLER, 0) == BandEvents.AAI_TIPS,
                    "the hint only the first " + BandEvents.AAI_TIPS + " times");
            // a sitting guh stays sitting (standing up is the menu's button), and the emote picker doesn't offer petting
            guh.emotes.stop();
            guh.toggleSit();
            guh.onOwnerTap(p);
            helper.assertTrue(guh.isOrderedToSit(), "still sitting after a pet");
            helper.assertTrue(!nl.juiced.guhs.feature.emotes.Emote.AAIEN.kiesbaar()
                    && !nl.juiced.guhs.feature.emotes.Emote.kiesbare().contains(nl.juiced.guhs.feature.emotes.Emote.AAIEN), "not in the picker");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void bandVoerenEnKnuffelenGevenMomenten(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        UUID id = Band.id(guh);
        List<String> momenten = new CopyOnWriteArrayList<>();
        Band.opMoment((g, speler, m, waarde) -> {
            if (Band.id(g).equals(id)) {
                momenten.add(m.id() + ":" + waarde);
            }
        });
        // feeding a snack by hand (the real click)
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUH_CUPCAKE.get(), 3));
        guh.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().getCount() == 2, "one cupcake eaten");
        helper.assertTrue(Band.hartjes(guh) == Reden.VOEREN.standaard(), "feeding gives hearts: " + Band.hartjes(guh));
        helper.assertTrue(momenten.contains("gegeten:guhs:guh_cupcake"), "the GEGETEN moment with the item: " + momenten);
        helper.assertTrue(Dagboek.stat(p.level().getServer(), p.getUUID(), id, DagboekStat.KNABBELS_GEGETEN) == 1, "counted in the dagboekje");
        // the menu's big cuddle (once it stands on the grass), then 30 seconds rest
        helper.runAfterDelay(10, () -> {
            guh.emotes.stop();
            helper.assertTrue(BandEvents.knuffel(guh, p), "a cuddle");
            int na = Band.hartjes(guh);
            helper.assertTrue(!BandEvents.knuffel(guh, p) && Band.hartjes(guh) == na, "right after: resting, nothing more");
            helper.assertTrue(momenten.contains("geknuffeld:") && Dagboek.heeftEersteKeer(p.level().getServer(), p.getUUID(), id, "eerste_knuffel"),
                    "the GEKNUFFELD moment and the first time");
            // a tap: a little pet
            BandEvents.aai(guh, p);
            helper.assertTrue(momenten.contains("aangeaaid:"), "the AANGEAAID moment");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandFavorietenVastEnEenKeerOntdekt(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity a = guh(helper, p, new BlockPos(3, 1, 3));
        Map<FavorietSoort, String> fav = Favorieten.van(a);
        for (FavorietSoort s : FavorietSoort.values()) {
            if (s != FavorietSoort.VRIEND) {
                helper.assertTrue(fav.get(s) != null && Favorieten.kandidaten(p.level().getServer(), s).contains(fav.get(s)), "a favourite " + s + ": " + fav.get(s));
            }
        }
        helper.assertTrue(!fav.containsKey(FavorietSoort.VRIEND), "no friend favourite without a second guh");
        helper.assertTrue(Favorieten.van(a).equals(fav), "made once: the same every time");
        helper.assertTrue(Favorieten.kandidaten(p.level().getServer(), FavorietSoort.ETEN).size() >= 10, "at least ten favourite foods");
        helper.assertTrue(Favorieten.kandidaten(p.level().getServer(), FavorietSoort.PLEK).contains("guhs:pink_puffs"), "the favourite places tag");
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 8));
        helper.assertTrue(Band.id(b).toString().equals(Favorieten.waarde(a, FavorietSoort.VRIEND)), "with a second guh: that one is its friend");
        // discovering
        helper.assertTrue(!Favorieten.ontdekt(a, FavorietSoort.ETEN), "not discovered yet");
        helper.assertTrue(Favorieten.ontdek(a, p, FavorietSoort.ETEN) && !Favorieten.ontdek(a, p, FavorietSoort.ETEN), "discovered once");
        helper.assertTrue(Favorieten.ontdekt(p.level().getServer(), p.getUUID(), Band.id(a)).equals(EnumSet.of(FavorietSoort.ETEN)), "stored");
        helper.assertTrue(!Favorieten.naam(FavorietSoort.ETEN, fav.get(FavorietSoort.ETEN)).getString().isEmpty(), "it has a name");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandVriendjesDrempels(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity a = guh(helper, p, new BlockPos(3, 1, 3));
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 8));
        MinecraftServer s = p.level().getServer();
        List<Boolean> nieuw = new CopyOnWriteArrayList<>();
        Vriendjes.opNieuw((server, x, y, besties) -> {
            if ((x.equals(Band.id(a)) && y.equals(Band.id(b))) || (x.equals(Band.id(b)) && y.equals(Band.id(a)))) {
                nieuw.add(besties);
            }
        });
        helper.assertTrue(Vriendjes.samen(a, b, Vriendjes.VRIENDJES - 1) == Vriendjes.VRIENDJES - 1 && !Vriendjes.vrienden(s, Band.id(a), Band.id(b)),
                "almost friends");
        helper.assertTrue(Vriendjes.samen(b, a, -100) == Vriendjes.VRIENDJES - 1, "points never go down");
        Vriendjes.samen(a, b, 1);
        helper.assertTrue(Vriendjes.vrienden(s, Band.id(a), Band.id(b)) && nieuw.equals(List.of(false)), "friends, told once: " + nieuw);
        helper.assertTrue(Vriendjes.vriendenVan(s, Band.id(a)).equals(List.of(Band.id(b))) && Vriendjes.bestie(s, Band.id(a)) == null,
                "a friend, not a bestie yet");
        Vriendjes.samen(a, b, Vriendjes.BESTIES);
        helper.assertTrue(Vriendjes.besties(s, Band.id(a), Band.id(b)) && Band.id(b).equals(Vriendjes.bestie(s, Band.id(a)))
                && nieuw.equals(List.of(false, true)), "besties: " + nieuw);
        helper.assertTrue(Vriendjes.samen(a, a, 50) == 0, "no friendship with itself");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandDagboekje(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        UUID id = Band.id(guh);
        helper.assertTrue(Dagboek.heeftEersteKeer(p.level().getServer(), p.getUUID(), id, "getemd"), "taming is the first page");
        Dagboek.tel(guh, DagboekStat.KLUSJES, 3);
        Dagboek.tel(guh, DagboekStat.KLUSJES, 2);
        Dagboek.tel(guh, DagboekStat.KLUSJES, -7);
        helper.assertTrue(Dagboek.stat(p.level().getServer(), p.getUUID(), id, DagboekStat.KLUSJES) == 5, "stats add up (never down)");
        helper.assertTrue(Dagboek.eersteKeer(guh, p, "eerste_klusje") && !Dagboek.eersteKeer(guh, p, "eerste_klusje"), "a first time is once");
        for (int i = 0; i < 45; i++) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.test", String.valueOf(i));
        }
        BandData.Rec r = BandData.get(p.level().getServer()).vind(p.getUUID(), id);
        helper.assertTrue(r.wist.size() == BandData.WIST_MAX && r.wist.get(0).args().get(0).equals(net.minecraft.network.chat.Component.literal("44")), "newest first, 40 kept");
        // saved and loaded
        CompoundTag tag = BandData.get(p.level().getServer()).save(new CompoundTag());
        BandData.Rec terug = BandData.load(tag).vind(p.getUUID(), id);
        helper.assertTrue(terug != null && terug.stat(DagboekStat.KLUSJES) == 5 && terug.heeftEerste("eerste_klusje") && terug.wist.size() == 40,
                "save and load keep the dagboekje");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void bandVolgerOppakkenEnGuhWiel(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        UUID id = Band.id(guh);
        MinecraftServer s = p.level().getServer();
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        helper.assertTrue(GuhVolger.plek(s, p.getUUID(), id).soort() == PlekSoort.WERELD, "walking around");
        ItemStack item = PickedUpGuhItem.pickUp(guh);
        helper.assertTrue(GuhVolger.plek(s, p.getUUID(), id).soort() == PlekSoort.ITEM_SPELER, "picked up: in the pockets");
        // into a Guh Wheel
        BlockPos wiel = new BlockPos(6, 1, 3);
        helper.setBlock(wiel, ModBlocks.GUH_WHEEL.get().defaultBlockState().setValue(nl.juiced.guhs.block.GuhWheelBlock.FACING, Direction.SOUTH));
        BlockPos abs = helper.absolutePos(wiel);
        BlockState state = helper.getLevel().getBlockState(abs);
        p.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        state.useItemOn(p.getMainHandItem(), helper.getLevel(), p, InteractionHand.MAIN_HAND, hit);
        Plek plek = GuhVolger.plek(s, p.getUUID(), id);
        helper.assertTrue(plek.soort() == PlekSoort.GUHWIEL && plek.pos().equals(abs), "running in the wheel: " + plek);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), p, hit);
        helper.assertTrue(GuhVolger.plek(s, p.getUUID(), id).soort() == PlekSoort.ITEM_SPELER, "out of the wheel: back in the pockets");
        helper.assertTrue(!GuhVolger.tekst(GuhVolger.plek(s, p.getUUID(), id)).getString().isEmpty(), "a text for the Guhdex");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandMijnGuhsSnapshot(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhEntity a = guh(helper, p, new BlockPos(3, 1, 3));
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 8));
        a.setCustomName(net.minecraft.network.chat.Component.literal("Knabbeltje"));
        Band.geefHartjes(a, p, 150, Reden.FAVORIET_ONTDEKT);
        Favorieten.van(a);
        Favorieten.ontdek(a, p, FavorietSoort.KLEUR);
        CompoundTag data = MijnGuhs.snapshot(p, Band.id(a));
        ListTag guhs = data.getListOrEmpty("Guhs");
        helper.assertTrue(guhs.size() == 2 && data.getStringOr("Focus", "").equals(Band.id(a).toString()), "both guhs, and the focus");
        CompoundTag eerste = null;
        for (int i = 0; i < guhs.size(); i++) {
            if (guhs.getCompoundOrEmpty(i).getStringOr("Id", "").equals(Band.id(a).toString())) {
                eerste = guhs.getCompoundOrEmpty(i);
            }
        }
        helper.assertTrue(eerste != null && eerste.getStringOr("Naam", "").equals("Knabbeltje") && eerste.getIntOr("Hartjes", 0) == 150
                && eerste.getIntOr("Niveau", 0) == BandNiveau.LIEF.ordinal() && eerste.getIntOr("Volgende", 0) == 600, "name, hearts, level: " + eerste);
        helper.assertTrue(eerste.getCompoundOrEmpty("Looks").getStringOr("Variant", "").equals(a.getVariant().id()), "the looks for the preview");
        ListTag fav = eerste.getListOrEmpty("Fav");
        int bekend = 0;
        for (int i = 0; i < fav.size(); i++) {
            bekend += fav.getCompoundOrEmpty(i).getStringOr("Naam", "").isEmpty() ? 0 : 1;
        }
        helper.assertTrue(fav.size() == FavorietSoort.values().length && bekend == 1, "one discovered favourite, the rest ???");
        helper.assertTrue(eerste.getCompoundOrEmpty("Stats").keySet().size() == DagboekStat.values().length && !eerste.getStringOr("Plek", "").isEmpty()
                && !eerste.getListOrEmpty("Eerste").isEmpty(), "stats, where it is, eerste keren");
        helper.assertTrue(BandData.snapshot(p).size() == 2, "BandData.snapshot");
        helper.assertTrue(b.isAlive(), "(the second guh)");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH)
    public static void bandTemmenOpentHetHoofdstuk(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        List<String> momenten = new ArrayList<>();
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 1, 5));
        UUID id = guh.getUUID();
        Band.opMoment((g, speler, m, waarde) -> {
            if (g.getUUID().equals(id)) {
                momenten.add(m.id());
            }
        });
        guh.tame(p);
        helper.assertTrue(momenten.contains("getemd"), "tamed: the GETEMD moment");
        helper.assertTrue(nl.juiced.guhs.feature.gids.GidsFeature.heeft(p, "lieve_vadsjes/root"), "the Lieve vadsjes tab opens");
        helper.assertTrue(BandData.get(p.level().getServer()).vind(p.getUUID(), id) != null, "it has a record");
        weg(helper, p);
        helper.succeed();
    }
}
