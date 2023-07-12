import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Time;
import java.util.*;

public class DMBServer {

    static Configuration c_;
    static int port; //server port
    static String boardDirectory;

    /**
     * Main method sets up the config file
     * and then goes on to create a new server socket and have a while true loop that will loop the operation of the server
     * until it is killed with ctrl + c
     * @param args no arguments
     */
    public static void main(String[] args) {

        //get port and board directory from config file
        c_ = new Configuration("cs2003-net2.properties");
        try {
            port = c_.serverPort;
            boardDirectory = c_.boardDirectory;
        } catch (NumberFormatException e) {
            System.out.println("can't configure port: " + e.getMessage());
            System.exit(0);
        }

        try {
            //creating a socket
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("\n++ Socket created at port: " + port);

            //loop forever
            while (true) {
                loop(serverSocket);
            }


            //catch an IO exception thrown by creating a bad socket
        } catch (IOException e) {
            System.out.println("I/O error" + e);
        }
    }

    /**
     * main loop of server program operation
     * @param serverSocket socket for server as created earlier
     * @throws IOException when getting streams from socket
     */
    static void loop(ServerSocket serverSocket) throws IOException {

        //receive connection from client
        System.out.println("\n++ Waiting for client connection");
        Socket socket = serverSocket.accept();
        System.out.println("\n++ Connection made");

        //receive clients input
        InputStream input = socket.getInputStream();

        //convert client's input to string
        String inputStr = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("\n++ Received data from client: " + inputStr);

        //close the connection
        socket.close();
        System.out.println("\n++ Connection closed\n");

        //response var
        String response;

        try {

            //check the start of the input string
            String command = inputStr.substring(0, 4);

            //if fetch command
            if (command.equals("::fe")) {
                //handle fetch and send a response
                response = handleFetch(inputStr);
                sendResponse(response, serverSocket);

                //if from command
            } else if (command.equals("::fr")) {

                //handle from
                handleFrom(inputStr);

                //if neither of these two commands then error msg
            } else {
                System.out.println("\n Error: server received unrecognised command");
                System.exit(0);
            }

        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("\n Error: server received unrecognised command");
        }


    }

    /**
     * splits up client's input and passes it into the DirAndFile class
     * @param inputStr input from client
     */
    static void handleFrom(String inputStr) {

        //remove :: from start of string
        inputStr = inputStr.substring(2);

        //array of details to be sent to dirandfile program
        String[] details = new String[3];
        TimeStamp timeStamp = new TimeStamp();
        details[0] = timeStamp.getDate();
        details[1] = boardDirectory + details[0].substring(0, 10);
        details[2] = inputStr;
        DirAndFile.main(details);


    }

    /**
     *
     * @param inputStr input from client
     * @return response to be sent back by server
     * @throws IOException exception from accessing files
     */
    static String handleFetch(String inputStr) throws IOException {

        //split up client's input and get the date
        String[] inputArray = inputStr.split(" ");
        String date = inputArray[2];

        //get a list of all files and folders in the filespace
        File fileSpace = new File(boardDirectory);
        File[] files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(fileSpace.listFiles()))).toArray(new File[0]);

        //see if the date specified exists as a folder
        File dateFolder = null;
        boolean dateFolderExists = false;
        for (File file : files) {
            if (file.getName().equals(date)) {
                dateFolder = file;
                dateFolderExists = true;
            }
        }

        //if it isnt found return the none string
        if (!dateFolderExists) {
            return "::none";
        } else {

            //get all message files from the date folder
            File[] messageFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(dateFolder.listFiles()))).toArray(new File[0]);

            //put the filename and contents into a hashmap
            HashMap<String, String> messages = new HashMap<>();
            for (File file : messageFiles) {
                BufferedReader br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
                messages.put(file.getName(), br.readLine());
            }

            //set the response string to every message and then wrap it in ::messages and ::end
            String responseStr = "::messages " + date;
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                responseStr += "\n\t" + entry.getKey() + " " + entry.getValue();
            }
            responseStr += "\n::end";

            return responseStr;
        }
    }

    /**
     * this method accepts a new connection from the client and then sends back the response
     * @param response message to be sent back to client
     * @param serverSocket socket from earlier
     * @throws IOException when getting streams from socket
     */
    static void sendResponse (String response, ServerSocket serverSocket) throws IOException {

            //receive connection from client
            System.out.println("\n++ Waiting for client connection");
            Socket socket = serverSocket.accept();
            System.out.println("\n++ Connection made");
            OutputStream output = socket.getOutputStream();

            //create and write a buffer to the client
            byte[] buffer;
            buffer = response.getBytes();
            System.out.print("\n++ Sending response...");
            output.write(buffer, 0, buffer.length);
            System.out.println(" ...response sent");

            //close the connection
            socket.close();
            System.out.println("\n++ Connection closed");
    }


    }