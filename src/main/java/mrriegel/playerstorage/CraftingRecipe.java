package mrriegel.playerstorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import mrriegel.limelib.gui.ContainerNull;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CraftingRecipe {

	NonNullList<ItemStack> stacks;
	NonNullList<Ingredient> ings;
	IRecipe recipe;
	boolean exact;

	@Nullable
	public static CraftingRecipe from(ItemStack... stacks) {
		return from(Arrays.asList(stacks));
	}

	@Nullable
	public static CraftingRecipe from(List<ItemStack> stacks) {
		CraftingRecipe cr = new CraftingRecipe();
		if (stacks.size() > 9)
			return null;
		cr.stacks = NonNullList.create();
		cr.stacks.addAll(stacks);
		while (cr.stacks.size() < 9)
			cr.stacks.add(ItemStack.EMPTY);
		InventoryCrafting inv = new InventoryCrafting(new ContainerNull(), 3, 3);
		for (int i = 0; i < 9; i++)
			inv.setInventorySlotContents(i, cr.stacks.get(i));
		cr.recipe = CraftingManager.findMatchingRecipe(inv, FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0));
		if (cr.recipe == null)
			return null;
		cr.ings = map(cr.stacks, cr.recipe);

		return cr;
	}

	private CraftingRecipe() {
	}

	private static NonNullList<Ingredient> map(List<ItemStack> stacks, IRecipe recipe) {
		Validate.isTrue(recipe.getIngredients().size() >= stacks.stream().filter(s -> !s.isEmpty()).count(), recipe.getRecipeOutput() + " " + stacks);
		List<Ingredient> ings = new ReferenceArrayList<>(recipe.getIngredients());
		Int2ObjectMap<Ingredient> map = new Int2ObjectOpenHashMap<>();
		Reference2ReferenceMap<Ingredient, List<Integer>> ingMap = new Reference2ReferenceArrayMap<>();
		for (Ingredient ing : ings) {
			//			ingMap.put(ing, IntStream.range(0, 9).boxed().filter(i -> ing.apply(stacks.get(i))).collect(Collectors.toList()));
		}
		//		for (int i = 0; i < 9; i++) {
		//			if (map.containsKey(i))
		//				continue;
		//			List<Integer> ls = ingMap.get();
		//			if (ls.size() == 1)
		//				map.put(i, stacks.get(ls.get(0)));
		//			else {
		//
		//			}
		//		}

		for (int a = 0; a < 9 && false; a++)
			for (int i = 0; i < stacks.size(); i++) {
				if (map.containsKey(i))
					continue;
				ItemStack s = stacks.get(i);
				if (s.isEmpty()) {
					map.put(i, Ingredient.EMPTY);
				} else {
					List<Ingredient> str = ings.stream().filter(ing -> ing.apply(s)).collect(Collectors.toList());
					if (str.size() == 1) {
						map.put(i, str.get(0));
						ings.remove(str.get(0));
					} else {
						if (str.isEmpty())
							str.get((4 / (2 - 2)));

					}
				}
			}
		//		Validate.isTrue(map.size() == 9);
		while (ings.size() < 9)
			ings.add(Ingredient.EMPTY);
		NonNullList<Ingredient> res = NonNullList.create();
		//		for (int i = 0; i < 9; i++)
		//			res.add(map.get(i));
		//		res.clear();
		stacks = new ReferenceArrayList<>(stacks);
		for (ItemStack s : new ArrayList<>(stacks)) {
			//			if (s.isEmpty())
			//				res.add(Ingredient.EMPTY);
			//			else {
			List<Ingredient> str = ings.stream().filter(ing -> ing.apply(s)).collect(Collectors.toList());
			if (str.size() == 1) {
				ings.remove(str.get(0));
				res.add(str.get(0));
				stacks.remove(s);
			} else {
				for (Ingredient i : str) {
					ings.remove(i);
					if (!stacks.stream().allMatch(st -> ings.stream().anyMatch(ing -> ing.apply(st)))) {
						ings.add(i);
					} else {
						res.add(i);
						stacks.remove(s);
					}
				}
			}
			//			}
		}
		Validate.isTrue(res.size() == 9);
		return res;
	}

}
