package ui;

import model.ExamSessionInfo;
import socket.AssignmentResponse;
import socket.ClientSocketService;
import socket.ImportResponse;
import util.AppConstants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientFrame extends JFrame {
    private static final Color APP_BG = new Color(0xF7F8FC);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY = new Color(0x0057D9);
    private static final Color PRIMARY_HOVER = new Color(0x0047B3);
    private static final Color PRIMARY_SOFT = new Color(0xEAF1FF);
    private static final Color BORDER = new Color(0xD6DBE6);
    private static final Color TEXT = new Color(0x1F2937);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color SUCCESS = new Color(0x15803D);
    private static final Color DANGER = new Color(0xB91C1C);
    private static final Color STATUS_BG = new Color(0xEAF1FF);

    private static final Font FONT_BASE = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 25);
    private static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);

    private final JTextField txtIp = createTextField(AppConstants.DEFAULT_HOST);
    private final JTextField txtPort = createTextField(String.valueOf(AppConstants.DEFAULT_PORT));
    private final JTextField txtSoCanBo = createTextField("");
    private final JTextField txtSoPhongThi = createTextField("");
    private final JLabel lblCaThiAuto = new JLabel("Ca thi sẽ tự động tạo khi chạy phân công");
    private final JLabel lblConnectionStatus = new JLabel();
    private final JLabel lblStatusBar = new JLabel();
    private final JTextArea txtLog = new JTextArea();

    private final JButton btnConnect = createPrimaryButton("Connect");
    private final JButton btnImportExcel = createOutlineButton("Import Excel vào database");
    private final JButton btnRunAssign = createPrimaryButton("Chạy phân công và xuất Excel");
    private final JButton btnOpenOutputFolder = createOutlineButton("Mở thư mục kết quả");

    private final ClientSocketService socketService = new ClientSocketService();

    public ClientFrame() {
        initLookAndFeel();
        setTitle("Client - Phân công cán bộ coi thi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(1024, 620));
        setSize(1280, 720);
        initComponents();
        registerEvents();
        updateConnectionStatus(false);
        appendLog("Khởi tạo hệ thống...");
        appendLog("Sẵn sàng thực hiện tác vụ.");
        setLocationRelativeTo(null);
    }

    private void initLookAndFeel() {
        UIManager.put("defaultFont", FONT_BASE);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(APP_BG);
        setContentPane(root);

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        cards.add(createConnectionCard());
        cards.add(createExamInfoCard());
        gbc.gridy = 0;
        center.add(cards, gbc);

        gbc.gridy = 1;
        center.add(createActionBar(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        center.add(createLogPanel(), gbc);

        root.add(center, BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(APP_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel title = new JLabel("PHÂN CÔNG CÁN BỘ COI THI");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);

        styleStatusBadge(false);
        header.add(title, BorderLayout.WEST);
        header.add(lblConnectionStatus, BorderLayout.EAST);
        return header;
    }

    private JPanel createConnectionCard() {
        JPanel card = createCard("Kết nối server");
        addFormRow(card, 2, "IP server", txtIp);
        addFormRow(card, 3, "Port", txtPort);

        GridBagConstraints gbc = baseGbc();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        card.add(btnConnect, gbc);
        return card;
    }

    private JPanel createExamInfoCard() {
        JPanel card = createCard("Thông tin kỳ thi");

        JPanel inputGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        inputGrid.setOpaque(false);
        inputGrid.add(createStackedField("SỐ CÁN BỘ", txtSoCanBo));
        inputGrid.add(createStackedField("SỐ PHÒNG THI", txtSoPhongThi));

        GridBagConstraints gbc = baseGbc();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 0, 14, 0);
        card.add(inputGrid, gbc);

        JLabel caLabel = createLabel("CA THI");
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        card.add(caLabel, gbc);

        styleReadonlyBox(lblCaThiAuto);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(lblCaThiAuto, gbc);
        return card;
    }

    private JPanel createActionBar() {
        JPanel actionBar = new JPanel(new GridBagLayout());
        actionBar.setBackground(CARD_BG);
        actionBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 10);

        gbc.gridx = 0;
        actionBar.add(btnImportExcel, gbc);

        gbc.gridx = 1;
        actionBar.add(btnRunAssign, gbc);

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        gbc.gridx = 2;
        gbc.weightx = 1;
        actionBar.add(spacer, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        actionBar.add(btnOpenOutputFolder, gbc);
        return actionBar;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(STATUS_BG);
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel("Log thực hiện");
        title.setFont(FONT_BOLD);
        title.setForeground(TEXT);

        JLabel realtime = new JLabel("Real-time update");
        realtime.setFont(FONT_BOLD);
        realtime.setForeground(MUTED);

        header.add(title, BorderLayout.WEST);
        header.add(realtime, BorderLayout.EAST);

        txtLog.setEditable(false);
        txtLog.setFont(FONT_MONO);
        txtLog.setForeground(TEXT);
        txtLog.setBackground(Color.WHITE);
        txtLog.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(STATUS_BG);
        status.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        lblStatusBar.setFont(FONT_BOLD);
        lblStatusBar.setForeground(TEXT);
        status.add(lblStatusBar, BorderLayout.WEST);
        return status;
    }

    private void registerEvents() {
        btnConnect.addActionListener(event -> connect());
        btnImportExcel.addActionListener(event -> importExcelToDatabase());
        btnRunAssign.addActionListener(event -> assign());
        btnOpenOutputFolder.addActionListener(event -> openDownloadFolder());
    }

    private void connect() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException exception) {
            appendLog("Lỗi kết nối: port phải là số nguyên.");
            showWarning("Port phải là số nguyên.");
            return;
        }

        btnConnect.setEnabled(false);
        setBusy(true, "Đang kết nối...");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                socketService.connect(txtIp.getText().trim(), port, this::publish);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(ClientFrame.this::appendLog);
            }

            @Override
            protected void done() {
                btnConnect.setEnabled(true);
                setBusy(false, "Sẵn sàng");
                try {
                    get();
                    updateConnectionStatus(true);
                    appendLog("Đã kết nối server " + getServerAddress());
                } catch (Exception exception) {
                    updateConnectionStatus(false);
                    appendLog("Lỗi kết nối: " + getErrorMessage(exception));
                }
            }
        };
        worker.execute();
    }

    private void importExcelToDatabase() {
        if (!socketService.isConnected()) {
            showWarning("Vui lòng bấm Connect trước khi import Excel.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File excelFile = fileChooser.getSelectedFile();
        btnImportExcel.setEnabled(false);
        btnRunAssign.setEnabled(false);
        setBusy(true, "Đang import Excel vào database...");
        appendLog("Bắt đầu import Excel: " + excelFile.getName());

        SwingWorker<ImportResponse, String> worker = new SwingWorker<>() {
            @Override
            protected ImportResponse doInBackground() throws Exception {
                return socketService.sendImportRequest(excelFile, this::publish);
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(ClientFrame.this::appendLog);
            }

            @Override
            protected void done() {
                setBusy(false, "Sẵn sàng");
                try {
                    ImportResponse response = get();
                    txtSoCanBo.setText(String.valueOf(response.getSoCanBo()));
                    txtSoPhongThi.setText(String.valueOf(response.getSoPhongThi()));
                    appendLog("Import thành công: " + response.getSoCanBo()
                            + " cán bộ, " + response.getSoPhongThi() + " phòng thi.");
                    btnRunAssign.setEnabled(true);
                } catch (Exception exception) {
                    String message = getErrorMessage(exception);
                    appendLog("Lỗi import Excel: " + message);
                    JOptionPane.showMessageDialog(ClientFrame.this, message, "Lỗi import", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnImportExcel.setEnabled(socketService.isConnected());
                }
            }
        };
        worker.execute();
    }

    private void assign() {
        if (!socketService.isConnected()) {
            showWarning("Vui lòng bấm Connect trước khi phân công.");
            return;
        }

        ExamSessionInfo sessionInfo = validateInputMN();
        if (sessionInfo == null) {
            return;
        }

        btnRunAssign.setEnabled(false);
        btnImportExcel.setEnabled(false);
        setBusy(true, "Đang phân công và xuất Excel...");
        appendLog(">>> Bắt đầu phiên làm việc mới.");
        appendLog("Yêu cầu phân công: " + sessionInfo.getSoLuongCanBo()
                + " cán bộ, " + sessionInfo.getSoLuongPhongThi() + " phòng thi.");

        SwingWorker<AssignmentResponse, String> worker = new SwingWorker<>() {
            @Override
            protected AssignmentResponse doInBackground() throws Exception {
                return socketService.sendAssignmentRequest(sessionInfo, this::publish);
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(ClientFrame.this::appendLog);
            }

            @Override
            protected void done() {
                setBusy(false, "Sẵn sàng");
                try {
                    AssignmentResponse response = get();
                    lblCaThiAuto.setText(response.getTenCaThi());
                    lblCaThiAuto.setForeground(SUCCESS);
                    appendLog("Tên ca thi: " + response.getTenCaThi());
                    appendResultFiles(response.getFiles());
                } catch (Exception exception) {
                    String message = getErrorMessage(exception);
                    appendLog("Lỗi phân công: " + message);
                    JOptionPane.showMessageDialog(ClientFrame.this, message, "Lỗi phân công", JOptionPane.ERROR_MESSAGE);
                } finally {
                    socketService.disconnect();
                    updateConnectionStatus(false);
                    btnImportExcel.setEnabled(false);
                    btnRunAssign.setEnabled(false);
                }
            }
        };
        worker.execute();
    }

    private void openDownloadFolder() {
        try {
            Files.createDirectories(AppConstants.CLIENT_DOWNLOAD_DIR);
            Desktop.getDesktop().open(AppConstants.CLIENT_DOWNLOAD_DIR.toFile());
            appendLog("Mở thư mục kết quả: " + AppConstants.CLIENT_DOWNLOAD_DIR.toAbsolutePath());
        } catch (Exception exception) {
            String message = getErrorMessage(exception);
            appendLog("Lỗi mở thư mục kết quả: " + message);
            JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ExamSessionInfo validateInputMN() {
        String mText = txtSoCanBo.getText().trim();
        String nText = txtSoPhongThi.getText().trim();
        if (mText.isEmpty() || nText.isEmpty()) {
            appendLog("Lỗi nhập liệu: số cán bộ và số phòng thi không được rỗng.");
            showWarning("Số cán bộ và số phòng thi không được rỗng.");
            return null;
        }

        int m;
        int n;
        try {
            m = Integer.parseInt(mText);
            n = Integer.parseInt(nText);
        } catch (NumberFormatException exception) {
            appendLog("Lỗi nhập liệu: m và n phải là số nguyên.");
            showWarning("m và n phải là số nguyên.");
            return null;
        }

        if (m <= 0 || n <= 0) {
            appendLog("Lỗi nhập liệu: m và n phải lớn hơn 0.");
            showWarning("m và n phải lớn hơn 0.");
            return null;
        }

        if (m < 2 * n) {
            appendLog("Không đủ cán bộ. Cần ít nhất 2*n cán bộ để coi thi.");
            showWarning("Không đủ cán bộ. Cần ít nhất 2*n cán bộ để coi thi.");
            return null;
        }

        return new ExamSessionInfo(m, n);
    }

    private void appendResultFiles(List<File> files) {
        appendLog("Hoàn tất phân công, đã nhận " + files.size() + " file Excel.");
        for (File file : files) {
            appendLog("File: " + file.getName() + " - " + file.getAbsolutePath());
        }
    }

    private void appendLog(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtLog.append("[" + time + "] " + message + System.lineSeparator());
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void updateConnectionStatus(boolean connected) {
        styleStatusBadge(connected);
        btnImportExcel.setEnabled(connected);
        btnRunAssign.setEnabled(connected);
        updateStatusBar("Sẵn sàng");
    }

    private void setBusy(boolean busy, String message) {
        Cursor cursor = Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR);
        setCursor(cursor);
        btnConnect.setCursor(cursor);
        updateStatusBar(message);
    }

    private void updateStatusBar(String message) {
        String connectionText = socketService.isConnected() ? "Đã kết nối" : "Chưa kết nối";
        lblStatusBar.setText(message + " | " + connectionText + " | " + getServerAddress());
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_CARD_TITLE);
        titleLabel.setForeground(TEXT);

        GridBagConstraints gbc = baseGbc();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(createSeparator(), gbc);
        return panel;
    }

    private JPanel createStackedField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(createLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(MUTED);
        return label;
    }

    private JTextField createTextField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(FONT_BASE);
        field.setForeground(TEXT);
        field.setPreferredSize(new Dimension(160, 38));
        field.setMargin(new Insets(8, 12, 8, 12));
        return field;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, PRIMARY, PRIMARY_HOVER, Color.WHITE);
        return button;
    }

    private JButton createOutlineButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, Color.WHITE, PRIMARY_SOFT, PRIMARY);
        button.setBorder(BorderFactory.createLineBorder(BORDER));
        return button;
    }

    private void styleButton(JButton button, Color normal, Color hover, Color foreground) {
        button.setFont(FONT_BUTTON);
        button.setForeground(foreground);
        button.setBackground(normal);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(250, 40));
        button.setMinimumSize(new Dimension(160, 40));
        button.putClientProperty("JButton.arc", 10);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(normal);
                button.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void styleReadonlyBox(JLabel label) {
        label.setFont(FONT_BASE);
        label.setForeground(PRIMARY);
        label.setOpaque(true);
        label.setBackground(new Color(0xF0F4FF));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private void styleStatusBadge(boolean connected) {
        lblConnectionStatus.setText(connected ? "Đã kết nối" : "Chưa kết nối");
        lblConnectionStatus.setFont(FONT_BOLD);
        lblConnectionStatus.setOpaque(true);
        lblConnectionStatus.setForeground(connected ? SUCCESS : DANGER);
        lblConnectionStatus.setBackground(connected ? new Color(0xECFDF5) : new Color(0xFEF2F2));
        lblConnectionStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(connected ? new Color(0xBBF7D0) : new Color(0xFECACA)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    private void addFormRow(JPanel panel, int row, String label, JTextField field) {
        GridBagConstraints labelGbc = baseGbc();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.weightx = 0;
        labelGbc.insets = new Insets(8, 0, 8, 16);
        panel.add(createLabel(label), labelGbc);

        GridBagConstraints fieldGbc = baseGbc();
        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        fieldGbc.weightx = 1;
        fieldGbc.insets = new Insets(8, 0, 8, 0);
        panel.add(field, fieldGbc);
    }

    private JPanel createSeparator() {
        JPanel separator = new JPanel(new BorderLayout());
        separator.setBackground(BORDER);
        separator.setPreferredSize(new Dimension(1, 1));
        return separator;
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        return gbc;
    }

    private String getServerAddress() {
        return txtIp.getText().trim() + ":" + txtPort.getText().trim();
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
    }

    private String getErrorMessage(Exception exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception.getMessage() : cause.getMessage();
    }
}
