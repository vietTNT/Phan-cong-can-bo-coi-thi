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
    public AssignmentHistory loadHistory() throws SQLException {
        AssignmentHistory history = new AssignmentHistory();
        try (Connection connection = DBConnection.getConnection()) {
            loadCanBoPhongHistory(connection, history);
            loadPairHistory(connection, history);
            loadGiamSatHistory(connection, history);
        }
        return history;
    }

    public void saveInputData(List<CanBo> danhSachCanBo, List<PhongThi> danhSachPhongThi) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                clearInputData(connection);
                saveCanBo(connection, danhSachCanBo);
                savePhongThi(connection, danhSachPhongThi);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public List<CanBo> findCanBoForAssignment(int limit) throws SQLException {
        String sql = """
                SELECT tt, ho_ten, ngay_sinh, ma_gv, don_vi_cong_tac
                FROM can_bo
                ORDER BY COALESCE(tt, 2147483647), ma_gv
                LIMIT ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CanBo> result = new java.util.ArrayList<>();
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

    public List<PhongThi> findPhongThiForAssignment(int limit) throws SQLException {
        String sql = """
                SELECT stt, phong_thi, ghi_chu
                FROM phong_thi
                ORDER BY COALESCE(stt, 2147483647), phong_thi
                LIMIT ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PhongThi> result = new java.util.ArrayList<>();
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

    public long saveAssignmentResult(String maCaThi,
                                     String tenFileExcel,
                                     ExamSessionInfo sessionInfo,
                                     List<PhanCong> danhSachPhanCong,
                                     List<GiamSat> danhSachGiamSat) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long caThiId = createCaThi(connection, maCaThi, tenFileExcel, sessionInfo);
                updateAutoTenCaThi(connection, caThiId);
                savePhanCongCoiThi(connection, caThiId, danhSachPhanCong);
                savePhanCongGiamSat(connection, caThiId, danhSachGiamSat);
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
                clearInputData(connection);
                saveCanBo(connection, danhSachCanBo);
                savePhongThi(connection, danhSachPhongThi);
                long caThiId = createCaThi(connection, maCaThi, tenFileExcel, sessionInfo);
                updateAutoTenCaThi(connection, caThiId);
                savePhanCongCoiThi(connection, caThiId, danhSachPhanCong);
                savePhanCongGiamSat(connection, caThiId, danhSachGiamSat);
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

    public List<PhanCong> findPhanCongByCaThi(long caThiId) throws SQLException {
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

    public List<GiamSat> findGiamSatByCaThi(long caThiId) throws SQLException {
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
                            List.of(),
                            resultSet.getString("mo_ta_phong_giam_sat")
                    ));
                }
                return result;
            }
        }
    }

    private void saveCanBo(Connection connection, List<CanBo> danhSachCanBo) throws SQLException {
        String sql = """
                INSERT INTO can_bo (ma_gv, tt, ho_ten, ngay_sinh, don_vi_cong_tac)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (CanBo canBo : danhSachCanBo) {
                statement.setString(1, canBo.getMaCanBo());
                statement.setInt(2, canBo.getStt());
                statement.setString(3, canBo.getHoTen());
                statement.setString(4, canBo.getNgaySinh());
                statement.setString(5, canBo.getDonViCongTac());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void clearInputData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM can_bo");
            statement.executeUpdate("DELETE FROM phong_thi");
        }
    }

    private void savePhongThi(Connection connection, List<PhongThi> danhSachPhongThi) throws SQLException {
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
                statement.setString(1, phongThi.getPhongThi());
                statement.setString(2, phongThi.getPhongThi());
                statement.setInt(3, phongThi.getStt());
                statement.setString(4, phongThi.getGhiChu());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private long createCaThi(Connection connection, String maCaThi, String tenFileExcel, ExamSessionInfo sessionInfo) throws SQLException {
        String sql = """
                INSERT INTO ca_thi (ma_ca_thi, ten_ca, ten_ca_thi, so_luong_can_bo, so_luong_phong_thi, ten_file_excel)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, maCaThi);
            statement.setString(2, sessionInfo.getTenCaThi());
            statement.setString(3, sessionInfo.getTenCaThi());
            statement.setInt(4, sessionInfo.getSoLuongCanBo());
            statement.setInt(5, sessionInfo.getSoLuongPhongThi());
            statement.setString(6, tenFileExcel);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được ID ca thi vừa tạo");
    }

    private void updateAutoTenCaThi(Connection connection, long caThiId) throws SQLException {
        String sql = "UPDATE ca_thi SET ten_ca = ?, ten_ca_thi = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "Ca " + caThiId);
            statement.setString(2, "Ca " + caThiId);
            statement.setLong(3, caThiId);
            statement.executeUpdate();
        }
    }

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
                statement.setString(5, phanCong.isGiamThi1() ? "GIAM_THI_1" : "GIAM_THI_2");
                statement.setString(6, phanCong.getPhongThi());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

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
                statement.setString(5, giamSat.getDanhSachPhongGiamSat().stream()
                        .map(PhongThi::getPhongThi)
                        .collect(Collectors.joining(", ")));
                statement.setString(6, giamSat.getMoTaPhongGiamSat());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void loadCanBoPhongHistory(Connection connection, AssignmentHistory history) throws SQLException {
        String sql = "SELECT ma_gv, phong_thi FROM phan_cong_coi_thi";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                history.addCanBoPhong(resultSet.getString("ma_gv"), resultSet.getString("phong_thi"));
            }
        }
    }

    private void loadPairHistory(Connection connection, AssignmentHistory history) throws SQLException {
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
                history.addCanBoPair(resultSet.getString("ma_1"), resultSet.getString("ma_2"));
            }
        }
    }

    private void loadGiamSatHistory(Connection connection, AssignmentHistory history) throws SQLException {
        String sql = "SELECT ma_gv, phong_thi_duoc_giam_sat FROM phan_cong_giam_sat";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String rooms = resultSet.getString("phong_thi_duoc_giam_sat");
                if (rooms == null || rooms.isBlank()) {
                    continue;
                }
                for (String room : rooms.split(",")) {
                    history.addGiamSatPhong(resultSet.getString("ma_gv"), room.trim());
                }
            }
        }
    }
}
