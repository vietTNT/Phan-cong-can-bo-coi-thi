package dao;

import model.CanBo;
import model.ExamSessionInfo;
import model.GiamSat;
import model.PhanCong;
import model.PhongThi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class PhanCongDAO {
    // hàm này tải lịch sử phân công từ database.
    public AssignmentHistory loadHistory() throws SQLException {
        // tạo đối tượng lưu lịch sử.
        AssignmentHistory history = new AssignmentHistory();
        // mở kết nối database, dùng try-with-resources nên kết nối sẽ tự đóng sau khi dùng xong.
        try (Connection connection = DBConnection.getConnection()) {
            // lần lượt tải lịch sử cán bộ đã coi phòng nào, cặp cán bộ đã đi chung, và cán bộ đã giám sát phòng nào.
            loadCanBoPhongHistory(connection, history);
            loadPairHistory(connection, history);
            loadGiamSatHistory(connection, history);
        }
        // trả về lịch sử đã tải.
        return history;
    }

    // hàm này lưu dữ liệu đầu vào gồm danh sách cán bộ và phòng thi.
    public void saveInputData(List<CanBo> danhSachCanBo, List<PhongThi> danhSachPhongThi) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            // lưu lại trạng thái auto commit ban đầu để khôi phục sau khi xử lý xong.
            boolean oldAutoCommit = connection.getAutoCommit();
            // tắt tự động commit để dùng transaction: hoặc lưu thành công hết, hoặc nếu lỗi thì hoàn tác hết.
            connection.setAutoCommit(false);
            try {
                // xóa dữ liệu cũ trong bảng can_bo và phong_thi.
                clearInputData(connection);
                // lưu danh sách cán bộ và phòng thi mới.
                saveCanBo(connection, danhSachCanBo);
                savePhongThi(connection, danhSachPhongThi);
                // nếu không lỗi, xác nhận lưu vào database.
                connection.commit();
            } catch (SQLException exception) {
                // nếu có lỗi, quay lại trạng thái trước khi lưu.
                connection.rollback();
                throw exception;
            } finally {
                // khôi phục trạng thái auto commit ban đầu.
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    // lấy danh sách cán bộ từ database để phân công.
    public List<CanBo> findCanBoForAssignment(int limit) throws SQLException {
        // lấy cán bộ từ bảng can_bo, sắp xếp theo tt; nếu tt null thì đưa xuống cuối, rồi giới hạn số lượng.
        String sql = """
                SELECT tt, ho_ten, ngay_sinh, ma_gv, don_vi_cong_tac
                FROM can_bo
                ORDER BY COALESCE(tt, 2147483647), ma_gv
                LIMIT ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            // gán giá trị cho dấu ? trong LIMIT.
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CanBo> result = new java.util.ArrayList<>();
                // duyệt từng dòng kết quả, tạo object CanBo từ dữ liệu database rồi thêm vào danh sách.
                while (resultSet.next()) {
                    result.add(new CanBo(
                            resultSet.getInt("tt"),
                            resultSet.getString("ho_ten"),
                            resultSet.getString("ngay_sinh"),
                            resultSet.getString("ma_gv"),
                            resultSet.getString("don_vi_cong_tac")
                    ));
                }
                return result;
            }
        }
    }

    // lấy danh sách phòng thi từ database để phân công.
    public List<PhongThi> findPhongThiForAssignment(int limit) throws SQLException {
        // lấy phòng thi từ bảng phong_thi, sắp xếp theo stt; nếu stt null thì đưa xuống cuối, rồi giới hạn số lượng.
        String sql = """
                SELECT stt, phong_thi, ghi_chu
                FROM phong_thi
                ORDER BY COALESCE(stt, 2147483647), phong_thi
                LIMIT ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            // gán số lượng phòng thi cần lấy vào dấu ?.
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PhongThi> result = new java.util.ArrayList<>();
                // duyệt từng dòng kết quả, tạo object PhongThi rồi thêm vào danh sách.
                while (resultSet.next()) {
                    result.add(new PhongThi(
                            resultSet.getInt("stt"),
                            resultSet.getString("phong_thi"),
                            resultSet.getString("ghi_chu")
                    ));
                }
                return result;
            }
        }
    }

    // hàm này lưu kết quả phân công vào database: thông tin ca thi, phân công coi thi và phân công giám sát.
    public long saveAssignmentResult(String maCaThi,
                                     String tenFileExcel,
                                     ExamSessionInfo sessionInfo,
                                     List<PhanCong> danhSachPhanCong,
                                     List<GiamSat> danhSachGiamSat) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            // dùng transaction để đảm bảo tạo ca thi và lưu các danh sách phân công cùng thành công hoặc cùng rollback.
            connection.setAutoCommit(false);
            try {
                // tạo một ca thi mới trong bảng ca_thi.
                long caThiId = createCaThi(connection, maCaThi, tenFileExcel, sessionInfo);
                // cập nhật tên ca thành dạng Ca 1, Ca 2, Ca 3... theo ID vừa tạo.
                updateAutoTenCaThi(connection, caThiId);
                // lưu danh sách giám thị coi thi và danh sách giám sát.
                savePhanCongCoiThi(connection, caThiId, danhSachPhanCong);
                savePhanCongGiamSat(connection, caThiId, danhSachGiamSat);
                // lưu thành công thì commit và trả về ID ca thi.
                connection.commit();
                return caThiId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    // hàm này lưu toàn bộ dữ liệu: input cán bộ/phòng thi, ca thi, phân công coi thi và giám sát.
    public long saveFullResult(String maCaThi,
                               String tenFileExcel,
                               ExamSessionInfo sessionInfo,
                               List<CanBo> danhSachCanBo,
                               List<PhongThi> danhSachPhongThi,
                               List<PhanCong> danhSachPhanCong,
                               List<GiamSat> danhSachGiamSat) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                // xóa dữ liệu cán bộ/phòng cũ rồi lưu cán bộ và phòng thi mới.
                clearInputData(connection);
                saveCanBo(connection, danhSachCanBo);
                savePhongThi(connection, danhSachPhongThi);
                // tạo ca thi, cập nhật tên ca tự động, rồi lưu kết quả phân công.
                long caThiId = createCaThi(connection, maCaThi, tenFileExcel, sessionInfo);
                updateAutoTenCaThi(connection, caThiId);
                savePhanCongCoiThi(connection, caThiId, danhSachPhanCong);
                savePhanCongGiamSat(connection, caThiId, danhSachGiamSat);
                // nếu toàn bộ bước lưu thành công thì commit và trả về ID ca thi.
                connection.commit();
                return caThiId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    // lấy danh sách phân công coi thi theo ID ca thi.
    public List<PhanCong> findPhanCongByCaThi(long caThiId) throws SQLException {
        // lấy dữ liệu từ bảng phan_cong_coi_thi theo ca_thi_id và sắp xếp theo stt.
        String sql = """
                SELECT stt, ma_gv, ho_ten, vai_tro, phong_thi
                FROM phan_cong_coi_thi
                WHERE ca_thi_id = ?
                ORDER BY stt
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, caThiId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PhanCong> result = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    // lấy vai trò của cán bộ để xác định là GIAM_THI_1 hay GIAM_THI_2.
                    String vaiTro = resultSet.getString("vai_tro");
                    result.add(new PhanCong(
                            resultSet.getInt("stt"),
                            resultSet.getString("ma_gv"),
                            resultSet.getString("ho_ten"),
                            "GIAM_THI_1".equals(vaiTro),
                            "GIAM_THI_2".equals(vaiTro),
                            resultSet.getString("phong_thi")
                    ));
                }
                return result;
            }
        }
    }

    // lấy danh sách giám sát theo ca thi.
    public List<GiamSat> findGiamSatByCaThi(long caThiId) throws SQLException {
        // lấy dữ liệu từ bảng phan_cong_giam_sat theo ca_thi_id và sắp xếp theo stt.
        String sql = """
                SELECT stt, ma_gv, ho_ten, phong_thi_duoc_giam_sat, mo_ta_phong_giam_sat
                FROM phan_cong_giam_sat
                WHERE ca_thi_id = ?
                ORDER BY stt
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, caThiId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GiamSat> result = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    result.add(new GiamSat(
                            resultSet.getInt("stt"),
                            resultSet.getString("ma_gv"),
                            resultSet.getString("ho_ten"),
                            // truyền danh sách phòng rỗng vì dữ liệu phòng đã được lưu dạng mô tả chuỗi trong database.
                            List.of(),
                            resultSet.getString("mo_ta_phong_giam_sat")
                    ));
                }
                return result;
            }
        }
    }

    // lưu danh sách cán bộ vào bảng can_bo.
    private void saveCanBo(Connection connection, List<CanBo> danhSachCanBo) throws SQLException {
        String sql = """
                INSERT INTO can_bo (ma_gv, tt, ho_ten, ngay_sinh, don_vi_cong_tac)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // duyệt từng cán bộ và gán dữ liệu vào các dấu ?.
            for (CanBo canBo : danhSachCanBo) {
                statement.setString(1, canBo.getMaCanBo());
                statement.setInt(2, canBo.getStt());
                statement.setString(3, canBo.getHoTen());
                statement.setString(4, canBo.getNgaySinh());
                statement.setString(5, canBo.getDonViCongTac());
                statement.addBatch();
            }
            // chạy tất cả câu insert một lần, dùng batch giúp lưu nhiều dòng nhanh hơn.
            statement.executeBatch();
        }
    }

    // xóa toàn bộ dữ liệu trong bảng cán bộ và phòng thi.
    private void clearInputData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM can_bo");
            statement.executeUpdate("DELETE FROM phong_thi");
        }
    }

    // lưu danh sách phòng thi vào database.
    private void savePhongThi(Connection connection, List<PhongThi> danhSachPhongThi) throws SQLException {
        // nếu ma_phong đã tồn tại thì cập nhật lại dữ liệu cũ thay vì báo lỗi.
        String sql = """
                INSERT INTO phong_thi (ma_phong, phong_thi, stt, ghi_chu)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    ma_phong = VALUES(ma_phong),
                    phong_thi = VALUES(phong_thi),
                    stt = VALUES(stt),
                    ghi_chu = VALUES(ghi_chu)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PhongThi phongThi : danhSachPhongThi) {
                // ma_phong và phong_thi đều lấy cùng giá trị phòng thi, ví dụ P101.
                statement.setString(1, phongThi.getPhongThi());
                statement.setString(2, phongThi.getPhongThi());
                statement.setInt(3, phongThi.getStt());
                statement.setString(4, phongThi.getGhiChu());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    // tạo một ca thi mới trong bảng ca_thi.
    private long createCaThi(Connection connection, String maCaThi, String tenFileExcel, ExamSessionInfo sessionInfo) throws SQLException {
        String sql = """
                INSERT INTO ca_thi (ma_ca_thi, ten_ca, ten_ca_thi, so_luong_can_bo, so_luong_phong_thi, ten_file_excel)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        // yêu cầu database trả về ID tự tăng vừa tạo.
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, maCaThi);
            statement.setString(2, sessionInfo.getTenCaThi());
            statement.setString(3, sessionInfo.getTenCaThi());
            statement.setInt(4, sessionInfo.getSoLuongCanBo());
            statement.setInt(5, sessionInfo.getSoLuongPhongThi());
            statement.setString(6, tenFileExcel);
            statement.executeUpdate();
            // lấy ID vừa được sinh ra và trả về cho hàm gọi.
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        // nếu không lấy được ID thì báo lỗi.
        throw new SQLException("Không lấy được ID ca thi vừa tạo");
    }

    // cập nhật tên ca thi sau khi đã có ID, ví dụ caThiId = 5 thì ten_ca và ten_ca_thi là Ca 5.
    private void updateAutoTenCaThi(Connection connection, long caThiId) throws SQLException {
        String sql = "UPDATE ca_thi SET ten_ca = ?, ten_ca_thi = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "Ca " + caThiId);
            statement.setString(2, "Ca " + caThiId);
            statement.setLong(3, caThiId);
            statement.executeUpdate();
        }
    }

    // lưu kết quả phân công coi thi.
    private void savePhanCongCoiThi(Connection connection, long caThiId, List<PhanCong> danhSachPhanCong) throws SQLException {
        String sql = """
                INSERT INTO phan_cong_coi_thi (ca_thi_id, stt, ma_gv, ho_ten, vai_tro, phong_thi)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PhanCong phanCong : danhSachPhanCong) {
                statement.setLong(1, caThiId);
                statement.setInt(2, phanCong.getStt());
                statement.setString(3, phanCong.getMaCanBo());
                statement.setString(4, phanCong.getHoTen());
                // nếu là giám thị 1 thì lưu GIAM_THI_1, ngược lại lưu GIAM_THI_2.
                statement.setString(5, phanCong.isGiamThi1() ? "GIAM_THI_1" : "GIAM_THI_2");
                statement.setString(6, phanCong.getPhongThi());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    // lưu kết quả giám sát.
    private void savePhanCongGiamSat(Connection connection, long caThiId, List<GiamSat> danhSachGiamSat) throws SQLException {
        String sql = """
                INSERT INTO phan_cong_giam_sat (ca_thi_id, stt, ma_gv, ho_ten, phong_thi_duoc_giam_sat, mo_ta_phong_giam_sat)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (GiamSat giamSat : danhSachGiamSat) {
                statement.setLong(1, caThiId);
                statement.setInt(2, giamSat.getStt());
                statement.setString(3, giamSat.getMaCanBo());
                statement.setString(4, giamSat.getHoTen());
                // chuyển danh sách phòng giám sát thành chuỗi, ví dụ [P101, P102] thành P101, P102.
                statement.setString(5, giamSat.getDanhSachPhongGiamSat().stream()
                        .map(PhongThi::getPhongThi)
                        .collect(Collectors.joining(", ")));
                statement.setString(6, giamSat.getMoTaPhongGiamSat());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    // lấy toàn bộ lịch sử cán bộ đã từng coi phòng nào.
    private void loadCanBoPhongHistory(Connection connection, AssignmentHistory history) throws SQLException {
        String sql = "SELECT ma_gv, phong_thi FROM phan_cong_coi_thi";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                // thêm mã cán bộ và phòng thi đã coi vào AssignmentHistory.
                history.addCanBoPhong(resultSet.getString("ma_gv"), resultSet.getString("phong_thi"));
            }
        }
    }

    // tìm các cặp cán bộ đã từng đi chung trong cùng ca thi và cùng phòng thi.
    private void loadPairHistory(Connection connection, AssignmentHistory history) throws SQLException {
        // điều kiện a.ma_gv < b.ma_gv giúp tránh lặp cặp như CB01-CB02 và CB02-CB01.
        String sql = """
                SELECT a.ma_gv AS ma_1, b.ma_gv AS ma_2
                FROM phan_cong_coi_thi a
                JOIN phan_cong_coi_thi b
                    ON a.ca_thi_id = b.ca_thi_id
                    AND a.phong_thi = b.phong_thi
                    AND a.ma_gv < b.ma_gv
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                // lưu cặp cán bộ đã từng coi chung một phòng vào AssignmentHistory.
                history.addCanBoPair(resultSet.getString("ma_1"), resultSet.getString("ma_2"));
            }
        }
    }

    // lấy lịch sử giám sát.
    private void loadGiamSatHistory(Connection connection, AssignmentHistory history) throws SQLException {
        String sql = "SELECT ma_gv, phong_thi_duoc_giam_sat FROM phan_cong_giam_sat";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String rooms = resultSet.getString("phong_thi_duoc_giam_sat");
                // nếu danh sách phòng rỗng thì bỏ qua.
                if (rooms == null || rooms.isBlank()) {
                    continue;
                }
                // tách chuỗi phòng theo dấu phẩy, ví dụ P101, P102, P103 thành từng phòng riêng.
                for (String room : rooms.split(",")) {
                    // thêm từng phòng vào lịch sử giám sát.
                    history.addGiamSatPhong(resultSet.getString("ma_gv"), room.trim());
                }
            }
        }
    }
}
