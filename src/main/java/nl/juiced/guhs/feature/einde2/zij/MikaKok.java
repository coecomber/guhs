package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhTime;

/**
 * einde2 zij: de spijtige Mika-kok ({@code mika_kok}, five steps, no element). In the kitchen of every Mika-vesting a cook
 * with a sagging hat stirs a kettle and secretly feeds the prisoners in the cells.
 * <ol start="0">
 *   <li>talk to the cook in a vesting's kitchen ({@code mika_vesting/kok});</li>
 *   <li>bring him ingredients from the outer islands ({@link #INGREDIENTEN});</li>
 *   <li>distract the fortress captain on the ground floor ({@code mika_vesting/kapitein}): pick the trick that works;</li>
 *   <li>back to the cook: the soup goes to the cells, and he makes up his mind;</li>
 *   <li>visit him in his own kitchen at the Leegtehaven, De Spijtige Spatel ({@code leegtehaven/mika_kok}).</li>
 * </ol>
 * Reward: the recipe of the Spijtsoep (and three bowls). Per player: the vesting cook is gone for you once he left
 * ({@link Zicht}), the harbour cook is there for you from then on ({@link #houdHavenkok}); after the line he keeps a
 * bowl warm for you, once a day.
 */
public final class MikaKok {
    static final String Q = Zij.Q + "kok.";
    public static final String HAVENKOK = "guhs_einde2_zij_havenkok";
    static final String DAG = "guhs_einde2_zij_soep_dag";
    static final String RECEPT = "einde2_zij_spijtsoep";

    /** What the cook needs: one of each rare biome (item, count). */
    public static final List<Object[]> INGREDIENTEN = List.of(new Object[]{"guhs:schimmelpaddenstoel", 2}, new Object[]{"guhs:kristalkaas_brokje", 2},
            new Object[]{"guhs:knabbelkoorvrucht", 2}, new Object[]{"guhs:kruimelwolk", 1});

    private MikaKok() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.KOK;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.MIKA_KOK, Einde2Ids.MIKA_VESTING + "/kok", new KokRol());
        NpcRollen.zet(GuhNpcEntity.Kind.VESTINGKAPITEIN_MIKA, Einde2Ids.MIKA_VESTING + "/kapitein", new KapiteinRol());
        NpcRollen.zet(GuhNpcEntity.Kind.MIKA_KOK, Einde2Ids.LEEGTEHAVEN + "/mika_kok", new HavenRol());
        Bewoners.BIJ_MAKEN.add(MikaKok::zicht);
    }

    /** The vesting cook is gone for whoever saw him leave. */
    static void zicht(Entity e, String plek) {
        if (plek.equals(Einde2Ids.MIKA_VESTING + "/kok")) {
            Zicht.alleenBij(e, ln().id(), 0, 3);
        }
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /** The Guhdex list of what the cook still needs. */
    public static List<VerhaalStand.Nodig> nodig(ServerPlayer p) {
        List<VerhaalStand.Nodig> uit = new ArrayList<>();
        for (Object[] r : INGREDIENTEN) {
            uit.add(Verhaallijn.nodig((String) r[0], GuhQuests.count(p, item((String) r[0])), (Integer) r[1]));
        }
        return uit;
    }

    static boolean alles(ServerPlayer p) {
        for (Object[] r : INGREDIENTEN) {
            if (GuhQuests.count(p, item((String) r[0])) < (Integer) r[1]) {
                return false;
            }
        }
        return true;
    }

    static final class KokRol extends QuestRol {
        KokRol() {
            super(ZijLijnen.KOK);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            switch (stap) {
                case 0 -> {
                    npc.speel("spijt");
                    ln().begin(p);
                    scherm(p, npc, Q + "welkom", new Praat.Optie(1, Q + "optie.help"), new Praat.Optie(2, Q + "optie.waarom"));
                }
                case 1 -> {
                    npc.speel("roer");
                    if (!alles(p)) {
                        zeg(p, npc, Q + "ingredienten_nog");
                        hint(p, Q + "hint.ingredienten");
                        return;
                    }
                    for (Object[] r : INGREDIENTEN) {
                        GuhQuests.take(p, item((String) r[0]), (Integer) r[1]);
                    }
                    if (verder(p, 1)) {
                        zeg(p, npc, Q + "ingredienten_klaar");
                        hint(p, Q + "hint.kapitein");
                    }
                }
                case 2 -> {
                    npc.speel("spijt");
                    zeg(p, npc, Q + "kapitein_nog");
                    hint(p, Q + "hint.kapitein");
                }
                case 3 -> {
                    npc.speel("roer");
                    scherm(p, npc, Q + "soep", new Praat.Optie(1, Q + "optie.ga"));
                }
                default -> zeg(p, npc, Q + "weg");
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap == 0) {
                if (optie == 2) {
                    scherm(p, npc, Q + "uitleg", new Praat.Optie(1, Q + "optie.help"));
                } else if (optie == 1 && verder(p, 0)) {
                    zeg(p, npc, Q + "ingredienten");
                    hint(p, Q + "hint.ingredienten");
                }
            } else if (stap == 3 && optie == 1 && verder(p, 3)) {
                ServerLevel level = p.level();
                level.sendParticles(p, ParticleTypes.POOF, true, false, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
                Zij.geluid(p, SoundEvents.CHICKEN_EGG, 0.8f, 0.7f);
                zeg(p, npc, Q + "vertrek");
                hint(p, Q + "hint.haven");
                ZijEvents.later(30, () -> Zicht.kijkOpnieuw(p));
            }
        }
    }

    static final class KapiteinRol implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (ln().stap(p) == 2) {
                npc.speel("tuur");
                Praat.open(p, npc, null, Q + "kapitein", new Object[0], new Praat.Optie(1, Q + "afleiding.1"), new Praat.Optie(2, Q + "afleiding.2"),
                        new Praat.Optie(3, Q + "afleiding.3"));
                return;
            }
            npc.speel("brul");
            GuhQuests.say(p, npc, "quest.guhs.einde2_cast_schurk.praatje.vestingkapitein_mika." + p.getRandom().nextInt(2));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            if (ln().stap(p) != 2 || optie < 1) {
                return;
            }
            switch (optie) {
                case 1 -> {
                    npc.speel("tuur");
                    GuhQuests.say(p, npc, Q + "afgeleid");
                    if (ln().verder(p, 2)) {
                        Zij.hint(p, Q + "hint.terug");
                    }
                }
                case 2 -> GuhQuests.say(p, npc, Q + "hoed");
                default -> {
                    npc.speel("brul");
                    GuhQuests.say(p, npc, Q + "nee");
                }
            }
        }
    }

    static final class HavenRol extends QuestRol {
        HavenRol() {
            super(ZijLijnen.KOK);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("roer");
            if (stap == 4) {
                if (verder(p, 4)) {
                    scherm(p, npc, Q + "haven", new Praat.Optie(9, Q + "optie.smakelijk"));
                    recept(p);
                    Zij.eenmalig(ln(), p, "beloning", new ItemStack(E2ZijFeature.SPIJTSOEP.get(), 3));
                    Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.1f);
                }
                return;
            }
            if (stap < 4) {
                zeg(p, npc, Q + "haven_vreemd");
                return;
            }
            long dag = GuhTime.day(p.level());
            if (GuhQuests.saved(p).getLongOr(DAG, -1L) != dag) {
                GuhQuests.saved(p).putLong(DAG, dag);
                Zij.geef(p, new ItemStack(E2ZijFeature.SPIJTSOEP.get()));
                zeg(p, npc, Q + "dagsoep");
            } else {
                zeg(p, npc, Q + "later." + p.getRandom().nextInt(3));
            }
        }
    }

    /** Unlocks the Spijtsoep recipe in the recipe book. */
    static void recept(ServerPlayer p) {
        p.awardRecipesByKey(List.of(ResourceKey.create(Registries.RECIPE, Guhs.id(RECEPT))));
    }

    /**
     * Every 40 ticks for a player near the harbour: the cook of the harbour stands at his spot ({@code leegtehaven/mika_kok})
     * of this copy, one of him, seen only by players from step 4 on.
     */
    static void houdHavenkok(ServerPlayer p) {
        ZijPlek.Kopie k = ZijPlek.bij(p, Einde2Ids.LEEGTEHAVEN, 32);
        BlockPos pos = k == null ? null : k.pos("mika_kok");
        ServerLevel level = p.level();
        if (pos == null || !level.isPositionEntityTicking(pos)) {
            return;
        }
        String tag = "einde2_leegtehaven_mika_kok@" + k.start().getChunkPos().pack();
        List<GuhNpcEntity> er = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos).inflate(12),
                n -> n.isAlive() && tag.equals(n.getPersistentData().getStringOr(HAVENKOK, "")));
        for (int i = 1; i < er.size(); i++) {
            er.get(i).discard();
        }
        if (!er.isEmpty()) {
            return;
        }
        nl.juiced.guhs.feature.einde2.Plekken.Plek plek = nl.juiced.guhs.feature.einde2.Plekken.plek(Einde2Ids.LEEGTEHAVEN, "mika_kok");
        float yaw = plek == null ? 0f : plek.yaw();
        Entity e = Bewoners.maak(level, Vec3.atBottomCenterOf(pos), Rotation.NONE, new Bewoners.Bewoner(Einde2Ids.LEEGTEHAVEN, "mika_kok",
                GuhNpcEntity.Kind.MIKA_KOK, null, -1, true, 0, null, yaw, 12));
        if (e == null) {
            return;
        }
        e.getPersistentData().putString(HAVENKOK, tag);
        e.setInvulnerable(true);
        if (e instanceof net.minecraft.world.entity.Mob m) {
            m.setPersistenceRequired();
        }
        Zicht.alleenBij(e, ln().id(), 4, 99);
        level.addFreshEntity(e);
    }
}
