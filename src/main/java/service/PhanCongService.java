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
   // hàm phân công hoàn chỉnh, sẽ được gọi trong mỗi lần thử phân công.
    private AssignmentResult tryAssignOnce(List<CanBo> danhSachCanBo,
                                           List<PhongThi> danhSachPhong,
                                           AssignmentHistory history) {
        List<CanBo> candidates = new ArrayList<>(danhSachCanBo); // tạo danh sách mới từ danh sách gốc.
        Collections.shuffle(candidates);  // xáo trộn danh sách ngẫu nhiên mỗi lần thử 

        Set<String> usedTeachers = new HashSet<>();   // lưu các cán bộ được phân công làm giám thị để tránh phân công trùng, dùng set để kiểm tra nhanh
        List<PhanCong> danhSachPhanCong = new ArrayList<>();  // tạo danh sách kq phân công giám thị
        int stt = 1; 
         // duyệt từng phòng thi để phân công 2 giám thị                           
        for (PhongThi phongThi : danhSachPhong) {
            CanBo giamThi1 = findTeacherForRoom(candidates, usedTeachers, phongThi, null, history); // tìm giám thị 1, lần 1 không có người coi thi chung nên pairedTeacher là null
            usedTeachers.add(normalize(giamThi1.getMaCanBo()));   // đánh dấu giám thị 1 đã chọn

            CanBo giamThi2 = findTeacherForRoom(candidates, usedTeachers, phongThi, giamThi1, history); // tìm giám thị 2, lần này có giamThi1 làm pairedTeacher để tránh trùng lặp
            usedTeachers.add(normalize(giamThi2.getMaCanBo())); // đánh dấu giám thị 2 đã chọn


            danhSachPhanCong.add(new PhanCong(stt++, giamThi1.getMaCanBo(), giamThi1.getHoTen(), true, false, phongThi.getPhongThi()));
            danhSachPhanCong.add(new PhanCong(stt++, giamThi2.getMaCanBo(), giamThi2.getHoTen(), false, true, phongThi.getPhongThi()));
        }
       // lọc các cán bộ còn lại, chưa được dùng sẽ làm giám sát.
        List<CanBo> canBoGiamSat = candidates.stream()
                .filter(canBo -> !usedTeachers.contains(normalize(canBo.getMaCanBo())))
                .toList();
        List<GiamSat> danhSachGiamSat = assignSupervisors(canBoGiamSat, danhSachPhong, history); // gọi hàm phân công giám sát và kiểm tra điều kiện phân công giám sát

        return new AssignmentResult(danhSachPhanCong, danhSachGiamSat);
    }
    
    // tìm giám thị coi thi  hợp lệ cho 1 phòng thi
    private CanBo findTeacherForRoom(List<CanBo> candidates,
                                     Set<String> usedTeachers,
                                     PhongThi phongThi,
                                     CanBo pairedTeacher,
                                     AssignmentHistory history) {
        // duyệt qua danh sách cán bộ đã được xáo trộn.                                
        for (CanBo canBo : candidates) {
            String maCanBo = normalize(canBo.getMaCanBo()); // chuẩn hóa mã cán bộ về chữ thường để so sánh, tránh lỗi do khoảng trắng hoặc chữ hoa chữ thường.
            // kiểm tra cán bộ đã được phân công hay chưa, nếu đã dùng rồi thì bỏ qua.
            if (usedTeachers.contains(maCanBo)) {
                continue;
            }
            // kiểm tra cán bộ này đã từng làm giám thị phòng thi này chưa, nếu có rồi thì bỏ qua để tránh phân công lại.
            if (history.hasCanBoPhong(maCanBo, phongThi.getPhongThi())) {
                continue;
            }
            // nếu đang chọn giám thị 2, thì kiểm tra xem cán bộ này đã từng làm giám thị chung với giám thị 1 chưa, nếu có rồi thì bỏ qua để tránh phân công lại.
            if (pairedTeacher != null && history.hasCanBoPair(maCanBo, pairedTeacher.getMaCanBo())) {
                continue;
            }
            return canBo;
        }
        throw new AssignmentDeadlockException();
    }
    // tìm giám thị giám sát hợp lệ 
    private List<GiamSat> assignSupervisors(List<CanBo> canBoGiamSat,
                                            List<PhongThi> danhSachPhong,
                                            AssignmentHistory history) {
        List<GiamSat> result = new ArrayList<>();   // danh sách kết quả phân công giám sát
        if (canBoGiamSat.isEmpty()) {
            return result;   // nếu không còn cán bộ nào để phân công giám sát thì trả về danh sách rỗng.
        }

        int totalRooms = danhSachPhong.size();  // tổng số phòng thi
        int totalSupervisors = canBoGiamSat.size(); // tổng số cán bộ giám sát còn lại
        // tạm thời không sài
        if (isSupervisorRoomLimitEnabled()
                && totalSupervisors > (long) totalRooms * MAX_SUPERVISORS_PER_ROOM) {
            throw new IllegalArgumentException("Số cán bộ giám sát vượt quá giới hạn "
                    + MAX_SUPERVISORS_PER_ROOM + " người/phòng");
        }
        // nếu số giám sát nhiều hơn số phòng thi
        if (totalSupervisors > totalRooms) {
            return assignManySupervisors(canBoGiamSat, danhSachPhong, history);
        }
        // tính số phòng trung bình mỗi giám sát sẽ phụ trách , vd 10 phòng, 3 giám sát
        int base = totalRooms / totalSupervisors;   // 10 / 3 = 3 phòng mỗi giám sát 1 phòng
        int remainder = totalRooms % totalSupervisors; // 10 % 3 = 1 phòng còn lại, phân cho 1 giám sát đầu tiên trong số 3 giám sát.

        Set<String> assignedRoomThisAttempt = new HashSet<>();  // lưu các phòng đã có giám sát trong phần thử này.

        // duyệt qua từng giám sát để phân công phòng thi, ưu tiên phân cho giám sát đầu tiên nếu có phòng dư.
        for (int i = 0; i < totalSupervisors; i++) {
            CanBo canBo = canBoGiamSat.get(i);
            int roomCount = base + (i < remainder ? 1 : 0); // ưu tiên phân cho người đầu tiên nếu có phòng dư.
            List<PhongThi> rooms = new ArrayList<>(); // danh sách phòng thi được phân cho giám sát này
            // duyệt để phân công đủ số phòng cho mỗi giám sát .
            for (int j = 0; j < roomCount; j++) {
                PhongThi room = chooseSupervisorRoom(canBo, danhSachPhong, assignedRoomThisAttempt, history); // chọn phòng hợp lệ
                assignedRoomThisAttempt.add(normalize(room.getPhongThi()));  // đánh dấu phòng đã có giám sát trong lần thử này.
                rooms.add(room); // thêm phòng vào danh sách phòng của cán bộ giám sát này.
            }
            // sắp xếp phòng theo số thứ tự
            rooms.sort(Comparator.comparingInt(PhongThi::getStt));
            // kết quả được đưa vào danh sách
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
    // khi số giám sát nhiều hơn số phòng thi, thì mỗi giám sát sẽ được phân công 1 phòng, sau đó sẽ phân công thêm cho đến khi hết phòng hoặc hết giám sát.
    private List<GiamSat> assignManySupervisors(List<CanBo> canBoGiamSat,
                                                List<PhongThi> danhSachPhong,
                                                AssignmentHistory history) {
        List<GiamSat> result = new ArrayList<>();
        Map<String, Integer> supervisorCountByRoom = new HashMap<>(); // lưu số giám sát đã phân cho mỗi phòng, key là tên phòng đã chuẩn hóa, value là số giám sát đã phân cho phòng đó.
        
        // duyệt cán bộ giám sát
        for (int i = 0; i < canBoGiamSat.size(); i++) {
            CanBo canBo = canBoGiamSat.get(i);
            PhongThi room = chooseSupervisorRoomWithCapacity(canBo, danhSachPhong, supervisorCountByRoom, history); // chọn phòng hợp lệ, ưu tiên phòng có ít giám sát nhất
            supervisorCountByRoom.merge(normalize(room.getPhongThi()), 1, Integer::sum); // nếu chưa có ai thì gán 1 , còn đã có thì +1 giám sát cho phòng đó.
            List<PhongThi> rooms = List.of(room); // mỗi giám sát được phân 1 phòng trong trường hợp này, nên tạo danh sách phòng chỉ có 1 phần tử.

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
    // chọn phòng hợp lệ cho giám sát, khi giám sát > số phòng.
    private PhongThi chooseSupervisorRoomWithCapacity(CanBo canBo,
                                                      List<PhongThi> rooms,
                                                      Map<String, Integer> supervisorCountByRoom,
                                                      AssignmentHistory history) {
        PhongThi selectedRoom = null; // chưa chọn phòng nào
        int selectedRoomCount = Integer.MAX_VALUE; // số giám sát của phòng đã chọn, khởi tạo lớn nhất để tìm phòng có ít giám sát nhất.
        // duyệt từng phòng.
        for (PhongThi room : rooms) {
            String roomKey = normalize(room.getPhongThi()); // chuẩn hóa tên phòng để dùng làm key trong map, tránh lỗi do khoảng trắng hoặc chữ hoa chữ thường.
            int currentCount = supervisorCountByRoom.getOrDefault(roomKey, 0); // số giám sát hiện tại của phòng này để biết có bao nhiêu cán bộ giám sát rồi, nếu chưa có thì mặc định là 0.
            // kiểm tra giới hạn số lượng giám sát cho phòng này, nếu đã đủ rồi thì bỏ qua.
            if (currentCount >= MAX_SUPERVISORS_PER_ROOM) {
                continue;
            }
            // kiểm tra cán bộ này đã từng làm giám thị phòng thi này chưa, nếu có rồi thì bỏ qua.
            if (history.hasCanBoPhong(canBo.getMaCanBo(), room.getPhongThi())) {
                continue;
            }
            // kiểm tra cán bộ này đã từng làm giám sát phòng thi này chưa, nếu có rồi thì bỏ qua.
            if (history.hasGiamSatPhong(canBo.getMaCanBo(), room.getPhongThi())) {
                continue;
            }
            // chọn phòng có số lượng giám sát ít nhất
            if (currentCount < selectedRoomCount) {
                selectedRoom = room;
                selectedRoomCount = currentCount;
            }
        }
        // tìm được phòng hợp lệ nào thì trả về.
        if (selectedRoom != null) {
            return selectedRoom;
        }
        throw new AssignmentDeadlockException(); // nếu không tìm được thì ném ra ngoại lệ để thử lại.
    }

    private boolean isSupervisorRoomLimitEnabled() {
        return MAX_SUPERVISORS_PER_ROOM != UNLIMITED_SUPERVISORS_PER_ROOM;      
    }

    // chọn phòng khi giám sát <= số phòng.
    private PhongThi chooseSupervisorRoom(CanBo canBo,
                                          List<PhongThi> rooms,
                                          Set<String> assignedRoomThisAttempt,
                                          AssignmentHistory history) {
        // duyệt từng phòng để tìm phòng hợp lệ cho giám sát này.
        for (PhongThi room : rooms) {
            // phòng chưa được phân công giám sát trong lần thử này, và cán bộ này chưa từng làm giám thị hoặc giám sát phòng thi này trong lịch sử, thì chọn phòng này.
            if (!assignedRoomThisAttempt.contains(normalize(room.getPhongThi()))
                    && !history.hasCanBoPhong(canBo.getMaCanBo(), room.getPhongThi())
                    && !history.hasGiamSatPhong(canBo.getMaCanBo(), room.getPhongThi())) {
                return room;
            }
        }
        throw new AssignmentDeadlockException();
    }

    private String createRoomDescription(List<PhongThi> rooms) {
        // nếu không có phòng nào thì trả về "Không phân công phòng"
        if (rooms.isEmpty()) {
            return "Không phân công phòng";
        }
        // nếu chỉ có 1 phòng thì trả về tên phòng đó
        if (rooms.size() == 1) {
            return rooms.get(0).getPhongThi();
        }
        return "Từ " + rooms.get(0).getPhongThi() + " đến " + rooms.get(rooms.size() - 1).getPhongThi();
    }
    // kiểm tra dữ liệu trước khi phân công.
    private void validateInput(List<CanBo> danhSachCanBo, List<PhongThi> danhSachPhong, int maxRetry) {
        // kiểm tra danh sách cán bộ và phòng thi không được rỗng, nếu có lỗi thì ném ra ngoại lệ.
        if (danhSachCanBo == null || danhSachCanBo.isEmpty()) {
            throw new IllegalArgumentException("Danh sách cán bộ coi thi đang rỗng");
        }
        // néu danh sách phòng thi rỗng thì không thể phân công được, nên ném ra ngoại lệ.
        if (danhSachPhong == null || danhSachPhong.isEmpty()) {
            throw new IllegalArgumentException("Danh sách phòng thi đang rỗng");
        }
        // kiểm tra số lượng cán bộ có đủ để phân công ít nhất 2 giám thị cho mỗi phòng thi hay không, nếu không đủ thì ném ra ngoại lệ.
        if (danhSachCanBo.size() < danhSachPhong.size() * 2) {
            throw new IllegalArgumentException("Không đủ cán bộ để phân công coi thi");
        }
        // kiểm tra maxRetry phải lớn hơn 0, nếu không thì ném ra ngoại lệ.
        if (maxRetry <= 0) {
            throw new IllegalArgumentException("maxRetry phải lớn hơn 0");
        }
    }
    // chuẩn hóa chuỗi để so sánh, tránh lỗi do khoảng trắng hoặc chữ hoa chữ thường. Nếu giá trị null thì trả về chuỗi rỗng.
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
    // ngoại lệ được ném ra khi không tìm được phương án phân công hợp lệ trong một lần thử, để kích hoạt việc thử lại.
    private static class AssignmentDeadlockException extends RuntimeException {
    }
    // lớp kết quả phân công, chứa danh sách phân công giám thị và danh sách phân công giám sát, được trả về sau khi phân công thành công.
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
