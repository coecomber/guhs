package nl.juiced.guhs.feature.safari.kern;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.gametest.GuhTest;

/** Op Safari! step 2 (S0): safarimunten, the dex record and the board cooldown, on a fresh (offline) player id. */
public class SafariKernGameTests {
    private static final String BATCH = "safari";

    /** Earning once, daily caps, paying: the balance never goes below 0 and the total never goes down. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void munten(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        AtomicInteger gemeld = new AtomicInteger();
        SafariEvents.opMunten((s, u, n, r) -> {
            if (u.equals(id)) {
                gemeld.addAndGet(n);
            }
        });
        helper.assertTrue(SafariMunten.verdienEens(server, id, "test:eens", 25, "test"), "a key pays the first time");
        helper.assertFalse(SafariMunten.verdienEens(server, id, "test:eens", 25, "test"), "and never again");
        helper.assertTrue(SafariMunten.verdienDagelijks(server, id, "test", 40, 50, "test") == 40, "the daily pot pays 40");
        helper.assertTrue(SafariMunten.verdienDagelijks(server, id, "test", 40, 50, "test") == 10, "then only up to the cap");
        helper.assertTrue(SafariMunten.saldo(server, id) == 75, "25 + 40 + 10");
        helper.assertFalse(SafariMunten.neem(server, id, 100), "too little: nothing taken");
        helper.assertTrue(SafariMunten.neem(server, id, 70) && SafariMunten.saldo(server, id) == 5, "paying 70 leaves 5");
        helper.assertTrue(SafariMunten.totaal(server, id) == 75, "the total does not go down");
        helper.assertTrue(gemeld.get() == 5, "the event saw +75 and -70");
        SafariSpeler.wis(server, id);
        helper.assertTrue(SafariMunten.saldo(server, id) == 0, "wis forgets the coins");
        helper.succeed();
    }

    /** A better photo replaces the record (with its data), a worse one does not; the cooldown runs 7 real days. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dexEnBord(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        CompoundTag foto = new CompoundTag();
        foto.putString("actie", "slaap");
        helper.assertTrue(SafariSpeler.registreerFoto(server, id, "leeuw", 2, foto) == 0, "first photo: was 0");
        CompoundTag slechter = new CompoundTag();
        slechter.putString("actie", "loop");
        helper.assertTrue(SafariSpeler.registreerFoto(server, id, "leeuw", 1, slechter) == 2, "a worse photo keeps the 2");
        SafariSpeler.DexRegel r = SafariSpeler.dex(server, id, "leeuw");
        helper.assertTrue(r.gezien() && r.sterren() == 2 && "slaap".equals(r.foto().getStringOr("actie", "")), "the best photo is kept");
        helper.assertTrue(SafariSpeler.gefotografeerd(server, id).contains("leeuw"), "leeuw is in the dex");
        helper.assertTrue(SafariSpeler.kanBordKopen(server, id, "leeuw"), "a board may be bought");
        SafariSpeler.bordGekocht(server, id, "leeuw");
        helper.assertFalse(SafariSpeler.kanBordKopen(server, id, "leeuw"), "then not for 7 days");
        helper.assertTrue(SafariSpeler.bordVrijOp(server, id, "leeuw") - Klok.nu() > 6 * Klok.DAG, "the cooldown is 7 days");
        helper.assertTrue(SafariSpeler.vindGeheimpje(server, id, "savanne") && !SafariSpeler.vindGeheimpje(server, id, "savanne"),
                "a geheimpje is new only once");
        SafariSpeler.wis(server, id);
        helper.succeed();
    }
}
