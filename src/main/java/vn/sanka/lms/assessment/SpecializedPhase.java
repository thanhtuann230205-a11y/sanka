package vn.sanka.lms.assessment;

public enum SpecializedPhase {
    TERMS("Từ vựng và thuật ngữ kỹ thuật",Stage.TECHNICAL),
    READING("Đọc hiểu tài liệu chuyên ngành",Stage.READING),
    SITUATION("Giao tiếp và xử lý tình huống thực tế",Stage.SITUATION),
    WRITING("Viết email và báo cáo công việc",null),
    APPLICATION("Đánh giá năng lực ứng dụng",Stage.APPLICATION),
    ADVANCED("Tiếp tục nâng cao năng lực chuyên ngành",Stage.ADVANCED);
    private final String label;
    private final Stage exam;
    SpecializedPhase(String label,Stage exam){this.label=label;this.exam=exam;}
    public String getLabel(){return label;}
    public Stage getExam(){return exam;}
}
