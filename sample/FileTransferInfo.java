
import java.io.Serializable;

/**
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 *
 */
public interface FileTransferInfo extends Serializable {
	
	public String getFileName();
	public int getFileSize();
	public String getErrorMessage();
	public void setErrorMessage(String msg);

	public byte[] getFileContent();
	
	public boolean hasNoError();
	public FileTransferError getError();
	public void setError(FileTransferError error);
	

}



