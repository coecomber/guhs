package nl.juiced.guhs.feature.einde2.zij;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhTime;

/**
 * einde2 zij: the Bibliotheek van Wan Shi Guh ({@code bibliotheek_wan_shi_guh}, four steps, no element), half sunk in
 * the crust desert.
 * <ol start="0">
 *   <li>the owl Wan Shi Guh in his hall (the first time the wink with Mew-guh plays first: {@code knipoog_mew}); his
 *       rules: no Mika's, no fire, and whoever takes knowledge brings knowledge;</li>
 *   <li>bring {@link #KENNIS_NODIG} pieces of knowledge (a map, a compass, a clock, a spyglass, a book ...: hold it and
 *       click him);</li>
 *   <li>a fox guh brings the book that fits you (click one of them);</li>
 *   <li>the planetarium: turn the wheel by professor Zei-guh ({@code planetarium_wiel}): a day when the sun goes chonking.</li>
 * </ol>
 * After the line, the owl trades one piece of knowledge a day for a book ({@link ZijBoeken}).
 */
public final class Bibliotheek {
    static final String Q = Zij.Q + "bieb.";
    static final String S = Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH;
    public static final int KENNIS_NODIG = 3;
    static final String SLEUTEL_REGELS = "einde2_zij_bieb_regels";
    static final String DAG = "guhs_einde2_zij_bieb_dag";
    /** What counts as a piece of knowledge. */
    public static final Set<Item> KENNIS = Set.of(Items.FILLED_MAP, Items.MAP, Items.COMPASS, Items.RECOVERY_COMPASS, Items.CLOCK, Items.SPYGLASS,
            Items.BOOK, Items.WRITABLE_BOOK, Items.WRITTEN_BOOK, Items.ENCHANTED_BOOK, Items.KNOWLEDGE_BOOK);

    private Bibliotheek() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.BIEB;
    }

    static void register() {
        CastWezen.BIJ_KLIK.add(Bibliotheek::klik);
        NpcRollen.zet(GuhNpcEntity.Kind.PROFESSOR_ZEI_GUH, S + "/professor", new ProfessorRol());
        Praat.luister(SLEUTEL_REGELS, (p, spreker, optie) -> regels(p, optie));
    }

    /** How many pieces of knowledge this player has brought. */
    public static int gebracht(ServerPlayer p) {
        return ln().teller(p, "kennis");
    }

    static boolean klik(CastWezen w, ServerPlayer p) {
        if (!Zij.plek(w).startsWith(S + "/")) {
            return false;
        }
        if (w.wie().equals("wan_shi_guh")) {
            uil(w, p);
            return true;
        }
        if (w.wie().equals("vosguh") && ln().stap(p) == 2) {
            w.zetAnimatie("breng");
            w.level().playSound(null, w.blockPosition(), SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1.2f);
            if (ln().verder(p, 2)) {
                Zij.geef(p, ZijBoeken.volgende(p));
                Zij.zeg(p, w, Q + "vos");
                Zij.hint(p, Q + "hint.planetarium");
            }
            return true;
        }
        return false;
    }

    private static void uil(CastWezen uil, ServerPlayer p) {
        int stap = ln().stap(p);
        uil.zetAnimatie("praat");
        switch (stap) {
            case 0 -> {
                BlockPos anker = ankerMew(p);
                if (anker != null && !Cutscenes.gezien(p, ZijScenes.MEW.id()) && !Cutscenes.bezig(p)) {
                    Cutscenes.speel(p, ZijScenes.MEW, anker, Rotation.NONE, speler -> regelsScherm(speler, uil));
                } else {
                    regelsScherm(p, uil);
                }
            }
            case 1 -> {
                ItemStack hand = p.getItemInHand(InteractionHand.MAIN_HAND);
                if (!KENNIS.contains(hand.getItem())) {
                    Zij.zeg(p, uil, Q + "kennis_wat", KENNIS_NODIG - gebracht(p));
                    Zij.hint(p, Q + "hint.kennis");
                    return;
                }
                hand.shrink(1);
                int n = gebracht(p) + 1;
                ln().teller(p, "kennis", n);
                uil.zetAnimatie("buig");
                Zij.geluid(p, SoundEvents.BOOK_PUT, 1f, 1f);
                if (n >= KENNIS_NODIG && ln().verder(p, 1)) {
                    uil.zetAnimatie("spreid");
                    Zij.zeg(p, uil, Q + "kennis_klaar");
                    Zij.hint(p, Q + "hint.vos");
                    nl.juiced.guhs.feature.einde2.zij.ZijEvents.later(60, () -> uil.zetAnimatie("idle"));
                } else {
                    Zij.zeg(p, uil, Q + "kennis_een." + Math.min(2, n - 1), KENNIS_NODIG - n);
                }
            }
            case 2 -> {
                Zij.zeg(p, uil, Q + "vos_zoek");
                Zij.hint(p, Q + "hint.vos");
            }
            case 3 -> {
                Zij.zeg(p, uil, Q + "planetarium");
                Zij.hint(p, Q + "hint.planetarium");
            }
            default -> ruil(uil, p);
        }
    }

    private static void regelsScherm(ServerPlayer p, Entity uil) {
        Praat.scene(p, SLEUTEL_REGELS, List.of(new Praat.Regel(uil, "", Q + "regels.1"), new Praat.Regel(uil, "", Q + "regels.2"),
                new Praat.Regel(uil, "", Q + "regels.3")), new Praat.Optie(1, Q + "optie.kennis"));
    }

    private static void regels(ServerPlayer p, int optie) {
        if (optie >= 0 && ln().stap(p) == 0) {
            ln().begin(p);
            if (ln().verder(p, 0)) {
                Zij.hint(p, Q + "hint.kennis");
            }
        }
    }

    /** After the line: one piece of knowledge a day for a book. */
    private static void ruil(CastWezen uil, ServerPlayer p) {
        ItemStack hand = p.getItemInHand(InteractionHand.MAIN_HAND);
        long dag = GuhTime.day(p.level());
        if (!KENNIS.contains(hand.getItem())) {
            Zij.zeg(p, uil, Q + "later." + p.getRandom().nextInt(3));
            return;
        }
        if (GuhQuests.saved(p).getLongOr(DAG, -1L) == dag) {
            Zij.zeg(p, uil, Q + "ruil_morgen");
            return;
        }
        hand.shrink(1);
        GuhQuests.saved(p).putLong(DAG, dag);
        uil.zetAnimatie("buig");
        Zij.geef(p, ZijBoeken.volgende(p));
        Zij.zeg(p, uil, Q + "ruil");
        Zij.geluid(p, SoundEvents.BOOK_PAGE_TURN, 1f, 0.9f);
    }

    /** The anchor of the wink: the owl's spot of the copy the player is in. */
    static BlockPos ankerMew(ServerPlayer p) {
        ZijPlek.Kopie k = ZijPlek.bij(p, S, 8);
        return k == null ? null : k.pos("wan_shi_guh");
    }

    // --- the planetarium -------------------------------------------------------------------------------------------------------

    static final class ProfessorRol extends QuestRol {
        ProfessorRol() {
            super(ZijLijnen.BIEB);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("meet");
            if (stap == 3) {
                zeg(p, npc, Q + "prof_wiel");
                hint(p, Q + "hint.planetarium");
            } else if (stap > 3) {
                zeg(p, npc, Q + "prof_later." + p.getRandom().nextInt(3));
            } else {
                zeg(p, npc, "quest.guhs.einde2_cast_volk.praatje.professor_zei_guh." + p.getRandom().nextInt(2));
            }
        }
    }

    /** A click on (or next to) the wheel of the planetarium at step 3. */
    static boolean klikBlok(ServerPlayer p, BlockPos pos) {
        if (ln().stap(p) != 3) {
            return false;
        }
        ZijPlek.Kopie k = ZijPlek.bij(p, S, 4);
        if (k == null || !k.bij("planetarium_wiel", Vec3.atBottomCenterOf(pos), 2.6)) {
            return false;
        }
        ServerLevel level = p.level();
        Zij.geluid(p, SoundEvents.GRINDSTONE_USE, 1f, 0.7f);
        Zij.geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.6f);
        level.sendParticles(p, ParticleTypes.END_ROD, true, false, pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5, 60, 4, 3, 4, 0.01);
        if (ln().verder(p, 3)) {
            Zij.stem(p, Q + "naam.professor", Q + "planetarium_klaar.1");
            Zij.stem(p, Q + "naam.professor", Q + "planetarium_klaar.2");
            Zij.stem(p, Q + "naam.uil", Q + "klaar");
            Zij.eenmalig(ln(), p, "beloning", ZijBoeken.volgende(p), new ItemStack(ModItems.KAAS_KNABBELS.get(), 12));
            Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.1f);
        }
        return true;
    }
}
