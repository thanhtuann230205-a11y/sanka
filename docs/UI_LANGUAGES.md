# Ngôn ngữ giao diện

Bộ chọn Nhật / Anh / Việt dùng chung tại `templates/fragments/language.html`.
Các trang trong không gian học lấy bộ chọn từ topbar; trang đăng nhập, đăng ký,
quiz, kết quả và trang lỗi cũng dùng fragment này. Cờ là SVG, không phụ thuộc
font emoji của máy người dùng.

`static/js/ui-translations.js` chứa các bộ ba `[tiếng Việt, English, 日本語]`.
`home-language.js` áp dụng bản dịch cho chữ giao diện và các thuộc tính trợ năng,
giữ nguyên các phần tử biểu mẫu, sự kiện và giá trị người dùng nhập. Tên file cũ
được giữ lại để không thay đổi đường dẫn asset đã dùng ở trang chủ.

Ngôn ngữ được lưu bằng `localStorage` với khóa `sanka.ui.language`, dùng khi chuyển
trang và đồng bộ giữa các tab cùng tên miền. Nếu trình duyệt chặn lưu trữ, vẫn
đổi được ngôn ngữ trên trang hiện tại. Đây là tùy chọn trên trình duyệt, không phải
thiết lập tài khoản được đồng bộ giữa thiết bị.

Nội dung học liệu, đề, đáp án, bài làm, tên người dùng và phản hồi gốc được giữ
nguyên bằng `data-i18n-ignore` hoặc các vùng nội dung được loại trừ. Không gọi
dịch vụ dịch bên ngoài. Giao diện do quiz cập nhật sau thao tác được theo dõi để
giữ ngôn ngữ đã chọn; số câu, giá trị radio và cách chấm không thay đổi.

Khi thêm chữ giao diện, thêm bản dịch đầy đủ vào từ điển. Khi thêm nội dung do
người dùng tạo hoặc học liệu, đặt `data-i18n-ignore` lên vùng chứa nội dung đó.
Các câu mới chưa có trong từ điển sẽ giữ nguyên văn bản gốc.
