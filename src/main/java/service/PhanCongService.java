package service;

import dao.AssignmentHistory;
import model.CanBo;
import model.GiamSat;
import model.PhanCong;
import model.PhongThi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhanCongService {
    private static final int DEFAULT_MAX_RETRY = 100;
    private static final int UNLIMITED_SUPERVISORS_PER_ROOM = Integer.MAX_VALUE;
    private static final int MAX_SUPERVISORS_PER_ROOM = UNLIMITED_SUPERVISORS_PER_ROOM;

    public AssignmentResult phanCong(List<CanBo> danhSachCanBo,
                                     List<PhongThi> danhSachPhong,
                                     AssignmentHistory history) {
        return phanCong(danhSachCanBo, danhSachPhong, history, DEFAULT_MAX_RETRY);
    }

    public AssignmentResult phanCong(List<CanBo> danhSachCanBo,
                                     List<PhongThi> danhSachPhong,
                                     AssignmentHistory history,
                                     int maxRetry) {
        validateInput(danhSachCanBo, danhSachPhong, maxRetry);
        AssignmentHistory safeHistory = history == null ? new AssignmentHistory() : history;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                return tryAssignOnce(danhSachCanBo, danhSachPhong, safeHistory);
            } catch (AssignmentDeadlockException ignored) {
                // Rollback ca hiện tại bằng cách bỏ toàn bộ state trong attempt này và thử lại.
            }
        }

        throw new IllegalStateException("Không tìm được phương án phân công sau " + maxRetry + " lần thử");
    }

    private AssignmentResult tryAssignOnce(List<CanBo> danhSachCanBo,
                                           List<PhongThi> danhSachPhong,
                                           AssignmentHistory history) {
        List<CanBo> candidates = new ArrayList<>(danhSachCanBo);
        Collections.shuffle(candidates);

        Set<String> usedTeachers = new HashSet<>();
        List<PhanCong> danhSachPhanCong = new ArrayList<>();
        int stt = 1;

        for (PhongThi phongThi : danhSachPhong) {
            CanBo giamThi1 = findTeacherForRoom(candidates, usedTeachers, phongThi, null, history);
            usedTeachers.add(normalize(giamThi1.getMaCanBo()));

            CanBo giamThi2 = findTeacherForRoom(candidates, usedTeachers, phongThi, giamThi1, history);
            usedTeachers.add(normalize(giamThi2.getMaCanBo()));

            danhSachPhanCong.add(new PhanCong(stt++, giamThi1.getMaCanBo(), giamThi1.getHoTen(), true, false, phongThi.getPhongThi()));
            danhSachPhanCong.add(new PhanCong(stt++, giamThi2.getMaCanBo(), giamThi2.getHoTen(), false, true, phongThi.getPhongThi()));
        }

        List<CanBo> canBoGiamSat = candidates.stream()
                .filter(canBo -> !usedTeachers.contains(normalize(canBo.getMaCanBo())))
                .toList();
        List<GiamSat> danhSachGiamSat = assignSupervisors(canBoGiamSat, danhSachPhong, history);

        return new AssignmentResult(danhSachPhanCong, danhSachGiamSat);
    }

    private CanBo findTeacherForRoom(List<CanBo> candidates,
                                     Set<String> usedTeachers,
                                     PhongThi phongThi,
                                     CanBo pairedTeacher,
                                     AssignmentHistory history) {
        for (CanBo canBo : candidates) {
            String maCanBo = normalize(canBo.getMaCanBo());
            if (usedTeachers.contains(maCanBo)) {
                continue;
            }
            if (history.hasCanBoPhong(maCanBo, phongThi.getPhongThi())) {
                continue;
            }
            if (pairedTeacher != null && history.hasCanBoPair(maCanBo, pairedTeacher.getMaCanBo())) {
                continue;
            }
            return canBo;
        }
        throw new AssignmentDeadlockException();
    }

    private List<GiamSat> assignSupervisors(List<CanBo> canBoGiamSat,
                                            List<PhongThi> danhSachPhong,
                                            AssignmentHistory history) {
        List<GiamSat> result = new ArrayList<>();
        if (canBoGiamSat.isEmpty()) {
            return result;
        }

        int totalRooms = danhSachPhong.size();
        int totalSupervisors = canBoGiamSat.size();
        if (isSupervisorRoomLimitEnabled()
                && totalSupervisors > (long) totalRooms * MAX_SUPERVISORS_PER_ROOM) {
            throw new IllegalArgumentException("Số cán bộ giám sát vượt quá giới hạn "
                    + MAX_SUPERVISORS_PER_ROOM + " người/phòng");
        }
        if (totalSupervisors > totalRooms) {
            return assignManySupervisors(canBoGiamSat, danhSachPhong, history);
        }

        int base = totalRooms / totalSupervisors;
        int remainder = totalRooms % totalSupervisors;
        Set<String> assignedRoomThisAttempt = new HashSet<>();

        for (int i = 0; i < totalSupervisors; i++) {
            CanBo canBo = canBoGiamSat.get(i);
            int roomCount = base + (i < remainder ? 1 : 0);
            List<PhongThi> rooms = new ArrayList<>();

            for (int j = 0; j < roomCount; j++) {
                PhongThi room = chooseSupervisorRoom(canBo, danhSachPhong, assignedRoomThisAttempt, history);
                assignedRoomThisAttempt.add(normalize(room.getPhongThi()));
                rooms.add(room);
            }

            rooms.sort(Comparator.comparingInt(PhongThi::getStt));
            result.add(new GiamSat(
                    i + 1,
                    canBo.getMaCanBo(),
                    canBo.getHoTen(),
                    rooms,
                    createRoomDescription(rooms)
            ));
        }

        return result;
    }

    private List<GiamSat> assignManySupervisors(List<CanBo> canBoGiamSat,
                                                List<PhongThi> danhSachPhong,
                                                AssignmentHistory history) {
        List<GiamSat> result = new ArrayList<>();
        Map<String, Integer> supervisorCountByRoom = new HashMap<>();

        for (int i = 0; i < canBoGiamSat.size(); i++) {
            CanBo canBo = canBoGiamSat.get(i);
            PhongThi room = chooseSupervisorRoomWithCapacity(canBo, danhSachPhong, supervisorCountByRoom, history);
            supervisorCountByRoom.merge(normalize(room.getPhongThi()), 1, Integer::sum);
            List<PhongThi> rooms = List.of(room);

            result.add(new GiamSat(
                    i + 1,
                    canBo.getMaCanBo(),
                    canBo.getHoTen(),
                    rooms,
                    createRoomDescription(rooms)
            ));
        }

        return result;
    }

    private PhongThi chooseSupervisorRoomWithCapacity(CanBo canBo,
                                                      List<PhongThi> rooms,
                                                      Map<String, Integer> supervisorCountByRoom,
                                                      AssignmentHistory history) {
        PhongThi selectedRoom = null;
        int selectedRoomCount = Integer.MAX_VALUE;

        for (PhongThi room : rooms) {
            String roomKey = normalize(room.getPhongThi());
            int currentCount = supervisorCountByRoom.getOrDefault(roomKey, 0);
            if (currentCount >= MAX_SUPERVISORS_PER_ROOM) {
                continue;
            }
            if (history.hasGiamSatPhong(canBo.getMaCanBo(), room.getPhongThi())) {
                continue;
            }
            if (currentCount < selectedRoomCount) {
                selectedRoom = room;
                selectedRoomCount = currentCount;
            }
        }

        if (selectedRoom != null) {
            return selectedRoom;
        }
        throw new AssignmentDeadlockException();
    }

    private boolean isSupervisorRoomLimitEnabled() {
        return MAX_SUPERVISORS_PER_ROOM != UNLIMITED_SUPERVISORS_PER_ROOM;
    }

    private PhongThi chooseSupervisorRoom(CanBo canBo,
                                          List<PhongThi> rooms,
                                          Set<String> assignedRoomThisAttempt,
                                          AssignmentHistory history) {
        for (PhongThi room : rooms) {
            if (!assignedRoomThisAttempt.contains(normalize(room.getPhongThi()))
                    && !history.hasGiamSatPhong(canBo.getMaCanBo(), room.getPhongThi())) {
                return room;
            }
        }
        throw new AssignmentDeadlockException();
    }

    private String createRoomDescription(List<PhongThi> rooms) {
        if (rooms.isEmpty()) {
            return "Không phân công phòng";
        }
        if (rooms.size() == 1) {
            return rooms.get(0).getPhongThi();
        }
        return "Từ " + rooms.get(0).getPhongThi() + " đến " + rooms.get(rooms.size() - 1).getPhongThi();
    }

    private void validateInput(List<CanBo> danhSachCanBo, List<PhongThi> danhSachPhong, int maxRetry) {
        if (danhSachCanBo == null || danhSachCanBo.isEmpty()) {
            throw new IllegalArgumentException("Danh sách cán bộ coi thi đang rỗng");
        }
        if (danhSachPhong == null || danhSachPhong.isEmpty()) {
            throw new IllegalArgumentException("Danh sách phòng thi đang rỗng");
        }
        if (danhSachCanBo.size() < danhSachPhong.size() * 2) {
            throw new IllegalArgumentException("Không đủ cán bộ để phân công coi thi");
        }
        if (maxRetry <= 0) {
            throw new IllegalArgumentException("maxRetry phải lớn hơn 0");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static class AssignmentDeadlockException extends RuntimeException {
    }

    public static class AssignmentResult {
        private final List<PhanCong> danhSachPhanCong;
        private final List<GiamSat> danhSachGiamSat;

        public AssignmentResult(List<PhanCong> danhSachPhanCong, List<GiamSat> danhSachGiamSat) {
            this.danhSachPhanCong = List.copyOf(danhSachPhanCong);
            this.danhSachGiamSat = List.copyOf(danhSachGiamSat);
        }

        public List<PhanCong> getDanhSachPhanCong() {
            return danhSachPhanCong;
        }

        public List<GiamSat> getDanhSachGiamSat() {
            return danhSachGiamSat;
        }
    }
}
