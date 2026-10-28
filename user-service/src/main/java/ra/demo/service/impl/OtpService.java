package ra.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final StringRedisTemplate redisTemplate;
    private static final String OTP_PREFIX = "OTP_DRIVER:";
    private static final long OTP_EXPIRE_MINUTES = 2; // OTP hết hạn sau 3 phút

    public String generateAndSaveOtp(String phoneNumber) {
        // 1. Tạo ngẫu nhiên 6 chữ số OTP
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);

        // 2. Lưu vào Redis với Key = "OTP_DRIVER:0386713099", Value = OTP, TTL = 2 phút
        String key = OTP_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, otp, OTP_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // TODO: Ở môi trường thật, tích hợp Twilio, SMS Brandname hoặc Firebase tại đây để gửi OTP đến SĐT
        return otp;
    }

    public boolean validateOtp(String phoneNumber, String inputOtp) {
        String key = OTP_PREFIX + phoneNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(inputOtp)) {
            // Xác thực thành công -> Xóa OTP khỏi Redis ngay lập tức
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
