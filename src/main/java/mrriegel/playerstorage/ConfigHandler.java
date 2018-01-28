package mrriegel.playerstorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ConfigHandler {

	public static Configuration config;

	public static boolean infiniteSpace, remote, /*TODO remove to exi*/noshift;
	public static int itemCapacity, fluidCapacity;
	public static Map<String, Unit2> apples = new HashMap<>();
	@SuppressWarnings("serial")
	public static List<String> appleList = new ArrayList<String>(6) {
		@Override
		public String get(int index) {
			if (index >= size())
				return "";
			return super.get(index);
		}
	};

	public static void refreshConfig(File file) {
		config = new Configuration(file);
		Gson gson = new Gson();
		infiniteSpace = config.getBoolean("infiniteSpace", Configuration.CATEGORY_GENERAL, false, "Enable infinite inventory.");
		remote = config.getBoolean("remote", Configuration.CATEGORY_GENERAL, false, "Enable remote item to access your inventory without pressing a key.");
		itemCapacity = config.getInt("itemCapacity", Configuration.CATEGORY_GENERAL, 2000, 0, Integer.MAX_VALUE, "Item capacity at the beginning");
		fluidCapacity = config.getInt("fluidCapacity", Configuration.CATEGORY_GENERAL, 20000, 0, Integer.MAX_VALUE, "Fluid capacity at the beginning");
		Property prop = config.get(Configuration.CATEGORY_GENERAL, "appleTiers", Arrays.asList(//
				new Unit1("blockIron", 3200, 32000), //
				new Unit1("blockGold", 25600, 256000), //
				new Unit1("blockDiamond", 102400, 1024000), //
				new Unit1("netherStar", 2000000, 20000000))//
				.stream().map(gson::toJson).toArray(String[]::new));
		prop.setLanguageKey("appleTiers");
		prop.setComment("Tiers for apples");
		for (String s : prop.getStringList()) {
			@SuppressWarnings("serial")
			Unit1 unit = gson.fromJson(s, new TypeToken<Unit1>() {
			}.getType());
			if (!apples.containsKey(unit.oreName))
				apples.put(unit.oreName, unit.entry);
			appleList.add(unit.oreName);
		}
		noshift = config.getBoolean("noshift", Configuration.CATEGORY_GENERAL, false, "Usually you use shift-click to transfer items into the player storage" + Configuration.NEW_LINE + "When this is true you transfer items with ctrl, so you can use shift to transfer items between player inventory and hotbar.");

		if (config.hasChanged()) {
			config.save();
		}
	}

	static class Unit1 {
		public String oreName;
		public Unit2 entry;

		public Unit1(String oreName, int i1, int i2) {
			super();
			this.oreName = oreName;
			this.entry = new Unit2(i1, i2);
		}
	}

	public static class Unit2 {
		public int itemLimit, fluidLimit;

		public Unit2(int itemLimit, int fluidLimit) {
			super();
			this.itemLimit = itemLimit;
			this.fluidLimit = fluidLimit;
		}
	}

}
