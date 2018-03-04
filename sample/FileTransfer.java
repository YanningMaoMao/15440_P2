
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 *
 */
public class FileTransfer implements FileTransferInfo {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String fname;
	private String fpath;
	
	private int fileSize;
	private byte[] fileContent;
	
	private FileTransferError error;
	private String errorMsg;

	
	public FileTransfer(String fname, String dirPath) {
		// set file name and file path
		this.fname = fname;
		this.fpath = dirPath + "/" + fname;
		// initialize error flag and error message
		error = FileTransferError.NONE;
		errorMsg = "";
		// read file content into buffer
		readFile();
	}

	@Override
	public String getFileName() {
		return fname;
	}
	
	@Override
	public int getFileSize() {
		return fileSize;
	}
	
	@Override
	public FileTransferError getError() {
		return error;
	}
	
	@Override
	public void setError(FileTransferError error) {
		this.error = error;
	}
	
	@Override
	public boolean hasNoError() {
		return error.equals(FileTransferError.NONE);
	}
	 
	@Override
	public String getErrorMessage() {
		return errorMsg;
	}
	
	@Override
	public void setErrorMessage(String msg) {
		errorMsg = msg;
	}
	
	@Override
	public byte[] getFileContent() {
		assert(fileContent != null);
		assert(fileContent.length == fileSize);
		assert(error.equals(FileTransferError.NONE));
		return (fileContent);
	}
	
	private void readFile() {
		
		// open file input stream, set up error variable if any
		File file = new File(fpath);
		BufferedInputStream inStream;
		try {
			inStream = new BufferedInputStream(new FileInputStream(file));

			// read the file content into the buffer
			fileSize = (int)file.length();
			fileContent = new byte[fileSize];
			inStream.read(fileContent, 0, fileSize);
			
		} catch (FileNotFoundException e) {
			error = FileTransferError.FILE_NOT_FOUND;
			setErrorMessage(e.getMessage());
		} catch (IOException e) {
			error = FileTransferError.OTHER_IOE;
			setErrorMessage(e.getMessage());
		}
		
	}
	

}



