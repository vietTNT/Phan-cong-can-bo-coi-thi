package dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssignmentHistory {
    private final Map<String, Set<String>> canBoDaCoiPhong = new HashMap<>();
    private final Map<String, Set<String>> canBoDaGiamSatPhong = new HashMap<>();
    private final Set<String> capCanBoDaDiChung = new HashSet<>();

    public void addCanBoPhong(String maCanBo, String phongThi) {
        canBoDaCoiPhong.computeIfAbsent(normalize(maCanBo), key -> new HashSet<>()).add(normalize(phongThi));
    }

    public void addGiamSatPhong(String maCanBo, String phongThi) {
        canBoDaGiamSatPhong.computeIfAbsent(normalize(maCanBo), key -> new HashSet<>()).add(normalize(phongThi));
    }

    public void addCanBoPair(String maCanBo1, String maCanBo2) {
        capCanBoDaDiChung.add(pairKey(maCanBo1, maCanBo2));
    }

    public boolean hasCanBoPhong(String maCanBo, String phongThi) {
        return canBoDaCoiPhong.getOrDefault(normalize(maCanBo), Set.of()).contains(normalize(phongThi));
    }

    public boolean hasGiamSatPhong(String maCanBo, String phongThi) {
        return canBoDaGiamSatPhong.getOrDefault(normalize(maCanBo), Set.of()).contains(normalize(phongThi));
    }

    public boolean hasCanBoPair(String maCanBo1, String maCanBo2) {
        return capCanBoDaDiChung.contains(pairKey(maCanBo1, maCanBo2));
    }

    private String pairKey(String maCanBo1, String maCanBo2) {
        String first = normalize(maCanBo1);
        String second = normalize(maCanBo2);
        return first.compareTo(second) <= 0 ? first + "|" + second : second + "|" + first;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
