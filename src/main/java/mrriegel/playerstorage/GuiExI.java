package mrriegel.playerstorage;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.gui.CommonGuiContainer;
import mrriegel.limelib.gui.GuiDrawer;
import mrriegel.limelib.gui.GuiDrawer.Direction;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.button.CommonGuiButton.Design;
import mrriegel.limelib.gui.element.AbstractSlot;
import mrriegel.limelib.gui.element.AbstractSlot.FluidSlot;
import mrriegel.limelib.gui.element.AbstractSlot.ItemSlot;
import mrriegel.limelib.gui.element.ScrollBar;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.plugin.JEI;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.limelib.util.Utils;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

public class GuiExI extends CommonGuiContainer {

	public List<StackWrapper> items;
	public List<FluidStack> fluids;
	protected List<AbstractSlot<?>> slots;
	protected long lastClick;
	protected CommonGuiButton sort, direction, clear, jei, modeButton, inc, dec;
	protected GuiTextField searchBar;
	protected int currentPos = 0, maxPos = 0;
	protected AbstractSlot<?> over;
	protected final GuiMode mode;
	protected ScrollBar scrollBar;

	private int gridWidth = 12, gridHeight;
	private ContainerExI con;

	public boolean canClick() {
		return System.currentTimeMillis() > lastClick + 150L;
	}

	public GuiExI(ContainerExI inventorySlotsIn) {
		super(inventorySlotsIn);
		lastClick = System.currentTimeMillis();
		con = inventorySlotsIn;
		mode = Validate.notNull(con.ei.mode);
		gridHeight = con.ei.gridHeight;
		con.ei.dirty = true;
	}

	@Override
	public void initGui() {
		xSize = 248;
		ySize = 112 + 18 * gridHeight;
		super.initGui();
		searchBar = new GuiTextField(0, fontRenderer, guiLeft + 154, guiTop + 13 + 18 * gridHeight, 85, fontRenderer.FONT_HEIGHT);
		searchBar.setMaxStringLength(30);
		searchBar.setEnableBackgroundDrawing(!false);
		searchBar.setVisible(true);
		searchBar.setTextColor(16777215);
		searchBar.setFocused(true);
		buttonList.add(sort = new CommonGuiButton(MessageAction.SORT.ordinal(), guiLeft + 7, guiTop + 11 + 18 * gridHeight, 42, 12, null).setDesign(Design.SIMPLE));
		buttonList.add(direction = new CommonGuiButton(MessageAction.DIRECTION.ordinal(), guiLeft + 55, guiTop + 11 + 18 * gridHeight, 42, 12, null).setDesign(Design.SIMPLE));
		if (mode == GuiMode.ITEM)
			buttonList.add(clear = new CommonGuiButton(MessageAction.CLEAR.ordinal(), guiLeft + 62, guiTop + 29 + 18 * gridHeight, 7, 7, null).setTooltip("Clear grid").setDesign(Design.SIMPLE));
		if (LimeLib.jeiLoaded)
			buttonList.add(jei = new CommonGuiButton(MessageAction.JEI.ordinal(), guiLeft + 103, guiTop + 11 + 18 * gridHeight, 42, 12, null).setTooltip("Enable synchronized search with JEI").setDesign(Design.SIMPLE));
		buttonList.add(modeButton = new CommonGuiButton(MessageAction.GUIMODE.ordinal(), guiLeft - 23, guiTop + ySize - 20, 20, 20, "").setTooltip("Toggle Mode").setDesign(Design.SIMPLE));
		buttonList.add(inc = new CommonGuiButton(MessageAction.INCGRID.ordinal(), guiLeft - 23, guiTop + 1, 20, 10, "+").setTooltip("Increase Grid Height").setDesign(Design.SIMPLE).setButtonColor(Color.GRAY.getRGB()));
		buttonList.add(dec = new CommonGuiButton(MessageAction.DECGRID.ordinal(), guiLeft - 23, guiTop + 14, 20, 10, "-").setTooltip("Decrease Grid Height").setDesign(Design.SIMPLE).setButtonColor(Color.GRAY.getRGB()));
		scrollBar = new ScrollBar(0, 227, 7, 14, 18 * gridHeight, drawer, Plane.VERTICAL);
		slots = new ArrayList<>();
		for (int i = 0; i < gridHeight; i++) {
			for (int j = 0; j < gridWidth; j++) {
				if (mode == GuiMode.ITEM)
					slots.add(new ItemSlot(ItemStack.EMPTY, 0, guiLeft + 8 + j * 18, guiTop + 8 + i * 18, 0, drawer, true, true, false, true));
				else
					slots.add(new FluidSlot(null, 0, guiLeft + 8 + j * 18, guiTop + 8 + i * 18, 0, drawer, true, true, false, true));
			}
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		drawer.drawBackgroundTexture(xSize - 35, -15, 35, 20);
		drawer.drawBackgroundTexture();
		drawer.drawPlayerSlots(79, 29 + 18 * gridHeight);
		drawer.drawSlots(7, 7, gridWidth, gridHeight);
		searchBar.drawTextBox();
		if (mode == GuiMode.ITEM) {
			drawer.drawSlots(7, 29 + 18 * gridHeight, 3, 3);
			drawer.drawProgressArrow(13, 88 + 18 * gridHeight, 0F, Direction.RIGHT);
			drawer.drawSlot(43, 87 + 18 * gridHeight);
		}
		scrollBar.draw(mouseX, mouseY);
		boolean uni = fontRenderer.getUnicodeFlag();
		fontRenderer.setUnicodeFlag(true);
		for (AbstractSlot<?> slot : slots) {
			slot.draw(mouseX, mouseY);
		}
		fontRenderer.setUnicodeFlag(uni);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		if (mode == GuiMode.ITEM)
			fontRenderer.drawString("x", 63, 28 + 18 * gridHeight, 0xE0E0E0);
		fontRenderer.drawString(TextFormatting.BOLD + "INFO", xSize - 30, -9, !isPointInRegion(xSize - 35, -15, 35, 17, mouseX, mouseY) ? 0x6e6e6e : 0x3e3e3e);
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		for (AbstractSlot<?> slot : slots) {
			if (slot.isMouseOver(mouseX, mouseY))
				slot.drawTooltip(mouseX - guiLeft, mouseY - guiTop);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (isPointInRegion(xSize - 35, -15, 35, 17, mouseX, mouseY)) {

		}

	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (searchBar.isFocused() && LimeLib.jeiLoaded && JEI.hasKeyboardFocus())
			searchBar.setFocused(false);

		over = null;
		if (mode == GuiMode.ITEM) {
			if (con.ei.dirty) {
				items = con.ei.getItems();
				con.ei.dirty = false;
			}
			List<StackWrapper> tmp = getFilteredItems();
			int invisible = tmp.size() - gridWidth * gridHeight;
			if (invisible <= 0)
				maxPos = 0;
			else {
				maxPos = invisible / gridWidth;
				if (invisible % gridWidth != 0)
					maxPos++;
			}
			if (currentPos > maxPos)
				currentPos = maxPos;
			int index = currentPos * gridWidth;
			int s = 0;
			for (int i = 0; i < gridHeight; i++) {
				for (int j = 0; j < gridWidth; j++) {
					ItemSlot slot = (ItemSlot) slots.get(s);
					if (index >= tmp.size())
						slot.stack = ItemStack.EMPTY;
					else {
						StackWrapper w = tmp.get(index);
						slot.stack = w.getStack();
						slot.amount = w.getSize();
						index++;
					}
					s++;
				}
			}
		} else {
			if (con.ei.dirty) {
				fluids = con.ei.getFluids();
				con.ei.dirty = false;
			}
			List<FluidStack> tmp = getFilteredFluids();
			int invisible = tmp.size() - gridWidth * gridHeight;
			if (invisible <= 0)
				maxPos = 0;
			else {
				maxPos = invisible / gridWidth;
				if (invisible % gridWidth != 0)
					maxPos++;
			}
			if (currentPos > maxPos)
				currentPos = maxPos;
			int index = currentPos * gridWidth;
			int s = 0;
			for (int i = 0; i < gridHeight; i++) {
				for (int j = 0; j < gridWidth; j++) {
					FluidSlot slot = (FluidSlot) slots.get(s);
					if (index >= tmp.size())
						slot.stack = null;
					else {
						FluidStack w = tmp.get(index);
						slot.stack = w;
						slot.amount = w.amount;
						index++;
					}
					s++;
				}
			}
		}

		for (AbstractSlot<?> slot : slots)
			if (slot.isMouseOver(GuiDrawer.getMouseX(), GuiDrawer.getMouseY())) {
				over = slot;
				break;
			}

		scrollBar.status = currentPos / (double) maxPos;
		if (scrollDrag) {
			scrollBar.status = (GuiDrawer.getMouseY() - guiTop - scrollBar.y) / (double) scrollBar.height;
			scrollBar.status = MathHelper.clamp(scrollBar.status, 0, 1);
			currentPos = MathHelper.clamp((int) Math.round(maxPos * scrollBar.status), 0, maxPos);
		}

		dec.visible = gridHeight > 1;
		inc.visible = guiTop > 5;

		sort.setTooltip("Sort by " + con.ei.sort.name().toLowerCase());
		sort.displayString = con.ei.sort.name();
		direction.setTooltip("Sort direction: " + (con.ei.topdown ? "top-down" : "bottom-up"));
		direction.displayString = con.ei.topdown ? "TD" : "BU";
		modeButton.setStack(new ItemStack(mode == GuiMode.ITEM ? Items.WATER_BUCKET : Items.APPLE));
		if (jei != null)
			jei.displayString = (con.ei.jeiSearch ? TextFormatting.GREEN : TextFormatting.RED) + "JEI";
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		NBTTagCompound nbt = new NBTTagCompound();
		NBTHelper.set(nbt, "action", MessageAction.values()[button.id]);
		PacketHandler.sendToServer(new MessageInventory(nbt));
		new MessageInventory().handleMessage(mc.player, nbt, Side.CLIENT);
	}

	protected void sendSlot(AbstractSlot<?> slot, int mouseButton) {
		if (mouseButton != 0 && mouseButton != 1)
			return;
		NBTTagCompound nbt = new NBTTagCompound();
		NBTHelper.set(nbt, "action", MessageAction.SLOT);
		if (slot instanceof ItemSlot) {
			if (!((ItemSlot) slot).stack.isEmpty())
				NBTHelper.set(nbt, "slot", ((ItemSlot) slot).stack.writeToNBT(new NBTTagCompound()));
		} else if (slot instanceof FluidSlot) {
			if (((FluidSlot) slot).stack != null)
				NBTHelper.set(nbt, "slot", ((FluidSlot) slot).stack.writeToNBT(new NBTTagCompound()));
		} else
			throw new RuntimeException();
		NBTHelper.set(nbt, "mouse", mouseButton);
		NBTHelper.set(nbt, "shift", isShiftKeyDown());
		NBTHelper.set(nbt, "ctrl", isCtrlKeyDown());
		PacketHandler.sendToServer(new MessageInventory(nbt));
	}

	private boolean scrollDrag = false;

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		searchBar.mouseClicked(mouseX, mouseY, mouseButton);
		if (searchBar.isFocused() && mouseButton == 1) {
			searchBar.setText("");
			if (LimeLib.jeiLoaded && con.ei.jeiSearch)
				JEI.setFilterText(searchBar.getText());
		}
		if (canClick()) {
			if (over != null)
				sendSlot(over, mouseButton);
			lastClick = System.currentTimeMillis();
		}
		if (scrollBar.isMouseOver(mouseX - guiLeft, mouseY - guiTop)) {
			scrollBar.status = (mouseY - guiTop - scrollBar.y) / (double) scrollBar.height;
			currentPos = MathHelper.clamp((int) Math.round(maxPos * scrollBar.status), 0, maxPos);
			scrollDrag = true;
		}
		if (isPointInRegion(xSize - 35, -15, 35, 17, mouseX, mouseY)) {
			if (mouseButton == 0)
				GuiDrawer.openGui(new GuiInfo());

		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		if (scrollDrag)
			scrollDrag = false;
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		if (scrollBar.isMouseOver(GuiDrawer.getMouseX() - guiLeft, GuiDrawer.getMouseY() - guiTop) || isPointInRegion(7, 7, 18 * gridWidth, 18 * gridHeight, GuiDrawer.getMouseX(), GuiDrawer.getMouseY())) {
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
		NBTTagCompound nbt = new NBTTagCompound();
		NBTHelper.set(nbt, "action", MessageAction.KEYUPDATE);
		NBTHelper.set(nbt, "space", Keyboard.isKeyDown(Keyboard.KEY_SPACE));
		NBTHelper.set(nbt, "shift", isShiftKeyDown());
		NBTHelper.set(nbt, "ctrl", isCtrlKeyDown());
		PacketHandler.sendToServer(new MessageInventory(nbt));
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (!this.checkHotbarKeys(keyCode)) {
			if (over != null && LimeLib.jeiLoaded && (keyCode == Keyboard.KEY_R || keyCode == Keyboard.KEY_U) && (!searchBar.isFocused() || searchBar.getText().isEmpty())) {
				if (keyCode == Keyboard.KEY_R)
					JEI.showRecipes(over.stack);
				else
					JEI.showUsage(over.stack);
			} else if (this.searchBar.textboxKeyTyped(typedChar, keyCode)) {
				if (con.ei.jeiSearch && LimeLib.jeiLoaded)
					JEI.setFilterText(searchBar.getText());
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	private List<StackWrapper> getFilteredItems() {
		String search = searchBar.getText().toLowerCase().trim();
		List<StackWrapper> tmp = !search.isEmpty() ? new ArrayList<>() : new ArrayList<>(items);
		if (!search.isEmpty())
			for (StackWrapper w : items) {
				if (match(w.getStack(), search))
					tmp.add(w);

			}
		int mul = !con.ei.topdown ? -1 : 1;
		tmp.sort((StackWrapper o2, StackWrapper o1) -> {
			switch (con.ei.sort) {
			case AMOUNT:
				return Integer.compare(o1.getSize(), o2.getSize()) * mul;
			case NAME:
				return TextFormatting.getTextWithoutFormattingCodes(o2.getStack().getDisplayName()).compareToIgnoreCase(TextFormatting.getTextWithoutFormattingCodes(o1.getStack().getDisplayName())) * mul;
			case MOD:
				return Utils.getModName(o2.getStack().getItem()).compareToIgnoreCase(Utils.getModName(o1.getStack().getItem())) * mul;
			}
			return 0;
		});
		return tmp;
	}

	private List<FluidStack> getFilteredFluids() {
		String search = searchBar.getText().toLowerCase().trim();
		List<FluidStack> tmp = !search.isEmpty() ? new ArrayList<>() : new ArrayList<>(fluids);
		if (!search.isEmpty())
			for (FluidStack w : fluids) {
				if (match(w, search))
					tmp.add(w);

			}
		int mul = !con.ei.topdown ? -1 : 1;
		tmp.sort((FluidStack o2, FluidStack o1) -> {
			switch (con.ei.sort) {
			case AMOUNT:
				return Integer.compare(o1.amount, o2.amount) * mul;
			case NAME:
				return TextFormatting.getTextWithoutFormattingCodes(o2.getLocalizedName()).compareToIgnoreCase(TextFormatting.getTextWithoutFormattingCodes(o1.getLocalizedName())) * mul;
			case MOD:
				return FluidRegistry.getDefaultFluidName(o2.getFluid()).split(":")[0].compareToIgnoreCase(FluidRegistry.getDefaultFluidName(o1.getFluid()).split(":")[0]) * mul;
			}
			return 0;
		});
		return tmp;
	}

	private boolean match(ItemStack stack, String text) {
		String[] list = text.split("\\|");
		for (String w : list) {
			final String word = w.trim();
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

	private boolean match(FluidStack stack, String text) {
		String[] list = text.split("\\|");
		for (String w : list) {
			final String word = w.trim();
			if (word.startsWith("@")) {
				if (FluidRegistry.getDefaultFluidName(stack.getFluid()).split(":")[0].toLowerCase().contains(word.substring(1)))
					return true;
			} else {
				if (TextFormatting.getTextWithoutFormattingCodes(stack.getLocalizedName()).toLowerCase().contains(word) || //
						stack.getUnlocalizedName().toLowerCase().contains(word))
					return true;
			}
		}
		return false;
	}
}
