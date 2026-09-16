package dev.hourlydiamonds.client;

import dev.hourlydiamonds.network.ClientboundPlaytimeSyncPacket;

public final class ClientPlaytimeHandler {
   public static void handleSync(ClientboundPlaytimeSyncPacket packet) {
      ClientPlaytimeState.apply(packet.hoursElapsed(), packet.nextHourReadyAtEpochMillis(), packet.claimedHours());
   }

   private ClientPlaytimeHandler() {
   }
}
