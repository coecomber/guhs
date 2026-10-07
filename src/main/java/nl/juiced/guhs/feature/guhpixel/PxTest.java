package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Helpers for the game tests of guhpixel. The game test server has no datapack dimensions ({@link Guhpixel#level} is
 * null there), so a test marks its OWN box as guhpixel: the rules and {@link Guhpixel#in} then apply there and nowhere
 * else. Pattern:
 * <pre>
 * PxTest.gebied(helper);
 * ServerPlayer p = PxTest.speler(helper);          // survival, unlocked, standing on the test origin
 * ...
 * PxTest.klaar(helper, p);                         // players away, the clock back to real time, the box forgotten
 * helper.succeed();
 * </pre>
 * Test rooms of the kern: px_test_16, px_test_48, px_test_96 (a floor only; as in every Guhs test the origin lies one block
 * below the template, so the floor is relative y 1 and things stand on relative y 2).
 */
public final class PxTest {
    /** Marks the test's own box (and the air above it) as guhpixel. */
    public static AABB gebied(GameTestHelper helper) {
        AABB doos = helper.getBounds().inflate(0, 4, 0);
        Guhpixel.testGebied(helper.getLevel().dimension(), doos);
        return doos;
    }

    /** A survival mock player who unlocked Guhpixel, standing in the middle of the test room's floor. */
    public static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        AABB b = helper.getBounds();
        p.snapTo((b.minX + b.maxX) / 2, b.minY + 1, (b.minZ + b.maxZ) / 2);
        Muntjes.data(p).putBoolean("Ontgrendeld", true);
        return p;
    }

    /** Skips the clock forward (decimals allowed). */
    public static void spoel(double uren) {
        Klok.spoel(Math.round(uren * Klok.UUR));
    }

    /** The end of a test: stops the players' games, removes the players, the clock back, the test boxes of this level forgotten. */
    public static void klaar(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            Sessies.verlaat(p, Vertrek.GESTOPT);
            GuhQuests.saved(p).remove(Kluis.SLEUTEL);
            Minigames.forget(p);
            if (!p.isRemoved()) {
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            }
        }
        Klok.herstel();
        Guhpixel.testGebiedWeg(helper.getLevel().dimension(), helper.getBounds().inflate(0, 4, 0));
    }

    private PxTest() {
    }

    /**
     * How many step rows the Guhdex page shows for these joke games: the step rows between the heading of that game and
     * the next heading. (A test counts its own games only: the page also holds the sections of every other slice.)
     */
    public static int stappen(net.minecraft.nbt.ListTag rijen, String... grapIds) {
        java.util.Set<String> koppen = new java.util.HashSet<>();
        for (String id : grapIds) {
            koppen.add("gui.guhs." + id + ".grap.naam");
        }
        int n = 0;
        boolean eigen = false;
        for (net.minecraft.nbt.Tag rij : rijen) {
            net.minecraft.nbt.CompoundTag r = (net.minecraft.nbt.CompoundTag) rij;
            String soort = r.getStringOr("T", "");
            if (soort.equals(GidsBlad.KOP)) {
                eigen = nl.juiced.guhs.taal.Tekst.get(r, "A").getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && koppen.contains(tc.getKey());
            } else if (eigen && soort.equals(GidsBlad.STAP)) {
                n++;
            }
        }
        return n;
    }
}
