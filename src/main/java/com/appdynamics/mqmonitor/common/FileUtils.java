/**
 *
 */
package com.appdynamics.monitors.mqmonitor.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James Schneider
 *
 */
public class FileUtils {

	public static final String DBL_BAK_SLASH = "\\" + "\\";
	public static final String DBL_FWD_SLASH = "//";
	
	public static final String SINGLE_BAK_SLASH = "\\";
	public static final String SINGLE_FWD_SLASH = "/";
	
	
	public static void main(String[] args) {
		String str;

		
		File theFile;
		
		str = "C:/XP/AA/xpe/bin";
		theFile = new File(str);
		if (theFile.isDirectory()) {
			System.out.println(str + " is a directory");
		} else {
			System.out.println(str + " is not a directory");
		}
		
		if (theFile.isFile()) {
			System.out.println(str + " is a file");
		} else {
			System.out.println(str + " is not a file");
		}

		str = "C:/XP/AA/xpe/bin/";
		theFile = new File(str);
		if (theFile.isDirectory()) {
			System.out.println(str + " is a directory");
		} else {
			System.out.println(str + " is not a directory");
		}
		
		if (theFile.isFile()) {
			System.out.println(str + " is a file");
		} else {
			System.out.println(str + " is not a file");
		}
		
		str = "C:/XP/AA/xpe/bin/fh_run_control";
		
		theFile = new File(str);
		boolean exists = theFile.exists();
		
		if (!exists) {
			try {
				if (FileUtils.isWindows()) {
					exists = theFile.createNewFile();
					FileUtils.hideWindowsFile(theFile);
					
				} else {
					String fileName = FileUtils.parseFileNameFromPath(str);
					if (fileName != null) {
						if (!fileName.startsWith(".")) {
							
						}
					}
				}

				

			} catch (Throwable ex) {
				
			}
		}
		
		if (exists) {
			System.out.println(str + " does exist");
		} else {
			System.out.println(str + " does not exist");
		}

		if (theFile.isHidden()) {
			System.out.println(str + " is hidden");
		} else {
			System.out.println(str + " is not hidden");
		}
		
		
	}

	/**
	 * Replaces double slashes and single back slashes in the path
	 * with single forward slashes which works on most platforms.
	 *
	 * @param fileSystemPath
	 * @return
	 */
	public static String convertFileSystemPath(String fileSystemPath) {
				
		if (fileSystemPath.indexOf(DBL_BAK_SLASH) > -1) {
			// replace double back slash
			fileSystemPath = StringUtils.replaceAll(fileSystemPath, DBL_BAK_SLASH, File.separator);
		} else if (fileSystemPath.indexOf(DBL_FWD_SLASH) > -1) {
			// replace double forward slash
			fileSystemPath = StringUtils.replaceAll(fileSystemPath, DBL_FWD_SLASH, File.separator);
		}
		
		String wrongSlash = SINGLE_FWD_SLASH;
		
		// determine what is the wrong type of single slash to look for
		if (File.separator.equals(SINGLE_FWD_SLASH)) {
			wrongSlash = SINGLE_BAK_SLASH;
		}
		
		if (fileSystemPath.indexOf(wrongSlash) > -1) {
			// replace single wrong slash
			fileSystemPath = StringUtils.replaceAll(fileSystemPath, wrongSlash, File.separator);
		}

		return fileSystemPath;
	}

	public static void hideWindowsFile(File src) throws InterruptedException, IOException {
	    // win32 command line variant
	    Process p = Runtime.getRuntime().exec("attrib +h " + src.getPath());
	    p.waitFor();
	}

	public static boolean isWindows() {
		boolean isWindows = false;
		String osName = System.getProperty("os.name");
		//System.out.println("os.name = " + osName);
		if (osName.startsWith("Windows")) {
			isWindows = true;
		}
		return isWindows;
	}
	
	/**
	 * Returns a list file paths plus names that match the
	 * pattern and attributes passed in.
	 *
	 * @param filePathWithNamePattern
	 * @param checkCanRead
	 * @param checkCanWrite
	 * @return
	 */

	/**
	 * Returns a list files that match the pattern passed in.
	 *
	 * @param directoryPath
	 * @param fileNamePattern
	 * @return
	 */
	public static File[] findSubdirectoriesInDirectory(String directoryPath) {
		if (directoryPath != null) {
			File dir = new File(directoryPath);
			//System.out.println("FileUtils.findMatchingFilesInDirectory(2 params) directoryPath = " + directoryPath);
			//System.out.println("FileUtils.findMatchingFilesInDirectory(2 params) fileNamePattern = " + fileNamePattern);

			if (!dir.exists()) {
				//System.out.println("FileUtils.findMatchingFilesInDirectory(2 params) directoryPath = " + directoryPath + " does not exist");
				return null;
			}
			if (!dir.isDirectory()) {
				//System.out.println("FileUtils.findMatchingFilesInDirectory(2 params) directoryPath = " + directoryPath + " was not a directory");
				return null;
			}
			if (!dir.canRead()) {
				//System.out.println("FileUtils.findMatchingFilesInDirectory(2 params) directoryPath = " + directoryPath + " cannot read directory");
				return null;
			}
	
			
			if (dir.exists() && dir.isDirectory() && dir.canRead()) {
				
				File[] files = dir.listFiles();
				
				if (files != null && files.length > 0) {
					List<File> directories = new ArrayList<File>();
					
					for (int i = 0; i < files.length; i++) {
						if (files[i].isDirectory()) {
							directories.add(files[i]);
						} 
					}
					
					files = new File[directories.size()];
					int cntr = 0;
					for (File file : directories) {
						
						files[cntr] = file;
						cntr++;
					}
					
					return files;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	


	/**
	 * Returns the directory from the full path passed in.
	 *
	 * @param fullPathToFile
	 * @return
	 */
	public static String parseDirectoryFromPath(String fullPathToFile) {
		if (fullPathToFile != null) {
			String dirPath;
			int lastSlash = -1;

			fullPathToFile = convertFileSystemPath(fullPathToFile);
			
			//System.out.println("FileUtils.parseDirectoryFromPath(...) fullPathToFile = " + fullPathToFile);
			
			lastSlash = fullPathToFile.lastIndexOf(SINGLE_FWD_SLASH);

			if (lastSlash > -1) {
				dirPath = fullPathToFile.substring(0, lastSlash + 1);
				//System.out.println("FileUtils.parseDirectoryFromPath(...) dirPath = " + dirPath);
				return dirPath;
			} else {
				lastSlash = fullPathToFile.lastIndexOf(SINGLE_BAK_SLASH);
				if (lastSlash > -1) {
					dirPath = fullPathToFile.substring(0, lastSlash + 1);
					//System.out.println("FileUtils.parseDirectoryFromPath(...) dirPath = " + dirPath);
					return dirPath;
				} else {
					return null;
				}				
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the file name from the full path passed in.
	 *
	 * @param filePathWithNamePattern
	 * @return
	 */
	public static String parseFileNameFromPath(String filePathWithNamePattern) {
		if (filePathWithNamePattern != null) {
			String fileName;
			int lastSlash = -1;

			filePathWithNamePattern = convertFileSystemPath(filePathWithNamePattern);

			lastSlash = filePathWithNamePattern.lastIndexOf(SINGLE_FWD_SLASH);

			if (lastSlash > -1) {
				fileName = filePathWithNamePattern.substring((lastSlash + 1));
				System.out.println("FileUtils.parseFileNameFromPath(...) fileName = " + fileName);
				return fileName;
			} else {
				lastSlash = filePathWithNamePattern.lastIndexOf(SINGLE_BAK_SLASH);
				if (lastSlash > -1) {
					fileName = filePathWithNamePattern.substring(lastSlash + 1);
					System.out.println("FileUtils.parseFileNameFromPath(...) fileName = " + fileName);
					return fileName;
				} else {
					return null;
				}				
			}
		} else {
			return null;
		}
	}

}
