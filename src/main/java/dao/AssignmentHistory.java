package dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/*
HashMap : lưu dữ liệu dạng key-value
HashSet : lưu danh sách không trùng lặp
Map     : kiểu dữ liệu ánh xạ key-value
Set     : kiểu tập hợp, không cho phần tử trùng */
public class AssignmentHistory {
    private final Map<String, Set<String>> canBoDaCoiPhong = new HashMap<>();  // danh sách cán bộ đã coi phòng thi, key là mã cán bộ, value là tập hợp các phòng thi đã coi
    private final Map<String, Set<String>> canBoDaGiamSatPhong = new HashMap<>(); // danh sách cán bộ đã giám sát phòng thi, key là mã cán bộ, value là tập hợp các phòng thi đã giám sát
    private final Set<String> capCanBoDaDiChung = new HashSet<>(); // danh sách cặp cán bộ đã đi chung, mỗi cặp được lưu dưới dạng "maCanBo1|maCanBo2" với maCanBo1 <= maCanBo2 để tránh trùng lặp

    // thêm lịch sử : cán bộ đã từng coi phòng nào.
    public void addCanBoPhong(String maCanBo, String phongThi) {
        canBoDaCoiPhong.computeIfAbsent(normalize(maCanBo), key -> new HashSet<>()).add(normalize(phongThi));
    }
    // thêm lịch sử : cán bộ đã từng giám sát phòng nào.
    public void addGiamSatPhong(String maCanBo, String phongThi) {
        canBoDaGiamSatPhong.computeIfAbsent(normalize(maCanBo), key -> new HashSet<>()).add(normalize(phongThi));
    }
    // thêm lịch sử : cặp cán bộ đã đi chung.
    public void addCanBoPair(String maCanBo1, String maCanBo2) {
        capCanBoDaDiChung.add(pairKey(maCanBo1, maCanBo2));
    }
    // kiểm tra lịch sử : cán bộ đã từng coi phòng nào.
    public boolean hasCanBoPhong(String maCanBo, String phongThi) {
        return canBoDaCoiPhong.getOrDefault(normalize(maCanBo), Set.of()).contains(normalize(phongThi));
    }
    // kiểm tra lịch sử : cán bộ đã từng giám sát phòng nào.
    public boolean hasGiamSatPhong(String maCanBo, String phongThi) {
        return canBoDaGiamSatPhong.getOrDefault(normalize(maCanBo), Set.of()).contains(normalize(phongThi));
    }
    // kiểm tra lịch sử : cặp cán bộ đã đi chung.
    public boolean hasCanBoPair(String maCanBo1, String maCanBo2) {
        return capCanBoDaDiChung.contains(pairKey(maCanBo1, maCanBo2));
    }
    // tạo khóa cho cặp cán bộ, đảm bảo thứ tự cố định để tránh trùng lặp (maCanBo1|maCanBo2 hoặc maCanBo2|maCanBo1 đều được chuẩn hóa thành cùng một chuỗi).
    private String pairKey(String maCanBo1, String maCanBo2) {
        String first = normalize(maCanBo1);
        String second = normalize(maCanBo2);
        return first.compareTo(second) <= 0 ? first + "|" + second : second + "|" + first;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
