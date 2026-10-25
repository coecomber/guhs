package nl.juiced.guhs.feature.techbeurs;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.7.0 (ronde 45-47): de Guhtechniek-beurs, a surface exhibition hall with a glass roof in the Guhmension (structure
 * {@code guhs:guhtechniek_beurs}; guaranteed once 800-1500 blocks from spawn in new terrain, rarely elsewhere; Superkompas tab
 * Knus). Every part of the Guh-technologie has a stand that really works (a sign with its vadskracht, a Vadsschakelaar to
 * switch it off and on, a beursguh with a joke and a tip), and the middle holds the Droomfabriek where everything works
 * together. Built by tools/features/techbeurs.py.
 * <ul>
 *   <li>protected like every guh building ({@link Bescherming}); its own owner-less machines may still change blocks
 *       ({@link Bescherming#machinesMogen}), and visitors may flip switches and open doors but not take things out of the
 *       stands ({@link #kijkenMag}: "kijken mag, pakken niet");</li>
 *   <li>a hidden {@link BeursBeheer} keeps the stands going for ever (inputs topped up, outputs emptied).</li>
 * </ul>
 */
public final class Beurs {
    public static final String STRUCTUUR = "guhtechniek_beurs";

    /** The beursguhs: plek (RoleData guhs_plek "techbeurs_<stand>") -> (kind, number of lines). */
    public static final Map<String, Praat> PRAAT = new LinkedHashMap<>();

    static {
        GuhNpcEntity.Kind uitvinder = GuhNpcEntity.Kind.UITVINDERGUH, werktuig = GuhNpcEntity.Kind.WERKTUIGGUH,
                kraam = GuhNpcEntity.Kind.KRAAMGUH, loket = GuhNpcEntity.Kind.LOKETGUH;
        zet("welkom", loket, 3);
        zet("guhrad", uitvinder, 2);
        zet("knuffel", kraam, 2);
        zet("disco", kraam, 2);
        zet("blub", werktuig, 2);
        zet("kern", uitvinder, 2);
        zet("molen", werktuig, 2);
        zet("oogster", kraam, 2);
        zet("buizen", werktuig, 2);
        zet("opzuiger", kraam, 2);
        zet("knabbelaar", werktuig, 2);
        zet("knutsel", uitvinder, 2);
        zet("plantagebak", kraam, 2);
        zet("pomp", werktuig, 2);
        zet("sauzen", kraam, 2);
        zet("bezorg", loket, 2);
        zet("droom", uitvinder, 3);
    }

    private static void zet(String stand, GuhNpcEntity.Kind kind, int regels) {
        PRAAT.put("techbeurs_" + stand, new Praat(kind, regels));
    }

    /** A beursguh: its look and how many lines it has ({@code quest.guhs.techbeurs.<stand>.<1..n>}). */
    public record Praat(GuhNpcEntity.Kind kind, int regels) {
    }

    static void register() {
        PRAAT.forEach((plek, p) -> NpcRollen.zet(p.kind(), plek, (npc, player) -> GuhQuests.say(player, npc,
                "quest.guhs.techbeurs." + plek.substring("techbeurs_".length()) + "." + (1 + player.getRandom().nextInt(p.regels())))));
        Bescherming.registreer(STRUCTUUR, 2);
        Bescherming.machinesMogen(STRUCTUUR);
        nl.juiced.guhs.item.SuperkompasItem.voegToe("knus", STRUCTUUR);
        NeoForge.EVENT_BUS.addListener(Beurs::opKlik);
    }

    /**
     * In the beurs a visitor may look but not take: a click on a block that holds items (a chest, a machine, a station, a
     * hopper) does nothing for players who are not in creative. Switches, doors and signs work as always.
     */
    private static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!kijkenMag(level, player, event.getPos())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("gui.guhs.techbeurs.kijken"));
        }
    }

    /** May this player use the block here (false: a stand's inventory in the beurs, and the player is not creative)? */
    public static boolean kijkenMag(ServerLevel level, ServerPlayer player, net.minecraft.core.BlockPos pos) {
        if (player.getAbilities().instabuild || !STRUCTUUR.equals(Bescherming.structuurBij(level, pos))) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof VadsschakelaarBlock.Kern) {
            return true;
        }
        return level.getCapability(Capabilities.Item.BLOCK, pos, null) == null
                && level.getCapability(Capabilities.Fluid.BLOCK, pos, null) == null
                && !(be instanceof net.minecraft.world.MenuProvider);
    }

    private Beurs() {
    }
}
