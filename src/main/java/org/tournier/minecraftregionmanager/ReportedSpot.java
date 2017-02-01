package org.tournier.minecraftregionmanager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

public class ReportedSpot
{
	// A reported spot is defined by 4 items:
	private String worldName;
	private int x, z;
	private String reportReason;
	
	// And associated to a list of player's UUID
	private LinkedList<UUID> reportingPlayers;
	
	// Constructor
    public ReportedSpot(String world, int sx, int sz, String reason, UUID id)
    {
        worldName = world;
        x = sx;
        z = sz;
        reportReason = reason.toLowerCase();
        reportingPlayers = new LinkedList<UUID>();
        reportingPlayers.addLast(id);
    }
    
    public String getWorldName()
    {
    	return worldName;
    }
    
    public int getX()
    {
    	return x;
    }
    
    public int getZ()
    {
    	return z;
    }
    
    public String getReportReason()
    {
    	return reportReason;
    }
    
    public LinkedList<UUID> getReportingPlayers()
    {
    	return reportingPlayers;
    }
    
    public boolean IsPlayerAmongReporters(UUID id)
    {
        Iterator<UUID> i = reportingPlayers.iterator();
        while(i.hasNext())
        {
        	UUID u = i.next();
        	if (u.equals(id))
        		return true;
        }
    	return false;
    }

    public void AddReporter(UUID id)
    {
    	reportingPlayers.addLast(id);
    }
    
    public String printMark()
    {
    	String s = worldName + ":" + x + "," + z + ":" + reportReason + " (reported by " + reportingPlayers.size() + " player";
    	if (reportingPlayers.size() > 1)
    		s += "s";
    	s += ")";
    	return s;
    }
}
