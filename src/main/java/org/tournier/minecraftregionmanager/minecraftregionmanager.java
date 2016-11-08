package org.tournier.minecraftregionmanager;

// TODO free reported spots lists

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
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

public final class minecraftregionmanager extends JavaPlugin implements Listener {
	
	private static final int AREA_MARK_MAX_LENGTH = 64;
	
	private HashMap<String, AreaMark> playersPartialAreaMark = new HashMap<String, AreaMark>();
	private HashMap<String, LinkedList<AreaMark>> playersAreaMarks = new HashMap<String, LinkedList<AreaMark>>();
	private HashMap<String, LinkedList<ChunkMark>> playersChunkMarks = new HashMap<String, LinkedList<ChunkMark>>();
	private HashMap<String, LinkedList<RegionMark>> playersRegionMarks = new HashMap<String, LinkedList<RegionMark>>();
	private LinkedList<ReportedSpot> reportedRegions = new LinkedList<ReportedSpot>();
	private LinkedList<ReportedSpot> reportedChunks = new LinkedList<ReportedSpot>();	

	///////////////////////////////////////////////////////////////////////////////////////
	private void loadPlayerMarks(Player player) throws FileNotFoundException, IOException {
	///////////////////////////////////////////////////////////////////////////////////////
		final int NOWHERE = 0;
		final int IN_AREA_MARKS = 1;
		final int IN_CHUNK_MARKS = 2;
		final int IN_REGION_MARKS = 3;
		
    	LinkedList<AreaMark> areaMarks = new LinkedList<AreaMark>();
    	LinkedList<ChunkMark> chunkMarks = new LinkedList<ChunkMark>();
    	LinkedList<RegionMark> regionMarks = new LinkedList<RegionMark>();

        try {
            int status = NOWHERE;
            Pattern areaPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):([-0-9]*),([-0-9]*):(.*)");
            Pattern chunkPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):(.*)");
            Pattern regionPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):(.*)");
            BufferedReader buffer = new BufferedReader(new FileReader("./plugins/minecraftregionmanager/userdata/" + player.getUniqueId() + ".txt"));
            String line;
            while (null != (line = buffer.readLine())) {
            	if (line.startsWith("Areas:")) {
            		status = IN_AREA_MARKS;
            	} else if (line.startsWith("Chunks:")) {
            		status = IN_CHUNK_MARKS;
            	} else if (line.startsWith("Regions:")) {
            		status = IN_REGION_MARKS;
            	} else if (line.startsWith("  ")) {
            		Matcher m;
            		switch (status) {
            			case IN_AREA_MARKS:
            				m = areaPattern.matcher(line);
                    		if (m.matches()) {
                    			AreaMark mark = new AreaMark(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), m.group(6));
                    	        areaMarks.addLast(mark);    	        
                            }
            				break;
            			case IN_CHUNK_MARKS: ;
        					m = chunkPattern.matcher(line);
        	        		if (m.matches()) {
                    			ChunkMark mark = new ChunkMark(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4));
                    	        chunkMarks.addLast(mark);    	        
        	                }
        					break;
            			case IN_REGION_MARKS: ;
            				m = regionPattern.matcher(line);
                    		if (m.matches()) {
                    			RegionMark mark = new RegionMark(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4));
                    	        regionMarks.addLast(mark);    	        
                            }
        					break;
            		}
            	}
            }
            buffer.close();

        } catch (FileNotFoundException e1) {
        	// The player's list of marks has to be initialized
        	playersAreaMarks.put(player.getName(), areaMarks);
        	playersChunkMarks.put(player.getName(), chunkMarks);
        	playersRegionMarks.put(player.getName(), regionMarks);

        } catch (IOException e2) {
			e2.printStackTrace();
        }
        
    	playersAreaMarks.put(player.getName(), areaMarks);
    	playersChunkMarks.put(player.getName(), chunkMarks);
    	playersRegionMarks.put(player.getName(), regionMarks);
	}

	////////////////////////////////////////////////////////////////
	private void savePlayerMarks(Player player) throws IOException {
	////////////////////////////////////////////////////////////////
		FileWriter file = null;

		try {
			boolean firstArea = true;
			boolean firstChunk = true;
			boolean firstRegion = true;
			
			file = new FileWriter("./plugins/minecraftregionmanager/userdata/" + player.getUniqueId() + ".txt");

	    	// Write the marked areas list
	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
	        Iterator<AreaMark> i = areaMarks.iterator();
	        while(i.hasNext()){
	        	AreaMark a = i.next();
	        	if (firstArea == true) {
	    			file.write("Areas:" + System.getProperty("line.separator"));
	        		firstArea = false;
	        	}
	    		file.write("  " + a.getWorldName() + ":" + a.getX1() + "," + a.getZ1() + ":" + a.getX2() + "," + a.getZ2() + ":" + a.getAreaName() + System.getProperty("line.separator"));        				
	        }

	    	// Write the marked chunks list
	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(player.getName());    			
	        Iterator<ChunkMark> j = chunkMarks.iterator();
	        while(j.hasNext()){
	        	ChunkMark c = j.next();
	        	if (firstChunk == true) {
	    			file.write("Chunks:" + System.getProperty("line.separator"));
	    	        firstChunk = false;
	        	}
	        	file.write("  " + c.getWorldName() + ":" + c.getX() + "," + c.getZ() + ":" + c.getChunkName() + System.getProperty("line.separator"));
	        }
			
	    	// Write the marked regions list
	    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(player.getName());
	        Iterator<RegionMark> k = regionMarks.iterator();
	        while(k.hasNext()){
	        	RegionMark r = k.next();
	        	if (firstRegion == true) {
	    			file.write("Regions:" + System.getProperty("line.separator"));
	    			firstRegion = false;
	        	}
	        	file.write("  " + r.getWorldName() + ":" + r.getX() + "," + r.getZ() + ":" + r.getRegionName() + System.getProperty("line.separator"));
	        }

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			if (file != null) {
				file.close();
			}
		}		
	}
	
	/////////////////////////////////////////////////
	private void freePlayerMarks(String playerName) {
	/////////////////////////////////////////////////
        // Perhaps overdoing it!
        
		// Get the marked regions list for this player
    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(playerName);
    	// Empty it
    	regionMarks.clear();    	        
		// Put it back in the full list
		playersRegionMarks.put(playerName, regionMarks);
		// Remove the hash entry for this player
    	playersRegionMarks.remove(playerName);

		// Get the marked chunks list for this player
    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(playerName);
    	// Empty it
    	chunkMarks.clear();    	        
		// Put it back in the full list
		playersChunkMarks.put(playerName, chunkMarks);
		// Remove the hash entry for this player
        playersChunkMarks.remove(playerName);

		// Get the marked areas list for this player
    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(playerName);
    	// Empty it
    	areaMarks.clear();    	        
		// Put it back in the full list
		playersAreaMarks.put(playerName, areaMarks);
		// Remove the hash entry for this player
        playersAreaMarks.remove(playerName);
	}

	///////////////////////////////////////////////////////////////////////////////////////
	private void loadAdminReports() throws FileNotFoundException, IOException {
	///////////////////////////////////////////////////////////////////////////////////////
		final int NOWHERE = 0;
		final int IN_REPORTED_REGIONS = 1;
		final int IN_REPORTED_CHUNKS = 2;
		
        try {
            int status = NOWHERE;
            Pattern spotPattern = Pattern.compile("^  ([^:]*):([-0-9]*),([-0-9]*):([^:]*):(.*)");
            BufferedReader buffer = new BufferedReader(new FileReader("./plugins/minecraftregionmanager/reports.txt"));
            String line;
            while (null != (line = buffer.readLine())) {
            	if (line.startsWith("Reported regions:")) {
            		status = IN_REPORTED_REGIONS;
            	} else if (line.startsWith("Reported chunks:")) {
            		status = IN_REPORTED_CHUNKS;
            	} else if (line.startsWith("  ")) {
            		Matcher m;
            		String[] r;
            		switch (status) {
            			case IN_REPORTED_REGIONS: ;
        					m = spotPattern.matcher(line);
        	        		if (m.matches()) {
        	            		r = m.group(5).split(":", -1);
        	        			ReportedSpot s = new ReportedSpot(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4), UUID.fromString(r[0]));
                    			for (int i = 1; i < r.length; i++) {
                    				s.AddReporter(UUID.fromString(r[i]));
                    			}
                    			reportedRegions.addLast(s);
        	                }
        					break;
            			case IN_REPORTED_CHUNKS: ;
            				m = spotPattern.matcher(line);
            				if (m.matches()) {
        	            		r = m.group(5).split(":", -1);
            					ReportedSpot s = new ReportedSpot(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(4), UUID.fromString(r[0]));
            					for (int i = 1; i < r.length; i++) {
            						s.AddReporter(UUID.fromString(r[i]));
            					}
            					reportedChunks.addLast(s);
            				}
        					break;
            		}
            	}
            }
            buffer.close();

        } catch (FileNotFoundException e1) {
        	// Nothing to do, it's OK
        	
        } catch (IOException e2) {
			e2.printStackTrace();
        }
	}
	
	///////////////////////////////////////////////////////////////////////////////////////
	private void saveAdminReports() throws FileNotFoundException, IOException {
	///////////////////////////////////////////////////////////////////////////////////////
		boolean firstRegion = true;
		boolean firstChunk = true;

		FileWriter file = null;

		try {
			file = new FileWriter("./plugins/minecraftregionmanager/reports.txt");

	    	// Write the reported regions list
	        Iterator<ReportedSpot> i = reportedRegions.iterator();
	        while (i.hasNext()) {
	        	ReportedSpot s = i.next();
	        	if (firstRegion == true) {
	    			file.write("Reported regions:" + System.getProperty("line.separator"));
	        		firstRegion = false;
	        	}
	    		file.write("  " + s.getWorldName() + ":" + s.getX() + "," + s.getZ() + ":" + s.getReportReason());
	    		LinkedList<UUID> players = s.getReportingPlayers();
	    		Iterator<UUID> k = players.iterator();
	    		while (k.hasNext()) {
	    			UUID id = k.next();
	    			file.write(":" + id);
	    		}
	    		file.write(System.getProperty("line.separator"));    				
	        }

	    	// Write the reported chunks list
	        Iterator<ReportedSpot> j = reportedChunks.iterator();
	        while (j.hasNext()) {
	        	ReportedSpot s = j.next();
	        	if (firstChunk == true) {
	    			file.write("Reported chunks:" + System.getProperty("line.separator"));
	        		firstChunk = false;
	        	}
	    		file.write("  " + s.getWorldName() + ":" + s.getX() + "," + s.getZ() + ":" + s.getReportReason());
	    		LinkedList<UUID> players = s.getReportingPlayers();
	    		Iterator<UUID> k = players.iterator();
	    		while (k.hasNext()) {
	    			UUID id = k.next();
	    			file.write(":" + id);
	    		}
	    		file.write(System.getProperty("line.separator"));    				
	        }
	        
		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			if (file != null) {
				file.close();
			}
		}
	}
	
	////////////////////////////////////////////
	private String getUniqueWorldName(World w) {
	////////////////////////////////////////////
		// Build a truly unique world name
		// With the LOTR mod, there are 3 world named "world":
		//   'world'/Overworld
		//   'world'/Utumno
		//   'world'/MiddleEarth
		String n = w.getWorldFolder().toString();
		n = n.replaceFirst("^..", "");
    	n = n.replaceAll("\\\\", "/");
    	
    	return n;
	}

	///////////////////////////////////////////
	private String getMarkName(String[] args) {
	///////////////////////////////////////////
		// Build the name of a mark from a Minecraft command args
		// and remove forbidden characters
    	String n = "";
		for (int i = 0; i < args.length; i++) {
			n += args[i] + " ";
		}
    	n = n.replaceAll(":", " ");
		n = n.trim();
		
		return n;
	}
	
	////////////////////////
    @Override
    public void onEnable() {
    ////////////////////////
    	// Create plugin directories if non existent
    	File directory = new File ("./plugins/minecraftregionmanager/userdata");
    	if (directory.exists()) {
    		// Load players' marks
           	for (Player player : Bukkit.getServer().getOnlinePlayers()) {
				try {
					loadPlayerMarks(player);
				} catch (Exception e) {
					e.printStackTrace();
				}
           	}
        } else {
        	directory.mkdirs();
    	}
    	
    	// Load admin reports
    	try {
			loadAdminReports();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	// Register our listener
    	getServer().getPluginManager().registerEvents(this, this);
    }

    /////////////////////////
    @Override
    public void onDisable() {
    /////////////////////////
    	// Write players' marks and free memory
    	for (Player player : Bukkit.getServer().getOnlinePlayers())
    	{
    		try {
				savePlayerMarks(player);
			} catch (IOException e) {
				e.printStackTrace();
			}
    		freePlayerMarks(player.getName());
    	}
    	
    	try {
			saveAdminReports();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    ///////////////////////////////////////////////
    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
    ///////////////////////////////////////////////
    	// Hooking on this event provides a way to save marks to disk every 5 minutes
        // TODO: would be better to do it asynchronously
    	String w = getUniqueWorldName(event.getWorld());

    	// Only do it when saving the Overworld, not for the Nether, the End or other dimensions
    	if (w.equals("world")) {
    		// Write players' marks
    		for (Player player : Bukkit.getServer().getOnlinePlayers())
    		{
    			try {
					savePlayerMarks(player);
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    		        	
        	try {
    			saveAdminReports();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }

    /////////////////////////////////////////////////
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    /////////////////////////////////////////////////
    	Player player = event.getPlayer();

   		try {
			loadPlayerMarks(player);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    /////////////////////////////////////////////////
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
    /////////////////////////////////////////////////
        Player player = event.getPlayer();
        
		try {
			savePlayerMarks(player);
		} catch (IOException e) {
			e.printStackTrace();
		}
		freePlayerMarks(player.getName());
    }
  
    //////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    //////////////////////////////////////////////////////////////////////////////////////////
    	
    	if (cmd.getName().equalsIgnoreCase("showlocation") || cmd.getName().equalsIgnoreCase("hsl")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    			sender.sendMessage("Chunk coordinates: cx=" + cx + ", cz=" + cz);
    			sender.sendMessage("Region coordinates: rx=" + rx + ", rz=" + rz);
    			if ((yaw > -361 && yaw < -337) || (yaw >= -22 && yaw < 23) || (yaw >= 338 && yaw < 361)) {
    				direction = "South";
    			} else if ((yaw >= -337 && yaw < -292) || (yaw >= 23 && yaw < 67)) {
    				direction = "South-West";
    			} else if ((yaw >= -292 && yaw < -247) || (yaw >= 68 && yaw < 112)) {
    				direction = "West";
    			} else if ((yaw >= -247 && yaw < -202) || (yaw >= 113 && yaw < 157)) {
    				direction = "North-West";
    			} else if ((yaw >= -202 && yaw < -157) || (yaw >= 158 && yaw < 202)) {
    				direction = "North";
    			} else if ((yaw >= -157 && yaw < -112) || (yaw >= 203 && yaw < 247)) {
    				direction = "North-East";
    			} else if ((yaw >= -112 && yaw < -67) || (yaw >= 248 && yaw < 292)) {
    				direction = "East";
    			} else if ((yaw >= -67 && yaw < -22) || (yaw >= 293 && yaw < 337)) {
    				direction = "South-East";
    			}
    			sender.sendMessage("Direction: " + direction);
    			sender.sendMessage("Distance to chunk borders: N=" + Math.abs((cz*16)-bz) + ", E=" + Math.abs((cx*16) +15 -bx) + ", S=" + Math.abs((cz*16) + 15 -bz) + ", W=" + Math.abs((cx*16)-bx) + " (in blocks)");
    			sender.sendMessage("Distance to region borders: N=" + Math.abs((rz*16*32)-bz) + ", E=" + Math.abs((rx*16*32) +511 -bx) + ", S=" + Math.abs((rz*16*32) + 511 -bz) + ", W=" + Math.abs((rx*16*32)-bx) + " (in blocks)");
    		}
    		return true;
    	}
    	
    	else if (cmd.getName().equalsIgnoreCase("markregion") || cmd.getName().equalsIgnoreCase("hmr")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    	        while(i.hasNext()){
    	        	RegionMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == rx && m.getZ() == rz) {
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
    	
    	else if (cmd.getName().equalsIgnoreCase("markchunk") || cmd.getName().equalsIgnoreCase("hmc")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    	        while(i.hasNext()){
    	        	ChunkMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == cx && m.getZ() == cz) {
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
    	
    	else if (cmd.getName().equalsIgnoreCase("markarea") || cmd.getName().equalsIgnoreCase("hma")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    			if (partialAreaMark == null) {
    				// First call
    				partialAreaMark = new AreaMark(w, cx, cz, 0, 0, n);
    				playersPartialAreaMark.put(player.getName(), partialAreaMark);
    			} else {
    				// Second call
        			partialAreaMark.setX2(cx);
        			partialAreaMark.setZ2(cz);
        			
        			// Check if the area name was provided at first, second or both calls
        			if (! n.equals("")) {
        				partialAreaMark.setAreaName(n);
        			} else {
        				n = partialAreaMark.getAreaName();
        			}
        			
        			// Check if the marked area second call is in the same dimension than the first one
        			if (! partialAreaMark.getWorldName().equals(w)) {
            			sender.sendMessage("Sorry, this area is multi dimensional, which is not allowed!");     				
        			}
        			// Check if the marked area exceeds allowed height or width
        			else if ((Math.abs(cx - partialAreaMark.getX1() + 1)) > AREA_MARK_MAX_LENGTH || (Math.abs(cz - partialAreaMark.getZ1() + 1)) > AREA_MARK_MAX_LENGTH) {
            			sender.sendMessage("Sorry, this area exceeds the allowed height or width of " + AREA_MARK_MAX_LENGTH + " chunks");
        			} else {
            			// Get the marked areas list for this player
            	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
            			
            	    	// Add the completed area to this list
            	        Iterator<AreaMark> i = areaMarks.iterator();
            	        while(i.hasNext()){
            	        	AreaMark m = i.next();
            	        	if (m.getWorldName().equals(w) && m.getX1() == partialAreaMark.getX1() && m.getZ1() == partialAreaMark.getZ1() && m.getX2() == cx && m.getZ2() == cz) {
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
    	
    	else if (cmd.getName().equalsIgnoreCase("unmarkregion") || cmd.getName().equalsIgnoreCase("hur")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    	        while(i.hasNext()){
    	        	RegionMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == rx && m.getZ() == rz) {
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
    	
    	else if (cmd.getName().equalsIgnoreCase("unmarkchunk") || cmd.getName().equalsIgnoreCase("huc")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    	        while(i.hasNext()){
    	        	ChunkMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.getX() == cx && m.getZ() == cz) {
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
    	
    	else if (cmd.getName().equalsIgnoreCase("unmarkarea") || cmd.getName().equalsIgnoreCase("hua")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
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
    	        while(i.hasNext()){
    	        	AreaMark m = i.next();
    	        	if (m.getWorldName().equals(w) && m.contains(cx, cz)) {
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
    	
    	else if (cmd.getName().equalsIgnoreCase("listmarks") || cmd.getName().equalsIgnoreCase("hlm")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
    			Player player = (Player) sender;
    			boolean firstArea = true;
    			boolean firstChunk = true;
    			boolean firstRegion = true;
   			
    	        sender.sendMessage("");

    	        // Print the marked areas list
    	    	LinkedList<AreaMark> areaMarks = playersAreaMarks.get(player.getName());
    	        Iterator<AreaMark> k = areaMarks.iterator();
    	        while(k.hasNext()){
    	        	AreaMark a = k.next();
    	        	if (firstArea == true) {
    	    	        sender.sendMessage("Marked areas: ========================================================");
    	        		firstArea = false;
    	        	}
   	    			sender.sendMessage(" " + a.printMark());        				
    	        }

    	    	// Print the marked chunks list
    	    	LinkedList<ChunkMark> chunkMarks = playersChunkMarks.get(player.getName());    			
    	        Iterator<ChunkMark> j = chunkMarks.iterator();
    	        while(j.hasNext()){
    	        	ChunkMark c = j.next();
    	        	if (firstChunk == true) {
    	    	        sender.sendMessage("Marked chunks: =======================================================");
    	    	        firstChunk = false;
    	        	}
    	        	sender.sendMessage(" " + c.printMark());
    	        }
    			
    	    	// Print the marked regions list
    	    	LinkedList<RegionMark> regionMarks = playersRegionMarks.get(player.getName());
    	        Iterator<RegionMark> i = regionMarks.iterator();
    	        while(i.hasNext()){
    	        	RegionMark r = i.next();
    	        	if (firstRegion == true) {
    	    	        sender.sendMessage("Marked regions: ======================================================");
    	    	        firstRegion = false;
    	        	}
    	        	sender.sendMessage(" " + r.printMark());
    	        }

    	        if (firstArea == true && firstChunk == true && firstRegion == true) {
	    	    	sender.sendMessage("No marks yet.");
    	        }
    		}
    		return true;
    	}
    	
    	else if (cmd.getName().equalsIgnoreCase("reportregion") || cmd.getName().equalsIgnoreCase("hrr")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
    			Player player = (Player) sender;
    			Location location = player.getLocation();

    			String r = "";
    			if (args.length == 0 || ! (args[0].equalsIgnoreCase("generation") || args[0].equalsIgnoreCase("griefing") || args[0].equalsIgnoreCase("offensive") || args[0].equalsIgnoreCase("corruption"))) {
    				return false;
    			} else {
    				r = args[0].toLowerCase();
    			}
    			
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
    	        while(i.hasNext()){
    	        	ReportedSpot s = i.next();
    	        	if (s.getWorldName().equals(w) && s.getX() == rx && s.getZ() == rz && s.getReportReason().equals(r)) {
    	        		// This region has already been reported for this reason
    	        		if (s.IsPlayerAmongReporters(player.getUniqueId()) == false) {
    	        			// But not by this player. Add him to the reporters
    	        			s.AddReporter(player.getUniqueId());
    	        			sender.sendMessage("Reported region: " + s.printMark());
    	        		} else {
    	        			sender.sendMessage("You have already reported this region for this reason.");   	        			
    	        		}
       	        		found = true;
    	        		break;
    	        	}
    	        }
    	        if (found == false) {
        			ReportedSpot spot = new ReportedSpot(w, rx, rz, r, player.getUniqueId());
        	        reportedRegions.addLast(spot);    	        
        			sender.sendMessage("Reported region: " + spot.printMark());
    	        }
    		}
    		return true;
    	}
    	
    	else if (cmd.getName().equalsIgnoreCase("reportchunk") || cmd.getName().equalsIgnoreCase("hrc")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
    			Player player = (Player) sender;
    			Location location = player.getLocation();
    			
    			String r = "";
    			if (args.length == 0 || ! (args[0].equalsIgnoreCase("generation") || args[0].equalsIgnoreCase("griefing") || args[0].equalsIgnoreCase("offensive") || args[0].equalsIgnoreCase("corruption"))) {
    				return false;
    			} else {
    				r = args[0].toLowerCase();
    			}
    			
    			// Get the region coordinates
    			String w = getUniqueWorldName(location.getWorld());
    			int bx = location.getBlockX();
    			int bz = location.getBlockZ();
    			int cx = (int) Math.floor((double) bx / 16);
    			int cz = (int) Math.floor((double) bz / 16);

    	        // Add the current spot to the list 			
    			boolean found = false;
    	        Iterator<ReportedSpot> i = reportedChunks.iterator();
    	        while(i.hasNext()){
    	        	ReportedSpot s = i.next();
    	        	if (s.getWorldName().equals(w) && s.getX() == cx && s.getZ() == cz && s.getReportReason().equals(r)) {
    	        		// This Chunk has already been reported for this reason
    	        		if (s.IsPlayerAmongReporters(player.getUniqueId()) == false) {
    	        			// But not by this player. Add him to the reporters
    	        			s.AddReporter(player.getUniqueId());
    	        			sender.sendMessage("Reported chunk: " + s.printMark());
    	        		} else {
    	        			sender.sendMessage("You have already reported this chunk for this reason.");   	        			
    	        		}
       	        		found = true;
    	        		break;
    	        	}
    	        }
    	        if (found == false) {
        			ReportedSpot spot = new ReportedSpot(w, cx, cz, r, player.getUniqueId());
        	        reportedChunks.addLast(spot);     
        			sender.sendMessage("Reported chunk: " + spot.printMark());
    	        }
    		}
    		return true;
    	}
    	
    	else if (cmd.getName().equalsIgnoreCase("namepoi") || cmd.getName().equalsIgnoreCase("hnp")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
    			Player player = (Player) sender;
    			sender.sendMessage("Sorry. This command is not implemented yet. Should be soon!");
    			// do something
    		}
    		return true;
    	}
    	
    	return false; 
    }
}
