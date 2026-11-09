# Phiên đăng nhập dùng chung

Spring Session JDBC lưu phiên trong TiDB/MySQL thay vì bộ nhớ của từng container.
Các instance phải dùng cùng database và cùng tên miền truy cập. Phiên hết hạn sau
8 giờ không hoạt động (đổi bằng biến `SESSION_TIMEOUT`, ví dụ `4h`). Cookie
`SANKA_SESSION` có HttpOnly, SameSite=Lax; profile prod tiếp tục bật Secure và
forwarded headers cho HTTPS sau proxy. Đóng trình duyệt có thể kết thúc cookie
phiên; đây không phải tính năng ghi nhớ đăng nhập vĩnh viễn.

Migration V10 tạo bảng `SANKA_SESSION` và `SANKA_SESSION_ATTRIBUTES`. Flyway quản
lý schema, Spring Session không tự tạo bảng. Không sửa hoặc xóa migration cũ.

Trước khi push/deploy, chạy từ D:\Sanka:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\migrate-cloud.ps1 -PasswordFromClipboard
```

Không thêm `-RecoverV3`. Dùng đúng database đang cấu hình trên Vercel. Chờ SUCCESS,
rồi push code mới. Không điền mật khẩu vào file hoặc commit. Các bảng mới tương
thích với bản ứng dụng cũ nên có thể tạo trước khi chuyển bản deploy.

Sau lần chuyển này, người dùng đăng nhập lại một lần vì cookie phiên cũ không
được chuyển đổi. Sau đó phiên được chia sẻ giữa các instance và còn hiệu lực
khi container khởi động lại, miễn chưa hết hạn hoặc đăng xuất. Đăng xuất xóa
phiên trong database nên cookie cũ không còn đăng nhập được trên instance khác.

Chỉ dùng một tên miền chính: cookie không được chia sẻ giữa hai tên miền
`*.vercel.app` khác nhau hoặc giữa domain riêng và domain Vercel. JDBC session
không xử lý lỗi 500 do quá thời gian khởi động hay database mất kết nối.

Kiểm thử HTTP dùng database H2 riêng: đăng nhập trên instance A, dùng cùng cookie
trên B, tắt A và B rồi khởi động C, kiểm tra giữ đăng nhập; kiểm tra CSRF và
cookie cũ bị từ chối sau đăng xuất. Không kết nối TiDB thật trong kiểm thử.
