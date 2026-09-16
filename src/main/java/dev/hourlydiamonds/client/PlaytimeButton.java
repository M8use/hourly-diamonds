package dev.hourlydiamonds.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.fml.ModList;

/**
 * Stacks as the 4th button in the shared inventory-button convention — behind Upgrader, Daily
 * Rewards, and QuestLog, in that order, but only reserving space for whichever of those three are
 * actually installed (see {@link #countLoadedCompanions()}). With none of them installed this
 * button sits at the very top instead of floating with an empty gap above it.
 */
public final class PlaytimeButton extends Button {
   public static final int SIZE = 26;
   private static final String UPGRADER_MOD_ID = "upgrader";
   private static final String DAILY_REWARDS_MOD_ID = "dailyrewards";
   private static final String QUESTLOG_MOD_ID = "questlog";
   private static final int OTHER_BUTTONS_RESERVED = countLoadedCompanions() * (SIZE + 6);
   private static final int BEVEL_OUTLINE = 0xFF0B2233;
   private static final int BEVEL_HIGHLIGHT = 0xFF9FE8FF;
   private static final int BEVEL_SHADOW = 0xFF124A63;
   private static final int FILL_TOP = 0xFF5FD0F2;
   private static final int FILL_BOTTOM = 0xFF1C8CB8;
   private static final int HOVER_WASH = 0x33FFFFFF;
   private static final int GIFT_RIBBON_OUTLINE = 0xFF1B4A2E;
   private static final int GIFT_BODY_TOP = 0xFFFFFFFF;
   private static final int GIFT_BODY_BOTTOM = 0xFFE4F6FA;
   private static final int GIFT_RIBBON = 0xFF2FA6C8;

   private final InventoryScreen inventoryScreen;

   private PlaytimeButton(InventoryScreen inventoryScreen) {
      super(0, 0, SIZE, SIZE, Component.translatable("gui.hourlydiamonds.button"), PlaytimeButton::press, DEFAULT_NARRATION);
      this.inventoryScreen = inventoryScreen;
      this.setTooltip(Tooltip.create(Component.translatable("gui.hourlydiamonds.button")));
   }

   public static PlaytimeButton create(InventoryScreen inventoryScreen) {
      return new PlaytimeButton(inventoryScreen);
   }

   /** Counts how many of the three "earlier in the stack" companion mods are actually installed, in the agreed Upgrader -> Daily Rewards -> QuestLog -> (this mod) order. */
   private static int countLoadedCompanions() {
      int count = 0;
      if (ModList.get().isLoaded(UPGRADER_MOD_ID)) {
         count++;
      }

      if (ModList.get().isLoaded(DAILY_REWARDS_MOD_ID)) {
         count++;
      }

      if (ModList.get().isLoaded(QUESTLOG_MOD_ID)) {
         count++;
      }

      return count;
   }

   private static void press(Button button) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.5F));
         minecraft.setScreen(new PlaytimeRewardsScreen());
      }
   }

   private void followInventoryLayout() {
      int x = Math.min(this.inventoryScreen.getGuiLeft() + this.inventoryScreen.getXSize() + 6, this.inventoryScreen.width - SIZE - 4);
      int y = this.inventoryScreen.getGuiTop() + 6 + OTHER_BUTTONS_RESERVED;
      this.setX(x);
      this.setY(y);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.followInventoryLayout();
      int x1 = this.getX();
      int y1 = this.getY();
      int x2 = x1 + this.width;
      int y2 = y1 + this.height;
      float cx = (float)(x1 + x2) / 2.0F;
      float cy = (float)(y1 + y2) / 2.0F;
      boolean claimable = ClientPlaytimeState.hasAnyClaimable();
      if (claimable) {
         float rotation = (float)(System.currentTimeMillis() / 30L % 360L);
         float pulse = 0.5F + 0.5F * (float)Math.sin((double)System.currentTimeMillis() / 400.0);
         int alpha = (int)(90.0F + 60.0F * pulse);
         WheelRenderer.sunRays(graphics, cx, cy, (float)SIZE * 0.9F, rotation, 10, alpha << 24 | 0xB6F2FF);
      }

      WheelRenderer.pixelBevelBorder(graphics, x1, y1, x2, y2, BEVEL_OUTLINE, BEVEL_HIGHLIGHT, BEVEL_SHADOW);
      WheelRenderer.roundedRect(graphics, x1 + 2, y1 + 2, x2 - 2, y2 - 2, 0, FILL_TOP, FILL_BOTTOM);
      if (this.isHoveredOrFocused()) {
         graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, HOVER_WASH);
      }

      this.drawGiftIcon(graphics, Math.round(cx), Math.round(cy));
      if (claimable) {
         int badgeCx = x2 - 2;
         int badgeCy = y1 + 2;
         WheelRenderer.disc(graphics, (float)badgeCx, (float)badgeCy, 6.0F, 0xFFCC2A2A);
         WheelRenderer.disc(graphics, (float)badgeCx, (float)badgeCy, 4.8F, 0xFFE23C3C);
         // Exclamation mark drawn as plain rectangles (long stroke + small dot), each 2px wide/tall
         // and symmetric around the badge center — a 1px mark starting exactly at the center point
         // is actually offset half a pixel off-true-center, not centered at all.
         graphics.fill(badgeCx - 1, badgeCy - 4, badgeCx + 1, badgeCy - 1, 0xFFFFFFFF);
         graphics.fill(badgeCx - 1, badgeCy + 1, badgeCx + 1, badgeCy + 2, 0xFFFFFFFF);
      }
   }

   private void drawGiftIcon(GuiGraphics graphics, int cx, int cy) {
      WheelRenderer.card(graphics, cx - 6, cy - 3, cx + 6, cy + 7, 1, GIFT_RIBBON_OUTLINE, GIFT_BODY_TOP, GIFT_BODY_BOTTOM);
      graphics.fill(cx - 1, cy - 3, cx + 1, cy + 7, GIFT_RIBBON);
      graphics.fill(cx - 6, cy + 1, cx + 6, cy + 3, GIFT_RIBBON);
      graphics.fill(cx - 3, cy - 7, cx + 3, cy - 3, GIFT_RIBBON);
   }
}
