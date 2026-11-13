# Đăng nhập / đăng ký với Google

Hai trang `/login` và `/register` có nút **Tiếp tục với Google**. Người dùng chọn tài khoản Google rồi được đưa về `/start` để tiếp tục lộ trình Sanka.

## Google Cloud

1. Mở https://console.cloud.google.com/ và chọn hoặc tạo project. Có thể dùng cùng project Gemini, nhưng phải tạo OAuth Client riêng, không dùng Gemini API key.
2. Trong Google Auth Platform, cấu hình tên ứng dụng, email hỗ trợ và Audience. Nếu ứng dụng ở chế độ Testing, thêm tài khoản thử nghiệm vào Test users.
3. Tạo OAuth Client loại **Web application**.
4. Trong **Authorized redirect URIs**, thêm chính xác:
   - Production: `https://sanka-k9cm.onrender.com/login/oauth2/code/google` (thay hostname bằng URL Render thực tế nếu đã đổi).
   - Local: `http://localhost:8080/login/oauth2/code/google`.
5. Sao chép Client ID và Client Secret.

## Render

Thêm hai biến trong service Sanka → Environment:

| Key | Value |
| --- | --- |
| `GOOGLE_CLIENT_ID` | OAuth Client ID, thường kết thúc bằng `.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | OAuth Client Secret |

Không dùng `GEMINI_API_KEY` thay cho hai biến này. Không đưa Client Secret vào HTML, JavaScript hoặc GitHub.
Đẩy code, lưu Environment và deploy lại. Migration V12 bổ sung `google_subject` và chỉ mục duy nhất.
Nếu chưa có credentials, ứng dụng vẫn chạy và đăng nhập email vẫn hoạt động; bấm Google sẽ báo chưa được cấu hình.
Sau khi đổi domain phải thêm redirect URI mới trong Google Cloud. Cấu hình proxy HTTPS hiện có của profile prod được dùng để xác định URL callback.

## Tài khoản và bảo mật

- Spring Security thực hiện OAuth/OIDC authorization-code flow, kiểm tra token và state. Ứng dụng chỉ xin `openid`, `profile`, `email`; không truy cập hộp thư Gmail.
- Chỉ chấp nhận email đã xác minh. Lần liên kết đầu hỗ trợ Gmail hoặc Google Workspace (`hd`); Google account dùng email bên thứ ba cần đăng ký bằng mã email.
- Google subject được lưu để nhận diện tài khoản ổn định. Subject khác không được ghi đè liên kết cũ.
- Email trùng tài khoản Sanka: liên kết sau khi Google xác thực quyền sở hữu, giữ mật khẩu, ngành, trình độ và quyền hiện có. Tài khoản mới có quyền STUDENT.
- Email chưa có tài khoản: tạo tài khoản mới, không đặt mật khẩu dễ đoán. Người dùng tiếp tục dùng Google để đăng nhập.
- Tài khoản Google đã xác minh không cần gửi OTP Mailjet khi đăng ký qua Google.
- Principal đăng nhập trả về email Sanka để tương thích các controller hiện có. Phiên đăng nhập tiếp tục lưu qua Spring Session JDBC.

## Kiểm tra

Sau deploy, thử đăng nhập với Google ở trình duyệt riêng. Nếu gặp `redirect_uri_mismatch`, kiểm tra URI callback; nếu `access_denied`, kiểm tra Audience/Test users và chính sách tài khoản Google.
Kiểm thử tự động dùng token và dữ liệu giả, không gọi Google hoặc tạo tài khoản Google thật.

Tài liệu: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
Google: https://developers.google.com/identity/openid-connect/openid-connect
