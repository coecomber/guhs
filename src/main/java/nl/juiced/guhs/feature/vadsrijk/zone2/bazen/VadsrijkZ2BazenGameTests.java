package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boorkop;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boormeester;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf.Galmwolf;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.KristalBlokken;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Lichtstraal;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.SpiegelBlok;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelbeeld;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelzucht;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-2 bazen: only the risky rules. The Galmwolf (targets a player at range and starts moving/attacking, the pillar shields
 * and cracks then shatters, a careless howl takes 60 %, the interrupt, phases + blizzard, no damage from outside), Vrouwe
 * Spiegelzucht (only the image without a shadow can be hit, the light cone, the mirror beam hits her only through the folded
 * mirror), the three Spiegelburcht puzzles really solve with their solution and not with their start, and the Mika-boormeester
 * (armour, wrong order = steam, right order = meltdown, defeat = flight on a cart + chests). Run:
 * {@code runGameTestServer -Pgt=VadsrijkZ2Bazen}. Each test has its own batch: the bosses fight every player within 24 blocks, and
 * side-by-side tests would fight each other's mock players.
 */
public class VadsrijkZ2BazenGameTests {
    private static final String BATCH = "vadsrijk_z2_bazen";

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static void vloer(ServerLevel sl, BlockPos c, int r) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
    }

    // ---- De Galmwolf ----------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_galmwolfvechtenpilaren", timeoutTicks = 200)
    public static void galmwolfVechtEnPilaren(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 24);
        Galmwolf w = Z2BazenRegistry.GALMWOLF.get().create(sl, EntitySpawnReason.COMMAND);
        w.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        w.stelIn(24, Element.WATER, false, Set.of());
        sl.addFreshEntity(w);
        // a player at range, 17 blocks away (no melee): he must pick her and start his rotation
        ServerPlayer ver = speler(helper, Vec3.atBottomCenterOf(c).add(-17, 0, 0));
        helper.runAfterDelay(32, () -> {
            // (tests run side by side: other tests' mock players may be nearer, so: she is one of his fighters and he has a target)
            helper.assertTrue(w.strijders().contains(ver) && w.zDoel() != null, "the player at range is in his fight, he has a target");
            helper.assertTrue(w.aanvallenGedaan >= 1, "he started his rotation (" + w.laatsteAanval + ")");
            // from here on the rules are driven by hand: stun him so his own moves don't faint the (level 1) mock player
            Statussen.geef(w, Status.VERDOOFD, 400, 1, null);
            // (neighbouring tests may have put blocks on a pillar spot: pillars only replace air/ice)
            helper.assertTrue(w.intact(sl) >= Galmwolf.pilaren() - 2, "the pillars stand (" + w.intact(sl) + ")");
            w.zetPilaar(sl, 0);   // (a neighbouring test's structure may overlap the spot: put pillar 0 back for sure)
            helper.assertTrue(w.intact(sl, 0), "pillar 0 stands");
            // the shield: stand right behind pillar 0, seen from the wolf
            Vec3 m = Vec3.atBottomCenterOf(w.pilaarBasis(0));
            Vec3 r = m.subtract(Vec3.atBottomCenterOf(c)).multiply(1, 0, 1).normalize();
            Vec3 wp = m.subtract(r.scale(6));   // (the wolf between the middle and the pillar: the test's barrier box stays out of the line)
            w.snapTo(wp.x, wp.y, wp.z, 0, 0);
            Vec3 achter = m.add(r.scale(3.5));
            ver.snapTo(achter.x, achter.y, achter.z);
            Spelers.data(ver).vads = Spelers.maxVads(ver);
            var hit = sl.clip(new net.minecraft.world.level.ClipContext(w.position().add(0, w.getBbHeight() * 0.7, 0), ver.getEyePosition(),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, w));
            helper.assertTrue(w.afgeschermd(sl, ver) == 0, "pillar 0 shields the player (" + w.afgeschermd(sl, ver) + ", wolf " + w.position() + ", player "
                    + ver.position() + ", pillar " + w.pilaarBasis(0) + " " + sl.getBlockState(w.pilaarBasis(0).above(2)) + ", hit " + hit.getType() + " "
                    + hit.getBlockPos() + " " + sl.getBlockState(hit.getBlockPos()) + ")");
            w.galm(sl);
            helper.assertTrue(Spelers.vads(ver) >= Spelers.maxVads(ver) - 0.01, "no damage behind the pillar");
            helper.assertTrue(w.intact(sl, 0) && sl.getBlockState(w.pilaarBasis(0).above()).getValue(
                    nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf.IJspilaarBlok.BARST) == 1, "the pillar cracked");
            w.galm(sl);
            helper.assertTrue(!w.intact(sl, 0), "the second howl shattered it");
            // in the open now
            ver.snapTo(achter.x, achter.y, achter.z);
            double max = Spelers.maxVads(ver);
            Spelers.data(ver).vads = max;
            w.galm(sl);
            helper.assertTrue(Spelers.vads(ver) <= max * (1 - Galmwolf.GALM_FRACTIE) + 0.5, "a careless howl takes 60 % (" + Spelers.vads(ver) + "/" + max + ")");
            // interrupt
            Statussen.wis(w, Status.VERDOOFD);
            ver.snapTo(c.getX() - 5, c.getY(), c.getZ());
            w.startGalm(sl);
            helper.assertTrue(w.actie() == Galmwolf.Actie.GALM, "a howl charges");
            w.krijgSchade(w.maxVads() * 0.035, ver);
            helper.assertTrue(w.onderbroken == 1 && w.actie() == Galmwolf.Actie.GEEN && Statussen.heeft(w, Status.VERDOOFD), "3 % interrupts it");
            // no damage from outside the fight
            ServerPlayer buiten = speler(helper, Vec3.atBottomCenterOf(c).add(40, 0, 0));
            double v = w.vads();
            w.krijgSchade(500, buiten);
            helper.assertTrue(w.vads() == v, "a hit from outside the fight does nothing");
            // phases
            w.krijgSchade(w.vads() - w.maxVads() * 0.59, ver);
            helper.assertTrue(w.fase() == 2, "fase 2 at 60 %");
            w.krijgSchade(w.vads() - w.maxVads() * 0.29, ver);
            helper.assertTrue(w.fase() == 3 && w.blizzard(), "fase 3: the blizzard");
            w.discard();
            helper.assertTrue(!w.intact(sl, 1), "a reset takes the pillars away (restored)");
            helper.succeed();
        });
    }

    // ---- Vrouwe Spiegelzucht -------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_spiegelzuchtschaduwenstraal", timeoutTicks = 200)
    public static void spiegelzuchtSchaduwEnStraal(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 23);
        Spiegelzucht s = Z2BazenRegistry.SPIEGELZUCHT.get().create(sl, EntitySpawnReason.COMMAND);
        s.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        s.stelIn(33, Element.NATUUR, false, Set.of());
        sl.addFreshEntity(s);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 15));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(s.strijders().contains(p) && s.zDoel() != null, "the player at range is in her fight, she has a target");
            helper.assertTrue(s.aanvallenGedaan >= 1, "she started her rotation (" + s.laatsteAanval + ")");
            helper.assertTrue(sl.getBlockState(s.oog(1)).getBlock() instanceof KristalBlokken.Lichtbron
                    && sl.getBlockState(s.spiegel(1)).getBlock() instanceof SpiegelBlok, "the hall has its eyes and mirrors");
            helper.assertTrue(s.straalPad(sl).cellen().size() > 3, "an eye's beam runs into the hall");
            // the split: images have a shadow, she hasn't; hitting an image punishes, hitting her counts
            s.splits(sl);
            var beelden = s.beelden(sl);
            helper.assertTrue(s.gesplitst() && beelden.size() == 3, "fase 1: three images (" + beelden.size() + ")");
            helper.assertTrue(beelden.stream().allMatch(b -> s.verlicht(b.position())), "fase 1: the light falls on every image (all have a shadow)");
            double hp = s.vads(), max = Spelers.maxVads(p);
            Spelers.data(p).vads = max;
            Spiegelbeeld fout = beelden.get(0);
            fout.krijgSchade(80, p);
            helper.assertTrue(fout.isRemoved() && s.beeldenGebroken == 1, "the image shattered");
            helper.assertTrue(Spelers.vads(p) <= max * 0.81, "and struck back at the hitter (" + Spelers.vads(p) + "/" + max + ")");
            helper.assertTrue(s.vads() == hp, "she lost nothing");
            s.krijgSchade(100, p);
            helper.assertTrue(Math.abs(hp - 100 - s.vads()) < 0.01, "the shadowless one is her: hit");
            s.krijgSchade(s.maxVads() * 0.06, p);
            helper.assertTrue(!s.gesplitst() && s.beelden(sl).isEmpty(), "5 % on her while split: found, the images break");
            // fase 2: the rotating light: only images in its cone have a shadow
            s.krijgSchade(s.vads() - s.maxVads() * 0.69, p);
            helper.assertTrue(s.fase() == 2, "fase 2");
            Vec3 licht = s.lichten().get(0);
            helper.assertTrue(s.verlicht(s.zMidden().add(licht.scale(8))), "in the light cone: a shadow");
            helper.assertTrue(!s.verlicht(s.zMidden().subtract(licht.scale(8))), "on the dark side: no shadow (can't tell)");
            // the mirror beam: through a diagonal mirror it misses, through the folded one it blinds her
            Statussen.wis(s, Status.VERDOOFD);
            s.zetPoort(0);
            Vec3 plek = s.straalPlek(0);
            s.snapTo(plek.x, plek.y, plek.z, 0, 0);
            sl.setBlock(s.spiegel(0), Z2BazenRegistry.SPIEGEL.get().defaultBlockState().setValue(SpiegelBlok.STAND, 0), 3);
            s.vuurStraal(sl);
            helper.assertTrue(s.straalMis == 1 && s.straalRaak == 0, "diagonal mirror: the beam turns away");
            sl.setBlock(s.spiegel(0), Z2BazenRegistry.SPIEGEL.get().defaultBlockState().setValue(SpiegelBlok.STAND, 2), 3);
            double voor = s.vads();
            s.vuurStraal(sl);
            helper.assertTrue(s.straalRaak == 1 && Statussen.heeft(s, Status.VERDOOFD), "folded mirror: the beam blinds her");
            helper.assertTrue(voor - s.vads() >= s.maxVads() * 0.079, "and costs her 8 %");
            s.krijgSchade(s.vads() - s.maxVads() * 0.34, p);
            helper.assertTrue(s.fase() == 3 && s.lichten().size() == 2, "fase 3: two lights");
            s.discard();
            helper.assertTrue(sl.getBlockState(s.oog(1)).isAir(), "a reset restores the hall");
            helper.succeed();
        });
    }

    /** The three Spiegelburcht puzzles, built at their real relative layout: the solution lights every lock, the start doesn't. */
    @GuhTest(template = "empty", batch = BATCH + "_spiegelpuzzels")
    public static void spiegelPuzzels(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        // the pure reflection rules
        helper.assertTrue(Lichtstraal.kaats(1, 0) == 0 && Lichtstraal.kaats(0, 0) == 1 && Lichtstraal.kaats(3, 0) == 2 && Lichtstraal.kaats(2, 0) == 3,
                "\"/\": east<->north, west<->south");
        helper.assertTrue(Lichtstraal.kaats(1, 1) == 2 && Lichtstraal.kaats(3, 1) == 0 && Lichtstraal.kaats(0, 1) == 3, "\"\\\": east->south, west->north");
        helper.assertTrue(Lichtstraal.kaats(1, 2) == 1, "flat: straight on");
        for (Z2BazenPlekken.Puzzel pz : Z2BazenPlekken.PUZZELS) {
            for (int poging = 0; poging < 2; poging++) {
                boolean oplossing = poging == 1;
                BlockPos ref = new BlockPos(0, Z2BazenPlekken.BURCHT_Y, pz.z1());
                java.util.function.Function<BlockPos, BlockPos> hier = b -> o.offset(b.getX() - ref.getX() + 16, 0, b.getZ() - ref.getZ() - 1);
                for (int x = -pz.x() - 1; x <= pz.x() + 1; x++) {
                    for (int z = pz.z0() - 1; z <= pz.z1() + 1; z++) {
                        sl.setBlock(hier.apply(new BlockPos(x, ref.getY(), z)), Math.abs(x) == pz.x() + 1 || z == pz.z0() - 1 || z == pz.z1() + 1
                                ? Blocks.POLISHED_TUFF.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int i = 0; i < pz.spiegels().size(); i++) {
                    int st = oplossing && pz.oplossing().get(i) >= 0 ? pz.oplossing().get(i) : pz.startStanden().get(i);
                    sl.setBlock(hier.apply(pz.spiegels().get(i)), Z2BazenRegistry.SPIEGEL.get().defaultBlockState().setValue(SpiegelBlok.STAND, st), 2);
                }
                for (BlockPos s : pz.sloten()) {
                    sl.setBlock(hier.apply(s), Z2BazenRegistry.KRISTALSLOT.get().defaultBlockState(), 2);
                }
                int geraakt = 0;
                for (int i = 0; i < pz.bronnen().size(); i++) {
                    Lichtstraal.Pad pad = Lichtstraal.volg(sl, hier.apply(pz.bronnen().get(i)), pz.richtingen().get(i));
                    if (pad.slot()) {
                        geraakt++;
                    }
                }
                if (oplossing) {
                    helper.assertTrue(geraakt == pz.sloten().size(), pz.id() + ": the solution lights every lock (" + geraakt + ")");
                } else {
                    helper.assertTrue(geraakt < pz.sloten().size(), pz.id() + ": the start is not already solved (" + geraakt + ")");
                }
            }
        }
        helper.succeed();
    }

    // ---- De Mika-boormeester -------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_boormeestervolgorde", timeoutTicks = 260)
    public static void boormeesterVolgorde(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 20);
        Boormeester m = Z2BazenRegistry.BOORMEESTER.get().create(sl, EntitySpawnReason.COMMAND);
        m.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        m.stelIn(32, Element.VUUR, false, Set.of());
        m.zetOntvadst(false);
        sl.addFreshEntity(m);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(16, 0, 0));
        helper.runAfterDelay(2, () -> {
            double v0 = m.vads();
            m.krijgSchade(100, p);
            helper.assertTrue(m.vads() == v0, "asleep: nothing happens");
            helper.assertTrue(!Boormeester.MAG_STARTEN.test(p), "not unlocked for this player yet");
            m.start(sl, p);
            helper.assertTrue(m.toestand() == Boormeester.VECHT && m.kop(0) != null && m.kop(2) != null, "awake, 3 drill heads");
        });
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(m.strijders().contains(p) && m.zDoel() != null, "after the intro the player at range is in his fight, he has a target");
            helper.assertTrue(m.aanvallenGedaan >= 1, "and starts his rotation (" + m.laatsteAanval + ")");
            double v = m.vads();
            m.krijgSchade(100, p);
            helper.assertTrue(Math.abs(v - m.vads() - 100 * Boormeester.PANTSER) < 0.01, "thick armour: 25 %");
            m.zetVolgorde(2, 0, 1);
            Spelers.data(p).vads = Spelers.maxVads(p);
            p.snapTo(m.getX() + 3, m.getY(), m.getZ());
            Boorkop rood = m.kop(0), geel = m.kop(1), blauw = m.kop(2);
            double vol = m.maxVads() * Boormeester.HITTE_PER;
            // wrong: red first
            rood.krijgSchade(vol * 1.01, p);
            helper.assertTrue(m.stoom == 1 && rood.hitte() == 0 && !rood.oververhit(), "wrong order: steam, the heads cool down");
            // right: blue, red, yellow
            double voor = m.vads();
            blauw.krijgSchade(vol * 1.01, p);
            helper.assertTrue(blauw.oververhit() && m.stap() == 1, "blue first: overheated");
            rood.krijgSchade(vol * 1.01, p);
            geel.krijgSchade(vol * 1.01, p);
            helper.assertTrue(m.goedeHitte == 3 && m.smelt() && m.smeltingen == 1, "all three in order: meltdown");
            helper.assertTrue(voor - m.vads() >= m.maxVads() * 0.29, "3 x 10 % (" + (voor - m.vads()) / m.maxVads() + ")");
            double s = m.vads();
            m.krijgSchade(100, p);
            helper.assertTrue(Math.abs(s - m.vads() - 200) < 0.01, "melting: double damage");
            // defeat: no death, a flight on a cart, chests
            m.krijgSchade(m.vads() * 10, p);
            helper.assertTrue(m.isRemoved(), "he is gone (fled)");
            helper.assertTrue(!m.kisten.isEmpty(), "a personal chest for the fighter");
            var karren = sl.getEntitiesOfClass(AbstractMinecart.class, m.getBoundingBox().inflate(6), k -> !k.getPassengers().isEmpty());
            helper.assertTrue(!karren.isEmpty() && karren.get(0).getPassengers().get(0) instanceof Z2Hulpje h && h.vluchter,
                    "the Mika flees on a mine cart");
        });
        helper.runAfterDelay(250, helper::succeed);
    }

    @GuhTest(template = "empty", batch = BATCH + "_z2loot")
    public static void z2Loot(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        for (String baas : new String[] {"galmwolf", "spiegelzucht", "boormeester"}) {
            var r = Z2BazenLoot.kist(baas, p, 33, RandomSource.create(baas.hashCode()));
            boolean legendarisch = false, trofee = false;
            for (ItemStack s : r.items()) {
                Uitrusting u = Rollen.van(s);
                if (u != null) {
                    helper.assertTrue(u.gebonden(), "boss loot is gebonden: " + s);
                    legendarisch |= u.type().startsWith("z2baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
                } else {
                    trofee |= s.is(Z2BazenLoot.trofee(baas).getItem());
                }
            }
            helper.assertTrue(legendarisch && trofee, baas + " first kill: a named Legendary + the trophy");
        }
        long guhddelijk = Z2BazenLoot.BENOEMD.stream().filter(b -> b.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK).count();
        long leg = Z2BazenLoot.BENOEMD.stream().filter(b -> b.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH).count();
        helper.assertTrue(guhddelijk == 2 && leg == 6, "6 Legendary + 2 Guhddelijk");
        ItemStack kroon = Z2BazenLoot.maak(Z2BazenLoot.benoemd("z2baas_spiegelkroon"), 33, RandomSource.create(1));
        helper.assertTrue(Rollen.van(kroon) != null && Rollen.van(kroon).gebonden(), "the Guhddelijk crown rolls and is gebonden");
        helper.succeed();
    }
}
