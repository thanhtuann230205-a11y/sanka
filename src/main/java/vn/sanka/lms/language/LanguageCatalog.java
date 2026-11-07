package vn.sanka.lms.language;

import org.springframework.stereotype.Component;
import vn.sanka.lms.learning.*;
import java.util.*;

/** Sample teaching content, grouped as in the product's five-level model. */
@Component
public class LanguageCatalog {
    public record Word(String japanese,String reading,String meaning,String example) {}
    public record Grammar(String pattern,String explanation,String example) {}
    public record Course(Level level,String theme,List<Word> words,List<Grammar> grammar,
                         String audioId,String transcript,String reading,String translation,String exercise) {}
    private Word w(String jp,String reading,String vn,String example){return new Word(jp,reading,vn,example);}
    private Grammar g(String pattern,String explanation,String example){return new Grammar(pattern,explanation,example);}
    public Course course(Level level) {
        return switch(level) {
            case ZERO -> new Course(level,"Bảng chữ cái · phát âm · chào hỏi",List.of(
                w("私","わたし","Tôi","私はアンです。"),w("学生","がくせい","Sinh viên","私は学生です。"),
                w("本","ほん","Sách","これは本です。"),w("学校","がっこう","Trường học","学校に行きます。"),
                w("名前","なまえ","Tên","お名前は何ですか。"),w("先生","せんせい","Giáo viên","先生に聞きます。")),List.of(
                g("AはBです","Giới thiệu A là B, dùng cách nói lịch sự.","私は学生です。"),
                g("～ですか","Thêm か để đặt câu hỏi.","学生ですか。"),g("これ・それ","Chỉ đồ vật gần người nói hoặc người nghe.","これは本です。")),
                "n0","おはようございます。私はアンです。学生です。よろしくお願いします。",
                "私はアンです。学生です。これは日本語の本です。","Tôi là An. Tôi là sinh viên. Đây là sách tiếng Nhật.",
                "Viết lời chào và giới thiệu bản thân với nhóm thực tập trong ngành đang chọn: tên giả, vai trò và một đồ vật bạn dùng khi học. Tối thiểu 20 ký tự.");
            case N5 -> new Course(level,"Sinh hoạt · thời gian · học tập",List.of(
                w("毎日","まいにち","Mỗi ngày","毎日勉強します。"),w("昨日","きのう","Hôm qua","昨日働きました。"),
                w("明日","あした","Ngày mai","明日学校に行きます。"),w("時間","じかん","Thời gian","時間を確認します。"),
                w("仕事","しごと","Công việc","仕事は9時からです。"),w("休み","やすみ","Giờ nghỉ/ngày nghỉ","お昼に休みます。")),List.of(
                g("に・で・を","に chỉ đích đến, で chỉ nơi hành động, を chỉ đối tượng.","学校で本を読みます。"),
                g("～から～まで","Chỉ khoảng từ…đến….","9時から5時まで働きます。"),g("～ました","Dạng lịch sự quá khứ.","昨日、勉強しました。")),
                "n5","毎日、九時から五時まで働きます。お昼に一時間休みます。",
                "明日は9時から学校で勉強します。午後は図書館で日本語の本を読みます。","Ngày mai tôi học ở trường từ 9 giờ. Buổi chiều đọc sách tiếng Nhật ở thư viện.",
                "Viết lịch một ngày thực tập trong ngành của bạn: thời gian bắt đầu, kết thúc, nghỉ và ít nhất hai hoạt động. Dùng から・まで・で; tối thiểu 40 ký tự.");
            case N4 -> new Course(level,"Giao tiếp đời sống · quy trình công việc",List.of(
                w("確認","かくにん","Xác nhận","資料を確認します。"),w("故障","こしょう","Hỏng hóc","故障を報告します。"),
                w("担当者","たんとうしゃ","Người phụ trách","担当者に連絡します。"),w("資料","しりょう","Tài liệu","資料を読みます。"),
                w("開始","かいし","Bắt đầu","作業を開始します。"),w("連絡","れんらく","Liên hệ","すぐに連絡します。")),List.of(
                g("～てから","Thực hiện việc sau khi xong việc trước.","確認してから始めます。"),
                g("～ので","Nêu nguyên nhân lịch sự.","故障したので、連絡しました。"),g("～てはいけません","Diễn tả điều không được phép.","入ってはいけません。")),
                "n4","資料を確認してから、会議を始めます。まだ確認していない方は、先に資料を読んでください。",
                "明日の会議は午後2時からです。資料を読んでから、参加してください。不明な点は担当者に確認してください。",
                "Cuộc họp ngày mai từ 2 giờ chiều. Đọc tài liệu trước khi tham gia. Điểm chưa rõ hãy xác nhận với người phụ trách.",
                "Viết hướng dẫn ba bước cho hoạt động học/thực tập trong ngành. Nêu một điều cần xác nhận và một quy định. Dùng てから・てください; tối thiểu 80 ký tự.");
            case N3 -> new Course(level,"Trao đổi thông tin · báo cáo thay đổi",List.of(
                w("改善","かいぜん","Cải tiến","改善案を提案します。"),w("手順","てじゅん","Trình tự","手順を確認します。"),
                w("変更","へんこう","Thay đổi","変更点を報告します。"),w("結果","けっか","Kết quả","結果を共有します。"),
                w("期限","きげん","Thời hạn","期限までに提出します。"),w("原因","げんいん","Nguyên nhân","原因は調査中です。")),List.of(
                g("～次第","Ngay khi hoàn tất một việc.","確認でき次第、連絡します。"),
                g("～までに","Hạn chót hoàn tất.","3時までに送ってください。"),g("～まで～ない","Chưa làm việc sau cho đến khi điều kiện trước hoàn tất.","確認が済むまで変更しません。")),
                "n3","検証結果は、午後三時までに担当者へ送ってください。変更点も一緒に報告してください。",
                "手順を変更したため、作業前に全員で確認する必要があります。確認が済むまで変更を反映せず、不明点は担当者に問い合わせてください。",
                "Do thay đổi quy trình, mọi người cần xác nhận trước khi làm việc. Chưa áp dụng khi chưa xác nhận xong; điểm chưa rõ hãy hỏi người phụ trách.",
                "Viết thông báo thay đổi quy trình trong ngành đang chọn: thay đổi gì, ai xác nhận, thời hạn và điều kiện áp dụng. Tách dữ kiện và giả định; tối thiểu 100 ký tự.");
            case N2 -> new Course(level,"Đọc hiểu phức hợp · lập luận · giao tiếp chuyên môn",List.of(
                w("検証","けんしょう","Kiểm chứng","仮説を検証します。"),w("不可欠","ふかけつ","Không thể thiếu","検証は不可欠です。"),
                w("懸念","けねん","Lo ngại","負荷の増大が懸念されます。"),w("影響範囲","えいきょうはんい","Phạm vi ảnh hưởng","影響範囲を調べます。"),
                w("妥当性","だとうせい","Tính hợp lý","妥当性を検討します。"),w("見合わせる","みあわせる","Tạm hoãn","適用を見合わせます。")),List.of(
                g("～うえで","Nêu điều quan trọng trong việc thực hiện.","品質を維持するうえで不可欠です。"),
                g("～一方","Trình bày hai mặt hoặc hai xu hướng.","効果がある一方、負荷も増えます。"),
                g("～を踏まえて","Dựa trên và cân nhắc thông tin đã có.","結果を踏まえて判断します。")),
                "n2","一部の検証が完了していないため、全体への適用は見合わせます。影響範囲を確認したうえで、改めて判断する必要があります。",
                "改善案の効果が認められる一方、運用負荷の増大も懸念される。一部の検証が未完了であることを踏まえ、段階的な適用と追加調査の妥当性を検討する。",
                "Đề xuất có hiệu quả nhưng cũng lo tăng gánh nặng vận hành. Vì còn kiểm chứng chưa xong, xem xét tính hợp lý của việc áp dụng từng bước và khảo sát bổ sung.",
                "Viết bản kiến nghị chuyên môn: so sánh hai phương án cải tiến đúng ngành, nêu căn cứ, giới hạn dữ liệu, ưu/nhược và đề xuất có điều kiện. Tối thiểu 150 ký tự. Nhóm N2–N1 là nhóm học chung, chưa chứng nhận riêng N1.");
        };
    }
    public int minimum(Level level){return switch(level){case ZERO->20;case N5->40;case N4->80;case N3->100;case N2->150;};}
}
