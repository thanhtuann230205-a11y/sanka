package vn.sanka.lms.learning;
public enum Level {
 ZERO("Chưa đạt S5"), N5("S5"), N4("S4"), N3("S3"), N2("S2–S1");
 private final String label;
 Level(String label){this.label=label;}
 public String getLabel(){return label;}
}
