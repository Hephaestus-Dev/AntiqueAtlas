package hunternif.mc.atlas.item;

import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.core.AtlasData;
import hunternif.mc.atlas.core.TileInfo;
import hunternif.mc.atlas.marker.MarkersData;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.client.DimensionUpdatePacket;

public class ItemAtlas extends Item {
	static final String WORLD_ATLAS_DATA_ID = "aAtlas";

	public ItemAtlas(Item.Settings settings) {
		super(settings);
	}

	public int getAtlasID(ItemStack stack) {
		return stack.getOrCreateTag().getInt("atlasID");
	}

	/* @Override
	public String i(ItemStack stack) {
		return super.XX_1_13_i_XX(stack) + " #" + stack.getDamage();
	} */

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity playerIn,
			Hand hand) {
		ItemStack stack = playerIn.getStackInHand(hand);

		if (world.isClient) {
			AntiqueAtlasMod.proxy.openAtlasGUI(stack);
		}

		return new TypedActionResult<>(ActionResult.SUCCESS, stack);
	}

	@Override
	public void onEntityTick(ItemStack stack, World world, Entity entity, int slot, boolean isEquipped) {
		AtlasData data = AntiqueAtlasMod.atlasData.getAtlasData(stack, world);
		if (data == null || !(entity instanceof PlayerEntity)) return;

		// On the first run send the map from the server to the client:
		PlayerEntity player = (PlayerEntity) entity;
		if (!world.isClient && !data.isSyncedOnPlayer(player) && !data.isEmpty()) {
			data.syncOnPlayer(stack.getDamage(), player);
		}

		// Same thing with the local markers:
		MarkersData markers = AntiqueAtlasMod.markersData.getMarkersData(stack, world);
		if (!world.isClient && !markers.isSyncedOnPlayer(player) && !markers.isEmpty()) {
			markers.syncOnPlayer(stack.getDamage(), player);
		}

		// Updating map around player
		ArrayList<TileInfo> newTiles = data.updateMapAroundPlayer(player);
		
		if (!world.isClient) {
			if (newTiles.size() > 0) {
				DimensionUpdatePacket packet = new DimensionUpdatePacket(stack.getDamage(), player.dimension);
				for (TileInfo t : newTiles) {
					packet.addTile(t.x, t.z, t.biome);
				}
				PacketDispatcher.sendToAll(packet);
			}
		}
	}

}
