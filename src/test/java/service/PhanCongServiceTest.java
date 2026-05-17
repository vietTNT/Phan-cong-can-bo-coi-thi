package service;

import dao.AssignmentHistory;
import model.CanBo;
import model.GiamSat;
import model.PhanCong;
import model.PhongThi;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhanCongServiceTest {
    private final PhanCongService service = new PhanCongService();

    @Test
    void assignThrowsWhenNotEnoughTeachers() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.phanCong(createCanBo(3), createPhongThi(2), new AssignmentHistory())
        );

        assertEquals("Không đủ cán bộ để phân công coi thi", exception.getMessage());
    }

    @Test
    void assignCreatesTwoInvigilatorsPerRoomAndUsesEachTeacherOnce() {
        PhanCongService.AssignmentResult result = service.phanCong(createCanBo(8), createPhongThi(3), new AssignmentHistory());

        assertEquals(6, result.getDanhSachPhanCong().size());
        assertEquals(2, result.getDanhSachGiamSat().size());
        assertEquals(3, result.getDanhSachPhanCong().stream().filter(PhanCong::isGiamThi1).count());
        assertEquals(3, result.getDanhSachPhanCong().stream().filter(PhanCong::isGiamThi2).count());

        Set<String> usedTeachers = new HashSet<>();
        for (PhanCong phanCong : result.getDanhSachPhanCong()) {
            assertTrue(usedTeachers.add(phanCong.getMaCanBo()));
        }
    }

    @Test
    void assignAvoidsTeacherRoomHistoryAndPairHistory() {
        List<CanBo> canBo = createCanBo(4);
        List<PhongThi> phongThi = List.of(new PhongThi(1, "P001", "Ghi chú"));
        AssignmentHistory history = new AssignmentHistory();
        history.addCanBoPhong("CB1", "P001");
        history.addCanBoPhong("CB2", "P001");
        history.addCanBoPair("CB3", "CB4");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.phanCong(canBo, phongThi, history)
        );

        assertTrue(exception.getMessage().contains("Không tìm được phương án phân công"));
    }

    @Test
    void supervisorsAreDividedEvenly() {
        PhanCongService.AssignmentResult result = service.phanCong(createCanBo(26), createPhongThi(10), new AssignmentHistory());

        List<GiamSat> giamSat = result.getDanhSachGiamSat();
        assertEquals(6, giamSat.size());
        assertEquals(2, giamSat.get(0).getDanhSachPhongGiamSat().size());
        assertEquals(2, giamSat.get(1).getDanhSachPhongGiamSat().size());
        assertEquals(2, giamSat.get(2).getDanhSachPhongGiamSat().size());
        assertEquals(2, giamSat.get(3).getDanhSachPhongGiamSat().size());
        assertEquals(1, giamSat.get(4).getDanhSachPhongGiamSat().size());
        assertEquals(1, giamSat.get(5).getDanhSachPhongGiamSat().size());
    }

    @Test
    void manySupervisorsAreAssignedUpToTwoPerRoomWithoutEmptyWork() {
        PhanCongService.AssignmentResult result = service.phanCong(createCanBo(16), createPhongThi(5), new AssignmentHistory());

        List<GiamSat> giamSat = result.getDanhSachGiamSat();
        assertEquals(6, giamSat.size());
        assertTrue(giamSat.stream().allMatch(item -> item.getDanhSachPhongGiamSat().size() == 1));

        Map<String, Long> supervisorsByRoom = giamSat.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getDanhSachPhongGiamSat().get(0).getPhongThi(),
                        Collectors.counting()
                ));
        assertTrue(supervisorsByRoom.values().stream().allMatch(count -> count <= 2));
    }

    @Test
    void manySupervisorsCanExceedTwoPerRoomWhenNoLimitConfigured() {
        PhanCongService.AssignmentResult result = service.phanCong(createCanBo(22), createPhongThi(5), new AssignmentHistory());

        List<GiamSat> giamSat = result.getDanhSachGiamSat();
        assertEquals(12, giamSat.size());

        Map<String, Long> supervisorsByRoom = giamSat.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getDanhSachPhongGiamSat().get(0).getPhongThi(),
                        Collectors.counting()
                ));
        assertTrue(supervisorsByRoom.values().stream().anyMatch(count -> count > 2));
    }

    private List<CanBo> createCanBo(int count) {
        List<CanBo> canBo = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            canBo.add(new CanBo(i, "Cán bộ " + i, "01/01/1990", "CB" + i, "Đơn vị"));
        }
        return canBo;
    }

    private List<PhongThi> createPhongThi(int count) {
        List<PhongThi> phongThi = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            phongThi.add(new PhongThi(i, "C" + String.format("%03d", i), "Ghi chú"));
        }
        return phongThi;
    }
}
