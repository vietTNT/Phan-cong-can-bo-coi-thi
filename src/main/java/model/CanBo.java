package model;

public class CanBo {
    private int stt;
    private String hoTen;
    private String ngaySinh;
    private String maCanBo;
    private String donViCongTac;

    public CanBo() {
    }

    public CanBo(int stt, String hoTen, String ngaySinh, String maCanBo, String donViCongTac) {
        this.stt = stt;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.maCanBo = maCanBo;
        this.donViCongTac = donViCongTac;
    }

    public int getStt() {
        return stt;
    }

    public void setStt(int stt) {
        this.stt = stt;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(String ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getMaCanBo() {
        return maCanBo;
    }

    public void setMaCanBo(String maCanBo) {
        this.maCanBo = maCanBo;
    }

    public String getDonViCongTac() {
        return donViCongTac;
    }

    public void setDonViCongTac(String donViCongTac) {
        this.donViCongTac = donViCongTac;
    }
}
