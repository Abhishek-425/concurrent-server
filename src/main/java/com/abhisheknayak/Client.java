package com.abhisheknayak;


import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class Client {
    public static void main(String[] args) {
        try{

            ConnectionConfig config = new EndPoint("127.0.0.1", 8089);
            ClientMessageHandler messageHandler = new NetworkMessageHandler(config);
            GUI window = new ChatWindow(messageHandler);
            ControlReceivedMessage controller = new ControlReceivedMessage(window, messageHandler);
            controller.processReceivedMessages();
        }
        catch(Exception io){
            System.err.println("Error happened in main " + io.getMessage());
        }

    }
}

//interface for GUI logic 
interface GUI{
    String getTextField();
    void clearInput();
    void displayMessage(String message);

}

//interface for ip and port

interface ConnectionConfig{

    String getIP();
    int getPort();
}


interface ClientMessageHandler{
    String receiveMessage() throws Exception;
    void sendMessage(String message);
    void closeConnection();
}

//implemting ConnectionConfig
class EndPoint implements ConnectionConfig{

    private String ip;
    private int port;

    public EndPoint(String IP, int Port){
        this.ip = IP;
        this.port = Port;
    }

    //implementing getIP and getPort

    @Override
    public String getIP(){
        return ip;
    }

    @Override
    public int getPort(){
        return port;
    }
}


class ChatWindow extends JFrame implements GUI{
    private JTextArea textArea;
    private JTextField textField;
    private JButton sendButton;
    private ClientMessageHandler messageHandler;

    public ChatWindow(ClientMessageHandler messageHandler){
        this.messageHandler = messageHandler;
        textArea = new JTextArea(20,30);
        textField = new JTextField(20);
        sendButton = new JButton("Send");

        setupGUI();
        setupListeners();
    }

    private void setupGUI(){
        JPanel inputPanel = new JPanel();
        this.add(inputPanel, BorderLayout.SOUTH);
        inputPanel.add(textField);
        inputPanel.add(sendButton);

        add(new JScrollPane(textArea), BorderLayout.CENTER);        
        textArea.setEditable(false);
        setSize(400,400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }


    private void setupListeners(){

        ActionListener messageListener = new  ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                handleMessaging();
            }
        };

        sendButton.addActionListener(messageListener);
        textField.addActionListener(messageListener);
    }

    private void handleMessaging(){

        String message = getTextField();

        if(!(message.isEmpty())){
            messageHandler.sendMessage(message);
            clearInput();

            if(message.equals("End")){
                messageHandler.closeConnection();
                System.exit(1);
            }
        }
    }

    @Override
    public String getTextField(){
        return textField.getText();
    }

    @Override
    public void clearInput(){
        textField.setText("");
    }

    @Override
    public void displayMessage(String message){
        textArea.append(message + "\n");
    }
}

class NetworkMessageHandler implements ClientMessageHandler{

    private Socket soc;
    private PrintWriter writer;
    private BufferedReader reader;

    public NetworkMessageHandler(ConnectionConfig config) throws Exception{
        this.soc = new Socket(config.getIP(), config.getPort());
        this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(soc.getOutputStream())), true);
        this.reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
    }

    @Override
    public void sendMessage(String message){
        System.out.println("Client sending message " + message);
        writer.println(message);
        writer.flush();
    }

    @Override
    public String receiveMessage() throws Exception{
        String message = reader.readLine();
        System.out.println("Client received raw message " + message);
        return message;
    }

    @Override
    public void closeConnection(){
        try{
            soc.close();
            writer.close();
            reader.close();
        }
        catch(Exception io){
            System.err.println("Exception occurred closing connection " + io.getMessage());
        }
    }
}

class ControlReceivedMessage{

    private GUI gui;
    private ClientMessageHandler messageHandler;

    public ControlReceivedMessage(GUI gui, ClientMessageHandler messageHandler){
        this.gui = gui;
        this.messageHandler = messageHandler;
    }

    public void processReceivedMessages() throws Exception{
        String message;

        while(!(message = messageHandler.receiveMessage()).equals("End")){
            System.out.println("Client received " + message);
            gui.displayMessage(message);
        }
        messageHandler.closeConnection();
        gui.displayMessage("Client signing off");
    }
}
