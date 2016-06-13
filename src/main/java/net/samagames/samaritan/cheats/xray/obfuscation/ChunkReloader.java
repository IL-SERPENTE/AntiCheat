/**
 * @author Aleksey Terzi
 *
 */

package net.samagames.samaritan.cheats.xray.obfuscation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.samagames.samaritan.cheats.xray.Orebfuscator;
import net.samagames.samaritan.cheats.xray.OrebfuscatorConfig;
import net.samagames.samaritan.cheats.xray.api.nms.IChunkManager;
import net.samagames.samaritan.cheats.xray.api.types.ChunkCoord;
import net.samagames.samaritan.cheats.xray.cache.ObfuscatedCachedChunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ChunkReloader extends Thread implements Runnable {
    private static final Map<World, HashSet<ChunkCoord>> loadedChunks = new WeakHashMap<World, HashSet<ChunkCoord>>();
    private static final Map<World, HashSet<ChunkCoord>> unloadedChunks = new WeakHashMap<World, HashSet<ChunkCoord>>();

    private static ChunkReloader thread = new ChunkReloader();

    private long lastExecute = System.currentTimeMillis();
    private AtomicBoolean kill = new AtomicBoolean(false);

    public static void Load() {
        if (thread == null || thread.isInterrupted() || !thread.isAlive()) {
            thread = new ChunkReloader();
            thread.setName("Orebfuscator ChunkReloader Thread");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    public static void terminate() {
        if (thread != null) {
            thread.kill.set(true);
        }
    }

    public void run() {
        HashSet<ChunkCoord> loadedChunksForProcess = new HashSet<ChunkCoord>();
        HashSet<ChunkCoord> unloadedChunksForProcess = new HashSet<ChunkCoord>();
        Map<World, HashSet<ChunkCoord>> chunksForReload = new WeakHashMap<World, HashSet<ChunkCoord>>();
        ArrayList<World> localWorldsToCheck = new ArrayList<World>();
        ArrayList<ChunkCoord> reloadedChunks = new ArrayList<ChunkCoord>();

        while (!this.isInterrupted() && !kill.get()) {
            try {
                // Wait until necessary
                long timeWait = lastExecute + OrebfuscatorConfig.ChunkReloaderRate - System.currentTimeMillis();
                lastExecute = System.currentTimeMillis();
                if (timeWait > 0) {
                    Thread.sleep(timeWait);
                }

                synchronized (loadedChunks) {
                    localWorldsToCheck.addAll(loadedChunks.keySet());
                }

                for(World world : localWorldsToCheck) {
                    HashSet<ChunkCoord> chunksForReloadForWorld = chunksForReload.get(world);
                    if(chunksForReloadForWorld == null) {
                        chunksForReload.put(world, chunksForReloadForWorld = new HashSet<ChunkCoord>());
                    }

                    synchronized (unloadedChunks) {
                        HashSet<ChunkCoord> unloadedChunksForWorld = unloadedChunks.get(world);

                        if(unloadedChunksForWorld != null && !unloadedChunksForWorld.isEmpty()) {
                            unloadedChunksForProcess.addAll(unloadedChunksForWorld);
                            unloadedChunksForWorld.clear();
                        }
                    }

                    for(ChunkCoord unloadedChunk : unloadedChunksForProcess) {
                        chunksForReloadForWorld.remove(unloadedChunk);
                    }

                    unloadedChunksForProcess.clear();

                    synchronized (loadedChunks) {
                        HashSet<ChunkCoord> loadedChunksForWorld = loadedChunks.get(world);

                        if(loadedChunksForWorld != null && !loadedChunksForWorld.isEmpty()) {
                            loadedChunksForProcess.addAll(loadedChunksForWorld);
                            loadedChunksForWorld.clear();
                        }
                    }

                    for(ChunkCoord loadedChunk : loadedChunksForProcess) {
                        ChunkCoord chunk1 = new ChunkCoord(loadedChunk.x - 1, loadedChunk.z);
                        ChunkCoord chunk2 = new ChunkCoord(loadedChunk.x + 1, loadedChunk.z);
                        ChunkCoord chunk3 = new ChunkCoord(loadedChunk.x, loadedChunk.z - 1);
                        ChunkCoord chunk4 = new ChunkCoord(loadedChunk.x, loadedChunk.z + 1);

                        chunksForReloadForWorld.add(chunk1);
                        chunksForReloadForWorld.add(chunk2);
                        chunksForReloadForWorld.add(chunk3);
                        chunksForReloadForWorld.add(chunk4);
                    }

                    loadedChunksForProcess.clear();

                    if(!chunksForReloadForWorld.isEmpty()) {
                        reloadChunks(world, chunksForReloadForWorld, reloadedChunks);

                        chunksForReloadForWorld.removeAll(reloadedChunks);
                        reloadedChunks.clear();
                    }
                }

                localWorldsToCheck.clear();
            } catch (Exception e) {
                Orebfuscator.log(e);
            }
        }
    }

    private static void reloadChunks(
            World world,
            HashSet<ChunkCoord> chunksForReloadForWorld,
            ArrayList<ChunkCoord> reloadedChunks
    )
    {
        File cacheFolder = new File(OrebfuscatorConfig.getCacheFolder(), world.getName());
        final IChunkManager chunkManager = Orebfuscator.nms.getChunkManager(world);
        final ArrayList<ChunkCoord> scheduledChunksForReload = new ArrayList<ChunkCoord>();

        for(ChunkCoord chunk : chunksForReloadForWorld) {
            if(!chunkManager.canResendChunk(chunk.x, chunk.z)) continue;

            reloadedChunks.add(chunk);

            if(OrebfuscatorConfig.UseCache) {
                ObfuscatedCachedChunk cache = new ObfuscatedCachedChunk(cacheFolder, chunk.x, chunk.z);
                if(cache.getHash() != 0) continue;
            }

            scheduledChunksForReload.add(chunk);

            //Orebfuscator.log("Add chunk x = " + chunk.x + ", z = " + chunk.z + " to schedule for reload for players");/*debug*/
        }

        Orebfuscator.instance.runTask(new Runnable() {
            public void run() {
                //Reload chunk for players
                HashSet<Player> affectedPlayers = new HashSet<Player>();

                for(ChunkCoord chunk : scheduledChunksForReload) {
                    chunkManager.resendChunk(chunk.x, chunk.z, affectedPlayers);

                    //Orebfuscator.log("Force chunk x = " + chunk.x + ", z = " + chunk.z + " to reload for players");/*debug*/
                }

                ProximityHider.addPlayersToReload(affectedPlayers);
            }
        });
    }

    private static void restart() {
        synchronized (thread) {
            if (thread.isInterrupted() || !thread.isAlive()) {
                ChunkReloader.Load();
            }
        }
    }

    public static void addLoadedChunk(World world, int chunkX, int chunkZ) {
        restart();

        synchronized (loadedChunks) {
            HashSet<ChunkCoord> chunks = loadedChunks.get(world);

            if(chunks == null) {
                loadedChunks.put(world, chunks = new HashSet<ChunkCoord>());
            }

            chunks.add(new ChunkCoord(chunkX, chunkZ));
        }
    }

    public static void addUnloadedChunk(World world, int chunkX, int chunkZ) {
        restart();

        synchronized (unloadedChunks) {
            HashSet<ChunkCoord> chunks = unloadedChunks.get(world);

            if(chunks == null) {
                unloadedChunks.put(world, chunks = new HashSet<ChunkCoord>());
            }

            chunks.add(new ChunkCoord(chunkX, chunkZ));
        }
    }
}