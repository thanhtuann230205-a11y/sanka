INSERT INTO lessons(id,title,content,level,major,minutes) VALUES
 (1,'Bắt đầu với Hiragana','あ・い・う・え・お
Đọc lần lượt: a, i, u, e, o.
Luyện viết mỗi chữ 5 lần, sau đó đọc từ あお (màu xanh).','ZERO',NULL,10),
 (2,'Katakana trong công việc','カメラ: máy ảnh. コンピューター: máy tính.
Katakana thường dùng để viết từ mượn. Hãy viết tên 3 thiết bị bạn sử dụng.','ZERO',NULL,15),
 (3,'Giới thiệu bản thân — 自己紹介','はじめまして。私はアンです。
Rất vui được gặp bạn. Tôi là An.
私は学生です。Tôi là sinh viên.
Hãy thay tên và nghề nghiệp để tự giới thiệu.','N5',NULL,15),
 (4,'N4 · Trình bày quy trình','まず、電源を入れてください。
Trước tiên, hãy bật nguồn.
次に、ボタンを押してください。
Tiếp theo, hãy nhấn nút.
Luyện tập: mô tả một quy trình bằng まず và 次に.','N4',NULL,20),
 (5,'Cơ khí · Kiểm tra máy móc','機械（きかい）: máy móc. 部品（ぶひん）: linh kiện.
作業の前に、機械を点検してください。
Hãy kiểm tra máy trước khi làm việc.
Thực hành ngôn ngữ: viết một câu đề nghị kiểm tra linh kiện.','N4','MECHANICAL',20),
 (6,'Điện · Đọc thông số','電圧（でんあつ）: điện áp. 電流（でんりゅう）: dòng điện.
電圧を確認してください。Hãy kiểm tra điện áp.
Thực hành ngôn ngữ: trình bày thông số của một thiết bị.','N4','ELECTRICAL',20),
 (7,'Cơ điện tử · Cảm biến','センサー: cảm biến. モーター: động cơ.
センサーが反応しません。Cảm biến không phản hồi.
Thực hành: báo cáo hiện tượng cho đồng nghiệp.','N4','MECHATRONICS',20),
 (8,'Tự động hóa · Dây chuyền','生産ライン（せいさんライン）: dây chuyền sản xuất.
ラインを停止してください。Hãy dừng dây chuyền.
Thực hành ngôn ngữ: viết thông báo trạng thái dây chuyền.','N4','AUTOMATION',20),
 (9,'CNTT · Báo cáo lỗi phần mềm','不具合（ふぐあい）: lỗi. データベース: cơ sở dữ liệu.
ログイン画面に不具合があります。Màn hình đăng nhập có lỗi.
Thực hành: mô tả bước tái hiện lỗi, kết quả mong đợi và thực tế.','N4','IT',20);
INSERT INTO terms(id,japanese,reading,vietnamese,example,major) VALUES
 (1,'機械','きかい','Máy móc','機械を点検します。','MECHANICAL'),
 (2,'部品','ぶひん','Linh kiện','部品を交換します。','MECHANICAL'),
 (3,'電圧','でんあつ','Điện áp','電圧を測ります。','ELECTRICAL'),
 (4,'電流','でんりゅう','Dòng điện','電流を確認します。','ELECTRICAL'),
 (5,'センサー','せんさー','Cảm biến','センサーが反応します。','MECHATRONICS'),
 (6,'制御','せいぎょ','Điều khiển','ロボットを制御します。','MECHATRONICS'),
 (7,'生産ライン','せいさんライン','Dây chuyền sản xuất','生産ラインを確認します。','AUTOMATION'),
 (8,'信号','しんごう','Tín hiệu','信号を送ります。','AUTOMATION'),
 (9,'不具合','ふぐあい','Lỗi phần mềm','不具合を報告します。','IT'),
 (10,'実装','じっそう','Triển khai chức năng','機能を実装します。','IT');
