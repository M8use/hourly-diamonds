package dev.hourlydiamonds.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientPlaytimeState {
   private static int hoursElapsed;
   private static long nextHourReadyAtEpochMillis;
   private static Set<Integer> claimedHours = Set.of();
   private static boolean anyClaimable;

   public static void apply(int hoursElapsed, long nextHourReadyAtEpochMillis, List<Integer> claimedHours) {
      ClientPlaytimeState.hoursElapsed = hoursElapsed;
      ClientPlaytimeState.nextHourReadyAtEpochMillis = nextHourReadyAtEpochMillis;
      ClientPlaytimeState.claimedHours = new HashSet<>(claimedHours);
      // Computed once here, at sync time, rather than re-scanning every render frame — state only
      // actually changes on a sync, so there's nothing to gain by rechecking it more often than that.
      boolean claimable = false;

      for (int hour = 1; hour <= hoursElapsed; hour++) {
         if (!ClientPlaytimeState.claimedHours.contains(hour)) {
            claimable = true;
            break;
         }
      }

      ClientPlaytimeState.anyClaimable = claimable;
   }

   public static int hoursElapsed() {
      return hoursElapsed;
   }

   public static long nextHourReadyAtEpochMillis() {
      return nextHourReadyAtEpochMillis;
   }

   public static boolean isClaimed(int hour) {
      return claimedHours.contains(hour);
   }

   /** True the moment any completed hour (1..hoursElapsed) is sitting unclaimed — drives the button's badge and sunray. */
   public static boolean hasAnyClaimable() {
      return anyClaimable;
   }

   private ClientPlaytimeState() {
   }
}
