/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package net.samagames.samaritan.cheats.xray.v1_9_R2;

import net.minecraft.server.v1_9_R2.Block;
import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.Chunk;
import net.minecraft.server.v1_9_R2.ChunkProviderServer;
import net.minecraft.server.v1_9_R2.IBlockData;
import net.minecraft.server.v1_9_R2.Packet;
import net.minecraft.server.v1_9_R2.PlayerChunkMap;
import net.minecraft.server.v1_9_R2.TileEntity;
import net.minecraft.server.v1_9_R2.WorldServer;

import net.samagames.samaritan.cheats.xray.api.nms.*;
import net.samagames.samaritan.cheats.xray.api.types.BlockCoord;
import net.samagames.samaritan.cheats.xray.api.types.BlockState;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class NmsManager implements INmsManager {
    private int maxLoadedCacheFiles;

    public void setMaxLoadedCacheFiles(int value) {
        this.maxLoadedCacheFiles = value;
    }

    public INBT createNBT() {
        return new NBT();
    }

    public IChunkCache createChunkCache() {
        return new ChunkCache(this.maxLoadedCacheFiles);
    }

    public IChunkManager getChunkManager(World world) {
        WorldServer worldServer = ((CraftWorld)world).getHandle();
        PlayerChunkMap chunkMap = worldServer.getPlayerChunkMap();

        return new ChunkManager(chunkMap);
    }

    public void updateBlockTileEntity(BlockCoord blockCoord, Player player) {
        CraftWorld world = (CraftWorld)player.getWorld();
        TileEntity tileEntity = world.getTileEntityAt(blockCoord.x, blockCoord.y, blockCoord.y);

        if (tileEntity == null) {
            return;
        }

        Packet<?> packet = tileEntity.getUpdatePacket();

        if (packet != null) {
            CraftPlayer player2 = (CraftPlayer)player;
            player2.getHandle().playerConnection.sendPacket(packet);
        }
    }

    public void notifyBlockChange(World world, IBlockInfo blockInfo) {
        BlockPosition blockPosition = new BlockPosition(blockInfo.getX(), blockInfo.getY(), blockInfo.getZ());
        IBlockData blockData = ((BlockInfo)blockInfo).getBlockData();

        ((CraftWorld)world).getHandle().notify(blockPosition, blockData, blockData, 0);
    }

    public int getBlockLightLevel(World world, int x, int y, int z) {
        return ((CraftWorld)world).getHandle().getLightLevel(new BlockPosition(x, y, z));
    }

    public IBlockInfo getBlockInfo(World world, int x, int y, int z) {
        IBlockData blockData = getBlockData(world, x, y, z);

        return blockData != null
                ? new BlockInfo(x, y, z, blockData)
                : null;
    }

    public BlockState getBlockState(World world, int x, int y, int z) {
        IBlockData blockData = getBlockData(world, x, y, z);

        if(blockData == null) return null;

        Block block = blockData.getBlock();

        BlockState blockState = new BlockState();
        blockState.id = Block.getId(block);
        blockState.meta = block.toLegacyData(blockData);

        return blockState;
    }

    public int getBlockId(World world, int x, int y, int z) {
        IBlockData blockData = getBlockData(world, x, y, z);

        return blockData != null ? Block.getId(blockData.getBlock()): -1;
    }

    private static IBlockData getBlockData(World world, int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        WorldServer worldServer = ((CraftWorld)world).getHandle();
        ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();

        if(!chunkProviderServer.isLoaded(chunkX, chunkZ)) return null;

        Chunk chunk = chunkProviderServer.getOrLoadChunkAt(chunkX, chunkZ);

        return chunk.getBlockData(new BlockPosition(x, y, z));
    }
}