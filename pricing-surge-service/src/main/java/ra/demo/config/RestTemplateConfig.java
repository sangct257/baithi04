package ra.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Cấu hình timeout kết nối và đọc dữ liệu (2 giây) tránh bị treo khi API bên ngoài bị nghẽn
        factory.setConnectTimeout(2000); // 2 seconds
        factory.setReadTimeout(2000); // 2 seconds
        // Cần gán factory vào RestTemplate vừa tạo
        return new RestTemplate(factory);
    }

}
