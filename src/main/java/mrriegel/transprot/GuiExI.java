package mrriegel.transprot;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.gui.CommonGuiContainer;
import mrriegel.limelib.gui.GuiDrawer;
import mrriegel.limelib.gui.GuiDrawer.Direction;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.element.AbstractSlot;
import mrriegel.limelib.gui.element.AbstractSlot.FluidSlot;
import mrriegel.limelib.gui.element.AbstractSlot.ItemSlot;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.plugin.JEI;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.limelib.util.Utils;
import mrriegel.transprot.Enums.MessageAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.oredict.OreDictionary;

public class GuiExI extends CommonGuiContainer {

	public List<StackWrapper> wrappers;
	protected List<ItemSlot> items = Lists.newArrayList();
	protected long lastClick;
	protected CommonGuiButton sort, direction, clear, jei;
	protected GuiTextField searchBar;
	protected int currentPos = 0, maxPos = 0;
	protected ItemSlot over;

	public boolean canClick() {
		return System.currentTimeMillis() > lastClick + 300L;
	}

	public GuiExI(ContainerExI inventorySlotsIn) {
		super(inventorySlotsIn);
		ySize = 220;
		xSize += 72;
		lastClick = System.currentTimeMillis();
	}

	private ContainerExI getContainer() {
		return (ContainerExI) inventorySlots;
	}

	@Override
	public void initGui() {
		super.initGui();
		wrappers = ExInventory.getInventory(mc.player).getItems();
		searchBar = new GuiTextField(0, fontRenderer, guiLeft + 154, guiTop + 121, 85, fontRenderer.FONT_HEIGHT);
		searchBar.setMaxStringLength(30);
		searchBar.setEnableBackgroundDrawing(!false);
		searchBar.setVisible(true);
		searchBar.setTextColor(16777215);
		searchBar.setFocused(true);
		buttonList.add(sort = new CommonGuiButton(0, guiLeft + 7, guiTop + 116, 45, 12, "sort"));
		buttonList.add(direction = new CommonGuiButton(1, guiLeft + 55, guiTop + 116, 20, 12, "direct"));
		buttonList.add(clear = new CommonGuiButton(2, guiLeft + 62, guiTop + 137, 7, 7, "").setTooltip("Clear grid"));
		if (LimeLib.jeiLoaded)
			buttonList.add(jei = new CommonGuiButton(3, guiLeft + 125, guiTop + 116, 24, 12, "").setTooltip("Enable synchronized search with JEI"));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		drawer.drawBackgroundTexture();
		drawer.drawPlayerSlots(79, 137);
		drawer.drawSlots(7, 7, 13, 6);
		searchBar.drawTextBox();
		drawer.drawSlots(7, 137, 3, 3);
		drawer.drawProgressArrow(13, 196, 0F, Direction.RIGHT);
		drawer.drawSlot(43, 195);
		drawer.drawColoredRectangle(7, 7, 13 * 18, 6 * 18, ColorHelper.getRGB(Color.GRAY.getRGB(), 80));
		boolean uni = fontRenderer.getUnicodeFlag();
		fontRenderer.setUnicodeFlag(true);
		for (ItemSlot slot : items) {
			slot.draw(mouseX, mouseY);
			if (slot.isMouseOver(mouseX, mouseY)) {
				over = slot;
			}
		}
		fontRenderer.setUnicodeFlag(uni);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString("x", 63, 136, 0xE0E0E0);
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		for (ItemSlot slot : items) {
			if (slot.isMouseOver(mouseX, mouseY))
				slot.drawTooltip(mouseX - guiLeft, mouseY - guiTop);
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (searchBar.isFocused() && LimeLib.jeiLoaded && JEI.hasKeyboardFocus())
			searchBar.setFocused(false);
		wrappers = ExInventory.getInventory(mc.player).getItems();

		over = null;
		int invisible = wrappers.size() - 13 * 6;
		if (invisible <= 0)
			maxPos = 0;
		else {
			maxPos = invisible / 13;
			if (invisible % 13 != 0)
				maxPos++;
		}
		if (currentPos > maxPos)
			currentPos = maxPos;
		items.clear();
		List<StackWrapper> tmp = getFilteredList();
		int index = currentPos * 13;
		line: for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 13; j++) {
				if (index >= tmp.size())
					break line;
				StackWrapper w = tmp.get(index);
				items.add(new ItemSlot(w.getStack(), index, guiLeft + 8 + j * 18, guiTop + 8 + i * 18, w.getSize(), drawer, true, true, !true, true));
				index++;
			}
		}

		//		sort.setTooltip("Sort by " + getContainer().getSort().name().toLowerCase());
		//		sort.displayString = getContainer().getSort().name();
		direction.setTooltip("Sort direction: " + (getContainer().isTopdown() ? "top-down" : "bottom-up"));
		direction.displayString = getContainer().isTopdown() ? "TD" : "BU";
		if (jei != null)
			jei.displayString = (getContainer().isJEI() ? TextFormatting.GREEN : TextFormatting.RED) + "JEI";
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("button", button.id);
		//		PacketHandler.sendToServer(new MessageRequest(nbt));
		//		new MessageRequest().handleMessage(mc.player, nbt, Side.CLIENT);
	}

	protected void sendSlot(AbstractSlot slot, int mouseButton) {
		if(mouseButton!=0&&mouseButton!=1)
			return;
		NBTTagCompound nbt = new NBTTagCompound();
		NBTHelper.set(nbt, "action", MessageAction.SLOT);
		if (slot instanceof ItemSlot) {
			if (!((ItemSlot) slot).stack.isEmpty())
				NBTHelper.set(nbt, "slot", ((ItemSlot) slot).stack.writeToNBT(new NBTTagCompound()));
		} else if (slot instanceof FluidSlot) {
			if (((FluidSlot) slot).fluid != null)
				NBTHelper.set(nbt, "slot", ((FluidSlot) slot).fluid.writeToNBT(new NBTTagCompound()));
		} else
			throw new RuntimeException();
		NBTHelper.set(nbt, "mouse", mouseButton);
		NBTHelper.set(nbt, "shift", isShiftKeyDown());
		NBTHelper.set(nbt, "ctrl", isCtrlKeyDown());
		PacketHandler.sendToServer(new MessageInventory(nbt));
	}

	//	abstract protected void addSlot(AbstractSlot slot, NBTTagCompound nbt);

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		searchBar.setFocused(isPointInRegion(searchBar.x, searchBar.y, searchBar.width, searchBar.height, mouseX + guiLeft, mouseY + guiTop));
		if (searchBar.isFocused() && mouseButton == 1) {
			searchBar.setText("");
			if (LimeLib.jeiLoaded && getContainer().isJEI())
				JEI.setFilterText(searchBar.getText());
		}
		if (canClick()) {
			if (over != null)
				sendSlot(over, mouseButton);
			else
				;//TODO dump item/fluid
			lastClick = System.currentTimeMillis();
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		if (isPointInRegion(7, 7, 18 * 13, 18 * 6, GuiDrawer.getMouseX(), GuiDrawer.getMouseY())) {
			int mouse = Mouse.getEventDWheel();
			if (mouse == 0)
				return;
			if (mouse > 0 && currentPos > 0)
				currentPos--;
			if (mouse < 0 && currentPos < maxPos)
				currentPos++;
		}
	}

	@Override
	public void handleKeyboardInput() throws IOException {
		super.handleKeyboardInput();
		//		PacketHandler.sendToServer(new MessageInvTweaks(Keyboard.isKeyDown(Keyboard.KEY_SPACE), isShiftKeyDown(), isCtrlKeyDown()));
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (!this.checkHotbarKeys(keyCode)) {
			if (over != null && LimeLib.jeiLoaded && (keyCode == Keyboard.KEY_R || keyCode == Keyboard.KEY_U) && (!searchBar.isFocused() || searchBar.getText().isEmpty())) {
				if (keyCode == Keyboard.KEY_R)
					JEI.showRecipes(over.stack);
				else
					JEI.showUsage(over.stack);
				return;
			} else if (this.searchBar.textboxKeyTyped(typedChar, keyCode)) {
				if (getContainer().isJEI() && LimeLib.jeiLoaded)
					JEI.setFilterText(searchBar.getText());
				return;
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	private List<StackWrapper> getFilteredList() {
		String search = searchBar.getText().toLowerCase().trim();
		List<StackWrapper> tmp = !search.isEmpty() ? Lists.<StackWrapper> newArrayList() : Lists.newArrayList(wrappers);
		if (!search.isEmpty())
			for (StackWrapper w : wrappers) {
				if (match(w.getStack(), search))
					tmp.add(w);

			}
		Collections.sort(tmp, new Comparator<StackWrapper>() {
			int mul = !getContainer().isTopdown() ? -1 : 1;

			@Override
			public int compare(StackWrapper o2, StackWrapper o1) {
				//				switch (getContainer().getSort()) {
				//				case AMOUNT:
				//					return Integer.compare(o1.getSize(), o2.getSize()) * mul;
				//				case NAME:
				//					return TextFormatting.getTextWithoutFormattingCodes(o2.getStack().getDisplayName()).compareToIgnoreCase(TextFormatting.getTextWithoutFormattingCodes(o1.getStack().getDisplayName())) * mul;
				//				case MOD:
				//					return Utils.getModName(o2.getStack().getItem()).compareToIgnoreCase(Utils.getModName(o1.getStack().getItem())) * mul;
				//				}
				return 0;
			}
		});
		return tmp;
	}

	private boolean match(ItemStack stack, String text) {
		String[] list = text.split("\\|");
		for (String w : list) {
			final String word = w.trim().toLowerCase();
			if (word.startsWith("@")) {
				if (Utils.getModID(stack.getItem()).toLowerCase().contains(word.substring(1)))
					return true;
			} else if (word.startsWith("#")) {
				List<String> tooltip = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
				if (!tooltip.isEmpty() && tooltip.get(0).contains(stack.getDisplayName()))
					tooltip.remove(0);
				for (String s : tooltip) {
					if (TextFormatting.getTextWithoutFormattingCodes(s).toLowerCase().contains(word.substring(1))) {
						return true;
					}
				}
			} else if (word.startsWith("$")) {
				return Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).anyMatch(s -> s.toLowerCase().contains(word.substring(1)));
			} else if (word.startsWith("%")) {
				return Arrays.stream(stack.getItem().getCreativeTabs()).filter(c -> c != null).map(CreativeTabs::getTranslatedTabLabel).anyMatch(s -> s.toLowerCase().contains(word.substring(1)));
			} else {
				if (TextFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()).toLowerCase().contains(word))
					return true;
			}
		}
		return false;
	}
}
