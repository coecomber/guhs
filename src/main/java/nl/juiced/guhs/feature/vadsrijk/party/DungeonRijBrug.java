package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepVulling;

/**
 * W-INT (W-PARTY x W-GROT): the party tab and the nep-spelers on w-grot's dungeon finder ({@link DungeonRij}).
 * <ul>
 *   <li>the tab's "Party in rij" = {@link DungeonRij#zetGroepInRij} (the leader's party as one block);</li>
 *   <li>nep-spelers fill a queue as {@link DungeonRij.Metgezel}s (auto "yes" in the ready check, no loot/clear/cooldown, they count
 *       for the run's scaling);</li>
 *   <li>at the start w-grot hands the companions to {@link DungeonRij#BIJ_START}; {@link NepVulling#binnen} spawns them;</li>
 *   <li>the per-player "Nep-spelers" switch is mirrored into w-grot's saved "no NPC fill" flag.</li>
 * </ul>
 */
public final class DungeonRijBrug implements DungeonWachtrij {
    private DungeonRijBrug() {
    }

    /** Called once from {@link PartyRegistry#register}. */
    public static void koppel() {
        Wachtrijen.DUNGEON = new DungeonRijBrug();
        DungeonRij.BIJ_START.add((in, spelers, mee) -> NepVulling.binnen(in.dungeon, spelers, mee.stream().map(DungeonRij.Metgezel::id).toList()));
        NepSpelers.STAND_HAAK.add((p, aan) -> DungeonRij.zetGeenNpcVulling(p, !aan));
    }

    /** Puts the real coupling back after a test swapped {@link Wachtrijen#DUNGEON} (the hooks stay registered once). */
    public static void herstel() {
        Wachtrijen.DUNGEON = new DungeonRijBrug();
    }

    @Override
    public List<String> dungeons() {
        return Dungeons.alle().stream().map(Dungeon::id).toList();
    }

    @Override
    public Component naam(String d) {
        return DungeonRij.dungeonNaam(d);
    }

    @Override
    @Nullable
    public Component partyInRij(ServerPlayer leider, List<ServerPlayer> leden, String d) {
        return DungeonRij.zetGroepInRij(leider, d) ? null : Component.empty();   // (DungeonRij already told the reason)
    }

    @Override
    @Nullable
    public String rijVan(UUID id) {
        return DungeonRij.rijVan(id);
    }

    @Override
    public Map<String, List<UUID>> rijen() {
        Map<String, List<UUID>> m = new LinkedHashMap<>();
        DungeonRij.RIJ.forEach((d, r) -> m.put(d, new ArrayList<>(r)));
        return m;
    }

    @Override
    public int groepMax() {
        return DungeonRij.GROEP_MAX;
    }

    @Override
    public boolean bezet(String d) {
        return DungeonRij.BEZET.containsKey(d) || DungeonRij.CHECKS.containsKey(d);
    }

    @Override
    public boolean isNep(UUID id) {
        return DungeonRij.METGEZELLEN.containsKey(id) || DungeonRij.NEP.containsKey(id);
    }

    @Override
    public boolean voegNepToe(String d, List<NepLid> nep) {
        boolean ok = false;
        for (NepLid l : nep) {
            ok |= DungeonRij.voegMetgezelToe(d, new DungeonRij.Metgezel(l.id(), l.naam(), l.rol().id()));
        }
        return ok;
    }

    @Override
    @Nullable
    public String dungeonVan(UUID id) {
        return DungeonRij.dungeonVan(id);
    }
}
