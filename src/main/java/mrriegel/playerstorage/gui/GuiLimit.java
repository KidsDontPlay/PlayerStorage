package mrriegel.playerstorage.gui;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.element.AbstractSlot;
import mrriegel.limelib.gui.element.AbstractSlot.FluidSlot;
import mrriegel.limelib.gui.element.AbstractSlot.ItemSlot;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.playerstorage.Enums.MessageAction;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.Message2Server;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.relauncher.Side;

public class GuiLimit extends CommonGuiScreenSub {

	AbstractSlot<?> slot;
	ExInventory ei;
	GuiTextField min, max;
	boolean itemMode;

	public GuiLimit(AbstractSlot<?> slot) {
		super();
		this.slot = slot;
		this.ei = ExInventory.getInventory(Minecraft.getMinecraft().player);
		xSize = ySize = 100;
		itemMode = slot instanceof ItemSlot;
	}

	@Override
	public void initGui() {
		super.initGui();
		if (slot instanceof ItemSlot)
			elementList.add(new ItemSlot((ItemStack) slot.stack, 0, 7 + guiLeft, 17 + guiTop, 1, drawer, false, false, false, !false));
		else if (slot instanceof FluidSlot)
			elementList.add(new FluidSlot((FluidStack) slot.stack, 0, 7 + guiLeft, 17 + guiTop, 1, drawer, false, false, false, !false));
		buttonList.add(new CommonGuiButton(0, guiLeft + 45, guiTop + 77, 45, 18, "Apply"));
		buttonList.add(new GuiCheckBox(1, guiLeft + 7, guiTop + 77, "Void", itemMode ? ei.itemLimits.get(slot.stack).getRight() : ei.fluidLimits.get(slot.stack).getRight()));
		min = new GuiTextField(0, fontRenderer, guiLeft + 29, guiTop + 37, 60, fontRenderer.FONT_HEIGHT);
		min.setMaxStringLength(8);
		min.setValidator(s -> s.isEmpty() || StringUtils.isNumeric(s));
		min.setText(itemMode ? ei.itemLimits.get(slot.stack).getLeft() + "" : ei.fluidLimits.get(slot.stack).getLeft() + "");
		max = new GuiTextField(0, fontRenderer, guiLeft + 29, guiTop + 57, 60, fontRenderer.FONT_HEIGHT);
		max.setMaxStringLength(8);
		max.setValidator(s -> s.isEmpty() || StringUtils.isNumeric(s));
		max.setText(itemMode ? ei.itemLimits.get(slot.stack).getMiddle() + "" : ei.fluidLimits.get(slot.stack).getMiddle() + "");
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		drawer.drawBackgroundTexture();
		drawer.drawSlot(6, 16);
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		fontRenderer.drawString(TextFormatting.UNDERLINE + "Limitation", 6 + guiLeft, 6 + guiTop, 0x2e2e2e);
		fontRenderer.drawString("Min:", 8 + guiLeft, 37 + guiTop, 0x5e5e5e);
		fontRenderer.drawString("Max:", 8 + guiLeft, 57 + guiTop, 0x5e5e5e);
		min.drawTextBox();
		max.drawTextBox();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		min.mouseClicked(mouseX, mouseY, mouseButton);
		max.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.id == 0) {
			NBTTagCompound nbt = new NBTTagCompound();
			MessageAction.SETLIMIT.set(nbt);
			NBTHelper.set(nbt, "stack", slot.stack);
			NBTHelper.set(nbt, "min", min.getText().isEmpty() ? 0 : Integer.parseInt(min.getText()));
			NBTHelper.set(nbt, "max", max.getText().isEmpty() ? 0 : Integer.parseInt(max.getText()));
			GuiCheckBox box = (GuiCheckBox) buttonList.stream().filter(b -> b.id == 1).findAny().orElse(null);
			if (box != null)
				NBTHelper.set(nbt, "void", box.isChecked());
			PacketHandler.sendToServer(new Message2Server(nbt));
			new Message2Server().handleMessage(mc.player, nbt, Side.CLIENT);
			keyTyped(' ', Keyboard.KEY_ESCAPE);
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		min.textboxKeyTyped(typedChar, keyCode);
		max.textboxKeyTyped(typedChar, keyCode);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

}
