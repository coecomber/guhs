package nl.juiced.guhs.quest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.4.2 (fixes, bug 5): a guh portal makes its Reisguh only once ever. The test server has no Guhmension, so this asks
 * {@link Reisguh#nearPortalIn} in the test level (the same code {@link Reisguh#nearPortal} runs in the Guhmension).
 */
public class ReisguhPortaalGameTests {

    private static List<GuhNpcEntity> reisguhs(ServerLevel level, BlockPos portaal) {
        return level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(portaal).inflate(8),
                n -> n.getKind() == GuhNpcEntity.Kind.REISGUH && !n.isRemoved());
    }

    @GuhTest(template = "empty", batch = "reisguh_portaal")
    public static void reisguhPortaalMaarEenKeer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portaal = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlockAndUpdate(portaal.offset(dx, -1, dz), Blocks.PINK_WOOL.defaultBlockState());
            }
        }
        ReisguhPortalen gehad = ReisguhPortalen.van(level);
        gehad.vergeet(portaal);
        try {
            Reisguh.nearPortalIn(level, portaal);
            Reisguh.nearPortalIn(level, portaal);          // (building a portal and arriving both ask)
            List<GuhNpcEntity> eerst = reisguhs(level, portaal);
            helper.assertTrue(eerst.size() == 1, "a new portal gets one Reisguh: " + eerst.size());
            helper.assertTrue(gehad.gehad(portaal) && gehad.gehad(portaal.offset(1, 2, 0)), "the portal is remembered (every block of it)");
            // the player picks her up (she is gone) and goes through the portal again, a few times
            eerst.forEach(n -> n.discard());
            for (int i = 0; i < 3; i++) {
                Reisguh.nearPortalIn(level, portaal);
                Reisguh.nearPortalIn(level, portaal.offset(1, 1, 0));
            }
            helper.assertTrue(reisguhs(level, portaal).isEmpty(), "no new Reisguh after picking her up: " + reisguhs(level, portaal).size());

            // a portal from before 1.4.2 (not on the list) whose Reisguh still stands: no second one, and remembered
            gehad.vergeet(portaal);
            Reisguh.place(level, portaal.offset(3, 0, 2), 0, Component.translatable("quest.guhs.reis.portal_name", portaal.getX() + 3, portaal.getZ() + 2));
            Reisguh.nearPortalIn(level, portaal);
            helper.assertTrue(reisguhs(level, portaal).size() == 1 && gehad.gehad(portaal), "an old portal with its Reisguh: no second one, remembered");
            reisguhs(level, portaal).forEach(n -> n.discard());
            Reisguh.nearPortalIn(level, portaal);
            helper.assertTrue(reisguhs(level, portaal).isEmpty(), "and after picking that one up: none either");

            // a portal Reisguh carried off before 1.4.2 is known by her automatic name (the spot she was made at)
            helper.assertTrue(ReisguhPortalen.portaalNaamBij(Component.translatable("quest.guhs.reis.portal_name", portaal.getX() + 3, portaal.getZ() - 2), portaal),
                    "her name says she came from this portal");
            helper.assertTrue(!ReisguhPortalen.portaalNaamBij(Component.translatable("quest.guhs.reis.portal_name", portaal.getX() + 40, portaal.getZ()), portaal)
                    && !ReisguhPortalen.portaalNaamBij(Component.literal("Guhstation"), portaal), "other names do not");
            helper.assertTrue(ReisguhPortalen.portaalNaamBij(Reisguh.vanOud(Component.literal("Guhportaal (" + (portaal.getX() - 4) + ", " + portaal.getZ() + ")")), portaal),
                    "a name saved before 1.2.0 too");
            // another portal further away is a portal of its own
            helper.assertTrue(!gehad.gehad(portaal.offset(30, 0, 0)), "a portal 30 blocks away is another portal");
        } finally {
            gehad.vergeet(portaal);
            reisguhs(level, portaal).forEach(n -> n.discard());
        }
        helper.succeed();
    }
}
