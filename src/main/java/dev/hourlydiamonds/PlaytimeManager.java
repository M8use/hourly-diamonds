package dev.hourlydiamonds;

import dev.hourlydiamonds.network.ClientboundPlaytimeSyncPacket;
import dev.hourlydiamonds.network.PlaytimeNetwork;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * All Playtime Rewards server logic lives here — ticking, hour rollover, and claiming. The client
 * never decides any of this; it only ever displays what the last sync packet said.
 */
public final class PlaytimeManager {
   public static final int TICKS_PER_HOUR = 72000;
   private static final long MS_PER_TICK = 50L;

   /** Called once per tick per player (see the PlayerTickEvent listener) — the only place playTicks/hoursElapsed ever change on their own. */
   public static void onPlayerTick(ServerPlayer player) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      int ticks = data.playTicks + 1;
      if (ticks >= TICKS_PER_HOUR && player.isAlive()) {
         data.playTicks = 0;
         data.hoursElapsed++;
         announceReady(player, data.hoursElapsed);
         sync(player);
      } else {
         data.playTicks = ticks;
      }
   }

   private static void announceReady(ServerPlayer player, int hour) {
      PlaytimeTrack.RewardDef reward = PlaytimeTrack.rewardForHour(hour);
      Component itemName = reward.item().getDefaultInstance().getHoverName();
      player.sendSystemMessage(
         Component.literal("§b\u2726 Your Hour " + hour + " Playtime Reward is ready! §7(" + reward.quantity() + "x ").append(itemName).append(Component.literal("§7)"))
      );
      // A gentle "something's ready" ping, distinct from the bigger sound played on actual claim.
      player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.4F);
   }

   /** Claims the reward for a specific cumulative hour — validated server-side; a spoofed/duplicate/out-of-range request is simply a no-op. */
   public static void claim(ServerPlayer player, int hour) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      if (hour >= 1 && hour <= data.hoursElapsed && !data.isClaimed(hour)) {
         data.claimedHours.add(hour);
         PlaytimeTrack.RewardDef reward = PlaytimeTrack.rewardForHour(hour);
         ItemStack stack = new ItemStack(reward.item(), reward.quantity());
         if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }

         boolean milestone = reward.milestone();
         player.playNotifySound(
            milestone ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS,
            milestone ? 0.9F : 1.0F,
            milestone ? 0.7F : 1.0F
         );
         Component itemName = stack.getHoverName();
         player.sendSystemMessage(
            Component.literal("§bYou've received ").append(itemName).append(Component.literal(" §bfor " + hour + " hour" + (hour == 1 ? "" : "s") + " played!"))
         );
         sync(player);
      }
   }

   public static void sync(ServerPlayer player) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      long nextHourReadyAtEpochMillis = System.currentTimeMillis() + (long)(TICKS_PER_HOUR - data.playTicks) * MS_PER_TICK;
      PlaytimeNetwork.toClient(
         player, new ClientboundPlaytimeSyncPacket(data.hoursElapsed, nextHourReadyAtEpochMillis, List.copyOf(data.claimedHours))
      );
   }

   /** Dev/testing only — see {@code /hourlydiamonds} command, gated to op permission level 2. Advances silently (no per-hour chat spam); the command itself sends one summary message. */
   public static void debugAddHours(ServerPlayer player, int hours) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      data.hoursElapsed += hours;
      sync(player);
   }

   public static void debugMakeReady(ServerPlayer player) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      data.playTicks = TICKS_PER_HOUR - 1;
   }

   public static void debugReset(ServerPlayer player) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      data.playTicks = 0;
      data.hoursElapsed = 0;
      data.claimedHours.clear();
      sync(player);
   }

   private PlaytimeManager() {
   }
}
