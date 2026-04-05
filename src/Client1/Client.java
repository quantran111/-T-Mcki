package Client1;

import java.awt.*;
import java.util.HashMap;
import javax.swing.*;

public class Client extends JFrame {

    private JTabbedPane tab;

    // Dữ liệu ghế dùng chung giữa 2 tab
    // Key = "A1", "B3"... | Value = thông tin vé
    private final HashMap<String, String> sharedSeatData = new HashMap<>();

    public Client() {
        super("Hệ Thống Đặt Vé Rạp Chiếu Phim - Phân Tán");
        setSize(650, 680);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 14);
        UIManager.put("Label.font",      mainFont);
        UIManager.put("Button.font",     boldFont);
        UIManager.put("TabbedPane.font", boldFont);
        UIManager.put("TextField.font",  mainFont);
        UIManager.put("TextArea.font",   mainFont);
        UIManager.put("ComboBox.font",   mainFont);

        UIManager.put("TabbedPane.background", new Color(40, 40, 40));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.selected",   new Color(229, 9, 20));

        // Truyền sharedSeatData vào cả 2 tab
        View viewTab       = new View(sharedSeatData);
        ActPark actParkTab = new ActPark(sharedSeatData);

        tab = new JTabbedPane();
        tab.addTab("ĐẶT / HỦY VÉ",   actParkTab);
        tab.addTab("SƠ ĐỒ RẠP PHIM", viewTab);

        add(tab);
        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
