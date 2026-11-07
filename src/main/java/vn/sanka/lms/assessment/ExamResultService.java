package vn.sanka.lms.assessment;

import org.springframework.stereotype.Service;
import vn.sanka.lms.learning.Level;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ExamResultService {
    private final ExamAnswerRepository snapshots;
    public ExamResultService(ExamAnswerRepository snapshots){this.snapshots=snapshots;}
    public record Option(String text,boolean selected,boolean correct){}
    public record Question(int number,String prompt,String skill,String audioId,List<Option> options,boolean correct,String explanation){}
    public record Summary(Level level,String nextStep,int correctCount,int totalQuestions,List<Question> questions,boolean legacy){}
    // The controller must establish attempt ownership before calling this service.
    public Summary summary(ExamAttempt attempt){
        var stored=snapshots.findByAttemptIdOrderByQuestionNumberAsc(attempt.getId());
        var questions=new ArrayList<Question>();
        for(var answer:stored){
            var options=new ArrayList<Option>();
            for(int i=0;i<answer.getOptions().size();i++)options.add(new Option(answer.getOptions().get(i),i==answer.getChosenIndex(),i==answer.getCorrectIndex()));
            questions.add(new Question(answer.getQuestionNumber(),answer.getPrompt(),answer.getSkill(),answer.getAudioId(),options,
                answer.getChosenIndex()==answer.getCorrectIndex(),answer.getExplanation()));
        }
        if(stored.isEmpty()){
            // Older attempts retained the exact question, selected answer, correct answer and explanation as text.
            var matcher=Pattern.compile("(?m)^([^\\n]+)\\nĐã chọn: ([^\\n]+) ([✓✗])\\nĐáp án: ([^\\n]+)\\nGiải thích: ([\\s\\S]*?)(?=\\n\\n|$)").matcher(attempt.getEvidence());
            while(matcher.find()){
                String selected=matcher.group(2),correct=matcher.group(4);boolean right="✓".equals(matcher.group(3));
                var options=new ArrayList<Option>();options.add(new Option(selected,true,selected.equals(correct)));
                if(!selected.equals(correct))options.add(new Option(correct,false,true));
                questions.add(new Question(questions.size()+1,matcher.group(1),"Kết quả đã lưu",null,options,right,matcher.group(5)));
            }
        }
        Level level=attempt.getAssignedLevel();
        if(level==null&&attempt.getStage()==Stage.PLACEMENT){
            var match=Pattern.compile("Lộ trình được xếp: (ZERO|N5|N4|N3|N2)\\b").matcher(attempt.getEvidence());
            if(match.find())level=Level.valueOf(match.group(1));
        }
        String nextStep=level==null?"Tiếp tục theo lộ trình học của bạn.":switch(level){
            case ZERO,N5 -> "Học cơ bản S5 (tương đương N5), qua kiểm tra S5 rồi học cơ bản S4. Đạt S4 mới được chọn chuyên ngành.";
            case N4,N3,N2 -> "Bạn đạt S4 trở lên. Chọn chuyên ngành để bắt đầu học tiếng Nhật chuyên ngành phù hợp với bạn.";
        };
        return new Summary(level,nextStep,(int)questions.stream().filter(Question::correct).count(),questions.size(),List.copyOf(questions),stored.isEmpty());
    }
}
