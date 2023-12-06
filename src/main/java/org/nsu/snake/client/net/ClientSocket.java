package org.nsu.snake.client.net;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

public class ClientSocket {
    private final String ipAddress = "239.192.0.4";
    private final int port = 9192;

    private final MulticastSocket multicastListener;
    private final DatagramSocket multicastSender;

    public ClientSocket() throws IOException {
        multicastListener = new MulticastSocket(port);
        multicastListener.joinGroup(new InetSocketAddress(InetAddress.getByName(ipAddress), port), NetworkInterface.getNetworkInterfaces().nextElement());

        multicastSender = new DatagramSocket();
    }

    public void sendMessage(SnakesProto.GameMessage message) throws IOException {
        byte[] messageBytes = message.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, InetAddress.getByName(ipAddress), port);
        multicastSender.send(packet);
    }
    public SocketAnswer getMessage() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        multicastListener.receive(packet);
        byte[] actualData = Arrays.copyOf(packet.getData(), packet.getLength());

        SocketAnswer answer = new SocketAnswer();
        answer.gameMessage = SnakesProto.GameMessage.parseFrom(actualData);

        InetAddress addr = packet.getAddress();
        answer.senderIP = addr.getHostAddress();

        return answer;
    }

    public class SocketAnswer {
        public SnakesProto.GameMessage gameMessage;
        public String senderIP;
    }

    public static void main(String[] args) throws IOException {
        ClientSocket clientSocket = new ClientSocket();


    }
}
