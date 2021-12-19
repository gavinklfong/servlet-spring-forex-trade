package space.gavinklfong.forex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import space.gavinklfong.forex.filters.IpAddressFilter;

import java.util.List;

@Configuration
public class FilterConfig {

    @Value("${app.allowed-ip-range:}")
    private List<String> allowedIpRanges;

    @Bean
    public FilterRegistrationBean ipAddressFilter() {
        FilterRegistrationBean filterReg = new FilterRegistrationBean();
        filterReg.setFilter(new IpAddressFilter(allowedIpRanges));
        filterReg.addUrlPatterns("/deals");
        return filterReg;
    }

    @Bean
    public CommonsRequestLoggingFilter requestLogFilter() {
        CommonsRequestLoggingFilter filter
                = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(5000);
        filter.setIncludeHeaders(true);
        filter.setBeforeMessagePrefix("[Request] : ");
        filter.setAfterMessagePrefix("[Response] : ");
        return filter;
    }
}
