package mrriegel.playerstorage;

import mrriegel.limelib.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy implements IGuiHandler {

	public void preInit(FMLPreInitializationEvent event) {
		ConfigHandler.refreshConfig(event.getSuggestedConfigurationFile());
		ExInventory.register();
		PlayerStorage.interfac.registerBlock();
	}

	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(PlayerStorage.instance, this);
		PacketHandler.registerMessage(MessageCapaSync.class, Side.CLIENT);
		PacketHandler.registerMessage(MessageInventory.class, Side.SERVER);
	}

	public void postInit(FMLPostInitializationEvent event) {
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (!world.isRemote) {
			ExInventory.sync((EntityPlayerMP) player);
			if (player.openContainer instanceof ContainerExI)
				((ContainerExI) player.openContainer).saveMatrix();
		}
		if (ID == 0)
			return new ContainerExI(player.inventory);
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID == 0)
			return new GuiExI(new ContainerExI(player.inventory));
		return null;
	}

}
