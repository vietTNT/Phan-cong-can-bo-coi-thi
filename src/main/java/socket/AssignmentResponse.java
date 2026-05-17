package socket;

import java.io.File;
import java.util.List;

public class AssignmentResponse {
    private final String tenCaThi;
    private final List<File> files;

    public AssignmentResponse(String tenCaThi, List<File> files) {
        this.tenCaThi = tenCaThi;
        this.files = List.copyOf(files);
    }

    public String getTenCaThi() {
        return tenCaThi;
    }

    public List<File> getFiles() {
        return files;
    }
}
