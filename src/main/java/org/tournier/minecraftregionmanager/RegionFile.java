package org.tournier.minecraftregionmanager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

// The objects of this class represent in-memory region files with compressed chunks
// For the purpose of deleting/replacing chunks it is not necessary (and quicker) to keep chunks compressed
// For the purpose of copying/replacing region files, these objects are not directly associated with files
public class RegionFile
{
	public static final int REGION_WIDTH_IN_CHUNKS = 32;
	public static final int CHUNKS_PER_REGION = REGION_WIDTH_IN_CHUNKS * REGION_WIDTH_IN_CHUNKS;
	public static final int INT_SIZE = 4;
	public static final int UNDEFINED_COMPRESSION_TYPE = 0;
	public static final int COMPRESSION_TYPE_GZIP = 1;
	public static final int COMPRESSION_TYPE_ZLIB = 2;
	
	private int[]		offset = new int[CHUNKS_PER_REGION];
	private int[]		sectors = new int[CHUNKS_PER_REGION];
	private int[]		timestamp = new int[CHUNKS_PER_REGION];
	private int[]		length = new int[CHUNKS_PER_REGION];
	private byte[]		compressionType = new byte[CHUNKS_PER_REGION];
	private byte[][]	compressedData = new byte[CHUNKS_PER_REGION][];
	private int[]		uncompressedLength = new int[CHUNKS_PER_REGION];
	private byte[][]	uncompressedData = new byte[CHUNKS_PER_REGION][];
	
	///////////////////
    public RegionFile()
   	///////////////////
    {
    	for (int i = 0; i < CHUNKS_PER_REGION; i++)
    	{
    		offset[i] = 0;
    		sectors[i] = 0;
    		timestamp[i] = 0;
    		length[i] = 0;
    		compressionType[i] = UNDEFINED_COMPRESSION_TYPE;
    		compressedData[i] = null;
    		uncompressedLength[i] = 0;
    		uncompressedData[i] = null;
    	}
    }

	///////////////////////////////////////////////////////
    public int loadRegion(Path fileName) throws IOException
	///////////////////////////////////////////////////////
    {
        try
        {
        	File fileObject = new File(fileName.toString());
        	if (! fileObject.exists())
        		return 1; // file not found
        	
            RandomAccessFile file = new RandomAccessFile(fileObject, "rw");
            
            if (file.length() < 2 * INT_SIZE * CHUNKS_PER_REGION)
            {
                file.close();           
            	return 2; // Invalid file structure (file is not big enough to contain a valid region file header)
            }
            if ((file.length() & 0xfff) != 0)
            {
                file.close();           
            	return 3; // Invalid file structure (file size is not a multiple of 4K)
            }
            
            // Load the header
            for (int i = 0; i < CHUNKS_PER_REGION; i++)
            {
            	int j = file.readInt();
            	offset[i] = (j & 0xFFFFFF00) >> 8;
            	sectors[i] = (j & 0x000000FF);
            }
            for (int i = 0; i < CHUNKS_PER_REGION; i++)
            	timestamp[i] = file.readInt();

            // Load the chunks
            for (int i = 0; i < CHUNKS_PER_REGION; i++)
            {
            	if (offset[i] != 0)
            	{
            		// Move to the starting position of the chunk
            		file.seek(offset[i] * INT_SIZE * CHUNKS_PER_REGION);
            		
            		// Load the chunk header
            		length[i] = file.readInt();
            		compressionType[i] = file.readByte();
            		
            		// Verify the chunk header
            		if (length[i] == 0)
            		{
                        file.close();           
            			return 4; // Empty chunk
            		}
            		if (length[i] > sectors[i] * INT_SIZE * CHUNKS_PER_REGION)
            		{
                        file.close();
            			return 5; // Invalid chunk length
            		}
            		if (! (compressionType[i] == COMPRESSION_TYPE_GZIP || compressionType[i] == COMPRESSION_TYPE_ZLIB))
            		{
                        file.close();           
            			return 6; // Unknown compression type
            		}
            	
            		// Load the chunk compressed data
            		compressedData[i] = new byte[length[i] - 1];
            		file.read(compressedData[i]);
            	}
            }

            file.close();           
            return 0; // OK
        }
        catch (IOException e2)
        {
			e2.printStackTrace();
	        return 7; // IO error
        }
    }
    
	////////////////////////////////////////////////////////
    public int saveRegion(Path fileName) throws IOException
   	////////////////////////////////////////////////////////
    {
        try
        {
        	File fileObject = new File(fileName.toString());
        	if (fileObject.exists())
        		return 1; // file already exist we won't try to overwrite it
        	
            RandomAccessFile file = new RandomAccessFile(fileObject, "rw");

            int newOffset = 2; // 2 * INT_SIZE * CHUNKS_PER_REGION
            
            // Write all chunks
            for (int i = 0; i < CHUNKS_PER_REGION; i++)
            {
        		// Write an entry in the region file header
        		file.seek(i * INT_SIZE);
        		int h = 0;
        		if (sectors[i] > 0)
        			h = (newOffset << 8) + sectors[i];
    			file.writeInt(h);
        		
    			// Write an entry in the region file timestamps table
        		file.seek((INT_SIZE * CHUNKS_PER_REGION) + (i * INT_SIZE));
    			file.writeInt(timestamp[i]);

        		if (sectors[i] > 0)
            	{
            		// Write the chunk
            		file.seek(newOffset * INT_SIZE * CHUNKS_PER_REGION);
        			file.writeInt(length[i]);
        			file.writeByte(compressionType[i]);
        			file.write(compressedData[i]);

        			// Pad with 0 if the chunk header+data is not a 4K multiple
        			int m = (INT_SIZE + length[i]) % (INT_SIZE * CHUNKS_PER_REGION);
        			if (m != 0)
        				for (int j = 0; j < ((INT_SIZE * CHUNKS_PER_REGION) - m); j++)
        					file.writeByte(0);
        			
        			newOffset += sectors[i];
            	}
            }

            file.close();           
            return 0; // OK
        }
        catch (IOException e)
        {
			e.printStackTrace();
	        return 2; // IO error
        }
    }

   	//////////////////////////////////////////////////
    private int getIndexFromCoordinates(int cx, int cz)
   	//////////////////////////////////////////////////
    {
    	int mcx = ((cx % REGION_WIDTH_IN_CHUNKS) + REGION_WIDTH_IN_CHUNKS) % REGION_WIDTH_IN_CHUNKS;
    	int mcz = ((cz % REGION_WIDTH_IN_CHUNKS) + REGION_WIDTH_IN_CHUNKS) % REGION_WIDTH_IN_CHUNKS;

    	return mcx + mcz * REGION_WIDTH_IN_CHUNKS;
    }
    
   	///////////////////////////////////////////
    public boolean isChunkEmpty(int cx, int cz)
   	///////////////////////////////////////////
    {
    	int i = getIndexFromCoordinates(cx, cz);
    	
    	if (length[i] > 0)
    		return false;
    	else
    		return true;
    }

   	///////////////////////////////////////
    public void deleteChunk(int cx, int cz)
   	///////////////////////////////////////
    {
    	int i = getIndexFromCoordinates(cx, cz);
    	
		offset[i] = 0;
		sectors[i] = 0;
		timestamp[i] = 0;
		length[i] = 0;
		compressionType[i] = UNDEFINED_COMPRESSION_TYPE;
		compressedData[i] = null;
		uncompressedLength[i] = 0;
		uncompressedData[i] = null;
    }
}

// TODO provide decompression method 
// TODO provide test decompression method
// TODO provide test NBT structure method
// TODO provide test blocks/items ID method