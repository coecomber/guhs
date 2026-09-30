package nl.juiced.guhs.feature.theehuis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 2.8 merge QA (cross-slice, see gametest.KnuffeldalKruisGameTests): the theekransje with real products of the other
 * slices. In this package because the kransje's guests are package-private.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class TheehuisKruisGameTests {
    private static final String THEEKAMER = "theehuis_test_kamer";

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(Guhs.id(id)));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** Home-baked at the theekransje: a real bakery pastry counts as zelfgebakken, a real kaasmelkthee as a special tea. */
    @GameTest(template = THEEKAMER)
    public static void theehuisKruisZelfgebakkenUitDeBakkerij(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(10, 2, 10));
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(12, 2, 12));
        npc.setKind(GuhNpcEntity.Kind.THEEGUH);
        GuhEntity a = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 12));
        GuhEntity b = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 12));
        Theekransje k = Theekransje.of(npc);
        try {
            a.tame(p);
            b.tame(p);
            helper.assertTrue(k.start(npc, p), "a theekransje starts");
            for (int i = 0; i <= Theekransje.LOOP_TICKS; i++) {
                k.tick(npc);
            }
            helper.assertTrue(k.gasten.stream().allMatch(g -> g.zit), "everybody sits down");
            Theekransje.Gast ga = k.gast(a), gb = k.gast(b);
            int voor = k.gezelligheid();
            gb.wens = Theekransje.Wens.GEBAK;
            p.setItemInHand(InteractionHand.MAIN_HAND, stack("guhcroissant"));
            Theekransje.klikOpGuh(b, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(k.gezelligheid() == voor + Theekransje.PUNT_ZELFGEBAKKEN && p.getMainHandItem().isEmpty(),
                    "a guhcroissant from the Knabbelbakkerij is zelfgebakken: +" + Theekransje.PUNT_ZELFGEBAKKEN + " (" + (k.gezelligheid() - voor) + ")");
            helper.assertTrue(KnusVoortgang.teller(p, TheehuisVoortgang.ZELFGEBAKKEN) == 1, "the Knus counter for home-baked");
            ga.wens = Theekransje.Wens.THEE;
            int tussen = k.gezelligheid();
            p.setItemInHand(InteractionHand.MAIN_HAND, stack("kaasmelkthee"));
            Theekransje.klikOpGuh(a, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(k.gezelligheid() > tussen, "kaasmelkthee (from the farm's kaasmelk) is poured");
        } finally {
            if (k.isBezig()) {
                k.klaar(helper.getLevel(), false);
            }
            Theekransje.vergeet(npc);
            npc.discard();
            a.discard();
            b.discard();
            leave(helper, p);
        }
        helper.succeed();
    }

}
