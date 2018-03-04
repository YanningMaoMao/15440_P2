
/**
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 *
 */
public class FileOpen implements FileOpenInfo {
	
	private String fname;
	private FileHandling.OpenOption openOp;
	
	public FileOpen(String fname, FileHandling.OpenOption openOp) {
		this.fname = fname;
		this.openOp = openOp;
	}
	
	@Override
	public String getFileName() {
		return fname;
	}
	
	@Override
	public FileHandling.OpenOption getOpenOption() {
		return openOp;
	}

}


