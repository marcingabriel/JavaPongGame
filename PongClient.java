import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class PongClient {
    private static final int WIDTH = 500, HEIGHT = 500;
    private JFrame frame;
    private PongPanel panel;
    private int paddle1Y = 100; // Posição da paleta do jogador 1
    private int paddle2Y = 100; // Posição da paleta do jogador 2
    private int ballX = 150, ballY = 100;
    private int playerId; // ID do jogador
    private PrintWriter out;
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private boolean isUDP;
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;


    public static void main(String[] args) {
        // Painel para a entrada de IP e escolha do protocolo
        JPanel panel = new JPanel(new GridLayout(3, 1));
        
        JTextField ipField = new JTextField();
        panel.add(new JLabel("Enter Server IP:"));
        panel.add(ipField);
        
        JRadioButton tcpButton = new JRadioButton("TCP", true);
        JRadioButton udpButton = new JRadioButton("UDP");
        
        ButtonGroup group = new ButtonGroup();
        group.add(tcpButton);
        group.add(udpButton);
        
        JPanel protocolPanel = new JPanel();
        protocolPanel.add(tcpButton);
        protocolPanel.add(udpButton);
        
        panel.add(protocolPanel);

        // Exibe o diálogo de entrada
        int result = JOptionPane.showConfirmDialog(null, panel, "Enter Server IP and Choose Protocol",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String serverIP = ipField.getText();
            boolean isUDP = udpButton.isSelected();
            
            try {
                new PongClient(serverIP, isUDP);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public PongClient(String serverIP, boolean isUDP) throws IOException {
        this.isUDP = isUDP;
        frame = new JFrame("Pong Client");
        panel = new PongPanel();
        frame.add(panel);
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        serverAddress = InetAddress.getByName(serverIP);
    
        if (isUDP) {
            udpSocket = new DatagramSocket();
            sendUDPMessage("CONNECT");
            startUDPReceiver();  // Inicializa a thread para recepção UDP
        } else {
            Socket socket = new Socket(serverIP, 59090);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startTCPReceiver(in);
        }
    
        setupControls();
    }
    
    private void startUDPReceiver() {
        new Thread(() -> {
            byte[] buffer = new byte[256];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    
                    String response = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Recebido via UDP: " + response); // Log para verificar a recepção
    
                    if (response.startsWith("CONNECTED")) {
                        playerId = Integer.parseInt(response.split(" ")[1]);
                    } else if (response.startsWith("UPDATE")) {
                        processUpdate(response);
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }
    

    private void setupControls() {
        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                try {
                    if (playerId == 1) {
                        if (e.getKeyCode() == KeyEvent.VK_W && paddle1Y > 0) {
                            paddle1Y -= 10;
                            sendMessage("MOVE 1 " + paddle1Y);
                        }
                        if (e.getKeyCode() == KeyEvent.VK_S && paddle1Y < HEIGHT - 60) {
                            paddle1Y += 10;
                            sendMessage("MOVE 1 " + paddle1Y);
                        }
                    } else if (playerId == 2) {
                        if (e.getKeyCode() == KeyEvent.VK_UP && paddle2Y > 0) {
                            paddle2Y -= 10;
                            sendMessage("MOVE 2 " + paddle2Y);
                        }
                        if (e.getKeyCode() == KeyEvent.VK_DOWN && paddle2Y < HEIGHT - 60) {
                            paddle2Y += 10;
                            sendMessage("MOVE 2 " + paddle2Y);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        panel.setFocusable(true);
        panel.requestFocusInWindow();
        
    }
    

    private void sendMessage(String message) throws IOException {
        if (isUDP) {
            System.out.println("Movimento enviado via UDP: " + message);
            sendUDPMessage(message);
        } else {
            out.println(message);
        }
    }

    private void sendUDPMessage(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, 59091);
        udpSocket.send(packet);
    }

    private void startTCPReceiver(BufferedReader in) {
        new Thread(() -> {
            try {
                String response = in.readLine();
                if (response != null && response.startsWith("CONNECTED")) {
                    playerId = Integer.parseInt(response.split(" ")[1]);
                }
                while (true) {
                    response = in.readLine();
                    if (response == null) {
                        System.out.println("Server connection lost.");
                        break; // Saia do loop se a resposta for nula
                    }
                    processUpdate(response);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server");
            }
        }).start();
    }
    

    private void processUpdate(String update) {
        if (update.startsWith("UPDATE")) {
            String[] data = update.split(" ");
            ballX = Integer.parseInt(data[1]);
            ballY = Integer.parseInt(data[2]);
            paddle1Y = Integer.parseInt(data[3]);
            paddle2Y = Integer.parseInt(data[4]);
            scorePlayer1 = Integer.parseInt(data[5]);
            scorePlayer2 = Integer.parseInt(data[6]);
            
            panel.repaint();  // Atualiza a tela após as mudanças
        }
    }
    

    private class PongPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
    
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            // Definir as cores neon
            Color neonBlueDark = new Color(0, 0, 60); // Azul escuro neon
            Color neonRedDark = new Color(139, 0, 0); // Vermelho escuro
            Color neonBlack = new Color(30, 30, 30); // Preto neon leve
            Color neonBlue = new Color(0, 70, 180); // Azul claro neon
    
            // Desenhar o brilho neon para as paletas em preto neon
            g2d.setColor(new Color(30, 30, 30, 100)); // Preto com transparência para brilho suave
            for (int i = 0; i < 5; i++) { // 5 camadas para o brilho
                g2d.fillRect(10 - i, paddle1Y - i, 10 + 2 * i, 60 + 2 * i);
                g2d.fillRect(464 - i, paddle2Y - i, 10 + 2 * i, 60 + 2 * i);
            }
    
            // Desenhar as paletas em preto neon
            g2d.setColor(neonBlack);
            g2d.fillRect(10, paddle1Y, 10, 60);
            g2d.fillRect(464, paddle2Y, 10, 60);
    
   
           // Desenhar a bola
           g2d.setColor(neonBlue);
           g.fillRect(ballX, ballY, 10, 10);
    
            // Desenhar a bola em azul neon
            g2d.setColor(neonBlue);
            g2d.fillOval(ballX, ballY, 10, 10);
    
            // Desenhar linha tracejada no centro da tela em azul escuro neon
            g2d.setColor(neonBlack);
            for (int i = 1; i < 500; i += 20) { // Alterne a cada 20 pixels
                g2d.fillRect(250, i, 5, 10);
            }
    
            // Desenhar retângulos marcando as bordas superior e inferior em azul escuro neon
            g2d.setColor(neonBlack);
            g2d.fillRect(0, 452, 500, 10); // Retângulo na parte inferior
            g2d.fillRect(0, 1, 500, 10); // Retângulo na parte superior
    
            // Exibir pontuação dos jogadores com vermelho escuro e fonte maior
            g2d.setColor(neonRedDark);
            g2d.setFont(new Font("Arial", Font.BOLD, 48)); // Tamanho de fonte ajustado para 48
            g2d.drawString(String.valueOf(scorePlayer1), 150, 50); // Posição para o jogador 1
            g2d.drawString(String.valueOf(scorePlayer2), 320, 50); // Posição para o jogador 2
        }
    }

    
    
}
