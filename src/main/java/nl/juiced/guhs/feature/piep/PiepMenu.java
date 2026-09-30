package nl.juiced.guhs.feature.piep;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The little right-click menu of a piep-maatje (server side; the screen is client.PiepMenuScreen).
 * <p>
 * <b>Click rules</b> (the same for the pieppiepmuisje, Poepschilly and Schilly):
 * <ul>
 *   <li>holding its food (kaasknabbel; the turtles also zeewier): feeding / taming, as before;</li>
 *   <li>any other item: the item's own use (lead, name tag...), as before;</li>
 *   <li>empty hand, the owner: a little love (hearts) and its menu opens;</li>
 *   <li>sneak + empty hand, the owner: picked up straight away ({@link PiepDierItem}; the same as "Oppakken" in the menu);</li>
 *   <li>empty hand, anyone else: a little love only (the muisje is petted: aaien counts, as before).</li>
 * </ul>
 * A hidden muisje or a Poepschilly inside a guh can't be clicked into a menu (clicking the hidden muisje finds it).
 */
public final class PiepMenu {
    /** The same limit as renaming a guh (GuhActionPayload.MAX_NAME_LENGTH). */
    public static final int MAX_NAAM = nl.juiced.guhs.network.GuhActionPayload.MAX_NAME_LENGTH;
    /** How far away the owner may be to use the menu buttons. */
    public static final double BEREIK = 10.0;

    public enum Actie {
        /** Switch a setting (waarde = PiepInstelling ordinal). */
        WISSEL,
        /** Rename (tekst; empty = no name any more), exactly like a guh. */
        NAAM,
        /** Pick it up (like sneak + click). */
        OPPAKKEN,
        /** The big button: shoulder (muisje), poetsbeurt (Poepschilly), bestie-moment (Schilly). */
        SPECIAAL;

        public static Actie byIndex(int i) {
            Actie[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    private PiepMenu() {
    }

    /** May this player use the menu of this maatje (its owner, close by, and it is free)? */
    public static boolean mag(Player player, PiepMaatje maatje) {
        TamableAnimal dier = maatje.dier();
        return dier.isAlive() && dier.isTame() && dier.isOwnedBy(player) && player.distanceToSqr(dier) <= BEREIK * BEREIK;
    }

    /** Opens the menu for the owner (hearts and a happy little animation, then the screen). */
    public static void open(ServerPlayer player, PiepMaatje maatje) {
        TamableAnimal dier = maatje.dier();
        if (dier.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, dier.getX(), dier.getY() + dier.getBbHeight() + 0.15, dier.getZ(), 1, 0.15, 0.05, 0.15, 0);
        }
        ModNetworking.sendTo(player, new PiepPayloads.MenuOpen(dier.getId(), maatje.rustSeconden()));
    }

    /** A button of the menu (after the checks). Returns whether something happened. */
    public static boolean doe(ServerPlayer player, PiepMaatje maatje, Actie actie, int waarde, String tekst) {
        if (!mag(player, maatje)) {
            return false;
        }
        switch (actie) {
            case WISSEL -> {
                PiepInstelling instelling = PiepInstelling.byIndex(waarde);
                if (!maatje.instellingen().contains(instelling)) {
                    return false;
                }
                boolean nu = !maatje.aan(instelling);
                maatje.zet(instelling, nu);
                player.sendOverlayMessage(Component.translatable(instelling.key() + (nu ? ".aan" : ".uit"), maatje.dier().getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                return true;
            }
            case NAAM -> {
                hernoem(maatje.dier(), tekst);
                return true;
            }
            case OPPAKKEN -> {
                return PiepDierItem.pakOp(maatje, player);
            }
            case SPECIAAL -> {
                if (maatje.isBezig()) {
                    return false;
                }
                maatje.speciaal(player);
                return true;
            }
        }
        return false;
    }

    /** Renaming: the same as renaming a guh in its menu (GuhActionPayload RENAME): trimmed, empty = no name. */
    public static void hernoem(TamableAnimal dier, String tekst) {
        String naam = tekst == null ? "" : tekst.strip();
        if (naam.length() > MAX_NAAM) {
            naam = naam.substring(0, MAX_NAAM);
        }
        dier.setCustomName(naam.isEmpty() ? null : Component.literal(naam));
    }
}
