
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 
 */

/**
 * @author YanningMao <yanningm@andrew.cmu.edu>
 *
 */
public interface ServerInterface extends Remote {
		
	public String returnString() throws RemoteException;
	public int returnInt() throws RemoteException;
	
	public boolean isClientProxy(int clientID) throws RemoteException;
	public int registerProxy () throws RemoteException;
	
	public void updateFileFromProxy(FileTransferInfo fileTransInfo) throws RemoteException;
	public FileTransferInfo transferFileToProxy(String fname) throws RemoteException;
	
	public boolean hasLatestFileVersion(int proxyID, String fname) throws RemoteException;
	
	
	public int openFile(int proxyID, String fname) throws RemoteException;
	
	
	
}



