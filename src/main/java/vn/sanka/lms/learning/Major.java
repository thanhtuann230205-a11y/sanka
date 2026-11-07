package vn.sanka.lms.learning;
public enum Major {
 MECHANICAL("Cơ khí"), ELECTRICAL("Điện – Điện tử"), MECHATRONICS("Cơ điện tử"), AUTOMATION("Tự động hóa"), IT("Công nghệ thông tin");
 private final String label;
 Major(String label) {this.label=label;}
 public String getLabel(){return label;}
}