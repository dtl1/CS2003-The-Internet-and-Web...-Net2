import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Daily Message Board Client
 *
 * based on code by Saleem Bhatti, 28 Aug 2019
 */
public class DMBClient {

    static int maxTextLen_ = 256;
    static Configuration c_;

    // from configuration file
    static String server; // FQDN
    static int port; //server port

    //timestamp object to be used by program
    static final TimeStamp timeStamp = new TimeStamp();

    //class wide variables
    static String requestType = "";
    static String message = "";

    /**
     * main method sets up configeration then calls methods to validate the clients input,
     * send data to the server,
     * and if a fetch command is being used then receive a response from the server
     * @param args no arguments
     */
    public static void main(String[] args) {

        //get port and server from config file
        c_ = new Configuration("cs2003-net2.properties");
        try {
            server = c_.defaultUser + c_.serverAddress;
            port = c_.serverPort;
        } catch (NumberFormatException e) {
            System.out.println("can't configure port: " + e.getMessage());
        }

        validateInput();
        sendData();

        if(requestType.equals("fetch"))
            handleResponse();

    }

    /**
     * uses a scanner to take in user input and a while loop to repeat until the user has entered
     * a valid input
     */
    static void validateInput() {

        //create hashmap of users and ports from csv file
        HashMap<String, Integer> userPorts = readInCSV();

        //get users input
        Scanner scanner = new Scanner(System.in);

        //loops until input is valid
        boolean validInput = false;
        while (!validInput) {

            System.out.println("\nInput a command:");
            String userInput = scanner.nextLine();

            //split up user input
            String[] inputArray = userInput.split(" ");

            //check if user is using a fetch command
            if (inputArray[0].equals("::fetch")) {

                validInput = handleFetch(inputArray, validInput);

                //or a to command
            } else if (inputArray[0].equals("::to")) {

                validInput = handleTo(inputArray, validInput, userPorts);

                //otherwise, display error message for unknown command
            } else {
                System.out.println("\n Error, unrecognised command");
            }

        }
    }

    /**
     * creates a connection with and sends data to the server
     */
    static void sendData() {

        try {
            Socket connection;
            OutputStream tx;
            byte[] buffer;
            String quit = "quit";
            int r;

            //open the connection
            connection = startClient(server, port);

            //check if connection successfully created
            if (connection != null) {
                System.out.println("\n++ Connection successful\n");
            } else {
                System.out.println("\n++ Connection unsuccessful\n");
                System.exit(0);
            }

            //create buffer with correct command, user and message
            tx = connection.getOutputStream();
            buffer = ("::" + requestType + " " + timeStamp.getUser() + " " + message).getBytes();

            //write buffer off to server
            r = buffer.length;
            if (r > maxTextLen_) {
                System.out.println("++ You entered more than " + maxTextLen_ + "bytes ... truncating.");
                r = maxTextLen_;
            }
            System.out.println("Sending " + r + " bytes");
            tx.write(buffer, 0, r); // to server

            //close the connection
            System.out.print("\n++ Closing connection ... ");
            connection.close();
            System.out.println("... closed.");


        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }
    }

    /**
     * creates a connection with and receives data from the server
     */
    static void handleResponse() {

        try {

            //new connection
            Socket connection = startClient(server, port);

            //check if connection successfully created
            if (connection != null) {
                System.out.println("\n++ Connection successful\n");
            } else {
                System.out.println("\n++ Connection unsuccessful\n");
                System.exit(0);
            }

            InputStream input = connection.getInputStream();

            //get response
            System.out.println("\n++ Waiting for response");
            String response = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("++ Received response from server");
            System.out.println(response);


            //close connection
            System.out.print("\n++ Closing connection ... ");
            connection.close();
            System.out.println("... closed.");

        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }
    }

    /**
     * this method checks the fetch command to see if it is valid and if it is then it updates the message
     * and request type fields
     *
     * @param inputArray user's split input
     * @param validInput boolean var to check if input is valid
     * @return true if valid fetch command, false if not
     */
    static boolean handleFetch(String[] inputArray, boolean validInput) {

        //if the client has specified a date
        if (inputArray.length == 2) {

            validInput = true;
            requestType = "fetch";

            //validate the date the client has specified
            String dateRegex = "(?<date>\\d{4}-\\d{2}-\\d{2})";
            Pattern p = Pattern.compile(dateRegex);
            Matcher m = p.matcher(inputArray[1]);

            if(m.find()){
                message = m.group("date");
            } else{
                System.out.println("++Invalid date provided\n");
                validInput = false;
            }


            ///if they havent
        } else if (inputArray.length == 1) {

            validInput = true;
            requestType = "fetch";

            //today's date
            message = timeStamp.getDate().substring(0, 10);

        } else {
            System.out.println("\nInvalid input syntax\nCorrect syntax: \"::fetch <date>\"");
            System.out.println("Or use: \t\"::fetch\" \t to use today's date");
        }

        return validInput;
    }

    /**
     * this method checks the to command to see if it is valid and if it is then it updates the message
     * and request type fields
     *
     * @param inputArray user's split input
     * @param validInput boolean var to check if input is valid
     * @param userPorts hashmap of usernames and their ports
     * @return true if valid to command, false if not
     */
    static boolean handleTo(String[] inputArray, boolean validInput, HashMap<String, Integer> userPorts) {

        //variable to hold specified username to send to
        String user;

        if (inputArray.length < 3) {
            System.out.println("\nInvalid input syntax\nCorrect syntax: \"::to <user> <message>\"");

            //if specified user isn't in the class list
        } else if (!(userPorts.containsKey(inputArray[1]))) {
            System.out.println("No user exists with this username");

        } else {
            validInput = true;
            requestType = "from";

            user = inputArray[1];

            //set message variable to concatenation of every element in the input array after the username
            for (int i = 2; i < inputArray.length; i++) {
                message += inputArray[i] + " ";
            }

            //remove last space from message
            message = message.substring(0, message.length() - 1);

            //update server and port
            server = user + c_.serverAddress;
            port = userPorts.get(user);
        }

        return validInput;


    }

    /**
     * start client code as provided from studres, creates a new connection
     * @param hostname ip
     * @param portnumber port
     * @return the created connection
     */
    static Socket startClient(String hostname, int portnumber) {

        Socket connection = null;

        try {
            String address;
            int port;

            address = hostname;
            port = portnumber;

            connection = new Socket(address, port); // make a socket

            System.out.println("\n++ Connecting to " + hostname + ":" + port
                    + " -> " + connection);

        } catch (UnknownHostException e) {

            System.err.println("UnknownHost Exception: " + hostname + " "
                    + e.getMessage());

        } catch (IOException e) {

            System.err.println("IO Exception: " + e.getMessage());
        }

        return connection;
    }

    /**
     * reads in the cs2003-usernames-2020.csv file into a hashmap
     * @return the hasmap of usernames and ports
     */
    static HashMap<String, Integer> readInCSV() {

        HashMap<String, Integer> userPorts = new HashMap<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader("CS2003-usernames-2020.csv"));

            String line = br.readLine();

            //read lines into hashmap, split on comma
            while ((line = br.readLine()) != null) {
                String[] lineData = line.split(",");
                userPorts.put(lineData[0], Integer.valueOf(lineData[1]));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return userPorts;
    }
}
