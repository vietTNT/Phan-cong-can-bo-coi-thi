package socket;

import model.ExamSessionInfo;
import util.AppConstants;
import util.FileTransferUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

public class ClientSocketService {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public void connect(String host, int port, Consumer<String> logConsumer) throws IOException {
        disconnect();
        log(logConsumer, "Đang kết nối server " + host + ":" + port);
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        log(logConsumer, "Đã kết nối server");
    }

    public AssignmentResponse sendAssignmentRequest(ExamSessionInfo sessionInfo,
                                                    Consumer<String> logConsumer) throws IOException {
        if (!isConnected() || out == null || in == null) {
            throw new IOException("Client chưa kết nối server");
        }
        Files.createDirectories(AppConstants.CLIENT_DOWNLOAD_DIR);

        log(logConsumer, "Đang gửi thông tin kỳ thi");
        out.writeUTF("ASSIGN_REQUEST");
        out.writeInt(sessionInfo.getSoLuongCanBo());
        out.writeInt(sessionInfo.getSoLuongPhongThi());
        out.writeUTF(sessionInfo.getTenCaThi());
        out.flush();

        log(logConsumer, "Server đang xử lý từ dữ liệu database");
        String status = in.readUTF();
        if ("ERROR".equals(status)) {
            throw new IOException(in.readUTF());
        }
        if (!"SUCCESS".equals(status)) {
            throw new IOException("Server trả trạng thái không hợp lệ: " + status);
        }

        String tenCaThi = in.readUTF();
        log(logConsumer, "Ca thi: " + tenCaThi);
        List<File> resultFiles = FileTransferUtil.receiveMultipleFiles(in, AppConstants.CLIENT_DOWNLOAD_DIR.toFile());
        for (File resultFile : resultFiles) {
            log(logConsumer, "Đã nhận " + resultFile.getName());
        }
        return new AssignmentResponse(tenCaThi, resultFiles);
    }

    public ImportResponse sendImportRequest(File excelFile, Consumer<String> logConsumer) throws IOException {
        if (!isConnected() || out == null || in == null) {
            throw new IOException("Client chưa kết nối server");
        }

        log(logConsumer, "Đang gửi file Excel để import vào database");
        out.writeUTF("IMPORT_REQUEST");
        FileTransferUtil.sendMultipleFiles(out, List.of(excelFile));

        String status = in.readUTF();
        if ("ERROR".equals(status)) {
            throw new IOException(in.readUTF());
        }
        if (!"SUCCESS".equals(status)) {
            throw new IOException("Server trả trạng thái không hợp lệ: " + status);
        }

        int soCanBo = in.readInt();
        int soPhongThi = in.readInt();
        log(logConsumer, "Server đã import vào database: " + soCanBo + " cán bộ, " + soPhongThi + " phòng thi");
        return new ImportResponse(soCanBo, soPhongThi);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() {
        closeQuietly(in);
        closeQuietly(out);
        closeQuietly(socket);
        in = null;
        out = null;
        socket = null;
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // Closing an old socket is best-effort only.
        }
    }

    private void log(Consumer<String> logConsumer, String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }
}
