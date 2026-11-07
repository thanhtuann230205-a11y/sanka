# Sanka LMS

Project Spring Boot 3.5, Java 21+, Thymeleaf, Spring Security, JPA, Flyway và MySQL, dựa trên blueprint Sanka. Giao diện tiếng Việt, hỗ trợ nội dung tiếng Nhật, responsive và không cần Node.js để chạy.

## Chạy ứng dụng

Máy hiện tại có `application-local.yml` ở thư mục gốc để kết nối MySQL bằng tài khoản cục bộ đã cung cấp. File này được Git bỏ qua. Khi MySQL đang chạy, chạy `./mvnw.cmd spring-boot:run` từ thư mục `D:\Sanka`. Kết nối cục bộ bật `createDatabaseIfNotExist=true` để tạo `sanka_lms` nếu chưa có; tài khoản MySQL cần quyền tạo database. Các biến `DB_URL`, `DB_USERNAME` và `DB_PASSWORD` (nếu có) sẽ ghi đè cấu hình cục bộ.

Yêu cầu: JDK 21 trở lên, Docker Compose (hoặc MySQL 8.4 có sẵn). Maven Wrapper tự tải Maven ở lần chạy đầu.

```powershell
Copy-Item .env.example .env
# Sửa hai mật khẩu trong .env trước khi khởi động MySQL
docker compose up -d
$env:DB_PASSWORD = 'mat-khau-DB_PASSWORD-ban-da-dat'
.\mvnw.cmd spring-boot:run
```

Mở http://localhost:8080, đăng ký tài khoản, đăng nhập, chọn ngành rồi làm kiểm tra đầu vào. Không có tài khoản hay mật khẩu mẫu được cài sẵn.

Nếu dùng MySQL có sẵn, tạo database `sanka_lms` với charset `utf8mb4` và một user có quyền trên database đó. Đặt `DB_USERNAME`, `DB_PASSWORD`, tùy chọn `DB_URL`. Flyway tạo bảng và nạp học liệu mẫu khi ứng dụng khởi động. File `.env` được Docker Compose đọc; Spring Boot nhận biến môi trường từ shell, không tự đọc `.env`.

```powershell
$env:DB_USERNAME = 'sanka'
$env:DB_PASSWORD = 'your-password'
$env:DB_URL = 'jdbc:mysql://localhost:3306/sanka_lms?serverTimezone=Asia/Ho_Chi_Minh'
.\mvnw.cmd spring-boot:run
```

## Phạm vi hiện có

- Đăng ký, đăng nhập, BCrypt, CSRF, kiểm tra dữ liệu phía server.
- Chọn một trong 5 ngành; Placement Test xác định Pre-N5/Zero, N5, N4, N3 hoặc nhóm N2–N1.
- Dashboard, lộ trình, bài học mẫu, lưu hoàn thành xuyên phiên, 20 XP/bài và tránh cộng lặp.
- Phân luồng Zero → học Zero/N5 → kiểm tra N5 → học N4 → kiểm tra N4 → chuyên ngành. Đầu vào đạt N5/N4 được vào nhánh phù hợp.
- Khóa/mở bài và kiểm tra ở server; không thể tự khai trình độ hoặc gửi điểm từ trình duyệt để mở khóa.
- Thuật ngữ chuyên ngành → đọc hiểu tài liệu → xử lý tình huống → viết hội thoại → email → báo cáo → đánh giá ứng dụng → nâng cao và đề xuất cải tiến.
- Trắc nghiệm chấm tự động; lưu điểm, lựa chọn, đáp án và giải thích cho từng lần thi. Ngưỡng đạt 80%; thi lại không làm mất kết quả đạt trước đó.
- Bài hội thoại/email/báo cáo/đề xuất được giảng viên chấm 4 tiêu chí, phản hồi và yêu cầu sửa; không tự động coi bài viết là đạt.
- Hồ sơ năng lực: lịch sử bài thi, nội dung đã nộp, điểm rubric, nhận xét và bước tiếp theo. Minh chứng chuyên ngành được xét riêng theo ngành.
- Vai trò giảng viên, hàng đợi chấm bài, phân quyền và kiểm tra dữ liệu phía server.
- Từ điển mẫu Nhật–Việt, tìm theo Kanji/cách đọc/nghĩa và lọc ngành.
- Khóa theo 5 nhóm trình độ: bài chủ đề, ngân hàng từ vựng, ngữ pháp, nghe có file WAV, đọc Nhật–Việt, lịch ôn 7 ngày và bài ứng dụng kỹ thuật có giảng viên chấm.
- Nhánh N3 và N2–N1 có thể học song song với chuyên ngành sau N4; tiến cấp qua kiểm tra N3 và nhóm N2–N1.
- Gamification: chuỗi ngày học, XP theo hoạt động, 10 huy hiệu, mục tiêu XP ngày, lịch hoạt động và bảng xếp hạng tự nguyện theo tuần/toàn thời gian, lọc ngành/trình độ.
- Flyway migration và Docker Compose cho MySQL.

Đã triển khai các nhóm chức năng trong sơ đồ người học và mô hình hai luồng trình độ/chuyên ngành. Ngân hàng câu hỏi và học liệu là mẫu, không phải giáo trình JLPT đầy đủ hay chứng chỉ. Cần giảng viên thẩm định độ khó, ngưỡng phân luồng, rubric và mở rộng học liệu trước khi đánh giá chính thức. N2–N1 là một nhóm học chung theo ảnh thiết kế, không đánh giá riêng N1. Chưa có nhận dạng giọng nói, upload tài liệu, trình biên tập học liệu hoặc khôi phục mật khẩu; những mục này nằm ngoài phạm vi đang triển khai. Giao tiếp hiện là đóng vai viết có phản hồi giảng viên.

### Quy tắc đầu vào và tiến cấp

Placement Test có 20 câu: 4 nhóm N5, N4, N3, N2–N1, mỗi nhóm 5 phần chữ viết, ngữ pháp, từ vựng, nghe, đọc. Cần đạt ít nhất 4/5 ở từng nhóm liên tiếp để được xếp cấp tương ứng; trượt N5 vào Pre-N5/Zero. Đúng nhóm cao nhưng trượt nhóm thấp không được nhảy cấp. Đầu vào làm một lần; sau đó tiến cấp bằng kiểm tra N5/N4/N3/N2–N1 sau khi hoàn thành bài tương ứng. Kết quả đạt đã lưu không bị hạ khi thi lại. Tài khoản đã kiểm tra theo ngân hàng cũ giữ nguyên kết quả, không bị đặt lại. Các tài khoản cũ chỉ tự khai trình độ trước V3 cần làm đầu vào; tiến độ bài học vẫn được giữ.

Sau đăng nhập, `/start` dẫn sinh viên chưa chọn ngành đến onboarding, chưa kiểm tra đến Placement Test, đã kiểm tra đến dashboard. Dashboard và danh sách bài học chỉ ưu tiên chặng cần học: Zero học nền tảng N5 (gồm bảng chữ cái), đạt N5 học N4, đạt N4 học chuyên ngành. Học xong chặng hiện tại sẽ có nút kiểm tra đạt cấp hoặc chuyển học phần chuyên ngành. Bài nền tảng đã mở vẫn có thể truy cập lại qua đường dẫn để ôn tập. Giảng viên đăng nhập được chuyển thẳng đến hàng đợi chấm bài.

### Trang quiz kiểm tra đầu vào

`/exams/PLACEMENT` dùng trang riêng `templates/placement-quiz.html`, CSS/JavaScript riêng, không có sidebar của không gian học. Trang có màn hình bắt đầu, mỗi lần một câu, thẻ lựa chọn, tiến độ đã trả lời, bản đồ câu hỏi, đánh dấu xem lại, nút trước/sau, phần nghe và màn hình rà soát trước khi nộp. Có thể bỏ qua tạm thời rồi quay lại; hệ thống yêu cầu đủ đáp án khi nộp.

Lựa chọn và câu đang làm được lưu bằng `sessionStorage` trong tab hiện tại, tách theo tài khoản/chuyên ngành và phiên bản nội dung. Không phải bản nháp đồng bộ giữa thiết bị. Bản lưu được xóa khi server trả trang kết quả thành công. Khi không có JavaScript, trang hiện toàn bộ câu hỏi và vẫn gửi biểu mẫu bình thường. Server giữ nguyên kiểm tra CSRF, cách chấm/phân luồng và điều kiện làm đầu vào một lần; lỗi thiếu đáp án hiển thị lại trang quiz và giữ các lựa chọn hợp lệ.

Sau khi nộp, `/results/{id}` hiển thị trình độ được xếp rõ ràng (Pre-N5/Zero, N5, N4, N3 hoặc N2–N1), hướng học tiếp, số câu đúng và điểm. Người học chọn **Học tiếp** để về lộ trình hoặc **Xem lại kết quả** để mở `/results/{id}/review`. Màn hình xem lại hiện từng câu, đầy đủ lựa chọn ban đầu, đáp án đã chọn, đáp án đúng/sai, giải thích và audio nếu có; có nút trước/sau và bản đồ câu tô màu theo kết quả. Khi tắt JavaScript, toàn bộ các câu vẫn hiển thị được.

Migration V8 lưu trình độ được xếp theo từng lần thi và tạo bảng snapshot câu trả lời. Các câu hỏi, lựa chọn, chỉ số đáp án, giải thích và mã audio được lưu khi chấm, không lấy lại đáp án từ đề hiện tại để chấm lịch sử. Lên cấp sau đó không làm thay đổi trình độ trong kết quả đầu vào cũ. Với bài trước V8, hệ thống đọc câu hỏi/lựa chọn/đáp án/giải thích từ minh chứng văn bản đã lưu; không tự tạo các lựa chọn khác vốn chưa được lưu. Trang kết quả và xem lại chỉ dành cho chủ bài, không cộng thêm XP khi mở lại.

### Tiếng Nhật theo cấp, nghe và kiểm tra định kỳ

Mở `/language` hoặc menu **Tiếng Nhật theo cấp**. Từng khóa Pre-N5, N5, N4, N3, N2–N1 có bài chủ đề, bảng từ vựng Nhật–Việt/cách đọc/ví dụ, ngữ pháp, bài nghe, đọc hiểu và bài ứng dụng theo ngành đang chọn. Có thể ôn cấp cũ và học cấp kế tiếp; N3 mở từ N4, N2–N1 mở từ N3. Chuyên ngành vẫn mở từ N4.

Chọn **Bắt đầu khóa và bật lịch ôn** để lưu ngày bắt đầu và lịch ôn sau 7 ngày, theo múi giờ Việt Nam. Mỗi lần ôn lưu điểm, lựa chọn, đáp án, giải thích theo kỹ năng; cập nhật hạn kế tiếp sau 7 ngày. Có thể làm sớm. Lịch và trạng thái đến hạn hiển thị trong khóa và `/language/history`, không gửi email hay thông báo ngoài ứng dụng. Điểm định kỳ không nâng/hạ trình độ; thi đạt cấp là luồng riêng.

Bài ứng dụng kỹ thuật có độ dài tối thiểu theo cấp (20/40/80/100/150 ký tự), giới hạn 10.000 ký tự. Giảng viên chấm tại `/instructor/language-reviews` bằng 4 tiêu chí như rubric thực hành. Lịch sử, điểm và phản hồi tách theo người học/cấp/ngành; bài cần sửa được điền lại để chỉnh sửa. Mỗi cấp/ngành chỉ có một bài đang chờ chấm.

Các file `src/main/resources/static/audio/n0.wav` đến `n5.wav` (gồm n0, n2, n3, n4, n5) đã được tạo bằng giọng Windows tiếng Nhật Haruka; đây là giọng tổng hợp. File WAV được đóng gói trong JAR, phát trên trình duyệt bằng HTML audio, không cần TTS hay mạng khi chạy. Có thể tạo lại trên Windows đã có giọng ja-JP bằng `./scripts/generate-audio.ps1`; lúc tạo lại cần cài giọng tiếng Nhật, nhưng máy chạy ứng dụng chỉ cần các file đã đóng gói. Bản chép có trong trang luyện nghe; trang kiểm tra chỉ có âm thanh, đáp án và giải thích xuất hiện sau khi nộp.

Migration V6 tạo lịch học, lịch sử ôn, bài ứng dụng theo cấp và thêm bài N3/N2–N1. Không xóa kết quả, bài nộp hoặc tiến độ trước đó.

### Khu vực học chuyên ngành

Chọn **Học chuyên ngành** trong menu hoặc mở `/specialized`. Khu vực này có một luồng 6 bước, tiến độ riêng và nút tiếp tục bước chưa hoàn thành. Sinh viên dưới N4 thấy điều kiện cần đạt và liên kết quay lại nền tảng. Mỗi ngành có thuật ngữ, tài liệu và tình huống tương ứng.

| Học phần | Đường dẫn | Nội dung và điều kiện hoàn thành |
| --- | --- | --- |
| Từ vựng và thuật ngữ | `/specialized/TERMS` | Kanji, cách đọc, nghĩa Việt, giải thích Nhật, ví dụ, thẻ tự ôn, lưu/bỏ lưu và lọc từ đã lưu. Hoàn thành bài mở đầu rồi đạt kiểm tra thuật ngữ. |
| Đọc tài liệu | `/specialized/READING` | Biên bản kỹ thuật, phần nghĩa Việt có thể mở, câu hỏi định hướng và cấu trúc câu. Đạt kiểm tra đọc hiểu. |
| Giao tiếp, tình huống | `/specialized/SITUATION` | Bối cảnh, vai trò và hội thoại mẫu Nhật–Việt. Đạt kiểm tra xử lý tình huống, sau đó nộp hội thoại cho giảng viên và được chấm đạt. |
| Email và báo cáo | `/specialized/WRITING` | Mẫu bố cục, câu dùng trong công việc và checklist. Nộp email rồi báo cáo; cả hai cần giảng viên chấm đạt. |
| Đánh giá ứng dụng | `/specialized/APPLICATION` | Điểm theo 4 kỹ năng, minh chứng và khuyến nghị; bài kiểm tra kiến thức ứng dụng. Mở khi các bài thực hành trước đã đạt. |
| Nâng cao | `/specialized/ADVANCED` | Ngôn ngữ cải tiến, phương pháp đề xuất và đo trước/sau; kiểm tra nâng cao và đề xuất cải tiến được giảng viên chấm. |

Màn hình bài viết cho phép chèn bố cục vào bài trống, lưu nháp thủ công, tiếp tục ở lần đăng nhập sau và xem phản hồi lần trước. Bài cần sửa được điền lại nội dung để chỉnh sửa. Nộp thành công sẽ xóa bản nháp tương ứng; bài đang chờ chấm được hiển thị ở chế độ xem. Bản nháp tách theo tài khoản, chuyên ngành và loại bài. Từ đã lưu cũng tách theo tài khoản và được lọc theo ngành đang học.

Điểm tổng hợp kỹ năng là trung bình từ lần chấm gần nhất của từng loại bài trong ngành hiện tại, không thay thế điều kiện đạt từng bài. Các kết quả, điểm rubric và phản hồi chi tiết vẫn được giữ trong hồ sơ năng lực. Migration V5 tạo bảng bản nháp và từ đã lưu, không sửa nội dung bài đã nộp.

### Tạo tài khoản giảng viên

Dùng email riêng chưa đăng ký sinh viên. Đặt biến môi trường trước khi khởi động:

```powershell
$env:SANKA_INSTRUCTOR_EMAIL = 'teacher@example.com'
$env:SANKA_INSTRUCTOR_PASSWORD = 'your-unique-password-at-least-12-characters'
.\mvnw.cmd spring-boot:run
```

Đăng nhập bằng tài khoản này, chọn **Chấm bài thực hành** hoặc mở `/instructor/reviews`. Sau khi tài khoản được tạo, có thể bỏ hai biến môi trường. Bootstrap không đổi mật khẩu hay tự nâng quyền tài khoản sinh viên đã tồn tại.

Rubric: ngôn ngữ, chính xác kỹ thuật, giao tiếp, hoàn thành nhiệm vụ — mỗi tiêu chí 0–5; đạt khi tổng ít nhất 14/20 và mỗi tiêu chí từ 3. Bài chưa đạt có thể nộp lại sau phản hồi; chỉ một bài chờ chấm cho mỗi loại/ngành. Giảng viên không được tự chấm bài của mình. Email chỉ mở sau hội thoại đạt, báo cáo chỉ mở sau email đạt. Đánh giá ứng dụng mở sau cả ba bài đạt; nâng cao mở sau kiến thức ứng dụng đạt.

## Động lực học tập và bảng xếp hạng

Mở **Động lực & lịch học** (`/motivation`) để xem streak hiện tại/dài nhất, tổng XP, mục tiêu ngày, lịch hoạt động theo tháng và huy hiệu. Lịch có thể chuyển tháng và chọn ngày để xem các mốc hoạt động. Mọi ngày/tuần được tính theo `Asia/Ho_Chi_Minh`.

| Hoạt động | XP | Giới hạn |
| --- | --- | --- |
| Hoàn thành bài học | 20 | Một lần mỗi bài |
| Hoàn thành đầu vào | 20 | Một lần |
| Đạt kiểm tra | 40 | Một lần mỗi cấp; với chuyên ngành là mỗi học phần/ngành |
| Thực hành chuyên ngành được chấm đạt | 50 | Một lần mỗi loại bài/ngành |
| Ứng dụng theo cấp được chấm đạt | 40 | Một lần mỗi cấp/ngành |
| Ôn định kỳ | 15 | Một lần mỗi cấp/tuần, bắt đầu thứ Hai |
| Nộp bài hợp lệ hoặc kiểm tra chưa đạt | 0 | Ghi nhận ngày học tối đa một lần/ngày cho cùng ngữ cảnh |

Sổ XP có khóa duy nhất theo tài khoản/nội dung và được ghi trong cùng transaction với hoạt động học. Tải lại, nhấn hoàn thành nhiều lần, thi lại bài đã đạt hoặc chấm đạt lại cùng loại không cộng lặp. Lượt ôn định kỳ trùng cấp trong tuần không tạo thêm XP/ngày học; có thể tiếp tục ôn mà không được thưởng thêm. Đăng nhập, xem trang, lưu nháp, lưu từ không tạo XP hay streak. Không có API nhận số XP từ người dùng.

Streak đếm ngày liên tiếp có hoạt động hợp lệ; nếu hôm nay chưa học, chuỗi kết thúc hôm qua được giữ đến hết hôm nay. Bài nộp được ghi nhận ngày học ngay, XP chấm đạt xuất hiện sau khi giảng viên chấm nhưng thuộc ngày nộp, tránh tạo streak vào ngày chỉ có giảng viên chấm. Chuỗi dài nhất và huy hiệu đã nhận được giữ khi nghỉ học.

Mục tiêu mặc định 40 XP, có thể đặt 20–200 XP. Cập nhật áp dụng hôm nay và mặc định cho ngày tới; mục tiêu của ngày cũ được lưu riêng. Huy hiệu tự cấp một lần theo cột mốc: bài đầu, 5 bài, streak 3/7 ngày, 100/500 XP, kiểm tra đầu tiên đạt, thực hành đầu tiên đạt, lần ôn đầu và mục tiêu ngày đầu. Huy hiệu không thưởng thêm XP và không phải chứng nhận năng lực.

`/leaderboard` chỉ hiển thị sinh viên chủ động bật tham gia bằng checkbox và bí danh. Mặc định tắt; bỏ chọn và lưu sẽ rút ngay khỏi bảng mà không mất tiến độ/XP. Chỉ hiển thị bí danh, ngành, trình độ và XP; không hiển thị email hay tên tài khoản của người khác. Có bảng tuần/toàn thời gian, bộ lọc ngành và trình độ, tối đa 50 người; cùng XP được cùng hạng. Giảng viên không tham gia xếp hạng.

Migration V7 giữ lại 20 XP cho từng bài đã hoàn thành vào ngày hoàn thành gốc, tạo mục tiêu mặc định 40 XP cho các ngày đó. Startup cấp các huy hiệu lịch sử phù hợp. Các loại thưởng ngoài bài học bắt đầu từ phiên bản này, không truy thưởng các bài thi/thực hành cũ. Tài khoản cũ vẫn mặc định không tham gia bảng xếp hạng.

## Kiểm thử và đóng gói

Trước khi chạy `clean package`, dừng phiên ứng dụng đang chạy bằng nút Stop của IDE hoặc Ctrl+C trong terminal chạy ứng dụng. Trên Windows, `java -jar target/...jar` giữ file JAR và khiến Maven không xóa được `target`. Trong VS Code có thể dùng cấu hình `Run Sanka LMS` ở Run and Debug; cấu hình này chạy main class với thư mục làm việc đúng gốc project.

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/sanka-lms-0.0.1-SNAPSHOT.jar
```

Test dùng H2 ở chế độ MySQL, không cần database bên ngoài. Kiểm thử bao phủ các trang Thymeleaf, đăng ký/đăng nhập, CSRF, đủ 5 nhóm đầu vào, tiến cấp Zero/N5/N4/N3/N2–N1, luồng đầy đủ của cả 5 ngành, file nghe WAV, lịch ôn, bài ứng dụng theo cấp, quyền giảng viên, nộp bài trùng, giới hạn ngành/trình độ và tách dữ liệu người học. Gamification được kiểm tra với clock điều khiển: qua nửa đêm Việt Nam, streak và nghỉ ngày, mục tiêu lịch sử, nhận huy hiệu một lần, thưởng XP đồng thời, tuần mới, quyền riêng tư và rút khỏi leaderboard, cùng các điểm tích hợp vào luồng học thực.

## Cấu trúc

```text
src/main/java/vn/sanka/lms/
  auth/          # tài khoản và bảo mật
  learning/      # lộ trình, bài học, tiến độ, từ điển
  assessment/    # kiểm tra, phân luồng, thực hành, rubric và hồ sơ năng lực
  language/      # khóa theo cấp, ngân hàng từ/ngữ pháp, lịch ôn và ứng dụng theo cấp
  gamification/  # sổ XP, streak, mục tiêu ngày, huy hiệu, lịch và leaderboard
src/main/resources/
  templates/     # Thymeleaf và fragments dùng lại
  static/css/    # design tokens và giao diện responsive
  db/migration/  # schema và học liệu mẫu
src/test/        # kiểm thử luồng học
```

Trước khi public: cấu hình HTTPS/cookie Secure, bí mật môi trường, sao lưu MySQL và cơ chế giới hạn đăng nhập phù hợp môi trường triển khai.
