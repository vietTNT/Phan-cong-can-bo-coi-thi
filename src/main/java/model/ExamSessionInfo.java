package model;

public class ExamSessionInfo {
    private final int soLuongCanBo;
    private final int soLuongPhongThi;
    private final String tenCaThi;

    public ExamSessionInfo(int soLuongCanBo, int soLuongPhongThi) {
        this(soLuongCanBo, soLuongPhongThi, "Tự động");
    }

    public ExamSessionInfo(int soLuongCanBo, int soLuongPhongThi, String tenCaThi) {
        this.soLuongCanBo = soLuongCanBo;
        this.soLuongPhongThi = soLuongPhongThi;
        this.tenCaThi = tenCaThi == null || tenCaThi.isBlank() ? "Tự động" : tenCaThi.trim();
    }

    public int getSoLuongCanBo() {
        return soLuongCanBo;
    }

    public int getSoLuongPhongThi() {
        return soLuongPhongThi;
    }

    public String getTenCaThi() {
        return tenCaThi;
    }
}
