package ui;

import socket.ClientConnectionListener;
import socket.ServerSocketService;
import util.AppConstants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerFrame extends JFrame implements ClientConnectionListener {
    private static final Color APP_BG = new Color(0xF7F8FC);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(0xD6DBE6);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_HOVER = new Color(0x1D4ED8);
    private static final Color DANGER = new Color(0xDC2626);
    private static final Color DANGER_HOVER = new Color(0xB91C1C);
    private static final Color SUCCESS = new Color(0x16A34A);
    private static final Color TEXT = new Color(0x111827);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color INFO_BLUE = new Color(0x1D4ED8);
    private static final Color ACTIVE_LIME = new Color(0x84CC16);
    private static final Color ACTIVE_LIME_SOFT = new Color(0xECFCCB);
    private static final Color ACTIVE_LIME_TEXT = new Color(0x365314);
    private static final Color TABLE_HEADER = new Color(0xEAF1FF);
    private static final Color STATUS_BG = new Color(0xEAF1FF);
    private static final Color LOG_BG = new Color(0x1E1E1E);
    private static final Color LOG_HEADER = new Color(0x252525);
    private static final Color LOG_TEXT = new Color(0xE5E7EB);

    private static final Font FONT_BASE = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);

    private final JTextField txtPort = createTextField(String.valueOf(AppConstants.DEFAULT_PORT));
    private final JButton btnStartServer = createActionButton("Start Server", PRIMARY, PRIMARY_HOVER);
    private final JButton btnStopServer = createActionButton("Stop Server", DANGER, DANGER_HOVER);

    private final JLabel lblTitle = new JLabel("SERVER PHÂN CÔNG CÁN BỘ COI THI");
    private final JLabel lblIp = new JLabel(getLocalIp());
    private final JLabel lblLocalhost = new JLabel("127.0.0.1");
    private final JLabel lblServerPort = new JLabel(String.valueOf(AppConstants.DEFAULT_PORT));
    private final JLabel lblStatus = new JLabel();
    private final JLabel lblOnlineCount = new JLabel("0");
    private final JLabel lblActiveBadge = new JLabel("0 Active");
    private final JLabel lblStatusBar = new JLabel();
    private final JTextArea txtLog = new JTextArea();

    private final DefaultTableModel clientTableModel = new DefaultTableModel(
            new Object[]{"STT", "Client IP", "Port", "Thời gian", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblClients = new JTable(clientTableModel);
    private final Map<Integer, Integer> clientRowByPort = new LinkedHashMap<>();
    private ServerSocketService serverSocketService;

    public ServerFrame() {
        initLookAndFeel();
        setTitle("Server - Phân công cán bộ coi thi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(980, 620));
        setSize(1200, 720);
        initComponents();
        registerEvents();
        updateServerStatus(false);
        updateOnlineCount();
        appendLog("Server interface initialized.");
        setLocationRelativeTo(null);
    }

    private void initLookAndFeel() {
        UIManager.put("defaultFont", FONT_BASE);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(APP_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 14, 24));
        setContentPane(root);

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 16, 0);
        center.add(createTopCardsPanel(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        center.add(createLogPanel(), gbc);

        root.add(center, BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(TEXT);
        header.add(lblTitle, BorderLayout.WEST);
        return header;
    }

    private JPanel createTopCardsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 14);

        JPanel leftCards = new JPanel(new GridLayout(1, 2, 18, 0));
        leftCards.setOpaque(false);
        leftCards.add(createControlCard());
        leftCards.add(createInfoCard());

        gbc.gridx = 0;
        gbc.weightx = 0.50;
        panel.add(leftCards, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.50;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(createClientsCard(), gbc);
        return panel;
    }

    private JPanel createControlCard() {
        JPanel card = createCard("Điều khiển");
        GridBagConstraints gbc = baseGbc();

        JLabel portLabel = createUpperLabel("PORT");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(12, 0, 8, 0);
        card.add(portLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 18, 0);
        card.add(txtPort, gbc);

        btnStopServer.setEnabled(false);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(btnStartServer, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(btnStopServer, gbc);
        return card;
    }

    private JPanel createInfoCard() {
        JPanel card = createCard("Thông tin");
        addInfoRow(card, 2, "IP:", lblIp);
        addInfoRow(card, 3, "Localhost:", lblLocalhost);
        addInfoRow(card, 4, "Port:", lblServerPort);
        addInfoRow(card, 5, "Status:", lblStatus);
        addInfoRow(card, 6, "Online count:", lblOnlineCount);

        lblIp.setForeground(INFO_BLUE);
        lblLocalhost.setForeground(INFO_BLUE);
        lblOnlineCount.setForeground(PRIMARY);
        lblOnlineCount.setFont(FONT_BOLD);
        return card;
    }

    private JPanel createClientsCard() {
        JPanel card = createBorderCard();
        card.add(createClientsHeader(), BorderLayout.NORTH);

        styleClientTable();
        JScrollPane scrollPane = new JScrollPane(tblClients);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(560, 230));
        return card;
    }

    private JPanel createClientsHeader() {
        JPanel header = createCardHeader("Client đang kết nối");
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
        styleActiveBadge();
        header.add(lblActiveBadge, BorderLayout.EAST);
        return header;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(LOG_BG);
        panel.setBorder(BorderFactory.createLineBorder(new Color(0x303030)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LOG_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Log realtime");
        title.setFont(FONT_BOLD);
        title.setForeground(LOG_TEXT);
        header.add(title, BorderLayout.WEST);

        txtLog.setEditable(false);
        txtLog.setFont(FONT_MONO);
        txtLog.setBackground(LOG_BG);
        txtLog.setForeground(LOG_TEXT);
        txtLog.setCaretColor(LOG_TEXT);
        txtLog.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createEmptyBorder());
        scrollLog.getViewport().setBackground(LOG_BG);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollLog, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(STATUS_BG);
        status.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));

        lblStatusBar.setFont(FONT_BOLD);
        lblStatusBar.setForeground(TEXT);
        status.add(lblStatusBar, BorderLayout.WEST);
        return status;
    }

    private void registerEvents() {
        btnStartServer.addActionListener(event -> startServer());
        btnStopServer.addActionListener(event -> stopServer());
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên.", "Port không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        serverSocketService = new ServerSocketService(port, this::appendLog, this::updateStatus, this);
        try {
            serverSocketService.start();
            txtPort.setEnabled(false);
            lblServerPort.setText(String.valueOf(port));
            lblIp.setText(getLocalIp());
            updateServerStatus(true);
            updateStatus("Running");
            appendLog("Server initialized on port " + port + ".");
            appendLog("Listening for incoming connections...");
        } catch (Exception exception) {
            appendLog("Lỗi start server: " + exception.getMessage());
            JOptionPane.showMessageDialog(this, "Không khởi động được server: " + exception.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (serverSocketService != null) {
            serverSocketService.stop();
        }
        txtPort.setEnabled(true);
        updateServerStatus(false);
        updateStatus("Stopped");
        appendLog("Server stopped.");
    }

    @Override
    public void clientConnected(Socket socket) {
        SwingUtilities.invokeLater(() -> {
            addOrUpdateClient(socket, "Connected");
            updateOnlineCount();
        });
    }

    @Override
    public void clientDisconnected(Socket socket) {
        SwingUtilities.invokeLater(() -> {
            addOrUpdateClient(socket, "Disconnected");
            updateOnlineCount();
        });
    }

    private void updateServerStatus(boolean running) {
        btnStartServer.setEnabled(!running);
        btnStopServer.setEnabled(running);
        styleStatusBadge(running);
        updateStatusBar(running ? "Ready" : "Ready");
    }

    private void updateOnlineCount() {
        int onlineCount = clientRowByPort.size();
        lblOnlineCount.setText(String.valueOf(onlineCount));
        lblActiveBadge.setText(onlineCount + " Active");
        updateStatusBar(serverSocketService != null ? "Ready" : "Ready");
    }

    private void addOrUpdateClient(Socket socket, String status) {
        Integer row = clientRowByPort.get(socket.getPort());
        if ("Disconnected".equals(status)) {
            row = clientRowByPort.remove(socket.getPort());
        }

        String clientIp = socket.getInetAddress().getHostAddress();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        if (row != null && row < clientTableModel.getRowCount()) {
            clientTableModel.setValueAt(status, row, 4);
            return;
        }

        if ("Connected".equals(status)) {
            int newRow = clientTableModel.getRowCount();
            clientRowByPort.put(socket.getPort(), newRow);
            clientTableModel.addRow(new Object[]{
                    newRow + 1,
                    clientIp,
                    socket.getPort(),
                    time,
                    status
            });
        }
    }

    private String getLocalIp() {
        return ServerSocketService.getLanIpAddress();
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLog.append("[" + time + "] " + message + System.lineSeparator());
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            lblStatusBar.setText(status + " | " + getRunningText() + " | Clients: "
                    + clientRowByPort.size() + " | " + lblIp.getText() + ":" + lblServerPort.getText());
        });
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = baseGbc();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(createCardHeader(title), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(createSeparator(), gbc);
        return panel;
    }

    private JPanel createBorderCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createLineBorder(BORDER));
        return panel;
    }

    private JPanel createCardHeader(String title) {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_CARD_TITLE);
        titleLabel.setForeground(TEXT);
        header.add(titleLabel, BorderLayout.WEST);
        return header;
    }

    private void addInfoRow(JPanel panel, int row, String key, JLabel value) {
        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(FONT_BASE);
        keyLabel.setForeground(new Color(0x4B5563));

        value.setFont(FONT_BOLD);
        value.setForeground(TEXT);

        GridBagConstraints gbc = baseGbc();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.insets = new Insets(8, 0, 8, 12);
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(8, 0, 8, 0);
        panel.add(value, gbc);
    }

    private JLabel createUpperLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(MUTED);
        return label;
    }

    private JTextField createTextField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(FONT_BOLD);
        field.setForeground(TEXT);
        field.setPreferredSize(new Dimension(160, 42));
        field.setMargin(new Insets(9, 12, 9, 12));
        return field;
    }

    private JButton createActionButton(String text, Color normal, Color hover) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setForeground(Color.WHITE);
        button.setBackground(normal);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(220, 48));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.arc", 8);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                button.setBackground(normal);
            }
        });
        return button;
    }

    private void styleStatusBadge(boolean running) {
        lblStatus.setText(running ? "● Running" : "● Stopped");
        lblStatus.setFont(FONT_BOLD);
        lblStatus.setOpaque(true);
        lblStatus.setForeground(running ? SUCCESS : DANGER);
        lblStatus.setBackground(running ? new Color(0xECFDF5) : new Color(0xFEF2F2));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    private void styleActiveBadge() {
        lblActiveBadge.setFont(FONT_BOLD);
        lblActiveBadge.setForeground(ACTIVE_LIME_TEXT);
        lblActiveBadge.setOpaque(true);
        lblActiveBadge.setBackground(ACTIVE_LIME_SOFT);
        lblActiveBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACTIVE_LIME),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void styleClientTable() {
        tblClients.setFont(FONT_BASE);
        tblClients.setForeground(TEXT);
        tblClients.setRowHeight(34);
        tblClients.setShowGrid(false);
        tblClients.setIntercellSpacing(new Dimension(0, 0));
        tblClients.setFillsViewportHeight(true);
        tblClients.setSelectionBackground(new Color(0xDBEAFE));
        tblClients.setSelectionForeground(TEXT);

        JTableHeader header = tblClients.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setForeground(TEXT);
        header.setBackground(TABLE_HEADER);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tblClients.getColumnCount(); i++) {
            tblClients.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        tblClients.getColumnModel().getColumn(4).setCellRenderer(new ClientStatusRenderer());
    }

    private JPanel createSeparator() {
        JPanel separator = new JPanel();
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

    private void updateStatusBar(String latestStatus) {
        lblStatusBar.setText(latestStatus + " | " + getRunningText() + " | Clients: "
                + clientRowByPort.size() + " | " + lblIp.getText() + ":" + lblServerPort.getText());
    }

    private String getRunningText() {
        return btnStopServer.isEnabled() ? "Running" : "Stopped";
    }

    private static class ClientStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.CENTER);
            if (!isSelected) {
                component.setBackground(Color.WHITE);
                String status = String.valueOf(value);
                component.setForeground("Connected".equals(status) ? SUCCESS : DANGER);
            }
            setFont(FONT_BOLD);
            return component;
        }
    }
}
