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

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog("Enter Server IP:");
        String[] options = {"TCP", "UDP"};
        int protocolChoice = JOptionPane.showOptionDialog(null, "Choose Protocol", "Protocol Selection",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        boolean isUDP = protocolChoice == 1;
        
        try {
            new PongClient(serverIP, isUDP);
        } catch (IOException e) {
            e.printStackTrace();
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
        } else {
            Socket socket = new Socket(serverIP, 59090);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startTCPReceiver(in);
        }

        setupControls();
    }

    private void setupControls() {
        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                try {
                    if (playerId == 1) {
                        if (e.getKeyCode() == KeyEvent.VK_W && paddle1Y > 0) {
                            paddle1Y -= 5;
                            sendMessage("MOVE 1 " + paddle1Y);
                        }
                        if (e.getKeyCode() == KeyEvent.VK_S && paddle1Y < HEIGHT - 60) {
                            paddle1Y += 5;
                            sendMessage("MOVE 1 " + paddle1Y);
                        }
                    } else if (playerId == 2) {
                        if (e.getKeyCode() == KeyEvent.VK_UP && paddle2Y > 0) {
                            paddle2Y -= 5;
                            sendMessage("MOVE 2 " + paddle2Y);
                        }
                        if (e.getKeyCode() == KeyEvent.VK_DOWN && paddle2Y < HEIGHT - 60) {
                            paddle2Y += 5;
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
            sendUDPMessage(message);
        } else {
            out.println(message);
        }
    }

    private void sendUDPMessage(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, 59090);
        udpSocket.send(packet);
    }

    private void startTCPReceiver(BufferedReader in) {
        new Thread(() -> {
            try {
                String response = in.readLine();
                if (response.startsWith("CONNECTED")) {
                    playerId = Integer.parseInt(response.split(" ")[1]);
                }
                while (true) {
                    response = in.readLine();
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
            panel.repaint();
        }
    }

    private class PongPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Desenhar as paletas
            g.fillRect(10, paddle1Y, 10, 60);
            g.fillRect(470, paddle2Y, 10, 60);
            
            // Desenhar a bola
            g.fillRect(ballX, ballY, 10, 10);
            
            // Desenhar linha tracejada no centro da tela
            g.setColor(Color.BLACK); // Cor preta para os retângulos
            for (int i = 1; i < 500; i += 20) { // Alterne a cada 20 pixels       
                g.fillRect(250, i, 5, 10);
            }
    
            // Desenhar retângulos marcando as bordas superior e inferior
            g.setColor(Color.BLACK); // Cor preta para os retângulos
            g.fillRect(0, 452, 500, 10); // Retângulo na parte inferior
            g.fillRect(0, 1,500, 10); // Retângulo na parte superior
        }
    }
    
    
}
