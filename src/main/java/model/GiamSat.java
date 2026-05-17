package model;

import java.util.ArrayList;
import java.util.List;

public class GiamSat {
    private int stt;
    private String maCanBo;
    private String hoTen;
    private List<PhongThi> danhSachPhongGiamSat = new ArrayList<>();
    private String moTaPhongGiamSat;

    public GiamSat() {
    }

    public GiamSat(int stt, String maCanBo, String hoTen, List<PhongThi> danhSachPhongGiamSat, String moTaPhongGiamSat) {
        this.stt = stt;
        this.maCanBo = maCanBo;
        this.hoTen = hoTen;
        this.danhSachPhongGiamSat = new ArrayList<>(danhSachPhongGiamSat);
        this.moTaPhongGiamSat = moTaPhongGiamSat;
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

    public List<PhongThi> getDanhSachPhongGiamSat() {
        return danhSachPhongGiamSat;
    }

    public void setDanhSachPhongGiamSat(List<PhongThi> danhSachPhongGiamSat) {
        this.danhSachPhongGiamSat = new ArrayList<>(danhSachPhongGiamSat);
    }

    public String getMoTaPhongGiamSat() {
        return moTaPhongGiamSat;
    }

    public void setMoTaPhongGiamSat(String moTaPhongGiamSat) {
        this.moTaPhongGiamSat = moTaPhongGiamSat;
    }
}
