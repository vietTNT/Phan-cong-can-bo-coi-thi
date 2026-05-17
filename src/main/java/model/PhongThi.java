package model;

public class PhongThi {
    private int stt;
    private String phongThi;
    private String diaDiem;

    public PhongThi() {
    }

    public PhongThi(int stt, String phongThi, String diaDiem) {
        this.stt = stt;
        this.phongThi = phongThi;
        this.diaDiem = diaDiem;
    }

    public int getStt() {
        return stt;
    }

    public void setStt(int stt) {
        this.stt = stt;
    }

    public String getPhongThi() {
        return phongThi;
    }

    public void setPhongThi(String phongThi) {
        this.phongThi = phongThi;
    }

    public String getDiaDiem() {
        return diaDiem;
    }

    public void setDiaDiem(String diaDiem) {
        this.diaDiem = diaDiem;
    }

    public String getGhiChu() {
        return diaDiem;
    }

    public void setGhiChu(String ghiChu) {
        this.diaDiem = ghiChu;
    }
}
