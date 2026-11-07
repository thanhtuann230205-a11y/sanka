package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import java.util.List;

@Entity @Table(name="exam_answer_snapshots",uniqueConstraints=@UniqueConstraint(columnNames={"attempt_id","question_number"}))
public class ExamAnswerSnapshot {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long attemptId;
    @Column(nullable=false) private int questionNumber;
    @Column(nullable=false,length=80) private String questionId;
    @Column(nullable=false,columnDefinition="TEXT") private String prompt;
    @Column(nullable=false,length=80) private String skill;
    @Column(length=80) private String audioId;
    @Convert(converter=StringListConverter.class) @Column(name="options_json",nullable=false,columnDefinition="TEXT") private List<String> options;
    @Column(nullable=false) private int chosenIndex;
    @Column(nullable=false) private int correctIndex;
    @Column(nullable=false,columnDefinition="TEXT") private String explanation;
    protected ExamAnswerSnapshot(){}
    public ExamAnswerSnapshot(Long attemptId,int number,LearningCatalog.Question question,int chosen){
        this.attemptId=attemptId;this.questionNumber=number;this.questionId=question.id();this.prompt=question.prompt();
        this.skill=question.skill();this.audioId=question.audioId();this.options=List.copyOf(question.options());
        this.chosenIndex=chosen;this.correctIndex=question.correct();this.explanation=question.explanation();
    }
    public int getQuestionNumber(){return questionNumber;}
    public String getPrompt(){return prompt;}
    public String getSkill(){return skill;}
    public String getAudioId(){return audioId;}
    public List<String> getOptions(){return List.copyOf(options);}
    public int getChosenIndex(){return chosenIndex;}
    public int getCorrectIndex(){return correctIndex;}
    public String getExplanation(){return explanation;}
}
