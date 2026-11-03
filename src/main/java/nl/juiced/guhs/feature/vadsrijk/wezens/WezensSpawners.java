package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The zone-1 spawners (CONTRACT_VADSRIJK 5, 13), all in kern's {@link Spawners}. Surface areas: open air (heightmap). Caves and
 * the temple rooms: kern's cave mode ({@link Spawners.Spawner#grot}) searching the floor from just above the centre down to 24
 * below (so never the cave roof). The mountain path: cave mode over a wide y-range (= the top surface, "player near" measured flat).
 * (Integration 1.10.0 lifted the slice's own floor-search spawner onto kern.)
 * <p>Balance: "lively, not crowded": 2-5 alive per spawner, 25-45 s respawn, 6 % elites (the Moederklodder is always one).
 */
public final class WezensSpawners {
    public static final double ELITE = 0.06;
    private static int oppervlak, vloer;

    private WezensSpawners() {
    }

    private static void opp(String naam, BlockPos[] plekken, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int straal, int max, int respawn,
            int min, int maxLvl, Element e, double elite) {
        for (int i = 0; i < plekken.length; i++) {
            Spawners.registreer(new Spawners.Spawner("wezens:" + naam + "_" + i, plekken[i], straal, type, max, respawn, min, maxLvl, e, elite));
            oppervlak++;
        }
    }

    private static void vloer(String naam, BlockPos[] plekken, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int straal, int max, int respawn,
            int min, int maxLvl, Element e, double elite) {
        for (int i = 0; i < plekken.length; i++) {
            Spawners.registreer(grot(new Spawners.Spawner("wezens:" + naam + "_" + i, plekken[i], straal, type, max, respawn, min, maxLvl, e, elite)));
            vloer++;
        }
    }

    /** The cave/room form of a spawner: floor search from y+1 down to y-24 (a roof two blocks up is never found). */
    public static Spawners.Spawner grot(Spawners.Spawner s) {
        return s.grot(s.centrum().getY() - 24, s.centrum().getY() + 1);
    }

    /** The mountain path: any height around the centre (the first floor from above = the surface), "player near" measured flat. */
    private static void berg(String naam, BlockPos[] plekken, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int straal, int max, int respawn,
            int min, int maxLvl, Element e, double elite) {
        for (int i = 0; i < plekken.length; i++) {
            int y = plekken[i].getY();
            Spawners.registreer(new Spawners.Spawner("wezens:" + naam + "_" + i, plekken[i], straal, type, max, respawn, min, maxLvl, e, elite)
                    .grot(y - 60, y + 60));
            vloer++;
        }
    }

    /** From WezensRegistry.register. */
    public static void registreer() {
        // Klodderweide, lv 1-6
        opp("klodder", WezensPlekken.KLODDER, WezensRegistry.KLODDER, 18, 4, 600, 1, 4, Element.WATER, ELITE);
        opp("bubbelklodder", WezensPlekken.BUBBELKLODDER, WezensRegistry.BUBBELKLODDER, 16, 3, 700, 2, 5, Element.WATER, ELITE);
        opp("stuiterklodder", WezensPlekken.STUITERKLODDER, WezensRegistry.STUITERKLODDER, 16, 3, 700, 3, 6, Element.WATER, ELITE);
        opp("moederklodder", WezensPlekken.MOEDERKLODDER, WezensRegistry.MOEDERKLODDER, 10, 1, 3000, 5, 6, Element.WATER, 1.0);
        // Sporenwoud, lv 6-13
        opp("hoedjesbol", WezensPlekken.HOEDJESBOL, WezensRegistry.HOEDJESBOL, 18, 4, 600, 6, 10, Element.NATUUR, ELITE);
        opp("sporenpuffer", WezensPlekken.SPORENPUFFER, WezensRegistry.SPORENPUFFER, 16, 3, 700, 7, 11, Element.NATUUR, ELITE);
        opp("zwamwachter", WezensPlekken.ZWAMWACHTER, WezensRegistry.ZWAMWACHTER, 14, 2, 900, 9, 13, Element.NATUUR, ELITE);
        opp("mosgeest", WezensPlekken.MOSGEEST, WezensRegistry.MOSGEEST, 16, 3, 700, 8, 13, Element.NATUUR, ELITE);
        // Druipgrotten, lv 12-18: surface + the caves (floor search)
        vloer("druipslijm", WezensPlekken.DRUIPSLIJM, WezensRegistry.DRUIPSLIJM, 12, 3, 700, 12, 15, Element.WATER, ELITE);
        vloer("kristalklodder_grot", WezensPlekken.KRISTALKLODDER_GROT, WezensRegistry.KRISTALKLODDER, 8, 2, 800, 15, 18, Element.WATER, ELITE);
        opp("kristalklodder", WezensPlekken.KRISTALKLODDER, WezensRegistry.KRISTALKLODDER, 14, 3, 700, 13, 16, Element.WATER, ELITE);
        opp("knaag", WezensPlekken.KNAAG, WezensRegistry.KNAAG, 14, 5, 800, 12, 16, Element.GEEN, ELITE);
        vloer("knaag_grot", WezensPlekken.KNAAG_GROT, WezensRegistry.KNAAG, 10, 4, 800, 13, 17, Element.GEEN, ELITE);
        opp("glimzwam", WezensPlekken.GLIMZWAM, WezensRegistry.GLIMZWAM, 12, 2, 900, 13, 17, Element.NATUUR, ELITE);
        vloer("glimzwam_grot", WezensPlekken.GLIMZWAM_GROT, WezensRegistry.GLIMZWAM, 6, 1, 900, 16, 18, Element.NATUUR, ELITE);
        // Zwamtempel, lv 18-20: the temple rooms (floor search) + the mountain path
        vloer("tempelridder", WezensPlekken.TEMPELRIDDER, WezensRegistry.TEMPELRIDDER, 6, 2, 900, 19, 20, Element.NATUUR, ELITE);
        vloer("sporenpriester", WezensPlekken.SPORENPRIESTER, WezensRegistry.SPORENPRIESTER, 5, 1, 1200, 19, 20, Element.NATUUR, ELITE);
        berg("tempelridder_pad", WezensPlekken.TEMPELRIDDER_PAD, WezensRegistry.TEMPELRIDDER, 14, 2, 900, 18, 19, Element.NATUUR, ELITE);
        berg("sporenpriester_pad", WezensPlekken.SPORENPRIESTER_PAD, WezensRegistry.SPORENPRIESTER, 12, 1, 1200, 18, 19, Element.NATUUR, ELITE);
    }

    public static int aantalOppervlak() {
        return oppervlak;
    }

    public static int aantalVloer() {
        return vloer;
    }

    /** One spawn of this spawner now (kern's spawn; the gametest and the dev command). */
    @Nullable
    public static VadsrijkMob spawn(ServerLevel sl, Spawners.Spawner s) {
        return Spawners.spawn(sl, s);
    }
}
