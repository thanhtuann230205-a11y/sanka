package vn.sanka.lms.gamification;

public enum ActivityKind {
    LESSON("Hoàn thành bài học"), PLACEMENT("Hoàn thành kiểm tra đầu vào"),
    EXAM_PASS("Đạt bài kiểm tra"), EXAM_PRACTICE("Luyện tập kiểm tra"),
    PRACTICE_PASS("Thực hành được giảng viên chấm đạt"),
    PERIODIC("Hoàn thành ôn tập định kỳ"), LANGUAGE_PASS("Ứng dụng theo cấp được chấm đạt"),
    SUBMISSION("Nộp bài thực hành");
    private final String label;
    ActivityKind(String label){this.label=label;}
    public String getLabel(){return label;}
}
