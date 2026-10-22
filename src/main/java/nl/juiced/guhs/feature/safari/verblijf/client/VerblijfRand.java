package nl.juiced.guhs.feature.safari.verblijf.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Test round 5: the home's edge, drawn by the client itself (the board menu's "Rand tonen" / "Rand verstoppen"): big pink
 * dust along the edge, on the ground, every few ticks, until switched off, after {@link #DUUR} ticks, or when you walk away.
 * One board at a time. A changed range or shape (the sliders) shows the new edge straight away.
 */
public final class VerblijfRand {
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF5FB4, 1.6f);
    private static final DustParticleOptions HOEK = new DustParticleOptions(0xFFF07A, 2.0f);
    static final int DUUR = 20 * 120;

    @Nullable
    private static BlockPos bord;
    private static boolean rechthoek;
    private static int straal, breedte, diepte;
    private static int over;

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    /** Is the edge of this board showing? */
    static boolean aan(BlockPos pos) {
        return bord != null && bord.equals(pos) && over > 0;
    }

    /** Switches the edge of this board on or off (the menu's button). */
    static void wissel(CompoundTag data) {
        BlockPos pos = BlockPos.of(data.getLongOr("Pos", 0L));
        if (aan(pos)) {
            bord = null;
            over = 0;
        } else {
            toon(data, DUUR);
        }
    }

    /** Shows this board's edge for a while (also after a slider changed it). */
    static void toon(CompoundTag data, int ticks) {
        bord = BlockPos.of(data.getLongOr("Pos", 0L));
        rechthoek = data.getBooleanOr("Rechthoek", false);
        straal = data.getIntOr("Straal", 12);
        breedte = data.getIntOr("Breedte", 12);
        diepte = data.getIntOr("Diepte", 12);
        over = Math.max(over, ticks);
    }

    /** New state of a board (the menu refreshed): a showing edge takes its new shape. */
    static void vernieuw(CompoundTag data) {
        if (aan(BlockPos.of(data.getLongOr("Pos", 0L)))) {
            toon(data, 0);
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (bord == null || over <= 0 || level == null || mc.player == null) {
            return;
        }
        over--;
        if (mc.player.blockPosition().distSqr(bord) > 96 * 96) {
            bord = null;
            over = 0;
            return;
        }
        if (level.getGameTime() % 4 != 0) {
            return;
        }
        int n = rechthoek ? (breedte + diepte) * 4 : (int) Math.ceil(2 * Math.PI * straal);
        for (int i = 0; i < n; i++) {
            int x, z;
            if (rechthoek) {
                double t = i / (double) n * 4;
                int kant = (int) t;
                double f = t - kant;
                x = bord.getX() + switch (kant) {
                    case 0 -> (int) Math.round(-breedte + f * 2 * breedte);
                    case 1 -> breedte;
                    case 2 -> (int) Math.round(breedte - f * 2 * breedte);
                    default -> -breedte;
                };
                z = bord.getZ() + switch (kant) {
                    case 0 -> -diepte;
                    case 1 -> (int) Math.round(-diepte + f * 2 * diepte);
                    case 2 -> diepte;
                    default -> (int) Math.round(diepte - f * 2 * diepte);
                };
            } else {
                double hoek = i / (double) n * Math.PI * 2;
                x = bord.getX() + (int) Math.round(Math.cos(hoek) * straal);
                z = bord.getZ() + (int) Math.round(Math.sin(hoek) * straal);
            }
            if (!level.isLoaded(new BlockPos(x, bord.getY(), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            boolean hoekje = rechthoek && (Math.abs(x - bord.getX()) == breedte && Math.abs(z - bord.getZ()) == diepte);
            level.addParticle(hoekje ? HOEK : ROZE, x + 0.5, y + 0.25, z + 0.5, 0, 0.01, 0);
            level.addParticle(ROZE, x + 0.5, y + 1.1, z + 0.5, 0, 0.01, 0);
        }
    }

    private VerblijfRand() {
    }
}
