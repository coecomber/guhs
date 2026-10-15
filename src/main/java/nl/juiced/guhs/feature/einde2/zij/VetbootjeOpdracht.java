package nl.juiced.guhs.feature.einde2.zij;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.mikas.E2MikasFeature;
import nl.juiced.guhs.feature.einde2.vesting.E2VestingFeature;
import nl.juiced.guhs.feature.einde2.vetbootje.Vetbootje;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 zij: the vetbootje commission ({@code vetbootje}, three steps): once per player your own small, slow flying ship.
 * <ol start="0">
 *   <li>read the notice on the harbour board of a Mika-vesting's pier ({@code mika_vesting/vetbootje_opdracht}: click
 *       the board or the spot beside it);</li>
 *   <li>bring the building materials to the shipwright of the Leegtehaven ({@link #MATERIAAL}); he builds it on the
 *       slipway ({@code helling}) while you watch ({@link #BOUW_TICKS});</li>
 *   <li>christen it: the shipwright hands it over ({@link Vetbootje#geef}) and opens the shipyard (colour, sail, name).</li>
 * </ol>
 * After that the shipwright opens the shipyard for you any time.
 */
public final class VetbootjeOpdracht {
    static final String Q = Zij.Q + "bootje.";
    public static final int BOUW_TICKS = 200;

    /** What the shipwright needs: a tag or an item, and how many. */
    record Materiaal(String tekst, String icoon, int n, TagKey<Item> tag, Item item) {
        int heb(ServerPlayer p) {
            int c = 0;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                if (tag != null ? s.is(tag) : s.is(item)) {
                    c += s.getCount();
                }
            }
            return c;
        }

        void neem(ServerPlayer p) {
            int rest = n;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                if (rest <= 0) {
                    return;
                }
                if (tag != null ? s.is(tag) : s.is(item)) {
                    int t = Math.min(rest, s.getCount());
                    s.shrink(t);
                    rest -= t;
                }
            }
        }
    }

    static List<Materiaal> materiaal() {
        return List.of(new Materiaal(Zij.GUI + "nodig.planken", "minecraft:oak_planks", 24, ItemTags.PLANKS, null),
                new Materiaal(Zij.GUI + "nodig.wol", "minecraft:white_wool", 8, ItemTags.WOOL, null),
                new Materiaal(Zij.GUI + "nodig.vet", "guhs:mika_vet", 4, null, ModItems.MIKA_VET.get()),
                new Materiaal(Zij.GUI + "nodig.flitskruimel", "guhs:flitskruimel", 2, null, E2MikasFeature.FLITSKRUIMEL.get()));
    }

    /** (not saved) player -&gt; the game time their boat is finished on the slipway. */
    private static final Map<UUID, Long> BOUW = new ConcurrentHashMap<>();

    private VetbootjeOpdracht() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.BOOTJE;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.SCHEEPSBOUWERGUH, Einde2Ids.LEEGTEHAVEN + "/scheepsbouwer", new BouwerRol());
    }

    public static List<VerhaalStand.Nodig> nodig(ServerPlayer p) {
        return materiaal().stream().map(m -> Verhaallijn.nodig(m.icoon(), m.tekst(), m.heb(p), m.n())).toList();
    }

    /** A click near the harbour board of a modular vesting at step 0: the notice. */
    static boolean klikBlok(ServerPlayer p, BlockPos pos) {
        if (ln().stap(p) != 0 || !bijBord(p.level(), pos)) {
            return false;
        }
        lees(p);
        return true;
    }

    /** Is this spot within reach of the harbour board of a modular vesting? */
    static boolean bijBord(ServerLevel level, BlockPos pos) {
        StructureStart start = Plekken.bij(level, Einde2Ids.MIKA_VESTING, pos, 48);
        Plekken.Plek bord = Plekken.plek(Einde2Ids.MIKA_VESTING, "vetbootje_opdracht");
        if (start == null || bord == null || !E2VestingFeature.modulair(start)) {
            return false;
        }
        for (BlockPos b : Plekken.alle(start, bord)) {
            if (b.distSqr(pos) <= 2.6 * 2.6) {
                return true;
            }
        }
        return false;
    }

    static void lees(ServerPlayer p) {
        ln().begin(p);
        for (int i = 1; i <= 4; i++) {
            p.sendSystemMessage(Component.translatable(Q + "briefje." + i).withStyle(i == 1 ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        }
        Zij.geluid(p, SoundEvents.BOOK_PAGE_TURN, 1f, 1f);
        if (ln().verder(p, 0)) {
            Zij.hint(p, Q + "hint.werf");
        }
    }

    static final class BouwerRol extends QuestRol {
        BouwerRol() {
            super(ZijLijnen.BOOTJE);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("timmer");
            switch (stap) {
                case 0 -> {
                    if (Vetbootje.heeft(p)) {
                        zeg(p, npc, Q + "werf");
                        Vetbootje.openWerf(p);
                    } else {
                        zeg(p, npc, Q + "geen_opdracht");
                        hint(p, Q + "hint.briefje");
                    }
                }
                case 1 -> {
                    Long klaar = BOUW.get(p.getUUID());
                    if (klaar != null) {
                        zeg(p, npc, Q + "bezig");
                        return;
                    }
                    List<Materiaal> m = materiaal();
                    if (m.stream().anyMatch(x -> x.heb(p) < x.n())) {
                        scherm(p, npc, Q + "materiaal", new Praat.Optie(9, Q + "optie.ok"));
                        hint(p, Q + "hint.materiaal");
                        return;
                    }
                    m.forEach(x -> x.neem(p));
                    BOUW.put(p.getUUID(), p.level().getGameTime() + BOUW_TICKS);
                    zeg(p, npc, Q + "begint");
                }
                case 2 -> {
                    if (verder(p, 2)) {
                        Vetbootje.geef(p);
                        scherm(p, npc, Q + "doop", new Praat.Optie(1, Q + "optie.doop"));
                        Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1f);
                    }
                }
                default -> {
                    zeg(p, npc, Q + "werf");
                    if (!Vetbootje.heeft(p)) {
                        Vetbootje.geef(p);
                    }
                    Vetbootje.openWerf(p);
                }
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (optie == 1 && ln().klaar(p)) {
                Vetbootje.openWerf(p);
            }
        }
    }

    /** Every 10 ticks per player at step 1: the boat grows on the slipway; when it is done, step 2. */
    static void tick(ServerPlayer p) {
        Long klaar = BOUW.get(p.getUUID());
        if (klaar == null) {
            return;
        }
        if (ln().stap(p) != 1) {
            BOUW.remove(p.getUUID());
            return;
        }
        ServerLevel level = p.level();
        ZijPlek.Kopie k = ZijPlek.bij(p, Einde2Ids.LEEGTEHAVEN, 64);
        Vec3 helling = k == null ? null : k.midden("helling");
        long nu = level.getGameTime();
        if (helling != null && nu % 20 == 0) {
            level.sendParticles(p, ParticleTypes.CRIT, true, false, helling.x, helling.y + 2, helling.z, 10, 3, 1, 2, 0.1);
            hamer(p, helling);
        }
        if (nu >= klaar) {
            BOUW.remove(p.getUUID());
            if (ln().verder(p, 1)) {
                Zij.titel(p, Component.translatable(Q + "af").withStyle(ChatFormatting.GOLD), Component.translatable(Q + "af_sub"), 6, 50, 10);
                Zij.stem(p, Q + "naam", Q + "af_zeg");
                Zij.hint(p, Q + "hint.doop");
            }
        }
    }

    private static void hamer(ServerPlayer p, Vec3 helling) {
        p.level().playSound(null, helling.x, helling.y, helling.z, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3f, 1.6f);
    }

    static void vergeet(UUID id) {
        BOUW.remove(id);
    }
}
