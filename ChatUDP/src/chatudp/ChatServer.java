package chatudp;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 8888;
    private DatagramSocket socket;
    private Map<String, ClientInfo> clients;
    private List<Message> messageHistory;
    private Map<String, Integer> messageCount;
    private boolean running;
    
    public ChatServer() throws SocketException {
        socket = new DatagramSocket(PORT);
        clients = new ConcurrentHashMap<>();
        messageHistory = new ArrayList<>();
        messageCount = new ConcurrentHashMap<>();
        running = false;
        
        // Thread para verificar clientes offline
        startHeartbeatChecker();
    }
    
    public void start() {
        running = true;
        System.out.println("Servidor iniciado na porta " + PORT);
        
        while (running) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String received = new String(packet.getData(), 0, packet.getLength());
                processMessage(received, packet.getAddress(), packet.getPort());
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro no servidor: " + e.getMessage());
                }
            }
        }
    }
    
    private void processMessage(String message, InetAddress address, int port) {
        String[] parts = message.split("\\|", 3);
        String command = parts[0];
        
        try {
            switch (command) {
                case "REGISTER":
                    handleRegister(parts[1], address, port);
                    break;
                case "MESSAGE":
                    handleMessage(parts[1], parts[2], address, port);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(parts[1], address, port);
                    break;
                case "GET_USERS":
                    sendUserList(address, port);
                    break;
                case "GET_MESSAGES":
                    sendMessageHistory(address, port);
                    break;
                case "GET_REPORT":
                    sendReport(address, port);
                    break;
                case "DISCONNECT":
                    handleDisconnect(parts[1]);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erro processando comando: " + e.getMessage());
        }
    }
    
    private void handleRegister(String username, InetAddress address, int port) {
        ClientInfo client = new ClientInfo(username, address, port);
        clients.put(username, client);
        messageCount.put(username, 0);
        
        System.out.println("Usuário registrado: " + username);
        
        // Notifica outros clientes sobre novo usuário
        broadcastUserList();
        
        // Envia confirmação
        sendMessage("REGISTER_OK", address, port);
    }
    
    private void handleMessage(String username, String messageText, InetAddress address, int port) {
        if (!clients.containsKey(username)) {
            sendMessage("ERROR|Usuário não registrado", address, port);
            return;
        }
        
        // Atualiza heartbeat do cliente
        clients.get(username).updateHeartbeat();
        
        // Cria mensagem
        Message msg = new Message(username, messageText);
        messageHistory.add(msg);
        
        // Atualiza contador de mensagens
        messageCount.put(username, messageCount.get(username) + 1);
        
        System.out.println("[" + msg.getTimestamp() + "] " + username + ": " + messageText);
        
        // Retransmite mensagem para todos os clientes online
        broadcastMessage(msg);
    }
    
    private void handleHeartbeat(String username, InetAddress address, int port) {
        ClientInfo client = clients.get(username);
        if (client != null) {
            client.updateHeartbeat();
        }
    }
    
    private void handleDisconnect(String username) {
        ClientInfo client = clients.get(username);
        if (client != null) {
            client.setOnline(false);
            System.out.println("Usuário desconectado: " + username);
            broadcastUserList();
        }
    }
    
    private void broadcastMessage(Message message) {
        String msgData = "NEW_MESSAGE|" + message.getUsername() + "|" + 
                        message.getText() + "|" + message.getTimestamp();
        
        for (ClientInfo client : clients.values()) {
            if (client.isOnline()) {
                sendMessage(msgData, client.getAddress(), client.getPort());
            }
        }
    }
    
    private void broadcastUserList() {
        StringBuilder userList = new StringBuilder("USER_LIST");
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            userList.append("|").append(entry.getKey())
                   .append(":").append(entry.getValue().isOnline() ? "online" : "offline");
        }
        
        for (ClientInfo client : clients.values()) {
            if (client.isOnline()) {
                sendMessage(userList.toString(), client.getAddress(), client.getPort());
            }
        }
    }
    
    private void sendUserList(InetAddress address, int port) {
        StringBuilder userList = new StringBuilder("USER_LIST");
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            userList.append("|").append(entry.getKey())
                   .append(":").append(entry.getValue().isOnline() ? "online" : "offline");
        }
        sendMessage(userList.toString(), address, port);
    }
    
    private void sendMessageHistory(InetAddress address, int port) {
        for (Message msg : messageHistory) {
            String msgData = "HISTORY_MESSAGE|" + msg.getUsername() + "|" + 
                           msg.getText() + "|" + msg.getTimestamp();
            sendMessage(msgData, address, port);
        }
        sendMessage("HISTORY_END", address, port);
    }
    
    private void sendReport(InetAddress address, int port) {
        StringBuilder report = new StringBuilder("REPORT");
        for (Map.Entry<String, Integer> entry : messageCount.entrySet()) {
            report.append("|").append(entry.getKey())
                  .append(":").append(entry.getValue());
        }
        sendMessage(report.toString(), address, port);
    }
    
    private void sendMessage(String message, InetAddress address, int port) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Erro enviando mensagem: " + e.getMessage());
        }
    }
    
    private void startHeartbeatChecker() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkOfflineClients();
            }
        }, 10000, 10000); // Verifica a cada 10 segundos
    }
    
    private void checkOfflineClients() {
        long now = System.currentTimeMillis();
        boolean userListChanged = false;
        
        for (ClientInfo client : clients.values()) {
            if (client.isOnline() && (now - client.getLastHeartbeat() > 15000)) {
                client.setOnline(false);
                userListChanged = true;
                System.out.println("Cliente offline detectado: " + client.getUsername());
            }
        }
        
        if (userListChanged) {
            broadcastUserList();
        }
    }
    
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer();
            
            // Adiciona hook para shutdown gracioso
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            server.start();
        } catch (SocketException e) {
            System.err.println("Erro iniciando servidor: " + e.getMessage());
        }
    }
}

// Classe auxiliar para informações do cliente
class ClientInfo {
    private String username;
    private InetAddress address;
    private int port;
    private long lastHeartbeat;
    private boolean online;
    
    public ClientInfo(String username, InetAddress address, int port) {
        this.username = username;
        this.address = address;
        this.port = port;
        this.lastHeartbeat = System.currentTimeMillis();
        this.online = true;
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.online = true;
    }
    
    // Getters e Setters
    public String getUsername() { return username; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}

// Classe auxiliar para mensagens
class Message {
    private String username;
    private String text;
    private String timestamp;
    
    public Message(String username, String text) {
        this.username = username;
        this.text = text;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
    
    // Getters
    public String getUsername() { return username; }
    public String getText() { return text; }
    public String getTimestamp() { return timestamp; }
}