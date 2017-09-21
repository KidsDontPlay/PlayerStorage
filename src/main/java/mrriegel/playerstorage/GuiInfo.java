package mrriegel.playerstorage;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.button.CommonGuiButton.Design;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.util.Utils;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class GuiInfo extends CommonGuiScreenSub {

	ExInventory ei;
	private static int index = 0;
	List<Tab> tabs = new ArrayList<>();
	Integer active = null;
	long lastInvite = System.currentTimeMillis() - 5000L;
	List<String> team, other;

	public GuiInfo() {
		super();
		this.ei = ExInventory.getInventory(Minecraft.getMinecraft().player);
		xSize = 230;
		ySize = 160;
		tabs.add(new Tab("INFO", () -> {
			int w = 208, h = 11;
			int c = Color.orange.getRGB();
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
		}));
		tabs.add(new Tab("Team", () -> {
			List<String> lis = mc.world.playerEntities.stream().filter(p -> p != mc.player).map(EntityPlayer::getName).collect(Collectors.toList());
			team = lis.stream().filter(s -> ei.members.contains(s)).collect(Collectors.toList());
			other = lis.stream().filter(s -> !ei.members.contains(s)).collect(Collectors.toList());
			//			team = Stream.of("hasl", "fgsdgasva", "klsidsd", "muas", "fgsadv").collect(Collectors.toList());
			//			other = new ArrayList<>(Lists.reverse(team));
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
		}, () -> {
			for (int i = 0; i < 13; i++) {
				buttonList.add(new CommonGuiButton(i, guiLeft + 95, guiTop + 21 + 10 * i, 14, 8, TextFormatting.RED + "" + TextFormatting.BOLD + "-").setDesign(Design.NONE).setTooltip("Uninvite player"));
				buttonList.add(new CommonGuiButton(i + 100, guiLeft + 206, guiTop + 21 + 10 * i, 14, 8, TextFormatting.GREEN + "" + TextFormatting.BOLD + "+").setDesign(Design.NONE).setTooltip("Invite player"));
			}
		}));
	}

	@Override
	public void initGui() {
		super.initGui();
		tabs.get(index).init.run();
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
		tabs.get(index).draw.run();

	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (tabs.get(index).name.equals("Team")) {
			if (button.id < 30) {
				NBTTagCompound nbt = new NBTTagCompound();
				NBTHelper.set(nbt, "action", MessageAction.TEAMUNINVITE);
				NBTHelper.set(nbt, "player1", mc.player.getName());
				NBTHelper.set(nbt, "player2", team.get(button.id));
				PacketHandler.sendToServer(new Message2Server(nbt));
			} else {
				invite(other.get(button.id - 100));
			}
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (mouseButton == 0) {
			for (int i = 0; i < tabs.size(); i++) {
				if (active != null) {
					index = active;
					buttonList.clear();
					tabs.get(index).init.run();
					break;
				}
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
		NBTHelper.set(nbt, "action", MessageAction.TEAMINVITE);
		NBTHelper.set(nbt, "player1", mc.player.getName());
		NBTHelper.set(nbt, "player2", p);
		PacketHandler.sendToServer(new Message2Server(nbt));
	}

	static class Tab {
		String name;
		Runnable draw, init;

		public Tab(String name, Runnable draw) {
			this(name, draw, () -> {
			});
		}

		public Tab(String name, Runnable draw, Runnable init) {
			this.name = name;
			this.draw = draw;
			this.init = init;
		}

	}

}
