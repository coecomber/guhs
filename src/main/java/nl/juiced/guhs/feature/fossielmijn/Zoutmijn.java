package nl.juiced.guhs.feature.fossielmijn;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (fossiel-mijn): the Zoutkristalmijn. Questline {@link FossielmijnFeature#MIJNWERKER} (per player):
 * <ol start="0">
 *   <li>talk to the Mijnwerker-guh;</li>
 *   <li>hack {@link #PUIN_NODIG} lumps of rubble off the cart track ({@link #puinWeg}; the rubble falls back by itself);</li>
 *   <li>find the vein in the grotto: hack into it ({@link #hak});</li>
 *   <li>bring him {@link #BOTERHAM} zoutkristallen: the Zoutkristalhouweel.</li>
 * </ol>
 * <b>The vein is every player's own, and it never runs dry.</b> A vein block never breaks. Each player has a stock of salt
 * in "the vein" (any vein block of any Zoutkristalmijn): at most {@link #VOORRAAD_MAX}, one more every
 * {@link #AANGROEI_TICKS} ticks, kept in the player's saved guh data ({@link #VOORRAAD}, {@link #VOORRAAD_TIJD}). A hit that
 * hacks the block "loose" takes one from the stock and gives one zoutkristal (two with the Zoutkristalhouweel). So the
 * source works for any number of players, and in a world where every ore-bearing chunk was dug out long ago.
 */
public final class Zoutmijn {
    public static final String VOORRAAD = "guhs_fossielmijn_zout", VOORRAAD_TIJD = "guhs_fossielmijn_zout_tijd";
    /** The stock of a player who was never here before, and the most it grows back to. */
    public static final int VOORRAAD_MAX = 24;
    /** Ticks per crystal that grows back (30 s: an empty vein is full again after 12 minutes). */
    public static final int AANGROEI_TICKS = 600;
    public static final String PUIN = "puin";
    public static final int PUIN_NODIG = 5, BOTERHAM = 3, TIPS = 3;

    private Zoutmijn() {
    }

    // --- the vein -----------------------------------------------------------------------------------------------------------

    private static long nu(ServerPlayer p) {
        return p.level().getServer().overworld().getGameTime();
    }

    /** This player's stock of salt in the vein, now (what grew back since the last time is added). */
    public static int voorraad(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        Optional<Integer> bewaard = saved.getInt(VOORRAAD);
        if (bewaard.isEmpty()) {
            return VOORRAAD_MAX;
        }
        long sinds = Math.max(0L, nu(p) - saved.getLongOr(VOORRAAD_TIJD, nu(p)));
        return (int) Math.min(VOORRAAD_MAX, bewaard.get() + sinds / AANGROEI_TICKS);
    }

    /** Sets the stock (the op command, tests): it grows back from now. */
    public static void zetVoorraad(ServerPlayer p, int n) {
        CompoundTag saved = GuhQuests.saved(p);
        saved.putInt(VOORRAAD, Math.max(0, Math.min(VOORRAAD_MAX, n)));
        saved.putLong(VOORRAAD_TIJD, nu(p));
    }

    /** (tests) as if the stock was last touched this many ticks ago. */
    public static void verschuif(ServerPlayer p, long ticks) {
        CompoundTag saved = GuhQuests.saved(p);
        saved.putLong(VOORRAAD_TIJD, saved.getLongOr(VOORRAAD_TIJD, nu(p)) - ticks);
    }

    /** One crystal out of the stock; the time it grows back from keeps the part of a crystal that was already growing. */
    private static void neemEen(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        long nu = nu(p);
        int voor = voorraad(p);
        long sinds = Math.max(0L, nu - saved.getLongOr(VOORRAAD_TIJD, nu));
        long rest = voor >= VOORRAAD_MAX || saved.getInt(VOORRAAD).isEmpty() ? 0L : sinds % AANGROEI_TICKS;
        saved.putInt(VOORRAAD, voor - 1);
        saved.putLong(VOORRAAD_TIJD, nu - rest);
    }

    private static void meld(ServerPlayer p, String key, ChatFormatting kleur, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
    }

    /**
     * A player hacked a vein block "loose" (it stays where it is): salt from their own stock. Only once the Mijnwerker-guh's
     * track is clear (step 2: finding the vein is that step), and only with something that can mine it (a pickaxe).
     * Returns how many zoutkristallen the player got.
     */
    public static int hak(ServerPlayer p, BlockPos pos, BlockState state, ItemStack tool) {
        Verhaallijn lijn = FossielmijnFeature.MIJNWERKER;
        ServerLevel level = p.level();
        if (lijn.stap(p) < 2) {
            meld(p, "gui.guhs.fossielmijn.ader.eerst", ChatFormatting.LIGHT_PURPLE);
            return 0;
        }
        if (!tool.isCorrectToolForDrops(state)) {
            meld(p, "gui.guhs.fossielmijn.ader.houweel", ChatFormatting.LIGHT_PURPLE);
            return 0;
        }
        if (voorraad(p) <= 0) {
            meld(p, "gui.guhs.fossielmijn.ader.op", ChatFormatting.LIGHT_PURPLE);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8f, 0.5f);
            return 0;
        }
        neemEen(p);
        int n = tool.is(FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()) ? 2 : 1;
        Minigames.give(p, new ItemStack(FossielmijnFeature.ZOUTKRISTAL.get(), n));
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1f, 1.1f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.45, 0.45, 0.45, 0.02);
        if (lijn.verder(p, 2)) {
            meld(p, "gui.guhs.fossielmijn.ader.gevonden", ChatFormatting.GOLD);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        } else {
            meld(p, "gui.guhs.fossielmijn.ader.hak", ChatFormatting.YELLOW, n, voorraad(p));
        }
        return n;
    }

    // --- the rubble ---------------------------------------------------------------------------------------------------------

    /** This player hacked a lump of rubble off the track: it counts while they are clearing it for the Mijnwerker-guh. */
    public static void puinWeg(ServerPlayer p) {
        Verhaallijn lijn = FossielmijnFeature.MIJNWERKER;
        if (lijn.stap(p) != 1) {
            return;
        }
        int n = lijn.teller(p, PUIN) + 1;
        lijn.teller(p, PUIN, n);
        if (n >= PUIN_NODIG) {
            lijn.verder(p, 1);
            meld(p, "gui.guhs.fossielmijn.puin_klaar", ChatFormatting.GOLD);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        } else {
            meld(p, "gui.guhs.fossielmijn.puin", ChatFormatting.YELLOW, n);
        }
    }

    // --- the Mijnwerker-guh -------------------------------------------------------------------------------------------------

    /** The role of the Mijnwerker-guh; after the questline he sells a new Zoutkristalhouweel to whoever wore theirs out. */
    public static final class Rol extends QuestRol {
        public Rol() {
            super(FossielmijnFeature.MIJNWERKER);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.7f);
            switch (stap) {
                case 0 -> {
                    zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.hallo1");
                    zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.hallo2");
                    verder(p, 0);
                }
                case 1 -> zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.puin", PUIN_NODIG - FossielmijnFeature.MIJNWERKER.teller(p, PUIN));
                case 2 -> zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.ader");
                case 3 -> {
                    if (!heeft(p, FossielmijnFeature.ZOUTKRISTAL.get(), BOTERHAM)) {
                        zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.breng", GuhQuests.count(p, FossielmijnFeature.ZOUTKRISTAL.get()));
                    } else if (neem(p, FossielmijnFeature.ZOUTKRISTAL.get(), BOTERHAM) && verder(p, 3)) {
                        zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.klaar1");
                        zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.klaar2");
                        geefEenmalig(p, "houweel", new ItemStack(FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()));
                        zichtbaar(p, "barbecuether/fossiel_mijn_mijnwerker");
                        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
                        if (npc.level() instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1.2, npc.getZ(), 24, 0.6, 0.6, 0.6, 0.02);
                        }
                    }
                }
                default -> {
                    if (npc.getRandom().nextBoolean()) {
                        zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.voorraad", voorraad(p));
                    } else {
                        zeg(p, npc, "quest.guhs.fossielmijn.mijnwerker.tip" + npc.getRandom().nextInt(TIPS));
                    }
                    npc.openShop(p);
                }
            }
        }

        @Nullable
        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            MerchantOffers offers = new MerchantOffers();
            offers.add(new MerchantOffer(new ItemCost(FossielmijnFeature.ZOUTKRISTAL.get(), 6), Optional.of(new ItemCost(ModItems.KAAS_KNABBELS.get(), 16)),
                    new ItemStack(FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()), Integer.MAX_VALUE, 0, 0));
            offers.add(new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 6), Optional.empty(),
                    new ItemStack(FossielmijnFeature.ZOUTKRISTALLETJES.get(), 2), Integer.MAX_VALUE, 0, 0));
            return offers;
        }
    }

    /** What the Guhdex shows as "needed" for a step. */
    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        return switch (stap) {
            case 1 -> List.of(Verhaallijn.nodig("guhs:fossielmijn_puin", FossielmijnFeature.MIJNWERKER.teller(p, PUIN), PUIN_NODIG));
            case 3 -> List.of(Verhaallijn.nodig("guhs:zoutkristal", GuhQuests.count(p, FossielmijnFeature.ZOUTKRISTAL.get()), BOTERHAM));
            default -> List.of();
        };
    }
}
