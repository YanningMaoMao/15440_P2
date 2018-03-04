
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 *
 */

public class Cache {
	
	private static Map<Integer, FileOpenInfo> files;

	// private String dir = "./cache";
	// public String getFilePath(String fname)
	
	public Cache() {
		files = new LinkedHashMap<>();
	}
	
	public boolean containsFile(int fd) {
		return files.containsKey(fd);
	}
	
	public boolean containsFile(String fname) {
		for (Integer fd : files.keySet()) {
			if (files.get(fd).getFileName().equals(fname)) {
				return true;
			}
		}
		return false;
	}
	
	public int getFileFD(String fname) {
				
		for (Integer fd : files.keySet()) {
			FileOpenInfo file = files.get(fd);
			if (file.getFileName().equals(fname)) {
				return fd;
			}
		}
		
		return -1;
		
	}
	
	public void addFile(int fd, String fname, FileHandling.OpenOption op) {
		assert(fd > 0);
		
		FileOpenInfo newFile = new FileOpen(fname, op);
		files.put(fd, newFile);
	}
	
	public void addFile(int fd, FileOpenInfo file) {
		assert(fd > 0);
		assert(file != null);
		files.put(fd, file);
	}
	
	public void removeFile(String fname) {
		
		int fd = getFileFD(fname);
		if (fd < 0) {
			return;
		}
		else {
			files.remove(fd);
		}
	}
	
	public void removeFile(int fd) {
		
		if (files.containsKey(fd)) {
			files.remove(fd);
		}
	}

	public FileOpenInfo getFileInfo(String name) {
		
		int fd = getFileFD(name);
		
		if (fd < 0) {
			return null;
		}
		
		else {
			return files.get(fd);
		}

	}

	public FileOpenInfo getFileInfo(int fd) {
		
		return files.get(fd);
	
	}
	
	
}


