package ra.demo.constains;

public enum TransactionType {
    // === CÁC ENUM CŨ BẠN ĐÃ CÓ ===
    COMMISSION_DEDUCTION, // Trừ hoa hồng (khi khách trả tiền mặt)
    TRIP_EARNING,         // Thu nhập chuyến đi (80% cho tài xế)
    TOP_UP,               // Nạp tiền vào ví
    WITHDRAW,             // Rút tiền từ ví

    // === CÁC ENUM CẦN BỔ SUNG ===
    HOLD_FOR_TRIP,        // Tạm giữ tiền ví khách hàng khi vừa đặt chuyến
    UNHOLD_TRIP,          // Hoàn/Giải phóng tiền giữ cho khách khi HỦY CHUYẾN
    TRIP_PAYMENT,         // Trừ tiền thanh toán chính thức từ khoản Hold khi HOÀN THÀNH
    PLATFORM_COMMISSION   // Cộng 20% tiền hoa hồng vào Ví Sàn/Hệ thống
}