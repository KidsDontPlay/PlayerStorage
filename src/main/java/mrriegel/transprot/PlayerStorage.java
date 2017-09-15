package mrriegel.transprot;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = PlayerStorage.MODID, name = PlayerStorage.MODNAME, version = PlayerStorage.VERSION, dependencies = "required-after:limelib@[1.6.0,)")
public class PlayerStorage {
	public static final String MODID = "ppppppppp";
	public static final String VERSION = "1.0.0";
	public static final String MODNAME = "PlayerStorage";

	@Instance(PlayerStorage.MODID)
	public static PlayerStorage instance;

	@SidedProxy(clientSide = "mrriegel.transprot.ClientProxy", serverSide = "mrriegel.transprot.CommonProxy")
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