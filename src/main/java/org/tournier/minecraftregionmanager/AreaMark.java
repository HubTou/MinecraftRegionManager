package org.tournier.minecraftregionmanager;

public class AreaMark
{
	// An area mark is defined by 6 items:
	private String worldName;
	private int x1, z1, x2, z2;
	private String areaName;
	
	// Constructor
    public AreaMark(String world, int cx1, int cz1, int cx2, int cz2, String name)
    {
        worldName = world;
        x1 = cx1;
        z1 = cz1;
        x2 = cx2;
        z2 = cz2;
        areaName = name;
    }
    
    public String getWorldName()
    {
    	return worldName;
    }
    
    public int getX1()
    {
    	return x1;
    }
    
    public int getZ1()
    {
    	return z1;
    }
    
    public int getX2()
    {
    	return x2;
    }
    
    public int getZ2()
    {
    	return z2;
    }
    
    public String getAreaName()
    {
    	return areaName;
    }
    
    public void setX2(int cx2)
    {
    	x2 = cx2;
    }
    
    public void setZ2(int cz2)
    {
    	z2 = cz2;
    }
    
    public void setAreaName(String name)
    {
    	areaName = name;
    }
    
    public int getAreaSize()
    {
    	return ((Math.abs(x2 - x1) + 1) * (Math.abs(z2 - z1) + 1));
    }
    
    public boolean contains(int cx, int cz)
    {
    	if (cx >= x1 && cx <= x2 && cz >= z1 && cz <= z2)
        	return true;
    	else
        	return false;
    }
    
    public String printMark()
    {
    	String s = "";
    	
    	if (x1 < x2)
        	s = worldName + ":" + x1 + "," + z1 + ":" + x2 + "," + z2 + ":"+ areaName + " (blocks " + (x1*16) + "," + (z1*16) + " to " + ((x2*16)+15) + "," + ((z2*16)+15) + ")";
    	else
        	s = worldName + ":" + x1 + "," + z1 + ":" + x2 + "," + z2 + ":"+ areaName + " (blocks " + (x2*16) + "," + (z2*16) + " to " + ((x1*16)+15) + "," + ((z1*16)+15) + ")";    		

    	return s;
    }
}
