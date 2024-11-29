import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PongClient {
    private static final int WIDTH = 500, HEIGHT = 500;
    private PongService service;
    private JFrame frame;
    private PongPanel panel;
    private int paddleY = 100; // Posição da paleta
    private int ballX = 150, ballY = 100;
    private int playerId;
    private int paddle1Y = 100, paddle2Y = 100;
    private int scorePlayer1 = 0, scorePlayer2 = 0;

    public static void main(String[] args) {
        try {
            String serverIP = JOptionPane.showInputDialog("Digite o IP do servidor:");
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            PongService service = (PongService) registry.lookup("PongService");
            new PongClient(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PongClient(PongService service) {
        this.service = service;

        try {
            playerId = service.connect(); // Conecta o jogador
            System.out.println("Conectado como jogador " + playerId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        frame = new JFrame("Pong Client");
        panel = new PongPanel();
        frame.add(panel);
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        setupControls();
        startGameLoop();
    }

    private void setupControls() {
        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                try {
                    if (playerId == 1) {
                        if (e.getKeyCode() == KeyEvent.VK_W && paddleY > 0) {
                            paddleY -= 10;
                            service.movePaddle(playerId, paddleY); // Move a paleta do jogador
                        }
                        if (e.getKeyCode() == KeyEvent.VK_S && paddleY < HEIGHT - 60) {
                            paddleY += 10;
                            service.movePaddle(playerId, paddleY); // Move a paleta do jogador
                        }
                    } else if (playerId == 2) {
                        if (e.getKeyCode() == KeyEvent.VK_UP && paddleY > 0) {
                            paddleY -= 10;
                            service.movePaddle(playerId, paddleY); // Move a paleta do jogador
                        }
                        if (e.getKeyCode() == KeyEvent.VK_DOWN && paddleY < HEIGHT - 60) {
                            paddleY += 10;
                            service.movePaddle(playerId, paddleY); // Move a paleta do jogador
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        panel.setFocusable(true);
        panel.requestFocusInWindow();
    }

    private void startGameLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    String gameState = service.getGameState(); // Obtém o estado do jogo
                    processGameState(gameState);
                    panel.repaint();
                    Thread.sleep(20); // Atualiza a cada 20ms
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processGameState(String gameState) {
        String[] data = gameState.split(" ");
        ballX = Integer.parseInt(data[0]);
        ballY = Integer.parseInt(data[1]);
        paddle1Y = Integer.parseInt(data[2]);
        paddle2Y = Integer.parseInt(data[3]);
        scorePlayer1 = Integer.parseInt(data[4]);
        scorePlayer2 = Integer.parseInt(data[5]);
    }

    private class PongPanel extends JPanel {
        private final LinkedList<int[]> ballTrail = new LinkedList<>(); // Lista para armazenar a trilha da bola
        private static final int TRAIL_SIZE = 10; // Número de posições anteriores para criar o efeito de rastro

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);


            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Definir as cores neon
            Color neonBlueDark = new Color(0, 0, 60); // Azul escuro neon
            Color neonRedDark = new Color(139, 0, 0); // Vermelho escuro
            Color neonBlack = new Color(30, 30, 30); // Preto neon leve
            Color neonBlue = new Color(0, 70, 180); // Azul claro neon

            // Adicionar a posição atual da bola à lista de trilha
            if (ballTrail.size() >= TRAIL_SIZE) {
                ballTrail.removeFirst(); // Remove o ponto mais antigo
            }
            ballTrail.add(new int[]{ballX, ballY});

            // Desenhar a trilha da bola
            for (int i = 0; i < ballTrail.size(); i++) {
                int[] pos = ballTrail.get(i);
                float alpha = (float) i / TRAIL_SIZE; // Transição de opacidade do início ao fim
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(neonBlue);
                g2d.fillOval(pos[0], pos[1], 10, 10);
            }
            
            // Restaura a opacidade para o restante dos elementos
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // Desenhar as paletas em preto neon
            g2d.setColor(neonBlack);
            g2d.fillRect(10, paddle1Y, 10, 60);
            g2d.fillRect(464, paddle2Y, 10, 60);

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
