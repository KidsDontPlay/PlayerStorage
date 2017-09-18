package mrriegel.playerstorage;

import java.awt.Color;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.util.Utils;
import net.minecraft.client.Minecraft;

public class GuiInfo extends CommonGuiScreenSub {

	ExInventory ei;

	public GuiInfo() {
		super();
		this.ei = ExInventory.getInventory(Minecraft.getMinecraft().player);
		xSize = ySize = 100;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		drawer.drawBackgroundTexture();
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		//		RenderHelper.disableStandardItemLighting();
		//		drawer.drawColoredRectangle(xSize - 100 - 3, 1, 100, 100, 0xFF555555);
		//		drawer.drawFrame(xSize - 100 - 3, 1, 100, 100, 2, 0xFF999999);

		int c = Color.orange.getRGB();
		drawer.drawColoredRectangle(xSize - 87, 13, 70, 11, ColorHelper.darker(c, .6));
		double foo = ei.getItemCount() / (double) ei.itemLimit;
		drawer.drawColoredRectangle(xSize - 87, 13, (int) (70 * foo), 11, c);
		drawer.drawFrame(xSize - 87, 13, 70, 11, 1, 0xFF000000);
		drawCenteredString(fontRenderer, (isShiftKeyDown() ? ei.getItemCount() : Utils.formatNumber(ei.getItemCount())) + "/" + (isShiftKeyDown() ? ei.itemLimit : Utils.formatNumber(ei.itemLimit)), guiLeft + xSize - 52, guiTop + 28, 0xf7f7f7);

		c = 0xff00c5cd;
		drawer.drawColoredRectangle(xSize - 87, 55, 70, 11, ColorHelper.darker(c, .6));
		foo = ei.getFluidCount() / (double) ei.fluidLimit;
		drawer.drawColoredRectangle(xSize - 87, 55, (int) (70 * foo), 11, c);
		drawer.drawFrame(xSize - 87, 55, 70, 11, 1, 0xFF000000);
		drawCenteredString(fontRenderer, (isShiftKeyDown() ? ei.getFluidCount() : Utils.formatNumber(ei.getFluidCount())) + "/" + (isShiftKeyDown() ? ei.fluidLimit : Utils.formatNumber(ei.fluidLimit)), guiLeft + xSize - 52, guiTop + 70, 0xf7f7f7);

	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
	}

}
