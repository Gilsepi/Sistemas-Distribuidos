package chatudp;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class ChatClient extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private String username;
    private boolean connected = false;
    
    // Componentes da interface
    private JTextField usernameField;
    private JButton connectButton;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JButton reportButton;
    private JPanel connectionPanel;
    private JPanel chatPanel;
    
    // Timer para heartbeat
    private Timer heartbeatTimer;
    
    public ChatClient() {
        initializeGUI();
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_HOST);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro inicializando cliente: " + e.getMessage(), 
                "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // Thread para receber mensagens do servidor
        startMessageReceiver();
    }
    
    private void initializeGUI() {
        setTitle("Chat UDP - Cliente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Painel de conexão
        connectionPanel = createConnectionPanel();
        
        // Painel do chat
        chatPanel = createChatPanel();
        chatPanel.setVisible(false);
        
        // Layout principal
        setLayout(new CardLayout());
        add(connectionPanel, "connection");
        add(chatPanel, "chat");
        
        // Handler para fechar janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });
    }
    
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Título
        JLabel titleLabel = new JLabel("Conectar ao Chat");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(titleLabel, gbc);
        
        // Campo de usuário
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(new JLabel("Nome/Apelido:"), gbc);
        
        usernameField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(usernameField, gbc);
        
        // Botão conectar
        connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> connect());
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(connectButton, gbc);
        
        // Enter no campo de usuário conecta
        usernameField.addActionListener(e -> connect());
        
        return panel;
    }
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Área de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScroll.setBorder(new TitledBorder("Mensagens"));
        
        // Lista de usuários
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(new TitledBorder("Usuários Online"));
        
        // Painel de entrada de mensagem
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Painel de botões
        JPanel buttonPanel = new JPanel(new FlowLayout());
        reportButton = new JButton("Relatório de Frequência");
        reportButton.addActionListener(e -> requestReport());
        
        JButton disconnectButton = new JButton("Desconectar");
        disconnectButton.addActionListener(e -> disconnect());
        
        buttonPanel.add(reportButton);
        buttonPanel.add(disconnectButton);
        
        // Painel inferior
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Layout do painel do chat
        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(userScroll, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void connect() {
        String name = usernameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, insira um nome/apelido", 
                "Nome Obrigatório", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (name.contains("|") || name.contains(":")) {
            JOptionPane.showMessageDialog(this, 
                "Nome não pode conter os caracteres | ou :", 
                "Nome Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        username = name;
        
        // Envia registro para o servidor
        sendToServer("REGISTER|" + username);
        
        connectButton.setEnabled(false);
        usernameField.setEnabled(false);
    }
    
    private void onConnected() {
        connected = true;
        
        // Mostra painel do chat
        CardLayout layout = (CardLayout) getContentPane().getLayout();
        layout.show(getContentPane(), "chat");
        
        setTitle("Chat UDP - " + username);
        
        // Solicita histórico de mensagens
        sendToServer("GET_MESSAGES");
        
        // Solicita lista de usuários
        sendToServer("GET_USERS");
        
        // Inicia heartbeat
        startHeartbeat();
        
        // Foca no campo de mensagem
        messageField.requestFocus();
        
        appendToChat("=== Conectado ao chat! ===");
    }
    
    private void sendMessage() {
        if (!connected) return;
        
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;
        
        sendToServer("MESSAGE|" + username + "|" + message);
        messageField.setText("");
    }
    
    private void requestReport() {
        if (!connected) return;
        sendToServer("GET_REPORT");
    }
    
    private void disconnect() {
        if (connected) {
            sendToServer("DISCONNECT|" + username);
            connected = false;
        }
        
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        
        // Volta para tela de conexão
        CardLayout layout = (CardLayout) getContentPane().getLayout();
        layout.show(getContentPane(), "connection");
        
        connectButton.setEnabled(true);
        usernameField.setEnabled(true);
        chatArea.setText("");
        userListModel.clear();
        
        setTitle("Chat UDP - Cliente");
    }
    
    private void sendToServer(String message) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddress, SERVER_PORT);
            socket.send(packet);
        } catch (IOException e) {
            appendToChat("Erro enviando mensagem: " + e.getMessage());
        }
    }
    
    private void startMessageReceiver() {
        Thread receiver = new Thread(() -> {
            byte[] buffer = new byte[1024];
            
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    SwingUtilities.invokeLater(() -> processServerMessage(message));
                    
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        SwingUtilities.invokeLater(() -> 
                            appendToChat("Erro recebendo mensagem: " + e.getMessage()));
                    }
                    break;
                }
            }
        });
        receiver.setDaemon(true);
        receiver.start();
    }
    
    private void processServerMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];
        
        switch (command) {
            case "REGISTER_OK":
                onConnected();
                break;
                
            case "NEW_MESSAGE":
                if (parts.length >= 4) {
                    String sender = parts[1];
                    String text = parts[2];
                    String timestamp = parts[3];
                    appendToChat("[" + timestamp + "] " + sender + ": " + text);
                }
                break;
                
            case "HISTORY_MESSAGE":
                if (parts.length >= 4) {
                    String sender = parts[1];
                    String text = parts[2];
                    String timestamp = parts[3];
                    appendToChat("[" + timestamp + "] " + sender + ": " + text);
                }
                break;
                
            case "HISTORY_END":
                appendToChat("=== Fim do histórico ===");
                break;
                
            case "USER_LIST":
                updateUserList(parts);
                break;
                
            case "REPORT":
                showReport(parts);
                break;
                
            case "ERROR":
                if (parts.length > 1) {
                    JOptionPane.showMessageDialog(this, 
                        parts[1], "Erro do Servidor", JOptionPane.ERROR_MESSAGE);
                    disconnect();
                }
                break;
        }
    }
    
    private void updateUserList(String[] parts) {
        userListModel.clear();
        for (int i = 1; i < parts.length; i++) {
            String[] userInfo = parts[i].split(":");
            if (userInfo.length == 2) {
                String user = userInfo[0];
                String status = userInfo[1];
                userListModel.addElement(user + (status.equals("online") ? " (online)" : " (offline)"));
            }
        }
    }
    
    private void showReport(String[] parts) {
        StringBuilder report = new StringBuilder("Relatório de Frequência de Mensagens:\n\n");
        
        for (int i = 1; i < parts.length; i++) {
            String[] userInfo = parts[i].split(":");
            if (userInfo.length == 2) {
                String user = userInfo[0];
                String count = userInfo[1];
                report.append(user).append(": ").append(count).append(" mensagens\n");
            }
        }
        
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Relatório de Frequência", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void startHeartbeat() {
        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connected) {
                    sendToServer("HEARTBEAT|" + username);
                }
            }
        }, 5000, 5000); // A cada 5 segundos
    }
    
    private void appendToChat(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Ignora erro de Look and Feel
            }
            
            new ChatClient().setVisible(true);
        });
    }
 }

