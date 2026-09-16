package dev.hourlydiamonds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Stored as a NeoForge data attachment on the player entity (saved/loaded with normal player NBT)
 * — survives relogging, world reopen, and server restarts. Plain mutable holder; the attachment is
 * re-serialized from the live object whenever the player is saved, no separate "mark dirty" step.
 */
public final class PlayerPlaytimeData {
   /** Ticks accumulated since the last completed hour (0..TICKS_PER_HOUR-1). */
   public int playTicks;
   /** Total cumulative hours completed — never resets, counts forever. Determines which track entry the NEXT hour will be. */
   public int hoursElapsed;
   /** Cumulative hour numbers (1-indexed) whose reward has already been claimed — checked to prevent double-claiming. */
   public List<Integer> claimedHours = new ArrayList<>();

   public PlayerPlaytimeData() {
   }

   public PlayerPlaytimeData(int playTicks, int hoursElapsed, List<Integer> claimedHours) {
      this.playTicks = playTicks;
      this.hoursElapsed = hoursElapsed;
      this.claimedHours = new ArrayList<>(claimedHours);
   }

   public boolean isClaimed(int hour) {
      return this.claimedHours.contains(hour);
   }

   public static final Codec<PlayerPlaytimeData> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.fieldOf("play_ticks").forGetter(d -> d.playTicks),
            Codec.INT.fieldOf("hours_elapsed").forGetter(d -> d.hoursElapsed),
            Codec.INT.listOf().optionalFieldOf("claimed_hours", List.of()).forGetter(d -> d.claimedHours)
         )
         .apply(instance, PlayerPlaytimeData::new)
   );
}
