package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/phan_cong_coi_thi"
            + "?createDatabaseIfNotExist=true"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8"
            + "&serverTimezone=Asia/Bangkok";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public static String getUrl() {
        return firstNonBlank(System.getProperty("db.url"), System.getenv("DB_URL"), DEFAULT_URL);
    }

    public static String getUser() {
        return firstNonBlank(System.getProperty("db.user"), System.getenv("DB_USER"), DEFAULT_USER);
    }

    public static String getPassword() {
        return firstNonBlank(System.getProperty("db.password"), System.getenv("DB_PASSWORD"), DEFAULT_PASSWORD);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
