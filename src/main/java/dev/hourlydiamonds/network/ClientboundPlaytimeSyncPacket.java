package dev.hourlydiamonds.network;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * {@code nextHourReadyAtEpochMillis} is a real wall-clock timestamp (not a tick count), so the
 * client can render a smoothly updating countdown purely from its own clock — no repeated syncing
 * needed just to keep a timer ticking down on screen.
 */
public record ClientboundPlaytimeSyncPacket(int hoursElapsed, long nextHourReadyAtEpochMillis, List<Integer> claimedHours) {
   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.hoursElapsed);
      buf.writeLong(this.nextHourReadyAtEpochMillis);
      buf.writeCollection(this.claimedHours, FriendlyByteBuf::writeVarInt);
   }

   public static ClientboundPlaytimeSyncPacket decode(FriendlyByteBuf buf) {
      return new ClientboundPlaytimeSyncPacket(buf.readVarInt(), buf.readLong(), buf.readList(FriendlyByteBuf::readVarInt));
   }
}
