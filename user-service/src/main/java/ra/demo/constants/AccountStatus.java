package ra.demo.constants;

public enum AccountStatus {
    ACTIVE,                         // Hoạt động bình thường
    CUSTOMER_LOCKED,                // Bị Admin/Hệ thống khóa
    DRIVER_LOCKED,                  // Bị Admin/Hệ thống khóa
    CUSTOMER_DELETED,               // Người dùng đã xóa tài khoản
    DRIVER_DELETED                  // Người dùng đã xóa tài khoản
}