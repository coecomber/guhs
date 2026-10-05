package nl.juiced.guhs.feature.guhpixel;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** What a new {@link Sessie} gets: its id, kind, arena, players and free options (difficulty...). */
public record SessieStart(UUID id, SpelSoort soort, Arena arena, List<ServerPlayer> spelers, CompoundTag opties) {
}
