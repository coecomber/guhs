package nl.juiced.guhs.feature.elftocht;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhpolder.PinguhMeeglijden;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The Elf-Guhjestocht itself: per player a ride ({@link Rit}; friends can ride at the same time, each with their own time).
 * <ul>
 *   <li>{@link #start}: Schaatsmeester Guhglij lends skates and a stempelkaart, puts you on the start line (in front of the
 *       start arch, heading east) and counts down 3-2-1 - his whistle: VAHOEG! The clock runs from there.</li>
 *   <li>{@link #stempel}: at every Stempelguh in the fixed order {@link #VOLGORDE} (2..11, then 1 = Guhwarden: the finish):
 *       plof! A split time in the actionbar, green when it's faster than at that village in your best tour, red when
 *       slower. The wrong village doesn't count ("njeg, eerst naar ...").</li>
 *   <li>{@link #finish}: the total time on the board {@code elfguhjestocht}, your best tour's splits and the best split per
 *       village kept, and elfstempels: always {@link #BASIS}, +{@link #PER_GRENS} for every speed mark in {@link #SNEL}
 *       (so up to 8 extra); the very first time the Elf-Guhjeskruisje and {@link #EERSTE_KEER} extra (2.10.1: more generous).</li>
 *   <li>Free skating ({@code vrij}): skates, no card, no clock, no coins.</li>
 * </ul>
 * A ride stops when you log out, die, change dimension, leave the tour or take longer than {@link #MAX_TICKS}; the loaned
 * things go back. "The tour" is the WHOLE Elf-Guhjestocht structure (its bounding box, {@link #MARGE} blocks around it
 * too, see {@link #opDeTocht}); only after {@link #GRATIE} ticks outside it do the skates go back (2.10: in 2.9 a circle of
 * 220 around Guhwarden, at the edge of the square, took them away on the far half of the canal). Outside a structure
 * (a test rink) the circle of {@link #VERLATEN} around the start stays the rule.
 */
public final class ElftochtTocht {
    public static final String BOARD = "elfguhjestocht";
    /** The stamp order: village 2 first, ..., village 11, and Guhwarden (1) last: that's the finish. */
    public static final int[] VOLGORDE = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 1};
    public static final int AFTELLEN = 60;
    public static final int MAX_TICKS = 20 * 60 * 20;
    public static final int VERLATEN = 220;
    /** How far around the tour's square you may still skate (blocks), and how long outside it (ticks) before the skates go
     *  back. */
    public static final int MARGE = 16, GRATIE = 60;
    /** Elfstempels for every finished tour (2.10.1: was 5; the whole shop costs 26, a tour is a long ride). */
    public static final int BASIS = 12;
    /** Extra elfstempels per speed mark beaten (2.10.1: was 1). */
    public static final int PER_GRENS = 2;
    /** Extra elfstempels the very first time you finish (with the kruisje). */
    public static final int EERSTE_KEER = 5;
    /** Speed marks (ticks): every one you beat is +{@link #PER_GRENS} elfstempels. */
    public static final int[] SNEL = {20 * 60 * 6, 20 * 60 * 5, 20 * (4 * 60 + 15), 20 * (3 * 60 + 40)};
    /** Saved per player (GuhQuests.saved): the splits of your best tour, the best split per village, tours done, kruisje. */
    public static final String PB = "guhs_elftocht_pb", BESTE = "guhs_elftocht_beste", RITTEN = "guhs_elftocht_ritten",
            KRUISJE = "guhs_elftocht_kruisje";

    /** One player's ride. */
    public static final class Rit {
        public final UUID speler;
        public final boolean vrij;
        public final BlockPos start;
        public final ResourceKey<Level> dimensie;
        public final float richting;
        int aftellen;
        int tijd;
        int volgende;
        final int[] splits = new int[VOLGORDE.length];
        int splitToon;
        /** The tour's square (the structure's bounding box) when the ride started at a tour, else null. */
        @Nullable
        BoundingBox gebied;
        /** Ticks in a row outside the tour. */
        int buiten;

        Rit(UUID speler, boolean vrij, BlockPos start, ResourceKey<Level> dimensie, float richting) {
            this.speler = speler;
            this.vrij = vrij;
            this.start = start;
            this.dimensie = dimensie;
            this.richting = richting;
            this.aftellen = vrij ? 0 : AFTELLEN;
        }

        public int tijd() {
            return tijd;
        }

        public int volgende() {
            return volgende;
        }

        public int aftellen() {
            return aftellen;
        }

        public int[] splits() {
            return splits.clone();
        }
    }

    private static final Map<UUID, Rit> RITTEN_NU = new ConcurrentHashMap<>();

    // --- who is riding ---------------------------------------------------------------------------------------------------

    /** Skating now (on the tour or free skating)? */
    public static boolean isBezig(Player player) {
        return RITTEN_NU.containsKey(player.getUUID());
    }

    /** On the tour (not free skating)? */
    public static boolean opTocht(Player player) {
        Rit rit = RITTEN_NU.get(player.getUUID());
        return rit != null && !rit.vrij;
    }

    @Nullable
    public static Rit rit(Player player) {
        return RITTEN_NU.get(player.getUUID());
    }

    public static int rijders() {
        return RITTEN_NU.size();
    }

    // --- the rules --------------------------------------------------------------------------------------------------------

    /** How many speed marks this time beats (0-4). */
    public static int bonus(int ticks) {
        int n = 0;
        for (int mark : SNEL) {
            if (ticks < mark) {
                n++;
            }
        }
        return n;
    }

    /** The extra elfstempels for speed ({@link #PER_GRENS} per mark beaten). */
    public static int snelheid(int ticks) {
        return PER_GRENS * bonus(ticks);
    }

    /** The elfstempels for a finished tour (without the first-time extra). */
    public static int munten(int ticks) {
        return BASIS + snelheid(ticks);
    }

    /** The colour of a split: green = faster than your best tour there, red = slower, yellow = the same / nothing yet. */
    public static ChatFormatting kleur(int split, int best) {
        if (best <= 0) {
            return ChatFormatting.YELLOW;
        }
        return split < best ? ChatFormatting.GREEN : split > best ? ChatFormatting.RED : ChatFormatting.YELLOW;
    }

    public static Component dorpNaam(int dorp) {
        return Component.translatable("gui.guhs.elftocht.dorp." + dorp);
    }

    /** Where in the order this village is (0..10), or -1. */
    public static int plaats(int dorp) {
        for (int k = 0; k < VOLGORDE.length; k++) {
            if (VOLGORDE[k] == dorp) {
                return k;
            }
        }
        return -1;
    }

    // --- start / stop -----------------------------------------------------------------------------------------------------

    /** The start line in front of Schaatsmeester Guhglij, from where he stands and faces (his RoleData, set by the template). */
    public static Vec3 startPlek(GuhNpcEntity npc) {
        float yaw = SchaatsmeesterRole.kijk(npc);
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        double vooruit = npc.roleData.contains("StartVooruit") ? npc.roleData.getFloat("StartVooruit") : 4;
        double links = npc.roleData.contains("StartLinks") ? npc.roleData.getFloat("StartLinks") : 0;
        // his left, looking along (fx, fz), is (fz, -fx)
        double x = npc.getBlockX() + 0.5 + fx * vooruit + fz * links;
        double z = npc.getBlockZ() + 0.5 + fz * vooruit - fx * links;
        return new Vec3(Math.floor(x) + 0.5, npc.getY(), Math.floor(z) + 0.5);
    }

    /** Starts the tour (or free skating) for this player at this Schaatsmeester. False when that can't be now. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player, boolean vrij) {
        if (Minigames.refuse(player, npc, Minigames.ELFTOCHT)) {
            return false;
        }
        Rit oud = RITTEN_NU.get(player.getUUID());
        if (oud != null) {
            if (oud.vrij && !vrij) {
                stop(player, null);     // from free skating straight onto the tour
            } else {
                GuhQuests.say(player, npc, "quest.guhs.elftocht.al_bezig");
                return false;
            }
        }
        float richting = SchaatsmeesterRole.kijk(npc) - 90f;   // along the start line: his left
        Vec3 plek = startPlek(npc);
        Rit rit = new Rit(player.getUUID(), vrij, npc.blockPosition(), player.level().dimension(), richting);
        rit.gebied = tochtGebied((ServerLevel) player.level(), npc.blockPosition());
        RITTEN_NU.put(player.getUUID(), rit);
        Minigames.startKeeping(player);
        geef(player, new ItemStack(ElftochtFeature.SCHAATSEN.get()));
        if (vrij) {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.vrij");
            return true;
        }
        ItemStack kaart = new ItemStack(ElftochtFeature.STEMPELKAART.get());
        StempelkaartItem.schrijf(kaart, 0, new int[0]);
        geef(player, kaart);
        player.teleportTo((ServerLevel) player.level(), plek.x, plek.y, plek.z, richting, 5f);
        GuhQuests.say(player, npc, "quest.guhs.elftocht.start", dorpNaam(VOLGORDE[0]));
        ElftochtVoortgang.grant(player, "elftocht_eerste_rit");
        return true;
    }

    private static void geef(ServerPlayer player, ItemStack stack) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(stack.getItem())) {
                return;
            }
        }
        if (player.getOffhandItem().is(stack.getItem())) {
            return;
        }
        Minigames.give(player, stack);
    }

    /** Ends a ride (the loaned things go back); reden = a lang key for the actionbar, or null. */
    public static void stop(ServerPlayer player, @Nullable String reden) {
        Rit rit = RITTEN_NU.remove(player.getUUID());
        Minigames.forget(player);
        PinguhMeeglijden.stop(player);      // the Pinguhs just follow as usual again
        opruimen(player);
        if (rit != null && reden != null) {
            player.displayClientMessage(Component.translatable(reden).withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** Takes the loaned skates and stamp card back. */
    public static void opruimen(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ElftochtFeature.SCHAATSEN.get()) || s.is(ElftochtFeature.STEMPELKAART.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu != null && (player.containerMenu.getCarried().is(ElftochtFeature.SCHAATSEN.get())
                || player.containerMenu.getCarried().is(ElftochtFeature.STEMPELKAART.get()))) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    // --- where the tour is ------------------------------------------------------------------------------------------------

    /** The square of the Elf-Guhjestocht at this spot (the structure start's bounding box), or null outside one. */
    @Nullable
    static BoundingBox tochtGebied(ServerLevel level, BlockPos pos) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ElftochtFeature.STRUCTURE);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, structure);
        return start.isValid() ? start.getBoundingBox() : null;
    }

    /** Is (x, z) still on this ride's tour: anywhere in the tour's square or up to {@link #MARGE} blocks around it; without
     *  a square (a test rink) within {@link #VERLATEN} of the start. */
    public static boolean opDeTocht(Rit rit, double x, double z) {
        BoundingBox b = rit.gebied;
        if (b != null) {
            return x >= b.minX() - MARGE && x < b.maxX() + 1 + MARGE && z >= b.minZ() - MARGE && z < b.maxZ() + 1 + MARGE;
        }
        return Math.hypot(x - rit.start.getX(), z - rit.start.getZ()) <= VERLATEN;
    }

    /** (For the tests) this player's ride as if it started at {@code start} on a tour with this square (the clock
     *  running, no countdown). */
    static Rit testRit(ServerPlayer player, BlockPos start, @Nullable BoundingBox gebied) {
        Rit oud = RITTEN_NU.get(player.getUUID());
        Rit rit = new Rit(player.getUUID(), oud != null && oud.vrij, start, player.level().dimension(), oud == null ? 0f : oud.richting);
        rit.aftellen = 0;
        rit.gebied = gebied;
        RITTEN_NU.put(player.getUUID(), rit);
        return rit;
    }

    // --- every tick -------------------------------------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        Rit rit = RITTEN_NU.get(player.getUUID());
        if (rit == null) {
            return;
        }
        Minigames.keep(player);
        if (player.level().dimension() != rit.dimensie) {
            stop(player, "gui.guhs.elftocht.verlaten");
            return;
        }
        if (opDeTocht(rit, player.getX(), player.getZ())) {
            rit.buiten = 0;
        } else if (++rit.buiten > GRATIE) {
            stop(player, "gui.guhs.elftocht.verlaten");
            return;
        } else if (rit.buiten % 10 == 1) {
            player.displayClientMessage(Component.translatable("gui.guhs.elftocht.bijna_weg").withStyle(ChatFormatting.GOLD), true);
        }
        if (player.tickCount % 20 == 0) {
            ElftochtPubliek.pinguhs(player);
        }
        if (rit.vrij) {
            if (player.tickCount % 20 == 0 && rit.buiten == 0) {
                player.displayClientMessage(Component.translatable("gui.guhs.elftocht.vrij_balk").withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        if (rit.aftellen > 0) {
            aftellen(player, rit);
            return;
        }
        rit.tijd++;
        if (rit.tijd > MAX_TICKS) {
            stop(player, "gui.guhs.elftocht.te_lang");
            return;
        }
        if (rit.splitToon > 0) {
            rit.splitToon--;
        } else if (rit.tijd % 4 == 0 && rit.buiten == 0) {
            player.displayClientMessage(Component.translatable("gui.guhs.elftocht.balk", Highscores.tijd(rit.tijd),
                    dorpNaam(VOLGORDE[rit.volgende]), rit.volgende, VOLGORDE.length).withStyle(ChatFormatting.WHITE), true);
        }
    }

    private static void aftellen(ServerPlayer player, Rit rit) {
        int t = rit.aftellen--;
        if (t % 20 == 0) {
            int n = t / 20;
            titel(player, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.elftocht.klaar"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 0.8f + 0.1f * (3 - n));
        }
        if (rit.aftellen == 0) {
            titel(player, Component.literal("VAHOEG!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.elftocht.go", dorpNaam(VOLGORDE[0])));
            player.level().playSound(null, player.blockPosition(), ElftochtFeature.FLUIT.get(), SoundSource.PLAYERS, 1.2f, 1f);
        } else if (player.getDeltaMovement().horizontalDistanceSqr() > 0.04 && rit.aftellen < AFTELLEN - 10) {
            // no false starts: stand still until the whistle
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
    }

    static void titel(ServerPlayer player, Component titel, Component onder) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(2, 16, 6));
        player.connection.send(new ClientboundSetSubtitleTextPacket(onder));
        player.connection.send(new ClientboundSetTitleTextPacket(titel));
    }

    // --- stamps -----------------------------------------------------------------------------------------------------------

    /** A player right-clicked the Stempelguh of village {@code dorp} (1..11). Returns true when it stamped. */
    public static boolean stempel(GuhNpcEntity npc, ServerPlayer player, int dorp) {
        Rit rit = RITTEN_NU.get(player.getUUID());
        if (dorp < 1 || dorp > VOLGORDE.length) {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.stempel.kwijt");
            return false;
        }
        if (rit == null) {
            GuhQuests.say(player, npc, dorp == 1 ? "quest.guhs.elftocht.stempel.geen_tocht_start" : "quest.guhs.elftocht.stempel.geen_tocht",
                    dorpNaam(dorp));
            return false;
        }
        if (rit.vrij) {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.stempel.vrij", dorpNaam(dorp));
            ((ServerLevel) npc.level()).sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.6, npc.getZ(), 3, 0.3, 0.2, 0.3, 0);
            return false;
        }
        if (rit.aftellen > 0) {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.stempel.wacht");
            return false;
        }
        int plaats = plaats(dorp);
        if (plaats < rit.volgende) {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.stempel.al", dorpNaam(dorp), dorpNaam(VOLGORDE[rit.volgende]));
            return false;
        }
        if (plaats != rit.volgende) {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.stempel.volgorde", dorpNaam(VOLGORDE[rit.volgende]));
            player.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.6f, 1.4f);
            return false;
        }
        // PLOF!
        int k = rit.volgende;
        rit.splits[k] = rit.tijd;
        rit.volgende++;
        plof(npc);
        int[] pb = GuhQuests.saved(player).getIntArray(PB);
        int best = pb.length == VOLGORDE.length ? pb[k] : 0;
        MutableComponent split = Component.translatable("gui.guhs.elftocht.split", dorpNaam(dorp), Highscores.tijd(rit.tijd));
        if (best > 0) {
            int diff = rit.tijd - best;
            split.append(Component.literal("  (" + (diff < 0 ? "-" : "+") + Highscores.tijd(Math.abs(diff)) + ")"));
        }
        player.displayClientMessage(split.withStyle(kleur(rit.tijd, best)), true);
        rit.splitToon = 50;
        ElftochtPubliek.juichAllemaal((ServerLevel) npc.level(), npc.position(), 24, player);   // the whole village cheers
        kaart(player, rit);
        if (rit.volgende >= VOLGORDE.length) {
            finish(npc, player, rit);
        } else {
            GuhQuests.say(player, npc, "quest.guhs.elftocht.stempel.plof", dorpNaam(dorp), VOLGORDE.length - rit.volgende,
                    dorpNaam(VOLGORDE[rit.volgende]));
        }
        return true;
    }

    /** The stamp: a PLOF sound, a puff of flour-white and pink hearts, and the Stempelguh's stamping animation. */
    static void plof(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        level.playSound(null, npc.blockPosition(), ElftochtFeature.PLOF.get(), SoundSource.NEUTRAL, 1f, 0.9f + level.random.nextFloat() * 0.2f);
        Vec3 voor = Vec3.directionFromRotation(0, npc.getYRot()).scale(0.6);
        level.sendParticles(ParticleTypes.POOF, npc.getX() + voor.x, npc.getY() + 0.6, npc.getZ() + voor.z, 8, 0.15, 0.1, 0.15, 0.02);
        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.7, npc.getZ(), 2, 0.3, 0.2, 0.3, 0);
        PacketDistributor.sendToPlayersTrackingEntity(npc, new ElftochtPayloads.Plof(npc.getId()));
    }

    /** A little (harmless: particles only) fireworks show over the finish: bursts of pink, orange and blue sparks. */
    static void vuurwerk(ServerLevel level, Vec3 boven) {
        int[] kleuren = {0xFF7FC0, 0xFF9020, 0x5AA0FF, 0xFFFFFF, 0xFFD040};
        for (int b = 0; b < 5; b++) {
            double bx = boven.x + (level.random.nextDouble() - 0.5) * 10, by = boven.y + level.random.nextDouble() * 4,
                    bz = boven.z + (level.random.nextDouble() - 0.5) * 10;
            int c = kleuren[b % kleuren.length];
            var stof = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((c >> 16) & 255) / 255f,
                    ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.6f);
            level.sendParticles(stof, bx, by, bz, 50, 1.4, 1.4, 1.4, 0);
            level.sendParticles(ParticleTypes.FIREWORK, bx, by, bz, 25, 0.2, 0.2, 0.2, 0.25);
        }
        level.playSound(null, BlockPos.containing(boven), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, 2f, 1f);
        level.playSound(null, BlockPos.containing(boven), SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.AMBIENT, 2f, 1f);
    }

    private static void kaart(ServerPlayer player, Rit rit) {
        int[] tijden = java.util.Arrays.copyOf(rit.splits, rit.volgende);
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ElftochtFeature.STEMPELKAART.get())) {
                StempelkaartItem.schrijf(inv.getItem(i), rit.volgende, tijden);
            }
        }
    }

    /** The card in the chat: every village, stamped or not, with your split and your best there. */
    public static void toonKaart(ServerPlayer player) {
        Rit rit = RITTEN_NU.get(player.getUUID());
        int[] pb = GuhQuests.saved(player).getIntArray(PB);
        player.sendSystemMessage(Component.translatable("gui.guhs.elftocht.kaart.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int k = 0; k < VOLGORDE.length; k++) {
            boolean klaar = rit != null && k < rit.volgende;
            MutableComponent line = Component.literal(klaar ? " ✔ " : " ○ ").append(dorpNaam(VOLGORDE[k]));
            if (klaar) {
                line.append("  " + Highscores.tijd(rit.splits[k]));
            }
            if (pb.length == VOLGORDE.length) {
                line.append(Component.literal("  (" + Highscores.tijd(pb[k]) + ")").withStyle(ChatFormatting.DARK_GRAY));
            }
            player.sendSystemMessage(line.withStyle(klaar ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }

    // --- the finish -------------------------------------------------------------------------------------------------------

    private static void finish(GuhNpcEntity npc, ServerPlayer player, Rit rit) {
        int tijd = rit.tijd;
        CompoundTag saved = GuhQuests.saved(player);
        boolean eerste = !saved.getBoolean(KRUISJE);
        int munten = munten(tijd) + (eerste ? EERSTE_KEER : 0);
        Minigames.give(player, new ItemStack(ElftochtFeature.ELFSTEMPEL.get(), munten));
        if (eerste) {
            saved.putBoolean(KRUISJE, true);
            Minigames.give(player, new ItemStack(ElftochtFeature.KRUISJE_ITEM.get()));
        }
        int[] pb = saved.getIntArray(PB);
        boolean record = pb.length != VOLGORDE.length || tijd < pb[VOLGORDE.length - 1];
        if (record) {
            saved.putIntArray(PB, rit.splits.clone());
        }
        int[] beste = saved.getIntArray(BESTE);
        if (beste.length != VOLGORDE.length) {
            beste = rit.splits.clone();
        } else {
            for (int k = 0; k < beste.length; k++) {
                beste[k] = Math.min(beste[k], rit.splits[k]);
            }
        }
        saved.putIntArray(BESTE, beste);
        saved.putInt(RITTEN, saved.getInt(RITTEN) + 1);
        int plaats = Scorebord.submit(player, BOARD, tijd, true);
        ElftochtVoortgang.grant(player, "elftocht_uitgereden");
        if (bonus(tijd) == SNEL.length) {
            ElftochtVoortgang.grant(player, "elftocht_snel");
        }
        ElftochtVoortgang.kleding(player);
        // the party
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), ElftochtFeature.FINISH.get(), SoundSource.PLAYERS, 1.2f, 1f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 60, 0.6, 0.8, 0.6, 0.35);
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 2.5, player.getZ(), 40, 1.2, 0.8, 1.2, 0.08);
        vuurwerk(level, player.position().add(0, 6, 0));
        ElftochtPubliek.juichAllemaal(level, player.position(), 48, player);    // Guhwarden and the grandstand go wild
        titel(player, Component.translatable("gui.guhs.elftocht.finish").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(Highscores.tijd(tijd)).withStyle(record ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        GuhQuests.say(player, npc, "quest.guhs.elftocht.finish", Highscores.tijd(tijd), munten, snelheid(tijd));
        if (record) {
            player.sendSystemMessage(Component.translatable("gui.guhs.elftocht.record").withStyle(ChatFormatting.GREEN));
        }
        if (eerste) {
            player.sendSystemMessage(Component.translatable("gui.guhs.elftocht.kruisje", EERSTE_KEER).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (plaats == 1) {
            player.sendSystemMessage(Component.translatable("gui.guhs.elftocht.server_record").withStyle(ChatFormatting.GOLD));
        }
        RITTEN_NU.remove(player.getUUID());
        Minigames.forget(player);
        PinguhMeeglijden.stop(player);
        opruimen(player);
    }

    /** Your best split per village (the fastest time you reached it in any tour), in stamp order; empty when none. */
    public static int[] besteSplits(Player player) {
        return GuhQuests.saved(player).getIntArray(BESTE);
    }

    // --- tests ------------------------------------------------------------------------------------------------------------

    /** (Tests) sets the clock of a running tour (and ends the countdown). */
    public static void testTijd(Player player, int ticks) {
        Rit rit = RITTEN_NU.get(player.getUUID());
        if (rit != null) {
            rit.aftellen = 0;
            rit.tijd = ticks;
        }
    }

    /** (Tests) as if the first k stamps were done already, each split 100 ticks after the previous one. */
    public static void testStempels(Player player, int k) {
        Rit rit = RITTEN_NU.get(player.getUUID());
        if (rit != null) {
            rit.aftellen = 0;
            for (int i = 0; i < k; i++) {
                rit.splits[i] = 100 * (i + 1);
            }
            rit.volgende = k;
            rit.tijd = Math.max(rit.tijd, 100 * k);
        }
    }

    /** (Tests / server stop) forget every ride. */
    public static void vergeetAlles() {
        RITTEN_NU.clear();
    }

    private ElftochtTocht() {
    }
}
