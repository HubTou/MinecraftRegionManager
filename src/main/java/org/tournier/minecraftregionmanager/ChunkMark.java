package org.tournier.minecraftregionmanager;

import java.io.File;

public class ChunkMark {
	// A chunk mark is defined by 4 items:
	private String worldName;
	private int x, z;
	private String chunkName;
	
	// Constructor
    public ChunkMark(String world, int cx, int cz, String name) {
        worldName = world;
        x = cx;
        z = cz;
        chunkName = name;
    }
    
    public String getWorldName() {
    	return worldName;
    }
    
    public int getX() {
    	return x;
    }
    
    public int getZ() {
    	return z;
    }
    
    public String getChunkName() {
    	return chunkName;
    }
    
    public File getFileName() {
		int rx = (int) Math.floor((double) x / 32);
		int rz = (int) Math.floor((double) z / 32);
    	File f = new File("./" + worldName + "/r." + rx + "." + rz + ".mca");
    	return f;
	}
    
    public String printMark() {
    	String s = worldName + ":" + x + "," + z + ":" + chunkName + " (blocks " + (x*16) + "," + (z*16) + " to " + ((x*16)+15) + "," + ((z*16)+15) + ")";
    	return s;
    }
}
