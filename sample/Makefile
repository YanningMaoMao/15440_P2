all: Proxy.class Cache.class FileOpen.class FileOpenInfo.class FileTransfer.class FileTransferError.java FileTransferInfo.class Server.class ServerInterface.class

%.class: %.java
	javac $<

clean:
	rm -f *.class
