package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One village as the world map shows it (the compact network form): a real player's base or an NPC village.
 * {@code id} = the base id (a player's uuid, or the NPC village's id); {@code cx, cz} = its plot cell = its map position;
 * {@code punten} = Knusheid; {@code kleur} = flag colour (opaque ARGB); {@code nieuw} = founded or grew a short while ago.
 */
public record KaartDorp(UUID id, String naam, String eigenaar, boolean npc, int cx, int cz, int stadhuisLevel, int punten, int kleur,
        boolean nieuw) {
    void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(naam, 64);
        buf.writeUtf(eigenaar, 64);
        buf.writeBoolean(npc);
        buf.writeVarInt(cx);
        buf.writeVarInt(cz);
        buf.writeByte(stadhuisLevel);
        buf.writeVarInt(punten);
        buf.writeInt(kleur);
        buf.writeBoolean(nieuw);
    }

    static KaartDorp read(FriendlyByteBuf buf) {
        return new KaartDorp(buf.readUUID(), buf.readUtf(64), buf.readUtf(64), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                buf.readByte(), buf.readVarInt(), buf.readInt(), buf.readBoolean());
    }
}
