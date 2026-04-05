package Client1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class View extends JPanel {

    private JPanel seatPanel;
    private JTextArea display;
    // Dữ liệu ghế chia sẻ với ActPark qua Client
    private final HashMap<String, String> bookedSeatsInfo;

    private static final String[] SERVER_IPS   = {"136.110.4.186", "34.28.238.63", "34.55.107.207", "34.71.156.133", "104.154.64.145"};
    private static final int[]    SERVER_PORTS  = {2001, 2002, 2003, 2004, 2005};
    private static final String[] SERVER_NAMES  = {"Server 1", "Server 2", "Server 3", "Server 4", "Server 5"};

    public View(HashMap<String, String> sharedSeatData) {
        this.bookedSeatsInfo = sharedSeatData;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- 1. PHẦN TOP: NÚT CẬP NHẬT ---
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlTop.setBackground(new Color(30, 30, 30));

        JLabel lblInfo = new JLabel("Dữ liệu được tổng hợp từ tất cả 5 server (Read Quorum)");
        lblInfo.setForeground(new Color(180, 180, 180));
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        JButton btnLoad = new JButton("↻  TẢI DỮ LIỆU RẠP");
        btnLoad.setBackground(new Color(229, 9, 20));
        btnLoad.setForeground(Color.WHITE);
        btnLoad.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLoad.setFocusPainted(false);
        btnLoad.setPreferredSize(new Dimension(200, 38));
        btnLoad.addActionListener(e -> new Thread(this::loadMapData).start());

        pnlTop.add(lblInfo);
        pnlTop.add(btnLoad);

        // --- 2. PHẦN GIỮA: SƠ ĐỒ RẠP PHIM ---
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 8));
        pnlCenter.setBackground(new Color(30, 30, 30));

        // Màn hình chiếu
        JLabel lblScreen = new JLabel("🎬  MÀN HÌNH CHIẾU", SwingConstants.CENTER);
        lblScreen.setOpaque(true);
        lblScreen.setBackground(new Color(70, 70, 70));
        lblScreen.setForeground(Color.WHITE);
        lblScreen.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblScreen.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        lblScreen.setPreferredSize(new Dimension(0, 38));

        // Lưới ghế ngồi — 3 hàng × 10 ghế = 30 ghế
        seatPanel = new JPanel(new GridBagLayout());
        seatPanel.setBackground(new Color(30, 30, 30));
        seatPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        drawEmptySeats();

        // Chú thích
        JPanel pnlLegend = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 5));
        pnlLegend.setBackground(new Color(30, 30, 30));
        pnlLegend.add(createLegendItem("Trống (Normal)",    new Color(46, 204, 113)));
        pnlLegend.add(createLegendItem("Đã đặt (Normal)",   new Color(231, 76, 60)));
        pnlLegend.add(createLegendItem("Trống (VIP - Hàng C)", new Color(241, 196, 15)));
        pnlLegend.add(createLegendItem("Đã đặt (VIP - Hàng C)", new Color(155, 89, 182)));

        pnlCenter.add(lblScreen,  BorderLayout.NORTH);
        pnlCenter.add(seatPanel,  BorderLayout.CENTER);
        pnlCenter.add(pnlLegend, BorderLayout.SOUTH);

        // --- 3. PHẦN DƯỚI: BẢNG CHI TIẾT ---
        display = new JTextArea();
        display.setEditable(false);
        display.setBackground(new Color(20, 20, 20));
        display.setForeground(new Color(0, 255, 150));
        display.setFont(new Font("Consolas", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(display);
        scrollPane.setPreferredSize(new Dimension(550, 180));
        TitledBorder tb = BorderFactory.createTitledBorder(null, "Chi tiết vé đã đặt",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), Color.WHITE);
        scrollPane.setBorder(tb);

        add(pnlTop,    BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
    }

    // Vẽ sơ đồ ghế trống ban đầu
    private void drawEmptySeats() {
        seatPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill   = GridBagConstraints.BOTH;

        String[] rows = {"A", "B", "C"};
        for (int r = 0; r < rows.length; r++) {
            // Nhãn hàng
            JLabel rowLabel = new JLabel(rows[r] + (rows[r].equals("C") ? " ★VIP" : "    "));
            rowLabel.setForeground(rows[r].equals("C") ? new Color(241, 196, 15) : Color.WHITE);
            rowLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            rowLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            gbc.gridx = 0; gbc.gridy = r; gbc.weightx = 0;
            seatPanel.add(rowLabel, gbc);

            for (int i = 1; i <= 10; i++) {
                JButton btn = makeSeatButton(rows[r] + i, rows[r].equals("C"), false, "");
                gbc.gridx = i; gbc.gridy = r; gbc.weightx = 1;
                seatPanel.add(btn, gbc);
            }
        }
        seatPanel.revalidate();
        seatPanel.repaint();
    }

    // Tạo nút ghế với màu theo trạng thái
    private JButton makeSeatButton(String seatName, boolean isVip, boolean isBooked, String tooltip) {
        JButton btn = new JButton(seatName);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(52, 36));

        if (isVip) {
            btn.setBackground(isBooked ? new Color(155, 89, 182)  // Tím = VIP đã đặt
                                       : new Color(241, 196, 15)); // Vàng = VIP trống
            if (!isBooked) btn.setForeground(new Color(30, 30, 30));
        } else {
            btn.setBackground(isBooked ? new Color(231, 76, 60)   // Đỏ = Normal đã đặt
                                       : new Color(46, 204, 113)); // Xanh = Normal trống
        }

        if (!tooltip.isEmpty()) {
            btn.setToolTipText(tooltip);
            btn.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                    "Ghế " + seatName + "\n" + tooltip,
                    "Thông tin vé", JOptionPane.INFORMATION_MESSAGE));
        } else {
            btn.setToolTipText("Ghế trống");
        }
        return btn;
    }

    private JPanel createLegendItem(String label, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        p.setBackground(new Color(30, 30, 30));
        JLabel box = new JLabel("   ");
        box.setOpaque(true);
        box.setBackground(color);
        box.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JLabel text = new JLabel(label);
        text.setForeground(Color.WHITE);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(box);
        p.add(text);
        return p;
    }

    // =====================================================================
    // HÀM CHÍNH: Hỏi TẤT CẢ 5 server song song, merge dữ liệu (Read Quorum)
    // =====================================================================
    public void loadMapData() {
        bookedSeatsInfo.clear(); // Xóa cache cũ trước khi tải mới
        SwingUtilities.invokeLater(() -> {
            display.setText("");
            display.append("[" + new Date() + "] Đang truy vấn 5 server...\n");
            display.append("─────────────────────────────────────────────────────────────────\n");
        });

        // Dùng thread pool để gửi song song tới 5 server
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> queryServer(SERVER_IPS[idx], SERVER_PORTS[idx])));
        }
        pool.shutdown();

        // Map dùng chung để merge — key = seatPos, value = dòng hiển thị
        // LinkedHashMap để giữ thứ tự ghế
        Map<String, String> mergedMap = new LinkedHashMap<>();
        StringBuilder logBuilder = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            String raw = "";
            try {
                raw = futures.get(i).get(6, TimeUnit.SECONDS);
                if (raw == null) raw = "";
                logBuilder.append("[").append(SERVER_NAMES[i]).append("] ")
                          .append(raw.isEmpty() ? "Không có dữ liệu." : "OK")
                          .append("\n");
            } catch (TimeoutException te) {
                logBuilder.append("[").append(SERVER_NAMES[i]).append("] Timeout!\n");
            } catch (Exception e) {
                logBuilder.append("[").append(SERVER_NAMES[i]).append("] Lỗi kết nối.\n");
            }

            // Dùng Y CHANG parse logic gốc của code ban đầu
            // chỉ ghế chưa có trong map mới được thêm (tránh trùng)
            String inLine = raw;
            while (!inLine.equalsIgnoreCase("") && inLine.contains("|")) {
                StringBuilder row = new StringBuilder();
                String seatPos = "";
                String customer = "", ticketType = "";

                for (int j = 0; j < 5; j++) {
                    int k = 0;
                    try { k = inLine.indexOf("|"); } catch (Exception ex) { k = 0; }
                    if (k == -1) break;

                    String vt = inLine.substring(0, k);

                    if (j == 0) seatPos    = vt;
                    if (j == 1) customer   = vt;
                    if (j == 2) ticketType = vt;

                    row.append(vt);
                    if (j < 4) row.append("      |   ");

                    k += 1;
                    if (k <= inLine.length()) {
                        inLine = inLine.substring(k);
                    } else {
                        inLine = "";
                    }
                }

                // Chỉ thêm nếu ghế chưa tồn tại trong map (server nào trả về trước thì ưu tiên)
                String key = seatPos.toUpperCase();
                if (!key.isEmpty() && !mergedMap.containsKey(key)) {
                    mergedMap.put(key, row.toString());
                    // Lưu thêm tooltip gọn cho sơ đồ: "Khách: X | Vé: Y"
                    bookedSeatsInfo.put(key, "Khách: " + customer + " | Vé: " + ticketType);
                }
            }
        }

        // Cập nhật UI
        final Map<String, String> finalMap = mergedMap;
        final String finalLog = logBuilder.toString();
        SwingUtilities.invokeLater(() -> {
            display.append(finalLog);
            display.append("─────────────────────────────────────────────────────────────────\n");
            display.append(String.format("%-12s %-15s %-13s %-15s %s\n",
                    "Phòng+Ghế", "Tên Khách", "Loại vé", "Thanh toán", "Giờ đặt"));
            display.append("─────────────────────────────────────────────────────────────────\n");

            if (finalMap.isEmpty()) {
                display.append("Chưa có ghế nào được đặt.\n");
            } else {
                for (String row : finalMap.values()) {
                    display.append(row + "\n");
                }
            }

            updateSeatsUI();
        });
    }

    // Gửi lệnh VIEW tới 1 server, trả về chuỗi raw y như code gốc
    private String queryServer(String ip, int port) {
        try (Socket client = new Socket(ip, port)) {
            client.setSoTimeout(5000);
            BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"));

            String message = "|||||VIEW";
            String fullMsg = "@$0|00000|000|Client|Send|1|123$$" + message + "$@";
            out.write(fullMsg);
            out.newLine();
            out.flush();

            String result = in.readLine();
            return (result != null) ? result : "";
        } catch (Exception e) {
            return "";
        }
    }

    // Vẽ lại sơ đồ ghế dựa theo dữ liệu đã merge
    private void updateSeatsUI() {
        seatPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill   = GridBagConstraints.BOTH;

        String[] rows = {"A", "B", "C"};
        for (int r = 0; r < rows.length; r++) {
            boolean isVipRow = rows[r].equals("C");

            // Nhãn hàng
            JLabel rowLabel = new JLabel(isVipRow ? "C ★VIP" : rows[r] + "     ");
            rowLabel.setForeground(isVipRow ? new Color(241, 196, 15) : Color.WHITE);
            rowLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            rowLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            gbc.gridx = 0; gbc.gridy = r; gbc.weightx = 0;
            seatPanel.add(rowLabel, gbc);

            for (int i = 1; i <= 10; i++) {
                String seatName = rows[r] + i;
                boolean booked  = bookedSeatsInfo.containsKey(seatName);
                String tip      = booked ? bookedSeatsInfo.get(seatName) : "";
                JButton btn     = makeSeatButton(seatName, isVipRow, booked, tip);
                gbc.gridx = i; gbc.gridy = r; gbc.weightx = 1;
                seatPanel.add(btn, gbc);
            }
        }
        seatPanel.revalidate();
        seatPanel.repaint();
    }
}
