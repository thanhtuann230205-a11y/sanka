package vn.sanka.lms.assessment;

import org.springframework.stereotype.Component;
import vn.sanka.lms.learning.Major;
import java.util.*;

@Component
public class SpecializationContent {
    private final LearningCatalog catalog;
    public SpecializationContent(LearningCatalog catalog){this.catalog=catalog;}
    public String translation(Major major) {
        return switch(major) {
            case MECHANICAL -> "Biên bản kiểm tra: linh kiện A có vết xước. Hãy báo người phụ trách và xác nhận có cần thay thế không. Chỉ tiếp tục công việc sau khi người có trách nhiệm cho phép.";
            case ELECTRICAL -> "Biên bản kiểm tra: điện áp của thiết bị B nằm ngoài phạm vi thông số. Tạm dừng công việc và liên hệ người có trách nhiệm. Không tiếp tục cho đến khi việc xác nhận hoàn tất.";
            case MECHATRONICS -> "Biên bản kiểm tra: cảm biến của robot C không phản hồi. Dừng vận hành và báo cáo tình trạng cho người phụ trách. Cần xác nhận của người có trách nhiệm trước khi tiếp tục.";
            case AUTOMATION -> "Biên bản kiểm tra: dây chuyền D đã dừng do tín hiệu bất thường. Ghi lại thời điểm dừng và nội dung hiển thị, rồi báo cáo người có trách nhiệm. Không khởi động lại khi chưa được phép.";
            case IT -> "Bản ghi sự cố: lỗi hiển thị sau khi đăng nhập. Ghi các bước tái hiện và thời điểm xảy ra, rồi báo người phụ trách. Không đưa thông tin cá nhân vào báo cáo.";
        };
    }
    public record Dialogue(String role,String japanese,String vietnamese) {}
    public List<Dialogue> dialogue(Major major) {
        var t=catalog.topic(major);
        return List.of(
            new Dialogue("Bạn", "お忙しいところ失礼します。"+t.incident(),"Xin phép làm phiền anh/chị. Tôi xin báo hiện tượng ghi trong tình huống."),
            new Dialogue("Trưởng nhóm","いつ、どのような状況で発生しましたか。","Hiện tượng xảy ra khi nào, trong hoàn cảnh nào?"),
            new Dialogue("Bạn","本日10時ごろ確認しました。原因はまだ確認できていません。","Tôi ghi nhận khoảng 10 giờ hôm nay. Hiện chưa xác nhận được nguyên nhân."),
            new Dialogue("Trưởng nhóm","確認した内容を記録して、共有してください。","Hãy ghi lại và chia sẻ những nội dung đã xác nhận."),
            new Dialogue("Bạn",t.resolution()+"対応方法を確認していただけますか。","Tôi sẽ thực hiện hướng báo cáo trong tình huống. Anh/chị có thể xác nhận cách xử lý giúp tôi không?"),
            new Dialogue("Trưởng nhóm","確認します。分かったことと未確認のことを分けて報告してください。","Tôi sẽ xác nhận. Khi báo cáo, hãy tách điều đã biết và điều chưa xác nhận."));
    }
    public String outline(PracticeKind kind) {
        return switch(kind) {
            case SITUATION -> "【状況の報告】\n私：…\n上司：…\n【情報の確認】\n私：…\n上司：…\n【対応の相談】\n私：…\n上司：…\n【次の行動】\n私：…";
            case EMAIL -> "件名：【報告】…について\n\n…様\nお疲れ様です。…です。\n\n【発生日時・現象】\n…\n【確認した事実・初期対応】\n…\n【未確認事項・確認のお願い】\n…\n【今後の連絡】\n…\n\nよろしくお願いいたします。\n署名：…";
            case REPORT -> "業務報告書\n報告日：…\n報告者：…\n対象：…\n\n1. 発生日時と現象\n…\n2. 確認した事実と根拠\n…\n3. 影響範囲\n…\n4. 実施した初期対応\n…\n5. 未確認事項・仮説\n…\n6. 提案・次の行動・担当・期限\n…";
            case ADVANCED -> "改善提案書\nテーマ：…\n\n1. 現状とデータ\n…\n2. 検証が必要な原因\n…\n3. 改善案と選定理由\n…\n4. 実施計画（担当・期限）\n…\n5. 検証方法（改善前・改善後の指標）\n…\n6. リスクと確認事項\n…";
        };
    }
    public List<String> checklist(PracticeKind kind) {
        return switch(kind) {
            case SITUATION -> List.of("Có ít nhất 4 lượt hội thoại và vai trò rõ ràng", "Nêu hiện tượng, thời điểm và điều chưa xác nhận", "Nhờ hỗ trợ lịch sự và thống nhất hành động tiếp theo");
            case EMAIL -> List.of("Có tiêu đề, người nhận, chào hỏi và chữ ký", "Nêu thời điểm, hiện tượng, dữ liệu và xử lý ban đầu", "Có yêu cầu xác nhận, lời kết và kế hoạch cập nhật");
            case REPORT -> List.of("Đủ ngày giờ, đối tượng, hiện tượng và bằng chứng", "Nêu phạm vi ảnh hưởng và xử lý đã thực hiện", "Tách sự thật với giả định; có đề xuất, người phụ trách và thời hạn");
            case ADVANCED -> List.of("Có hiện trạng và dữ liệu cơ sở", "Nguyên nhân cần kiểm chứng và hành động cải tiến đúng ngành", "Có chỉ số trước/sau, người phụ trách, thời hạn và rủi ro cần xác nhận");
        };
    }
    public String explanation(String term) {
        return Map.ofEntries(
            Map.entry("機械","動力を使って、決められた仕事をする装置。"),
            Map.entry("部品","機械や装置を作る、一つ一つの部分。"),
            Map.entry("電圧","電流を流すはたらきに関係する量。単位はボルト。"),
            Map.entry("電流","電気の流れ。単位はアンペア。"),
            Map.entry("センサー","温度や位置などの変化を検出する装置。"),
            Map.entry("制御","目的に合わせて、機械などの動きを調整すること。"),
            Map.entry("生産ライン","製品を作るために並べられた工程や設備。"),
            Map.entry("信号","情報や状態を伝えるためのもの。"),
            Map.entry("不具合","期待したとおりに動かない問題。"),
            Map.entry("実装","設計した機能を、実際に使える形にすること。"),
            Map.entry("点検","異常がないか、決められた項目を調べること。"),
            Map.entry("交換","使っているものを、別のものに取り替えること。"),
            Map.entry("仕様","製品や機能が満たすべき条件。"),
            Map.entry("測定","道具を使って、長さや電圧などの値を調べること。"),
            Map.entry("反応","ある変化や入力に対して起こる動き。"),
            Map.entry("動作","機械やシステムが動くこと。"),
            Map.entry("異常","いつもの正常な状態とは違うこと。"),
            Map.entry("停止","動いていたものが止まること。"),
            Map.entry("再現手順","同じ問題をもう一度起こすための操作の順番。"),
            Map.entry("回帰テスト","変更によって既存の機能が壊れていないか調べるテスト。")
        ).getOrDefault(term,"専門分野で使われる言葉。例文と一緒に確認しましょう。");
    }
}
