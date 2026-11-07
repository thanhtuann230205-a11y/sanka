package vn.sanka.lms.learning;
public enum Level {
 ZERO("Chưa đạt S5"), N5("S5 (tương đương N5)"), N4("S4 (tương đương N4)"), N3("S3 (tương đương N3)"), N2("S2–S1 (tương đương N2–N1)");
 private final String label;
 Level(String label){this.label=label;}
 public String getLabel(){return label;}
}
