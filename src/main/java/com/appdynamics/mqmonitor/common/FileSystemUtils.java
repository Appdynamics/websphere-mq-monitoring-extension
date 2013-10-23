/**
 * FileSystemUtils.java
 */

package com.appdynamics.monitors.mqmonitor.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileSystemUtils
{

    // create a directory path - string may contain subdirectories
    static public boolean createDir(String aDir)
    {
 		boolean success = (new File(aDir)).mkdirs();
        return success;
    }


    // delete a directory path - string may contain subdirectories
    // if not empty, directory and subdirectory contents will be deleted
	static public boolean deleteDir(File dir)
	{
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}


	// deletes a file or directory - if directory, it must be empty
	static public boolean deleteFile(String filePathName)
	{
		boolean success = (new File(filePathName)).delete();
		return success;
	}


	// renames a file
	static public boolean renameFile(String oldFileName, String newFileName)
	{
	  	boolean success = false;
	  	try {
	  		File fileToRename = new File(oldFileName);
	  		success = fileToRename.renameTo(new File(newFileName));
	  	} catch (Exception e){
	  	}
	  	return success;
	}

	// compare two files for identical content
	static public boolean compareFileContents (String firstFilePathName, String secondFilePathName, int skipLines)
	{
		boolean equalContent = true;
		int skipLineCount = 0;

		try {
			BufferedReader in1 = new BufferedReader(new FileReader(firstFilePathName));
			BufferedReader in2 = new BufferedReader(new FileReader(secondFilePathName));
			String str1;
			String str2;
			if (skipLines < 0) skipLines = 0; // protect ourselves
			while ((str1 = in1.readLine()) != null) {
				if ((str2 = in2.readLine()) != null) {
					if (skipLines > 0 && skipLineCount < skipLines){
						skipLineCount++;
					} else {
						if (str1.equalsIgnoreCase(str2) == false){
							equalContent = false;
							break;
						}
					}
				}
			}
			in1.close();
			in2.close();
		} catch (IOException e) {
		}
		return equalContent;
	}


}



