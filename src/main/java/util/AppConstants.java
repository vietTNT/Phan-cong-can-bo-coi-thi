package util;

import java.nio.file.Path;

public final class AppConstants {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 9999;
    public static final int BUFFER_SIZE = 8192;

    public static final String SHEET_CAN_BO = "Danh sách cán bộ";
    public static final String SHEET_PHONG_THI = "DS phong thi";
    public static final String OUTPUT_PHAN_CONG_FILE = "DANHSACHPHANCONG.xlsx";
    public static final String OUTPUT_GIAM_SAT_FILE = "DANHSACHGIAMSAT.xlsx";

    public static final Path CLIENT_DOWNLOAD_DIR = Path.of("client_downloads");
    public static final Path SERVER_UPLOAD_DIR = Path.of("server_uploads");
    public static final Path SERVER_OUTPUT_DIR = Path.of("server_outputs");

    private AppConstants() {
    }
}
