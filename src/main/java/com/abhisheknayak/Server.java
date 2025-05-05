package com.abhisheknayak;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    public static List<PrintWriter> al = new ArrayList<>();
    public static void main(String[] args) {
        ServerOperations server = new Server1(8089, 20);

        try{
            server.start();
        }
        catch(Exception io){
            System.err.println("Error starting server");
        }
    }
}

interface ServerOperations{
    void start() throws Exception;
    void stop() throws Exception;
}

interface ServerMessageHandling{
    void receiveMessagefromClient() throws Exception;
}

class Server1 implements ServerOperations{

    private int port;
    private int maxConnections;
    private ServerSocket serversocket;
    private boolean isRunning;

    public Server1(int port, int maxConnections){
        this.port = port;
        this.maxConnections = maxConnections;
    }

    //overriding methods

    @Override
    public void start() throws Exception{
        System.out.println("Server signing on...");

        serversocket = new ServerSocket(port);

        isRunning = true;

        try{
            this.acceptConnections();
        }
        finally{
            this.stop();
        }

    }

    private void acceptConnections(){
        int connectionCount=0;

        while(isRunning && (connectionCount<maxConnections)){
            try{
                Socket socket = serversocket.accept();

                ServerMessageHandling handler = new ClientConversationHandler(socket);

                ConversationThread clientThread = new ConversationThread(handler);
                clientThread.start();
                connectionCount++;
            }
            catch(Exception io){
                System.err.println("Error accepting incoming client connection "+ io.getMessage());
            }
        }

        }

    @Override
    public void stop() throws Exception{
        isRunning = false;
        if((serversocket!=null) && !(serversocket.isClosed())){
            serversocket.close();
        }

        System.out.println("Server signing off...");
    }
}



class ClientConversationHandler implements ServerMessageHandling{

    private Socket soc;
    private PrintWriter writer;
    private BufferedReader reader;

    public ClientConversationHandler(Socket soc){
        this.soc = soc;
    }

    private synchronized void addingwritertoarraylist(PrintWriter writer){
        
        if(writer !=null){
        Server.al.add(writer);
        System.out.println("Connected clients " + Server.al.size());
    }
}

    private synchronized void removeWriterFromArrayList(PrintWriter writer){
        if(writer!=null && Server.al.contains(writer)){
        Server.al.remove(writer);
        System.out.println(" Client disconnected. Active clients: " + Server.al.size());
    }
}

    @Override
    public void receiveMessagefromClient(){

        try{
            reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(soc.getOutputStream())), true);
            processClientMessages(reader);
        }
        catch(Exception io){
            System.err.println("Error processing client messages " + io.getMessage());
        }
        finally{
            removeWriterFromArrayList(writer);
            closeSocket();
        }
    }

    private void processClientMessages(BufferedReader reader) throws Exception{
        String message;

        addingwritertoarraylist(writer);
        while((message = reader.readLine())!= null && ! (message.equals("End"))){
            System.out.println("Server received " + message);
            String responseMessage = message;
            System.out.println("server sending responseMessage " + responseMessage);

            synchronized(Server.al){
            for(PrintWriter obj : Server.al){
                obj.println(responseMessage);
            }  
        }
    }

        if(message != null && message.equals("End")){
            removeWriterFromArrayList(writer);
        }
    }

    private void closeSocket(){
        try{
            if((soc!=null) && !(soc.isClosed())){
                soc.close();
            }
        }
        catch(Exception io){
            System.err.println("Error closing socket " + io.getMessage());
        }
    }
}

class ConversationThread extends Thread{

    private ServerMessageHandling handler;

    public ConversationThread(ServerMessageHandling handler){
        this.handler = handler;
    }

    @Override
    public void run(){
        try{
            handler.receiveMessagefromClient();
        }
        catch(Exception io){
            System.err.println("Error handling client " + io.getMessage());
        }
    }
}
