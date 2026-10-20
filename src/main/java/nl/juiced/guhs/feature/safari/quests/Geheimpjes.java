package nl.juiced.guhs.feature.safari.quests;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * The geheimpjes: one per zone, the block the zone module built (a chest in the tall grass, the crocodile's sunglasses,
 * the pot in the temple ruin, the teapot behind the pagode, the barrel in the wreck, the fossil's skull, the ranger
 * chest in the hollow oak, the lost postcard): right-click it. New: 25 safarimunten and the hidden advancement; all of
 * them: the geheimpjestrofee and (title predicate) Speurneus. The blocks themselves are in geheimpjes.json.
 */
public final class Geheimpjes {
    public static final int MUNTEN = 25;
    static final String TROFEE = "trofee_geheimpjes";

    /** A right click on a block in the park: true when it was a geheimpje (then nothing else happens). */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        QuestData.Geheimpje g = QuestData.get().geheimpjeOp(pos);
        if (g == null) {
            return false;
        }
        vind(p, g);
        return true;
    }

    /** Finds a geheimpje for this player (also used by the gametests). */
    static void vind(ServerPlayer p, QuestData.Geheimpje g) {
        MinecraftServer server = p.level().getServer();
        Component naam = Component.translatable("gui.guhs.safari.geheimpje." + g.id()).withStyle(ChatFormatting.YELLOW);
        if (!SafariSpeler.vindGeheimpje(server, p.getUUID(), g.id())) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.geheimpje.al", naam).withStyle(ChatFormatting.GRAY));
            return;
        }
        int n = gevonden(p), totaal = QuestData.get().geheimpjes.size();
        SafariQuests.zeg(p, "gui.guhs.safari.geheimpje.gevonden", naam, n, totaal);
        SafariMunten.verdienEens(p, "geheimpje_" + g.id(), MUNTEN, "geheimpje");
        Beloningen.adv(p, "safari_geheimpje");
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, g.pos().getX() + 0.5, g.pos().getY() + 1.0, g.pos().getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.0);
            level.playSound(null, g.pos(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        if (allemaal(p)) {
            allesGevonden(p);
        }
    }

    static void allesGevonden(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        Beloningen.adv(p, "safari_geheimpjes_alle");
        if (!SafariSpeler.vlag(server, p.getUUID(), TROFEE)) {
            SafariSpeler.zetVlag(server, p.getUUID(), TROFEE, true);
            SafariQuests.zeg(p, "gui.guhs.safari.geheimpje.alle");
            SafariQuests.geef(p, new ItemStack(SafariQuests.GEHEIMPJES_TROFEE_ITEM.get()));
        }
    }

    public static int gevonden(ServerPlayer p) {
        java.util.Set<String> ids = SafariSpeler.geheimpjes(p.level().getServer(), p.getUUID());
        int n = 0;
        for (QuestData.Geheimpje g : QuestData.get().geheimpjes) {
            if (ids.contains(g.id())) {
                n++;
            }
        }
        return n;
    }

    /** All the geheimpjes of the park (false when the park has none: then nobody is a Speurneus yet). */
    public static boolean allemaal(ServerPlayer p) {
        int totaal = QuestData.get().geheimpjes.size();
        return totaal > 0 && gevonden(p) >= totaal;
    }

    private Geheimpjes() {
    }
}
