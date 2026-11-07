# Lộ trình từ S5 đến chuyên ngành

Sau đăng nhập, sinh viên chưa được kiểm tra đi thẳng tới `/exams/PLACEMENT`.
Bài đầu vào không yêu cầu chuyên ngành. Các nhóm hiển thị là S5, S4, S3,
S2–S1, tương đương nhóm N5, N4, N3, N2–N1; đây là phân loại nội bộ.

- Đạt S4 trở lên: chọn ngành tại `/onboarding`, sau đó học chuyên ngành.
- Zero hoặc S5: học cơ bản S5 (gồm phần nhập môn), hoàn thành các bài rồi
  kiểm tra S5. Qua S5 mới mở cơ bản S4. Hoàn thành và qua kiểm tra S4 mới
  được chọn chuyên ngành. Trượt bài kiểm tra có thể ôn và thi lại.

Không còn menu chọn các khóa tiếng Nhật theo cấp. `/language` chuyển về
lộ trình hiện tại; các thao tác học/đăng ký/ôn/nộp bài của module cũ bị chặn.
Các kỳ thi N3/N2 cũ bị chặn. Hồ sơ, lịch sử và XP cũ được giữ nguyên.
Mã enum trong database và URL kiểm tra S5/S4 vẫn là N5/N4 để tương thích dữ liệu.

V9 cho phép `exam_attempts.major` để trống cho bài đầu vào và bài cơ bản,
không thay đổi checksum của V1–V8. Trước khi deploy lên Vercel, nên chạy
migration từ máy để tránh giới hạn khởi động:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\migrate-cloud.ps1 -PasswordFromClipboard
```

Lần này **không dùng `-RecoverV3`**: database đã khôi phục V3 và đang ở V8.
Nhập URL và tài khoản TiDB theo lời nhắc. Khi hiện SUCCESS, push code lên
GitHub để Vercel deploy phiên bản mới. Không lưu thông tin kết nối vào repo.
