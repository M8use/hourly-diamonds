package dev.hourlydiamonds.network;

import net.minecraft.server.level.ServerPlayer;

public final class PlaytimeNetwork {
   private static PlaytimeNetwork.Sender sender = new PlaytimeNetwork.Sender() {
      @Override
      public void toClient(ServerPlayer player, Object message) {
      }

      @Override
      public void toServer(Object message) {
      }
   };

   public static void bind(PlaytimeNetwork.Sender platform) {
      sender = platform;
   }

   public static void toClient(ServerPlayer player, Object message) {
      sender.toClient(player, message);
   }

   public static void toServer(Object message) {
      sender.toServer(message);
   }

   private PlaytimeNetwork() {
   }

   public interface Sender {
      void toClient(ServerPlayer player, Object message);

      void toServer(Object message);
   }
}
