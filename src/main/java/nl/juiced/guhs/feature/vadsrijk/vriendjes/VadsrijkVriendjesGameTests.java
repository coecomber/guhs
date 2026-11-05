package nl.juiced.guhs.feature.vadsrijk.vriendjes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;
import nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcRegistry;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * N-EXTRA: the pet nameplate "&lt;pet&gt; van &lt;player&gt;" (a real translatable component with the pet's name key and the player's
 * name, the lang key exists, never the raw type key; Vahoegje too), and the critters around a pink dragon (they gather while a
 * player is near, are never saved/pickable, and all leave once the dragon is gone).
 * Run: {@code runGameTestServer -Pgt=VadsrijkVriendjes}.
 */
public class VadsrijkVriendjesGameTests {
    private static final String BATCH = "vadsrijk_vriendjes";

    private static void naamKlopt(GameTestHelper helper, Component naam, Huisdier h, ServerPlayer p) {
        helper.assertTrue(naam != null && naam.getContents() instanceof TranslatableContents tc && tc.getKey().equals("vadsrijk.huisdieren.naam_van")
                && tc.getArgs().length == 2 && tc.getArgs()[0] instanceof Component a && a.getContents() instanceof TranslatableContents ta
                && ta.getKey().equals(h.naamKey()) && tc.getArgs()[1] instanceof Component b && b.getString().equals(p.getName().getString()),
                "nameplate of " + h.id() + " = naam_van(<pet name>, <player>): " + naam);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void huisdierNaamVanSpeler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Language t = Language.getInstance();
        helper.assertTrue(t.has("vadsrijk.huisdieren.naam_van") && t.getOrDefault("vadsrijk.huisdieren.naam_van").equals("%s van %s"), "lang naam_van");
        Huisdier vahoegje = Huisdieren.van(Z3NpcRegistry.VAHOEGJE);
        helper.assertTrue(vahoegje != null, "Vahoegje is a pet");
        for (Huisdier h : new Huisdier[] {Huisdieren.KONING_KLODDER, vahoegje}) {
            helper.assertTrue(t.has(h.naamKey()), "pet name text " + h.naamKey());
            naamKlopt(helper, Huisdieren.naamVan(h, p.getName()), h, p);
        }
        // the real summon puts it on the entity
        Huisdieren.reset(p);
        try {
            Huisdieren.geef(p, "koning_klodder");
            var e = Huisdieren.roepVoorTest(p, Huisdieren.KONING_KLODDER);
            helper.assertTrue(e != null && e.hasCustomName() && !e.isCustomNameVisible(), "the summoned pet carries its name (shown on look only)");
            naamKlopt(helper, e.getCustomName(), Huisdieren.KONING_KLODDER, p);
            helper.assertTrue(!e.getName().getString().contains("entity.guhs"), "never the raw key: " + e.getName().getString());
            e.discard();
        } finally {
            Huisdieren.reset(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void vriendjesRondRozeDraak(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        ServerLevel sl = helper.getLevel();
        // a stand-in pink dragon (a prop), on the test floor
        BlockPos m = helper.absolutePos(new BlockPos(2, 1, 2));
        Z3Figurant draak = Z3Figurant.maak(sl, Vec3.atBottomCenterOf(m), 0, "vadsrijk/wezens/draak", "textures/vadsrijk/z3bazen/vahoegnir_5.png",
                "vr_z3_baas_vahoegnir", "idle", 1.6f);
        helper.assertTrue(draak != null, "dragon prop");
        DraakVriendjes.volg(draak);
        helper.assertTrue(DraakVriendjes.roze(draak), "registered = pink");
        helper.runAfterDelay(140, () -> {
            int n = DraakVriendjes.aantal(draak);
            helper.assertTrue(n >= 10, "critters gathered around the pink dragon: " + n);
            for (Z3Figurant f : DraakVriendjes.rond(draak)) {
                helper.assertTrue(!f.shouldBeSaved() && !f.isPickable(), "never saved, never in the way");
                helper.assertTrue(f.position().distanceTo(draak.position()) < DraakVriendjes.STRAAL + 40, "near the dragon");
            }
            draak.discard();
        });
        helper.runAfterDelay(320, () -> {
            helper.assertTrue(DraakVriendjes.aantal(draak) == 0, "all gone after the dragon left: " + DraakVriendjes.aantal(draak));
            helper.succeed();
        });
    }
}
