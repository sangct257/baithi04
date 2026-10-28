package ra.demo.service;

import ra.demo.dto.request.DriverProfileRegisterRequest;
import ra.demo.dto.response.DriverProfileResponse;

public interface DriverService {
    // Driver Profile Register OR Update
    DriverProfileResponse createOrUpdateProfile(Long userId, DriverProfileRegisterRequest driverProfileRegisterRequest);

    // Driver Profile Get
    DriverProfileResponse getProfile(Long userId);

    // Driver Change Status (ONLINE, OFFLINE)
    DriverProfileResponse changeStatus(Long userId);

    // Check Admin Approve For Driver Profile
    DriverProfileResponse approveDriverProfile(Long userId);
}
