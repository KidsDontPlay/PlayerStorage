package mrriegel.playerstorage;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {

	public static Configuration config;
	
	public static boolean infiniteSpace;

	public static void refreshConfig(File file) {
		config = new Configuration(file);
		
		config.getBoolean("infiniteSpace", config.CATEGORY_GENERAL, true, "Enable infinite inventory.");

		if (config.hasChanged()) {
			config.save();
		}
	}

}
