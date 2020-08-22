package hunternif.mc.impl.atlas.ext;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class TileDataHandler {
	private static final String DATA_KEY = "aAtlasTiles";

	private TileDataStorage data;

	public void onWorldLoad(ServerWorld world) {
		data = world.getPersistentStateManager().getOrCreate(() -> {
			data = new TileDataStorage(DATA_KEY);
			data.markDirty();
			return data;
		}, DATA_KEY);
	}

	public TileDataStorage getData() {
		if (data == null) { // This will happen on the client
			data = new TileDataStorage(DATA_KEY);
		}
		return data;
	}

	public void onPlayerLogin(ServerPlayerEntity player) {
		data.syncOnPlayer(player);
	}

}