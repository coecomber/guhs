package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.feature.erfenis.koppeling.Makelaarsbrief;
import nl.juiced.guhs.feature.erfenis.sherlock.Guhrrit;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * (fix round 3, i-ui) Makelaar Guhrrit, the Postguh and the rest of the mod's NPCs and followers stay outside the Vadsrijk.
 * Run: {@code runGameTestServer -Pgt=VadsrijkBuitenstaanders}.
 */
public class VadsrijkBuitenstaandersGameTests {
    private static final String BATCH = "vadsrijk_buitenstaanders";

    @GuhTest(template = "empty", batch = BATCH)
    public static void makelaarBlijftBuiten(GameTestHelper helper) {
        // which types belong
        helper.assertTrue(!Buitenstaanders.hoortErbij(ModEntities.GUH_NPC.get()), "a Guhs NPC (makelaar, Postguh, cast) does not belong");
        helper.assertTrue(!Buitenstaanders.hoortErbij(ModEntities.GUH.get()), "a guh from home does not belong");
        helper.assertTrue(Buitenstaanders.hoortErbij(HuisdierenRegistry.HUISDIER.get()), "a Vadsrijk pet belongs");
        helper.assertTrue(Buitenstaanders.hoortErbij(ModEntities.GUH_VIS.get()), "the ambient guh fish belongs");
        helper.assertTrue(Buitenstaanders.hoortErbij(EntityType.PIG), "vanilla entities are left alone");
        // the makelaar himself: refused in the Vadsrijk (spawn and travel), fine everywhere else
        GuhNpcEntity makelaar = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(1, 2, 1));
        makelaar.setKind(GuhNpcEntity.Kind.MAKELAAR_GUHRRIT);
        helper.assertTrue(Buitenstaanders.weigert(Vadsrijk.DIM, makelaar), "the makelaar is refused in the Vadsrijk");
        helper.assertTrue(!Buitenstaanders.weigert(helper.getLevel().dimension(), makelaar), "...but not at home");
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(!Buitenstaanders.weigert(Vadsrijk.DIM, p), "players always belong");
        makelaar.discard();

        // post while inside waits with the home inventory and comes out on the way home
        Inventaris.naarRijk(p);
        int voor = Spelers.data(p).opgeborgen.size();
        Inventaris.legThuis(p, new ItemStack(Items.PAPER));
        helper.assertTrue(!p.getInventory().contains(new ItemStack(Items.PAPER)), "the post is not in the Vadsrijk inventory");
        helper.assertTrue(Spelers.data(p).opgeborgen.size() == voor + 1, "the post waits with the home inventory");
        Inventaris.naarThuis(p);
        helper.assertTrue(p.getInventory().contains(new ItemStack(Items.PAPER)), "back home the post is in the inventory");

        // the real dimension, when this server has it (the gametest server may not)
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        if (vr != null) {
            BlockPos plek = new BlockPos(8, Vadsrijk.GROND + 1, 8);
            vr.getChunk(plek);
            helper.assertTrue(Guhrrit.verschijn(vr, plek) == null, "Guhrrit does not appear in the Vadsrijk");
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(vr, EntitySpawnReason.TRIGGERED);
            npc.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
            helper.assertTrue(!vr.addFreshEntity(npc), "a makelaar spawned in the Vadsrijk is refused");
            GuhNpcEntity buiten = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(1, 2, 1));
            Entity aangekomen = buiten.teleport(new TeleportTransition(vr, Vec3.atBottomCenterOf(plek), Vec3.ZERO, 0f, 0f, Set.<Relative>of(),
                    TeleportTransition.DO_NOTHING));
            helper.assertTrue(aangekomen == null && buiten.isAlive(), "the trip in is cancelled, he stays home");
            buiten.discard();
            ServerLevel thuis = helper.getLevel();
            Vec3 terug = p.position();
            p.teleport(new TeleportTransition(vr, Vec3.atBottomCenterOf(plek), Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
            helper.assertTrue(Roepen.roep(p, UUID.randomUUID()) == Roepen.Uitkomst.NIET_THUIS, "no guh can be called into the Vadsrijk");
            Makelaarsbrief.bezorgPost(p, new ItemStack(Items.PAPER), Component.literal("test"));
            helper.assertTrue(vr.getEntitiesOfClass(GuhNpcEntity.class, p.getBoundingBox().inflate(6)).isEmpty(), "no Postguh in the Vadsrijk");
            p.teleport(new TeleportTransition(thuis, terug, Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        }
        helper.succeed();
    }
}
