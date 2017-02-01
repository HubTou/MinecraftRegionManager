package org.tournier.minecraftregionmanager;

public class MarkedRegion
{
	public static final int REGION_WIDTH_IN_CHUNKS = 32;

	// the world this region belongs to, is not kept inside this class
	// region coordinates:
	private int 		x, z;
	// number of chunks marked (REGION_WIDTH_IN_CHUNKS x REGION_WIDTH_IN_CHUNKS means whole region):
	private int			n;
	// marked chunks map (true means *individually* marked):
	private boolean[][]	chunk = new boolean[REGION_WIDTH_IN_CHUNKS][REGION_WIDTH_IN_CHUNKS];
	
	// Constructor
    public MarkedRegion(int rx, int rz)
    {
        x = rx;
        z = rz;
        n = 0;
        
        for (int i = 0; i < REGION_WIDTH_IN_CHUNKS; i++)
        	for (int j = 0; j < REGION_WIDTH_IN_CHUNKS; j++)
        		chunk[i][j] = false;
    }
    
    public int getX()
    {
    	return x;
    }
    
    public int getZ()
    {
    	return z;
    }

    public int getNumberOfMarkedChunks()
    {
    	return n;
    }

    public boolean IsWhollyMarked()
    {
    	return (n == REGION_WIDTH_IN_CHUNKS * REGION_WIDTH_IN_CHUNKS);
    }

    public boolean IsChunkMarked(int cx, int cz)
    {
    	int mcx = ((cx % REGION_WIDTH_IN_CHUNKS) + REGION_WIDTH_IN_CHUNKS) % REGION_WIDTH_IN_CHUNKS;
    	int mcz = ((cz % REGION_WIDTH_IN_CHUNKS) + REGION_WIDTH_IN_CHUNKS) % REGION_WIDTH_IN_CHUNKS;
    	
    	return chunk[mcx][mcz];
    }
    
    public void MarkAllChunks()
    {
    	n = REGION_WIDTH_IN_CHUNKS * REGION_WIDTH_IN_CHUNKS;
    }
    
    public void MarkChunk(int cx, int cz)
    {
    	int mcx = ((cx % REGION_WIDTH_IN_CHUNKS) + REGION_WIDTH_IN_CHUNKS) % REGION_WIDTH_IN_CHUNKS;
    	int mcz = ((cz % REGION_WIDTH_IN_CHUNKS) + REGION_WIDTH_IN_CHUNKS) % REGION_WIDTH_IN_CHUNKS;
    	
    	if (chunk[mcx][mcz] == false && n < REGION_WIDTH_IN_CHUNKS * REGION_WIDTH_IN_CHUNKS)
    		n++;
    	chunk[mcx][mcz] = true;    	
    }
}
