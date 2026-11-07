# Deploy Sanka lên Railway

Triển khai hai service trong cùng một project/environment: **MySQL** và **Sanka (Spring Boot + Thymeleaf)**. Không cần Netlify. Railway tự phát hiện `Dockerfile` ở thư mục gốc. Image có toàn bộ HTML/CSS/JS/audio, chạy Java 21 và profile `prod`.

## 1. Đưa mã nguồn lên GitHub

Tạo repository GitHub rồi publish project `D:\Sanka` vào repository đó. `Dockerfile`, `pom.xml` và thư mục `src` phải nằm ngay thư mục gốc repo. Có thể dùng mục Source Control → Publish to GitHub trong VS Code.

File cấu hình cục bộ `application-local.yml`, `.env`, `.tools`, `.m2` và `target` đã được bỏ qua. Không upload các file này bằng cách kéo thả thủ công. Container chỉ copy `pom.xml` và `src`, không chứa mật khẩu MySQL trên máy.

## 2. Tạo MySQL trên Railway

Đăng nhập https://railway.com → tạo project → thêm database MySQL. Giữ tên service là **MySQL** để các tham chiếu bên dưới khớp. Chờ database chạy thành công. Giữ volume của database để lưu dữ liệu xuyên lần deploy.

Trong Variables của MySQL, Railway cung cấp `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`. Dùng kết nối private giữa hai service; database không cần public domain/TCP proxy để ứng dụng kết nối.

## 3. Thêm service ứng dụng

Trong cùng project, thêm service từ GitHub repo vừa tạo. Chọn branch triển khai. Nếu Railway bắt đầu deploy trước khi nhập biến và báo lỗi kết nối database, nhập biến ở bước tiếp theo rồi redeploy.

Trong **Variables của service ứng dụng**, thêm:

```dotenv
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```

Đây là cú pháp Reference Variable của Railway; dán vào Railway, không chạy trong PowerShell. Nếu đổi tên MySQL service, sửa tên trước dấu chấm tương ứng. `DB_URL` phải bắt đầu bằng `jdbc:mysql://`, không dùng nguyên `mysql://...` từ `MYSQL_URL`.

Profile `prod` đã được đặt trong Dockerfile. Không cần nhập Build Command hoặc Start Command; container build bằng Maven và chạy JAR. Ứng dụng đọc `PORT` do Railway cấp. Không đặt `DB_URL=localhost` và không dùng mật khẩu local `123456`; các reference ở trên lấy thông tin từ database Railway.

Tùy chọn, tạo giảng viên ngay lần khởi động đầu bằng hai biến:

```dotenv
SANKA_INSTRUCTOR_EMAIL=teacher@example.com
SANKA_INSTRUCTOR_PASSWORD=<mật-khẩu-riêng-tối-thiểu-12-ký-tự>
```

Dùng email chưa đăng ký sinh viên. Sau khi tạo được tài khoản, có thể xóa hai biến rồi redeploy; tài khoản đã lưu trong MySQL.

## 4. Deploy và tạo domain

Áp dụng thay đổi và deploy/redeploy. Trong deployment logs, chờ `Started SankaApplication`. Flyway tự tạo bảng và nạp học liệu mẫu trên database mới; không cần chạy các file SQL bằng tay.

Mở **Settings → Networking → Public Networking → Generate Domain** của service ứng dụng. Truy cập domain HTTPS Railway cung cấp. Profile `prod` bật cookie Secure, xử lý header proxy và cache Thymeleaf; dùng HTTPS khi đăng nhập.

Nếu cấu hình Healthcheck Path, dùng `/` và có thể đặt timeout 180 giây cho lần khởi động đầu. Nếu muốn luôn chạy, tắt **Settings → Deploy → Serverless** cho ứng dụng và database, rồi redeploy khi Railway yêu cầu. Tài nguyên chạy liên tục có tính phí theo gói/usage của tài khoản.

## 5. Kiểm tra sau triển khai

- Mở trang chủ và đăng ký một tài khoản mới.
- Chọn ngành, làm kiểm tra đầu vào, mở kết quả và xem lại từng câu.
- Hoàn thành một bài và đăng nhập lại để kiểm tra dữ liệu được lưu.
- Thử phát audio và kiểm tra các trang trên điện thoại.

Database mới chỉ có học liệu mẫu và dữ liệu tạo sau deploy. Tài khoản, bài nộp và tiến độ trên MySQL ở máy cá nhân không tự chuyển lên. Nếu cần giữ dữ liệu cũ, thực hiện xuất/nhập database đầy đủ, gồm cả `flyway_schema_history`; không nhập chồng tùy ý lên database đã chạy migration.

## Khi deploy lỗi

| Triệu chứng | Kiểm tra |
| --- | --- |
| Build không thấy Dockerfile/pom.xml | Root Directory phải trỏ đúng thư mục chứa cả hai file. |
| `Access denied` hoặc `Communications link failure` | Hai service cùng project/environment, MySQL đang chạy, 3 biến DB dùng reference đúng tên service. |
| `No suitable driver` hoặc URL sai | `DB_URL` bắt đầu bằng `jdbc:mysql://`. |
| Container chạy nhưng domain lỗi 502 | Domain trỏ đúng cổng app đang nghe; app dùng `PORT` Railway cung cấp; xem startup logs và trạng thái database. |
| Không giữ được đăng nhập | Dùng domain HTTPS và profile `prod`. Bản hiện tại dùng session trong bộ nhớ: redeploy/restart có thể yêu cầu đăng nhập lại; chạy một replica. |
| Flyway báo checksum/schema conflict | Kiểm tra lịch sử nhập database/migration; giữ nguyên các migration đã chạy, không xóa bảng hay history để bỏ qua lỗi. |

Docker build chưa được kiểm tra bằng Docker trên máy hiện tại nếu Docker chưa được cài. Maven build/test có thể chạy bằng `./mvnw.cmd package`.

Tài liệu chính thức: [Dockerfile](https://docs.railway.com/builds/dockerfiles), [MySQL](https://docs.railway.com/databases/mysql), [Variables](https://docs.railway.com/variables), [Domain](https://docs.railway.com/networking/domains/working-with-domains).
