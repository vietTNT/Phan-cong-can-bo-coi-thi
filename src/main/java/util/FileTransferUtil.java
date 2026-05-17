package util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class FileTransferUtil {
    private FileTransferUtil() {
    }

    public static void sendFile(DataOutputStream out, File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("File không tồn tại: " + file);
        }

        out.writeUTF(file.getName());
        out.writeLong(file.length());

        byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        out.flush();
    }

    public static File receiveFile(DataInputStream in, File targetDir) throws IOException {
        Files.createDirectories(targetDir.toPath());

        String fileName = in.readUTF();
        long fileSize = in.readLong();
        if (fileSize < 0) {
            throw new IOException("Kích thước file không hợp lệ: " + fileName);
        }

        File outputFile = new File(targetDir, sanitizeFileName(fileName));
        byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
        long remainingBytes = fileSize;

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            while (remainingBytes > 0) {
                int bytesToRead = (int) Math.min(buffer.length, remainingBytes);
                int bytesRead = in.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    throw new EOFException("Mất kết nối khi đang nhận file: " + fileName);
                }
                fileOutputStream.write(buffer, 0, bytesRead);
                remainingBytes -= bytesRead;
            }
        }

        return outputFile;
    }

    public static void sendMultipleFiles(DataOutputStream out, List<File> files) throws IOException {
        out.writeInt(files.size());
        for (File file : files) {
            sendFile(out, file);
        }
        out.flush();
    }

    public static List<File> receiveMultipleFiles(DataInputStream in, File targetDir) throws IOException {
        int fileCount = in.readInt();
        if (fileCount < 0 || fileCount > 20) {
            throw new IOException("Số lượng file không hợp lệ: " + fileCount);
        }

        List<File> receivedFiles = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            receivedFiles.add(receiveFile(in, targetDir));
        }
        return receivedFiles;
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
