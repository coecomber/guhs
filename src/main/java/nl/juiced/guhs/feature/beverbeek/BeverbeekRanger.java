package nl.juiced.guhs.feature.beverbeek;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * The ranger-guh of a Beverbeek ranger post (BEVERBEEK_RANGER): tells about beavers (a fact per click, from
 * {@link #FEITJES} lang lines), explains that his beaver may be petted but stays here, and points the way to the safari
 * bus. He also looks after that one beaver: every few seconds while his chunk is loaded he makes sure exactly one
 * {@link Aaibever} lives around the post (spawned when missing, doubles removed).
 */
public final class BeverbeekRanger implements NpcRole {
    public static final BeverbeekRanger ROL = new BeverbeekRanger();
    public static final int FEITJES = 8;
    /** How far from the ranger the beaver may roam (its home is the ranger's spot). */
    public static final int STRAAL = 7;
    private static final int ELKE = 100;
    private static final String T = "gui.guhs.beverbeek.ranger.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        Praat.open(p, npc, null, T + "hallo", new Object[0], opties());
    }

    private static Praat.Optie[] opties() {
        return new Praat.Optie[]{new Praat.Optie(1, T + "optie.bevers"), new Praat.Optie(2, T + "optie.aaien"),
                new Praat.Optie(3, T + "optie.bus"), new Praat.Optie(9, T + "optie.doei")};
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        switch (optie) {
            case 1 -> Praat.open(p, npc, null, T + "feit." + npc.getRandom().nextInt(FEITJES), new Object[0],
                    new Praat.Optie(1, T + "optie.nog"), new Praat.Optie(4, T + "optie.terug"), new Praat.Optie(9, T + "optie.doei"));
            case 2 -> Praat.open(p, npc, null, T + "aaien", new Object[0], new Praat.Optie(4, T + "optie.terug"), new Praat.Optie(9, T + "optie.doei"));
            case 3 -> bus(npc, p);
            case 4 -> talk(npc, p);
            case 9 -> Praat.sluit(p);
            default -> {
            }
        }
    }

    /**
     * Test round 5: "Waar is het safaripark?": the ranger points the way to the nearest busstation (direction in words and
     * about how far), and tells about the Superkompas; without one in reach his old answer.
     */
    private static void bus(GuhNpcEntity npc, ServerPlayer p) {
        net.minecraft.core.BlockPos station = npc.level() instanceof ServerLevel level
                ? nl.juiced.guhs.feature.safari.bus.Busstation.dichtstbij(level, npc.blockPosition()) : null;
        Praat.Optie terug = new Praat.Optie(4, T + "optie.terug"), doei = new Praat.Optie(9, T + "optie.doei");
        if (station == null) {
            Praat.open(p, npc, null, T + "bus", new Object[0], terug, doei);
            return;
        }
        double dx = station.getX() - npc.getX(), dz = station.getZ() - npc.getZ();
        long ver = Math.max(100, Math.round(Math.hypot(dx, dz) / 100.0) * 100);
        Praat.open(p, npc, null, T + "bus.richting", new Object[]{ver, net.minecraft.network.chat.Component.translatable(T + "richting." + richting(dx, dz))},
                terug, doei);
    }

    /** The compass direction of (dx, dz) in eight steps: noord, noordoost, oost... (north is -z). */
    public static String richting(double dx, double dz) {
        String[] acht = {"zuid", "zuidwest", "west", "noordwest", "noord", "noordoost", "oost", "zuidoost"};
        double hoek = Math.toDegrees(Math.atan2(-dx, dz));          // like a mob's yaw: 0 = south (+z), 90 = west (-x)
        return acht[Math.floorMod((int) Math.round(hoek / 45.0), 8)];
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level) || (level.getGameTime() + npc.getId()) % ELKE != 0) {
            return;
        }
        zorgVoorBever(level, npc.blockPosition());
    }

    /** Exactly one petting beaver around this post (returns it; null when it could not be made). */
    public static SafariDier zorgVoorBever(ServerLevel level, BlockPos post) {
        List<SafariDier> hier = level.getEntitiesOfClass(SafariDier.class, new AABB(post).inflate(STRAAL + 9, 8, STRAAL + 9), Aaibever::is);
        for (int i = 1; i < hier.size(); i++) {
            hier.get(i).discard();
        }
        if (!hier.isEmpty()) {
            return hier.get(0);
        }
        SafariDier bever = SafariFeature.DIER.get().create(level, EntitySpawnReason.STRUCTURE);
        if (bever == null) {
            return null;
        }
        int x = post.getX() + 2, z = post.getZ() + 2;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        bever.zet("bever", "vrouw", post, STRAAL, -1, -1, 0);
        bever.snapTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        bever.addTag(Aaibever.TAG);
        level.addFreshEntity(bever);
        return bever;
    }

    private BeverbeekRanger() {
    }
}
