
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author YanningMao <yanningm@andrew.cmu.edu>
 */

enum UpdateStatus {
	UPDATED,
	UNUPDATED
}

public class Server extends UnicastRemoteObject implements ServerInterface {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int NUM_SERVER_INPUT_ARGS = 2;
	
	private int port;
	private String rootDir;
	
	private Set<Integer> proxies;
	private int nextProxyID;
	
	private int nextFD;
	private List<Integer> unusedFDs;
	private Set<Integer> usingFDs;
	
	private Map<Integer, Map<String, UpdateStatus>> proxyFileVersionTracker;
	
	public Server(int port, String rootDir) throws RemoteException {
		super();
		this.port = port;
		this.rootDir = rootDir;
		// initialize proxies information
		nextProxyID = 1;
		proxies = new HashSet<>();
		// initialize file descriptor information
		nextFD = 0;
		unusedFDs = new ArrayList<>();
		usingFDs = new HashSet<>();
		// initialize file version information
		proxyFileVersionTracker = new HashMap<>();
	}
	
	public int getPort() {
		return port;
	}
	
	public String getRootDir() {
		return rootDir;
	}
	
	@Override
	public String returnString () throws RemoteException {
		return "Hello Client";
	}
	
	@Override
	public int returnInt() throws RemoteException {
		return 3;
	}
	
	@Override
	public synchronized int registerProxy () throws RemoteException {
		
		// check if the list of clients is initialized
		if (proxies == null) {
			proxies = new HashSet<>();
		}
		
		// get proxy ID
		int proxyID = nextProxyID;
		nextProxyID += 1;
		
		// register the proxy
		proxies.add(proxyID);
		
		// add to file version tracker
		proxyFileVersionTracker.put(new Integer(proxyID), new HashMap<>());
		
		return proxyID;
	}
	
	@Override
	public boolean isClientProxy (int clientID) throws RemoteException {
		
		if (proxies == null) {
			return false;
		}
		else if (proxies.isEmpty()) {
			return false;
		}
		else {
			return proxies.contains(clientID);
		}
	}
	
	private synchronized int getNewFD() {
		
		int fd;
		
		// get file descriptor
		if (!(unusedFDs.isEmpty())) {
			fd = unusedFDs.get(0);
			unusedFDs.remove(0);
		}
		else {
			fd = nextFD;
			nextFD += 1;
		}

		usingFDs.add(fd);
		return fd;
		
	}
	
	@Override
	public boolean hasLatestFileVersion(int proxyID, String fname) throws RemoteException {
		
		UpdateStatus status;
		synchronized (proxyFileVersionTracker) {
			// check if proxy has the latest version
			status = proxyFileVersionTracker.get(proxyID).get(fname);
			// if the proxy never had client open this file
			if (status == null) {
				status = UpdateStatus.UNUPDATED;
				proxyFileVersionTracker.get(proxyID).put(fname, UpdateStatus.UNUPDATED);
			}
		}
		
		if (status.equals(UpdateStatus.UNUPDATED)) {
			return false;
		} else {
			return true;
		}

	}
	
	
	
	// if updated, return the FD to the proxy;
	// if unupdated, send the file to the proxy (remember to write lock on the file.
	// It can be simultaneously sent to multiple proxies, however, it cannot be modified.
	/* It has to keep its currrent version. Therefore, after acquiring the lock, the file
	 * can be send to multiple proxies; however, proxies trying to update the file to the server
	 * have to wait), then return the FD
	 * 
	 * The transfer file to client function does not have to be synchronized (though the proxy
	 * side has to deal with locking, because it is receiving the file, and therefore it is facing
	 * file update on its side.
	 * The transfer file to client function has to set the flag that the file is being
	 * transferred to proxy.
	 * 
	 * The receiving update file from client function has to be synchronized, because clients cannot be
	 * simultaneously updating the same file.
	 * Moreover, the function need to check if the flag that the file is being transferred to the proxy
	 * is set. It can update only if it is not.
	 * Or, maybe, if you do not want to affect the progress on the proxy side, you can first name the
	 * updated files with a _v[i] version suffix, then replace the old file when the flag is down.
	 */
	
	@Override
	public FileTransferInfo transferFileToProxy(String fname) throws RemoteException {
		// TODO lock needed
		FileTransferInfo fileTransInfo = new FileTransfer(fname, rootDir);
		return fileTransInfo;
	}
	
	@Override
	public int openFile(int proxyID, String fname) throws RemoteException {
		
		// get new file descriptor number
		int fd = getNewFD();
		
		return fd;
		
	}
	
	/**
	@Override
	public void closeFile(int fd)
	{
		
	}
	*/
	
	public String getFilePathWithRootDir (String fpath) {
		
		return rootDir + "/" + fpath;
	}
	
	public static void main(String[] args) {
		
		Server server;
		
		try
		{
			// check number of input arguments
			if (args == null || args.length < NUM_SERVER_INPUT_ARGS) {
				throw new Exception("Wrong number of command line arguments."
						+ "There should be " + NUM_SERVER_INPUT_ARGS + ".");
			}
			
			// extract the port
			int port = Integer.parseInt(args[0]);
			// extract the root directory path
			String rootDir = args[1];
			
			// create server and bind remote object's stub
			server = new Server(port, rootDir);
			LocateRegistry.createRegistry(server.port);
			Registry registry = LocateRegistry.getRegistry(port);
			registry.bind("ServerInterface", server);
			
			// notify the user
			System.out.println("Server setup succeeded.");
			
		}
		catch (Exception e)
		{
			System.err.println("Server setup failed : " + e.getMessage());
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void updateFileFromProxy(FileTransferInfo fileTransInfo) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
}



