package nl.juiced.guhs.feature.vadsrijk.wereld;

import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;

/**
 * Kern adapter (see NOTITIES_WERELD.md): the StructureTemplateManager keeps every loaded template forever, and the map has
 * ~690 tiles with ~12.5 M blocks in all. After a long exploration that would be well over a gigabyte of templates on the
 * heap. Every 5 minutes this forgets the loaded map tiles; a tile that is still needed is simply loaded again (~10 ms).
 * Kern may move this into KaartFeature (e.g. forget a tile once all 16 of its chunks are generated).
 */
final class TegelGeheugen {
    private TegelGeheugen() {
    }

    static void ruimOp(ServerLevel sl) {
        var mgr = sl.getStructureManager();
        for (int tx = WereldPlekken.TEGEL_X0; tx <= WereldPlekken.TEGEL_X1; tx++) {
            for (int tz = WereldPlekken.TEGEL_Z0; tz <= WereldPlekken.TEGEL_Z1; tz++) {
                mgr.remove(KaartFeature.tegelId(tx, tz));
            }
        }
    }
}
