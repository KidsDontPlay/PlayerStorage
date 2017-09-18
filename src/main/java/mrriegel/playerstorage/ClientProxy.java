package mrriegel.playerstorage;

import org.lwjgl.input.Keyboard;

import mrriegel.limelib.network.OpenGuiMessage;
import mrriegel.limelib.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class ClientProxy extends CommonProxy {

	public static final KeyBinding GUI = new KeyBinding("Open GUI", Keyboard.KEY_I, PlayerStorage.MODID);

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		ClientRegistry.registerKeyBinding(GUI);
		PlayerStorage.interfac.initModel();
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		MinecraftForge.EVENT_BUS.register(ClientProxy.class);
	}

	@SubscribeEvent
	public static void key(InputEvent.KeyInputEvent event) {
		if (Minecraft.getMinecraft().inGameHasFocus && GUI.isPressed() && Minecraft.getMinecraft().player.hasCapability(ExInventory.EXINVENTORY, null)) {
			PacketHandler.sendToServer(new OpenGuiMessage(PlayerStorage.MODID, 0, null));
		}
	}

}
