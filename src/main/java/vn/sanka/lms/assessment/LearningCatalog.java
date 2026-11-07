package vn.sanka.lms.assessment;

import org.springframework.stereotype.Component;
import vn.sanka.lms.learning.Major;
import java.util.*;

/** Version 1 sample bank. Correct answers stay on the server, never in form models. */
@Component
public class LearningCatalog {
    public record Question(String id, String prompt, List<String> options, int correct, String explanation) {
        public String skill(){return id.matches("n[02345][a-e]")?switch(id.charAt(2)){
            case 'a' -> "Kanji / chữ viết";case 'b' -> "Ngữ pháp";case 'c' -> "Từ vựng";
            case 'd' -> "Nghe";default -> "Đọc";
        }:"Ứng dụng";}
        public String audioId(){return id.matches("n[02345]d")?id.substring(0,2):null;}
        public QuestionView view(){return new QuestionView(id,prompt,options,skill(),audioId());}
    }
    public record QuestionView(String id, String prompt, List<String> options,String skill,String audioId) {}
    public record Topic(String word, String reading, String meaning, String document, String incident,
                        String resolution, String wrongAction, String improvement) {}

    public Topic topic(Major major) {
        return switch(major) {
            case MECHANICAL -> new Topic("部品","ぶひん","Linh kiện",
                "点検記録：部品Aに傷があります。担当者に報告し、交換が必要か確認してください。作業再開は責任者の許可後です。",
                "部品Aに傷を見つけました。", "責任者に報告し、交換の必要性を確認します。", "傷を無視して作業を続けます。",
                "点検記録を共有し、傷の発生原因を調査します。");
            case ELECTRICAL -> new Topic("電圧","でんあつ","Điện áp",
                "点検記録：装置Bの電圧が仕様の範囲外です。作業を中断し、責任者に連絡してください。確認が終わるまで再開しないでください。",
                "装置Bの電圧が仕様の範囲外です。", "作業を中断し、責任者に確認を依頼します。", "確認せずに運転を続けます。",
                "測定記録と仕様を照合し、原因と再発防止策を検討します。");
            case MECHATRONICS -> new Topic("センサー","せんさー","Cảm biến",
                "点検記録：ロボットCのセンサーが反応しません。運転を止め、担当者に状況を報告してください。再開には責任者の確認が必要です。",
                "ロボットCのセンサーが反応しません。", "運転を止め、担当者に状況を報告します。", "確認せずにロボットを動かします。",
                "発生条件と記録を整理し、担当者と改善案を検討します。");
            case AUTOMATION -> new Topic("生産ライン","せいさんライン","Dây chuyền sản xuất",
                "点検記録：生産ラインDが異常信号で停止しました。停止時刻と表示内容を記録し、責任者に報告してください。許可なく再起動しないでください。",
                "生産ラインDが異常信号で停止しました。", "停止時刻と表示内容を記録し、責任者に報告します。", "許可なく再起動します。",
                "停止履歴を分析し、異常信号の原因と改善策を提案します。");
            case IT -> new Topic("不具合","ふぐあい","Lỗi phần mềm",
                "障害記録：ログイン後にエラーが表示されます。再現手順と発生時刻を記録して、担当者に報告してください。個人情報は報告に含めないでください。",
                "ログイン後にエラーが表示されます。", "再現手順と発生時刻を記録して、担当者に報告します。", "個人情報を公開して報告します。",
                "再現条件とログを分析し、修正後に回帰テストを実施します。");
        };
    }

    private Question q(String id,String prompt,int correct,String explanation,String...options) {
        return new Question(id,prompt,List.of(options),correct,explanation);
    }
    public List<Question> questions(Stage stage, Major major) {
        if(stage==Stage.PLACEMENT) {
            var all=new ArrayList<Question>();
            for(var group:List.of(Stage.N5,Stage.N4,Stage.N3,Stage.N2))all.addAll(questions(group,major));
            return all;
        }
        if(stage==Stage.N5) return List.of(
            q("n5a","『学生』の読み方は？",1,"学生 được đọc là がくせい.","せんせい","がくせい","かいしゃ"),
            q("n5b","私は毎日学校（　）行きます。",2,"に chỉ đích đến với 行きます.","を","で","に"),
            q("n5c","『昨日』の意味は？",0,"昨日: ngày hôm qua.","Hôm qua","Hôm nay","Ngày mai"),
            q("n5d","Nghe âm thanh. Người nói làm việc vào khoảng thời gian nào?",1,"毎日9時から5時まで働きます。から…まで: từ…đến.","Làm lúc 5 giờ","Làm từ 9 đến 5 giờ","Nghỉ từ 9 đến 5 giờ"),
            q("n5e","『これは私の本です』 nghĩa là gì?",2,"の biểu thị sở hữu.","Tôi đọc sách","Đó là trường tôi","Đây là sách của tôi"));
        if(stage==Stage.N4) return List.of(
            q("n4a","作業の前に、電源を（　）ください。",0,"Dạng て + ください dùng để yêu cầu lịch sự.","確認して","確認する","確認した"),
            q("n4b","『故障したので、修理を依頼しました』 nghĩa là gì?",2,"ので nêu nguyên nhân.","Đã sửa nên hỏng","Không cần sửa","Vì hỏng nên đã yêu cầu sửa chữa"),
            q("n4c","『入ってはいけません』 diễn đạt gì?",1,"てはいけません: không được phép.","Hãy vào","Không được vào","Có thể vào"),
            q("n4d","Nghe âm thanh. Người nói sẽ làm gì trước?",0,"資料を確認してから、会議を始めます。てから chỉ thứ tự trước/sau.","Kiểm tra tài liệu rồi bắt đầu họp","Bắt đầu họp rồi kiểm tra","Hủy cuộc họp"),
            q("n4e","『担当者に連絡したほうがいいです』 là gì?",2,"たほうがいい: lời khuyên nên làm.","Đừng liên hệ","Đã liên hệ xong","Nên liên hệ người phụ trách"));
        if(stage==Stage.N3)return List.of(
            q("n3a","『改善』の読み方は？",2,"改善 đọc là かいぜん.","かいぎ","かいしゃ","かいぜん"),
            q("n3b","作業が終わり（　）、報告してください。",0,"次第: ngay sau khi hoàn thành.","次第","ながら","ばかり"),
            q("n3c","『手順』 nghĩa là gì?",1,"手順: trình tự, các bước thực hiện.","Giá thành","Trình tự thực hiện","Thời tiết"),
            q("n3d","Nghe thông báo. Nhóm cần gửi gì trước 3 giờ?",2,"検証結果は、午後3時までに担当者へ送ってください。Gửi kết quả kiểm chứng trước 3 giờ.","Đơn nghỉ phép","Danh sách khách hàng","Kết quả kiểm chứng"),
            q("n3e","『確認が済むまで、変更を反映しないでください』 yêu cầu gì?",0,"まで nêu giới hạn: chưa áp dụng trước khi xác nhận xong.","Chờ xác nhận xong mới áp dụng thay đổi","Áp dụng ngay","Không cần xác nhận"));
        if(stage==Stage.N2)return List.of(
            q("n2a","『検証』の読み方は？",1,"検証 đọc là けんしょう.","けんせつ","けんしょう","けんとう"),
            q("n2b","品質を維持する（　）、十分な検証が不可欠です。",2,"うえで: trong việc/để thực hiện việc; điều kiện quan trọng.","たびに","ものの","うえで"),
            q("n2c","『不可欠』 nghĩa là gì?",0,"不可欠: không thể thiếu.","Không thể thiếu","Không cần thiết","Chưa sử dụng"),
            q("n2d","Nghe cuộc trao đổi. Vì sao chưa nên áp dụng toàn bộ thay đổi?",1,"一部の検証が完了していないため、全体への適用は見合わせます。Chưa kiểm chứng xong một phần.","Đã hoàn thành toàn bộ","Một phần chưa kiểm chứng xong","Đã hết giờ làm"),
            q("n2e","『効果が認められる一方、運用負荷の増大も懸念される』 thể hiện gì?",2,"一方 nêu hai mặt; 懸念 là lo ngại.","Chỉ có ưu điểm","Không có hiệu quả","Có hiệu quả nhưng lo tăng gánh nặng vận hành"));
        Topic t=topic(major);
        return switch(stage) {
            case TECHNICAL -> List.of(
                q("tech1","『"+t.word()+"』の読み方は？",1,"Cách đọc: "+t.reading(),"かいぎ",t.reading(),"よてい"),
                q("tech2","『"+t.word()+"』の意味は？",0,"Nghĩa: "+t.meaning(),t.meaning(),"Lịch họp","Ngày nghỉ"),
                q("tech3","『担当者』 là ai?",2,"担当者 là người phụ trách.","Khách du lịch","Sinh viên mới","Người phụ trách"),
                q("tech4","『確認』 nghĩa là gì?",1,"確認: kiểm tra/xác nhận.","Bỏ qua","Kiểm tra, xác nhận","Kết thúc hợp đồng"));
            case READING -> List.of(
                q("read1","Theo tài liệu, bước phù hợp là gì?",2,"Tài liệu yêu cầu báo cáo và xác nhận.","Tự tiếp tục không báo cáo",t.wrongAction(),t.resolution()),
                q("read2","『記録』 có nghĩa là gì?",0,"記録: ghi chép, bản ghi.","Ghi chép","Xóa thông tin","Nghỉ ngơi"),
                q("read3","『報告してください』 yêu cầu gì?",1,"報告してください: hãy báo cáo.","Hãy bỏ qua","Hãy báo cáo","Hãy tự kết luận"),
                q("read4","Một báo cáo nên dựa trên điều gì?",2,"Cần bằng chứng, hiện tượng và thời điểm quan sát được.","Suy đoán không có căn cứ","Tin đồn","Hiện tượng và dữ liệu đã ghi nhận"));
            case SITUATION -> List.of(
                q("sit1","同僚：『"+t.incident()+"』 Bạn phản hồi thế nào?",0,"Phản hồi cần đúng quy trình và báo cáo người phụ trách.",t.resolution(),t.wrongAction(),"何も報告しません。"),
                q("sit2","Câu nhờ đồng nghiệp xác nhận lịch sự là gì?",2,"いただけますか là cách nhờ lịch sự.","知らない。","確認しない。","確認していただけますか。"),
                q("sit3","Chưa biết nguyên nhân, bạn nên nói gì?",1,"Phân biệt quan sát thực tế và nguyên nhân chưa xác nhận.","原因は絶対にあなたです。","原因はまだ確認できていません。","原因を調べる必要はありません。"),
                q("sit4","Thông tin nào nên đi kèm báo cáo sự cố?",0,"Nêu thời điểm, hiện tượng, xử lý ban đầu và yêu cầu hỗ trợ.","Thời điểm và hiện tượng","Chỉ cảm xúc cá nhân","Chỉ tên đồng nghiệp"));
            case APPLICATION -> List.of(
                q("app1","Chọn thứ tự báo cáo phù hợp.",1,"Quan sát → báo cáo → xác nhận hướng xử lý.","Kết luận → bỏ qua dữ liệu","Ghi nhận → báo cáo → xác nhận","Tiếp tục → che giấu"),
                q("app2","Tiêu đề email báo cáo phù hợp là gì?",2,"Tiêu đề cụ thể giúp nhận biết sự cố.","こんにちは","無題","【報告】作業中に発生した問題について"),
                q("app3","Khi chưa xác nhận được thời gian khắc phục, nên viết gì?",0,"Tránh cam kết không có căn cứ; hẹn cập nhật sau xác nhận.","確認でき次第、ご連絡いたします。","必ず1分で直ります。","連絡しません。"),
                q("app4","Đâu là đề xuất cải tiến phù hợp ngành?",1,"Đề xuất cần dựa vào dữ liệu và có kiểm chứng.",t.wrongAction(),t.improvement(),"記録を全部消します。"));
            case ADVANCED -> List.of(
                q("adv1","Đâu là bước nâng cao phù hợp?",2,"Cải tiến cần dựa trên phân tích và xác minh.","Không xem dữ liệu","Chỉ tăng tốc làm việc",t.improvement()),
                q("adv2","『再発防止』 nghĩa là gì?",0,"再発防止: ngăn ngừa tái diễn.","Ngăn ngừa tái diễn","Lặp lại lỗi","Ẩn báo cáo"),
                q("adv3","Để đánh giá hiệu quả cải tiến cần gì?",1,"Cần đo trước/sau bằng chỉ số nhất quán.","Chỉ ý kiến cá nhân","So sánh số liệu trước và sau","Không cần kiểm tra"),
                q("adv4","『検証』 nghĩa là gì?",2,"検証: kiểm chứng/xác minh.","Bỏ qua","Quảng cáo","Kiểm chứng"));
            default -> throw new IllegalArgumentException("Unsupported stage");
        };
    }

    public String lesson(Stage stage, Major major) {
        if(stage==Stage.PLACEMENT)return "20 câu theo bốn nhóm N5, N4, N3, N2–N1; mỗi nhóm có chữ viết, ngữ pháp, từ vựng, nghe và đọc. Cần đạt 4/5 câu ở mỗi nhóm liên tiếp để được xếp lên nhóm tiếp theo. Nếu chưa đạt N5, vào Pre-N5/Zero. Đây là phân luồng nội bộ, không thay thế chứng chỉ JLPT; N2–N1 là một nhóm học chung.";
        if(List.of(Stage.N5,Stage.N4,Stage.N3,Stage.N2).contains(stage))return "Hoàn thành bài học tương ứng trước khi thi. Ngưỡng đạt 80%. Có thể ôn tập và thi lại; mỗi lần thi đều được lưu. Nghe file âm thanh ở câu nghe trước khi chọn đáp án.";
        Topic t=topic(major);
        return switch(stage) {
            case TECHNICAL -> t.word()+"（"+t.reading()+"）: "+t.meaning()+"\n担当者（たんとうしゃ）: người phụ trách\n確認（かくにん）: kiểm tra/xác nhận\n報告（ほうこく）: báo cáo\n記録（きろく）: ghi chép\n例："+t.incident();
            case READING -> "TÀI LIỆU MẪU\n"+t.document()+"\n\nĐọc để xác định: hiện tượng nào xảy ra, việc cần thực hiện, ai chịu trách nhiệm xác nhận. Đây là bài luyện ngôn ngữ; thao tác kỹ thuật thực tế phải theo quy trình của đơn vị.";
            case SITUATION -> "ĐÓNG VAI: bạn là kỹ thuật viên, người nhận là trưởng nhóm.\n同僚："+t.incident()+"\nMục tiêu: xác nhận thông tin, báo cáo hiện tượng, nhờ hỗ trợ lịch sự.\nMẫu：確認していただけますか。原因はまだ確認できていません。\nHướng xử lý: "+t.resolution();
            case APPLICATION -> "Ôn lại tài liệu, phản hồi tình huống, email và báo cáo đã được giảng viên nhận xét. Bài kiểm tra này đo kiến thức ứng dụng trong phạm vi học liệu mẫu; kết quả thực hành nằm ở rubric giảng viên chấm.";
            case ADVANCED -> "再発防止（さいはつぼうし）: ngăn ngừa tái diễn\n検証（けんしょう）: kiểm chứng\n改善（かいぜん）: cải tiến\n"+t.improvement()+"\nLập một đề xuất có dữ liệu hiện trạng, nguyên nhân giả định, hành động và chỉ số kiểm chứng.";
            default -> "";
        };
    }

    public List<Question> questionsForLevel(vn.sanka.lms.learning.Level level,Major major) {
        if(level!=vn.sanka.lms.learning.Level.ZERO)return questions(Stage.valueOf(level.name()),major);
        return List.of(
            q("n0a","Chọn cách đọc của あ.",0,"あ đọc là a.","a","i","u"),
            q("n0b","『私はアンです』 có ý nghĩa gì?",1,"です là cách kết thúc câu giới thiệu lịch sự.","Tôi đi làm","Tôi là An","Tôi nghỉ học"),
            q("n0c","『本』 nghĩa là gì?",2,"本: quyển sách.","Bút","Bàn","Sách"),
            q("n0d","Nghe lời chào và chọn nghĩa.",0,"おはようございます。Chào buổi sáng.","Chào buổi sáng","Chúc ngủ ngon","Tạm biệt"),
            q("n0e","『私は学生です』 nói về điều gì?",1,"学生: sinh viên/học sinh.","Tên người nói","Người nói là sinh viên","Địa chỉ người nói"));
    }

    public String prompt(PracticeKind kind, Major major) {
        Topic t=topic(major);
        String context="Chuyên ngành: "+major.getLabel()+"\nBối cảnh: "+t.incident()+"\nTài liệu: "+t.document()+"\n\n";
        return context+switch(kind) {
            case SITUATION -> "Viết hội thoại tiếng Nhật (ít nhất 4 lượt) giữa bạn và trưởng nhóm: mô tả hiện tượng, xác nhận thông tin, nhờ hỗ trợ và thống nhất bước tiếp theo. Không tự khẳng định nguyên nhân khi chưa có bằng chứng.";
            case EMAIL -> "Viết email tiếng Nhật cho trưởng nhóm gồm: tiêu đề, người nhận, chào hỏi, thời điểm/hiện tượng, xử lý ban đầu, yêu cầu xác nhận và lời kết. Không đưa thông tin cá nhân thật vào bài.";
            case REPORT -> "Viết báo cáo tiếng Nhật gồm: ngày giờ, đối tượng/thiết bị, hiện tượng và bằng chứng, phạm vi ảnh hưởng, xử lý ban đầu, vấn đề chưa xác nhận, kiến nghị và bước tiếp theo. Phân biệt sự thật và giả định.";
            case ADVANCED -> "Viết đề xuất cải tiến bằng tiếng Nhật: hiện trạng, nguyên nhân cần kiểm chứng, hành động đề xuất, người phụ trách, thời hạn và chỉ số đo trước/sau. Liên hệ đúng chuyên ngành.";
        };
    }
}
