package ra.demo.service;

import ra.demo.dto.request.CreateTripRequest;
import ra.demo.dto.response.TripResponse;

import java.util.List;

public interface TripService {
    // Chức năng dành cho CUSTOMER
    // CUSTOMER_CREATE_TRIP (Khách hàng tạo chuyến)
    TripResponse createTrip(CreateTripRequest createTripRequest);

    // Khách xem chi tiết 1 chuyến đi của chính mình
    TripResponse getCustomerMyTrip(Long tripId);

    // Khách xem danh sách lịch sử các chuyến đã đặt của chính miình
    List<TripResponse> getCustomerTripHistory();

    // Khách hủy chuyến đi
    TripResponse cancelTripByCustomer(Long tripId, String cancelReason);

    // Chức năng dành cho DRIVER
    // Tài xế chấp nhận chuyến đặt xe
    TripResponse acceptTrip(Long tripId);

    // Tài xế cập nhật trạng thái chuyến (Arrived -> InProgress -> Completed)
    TripResponse updateTripStatusByDriver(Long tripId);

    // Tài xế huỷ chuyến
    TripResponse cancelTripByDriver(Long tripId, String cancelReason);

    // Tài xế xem danh sách lịch sử các chuyến đã nhận
    List<TripResponse> getDriverTripHistory();
}
