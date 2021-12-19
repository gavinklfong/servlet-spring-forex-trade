package space.gavinklfong.forex.filters;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class IpAddressFilterTest {

    private static final List<String> IP_RANGES = asList("127.0.0.0/8", "192.168.0.0/16");

    private IpAddressFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IpAddressFilter(IP_RANGES);
    }

    @Test
    void givenRequestFromValidIpAddress_whenSubmitToFilter_thenReject() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (filterRequest, filterResponse) -> ((HttpServletResponse)filterResponse).setStatus(HttpStatus.OK.value());

        request.setRemoteAddr("192.168.1.1");
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void givenRequestFromLocalhost_whenSubmitToFilter_thenPassOnFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (filterRequest, filterResponse) -> ((HttpServletResponse)filterResponse).setStatus(HttpStatus.OK.value());

        request.setRemoteAddr("127.0.0.1");
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void givenRequestFromUnauthorizedIpAddress_whenSubmitToFilter_thenReject() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (filterRequest, filterResponse) -> ((HttpServletResponse)filterResponse).setStatus(HttpStatus.OK.value());

        request.setRemoteAddr("192.160.0.1");
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }
}
