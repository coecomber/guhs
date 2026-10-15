package nl.juiced.guhs.feature.einde2.zij;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 zij (CONTRACT_E2_B 8): three focused tests of the slice.
 * <ul>
 *   <li>{@link #einde2ZijKrachten}: the four lasting powers only with their element, the cooldown, the leap and its soft
 *       landing, the earth pillar that rises where you look and crumbles by itself, the paw flame that lights a campfire;</li>
 *   <li>{@link #einde2ZijLijnen}: the element gate, two players at one resident (per player), the Mika cook's line to the
 *       distracted captain, who sees which herd and cook ({@link Zicht});</li>
 *   <li>{@link #einde2ZijRaceHandelBoeken}: the ring crossing, the day's stall of a trader, the books of the owl, the titles.</li>
 * </ul>
 */
public class E2ZijGameTests {
    private static final String BATCH = "einde2_zij";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void vloer(GameTestHelper helper) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                for (int y = 2; y < 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void ruim(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            for (String lijn : ZijLijnen.ALLE) {
                E2VerhaalDev.wis(p, lijn);
            }
            GuhQuests.saved(p).remove(ZijKrachten.DEV_ELEMENTEN);
            ZijEvents.onLogoutVergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean heeftKracht(ServerPlayer p, String id) {
        return Krachten.beschikbaar(p).stream().anyMatch(k -> k.id().equals(id));
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2ZijKrachten(GameTestHelper helper) {
        vloer(helper);
        ServerPlayer p = speler(helper);
        Vec3 voet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 1)));
        p.snapTo(voet.x, voet.y, voet.z, 0f, 28f);
        // nothing learnt: none of the four
        helper.assertTrue(Krachten.beschikbaar(p).stream().noneMatch(k -> k.id().startsWith("zij_")), "no element: no lasting power");
        ZijKrachten.zetDev(p, Einde2Ids.Element.AARDE, true);
        helper.assertTrue(heeftKracht(p, ZijKrachten.AARDZUIL) && !heeftKracht(p, ZijKrachten.LUCHTSPRONG) && !heeftKracht(p, ZijKrachten.HANDVLAM),
                "earth only: only the pillar");
        // the pillar rises on the block the player looks at (three ahead), and only once per cooldown
        helper.assertTrue(Krachten.gebruik(p, ZijKrachten.AARDZUIL), "the pillar is used");
        helper.assertTrue(!Krachten.gebruik(p, ZijKrachten.AARDZUIL) && Krachten.afkoeling(p, ZijKrachten.AARDZUIL) > 0, "then it cools down");
        ZijKrachten.zuilNu(p);
        int zuil = 0;
        for (int dz = 2; dz <= 6; dz++) {
            for (int y = 2; y <= 4; y++) {
                zuil += helper.getBlockState(new BlockPos(3, y, dz)).is(E2ZijFeature.AARDZUIL.get()) ? 1 : 0;
            }
        }
        helper.assertTrue(zuil == ZijKrachten.ZUIL_HOOG, "a pillar of " + ZijKrachten.ZUIL_HOOG + " where the player looks (found " + zuil + ")");
        // the leap: up, cooldown, a soft landing
        ZijKrachten.zetDev(p, Einde2Ids.Element.LUCHT, true);
        p.setOnGround(true);
        helper.assertTrue(Krachten.gebruik(p, ZijKrachten.LUCHTSPRONG) && p.getDeltaMovement().y > 0.5 && ZijKrachten.zachteLanding(p),
                "the air leap throws the player up and the landing will not hurt");
        // the paw flame lights the campfire the player looks at
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        Vec3 bij = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 2, 1)));
        p.snapTo(bij.x, bij.y, bij.z, 0f, 30f);
        ZijKrachten.zetDev(p, Einde2Ids.Element.VUUR, true);
        helper.assertTrue(Krachten.gebruik(p, ZijKrachten.HANDVLAM) && helper.getBlockState(new BlockPos(5, 2, 3)).getValue(CampfireBlock.LIT),
                "the paw flame lights the campfire");
        helper.assertTrue(!ZijKrachten.vlam(p), "lighting something is not carrying a flame");
        helper.runAfterDelay(ZijKrachten.ZUIL_TICKS + 40, () -> {
            int over = 0;
            for (int dz = 2; dz <= 6; dz++) {
                for (int y = 2; y <= 4; y++) {
                    over += helper.getBlockState(new BlockPos(3, y, dz)).is(E2ZijFeature.AARDZUIL.get()) ? 1 : 0;
                }
            }
            helper.assertTrue(over == 0, "the pillar crumbled by itself (" + over + " left)");
            ruim(helper, p);
            helper.succeed();
        });
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, String plek, int x) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        npc.roleData.putString(NpcRollen.PLEK, plek);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 2, 5)));
        npc.snapTo(pos.x, pos.y, pos.z, 0f, 0f);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2ZijLijnen(GameTestHelper helper) {
        vloer(helper);
        ServerPlayer a = speler(helper), b = speler(helper);
        try {
            // the element gate of Omashguh: without earth the king only laughs; with it, a per-player start
            GuhNpcEntity bumi = npc(helper, GuhNpcEntity.Kind.BUMI_GUH, Einde2Ids.OMASHGUH + "/bumi", 1);
            NpcRole rol = NpcRollen.van(bumi);
            helper.assertTrue(rol instanceof Omashguh.Rol, "the king's spot has the role of Omashguh");
            rol.talk(bumi, a);
            helper.assertTrue(ZijLijnen.OMASH.stap(a) == 0 && !ZijLijnen.OMASH.begonnen(a), "no earth: the line does not begin");
            ZijKrachten.zetDev(a, Einde2Ids.Element.AARDE, true);
            rol.talk(bumi, a);
            rol.antwoord(bumi, a, 1);
            helper.assertTrue(ZijLijnen.OMASH.stap(a) == 1 && ZijLijnen.OMASH.stap(b) == 0 && !ZijLijnen.OMASH.begonnen(b),
                    "with earth: trial one for a, nothing for b");
            // the sorry Mika cook: ingredients, then the captain must be distracted the right way
            GuhNpcEntity kok = npc(helper, GuhNpcEntity.Kind.MIKA_KOK, Einde2Ids.MIKA_VESTING + "/kok", 3);
            GuhNpcEntity kapitein = npc(helper, GuhNpcEntity.Kind.VESTINGKAPITEIN_MIKA, Einde2Ids.MIKA_VESTING + "/kapitein", 5);
            E2VerhaalDev.zetTot(a, ZijLijnen.MIKA_KOK, 1);
            NpcRollen.van(kok).talk(kok, a);
            helper.assertTrue(ZijLijnen.KOK.stap(a) == 1, "without the ingredients the cook waits");
            for (Object[] r : MikaKok.INGREDIENTEN) {
                a.getInventory().add(new ItemStack(MikaKok.item((String) r[0]), (Integer) r[1]));
            }
            NpcRollen.van(kok).talk(kok, a);
            helper.assertTrue(ZijLijnen.KOK.stap(a) == 2 && MikaKok.nodig(a).stream().allMatch(n -> n.heb() == 0), "the ingredients go into the pot");
            NpcRollen.van(kapitein).antwoord(kapitein, a, 2);
            NpcRollen.van(kapitein).antwoord(kapitein, a, 3);
            helper.assertTrue(ZijLijnen.KOK.stap(a) == 2, "a crooked hat and a polite question do not distract him");
            NpcRollen.van(kapitein).antwoord(kapitein, b, 1);
            helper.assertTrue(ZijLijnen.KOK.stap(b) == 0, "b is not on that step: nothing happens for b");
            NpcRollen.van(kapitein).antwoord(kapitein, a, 1);
            helper.assertTrue(ZijLijnen.KOK.stap(a) == 3, "a ship without a captain does");
            // who sees which: the stray herd before step 2, the pens from 3 on; the vesting cook until he left
            Zicht.alleenBij(kok, "x", 0, 0);
            Zicht.voorIedereen(kok);
            MikaKok.zicht(kok, Einde2Ids.MIKA_VESTING + "/kok");
            helper.assertTrue(kok.getPersistentData().getStringOr(Zicht.BIJ, "").equals(ZijLijnen.MIKA_KOK + ":0-3"), "the vesting cook: steps 0-3");
            Luchttempel.zicht(bumi, Einde2Ids.WESTELIJKE_LUCHTTEMPEL + "/kudde_2");
            helper.assertTrue(bumi.getPersistentData().getStringOr(Zicht.BIJ, "").equals(ZijLijnen.LUCHTTEMPEL + ":0-1"), "the strays: steps 0-1");
            // the dance follows the beat of the tune (90 bpm: a call every two beats)
            double tel = KyoshiDorp.tel();
            helper.assertTrue(Math.abs(tel - 20.0 * 60 / 90) < 1e-6 && Math.abs(KyoshiDorp.tijdVan(1) - KyoshiDorp.tijdVan(0) - 2 * tel) <= 1,
                    "a call every two beats");
            for (Entity e : List.of(bumi, kok, kapitein)) {
                e.discard();
            }
        } finally {
            ruim(helper, a, b);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2ZijRaceHandelBoeken(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            // a ring is passed when the flight crosses its plane within its radius, in either direction
            Vec3 m = new Vec3(10, 50, 10);
            helper.assertTrue(Ringenrace.door(new Vec3(9, 50, 11), new Vec3(11, 51, 12), m, Direction.Axis.X, 5), "crossing x within the ring");
            helper.assertTrue(Ringenrace.door(new Vec3(11, 50, 10), new Vec3(9, 50, 10), m, Direction.Axis.X, 5), "the other way round too");
            helper.assertTrue(!Ringenrace.door(new Vec3(9, 50, 20), new Vec3(11, 50, 20), m, Direction.Axis.X, 5), "outside the ring: no");
            helper.assertTrue(!Ringenrace.door(new Vec3(8, 50, 10), new Vec3(9.5, 50, 10), m, Direction.Axis.X, 5), "not through the plane: no");
            helper.assertTrue(Ringenrace.door(new Vec3(10, 50, 9), new Vec3(10, 52, 11), m, Direction.Axis.Z, 5), "a ring across z");
            // a trader's stall of the day: the same for everyone that day, three different offers that all exist
            List<Leegtehaven.Ruil> vandaag = Leegtehaven.aanbod(42, 7);
            Set<Leegtehaven.Ruil> anders = new HashSet<>(vandaag);
            helper.assertTrue(vandaag.equals(Leegtehaven.aanbod(42, 7)) && vandaag.size() == Leegtehaven.PER_KRAAM && anders.size() == Leegtehaven.PER_KRAAM,
                    "a stall of the day");
            for (Leegtehaven.Ruil r : Leegtehaven.POOL) {
                helper.assertTrue(Leegtehaven.item(r.geef()) != null && Leegtehaven.item(r.krijg()) != null && (r.geef2() == null || Leegtehaven.item(r.geef2()) != null),
                        "every trade names real items: " + r);
            }
            boolean wisselt = false;
            for (long dag = 43; dag < 53 && !wisselt; dag++) {
                wisselt = !Leegtehaven.aanbod(dag, 7).equals(vandaag);
            }
            helper.assertTrue(wisselt, "the offers change from day to day");
            // the owl's books: our six in order, then the older Guhboeken (never the secret one)
            for (ZijBoeken.Boek boek : ZijBoeken.Boek.values()) {
                ItemStack s = ZijBoeken.volgende(p);
                helper.assertTrue(ZijBoeken.heeft(p, boek) && s.getCustomName() != null, "book " + boek.id);
            }
            for (int i = 0; i < 10; i++) {
                Guhboek g = Guhboek.of(ZijBoeken.volgende(p));
                helper.assertTrue(g != null && !g.secret(), "after the six: an older Guhboek, never the secret one");
            }
            // the titles of the slice
            Titels.Titel race = Titels.van(E2ZijFeature.TITEL_RACE), vesting = Titels.van(E2ZijFeature.TITEL_VESTING);
            helper.assertTrue(race != null && vesting != null && !race.behaald().test(p) && !vesting.behaald().test(p), "two titles, not earned yet");
            GuhQuests.saved(p).putInt(Ringenrace.BESTE, Ringenrace.PAR + 20);
            helper.assertTrue(!race.behaald().test(p), "a slow run: no title");
            GuhQuests.saved(p).putInt(Ringenrace.BESTE, Ringenrace.PAR - 20);
            helper.assertTrue(race.behaald().test(p), "under par: Ring Racer");
        } finally {
            GuhQuests.saved(p).remove(Ringenrace.BESTE);
            GuhQuests.saved(p).remove(ZijBoeken.GEKREGEN);
            ruim(helper, p);
        }
        helper.succeed();
    }
}
