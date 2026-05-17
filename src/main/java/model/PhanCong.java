package model;

public class PhanCong {
    private int stt;
    private String maCanBo;
    private String hoTen;
    private boolean giamThi1;
    private boolean giamThi2;
    private String phongThi;

    public PhanCong() {
    }

    public PhanCong(int stt, String maCanBo, String hoTen, boolean giamThi1, boolean giamThi2, String phongThi) {
        this.stt = stt;
        this.maCanBo = maCanBo;
        this.hoTen = hoTen;
        this.giamThi1 = giamThi1;
        this.giamThi2 = giamThi2;
        this.phongThi = phongThi;
    }

    public int getStt() {
        return stt;
    }

    public void setStt(int stt) {
        this.stt = stt;
    }

    public String getMaCanBo() {
        return maCanBo;
    }

    public void setMaCanBo(String maCanBo) {
        this.maCanBo = maCanBo;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public boolean isGiamThi1() {
        return giamThi1;
    }

    public void setGiamThi1(boolean giamThi1) {
        this.giamThi1 = giamThi1;
    }

    public boolean isGiamThi2() {
        return giamThi2;
    }

    public void setGiamThi2(boolean giamThi2) {
        this.giamThi2 = giamThi2;
    }

    public String getPhongThi() {
        return phongThi;
    }

    public void setPhongThi(String phongThi) {
        this.phongThi = phongThi;
    }
}
