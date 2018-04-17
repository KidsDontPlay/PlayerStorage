package mrriegel.playerstorage.gui;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.lwjgl.input.Keyboard;

import com.google.common.base.Predicate;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.button.CommonGuiButton.Design;
import mrriegel.limelib.gui.element.AbstractSlot.FluidSlot;
import mrriegel.limelib.gui.element.AbstractSlot.ItemSlot;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.playerstorage.Enums.MessageAction;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.Limit;
import mrriegel.playerstorage.Message2Server;
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

	Object stack;
	ExInventory ei;
	GuiTextField min, max;
	boolean itemMode;

	private GuiLimit() {
		super();
		ySize = 100;
		xSize = 130;
	}

	public GuiLimit(Object stack) {
		this();
		Validate.notNull(stack);
		Validate.isTrue(stack instanceof ItemStack || stack instanceof FluidStack);
		this.stack = stack;
		itemMode = stack instanceof ItemStack;
	}

	@Override
	public void initGui() {
		super.initGui();
		ei = ExInventory.getInventory(mc.player);
		if (itemMode)
			elementList.add(new ItemSlot((ItemStack) stack, 0, 7 + guiLeft, 17 + guiTop, 1, drawer, false, false, false, !false));
		else
			elementList.add(new FluidSlot((FluidStack) stack, 0, 7 + guiLeft, 17 + guiTop, 1, drawer, false, false, false, !false));
		buttonList.add(new CommonGuiButton(0, guiLeft + 78, guiTop + 75, 45, 18, "Apply").setDesign(Design.SIMPLE).setButtonColor(0xFF646464));
		GuiButton rem = new CommonGuiButton(1, guiLeft + 6, guiTop + 75, 45, 18, "Remove").setDesign(Design.SIMPLE).setButtonColor(0xFF646464);
		rem.visible = itemMode ? ei.itemLimits.containsKey(stack) : ei.fluidLimits.containsKey(stack);
		buttonList.add(rem);
		Limit limit = itemMode ? ei.itemLimits.get(stack) : ei.fluidLimits.get(stack);
		buttonList.add(new GuiCheckBox(2, guiLeft + 75, guiTop + 19, "Void", limit.voidd));
		min = new GuiTextField(0, fontRenderer, guiLeft + 29, guiTop + 37, 80, fontRenderer.FONT_HEIGHT);
		min.setMaxStringLength(11);
		Predicate<String> pred = s -> s.isEmpty() || (StringUtils.isNumeric(s) && Integer.parseInt(s) >= 0 && Integer.parseInt(s) <= Limit.defaultValue.max);
		min.setValidator(pred);
		min.setText(limit.min + "");
		max = new GuiTextField(0, fontRenderer, guiLeft + 29, guiTop + 57, 80, fontRenderer.FONT_HEIGHT);
		max.setMaxStringLength(11);
		max.setValidator(pred);
		max.setText(limit.max + "");
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
		if (button.id == 0 || button.id == 1) {
			NBTTagCompound nbt = new NBTTagCompound();
			NBTHelper.set(nbt, "itemMode", itemMode);
			MessageAction.SETLIMIT.set(nbt);
			NBTHelper.set(nbt, "stack", stack);
			if (button.id == 0) {
				NBTHelper.set(nbt, "min", min.getText().isEmpty() ? 0 : Integer.parseInt(min.getText()));
				NBTHelper.set(nbt, "max", max.getText().isEmpty() ? 0 : Integer.parseInt(max.getText()));
				GuiCheckBox box = (GuiCheckBox) buttonList.stream().filter(b -> b.id == 2).findAny().orElse(null);
				if (box != null)
					NBTHelper.set(nbt, "void", box.isChecked());
			} else if (button.id == 1) {
				NBTHelper.set(nbt, "remove", true);
			}
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
