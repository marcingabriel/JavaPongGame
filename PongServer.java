import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;


public class PongServer extends UnicastRemoteObject implements PongService {
    private static final int GAME_HEIGHT = 500;
    private static final int GAME_WIDTH = 500;

    private int paddle1Y = 100; // Posição da paleta do jogador 1
    private int paddle2Y = 100; // Posição da paleta do jogador 2
    private int ballX = GAME_WIDTH / 2; // Posição X da bola
    private int ballY = GAME_HEIGHT / 2; // Posição Y da bola
    private int ballXSpeed = 6; // Velocidade da bola em X
    private int ballYSpeed = 4; // Velocidade da bola em Y
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;

    private final List<Integer> connectedPlayers = new ArrayList<>();

    public PongServer() throws Exception {
        super();
        new Thread(this::gameLoop).start(); // Inicia o loop do jogo
    }

    @Override
    public synchronized int connect() throws RemoteException {
        if (connectedPlayers.size() < 2) {
            int playerId = connectedPlayers.size() + 1;
            connectedPlayers.add(playerId);
            System.out.println("Player " + playerId + " connected.");
            return playerId;
        } else {
            throw new RemoteException("Maximum players connected.");
        }
    }

    @Override
    public synchronized void movePaddle(int playerId, int newY) throws RemoteException {
        if (playerId == 1) {
            paddle1Y = newY;
        } else if (playerId == 2) {
            paddle2Y = newY;
        }
    }

    @Override
    public synchronized String getGameState() throws RemoteException {
        return String.format("%d %d %d %d %d %d", ballX, ballY, paddle1Y, paddle2Y, scorePlayer1, scorePlayer2);
    }

    private void gameLoop() {
        while (true) {
            try {
                Thread.sleep(20); // 50 FPS
                updateBallPosition();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void updateBallPosition() {
        ballX += ballXSpeed;
        ballY += ballYSpeed;

        if (ballY <= 5 || ballY >= GAME_HEIGHT - 55) {
            ballYSpeed = -ballYSpeed;
        }

        if (ballX <= 23 && ballY >= paddle1Y && ballY <= paddle1Y + 60) {
            ballXSpeed = -ballXSpeed;
        }
        if (ballX >= GAME_WIDTH - 47 && ballY >= paddle2Y && ballY <= paddle2Y + 60) {
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

    private synchronized void resetBall() {
        ballX = GAME_WIDTH / 2;
        ballY = GAME_HEIGHT / 2;
        ballXSpeed = -ballXSpeed;
    }

    public static void main(String[] args) {
        try {
            // Criação da implementação do serviço RMI
            PongService service = new PongServer();

            // Inicialização do registro RMI
            Registry registry = LocateRegistry.createRegistry(1099);

            // Ligação do serviço com o nome "PongService"
            registry.rebind("PongService", service);

            System.out.println("Pong Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
