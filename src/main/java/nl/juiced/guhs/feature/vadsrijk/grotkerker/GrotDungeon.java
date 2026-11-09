package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonNet;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonType;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit;

/**
 * W-GROT: a cave run (U-GROTKERKER) as a {@link Dungeon} of type {@link DungeonType#GROT}, for the generic door / window / queue /
 * instance / cooldown system. The id stays the cave id ("zwamgrot", ...), so the saved clears ("grotkerker" data, "&lt;cave&gt;.clears")
 * and the pity counters keep working. It reads the cave anew on every call ({@link Kerkers#van}: the gametests' stand-in caves too).
 */
public record GrotDungeon(String id) implements Dungeon {
    static void register() {
        Dungeons.bron(() -> {
            List<GrotDungeon> l = new ArrayList<>();
            for (Kerker k : Kerkers.alle()) {
                l.add(new GrotDungeon(k.id()));
            }
            return l;
        });
    }

    Kerker k() {
        return Kerkers.van(id);
    }

    @Override
    public DungeonType type() {
        return DungeonType.GROT;
    }

    @Override
    public Component naam() {
        return Component.translatable("vadsrijk.grotkerker.grot." + id);
    }

    @Override
    public Component onderTitel() {
        return Component.translatable("vadsrijk.grotkerker.baas." + id + ".onder");
    }

    @Override
    public int zone() {
        return k().zone();
    }

    @Override
    public int minLv() {
        return k().minLv();
    }

    @Override
    public int maxLv() {
        return k().baasLv();
    }

    @Override
    public boolean bevat(double x, double y, double z) {
        Kerker k = k();
        if (k == null || !k.bevat(x, y, z)) {
            return false;
        }
        // the cave box can reach out past a door (De Kristalgrot's mouth tunnel): the outer side of a door is outside the dungeon
        for (Deur d : DungeonDeur.van(id)) {
            if (DungeonDeur.midden(d).distToCenterSqr(x, y, z) <= 12 * 12 && !DungeonDeur.binnenKant(d, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean bezig() {
        return GrotKerker.staat(id) != GrotKerker.Staat.SLAAPT;
    }

    @Override
    public void reset(ServerLevel sl) {
        Kerker k = k();
        if (k != null) {
            GrotKerker.reset(sl, k, GrotKerker.run(id), null);
        }
    }

    @Override
    public int clears(ServerPlayer p) {
        return GrotKerker.clears(p, id);
    }

    /** The boss, the loot of every clear (KerkerLoot) and the personal rare rolls with their pity (the numbers of /guhs vadsrijk loot kansen). */
    @Override
    public void venster(ServerPlayer p, DungeonNet.Bouw b) {
        Kerker k = k();
        String grot = id;
        int zone = k.zone();
        b.lijn(Component.translatable("vadsrijk.dungeon.baas", Component.translatable("vadsrijk.grotkerker.baas." + grot)), 0xFF7A2A5A, true, false);
        b.lijn(Component.translatable("vadsrijk.grotkerker.baas." + grot + ".onder").withStyle(ChatFormatting.ITALIC), 0xFF9A8094);
        // loot (KerkerLoot: every clear)
        b.kop(Component.translatable("vadsrijk.dungeon.buit"));
        b.lijn(Component.translatable("vadsrijk.dungeon.buit.munten", 400L * zone * zone + 300), 0xFF5A3A56);
        boolean eerste = GrotKerker.clears(p, grot) == 0;
        b.lijn(eerste ? Component.translatable("vadsrijk.dungeon.buit.stuk.eerste", k.baasLv())
                : Component.translatable("vadsrijk.dungeon.buit.stuk", k.baasLv(), Math.round(KerkerLoot.EPISCH_KANS * 100)), 0xFF5A3A56);
        b.lijn(Component.translatable("vadsrijk.dungeon.buit.erts"), 0xFF5A3A56);
        if (zone <= 5) {
            b.lijn(Component.translatable("vadsrijk.dungeon.buit.kern", Math.round(KerkerLoot.KERN_KANS * 100)), 0xFF5A3A56);
        }
        // the rare personal rolls, the same numbers as /guhs vadsrijk loot kansen
        GrotBazen.Def def = GrotBazen.def(grot);
        if (def != null) {
            b.kop(Component.translatable("vadsrijk.dungeon.zeldzaam"));
            Component pet = Component.translatable("vadsrijk.huisdier." + def.pet() + ".naam");
            if (Huisdieren.heeft(p, def.pet())) {
                b.lijn(Component.translatable("vadsrijk.dungeon.pet.heb", pet), 0xFF2F8A5A);
            } else {
                int pech = Spelers.data(p).extra(GrotKerker.DATA).getIntOr(grot + ".petpech", 0);
                b.lijn(Component.translatable("vadsrijk.dungeon.pet", pet, Math.round(KerkerLoot.PET_KANS * 100), pech, KerkerLoot.PET_PITY), 0xFF7A4AB0);
            }
            Component kleur = Component.translatable("vadsrijk.economie.pluimkleur." + def.pluim().name().toLowerCase(Locale.ROOT));
            RijdierBuit.Bron bron = RijdierBuit.BRONNEN.get(def.mountBron());
            if (bron != null && bron.heeft().test(p)) {
                b.lijn(Component.translatable("vadsrijk.dungeon.pluim.heb", kleur), 0xFF2F8A5A);
            } else if (bron != null && !bron.mag().test(p)) {
                b.lijn(Component.translatable("vadsrijk.dungeon.pluim.geen", kleur, Math.round(RijdierBuit.KANS * 100)), 0xFF9A8094);
            } else {
                b.lijn(Component.translatable("vadsrijk.dungeon.pluim", kleur, Math.round(RijdierBuit.KANS * 100), RijdierBuit.pech(p, def.mountBron()),
                        RijdierBuit.PITY), 0xFF7A4AB0);
            }
            b.lijn(Component.translatable("vadsrijk.dungeon.pech.uitleg"), 0xFF9A8094);
        }
    }

    /** Zone 6's door moves with u-z6wereld's POI "zoutgrot" (like {@link Kerkers#zone6}). */
    @Override
    public Deur pasAan(Deur d) {
        if (!id.equals("zoutgrot")) {
            return d;
        }
        Map<String, BlockPos> m = Kerkers.z6Plekken();
        if (m == null || m.get("zoutgrot") == null) {
            return d;
        }
        BlockPos hal = m.get("zoutgrot");
        return DungeonDeur.verschoven(d, hal.getX() - Kerkers.Z6_HAL.getX(), hal.getY() - Kerkers.Z6_HAL.getY(), hal.getZ() - Kerkers.Z6_HAL.getZ());
    }
}
