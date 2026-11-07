package vn.sanka.lms.learning;

import jakarta.persistence.*;

@Entity @Table(name="term_bookmarks",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","term_id"}))
public class TermBookmark {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Column(nullable=false) private Long termId;
    protected TermBookmark() {}
    public TermBookmark(Long studentId,Long termId){this.studentId=studentId;this.termId=termId;}
    public Long getTermId(){return termId;}
}
