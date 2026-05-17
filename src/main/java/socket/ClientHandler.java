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

// class này implements Runnable nên mỗi client có thể được xử lý trong một thread riêng.
public class ClientHandler implements Runnable {
    // lưu socket kết nối với client.
    private final Socket clientSocket;
    // dùng để ghi log ra giao diện server.
    private final Consumer<String> logConsumer;
    // dùng để cập nhật trạng thái server.
    private final Consumer<String> statusConsumer;
    // dùng để báo khi client ngắt kết nối.
    private final ClientConnectionListener connectionListener;

    // constructor nhận thông tin cần thiết khi có client kết nối.
    public ClientHandler(Socket clientSocket,
                         Consumer<String> logConsumer,
                         Consumer<String> statusConsumer,
                         ClientConnectionListener connectionListener) {
        // gán dữ liệu truyền vào cho biến của class.
        this.clientSocket = clientSocket;
        this.logConsumer = logConsumer;
        this.statusConsumer = statusConsumer;
        this.connectionListener = connectionListener;
    }

    @Override
    public void run() {
        // lấy địa chỉ IP của client.
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        // mở socket và 2 luồng dữ liệu: DataInputStream để nhận dữ liệu, DataOutputStream để gửi dữ liệu về client.
        // dùng try-with-resources nên khi xong sẽ tự đóng socket và stream.
        try (Socket socket = clientSocket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            try {
                // khi socket chưa đóng, server tiếp tục xử lý yêu cầu từ client.
                while (!socket.isClosed()) {
                    processClient(socket, in, out);
                }
            } catch (EOFException ignored) {
                // nếu client đóng kết nối sau khi gửi xong request thì không coi là lỗi nghiêm trọng.
            } catch (Exception exception) {
                // nếu xử lý bị lỗi, server gửi về client ERROR và nội dung lỗi.
                out.writeUTF("ERROR");
                out.writeUTF(exception.getMessage() == null ? "Lỗi không xác định" : exception.getMessage());
                out.flush();
                // cập nhật trạng thái và ghi log lỗi.
                status("Lỗi xử lý client");
                log("Lỗi xử lý request từ " + clientIp + ": " + exception.getMessage());
            }
        } catch (Exception exception) {
            log("Lỗi kết nối client " + clientIp + ": " + exception.getMessage());
        } finally {
            // khối này luôn chạy dù xử lý thành công hay lỗi.
            if (connectionListener != null) {
                // báo cho server biết client đã ngắt kết nối.
                connectionListener.clientDisconnected(clientSocket);
            }
            // cập nhật trạng thái và ghi log client đã rời.
            status("client disconnected: " + clientIp);
            log("client disconnected: " + clientIp);
        }
    }

    // hàm này đọc lệnh từ client và điều hướng xử lý.
    private void processClient(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        // đọc chuỗi lệnh client gửi.
        String command = in.readUTF();
        // nếu client gửi IMPORT_REQUEST, server xử lý import Excel.
        if ("IMPORT_REQUEST".equals(command)) {
            processImportRequest(socket, in, out);
            return;
        }
        // nếu client gửi ASSIGN_REQUEST, server xử lý phân công.
        if ("ASSIGN_REQUEST".equals(command)) {
            processAssignmentRequest(socket, in, out);
            return;
        }
        // nếu lệnh không hợp lệ thì báo lỗi.
        throw new IllegalArgumentException("Yêu cầu không hợp lệ: " + command);
    }

    // hàm này xử lý yêu cầu phân công.
    private void processAssignmentRequest(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        // đọc thông tin ca thi từ client: số cán bộ cần lấy, số phòng cần lấy, và tên ca thi hoặc ghi chú.
        ExamSessionInfo sessionInfo = new ExamSessionInfo(
                in.readInt(),
                in.readInt(),
                in.readUTF()
        );

        // tạo tên phiên phân công theo thời gian, ví dụ ca_thi_20260518_003500_456.
        String sessionName = buildSessionName();
        // tạo thư mục lưu file Excel kết quả.
        File outputDir = AppConstants.SERVER_OUTPUT_DIR.resolve(sessionName).toFile();
        Files.createDirectories(outputDir.toPath());

        status("request received");
        log("request received từ " + socket.getInetAddress().getHostAddress()
                + " - lấy dữ liệu từ database");

        // đảm bảo database và bảng đã được tạo.
        new DatabaseInitializer().initialize();
        // tạo DAO để làm việc với database.
        PhanCongDAO dao = new PhanCongDAO();
        // lấy danh sách cán bộ và phòng thi từ database theo số lượng client yêu cầu.
        List<CanBo> danhSachCanBo = dao.findCanBoForAssignment(sessionInfo.getSoLuongCanBo());
        List<PhongThi> danhSachPhongThi = dao.findPhongThiForAssignment(sessionInfo.getSoLuongPhongThi());
        // kiểm tra dữ liệu có đủ để phân công không.
        validateSessionInfo(sessionInfo, danhSachCanBo.size(), danhSachPhongThi.size());
        log("Đã lấy dữ liệu từ database: " + danhSachCanBo.size()
                + " cán bộ, " + danhSachPhongThi.size() + " phòng thi");

        // tải lịch sử phân công cũ để tránh cán bộ coi lại phòng cũ, giám sát lại phòng cũ, hoặc đi chung cặp cũ.
        AssignmentHistory history = dao.loadHistory();
        log("Đã load lịch sử phân công từ database");

        // gọi thuật toán phân công, kết quả gồm danh sách giám thị coi thi và danh sách giám sát.
        PhanCongService.AssignmentResult result = new PhanCongService().phanCong(
                danhSachCanBo,
                danhSachPhongThi,
                history
        );
        // lưu kết quả phân công vào database.
        long caThiId = dao.saveAssignmentResult(
                sessionName,
                "database",
                sessionInfo,
                result.getDanhSachPhanCong(),
                result.getDanhSachGiamSat());
        // tạo tên ca thi theo ID, ví dụ Ca 1, Ca 2, Ca 3.
        String tenCaThi = "Ca " + caThiId;
        log("Phân công thành công, đã lưu database ca_thi.id = " + caThiId);

        // tạo công cụ xuất Excel và tạo 2 file kết quả: phân công coi thi và giám sát.
        ExcelUtil excelUtil = new ExcelUtil();
        File phanCongFile = new File(outputDir, buildOutputFileName(AppConstants.OUTPUT_PHAN_CONG_FILE, tenCaThi));
        File giamSatFile = new File(outputDir, buildOutputFileName(AppConstants.OUTPUT_GIAM_SAT_FILE, tenCaThi));
        // xuất danh sách phân công coi thi và danh sách giám sát ra Excel.
        excelUtil.writeDanhSachPhanCong(dao.findPhanCongByCaThi(caThiId), phanCongFile);
        excelUtil.writeDanhSachGiamSat(dao.findGiamSatByCaThi(caThiId), giamSatFile);
        log("Xuất Excel hoàn tất");

        // gửi về client SUCCESS, tên ca thi, và 2 file Excel kết quả.
        out.writeUTF("SUCCESS");
        out.writeUTF(tenCaThi);
        FileTransferUtil.sendMultipleFiles(out, List.of(phanCongFile, giamSatFile));
        status("Đã gửi file kết quả");
        log("Đã gửi file kết quả cho client");
    }

    // hàm này xử lý khi client gửi file Excel lên server.
    private void processImportRequest(Socket socket, DataInputStream in, DataOutputStream out) throws Exception {
        // tạo tên phiên import theo thời gian, ví dụ import_20260518_003012_123.
        String sessionName = "import_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        // tạo thư mục để lưu file client gửi lên.
        File uploadDir = AppConstants.SERVER_UPLOAD_DIR.resolve(sessionName).toFile();
        Files.createDirectories(uploadDir.toPath());

        status("request received");
        log("import request received từ " + socket.getInetAddress().getHostAddress());
        // nhận file từ client và lưu vào thư mục upload.
        List<File> inputFiles = FileTransferUtil.receiveMultipleFiles(in, uploadDir);
        // server yêu cầu client chỉ gửi đúng 1 file Excel gồm 2 sheet.
        if (inputFiles.size() != 1) {
            throw new IllegalArgumentException("Client phải gửi đúng 1 file Excel gồm 2 sheet");
        }

        // đọc file Excel, trong đó có danh sách cán bộ và danh sách phòng thi.
        ExcelUtil.ExcelData excelData = new ExcelUtil().readInputWorkbook(inputFiles.get(0));
        // khởi tạo database nếu chưa có bảng.
        new DatabaseInitializer().initialize();
        // lưu dữ liệu từ Excel vào database.
        new PhanCongDAO().saveInputData(excelData.danhSachCanBo(), excelData.danhSachPhongThi());

        // gửi kết quả về client: SUCCESS, số lượng cán bộ đã import, số lượng phòng thi đã import.
        out.writeUTF("SUCCESS");
        out.writeInt(excelData.danhSachCanBo().size());
        out.writeInt(excelData.danhSachPhongThi().size());
        out.flush();
        status("Import completed");
        log("Import completed và đã lưu database: " + excelData.danhSachCanBo().size()
                + " cán bộ, " + excelData.danhSachPhongThi().size() + " phòng thi");
    }

    // kiểm tra dữ liệu ca thi trước khi phân công.
    private void validateSessionInfo(ExamSessionInfo sessionInfo, int actualTeachers, int actualRooms) {
        // số cán bộ và số phòng phải lớn hơn 0.
        if (sessionInfo.getSoLuongCanBo() <= 0 || sessionInfo.getSoLuongPhongThi() <= 0) {
            throw new IllegalArgumentException("m và n phải lớn hơn 0");
        }
        // nếu database không đủ số cán bộ yêu cầu thì báo lỗi.
        if (actualTeachers < sessionInfo.getSoLuongCanBo()) {
            throw new IllegalArgumentException("Database chỉ có " + actualTeachers
                    + " cán bộ, không đủ theo m = " + sessionInfo.getSoLuongCanBo());
        }
        // nếu database không đủ số phòng yêu cầu thì báo lỗi.
        if (actualRooms < sessionInfo.getSoLuongPhongThi()) {
            throw new IllegalArgumentException("Database chỉ có " + actualRooms
                    + " phòng thi, không đủ theo n = " + sessionInfo.getSoLuongPhongThi());
        }
    }

    // tạo tên phiên theo thời gian, ví dụ ca_thi_20260518_003500_456.
    private String buildSessionName() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        return "ca_thi_" + timestamp;
    }

    // tạo tên file kết quả theo tên ca thi, ví dụ phan_cong_Ca_1.xlsx.
    private String buildOutputFileName(String baseFileName, String tenCaThi) {
        // tìm vị trí dấu chấm cuối cùng trong tên file.
        int dotIndex = baseFileName.lastIndexOf('.');
        // lấy phần tên file không có đuôi.
        String name = dotIndex > 0 ? baseFileName.substring(0, dotIndex) : baseFileName;
        // lấy phần đuôi file, ví dụ .xlsx.
        String extension = dotIndex > 0 ? baseFileName.substring(dotIndex) : "";
        // tạo hậu tố tên ca thi, ví dụ "Ca 1" thành "Ca_1".
        String suffix = tenCaThi == null || tenCaThi.isBlank() ? "Ca" : tenCaThi.trim().replaceAll("\\s+", "_");
        // ghép thành tên file hoàn chỉnh.
        return name + "_" + suffix + extension;
    }

    // nếu có logConsumer, gửi message cho nó để hiển thị log.
    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }

    // nếu có statusConsumer, gửi message để cập nhật trạng thái.
    private void status(String message) {
        if (statusConsumer != null) {
            statusConsumer.accept(message);
        }
    }
}
