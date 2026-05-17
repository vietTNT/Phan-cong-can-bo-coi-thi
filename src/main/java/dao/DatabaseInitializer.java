package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public void initialize() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS can_bo (
                        ma_gv VARCHAR(50) PRIMARY KEY,
                        tt INT,
                        ho_ten VARCHAR(255) NOT NULL,
                        ngay_sinh VARCHAR(50),
                        don_vi_cong_tac VARCHAR(255),
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            addColumnIfMissing(statement, "can_bo", "tt", "ALTER TABLE can_bo ADD COLUMN tt INT");
            addColumnIfMissing(statement, "can_bo", "ma_gv", "ALTER TABLE can_bo ADD COLUMN ma_gv VARCHAR(50)");
            addColumnIfMissing(statement, "can_bo", "ho_ten", "ALTER TABLE can_bo ADD COLUMN ho_ten VARCHAR(255)");
            addColumnIfMissing(statement, "can_bo", "ngay_sinh", "ALTER TABLE can_bo ADD COLUMN ngay_sinh VARCHAR(50)");
            addColumnIfMissing(statement, "can_bo", "don_vi_cong_tac", "ALTER TABLE can_bo ADD COLUMN don_vi_cong_tac VARCHAR(255)");
            modifyColumn(statement, "ALTER TABLE can_bo MODIFY COLUMN ngay_sinh VARCHAR(50)");
            dropIndexIfExists(statement, "can_bo", "uk_can_bo_ma_gv");
            dropIndexIfExists(statement, "can_bo", "ma_gv");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS phong_thi (
                        phong_thi VARCHAR(100) PRIMARY KEY,
                        stt INT,
                        ghi_chu VARCHAR(255),
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            addColumnIfMissing(statement, "phong_thi", "phong_thi", "ALTER TABLE phong_thi ADD COLUMN phong_thi VARCHAR(100)");
            addColumnIfMissing(statement, "phong_thi", "ma_phong", "ALTER TABLE phong_thi ADD COLUMN ma_phong VARCHAR(100)");
            addColumnIfMissing(statement, "phong_thi", "stt", "ALTER TABLE phong_thi ADD COLUMN stt INT");
            addColumnIfMissing(statement, "phong_thi", "ghi_chu", "ALTER TABLE phong_thi ADD COLUMN ghi_chu VARCHAR(255)");
            createIndexIfMissing(statement, "phong_thi", "uk_phong_thi",
                    "CREATE UNIQUE INDEX uk_phong_thi ON phong_thi(phong_thi)");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ca_thi (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        ma_ca_thi VARCHAR(80) NOT NULL UNIQUE,
                        ten_ca_thi VARCHAR(100),
                        so_luong_can_bo INT,
                        so_luong_phong_thi INT,
                        ten_file_excel VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            addColumnIfMissing(statement, "ca_thi", "ma_ca_thi", "ALTER TABLE ca_thi ADD COLUMN ma_ca_thi VARCHAR(80)");
            addColumnIfMissing(statement, "ca_thi", "ten_ca", "ALTER TABLE ca_thi ADD COLUMN ten_ca VARCHAR(100)");
            addColumnIfMissing(statement, "ca_thi", "ten_ca_thi", "ALTER TABLE ca_thi ADD COLUMN ten_ca_thi VARCHAR(100)");
            addColumnIfMissing(statement, "ca_thi", "so_luong_can_bo", "ALTER TABLE ca_thi ADD COLUMN so_luong_can_bo INT");
            addColumnIfMissing(statement, "ca_thi", "so_luong_phong_thi", "ALTER TABLE ca_thi ADD COLUMN so_luong_phong_thi INT");
            addColumnIfMissing(statement, "ca_thi", "ten_file_excel", "ALTER TABLE ca_thi ADD COLUMN ten_file_excel VARCHAR(255)");
            modifyColumn(statement, "ALTER TABLE ca_thi MODIFY COLUMN ten_ca VARCHAR(100) NULL");
            createIndexIfMissing(statement, "ca_thi", "uk_ca_thi_ma_ca_thi",
                    "CREATE UNIQUE INDEX uk_ca_thi_ma_ca_thi ON ca_thi(ma_ca_thi)");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS phan_cong_coi_thi (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        ca_thi_id BIGINT NOT NULL,
                        stt INT NOT NULL,
                        ma_gv VARCHAR(50) NOT NULL,
                        ho_ten VARCHAR(255) NOT NULL,
                        vai_tro VARCHAR(30) NOT NULL,
                        phong_thi VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_pcct_ca_thi
                            FOREIGN KEY (ca_thi_id) REFERENCES ca_thi(id)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            addColumnIfMissing(statement, "phan_cong_coi_thi", "stt", "ALTER TABLE phan_cong_coi_thi ADD COLUMN stt INT");
            addColumnIfMissing(statement, "phan_cong_coi_thi", "ma_gv", "ALTER TABLE phan_cong_coi_thi ADD COLUMN ma_gv VARCHAR(50)");
            addColumnIfMissing(statement, "phan_cong_coi_thi", "ho_ten", "ALTER TABLE phan_cong_coi_thi ADD COLUMN ho_ten VARCHAR(255)");
            addColumnIfMissing(statement, "phan_cong_coi_thi", "vai_tro", "ALTER TABLE phan_cong_coi_thi ADD COLUMN vai_tro VARCHAR(30)");
            addColumnIfMissing(statement, "phan_cong_coi_thi", "phong_thi", "ALTER TABLE phan_cong_coi_thi ADD COLUMN phong_thi VARCHAR(100)");
            modifyColumn(statement, "ALTER TABLE phan_cong_coi_thi MODIFY COLUMN phong_thi_id INT NULL");
            modifyColumn(statement, "ALTER TABLE phan_cong_coi_thi MODIFY COLUMN giam_thi_1_id INT NULL");
            modifyColumn(statement, "ALTER TABLE phan_cong_coi_thi MODIFY COLUMN giam_thi_2_id INT NULL");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS phan_cong_giam_sat (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        ca_thi_id BIGINT NOT NULL,
                        stt INT NOT NULL,
                        ma_gv VARCHAR(50) NOT NULL,
                        ho_ten VARCHAR(255) NOT NULL,
                        phong_thi_duoc_giam_sat TEXT,
                        mo_ta_phong_giam_sat VARCHAR(500),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_pcgs_ca_thi
                            FOREIGN KEY (ca_thi_id) REFERENCES ca_thi(id)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            addColumnIfMissing(statement, "phan_cong_giam_sat", "stt", "ALTER TABLE phan_cong_giam_sat ADD COLUMN stt INT");
            addColumnIfMissing(statement, "phan_cong_giam_sat", "ma_gv", "ALTER TABLE phan_cong_giam_sat ADD COLUMN ma_gv VARCHAR(50)");
            addColumnIfMissing(statement, "phan_cong_giam_sat", "ho_ten", "ALTER TABLE phan_cong_giam_sat ADD COLUMN ho_ten VARCHAR(255)");
            addColumnIfMissing(statement, "phan_cong_giam_sat", "phong_thi_duoc_giam_sat",
                    "ALTER TABLE phan_cong_giam_sat ADD COLUMN phong_thi_duoc_giam_sat TEXT");
            addColumnIfMissing(statement, "phan_cong_giam_sat", "mo_ta_phong_giam_sat",
                    "ALTER TABLE phan_cong_giam_sat ADD COLUMN mo_ta_phong_giam_sat VARCHAR(500)");
            modifyColumn(statement, "ALTER TABLE phan_cong_giam_sat MODIFY COLUMN can_bo_id INT NULL");
            modifyColumn(statement, "ALTER TABLE phan_cong_giam_sat MODIFY COLUMN phong_thi_id INT NULL");

            createIndexIfMissing(statement, "phan_cong_coi_thi", "idx_pcct_ma_gv_phong",
                    "CREATE INDEX idx_pcct_ma_gv_phong ON phan_cong_coi_thi(ma_gv, phong_thi)");
            createIndexIfMissing(statement, "phan_cong_coi_thi", "idx_pcct_ca_phong",
                    "CREATE INDEX idx_pcct_ca_phong ON phan_cong_coi_thi(ca_thi_id, phong_thi)");
            createIndexIfMissing(statement, "phan_cong_giam_sat", "idx_pcgs_ma_gv",
                    "CREATE INDEX idx_pcgs_ma_gv ON phan_cong_giam_sat(ma_gv)");
        }
    }

    private void createIndexIfMissing(Statement statement, String tableName, String indexName, String createSql) throws SQLException {
        try (var resultSet = statement.executeQuery("SHOW INDEX FROM " + tableName + " WHERE Key_name = '" + indexName + "'")) {
            if (!resultSet.next()) {
                statement.execute(createSql);
            }
        }
    }

    private void addColumnIfMissing(Statement statement, String tableName, String columnName, String alterSql) throws SQLException {
        try (var resultSet = statement.executeQuery("SHOW COLUMNS FROM " + tableName + " LIKE '" + columnName + "'")) {
            if (!resultSet.next()) {
                statement.execute(alterSql);
            }
        }
    }

    private void dropIndexIfExists(Statement statement, String tableName, String indexName) throws SQLException {
        try (var resultSet = statement.executeQuery("SHOW INDEX FROM " + tableName + " WHERE Key_name = '" + indexName + "'")) {
            if (resultSet.next()) {
                statement.execute("DROP INDEX " + indexName + " ON " + tableName);
            }
        }
    }

    private void modifyColumn(Statement statement, String alterSql) {
        try {
            statement.execute(alterSql);
        } catch (SQLException ignored) {
            // Giữ nguyên schema cũ nếu MySQL không cho phép đổi kiểu do ràng buộc hiện có.
        }
    }
}
