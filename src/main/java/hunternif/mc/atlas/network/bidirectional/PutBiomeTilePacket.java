package hunternif.mc.atlas.network.bidirectional;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.SettingsConfig;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.core.AtlasData;
import hunternif.mc.atlas.core.TileKindFactory;
import hunternif.mc.atlas.network.AbstractMessage;
import hunternif.mc.atlas.util.Log;
import net.fabricmc.api.EnvType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;

/**
 * Puts biome tile into one atlas. When sent to server, forwards it to every
 * client that has this atlas' data synced.
 * @author Hunternif
 */
public class PutBiomeTilePacket extends AbstractMessage<PutBiomeTilePacket> {
	private int atlasID, x, z;
	private DimensionType dimension;
	private Biome biomeID;
	
	public PutBiomeTilePacket() {}
	
	public PutBiomeTilePacket(int atlasID, DimensionType dimension, int x, int z, Biome biomeID) {
		this.atlasID = atlasID;
		this.dimension = dimension;
		this.x = x;
		this.z = z;
		this.biomeID = biomeID;
	}
	
	@Override
	protected void read(PacketByteBuf buffer) throws IOException {
		atlasID = buffer.readVarInt();
		dimension = Registry.DIMENSION.get(buffer.readVarInt());
		x = buffer.readVarInt();
		z = buffer.readVarInt();
		biomeID = Registry.BIOME.get(buffer.readVarInt());
	}

	@Override
	protected void write(PacketByteBuf buffer) throws IOException {
		buffer.writeVarInt(atlasID);
		buffer.writeVarInt(Registry.DIMENSION.getRawId(dimension));
		buffer.writeVarInt(x);
		buffer.writeVarInt(z);
		buffer.writeVarInt(Registry.BIOME.getRawId(biomeID));
	}

	@Override
	protected void process(PlayerEntity player, EnvType side) {
		if (side == EnvType.SERVER) {
			// Make sure it's this player's atlas :^)
			// TODO Fabric
			if (SettingsConfig.gameplay.itemNeeded /* &&
					!player.bB.h(new ata(RegistrarAntiqueAtlas.ATLAS, 1, atlasID)) */) {
				Log.warn("Player %s attempted to modify someone else's Atlas #%d",
						player.getCommandSource().getName(), atlasID);
				return;
			}
			AtlasAPI.tiles.putBiomeTile(player.getEntityWorld(), atlasID, biomeID, x, z);
		} else {
			AtlasData data = AntiqueAtlasMod.atlasData.getAtlasData(atlasID, player.getEntityWorld());
			data.setTile(dimension, x, z, TileKindFactory.get(biomeID));
		}
	}

}
