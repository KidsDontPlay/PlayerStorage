package mrriegel.playerstorage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import org.lwjgl.input.Keyboard;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.OpenGuiMessage;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.oredict.OreDictionary;

public class ClientProxy extends CommonProxy {

	public static final KeyBinding GUI = new KeyBinding("Open GUI", Keyboard.KEY_I, PlayerStorage.MODID);
	public static Int2IntOpenHashMap colorMap = new Int2IntOpenHashMap(4);

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		ClientRegistry.registerKeyBinding(GUI);
		Registry.initClient();
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		MinecraftForge.EVENT_BUS.register(ClientProxy.class);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tint) -> {
			/** @author mezz */
			if (tint != 0)
				return 0xffffff;
			Minecraft mc = Minecraft.getMinecraft();
			if (colorMap.containsKey(stack.getItemDamage()))
				return colorMap.get(stack.getItemDamage());
			String ore = ConfigHandler.appleList.get(stack.getItemDamage());
			List<ItemStack> ores = OreDictionary.getOres(ore);
			if (ores.isEmpty())
				return 0xffffff;
			TextureAtlasSprite tas = mc.getRenderItem().getItemModelMesher().getItemModel(ores.get(0)).getParticleTexture();
			if (tas == mc.getTextureMapBlocks().getMissingSprite() || tas.getIconHeight() <= 0 || tas.getIconWidth() <= 0 || tas.getFrameCount() <= 0)
				return 0xffffff;
			BufferedImage img = new BufferedImage(tas.getIconWidth(), tas.getIconHeight() * tas.getFrameCount(), BufferedImage.TYPE_4BYTE_ABGR);
			for (int i = 0; i < tas.getFrameCount(); i++) {
				int[][] frameTextureData = tas.getFrameTextureData(i);
				int[] largestMipMapTextureData = frameTextureData[0];
				img.setRGB(0, i * tas.getIconHeight(), tas.getIconWidth(), tas.getIconHeight(), largestMipMapTextureData, 0, tas.getIconWidth());
			}
			int red = 0, green = 0, blue = 0, count = 0;
			for (int x = 0; x < img.getWidth(); x++)
				for (int y = 0; y < img.getHeight(); y++) {
					int rgb = img.getRGB(x, y);
					if (ColorHelper.getAlpha(rgb) == 255) {
						red += ColorHelper.getRed(rgb);
						green += ColorHelper.getGreen(rgb);
						blue += ColorHelper.getBlue(rgb);
						count++;
					}
				}
			int c = new Color((red / count), (green / count), (blue / count)).getRGB();
			c = ColorHelper.brighter(c, .15);
			colorMap.put(stack.getItemDamage(), c);
			return c;
		}, Registry.apple);
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);

	}

	@SubscribeEvent
	public static void key(InputEvent.KeyInputEvent event) {
		if (Minecraft.getMinecraft().inGameHasFocus && GUI.isPressed() && Minecraft.getMinecraft().player.hasCapability(ExInventory.EXINVENTORY, null)) {
			PacketHandler.sendToServer(new OpenGuiMessage(PlayerStorage.MODID, 0, null));
		}
	}

	public static final String TEAMCODE = "²³¼′°°!^’â^^Ô`¸'";

	@SubscribeEvent
	public static void chat(ClientChatEvent event) {
		if (event.getMessage().contains(TEAMCODE)) {
			EntityPlayer sender = Minecraft.getMinecraft().world.getPlayerEntityByName(event.getMessage().replace(TEAMCODE, ""));
			if (sender != null) {
				NBTTagCompound nbt = new NBTTagCompound();
				NBTHelper.set(nbt, "action", MessageAction.TEAMACCEPT);
				NBTHelper.set(nbt, "player1", Minecraft.getMinecraft().player.getName());
				NBTHelper.set(nbt, "player2", sender.getName());
				PacketHandler.sendToServer(new Message2Server(nbt));
			}
			event.setMessage("");

		}
	}

}
