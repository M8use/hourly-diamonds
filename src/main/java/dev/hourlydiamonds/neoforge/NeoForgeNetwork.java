package dev.hourlydiamonds.neoforge;

import dev.hourlydiamonds.client.ClientPlaytimeHandler;
import dev.hourlydiamonds.network.ClientboundPlaytimeSyncPacket;
import dev.hourlydiamonds.network.PlaytimeNetwork;
import dev.hourlydiamonds.network.ServerboundClaimPlaytimeRewardPacket;
import dev.hourlydiamonds.network.ServerboundRequestPlaytimeSyncPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetwork {
   static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("hourlydiamonds", path);
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToServer(
         RequestSync.TYPE, RequestSync.CODEC, (payload, context) -> context.enqueueWork(() -> payload.inner().handle((ServerPlayer)context.player()))
      );
      registrar.playToServer(
         ClaimReward.TYPE, ClaimReward.CODEC, (payload, context) -> context.enqueueWork(() -> payload.inner().handle((ServerPlayer)context.player()))
      );
      registrar.playToClient(Sync.TYPE, Sync.CODEC, (payload, context) -> context.enqueueWork(() -> ClientPlaytimeHandler.handleSync(payload.inner())));
      PlaytimeNetwork.bind(new PlaytimeNetwork.Sender() {
         @Override
         public void toClient(ServerPlayer player, Object message) {
            if (message instanceof ClientboundPlaytimeSyncPacket packet) {
               PacketDistributor.sendToPlayer(player, new Sync(packet), new CustomPacketPayload[0]);
            }
         }

         @Override
         public void toServer(Object message) {
            if (message instanceof ServerboundRequestPlaytimeSyncPacket packet) {
               PacketDistributor.sendToServer(new RequestSync(packet), new CustomPacketPayload[0]);
            } else if (message instanceof ServerboundClaimPlaytimeRewardPacket packet) {
               PacketDistributor.sendToServer(new ClaimReward(packet), new CustomPacketPayload[0]);
            }
         }
      });
   }

   private NeoForgeNetwork() {
   }

   public static record RequestSync(ServerboundRequestPlaytimeSyncPacket inner) implements CustomPacketPayload {
      public static final Type<RequestSync> TYPE = new Type(NeoForgeNetwork.id("request_sync"));
      public static final StreamCodec<RegistryFriendlyByteBuf, RequestSync> CODEC = StreamCodec.of(
         (buf, msg) -> msg.inner().encode(buf), buf -> new RequestSync(ServerboundRequestPlaytimeSyncPacket.decode(buf))
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record ClaimReward(ServerboundClaimPlaytimeRewardPacket inner) implements CustomPacketPayload {
      public static final Type<ClaimReward> TYPE = new Type(NeoForgeNetwork.id("claim_reward"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ClaimReward> CODEC = StreamCodec.of(
         (buf, msg) -> msg.inner().encode(buf), buf -> new ClaimReward(ServerboundClaimPlaytimeRewardPacket.decode(buf))
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record Sync(ClientboundPlaytimeSyncPacket inner) implements CustomPacketPayload {
      public static final Type<Sync> TYPE = new Type(NeoForgeNetwork.id("sync"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of(
         (buf, msg) -> msg.inner().encode(buf), buf -> new Sync(ClientboundPlaytimeSyncPacket.decode(buf))
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
