package mrriegel.playerstorage;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mrriegel.limelib.gui.CommonGuiScreenSub;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.util.Utils;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

public class GuiInfo extends CommonGuiScreenSub {

	ExInventory ei;
	private static int index = 0;
	List<Tab> tabs = new ArrayList<>();
	Integer active = null;
	long lastInvite = System.currentTimeMillis();

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
		if (tabs.get(index).name.equals("Team")) {
			invite("someone");

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
		if (mc.player.getName().equals(p) || ExInventory.getInventory(mc.player).members.contains(p))
			return;
		lastInvite = System.currentTimeMillis();
		EntityPlayer player = mc.world.getPlayerEntityByName(p);
		if (player == null)
			return;
		ITextComponent text = new TextComponentString(mc.player.getName() + " invites you to join their PlayerStorage team.");
		ITextComponent yes = new TextComponentString("[Accept]");

		//		ITextComponent no = new TextComponentString("[Decline]");
		Style yesno = new Style();
		yesno.setColor(TextFormatting.GREEN);
		yesno.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click here")));
		yesno.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "") {
			long time = System.currentTimeMillis();

			@Override
			public Action getAction() {
				NBTTagCompound nbt = new NBTTagCompound();
				if (System.currentTimeMillis() - time >= 1000) {
					time = System.currentTimeMillis();
					NBTHelper.set(nbt, "action", MessageAction.TEAMINVITE);
					NBTHelper.set(nbt, "player1", p);
					NBTHelper.set(nbt, "player2", mc.player.getName());
					PacketHandler.sendToServer(new Message2Server(nbt));
				}
				return super.getAction();
			}
		});
		yes.setStyle(yesno);
		//		yesno = yesno.createShallowCopy();
		//		yesno.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "") {
		//			@Override
		//			public Action getAction() {
		//				System.out.println("no");
		//				return super.getAction();
		//			}
		//		});
		//		no.setStyle(yesno);
		text.appendText(" ");
		text.appendSibling(yes);
		//		text.appendText(" / ");
		//		text.appendSibling(no);
		player.sendMessage(text);
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
