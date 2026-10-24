package nl.juiced.guhs.feature.grotspellen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * The cave games (1.7.0 Onder de Wol): the treasure field and Snuffie's nose, the lights puzzle (always solvable, the
 * fewest presses), the coins per level (a good game ~30-40 on medium, ~50-60 on lastig, +5 for a record), the shops, the
 * borrowed tools, the protection, and a whole game in each real hall (scoring, the payout, the hall reset afterwards).
 */
public class GrotSpellenGameTests {
    private static final String EMPTY = "empty";
    private static final TagKey<Item> GRIJPTICKETS = ItemTags.create(Guhs.id("knus/grijptickets"));
    private static final TagKey<Item> LOANED = ItemTags.create(Guhs.id("loaned"));

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer p) {
        GrotSpel.verlaat(p);
        Minigames.forget(p);
        helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    private static int count(Player p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static GuhNpcEntity host(GameTestHelper helper, GuhNpcEntity.Kind kind) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(), n -> n.getKind() == kind);
        helper.assertTrue(npcs.size() == 1, "the host is in the hall: " + kind + " " + npcs.size());
        return npcs.get(0);
    }

    // --- pure logic -------------------------------------------------------------------------------------------------
    /** The field: sizes per level, digging, Snuffie's nose, points and schatmuntjes. */
    @GuhTest(template = EMPTY)
    public static void grotSchatVeld(GameTestHelper helper) {
        helper.assertTrue(SchatVeld.actief(Niveau.MAKKELIJK) == 7 && SchatVeld.actief(Niveau.MEDIUM) == 11 && SchatVeld.actief(Niveau.LASTIG) == 15,
                "a bigger field per level");
        helper.assertTrue(SchatVeld.hintTicks(Niveau.MAKKELIJK) < SchatVeld.hintTicks(Niveau.LASTIG)
                && SchatVeld.warmStraal(Niveau.MAKKELIJK) > SchatVeld.warmStraal(Niveau.LASTIG), "fewer hints on the harder levels");
        for (Niveau n : Niveau.values()) {
            for (int seed = 0; seed < 20; seed++) {
                SchatVeld v = new SchatVeld(n, RandomSource.create(seed));
                int schat = 0, goud = 0, piep = 0;
                for (int x = 0; x < SchatVeld.GROOTTE; x++) {
                    for (int z = 0; z < SchatVeld.GROOTTE; z++) {
                        SchatVeld.Inhoud i = v.inhoud(x, z);
                        helper.assertTrue(i == SchatVeld.Inhoud.LEEG || v.actief(x, z), "only under the played part");
                        schat += i == SchatVeld.Inhoud.SCHAT ? 1 : 0;
                        goud += i == SchatVeld.Inhoud.GOUD ? 1 : 0;
                        piep += i == SchatVeld.Inhoud.PIEP ? 1 : 0;
                    }
                }
                helper.assertTrue(schat == SchatVeld.schatten(n) && schat >= 6 && schat <= 10 && goud == 1 && piep == SchatVeld.piepkazen(n),
                        n + ": " + schat + " treasures, " + goud + " gold, " + piep + " piep");
            }
        }
        SchatVeld v = new SchatVeld(Niveau.MEDIUM, new int[][]{{7, 7}, {10, 3}}, new int[]{4, 11}, new int[][]{{5, 5}});
        helper.assertTrue(!v.actief(1, 1) && v.graaf(1, 1) == null, "outside the medium part: not dug");
        helper.assertTrue(v.hint(7.5, 7.5) == SchatVeld.Hint.HEET && v.hint(8.6, 8.4) == SchatVeld.Hint.HEET, "right on it / next to it: HEET");
        helper.assertTrue(v.hint(9.5, 7.5) == SchatVeld.Hint.WARM, "two away: warm");
        helper.assertTrue(v.hint(12.5, 12.5) == SchatVeld.Hint.KOUD, "far: koud");
        helper.assertTrue(v.graaf(6, 6) == SchatVeld.Inhoud.LEEG && v.graaf(6, 6) == null, "a cell is dug once");
        helper.assertTrue(v.graaf(5, 5) == SchatVeld.Inhoud.PIEP && v.piepen() == 1, "a piepkaas");
        helper.assertTrue(v.graaf(7, 7) == SchatVeld.Inhoud.SCHAT && v.gevonden() == 1 && v.over() == 1 && !v.alles(), "one of two");
        helper.assertTrue(v.hint(7.5, 7.5) != SchatVeld.Hint.HEET, "a dug treasure doesn't smell");
        helper.assertTrue(v.punten(600) == 10, "10 per treasure, no time bonus before everything is found: " + v.punten(600));
        helper.assertTrue(v.graaf(4, 11) == SchatVeld.Inhoud.GOUD && v.goud() && v.punten(600) == 35, "+25 for the golden cheese");
        helper.assertTrue(v.graaf(10, 3) == SchatVeld.Inhoud.SCHAT && v.alles() && v.punten(600) == 45 + 15, "all found: +1 per 2 s left");
        // the coins: a good medium game ~30-40, lastig ~50-60, the caps, +5 for a record
        helper.assertTrue(SchatVeld.munten(Niveau.MEDIUM, 85, false) >= 30 && SchatVeld.munten(Niveau.MEDIUM, 105, false) <= 40,
                "medium: " + SchatVeld.munten(Niveau.MEDIUM, 85, false) + "-" + SchatVeld.munten(Niveau.MEDIUM, 105, false));
        helper.assertTrue(SchatVeld.munten(Niveau.LASTIG, 95, false) >= 50 && SchatVeld.munten(Niveau.LASTIG, 105, false) <= 60,
                "lastig: " + SchatVeld.munten(Niveau.LASTIG, 95, false) + "-" + SchatVeld.munten(Niveau.LASTIG, 105, false));
        helper.assertTrue(SchatVeld.munten(Niveau.MAKKELIJK, 1000, false) == 25 && SchatVeld.munten(Niveau.MEDIUM, 1000, false) == 45
                && SchatVeld.munten(Niveau.LASTIG, 1000, false) == 70, "capped per level");
        helper.assertTrue(SchatVeld.munten(Niveau.MEDIUM, 85, true) == SchatVeld.munten(Niveau.MEDIUM, 85, false) + 5, "+5 for a record");
        helper.succeed();
    }

    /** The lights puzzle: a press switches a plus, every new puzzle can be solved, the solver finds the fewest presses. */
    @GuhTest(template = EMPTY)
    public static void grotLichtjesRaster(GameTestHelper helper) {
        LichtjesRaster r = new LichtjesRaster();
        helper.assertTrue(r.allesAan() && r.aantalAan() == 49 && r.minimaal() == 0, "all on: solved");
        r.druk(3, 3);
        helper.assertTrue(!r.aan(3, 3) && !r.aan(2, 3) && !r.aan(4, 3) && !r.aan(3, 2) && !r.aan(3, 4) && r.aan(2, 2) && r.aantalAan() == 44,
                "a press: the lamp and its four neighbours");
        r.druk(0, 0);
        helper.assertTrue(r.aantalAan() == 41 && !r.druk(7, 0), "a corner has two neighbours; outside nothing");
        r.druk(0, 0);
        r.druk(3, 3);
        helper.assertTrue(r.allesAan(), "twice is undone");
        for (Niveau n : Niveau.values()) {
            for (int seed = 0; seed < 40; seed++) {
                LichtjesRaster p = LichtjesRaster.puzzel(RandomSource.create(seed * 31L + n.ordinal()), LichtjesRaster.drukken(n));
                int[] o = p.oplossing();
                helper.assertTrue(!p.allesAan() && o != null, "a real, solvable puzzle");
                helper.assertTrue(o.length == LichtjesRaster.drukken(n), n + ": the fewest presses = the scramble (7x7 has one solution): " + o.length);
                LichtjesRaster k = p.kopie();
                for (int i : o) {
                    k.druk(i / LichtjesRaster.N, i % LichtjesRaster.N);
                }
                helper.assertTrue(k.allesAan(), "pressing the solution solves it");
            }
        }
        helper.assertTrue(LichtjesRaster.speelTicks(Niveau.LASTIG) < LichtjesRaster.speelTicks(Niveau.MAKKELIJK), "less time on lastig");
        // points and lampjesmunten
        helper.assertTrue(LichtjesRaster.punten(true, 600, 7, 7) == 100 + 30 + 50 && LichtjesRaster.punten(true, 600, 9, 7) == 100 + 30 + 30
                && LichtjesRaster.punten(false, 600, 3, 7) == 0, "fewer presses and more time = more points");
        int medium = LichtjesRaster.munten(Niveau.MEDIUM, true, 600, 2000, 8, 7, 49, false);
        int lastig = LichtjesRaster.munten(Niveau.LASTIG, true, 800, 1600, 11, 10, 49, false);
        helper.assertTrue(medium >= 30 && medium <= 40, "a good medium game: " + medium);
        helper.assertTrue(lastig >= 50 && lastig <= 60, "a good lastig game: " + lastig);
        helper.assertTrue(LichtjesRaster.munten(Niveau.MEDIUM, false, 0, 2000, 30, 7, 40, false) == 4, "not solved: a few for the lamps on");
        helper.assertTrue(LichtjesRaster.munten(Niveau.MEDIUM, true, 600, 2000, 8, 7, 49, true) == medium + 5, "+5 for a record");
        helper.succeed();
    }

    /** Glimwormvangen's numbers: more and quicker glimworms on the harder levels, the light, the coins. */
    @GuhTest(template = EMPTY)
    public static void grotGlimwormGetallen(GameTestHelper helper) {
        helper.assertTrue(Glimwormvangen.aantal(Niveau.MAKKELIJK) < Glimwormvangen.aantal(Niveau.LASTIG)
                && Glimwormvangen.snelheid(Niveau.MAKKELIJK) < Glimwormvangen.snelheid(Niveau.LASTIG)
                && Glimwormvangen.snelle(Niveau.MAKKELIJK) < Glimwormvangen.snelle(Niveau.LASTIG), "more and quicker on lastig");
        helper.assertTrue(Glimwormvangen.startLicht(Niveau.MAKKELIJK) > Glimwormvangen.startLicht(Niveau.LASTIG), "less light on lastig");
        helper.assertTrue(Glimwormvangen.lichtStand(Niveau.MEDIUM, 0) == 1 && Glimwormvangen.lichtStand(Niveau.MEDIUM, 4) == 2
                && Glimwormvangen.lichtStand(Niveau.MEDIUM, 100) == 4, "brighter with every 4 caught, at most 4");
        int medium = Glimwormvangen.munten(Niveau.MEDIUM, 20, false), lastig = Glimwormvangen.munten(Niveau.LASTIG, 24, false);
        helper.assertTrue(medium >= 30 && medium <= 40 && lastig >= 50 && lastig <= 60, "good games: " + medium + " / " + lastig);
        helper.assertTrue(Glimwormvangen.munten(Niveau.LASTIG, 500, true) == 75, "capped, +5 for a record");
        helper.succeed();
    }

    // --- the hosts, the shops, the coins ------------------------------------------------------------------------------
    @GuhTest(template = EMPTY)
    public static void grotWinkelsEnMunten(GameTestHelper helper) {
        for (GrotSpel.Soort soort : GrotSpel.Soort.values()) {
            GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
            npc.setKind(soort.npc());
            helper.assertTrue(Features.role(soort.npc()) != null, "a role for " + soort);
            var offers = npc.getOffers();
            helper.assertTrue(!offers.isEmpty() && offers.stream().allMatch(o -> o.getCostA().is(soort.munt())), soort + ": all for its own coin");
            int kleding = 0, blokken = 0, deco = 0;
            for (MerchantOffer o : offers) {
                ItemStack uit = o.getResult();
                int prijs = o.getCostA().getCount();
                if (uit.getItem() instanceof nl.juiced.guhs.item.GuhClothingItem) {
                    kleding++;
                    helper.assertTrue(prijs >= 40 && prijs <= 80, "clothes 40-80: " + prijs);
                } else if (uit.getCount() == GrotSpelRol.BLOKKEN_PER_MUNT) {
                    blokken++;
                    helper.assertTrue(prijs == 1, "building blocks: 1 coin for 4");
                } else {
                    deco++;
                    helper.assertTrue(prijs >= 20 && prijs <= 64, "deco 20-64: " + prijs + " " + uit);
                }
            }
            helper.assertTrue(kleding >= 1 && blokken >= 2 && deco >= 2, soort + ": clothes, blocks and deco");
            helper.assertTrue(new ItemStack(soort.munt()).is(GRIJPTICKETS), soort.munt() + " works in the grijpmachine");
            helper.assertTrue(SpelGroepen.van(soort.id) != null && SpelGroepen.van(soort.id).spellen().equals(soort.borden()), "in the Minigames tab");
            for (String bord : soort.borden()) {
                helper.assertTrue(Highscores.game(bord) != null, "a Highscores row " + bord);
            }
            npc.discard();
        }
        for (GuhClothes c : GrotSpellenFeature.KLEDING) {
            helper.assertTrue(KledingBronnen.bron(c) != null && KledingBronnen.prijs(c) != null, "a source for " + c);
        }
        helper.assertTrue(GuhClothes.SCHAT_PIRATENHOEDJE.slot == GuhClothes.Slot.HEAD && GuhClothes.SCHAT_OOGKLEPJE.slot == GuhClothes.Slot.EYES
                && GuhClothes.GLIM_ANTENNETJES.slot == GuhClothes.Slot.OREN && GuhClothes.GLIM_VLINDERNETJE.slot == GuhClothes.Slot.BACK
                && GuhClothes.LICHTJES_PADDENSTOELHOEDJE.slot == GuhClothes.Slot.HEAD, "the slots");
        for (Item tool : List.of(GrotSpellenFeature.SCHATSCHEPJE.get(), GrotSpellenFeature.GLIMNETJE.get(), GrotSpellenFeature.GLIMPOTJE_RUG.get())) {
            helper.assertTrue(new ItemStack(tool).is(LOANED) && GrotSpellenFeature.geleend(new ItemStack(tool)), tool + " is borrowed only");
        }
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(GrotSpellenFeature.SCHATMUNTJE.get()).getPath().equals("schatmuntje"), "ids");
        helper.succeed();
    }

    /** The halls can't be broken (creative may); the borrowed tools go when you leave; a glimworm without a game is gone. */
    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void grotBeschermingEnOpruimen(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        BoundingBox box = new BoundingBox(abs.getX() - 2, abs.getY() - 2, abs.getZ() - 2, abs.getX() + 2, abs.getY() + 2, abs.getZ() + 2);
        GrotSpellenEvents.TEST_AREAS.add(box);
        try {
            helper.setBlock(new BlockPos(1, 1, 1), GrotSpellenFeature.NACHTVILT.get());
            BlockState s = helper.getLevel().getBlockState(abs);
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, s, p)).isCanceled(), "no breaking in a hall");
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, s, p)).isCanceled(), "creative may");
            helper.assertTrue(!GrotSpellenEvents.inZaal(helper.getLevel(), abs.offset(20, 0, 0)), "outside it isn't a hall");
        } finally {
            GrotSpellenEvents.TEST_AREAS.remove(box);
        }
        p.getInventory().add(new ItemStack(GrotSpellenFeature.SCHATSCHEPJE.get()));
        p.getInventory().add(new ItemStack(GrotSpellenFeature.GLIMNETJE.get()));
        GrotSpel.neemTerug(p);
        helper.assertTrue(count(p, GrotSpellenFeature.SCHATSCHEPJE.get()) == 0 && count(p, GrotSpellenFeature.GLIMNETJE.get()) == 0, "tools taken back");
        VangGlimwormEntity wees = GrotSpellenFeature.VANGGLIMWORM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 plek = helper.absoluteVec(new Vec3(2.5, 2.5, 2.5));
        wees.snapTo(plek.x, plek.y, plek.z, 0, 0);
        helper.getLevel().addFreshEntity(wees);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(wees.isRemoved(), "a glimworm without a game is gone");
            leave(helper, p);
            helper.succeed();
        });
    }

    // --- whole games in the real halls --------------------------------------------------------------------------------
    /** Schatgraven on medium: Snuffie comes, digging shows what was under it, a piepkaas costs time, the payout, the refill. */
    @GuhTest(template = "grotspel_schatgraven", timeoutTicks = 400)
    public static void grotSchatgravenPotje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = host(helper, GuhNpcEntity.Kind.SCHATGRAAFGUH);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        Schatgraven spel = (Schatgraven) GrotSpel.van(npc);
        helper.assertTrue(spel.start(npc, p, Niveau.MEDIUM), "the game starts");
        helper.assertTrue(GrotSpel.speelt(p, GrotSpel.Soort.SCHATGRAVEN) && "schatgraven".equals(Minigames.playing(p)), "playing");
        helper.assertTrue(count(p, GrotSpellenFeature.SCHATSCHEPJE.get()) == 1, "the soft shovel");
        helper.assertTrue(spel.hoek() != null && spel.snuffie() != null && level.getEntity(spel.snuffie()) instanceof GuhNpcEntity, "the field and Snuffie");
        ServerPlayer ander = player(helper);
        ander.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1);
        helper.assertTrue(!spel.start(npc, ander, Niveau.MAKKELIJK), "one player at a time");
        leave(helper, ander);
        spel.testAftelOver();
        int[] tijd = new int[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(spel.fase() == GrotSpel.Fase.SPELEN, "playing: " + spel.fase());
            spel.testVeld(level, new SchatVeld(Niveau.MEDIUM, new int[][]{{7, 7}, {8, 9}}, new int[]{6, 6}, new int[][]{{9, 4}}));
            BlockPos hoek = spel.hoek();
            helper.assertTrue(level.getBlockState(hoek).getValue(GrotSpellenBlocks.STAAT) == GrotSpellenBlocks.Staat.RUST
                    && level.getBlockState(hoek.offset(7, 0, 7)).getValue(GrotSpellenBlocks.STAAT) == GrotSpellenBlocks.Staat.DICHT,
                    "medium plays the middle 11: the edge rests");
            p.snapTo(hoek.getX() + 7.5, hoek.getY() + 1, hoek.getZ() + 7.5);
            spel.testSnuif();
        });
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(spel.laatsteHint() == SchatVeld.Hint.HEET, "Snuffie: HEET on a treasure: " + spel.laatsteHint());
            BlockPos hoek = spel.hoek();
            helper.assertTrue(spel.graafHier(p, hoek.offset(0, 0, 0)) == null, "the resting edge isn't dug");
            helper.assertTrue(spel.graafHier(p, hoek.offset(5, 0, 5)) == SchatVeld.Inhoud.LEEG
                    && level.getBlockState(hoek.offset(5, 0, 5)).getValue(GrotSpellenBlocks.STAAT) == GrotSpellenBlocks.Staat.LEEG, "an empty pit");
            tijd[0] = spel.over();
            helper.assertTrue(spel.graafHier(p, hoek.offset(9, 0, 4)) == SchatVeld.Inhoud.PIEP && spel.over() == tijd[0] - SchatVeld.PIEP_STRAF,
                    "a piepkaas: 10 seconds off");
            helper.assertTrue(spel.graafHier(p, hoek.offset(6, 0, 6)) == SchatVeld.Inhoud.GOUD
                    && level.getBlockState(hoek.offset(6, 0, 6)).getValue(GrotSpellenBlocks.STAAT) == GrotSpellenBlocks.Staat.GOUD, "the golden cheese");
            helper.assertTrue(spel.graafHier(p, hoek.offset(7, 0, 7)) == SchatVeld.Inhoud.SCHAT && spel.graafHier(p, hoek.offset(8, 0, 9)) == SchatVeld.Inhoud.SCHAT,
                    "both treasures");
            tijd[0] = spel.punten();
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(spel.fase() == GrotSpel.Fase.UITSLAG, "everything found: the result: " + spel.fase());
            int punten = GrotSpel.best(p, GrotSpel.Soort.SCHATGRAVEN, Niveau.MEDIUM);
            helper.assertTrue(punten >= 45 && punten <= tijd[0], "2 x 10 + 25 + the time bonus: " + punten);
            int munten = count(p, GrotSpellenFeature.SCHATMUNTJE.get());
            helper.assertTrue(munten == SchatVeld.munten(Niveau.MEDIUM, punten, false), "schatmuntjes for the points: " + munten);
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == 16 && count(p, GrotSpellenFeature.GOUDEN_KAASJE_ITEM.get()) == 1, "a first-game present");
            helper.assertTrue(count(p, GrotSpellenFeature.SCHATSCHEPJE.get()) == 0, "the shovel went back");
            helper.assertTrue(Scorebord.top(level.getServer(), "schatgraven_medium").stream().anyMatch(e -> e.player().equals(p.getUUID())), "on the board");
        });
        helper.runAfterDelay(10 + GrotSpel.UITSLAG_TICKS + 5, () -> {
            helper.assertTrue(!spel.isRunning() && !GrotSpel.speelt(p), "the game is over");
            BlockPos hoek = spel.hoek();
            for (int x = 0; x < SchatVeld.GROOTTE; x++) {
                for (int z = 0; z < SchatVeld.GROOTTE; z++) {
                    helper.assertTrue(level.getBlockState(hoek.offset(x, 0, z)).getValue(GrotSpellenBlocks.STAAT) == GrotSpellenBlocks.Staat.DICHT,
                            "the field is filled in again at " + x + "," + z);
                }
            }
            helper.assertTrue(spel.snuffie() == null && level.getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                    n -> n.getKind() == GuhNpcEntity.Kind.SNUFFELHULPJE).isEmpty(), "Snuffie went home");
            helper.assertTrue(p.distanceTo(npc) < 5, "back next to the Kapitein");
            leave(helper, p);
            helper.succeed();
        });
    }

    /** Glimwormvangen on medium: the glimworms come, catching lights the hall and the jar, a swing of the net, the payout. */
    @GuhTest(template = "grotspel_glimwormen", timeoutTicks = 400)
    public static void grotGlimwormPotje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = host(helper, GuhNpcEntity.Kind.GLIMWORMGUH);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        Glimwormvangen spel = (Glimwormvangen) GrotSpel.van(npc);
        helper.assertTrue(spel.start(npc, p, Niveau.MEDIUM), "the game starts");
        helper.assertTrue(count(p, GrotSpellenFeature.GLIMNETJE.get()) == 1 && count(p, GrotSpellenFeature.GLIMPOTJE_RUG.get()) == 1, "net and jar");
        helper.assertTrue(spel.wormen().size() == Glimwormvangen.aantal(Niveau.MEDIUM), "the glimworms: " + spel.wormen().size());
        helper.assertTrue(spel.licht() == Glimwormvangen.startLicht(Niveau.MEDIUM)
                && spel.potten().stream().allMatch(pos -> level.getBlockState(pos).getValue(GrotSpellenBlocks.LICHT) == 1), "a dark hall to start");
        spel.testAftelOver();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(spel.fase() == GrotSpel.Fase.SPELEN, "playing");
            for (int i = 0; i < 4; i++) {
                VangGlimwormEntity w = (VangGlimwormEntity) level.getEntity(spel.wormen().get(0));
                spel.testVang(level, p, w);
            }
            helper.assertTrue(spel.gevangen() == 4 && spel.punten() == 4 && spel.wormen().size() == Glimwormvangen.aantal(Niveau.MEDIUM) - 4, "four caught");
            helper.assertTrue(spel.licht() == 2 && spel.potten().stream().allMatch(pos -> level.getBlockState(pos).getValue(GrotSpellenBlocks.LICHT) == 2),
                    "the hall is brighter");
            ItemStack potje = ItemStack.EMPTY;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(GrotSpellenFeature.GLIMPOTJE_RUG.get())) {
                    potje = p.getInventory().getItem(i);
                }
            }
            helper.assertTrue(GrotSpelItems.stand(potje) == 2, "and so is the jar: " + GrotSpelItems.stand(potje));
            // a swing at one right in front of you
            VangGlimwormEntity w = (VangGlimwormEntity) level.getEntity(spel.wormen().get(0));
            Vec3 voor = p.getEyePosition().add(p.getLookAngle().scale(1.0));
            w.snapTo(voor.x, voor.y - 0.2, voor.z, 0, 0);
            helper.assertTrue(Glimwormvangen.zwaai(p) >= 1 && spel.gevangen() >= 5, "the net catches it");
        });
        helper.runAfterDelay(5 + Glimwormvangen.NIEUW_NA + 5, () -> {
            helper.assertTrue(spel.wormen().size() == Glimwormvangen.aantal(Niveau.MEDIUM), "caught ones come back: " + spel.wormen().size());
            spel.testTijd(1);
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(spel.fase() == GrotSpel.Fase.UITSLAG, "time is up: " + spel.fase());
            int punten = spel.punten();
            helper.assertTrue(count(p, GrotSpellenFeature.GLIMMERTJE.get()) == Glimwormvangen.munten(Niveau.MEDIUM, punten, false), "glimmertjes for " + punten);
            helper.assertTrue(count(p, GrotSpellenFeature.GLIMNETJE.get()) == 0 && count(p, GrotSpellenFeature.GLIMPOTJE_RUG.get()) == 0, "net and jar back");
        });
        helper.runAfterDelay(60 + GrotSpel.UITSLAG_TICKS + 5, () -> {
            helper.assertTrue(!spel.isRunning() && spel.wormen().isEmpty(), "the game is over");
            helper.assertTrue(level.getEntitiesOfClass(VangGlimwormEntity.class, helper.getBounds()).isEmpty(), "every glimworm gone");
            helper.assertTrue(spel.potten().stream().allMatch(pos -> level.getBlockState(pos).getValue(GrotSpellenBlocks.LICHT) == 1), "the quiet glow again");
            leave(helper, p);
            helper.succeed();
        });
    }

    /** The Lichtjespuzzel on lastig: the raster in the floor follows the puzzle, the fewest presses solve it, the payout, all on again. */
    @GuhTest(template = "grotspel_lichtjes", timeoutTicks = 400)
    public static void grotLichtjesPotje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = host(helper, GuhNpcEntity.Kind.LAMPJESGUH);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        Lichtjespuzzel spel = (Lichtjespuzzel) GrotSpel.van(npc);
        helper.assertTrue(spel.start(npc, p, Niveau.LASTIG), "the game starts");
        LichtjesRaster r = spel.raster();
        helper.assertTrue(r != null && spel.minimaal() == LichtjesRaster.drukken(Niveau.LASTIG), "a lastig puzzle: " + spel.minimaal());
        BlockPos hoek = spel.hoek();
        for (int x = 0; x < LichtjesRaster.N; x++) {
            for (int z = 0; z < LichtjesRaster.N; z++) {
                for (int d = 0; d < Lichtjespuzzel.CEL * Lichtjespuzzel.CEL; d++) {
                    BlockPos pos = hoek.offset(x * Lichtjespuzzel.CEL + d % Lichtjespuzzel.CEL, 0, z * Lichtjespuzzel.CEL + d / Lichtjespuzzel.CEL);
                    helper.assertTrue(level.getBlockState(pos).getValue(GrotSpellenBlocks.AAN) == r.aan(x, z), "the floor shows the puzzle");
                }
            }
        }
        int[] cel = spel.celVan(hoek.offset(5, 0, 12));
        helper.assertTrue(cel != null && cel[0] == 2 && cel[1] == 6 && spel.celVan(hoek.offset(-1, 0, 0)) == null, "blocks to lamps");
        spel.testAftelOver();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(spel.fase() == GrotSpel.Fase.SPELEN, "playing");
            for (int i : spel.raster().oplossing()) {
                helper.assertTrue(spel.druk(level, p, i / LichtjesRaster.N, i % LichtjesRaster.N), "a press");
            }
            helper.assertTrue(spel.raster().allesAan() && spel.stappen() == spel.minimaal(), "solved in the fewest presses");
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(spel.fase() == GrotSpel.Fase.UITSLAG, "solved: the result");
            int punten = GrotSpel.best(p, GrotSpel.Soort.LICHTJESPUZZEL, Niveau.LASTIG);
            helper.assertTrue(punten > 150 + 60, "100 + the seconds left + 50: " + punten);
            int munten = count(p, GrotSpellenFeature.LAMPJESMUNT.get());
            helper.assertTrue(munten >= 34 + 13 + 12 && munten <= 70, "lastig, quick and perfect: " + munten);
            helper.assertTrue(count(p, GrotSpellenFeature.PADDENSTOELLAMPJE_ITEM.get()) == 1, "the first-game present");
        });
        helper.runAfterDelay(8 + GrotSpel.UITSLAG_TICKS + 5, () -> {
            helper.assertTrue(!spel.isRunning(), "the game is over");
            for (int x = 0; x < Lichtjespuzzel.MAAT; x++) {
                for (int z = 0; z < Lichtjespuzzel.MAAT; z++) {
                    helper.assertTrue(level.getBlockState(hoek.offset(x, 0, z)).getValue(GrotSpellenBlocks.AAN), "every lamp on again");
                }
            }
            leave(helper, p);
            helper.succeed();
        });
    }

    /** A second game on the same level that beats the first: +5 coins for the record. */
    @GuhTest(template = "grotspel_lichtjes", timeoutTicks = 400)
    public static void grotRecordBonus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = host(helper, GuhNpcEntity.Kind.LAMPJESGUH);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        Lichtjespuzzel spel = (Lichtjespuzzel) GrotSpel.van(npc);
        nl.juiced.guhs.quest.GuhQuests.saved(p).putInt(GrotSpel.bestKey(GrotSpel.Soort.LICHTJESPUZZEL, Niveau.MAKKELIJK), 1);
        nl.juiced.guhs.quest.GuhQuests.saved(p).putBoolean("guhs_lichtjespuzzel_eerste", true);
        helper.assertTrue(spel.start(npc, p, Niveau.MAKKELIJK), "the game starts");
        spel.testAftelOver();
        helper.runAfterDelay(5, () -> {
            for (int i : spel.raster().oplossing()) {
                spel.druk(level, p, i / LichtjesRaster.N, i % LichtjesRaster.N);
            }
        });
        helper.runAfterDelay(8, () -> {
            int punten = GrotSpel.best(p, GrotSpel.Soort.LICHTJESPUZZEL, Niveau.MAKKELIJK);
            int munten = count(p, GrotSpellenFeature.LAMPJESMUNT.get());
            int zonder = LichtjesRaster.munten(Niveau.MAKKELIJK, true, (punten - 150) * 20, LichtjesRaster.speelTicks(Niveau.MAKKELIJK), 4, 4, 49, false);
            helper.assertTrue(punten > 1 && munten >= zonder + GrotSpel.RECORD_BONUS - 1 && munten <= zonder + GrotSpel.RECORD_BONUS + 1,
                    "a record: +5 (" + munten + " vs " + zonder + ")");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == 0, "no present the second time");
        });
        helper.runAfterDelay(8 + GrotSpel.UITSLAG_TICKS + 5, () -> {
            leave(helper, p);
            helper.succeed();
        });
    }
}
