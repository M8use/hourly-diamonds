package dev.hourlydiamonds;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlaytimeAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
      NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HourlyDiamonds.MODID
   );

   public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerPlaytimeData>> DATA = ATTACHMENT_TYPES.register(
      "player_playtime_data", () -> AttachmentType.builder(PlayerPlaytimeData::new).serialize(PlayerPlaytimeData.CODEC).copyOnDeath().build()
   );

   public static void register(IEventBus modBus) {
      ATTACHMENT_TYPES.register(modBus);
   }

   private PlaytimeAttachments() {
   }
}
