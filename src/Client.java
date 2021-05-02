import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private static String w = "";      // Web server name i.e. (www.a.com)
    private static int x = 0;          // Num of bytes client will send to server
    private static int z = 0;
    private static int y = 0;         // Timeout period

    // Used to send info to server
    private static DatagramSocket socket;
    private static InetAddress address;
    private static Thread thread;

    // Used for step 2.8
    private static long start, end, bytes;

    public static void main(String[] args) throws Exception {
        getUserInput();

        // Start the server thread
        Server s = new Server(z);
        thread = new Thread(s);
        thread.start();

        contactWebServer();
        printFinalResults();
        System.exit(0);
    }

    /**
     * Gets the user input as per step 2.2. Uses the following format:
     * w - Must be a valid http/https url (i.e. https://www.asdf.com)
     * x - Must be a valid positive integer
     * y - Must be a valid positive integer
     */
    private static void getUserInput() {
        // STEP 2.2
        // Get user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input (w x y):");
        String line = scanner.nextLine();
        scanner.close();

        // Parse out info
        w = line.substring(0, line.indexOf(" "));
        line = line.substring(line.indexOf(" ") + 1);
        x = Integer.parseInt(line.substring(0, line.indexOf(" ")));
        z = Math.min(x, 1460);
        line = line.substring(line.indexOf(" ") + 1);
        y = Integer.parseInt(line);
    }

    /**
     * Makes contact with the web server provided in the user input and sends the streamed data to the server
     *
     * @throws Exception If there is a problem connecting to the URL, server, etc.
     */
    private static void contactWebServer() throws Exception {
        // Finish setting up socket connection
        socket = new DatagramSocket();
        socket.setSoTimeout(y); // Sets the timeout used in step 2.7
        address = InetAddress.getByName("localhost");

        // STEP 2.3
        // Make the GET request
        URL url = new URL(w);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        // STEP 2.4
        // Read each line
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        start = System.currentTimeMillis();
        while ((inputLine = in.readLine()) != null) {
            byte[] b = inputLine.getBytes();
            for (int i = 0; i < b.length; i = i + z) { // Make sure you send everything in this line of data
                byte[] buf = padBuffer(Arrays.copyOfRange(b, i, Math.min(i + z, b.length)), z); // Ensure the bytes sent in the packet are of size z
                DatagramPacket packet = new DatagramPacket(buf, z, address, 22333);
                // STEP 2.5
                socket.send(packet);
                System.out.println("C: " + new String(buf, 0, z));

                // STEP 2.7
                receiveAck(false, packet);
            }
        }
        in.close();
        end = System.currentTimeMillis();
    }

    /**
     * Prints the final metric results for step 2.8
     */
    private static void printFinalResults() {
        // STEP 2.8
        System.out.println("C: DONE");
        System.out.println("C: Total elapsed time (in ms) is " + (end - start));
        System.out.println("C: Total number of bytes sent successfully is " + bytes);
    }

    /**
     * Checks for the ACK from the server, retrying if necessary
     *
     * @param retry True if didn't receive the ACK by the timeout, false otherwise
     * @param p The packet to resend if we need to retry
     * @throws Exception Thrown when failing to receive an ACK twice in a row for a single packet
     */
    private static void receiveAck(boolean retry, DatagramPacket p) throws Exception {
        try {
            byte[] buf = new byte[z];
            DatagramPacket packet = new DatagramPacket(buf, z);
            socket.receive(packet);
            // Prints the ACK sent from the server
            System.out.println("C: " + new String(buf, 0, z));
            bytes += buf.length;
        }
        catch (SocketTimeoutException e) {
            // We reached the timeout... try resending if we aren't retrying already
            // Else error out and send the fail message
            if (!retry) {
                socket.send(p);
                receiveAck(true, p);
            } else {
                DatagramPacket packet = new DatagramPacket(padBuffer("FAIL".getBytes(), z), z, address, 22333);
                socket.send(packet);
                throw new Exception("Failed to send ACK twice in a row for one packet");
            }
        }
    }

    /**
     * This pads a existing buffer array to be of size z, this avoids exceptions being thrown with the datagram
     *
     * @param buffer The buffer to pad
     * @param z The size to buffer to
     * @return The new buffered array
     */
    private static byte[] padBuffer(byte[] buffer, int z) {
        byte[] buf = new byte[z];
        for (int i = 0; i < buffer.length; i++) {
            buf[i] = buffer[i];
        }
        return buf;
    }
}
