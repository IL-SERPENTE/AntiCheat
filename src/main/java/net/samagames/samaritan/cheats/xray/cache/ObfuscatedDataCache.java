/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.samagames.samaritan.cheats.xray.cache;

import net.samagames.samaritan.Samaritan;
import net.samagames.samaritan.cheats.xray.Orebfuscator;
import net.samagames.samaritan.cheats.xray.OrebfuscatorConfig;
import net.samagames.samaritan.cheats.xray.api.nms.IChunkCache;
import net.samagames.samaritan.cheats.xray.utils.FileHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ObfuscatedDataCache {
    private static final String cacheFileName = "cache_config.yml";
    private static IChunkCache internalCache;

    private static IChunkCache getInternalCache() {
        if (internalCache == null) {
            internalCache = Orebfuscator.nms.createChunkCache();
        }
        return internalCache;
    }

    public static void closeCacheFiles() {
        getInternalCache().closeCacheFiles();
    }

    public static void checkCacheAndConfigSynchronized() throws IOException {
        String configContent = Samaritan.get().getConfig().saveToString();

        File cacheFolder = OrebfuscatorConfig.getCacheFolder();
        File cacheConfigFile = new File(cacheFolder, cacheFileName);
        String cacheConfigContent = FileHelper.readFile(cacheConfigFile);

        if(Objects.equals(configContent, cacheConfigContent)) return;

        clearCache();
    }

    public static void clearCache() throws IOException {
        closeCacheFiles();

        File cacheFolder = OrebfuscatorConfig.getCacheFolder();
        File cacheConfigFile = new File(cacheFolder, cacheFileName);

        if(cacheFolder.exists()) {
            FileHelper.delete(cacheFolder);
        }

        Orebfuscator.log("Cache cleared.");

        cacheFolder.mkdirs();

        Samaritan.get().getConfig().save(cacheConfigFile);
    }

    public static DataInputStream getInputStream(File folder, int x, int z) {
        return getInternalCache().getInputStream(folder, x, z);
    }

    public static DataOutputStream getOutputStream(File folder, int x, int z) {
        return getInternalCache().getOutputStream(folder, x, z);
    }
}