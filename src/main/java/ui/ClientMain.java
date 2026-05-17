package ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            new ClientFrame().setVisible(true);
        });
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception flatLafException) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        return;
                    }
                }
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing sẽ dùng look and feel mặc định nếu mọi lựa chọn đều không khả dụng.
            }
        }
    }
}
