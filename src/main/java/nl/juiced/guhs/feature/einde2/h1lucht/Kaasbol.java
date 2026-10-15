package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 phase B, slice h1_lucht, step 0 (CONTRACT_E2_B 7.2): the kaasbol that washes up at the Knabbelberg. Every player
 * for whom {@code E2Verhaal.magBeginnen} (Opper-Mika beaten) and whose line is at step 0 gets their OWN ball near the place
 * where the portal sets you down ({@code GuheindeGevecht.ARRIVAL_X/Z}), at the shore east of it; only they see it
 * ({@code Zicht.alleenVoor}), so any number of players can open theirs at the same spot. A click: the scene kaasbol, the
 * opening film (intro), then step 1, Appa-guh, and the flight to the Zuidelijke Luchttempel ({@code E2Reizen.vlieg}).
 */
public final class Kaasbol {
    /** How far from the arrival (blocks east) the shore is looked for, and how far from the ball a player gets one. */
    static final int ZOEK = 64, BEREIK = 96;
    /** player -> their ball (not saved: the ball is put down again while they are near). */
    private static final Map<UUID, KaasbolEntity> BOLLEN = new ConcurrentHashMap<>();
    /** The spot of the ball per Guheinde (worked out once the chunks around the arrival are loaded). */
    private static final Map<ServerLevel, BlockPos> PLEK = Collections.synchronizedMap(new WeakHashMap<>());

    private Kaasbol() {
    }

    /** May this player get a ball: Opper-Mika beaten, and the chapter not begun past step 0. */
    public static boolean mag(ServerPlayer p) {
        return E2Verhaal.magBeginnen(p) && E2H1Lucht.LIJN.stap(p) == E2H1Lucht.KAASBOL;
    }

    /**
     * Where the ball lies: from the arrival eastwards over the island to the shore, three blocks back from the edge (the island
     * ends there: the ball has washed up). Null while those chunks are not loaded.
     */
    @Nullable
    public static BlockPos plek(ServerLevel guheinde) {
        BlockPos al = PLEK.get(guheinde);
        if (al != null) {
            return al;
        }
        int z = GuheindeGevecht.ARRIVAL_Z + 6;
        int laatsteX = GuheindeGevecht.ARRIVAL_X + 10, laatsteY = Integer.MIN_VALUE;
        for (int x = GuheindeGevecht.ARRIVAL_X + 4; x <= GuheindeGevecht.ARRIVAL_X + ZOEK; x++) {
            BlockPos kolom = new BlockPos(x, guheinde.getMinY() + 1, z);
            if (!guheinde.isLoaded(kolom)) {
                return null;
            }
            int y = guheinde.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= guheinde.getMinY() + 1) {
                break;                                   // (the void: the shore was one block back)
            }
            laatsteX = x;
            laatsteY = y;
        }
        if (laatsteY == Integer.MIN_VALUE) {
            int x = GuheindeGevecht.ARRIVAL_X + 10;
            laatsteY = guheinde.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            laatsteX = x;
        }
        int x = Math.max(GuheindeGevecht.ARRIVAL_X + 6, laatsteX - 3);
        BlockPos plek = new BlockPos(x, guheinde.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        PLEK.put(guheinde, plek);
        return plek;
    }

    /** (every second, players in the Guheinde) puts this player's ball down when they are near, or takes it away. */
    static void tick(ServerPlayer p) {
        if (!GuheindeFeature.isGuheinde(p.level()) || !mag(p)) {
            weg(p);
            return;
        }
        ServerLevel g = p.level();
        BlockPos plek = plek(g);
        if (plek == null || !plek.closerToCenterThan(p.position(), BEREIK)) {
            weg(p);
            return;
        }
        KaasbolEntity bol = BOLLEN.get(p.getUUID());
        if (bol == null || bol.isRemoved() || bol.level() != g) {
            bol = E2H1LuchtFeature.KAASBOL.get().create(g, EntitySpawnReason.TRIGGERED);
            if (bol == null) {
                return;
            }
            bol.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 200f, 0f);
            Zicht.alleenVoor(bol, p.getUUID());
            Tijdelijk.merk(bol);
            g.addFreshEntity(bol);
            BOLLEN.put(p.getUUID(), bol);
        }
        if (E2H1Lucht.LIJN.eenmalig(p, "kaasbol_melding")) {
            p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "kaasbol.melding").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Takes this player's ball away. */
    static void weg(ServerPlayer p) {
        KaasbolEntity bol = BOLLEN.remove(p.getUUID());
        if (bol != null && !bol.isRemoved()) {
            bol.discard();
        }
    }

    /** (logout, tests) forgets the player. */
    static void vergeet(UUID speler) {
        KaasbolEntity bol = BOLLEN.remove(speler);
        if (bol != null && !bol.isRemoved()) {
            bol.discard();
        }
    }

    /** The ball of this player (null: none). */
    @Nullable
    static KaasbolEntity van(ServerPlayer p) {
        return BOLLEN.get(p.getUUID());
    }

    /** A click on a ball: the two scenes, then {@link #geopend}. */
    static void open(ServerPlayer p, Entity bol) {
        if (BOLLEN.get(p.getUUID()) != bol || !mag(p) || Cutscenes.bezig(p)) {
            return;
        }
        E2H1Lucht.LIJN.begin(p);
        p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "kaasbol.klik").withStyle(ChatFormatting.YELLOW));
        BlockPos anker = bol.blockPosition();
        // the music of the opening film runs under both scenes (CONTRACT_E2_B 5: moment right before, stop in daarna)
        E2Audio.moment(p, "intro");
        if (!Cutscenes.speel(p, H1Scenes.KAASBOL, anker, Rotation.NONE, s -> {
            if (!Cutscenes.speel(s, H1Scenes.INTRO, anker, Rotation.NONE, s2 -> {
                E2Audio.stopMoment(s2, 40);
                geopend(s2);
            })) {
                E2Audio.stopMoment(s, 40);
                geopend(s);
            }
        })) {
            E2Audio.stopMoment(p, 40);
        }
    }

    /** The ball is open (both scenes seen): step 1, the ball goes, Appa-guh is yours, and he flies you to the temple. */
    static void geopend(ServerPlayer p) {
        E2H1Lucht.LIJN.verder(p, E2H1Lucht.KAASBOL);
        weg(p);
        Appa.geef(p);
        vlieg(p);
    }

    /** The story flight to the temple (step 1); when it cannot start yet, a line (the whistle or a next try will do). */
    static void vlieg(ServerPlayer p) {
        if (!E2Reizen.vlieg(p, E2H1Lucht.STRUCTUUR, E2Reizen.Vlucht.START, Tempel::aangekomen)) {
            p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "vlucht.mislukt").withStyle(ChatFormatting.GRAY));
        }
    }
}
