package vn.sanka.lms.assessment;

public enum Stage {
    PLACEMENT("Kiểm tra đầu vào"), N5("Kiểm tra cơ bản S5"), N4("Kiểm tra cơ bản S4"),
    N3("Kiểm tra đạt N3"), N2("Kiểm tra nhóm N2–N1"),
    TECHNICAL("Thuật ngữ chuyên ngành"), READING("Đọc hiểu tài liệu"),
    SITUATION("Giao tiếp và xử lý tình huống"), APPLICATION("Đánh giá kiến thức ứng dụng"),
    ADVANCED("Kiến thức nâng cao");

    private final String label;
    Stage(String label) { this.label = label; }
    public String getLabel() { return label; }
}
