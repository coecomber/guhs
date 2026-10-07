package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.Films;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.Grap;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the guhpixel slice "grap1" (batch px_grap1; run with {@code -Pgt=px_grap1}): Skyblok, Bedwars and Vadsnite
 * from start to punchline in their own arenas (the templates are checked against the Java constants on the way), the
 * reward of the first time against a replay, two players at once, leaving in the middle, the 99 cheap guhs of Vadsnite
 * (made, counted, removed), the draw, the lobby NPCs' roles, the keepsakes and the Guhdex section. The game test server
 * has no guhpixel dimension: every test marks its own box ({@link PxTest#gebied}) and starts its game with
 * {@link Sessies#startOp}.
 */
public class PxGrap1GameTests {
    private static final String BATCH = "px_grap1";
    private static final String SKY_KAMER = "skyblok_test_kamer", BED_KAMER = "bedwars_test_kamer", VADS_KAMER = "vadsnite_test_kamer";
    private static final BlockPos HOEK = new BlockPos(2, 2, 2);

    private static BlockPos w(Arena a, BlockPos lokaal) {
        return a.wereld(lokaal.getX(), lokaal.getY(), lokaal.getZ());
    }

    /** Puts the player next to this bed and lets them lie down in it; true when they lie. */
    private static boolean gaLiggen(ServerPlayer p, BlockPos bedHoofd) {
        p.snapTo(bedHoofd.getX() + 0.5, bedHoofd.getY(), bedHoofd.getZ() + 1.5, 0f, 0f);
        return p.startSleepInBed(bedHoofd).right().isPresent() && p.isSleeping();
    }

    private static int aantal(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    // --- Skyblok -------------------------------------------------------------------------------------------------------------

    @GuhTest(template = SKY_KAMER, batch = BATCH, timeoutTicks = 300)
    public static void skyblokNiksWerktBehalveHetBed(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        p.getInventory().setItem(3, new ItemStack(Items.EMERALD, 7));
        Sessie sessie = Sessies.startOp(level, helper.absolutePos(HOEK), SkyblokSessie.SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(sessie instanceof SkyblokSessie, "the game starts");
        SkyblokSessie s = (SkyblokSessie) sessie;
        Arena a = s.arena();
        // the template against the constants
        BlockPos voet = w(a, SkyblokSessie.BED_VOET), hoofd = w(a, SkyblokSessie.BED_HOOFD), kist = w(a, SkyblokSessie.KIST), deur = w(a, SkyblokSessie.DEUR);
        helper.assertTrue(level.getBlockState(voet).is(BlockTags.BEDS) && level.getBlockState(hoofd).is(BlockTags.BEDS), "the bed");
        helper.assertTrue(level.getBlockState(deur).getBlock() instanceof DoorBlock && level.getBlockState(deur.above()).getBlock() instanceof DoorBlock,
                "the staff door in the sky");
        helper.assertTrue(level.getBlockState(a.wereld(5, 5, 0)).is(Blocks.LIGHT_BLUE_TERRACOTTA) && level.getBlockState(a.wereld(9, 5, 0)).is(Blocks.CYAN_TERRACOTTA)
                && level.getBlockState(a.wereld(5, 15, 0)).is(Blocks.WHITE_CONCRETE), "the sky is painted: blue terracotta, a seam, a white cloud");
        helper.assertTrue(level.getBlockState(a.wereld(20, 8, 1)).is(Blocks.LADDER) && level.getBlockState(a.wereld(13, 23, 13)).is(Blocks.GLOWSTONE),
                "a ladder up the sky and a lamp for a sun");
        helper.assertTrue(level.getBlockState(a.wereld(11, SkyblokSessie.EILAND_Y, 12)).is(Blocks.GRASS_BLOCK)
                && level.getBlockState(a.wereld(14, SkyblokSessie.EILAND_Y, 14)).isAir() && level.getBlockState(a.wereld(11, 12, 14)).is(BlockTags.LOGS),
                "the L-shaped island with its tree");
        helper.assertTrue(level.getBlockEntity(kist) instanceof ChestBlockEntity k && k.getItem(12).is(Items.ICE) && k.getItem(14).is(Items.LAVA_BUCKET),
                "the start chest holds ice and a lava bucket");
        helper.assertTrue(p.position().distanceTo(a.start()) < 0.1 && p.getInventory().isEmpty(), "on the island, with empty pockets");
        helper.assertTrue(Grappen.stap(p, SkyblokSessie.ID) == 1 && s.stap() == 1, "step 1");
        // nothing works
        BlockPos gras = a.wereld(11, SkyblokSessie.EILAND_Y, 11);
        helper.assertTrue(!p.gameMode.destroyBlock(gras) && level.getBlockState(gras).is(Blocks.GRASS_BLOCK) && s.pogingen() == 1, "breaking does not work");
        helper.assertTrue(s.stap() == 3, "a failed try: on to 'make a cobblestone generator'");
        helper.assertTrue(!s.magPlaatsen(p, gras.above(), Blocks.ICE.defaultBlockState()), "placing does not work");
        helper.assertTrue(s.stap() == 4 && Grappen.stap(p, SkyblokSessie.ID) == 4, "a second try: give up");
        p.getInventory().setItem(0, new ItemStack(Items.LAVA_BUCKET));
        s.itemGebruik(p, p.getInventory().getItem(0));
        helper.assertTrue(p.getInventory().getItem(0).is(Items.LAVA_BUCKET) && s.pogingen() == 3, "the bucket stays full");
        helper.runAtTickTime(6, () -> {
            helper.assertTrue(s.magGebruiken(p, kist, level.getBlockState(kist)), "the chest opens");
            helper.assertTrue(s.magGebruiken(p, voet, level.getBlockState(voet)), "the bed may be used");
        });
        helper.runAtTickTime(12, () -> {
            helper.assertTrue(!s.magGebruiken(p, deur, level.getBlockState(deur)), "the staff door stays shut");
            helper.assertTrue(!s.inFinale() && !s.gewonnen(), "no punchline yet");
            helper.assertTrue(gaLiggen(p, hoofd), "the only thing that works: lying in the bed");
        });
        helper.runAtTickTime(16, () -> {
            helper.assertTrue(s.inFinale() && s.gewonnen() && p.isSleeping(), "SKYBLOK UITGESPEELD");
            helper.assertTrue(!Grappen.isKlaar(p, SkyblokSessie.ID) && Muntjes.saldo(p) == 0, "the reward waits until the own things are back");
            helper.assertTrue(!s.magGebruiken(p, kist, level.getBlockState(kist)), "after the punchline nothing is used any more");
            helper.assertTrue(Dutje.zet(p, 100) && p.isSleepingLongEnough(), "the sleep counter can be reached in this runtime");
        });
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(p.getSleepTimer() == 60 && !p.isSleepingLongEnough(), "a lone sleeper is never 'slept long enough': " + p.getSleepTimer());
            s.klaar(p);   // (the test does not sit through the credits)
            helper.assertTrue(Grappen.isKlaar(p, SkyblokSessie.ID) && Grappen.keren(p, SkyblokSessie.ID) == 1 && Muntjes.saldo(p) == Grappen.BELONING
                    && Films.heeft(p, SkyblokSessie.ID), "the first time: 100 muntjes and the film");
            helper.assertTrue(aantal(p, Grap1Slice.FLES_ITEM.get()) == 1 && p.getInventory().getItem(3).getCount() == 7
                    && aantal(p, Items.LAVA_BUCKET) == 0 && aantal(p, Items.ICE) == 0, "the keepsake is in the OWN inventory, the game items are gone");
            helper.assertTrue(!p.isSleeping() && !level.getBlockState(hoofd).getValue(BedBlock.OCCUPIED) && Sessies.van(p) == null && s.isGestopt(),
                    "out of bed, out of the game");
            helper.assertTrue(level.getBlockEntity(kist) instanceof ChestBlockEntity k && k.isEmpty(), "the arena is tidy again");
            // a replay: a speedrun straight into bed pays nothing and gives no second bottle
            SkyblokSessie weer = (SkyblokSessie) Sessies.startOp(level, helper.absolutePos(HOEK), SkyblokSessie.SPEL, List.of(p), new CompoundTag());
            helper.assertTrue(weer != null && weer.stap() == 1 && Grappen.stap(p, SkyblokSessie.ID) == 4, "the step of a replay starts again, the saved one stays");
            helper.assertTrue(gaLiggen(p, hoofd), "straight into bed");
        });
        helper.runAtTickTime(24, () -> {
            Sessie weer = Sessies.van(p);
            helper.assertTrue(weer instanceof SkyblokSessie w && w.gewonnen(), "the replay's punchline");
            Sessies.verlaat(p, Vertrek.VERLATEN);   // (/lobby during the credits: the run still counts)
            helper.assertTrue(Grappen.keren(p, SkyblokSessie.ID) == 2 && Muntjes.saldo(p) == Grappen.BELONING && aantal(p, Grap1Slice.FLES_ITEM.get()) == 1,
                    "a replay counts, pays nothing and gives no second keepsake");
            PxTest.klaar(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = BED_KAMER, batch = BATCH, timeoutTicks = 200)
    public static void tweeSpelersTegelijkEnWeglopen(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        q.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 2));
        SkyblokSessie sp = (SkyblokSessie) Sessies.startOp(level, helper.absolutePos(HOEK), SkyblokSessie.SPEL, List.of(p), new CompoundTag());
        SkyblokSessie sq = (SkyblokSessie) Sessies.startOp(level, helper.absolutePos(HOEK.offset(28, 0, 0)), SkyblokSessie.SPEL, List.of(q), new CompoundTag());
        helper.assertTrue(sp != null && sq != null && sp.arena() != sq.arena() && Sessies.van(p) == sp && Sessies.van(q) == sq && Sessies.aantalSpelers(SkyblokSessie.ID) >= 2,
                "two players, two islands");
        helper.assertTrue(Sessies.weigering(SkyblokSessie.SPEL, List.of(p, q)) != null, "a joke game is for one player");
        helper.assertTrue(level.getBlockEntity(w(sp.arena(), SkyblokSessie.KIST)) instanceof ChestBlockEntity a && !a.isEmpty()
                && level.getBlockEntity(w(sq.arena(), SkyblokSessie.KIST)) instanceof ChestBlockEntity b && !b.isEmpty(), "each their own start chest");
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(gaLiggen(p, w(sp.arena(), SkyblokSessie.BED_HOOFD)), "p lies down");
            // q takes the ice out of the chest and walks away in the middle of the game: nothing is paid, nothing is lost
            q.getInventory().setItem(0, new ItemStack(Items.ICE));
            Sessies.verlaat(q, Vertrek.UITGELOGD);
            helper.assertTrue(!Grappen.isKlaar(q, SkyblokSessie.ID) && Muntjes.saldo(q) == 0 && Grappen.stap(q, SkyblokSessie.ID) == 1, "no punchline, no reward");
            helper.assertTrue(q.getInventory().getItem(8).getCount() == 2 && aantal(q, Items.ICE) == 0 && sq.isGestopt() && !sp.isGestopt(),
                    "q has the own things back and the other game runs on");
        });
        helper.runAtTickTime(9, () -> {
            helper.assertTrue(sp.gewonnen() && !sq.gewonnen(), "only p reached the punchline");
            Sessies.verlaat(p, Vertrek.UITGELOGD);   // (logging out during the credits still pays)
            helper.assertTrue(Grappen.isKlaar(p, SkyblokSessie.ID) && Muntjes.saldo(p) == Grappen.BELONING && aantal(p, Grap1Slice.FLES_ITEM.get()) == 1,
                    "leaving after the punchline keeps the reward");
            helper.assertTrue(Sessies.van(p) == null && Sessies.van(q) == null && sp.isGestopt(), "both games are over");
            PxTest.klaar(helper, p, q);
            helper.succeed();
        });
    }

    // --- Bedwars -------------------------------------------------------------------------------------------------------------

    @GuhTest(template = BED_KAMER, batch = BATCH, timeoutTicks = 1400)
    public static void bedwarsTeamsKomenErbijLiggen(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        BedwarsSessie s = (BedwarsSessie) Sessies.startOp(level, helper.absolutePos(HOEK), BedwarsSessie.SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(s != null, "the game starts");
        Arena a = s.arena();
        BlockPos hoofd = w(a, BedwarsSessie.BED_HOOFD);
        helper.assertTrue(level.getBlockState(w(a, BedwarsSessie.BED_VOET)).is(BlockTags.BEDS) && level.getBlockState(hoofd).is(BlockTags.BEDS), "THE bed");
        List<TeamGuhEntity> guhs = level.getEntitiesOfClass(TeamGuhEntity.class, a.doos());
        helper.assertTrue(guhs.size() == 4 && s.ploegen().size() == 4, "four team guhs: " + guhs.size());
        for (BedwarsSessie.Ploeg pl : s.ploegen()) {
            TeamGuhEntity guh = pl.guh();
            BlockPos eiland = BlockPos.containing(a.wereld(pl.team.thuis())).below();
            helper.assertTrue(guh != null && guh.getType() != ModEntities.GUH.get() && guh.getType() == Grap1Slice.TEAMGUH.get() && !guh.isTame(),
                    "a stand-in, never a real guh");
            helper.assertTrue(guh.getClothes(GuhClothes.Slot.HEAD) == pl.team.muts && guh.hasCustomName(), "the team's nightcap");
            helper.assertTrue(level.getBlockState(eiland).is(pl.team.wol) && guh.distanceToSqr(a.wereld(pl.team.thuis())) < 0.3, "on its own island of team wool");
            for (int i = 0; i < BedwarsSessie.BRUG_LENGTE; i++) {
                helper.assertTrue(level.getBlockState(w(a, pl.team.brug(i))).isAir(), "no bridge yet");
            }
            BlockPos eerste = pl.team.brug(0), laatste = pl.team.brug(BedwarsSessie.BRUG_LENGTE - 1);
            helper.assertTrue(!level.getBlockState(w(a, eerste.offset(pl.team.dx, 0, pl.team.dz))).isAir()
                    && !level.getBlockState(w(a, laatste.offset(-pl.team.dx, 0, -pl.team.dz))).isAir(), "the bridge will join both islands");
        }
        helper.assertTrue(s.stap() == 1 && !p.gameMode.destroyBlock(hoofd) && level.getBlockState(hoofd).is(BlockTags.BEDS), "the bed cannot be broken, not even by you");
        helper.runAtTickTime(BedwarsSessie.BRUG_START + 30, () -> {
            helper.assertTrue(s.stap() == 2 && s.ploegen().get(0).gelegd >= 1 && level.getBlockState(w(a, BedwarsSessie.Team.ROOD.brug(0))).is(Blocks.RED_WOOL),
                    "they come: red bridges with red wool");
            helper.assertTrue(gaLiggen(p, hoofd), "defend the bed: lie in it");
        });
        helper.runAtTickTime(BedwarsSessie.BRUG_START + 34, () -> helper.assertTrue(s.stap() == 3 && s.dutjes() == 0, "in bed: step 3"));
        boolean[] klaar = new boolean[1];
        RuntimeException[] fout = new RuntimeException[1];
        helper.succeedWhen(() -> {
            if (fout[0] != null) {
                throw fout[0];
            }
            helper.assertTrue(s.gewonnen(), "all four lie around the bed: " + s.dutjes() + " naps, bridges "
                    + s.ploegen().stream().map(pl -> pl.team.id + "=" + pl.gelegd + "/" + pl.fase).toList());
            if (klaar[0]) {
                return;
            }
            klaar[0] = true;
            try {
            helper.assertTrue(s.dutjes() == 4 && s.stap() == BedwarsSessie.STAPPEN && p.isSleeping() && p.getHealth() == p.getMaxHealth(), "BED VERDEDIGD: nobody hurt");
            for (BedwarsSessie.Ploeg pl : s.ploegen()) {
                helper.assertTrue(pl.gelegd == BedwarsSessie.BRUG_LENGTE && level.getBlockState(w(a, pl.team.brug(6))).is(pl.team.wol), "a whole bridge of team wool");
                helper.assertTrue(pl.guh() != null && pl.guh().distanceToSqr(a.wereld(pl.team.bedplek())) < 1.0, "asleep beside the bed: " + pl.team.id);
            }
            helper.assertTrue(!KledingUnlocks.heeft(p, GuhClothes.BEDWARS_SLAAPMUTS_ROOD), "the keepsake comes with the reward");
            s.klaar(p);
            helper.assertTrue(Grappen.isKlaar(p, BedwarsSessie.ID) && Muntjes.saldo(p) == Grappen.BELONING, "100 muntjes");
            for (GuhClothes muts : Grap1Slice.MUTSEN) {
                helper.assertTrue(KledingUnlocks.heeft(p, muts), "the four Teamslaapmutsen are unlocked: " + muts);
            }
            helper.assertTrue(level.getEntitiesOfClass(TeamGuhEntity.class, a.doos()).isEmpty(), "the team guhs are gone");
            for (BedwarsSessie.Team t : BedwarsSessie.Team.values()) {
                for (int i = 0; i < BedwarsSessie.BRUG_LENGTE; i++) {
                    helper.assertTrue(level.getBlockState(w(a, t.brug(i))).isAir(), "the bridges are gone");
                }
            }
            } catch (RuntimeException e) {
                fout[0] = e;
                throw e;
            } finally {
                PxTest.klaar(helper, p);
            }
        });
    }

    // --- Vadsnite ------------------------------------------------------------------------------------------------------------

    private static int schermenInDoos(ServerLevel level, Arena a) {
        return level.getEntitiesOfClass(Display.BlockDisplay.class, a.doos().inflate(2), d -> d.entityTags().contains(VadsniteSessie.TAG)).size();
    }

    private static void spring(ServerPlayer p, Arena a) {
        Vec3 onder = a.wereld(new Vec3(VadsniteSessie.MIDDEN + 0.5, VadsniteSessie.BUS_Y - 3, VadsniteSessie.LUIK_Z0 + 1));
        p.snapTo(onder.x, onder.y, onder.z, 0f, 0f);
        p.setOnGround(false);
    }

    private static void land(ServerPlayer p, Arena a) {
        Vec3 gras = a.wereld(new Vec3(VadsniteSessie.MIDDEN + 0.5, VadsniteSessie.EILAND_Y + 1, VadsniteSessie.MIDDEN + 2.5));
        p.snapTo(gras.x, gras.y, gras.z, 0f, 0f);
        p.setOnGround(true);
    }

    @GuhTest(template = VADS_KAMER, batch = BATCH, timeoutTicks = 900)
    public static void vadsniteNegenennegentigGoedkopeGuhs(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        VadsniteSessie s = (VadsniteSessie) Sessies.startOp(level, helper.absolutePos(HOEK), VadsniteSessie.SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(s != null, "the game starts");
        Arena a = s.arena();
        // the template: the bus floor under the start, the open hatch, the island under it, the four sleeping spots
        helper.assertTrue(p.position().distanceTo(a.start()) < 0.1 && level.getBlockState(BlockPos.containing(a.start()).below()).is(Blocks.BLUE_CONCRETE), "in the Vadsbus");
        for (int x = VadsniteSessie.LUIK_X0; x <= VadsniteSessie.LUIK_X1; x++) {
            for (int z = VadsniteSessie.LUIK_Z0; z <= VadsniteSessie.LUIK_Z1; z++) {
                helper.assertTrue(level.getBlockState(a.wereld(x, VadsniteSessie.BUS_Y, z)).isAir()
                        && level.getBlockState(a.wereld(x, VadsniteSessie.EILAND_Y, z)).is(Blocks.MOSS_BLOCK), "the hatch is open above the moss");
            }
        }
        for (BlockPos voet : VadsniteSessie.BEDDEN) {
            helper.assertTrue(level.getBlockState(w(a, voet)).is(BlockTags.BEDS) && level.getBlockState(w(a, voet.north())).is(BlockTags.BEDS), "a sleeping spot at " + voet);
        }
        helper.assertTrue(s.wakker() == VadsniteSessie.GUHS + 1 && s.schermen() == 0 && schermenInDoos(level, a) == 0, "100 awake, nobody jumped yet");
        int alleGesprongen = VadsniteSessie.SPRING_START + VadsniteSessie.GUHS / VadsniteSessie.PER_TICK + 4;
        helper.runAtTickTime(alleGesprongen, () -> {
            helper.assertTrue(s.schermen() == 2 * VadsniteSessie.GUHS && schermenInDoos(level, a) == 2 * VadsniteSessie.GUHS,
                    "99 guhs in the air, each with a parachute: display entities only: " + schermenInDoos(level, a));
            helper.assertTrue(level.getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, a.doos()).isEmpty(), "not one real guh entity");
            helper.assertTrue(s.stap() == 2 && !s.geland(), "the player is still in the bus");
            spring(p, a);
        });
        helper.runAtTickTime(alleGesprongen + 3, () -> {
            helper.assertTrue(s.stap() == 3 && p.hasEffect(MobEffects.SLOW_FALLING), "out of the hatch: floating down");
            land(p, a);
        });
        int alleGeland = VadsniteSessie.SPRING_START + VadsniteSessie.GUHS / VadsniteSessie.PER_TICK + 2 + 2 * VadsniteSessie.GLIJ + 6;
        helper.runAtTickTime(alleGeland, () -> {
            helper.assertTrue(s.geland() && s.stap() == 4 && !p.hasEffect(MobEffects.SLOW_FALLING), "landed");
            helper.assertTrue(s.wakker() == 2 && schermenInDoos(level, a) == VadsniteSessie.GUHS,
                    "everyone fell asleep at once (the parachutes are gone), one guh and you are awake: " + s.wakker() + " / " + schermenInDoos(level, a));
            helper.assertTrue(s.uitslag() == VadsniteSessie.Uitslag.BEZIG, "the slaapwolk is still closing");
        });
        boolean[] klaar = new boolean[1];
        RuntimeException[] fout = new RuntimeException[1];
        helper.succeedWhen(() -> {
            if (fout[0] != null) {
                throw fout[0];
            }
            helper.assertTrue(s.gewonnen(), "the last guh nods off: " + s.wakker() + " awake at " + s.ticks());
            if (klaar[0]) {
                return;
            }
            klaar[0] = true;
            try {
            helper.assertTrue(s.uitslag() == VadsniteSessie.Uitslag.OVERWINNING && s.wakker() == 1 && s.ticks() >= VadsniteSessie.WOLK_TICKS, "#1 VADSOVERWINNING");
            s.klaar(p);
            helper.assertTrue(Grappen.isKlaar(p, VadsniteSessie.ID) && Muntjes.saldo(p) == Grappen.BELONING
                    && KledingUnlocks.heeft(p, GuhClothes.VADSNITE_PARACHUTERUGZAKJE), "100 muntjes and the Parachuterugzakje");
            helper.assertTrue(schermenInDoos(level, a) == 0 && s.schermen() == 0, "the 99 guhs are removed afterwards: " + schermenInDoos(level, a));
            } catch (RuntimeException e) {
                fout[0] = e;
                throw e;
            } finally {
                PxTest.klaar(helper, p);
            }
        });
    }

    @GuhTest(template = VADS_KAMER, batch = BATCH, timeoutTicks = 300)
    public static void vadsniteGelijkspelEnWeglopen(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        VadsniteSessie s = (VadsniteSessie) Sessies.startOp(level, helper.absolutePos(HOEK), VadsniteSessie.SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(s != null, "the game starts");
        Arena a = s.arena();
        helper.runAtTickTime(VadsniteSessie.SPRING_START + 10, () -> spring(p, a));
        helper.runAtTickTime(VadsniteSessie.SPRING_START + 13, () -> land(p, a));
        helper.runAtTickTime(VadsniteSessie.SPRING_START + 16, () -> {
            helper.assertTrue(s.geland() && s.wakker() > 50, "landed while most guhs are still in the air: " + s.wakker());
            BlockPos voet = w(a, VadsniteSessie.BEDDEN.get(0));
            helper.assertTrue(gaLiggen(p, voet.north()), "you lie down too");
        });
        helper.runAtTickTime(VadsniteSessie.SPRING_START + 20, () -> {
            helper.assertTrue(s.uitslag() == VadsniteSessie.Uitslag.GELIJKSPEL && s.gewonnen() && s.wakker() == 0, "Iedereen slaapt. Gelijkspel: " + s.wakker());
            helper.assertTrue(schermenInDoos(level, a) > 0, "the guhs that jumped lie asleep");
            Sessies.verlaat(p, Vertrek.VERLATEN);
            helper.assertTrue(Grappen.isKlaar(p, VadsniteSessie.ID) && Muntjes.saldo(p) == Grappen.BELONING, "a draw counts too");
            helper.assertTrue(schermenInDoos(level, a) == 0 && !p.hasEffect(MobEffects.SLOW_FALLING) && !p.isSleeping(), "everything is tidied up");
            // a new game that is left before anybody landed: no reward, and again nothing stays behind
            VadsniteSessie weer = (VadsniteSessie) Sessies.startOp(level, helper.absolutePos(HOEK), VadsniteSessie.SPEL, List.of(p), new CompoundTag());
            helper.assertTrue(weer != null && weer.wakker() == VadsniteSessie.GUHS + 1, "a fresh bus");
        });
        helper.runAtTickTime(2 * VadsniteSessie.SPRING_START + 40, () -> {
            helper.assertTrue(schermenInDoos(level, a) > 20, "guhs in the air again");
            Sessies.verlaat(p, Vertrek.UITGELOGD);
            helper.assertTrue(schermenInDoos(level, a) == 0 && Grappen.keren(p, VadsniteSessie.ID) == 1, "left in the middle: nothing stays, nothing counts");
            PxTest.klaar(helper, p);
            helper.succeed();
        });
    }

    // --- the lobby NPCs, keepsakes, the Guhdex ---------------------------------------------------------------------------------

    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void spelguhsAandenkensEnGids(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        GuhNpcEntity.Kind[] kinds = {GuhNpcEntity.Kind.SKYBLOK_GUH, GuhNpcEntity.Kind.BEDWARS_GUH, GuhNpcEntity.Kind.VADSNITE_GUH};
        for (int i = 0; i < 3; i++) {
            String id = Grap1Slice.GRAPPEN[i];
            Grap grap = Grappen.van(id);
            helper.assertTrue(grap != null && grap.stappen() == 4 && grap.npc() == kinds[i], "the questline of " + id);
            GuhNpcEntity spelguh = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            spelguh.setKind(kinds[i]);
            helper.assertTrue(Sessies.soort(id) != null && Sessies.soort(id).maxSpelers() == 1 && NpcRollen.van(spelguh) instanceof GrapRol, "its game and its NPC: " + id);
            spelguh.discard();
        }
        for (GuhClothes c : Grap1Slice.MUTSEN) {
            helper.assertTrue("bedwars_aandenken".equals(KledingBronnen.bron(c)) && c.slot == GuhClothes.Slot.HEAD, "the source of " + c);
        }
        helper.assertTrue("vadsnite_aandenken".equals(KledingBronnen.bron(GuhClothes.VADSNITE_PARACHUTERUGZAKJE))
                && GuhClothes.VADSNITE_PARACHUTERUGZAKJE.slot == GuhClothes.Slot.BACK, "the Parachuterugzakje goes on the back");
        // the Skyblok-guh gives a lost bottle again, but only after the game was finished and only when it is really gone
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.SKYBLOK_GUH);
        npc.snapTo(p.getX() + 1, p.getY(), p.getZ(), 0f, 0f);
        level.addFreshEntity(npc);
        NpcRole rol = NpcRollen.rol(GuhNpcEntity.Kind.SKYBLOK_GUH);
        rol.talk(npc, p);
        rol.antwoord(npc, p, GrapRol.KWIJT);
        helper.assertTrue(aantal(p, Grap1Slice.FLES_ITEM.get()) == 0, "nothing to give back before the game was finished");
        helper.assertTrue(Grappen.voltooi(p, SkyblokSessie.ID) && aantal(p, Grap1Slice.FLES_ITEM.get()) == 1, "finished: the bottle");
        rol.antwoord(npc, p, GrapRol.KWIJT);
        helper.assertTrue(aantal(p, Grap1Slice.FLES_ITEM.get()) == 1, "not lost: no second one");
        p.getInventory().clearContent();
        rol.antwoord(npc, p, GrapRol.KWIJT);
        helper.assertTrue(aantal(p, Grap1Slice.FLES_ITEM.get()) == 1, "lost: a new one");
        rol.antwoord(npc, q, GrapRol.KWIJT);
        helper.assertTrue(aantal(q, Grap1Slice.FLES_ITEM.get()) == 0 && !Grappen.isKlaar(q, SkyblokSessie.ID), "another player's progress is their own");
        // playing from the NPC needs the real dimension: here the start is refused politely and nothing changes
        rol.antwoord(npc, q, GrapRol.SPELEN);
        helper.assertTrue(Sessies.van(q) == null, "no guhpixel dimension on the test server: no game");
        // the Guhdex section: three questlines of four steps
        ListTag rijen = GidsBlad.stand(p).getListOrEmpty("Rijen");
        long stappen = PxTest.stappen(rijen, SkyblokSessie.ID, BedwarsSessie.ID, VadsniteSessie.ID);
        helper.assertTrue(stappen == 12, "the Guhdex shows the steps of the three games: " + stappen);
        npc.discard();
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    /** The clothes pieces of this slice exist as items with a texture name of their own (the wardrobe draws them by id). */
    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void kledingstukkenBestaan(GameTestHelper helper) {
        for (GuhClothes c : Grap1Slice.MUTSEN) {
            helper.assertTrue(nl.juiced.guhs.registry.ModItems.clothingItem(c) != null && c.shows("outfit_slaapmutsje")
                    && c.shows("outfit_slaapmutsje_pompon"), "the nightcap bones: " + c);
        }
        GuhClothes rugzak = GuhClothes.VADSNITE_PARACHUTERUGZAKJE;
        helper.assertTrue(nl.juiced.guhs.registry.ModItems.clothingItem(rugzak) != null && rugzak.shows("outfit_vadsnite_rugzak_scherm")
                && !rugzak.shows("outfit_backpack"), "the parachute pack has its own bones");
        helper.succeed();
    }
}
