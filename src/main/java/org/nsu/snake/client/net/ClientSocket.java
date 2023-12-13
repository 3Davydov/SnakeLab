package org.nsu.snake.client.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

public class ClientSocket extends Thread {
    private final String ipAddress = "239.192.0.4";
    private final int port = 9192;
    private final ClientMain clientMain;
    private final MulticastSocket multicastListener;
    private final DatagramChannel socketChannel;
    private final Selector selector;
    public ClientSocket(ClientMain clientMain) throws IOException {
        this.clientMain = clientMain;
        multicastListener = new MulticastSocket(port);
        multicastListener.joinGroup(new InetSocketAddress(InetAddress.getByName(ipAddress), port), NetworkInterface.getNetworkInterfaces().nextElement());
        multicastListener.setSoTimeout(100);

        socketChannel = DatagramChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.bind(new InetSocketAddress(InetAddress.getByName(null), 0));
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);
        start();
    }
    public String getSelfIP() {
        return socketChannel.socket().getLocalAddress().getHostAddress();
    }
    public int getSelfPort() {
        return socketChannel.socket().getLocalPort();
    }
    @Override
    public void run() {
        try {
            startRoutine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void startRoutine() throws IOException {
        while (! Thread.currentThread().isInterrupted()) {
            int readyChannels = selector.select(100);
            if (readyChannels == 0) {
                getMulticastMessage();
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isReadable()) {
                    getMessage();
                }
                keyIterator.remove();
            }
        }
    }
    public void sendMessage(SnakesProto.GameMessage message) throws IOException {
        byte[] messageBytes = message.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);
        socketChannel.send(byteBuffer, new InetSocketAddress(ipAddress, port));
    }
    public synchronized void sendUnicastMessage(String ip, int port, SnakesProto.GameMessage message) throws IOException {
        if (message == null) return;
        byte[] messageBytes = message.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);
        socketChannel.send(byteBuffer, new InetSocketAddress(ip, port));
    }
    private void getMulticastMessage() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            multicastListener.receive(packet);
        } catch (SocketTimeoutException e) {
            return;
        }
        byte[] actualData = Arrays.copyOf(packet.getData(), packet.getLength());

        SocketAnswer answer = new SocketAnswer();
        answer.gameMessage = SnakesProto.GameMessage.parseFrom(actualData);

        answer.senderIP = packet.getAddress().getHostAddress();
        answer.senderPort = packet.getPort();

        clientMain.processIncomingMessage(answer);
    }
    private void getMessage() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        InetSocketAddress senderAddress = (InetSocketAddress) socketChannel.receive(buffer);

        SocketAnswer answer = new SocketAnswer();
        buffer.flip();
        answer.gameMessage = SnakesProto.GameMessage.parseFrom(buffer);
        answer.senderIP = senderAddress.getAddress().getHostAddress();
        answer.senderPort = senderAddress.getPort();
        clientMain.processIncomingMessage(answer);
    }

    public class SocketAnswer {
        public SnakesProto.GameMessage gameMessage;
        public String senderIP;
        public int senderPort;
    }

    public static void main(String[] args) throws IOException {
        ClientSocket clientSocket = new ClientSocket(new ClientMain());
    }
}
