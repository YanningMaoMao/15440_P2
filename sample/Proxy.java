
/**
 * @author YanningMao <yanningm@andrew.cmu.edu>
 */
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Proxy {

	private static final int NUM_PROXY_INPUT_ARGS = 4;
	private static final int MAX_FILE_NUM = 10^8;
	
	// the server that the proxy communicates with
	private static ServerInterface server;
	
	// command line arguments
	private static String serverIP;
	private static int port;
	private static String cacheDir;
	private static int cacheSize;

	private static Integer curr_max_fd;
	
	private static Cache proxyCache;
	private static List<Integer> unused_fds;
	private static Map<Integer, FileOpenInfo> opened_files;
	
	private final int proxyID;
	
	public Proxy() throws RemoteException {
		proxyID = server.registerProxy();
	}
	
	public static void init() {
		proxyCache = new Cache();
		opened_files = new HashMap<>();
		unused_fds = new ArrayList<>();
		curr_max_fd = 0;
	}
	
	private static String getFilePath(String fname) {
		assert(fname != null);
		return cacheDir + "/" + fname;
	}
	
	// TODO Need to check if transfer succeeded when opening. That's why I return the object here.
	private static FileTransferInfo downloadFileFromServer(String fname) throws RemoteException {
		assert(fname != null && fname.length() > 0);
		
		FileTransferInfo fileTransInfo = server.transferFileToProxy(fname);
		
		try {
		
			// download the file to cache if file transfer succeeded
			if (fileTransInfo.hasNoError()) {
				String fpath = getFilePath(fname);
				File file = new File(fpath);
				if (file.exists()) {
					file.delete();
				}
				FileOutputStream fos = new FileOutputStream(fpath);
				fos.write(fileTransInfo.getFileContent());
				fos.close();
			}
			
		} catch (FileNotFoundException e) {
			fileTransInfo.setError(FileTransferError.FILE_NOT_FOUND);
			fileTransInfo.setErrorMessage(e.getMessage());
		} catch (IOException e) {
			fileTransInfo.setError(FileTransferError.OTHER_IOE);
			fileTransInfo.setErrorMessage(e.getMessage());
		}
		
		
		// return transfer result
		return fileTransInfo;
	}
	
	private static class FileHandler implements FileHandling {

		public int open( String path, OpenOption o ) {
			
			// check parameters
			if (path == null || path.length() == 0 || o == null)
			{
				return Errors.EINVAL;
			}
			
			// check number of files opened
			if (opened_files.size() >= MAX_FILE_NUM)
			{
				return Errors.EMFILE;
			}
			
			// check operation type
			File file = new File(path);
			
			if (file.exists() && (o == OpenOption.CREATE_NEW))
			{
				return Errors.EEXIST;
			}
			if (!file.exists() && (o == OpenOption.READ || o == OpenOption.WRITE))
			{
				return Errors.ENOENT;
			}
			
			// get file descriptor
			int fd;
			if (unused_fds.size() != 0)
			{
				synchronized (unused_fds)
				{
					fd = unused_fds.get(0);
					unused_fds.remove(0);
				}
			}
			else
			{
				synchronized (curr_max_fd)
				{
					curr_max_fd += 1;
					fd = curr_max_fd;
				}
				
			}
			
			// add to opened files and cache
			FileOpenInfo file_info = new FileOpen(path, o);
			opened_files.put(fd, file_info);
			synchronized (proxyCache)
			{
				if (!proxyCache.containsFile(fd))
				{
					proxyCache.addFile(fd, file_info);
				}
			}
			
			// perform open
			/*
			switch (o)
			{
			case READ:
			case WRITE:
			case CREATE:
			case CREATE_NEW:
			}
			*/
			
			return fd;
		}

		public int close( int fd ) {
			
			// file not opened
			if (fd < 0) {
				return Errors.EINVAL;
			}
			if (!opened_files.containsKey(fd)) {
				return Errors.EBADF;
			}
			
			// get the file information from opened files
			FileOpenInfo file_info = opened_files.get(fd);
			File file = new File(file_info.getFileName());
			
			// check if the file is directory
			if (file.isDirectory()) {
				return Errors.EISDIR;
			}
			
			// check if the file exists
			if (!file.exists()) {
				return Errors.ENOENT;
			}

			
			// remove from cache if exists
			if (proxyCache.containsFile(fd)) {
				proxyCache.removeFile(fd);
			}
			
			// remove from opened files
			opened_files.remove(fd);
			// close the file
			unused_fds.add(fd);

			return 0;
		}

		public synchronized long write( int fd, byte[] buf ) {
			
			// file not opened
			if (fd < 0) {
				return Errors.EINVAL;
			}
			if (!opened_files.containsKey(fd)) {
				return Errors.EBADF;
			}
			
			// get the file information from opened files
			FileOpenInfo file_info = opened_files.get(fd);
			File file = new File(file_info.getFileName());
			
			// check if write to the file is permitted
			if (file_info.getOpenOption() == OpenOption.READ) {
				return Errors.EPERM;
			}
			
			// check if the file is not a directory
			if (file.isDirectory()) {
				return Errors.EISDIR;
			}
			
			// check if the file exists
			if (!file.exists()) {
				return Errors.ENOENT;
			}


			// write to the file
			FileOutputStream fos;
			try
			{
				fos = new FileOutputStream(file.getName());
				fos.write(buf);
				fos.close();
			     
			}
			catch(FileNotFoundException ex)
			{
				return Errors.ENOENT;
			}
			catch(IOException ioe)
			{
				return -1;
			}
				
			// add to cache is not exist
			synchronized (proxyCache)
			{
				if (!proxyCache.containsFile(fd)) {
					proxyCache.addFile(fd, file_info);
				}
			}
			
			return buf.length;
			
		}

		public long read( int fd, byte[] buf ) {
			
			// file not opened
			if (fd < 0) {
				return Errors.EINVAL;
			}
			if (!opened_files.containsKey(fd)) {
				return Errors.EBADF;
			}
			
			// get the file information from opened files
			FileOpenInfo file_info = opened_files.get(fd);
			File file = new File(file_info.getFileName());
			
			// check if the file is not a directory
			if (file.isDirectory()) {
				return Errors.EISDIR;
			}
			
			// check if the file exists
			if (!file.exists()) {
				return Errors.ENOENT;
			}
			
			// read from the file
			int bytes_read = 0;
			try {
				RandomAccessFile file_io = new RandomAccessFile(file_info.getFileName(), "r");
				bytes_read = file_io.read(buf);
				file_io.close();
			}
			catch (IOException e) {
				e.printStackTrace();
				if (e instanceof FileNotFoundException) {
					return Errors.ENOENT;
				}
				if (e.getMessage().contains("Bad file")) {
					return Errors.EBADF;
				}
				if (e.getMessage().contains("Permission")) {
					return Errors.EPERM;
				}
			}
			
			synchronized(proxyCache)
			{
				if (!proxyCache.containsFile(fd)) {
					proxyCache.addFile(fd, file_info);
				}
			}
			
			return bytes_read;

		}

		public long lseek( int fd, long pos, LseekOption o ) {
			
			// check the parameters
			if (pos < 0) {
				return Errors.EINVAL;
			}
			if (o == null) {
				return Errors.EINVAL;
			}
			if (fd < 0 || fd > MAX_FILE_NUM) {
				return Errors.EINVAL;
			}
			
			// check if the file is opened
			if (!opened_files.containsKey(fd)) {
				return Errors.EBADF;
			}
			
			// get the file information from opened files
			FileOpenInfo file_info = opened_files.get(fd);
			File file = new File(file_info.getFileName());
			
			// check if the file is not a directory
			if (file.isDirectory()) {
				return Errors.EISDIR;
			}
			
			// check if the file exists
			if (!file.exists()) {
				return Errors.ENOENT;
			}

			// create file IO
			RandomAccessFile file_io;
			try
			{
				if (file_info.getOpenOption() == OpenOption.READ)
				{
					file_io = new RandomAccessFile(file_info.getFileName(), "r");
				}
				else
				{
					file_io = new RandomAccessFile(file_info.getFileName(), "rw");
				}
				
				// calculate seek position
				switch (o) {
				case FROM_CURRENT:
					pos += file_io.getFilePointer();
					break;
				case FROM_START:
					break;
				case FROM_END:
					pos = file_io.length() + pos;
				}
				
				file_io.close();
			}
			catch (IOException e) {
				return handleIOException(e);
			}
			
			// re-check parameter validity
			if (pos < 0) {
				return Errors.EINVAL;
			}
			
			// perform seek
			try
			{
				file_io.seek(pos);
			}
			catch (IOException e)
			{
				return handleIOException(e);
			}
			
			// update the cache
			synchronized (proxyCache) {
				if (!proxyCache.containsFile(fd)) {
					proxyCache.addFile(fd, file_info);
				}
			}
			
			return pos;
		}

		/**
		 * Delete the file indicated by String path.
		 * Returns 0 on success ,or a negative value indicating the error
		 * that occurred (see Errors enum).
		 */
		public int unlink( String path ) {
			
			// check input parameter
			if (path == null) {
				return Errors.EINVAL;
			}
			
			File file = new File(path);
			if (!file.exists())
			{
				return Errors.ENOENT;
			}
			try {
				file.delete();
				return 0;
			}
			catch (SecurityException e)
			{
				return Errors.EPERM;
			}
			
		}

		public void clientdone() {
			return;
		}
		
		private static int handleIOException(IOException e) {
			e.printStackTrace();
			if (e instanceof FileNotFoundException)
			{
				return Errors.ENOENT;
			}
			else
			{
				return -1;
			}
		}
		
	}
	
		
	
	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) {
		
		try {
			// read input arguments
			if (args.length < NUM_PROXY_INPUT_ARGS) {
				throw new Exception ("Wrong number of command line arguments."
							+ "There should be "
							+ NUM_PROXY_INPUT_ARGS + ".");
			}
			serverIP = args[0];
			port = Integer.parseInt(args[1]);
			cacheDir = args[2];
			cacheSize = Integer.parseInt(args[3]);
			
			// initialize the Proxy
			init();
			
			// connect to Server
			String serverName = "//" + serverIP + ":" + port + "/" + "ServerInterface";
			server = (ServerInterface) Naming.lookup(serverName);
			
			// notify the user
			System.out.println("Proxy setup succeeded.");
			
		} catch (Exception e) {
			
			System.err.println("Proxy Setup Failed : " + e.getMessage());
			e.printStackTrace();
			
		}
		
		System.out.println("Testing downloadFileFromServer() ...");
		String testFileName = "test_file.txt";
		FileTransferInfo transFileInfo;
		try {
			transFileInfo = downloadFileFromServer(testFileName);
			System.out.println("transFileInfo.error : " + transFileInfo.getError());
		} catch (RemoteException e) {
			System.err.println("Server Remote Exception : " + e.getMessage());
			e.printStackTrace();
		}
		
		
		/*
		// handle the Clients
		System.out.println("Waiting for Clients ...");
		try {
			(new RPCreceiver(new FileHandlingFactory())).run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
}



