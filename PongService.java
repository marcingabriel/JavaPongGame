import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PongService extends Remote {
    int connect() throws RemoteException;
    void movePaddle(int playerId, int newY) throws RemoteException;
    String getGameState() throws RemoteException;
}
