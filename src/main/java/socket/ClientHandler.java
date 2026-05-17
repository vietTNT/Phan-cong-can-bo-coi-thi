package socket;

import dao.AssignmentHistory;
import dao.DatabaseInitializer;
import dao.PhanCongDAO;
import model.CanBo;
import model.ExamSessionInfo;
import model.PhongThi;
import service.PhanCongService;
import util.AppConstants;
import util.ExcelUtil;
import util.FileTransferUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Consumer<String> logConsumer;
    private final Consumer<String> statusConsumer;
    private final ClientConnectionListener connectionListener;

    public ClientHandler(Socket clientSocket,
                         Consumer<String> logConsumer,
                         Consumer<String> statusConsumer,
                         ClientConnectionListener connectionListener) {
        this.clientSocket = clientSocket;
        this.logConsumer = logConsumer;
        this.statusConsumer = statusConsumer;
        this.connectionListener = connectionListener;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        try (Socket socket = clientSocket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            try {
                while (!socket.isClosed()) {
                    processClient(socket, in, out);
                }
            } catch (EOFException ignored) {
                // Client closed the connection after completing a request.
            } catch (Exception exception) {
                out.writeUTF("ERROR");
                out.writeUTF(exception.getMessage() == null ? "Lỗi không xác định" : exception.getMessage());
                out.flush();
                status("Lỗi xử lý client");
                log("Lỗi xử lý request từ " + clientIp + ": " + exception.getMessage());
            }
        } catch (Exception exception) {
            log("Lỗi kết nối client " + clientIp + ": " + exception.getMessage());
        } finally {
            if (connectionListener != null) {
                connectionListener.clientDisconnected(clientSocket);
            }
            status("client disconnected: " + clientIp);
            log("client disconnected: " + clientIp);
        }
    }

    private void processClient(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        String command = in.readUTF();
        if ("IMPORT_REQUEST".equals(command)) {
            processImportRequest(socket, in, out);
            return;
        }
        if ("ASSIGN_REQUEST".equals(command)) {
            processAssignmentRequest(socket, in, out);
            return;
        }
        throw new IllegalArgumentException("Yêu cầu không hợp lệ: " + command);
    }

    private void processAssignmentRequest(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        ExamSessionInfo sessionInfo = new ExamSessionInfo(
                in.readInt(),
                in.readInt(),
                in.readUTF()
        );

        String sessionName = buildSessionName();
        File outputDir = AppConstants.SERVER_OUTPUT_DIR.resolve(sessionName).toFile();
        Files.createDirectories(outputDir.toPath());

        status("request received");
        log("request received từ " + socket.getInetAddress().getHostAddress()
                + " - lấy dữ liệu từ database");

        new DatabaseInitializer().initialize();
        PhanCongDAO dao = new PhanCongDAO();
        List<CanBo> danhSachCanBo = dao.findCanBoForAssignment(sessionInfo.getSoLuongCanBo());
        List<PhongThi> danhSachPhongThi = dao.findPhongThiForAssignment(sessionInfo.getSoLuongPhongThi());
        validateSessionInfo(sessionInfo, danhSachCanBo.size(), danhSachPhongThi.size());
        log("Đã lấy dữ liệu từ database: " + danhSachCanBo.size()
                + " cán bộ, " + danhSachPhongThi.size() + " phòng thi");

        AssignmentHistory history = dao.loadHistory();
        log("Đã load lịch sử phân công từ database");

        PhanCongService.AssignmentResult result = new PhanCongService().phanCong(
                danhSachCanBo,
                danhSachPhongThi,
                history
        );
        long caThiId = dao.saveAssignmentResult(
                sessionName,
                "database",
                sessionInfo,
                result.getDanhSachPhanCong(),
                result.getDanhSachGiamSat());
        String tenCaThi = "Ca " + caThiId;
        log("Phân công thành công, đã lưu database ca_thi.id = " + caThiId);

        ExcelUtil excelUtil = new ExcelUtil();
        File phanCongFile = new File(outputDir, buildOutputFileName(AppConstants.OUTPUT_PHAN_CONG_FILE, tenCaThi));
        File giamSatFile = new File(outputDir, buildOutputFileName(AppConstants.OUTPUT_GIAM_SAT_FILE, tenCaThi));
        excelUtil.writeDanhSachPhanCong(dao.findPhanCongByCaThi(caThiId), phanCongFile);
        excelUtil.writeDanhSachGiamSat(dao.findGiamSatByCaThi(caThiId), giamSatFile);
        log("Xuất Excel hoàn tất");

        out.writeUTF("SUCCESS");
        out.writeUTF(tenCaThi);
        FileTransferUtil.sendMultipleFiles(out, List.of(phanCongFile, giamSatFile));
        status("Đã gửi file kết quả");
        log("Đã gửi file kết quả cho client");
    }

    private void processImportRequest(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        String sessionName = "import_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        File uploadDir = AppConstants.SERVER_UPLOAD_DIR.resolve(sessionName).toFile();
        Files.createDirectories(uploadDir.toPath());

        status("request received");
        log("import request received từ " + socket.getInetAddress().getHostAddress());
        List<File> inputFiles = FileTransferUtil.receiveMultipleFiles(in, uploadDir);
        if (inputFiles.size() != 1) {
            throw new IllegalArgumentException("Client phải gửi đúng 1 file Excel gồm 2 sheet");
        }

        ExcelUtil.ExcelData excelData = new ExcelUtil().readInputWorkbook(inputFiles.get(0));
        new DatabaseInitializer().initialize();
        new PhanCongDAO().saveInputData(excelData.danhSachCanBo(), excelData.danhSachPhongThi());

        out.writeUTF("SUCCESS");
        out.writeInt(excelData.danhSachCanBo().size());
        out.writeInt(excelData.danhSachPhongThi().size());
        out.flush();
        status("Import completed");
        log("Import completed và đã lưu database: " + excelData.danhSachCanBo().size()
                + " cán bộ, " + excelData.danhSachPhongThi().size() + " phòng thi");
    }

    private void validateSessionInfo(ExamSessionInfo sessionInfo, int actualTeachers, int actualRooms) {
        if (sessionInfo.getSoLuongCanBo() <= 0 || sessionInfo.getSoLuongPhongThi() <= 0) {
            throw new IllegalArgumentException("m và n phải lớn hơn 0");
        }
        if (actualTeachers < sessionInfo.getSoLuongCanBo()) {
            throw new IllegalArgumentException("Database chỉ có " + actualTeachers
                    + " cán bộ, không đủ theo m = " + sessionInfo.getSoLuongCanBo());
        }
        if (actualRooms < sessionInfo.getSoLuongPhongThi()) {
            throw new IllegalArgumentException("Database chỉ có " + actualRooms
                    + " phòng thi, không đủ theo n = " + sessionInfo.getSoLuongPhongThi());
        }
    }

    private String buildSessionName() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        return "ca_thi_" + timestamp;
    }

    private String buildOutputFileName(String baseFileName, String tenCaThi) {
        int dotIndex = baseFileName.lastIndexOf('.');
        String name = dotIndex > 0 ? baseFileName.substring(0, dotIndex) : baseFileName;
        String extension = dotIndex > 0 ? baseFileName.substring(dotIndex) : "";
        String suffix = tenCaThi == null || tenCaThi.isBlank() ? "Ca" : tenCaThi.trim().replaceAll("\\s+", "_");
        return name + "_" + suffix + extension;
    }

    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }

    private void status(String message) {
        if (statusConsumer != null) {
            statusConsumer.accept(message);
        }
    }
}
