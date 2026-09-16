package dev.hourlydiamonds;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The entire Playtime Rewards progression lives here and nowhere else — this is the "one obvious
 * place" the prompt asked for. To change a reward, edit a line below; nothing outside this file
 * needs to know what the values are.
 *
 * <p>The progression is a repeating 24-hour cycle ({@link #HOUR_CYCLE}, one entry per hour of
 * cumulative playtime, wrapping forever) with a small set of special one-off overrides at specific
 * cumulative hour counts ({@link #MILESTONE_OVERRIDES}) that take priority over whatever the cycle
 * would otherwise show at that hour — e.g. hour 48 is normally "another 24-cycle's hour-24", but the
 * override replaces it with a Netherite Ingot instead. Adding a new milestone (say, hour 96) is one
 * new map entry; changing hour 12 from 5 diamonds to 10 is one line in {@link #HOUR_CYCLE}.
 */
public final class PlaytimeTrack {
   /** How many hours before the regular (non-override) reward pattern repeats. */
   public static final int CYCLE_LENGTH = 24;

   /** Index 0 = hour 1 of the cycle, index 23 = hour 24. {@code milestone = true} gets the special rainbow-outline treatment in the menu. */
   private static final List<RewardDef> HOUR_CYCLE = List.of(
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 5, true),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND, 1, false),
      new RewardDef(Items.DIAMOND_BLOCK, 1, true)
   );

   /** Cumulative hour -> reward that overrides whatever {@link #HOUR_CYCLE} would show at that hour. Add new milestones here. */
   private static final Map<Integer, RewardDef> MILESTONE_OVERRIDES = Map.of(48, new RewardDef(Items.NETHERITE_INGOT, 1, true));

   /** The reward for a given cumulative hour count (1-indexed: the reward for finishing your 1st hour, 2nd hour, etc.), forever. */
   public static RewardDef rewardForHour(int cumulativeHour) {
      RewardDef override = MILESTONE_OVERRIDES.get(cumulativeHour);
      if (override != null) {
         return override;
      } else {
         int position = (cumulativeHour - 1) % CYCLE_LENGTH;
         return HOUR_CYCLE.get(position);
      }
   }

   public record RewardDef(Item item, int quantity, boolean milestone) {
   }

   private PlaytimeTrack() {
   }
}
