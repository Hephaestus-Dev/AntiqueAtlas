package hunternif.mc.atlas.api.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import hunternif.mc.atlas.core.TileKind;
import hunternif.mc.atlas.core.TileKindFactory;
import net.minecraft.client.network.packet.LoginSuccessS2CPacket;

import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.TileAPI;
import hunternif.mc.atlas.client.BiomeTextureMap;
import hunternif.mc.atlas.client.TextureSet;
import hunternif.mc.atlas.client.TextureSetMap;
import hunternif.mc.atlas.core.AtlasData;
import hunternif.mc.atlas.ext.ExtBiomeData;
import hunternif.mc.atlas.ext.ExtTileIdMap;
import hunternif.mc.atlas.ext.ExtTileTextureMap;
import hunternif.mc.atlas.ext.TileIdRegisteredEvent;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.bidirectional.PutBiomeTilePacket;
import hunternif.mc.atlas.network.client.DeleteCustomGlobalTilePacket;
import hunternif.mc.atlas.network.client.TileNameIDPacket;
import hunternif.mc.atlas.network.client.TilesPacket;
import hunternif.mc.atlas.network.server.RegisterTileIdPacket;
import hunternif.mc.atlas.util.Log;

public class TileApiImpl implements TileAPI {
	/**
	 * When a tile is being put in an atlas on the client, the pseudo-biome ID
	 * may have been unregistered yet at that moment. In that case the tile data
	 * is put into this map to be later registered when the server sends the
	 * packet with the pseudo-biome ID for the corresponding unique name.
	 */
	private final Map<String, TileData> pendingTiles = new HashMap<>();
	private static class TileData {
		final World world;
		final int atlasID, x, z;
		TileData(World world, int atlasID, int x, int z) {
			this.world = world;
			this.atlasID = atlasID;
			this.x = x;
			this.z = z;
		}
	}
	
	public TileApiImpl() {
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	
	@Override
	public TextureSet registerTextureSet(String name, Identifier... textures) {
		TextureSet textureSet = new TextureSet(name, textures);
		TextureSetMap.instance().register(textureSet);
		return textureSet;
	}
	
	
	// Biome textures ==========================================================

	@Override
	public void setBiomeTexture(Biome biome, String textureSetName, Identifier... textures) {
		TextureSet set = new TextureSet(textureSetName, textures);
		TextureSetMap.instance().register(set);
		BiomeTextureMap.instance().setTexture(biome, set);
	}

	@Override
	public void setBiomeTexture(Biome biome, TextureSet textureSet) {
		BiomeTextureMap.instance().setTexture(biome, textureSet);
	}
	
	
	// Custom tile textures ====================================================
	
	@Override
	public void setCustomTileTexture(String uniqueTileName, Identifier ... textures) {
		TextureSet set = new TextureSet(uniqueTileName, textures);
		TextureSetMap.instance().register(set);
		setCustomTileTexture(uniqueTileName, set);
	}
	
	@Override
	public void setCustomTileTexture(String uniqueTileName, TextureSet textureSet) {
		ExtTileTextureMap.instance().setTexture(uniqueTileName, textureSet);
	}
	
	
	// Biome tiles =============================================================

	@Override
	public void putBiomeTile(World world, int atlasID, Biome biome, int chunkX, int chunkZ) {
		DimensionType dimension = world.dimension.getType();
		PutBiomeTilePacket packet = new PutBiomeTilePacket(atlasID, dimension, chunkX, chunkZ, biome);
		if (world.isClient) {
			PacketDispatcher.sendToServer(packet);
		} else {
			AtlasData data = AntiqueAtlasMod.atlasData.getAtlasData(atlasID, world);
			TileKind tile = TileKindFactory.get(biome);
			data.setTile(dimension, chunkX, chunkZ, tile);
			for (PlayerEntity syncedPlayer : data.getSyncedPlayers()) {
				PacketDispatcher.sendTo(new PutBiomeTilePacket(atlasID, dimension, chunkX, chunkZ, biome), (ServerPlayerEntity) syncedPlayer);
			}
		}
	}
	
	
	// Custom tiles ============================================================
	
	@Override
	public void putCustomTile(World world, int atlasID, String tileName, int chunkX, int chunkZ) {
		if (tileName == null) {
			Log.error("Attempted to put custom tile with null name");
			return;
		}
		if (world.isClient) {
			int biomeID = ExtTileIdMap.instance().getPseudoBiomeID(tileName);
			if (biomeID != ExtTileIdMap.NOT_FOUND) {
				putBiomeTile(world, atlasID, biomeID, chunkX, chunkZ);
			} else {
				pendingTiles.put(tileName, new TileData(world, atlasID, chunkX, chunkZ));
				PacketDispatcher.sendToServer(new RegisterTileIdPacket(tileName));
			}
		} else {
			int biomeID = ExtTileIdMap.instance().getPseudoBiomeID(tileName);
			if (biomeID == ExtTileIdMap.NOT_FOUND) {
				biomeID = ExtTileIdMap.instance().getOrCreatePseudoBiomeID(tileName);
				TileNameIDPacket packet = new TileNameIDPacket();
				packet.put(tileName, biomeID);
				PacketDispatcher.sendToAll(packet);
			}
			putBiomeTile(world, atlasID, biomeID, chunkX, chunkZ);
		}
	}
	
	@Override
	public void putCustomGlobalTile(World world, String tileName, int chunkX, int chunkZ) {
		if (tileName == null) {
			Log.error("Attempted to put custom global tile with null name");
			return;
		}
		if (world.isClient) {
			Log.warn("Client attempted to put global tile");
			return;
		}
		boolean isIdRegistered = ExtTileIdMap.instance().getPseudoBiomeID(tileName) != ExtTileIdMap.NOT_FOUND;
		int biomeID = ExtTileIdMap.instance().getOrCreatePseudoBiomeID(tileName);
		ExtBiomeData data = AntiqueAtlasMod.extBiomeData.getData();
		data.setBiomeIdAt(world.dimension.getType(), chunkX, chunkZ, biomeID);
		// Send name-ID packet:
		if (!isIdRegistered) {
			TileNameIDPacket packet = new TileNameIDPacket();
			packet.put(tileName, biomeID);
			PacketDispatcher.sendToAll(packet);
		}
		// Send tile packet:
		TilesPacket packet = new TilesPacket(world.dimension.getType());
		packet.addTile(chunkX, chunkZ, biomeID);
		PacketDispatcher.sendToAll(packet);
	}
	
	@SubscribeEvent
	public void onTileIdRegistered(TileIdRegisteredEvent event) {
		for (Entry<String, Integer> entry : event.nameToIdMap.entrySet()) {
			// Put pending tiles:
			TileData tile = pendingTiles.remove(entry.getKey());
			if (tile != null) {
				putBiomeTile(tile.world, tile.atlasID, entry.getValue(), tile.x, tile.z);
			}
		}
	}


	@Override
	public void deleteCustomGlobalTile(World world, int chunkX, int chunkZ) {
		if (world.isClient) {
			Log.warn("Client attempted to delete global tile");
			return;
		}
		ExtBiomeData data = AntiqueAtlasMod.extBiomeData.getData();
		DimensionType dimension = world.dimension.getType();
		if (data.getBiomeIdAt(dimension, chunkX, chunkZ) != -1) {
			data.removeBiomeAt(dimension, chunkX, chunkZ);
			PacketDispatcher.sendToAll(new DeleteCustomGlobalTilePacket(dimension, chunkX, chunkZ));
		}
	}
}
