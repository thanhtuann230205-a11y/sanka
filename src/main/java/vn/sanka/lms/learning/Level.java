package vn.sanka.lms.learning;
public enum Level {
 ZERO("Pre-N5 / Zero"), N5("N5"), N4("N4"), N3("N3"), N2("N2–N1");
 private final String label;
 Level(String label){this.label=label;}
 public String getLabel(){return label;}
}
