package nl.juiced.guhs.feature.einde2.zij;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;

/**
 * einde2 zij: the Vuurtempel van Roku-guh ({@code vuurtempel_roku}, four steps, fire).
 * <ol start="0">
 *   <li>talk to Shyu, the friendly Vuurwijze by the hatch behind the altar (he asks for fire bending);</li>
 *   <li>light the five fire locks at the top of the tower ({@code vuurdeur_1..5}: stand before a door and use your FIRE);</li>
 *   <li>the doors are open (for you): step into the sanctum ({@code heiligdom}): the sun beam falls on the statue's eyes
 *       and Roku-guh appears (scene {@code roku_zonnestraal});</li>
 *   <li>talk with Roku-guh: about his friend, the fire lord who wanted more, and about the choices of friends.</li>
 * </ol>
 * Nothing of the building changes: what a player lit or opened is {@link Schijn} (sent to that player only); since the
 * server still has the door, walking into an open door carries the player through ({@link #tick}).
 */
public final class Vuurtempel {
    static final String Q = Zij.Q + "vuur.";
    static final String S = Einde2Ids.VUURTEMPEL_ROKU;
    public static final int DEUREN = 5;
    static final String SLEUTEL_ROKU = "einde2_zij_roku";

    /** (not saved) a player went through a door at this game time (no stepping back at once). */
    private static final Map<UUID, Long> DOOR = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> IN_HEILIGDOM = new ConcurrentHashMap<>();

    private Vuurtempel() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.VUUR;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.VUURWIJZE_SHYU, S + "/shyu", new ShyuRol());
        NpcRollen.zet(GuhNpcEntity.Kind.ROKU_GUH, S + "/roku", new RokuRol());
        Krachten.NA_GEBRUIK.add(Vuurtempel::kracht);
        Schijn.registreer(Vuurtempel::schijn);
        Bewoners.BIJ_MAKEN.add(Vuurtempel::zicht);
        Praat.luister(SLEUTEL_ROKU, (p, spreker, optie) -> rokuAntwoord(p, spreker, optie));
    }

    /** Roku-guh's spirit shows itself only to a player who saw it appear. */
    static void zicht(Entity e, String plek) {
        if (plek.equals(S + "/roku")) {
            Zicht.alleenBij(e, ln().id(), 3, 99);
        }
    }

    static final class ShyuRol extends QuestRol {
        ShyuRol() {
            super(ZijLijnen.VUUR);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("wenk");
            switch (stap) {
                case 0 -> {
                    if (!Zij.element(p, ZijLijnen.VUURTEMPEL, npc)) {
                        return;
                    }
                    ln().begin(p);
                    scherm(p, npc, Q + "welkom", new Praat.Optie(1, Q + "optie.ja"), new Praat.Optie(2, Q + "optie.wie"));
                }
                case 1 -> {
                    zeg(p, npc, Q + "deur_nog", DEUREN - aan(p));
                    hint(p, Q + "hint.deuren");
                }
                case 2 -> {
                    zeg(p, npc, Q + "open");
                    hint(p, Q + "hint.heiligdom");
                }
                case 3 -> {
                    zeg(p, npc, Q + "roku_wacht");
                    hint(p, Q + "hint.roku");
                }
                default -> zeg(p, npc, Q + "shyu_dank." + p.getRandom().nextInt(3));
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap != 0) {
                return;
            }
            if (optie == 2) {
                scherm(p, npc, Q + "uitleg", new Praat.Optie(1, Q + "optie.ja"));
            } else if (optie == 1 && verder(p, 0)) {
                zeg(p, npc, Q + "deuren");
                hint(p, Q + "hint.deuren");
            }
        }
    }

    static final class RokuRol extends QuestRol {
        RokuRol() {
            super(ZijLijnen.VUUR);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("raad");
            if (stap == 3) {
                Praat.scene(p, SLEUTEL_ROKU, List.of(new Praat.Regel(npc, "", Q + "roku.1"), new Praat.Regel(npc, "", Q + "roku.2"),
                        new Praat.Regel(npc, "", Q + "roku.3"), new Praat.Regel(npc, "", Q + "roku.4"), new Praat.Regel(npc, "", Q + "roku.5")),
                        new Praat.Optie(1, Q + "optie.dank_roku"));
            } else if (stap > 3) {
                zeg(p, npc, Q + "roku_later." + p.getRandom().nextInt(3));
            } else {
                zeg(p, npc, Q + "roku_vroeg");
            }
        }
    }

    private static void rokuAntwoord(ServerPlayer p, Entity spreker, int optie) {
        if (optie < 0 || ln().stap(p) != 3 || !ln().verder(p, 3)) {
            return;
        }
        Zij.stem(p, Q + "naam.roku", Q + "roku_klaar");
        Zij.eenmalig(ln(), p, "beloning", new ItemStack(nl.juiced.guhs.feature.einde2.deco.E2DecoFeature.EEUWIGE_VLAM_ITEM.get()),
                new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 16));
        Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.1f);
        if (spreker != null) {
            p.level().sendParticles(ParticleTypes.FLAME, spreker.getX(), spreker.getY() + 1.5, spreker.getZ(), 30, 0.5, 0.8, 0.5, 0.02);
        }
    }

    static int aan(ServerPlayer p) {
        int n = 0;
        for (int i = 1; i <= DEUREN; i++) {
            n += ln().vlag(p, "deur_" + i) ? 1 : 0;
        }
        return n;
    }

    // --- step 1: the fire locks ----------------------------------------------------------------------------------------------

    static void kracht(ServerPlayer p, Krachten.Kracht k) {
        if (k.element() != Einde2Ids.Element.VUUR || ln().stap(p) != 1) {
            return;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 8);
        if (kopie == null) {
            return;
        }
        for (int i = 1; i <= DEUREN; i++) {
            BlockPos slot = kopie.pos("vuurdeur_" + i);
            if (slot == null || Vec3.atCenterOf(slot).distanceToSqr(p.getEyePosition()) > 5.5 * 5.5) {
                continue;
            }
            if (ln().vlag(p, "deur_" + i)) {
                Zij.melding(p, Q + "deur_al");
                return;
            }
            ln().vlag(p, "deur_" + i, true);
            ServerLevel level = p.level();
            Schijn.toon(p, slot, lamp(level.getBlockState(slot)));
            level.sendParticles(p, ParticleTypes.FLAME, true, false, slot.getX() + 0.5, slot.getY() + 0.5, slot.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.03);
            Zij.geluid(p, SoundEvents.BLAZE_SHOOT, 0.6f, 1.2f);
            int n = aan(p);
            if (n >= DEUREN && ln().verder(p, 1)) {
                Zij.titel(p, net.minecraft.network.chat.Component.translatable(Q + "deuren_titel").withStyle(net.minecraft.ChatFormatting.GOLD),
                        net.minecraft.network.chat.Component.translatable(Q + "deuren_sub"), 4, 50, 10);
                Zij.geluid(p, SoundEvents.IRON_DOOR_OPEN, 1f, 0.5f);
                Zij.geluid(p, SoundEvents.BLAZE_AMBIENT, 0.7f, 0.6f);
                Schijn.straks(p);
                Zij.stem(p, Q + "naam.shyu", Q + "deuren_open");
                Zij.hint(p, Q + "hint.heiligdom");
            } else {
                Zij.melding(p, Q + "deur_een", n, DEUREN);
            }
            return;
        }
    }

    /** A lit lamp, whatever the lock is made of (a block without LIT: a glowing stand-in). */
    private static BlockState lamp(BlockState slot) {
        return slot.hasProperty(BlockStateProperties.LIT) ? slot.setValue(BlockStateProperties.LIT, true) : Blocks.SHROOMLIGHT.defaultBlockState();
    }

    /** {@link Schijn}: the locks this player lit, and from step 2 on the five doors open. */
    private static void schijn(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        int stap = ln().stap(p);
        if (stap < 1) {
            return;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 32);
        if (kopie == null) {
            return;
        }
        ServerLevel level = p.level();
        for (int i = 1; i <= DEUREN; i++) {
            BlockPos slot = kopie.pos("vuurdeur_" + i);
            if (slot == null) {
                continue;
            }
            if (stap >= 2) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        gewenst.put(slot.offset(dx, dy, 0), Blocks.AIR.defaultBlockState());
                    }
                }
            } else if (ln().vlag(p, "deur_" + i)) {
                gewenst.put(slot, lamp(level.getBlockState(slot)));
            }
        }
    }

    // --- every tick: through an open door; into the sanctum ---------------------------------------------------------------------

    static void tick(ServerPlayer p) {
        int stap = ln().stap(p);
        if (stap < 2) {
            return;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 8);
        if (kopie == null) {
            IN_HEILIGDOM.remove(p.getUUID());
            return;
        }
        AABB heiligdom = kopie.zone("heiligdom");
        if (heiligdom == null) {
            return;
        }
        long nu = p.level().getGameTime();
        Long laatst = DOOR.get(p.getUUID());
        if (laatst == null || nu - laatst > 15) {
            deur(p, kopie, heiligdom, nu);
        }
        boolean binnen = heiligdom.contains(p.position());
        Boolean was = IN_HEILIGDOM.put(p.getUUID(), binnen);
        if (binnen && !Boolean.TRUE.equals(was) && stap == 2) {
            zonnestraal(p, kopie);
        }
    }

    /** Walking into one of the open doors (from either side) carries the player through the wall. */
    private static void deur(ServerPlayer p, ZijPlek.Kopie kopie, AABB heiligdom, long nu) {
        double midZ = (heiligdom.minZ + heiligdom.maxZ) / 2;
        for (int i = 1; i <= DEUREN; i++) {
            BlockPos slot = kopie.pos("vuurdeur_" + i);
            if (slot == null) {
                continue;
            }
            double muur = slot.getZ() + 0.5;
            double richting = Math.signum(midZ - muur);   // the side of the sanctum
            double dx = p.getX() - (slot.getX() + 0.5), dz = p.getZ() - muur;
            double voet = slot.getY() - 1;
            if (Math.abs(dx) > 1.3 || Math.abs(dz) > 1.45 || p.getY() < voet - 0.6 || p.getY() > voet + 1.2) {
                continue;
            }
            Vec3 kijk = p.getLookAngle();
            boolean naarBinnen = dz * richting < 0 && kijk.z * richting > 0.35;
            boolean naarBuiten = dz * richting > 0 && kijk.z * richting < -0.35;
            if (!naarBinnen && !naarBuiten) {
                continue;
            }
            double doelZ = muur + (naarBinnen ? richting : -richting) * 1.7;
            DOOR.put(p.getUUID(), nu);
            p.teleportTo(p.level(), p.getX(), voet, doelZ, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
            p.level().sendParticles(p, ParticleTypes.FLAME, true, false, slot.getX() + 0.5, slot.getY(), slot.getZ() + 0.5, 20, 0.6, 1, 0.3, 0.02);
            Zij.geluid(p, SoundEvents.FIRECHARGE_USE, 0.5f, 0.7f);
            Schijn.straks(p);
            return;
        }
    }

    /** The sun beam: the scene, then step 3. */
    static void zonnestraal(ServerPlayer p, ZijPlek.Kopie kopie) {
        BlockPos anker = kopie.pos("roku_beeld");
        if (anker == null || Cutscenes.bezig(p)) {
            return;
        }
        Cutscenes.speel(p, ZijScenes.ROKU, anker, Rotation.NONE, speler -> {
            if (ln().verder(speler, 2)) {
                Zicht.kijkOpnieuw(speler);
                Zij.hint(speler, Q + "hint.roku");
            }
        });
    }

    static void vergeet(UUID id) {
        DOOR.remove(id);
        IN_HEILIGDOM.remove(id);
    }
}
