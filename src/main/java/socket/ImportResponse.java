package socket;

public class ImportResponse {
    private final int soCanBo;
    private final int soPhongThi;

    public ImportResponse(int soCanBo, int soPhongThi) {
        this.soCanBo = soCanBo;
        this.soPhongThi = soPhongThi;
    }

    public int getSoCanBo() {
        return soCanBo;
    }

    public int getSoPhongThi() {
        return soPhongThi;
    }
}
