package mrriegel.playerstorage;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = PlayerStorage.MODID, name = PlayerStorage.MODNAME, version = PlayerStorage.VERSION, acceptedMinecraftVersions = "[1.12,1.13)", dependencies = "required-after:limelib@[1.7.8,)")
public class PlayerStorage {
	public static final String MODID = "playerstorage";
	public static final String VERSION = "1.0.3";
	public static final String MODNAME = "PlayerStorage";

	@Instance(PlayerStorage.MODID)
	public static PlayerStorage instance;

	@SidedProxy(clientSide = "mrriegel.playerstorage.ClientProxy", serverSide = "mrriegel.playerstorage.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit(event);
	}

}