INSERT INTO lessons(id,title,content,level,major,minutes) VALUES
(10,'N5 · Trợ từ và địa điểm','学校に行きます。Tôi đi đến trường. に chỉ đích đến.
図書館で勉強します。Tôi học ở thư viện. で chỉ nơi diễn ra hành động.
本を読みます。Tôi đọc sách. を chỉ đối tượng hành động.
練習：viết 3 câu về nơi bạn học và việc bạn làm.','N5',NULL,20),
(11,'N5 · Thời gian và lịch học','毎日9時から5時まで働きます。Tôi làm mỗi ngày từ 9 giờ đến 5 giờ.
昨日、勉強しました。Hôm qua tôi đã học.
明日、学校に行きます。Ngày mai tôi đến trường.
練習：viết lịch học ngày mai và một việc bạn đã làm hôm qua.','N5',NULL,20),
(12,'N5 · Sở hữu và câu hỏi','これは私の本です。Đây là sách của tôi.
それは何ですか。Đó là cái gì?
これは日本語の教科書です。Đây là giáo trình tiếng Nhật.
練習：giới thiệu 3 đồ vật của bạn bằng これ và の.','N5',NULL,15),
(13,'N4 · Nguyên nhân và lời khuyên','故障したので、修理を依頼しました。Vì hỏng nên đã yêu cầu sửa.
担当者に連絡したほうがいいです。Nên liên hệ người phụ trách.
原因はまだ確認できていません。Chưa xác nhận được nguyên nhân.
練習：trình bày một sự cố và lời khuyên, tránh khẳng định nguyên nhân khi chưa có bằng chứng.','N4',NULL,25),
(14,'N4 · Quy định và trình tự','入ってはいけません。Không được vào.
確認してから、開始します。Kiểm tra rồi bắt đầu.
作業の前に、確認してください。Hãy xác nhận trước khi làm việc.
練習：viết 2 quy định và mô tả thứ tự 3 bước trong một hoạt động học tập.','N4',NULL,25),
(15,'N4 · Đọc thông báo công việc','明日の会議は午後2時からです。資料を読んでから、参加してください。
Cuộc họp ngày mai bắt đầu từ 2 giờ chiều. Hãy đọc tài liệu trước khi tham gia.
担当者に確認してください。Hãy xác nhận với người phụ trách.
練習：xác định thời gian, việc cần làm trước và người cần liên hệ.','N4',NULL,20);
INSERT INTO terms(id,japanese,reading,vietnamese,example,major) VALUES
(11,'点検','てんけん','Kiểm tra thiết bị','作業の前に点検します。','MECHANICAL'),
(12,'交換','こうかん','Thay thế','部品の交換が必要です。','MECHANICAL'),
(13,'仕様','しよう','Thông số kỹ thuật','仕様を確認します。','ELECTRICAL'),
(14,'測定','そくてい','Đo lường','測定結果を記録します。','ELECTRICAL'),
(15,'反応','はんのう','Phản hồi, phản ứng','センサーが反応しません。','MECHATRONICS'),
(16,'動作','どうさ','Hoạt động, vận hành','動作を確認します。','MECHATRONICS'),
(17,'異常','いじょう','Bất thường','異常信号が出ました。','AUTOMATION'),
(18,'停止','ていし','Dừng','停止時刻を記録します。','AUTOMATION'),
(19,'再現手順','さいげんてじゅん','Các bước tái hiện','再現手順を報告します。','IT'),
(20,'回帰テスト','かいきてすと','Kiểm thử hồi quy','修正後に回帰テストを実施します。','IT');
