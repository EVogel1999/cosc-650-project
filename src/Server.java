import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Server extends Thread {
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf;
    private int z;

    public Server(int z) throws SocketException {
        // STEP 2.1
        socket = new DatagramSocket(22333);
        buf = new byte[z];
        this.z = z;
    }

    @Override
    public void run() {
        super.run();
        try {
            long bytes = 0;
            running = true;

            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                // Get the data and parse it out
                bytes = bytes + packet.getData().length;
                InetAddress address = packet.getAddress();
                packet = new DatagramPacket(padBuffer(("ACK | Num of bytes received: " + bytes).getBytes(), z), buf.length, address, packet.getPort());
                String received = new String(buf, 0, packet.getLength());

                // STEP 2.6
                // Print out data received and send ACK
                System.out.println("S: " + received);
                socket.send(packet);
            }
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This pads a existing buffer array to be of size z, this avoids exceptions being thrown with the datagram
     *
     * @param buffer The buffer to pad
     * @param z The size to buffer to
     * @return The new buffered array
     */
    private byte[] padBuffer(byte[] buffer, int z) {
        byte[] buf = new byte[z];
        for (int i = 0; i < buffer.length; i++) {
            buf[i] = buffer[i];
        }
        return buf;
    }
}
