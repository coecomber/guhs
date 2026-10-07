package nl.juiced.guhs.feature.einde2.vesting;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeReis;
import nl.juiced.guhs.feature.guheinde.MagereCellen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice vesting: the cells with magere guhs in the modular Mika-vesting (the cellenblok next to the keep, and the
 * two cells of every kerkertoren). They work like the cells of a Knabbelkelder and lean on the same code
 * ({@link MagereCellen}): the grey guh stays in its cell for every player and remembers who fed it, the colourful guh it
 * sets free is a new one that runs off, an empty cell gets a new magere guh after a while.
 * <p>
 * What is ours: where the cells are (the markers {@code cel_<n>} of the sidecar, tools/features/einde2_vesting_bouw.py),
 * the tag every cell guh of a vesting carries ({@link #TAG}: the templates give it, and so does {@link #controleer} to a
 * guh it puts back), and the count per player ({@link #bevrijd}) with its two hidden advancements.
 */
public final class VestingCellen {
    /** Player data (GuhQuests.saved): how many magere guhs this player freed from vesting cells. */
    public static final String BEVRIJD = "guhs_einde2_vesting_bevrijd";
    /** Entity tag of a magere guh that lives in a cell of a vesting. */
    public static final String TAG = "guhs_einde2_vesting_cel";
    /** This many freed: the second advancement (and, later, the title). */
    public static final int BEVRIJDER = 6;
    /** A player this close to a piece of a vesting has its cells looked at. */
    public static final int BEREIK = 48;
    public static final String EERSTE = "einde2_vesting_bevrijd", ZES = "einde2_vesting_bevrijder";

    /** (one click, between our two listeners) the player who is about to feed a cell guh of a vesting for the first time. */
    @Nullable
    private static ServerPlayer wacht;
    @Nullable
    private static GuhEntity wachtGuh;

    /** hook fase B: how many vesting guhs this player freed (the title asks). */
    public static int bevrijd(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(BEVRIJD, 0);
    }

    /** Is this a magere guh in a cell of a vesting? */
    public static boolean isCelGuh(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.MAGER && guh.entityTags().contains(TAG);
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 100 != 0 || player.isSpectator()
                || !GuheindeFeature.isGuheinde(player.level())) {
            return;
        }
        ServerLevel level = player.level();
        StructureStart start = Plekken.bij(level, Einde2Ids.MIKA_VESTING, player.blockPosition(), BEREIK);
        if (start != null && E2VestingFeature.modulair(start)) {
            controleer(level, cellen(start), player.position());
        }
    }

    /** The cells of this copy (world positions): every {@code cel_<n>} marker, in every piece that has it. */
    public static List<BlockPos> cellen(StructureStart start) {
        List<BlockPos> uit = new ArrayList<>();
        for (Plekken.Plek plek : Plekken.van(Einde2Ids.MIKA_VESTING).values()) {
            if (plek.id().startsWith("cel_")) {
                uit.addAll(Plekken.alle(start, plek));
            }
        }
        return uit;
    }

    /**
     * Looks at these cells the way a Knabbelkelder's are looked at (strays go back, a cell near {@code bij} that has been
     * empty for a while gets a new magere guh); the new guhs carry our tag. Returns them.
     */
    public static List<GuhEntity> controleer(ServerLevel level, List<BlockPos> cellen, Vec3 bij) {
        List<GuhEntity> nieuw = MagereCellen.controleer(level, cellen, bij);
        for (GuhEntity guh : nieuw) {
            guh.addTag(TAG);
        }
        return nieuw;
    }

    /** A new magere guh in this cell of a vesting. */
    public static GuhEntity nieuwe(ServerLevel level, BlockPos cel) {
        GuhEntity guh = MagereCellen.nieuweMagere(level, cel);
        guh.addTag(TAG);
        return guh;
    }

    /**
     * Before the Guheinde's own listener feeds the guh: remember who is about to feed a cell guh of a vesting they did not
     * feed before ({@link #naKlik} counts it when the feeding really happened).
     */
    static void voorKlik(PlayerInteractEvent.EntityInteract event) {
        wacht = null;
        wachtGuh = null;
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getTarget() instanceof GuhEntity guh && event.getEntity() instanceof ServerPlayer player
                && isCelGuh(guh) && GuheindeReis.isKnabbel(player.getMainHandItem()) && !MagereCellen.heeftGevoerd(guh, player.getUUID())) {
            wacht = player;
            wachtGuh = guh;
        }
    }

    /** After it: the guh remembers this player now, so it was fed and its colourful guh ran off. One more on the count. */
    static void naKlik(PlayerInteractEvent.EntityInteract event) {
        ServerPlayer player = wacht;
        GuhEntity guh = wachtGuh;
        wacht = null;
        wachtGuh = null;
        if (player != null && guh != null && event.getEntity() == player && event.getTarget() == guh && MagereCellen.heeftGevoerd(guh, player.getUUID())) {
            tel(player);
        }
    }

    /** One more vesting guh freed by this player: the count, the advancements, a line. Returns the new count. */
    public static int tel(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        int n = saved.getIntOr(BEVRIJD, 0) + 1;
        saved.putInt(BEVRIJD, n);
        GuhAdvancements.grant(player, EERSTE);
        if (n >= BEVRIJDER) {
            GuhAdvancements.grant(player, ZES);
        }
        // hook fase B: the title "vestingbevrijder" (Titels) asks bevrijd(player) >= BEVRIJDER
        String key = n == 1 ? "gui.guhs.einde2_vesting.bevrijd.eerste" : n == BEVRIJDER ? "gui.guhs.einde2_vesting.bevrijd.zes" : "gui.guhs.einde2_vesting.bevrijd.teller";
        Component regel = Component.translatable(key, n).withStyle(ChatFormatting.LIGHT_PURPLE);
        if (n == 1 || n == BEVRIJDER) {
            player.sendSystemMessage(regel);
        } else {
            player.sendOverlayMessage(regel);
        }
        return n;
    }

    private VestingCellen() {
    }
}
