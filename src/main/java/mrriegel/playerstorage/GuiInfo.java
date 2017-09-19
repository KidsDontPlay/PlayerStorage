package mrriegel.playerstorage;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

public class GuiInfo extends CommonGuiScreenSub {

	ExInventory ei;
	private static int index = 0;
	List<Tab> tabs = new ArrayList<>();
	Integer active = null;

	public GuiInfo() {
		super();
		this.ei = ExInventory.getInventory(Minecraft.getMinecraft().player);
		xSize = 200;
		ySize = 100;
		tabs.add(new Tab("INFO", () -> {
			//			fontRenderer.drawString("INFO", 6 + guiLeft, 6 + guiTop, 0);
			int c = Color.orange.getRGB();
			drawer.drawColoredRectangle(10, 13, 177, 11, ColorHelper.darker(c, .6));
			double foo = ei.getItemCount() / (double) ei.itemLimit;
			drawer.drawColoredRectangle(10, 13, (int) (177 * foo), 11, c);
			drawer.drawFrame(10, 13, 177, 11, 1, 0xFF000000);
			fontRenderer.drawString((isShiftKeyDown() ? ei.getItemCount() : Utils.formatNumber(ei.getItemCount())) + "/" + (isShiftKeyDown() ? ei.itemLimit : Utils.formatNumber(ei.itemLimit))+" Items", guiLeft + 11, guiTop + 28, 0x3e3e3e);

			c = 0xff00c5cd;
			drawer.drawColoredRectangle(10, 55, 177, 11, ColorHelper.darker(c, .6));
			foo = ei.getFluidCount() / (double) ei.fluidLimit;
			drawer.drawColoredRectangle(10, 55, (int) (177 * foo), 11, c);
			drawer.drawFrame(10, 55, 177, 11, 1, 0xFF000000);
			fontRenderer.drawString((isShiftKeyDown() ? ei.getFluidCount() : Utils.formatNumber(ei.getFluidCount())) + "/" + (isShiftKeyDown() ? ei.fluidLimit : Utils.formatNumber(ei.fluidLimit))+" mB", guiLeft + 11, guiTop + 70, 0x3e3e3e);
		}));
		tabs.add(new Tab("Team", () -> {
			
		}));
		tabs.add(new Tab("kolibrienzauber", () -> {
		}));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		int x = 0;
		active = null;
		for (int i = 0; i < tabs.size(); i++) {
			Tab tab = tabs.get(i);
			String name = (i == index ? TextFormatting.BOLD : "") + tab.name;
			int w = fontRenderer.getStringWidth(name) + 10;
			drawer.drawBackgroundTexture(x, -15, w, 20);
			boolean in;
			if (in = isPointInRegion(x, -15, w - 2, 17, mouseX, mouseY)) 
				active = i;
			int c = !in && i != index ? 0x6e6e6e : 0x3e3e3e;
			fontRenderer.drawString(name, x + 5 + guiLeft, -9 + guiTop, c);
			x += w;
		}
		drawer.drawBackgroundTexture();
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		tabs.get(index).run.run();

	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (mouseButton == 0) {
			for (int i = 0; i < tabs.size(); i++) {
				if (active != null) {
					index = active;
					break;
				}
			}
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
	}

	static class Tab {
		String name;
		Runnable run;

		public Tab(String name, Runnable run) {
			super();
			this.name = name;
			this.run = run;
		}

	}

}
