# Xác nhận email khi đăng ký

Đăng ký mới phải xác nhận mã 6 số trước khi tài khoản được tạo. Tài khoản đã có vẫn đăng nhập như trước.
Mã có hiệu lực 10 phút, tối đa 5 lần nhập. Mỗi lần gửi cách nhau 60 giây, tối đa 5 mã/địa chỉ/giờ.
Mã và mật khẩu được băm trong bảng chờ; không trả mã trên giao diện hoặc ghi mã vào log.

## Thiết lập Mailjet trên Render

1. Tạo tài khoản tại https://app.mailjet.com/ .
2. Thêm địa chỉ người gửi trong phần Senders và xác minh địa chỉ đó theo hướng dẫn Mailjet. Hoàn tất yêu cầu xác thực domain nếu tài khoản yêu cầu.
3. Vào API Key Management và lấy cả API Key lẫn Secret Key cho cùng một tài khoản. Không dùng SMTP password.
4. Trong Render → service Sanka → Environment, thêm:

| Key | Value |
| --- | --- |
| `MAILJET_API_KEY` | API Key Mailjet của bạn |
| `MAILJET_SECRET_KEY` | Secret Key tương ứng của Mailjet |
| `EMAIL_FROM` | Địa chỉ người gửi đã xác minh trên Mailjet |

5. Đẩy code lên GitHub rồi Save, rebuild, and deploy.
6. Migration `V11__email_registration.sql` tạo bảng chờ. Nếu dùng quy trình migrate cloud thủ công, chạy script migration hiện có trước khi deploy.
7. Thử đăng ký bằng hộp thư bạn sở hữu; kiểm tra cả thư rác. Theo dõi email transactional trong dashboard Mailjet nếu chưa nhận được.

Ứng dụng gửi email qua API HTTPS vì Render Free chặn SMTP trên các cổng 25/465/587.
Nếu thiếu cấu hình hoặc gửi email thất bại, đăng ký báo lỗi và không tạo tài khoản; không có chế độ bỏ qua xác nhận.
Không gửi API key, Secret Key, mã OTP hoặc mật khẩu vào chat hay GitHub. Gemini API key không dùng để gửi email.

## Ghi chú

- Phải giữ cookie của trình duyệt đã bắt đầu đăng ký để xác nhận. Mã không thể dùng cho một phiên đăng ký khác.
- Gửi lại mã sẽ vô hiệu mã cũ. Đăng ký lại cùng email tạo phiên xác nhận mới, không thay đổi nội dung phiên cũ một cách âm thầm.
- Xác nhận chứng minh người dùng truy cập được hộp thư ở thời điểm đăng ký; không xác minh danh tính ngoài đời.
- Bảng chờ không thay đổi quyền đăng nhập của tài khoản cũ. Không xóa dữ liệu người học.
- Kiểm thử sử dụng mailer giả lập, không gửi email thật hoặc sử dụng quota của bạn.

Tài liệu Mailjet: https://dev.mailjet.com/docs/email-api/send-api-v31/send-basic-email
Render: https://render.com/docs/free

Code hiện dùng Mailjet Send API v3.1 qua HTTPS, xác thực bằng API Key và Secret Key. Biến `BREVO_API_KEY` cũ không còn được sử dụng và có thể xóa khỏi Render. Không cần thêm migration khi chuyển nhà cung cấp; vẫn dùng bảng xác nhận V11 hiện có.
