import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class PongServer {
    private static final int TCP_PORT = 59090;
    private static final int UDP_PORT = 59091;
    private static final int GAME_HEIGHT = 500;
    private static final int GAME_WIDTH = 500;
    private static int connectedPlayers = 0;
    private static int paddle1Y = 100; // Posição da paleta do jogador 1
    private static int paddle2Y = 100; // Posição da paleta do jogador 2
    private static int ballX = GAME_WIDTH / 2; // Posição X da bola
    private static int ballY = GAME_HEIGHT / 2; // Posição Y da bola
    private static int ballXSpeed = 2; // Velocidade da bola em X
    private static int ballYSpeed = 1; // Velocidade da bola em Y
    // Pontuação
    private static int scorePlayer1 = 0;
    private static int scorePlayer2 = 0;

    private static DatagramSocket udpSocket;


    public static void main(String[] args) throws IOException {
        System.out.println("Pong Server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(2);
    
        // Inicializa o udpSocket antes de iniciar a thread
        udpSocket = new DatagramSocket(UDP_PORT);
    
        new Thread(() -> handleTCPConnections(pool)).start();
        new Thread(() -> handleUDPConnections()).start();
        new Thread(() -> gameLoop()).start();
    }
    

    private static void handleTCPConnections(ExecutorService pool) {
        try (ServerSocket listener = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket socket = listener.accept();
                synchronized (PongServer.class) {
                    if (connectedPlayers < 2) { // Limitar a 2 jogadores
                        connectedPlayers++;
                        pool.execute(new PlayerHandler(socket, connectedPlayers));
                    } else {
                        // Rejeitar conexão se já houver 2 jogadores conectados
                        socket.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void handleUDPConnections() {
        byte[] buffer = new byte[256];
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
    
                if (message.startsWith("CONNECT")) {
                    synchronized (PongServer.class) {
                        connectedPlayers++;
                        // Armazenar endereço e porta do cliente para envio de atualizações
                        PlayerHandler.udpClients.add(new InetSocketAddress(packet.getAddress(), packet.getPort()));
                    }
                    String connectResponse = "CONNECTED " + connectedPlayers;
                    DatagramPacket responsePacket = new DatagramPacket(
                        connectResponse.getBytes(),
                        connectResponse.length(),
                        packet.getAddress(),
                        packet.getPort()
                    );
                    udpSocket.send(responsePacket);
                } else if (message.startsWith("MOVE")) {
                    System.out.println("Movimento recebido via UDP: " + message);
                    String[] parts = message.split(" ");
                    int player = Integer.parseInt(parts[1]);
                    int newY = Integer.parseInt(parts[2]);
                
                    if (player == 1) {
                        paddle1Y = newY;
                    } else if (player == 2) {
                        paddle2Y = newY;
                    }
                }
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

        if (ballY <= 5 || ballY >= GAME_HEIGHT -55) {
            ballYSpeed = -ballYSpeed; // Inverte a direção
        }

        if (ballX <= 16 && ballY >= paddle1Y && ballY <= paddle1Y + 60) {
            ballXSpeed = -ballXSpeed;
        }
        if (ballX >= GAME_WIDTH - 40 && ballY >= paddle2Y && ballY <= paddle2Y + 60) {
            ballXSpeed = -ballXSpeed;
        }

        if (ballX < 0) { // Bola passou pela parede esquerda
            scorePlayer2++;
            resetBall();
        } else if (ballX > GAME_WIDTH) { // Bola passou pela parede direita
            scorePlayer1++;
            resetBall();
        }
    }

    private static void resetBall() {
        ballX = GAME_WIDTH / 2;
        ballY = GAME_HEIGHT / 2;
        ballXSpeed = -ballXSpeed; // Inverte a direção para reiniciar a jogada
    }
    
    private static void sendGameUpdate() {
        String update = String.format("UPDATE %d %d %d %d %d %d", ballX, ballY, paddle1Y, paddle2Y, scorePlayer1, scorePlayer2);
        synchronized (PongServer.class) {
            // Remover jogadores desconectados da lista de jogadores TCP
            PlayerHandler.players.removeIf(player -> player.out == null);
    
            // Enviar atualização para clientes TCP
            for (PlayerHandler player : PlayerHandler.players) {
                player.sendUpdate(update);
            }
            
            // Enviar atualização para clientes UDP
            System.out.println("Enviando atualização para clientes: " + update);
            sendUDPUpdate(update);
        }
    }
    
    
    
    

    private static void sendUDPUpdate(String update) {
        try {
            byte[] buffer = update.getBytes();
            for (InetSocketAddress client : PlayerHandler.udpClients) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client.getAddress(), client.getPort());
                udpSocket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private static class PlayerHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        static final java.util.List<PlayerHandler> players = new java.util.ArrayList<>();
        static final java.util.List<InetSocketAddress> udpClients = new java.util.ArrayList<>();
        private int playerId;
    
        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
    
            try {
                // Inicializa o PrintWriter antes de adicionar à lista de jogadores
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // Agora adicione o jogador à lista após a inicialização de out
            players.add(this);
        }
    
        @Override
        public void run() {
            try {
                out.println("CONNECTED " + playerId); // Confirmação de conexão para o cliente
    
                String command;
                while ((command = in.readLine()) != null) {
                    if (command.startsWith("MOVE")) {
                        String[] parts = command.split(" ");
                        int player = Integer.parseInt(parts[1]);
                        int newY = Integer.parseInt(parts[2]);
    
                        if (player == 1) {
                            paddle1Y = newY;
                        } else if (player == 2) {
                            paddle2Y = newY;
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
                
                synchronized (PongServer.class) {
                    // Remove o jogador da lista e libera seu ID
                    PlayerHandler.players.remove(this); // Remove o jogador desconectado
                    PlayerHandler.udpClients.removeIf(client -> client.getPort() == socket.getPort());
                    
                    if (this.playerId == 1) {
                        PongServer.connectedPlayers--;
                    } else if (this.playerId == 2) {
                        PongServer.connectedPlayers--;
                    }
                }
            }
        }
    
        public void sendUpdate(String update) {
            if (out != null) { // Verifique se out não é nulo antes de tentar enviar
                out.println(update);
            }
        }
    }
}
