package chessFive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {
	//监听
    private ServerSocket serverSocket;
    //连接客户端
    private Socket clientSocket;
    //输入
    private BufferedReader in;
    //输出
    private PrintWriter out;
    //确定已连接
    private boolean connected = false;
    //管理线程，解决关闭服务器的时候线程一直阻塞重置导致卡死的问题
    private Thread acceptThread;

    // 构造
    public Server(int dk) throws IOException {
        serverSocket = new ServerSocket();
        // 先设置端口复用，再绑定端口（要不然只能创建一次服务器）
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress(dk));
    }

    // 接受客户端连接
    public void acceptClient() throws IOException {
        acceptThread = Thread.currentThread(); // 记录监听线程
        clientSocket = serverSocket.accept(); //阻塞等待连接
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
        connected = true;
    }

    // 发送消息
    public void send(String msg) {
        if (connected && out != null) out.println(msg);
    }
    // 接收消息
    public String receive() throws IOException {
        return connected && in != null ? in.readLine() : null;
    }

    // 是否连接
    public boolean isConnected() {
        return connected;
    }

    // 关闭
    public void close() {
        if (acceptThread != null && acceptThread.isAlive()) {
            acceptThread.interrupt();
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.setSoLinger(true, 0); // 强制关闭
                clientSocket.close();
            }
        } catch (IOException e) {}
        try {
            if (in != null) in.close();
        } catch (IOException e) {}
        try {
            if (out != null) out.close();
        } catch (Exception e) {}
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {}
        connected = false;
        acceptThread = null;
        serverSocket = null;
        clientSocket = null;
    }
}