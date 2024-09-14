package org.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable{
    private final Socket socket;
    public final static ArrayList<ClientManager> clients = new ArrayList<>();
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String name;


    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: "+ name+ " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()){
            try {
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
                break;
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClient();
        try {
            if (socket != null){
                socket.close();
            }
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    private void broadcastMessage(String message) {
        String name1 = null;
        String mes = null;

        String trimmedMessage = message.replaceFirst("^[^:]+: ", "");

        // Если сообщение начинается с '@', обрабатываем как личное сообщение
        if (trimmedMessage.startsWith("@")) {
            // Разделяем на имя и сообщение
            String[] parts = trimmedMessage.substring(1).split(" ", 2);

            if (parts.length == 2) {
                name1 = parts[0].trim();
                mes = parts[1].trim();

            }

            boolean recipientFound = false;  // Флаг для проверки, найден ли получатель

            // Ищем клиента по имени
            for (ClientManager client : clients) {
                if (client.name.trim().equals(name1)) {
                    recipientFound = true;
                    try {
                        // Отправляем сообщение только конкретному клиенту
                        client.bufferedWriter.write(this.name+ ": "+ mes);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    } catch (IOException e) {
                        closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);  // Закрываем ресурсы этого клиента
                    }
                }
            }

            // Если получатель не найден, можно добавить уведомление отправителю
            if (!recipientFound) {
                System.out.println("Client with name " + name1 + " not found.");
            }

        } else {
            for (ClientManager client : clients) {
                if (!client.name.trim().equals(this.name.trim())) {
                    try {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    } catch (IOException e) {
                        closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
                    }
                }
            }
        }
    }
    }

