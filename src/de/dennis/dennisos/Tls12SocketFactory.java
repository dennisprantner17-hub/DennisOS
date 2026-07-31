package de.dennis.dennisos;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Tls12SocketFactory
        extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    public Tls12SocketFactory(
            SSLSocketFactory delegate
    ) {
        this.delegate = delegate;
    }

    private Socket enableTls12(
            Socket socket
    ) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket)
                    .setEnabledProtocols(
                            new String[]{
                                    "TLSv1.2"
                            }
                    );
        }

        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(
            Socket socket,
            String host,
            int port,
            boolean autoClose
    ) throws IOException {
        return enableTls12(
                delegate.createSocket(
                        socket,
                        host,
                        port,
                        autoClose
                )
        );
    }

    @Override
    public Socket createSocket(
            String host,
            int port
    ) throws IOException {
        return enableTls12(
                delegate.createSocket(
                        host,
                        port
                )
        );
    }

    @Override
    public Socket createSocket(
            String host,
            int port,
            InetAddress localHost,
            int localPort
    ) throws IOException {
        return enableTls12(
                delegate.createSocket(
                        host,
                        port,
                        localHost,
                        localPort
                )
        );
    }

    @Override
    public Socket createSocket(
            InetAddress host,
            int port
    ) throws IOException {
        return enableTls12(
                delegate.createSocket(
                        host,
                        port
                )
        );
    }

    @Override
    public Socket createSocket(
            InetAddress address,
            int port,
            InetAddress localAddress,
            int localPort
    ) throws IOException {
        return enableTls12(
                delegate.createSocket(
                        address,
                        port,
                        localAddress,
                        localPort
                )
        );
    }
}
