package ElectronicAssetTradingPlatform.Server;

import ElectronicAssetTradingPlatform.AssetTrading.BuyOffer;
import ElectronicAssetTradingPlatform.AssetTrading.BuyOfferData;
import ElectronicAssetTradingPlatform.AssetTrading.SellOffer;
import ElectronicAssetTradingPlatform.AssetTrading.SellOfferData;
import ElectronicAssetTradingPlatform.Database.DBConnectivity;
import ElectronicAssetTradingPlatform.Database.MarketplaceDataSource;
import ElectronicAssetTradingPlatform.Database.UsersDataSource;
import ElectronicAssetTradingPlatform.Users.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkServer {
    private static final int PORT = 10000;
    /**
     * this is the timeout inbetween accepting clients, not reading from the socket itself.
     */
    private static final int SOCKET_ACCEPT_TIMEOUT = 100;

    /**
     * This timeout is used for the actual clients.
     */
    private static final int SOCKET_READ_TIMEOUT = 5000;

    private AtomicBoolean running = new AtomicBoolean(true);

    /**
     * The connection to the database where everything is stored.
     */
    private Connection database;


    // Exception codes: https://sqlite.org/rescode.html
    private static final int UNIQUE_CONSTRAINT_EXCEPTION_CODE = 19;

    /**
     * Returns the port the server is configured to use
     *
     * @return The port number
     */
    public static int getPort() {
        return PORT;
    }

    /**
     * Starts the server running on the default port
     */
    public void start() throws IOException {
        // Connect to the database
        database = DBConnectivity.getInstance();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setSoTimeout(SOCKET_ACCEPT_TIMEOUT);
            // The server is running
            while (running.get()) {
                try {
                    final Socket socket = serverSocket.accept();
                    socket.setSoTimeout(SOCKET_READ_TIMEOUT);

                    // We have a new connection from a client. Use a runnable and thread to handle
                    // the client. The lambda here wraps the functional interface (Runnable).
                    final Thread clientThread = new Thread(() -> handleConnection(socket));
                    clientThread.start();
                } catch (SocketTimeoutException ignored) {
                    // Do nothing. A timeout is normal- we just want the socket to
                    // occasionally timeout so we can check if the server is still running
                } catch (Exception e) {
                    // We will report other exceptions by printing the stack trace, but we
                    // will not shut down the networkExercise.server. A exception can happen due to a
                    // client malfunction (or malicious client)
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // If we get an error starting up, show an error dialog then exit
            e.printStackTrace();
            System.exit(1);
        }

        // The server is no longer running

        // Close down the server and db
        try {
            DBConnectivity.getInstance().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * Handles the connection received from ServerSocket
     * @param socket The socket used to communicate with the currently connected client
     */
    private void handleConnection(Socket socket) {
        try {

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                try {
                    // Reads Command and parameter object
                    NetworkCommands command = (NetworkCommands) objectInputStream.readObject();
                    if (command == null) {
                    }
                    handleCommand(command, objectInputStream, objectOutputStream, socket);
                } catch (SocketTimeoutException e) {
                    /**
                     * We catch SocketTimeoutExceptions, because that just means the client hasn't sent
                     * any new requests. We don't want to disconnect them otherwise. Another way to
                     * check if they're "still there would be with ping/pong commands.
                     */
                    continue;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (e.getErrorCode() == UNIQUE_CONSTRAINT_EXCEPTION_CODE)
                        objectOutputStream.writeObject("It already exists.");
                    else objectOutputStream.writeObject("It could not be found: " + e.getMessage());
                } catch (User.UserTypeException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | ClassCastException | ClassNotFoundException e) {
            System.out.println(String.format("Connection %s closed", socket.toString()));
        }
    }

    /**
     * Execute the command
     * @param command NetworkCommand from input stream
     * @param socket socket for the client
     * @param objectInputStream input stream to read objects from
     * @param objectOutputStream output stream to write objects to
     * @throws IOException if the client has disconnected
     * @throws ClassNotFoundException if the client sends an invalid object
     * @throws User.UserTypeException if the user is an invalid user type for the specified command
     */
    private void handleCommand (NetworkCommands command, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
            throws IOException, ClassNotFoundException, SQLException, User.UserTypeException {
        /*
         * Remember this is happening on separate threads for each client. Therefore access to the database
         * must be thread-safe in some way. The easiest way to achieve thread safety is to just put a giant
         * lock around all database operations, in this case with a synchronized block on the database object.
         */
        switch (command) {
            // Execute db queries based on command
            case RETRIEVE_USER -> {
                // Get input
                String username = (String) objectInputStream.readObject();

                User out;
                synchronized (database) {
                    out = UsersDataSource.getInstance().getUser(username);

                    // Write output
                    objectOutputStream.writeObject(out);
                    System.out.println("Wrote to socket: " + socket.toString() + out);
                }
            }
            case STORE_USER -> {
                // Get input
                User user = (User) objectInputStream.readObject();

                synchronized (database) {
                    // Save to db
                    UsersDataSource.getInstance().insertUser(user);

                    // Write success output
                    objectOutputStream.writeObject("Added user.");
                    System.out.println("Wrote to socket: " + socket.toString());
                }
            }
            case EDIT_USER -> {
                // Get input
                String[] editedValues = (String[]) objectInputStream.readObject();

                synchronized (database) {
                    // Save to db
                    UsersDataSource.getInstance().editUser(editedValues[0], editedValues[1], editedValues[2]);

                    // Write success output
                    objectOutputStream.writeObject("Edited user.");
                    System.out.println("Wrote to socket: " + socket.toString());
                    System.out.println("Wrote to socket: " + socket.toString());
                }
            }
            case ADD_BUY_OFFER -> {
                // Get input
                BuyOffer offer =  (BuyOffer) objectInputStream.readObject();
                synchronized (database) {
                    // add offer to the DB
                    MarketplaceDataSource.getInstance().insertBuyOffer(offer);
                    // write success output
                    objectOutputStream.writeObject("Added buy offer: " + offer);
                    System.out.println("Wrote to socket: " + socket.toString());

                }

            }
            case ADD_SELL_OFFER -> {
                SellOffer offer = (SellOffer) objectInputStream.readObject();
                synchronized (database) {
                    // add offer to the DB
                    MarketplaceDataSource.getInstance().insertSellOffer(offer);
                    // write success output
                    objectOutputStream.writeObject("Added buy offer: " + offer);
                    System.out.println("Wrote to socket: " + socket.toString());

                }
            }
            case REMOVE_OFFER -> {
                int ID = (int) objectInputStream.readObject();
                synchronized (database) {
                    // remove offer from DB
                    MarketplaceDataSource.getInstance().removeOffer(ID);
                    objectOutputStream.writeObject("Removed offer: #" + ID);
                }
                System.out.println("Wrote to socket:" + socket.toString());
            }
            case GET_BUY_OFFERS -> {
                synchronized (database) {
                    objectOutputStream.writeObject(MarketplaceDataSource.getInstance().getBuyOffers());
                }
                objectOutputStream.flush();
                System.out.println("Retrieved by buy offers and sent to client" + socket.toString());
            }
            case GET_SELL_OFFERS -> {
                synchronized (database) {
                    objectOutputStream.writeObject(MarketplaceDataSource.getInstance().getSellOffers());
                }
                objectOutputStream.flush();
                System.out.println("Retrieved by sell offers and sent to client" + socket.toString());
            }
            case GET_PLACED_OFFER -> {
                synchronized (database) {
                    objectOutputStream.writeObject(MarketplaceDataSource.getInstance().getPlacedOfferID());
                }
                objectOutputStream.flush();
                System.out.println("Retrieved placed offer ID and sent to client");
            }
            case UPDATE_OFFER -> {
                int newQuantity = (int) objectInputStream.readObject();
                System.out.println("New quantity:" + newQuantity);
                int ID = (int) objectInputStream.readObject();
                System.out.println("ID:" + ID);
                synchronized (database) {
                    MarketplaceDataSource.getInstance().updateOfferQuantity(newQuantity ,ID);
                }
                System.out.println("Updated offer quantity on behalf of client");
            }
            case EDIT_PASSWORD -> {
                // Get input
                String[] editedValues = (String[]) objectInputStream.readObject();

                synchronized (database) {
                    // Save to db
                    UsersDataSource.getInstance().editUserPassword(editedValues[0], editedValues[1], editedValues[2]);

                    // Write success output
                    objectOutputStream.writeObject("Password has changed.");
                    System.out.println("Wrote to socket: " + socket.toString());
                }
            }
        }
    }

    /**
     * Requests the server to shut down
     */
    public void shutdown() {
        // Shut the server down
        running.set(false);
    }

}
