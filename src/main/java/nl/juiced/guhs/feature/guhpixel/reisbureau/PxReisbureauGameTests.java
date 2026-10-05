package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.Tekst;

/**
 * Game tests of the guhpixel slice "reisbureau" (batch px_reisbureau): the daily offer, the questline of the
 * Reisagent-guh, a whole trip in real time (Klok.spoel), ONE guh at a time, "never lost" (the balie broken, the trip
 * dropped while the guh walks off, the guh both stored and in the world, calling back early), two players at once, the
 * rewards (ansichtkaart, souvenir, a duplicate becomes a stamp, the Gouden koffertje), the sunglasses, the screen's state
 * and the server-side checks of what the client asks, the Guhdex section, the template and the tables.
 * None of it needs the guhpixel dimension. The clock offset (Klok.spoel) is shared by the whole server, so the two tests
 * that wait for the guh's walk run alone, in their own batches (px_reisbureau_proef, px_reisbureau_kwijt); the others finish
 * within one tick and put the clock back.
 */
public final class PxReisbureauGameTests {
    private static final String BATCH = "px_reisbureau";
    private static final String KAMER = "reisbureau_test_balie";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    private static ServerPlayer speler(GameTestHelper helper, int stap) {
        ServerPlayer p = PxTest.speler(helper);
        Reizen.data(p).remove("Reis");
        Reizen.zetStap(p, stap);
        return p;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer p, String naam, int x, int z) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(x, 2, z));
        guh.tame(p);
        guh.setCustomName(Component.literal(naam));
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        return guh;
    }

    private static BlockPos balie(GameTestHelper helper, int x, int z) {
        BlockPos rel = new BlockPos(x, 2, z);
        helper.setBlock(rel, ReisbureauSlice.BALIE.get().defaultBlockState().setValue(BalieBlock.FACING, Direction.NORTH));
        return helper.absolutePos(rel);
    }

    private static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static Bestemming vandaag(GameTestHelper helper, int groep) {
        return Reizen.aanbod(helper.getLevel().getServer()).get(groep);
    }

    private static void klaar(GameTestHelper helper, ServerPlayer... spelers) {
        ServerLevel level = helper.getLevel();
        for (ServerPlayer p : spelers) {
            Reizen.data(p).remove("Reis");
            Praat.vergeet(p);
        }
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(3))) {
            g.discard();
        }
        PxTest.klaar(helper, spelers);
        helper.succeed();
    }

    // =====================================================================================================================
    // the offer
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void aanbodVierPerDag(GameTestHelper helper) {
        for (long seed : new long[] {0L, 20260926L, -77L}) {
            Bestemming[] gisteren = null;
            for (long dag = 20000; dag < 20040; dag++) {
                List<Bestemming> a = Reizen.aanbod(seed, dag);
                helper.assertTrue(a.size() == 4, "four trips a day");
                for (int g = 0; g < 4; g++) {
                    helper.assertTrue(a.get(g).minuten() == Bestemming.DUREN[g] && !a.get(g).isProef(), "one trip per duration, shortest first: " + a);
                    helper.assertTrue(gisteren == null || gisteren[g] != a.get(g), "never the same destination two days in a row: " + a.get(g) + " on day " + dag);
                }
                helper.assertTrue(a.equals(Reizen.aanbod(seed, dag)), "the same for everyone: derived from the day and the seed");
                gisteren = a.toArray(new Bestemming[0]);
            }
            // every block of four days shows all four destinations of every duration, so the whole pool comes by
            Set<Bestemming> gezien = EnumSet.noneOf(Bestemming.class);
            for (long dag = 20000; dag < 20008; dag++) {
                gezien.addAll(Reizen.aanbod(seed, dag));
            }
            helper.assertTrue(gezien.containsAll(Bestemming.ECHT), "within eight days all sixteen destinations were offered, seed " + seed + ": " + gezien);
        }
        helper.assertTrue(!Reizen.aanbod(1L, 20000).equals(Reizen.aanbod(1L, 20001)), "another day, another offer");
        helper.assertTrue(Bestemming.ECHT.size() == 16 && Bestemming.metDuur(60).size() == 4 && Bestemming.metDuur(1440).size() == 4, "sixteen, four per duration");
        helper.assertTrue(Bestemming.LINGSESDIJK.kans() == 5 && Bestemming.GUHWAII.kans() == 8 && Bestemming.NOMGUH.kans() == 15 && Bestemming.BALKONIE.kans() == 30,
                "the chances on the rare souvenir: 5, 8, 15, 30 %");
        helper.assertTrue(Bestemming.OM_DE_HOEK.duurMs() == 5 * 60_000L && Bestemming.KAASMAAN.duurMs() == 24 * Klok.UUR, "durations in real time");
        helper.succeed();
    }

    // =====================================================================================================================
    // the questline and the proefreisje (with the real walk-off)
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH + "_proef", timeoutTicks = 200)
    public static void questlineEnProefreis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, Reizen.NIEUW);
        GuhNpcEntity agent = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(6, 2, 10));
        agent.setKind(GuhNpcEntity.Kind.REISBUREAU_AGENT);
        BlockPos balie = balie(helper, 6, 8);
        GuhEntity guh = guh(helper, p, "Proefje", 6, 6);
        UUID id = guh.getUUID();
        // 1: kennismaken
        helper.assertTrue(Reizen.boek(p, id, Bestemming.OM_DE_HOEK, balie) == Reizen.Uitkomst.EERST_AGENT, "no trip before the Reisagent");
        Reisagent.ROL.talk(agent, p);
        Reisagent.ROL.antwoord(agent, p, Reisagent.WAT);
        helper.assertTrue(Reizen.stap(p) == Reizen.NIEUW, "asking how it works is no step");
        Reisagent.ROL.antwoord(agent, p, Reisagent.JA);
        helper.assertTrue(Reizen.stap(p) == Reizen.KOFFER, "step 1 done: kennismaken");
        // 2: the koffertje
        Reisagent.ROL.talk(agent, p);
        helper.assertTrue(Reizen.stap(p) == Reizen.KOFFER, "without the three things nothing happens");
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), Reisagent.KNABBELS + 2));
        p.getInventory().add(new ItemStack(Items.LIGHT_BLUE_WOOL, 3));
        p.getInventory().add(new ItemStack(Items.PAPER, 2));
        Reisagent.ROL.talk(agent, p);
        helper.assertTrue(Reizen.stap(p) == Reizen.PROEF, "step 2 done: the koffertje is packed");
        helper.assertTrue(tel(p, ModItems.KAAS_KNABBELS.get()) == 2 && tel(p, Items.LIGHT_BLUE_WOOL) == 2 && tel(p, Items.PAPER) == 1,
                "exactly 4 kaasknabbels, 1 wool and 1 paper were taken");
        // 3: the proefreisje; the real trips are still closed
        helper.assertTrue(Reizen.boek(p, id, vandaag(helper, 0), balie) == Reizen.Uitkomst.EERST_AGENT, "real trips only after the proefreisje");
        CompoundTag stand = Reizen.stand(p, balie, null);
        helper.assertTrue(stand.getListOrEmpty("Aanbod").size() == 5 && stand.getListOrEmpty("Aanbod").getCompoundOrEmpty(0).getBooleanOr("Proef", false)
                && !stand.getListOrEmpty("Aanbod").getCompoundOrEmpty(1).getBooleanOr("Open", true), "the screen: the proefreisje first, the four real trips closed");
        helper.assertTrue(Reizen.boek(p, id, Bestemming.OM_DE_HOEK, balie) == Reizen.Uitkomst.OK, "the proefreisje is booked");
        helper.assertTrue(Uitzwaaien.vertrekt(guh) && GuhHooks.heeft(guh, PxVlaggen.KOFFER) && guh.isAlive(), "the guh walks off with its suitcase");
        CompoundTag reis = Reizen.reis(level.getServer(), p.getUUID());
        helper.assertTrue(reis != null && !Reizen.isWeg(reis), "the trip record exists at once, the guh is not stored yet");
        helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.NOG_NIET, "it cannot be collected while it walks off");
        helper.runAfterDelay(Uitzwaaien.DUUR + 10, () -> {
            CompoundTag r = Reizen.reis(level.getServer(), p.getUUID());
            helper.assertTrue(guh.isRemoved() && level.getEntity(id) == null && r != null && Reizen.isWeg(r), "after the walk the guh is stored");
            helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.NOG_NIET, "five minutes are not over");
            PxTest.spoel(4.0 / 60);
            helper.assertTrue(!Reizen.isTerug(r), "four minutes: not back");
            PxTest.spoel(1.1 / 60);
            helper.assertTrue(Reizen.isTerug(r), "five minutes: back");
            helper.assertTrue(Reizen.haalOp(p, Reizen.uitstapPlek(level, balie, p), false) == Reizen.Uitkomst.OK, "collected at the balie");
            Entity terug = level.getEntity(id);
            helper.assertTrue(terug instanceof GuhEntity g && g.isTame() && g.getName().getString().equals("Proefje") && !GuhHooks.heeft(g, PxVlaggen.KOFFER)
                    && g.getClothes(GuhClothes.Slot.EYES) == GuhClothes.SUNGLASSES, "the very same guh, with sunglasses, without the suitcase");
            helper.assertTrue(terug.blockPosition().distManhattan(balie) <= 2, "it steps out in front of the balie");
            helper.assertTrue(Reizen.stap(p) == Reizen.KLAAR && tel(p, ReisbureauSlice.STEMPEL.get()) == 1, "step 3 done: the Reisstempel");
            helper.assertTrue(tel(p, Bestemming.OM_DE_HOEK.kaart()) == 1 && Reizen.souvenirs(Reizen.data(p)) == 0, "a postcard, no souvenir from the proefreisje");
            helper.assertTrue(Reizen.reis(level.getServer(), p.getUUID()) == null, "the record is gone after the guh is back");
            // afterwards: a lost stamp comes back, a kept one does not double
            Reisagent.ROL.antwoord(agent, p, Reisagent.STEMPEL);
            helper.assertTrue(tel(p, ReisbureauSlice.STEMPEL.get()) == 1, "no second stamp while you have one");
            p.getInventory().clearContent();
            Reisagent.ROL.antwoord(agent, p, Reisagent.STEMPEL);
            helper.assertTrue(tel(p, ReisbureauSlice.STEMPEL.get()) == 1, "a new stamp when it is lost");
            helper.assertTrue(Reizen.boek(p, id, Bestemming.OM_DE_HOEK, balie) == Reizen.Uitkomst.EERST_AGENT, "the proefreisje is once");
            agent.discard();
            klaar(helper, p);
        });
    }

    // =====================================================================================================================
    // a whole trip in real time, one at a time, the rewards
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void reisInEchteTijd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer p = speler(helper, Reizen.KLAAR);
        Reizen.data(p).remove("Souvenirs");
        Reizen.data(p).remove("Stempels");
        Reizen.data(p).remove("Zeldzaam");
        Reizen.data(p).putInt("Reizen", 0);
        BlockPos balie = balie(helper, 6, 8);
        GuhEntity guh = guh(helper, p, "Vadsje", 6, 6);
        GuhEntity tweede = guh(helper, p, "Thuisblijver", 8, 6);
        UUID id = guh.getUUID();
        Bestemming b = vandaag(helper, 2);   // eight hours
        Bestemming niet = null;
        for (Bestemming x : Bestemming.metDuur(480)) {
            niet = x != b ? x : niet;
        }
        helper.assertTrue(Reizen.boek(p, id, niet, balie) == Reizen.Uitkomst.NIET_VANDAAG, "only today's trips can be booked");
        helper.assertTrue(Reizen.boek(p, UUID.randomUUID(), b, balie) == Reizen.Uitkomst.GEEN_GUH, "an unknown guh");
        helper.assertTrue(Reizen.boek(p, id, b, balie) == Reizen.Uitkomst.OK, "booked");
        helper.assertTrue(Reizen.boek(p, tweede.getUUID(), b, balie) == Reizen.Uitkomst.BEZIG, "ONE guh at a time");
        helper.assertTrue(Reizen.opslaan(guh), "the guh leaves");
        helper.assertTrue(guh.isRemoved() && level.getEntity(id) == null, "it left the world");
        helper.assertTrue(!Reizen.opslaan(tweede) && tweede.isAlive(), "a guh without a trip stays");
        Plek plek = GuhVolger.plek(server, p.getUUID(), id);
        helper.assertTrue(plek != null && plek.soort() == PlekSoort.OP_VAKANTIE, "'Mijn guhs' says on holiday: " + plek);
        helper.assertTrue(GuhVolger.tekst(plek).getString().length() > 0 && Roepen.roep(p, id) == Roepen.Uitkomst.NIET_THUIS, "it cannot be called while away");
        CompoundTag stand = Reizen.stand(p, balie, null);
        helper.assertTrue(stand.contains("Reis") && !stand.contains("Guhs") && stand.getCompoundOrEmpty("Reis").getBooleanOr("Weg", false)
                && stand.getCompoundOrEmpty("Reis").getLongOr("Rest", 0L) > 7 * Klok.UUR, "the screen shows who is away and for how long");
        // the stored record is plain data in the player's own saved data: the whole guh
        CompoundTag reis = Reizen.reis(server, p.getUUID());
        helper.assertTrue(reis != null && id.equals(GuhOpslag.id(reis.getCompoundOrEmpty("Opslag")))
                && !reis.getCompoundOrEmpty("Opslag").getCompoundOrEmpty("Data").isEmpty(), "the record holds the whole guh");
        helper.assertTrue(reis == PxData.deel(server, p.getUUID(), Reizen.DEEL).getCompoundOrEmpty("Reis"), "also readable while the player is offline");
        // real time: nothing ticks, only the clock counts
        helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.NOG_NIET, "not back yet");
        PxTest.spoel(7.9);
        helper.assertTrue(!Reizen.isTerug(reis) && Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.NOG_NIET, "7.9 hours: not back");
        PxTest.spoel(0.2);
        helper.assertTrue(Reizen.isTerug(reis), "8.1 hours: back");
        Reizen.verversPlek(server, p.getUUID(), reis);
        reis.putBoolean("Zeldzaam", false);
        helper.assertTrue(Reizen.haalOp(p, Reizen.uitstapPlek(level, balie, p), false) == Reizen.Uitkomst.OK, "collected");
        Entity terug = level.getEntity(id);
        helper.assertTrue(terug instanceof GuhEntity g && g.isTame() && p.getUUID().equals(g.getOwnerUUID()) && g.getName().getString().equals("Vadsje"),
                "the very same guh is back");
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(3), x -> x.getUUID().equals(id)).size() == 1, "exactly one");
        helper.assertTrue(Reizen.reis(server, p.getUUID()) == null && Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.NIEMAND, "the record is gone");
        ItemStack kaart = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            kaart = p.getInventory().getItem(i).is(b.kaart()) ? p.getInventory().getItem(i) : kaart;
        }
        helper.assertTrue(!kaart.isEmpty() && KaartItem.afzender(kaart).getString().equals("Vadsje"), "the ansichtkaart is signed with the guh's name");
        helper.assertTrue(((KaartItem) kaart.getItem()).leesData(kaart).getStringOr("Bestemming", "").equals(b.id()), "and it knows where it came from");
        helper.assertTrue(tel(p, b.souvenir().asItem()) == 1 && tel(p, b.zeldzaam().asItem()) == 0, "the common souvenir");
        CompoundTag d = Reizen.data(p);
        helper.assertTrue(d.getIntOr("Reizen", 0) == 1 && Reizen.souvenirs(d) == 1 && d.getIntOr("Stempels", 0) == 0, "the reispas: one trip, one souvenir");
        // again (another day by now, so today's trip of that day): a souvenir the player already has becomes a stamp
        CompoundTag heb = PxData.sub(d, "Souvenirs");
        Bestemming b2 = vandaag(helper, 2);
        helper.assertTrue(Reizen.boek(p, id, b2, balie) == Reizen.Uitkomst.OK && Reizen.opslaan((GuhEntity) terug), "off again");
        Reizen.reis(server, p.getUUID()).putBoolean("Zeldzaam", false);
        heb.putInt("souvenir_" + b2.id(), 1);
        int had = tel(p, b2.souvenir().asItem()), album = Reizen.souvenirs(d);
        PxTest.spoel(8.1);
        helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.OK, "collected next to the player");
        helper.assertTrue(tel(p, b2.souvenir().asItem()) == had && d.getIntOr("Stempels", 0) == 1 && Reizen.souvenirs(d) == album,
                "a duplicate is a stamp on the reispas, not a second item");
        // with luck: the rare souvenir instead of the common one
        Bestemming b3 = vandaag(helper, 2);
        heb.remove("zeldzaam_" + b3.id());
        album = Reizen.souvenirs(d);
        had = tel(p, b3.souvenir().asItem());
        helper.assertTrue(Reizen.boek(p, id, b3, balie) == Reizen.Uitkomst.OK && Reizen.opslaan((GuhEntity) level.getEntity(id)), "and again");
        Reizen.reis(server, p.getUUID()).putBoolean("Zeldzaam", true);
        PxTest.spoel(8.1);
        helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.OK, "collected");
        helper.assertTrue(tel(p, b3.zeldzaam().asItem()) == 1 && tel(p, b3.souvenir().asItem()) == had && Reizen.souvenirs(d) == album + 1
                && d.getIntOr("Zeldzaam", 0) == 1 && d.getIntOr("Reizen", 0) == 3, "with luck: the rare souvenir instead");
        // nine stamps + one more = a Gouden koffertje and an empty reispas
        Bestemming b4 = vandaag(helper, 2);
        heb.putInt("souvenir_" + b4.id(), 1);
        d.putInt("Stempels", Reizen.STEMPELS_VOL - 1);
        d.putInt("Koffertjes", 0);
        helper.assertTrue(Reizen.boek(p, id, b4, balie) == Reizen.Uitkomst.OK && Reizen.opslaan((GuhEntity) level.getEntity(id)), "the tenth duplicate");
        Reizen.reis(server, p.getUUID()).putBoolean("Zeldzaam", false);
        PxTest.spoel(8.1);
        helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.OK, "collected");
        helper.assertTrue(tel(p, ReisbureauSlice.blok("gouden_koffertje").asItem()) == 1 && d.getIntOr("Stempels", 0) == 0 && d.getIntOr("Koffertjes", 0) == 1,
                "ten stamps: the Gouden koffertje");
        d.remove("Souvenirs");
        klaar(helper, p);
    }

    // =====================================================================================================================
    // never lost
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH + "_kwijt", timeoutTicks = 200)
    public static void nooitKwijt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer p = speler(helper, Reizen.KLAAR);
        BlockPos balie = balie(helper, 6, 8);
        GuhEntity guh = guh(helper, p, "Koffertje", 6, 6);
        UUID id = guh.getUUID();
        Bestemming b = vandaag(helper, 3);   // 24 hours
        int reizen = Reizen.data(p).getIntOr("Reizen", 0);
        // (a) the balie is broken while the guh is away: any other balie, or none at all, gives it back
        helper.assertTrue(Reizen.boek(p, id, b, balie) == Reizen.Uitkomst.OK && Reizen.opslaan(guh), "away for 24 hours");
        level.setBlockAndUpdate(balie, Blocks.AIR.defaultBlockState());
        helper.assertTrue(Reizen.reis(server, p.getUUID()) != null, "breaking the balie changes nothing: the trip is the player's own data");
        // (b) calling back early: the guh is back at once, without postcard and souvenir
        PxTest.spoel(3);
        helper.assertTrue(Reizen.haalOp(p, Reizen.uitstapPlek(level, balie, p), false) == Reizen.Uitkomst.NOG_NIET, "not back yet");
        helper.assertTrue(Reizen.haalOp(p, Reizen.uitstapPlek(level, balie, p), true) == Reizen.Uitkomst.OK, "called back early, without a balie");
        GuhEntity terug = (GuhEntity) level.getEntity(id);
        helper.assertTrue(terug != null && terug.isAlive() && terug.getName().getString().equals("Koffertje") && Reizen.reis(server, p.getUUID()) == null,
                "the guh is home");
        helper.assertTrue(Reizen.data(p).getIntOr("Reizen", 0) == reizen && tel(p, b.kaart()) == 0 && terug.getClothes(GuhClothes.Slot.EYES) == null,
                "an early return brings nothing (and costs nothing)");
        // (c) stored AND in the world (a crash between two saves): the world wins, the trip is dropped, nothing doubles
        BlockPos balie2 = balie(helper, 6, 8);
        helper.assertTrue(Reizen.boek(p, id, vandaag(helper, 3), balie2) == Reizen.Uitkomst.OK && Reizen.opslaan(terug), "away again");
        CompoundTag opslag = Reizen.reis(server, p.getUUID()).getCompoundOrEmpty("Opslag").copy();
        Entity dubbel = GuhOpslag.laatVrij(level, opslag, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4))), 0f);
        helper.assertTrue(dubbel != null, "(the stale copy of the crash)");
        PxTest.spoel(25);
        helper.assertTrue(Reizen.haalOp(p, p.position(), false) == Reizen.Uitkomst.NIEMAND && Reizen.reis(server, p.getUUID()) == null,
                "the trip is dropped: the guh in the world is the guh");
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(3), x -> x.getUUID().equals(id)).size() == 1, "never two of the same guh");
        // (d) the trip is dropped while the guh still walks off: it puts its suitcase down and stays
        GuhEntity loopt = (GuhEntity) dubbel;
        helper.assertTrue(Reizen.boek(p, id, vandaag(helper, 3), balie2) == Reizen.Uitkomst.OK && Uitzwaaien.vertrekt(loopt), "walking off");
        Reizen.data(p).remove("Reis");
        helper.runAfterDelay(Uitzwaaien.DUUR + 10, () -> {
            helper.assertTrue(loopt.isAlive() && !Uitzwaaien.vertrekt(loopt) && !GuhHooks.heeft(loopt, PxVlaggen.KOFFER)
                    && nl.juiced.guhs.feature.guhpixel.GuhKiezer.bezet(loopt) == null, "no trip any more: the guh stays home, free, without the suitcase");
            // (e) booked, but the guh disappeared before it was stored (unloaded): after a while the trip is dropped, the player can book again
            helper.assertTrue(Reizen.boek(p, id, vandaag(helper, 3), balie2) == Reizen.Uitkomst.OK, "booked");
            loopt.discard();   // (as if its chunk unloaded)
            helper.assertTrue(Reizen.controleer(server, p.getUUID()) == null && Reizen.reis(server, p.getUUID()) != null, "the first seconds the record waits");
            Klok.spoel(Reizen.VERTREK_MAX_MS + 1000);
            helper.assertTrue(Reizen.controleer(server, p.getUUID()) != null && Reizen.reis(server, p.getUUID()) == null,
                    "a trip whose guh never left is dropped (the guh is wherever it was: not stored, so not lost)");
            // (f) a guh that walks off but is stored late by the tidy-up: also fine
            GuhEntity nieuw = guh(helper, p, "Laatkomer", 8, 6);
            helper.assertTrue(Reizen.boek(p, nieuw.getUUID(), vandaag(helper, 3), balie2) == Reizen.Uitkomst.OK, "booked");
            nieuw.getPersistentData().remove(Uitzwaaien.TICKS);   // (its countdown got lost)
            Klok.spoel(Reizen.VERTREK_MAX_MS + 1000);
            helper.assertTrue(Reizen.controleer(server, p.getUUID()) == null && nieuw.isRemoved() && Reizen.isWeg(Reizen.reis(server, p.getUUID())),
                    "the tidy-up stores a guh that is still around");
            helper.assertTrue(Reizen.haalOp(p, p.position(), true) == Reizen.Uitkomst.OK && level.getEntity(nieuw.getUUID()) != null, "and it comes back");
            klaar(helper, p);
        });
    }

    // =====================================================================================================================
    // two players at once
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void tweeSpelersTegelijk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer a = speler(helper, Reizen.KLAAR), b = speler(helper, Reizen.KLAAR);
        BlockPos balie = balie(helper, 6, 8);
        GuhEntity ga = guh(helper, a, "Van A", 5, 6), gb = guh(helper, b, "Van B", 7, 6);
        Bestemming kort = vandaag(helper, 0), lang = vandaag(helper, 1);
        helper.assertTrue(Reizen.boek(a, gb.getUUID(), kort, balie) == Reizen.Uitkomst.GEEN_GUH, "you cannot send somebody else's guh");
        helper.assertTrue(Reizen.boek(a, ga.getUUID(), kort, balie) == Reizen.Uitkomst.OK && Reizen.boek(b, gb.getUUID(), lang, balie) == Reizen.Uitkomst.OK,
                "both players book at the same balie, even the same day's trips");
        helper.assertTrue(Reizen.opslaan(ga) && Reizen.opslaan(gb), "both guhs leave");
        PxTest.spoel(1.05);
        helper.assertTrue(Reizen.haalOp(b, b.position(), false) == Reizen.Uitkomst.NOG_NIET, "B's two-hour trip is not over");
        helper.assertTrue(Reizen.haalOp(a, a.position(), false) == Reizen.Uitkomst.OK, "A's one-hour trip is");
        helper.assertTrue(level.getEntity(ga.getUUID()) != null && level.getEntity(gb.getUUID()) == null, "A's guh is back, B's is not");
        helper.assertTrue(Reizen.reis(server, a.getUUID()) == null && Reizen.reis(server, b.getUUID()) != null, "each their own record");
        PxTest.spoel(1);
        helper.assertTrue(Reizen.haalOp(b, b.position(), false) == Reizen.Uitkomst.OK && level.getEntity(gb.getUUID()) instanceof GuhEntity g
                && b.getUUID().equals(g.getOwnerUUID()), "B's guh comes back to B");
        helper.assertTrue(tel(a, kort.kaart()) == 1 && tel(b, lang.kaart()) == 1 && tel(a, lang.kaart()) == 0, "each their own postcard");
        klaar(helper, a, b);
    }

    // =====================================================================================================================
    // the screen and what the client may ask
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void schermEnServerControle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer p = speler(helper, Reizen.KLAAR);
        BlockPos balie = balie(helper, 6, 8);
        GuhEntity dichtbij = guh(helper, p, "Dichtbij", 6, 6);
        GuhEntity baby = guh(helper, p, "Kleintje", 8, 6);
        baby.setBaby(true);
        CompoundTag stand = Reizen.stand(p, balie, null);
        ListTag aanbod = stand.getListOrEmpty("Aanbod");
        helper.assertTrue(aanbod.size() == 4 && stand.getIntOr("Stap", 0) == Reizen.KLAAR && stand.getLongOr("Morgen", 0L) > 0, "four trips, open");
        for (int i = 0; i < 4; i++) {
            CompoundTag t = aanbod.getCompoundOrEmpty(i);
            Bestemming b = Bestemming.vanId(t.getStringOr("Id", ""));
            helper.assertTrue(b != null && t.getBooleanOr("Open", false) && t.getIntOr("Minuten", 0) == Bestemming.DUREN[i] && t.getIntOr("Kans", 0) == b.kans()
                    && t.getStringOr("Souvenir", "").equals("guhs:reisbureau_souvenir_" + b.id())
                    && t.getStringOr("Zeldzaam", "").equals("guhs:reisbureau_zeldzaam_" + b.id()), "trip " + i + ": duration, chance, both souvenirs: " + t);
        }
        boolean zag = false, zagBaby = false;
        for (Tag raw : stand.getListOrEmpty("Guhs")) {
            CompoundTag t = (CompoundTag) raw;
            UUID id = GuhOpslag.id(t);
            if (dichtbij.getUUID().equals(id)) {
                zag = Tekst.empty(Tekst.get(t, "Uit"));
            }
            if (baby.getUUID().equals(id)) {
                zagBaby = !Tekst.empty(Tekst.get(t, "Uit"));
            }
        }
        helper.assertTrue(zag && zagBaby, "the picker: a guh nearby can go, a baby cannot (with the reason)");
        Bestemming b = vandaag(helper, 0);
        // what the client sends is checked: no balie there, too far away, nonsense
        Reizen.opActie(p, balie.above(3), ReisbureauPayloads.BOEK, b.id(), dichtbij.getUUID().toString());
        helper.assertTrue(Reizen.reis(server, p.getUUID()) == null, "no balie at that spot: nothing happens");
        Vec3 was = p.position();
        p.snapTo(was.x + 30, was.y, was.z);
        Reizen.opActie(p, balie, ReisbureauPayloads.BOEK, b.id(), dichtbij.getUUID().toString());
        helper.assertTrue(Reizen.reis(server, p.getUUID()) == null, "too far from the balie: nothing happens");
        p.snapTo(was.x, was.y, was.z);
        Reizen.opActie(p, balie, ReisbureauPayloads.BOEK, "geen_bestemming", dichtbij.getUUID().toString());
        Reizen.opActie(p, balie, ReisbureauPayloads.BOEK, b.id(), "geen-uuid");
        Reizen.opActie(p, balie, ReisbureauPayloads.BOEK, Bestemming.OM_DE_HOEK.id(), dichtbij.getUUID().toString());
        Reizen.opActie(p, balie, ReisbureauPayloads.BOEK, b.id(), baby.getUUID().toString());
        Reizen.opActie(p, balie, 99, b.id(), dichtbij.getUUID().toString());
        helper.assertTrue(Reizen.reis(server, p.getUUID()) == null, "nonsense, the proefreisje after the questline, a baby: nothing is booked");
        Reizen.opActie(p, balie, ReisbureauPayloads.OPHALEN, "", "");
        Reizen.opActie(p, balie, ReisbureauPayloads.BOEK, b.id(), dichtbij.getUUID().toString());
        helper.assertTrue(Reizen.reis(server, p.getUUID()) != null && Uitzwaaien.vertrekt(dichtbij), "a real booking through the balie");
        helper.assertTrue(Reizen.opslaan(dichtbij), "stored");
        Reizen.opActie(p, balie, ReisbureauPayloads.OPHALEN, "", "");
        helper.assertTrue(Reizen.reis(server, p.getUUID()) != null && level.getEntity(dichtbij.getUUID()) == null, "collecting before the time: refused");
        PxTest.spoel(1.01);
        Reizen.opActie(p, balie, ReisbureauPayloads.OPHALEN, "", "");
        helper.assertTrue(Reizen.reis(server, p.getUUID()) == null && level.getEntity(dichtbij.getUUID()) != null, "collecting after the time: the guh is back");
        // the structure's own counter cannot be broken, a home-made one can
        BlockState vast = ReisbureauSlice.BALIE.get().defaultBlockState().setValue(BalieBlock.VAST, true);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(vast.getDestroyProgress(p, level, balie) == 0f && ReisbureauSlice.BALIE.get().defaultBlockState().getDestroyProgress(p, level, balie) > 0f,
                "vast=true cannot be broken in survival");
        klaar(helper, p);
    }

    // =====================================================================================================================
    // the sunglasses
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void zonnebrilVoorEven(GameTestHelper helper) {
        ServerPlayer p = speler(helper, Reizen.KLAAR);
        GuhEntity guh = guh(helper, p, "Bril", 6, 6);
        GuhClothes eigen = null;
        for (GuhClothes c : GuhClothes.values()) {
            if (c.slot == GuhClothes.Slot.EYES && c != GuhClothes.SUNGLASSES) {
                eigen = c;
                break;
            }
        }
        if (eigen != null) {
            guh.wear(eigen);
        }
        Zonnebril.zetOp(guh);
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.EYES) == GuhClothes.SUNGLASSES && Zonnebril.draagt(guh), "sunglasses on");
        PxTest.spoel((Zonnebril.MINUTEN - 1) / 60.0);
        Zonnebril.zetOp(guh);   // (back from another trip meanwhile: the old piece is still remembered)
        PxTest.spoel((Zonnebril.MINUTEN + 1) / 60.0);
        Zonnebril.zetAf(guh);
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.EYES) == eigen && !Zonnebril.draagt(guh), "after a while: what it wore before is back: " + eigen);
        // the player dressed it differently meanwhile: that choice stays
        Zonnebril.zetOp(guh);
        guh.takeOff(GuhClothes.Slot.EYES);
        Zonnebril.zetAf(guh);
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.EYES) == null, "the player's own choice is not overwritten");
        // a guh with its own sunglasses keeps them
        guh.wear(GuhClothes.SUNGLASSES);
        Zonnebril.zetOp(guh);
        helper.assertTrue(!Zonnebril.draagt(guh) && guh.getClothes(GuhClothes.Slot.EYES) == GuhClothes.SUNGLASSES, "its own sunglasses are its own");
        klaar(helper, p);
    }

    // =====================================================================================================================
    // the Guhdex section, the tables, the template, the texts
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void gidsSectieEnAlbum(GameTestHelper helper) {
        ServerPlayer p = speler(helper, Reizen.NIEUW);
        nl.juiced.guhs.feature.guhpixel.Toegang.vergrendel(p);   // (the Reisbureau shows without the Guhpixel unlock)
        Reizen.data(p).remove("Souvenirs");
        PxData.sub(Reizen.data(p), "Souvenirs").putInt("souvenir_lingsesdijk", 1);
        int plaatjes = 0, behaald = 0;
        for (Tag raw : GidsBlad.stand(p).getListOrEmpty("Rijen")) {
            CompoundTag r = (CompoundTag) raw;
            if (GidsBlad.PLAATJE.equals(r.getStringOr("T", ""))) {
                plaatjes++;
                behaald += r.toString().contains("reisbureau_souvenir_lingsesdijk") ? 1 : 0;
            }
        }
        helper.assertTrue(plaatjes == 48, "the album: 16 souvenirs + 16 rare ones + 16 ansichtkaarten, got " + plaatjes);
        helper.assertTrue(behaald == 1, "the one souvenir the player has is in it");
        Reizen.data(p).remove("Souvenirs");
        klaar(helper, p);
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void souvenirsKloppenMetDeModellen(GameTestHelper helper) throws Exception {
        JsonObject vormen;
        try (var reader = helper.getLevel().getServer().getResourceManager().getResource(Guhs.id("reisbureau/vormen.json")).orElseThrow().openAsReader()) {
            vormen = JsonParser.parseReader(reader).getAsJsonObject();
        }
        helper.assertTrue(vormen.size() == Souvenirs.ALLE.size() && Souvenirs.ALLE.size() == 34, "34 blocks in both tables");
        int gewoon = 0, zeldzaam = 0;
        for (Souvenirs.Soort s : Souvenirs.ALLE) {
            JsonObject j = vormen.getAsJsonObject("reisbureau_" + s.id());
            helper.assertTrue(j != null, s.id() + " is in vormen.json");
            JsonArray v = j.getAsJsonArray("vorm");
            double[] java = {s.x0(), s.y0(), s.z0(), s.x1(), s.y1(), s.z1()};
            for (int i = 0; i < 6; i++) {
                helper.assertTrue(Math.abs(v.get(i).getAsDouble() - java[i]) < 0.01, s.id() + ": shape " + v + " differs from Souvenirs.java");
            }
            helper.assertTrue(j.get("muur").getAsBoolean() == s.muur() && j.get("licht").getAsInt() == s.licht(), s.id() + ": wall / light");
            String effect = j.get("effect").getAsString();
            helper.assertTrue((effect.isEmpty() ? "geen" : effect).equalsIgnoreCase(s.effect().name()), s.id() + ": effect " + effect);
            Block blok = ReisbureauSlice.blok(s.id());
            helper.assertTrue((blok instanceof MuurDecoBlock) == s.muur() && blok.asItem() != Items.AIR, s.id() + ": the right block class and an item");
            AABB doos = blok.defaultBlockState().getShape(helper.getLevel(), BlockPos.ZERO).bounds();
            helper.assertTrue(Math.abs(doos.minX * 16 - s.x0()) < 0.01 && Math.abs(doos.maxY * 16 - s.y1()) < 0.01, s.id() + ": the block's shape");
            helper.assertTrue(blok.defaultBlockState().getLightEmission() == s.licht(), s.id() + ": light");
            gewoon += s.id().startsWith("souvenir_") ? 1 : 0;
            zeldzaam += s.zeldzaam() ? 1 : 0;
        }
        helper.assertTrue(gewoon == 16 && zeldzaam == 16, "16 common and 16 rare souvenirs");
        for (Bestemming b : Bestemming.ECHT) {
            helper.assertTrue(b.souvenir() != null && b.zeldzaam() != null && b.souvenir() != b.zeldzaam() && b.kaart() instanceof KaartItem k && k.bestemming() == b,
                    b + ": a souvenir, a rare one and its own ansichtkaart");
        }
        helper.assertTrue(Bestemming.OM_DE_HOEK.souvenir() == null && Bestemming.OM_DE_HOEK.kaart() instanceof KaartItem, "the proefreisje: only a postcard");
        // every text of every destination exists, and the inside joke is spelled exactly
        JsonObject nl;
        try (var in = Guhs.class.getResourceAsStream("/assets/guhs/lang/nl_nl.json")) {
            nl = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> mist = new HashSet<>();
        for (Bestemming b : Bestemming.values()) {
            for (String k : List.of("gui.guhs.reisbureau.bestemming." + b.id(), "gui.guhs.reisbureau.bestemming." + b.id() + ".plek",
                    "gui.guhs.reisbureau.bestemming." + b.id() + ".uitleg", "book.guhs.reisbureau.kaart." + b.id(), "item.guhs.reisbureau_kaart_" + b.id())) {
                if (!nl.has(k)) {
                    mist.add(k);
                }
            }
        }
        for (Reizen.Uitkomst u : Reizen.Uitkomst.values()) {
            if (u != Reizen.Uitkomst.OK && u != Reizen.Uitkomst.GUH_BEZET && !nl.has("gui.guhs.reisbureau.melding." + u.name().toLowerCase(java.util.Locale.ROOT))) {
                mist.add(u.name());
            }
        }
        for (String k : List.of("gui.guhs.reisbureau.gids.stap.0", "gui.guhs.reisbureau.gids.stap.3", "gui.guhs.reisbureau.slot.0", "gui.guhs.reisbureau.slot.2",
                "quest.guhs.reisbureau.praatje.0", "quest.guhs.reisbureau.praatje.3", "quest.guhs.reisbureau.klaar", "gui.guhs.reisbureau.kaart.je_guh",
                "gui.guhs.reisbureau.gids.nu.onderweg", "gui.guhs.reisbureau.melding.guh_bezet", "gui.guhs.reisbureau.tijd.um")) {
            if (!nl.has(k)) {
                mist.add(k);
            }
        }
        helper.assertTrue(mist.isEmpty(), "missing texts: " + mist);
        helper.assertTrue(nl.get("gui.guhs.reisbureau.bestemming.lingsesdijk").getAsString().equals("Vadsen bij huize Lingsesdijk 86"),
                "the first destination is spelled exactly 'Vadsen bij huize Lingsesdijk 86'");
        helper.assertTrue(nl.get("block.guhs.reisbureau_souvenir_lingsesdijk").getAsString().contains("\"Huize Lingsesdijk 86\""), "its picture: Huize Lingsesdijk 86");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void templateVanHetReisbureau(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("reisbureau")).orElse(null);
        helper.assertTrue(t != null && t.getSize().getX() == 17 && t.getSize().getZ() == 17, "the template exists, 17 x 17");
        CompoundTag tag = t.save(new CompoundTag());
        ListTag palette = tag.getListOrEmpty("palette");
        int balies = 0, koffertjes = 0, jigsaw = 0;
        for (Tag raw : tag.getListOrEmpty("blocks")) {
            CompoundTag staat = palette.getCompoundOrEmpty(((CompoundTag) raw).getIntOr("state", 0));
            String naam = staat.getStringOr("Name", "");
            if (naam.equals("guhs:reisbureau_balie")) {
                helper.assertTrue(staat.getCompoundOrEmpty("Properties").getStringOr("vast", "").equals("true"), "the structure's counter is vast=true");
                balies++;
            }
            koffertjes += naam.equals("guhs:reisbureau_koffertje") ? 1 : 0;
            jigsaw += naam.equals("minecraft:jigsaw") ? 1 : 0;
            helper.assertTrue(!naam.contains("reisbureau_souvenir") && !naam.contains("reisbureau_zeldzaam") && !naam.contains("gouden_koffertje"),
                    "no souvenir stands in the structure (they only come from trips)");
        }
        helper.assertTrue(balies == 3 && jigsaw == 1 && koffertjes >= 2, "three counters, the anchor, some suitcases");
        int agenten = 0;
        for (Tag raw : tag.getListOrEmpty("entities")) {
            CompoundTag nbt = ((CompoundTag) raw).getCompoundOrEmpty("nbt");
            agenten += nbt.getStringOr("Kind", "").equals("reisbureau_agent") ? 1 : 0;
        }
        helper.assertTrue(agenten == 1, "exactly one Reisagent-guh");
        helper.succeed();
    }

    private PxReisbureauGameTests() {
    }
}
