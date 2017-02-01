package org.tournier.minecraftregionmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class minecraftregionmanager extends JavaPlugin implements Listener
{	
	private HashMap<String, AreaMark> playersPartialAreaMark = new HashMap<String, AreaMark>();
	private HashMap<String, LinkedList<AreaMark>> playersAreaMarks = new HashMap<String, LinkedList<AreaMark>>();
	private HashMap<String, LinkedList<ChunkMark>> playersChunkMarks = new HashMap<String, LinkedList<ChunkMark>>();
	private HashMap<String, LinkedList<RegionMark>> playersRegionMarks = new HashMap<String, LinkedList<RegionMark>>();
	private LinkedList<ReportedSpot> reportedRegions = new LinkedList<ReportedSpot>();
	private LinkedList<ReportedSpot> reportedChunks = new LinkedList<ReportedSpot>();	
	private HashMap<String, LinkedList<MarkedRegion>> worldsMarkedRegions = new HashMap<String, LinkedList<MarkedRegion>>();

	final int NOWHERE = 0;
	final int IN_AREA_MARKS = 1;
	final int IN_CHUNK_MARKS = 2;
	final int IN_REGION_MARKS = 3;
	final int REGION_WIDTH_IN_CHUNKS = 32;

	/////////////////////////////////////////////////////////////////////////////////////
	private void loadPlayerMarks(Player player) throws FileNotFoundException, IOException
	/////////////////////////////////////////////////////////////////////////////////////
	{
    	LinkedList<AreaMark> areaMarks = new LinkedList<AreaMark>();
    	LinkedList<ChunkMark> chunkMarks = new LinkedList<ChunkMark>();
    	LinkedList<RegionMark> regionMarks = new LinkedList<RegionMark>();

        try
        {
            int status = NOWHERE;
            Pattern areaPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):([-0-9]*),([-0-9]*):(.*)");
            Pattern chunkPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):(.*)");
            Pattern regionPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):(.*)");
            BufferedReader buffer = new BufferedReader(new FileReader("./plugins/minecraftregionmanager/userdata/" + player.getUniqueId() + ".txt"));
            String line;
            while (null != (line = buffer.readLine()))
            {
            	if (line.startsWith("Areas:"))
            		status = IN_AREA_MARKS;
            	else if (line.startsWith("Chunks:"))
            		status = IN_CHUNK_MARKS;
            	else if (line.startsWith("Regions:"))
            		status = IN_REGION_MARKS;
            	else if (line.startsWith("  "))
            	{
            		Matcher m;
            		switch (status)
            		{
            			case IN_AREA_MARKS:
            				m = areaPattern.matcher(line);
                    		if (m.matches())
                    		{
                    			AreaMark mark = new AreaMark(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), m.group(6));
                    	        areaMarks.addLast(mark);    	        
                            }
            				break;
            			case IN_CHUNK_MARKS: ;
        					m = chunkPattern.matcher(line);
        	        		if (m.matches())
        	        		{
                    			ChunkMark mark = new ChunkMark(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4));
                    	        chunkMarks.addLast(mark);    	        
        	                }
        					break;
            			case IN_REGION_MARKS: ;
            				m = regionPattern.matcher(line);
                    		if (m.matches())
                    		{
                    			RegionMark mark = new RegionMark(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4));
                    	        regionMarks.addLast(mark);    	        
                            }
        					break;
            		}
            	}
            }
            buffer.close();
        }
        catch (FileNotFoundException e1)
        {
        	// The player's list of marks has to be initialized
        	playersAreaMarks.put(player.getName(), areaMarks);
        	playersChunkMarks.put(player.getName(), chunkMarks);
        	playersRegionMarks.put(player.getName(), regionMarks);
        }
        catch (IOException e2)
        {
			e2.printStackTrace();
        }
        
    	playersAreaMarks.put(player.getName(), areaMarks);
    	playersChunkMarks.put(player.getName(), chunkMarks);
    	playersRegionMarks.put(player.getName(), regionMarks);
	}

	//////////////////////////////////////////////////////////////
	private void savePlayerMarks(Player player) throws IOException
	//////////////////////////////////////////////////////////////
	{
		FileWriter file = null;

		try
		{
			boolean firstArea = true;
			boolean firstChunk = true;
			boolean firstRegion = true;
			
			file = new FileWriter("./plugins/minecraftregionmanager/userdata/" + player.getUniqueId() + ".txt");

	    	// Write the marked areas list
	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
	        Iterator<AreaMark> i = areaMarks.iterator();
	        while(i.hasNext())
	        {
	        	AreaMark a = i.next();
	        	if (firstArea == true)
	        	{
	    			file.write("Areas:" + System.getProperty("line.separator"));
	        		firstArea = false;
	        	}
	    		file.write("  " + a.getWorldName() + ":" + a.getX1() + "," + a.getZ1() + ":" + a.getX2() + "," + a.getZ2() + ":" + a.getAreaName() + System.getProperty("line.separator"));        				
	        }

	    	// Write the marked chunks list
	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(player.getName());    			
	        Iterator<ChunkMark> j = chunkMarks.iterator();
	        while(j.hasNext())
	        {
	        	ChunkMark c = j.next();
	        	if (firstChunk == true)
	        	{
	    			file.write("Chunks:" + System.getProperty("line.separator"));
	    	        firstChunk = false;
	        	}
	        	file.write("  " + c.getWorldName() + ":" + c.getX() + "," + c.getZ() + ":" + c.getChunkName() + System.getProperty("line.separator"));
	        }
			
	    	// Write the marked regions list
	    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(player.getName());
	        Iterator<RegionMark> k = regionMarks.iterator();
	        while(k.hasNext())
	        {
	        	RegionMark r = k.next();
	        	if (firstRegion == true)
	        	{
	    			file.write("Regions:" + System.getProperty("line.separator"));
	    			firstRegion = false;
	        	}
	        	file.write("  " + r.getWorldName() + ":" + r.getX() + "," + r.getZ() + ":" + r.getRegionName() + System.getProperty("line.separator"));
	        }
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (file != null)
				file.close();
		}		
	}
	
	////////////////////////////////////////////////
	private void emptyPlayerMarks(String playerName)
	////////////////////////////////////////////////
	{
		// Get the marked regions list for this player
    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(playerName);
    	// Empty it
    	regionMarks.clear();    	        
		// Put it back in the full list
		playersRegionMarks.put(playerName, regionMarks);

		// Get the marked chunks list for this player
    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(playerName);
    	// Empty it
    	chunkMarks.clear();    	        
		// Put it back in the full list
		playersChunkMarks.put(playerName, chunkMarks);

		// Get the marked areas list for this player
    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(playerName);
    	// Empty it
    	areaMarks.clear();    	        
		// Put it back in the full list
		playersAreaMarks.put(playerName, areaMarks);
	}

	///////////////////////////////////////////////
	private void freePlayerMarks(String playerName)
	///////////////////////////////////////////////
	{
		// Remove all marks declared by this player 
		emptyPlayerMarks(playerName);
		
		// Remove the hash entries for this player
    	playersRegionMarks.remove(playerName);
        playersChunkMarks.remove(playerName);
        playersAreaMarks.remove(playerName);
	}

	//////////////////////////////////////////////
	private void loadAllMarks() throws IOException
	//////////////////////////////////////////////
	{
		File		directory = new File("./plugins/minecraftregionmanager/userdata");
		String[] 	filesList;

		filesList = directory.list();
		for(int i = 0; i < filesList.length; i++)
		{
	        try
	        {
	            int status = NOWHERE;
	            Pattern areaPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):([-0-9]*),([-0-9]*):(.*)");
	            Pattern chunkPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):(.*)");
	            Pattern regionPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):(.*)");
	            BufferedReader buffer = new BufferedReader(new FileReader("./plugins/minecraftregionmanager/userdata/" + filesList[i]));
	            String line;
	            while (null != (line = buffer.readLine()))
	            {
	            	if (line.startsWith("Areas:"))
	            		status = IN_AREA_MARKS;
	            	else if (line.startsWith("Chunks:"))
	            		status = IN_CHUNK_MARKS;
	            	else if (line.startsWith("Regions:"))
	            		status = IN_REGION_MARKS;
	            	else if (line.startsWith("  "))
	            	{
	            		Matcher m;
	            		switch (status) {
	            			case IN_AREA_MARKS:
	            				m = areaPattern.matcher(line);
	                    		if (m.matches())
	                    		{
	                    			// Get the marked regions for this world
	                    			LinkedList<MarkedRegion> markedRegionsList = worldsMarkedRegions.get(m.group(1));
	                    			if (markedRegionsList == null)
	                    				markedRegionsList = new LinkedList<MarkedRegion>(); 

	                    			int lx, hx, lz, hz;
	                    			if (Integer.parseInt(m.group(2)) <= Integer.parseInt(m.group(4)))
	                    			{
	                    				lx=Integer.parseInt(m.group(2));
	                    				hx=Integer.parseInt(m.group(4));
	                    			}
	                    			else
	                    			{
	                    				lx=Integer.parseInt(m.group(4));
	                    				hx=Integer.parseInt(m.group(2));                   				
	                    			}
	                    			if (Integer.parseInt(m.group(3)) <= Integer.parseInt(m.group(5)))
	                    			{
	                    				lz=Integer.parseInt(m.group(3));
	                    				hz=Integer.parseInt(m.group(5));
	                    			}
	                    			else
	                    			{
	                    				lz=Integer.parseInt(m.group(5));
	                    				hz=Integer.parseInt(m.group(3));                   				
	                    			}

	                    			MarkedRegion r = null;
	                    			for (int c = lx; c <= hx; c++)
	                    				for (int d = lz; d <= hz; d++)
	                    				{
	                        	    		int rx = (int) Math.floor((double) c / 32);
	                        	    		int rz = (int) Math.floor((double) d / 32);

	                        	    		// Are we still in the same region?
	                        	    		if (r != null && r.getX() == rx && r.getZ() == rz)
	                        	    			r.MarkChunk(c, d);
	                        	    		else
	                        	    		{
	                        	    			// Modify or add that chunk's region in the list of marked ones
	                        	    			Iterator<MarkedRegion> j = markedRegionsList.iterator();
	    	                    	        	boolean alreadyThere = false;
	    	                    	        	while(j.hasNext())
	    	                    	        	{
	    	                    	        		r = j.next();
	    	                    	        		if (r.getX() == rx && r.getZ() == rz)
	    	                    	        		{
	    	                    	        			alreadyThere = true;
	    	                    	        			r.MarkChunk(c, d);
	    	                    	        			break;
	    	                    	        		}
	    	                    	        	}
	    	                    	        	if (alreadyThere == false)
	    	                    	        	{
	    	                    	        		MarkedRegion markedRegion = new MarkedRegion(rx, rz);
	    		                    				markedRegion.MarkChunk(c, d);
	    		                    				markedRegionsList.addLast(markedRegion);
	    	                    	        	}
	                        	    		}
	                    				}
	                    			
	                    	        // Put the list back in place
	                    			worldsMarkedRegions.put(m.group(1), markedRegionsList);	                    		
	                            }
	            				break;
	            				
	            			case IN_CHUNK_MARKS: ;
	        					m = chunkPattern.matcher(line);
	        	        		if (m.matches())
	        	        		{
	                    			// Get the marked regions for this world
	                    			LinkedList<MarkedRegion> markedRegionsList = worldsMarkedRegions.get(m.group(1));
	                    			if (markedRegionsList == null)
	                    				markedRegionsList = new LinkedList<MarkedRegion>(); 
	                    	    	
	                    			// Modify or add that chunk's region in the list of marked ones
	                    	        Iterator<MarkedRegion> j = markedRegionsList.iterator();
	                    	        boolean alreadyThere = false;
                    	    		int rx = (int) Math.floor((double) Integer.parseInt(m.group(2)) / 32);
                    	    		int rz = (int) Math.floor((double) Integer.parseInt(m.group(3)) / 32);
	                    	        while(j.hasNext())
	                    	        {
	                    	        	MarkedRegion r = j.next();
	                    	        	if (r.getX() == rx && r.getZ() == rz)
	                    	        	{
	                    	        		alreadyThere = true;
	                    	        		r.MarkChunk(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
	                    	        		break;
	                    	        	}
	                    	        }
	                    	        if (alreadyThere == false)
	                    	        {
		                    			MarkedRegion markedRegion = new MarkedRegion(rx, rz);
		                    			markedRegion.MarkChunk(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
		                    			markedRegionsList.addLast(markedRegion);
	                    	        }

	                    	        // Put the list back in place
	                    			worldsMarkedRegions.put(m.group(1), markedRegionsList);	                    		
	        	                }
	        					break;
	        					
	            			case IN_REGION_MARKS: ;
	            				m = regionPattern.matcher(line);
	                    		if (m.matches())
	                    		{
	                    			// Get the marked regions for this world²
	                    			LinkedList<MarkedRegion> markedRegionsList = worldsMarkedRegions.get(m.group(1));
	                    			if (markedRegionsList == null)
	                    				markedRegionsList = new LinkedList<MarkedRegion>(); 
	                    	    	
	                    			// Modify or add that region in the list of marked ones
	                    	        Iterator<MarkedRegion> j = markedRegionsList.iterator();
	                    	        boolean alreadyThere = false;
	                    	        while(j.hasNext())
	                    	        {
	                    	        	MarkedRegion r = j.next();
	                    	        	if (r.getX() == Integer.parseInt(m.group(2)) && r.getZ() == Integer.parseInt(m.group(3)))
	                    	        	{
	                    	        		alreadyThere = true;
	                    	        		r.MarkAllChunks();
	                    	        		break;
	                    	        	}
	                    	        }
	                    	        if (alreadyThere == false)
	                    	        {
		                    			MarkedRegion markedRegion = new MarkedRegion(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
		                    			markedRegion.MarkAllChunks();
		                    			markedRegionsList.addLast(markedRegion);
	                    	        }

	                    	        // Put the list back in place
	                    			worldsMarkedRegions.put(m.group(1), markedRegionsList);
	                    		}
	        					break;
	            		}
	            	}
	            }
	            buffer.close();
	        }
	        catch (IOException e)
	        {
				e.printStackTrace();
	        }
		}
	}

	/////////////////////////////////////////////////////////////////////////
	private void loadAdminReports() throws FileNotFoundException, IOException
	/////////////////////////////////////////////////////////////////////////
	{
		final int NOWHERE = 0;
		final int IN_REPORTED_REGIONS = 1;
		final int IN_REPORTED_CHUNKS = 2;
		
        try
        {
            int status = NOWHERE;
            Pattern spotPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):([^:]*):(.*)");
            BufferedReader buffer = new BufferedReader(new FileReader("./plugins/minecraftregionmanager/reports.txt"));
            String line;
            while (null != (line = buffer.readLine()))
            {
            	if (line.startsWith("Reported regions:"))
            		status = IN_REPORTED_REGIONS;
            	else if (line.startsWith("Reported chunks:"))
            		status = IN_REPORTED_CHUNKS;
            	else if (line.startsWith("  "))
            	{
            		Matcher m;
            		String[] r;
            		switch (status) {
            			case IN_REPORTED_REGIONS: ;
        					m = spotPattern.matcher(line);
        	        		if (m.matches())
        	        		{
        	            		r = m.group(5).split(":", -1);
        	        			ReportedSpot s = new ReportedSpot(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4), UUID.fromString(r[0]));
                    			for (int i = 1; i < r.length; i++)
                    				s.AddReporter(UUID.fromString(r[i]));
                    			reportedRegions.addLast(s);
        	                }
        					break;
        					
            			case IN_REPORTED_CHUNKS: ;
            				m = spotPattern.matcher(line);
            				if (m.matches())
            				{
        	            		r = m.group(5).split(":", -1);
            					ReportedSpot s = new ReportedSpot(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4), UUID.fromString(r[0]));
            					for (int i = 1; i < r.length; i++)
            						s.AddReporter(UUID.fromString(r[i]));
            					reportedChunks.addLast(s);
            				}
        					break;
            		}
            	}
            }
            buffer.close();
        }
        catch (FileNotFoundException e1)
        {
        	// Nothing to do, it's OK	
        }
        catch (IOException e2)
        {
			e2.printStackTrace();
        }
	}
	
	/////////////////////////////////////////////////////////////////////////
	private void saveAdminReports() throws FileNotFoundException, IOException
	/////////////////////////////////////////////////////////////////////////
	{
		boolean firstRegion = true;
		boolean firstChunk = true;

		FileWriter file = null;

		try
		{
			file = new FileWriter("./plugins/minecraftregionmanager/reports.txt");

	    	// Write the reported regions list
	        Iterator<ReportedSpot> i = reportedRegions.iterator();
	        while (i.hasNext())
	        {
	        	ReportedSpot s = i.next();
	        	if (firstRegion == true)
	        	{
	    			file.write("Reported regions:" + System.getProperty("line.separator"));
	        		firstRegion = false;
	        	}
	    		file.write("  " + s.getWorldName() + ":" + s.getX() + "," + s.getZ() + ":" + s.getReportReason());
	    		LinkedList<UUID> players = s.getReportingPlayers();
	    		Iterator<UUID> k = players.iterator();
	    		while (k.hasNext())
	    		{
	    			UUID id = k.next();
	    			file.write(":" + id);
	    		}
	    		file.write(System.getProperty("line.separator"));    				
	        }

	    	// Write the reported chunks list
	        Iterator<ReportedSpot> j = reportedChunks.iterator();
	        while (j.hasNext())
	        {
	        	ReportedSpot s = j.next();
	        	if (firstChunk == true)
	        	{
	    			file.write("Reported chunks:" + System.getProperty("line.separator"));
	        		firstChunk = false;
	        	}
	    		file.write("  " + s.getWorldName() + ":" + s.getX() + "," + s.getZ() + ":" + s.getReportReason());
	    		LinkedList<UUID> players = s.getReportingPlayers();
	    		Iterator<UUID> k = players.iterator();
	    		while (k.hasNext())
	    		{
	    			UUID id = k.next();
	    			file.write(":" + id);
	    		}
	    		file.write(System.getProperty("line.separator"));    				
	        }
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (file != null)
				file.close();
		}
	}
	
	//////////////////////////////////////////
	private String getUniqueWorldName(World w)
	//////////////////////////////////////////
	{
		// Build a truly unique world name
		// With the LOTR mod, there are 3 worlds named "world":
		//   'world'/Overworld
		//   'world'/Utumno
		//   'world'/MiddleEarth
		String n = w.getWorldFolder().toString();
		n = n.replaceFirst("^..", "");
    	n = n.replaceAll("\\\\", "/");
    	
    	return n;
	}

	/////////////////////////////////////////
	private String getMarkName(String[] args)
	/////////////////////////////////////////
	{
		// Build the name of a mark from a Minecraft command args
		// and remove forbidden characters
    	String n = "";
		for (int i = 0; i < args.length; i++)
		{
			n += args[i] + " ";
		}
    	n = n.replaceAll(":", " ");
		n = n.trim();
		
		return n;
	}

	/////////////////////////////////////////////////////////////////////////
	private boolean isRegionMarked(String w, int rx, int rz, CommandSender s)
	/////////////////////////////////////////////////////////////////////////
	// NB: a region where all individual chunks are marked won't be reported as wholly marked 
	{
		Player p = (Player) s;
		if (! s.isOp())
		{
			// Get the marked regions list for this player
		   	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(p.getName());

	        // Search for the region in the list
	        Iterator<RegionMark> i = regionMarks.iterator();
	        while(i.hasNext())
	        {
	        	RegionMark m = i.next();
	        	if (m.getWorldName().equals(w) && m.getX() == rx && m.getZ() == rz)
	        		return true;
	        }
		}
		else
		{
			try
			{
				// Get marked items in this world for all players
				loadAllMarks();				
 				LinkedList<MarkedRegion> markedRegionsList = worldsMarkedRegions.get(w);
       			if (markedRegionsList != null)
       			{
       				// Search for the region in the list
  	    			Iterator<MarkedRegion> i = markedRegionsList.iterator();
       	        	while(i.hasNext())
       	        	{
       	        		MarkedRegion r = i.next();
       	        		if (r.getX() == rx && r.getZ() == rz)
       	        			if (r.IsWhollyMarked())
       	        				return true;
       	        			else
       	        				return false;
       	        	}                				
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return false;
 	}
	
	////////////////////////////////////////////////////////////////////////
	private boolean isChunkMarked(String w, int cx, int cz, CommandSender s)
	////////////////////////////////////////////////////////////////////////
	{
		Player p = (Player) s;
		if (! s.isOp())
		{
			// Get the marked chunks list for this player
	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(p.getName());

	        // Search for the chunk in the list
	        Iterator<ChunkMark> i = chunkMarks.iterator();
	        while(i.hasNext())
	        {
	        	ChunkMark m = i.next();
	        	if (m.getWorldName().equals(w) && m.getX() == cx && m.getZ() == cz)
	        		return true;
	        }			

	        // Get the marked areas list for this player
		   	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(p.getName());

	        // Search for the chunk in the list
	        Iterator<AreaMark> j = areaMarks.iterator();
	        while(j.hasNext())
	        {
	        	AreaMark a = j.next();
	        	if (a.getWorldName().equals(w) && a.contains(cx, cz))
	        		return true;
	        }
		}
		else
		{
			try
			{
				// Get marked items in this world for all players
				loadAllMarks();
    			int rx = (int) Math.floor((double) cx / 32);
    			int rz = (int) Math.floor((double) cz / 32);
 				LinkedList<MarkedRegion> markedRegionsList = worldsMarkedRegions.get(w);
       			if (markedRegionsList != null)
       			{
       				// Search for the chunk in the list
  	    			Iterator<MarkedRegion> i = markedRegionsList.iterator();
       	        	while(i.hasNext())
       	        	{
       	        		MarkedRegion r = i.next();
       	        		if (r.getX() == rx && r.getZ() == rz)
       	        			if (r.IsChunkMarked(cx, cz))
       	        				return true;
       	        			else
       	        				return false;
       	        	}                				
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}
	
	////////////////////////////////////////////////////////////////////
	private boolean loadRegionFile(Path fileName, RegionFile regionFile)
	////////////////////////////////////////////////////////////////////
	{
		if (regionFile == null)
			return false;
		
		try
		{
			int rc = regionFile.loadRegion(fileName);
			
			if (rc == 0)
               	return true;
			else if (rc == 1)
               	getLogger().info("Error: the " + fileName.toString() + " region file doesn't exist.");
			else if (rc == 2)
               	getLogger().info("Error: the " + fileName.toString() + " region file has an invalid file structure (it is not big enough to contain a valid region file header).");
   			else if (rc == 3)
               	getLogger().info("Error: the " + fileName.toString() + " region file has an invalid file structure (file size is not a multiple of 4K).");
       		else if (rc == 4)
               	getLogger().info("Warning: the " + fileName.toString() + " region file contains an empty chunk (which should not be here).");
          	else if (rc == 5)
               	getLogger().info("Error: the " + fileName.toString() + " region file contains a chunk with an invalid chunk length.");
            else if (rc == 6)
               	getLogger().info("Error: the " + fileName.toString() + " region file contains a chunk with an unknown compression type.");
            else if (rc == 7)
               	getLogger().info("Error: there was an IO error while reading the " + fileName.toString() + " region file.");
		}
		catch (IOException e)
		{
			// already handled
		}

		return false;
	}
	
	////////////////////////////////////////////////////////////////////
	private boolean saveRegionFile(Path fileName, RegionFile regionFile)
	////////////////////////////////////////////////////////////////////
	{
		if (regionFile == null)
			return false;
		
		try
		{
			int rc = regionFile.saveRegion(fileName);
			
			if (rc == 0)
               	return true;
			else if (rc == 1)
               	getLogger().info("Error: the " + fileName.toString() + " region file already exist.");
			else if (rc == 2)
               	getLogger().info("Error: there was an IO error while reading the " + fileName.toString() + " region file.");
		}
		catch (IOException e)
		{
			// already handled
		}

		return false;
	}
	
	//////////////////////
    @Override
    public void onEnable()
    //////////////////////
    {
    	// Create plugin directories if non existent
    	File directory = new File ("./plugins/minecraftregionmanager/userdata");
    	if (directory.exists())
    	{
    		// Load players' marks
           	for (Player player : Bukkit.getServer().getOnlinePlayers())
           	{
				try
				{
					loadPlayerMarks(player);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
           	}
    	}
        else
        	directory.mkdirs();

        // Save a copy of the default config.yml if one is not there
        this.saveDefaultConfig();
    	
    	// Load admin reports
    	try
    	{
			loadAdminReports();
		}
    	catch (Exception e)
    	{
			e.printStackTrace();
		}
    	
    	// Register our listener
    	getServer().getPluginManager().registerEvents(this, this);
    }

    ///////////////////////
    @Override
    public void onDisable()
    ///////////////////////
    {
    	// Write players' marks and free memory
    	for (Player player : Bukkit.getServer().getOnlinePlayers())
    	{
    		try
    		{
				savePlayerMarks(player);
			}
    		catch (IOException e)
    		{
				e.printStackTrace();
			}
    		freePlayerMarks(player.getName());
    	}
    	
    	try
    	{
			saveAdminReports();
		}
    	catch (Exception e)
    	{
			e.printStackTrace();
		}
    }

    /////////////////////////////////////////////
    @EventHandler
    public void onWorldSave(WorldSaveEvent event)
    /////////////////////////////////////////////
    {
    	// Hooking on this event provides a way to do things every 5 minutes
    	
    	String w = getUniqueWorldName(event.getWorld());

    	// Only do it when saving the Overworld, not for the Nether, the End or other dimensions
    	if (w.equals("world"))
    	{
    		// Write players' marks
    		for (Player player : Bukkit.getServer().getOnlinePlayers())
    		{
    			try
    			{
					savePlayerMarks(player);
				}
    			catch (IOException e)
    			{
					e.printStackTrace();
				}
    		}
    		
    		// Write reports to the admin
        	try
        	{
    			saveAdminReports();
    		}
        	catch (Exception e)
        	{
    			e.printStackTrace();
    		}
        	
        	// Reload config file in case it has changed
        	this.reloadConfig();
    	}
    }

    ///////////////////////////////////////////////
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    ///////////////////////////////////////////////
    {
    	Player player = event.getPlayer();

   		try
   		{
			loadPlayerMarks(player);
		}
   		catch (Exception e)
   		{
			e.printStackTrace();
		}
    }

    ///////////////////////////////////////////////
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    ///////////////////////////////////////////////
    {
        Player player = event.getPlayer();
        
		try
		{
			savePlayerMarks(player);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		freePlayerMarks(player.getName());
    }
  
    //////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    //////////////////////////////////////////////////////////////////////////////////////////
    {	
        ////////////////////////////////////////////////////////////////////////////////////////////
    	if (cmd.getName().equalsIgnoreCase("showlocation") || cmd.getName().equalsIgnoreCase("hsl"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    			double yaw = location.getYaw();
    			String direction = "";
    	    	
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int by = location.getBlockY();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);
    			int rx = (int) Math.floor((double) cx / 32);
    			int rz = (int) Math.floor((double) cz / 32);
    			    			
    			sender.sendMessage("");
    			sender.sendMessage("World name: " + w);
    			sender.sendMessage("Block coordinates: bx=" + bx + ", bz=" + bz + " (by=" + by + ")");
    			String t = "";
    			if (isChunkMarked(w, cx, cz, sender))
    				t = " (MARKED)";
    			sender.sendMessage("Chunk coordinates: cx=" + cx + ", cz=" + cz + t);
    			t = "";
    			if (isRegionMarked(w, rx, rz, sender))
    				t = " (WHOLLY MARKED)";
    			sender.sendMessage("Region coordinates: rx=" + rx + ", rz=" + rz + t);
    			if ((yaw > -361 && yaw < -337) || (yaw >= -22 && yaw < 23) || (yaw >= 338 && yaw < 361))
    				direction = "South";
    			else if ((yaw >= -337 && yaw < -292) || (yaw >= 23 && yaw < 67))
    				direction = "South-West";
    			else if ((yaw >= -292 && yaw < -247) || (yaw >= 68 && yaw < 112))
    				direction = "West";
    			else if ((yaw >= -247 && yaw < -202) || (yaw >= 113 && yaw < 157))
    				direction = "North-West";
    			else if ((yaw >= -202 && yaw < -157) || (yaw >= 158 && yaw < 202))
    				direction = "North";
    			else if ((yaw >= -157 && yaw < -112) || (yaw >= 203 && yaw < 247))
    				direction = "North-East";
    			else if ((yaw >= -112 && yaw < -67) || (yaw >= 248 && yaw < 292))
    				direction = "East";
    			else if ((yaw >= -67 && yaw < -22) || (yaw >= 293 && yaw < 337))
    				direction = "South-East";
    			sender.sendMessage("Direction: " + direction);
    			sender.sendMessage("Distance to chunk borders: N=" + Math.abs((cz*16)-bz) + ", E=" + Math.abs((cx*16) +15 -bx) + ", S=" + Math.abs((cz*16) + 15 -bz) + ", W=" + Math.abs((cx*16)-bx) + " (in blocks)");
    			sender.sendMessage("Distance to region borders: N=" + Math.abs((rz*16*32)-bz) + ", E=" + Math.abs((rx*16*32) +511 -bx) + ", S=" + Math.abs((rz*16*32) + 511 -bz) + ", W=" + Math.abs((rx*16*32)-bx) + " (in blocks)");
    		}
    		return true;
    	}
    	
        ///////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("markregion") || cmd.getName().equalsIgnoreCase("hmr"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    	    	String n = getMarkName(args);
    			
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);
    			int rx = (int) Math.floor((double) cx / 32);
    			int rz = (int) Math.floor((double) cz / 32);

    			// Get the marked regions list for this player
    	    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(player.getName());

    	        // Add the current region to the list
    			RegionMark mark = new RegionMark(w, rx, rz, n);
    	        Iterator<RegionMark> i = regionMarks.iterator();
    	        while(i.hasNext())
    	        {
    	        	RegionMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == rx && m.getZ() == rz)
    	        	{
    	        		// It's already there, remove it to avoid duplicates
       	        		i.remove();
    	        		break;
    	        	}
    	        }
    	        regionMarks.addLast(mark);    	        

    			// Put it back in the full list
    			playersRegionMarks.put(player.getName(), regionMarks);
    			
    			sender.sendMessage("Marked region: " + mark.printMark());
    		}
    		return true;
    	}
    	
        //////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("markchunk") || cmd.getName().equalsIgnoreCase("hmc"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    	    	String n = getMarkName(args);
    			
    			// Get the chunk coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);

    			// Get the marked chunks list for this player
    	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(player.getName());
    			
    	    	// Add the current chunk to this list
    			ChunkMark mark = new ChunkMark(w, cx, cz, n);
    	        Iterator<ChunkMark> i = chunkMarks.iterator();
    	        while(i.hasNext())
    	        {
    	        	ChunkMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == cx && m.getZ() == cz)
    	        	{
    	        		// It's already there, remove it to avoid duplicates
       	        		i.remove();
    	        		break;
    	        	}
    	        }
    			chunkMarks.addLast(mark);

    			// Put it back in the full list
    			playersChunkMarks.put(player.getName(), chunkMarks);
    			
    			sender.sendMessage("Marked chunk: " + mark.printMark());
    		}
    		return true;
    	}
    	
        /////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("markarea") || cmd.getName().equalsIgnoreCase("hma"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    	    	String n = getMarkName(args);
    			
    			// Get the chunk coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);
    			
    			AreaMark partialAreaMark = playersPartialAreaMark.get(player.getName());
    			if (partialAreaMark == null)
    			{
    				// First call
    				partialAreaMark = new AreaMark(w, cx, cz, 0, 0, n);
    				playersPartialAreaMark.put(player.getName(), partialAreaMark);
        			sender.sendMessage("First corner marked. Now go to the opposite corner, mark it and name it.");     				
    			}
    			else
    			{
    				int areaMarkMaxLength = this.getConfig().getInt("area_mark_max_length");
    				
    				// Second call
        			partialAreaMark.setX2(cx);
        			partialAreaMark.setZ2(cz);
        			
        			// Check if the area name was provided at first, second or both calls
        			if (! n.equals(""))
        				partialAreaMark.setAreaName(n);
        			else
        				n = partialAreaMark.getAreaName();
        			
        			// Check if the marked area second call is in the same dimension than the first one
        			if (! partialAreaMark.getWorldName().equals(w))
            			sender.sendMessage("Sorry, this area is multi dimensional, which is not allowed!");     				
        			// Check if the marked area exceeds allowed height or width
        			else if ((Math.abs(cx - partialAreaMark.getX1() + 1)) > areaMarkMaxLength || (Math.abs(cz - partialAreaMark.getZ1() + 1)) > areaMarkMaxLength)
            			sender.sendMessage("Sorry, this area exceeds the allowed height or width of " + areaMarkMaxLength + " chunks");
        			else
        			{
            			// Get the marked areas list for this player
            	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
            			
            	    	// Add the completed area to this list
            	        Iterator<AreaMark> i = areaMarks.iterator();
            	        while(i.hasNext())
            	        {
            	        	AreaMark m = i.next();
            	        	if (m.getWorldName().equals(w) && m.getX1() == partialAreaMark.getX1() && m.getZ1() == partialAreaMark.getZ1() && m.getX2() == cx && m.getZ2() == cz)
            	        	{
            	        		// It's already there, remove it to avoid duplicates
               	        		i.remove();
            	        		break;
            	        	}
            	        }
            			areaMarks.addLast(partialAreaMark);

            			// Put it back in the full list
            			playersAreaMarks.put(player.getName(), areaMarks);
            			
            			sender.sendMessage("Marked area: " + partialAreaMark.printMark());        				
        			}
        			
        			// Reinitialize the partial area mark for this player
        			playersPartialAreaMark.remove(player.getName());
    			}    			
    		}
    		return true;
    	}
    	
        /////////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("unmarkregion") || cmd.getName().equalsIgnoreCase("hur"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    	    	
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);
    			int rx = (int) Math.floor((double) cx / 32);
    			int rz = (int) Math.floor((double) cz / 32);

    			// Get the marked regions list for this player
    	    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(player.getName());

    	        // Remove the current region from the list, if it's there
    	        Iterator<RegionMark> i = regionMarks.iterator();
    	        while(i.hasNext())
    	        {
    	        	RegionMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == rx && m.getZ() == rz)
    	        	{
    	        		// It's there
    	    			sender.sendMessage("Unmarked region: " + m.printMark());
       	        		i.remove();
    	        		break;
    	        	}
    	        }

    			// Put it back in the full list
    			playersRegionMarks.put(player.getName(), regionMarks);
    		}
    		return true;
    	}
    	
        ////////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("unmarkchunk") || cmd.getName().equalsIgnoreCase("huc"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    	    	
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);

    			// Get the marked chunks list for this player
    	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(player.getName());

    	        // Remove the current chunk from the list, if it's there
    	        Iterator<ChunkMark> i = chunkMarks.iterator();
    	        while(i.hasNext())
    	        {
    	        	ChunkMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == cx && m.getZ() == cz)
    	        	{
    	        		// It's there
    	    			sender.sendMessage("Unmarked chunk: " + m.printMark());
       	        		i.remove();
    	        		break;
    	        	}
    	        }

    			// Put it back in the full list
    			playersChunkMarks.put(player.getName(), chunkMarks);
    		}
    		return true;
    	}
    	
        ///////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("unmarkarea") || cmd.getName().equalsIgnoreCase("hua"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    	    	
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);

    			// Get the marked areas list for this player
    	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
    			
    	    	// Remove all areas containing this chunk from the list
    	        Iterator<AreaMark> i = areaMarks.iterator();
    	        while(i.hasNext())
    	        {
    	        	AreaMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.contains(cx, cz))
    	        	{
    	        		// The current chunk is included in this area
       	    			sender.sendMessage("Unmarked area: " + m.printMark());        				
       	        		i.remove();
    	        	}
    	        }

    			// Put it back in the full list
    			playersAreaMarks.put(player.getName(), areaMarks);
    		}
    		return true;
    	}
    	
        /////////////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("unmarkeverything") || cmd.getName().equalsIgnoreCase("hue"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			emptyPlayerMarks(player.getName());
    		}
    		return true;
    	}
    	
        //////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("listmarks") || cmd.getName().equalsIgnoreCase("hlm"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			boolean firstArea = true;
    			boolean firstChunk = true;
    			boolean firstRegion = true;
   			
    	        sender.sendMessage("");

    	        // Print the marked areas list
    	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
    	        Iterator<AreaMark> k = areaMarks.iterator();
    	        while(k.hasNext())
    	        {
    	        	AreaMark a = k.next();
    	        	if (firstArea == true)
    	        	{
    	    	        sender.sendMessage("Marked areas: ========================================================");
    	        		firstArea = false;
    	        	}
   	    			sender.sendMessage(" " + a.printMark());        				
    	        }

    	    	// Print the marked chunks list
    	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(player.getName());    			
    	        Iterator<ChunkMark> j = chunkMarks.iterator();
    	        while(j.hasNext())
    	        {
    	        	ChunkMark c = j.next();
    	        	if (firstChunk == true)
    	        	{
    	    	        sender.sendMessage("Marked chunks: =======================================================");
    	    	        firstChunk = false;
    	        	}
    	        	sender.sendMessage(" " + c.printMark());
    	        }
    			
    	    	// Print the marked regions list
    	    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(player.getName());
    	        Iterator<RegionMark> i = regionMarks.iterator();
    	        while(i.hasNext())
    	        {
    	        	RegionMark r = i.next();
    	        	if (firstRegion == true)
    	        	{
    	    	        sender.sendMessage("Marked regions: ======================================================");
    	    	        firstRegion = false;
    	        	}
    	        	sender.sendMessage(" " + r.printMark());
    	        }

    	        if (firstArea == true && firstChunk == true && firstRegion == true)
	    	    	sender.sendMessage("No marks yet.");
    		}
    		return true;
    	}
    	
        /////////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("reportregion") || cmd.getName().equalsIgnoreCase("hrr"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();

    			String r = "";
    			if (args.length == 0 || ! (args[0].equalsIgnoreCase("generation") || args[0].equalsIgnoreCase("griefing") || args[0].equalsIgnoreCase("offensive") || args[0].equalsIgnoreCase("corruption")))
    				return false;
    			else
    				r = args[0].toLowerCase();
    			
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);
    			int rx = (int) Math.floor((double) cx / 32);
    			int rz = (int) Math.floor((double) cz / 32);

    	        // Add the current spot to the list
    			boolean found = false;
    	        Iterator<ReportedSpot> i = reportedRegions.iterator();
    	        while(i.hasNext())
    	        {
    	        	ReportedSpot s = i.next();
    	        	if (s.getWorldName().equals(w) && s.getX() == rx && s.getZ() == rz && s.getReportReason().equals(r))
    	        	{
    	        		// This region has already been reported for this reason
    	        		if (s.IsPlayerAmongReporters(player.getUniqueId()) == false)
    	        		{
    	        			// But not by this player. Add him to the reporters
    	        			s.AddReporter(player.getUniqueId());
    	        			sender.sendMessage("Reported region: " + s.printMark());
    	        		}
    	        		else
    	        			sender.sendMessage("You have already reported this region for this reason.");   	        			
       	        		found = true;
    	        		break;
    	        	}
    	        }
    	        if (found == false)
    	        {
        			ReportedSpot spot = new ReportedSpot(w, rx, rz, r, player.getUniqueId());
        	        reportedRegions.addLast(spot);    	        
        			sender.sendMessage("Reported region: " + spot.printMark());
    	        }
    		}
    		return true;
    	}
    	
        ////////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("reportchunk") || cmd.getName().equalsIgnoreCase("hrc"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    			
    			String r = "";
    			if (args.length == 0 || ! (args[0].equalsIgnoreCase("generation") || args[0].equalsIgnoreCase("griefing") || args[0].equalsIgnoreCase("offensive") || args[0].equalsIgnoreCase("corruption")))
    				return false;
    			else
    				r = args[0].toLowerCase();
    			
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);

    	        // Add the current spot to the list 			
    			boolean found = false;
    	        Iterator<ReportedSpot> i = reportedChunks.iterator();
    	        while(i.hasNext())
    	        {
    	        	ReportedSpot s = i.next();
    	        	if (s.getWorldName().equals(w) && s.getX() == cx && s.getZ() == cz && s.getReportReason().equals(r))
    	        	{
    	        		// This Chunk has already been reported for this reason
    	        		if (s.IsPlayerAmongReporters(player.getUniqueId()) == false)
    	        		{
    	        			// But not by this player. Add him to the reporters
    	        			s.AddReporter(player.getUniqueId());
    	        			sender.sendMessage("Reported chunk: " + s.printMark());
    	        		}
    	        		else
    	        			sender.sendMessage("You have already reported this chunk for this reason.");   	        			
       	        		found = true;
    	        		break;
    	        	}
    	        }
    	        if (found == false)
    	        {
        			ReportedSpot spot = new ReportedSpot(w, cx, cz, r, player.getUniqueId());
        	        reportedChunks.addLast(spot);     
        			sender.sendMessage("Reported chunk: " + spot.printMark());
    	        }
    		}
    		return true;
    	}
    	
        ////////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("listreports") || cmd.getName().equalsIgnoreCase("hlr"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else if (! sender.isOp())
    			sender.sendMessage("This command can only be used by an operator.");
    		else
    		{
    			Player player = (Player) sender;
    			boolean firstRegion = true;
    			boolean firstChunk = true;
   			
    	        sender.sendMessage("");

    	        // Print the reported regions list
    	        Iterator<ReportedSpot> i = reportedRegions.iterator();
    	        while(i.hasNext())
    	        {
    	        	ReportedSpot s = i.next();
    	        	if (firstRegion == true)
    	        	{
    	    	        sender.sendMessage("Reported regions: =====================================================");
    	    	        firstRegion = false;
    	        	}
        			player.sendMessage(" " + s.printMark());
    	        }

    	        // Print the reported chunks list
    	        Iterator<ReportedSpot> j = reportedChunks.iterator();
    	        while(j.hasNext())
    	        {
    	        	ReportedSpot s = j.next();
    	        	if (firstChunk == true)
    	        	{
    	    	        sender.sendMessage("Reported chunks: =====================================================");
    	    	        firstChunk = false;
    	        	}
        			player.sendMessage(" " + s.printMark());
    	        }
    		}
    		return true;
    	}
    	
        ////////////////////////////////////////////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("namepoi") || cmd.getName().equalsIgnoreCase("hnp"))
    	{
    		if (!(sender instanceof Player))
    			sender.sendMessage("This command can't be used from the console.");
    		else
    		{
    			sender.sendMessage("Sorry. This command is NOT IMPLEMENTED YET!");
    			// TODO
    		}
    		return true;
    	}
    	
        ////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("resetmap"))
    	{
    		if ((sender instanceof Player))
    			sender.sendMessage("This command can only be used from the console.");
    		else
    		{
    			DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
    			DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
               	symbols.setGroupingSeparator('.');
               	formatter.setDecimalFormatSymbols(symbols);
               	// TODO : limit floating values to 2 positions

               	if (args.length == 0)
    			{
        			try
        			{
        				loadAllMarks();

                       	getLogger().info("BEFORE RESETTING A WORLD, MAKE SURE NO ONE IS CONNECTED.");
                		if (Bukkit.hasWhitelist() == false)
                			getLogger().info("BEWARE! YOUR SERVER IS NOT WHITELISTED.");
                       	getLogger().info("");                       	

                       	getLogger().info("Worlds list:");
        				List<World> worldsList = Bukkit.getWorlds();
            			LinkedList<MarkedRegion> markedRegionsList;
                       	for (int i = 0; i < worldsList.size(); i++)
        				{
        					World world = worldsList.get(i);
        					String uniqueWorldName = getUniqueWorldName(world);
        					
        					getLogger().info("  " + uniqueWorldName);
//        					getLogger().info("  - Name               = " + world.getName());
         					getLogger().info("  - UID                = " + world.getUID().toString());
//         					getLogger().info("  - Folder             = " + world.getWorldFolder().getAbsolutePath());
//         					getLogger().info("  - Type               = " + world.getWorldType().getName());
           					getLogger().info("  - Environment        = " + world.getEnvironment().toString());
           					getLogger().info("  - Logged players     = " + world.getPlayers().size());
           					getLogger().info("  - Loaded chunks      = " + world.getLoadedChunks().length);
//         					getLogger().info("  - Entities           = " + world.getEntities().size());
//                			getLogger().info("  - Living entities    = " + world.getLivingEntities().size());

                   			int numberOfRegions = 0;
                   			long sizeOfRegions = 0L;
                   			File directory = new File(world.getWorldFolder().getAbsolutePath() + "/region");
                   	    	if (directory.exists())
                   			{
                       			String[] filesList = directory.list();
                       			for(int j = 0; j < filesList.length; j++)
                       			{
                       				if (filesList[j].endsWith(".mca"))
                       				{
                       					numberOfRegions++;
                       					File f = new File(world.getWorldFolder().getAbsolutePath() + "/region/" + filesList[j]);
                       					sizeOfRegions += f.length();
                       				}
                       			}                   				
                   			}
                           	getLogger().info("  - Regions            = " + formatter.format(numberOfRegions) + " (" + formatter.format(sizeOfRegions) + " bytes)");
                           	
                   			markedRegionsList = worldsMarkedRegions.get(uniqueWorldName);
                   			int numberOfMarkedRegions = 0;
                			if (markedRegionsList != null)
                				numberOfMarkedRegions = markedRegionsList.size();
                           	getLogger().info("  - Marked regions     = " + formatter.format(numberOfMarkedRegions));

                			if (markedRegionsList != null)
                			{
                       			int numberOfWhollyMarkedRegions = 0;
                       			int numberOfPartiallyMarkedRegions = 0;
                       			int numberOfIsolatedMarkedChunks = 0;
            	    			Iterator<MarkedRegion> k = markedRegionsList.iterator();
                	        	while(k.hasNext())
                	        	{
                	        		MarkedRegion r = k.next();
                	        		if (r.IsWhollyMarked())
                	        			numberOfWhollyMarkedRegions++;
                	        		else
                	        		{
                	        			numberOfPartiallyMarkedRegions++;
                	        			numberOfIsolatedMarkedChunks += r.getNumberOfMarkedChunks();
                	        		}
                	        	}                				
                               	getLogger().info("    - Whole regions    = " + formatter.format(numberOfWhollyMarkedRegions));
                               	getLogger().info("    - Partial regions  = " + formatter.format(numberOfPartiallyMarkedRegions) + " (" + formatter.format(numberOfIsolatedMarkedChunks) + " isolated chunks)");

                    			float percentagePreserved = 0;
                    			if (numberOfRegions != 0)
                    				percentagePreserved = (float) ((float) ((numberOfWhollyMarkedRegions * REGION_WIDTH_IN_CHUNKS * REGION_WIDTH_IN_CHUNKS) + numberOfIsolatedMarkedChunks) * 100) / ((float) numberOfRegions * REGION_WIDTH_IN_CHUNKS * REGION_WIDTH_IN_CHUNKS);
                               	getLogger().info("  In case of map reset, " + percentagePreserved + "% of this world would be preserved.");
                			}
                           	getLogger().info("");
        				}			
    				}
        			catch (IOException e)
        			{
    					e.printStackTrace();
    				}
    			}
    			else if (args.length == 1)
    			{
    				List<World> worldsList = Bukkit.getWorlds();
        			LinkedList<MarkedRegion> markedRegionsList;
        			
        			boolean validWorldUid = false;
    				for (int i = 0; i < worldsList.size(); i++)
    				{
    					World world = worldsList.get(i);
    					if (args[0].equalsIgnoreCase(world.getUID().toString()))
    					{
    						validWorldUid = true;
    						if (world.getPlayers().size() != 0)
    	                       	getLogger().info("Abort: there are logged players in this world.");
    						else
    						{
    							File directory = null;
    	    					String uniqueWorldName = getUniqueWorldName(world);
    	                       	getLogger().info("Resetting map " + uniqueWorldName + ":");
    	    					
    							///////////////////////////////////////////////////////////
    	                       	getLogger().info("  Step 1: Unload and save loaded chunks.");
    	                       	Chunk[] loadedChunks = world.getLoadedChunks();
    	                        int success = 0;
    	                       	for (int j = 0; j < loadedChunks.length; j++)
    	                       	{
    	                       		// Not sure this really works!!! At least not for the protected chunks in the spawn world
    	                       		if (loadedChunks[j].unload(true) == false)
    	    	                       	getLogger().info("    Warning: chunk (" + loadedChunks[j].getX() + ";" + loadedChunks[j].getZ() + ") has failed to unload properly.");
    	                       		else
    	                       			success++;
    	                       	}
    	                       	getLogger().info("  - Unloaded and saved " + success + " chunks out of " + loadedChunks.length + ".");
    	                       	
    	                       	///////////////////////////////////////////////////////////////////
    	                       	getLogger().info("  Step 2: Create backup and pruned subdirectories.");
    	                       	directory = new File(world.getWorldFolder().getPath() + "/region/backup");
    	                    	if (directory.exists())
    	                    	{
	                    			int j = 1;
    	                    		while (true)
    	                    		{
    	                    			File newName = new File(world.getWorldFolder().getPath() + "/region/backup." + j);
    	                    			if (newName.exists())
    	                    				j++;
    	                    			else
    	                    			{
    	                    				if (directory.renameTo(newName) == false)
    	                     					getLogger().info("    Error: unable to rename the existing backup directory");    	                    					
    	                    				break;
    	                    			}
    	                    		}
    	                    	}
    	                        directory.mkdirs();
    	                       	directory = new File(world.getWorldFolder().getPath() + "/region/pruned");
    	                    	if (directory.exists())
    	                    	{
	                    			int j = 1;
    	                    		while (true)
    	                    		{
    	                    			File newName = new File(world.getWorldFolder().getPath() + "/region/pruned." + j);
    	                    			if (newName.exists())
    	                    				j++;
    	                    			else
    	                    			{
    	                    				if (directory.renameTo(newName) == false)
    	                     					getLogger().info("    Error: unable to rename the existing pruned directory");    	                    					
    	                    				break;
    	                    			}
    	                    		}
    	                    	}
    	                        directory.mkdirs();

    	                        ////////////////////////////////////////////////////////////////////////////////////
    	                       	getLogger().info("  Step 3: Move the existing region files in the backup directory.");
                       			directory = new File(world.getWorldFolder().getPath() + "/region");
                       	    	if (directory.exists())
                       			{
                           			String[] filesList = directory.list();
                           			for(int j = 0; j < filesList.length; j++)
                           			{
                           				if (filesList[j].endsWith(".mca"))
                           				{
                           					Path srcFile = Paths.get(world.getWorldFolder().getPath() + "/region/" + filesList[j]);
                           					Path dstFile = Paths.get(world.getWorldFolder().getPath() + "/region/backup/" + filesList[j]);
                	                       	try
                	                       	{
												Files.move(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
											}
                	                       	catch (IOException e1)
                	                       	{
                    	                       	getLogger().info("    Warning: region file " + world.getWorldFolder().getPath() + "/region/" + filesList[j] + " is still loaded in Minecraft. Copying instead of moving.");
												try
												{
													Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
												} catch (IOException e2)
												{
													e2.printStackTrace();
												}
											}
                           				}
                           			}                   				
                       			}
    	                       	
                       	    	//////////////////////////////////////////////////////////////////////
    	                       	getLogger().info("  Step 4: Copy back the wholly marked region files.");
                       			markedRegionsList = worldsMarkedRegions.get(uniqueWorldName);
    	                        int copied = 0;
    	                        int skipped = 0;
    	                        int failed = 0;
                    			if (markedRegionsList != null)
                    			{
                	    			Iterator<MarkedRegion> j = markedRegionsList.iterator();
                    	        	while (j.hasNext())
                    	        	{
                    	        		MarkedRegion r = j.next();
                    	        		if (r.IsWhollyMarked())
                    	        		{
                    	        			String fileName = "r." + r.getX() + "." + r.getZ() + ".mca";
                           					Path srcFile = Paths.get(world.getWorldFolder().getPath() + "/region/backup/" + fileName);
                           					Path dstFile = Paths.get(world.getWorldFolder().getPath() + "/region/" + fileName);
                           					if (Files.exists(dstFile))
                           						skipped++;
                           					else
                           					{
                           						try
												{
													Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
													copied++;
												} catch (IOException e)
												{
													e.printStackTrace();
													failed++;
												}
                           					}
                    	        		}
                    	        	}                				
                    			}
                    			if (copied > 0)
                    				getLogger().info("  - Copied " + formatter.format(copied) + " files.");
                    			if (skipped > 0)
                    				getLogger().info("  - Skipped " + formatter.format(skipped) + " files that were still loaded.");
                    			if (failed > 0)
                    				getLogger().info("  - failed to copy " + formatter.format(failed) + " files. MANUAL ACTION REQUIRED!");
    	                       	
                    			////////////////////////////////////////////////////////////
    	                       	getLogger().info("  Step 5: Prune the partial region files.");
    	                       	int pruned = 0;
    	                       	int deleted = 0;
    	                       	int preserved = 0;
    	                       	int empty = 0;
    	                       	failed = 0;
                    			if (markedRegionsList != null)
                    			{
                	    			Iterator<MarkedRegion> j = markedRegionsList.iterator();
                    	        	while (j.hasNext())
                    	        	{
                    	        		MarkedRegion r = j.next();
                    	        		if (! r.IsWhollyMarked())
                    	        		{
                    	        			String fileName = "r." + r.getX() + "." + r.getZ() + ".mca";
                         					Path srcFile = Paths.get(world.getWorldFolder().getPath() + "/region/backup/" + fileName);
                           					Path dstFile = Paths.get(world.getWorldFolder().getPath() + "/region/pruned/" + fileName);
  
                           					RegionFile f = new RegionFile();
                            				if (loadRegionFile(srcFile, f) == true)
                            				{
                            					for (int x = 0; x < REGION_WIDTH_IN_CHUNKS; x++)
                            						for (int z = 0; z < REGION_WIDTH_IN_CHUNKS; z++)
                            							if (r.IsChunkMarked(x, z) == false)
                            							{
                            								if (f.isChunkEmpty(x, z))
                                								empty++;
                            								else
                            									deleted++;
                            								f.deleteChunk(x, z);
                            							}
                            							else
                            								preserved++;
												if (saveRegionFile(dstFile, f) == true)
													pruned++;
												else
													failed++;
                            				}                           					
                            				else
                           						failed++;
                    	        		}
                    	        	}                				
                    			}
                    			if (pruned > 0)
                    			{
                    				getLogger().info("  - Pruned " + formatter.format(pruned) + " files.");
                       				getLogger().info("    - Preserved " + formatter.format(preserved) + " chunks.");
                       				getLogger().info("    - Deleted " + formatter.format(deleted) + " chunks.");
                       				getLogger().info("    - Left out " + formatter.format(empty) + " empty chunks.");
                    			}
                    			if (failed > 0)
                    				getLogger().info("  - Failed to prune " + formatter.format(failed) + " files. MANUAL ACTION REQUIRED!");

    	                       	///////////////////////////////////////////////////////////////
    	                       	getLogger().info("  Step 6: Copy back the pruned region files.");    	                       	
    	                        copied = 0;
    	                        skipped = 0;
    	                        failed = 0;
                    			if (markedRegionsList != null)
                    			{
                	    			Iterator<MarkedRegion> j = markedRegionsList.iterator();
                    	        	while (j.hasNext())
                    	        	{
                    	        		MarkedRegion r = j.next();
                    	        		if (! r.IsWhollyMarked())
                    	        		{
                    	        			String fileName = "r." + r.getX() + "." + r.getZ() + ".mca";
                         					Path srcFile = Paths.get(world.getWorldFolder().getPath() + "/region/pruned/" + fileName);
                           					Path dstFile = Paths.get(world.getWorldFolder().getPath() + "/region/" + fileName);
                           					if (Files.exists(srcFile))
                           					{
                               					if (Files.exists(dstFile))
                               					{
                        	                       	getLogger().info("    Error: pruned region file " + world.getWorldFolder().getPath() + "/region/pruned/" + fileName + " is still loaded in Minecraft. You'll have to copy it manually.");
                               						skipped++;
                               					}
                               					else
                               					{
                               						try
    												{
    													Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
    													copied++;
    												} catch (IOException e)
    												{
    													e.printStackTrace();
    													failed++;
    												}
                               					}
                           					}
                           					// else silently skip the file. It will habe been reported at step 5
                    	        		}
                    	        	}                				
                    			}
                    			if (copied > 0)
                    				getLogger().info("  - Copied " + formatter.format(copied) + " files.");
                    			if (skipped > 0)
                    				getLogger().info("  - Skipped " + formatter.format(skipped) + " files that were still loaded. MANUAL ACTION REQUIRED!");
                    			if (failed > 0)
                    				getLogger().info("  - failed to copy " + formatter.format(failed) + " files. MANUAL ACTION REQUIRED!");

                    			getLogger().info("Done!");
    						}
        					break;
    					}
    				}
    				if (validWorldUid == false)
                       	getLogger().info("Error: use a valid world UID to select the world to reset.");
    			}
    			else
    				return false;
    		}
    		return true;
    	}
    	   	
        //////////////////////////////////////////////////////
    	else if (cmd.getName().equalsIgnoreCase("verifyregion") || cmd.getName().equalsIgnoreCase("hvr"))
    	{
    		if ((sender instanceof Player))
    			sender.sendMessage("This command can only be used from the console.");
    		else
    		{
    			if (args.length == 1 || args.length == 3)
    			{
					World w = null;
					int rx;
					int rz;
					
					List<World> worldsList = Bukkit.getWorlds();
        			boolean validWorldName = false;
    				for (int i = 0; i < worldsList.size(); i++)
    				{
    					w = worldsList.get(i);
    					if (args[0].equals(getUniqueWorldName(w)))
    					{
    						validWorldName = true;
    						break;
    					}
    				}
    				if (validWorldName == false)
    					return false;
    				
    				File srcDir = new File(w.getWorldFolder().getPath() + "/region");
    				File dstDir = new File(w.getWorldFolder().getPath() + "/region/tmp");
                	if (! dstDir.exists())
                		dstDir.mkdirs(); // it will be kept at the end

                	if (args.length == 3) // verify just the specified file
                	{
                		if (StringUtils.isNumeric(args[1]) && StringUtils.isNumeric(args[2]))
                		{
                			rx = Integer.parseInt(args[1]);
                			rz = Integer.parseInt(args[2]);

                			// we'll work on a temporary copy in order to avoid errors with region files loaded in Minecraft
           					Path srcFile = Paths.get(w.getWorldFolder().getPath() + "/region/r." + rx + "." + rz + ".mca");
           					Path dstFile = Paths.get(w.getWorldFolder().getPath() + "/region/tmp/r." + rx + "." + rz + ".mca");
           					if (Files.exists(srcFile))
           					{
               					try
    							{
    								Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
    							} catch (IOException e)
    							{
    								e.printStackTrace();
    							}
    		   					RegionFile r = new RegionFile();
    		    				if (loadRegionFile(dstFile, r) == true)
    		                       	getLogger().info("The " + srcFile.toString() + " region file is OK.");
    		    				// else the loadRegionFile method will print the relevant error or warning
    		    				try
    		    				{
    								Files.delete(dstFile);
    							}
    		    				catch (IOException e)
    		    				{
    								e.printStackTrace();
    							}
           					}
           					else
           					{
		                       	getLogger().info("The " + srcFile.toString() + " region file does not exist.");
           					}
                		}
                		else
                				return false;
                	}
                	else // verify all region files in this world
                	{
            			DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            			DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
                       	symbols.setGroupingSeparator('.');
                       	formatter.setDecimalFormatSymbols(symbols);
                       	
               			String[] filesList = srcDir.list();
               			int success = 0;
               			int failed = 0;
               			for(int i = 0; i < filesList.length; i++)
               			{
               				if (filesList[i].endsWith(".mca"))
               				{
                    			// we'll work on a temporary copy in order to avoid errors with region files loaded in Minecraft
               					Path srcFile = Paths.get(w.getWorldFolder().getPath() + "/region/" + filesList[i]);
               					Path dstFile = Paths.get(w.getWorldFolder().getPath() + "/region/tmp/" + filesList[i]);
								try
								{
									Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
								} catch (IOException e)
								{
									e.printStackTrace();
								}
			   					RegionFile r = new RegionFile();
			    				if (loadRegionFile(dstFile, r) == false)
			    					failed++; // the loadRegionFile method will print the relevant error or warning
			    				else
			    				{
			    					success++;
			    					if ((success % 1000) == 0)
			               				getLogger().info("Progress report: so far " + formatter.format(success) + " region files verified...");			    						
			    				}
			    				try
			    				{
									Files.delete(dstFile);
								}
			    				catch (IOException e)
			    				{
									e.printStackTrace();
								}
               				}
               			}
               			if (failed > 0)
               				getLogger().info(formatter.format(success) + " region files OK out of " + formatter.format(success + failed));
               			else
               				getLogger().info("All " + formatter.format(success) + " region files are OK");               				
                	}                	
                    return true;
    			}
    			else
    				return false;
    		}
    		return true;
    	}    	
    	return false; 
    }
}
