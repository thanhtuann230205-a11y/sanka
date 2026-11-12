# Chatbot Sanka (Gemini)

Trợ lý nằm ở góc dưới các trang. Người học cần đăng nhập để gửi câu hỏi.

## Cấu hình trên Render

1. Vào service Sanka → Environment → Add Environment Variable.
2. Thêm `GEMINI_API_KEY` bằng API key lấy từ Google AI Studio.
3. Có thể thêm `GEMINI_MODEL` để chọn model hỗ trợ generateContent trong tài khoản của bạn. Mặc định: `gemini-2.5-flash-lite`.
4. Save, rebuild and deploy sau khi đẩy code chatbot lên GitHub.

Không đặt key trong HTML/JavaScript, không commit key. Không có key thì ứng dụng vẫn khởi động, chatbot hiển thị chưa sẵn sàng.
Không cần migration database mới.

## Hành vi

- Chatbot giải thích tiếng Nhật và hướng dẫn lộ trình Sanka; không có quyền thay đổi điểm hay tiến độ.
- Câu hỏi và tối đa 12 tin nhắn ngữ cảnh được gửi tới Gemini. Không gửi hồ sơ, email, mật khẩu hoặc bài nộp từ database.
- Tin nhắn chỉ giữ trong bộ nhớ của trang, không lưu database hoặc localStorage; tải lại trang sẽ bắt đầu hội thoại mới.
- Tối đa 1500 ký tự/câu hỏi và 8 yêu cầu/phút/phiên. Giới hạn này không thay thế quota của nhà cung cấp.
- API phía máy chủ chờ tối đa 30 giây; giao diện cho thử lại khi mất kết nối hoặc gặp giới hạn sử dụng.
- Câu trả lời AI có thể sai. Đây là tích hợp API với hướng dẫn hệ thống, không phải mô hình được tự huấn luyện trên dữ liệu Sanka.

Tài liệu API: https://ai.google.dev/api/generate-content
