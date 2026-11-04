# Nhật Ký Đổ Xăng Android (`Mazda2FuelApp`)

Ứng dụng Android thuần (**Kotlin + Jetpack Compose + Room SQLite + WorkManager**) chuyên dụng để ghi nhật ký đổ xăng nhanh 1-chạm, tự động cập nhật bảng giá bán lẻ xăng dầu Petrolimex (**Vùng 1 & Vùng 2**) và tính mức tiêu hao nhiên liệu thực tế (`L/100km` & `đ/km`) theo ODO.

Được thiết kế đồng bộ hoàn toàn với dữ liệu xe **Mazda 2 Sedan 1.5L AT (`66A - 344.06`)** và tương thích 2 chiều với file [`mazda2_car_data.json`](../mazda2_car_data.json) của ứng dụng `Mazda2Care` trên macOS.

---

## Tính năng nổi bật

1. **Tự động cập nhật giá xăng Petrolimex (Vùng 1 & Vùng 2):**
   - Tự động tải bảng giá bán lẻ mới nhất (Xăng RON 95-III, RON 95-V, E5 RON 92, Dầu DO) ngay khi mở app và định kỳ mỗi 6 giờ qua `WorkManager` (`FuelPriceSyncWorker`).
   - Khi phát hiện kỳ điều chỉnh giá mới (thường vào 15h00 chiều thứ Năm hàng tuần), ứng dụng tự động lưu thêm một kỳ giá (`FuelPricePeriodEntity`) vào cơ sở dữ liệu và gửi thông báo trên điện thoại.
   - Hỗ trợ chuyển đổi nhanh giữa **Vùng 1** và **Vùng 2 (Đồng Tháp & các tỉnh xa cảng)**.

2. **Ghi nhận đổ xăng siêu nhanh 1-chạm (`SmartAddFuelBottomSheet`):**
   - Chọn nhanh mệnh giá: `300k`, `500k`, `700k`, `1.000k`, hoặc `Đầy bình (~38L)`.
   - Tự động tính ra **Số lít xăng nhận được**, **% dung tích bình (38L)** và **Quãng đường dự kiến đi được**.
   - Tự động khớp đơn giá xăng theo ngày đổ (`Hôm nay`, `Hôm qua`, `2 ngày trước`) dựa vào lịch sử các kỳ điều hành.
   - Chọn nhanh cây xăng quen thuộc (`CHXD 101 Cao Lãnh`, `CHXD 102 Cao Lãnh`, `CHXD 111 Cao Lãnh`).

3. **Tính mức tiêu hao nhiên liệu theo ODO (`L/100km` & `đ/km`):**
   - Tự động tính quãng đường chênh lệch (`+trip km`) giữa các lần đổ và hiệu suất tiêu thụ (`L/100km`) cho từng chặng lẫn trung bình toàn thời gian.

4. **Lưu trữ Offline (Room SQLite) & Sao lưu JSON tương thích máy Mac:**
   - Dữ liệu được khởi tạo sẵn (seed) từ [`app/src/main/assets/mazda2_car_data.json`](app/src/main/assets/mazda2_car_data.json) gồm thông tin xe Mazda 2 và lịch sử các lần đổ xăng hiện có.
   - Hỗ trợ **Xuất / Nhập file `mazda2_car_data.json`** để đồng bộ qua lại giữa điện thoại Android và ứng dụng `Mazda2Care` trên máy Mac.

---

## Cách mở và cài đặt lên điện thoại Android

### Cách 1: Dùng Android Studio (Khuyên dùng)
1. Mở **Android Studio** -> Chọn **Open** -> Trỏ vào thư mục:
   `/Users/thangnguyenviet/Documents/New Car/Mazda2FuelApp`
2. Đợi Android Studio tải Gradle & SDK tự động (~1-2 phút).
3. Cắm cáp điện thoại Android (bật *Gỡ lỗi USB / USB Debugging*) hoặc bật máy ảo -> Nhấn nút **Run (▶)** để cài trực tiếp lên máy, hoặc chọn **Build > Build Bundle(s) / APK(s) > Build APK(s)** để lấy file `.apk`.

### Cách 2: Cài Android CLI ngay trên máy Mac
Nếu chưa cài Android Studio, bạn có thể cài `android` CLI cho Mac Apple Silicon / Intel và chạy lệnh:
```bash
curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh | bash
android run
```
