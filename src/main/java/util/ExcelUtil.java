package util;

import model.CanBo;
import model.GiamSat;
import model.PhanCong;
import model.PhongThi;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExcelUtil {
    private static final int ROWS_PER_SHEET = 20;
    private static final int OUTPUT_HEADER_ROW_INDEX = 3;
    private static final int OUTPUT_DATA_START_ROW_INDEX = 4;
    private static final String NATIONAL_TITLE = "Cộng Hòa Xã Hội Chủ Nghĩa Việt Nam";
    private static final String NATIONAL_MOTTO = "Độc Lập - Tự Do - Hạnh Phúc";

    private final DataFormatter dataFormatter = new DataFormatter(new Locale("vi", "VN"));

    public ExcelData readInputWorkbook(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet canBoSheet = getRequiredSheet(workbook, AppConstants.SHEET_CAN_BO);
            Sheet phongThiSheet = getRequiredSheet(workbook, AppConstants.SHEET_PHONG_THI);
            return new ExcelData(readCanBoSheet(canBoSheet), readPhongThiSheet(phongThiSheet));
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Không đọc được file Excel đầu vào: " + exception.getMessage(), exception);
        }
    }

    public void writeDanhSachPhanCong(List<PhanCong> danhSachPhanCong, File outputFile) throws IOException {
        ensureParentDir(outputFile);
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle centerStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle leftStyle = createBodyStyle(workbook, HorizontalAlignment.LEFT);

            int totalSheets = Math.max(1, (int) Math.ceil(danhSachPhanCong.size() / (double) ROWS_PER_SHEET));
            for (int sheetIndex = 0; sheetIndex < totalSheets; sheetIndex++) {
                Sheet sheet = createOutputSheet(workbook, "Phân công " + (sheetIndex + 1));
                createNationalHeader(sheet, createNationalHeaderStyle(workbook), 5);
                createHeaderRow(sheet, OUTPUT_HEADER_ROW_INDEX, headerStyle,
                        "STT", "Mã GV", "Họ tên", "Giám thị 1", "Giám thị 2", "Phòng thi");

                int fromIndex = sheetIndex * ROWS_PER_SHEET;
                int toIndex = Math.min(fromIndex + ROWS_PER_SHEET, danhSachPhanCong.size());
                int rowIndex = OUTPUT_DATA_START_ROW_INDEX;
                for (int i = fromIndex; i < toIndex; i++) {
                    PhanCong phanCong = danhSachPhanCong.get(i);
                    Row row = sheet.createRow(rowIndex++);
                    createCell(row, 0, phanCong.getStt(), centerStyle);
                    createCell(row, 1, phanCong.getMaCanBo(), centerStyle);
                    createCell(row, 2, phanCong.getHoTen(), leftStyle);
                    createCell(row, 3, phanCong.isGiamThi1() ? "X" : "", centerStyle);
                    createCell(row, 4, phanCong.isGiamThi2() ? "X" : "", centerStyle);
                    createCell(row, 5, phanCong.getPhongThi(), centerStyle);
                }

                applySheetLayout(sheet);
                autoSizeColumns(sheet, 6);
            }

            workbook.write(outputStream);
            workbook.dispose();
        }
    }

    public void writeDanhSachGiamSat(List<GiamSat> danhSachGiamSat, File outputFile) throws IOException {
        ensureParentDir(outputFile);
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle centerStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle leftStyle = createBodyStyle(workbook, HorizontalAlignment.LEFT);

            int totalSheets = Math.max(1, (int) Math.ceil(danhSachGiamSat.size() / (double) ROWS_PER_SHEET));
            for (int sheetIndex = 0; sheetIndex < totalSheets; sheetIndex++) {
                Sheet sheet = createOutputSheet(workbook, "Giám sát " + (sheetIndex + 1));
                createNationalHeader(sheet, createNationalHeaderStyle(workbook), 3);
                createHeaderRow(sheet, OUTPUT_HEADER_ROW_INDEX, headerStyle,
                        "STT", "Mã GV", "Họ tên", "Phòng thi được giám sát");

                int fromIndex = sheetIndex * ROWS_PER_SHEET;
                int toIndex = Math.min(fromIndex + ROWS_PER_SHEET, danhSachGiamSat.size());
                int rowIndex = OUTPUT_DATA_START_ROW_INDEX;
                if (danhSachGiamSat.isEmpty()) {
                    Row row = sheet.createRow(rowIndex);
                    createCell(row, 0, 1, centerStyle);
                    createCell(row, 1, "", centerStyle);
                    createCell(row, 2, "", leftStyle);
                    createCell(row, 3, "Không có cán bộ giám sát", leftStyle);
                } else {
                    for (int i = fromIndex; i < toIndex; i++) {
                        GiamSat giamSat = danhSachGiamSat.get(i);
                        Row row = sheet.createRow(rowIndex++);
                        createCell(row, 0, giamSat.getStt(), centerStyle);
                        createCell(row, 1, giamSat.getMaCanBo(), centerStyle);
                        createCell(row, 2, giamSat.getHoTen(), leftStyle);
                        createCell(row, 3, giamSat.getMoTaPhongGiamSat(), leftStyle);
                    }
                }

                applySheetLayout(sheet);
                autoSizeColumns(sheet, 4);
            }

            workbook.write(outputStream);
            workbook.dispose();
        }
    }

    private List<CanBo> readCanBoSheet(Sheet sheet) {
        List<CanBo> result = new ArrayList<>();
        for (int rowIndex = findHeaderRowIndex(sheet, "Mã GV", 1) + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isRowBlank(row)) {
                continue;
            }

            String maGv = getCellString(row, 1);
            String hoTen = getCellString(row, 2);
            if (maGv.isBlank() && hoTen.isBlank()) {
                continue;
            }
            if (maGv.isBlank()) {
                maGv = "GV" + (result.size() + 1);
            }

            result.add(new CanBo(
                    getCellInt(row, 0, result.size() + 1),
                    hoTen,
                    getCellString(row, 3),
                    maGv,
                    getCellString(row, 4)
            ));
        }
        return result;
    }

    private List<PhongThi> readPhongThiSheet(Sheet sheet) {
        List<PhongThi> result = new ArrayList<>();
        for (int rowIndex = findHeaderRowIndex(sheet, "Phòng thi", 1) + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isRowBlank(row)) {
                continue;
            }

            String phongThi = getCellString(row, 1);
            if (phongThi.isBlank()) {
                continue;
            }

            result.add(new PhongThi(
                    getCellInt(row, 0, result.size() + 1),
                    phongThi,
                    getCellString(row, 2)
            ));
        }
        return result;
    }

    private Sheet getRequiredSheet(Workbook workbook, String sheetName) throws IOException {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IOException("Không tìm thấy sheet \"" + sheetName + "\" trong file Excel");
        }
        return sheet;
    }

    private int findHeaderRowIndex(Sheet sheet, String expectedHeader, int fallbackIndex) {
        String normalizedHeader = normalize(expectedHeader);
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int columnIndex = Math.max(0, row.getFirstCellNum()); columnIndex < row.getLastCellNum(); columnIndex++) {
                if (normalize(getCellString(row, columnIndex)).equals(normalizedHeader)) {
                    return rowIndex;
                }
            }
        }
        return fallbackIndex;
    }

    private boolean isRowBlank(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = Math.max(0, row.getFirstCellNum()); i < row.getLastCellNum(); i++) {
            if (!getCellString(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private int getCellInt(Row row, int columnIndex, int defaultValue) {
        String text = getCellString(row, columnIndex);
        if (text.isBlank()) {
            return defaultValue;
        }
        try {
            return (int) Double.parseDouble(text.replace(",", "."));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String getCellString(Row row, int columnIndex) {
        if (row == null || columnIndex < 0) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
    }

    private void ensureParentDir(File outputFile) throws IOException {
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Không tạo được thư mục: " + parentDir.getAbsolutePath());
        }
    }

    private Sheet createOutputSheet(SXSSFWorkbook workbook, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);
        ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
        return sheet;
    }

    private void createNationalHeader(Sheet sheet, CellStyle style, int lastColumnIndex) {
        Row titleRow = sheet.createRow(0);
        createCell(titleRow, 0, NATIONAL_TITLE, style);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumnIndex));

        Row mottoRow = sheet.createRow(1);
        createCell(mottoRow, 0, NATIONAL_MOTTO, style);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastColumnIndex));
    }

    private void createHeaderRow(Sheet sheet, int rowIndex, CellStyle style, String... headers) {
        Row headerRow = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], style);
        }
    }

    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private CellStyle createNationalHeaderStyle(SXSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBodyStyle(SXSSFWorkbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        applyBorder(style);
        return style;
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 15000));
        }
    }

    private void applySheetLayout(Sheet sheet) {
        sheet.setAutobreaks(true);
        sheet.setFitToPage(true);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("1:4"));

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(false);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ExcelData(List<CanBo> danhSachCanBo, List<PhongThi> danhSachPhongThi) {
    }
}
