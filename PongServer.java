import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class PongServer {
    private static final int TCP_PORT = 59090;
    private static final int GAME_HEIGHT = 500;
    private static final int GAME_WIDTH = 500;
    private static int connectedPlayers = 0;
    private static int paddle1Y = 100; // Posição da paleta do jogador 1
    private static int paddle2Y = 100; // Posição da paleta do jogador 2
    private static int ballX = GAME_WIDTH / 2; // Posição X da bola
    private static int ballY = GAME_HEIGHT / 2; // Posição Y da bola
    private static int ballXSpeed = 2; // Velocidade da bola em X
    private static int ballYSpeed = 1; // Velocidade da bola em Y

    public static void main(String[] args) throws IOException {
        System.out.println("Pong Server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        new Thread(() -> handleTCPConnections(pool)).start();
        new Thread(() -> gameLoop()).start();
    }

    private static void handleTCPConnections(ExecutorService pool) {
        try (ServerSocket listener = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket socket = listener.accept();
                synchronized (PongServer.class) {
                    connectedPlayers++;
                }
                pool.execute(new PlayerHandler(socket, connectedPlayers));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gameLoop() {
        while (true) {
            try {
                Thread.sleep(20); // 50 FPS
                updateBallPosition();
                sendGameUpdate();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void updateBallPosition() {
        ballX += ballXSpeed;
        ballY += ballYSpeed;

        // Colisão com as paredes
        if (ballY <= 0 || ballY >= GAME_HEIGHT - 10) {
            ballYSpeed = -ballYSpeed; // Inverte a direção
        }

        // Colisão com as paletas
        if (ballX <= 20 && ballY >= paddle1Y && ballY <= paddle1Y + 60) {
            ballXSpeed = -ballXSpeed; // Bate na paleta do jogador 1
        }
        if (ballX >= GAME_WIDTH - 40 && ballY >= paddle2Y && ballY <= paddle2Y + 60) {
            ballXSpeed = -ballXSpeed; // Bate na paleta do jogador 2
        }

        // Reset ball if it goes out of bounds
        if (ballX < 0 || ballX > GAME_WIDTH) {
            ballX = GAME_WIDTH / 2;
            ballY = GAME_HEIGHT / 2;
        }
    }

    private static void sendGameUpdate() {
        synchronized (PongServer.class) {
            String update = String.format("UPDATE %d %d %d %d", ballX, ballY, paddle1Y, paddle2Y);
            // Aqui você enviaria a atualização para ambos os jogadores
            for (PlayerHandler player : PlayerHandler.players) {
                player.sendUpdate(update);
            }
        }
    }

    private static class PlayerHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        static final java.util.List<PlayerHandler> players = new java.util.ArrayList<>();
        private int playerId;

        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            players.add(this);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Confirma conexão ao jogador
                out.println("CONNECTED " + playerId);
                
                String command;
                while ((command = in.readLine()) != null) {
                    if (command.startsWith("MOVE")) {
                        String[] parts = command.split(" ");
                        int player = Integer.parseInt(parts[1]);
                        int newY = Integer.parseInt(parts[2]);

                        if (player == 1) {
                            paddle1Y = newY; // Atualiza a paleta do jogador 1
                        } else if (player == 2) {
                            paddle2Y = newY; // Atualiza a paleta do jogador 2
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Player disconnected");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                players.remove(this);
            }
        }

        public void sendUpdate(String update) {
            out.println(update);
        }
    }
}
