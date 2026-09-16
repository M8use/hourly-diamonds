package dev.hourlydiamonds.client;

import dev.hourlydiamonds.PlaytimeTrack;
import dev.hourlydiamonds.network.PlaytimeNetwork;
import dev.hourlydiamonds.network.ServerboundClaimPlaytimeRewardPacket;
import dev.hourlydiamonds.network.ServerboundRequestPlaytimeSyncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Visualizes the existing hourly-diamond progression (see {@link PlaytimeTrack}) as a paginated
 * grid — 8 slots per page, 3 pages, covering the 24-hour cycle the player is currently in. Every
 * number shown here is exactly what the server last synced; this screen never decides anything on
 * its own, it only requests a claim and waits for the server to confirm it via the next sync.
 */
public final class PlaytimeRewardsScreen extends Screen {
   private static final int WIDTH = 300;
   private static final int HEIGHT = 260;
   private static final int COLUMNS = 4;
   private static final int ROWS = 2;
   private static final int SLOTS_PER_PAGE = COLUMNS * ROWS;
   private static final int PAGE_COUNT = (PlaytimeTrack.CYCLE_LENGTH + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
   private static final long TICKS_PER_HOUR_MS = 3600000L;
   private static final int TEXT_WHITE = -1;
   private static final int TEXT_DIM = 0xFFB0B0B0;
   private static final int ACCENT = 0xFF5FD0F2;

   private int leftPos;
   private int topPos;
   private int page;

   public PlaytimeRewardsScreen() {
      super(Component.translatable("gui.hourlydiamonds.title"));
   }

   @Override
   protected void init() {
      this.leftPos = (this.width - WIDTH) / 2;
      this.topPos = (this.height - HEIGHT) / 2;
      PlaytimeNetwork.toServer(new ServerboundRequestPlaytimeSyncPacket());
      if (this.minecraft != null) {
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.7F));
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   /** The first cumulative hour shown on the current page's grid — always the start of the 24-hour window the player is currently progressing through. */
   private int cycleBase() {
      int hoursElapsed = ClientPlaytimeState.hoursElapsed();
      return hoursElapsed / PlaytimeTrack.CYCLE_LENGTH * PlaytimeTrack.CYCLE_LENGTH;
   }

   /** Cumulative hour number for a given slot (0..SLOTS_PER_PAGE-1) on the current page. */
   private int hourForSlot(int slotOnPage) {
      return this.cycleBase() + this.page * SLOTS_PER_PAGE + slotOnPage + 1;
   }

   private long remainingMillisFor(int cumulativeHour) {
      int hoursElapsed = ClientPlaytimeState.hoursElapsed();
      long hoursAway = (long)(cumulativeHour - hoursElapsed - 1);
      long baseRemaining = ClientPlaytimeState.nextHourReadyAtEpochMillis() - System.currentTimeMillis();
      return baseRemaining + hoursAway * TICKS_PER_HOUR_MS;
   }

   private static String formatDuration(long millis) {
      long totalSeconds = Math.max(0L, millis) / 1000L;
      long hours = totalSeconds / 3600L;
      long minutes = totalSeconds % 3600L / 60L;
      long seconds = totalSeconds % 60L;
      if (hours > 0L) {
         return String.format("%d:%02d:%02d", hours, minutes, seconds);
      } else {
         return String.format("%02d:%02d", minutes, seconds);
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      WheelRenderer.dimBackground(graphics, this.width, this.height, 0xC0080C10);
      int x1 = this.leftPos;
      int y1 = this.topPos;
      int x2 = this.leftPos + WIDTH;
      int y2 = this.topPos + HEIGHT;
      WheelRenderer.roundedRect(graphics, x1, y1, x2, y2, 6, 0xFF1B4A5E);
      long backgroundPhaseMs = System.currentTimeMillis() % 9000L;
      float backgroundPhase = (float)backgroundPhaseMs / 9000.0F;
      WheelRenderer.movingSheenBackground(graphics, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 5, 0xFF255A70, 0xFFE8FBFF, backgroundPhase, false);
      float borderPhase = (float)(System.currentTimeMillis() / 6L % 360L);
      WheelRenderer.movingOutline(graphics, x1, y1, x2, y2, 2, 0, borderPhase, true);
      int centerX = (x1 + x2) / 2;
      this.drawGiftIcon(graphics, x1 + 22, y1 + 16);
      graphics.drawCenteredString(this.font, Component.translatable("gui.hourlydiamonds.title"), centerX + 8, y1 + 10, TEXT_WHITE);
      int closeSize = 16;
      int closeX1 = x2 - closeSize - 5;
      int closeY1 = y1 + 5;
      boolean hoveredClose = mouseX >= closeX1 && mouseX < closeX1 + closeSize && mouseY >= closeY1 && mouseY < closeY1 + closeSize;
      WheelRenderer.card(graphics, closeX1, closeY1, closeX1 + closeSize, closeY1 + closeSize, 3, 0xFF7A140C, hoveredClose ? 0xFFF15A4E : 0xFFD8332A, 0xFFB82A20);
      graphics.drawCenteredString(this.font, "X", closeX1 + closeSize / 2, closeY1 + 4, TEXT_WHITE);
      int gridTop = y1 + 34;
      int gridLeft = x1 + 10;
      int cellW = (WIDTH - 20) / COLUMNS;
      int cellH = 88;

      for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
         int col = slot % COLUMNS;
         int row = slot / COLUMNS;
         int cellX1 = gridLeft + col * cellW;
         int cellY1 = gridTop + row * cellH;
         this.renderCard(graphics, cellX1, cellY1, cellX1 + cellW - 4, cellY1 + cellH - 6, this.hourForSlot(slot), mouseX, mouseY);
      }

      int navY = gridTop + ROWS * cellH + 4;
      Component pageLabel = Component.literal((this.page + 1) + " / " + PAGE_COUNT);
      graphics.drawCenteredString(this.font, pageLabel, centerX, navY, TEXT_DIM);
      this.renderArrow(graphics, x1 + 14, navY - 1, false, this.page > 0, mouseX, mouseY);
      this.renderArrow(graphics, x2 - 20, navY - 1, true, this.page < PAGE_COUNT - 1, mouseX, mouseY);
   }

   private void renderArrow(GuiGraphics graphics, int x, int y, boolean pointRight, boolean enabled, int mouseX, int mouseY) {
      int size = 12;
      boolean hovered = enabled && mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
      int color = enabled ? (hovered ? TEXT_WHITE : ACCENT) : 0xFF3A4A50;
      Component glyph = Component.literal(pointRight ? ">" : "<");
      graphics.drawCenteredString(this.font, glyph, x + size / 2, y + 2, color);
   }

   private void renderCard(GuiGraphics graphics, int x1, int y1, int x2, int y2, int hour, int mouseX, int mouseY) {
      PlaytimeTrack.RewardDef reward = PlaytimeTrack.rewardForHour(hour);
      boolean claimed = ClientPlaytimeState.isClaimed(hour);
      boolean claimable = !claimed && hour <= ClientPlaytimeState.hoursElapsed();
      boolean hovered = claimable && mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
      int top;
      int bottom;
      int border;
      if (claimed) {
         top = 0xFF2E8B3F;
         bottom = 0xFF1E5C2A;
         border = 0xFF163F1C;
      } else if (claimable) {
         top = hovered ? 0xFF3FE0A0 : 0xFF2FC98A;
         bottom = 0xFF1C8F63;
         border = 0xFF115C40;
      } else {
         top = 0xFF2A4A56;
         bottom = 0xFF163038;
         border = 0xFF0D1E24;
      }

      WheelRenderer.card(graphics, x1, y1, x2, y2, 4, border, top, bottom);
      if (reward.milestone()) {
         float phase = (float)(System.currentTimeMillis() / 6L % 360L);
         WheelRenderer.movingOutline(graphics, x1, y1, x2, y2, 2, 0, phase, true);
      } else if (claimable) {
         float rayRotation = (float)(System.currentTimeMillis() / 25L % 360L);
         float pulse = 0.5F + 0.5F * (float)Math.sin((double)System.currentTimeMillis() / 300.0);
         int alpha = (int)(60.0F + 50.0F * pulse);
         WheelRenderer.sunRays(graphics, (float)(x1 + x2) / 2.0F, (float)(y1 + 27), 22.0F, rayRotation, 8, alpha << 24 | 0xB6FFCF);
      }

      int centerX = (x1 + x2) / 2;
      Component hourLabel = Component.translatable("gui.hourlydiamonds.hour_label", hour);
      graphics.drawCenteredString(this.font, hourLabel, centerX, y1 + 4, TEXT_WHITE);
      ItemStack stack = new ItemStack(reward.item(), reward.quantity());
      // Icon rendered ~1.5x bigger than vanilla native size; centered a bit lower than before (and
      // the quantity text pushed down to match) so the larger icon has room without touching the
      // hour label above or the state text below.
      int iconCenterY = y1 + 27;
      graphics.pose().pushPose();
      graphics.pose().translate((float)centerX, (float)iconCenterY, 0.0F);
      graphics.pose().scale(1.5F, 1.5F, 1.0F);
      graphics.renderItem(stack, -8, -8);
      graphics.pose().popPose();
      Component qty = Component.literal("x" + reward.quantity());
      graphics.drawCenteredString(this.font, qty, centerX, iconCenterY + 15, TEXT_WHITE);
      int stateY = y2 - 11;
      if (claimed) {
         graphics.drawCenteredString(this.font, Component.translatable("gui.hourlydiamonds.claimed"), centerX, stateY, TEXT_WHITE);
      } else if (claimable) {
         graphics.drawCenteredString(this.font, Component.translatable("gui.hourlydiamonds.ready"), centerX, stateY, TEXT_WHITE);
      } else {
         String timer = formatDuration(this.remainingMillisFor(hour));
         graphics.drawCenteredString(this.font, Component.literal(timer), centerX, stateY, TEXT_DIM);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int x1 = this.leftPos;
      int y1 = this.topPos;
      int x2 = this.leftPos + WIDTH;
      int y2 = this.topPos + HEIGHT;
      int closeSize = 16;
      int closeX1 = x2 - closeSize - 5;
      int closeY1 = y1 + 5;
      if (mouseX >= closeX1 && mouseX < closeX1 + closeSize && mouseY >= closeY1 && mouseY < closeY1 + closeSize) {
         this.onClose();
         return true;
      } else {
         int gridTop = y1 + 34;
         int gridLeft = x1 + 10;
         int cellW = (WIDTH - 20) / COLUMNS;
         int cellH = 88;
         int navY = gridTop + ROWS * cellH + 4;
         int arrowSize = 12;
         if (this.page > 0 && mouseX >= x1 + 14 && mouseX < x1 + 14 + arrowSize && mouseY >= navY - 1 && mouseY < navY - 1 + arrowSize) {
            this.page--;
            this.playNav();
            return true;
         } else if (this.page < PAGE_COUNT - 1
            && mouseX >= x2 - 20
            && mouseX < x2 - 20 + arrowSize
            && mouseY >= navY - 1
            && mouseY < navY - 1 + arrowSize) {
            this.page++;
            this.playNav();
            return true;
         } else {
            for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
               int col = slot % COLUMNS;
               int row = slot / COLUMNS;
               int cellX1 = gridLeft + col * cellW;
               int cellY1 = gridTop + row * cellH;
               int cellX2 = cellX1 + cellW - 4;
               int cellY2 = cellY1 + cellH - 6;
               if (mouseX >= cellX1 && mouseX < cellX2 && mouseY >= cellY1 && mouseY < cellY2) {
                  int hour = this.hourForSlot(slot);
                  boolean claimable = !ClientPlaytimeState.isClaimed(hour) && hour <= ClientPlaytimeState.hoursElapsed();
                  if (claimable) {
                     PlaytimeNetwork.toServer(new ServerboundClaimPlaytimeRewardPacket(hour));
                  }

                  return true;
               }
            }

            return true;
         }
      }
   }

   private void playNav() {
      if (this.minecraft != null) {
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F, 0.3F));
      }
   }

   private void drawGiftIcon(GuiGraphics graphics, int cx, int cy) {
      WheelRenderer.card(graphics, cx - 7, cy - 4, cx + 7, cy + 8, 1, 0xFF1B4A2E, 0xFFFFFFFF, 0xFFE4F6FA);
      graphics.fill(cx - 1, cy - 4, cx + 1, cy + 8, 0xFF2FA6C8);
      graphics.fill(cx - 7, cy + 1, cx + 7, cy + 3, 0xFF2FA6C8);
      graphics.fill(cx - 4, cy - 8, cx + 4, cy - 4, 0xFF2FA6C8);
   }
}
