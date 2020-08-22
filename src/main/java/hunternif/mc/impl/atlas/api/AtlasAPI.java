package hunternif.mc.impl.atlas.api;

import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.api.oldimpl.MarkerApiImpl;
import hunternif.mc.impl.atlas.api.oldimpl.TileApiImpl;
import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Use this class to obtain a reference to the APIs.
 * @author Hunternif
 */
public class AtlasAPI {
	private static final int VERSION = 4;
	public static final TileAPI tiles = new TileApiImpl();
	public static final MarkerAPI markers = new MarkerApiImpl();

	/** Version of the API, meaning only this particular class. You might
	 * want to check static field VERSION in the specific API interfaces. */
	public static int getVersion() {
		return VERSION;
	}

	public static Item getAtlasItem() {
		return Registry.ITEM.get(new Identifier("antiqueatlas:antique_atlas"));
	}

	/** API for biomes and custom tiles (i.e. dungeons, towns etc). */
	public static TileAPI getTileAPI() {
		return tiles;
	}
	
	/** API for custom markers. */
	public static MarkerAPI getMarkerAPI() {
		return markers;
	}
	
	/** Convenience method that returns a list of atlas IDs for all atlas items
	 * the player is currently carrying. **/
	public static List<Integer> getPlayerAtlases(PlayerEntity player) {
		if (!AntiqueAtlasMod.CONFIG.itemNeeded) {
			return Collections.singletonList(player.getUuid().hashCode());
		}

		List<Integer> list = new ArrayList<>();
		for (ItemStack stack : player.inventory.main) {
			if (!stack.isEmpty() && stack.getItem() instanceof AtlasItem) {
				list.add(((AtlasItem) stack.getItem()).getAtlasID(stack));
			}
		}
		for (ItemStack stack : player.inventory.offHand) {
			if (!stack.isEmpty() && stack.getItem() instanceof AtlasItem) {
				list.add(((AtlasItem) stack.getItem()).getAtlasID(stack));
			}
		}

		return list;
	}
}