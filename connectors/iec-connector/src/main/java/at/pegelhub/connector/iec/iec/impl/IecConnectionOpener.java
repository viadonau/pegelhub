package at.pegelhub.connector.iec.iec.impl;

import org.openmuc.j60870.ClientConnectionBuilder;
import org.openmuc.j60870.Connection;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@FunctionalInterface
interface IecConnectionOpener {
    Connection open(String host, int port, Socket socket) throws IOException;

    static Connection openTcp(String host, int port, Socket socket) throws IOException {
        return new ClientConnectionBuilder(InetAddress.getByName(host))
                .setPort(port)
                .setConnectionTimeout(10_000)
                .setSocketFactory(new SocketFactory() {
                    @Override
                    public Socket createSocket() { return socket; }

                    @Override
                    public Socket createSocket(String host, int port) throws IOException {
                        throw new IOException("Connected socket creation is not supported");
                    }

                    @Override
                    public Socket createSocket(String host, int port, InetAddress local, int localPort) throws IOException {
                        throw new IOException("Connected socket creation is not supported");
                    }

                    @Override
                    public Socket createSocket(InetAddress host, int port) throws IOException {
                        throw new IOException("Connected socket creation is not supported");
                    }

                    @Override
                    public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) throws IOException {
                        throw new IOException("Connected socket creation is not supported");
                    }
                }).build();
    }
}
