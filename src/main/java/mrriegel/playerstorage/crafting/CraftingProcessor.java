package mrriegel.playerstorage.crafting;

import java.util.ArrayList;
import java.util.List;

import mrriegel.limelib.util.StackWrapper;
import mrriegel.playerstorage.ExInventory;
import net.minecraft.item.ItemStack;

public class CraftingProcessor {

	CraftingTask task;
	ExInventory ei;
	List<StackWrapper> ingriedients = new ArrayList<>();

	public void tick() {
		if (task == null)
			return;
	}

	public void add(ItemStack stack) {
		StackWrapper.add(new StackWrapper(stack), ingriedients);
	}

	public void add(StackWrapper wrap) {
		StackWrapper.add(wrap, ingriedients);
	}

	public void release() {
		StackWrapper.toStackList(ingriedients).forEach(s -> ei.insertItem(s, true, false));
	}

}
