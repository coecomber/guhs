package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bezocht;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.ErfenisMomenten;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The entry of De Erfenis (ronde 1-2): {@value #WACHTTIJD} ticks of PLAYTIME (counted while online, persisted) after a player
 * first came into the Guhmension, the Postguh pops up next to them with a fat envelope: {@code guhs:makelaarsbrief}, from
 * makelaar Guhrrit. Reading it (right-click) shows the letter (client: MakelaarsbriefScherm) with the offer: an "opknapper"
 * for one kaasknabbel ("bouwkundig rapport: vads"). Buying takes one kaasknabbel, hands out the {@link ErfhekjeBlock Erfhekje}
 * and fires the moment {@code brief_gelezen}. After that the letter still reads, with a stamp "VERKOCHT", and sells you an
 * extra Erfhekje for one kaasknabbel (lost it, or want a second one).
 * <p>
 * Lost the letter before buying? {@code /guhs erfenis brief} (op) or wait: a player in state "bezorgd" without a letter in
 * the pockets gets a fresh one from the Postguh after another {@value #OPNIEUW} ticks of playtime.
 */
public final class Makelaarsbrief {
    /** Half an hour of playtime. */
    public static final int WACHTTIJD = 20 * 60 * 30;
    /** A lost letter comes again after this much more playtime. */
    static final int OPNIEUW = 20 * 60 * 20;
    /** How long the Postguh stays (ticks). */
    static final int POSTGUH_TIJD = 20 * 5;
    static final String POSTGUH_TAG = "guhs_erfenis_postguh";

    /** Postguhs standing next to somebody now: entity uuid -> server tick to go. */
    private static final Map<UUID, Long> POSTGUHS = new ConcurrentHashMap<>();

    private Makelaarsbrief() {
    }

    // =====================================================================================================================
    // the item
    // =====================================================================================================================

    public static class BriefItem extends Item {
        public BriefItem(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer p) {
                open(p);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.makelaarsbrief.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Shows the letter to this player (the state decides the buttons). */
    public static void open(ServerPlayer p) {
        KoppelingData d = KoppelingData.get(p.level().getServer());
        KoppelingData.Speler s = d.speler(p.getUUID());
        if (s.briefStaat == KoppelingData.WACHT) {
            s.briefStaat = KoppelingData.BEZORGD;   // (a letter from creative or a command counts as delivered)
            d.setDirty();
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.1f);
        KoppelingPayloads.stuur(p, new KoppelingPayloads.BriefOpen(s.briefStaat == KoppelingData.GEKOCHT, telKnabbels(p)));
    }

    // =====================================================================================================================
    // buying
    // =====================================================================================================================

    /** The result of a buy: done, no kaasknabbel, no letter in the pockets. */
    public enum Koop { GEKOCHT, EXTRA, GEEN_KNABBEL, GEEN_BRIEF }

    /** "Kopen voor 1 kaasknabbel" (first time: the Erfhekje + the moment brief_gelezen; later: an extra Erfhekje). */
    public static Koop koop(ServerPlayer p) {
        if (!heeftBrief(p) && !p.isCreative()) {
            p.sendSystemMessage(Component.translatable("erfenis.koppeling.brief.kwijt").withStyle(ChatFormatting.RED));
            return Koop.GEEN_BRIEF;
        }
        if (!neemKnabbel(p)) {
            p.sendSystemMessage(Component.translatable("erfenis.koppeling.brief.geen_knabbel").withStyle(ChatFormatting.RED));
            return Koop.GEEN_KNABBEL;
        }
        MinecraftServer server = p.level().getServer();
        KoppelingData d = KoppelingData.get(server);
        KoppelingData.Speler s = d.speler(p.getUUID());
        boolean eerste = s.briefStaat != KoppelingData.GEKOCHT;
        s.briefStaat = KoppelingData.GEKOCHT;
        d.setDirty();
        geef(p, new ItemStack(KoppelingFeature.ERFHEKJE_ITEM.get()));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        if (eerste) {
            geef(p, new ItemStack(KoppelingFeature.LEVERKIST_ITEM.get()));   // (the opknapper comes with one Leverkist for your base)
            p.sendSystemMessage(Component.translatable("erfenis.koppeling.brief.gekocht").withStyle(ChatFormatting.LIGHT_PURPLE));
            ErfenisMomenten.melding(server, p.getUUID(), "brief_gelezen");
            return Koop.GEKOCHT;
        }
        p.sendSystemMessage(Component.translatable("erfenis.koppeling.brief.extra").withStyle(ChatFormatting.LIGHT_PURPLE));
        return Koop.EXTRA;
    }

    /** Has this player bought their erfenis (the letter's opknapper)? For the core: the overview key, the Erfhekje. */
    public static boolean gekocht(MinecraftServer server, UUID speler) {
        KoppelingData.Speler s = KoppelingData.get(server).spelerAls(speler);
        return s != null && s.briefStaat == KoppelingData.GEKOCHT;
    }

    /** 0 = waiting for the post, 1 = delivered, 2 = bought. */
    public static int staat(MinecraftServer server, UUID speler) {
        KoppelingData.Speler s = KoppelingData.get(server).spelerAls(speler);
        return s == null ? KoppelingData.WACHT : s.briefStaat;
    }

    /** Ticks of playtime counted since the first visit to the Guhmension. */
    public static int ticks(MinecraftServer server, UUID speler) {
        KoppelingData.Speler s = KoppelingData.get(server).spelerAls(speler);
        return s == null ? 0 : s.briefTicks;
    }

    // =====================================================================================================================
    // the clock and the post
    // =====================================================================================================================

    /** Every second: the clock of every online player who has been in the Guhmension, and the Postguhs that are done. */
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long nu = server.getTickCount();
        if (!POSTGUHS.isEmpty()) {
            POSTGUHS.entrySet().removeIf(e -> {
                if (e.getValue() > nu) {
                    return false;
                }
                for (ServerLevel l : server.getAllLevels()) {
                    Entity en = l.getEntity(e.getKey());
                    if (en != null) {
                        poef(l, en.position());
                        en.discard();
                    }
                }
                return true;
            });
        }
        if (nu % 20 != 9) {
            return;
        }
        KoppelingData d = KoppelingData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            klok(p, d, 20);
        }
    }

    /** Counts {@code ticks} of playtime for this player and delivers when the time has come. True: the letter came now. */
    static boolean klok(ServerPlayer p, KoppelingData d, int ticks) {
        if (p.isSpectator() || !p.isAlive()) {
            return false;
        }
        KoppelingData.Speler s = d.spelerAls(p.getUUID());
        if (s == null) {
            if (!Bezocht.dimensie(p, ModDimensions.GUHMENSION) && p.level().dimension() != ModDimensions.GUHMENSION) {
                return false;   // (the clock starts at the first visit to the Guhmension)
            }
            s = d.speler(p.getUUID());
        }
        if (s.briefStaat == KoppelingData.GEKOCHT || s.briefStaat == KoppelingData.BEZORGD && heeftBrief(p)) {
            return false;
        }
        int grens = s.briefStaat == KoppelingData.WACHT ? WACHTTIJD : WACHTTIJD + OPNIEUW;
        if (s.briefTicks < grens) {
            s.briefTicks += ticks;
            d.setDirty();
            return false;
        }
        if (!magNu(p)) {
            return false;
        }
        bezorg(p);
        s.briefStaat = KoppelingData.BEZORGD;
        s.briefTicks = WACHTTIJD;   // (lost again: the next one after OPNIEUW more)
        d.setDirty();
        return true;
    }

    /** Is this a good moment for the Postguh (no scene, nothing ridden, not in the village itself)? */
    static boolean magNu(ServerPlayer p) {
        return !Cutscenes.bezig(p) && p.getVehicle() == null && !Erfenis.in(p) && !p.isSleeping()
                && !nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(p);   // (1.10: never in the Vadsrijk, the letter waits)
    }

    /** The Postguh pops up next to the player, hands over the letter, waves, and is gone after a few seconds. */
    public static void bezorg(ServerPlayer p) {
        ServerLevel level = p.level();
        postguhErbij(p);
        geef(p, new ItemStack(KoppelingFeature.MAKELAARSBRIEF.get()));
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
        p.sendSystemMessage(Component.translatable("erfenis.koppeling.postguh").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("erfenis.koppeling.brief.bezorgd", p.getName()).withStyle(ChatFormatting.WHITE)));
    }

    /**
     * (1.9 afwerking) Other post the same way: the Postguh pops up (not during a scene, a ride or sleep), hands over the item
     * and says his line (the makelaar's postcards from his holiday).
     */
    public static void bezorgPost(ServerPlayer p, ItemStack spul, Component regel) {
        boolean vadsrijk = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(p);
        if (!Cutscenes.bezig(p) && p.getVehicle() == null && !p.isSleeping() && !vadsrijk) {
            postguhErbij(p);
        }
        if (vadsrijk) {
            // (1.10: no Postguh in the Vadsrijk, and the post belongs in the normal inventory: it waits there)
            nl.juiced.guhs.feature.vadsrijk.kern.Inventaris.legThuis(p, spul);
        } else {
            geef(p, spul);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
        p.sendSystemMessage(Component.translatable("erfenis.koppeling.postguh").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                .append(regel.copy().withStyle(ChatFormatting.WHITE)));
    }

    /** The Postguh himself: pops up next to the player, waves, and is gone after a few seconds (no room: he stays away). */
    private static void postguhErbij(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 plek = plekVoorPostguh(level, p);
        if (plek != null) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (npc != null) {
                npc.setKind(GuhNpcEntity.Kind.POSTGUH);
                Vec3 naar = p.position().subtract(plek);
                float yaw = (float) (Math.toDegrees(Math.atan2(-naar.x, naar.z)));
                npc.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
                npc.yHeadRot = yaw;
                npc.yBodyRot = yaw;
                npc.setNoAi(true);
                npc.setInvulnerable(true);
                npc.setCustomName(Component.translatable("erfenis.koppeling.postguh"));
                npc.addTag(POSTGUH_TAG);
                if (level.addFreshEntity(npc)) {
                    POSTGUHS.put(npc.getUUID(), level.getServer().getTickCount() + (long) POSTGUH_TIJD);
                    poef(level, plek);
                }
            }
        }
    }

    /** A spot two blocks in front of the player (or beside them) with room to stand; null: no room (then the letter comes alone). */
    @Nullable
    static Vec3 plekVoorPostguh(ServerLevel level, ServerPlayer p) {
        Vec3 kijk = Vec3.directionFromRotation(0f, p.getYRot());
        double[][] proberen = {{kijk.x * 2, kijk.z * 2}, {-kijk.z * 2, kijk.x * 2}, {kijk.z * 2, -kijk.x * 2}, {kijk.x * 1.5, kijk.z * 1.5}};
        for (double[] v : proberen) {
            BlockPos basis = BlockPos.containing(p.getX() + v[0], p.getY(), p.getZ() + v[1]);
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos pos = basis.above(dy);
                if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
        }
        return null;
    }

    static void poef(ServerLevel level, Vec3 plek) {
        level.sendParticles(ParticleTypes.POOF, plek.x, plek.y + 0.8, plek.z, 12, 0.3, 0.5, 0.3, 0.02);
        level.sendParticles(ParticleTypes.HEART, plek.x, plek.y + 1.6, plek.z, 2, 0.3, 0.2, 0.3, 0);
        level.playSound(null, BlockPos.containing(plek), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.8f, 1.3f);
    }

    /** A Postguh saved with its chunk anyway (a server stop in the middle) does not come back. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(POSTGUH_TAG)) {
            event.setCanceled(true);
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    static boolean heeftBrief(Player p) {
        return p.getInventory().contains(s -> s.is(KoppelingFeature.MAKELAARSBRIEF.get()));
    }

    static int telKnabbels(Player p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.KAAS_KNABBELS.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    static boolean neemKnabbel(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.KAAS_KNABBELS.get())) {
                s.shrink(1);
                p.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    /** Into the pockets, or on the ground in front of the player when they are full. */
    static void geef(Player p, ItemStack stack) {
        if (!p.getInventory().add(stack) && !stack.isEmpty()) {
            p.drop(stack, false);
        }
    }
}
