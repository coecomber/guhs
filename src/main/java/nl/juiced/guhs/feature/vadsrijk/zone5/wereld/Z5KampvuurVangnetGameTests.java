package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones;
import nl.juiced.guhs.feature.vadsrijk.zone4.verhaal.Z4VerhaalPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone5.droom.Z5Droom;
import nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5VerhaalPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5Bouw;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6VerhaalPlekken;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Campfire travel against the fall nets (1.10.31 report, Luc: "from zone 5 to the Vuurtoreneiland I am put back in zone 5", with
 * the overlay of the Wolkvangnet). The campfires of zone 4, 5 and 6 with their real spots; arriving at any of them by campfire
 * travel (from any other one) is never followed by a net or a zone push-back: the Wolkvangnet ({@link Z5WereldRegistry#vangnet}),
 * the fin net ({@link Z5Bouw#vangplek}), the dream cloud's net ({@link Z5Droom}) and the zone rule ({@link ZonePoort#zoneOp}).
 * A real fall off the whale is still caught. Run: {@code runGameTestServer -Pgt=Z5KampvuurVangnet}.
 */
public class Z5KampvuurVangnetGameTests {
    private static final String BATCH = "vadsrijk_z5kampvuur_vangnet";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("guhs");

    private record Vuur(int zone, String naam, BlockPos plek) {
    }

    /** The campfires of the Verkenningsboek pages of zone 4, 5 and 6 (the same lists the pages are built from). */
    private static List<Vuur> vuren() {
        List<Vuur> l = new ArrayList<>();
        Z4VerhaalPlekken.KAMPVUUR.forEach((g, b) -> l.add(new Vuur(4, g, b)));
        Map<BlockPos, String> z5 = new LinkedHashMap<>();
        Z5VerhaalPlekken.KAMPVUUR.forEach((g, b) -> z5.put(b, g));
        for (BlockPos b : Z5VerhaalPlekken.KAMPVUREN) {
            l.add(new Vuur(5, z5.getOrDefault(b, "z5"), b));
        }
        Map<BlockPos, String> z6 = new LinkedHashMap<>();
        Z6VerhaalPlekken.KAMPVUUR.forEach((g, b) -> z6.put(b, g));
        for (BlockPos b : Z6VerhaalPlekken.KAMPVUREN) {
            l.add(new Vuur(6, z6.getOrDefault(b, "z6"), b));
        }
        return l;
    }

    /** Every spot campfire travel can put you at this campfire (KampvuurReizen.veiligePlek: up to 2 out, one up or down). */
    private static List<Vec3> aankomsten(BlockPos vuur) {
        List<Vec3> l = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx != 0 || dz != 0) {
                        l.add(new Vec3(vuur.getX() + dx + 0.5, vuur.getY() + dy, vuur.getZ() + dz + 0.5));
                    }
                }
            }
        }
        return l;
    }

    /** The Wolkvangnet's box and height (what the old rule looked at, besides "on the whale less than 40 s ago"). */
    private static boolean inNetvak(Vec3 v) {
        long nu = 1000;
        return Z5WereldRegistry.vangen(v.x, v.y, v.z, -0.5, nu, nu - 1);
    }

    /** The static rules: the real spots, the zone of every campfire, and no fin net / dream net at any arrival spot. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void aankomstBuitenDeNetten(GameTestHelper helper) {
        List<Vuur> vuren = vuren();
        helper.assertTrue(vuren.stream().filter(v -> v.zone() == 4).count() >= 8, "zone 4 has its campfires");
        helper.assertTrue(vuren.stream().filter(v -> v.zone() == 5).count() >= 4, "zone 5 has its campfires");
        helper.assertTrue(vuren.stream().filter(v -> v.zone() == 6).count() >= 7, "zone 6 has its campfires");
        List<String> inVak = new ArrayList<>();
        for (Vuur v : vuren) {
            BlockPos b = v.plek();
            boolean vak = false;
            for (Vec3 a : aankomsten(b)) {
                // the 3D zone rule files the campfire under its own zone (so the zone gate treats the arrival like the page)
                helper.assertTrue(ZonePoort.zoneOp(a.x, a.y, a.z) == v.zone(), v.naam() + " " + b + ": arrival " + a + " is zone "
                        + ZonePoort.zoneOp(a.x, a.y, a.z) + ", not " + v.zone());
                VadsrijkZones.Zone zo = VadsrijkZones.zoneVan(a.x, a.y, a.z);
                helper.assertTrue(zo != null && zo.id.equals("z" + v.zone()), v.naam() + ": zoneVan = " + zo);
                // the fin net (Z5Bouw.tick: only from 60 below zone 5's floor, then the boxes under the fins and the tail)
                helper.assertTrue(!(Z5WezensPlekken.inZone(a.x, a.y + 60, a.z) && Z5Bouw.vangplek(a.x, a.y, a.z) != null),
                        v.naam() + ": arrival " + a + " is inside the fin net");
                // the dream cloud's net (within 40 blocks, 6..120 below the cloud)
                double dx = a.x - (Z5Droom.PLEK.getX() + 0.5), dz = a.z - (Z5Droom.PLEK.getZ() + 0.5);
                helper.assertTrue(dx * dx + dz * dz > 40 * 40 || a.y > Z5Droom.PLEK.getY() - 6 || a.y < Z5Droom.PLEK.getY() - 120,
                        v.naam() + ": arrival " + a + " is inside the dream cloud's net");
                vak |= inNetvak(a);
            }
            if (vak) {
                inVak.add(v.naam());
            }
            LOG.info("Z5KampvuurVangnet: zone {} campfire {} at {} {} {}{}", v.zone(), v.naam(), b.getX(), b.getY(), b.getZ(),
                    vak ? "  <- inside the Wolkvangnet's box (below the whale)" : "");
        }
        // why the report happened: these lie below the whale, inside the Wolkvangnet's box. The net must tell them from a fall.
        helper.assertTrue(inVak.contains("z4_vuurtoreneiland"), "the Vuurtoreneiland campfire lies inside the net's box: " + inVak);
        LOG.info("Z5KampvuurVangnet: campfires inside the Wolkvangnet's box: {}", inVak);
        // every trip (any campfire -> any other, zones 4-6, both directions) is a teleport for the net, never "the same fall"
        int paren = 0;
        for (Vuur van : vuren) {
            for (Vuur naar : vuren) {
                if (van == naar) {
                    continue;
                }
                paren++;
                for (Vec3 a : aankomsten(van.plek())) {
                    for (Vec3 n : aankomsten(naar.plek())) {
                        helper.assertTrue(!Z5WereldRegistry.zelfdeVal(a, n, false, false), van.naam() + " -> " + naar.naam() + ": " + a + " -> " + n
                                + " still counts as a fall off the whale");
                    }
                }
            }
        }
        LOG.info("Z5KampvuurVangnet: {} campfire pairs checked", paren);
        helper.succeed();
    }

    /** The pure rule: a fall stays a fall; a teleport, landing or swimming below the net ends it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zelfdeValRegel(GameTestHelper helper) {
        Vec3 rand = new Vec3(2868.5, 204, -470.5);
        helper.assertTrue(Z5WereldRegistry.zelfdeVal(null, rand, false, false), "the first tick: nothing known, still counts");
        helper.assertTrue(Z5WereldRegistry.zelfdeVal(rand, rand.add(0.3, -3.9, 0.2), false, false), "falling at full speed: the same fall");
        helper.assertTrue(Z5WereldRegistry.zelfdeVal(rand, rand.add(2, -20, 2), false, false), "a laggy tick of falling: the same fall");
        helper.assertTrue(Z5WereldRegistry.zelfdeVal(rand, rand.add(0.2, 0, 0), true, false), "walking on the whale: nothing ends");
        helper.assertTrue(Z5WereldRegistry.zelfdeVal(new Vec3(2700, 140, -500), new Vec3(2700.2, 140, -500), true, false),
                "standing on a ledge under the deck but above the net: still counts");
        helper.assertTrue(!Z5WereldRegistry.zelfdeVal(rand, rand.add(20, 0, 0), false, false), "20 blocks sideways in a tick: a teleport");
        helper.assertTrue(!Z5WereldRegistry.zelfdeVal(new Vec3(2700, 100, -500), new Vec3(2700, 220, -500), false, false), "straight up: a teleport");
        helper.assertTrue(!Z5WereldRegistry.zelfdeVal(new Vec3(2404, 83, -486), new Vec3(2404.2, 82, -486), true, false),
                "standing below the net (an island of zone 4): not falling off the whale");
        helper.assertTrue(!Z5WereldRegistry.zelfdeVal(new Vec3(2500, 64, -500), new Vec3(2500.1, 63.5, -500), false, true),
                "swimming in the sea below the whale: not falling off the whale");
        helper.succeed();
    }

    /**
     * The real net with a player: stand at every zone-5 campfire, travel (a teleport, as KampvuurReizen.reis does) to every
     * campfire of zone 4 and 6, then step down a slope there (falling faster than the net's 0.2): never caught. From the same
     * campfire a real fall over the edge IS caught and set back on the whale.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void reisWordtNietGevangen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 thuis = p.position();
        List<Vuur> vuren = vuren();
        long t = 10_000;   // (a multiple of 10: the net notes the safe spot)
        int reizen = 0;
        try {
            for (Vuur van : vuren) {
                if (van.zone() != 5) {
                    continue;
                }
                Vec3 start = Vec3.atBottomCenterOf(van.plek().south(2));
                for (Vuur naar : vuren) {
                    if (naar.zone() == 5) {
                        continue;
                    }
                    Z5WereldRegistry.vergeet(p.getUUID());
                    // standing at the zone-5 campfire: the net remembers it as the last safe spot on the whale
                    p.snapTo(start.x, start.y, start.z);
                    p.setDeltaMovement(Vec3.ZERO);
                    p.setOnGround(true);
                    t += 10;
                    Z5WereldRegistry.vangnet(sl, p, t);
                    t += 10;
                    Z5WereldRegistry.vangnet(sl, p, t);
                    helper.assertTrue(Z5WereldRegistry.laatstVeilig(p.getUUID()) != null, van.naam() + ": the net knows the safe spot on the whale");
                    // the trip; right after it the server still thinks "on the ground" or not, either way
                    Vec3 aankomst = Vec3.atBottomCenterOf(naar.plek().east(2));
                    for (boolean grond : new boolean[] {true, false}) {
                        if (!grond) {
                            Z5WereldRegistry.vergeet(p.getUUID());
                            p.snapTo(start.x, start.y, start.z);
                            p.setOnGround(true);
                            t += 10;
                            Z5WereldRegistry.vangnet(sl, p, t);
                            t += 10;
                            Z5WereldRegistry.vangnet(sl, p, t);
                        }
                        p.snapTo(aankomst.x, aankomst.y, aankomst.z);
                        p.setOnGround(grond);
                        // ... and stepping down the slope next to the campfire for a second (the fall the old rule caught)
                        Vec3 nu = aankomst;
                        for (int i = 0; i < 20; i++) {
                            p.setDeltaMovement(0, -0.5, 0);
                            t++;
                            Z5WereldRegistry.vangnet(sl, p, t);
                            helper.assertTrue(p.position().distanceTo(nu) < 0.01, van.naam() + " -> " + naar.naam() + " (" + naar.plek()
                                    + "): the net moved the traveller to " + p.position());
                            nu = nu.add(0.2, -0.5, 0);
                            p.snapTo(nu.x, nu.y, nu.z);
                            p.setOnGround(false);
                        }
                        helper.assertTrue(Z5WereldRegistry.laatstVeilig(p.getUUID()) == null, van.naam() + " -> " + naar.naam()
                                + ": the safe spot on the whale is forgotten after the trip");
                    }
                    reizen++;
                }
            }
            LOG.info("Z5KampvuurVangnet: {} trips zone 5 -> zone 4/6 with the real net, none caught", reizen);
            helper.assertTrue(reizen >= 4 * 15, "all trips tried: " + reizen);
            // the control: a real fall from a zone-5 campfire over the edge is still caught (the net is not simply off)
            BlockPos park = Z5VerhaalPlekken.KAMPVUUR.get("z5_staartpark");
            Vec3 start = Vec3.atBottomCenterOf(park.south(2));
            Z5WereldRegistry.vergeet(p.getUUID());
            p.snapTo(start.x, start.y, start.z);
            p.setDeltaMovement(Vec3.ZERO);
            p.setOnGround(true);
            t += 10 - t % 10;
            Z5WereldRegistry.vangnet(sl, p, t);
            BlockPos veilig = Z5WereldRegistry.laatstVeilig(p.getUUID());
            helper.assertTrue(veilig != null, "the control: the safe spot is known");
            Vec3 nu = start;
            boolean gevangen = false;
            for (int i = 0; i < 60 && !gevangen; i++) {
                nu = nu.add(0.1, -3, 0.1);
                p.snapTo(nu.x, nu.y, nu.z);
                p.setOnGround(false);
                p.setDeltaMovement(0, -3, 0);
                t++;
                Z5WereldRegistry.vangnet(sl, p, t);
                gevangen = p.position().distanceTo(nu) > 1;
                helper.assertTrue(gevangen || nu.y > Z5WereldRegistry.VANG_Y - 4, "a real fall is caught at the net's height, not at y " + nu.y);
            }
            helper.assertTrue(gevangen, "a real fall off the whale is still caught");
            BlockPos terug = Z5WereldRegistry.dichtstbijzijnde(veilig);
            helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(terug)) < 1, "set back on the whale at " + terug + ": " + p.position());
        } finally {
            Z5WereldRegistry.vergeet(p.getUUID());
            p.snapTo(thuis.x, thuis.y, thuis.z);
            p.setDeltaMovement(Vec3.ZERO);
        }
        helper.succeed();
    }
}
