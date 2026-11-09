package vn.sanka.lms.learning;

import org.springframework.stereotype.Component;
import java.util.*;

/** Vocabulary stays with its lesson; reviewing a card never changes its meaning. */
@Component
public class FlashcardCatalog {
 public record Card(String word,String reading,String meaning,String example){}
 private final Map<Long,List<Card>> lessons=new HashMap<>();
 public FlashcardCatalog(){
  String data="""
1|あ|a|Chữ a|あお — màu xanh
1|い|i|Chữ i|いえ — ngôi nhà
1|う|u|Chữ u|うえ — phía trên
1|え|e|Chữ e|え — bức tranh
1|お|o|Chữ o|あお — màu xanh
1|あお|ao|Màu xanh|あおいそら — bầu trời xanh
2|カメラ|kamera|Máy ảnh|これはカメラです。
2|コンピューター|konpyūtā|Máy tính|コンピューターを使います。
2|テレビ|terebi|Ti vi|テレビを見ます。
2|ラジオ|rajio|Đài phát thanh|ラジオを聞きます。
3|私|わたし|Tôi|私はアンです。
3|学生|がくせい|Sinh viên, học sinh|私は学生です。
3|名前|なまえ|Tên|お名前は何ですか。
3|はじめまして|hajimemashite|Rất vui được gặp bạn lần đầu|はじめまして。アンです。
4|まず|mazu|Trước tiên|まず、電源を入れてください。
4|電源|でんげん|Nguồn điện|電源を入れてください。
4|次に|つぎに|Tiếp theo|次に、ボタンを押してください。
4|押す|おす|Nhấn, ấn|ボタンを押します。
5|機械|きかい|Máy móc|機械を点検します。
5|部品|ぶひん|Linh kiện|部品を交換します。
5|点検|てんけん|Kiểm tra thiết bị|作業の前に点検します。
5|作業|さぎょう|Công việc, thao tác|作業を始めます。
6|電圧|でんあつ|Điện áp|電圧を確認してください。
6|電流|でんりゅう|Dòng điện|電流を測定します。
6|測定|そくてい|Đo lường|測定結果を記録します。
6|仕様|しよう|Thông số kỹ thuật|仕様を確認します。
7|センサー|sensā|Cảm biến|センサーが反応しません。
7|モーター|mōtā|Động cơ|モーターが動きます。
7|反応|はんのう|Phản ứng, phản hồi|センサーが反応します。
7|動作|どうさ|Hoạt động, vận hành|動作を確認します。
8|生産ライン|せいさんライン|Dây chuyền sản xuất|生産ラインを確認します。
8|停止|ていし|Dừng|ラインを停止してください。
8|異常|いじょう|Bất thường|異常信号が出ました。
8|信号|しんごう|Tín hiệu|信号を確認します。
9|不具合|ふぐあい|Lỗi, sự cố|ログイン画面に不具合があります。
9|データベース|dētabēsu|Cơ sở dữ liệu|データベースを確認します。
9|再現手順|さいげんてじゅん|Các bước tái hiện|再現手順を報告します。
9|回帰テスト|かいきテスト|Kiểm thử hồi quy|修正後に回帰テストを実施します。
10|学校|がっこう|Trường học|学校に行きます。
10|図書館|としょかん|Thư viện|図書館で勉強します。
10|本|ほん|Sách|本を読みます。
10|読む|よむ|Đọc|日本語の本を読みます。
11|毎日|まいにち|Hằng ngày|毎日、勉強します。
11|昨日|きのう|Hôm qua|昨日、勉強しました。
11|明日|あした|Ngày mai|明日、学校に行きます。
11|働く|はたらく|Làm việc|9時から5時まで働きます。
12|これ|kore|Cái này|これは私の本です。
12|それ|sore|Cái đó|それは何ですか。
12|何|なに／なん|Cái gì|これは何ですか。
12|教科書|きょうかしょ|Giáo trình, sách giáo khoa|これは日本語の教科書です。
13|故障|こしょう|Hỏng hóc|機械が故障しました。
13|修理|しゅうり|Sửa chữa|修理を依頼しました。
13|担当者|たんとうしゃ|Người phụ trách|担当者に連絡してください。
13|原因|げんいん|Nguyên nhân|原因を確認します。
14|確認|かくにん|Xác nhận, kiểm tra|確認してから、開始します。
14|開始|かいし|Bắt đầu|作業を開始します。
14|前|まえ|Trước|作業の前に確認してください。
14|入る|はいる|Vào|入ってはいけません。
15|会議|かいぎ|Cuộc họp|明日の会議は午後2時からです。
15|資料|しりょう|Tài liệu|資料を読んでください。
15|参加|さんか|Tham gia|会議に参加します。
15|午後|ごご|Buổi chiều, giờ sau trưa|午後2時からです。
""";
  data.lines().filter(line->!line.isBlank()).forEach(line->{
   var parts=line.split("\\|",-1);
   lessons.computeIfAbsent(Long.valueOf(parts[0]),key->new ArrayList<>())
    .add(new Card(parts[1],parts[2],parts[3],parts[4]));
  });
 }
 public List<Card> cards(Lesson lesson){
  // Preserve content for any additional lesson until its vocabulary is authored.
  return List.copyOf(lessons.getOrDefault(lesson.getId(),List.of(new Card(lesson.getTitle(),"",lesson.getContent(),""))));
 }
}
