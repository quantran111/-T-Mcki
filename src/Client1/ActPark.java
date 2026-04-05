package Client1;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class ActPark extends JPanel {

    private JTextField txt_num, txt_time, txt_type;
    private JTextArea txa_info;
    private JComboBox<String> opt_row, opt_seat, opt_clr, opt_sv;
    private String act;
    private int lamportClock = 0;

    // Dữ liệu ghế chia sẻ từ Client (đồng bộ với tab View)
    private final java.util.HashMap<String, String> sharedSeatData;

    private static final String[] SERVER_IPS   = {"136.110.4.186", "34.28.238.63", "34.55.107.207", "34.71.156.133", "104.154.64.145"};
    private static final int[]    SERVER_PORTS  = {2001, 2002, 2003, 2004, 2005};

    public ActPark(java.util.HashMap<String, String> sharedSeatData) {
        this.sharedSeatData = sharedSeatData;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- PANEL CHỌN SERVER ---
        JPanel pnlServer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlServer.setBackground(new Color(30, 30, 30));
        JLabel lblSv = new JLabel("Chọn Máy Chủ Xử Lý:");
        lblSv.setForeground(Color.WHITE);
        lblSv.setFont(new Font("Segoe UI", Font.BOLD, 16));

        String[] servers = {"Server 1", "Server 2", "Server 3", "Server 4", "Server 5"};
        opt_sv = new JComboBox<>(servers);
        opt_sv.setPreferredSize(new Dimension(150, 30));
        pnlServer.add(lblSv);
        pnlServer.add(opt_sv);

        // --- PANEL FORM NHẬP LIỆU ---
        JPanel pnlForm = new JPanel(new GridLayout(5, 2, 10, 15));
        pnlForm.setBackground(new Color(45, 45, 45));
        TitledBorder border = BorderFactory.createTitledBorder("Thông tin Đặt vé");
        border.setTitleColor(new Color(255, 193, 7));
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        pnlForm.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(10, 10, 10, 10)));

        // Hàng ghế: A, B, C — chỉ có 1 phòng duy nhất
        String[] rows = {"A", "B", "C"};
        opt_row = new JComboBox<>(rows);
        opt_row.setPreferredSize(new Dimension(60, 30));

        // Số ghế 1-10
        String[] seatNums = {"1","2","3","4","5","6","7","8","9","10"};
        opt_seat = new JComboBox<>(seatNums);
        opt_seat.setPreferredSize(new Dimension(80, 30));

        // Gộp Hàng và Ghế vào 1 cell
        JPanel pnlSeat = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlSeat.setOpaque(false);
        pnlSeat.add(new JLabel("Hàng:") {{ setForeground(Color.LIGHT_GRAY); }});
        pnlSeat.add(opt_row);
        pnlSeat.add(new JLabel("Ghế:") {{ setForeground(Color.LIGHT_GRAY); }});
        pnlSeat.add(opt_seat);

        txt_num  = new JTextField();
        txt_type = new JTextField();
        txt_type.setEditable(false); // Tự động điền dựa theo hàng ghế

        String[] pays = {"Tiền mặt", "Momo", "Thẻ tín dụng"};
        opt_clr = new JComboBox<>(pays);
        txt_time = new JTextField();
        txt_time.setEditable(false);

        // Logic tự động điền loại vé khi chọn hàng
        opt_row.addActionListener(e -> {
            String selectedRow = opt_row.getSelectedItem().toString();
            if (selectedRow.equals("C")) {
                txt_type.setText("VIP");
                txt_type.setForeground(new Color(255, 193, 7)); // Vàng cho VIP
            } else {
                txt_type.setText("Normal");
                txt_type.setForeground(Color.WHITE);
            }
        });
        // Kích hoạt lần đầu
        txt_type.setText("Normal");
        txt_type.setForeground(Color.WHITE);

        addFormField(pnlForm, "Hàng & Số ghế:", pnlSeat);
        addFormField(pnlForm, "Tên Khách Hàng:", txt_num);
        addFormField(pnlForm, "Loại Vé (tự động):", txt_type);
        addFormField(pnlForm, "Hình Thức Thanh Toán:", opt_clr);
        addFormField(pnlForm, "Thời Gian Đặt:", txt_time);

        // --- PANEL BUTTONS ---
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        pnlButtons.setBackground(new Color(30, 30, 30));
        JButton btnDat = createButton("ĐẶT VÉ",  new Color(46, 204, 113));
        JButton btnHuy = createButton("HỦY VÉ",  new Color(231, 76, 60));
        JButton btnMoi = createButton("LÀM MỚI", new Color(52, 152, 219));

        btnDat.addActionListener(e -> {
            if (txt_num.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên khách hàng!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            act = "SET";
            new Thread(this::runClient).start();
        });

        btnHuy.addActionListener(e -> {
            String seatKey  = opt_row.getSelectedItem() + "" + opt_seat.getSelectedItem();
            String seatInfo = seatKey;

            // Kiểm tra local cache (dữ liệu đã tải từ tab Sơ đồ)
            if (sharedSeatData.isEmpty()) {
                // Chưa tải dữ liệu lần nào — cảnh báo nhưng vẫn cho tiếp tục
                int go = JOptionPane.showConfirmDialog(this,
                    "Chưa tải dữ liệu từ server (vào tab Sơ đồ và bấm Tải dữ liệu rạp trước).\n" +
                    "Bạn vẫn muốn gửi lệnh hủy ghế " + seatInfo + " không?",
                    "Chưa có dữ liệu local", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (go != JOptionPane.YES_OPTION) return;
            } else if (!sharedSeatData.containsKey(seatKey.toUpperCase())) {
                // Ghế không tồn tại trong dữ liệu đã tải
                JOptionPane.showMessageDialog(this,
                    "Ghế " + seatInfo + " không có trong danh sách đã đặt.\nKhông thể hủy!",
                    "Ghế không tồn tại", JOptionPane.ERROR_MESSAGE);
                return;
            } else {
                // Ghế tồn tại — hiện thông tin và xác nhận
                String detail = sharedSeatData.get(seatKey.toUpperCase());
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận hủy vé:\nGhế " + seatInfo + "\n" + detail + "\n\nBạn có chắc không?",
                    "Xác nhận hủy vé", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
            }

            act = "DEL";
            new Thread(this::runClient).start();
        });

        btnMoi.addActionListener(e -> {
            txt_num.setText("");
            txa_info.setText("");
            opt_row.setSelectedIndex(0);
            opt_seat.setSelectedIndex(0);
        });

        pnlButtons.add(btnDat);
        pnlButtons.add(btnHuy);
        pnlButtons.add(btnMoi);

        // --- PANEL THÔNG TIN LOG ---
        txa_info = new JTextArea();
        txa_info.setEditable(false);
        txa_info.setBackground(new Color(20, 20, 20));
        txa_info.setForeground(new Color(0, 255, 0));
        txa_info.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(txa_info);
        scrollPane.setPreferredSize(new Dimension(500, 150));
        scrollPane.setBorder(BorderFactory.createTitledBorder(null, "Kết quả xử lý",
                TitledBorder.LEFT, TitledBorder.TOP, null, Color.WHITE));

        // Gom các thành phần
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.add(pnlServer, BorderLayout.NORTH);
        pnlTop.add(pnlForm, BorderLayout.CENTER);
        pnlTop.add(pnlButtons, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Cập nhật giờ tự động
        new Thread(() -> {
            while (true) {
                try {
                    String time = new Date().toString();
                    SwingUtilities.invokeLater(() -> txt_time.setText(time));
                    Thread.sleep(1000);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void addFormField(JPanel panel, String labelText, JComponent comp) {
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(Color.WHITE);
        panel.add(lbl);
        panel.add(comp);
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 40));
        return btn;
    }

    // Tăng Lamport clock và trả về chuỗi 3 chữ số
    private synchronized String nextLamport() {
        lamportClock++;
        return String.format("%03d", lamportClock % 1000);
    }

    public String getMessage() {
        String seat = opt_row.getSelectedItem().toString() + opt_seat.getSelectedItem().toString();
        return seat + "|"
                + txt_num.getText() + "|"
                + txt_type.getText() + "|"
                + opt_clr.getSelectedItem().toString() + "|"
                + txt_time.getText() + "|"
                + act;
    }

    public void runClient() {
        int svIndex = opt_sv.getSelectedIndex();
        String ip   = SERVER_IPS[svIndex];
        int port    = SERVER_PORTS[svIndex];

        String seatNum = opt_seat.getSelectedItem().toString();
        if (seatNum.isEmpty()) {
            SwingUtilities.invokeLater(() -> txa_info.append("Vui lòng chọn số ghế!\n"));
            return;
        }
        connect2Server(ip, port);
    }

    public void connect2Server(String host, int port) {
        try (Socket client = new Socket(host, port)) {
            client.setSoTimeout(5000);
            BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"));

            String lamport = nextLamport();
            SwingUtilities.invokeLater(() -> txa_info.append(">> Gửi yêu cầu tới " + host + ":" + port + " [Lamport=" + lamport + "]\n"));

            String fullMsg = "@$0|00000|" + lamport + "|Client|Send|1|123$$" + getMessage() + "$@";
            out.write(fullMsg);
            out.newLine();
            out.flush();

            String res = in.readLine();
            if (res == null) res = "(Không có phản hồi)";
            final String response = res;
            SwingUtilities.invokeLater(() -> txa_info.append("<< Server phản hồi: " + response + "\n\n"));

        } catch (ConnectException ce) {
            SwingUtilities.invokeLater(() -> txa_info.append("!! Lỗi: Server từ chối kết nối (" + host + ":" + port + ")\n\n"));
        } catch (SocketTimeoutException ste) {
            SwingUtilities.invokeLater(() -> txa_info.append("!! Lỗi: Server không phản hồi (timeout 5s)\n\n"));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> txa_info.append("!! Lỗi: " + e.getMessage() + "\n\n"));
        }
    }
}
