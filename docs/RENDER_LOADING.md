# Trang chờ Sanka miễn phí

Trang `loading-site/` chạy độc lập với backend Java. Nó hiện “Loading Sanka…”, gọi `GET /api/readiness` rồi chuyển tới backend khi nhận đúng JSON của Sanka. Không nhầm trang chờ HTML HTTP 200 của Render với ứng dụng đã sẵn sàng. Sau 3 phút chưa kết nối được sẽ hiện nút thử lại và link trực tiếp.

## Triển khai

1. Kiểm tra địa chỉ backend trong `loading-site/config.js`. Hiện dùng `https://sanka-k9cm.onrender.com` theo tài liệu đăng nhập Google của dự án; sửa nếu service thực tế có URL khác. Không điền secret/token vào trang tĩnh.
2. Commit và push code. Chờ **backend** deploy bản mới có `/api/readiness` trước.
3. Trên Render chọn **New → Static Site**, kết nối cùng repository và branch với backend.
4. Đặt tên, ví dụ `sanka-loading`. Root Directory để trống; Build Command nhập `echo ready`; Publish Directory nhập `loading-site`.
5. Tạo Static Site. Trang này không cần biến môi trường và không cần Java/Maven.
6. Mở URL của Static Site, ví dụ `https://sanka-loading.onrender.com`, rồi dùng link này làm link truy cập/chia sẻ Sanka.

Không tạo Web Service cho trang chờ. Không thay cấu hình service backend hiện có. Không cần đổi callback đăng nhập Google: sau trang chờ, người dùng vẫn đăng nhập trực tiếp trên backend.

## Kiểm tra

- Mở `<backend>/api/readiness`: khi chạy phải trả JSON `{"application":"sanka","status":"ready"}` (thứ tự trường có thể khác), không yêu cầu đăng nhập.
- Khi backend đang chạy, trang chờ chuyển vào Sanka ngay sau khi probe thành công.
- Khi backend đang ngủ, trang tĩnh vẫn hiển thị và tự thử lại mỗi 4 giây sau mỗi lần gọi (mỗi lần gọi tối đa 12 giây).
- Khi mất mạng/backend không chạy, sau tối đa khoảng 3 phút hiển thị nút **Thử lại**.

Trang chờ không loại bỏ thời gian khởi động backend và không thay đổi màn hình mặc định nếu truy cập thẳng URL backend. Endpoint chỉ cung cấp tín hiệu ứng dụng đã phục vụ HTTP, không kiểm tra sức khỏe database hoặc Gmail.
