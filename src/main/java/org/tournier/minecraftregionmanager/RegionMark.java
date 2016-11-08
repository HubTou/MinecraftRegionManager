package org.tournier.minecraftregionmanager;

import java.io.File;

public class RegionMark {
	// A region mark is defined by 4 items:
	private String worldName;
	private int x, z;
	private String regionName;
	
	// Constructor
    public RegionMark(String world, int rx, int rz, String name) {
        worldName = world;
        x = rx;
        z = rz;
        regionName = name;
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
    
    public String getRegionName() {
    	return regionName;
    }
    
    public File getFileName() {
    	File f = new File("./" + worldName + "/r." + x + "." + z + ".mca");
    	return f;
	}
    
    public String printMark() {
    	String s = worldName + ":" + x + "," + z + ":" + regionName + " (blocks " + (x*16*32) + "," + (z*16*32) + " to " + ((x*16*32)+511) + "," + ((z*16*32)+511) + ")";
    	return s;
    }
}
