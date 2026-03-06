package chessFive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	//连接服务器
    private Socket socket;
    //输入
    private BufferedReader in;
    //输出
    private PrintWriter out;
    //是否连接
    private boolean connected = false;
    //构造
    public Client(String ip, int dk) throws IOException {
        socket = new Socket(ip, dk);//创建一个socket对象，要不然连接第二次会卡死
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        connected = true;
    }
    //发送
    public void send(String msg) {
        if (connected && out != null) out.println(msg);
    }
    //接收
    public String receive() throws IOException {
        return connected && in != null ? in.readLine() : null;
    }
    //是否连接
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
    //关闭连接
    public void close() {
        try {
            if (in != null) in.close();
        } catch (IOException e) {}
        try {
            if (out != null) out.close();
        } catch (Exception e) {}
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}
        connected = false;
    }
}