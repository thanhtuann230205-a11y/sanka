# Xác nhận email qua Gmail API

Đăng ký mới cần mã 6 số trước khi tạo tài khoản. Mã có hiệu lực 10 phút, tối đa 5 lần nhập. Gửi lại cách nhau 60 giây, tối đa 5 mã/địa chỉ/giờ. Mã và mật khẩu được băm trong bảng chờ. Giữ cookie của trình duyệt đã đăng ký để xác nhận.

Ứng dụng gửi qua HTTPS bằng Gmail API, không dùng SMTP hoặc Mailjet. Không cần tên miền riêng. Chỉ chủ hộp thư gửi cần cấp quyền Gmail; người đăng ký chỉ nhận OTP, không cần cấp quyền Gmail. Tài khoản cũ và đăng nhập Google giữ nguyên.

## 1. Chuẩn bị Google Cloud

1. Vào https://console.cloud.google.com/ và chọn hoặc tạo project.
2. Trong APIs & Services → Library, bật **Gmail API**.
3. Trong Google Auth Platform, cấu hình Branding và Audience (External nếu là Gmail cá nhân). Nếu đang Testing, thêm Gmail dùng gửi OTP vào Test users.
4. Trong Data Access, thêm đúng scope `https://www.googleapis.com/auth/gmail.send`. Không cần quyền đọc hộp thư.
5. Tạo OAuth client loại **Web application**. Có thể tạo client riêng cho gửi thư để không ảnh hưởng đăng nhập Google hiện có.
6. Thêm Authorized redirect URI chính xác: `https://developers.google.com/oauthplayground`.
7. Giữ Client ID và Client Secret của client này để dùng ở bước tiếp theo.

## 2. Lấy refresh token cho hộp thư gửi

1. Mở https://developers.google.com/oauthplayground/ .
2. Mở biểu tượng bánh răng, bật **Use your own OAuth credentials**. Điền Client ID và Client Secret vừa tạo. Chọn Access type **Offline** và yêu cầu consent nếu có tùy chọn.
3. Nhập scope `https://www.googleapis.com/auth/gmail.send`, chọn Authorize APIs.
4. Đăng nhập đúng Gmail sẽ gửi OTP và cấp quyền. Nếu Google báo ứng dụng chưa xác minh, chỉ tiếp tục khi đó là project của chính bạn và hộp thư được phép sử dụng ứng dụng.
5. Chọn Exchange authorization code for tokens. Sao chép **Refresh token**, không phải Access token. Nếu không có refresh token, thực hiện cấp quyền lại với offline access và consent.
6. Không gửi client secret, refresh token hoặc access token vào chat, log hoặc GitHub.

Refresh token thuộc đúng cặp OAuth client và tài khoản vừa cấp quyền. EMAIL_FROM phải là chính hộp thư đó (hoặc alias đã được Gmail cho phép); nên dùng địa chỉ Gmail chính, không dùng dạng `Tên <email>`.

**Lưu ý quan trọng:** OAuth app External ở trạng thái Testing có refresh token hết hạn sau 7 ngày với scope Gmail. Trước khi dùng ổn định, chuyển Audience sang In production và thực hiện lại việc cấp quyền/lấy refresh token. Việc publish không tự động hoàn thành xác minh OAuth; Google có thể yêu cầu xác minh tùy cách phân phối ứng dụng và phạm vi sử dụng. Token vẫn có thể bị thu hồi hoặc hết hạn vì lý do khác, khi đó cần cấp quyền lại.

## 3. Cấu hình Render

Thêm các biến sau trong service → Environment:

| Biến | Giá trị |
| --- | --- |
| `GMAIL_CLIENT_ID` | Client ID đã dùng trong OAuth Playground |
| `GMAIL_CLIENT_SECRET` | Client Secret của cùng client |
| `GMAIL_REFRESH_TOKEN` | Refresh token ở bước 2 |
| `EMAIL_FROM` | Gmail đã cấp quyền gửi thư |

Các biến này độc lập với `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` dành cho đăng nhập Google. Không thay thế biến đăng nhập bằng token gửi thư.

Deploy code mới lên Render cùng cấu hình trên (Save, rebuild, and deploy), chờ service Live, rồi đăng ký thử bằng hộp thư bạn sở hữu. Chỉ sửa Environment mà chưa deploy code Gmail API thì ứng dụng cũ vẫn dùng Mailjet. Các biến MAILJET_API_KEY và MAILJET_SECRET_KEY không còn được dùng và có thể xóa sau khi chuyển thành công.

Không cần migration mới; vẫn dùng bảng xác nhận V11 hiện có. Nếu gửi thất bại thì không tạo tài khoản, không bỏ qua xác minh. Gmail vẫn áp dụng giới hạn gửi và chống spam; phù hợp nhu cầu nhỏ, không bảo đảm thư luôn vào inbox.

## Kiểm tra lỗi

Log chỉ ghi giai đoạn và HTTP status, ví dụ `Gmail verification email failed at token: HTTP 400`.

- `token` HTTP 400/401: kiểm tra cặp client ID/secret và refresh token; token có thể hết hạn hoặc bị thu hồi.
- `send` HTTP 401/403: kiểm tra Gmail API đã bật, scope gmail.send, quyền gửi và trạng thái tài khoản. Cấp quyền lại nếu thiếu scope.
- HTTP 429: chạm giới hạn; chờ trước khi thử lại.
- HTTP 5xx hoặc lỗi kết nối: thử lại sau.
- API chấp nhận gửi không đồng nghĩa thư đã vào inbox: kiểm tra Sent của Gmail gửi và Spam của bên nhận.

Kiểm thử dùng HTTP giả lập, không gửi email thật hoặc dùng quota Gmail.

Tài liệu:
- https://developers.google.com/workspace/gmail/api/guides/sending
- https://developers.google.com/identity/protocols/oauth2/web-server#offline
- https://developers.google.com/identity/protocols/oauth2#expiration
