package vn.sanka.lms.assessment;

public enum PracticeKind {
    SITUATION("Phản hồi tình huống"), EMAIL("Viết email công việc"),
    REPORT("Viết báo cáo công việc"), ADVANCED("Đề xuất cải tiến chuyên ngành");
    private final String label;
    PracticeKind(String label) { this.label = label; }
    public String getLabel() { return label; }
}
