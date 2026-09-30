package nl.juiced.guhs.feature.wereldleven;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The kaasijsjes of IJscoguh Tingeling: three always, four only in their own season. Eat one yourself for a cute effect
 * (blosjes, zweverig, a little speed or healing), or give one to a guh: it gets an ice-cream hat (the IJSHOEDJE flag,
 * with the scoop in the flavour's colour), blushing cheeks (BLOSJES) and/or floats a bit (zweverig) for a while.
 * <p>
 * The flavour of the hat travels in the guh's KnusVlaggen: bits {@link #SMAAK_SHIFT}.. (3 bits, the flavour + 1).
 */
public final class Kaasijsjes {
    public enum Smaak {
        ROZE(null, 0xFF9EC8), MINT(null, 0x9FF0CF), CHOCO(null, 0x8A5536),
        BLOESEM(Seizoen.LENTE, 0xFFD2EA), ZONNETJE(Seizoen.ZOMER, 0xFFD85A), APPELTAART(Seizoen.HERFST, 0xE0A15A), SNEEUW(Seizoen.WINTER, 0xF2F8FF);

        /** The season it is sold in (null: always). */
        @Nullable
        public final Seizoen seizoen;
        /** The colour of the scoop (the guh's ice-cream hat, the particles). */
        public final int kleur;

        Smaak(@Nullable Seizoen seizoen, int kleur) {
            this.seizoen = seizoen;
            this.kleur = kleur;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String itemId() {
            return "kaasijsje_" + id();
        }

        /** Does a guh get the ice-cream hat / the blushing cheeks / float from it? */
        public boolean hoedje() {
            return this == MINT || this == CHOCO || this == ZONNETJE || this == SNEEUW || this == BLOESEM;
        }

        public boolean blosjes() {
            return this == ROZE || this == BLOESEM || this == APPELTAART || this == SNEEUW;
        }

        public boolean zweverig() {
            return this == CHOCO || this == ZONNETJE || this == SNEEUW;
        }

        @Nullable
        public static Smaak byId(String id) {
            for (Smaak s : values()) {
                if (s.id().equals(id)) {
                    return s;
                }
            }
            return null;
        }

        @Nullable
        public static Smaak byIndex(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }

        /** The seasonal flavour of this season. */
        public static Smaak van(Seizoen seizoen) {
            for (Smaak s : values()) {
                if (s.seizoen == seizoen) {
                    return s;
                }
            }
            return ROZE;
        }
    }

    /** How long a guh keeps its ice-cream hat and its blushing cheeks (a quarter of a day). */
    public static final int GUH_TICKS = 6000;
    /** The flavour of the hat: KnusVlaggen bits 5..7 (flavour + 1). Bit 4 (16) is Dagritme.DUTJE. */
    public static final int SMAAK_SHIFT = 5, SMAAK_MASK = 7 << SMAAK_SHIFT;
    static final String HOEDJE_TOT = "guhs_wereldleven_ijshoedje_tot", BLOSJES_TOT = "guhs_wereldleven_blosjes_tot";

    static void register() {
        GuhHooks.klik(Kaasijsjes::klik);
    }

    /** The flavour of a guh's ice-cream hat (client and server), or null. */
    @Nullable
    public static Smaak hoedjeSmaak(GuhEntity guh) {
        return Smaak.byIndex(((guh.getKnusVlaggen() & SMAAK_MASK) >> SMAAK_SHIFT) - 1);
    }

    /** Right-click a guh with a kaasijsje: it gets it. */
    private static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof KaasijsjeItem ijsje)) {
            return InteractionResult.PASS;
        }
        if (!guh.level().isClientSide() && player instanceof ServerPlayer sp) {
            geefGuh(guh, ijsje.smaak, sp);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** A guh eats an ice cream: its hat / blush / float, hearts, a VAHOEG! */
    public static void geefGuh(GuhEntity guh, Smaak smaak, @Nullable ServerPlayer from) {
        long now = guh.level().getGameTime();
        if (smaak.hoedje()) {
            guh.getPersistentData().putLong(HOEDJE_TOT, now + GUH_TICKS);
            GuhHooks.zet(guh, GuhHooks.IJSHOEDJE, true);
            guh.setKnusVlaggen((guh.getKnusVlaggen() & ~SMAAK_MASK) | ((smaak.ordinal() + 1) << SMAAK_SHIFT));
        }
        if (smaak.blosjes()) {
            guh.getPersistentData().putLong(BLOSJES_TOT, now + GUH_TICKS);
            GuhHooks.zet(guh, GuhHooks.BLOSJES, true);
            guh.addEffect(new MobEffectInstance(WereldlevenFeature.BLOSJES, 20 * 60, 0));
        }
        if (smaak.zweverig()) {
            guh.addEffect(new MobEffectInstance(WereldlevenFeature.ZWEVERIG, 20 * 20, 0));
        }
        guh.playSound(ModSounds.GUH_EAT.get(), 0.8f, guh.getVoicePitch() * 1.2f);
        guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight(), guh.getZ(), 6,
                    guh.getBbWidth() * 0.4, 0.2, guh.getBbWidth() * 0.4, 0.02);
        }
        guh.heal(guh.getMaxHealth() * 0.1f);
        if (GuhEmotes.canStart(guh) && guh.emotes.current() == null) {
            guh.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
        }
        if (from != null) {
            gegeten(from, smaak);
            from.sendOverlayMessage(Component.translatable("gui.guhs.wereldleven.ijsje_guh." + smaak.id(), guh.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** You eat an ice cream yourself. */
    public static void eet(LivingEntity entity, Smaak smaak) {
        switch (smaak) {
            case ROZE -> entity.addEffect(new MobEffectInstance(WereldlevenFeature.BLOSJES, 20 * 60, 0));
            case MINT -> entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 45, 0));
            case CHOCO -> entity.addEffect(new MobEffectInstance(WereldlevenFeature.ZWEVERIG, 20 * 30, 0));
            case BLOESEM -> {
                entity.addEffect(new MobEffectInstance(WereldlevenFeature.BLOSJES, 20 * 90, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0));
            }
            case ZONNETJE -> {
                entity.addEffect(new MobEffectInstance(WereldlevenFeature.ZWEVERIG, 20 * 40, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 40, 0));
            }
            case APPELTAART -> {
                entity.addEffect(new MobEffectInstance(WereldlevenFeature.BLOSJES, 20 * 60, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 0));
            }
            case SNEEUW -> {
                entity.addEffect(new MobEffectInstance(WereldlevenFeature.ZWEVERIG, 20 * 30, 0));
                entity.addEffect(new MobEffectInstance(WereldlevenFeature.BLOSJES, 20 * 60, 0));
            }
        }
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 5,
                    0.3, 0.2, 0.3, 0.02);
        }
        if (entity instanceof ServerPlayer player) {
            gegeten(player, smaak);
        }
    }

    /** Counts an ice cream (eaten or given) for the Knus tab, the ijsjes collection and the advancement. */
    static void gegeten(ServerPlayer player, Smaak smaak) {
        KnusVoortgang.tel(player, WereldlevenVoortgang.IJSJES, 1);
        KnusVoortgang.ontdek(player, WereldlevenVoortgang.IJSJES_BOEK, smaak.id());
        WereldlevenVoortgang.toon(player, "wereldleven_ijsje");
        if (KnusVoortgang.ontdekt(player, WereldlevenVoortgang.IJSJES_BOEK).size() >= Smaak.values().length) {
            nl.juiced.guhs.quest.GuhAdvancements.grant(player, "wereldleven_alle_ijsjes");
        }
    }

    /** (Guh tick, every 20 ticks) the hat and the cheeks go away after a while. */
    static void tick(GuhEntity guh) {
        int flags = guh.getKnusVlaggen();
        if ((flags & (GuhHooks.IJSHOEDJE | GuhHooks.BLOSJES)) == 0) {
            return;
        }
        long now = guh.level().getGameTime();
        if ((flags & GuhHooks.IJSHOEDJE) != 0 && guh.getPersistentData().getLongOr(HOEDJE_TOT, 0L) <= now) {
            GuhHooks.zet(guh, GuhHooks.IJSHOEDJE, false);
            guh.setKnusVlaggen(guh.getKnusVlaggen() & ~SMAAK_MASK);
            guh.getPersistentData().remove(HOEDJE_TOT);
        }
        if ((flags & GuhHooks.BLOSJES) != 0 && guh.getPersistentData().getLongOr(BLOSJES_TOT, 0L) <= now) {
            GuhHooks.zet(guh, GuhHooks.BLOSJES, false);
            guh.getPersistentData().remove(BLOSJES_TOT);
        }
    }

    private Kaasijsjes() {
    }
}
