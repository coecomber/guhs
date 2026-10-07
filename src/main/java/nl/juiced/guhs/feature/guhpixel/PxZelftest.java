package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.guhpixel.blok.PortaalBlock;
import org.slf4j.Logger;

/**
 * The dev-server self test of the real dimension ({@code /guhs px zelftest}, also from the server console; no player
 * needed). Every slice registers checks with {@link #registreer}; each check reports lines through its {@link Melder}.
 * The command prints one line per report, "[px-zelftest] &lt;name&gt;: OK ..." or "[px-zelftest] &lt;name&gt;: FOUT ...", and a
 * last line "[px-zelftest] klaar: n OK, m FOUT".
 */
public final class PxZelftest {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** What a check reports. */
    public interface Melder {
        void ok(String tekst);

        void fout(String tekst);

        /** ok when {@code goed}, else fout. */
        default void check(boolean goed, String tekst) {
            if (goed) {
                ok(tekst);
            } else {
                fout(tekst);
            }
        }
    }

    /** One check; level is the guhpixel dimension (never null here). */
    @FunctionalInterface
    public interface Check {
        void test(MinecraftServer server, ServerLevel level, Melder meld);
    }

    private static final Map<String, Check> CHECKS = new LinkedHashMap<>();

    public static void registreer(String naam, Check check) {
        CHECKS.put(naam, check);
    }

    /** Runs every check; returns the report lines (the last one is the summary). */
    public static List<String> draai(MinecraftServer server) {
        List<String> uit = new ArrayList<>();
        int[] tel = new int[2];
        ServerLevel level = Guhpixel.level(server);
        if (level == null) {
            uit.add("[px-zelftest] kern: FOUT the dimension guhs:guhpixel does not exist");
            tel[1]++;
        } else {
            for (Map.Entry<String, Check> c : CHECKS.entrySet()) {
                String naam = c.getKey();
                Melder meld = new Melder() {
                    @Override
                    public void ok(String tekst) {
                        uit.add("[px-zelftest] " + naam + ": OK " + tekst);
                        tel[0]++;
                    }

                    @Override
                    public void fout(String tekst) {
                        uit.add("[px-zelftest] " + naam + ": FOUT " + tekst);
                        tel[1]++;
                    }
                };
                try {
                    c.getValue().test(server, level, meld);
                } catch (RuntimeException e) {
                    LOGGER.error("Guhpixel zelftest: {} threw", naam, e);
                    meld.fout("exception " + e);
                }
            }
        }
        uit.add("[px-zelftest] klaar: " + tel[0] + " OK, " + tel[1] + " FOUT");
        for (String regel : uit) {
            LOGGER.info(regel);
        }
        return uit;
    }

    /** The kern's own checks: the dimension, the lobby, the anchors, the exit portal, the return maths, the arenas. */
    static void kern() {
        registreer("kern", (server, level, meld) -> {
            meld.ok("dimension " + level.dimension().identifier());
            meld.check(Lobby.zorg(level), "lobby stamped (version " + Lobby.gebouwd(server) + " of " + Lobby.versie(server) + ")");
            Vec3i maat = Stempel.maat(level, Lobby.TEMPLATE);
            meld.check(maat != null && maat.getX() == Guhpixel.LOBBY_BREEDTE && maat.getZ() == Guhpixel.LOBBY_BREEDTE && maat.getY() <= Guhpixel.LOBBY_HOOGTE,
                    "lobby template size " + maat);
            Stempel.laad(level, Guhpixel.LOBBY_MIN, new Vec3i(Guhpixel.LOBBY_BREEDTE, 1, Guhpixel.LOBBY_BREEDTE));
            for (LobbyPlek plek : LobbyPlek.values()) {
                String mis = pad(level, plek);
                meld.check(mis == null, "anchor " + plek + (mis == null ? " pad is solid and free" : ": " + mis));
            }
            int portaal = 0;
            for (int x = -1; x <= 1; x++) {
                for (int y = 100; y <= 102; y++) {
                    BlockState s = level.getBlockState(new BlockPos(x, y, 30));
                    if (s.is(GuhpixelFeature.PORTAAL.get()) && s.getValue(PortaalBlock.SOORT) == PortaalBlock.Soort.UIT) {
                        portaal++;
                    }
                }
            }
            meld.check(portaal == 9, "exit portal blocks at x -1..1, y 100..102, z 30: " + portaal + " of 9");
            meld.check(LobbyPlek.SPAWN.pos().x == 0.5 && LobbyPlek.SPAWN.pos().y == 100 && Guhpixel.inLobby(level, LobbyPlek.SPAWN.blok()),
                    "spawn point " + LobbyPlek.SPAWN.pos() + " lies in the lobby box");
            meld.check(Guhpixel.celVan(Guhpixel.celOorsprong(65)) == 65 && Guhpixel.celOorsprong(65).equals(new BlockPos(4096 + 512, 64, 512)),
                    "arena grid maths (cell 65 at " + Guhpixel.celOorsprong(65) + ")");
            meld.check(!Guhpixel.inLobby(level, Guhpixel.celOorsprong(0)), "arena cell 0 is outside the lobby");
            meld.ok("lobby NPCs standing: " + LobbyNpcs.controleer(level) + " of " + LobbyNpcs.aantal() + " registered");
        });
        registreer("arenas", (server, level, meld) -> {
            if (Sessies.soorten().isEmpty()) {
                meld.ok("no games registered yet");
            }
            for (SpelSoort soort : Sessies.soorten()) {
                Vec3i maat = Stempel.maat(level, soort.arena().template());
                if (maat == null) {
                    meld.fout(soort.id() + ": template " + soort.arena().template() + " is missing");
                    continue;
                }
                meld.check(maat.equals(soort.arena().maat()), soort.id() + ": template size " + maat + " = declared " + soort.arena().maat());
                Arena a = Arenas.neem(level, soort.arena());
                if (a == null) {
                    meld.fout(soort.id() + ": no arena");
                    continue;
                }
                BlockPos start = BlockPos.containing(a.start());
                boolean vloer = !level.getBlockState(start.below()).isAir() || !level.getBlockState(start.below(2)).isAir();
                meld.check(vloer, soort.id() + ": cell " + a.cel() + " at " + a.oorsprong() + ", a floor under the start " + start);
                Arenas.geefTerug(a);
                meld.check(!Arenas.bezet(a.cel()), soort.id() + ": the cell is free again");
            }
        });
    }

    /** Null when the 5 x 5 pad of this anchor has a solid floor at y-1 and 4 free blocks above; else what is wrong. */
    @Nullable
    static String pad(ServerLevel level, LobbyPlek plek) {
        BlockPos p = plek.blok();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos vloer = p.offset(dx, -1, dz);
                if (!level.getBlockState(vloer).isCollisionShapeFullBlock(level, vloer)) {
                    return "no solid floor at " + vloer.toShortString();
                }
                for (int dy = 0; dy < 4; dy++) {
                    BlockPos vrij = p.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(vrij);
                    if (!s.getCollisionShape(level, vrij).isEmpty()) {
                        return "blocked at " + vrij.toShortString() + " by " + s.getBlock();
                    }
                }
            }
        }
        return null;
    }

    private PxZelftest() {
    }
}
