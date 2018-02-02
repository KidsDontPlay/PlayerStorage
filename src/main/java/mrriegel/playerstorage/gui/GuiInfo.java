package mrriegel.playerstorage.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.gui.GuiDrawer;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.button.CommonGuiButton.Design;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.util.Utils;
import mrriegel.playerstorage.ClientProxy;
import mrriegel.playerstorage.Enums.MessageAction;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.Message2Server;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.relauncher.Side;

public class GuiInfo extends CommonGuiScreenSub {

	ExInventory ei;
	private static int index = 0;
	List<Tab> tabs = new ArrayList<>();
	Integer active = null;
	long lastInvite = 0L;
	List<String> team, other;

	public GuiInfo() {
		super();
		this.ei = ExInventory.getInventory(Minecraft.getMinecraft().player);
		xSize = 230;
		ySize = 160;
		tabs.add(new Tab("Info") {

			@Override
			void tooltip() {
				String inter = "Interfaces";
				if (isPointInRegion(11, 90, fontRenderer.getStringWidth(inter), fontRenderer.FONT_HEIGHT, GuiDrawer.getMouseX(), GuiDrawer.getMouseY())) {
					List<String> l = ei.tiles.stream().map(gp -> "Dim:" + gp.getDimension() + ", x:" + gp.getPos().getX() + " y:" + gp.getPos().getY() + " z:" + gp.getPos().getZ()).collect(Collectors.toList());
					drawHoveringText(l.isEmpty() ? Arrays.asList("No Interfaces") : l, GuiDrawer.getMouseX(), GuiDrawer.getMouseY());
				}
				if (buttonList.get(0).isMouseOver()) {
					drawHoveringText(Lists.newArrayList("Insert picked up items into your storage.", "Hold " + Keyboard.getKeyName(ClientProxy.INVERTPICKUP.getKeyCode()) + " to invert temporarily."), GuiDrawer.getMouseX(), GuiDrawer.getMouseY());
				}
				if (buttonList.get(1).isMouseOver()) {
					drawHoveringText("If enabled water will be generated when there are at least 2 buckets in your storage (to 10 buckets).", GuiDrawer.getMouseX(), GuiDrawer.getMouseY());
				}
				if (buttonList.get(2).isMouseOver()) {
					drawHoveringText("Usually you use shift-click to transfer items into the player storage. When this is enabled you transfer items with CTRL, so you can use shift to transfer items between player inventory and hotbar.", GuiDrawer.getMouseX(), GuiDrawer.getMouseY());
				}
			}

			@Override
			void init() {
				buttonList.add(new GuiCheckBox(MessageAction.PICKUP.ordinal(), guiLeft + 110, guiTop + 88, "Auto Pickup", ei.autoPickup));
				buttonList.add(new GuiCheckBox(MessageAction.WATER.ordinal(), guiLeft + 110, guiTop + 100, "Infinite Water", ei.infiniteWater));
				buttonList.add(new GuiCheckBox(MessageAction.NOSHIFT.ordinal(), guiLeft + 110, guiTop + 112, "CTRL <-> SHIFT", ei.noshift));
			}

			@Override
			void draw() {
				int w = 208, h = 11;
				int c = 0xffffc800;
				drawer.drawColoredRectangle(10, 13, w, 11, ColorHelper.darker(c, .6));
				double foo = ei.getItemCount() / (double) ei.itemLimit;
				drawer.drawColoredRectangle(10, 13, (int) (w * foo), h, c);
				drawer.drawFrame(10, 13, w, h, 1, 0xFF000000);
				fontRenderer.drawString((isShiftKeyDown() ? ei.getItemCount() : Utils.formatNumber(ei.getItemCount())) + "/" + (isShiftKeyDown() ? ei.itemLimit : Utils.formatNumber(ei.itemLimit)) + " Items", guiLeft + 11, guiTop + 28, 0x3e3e3e);

				c = 0xff00c5cd;
				drawer.drawColoredRectangle(10, 55, w, h, ColorHelper.darker(c, .6));
				foo = ei.getFluidCount() / (double) ei.fluidLimit;
				drawer.drawColoredRectangle(10, 55, (int) (w * foo), h, c);
				drawer.drawFrame(10, 55, w, h, 1, 0xFF000000);
				fontRenderer.drawString((isShiftKeyDown() ? ei.getFluidCount() : Utils.formatNumber(ei.getFluidCount())) + "/" + (isShiftKeyDown() ? ei.fluidLimit : Utils.formatNumber(ei.fluidLimit)) + " mB", guiLeft + 11, guiTop + 70, 0x3e3e3e);

				String inter = "Interfaces";
				drawer.drawColoredRectangle(10, 88, fontRenderer.getStringWidth(inter) + 2, fontRenderer.FONT_HEIGHT + 1, 0xffe1e1e1);
				drawer.drawFrame(10, 88, fontRenderer.getStringWidth(inter) + 2, fontRenderer.FONT_HEIGHT + 1, 1, 0xff1e1e1e);
				fontRenderer.drawString(inter, guiLeft + 12, guiTop + 90, 0x2e3e3e);
			}

			@Override
			void click(GuiButton button) {
				if (button.id == MessageAction.PICKUP.ordinal()) {
					NBTTagCompound nbt = new NBTTagCompound();
					MessageAction.PICKUP.set(nbt);
					NBTHelper.set(nbt, "pick", ((GuiCheckBox) button).isChecked());
					PacketHandler.sendToServer(new Message2Server(nbt));
					new Message2Server().handleMessage(mc.player, nbt, Side.CLIENT);
				} else if (button.id == MessageAction.WATER.ordinal()) {
					NBTTagCompound nbt = new NBTTagCompound();
					MessageAction.WATER.set(nbt);
					NBTHelper.set(nbt, "water", ((GuiCheckBox) button).isChecked());
					PacketHandler.sendToServer(new Message2Server(nbt));
					new Message2Server().handleMessage(mc.player, nbt, Side.CLIENT);
				} else if (button.id == MessageAction.NOSHIFT.ordinal()) {
					NBTTagCompound nbt = new NBTTagCompound();
					MessageAction.NOSHIFT.set(nbt);
					NBTHelper.set(nbt, "shift", ((GuiCheckBox) button).isChecked());
					PacketHandler.sendToServer(new Message2Server(nbt));
					new Message2Server().handleMessage(mc.player, nbt, Side.CLIENT);
				}
			}
		});
		tabs.add(new Tab("Team") {

			@Override
			void tooltip() {
			}

			@Override
			void init() {
				for (int i = 0; i < 13; i++) {
					buttonList.add(new CommonGuiButton(i, guiLeft + 95, guiTop + 21 + 10 * i, 14, 8, TextFormatting.RED + "" + TextFormatting.BOLD + "-").setDesign(Design.NONE).setTooltip("Uninvite player"));
					buttonList.add(new CommonGuiButton(i + 100, guiLeft + 206, guiTop + 21 + 10 * i, 14, 8, TextFormatting.GREEN + "" + TextFormatting.BOLD + "+").setDesign(Design.NONE).setTooltip("Invite player"));
				}
			}

			@Override
			void draw() {
				List<String> lis = mc.world.playerEntities.stream().filter(p -> p != mc.player).map(EntityPlayer::getName).collect(Collectors.toList());
				team = lis.stream().filter(s -> ei.members.contains(s)).collect(Collectors.toList());
				other = lis.stream().filter(s -> !ei.members.contains(s)).collect(Collectors.toList());
				for (GuiButton but : buttonList) {
					if (but.id < 30) {
						but.visible = but.id < team.size();
					} else {
						but.visible = but.id - 100 < other.size();
					}
				}
				int x = 12 + guiLeft, y = 12 + guiTop;
				drawer.drawColoredRectangle(8, 8, 100, 142, 0xffa2a2a2);
				drawer.drawFrame(8, 8, 100, 142, 1, 0xff080808);
				for (String s : Stream.concat(Stream.of(TextFormatting.DARK_GRAY + "" + TextFormatting.BOLD + "Team"), team.stream()).collect(Collectors.toList())) {
					fontRenderer.drawString(s, x, y, 0x2a2a2a);
					y += 10;
				}
				drawer.drawColoredRectangle(119, 8, 100, 142, 0xffa2a2a2);
				drawer.drawFrame(119, 8, 100, 142, 1, 0xff080808);
				x = 123 + guiLeft;
				y = 12 + guiTop;
				for (String s : Stream.concat(Stream.of(TextFormatting.DARK_GRAY + "" + TextFormatting.BOLD + "Players"), other.stream()).collect(Collectors.toList())) {
					fontRenderer.drawString(s, x, y, 0x2a2a2a);
					y += 10;
				}
			}

			@Override
			void click(GuiButton button) {
				if (button.id < 30) {
					NBTTagCompound nbt = new NBTTagCompound();
					MessageAction.TEAMUNINVITE.set(nbt);
					NBTHelper.set(nbt, "player1", mc.player.getName());
					NBTHelper.set(nbt, "player2", team.get(button.id));
					PacketHandler.sendToServer(new Message2Server(nbt));
				} else {
					invite(other.get(button.id - 100));
				}
			}
		});
		tabs.add(new Tab("Crafting") {

			@Override
			void tooltip() {
				//				drawHoveringText(Arrays.asList("micha"), drawer.getMouseX(), drawer.getMouseY());
			}

			@Override
			void init() {
			}

			@Override
			void draw() {
			}

			@Override
			void click(GuiButton button) {
			}
		});
	}

	@Override
	public void initGui() {
		super.initGui();
		tabs.get(index).init();
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
		//		GlStateManager.disableLighting();
		tabs.get(index).draw();
		//		GlStateManager.enableLighting();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		tabs.get(index).tooltip();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		tabs.get(index).click(button);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (mouseButton == 0) {
			if (active != null) {
				index = active;
				//				buttonList.clear();
				//				elementList.clear();
				//				tabs.get(index).init.run();
				initGui();
			}

		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	private void invite(String p) {
		if (System.currentTimeMillis() - lastInvite < 5000) {
			mc.player.sendMessage(new TextComponentString("Wait a bit... (Spam Protection)"));
			return;
		}
		if (mc.player.getName().equals(p) || ei.members.contains(p))
			return;
		lastInvite = System.currentTimeMillis();
		EntityPlayer player = mc.world.getPlayerEntityByName(p);
		if (player == null)
			return;
		NBTTagCompound nbt = new NBTTagCompound();
		MessageAction.TEAMINVITE.set(nbt);
		NBTHelper.set(nbt, "player1", mc.player.getName());
		NBTHelper.set(nbt, "player2", p);
		PacketHandler.sendToServer(new Message2Server(nbt));
	}

	static abstract class Tab {
		String name;

		public Tab(String name) {
			this.name = name;
		}

		abstract void init();

		abstract void draw();

		abstract void tooltip();

		abstract void click(GuiButton button);

	}

}
