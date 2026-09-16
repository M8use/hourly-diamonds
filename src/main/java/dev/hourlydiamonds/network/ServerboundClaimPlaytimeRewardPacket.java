package dev.hourlydiamonds.network;

import dev.hourlydiamonds.PlaytimeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Requests claiming the reward for a specific cumulative hour — the server independently re-validates this is actually legal before granting anything. */
public record ServerboundClaimPlaytimeRewardPacket(int hour) {
   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.hour);
   }

   public static ServerboundClaimPlaytimeRewardPacket decode(FriendlyByteBuf buf) {
      return new ServerboundClaimPlaytimeRewardPacket(buf.readVarInt());
   }

   public void handle(ServerPlayer player) {
      if (player != null) {
         PlaytimeManager.claim(player, this.hour);
      }
   }
}
