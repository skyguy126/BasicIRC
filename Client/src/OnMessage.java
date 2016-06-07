package socketclient;

public interface OnMessage
{
    void onMessage(String username, String message);
    void updateConnected(String[] list);
}
