package dev.hourlydiamonds.network;

import dev.hourlydiamonds.PlaytimeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record ServerboundRequestPlaytimeSyncPacket() {
   public void encode(FriendlyByteBuf buf) {
   }

   public static ServerboundRequestPlaytimeSyncPacket decode(FriendlyByteBuf buf) {
      return new ServerboundRequestPlaytimeSyncPacket();
   }

   public void handle(ServerPlayer player) {
      if (player != null) {
         PlaytimeManager.sync(player);
      }
   }
}
