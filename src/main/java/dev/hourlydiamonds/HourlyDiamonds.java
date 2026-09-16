package dev.hourlydiamonds;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hourlydiamonds.client.PlaytimeButton;
import dev.hourlydiamonds.neoforge.NeoForgeNetwork;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

/**
 * The original mod was a single hourly-diamond timer with a chat announcement and one test
 * command; this now additionally visualizes that same progression as a full Playtime Rewards menu
 * (see {@link PlaytimeManager}, {@link PlaytimeTrack}). The core tick-counting logic below is
 * unchanged from the original — it just no longer auto-grants the reward the instant the hour
 * completes, instead marking it claimable so the menu's claim flow (and its CLAIMED/CLAIMABLE/
 * UPCOMING states) has something real to reflect. The chat announcement moved from "hour complete"
 * to "reward claimed" for the same reason, but still fires exactly once per hour either way.
 */
@Mod("hourlydiamonds")
public class HourlyDiamonds {
   public static final String MODID = "hourlydiamonds";

   public HourlyDiamonds(IEventBus modBus) {
      PlaytimeAttachments.register(modBus);
      modBus.addListener(NeoForgeNetwork::register);
      NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
      NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
      NeoForge.EVENT_BUS.addListener(this::onInventoryScreenInit);
      NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
   }

   private void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PlaytimeManager.onPlayerTick(player);
      }
   }

   private void onInventoryScreenInit(ScreenEvent.Init.Post event) {
      if (event.getScreen() instanceof InventoryScreen inventory) {
         event.addListener(PlaytimeButton.create(inventory));
      }
   }

   private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         PlaytimeManager.sync(serverPlayer);
      }
   }

   private void onRegisterCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("hourlydiamonds").requires(source -> source.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("test")
                        .executes(ctx -> test((CommandSourceStack)ctx.getSource(), List.of(((CommandSourceStack)ctx.getSource()).getPlayerOrException()))))
                     .then(
                        Commands.argument("targets", EntityArgument.players())
                           .executes(ctx -> test((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                     )
               )
               .then(
                  Commands.literal("skip")
                     .then(
                        Commands.argument("hours", IntegerArgumentType.integer(1))
                           .executes(
                              ctx -> skip(
                                    (CommandSourceStack)ctx.getSource(),
                                    ((CommandSourceStack)ctx.getSource()).getPlayerOrException(),
                                    IntegerArgumentType.getInteger(ctx, "hours")
                                 )
                           )
                     )
               )
               .then(Commands.literal("skiphour").executes(ctx -> skipHour((CommandSourceStack)ctx.getSource(), ((CommandSourceStack)ctx.getSource()).getPlayerOrException())))
               .then(Commands.literal("ready").executes(ctx -> ready((CommandSourceStack)ctx.getSource(), ((CommandSourceStack)ctx.getSource()).getPlayerOrException())))
               .then(Commands.literal("reset").executes(ctx -> reset((CommandSourceStack)ctx.getSource(), ((CommandSourceStack)ctx.getSource()).getPlayerOrException())))
               .then(Commands.literal("status").executes(ctx -> status((CommandSourceStack)ctx.getSource(), ((CommandSourceStack)ctx.getSource()).getPlayerOrException())))
         );
   }

   /** Preserves the original command's exact behavior: an immediate, no-waiting reward. */
   private static int test(CommandSourceStack source, Collection<ServerPlayer> players) {
      players.forEach(player -> {
         PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
         data.hoursElapsed++;
         PlaytimeManager.claim(player, data.hoursElapsed);
      });
      int count = players.size();
      source.sendSuccess(() -> Component.literal("Sent an instant test reward to " + count + (count == 1 ? " player" : " players")), true);
      return count;
   }

   private static int skip(CommandSourceStack source, ServerPlayer player, int hours) {
      PlaytimeManager.debugAddHours(player, hours);
      source.sendSuccess(() -> Component.literal("Advanced " + player.getName().getString() + " by " + hours + " hour" + (hours == 1 ? "" : "s") + " (now claimable)"), true);
      return hours;
   }

   private static int skipHour(CommandSourceStack source, ServerPlayer player) {
      PlaytimeManager.debugAddHours(player, 1);
      int newHour = player.getData(PlaytimeAttachments.DATA).hoursElapsed;
      source.sendSuccess(
         () -> Component.literal("Hour " + newHour + " is now claimable for " + player.getName().getString() + " — open Playtime Rewards to claim it"), true
      );
      return newHour;
   }

   private static int ready(CommandSourceStack source, ServerPlayer player) {
      PlaytimeManager.debugMakeReady(player);
      source.sendSuccess(() -> Component.literal("The next hourly reward for " + player.getName().getString() + " will complete on their next tick"), true);
      return 1;
   }

   private static int reset(CommandSourceStack source, ServerPlayer player) {
      PlaytimeManager.debugReset(player);
      source.sendSuccess(() -> Component.literal("Reset Playtime Rewards progress for " + player.getName().getString()), true);
      return 1;
   }

   private static int status(CommandSourceStack source, ServerPlayer player) {
      PlayerPlaytimeData data = player.getData(PlaytimeAttachments.DATA);
      source.sendSuccess(
         () -> Component.literal(
               player.getName().getString()
                  + ": hoursElapsed="
                  + data.hoursElapsed
                  + ", playTicks="
                  + data.playTicks
                  + "/"
                  + PlaytimeManager.TICKS_PER_HOUR
                  + ", claimed="
                  + data.claimedHours.size()
            ),
         false
      );
      return 1;
   }
}
