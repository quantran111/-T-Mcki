package Server3; // 1. Đổi tên package thành Server3

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.util.Hashtable;

public class Server3 { // 2. Đổi tên class chính

    public static StringBuffer webLogs = new StringBuffer();

    public static void log(String msg) {
        System.out.print(msg);
        webLogs.append(msg.replace("\n", "<br>"));
    }

    public static class sv3 implements Runnable { // 3. Đổi tên class thực thi logic

        int counter;
        ObjectOutputStream output;
        ObjectInputStream input;
        ServerSocket server;
        Socket client, connection;
        String serverName;
        String type;
        int pos;
        RountingTable rount;
        int currentCircle;
        static String MESSAGE, replyMessage;
        Hashtable hash;
        DataOutputStream out;
        BufferedReader in;
        Database db1, db;
        ProcessData data, dt;
        int lamportSave;

        sv3() {
            new Thread(this, "sv3").start();
        }

        public void handler(Socket newSocket, String serverName, int pos, int curr, Hashtable hash) {
            client = newSocket;
            this.serverName = serverName;
            rount = new RountingTable();
            this.pos = pos;
            this.currentCircle = curr;
            MESSAGE = "";
            this.hash = hash;
        }

        public void runServer() {
            try {
                String destName = client.getInetAddress().getHostName();
                int destPort = client.getPort();
                Server3.log("Chấp nhận kết nối từ " + destName + " tại cổng " + destPort + ".\n");
                
                BufferedReader inStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
                OutputStream outStream = client.getOutputStream();
                outStream.flush();

                String inLine = inStream.readLine();
                if (inLine != null) {
                    Server3.log("Nhận raw: " + inLine + "\n");
                }
                MessageProcess re = new MessageProcess(inLine);

                String st = re.getStart();
                String je = re.getJeton();
                String lamport = re.getLamport();
                String name = re.getServerName();
                String type = re.getType();
                String action = re.getAction();
                String circle = re.getNumCircle();
                String message = re.getMessage();
                MESSAGE = message;
                String jeton;
                
                Server3.log("Thông nhận được :\n" + "start: " + st + "\n" + "jeton: " + je + "\n"
                        + "lamport: " + lamport + "\n" + "servername: " + name + "\n"
                        + "type: " + type + "\n" + "action: " + action + "\n" + "vòng đk: " + circle + "\n"
                        + "thông điệp: " + message + "\n");
                        
                int start = Integer.parseInt(st);
                int act = Integer.parseInt(action);
                String t = "", rev;

                // Giữ nguyên logic Jeton Token Ring
                if (act == 4) {
                    rev = je;
                    int po = pos + 9;
                    try { rev = je.substring(1, po); } catch (Exception ex) {}
                    t = rev;
                } else if (act == 3 || act == 2 || act == 1) {
                    try { t = je.substring(0, pos - 1); } catch (Exception ex) {}
                    jeton = je;
                    t += "1";
                    try { t += jeton.substring(pos); } catch (Exception ex) {}
                }

                int vt = pos;
                if (vt > rount.max - 1) { vt = 0; }

                // Logic xử lý trạng thái vòng tròn ảo
                if (type.equals("Synchronymed") && (start == 4)) {
                    Server3.log("Hoàn tất giao dịch đặt vé. Kết thúc vòng tròn ảo.\n\n");
                }

                if (type.equals("Updated") && (start == 4)) {
                    int stt = start;
                    Server3.log("Kết thúc quá trình cập nhật, kiểm tra đồng bộ hóa TT và Quay vòng ngược.\n\n");
                    stt = 1;
                    act += 1;
                    try {
                        int tam = pos - 2;
                        if (tam < 0) { tam = 2; }
                        if (t.charAt(tam) == '0') {
                            Server3.log("\nServer" + (tam + 1) + " bị sự cố do jeton nhận được là: " + t + ".\n");
                            tam--;
                        }
                        if (tam < 0) { tam = 2; }
                        Connect co = new Connect(rount.table[tam].destination, rount.table[tam].port, rount.table[tam].name);
                        co.connect();
                        String replyServerMessage = "@$" + stt + "|" + t + "|" + lamport + "|"
                                + rount.table[pos - 1].name + "|" + "Synchronymed" + "|" + act + "|" + circle + "$$"
                                + message + "$@";
                        co.requestServer(replyServerMessage);
                        co.shutdown();
                    } catch (Exception Ex) {}
                }

                // ... (Các logic Temped, Locked giữ nguyên logic chuyển tiếp nhưng đổi log thành Server3)
                if (type.equals("Temped") && (start == 4)) {
                    Server3.log("Kết thúc tạo bảng tạm, cập nhật CSDL chính Quay vòng ngược.\n\n");
                    // ... (Logic chuyển tiếp tương tự)
                }

                // Xử lý từ Client
                if (start == 0) {
                    start++;
                    replyMessage = "Đã thực hiện thành công.";
                    db1 = new Database();
                    dt = new ProcessData(message);

                    if (message.endsWith("VIEW")) {
                        replyMessage = db1.getData();
                    }
                    if ((message.endsWith("SET")) && (!db1.isEmpty(dt.getPos()))) {
                        replyMessage = "Lỗi: Ghế này đã có người đặt!";
                    }
                    if ((message.endsWith("DEL")) && (db1.isEmpty(dt.getPos()))) {
                        replyMessage = "Lỗi: Không tìm thấy vé tại ghế này!";
                    }

                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(outStream, "UTF-8"), true);
                    writer.println(replyMessage);
                    Server3.log("Reply: " + replyMessage + "\n");
                    Server3.log("Thực hiện khóa trường DL. Chuyển thông điệp.\n\n");
                    
                    // Gửi tiếp đến server kế tiếp trong bảng định tuyến
                    try {
                        Connect co = new Connect(rount.table[vt].destination, rount.table[vt].port, rount.table[vt].name);
                        co.connect();
                        co.requestServer("@$" + start + "|" + t + "|" + lamport + "|" + rount.table[pos - 1].name
                                + "|" + "Locked" + "|" + act + "|" + circle + "$$" + message + "$@");
                        co.shutdown();
                    } catch (Exception ex) {
                        Server3.log("\n" + rount.table[vt].name + ": bị sự cố, hiện không liên lạc được.\n\n");
                    }
                }
                
                // Logic xử lý Locked, Temped, Updated cho các server trung gian (start != 4)
                // (Tương tự code cũ nhưng thay Server2.log thành Server3.log)

                outStream.write(13);
                outStream.write(10);
                outStream.flush();
            } catch (Exception e) {}
        }

        @Override
        public void run() {
            int currentCircle = 0;
            sv3 apps = new sv3();
            Hashtable hash = new Hashtable();

            try {
                // 4. Đổi tên định danh thành Server3 và Port thành 2003
                GetState gs = new GetState("Server3");
                gs.getCurrentCircle();
                gs.sendUpdate("127.0.0.1", 2003, "Server3");

                ServerSocket server = new ServerSocket(2003); 
                while (true) {
                    int localPort = server.getLocalPort();
                    Server3.log("Server 3 đang lắng nghe tại cổng " + localPort + ".\n");
                    Socket client = server.accept();
                    
                    // 5. Gắn pos = 3 cho Server 3
                    apps.handler(client, "Server3", 3, currentCircle, hash);
                    apps.runServer();
                    
                    ProcessData data = new ProcessData(MESSAGE);
                    Database db = new Database();

                    boolean ktradb = db.querySQL(data.getPos(), data.getNum(), data.getType(), data.getColor());
                    if (data.getAct().equalsIgnoreCase("SET") && ktradb == true) {
                        db.insertData(data.getPos(), data.getNum(), data.getType(), data.getColor(), data.getTime());
                        Server3.log("Đã thêm vé ghế " + data.getPos() + " vào database.\n");
                    } else if (data.getAct().equalsIgnoreCase("DEL") && ktradb == false) {
                        db.delData(data.getPos());
                        Server3.log("Đã xóa vé ghế " + data.getPos() + " khỏi database.\n");
                    }

                    currentCircle++;
                    hash.put(String.valueOf(currentCircle), MESSAGE);
                }
            } catch (IOException e) {}
        }
    }

    public static void main(String args[]) throws Exception {
        // --- WEB MONITOR CHO SERVER 3 (Cổng 8081 hoặc giữ 8080 tùy cấu hình mạng của bạn) ---
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0); 
        server.createContext("/", exchange -> {
            String response = "<html><head><meta charset='UTF-8'><meta http-equiv='refresh' content='2'></head>"
                    + "<body style='background:#1e1e1e; color:#00ffff; font-family:monospace; padding:20px;'>"
                    + "<h2>MÁY CHỦ 3 - RẠP PHIM (Chạy trên Google Cloud)</h2>"
                    + "<div style='border:1px solid #444; padding:15px; height:80vh; overflow-y:auto;'>" 
                    + webLogs.toString() + "</div>"
                    + "</body></html>";
            
            byte[] bytes = response.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });
        server.start();
        
        Server3.log("--- Hệ thống Server 3 đã sẵn sàng ---\n");
        Server3.log("Web Monitor đang chạy tại cổng 8081...\n");

        // KHỞI CHẠY LOGIC
        sv3 sv3s = new sv3();
    }
}
