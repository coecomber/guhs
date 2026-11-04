package nl.juiced.guhs.feature.vadsrijk.kern.veilig;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Losknuffelen (the unstuck key): the target choice, the cooldown / boss-fight rules and the "shut in" check.
 * Run: {@code runGameTestServer -Pgt=VadsrijkVeilig}.
 */
public class VadsrijkVeiligGameTests {
    private static final String BATCH = "vadsrijk_veilig";

    private static VeiligePlekken.Plek plek(int x, int y, int z, int soort) {
        return new VeiligePlekken.Plek(new BlockPos(x, y, z), soort);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kiesDeDichtstbijzijndeGoedePlek(GameTestHelper helper) {
        Vec3 speler = new Vec3(0.5, 64, 0.5);
        BlockPos fout = new BlockPos(6, 64, 0);
        List<VeiligePlekken.Plek> l = List.of(
                plek(0, 64, 1, VeiligePlekken.GRID),          // where the player is stuck: skipped
                plek(6, 64, 0, VeiligePlekken.GRID),          // nearest, but the world says no
                plek(10, 64, 0, VeiligePlekken.GRID),
                plek(30, 64, 0, VeiligePlekken.POI));
        BlockPos b = VeiligePlekken.kies(l, speler, p -> !p.pos().equals(fout));
        helper.assertTrue(new BlockPos(10, 64, 0).equals(b), "the nearest valid spot (not the stuck spot, not the invalid one): " + b);
        // a campfire/POI a bit further away beats a plain spot (score x0.7), a path spot too (x0.85)
        BlockPos poi = VeiligePlekken.kies(List.of(plek(10, 64, 0, VeiligePlekken.GRID), plek(0, 64, 13, VeiligePlekken.POI)), speler, p -> true);
        helper.assertTrue(new BlockPos(0, 64, 13).equals(poi), "POI preferred when only a little further: " + poi);
        BlockPos ver = VeiligePlekken.kies(List.of(plek(10, 64, 0, VeiligePlekken.GRID), plek(0, 64, 40, VeiligePlekken.POI)), speler, p -> true);
        helper.assertTrue(new BlockPos(10, 64, 0).equals(ver), "but not a far one: " + ver);
        helper.assertTrue(VeiligePlekken.kies(List.of(plek(10, 64, 0, VeiligePlekken.GRID)), speler, p -> false) == null,
                "nothing valid -> null (the caller falls back to the campfire)");
        // the fixed spots (campfires + main POIs of the map) are always there, also without the json
        helper.assertTrue(VeiligePlekken.vasteplekken().size() > 40, "fixed spots: " + VeiligePlekken.vasteplekken().size());
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void cooldownEnBazengevecht(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        long nu = 1000;
        helper.assertTrue(Losknuffelen.mag(id, nu, false, false) == Losknuffelen.Reden.NIET_IN_RIJK, "only in the Vadsrijk");
        helper.assertTrue(Losknuffelen.mag(id, nu, true, true) == Losknuffelen.Reden.IN_GEVECHT, "not in a boss fight");
        helper.assertTrue(Losknuffelen.mag(id, nu, true, false) == Losknuffelen.Reden.OK, "else ok");
        Losknuffelen.zetCooldown(id, nu);
        helper.assertTrue(Losknuffelen.mag(id, nu + 20, true, false) == Losknuffelen.Reden.WACHT, "then a cooldown");
        helper.assertTrue(Losknuffelen.wachtSeconden(id, nu + 20) == 59, "59 s left: " + Losknuffelen.wachtSeconden(id, nu + 20));
        helper.assertTrue(Losknuffelen.mag(id, nu + Losknuffelen.COOLDOWN_TICKS, true, false) == Losknuffelen.Reden.OK, "after 60 s again");
        Losknuffelen.vergeet(id);

        // a real arena with a living boss blocks it; a beaten boss does not
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(2, 1, 2));
        Arena a = new Arena("veilig:test_" + c.asLong(), c, 10, c.east(8), 60, c.east(9), "vadsrijk.kern.baas.klodderreus");
        Arenas.registreer(a, l -> null);
        Vec3 binnen = Vec3.atBottomCenterOf(c.north(3)), buiten = Vec3.atBottomCenterOf(c.north(14));
        helper.assertFalse(Losknuffelen.inGevecht(sl, binnen), "an empty arena is no fight");
        Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.COMMAND);
        k.zetArena(a);
        k.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        sl.addFreshEntity(k);
        helper.assertTrue(Losknuffelen.inGevecht(sl, binnen), "inside the arena with its boss alive");
        helper.assertFalse(Losknuffelen.inGevecht(sl, buiten), "outside the arena it's fine");
        k.discard();
        helper.assertFalse(Losknuffelen.inGevecht(sl, binnen), "boss gone: fine again");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void opgeslotenInEenKuil(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        // a stone block x -2..6, y 1..5, z -2..6; a pit 1x1, 3 deep in the middle
        for (int x = -2; x <= 6; x++) {
            for (int z = -2; z <= 6; z++) {
                for (int y = 1; y <= 5; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
                for (int y = 6; y <= 8; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        for (int y = 3; y <= 5; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.AIR);
        }
        BlockPos kuil = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.assertTrue(Opsluiting.ingesloten(sl, kuil, 3, 500), "3 deep, 1 wide: shut in");
        helper.assertFalse(Losknuffelen.staanplek(sl, helper.absolutePos(new BlockPos(2, 4, 3))), "inside stone is no stand spot");
        helper.assertTrue(Losknuffelen.staanplek(sl, helper.absolutePos(new BlockPos(4, 6, 4))), "on top of the stone is");
        // a ladder makes it escapable
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.LADDER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.SOUTH));
        helper.setBlock(new BlockPos(2, 4, 2), Blocks.LADDER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.SOUTH));
        helper.setBlock(new BlockPos(2, 5, 2), Blocks.LADDER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.SOUTH));
        helper.assertFalse(Opsluiting.ingesloten(sl, kuil, 3, 500), "with a ladder you climb out");
        // a 1-block step out is fine too
        for (int y = 3; y <= 5; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.AIR);
        }
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 4, 2), Blocks.STONE);
        helper.assertFalse(Opsluiting.ingesloten(sl, helper.absolutePos(new BlockPos(2, 5, 2)), 3, 500), "1 deep: you just step out");
        helper.succeed();
    }
}
