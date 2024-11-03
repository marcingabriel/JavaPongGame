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

    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog("Enter Server IP:");
        try {
            new PongClient(serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PongClient(String serverAddress) throws IOException {
        frame = new JFrame("Pong Client");
        panel = new PongPanel();
        frame.add(panel);
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Socket socket = new Socket(serverAddress, 59090);
        out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Thread para receber atualizações do servidor
        new Thread(() -> {
            try {
                String response = in.readLine();
                if (response.startsWith("CONNECTED")) {
                    playerId = Integer.parseInt(response.split(" ")[1]);
                }

                while (true) {
                    response = in.readLine();
                    if (response.startsWith("UPDATE")) {
                        String[] data = response.split(" ");
                        ballX = Integer.parseInt(data[1]);
                        ballY = Integer.parseInt(data[2]);
                        paddle1Y = Integer.parseInt(data[3]);
                        paddle2Y = Integer.parseInt(data[4]);
                        panel.repaint();
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server");
            }
        }).start();

        // Controles para movimentação das paletas
        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (playerId == 1) {
                    if (e.getKeyCode() == KeyEvent.VK_W && paddle1Y > 0) {
                        paddle1Y -= 5; // Move paleta do jogador 1 para cima
                        out.println("MOVE 1 " + paddle1Y);
                    }
                    if (e.getKeyCode() == KeyEvent.VK_S && paddle1Y < HEIGHT - 60) {
                        paddle1Y += 5; // Move paleta do jogador 1 para baixo
                        out.println("MOVE 1 " + paddle1Y);
                    }
                } else if (playerId == 2) {
                    if (e.getKeyCode() == KeyEvent.VK_UP && paddle2Y > 0) {
                        paddle2Y -= 5; // Move paleta do jogador 2 para cima
                        out.println("MOVE 2 " + paddle2Y);
                    }
                    if (e.getKeyCode() == KeyEvent.VK_DOWN && paddle2Y < HEIGHT - 60) {
                        paddle2Y += 5; // Move paleta do jogador 2 para baixo
                        out.println("MOVE 2 " + paddle2Y);
                    }
                }
            }
        });
        panel.setFocusable(true);
        panel.requestFocusInWindow(); // Garante que o painel tenha o foco para capturar as teclas
    }

    // Painel onde o jogo é desenhado
    private class PongPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Desenha a paleta do jogador 1 à esquerda
            g.fillRect(10, paddle1Y, 10, 60);
            // Desenha a paleta do jogador 2 à direita
            g.fillRect(470, paddle2Y, 10, 60);
            // Desenha a bola
            g.fillRect(ballX, ballY, 10, 10);
        }
    }
}
