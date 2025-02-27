package org.opengroup.osdu.azure.filters;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.util.AuthUtils;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Slf4jMDCFilter}
 */
@ExtendWith(MockitoExtension.class)
public class Slf4jMDCFilterTest {

    public static final String OPERATION_NAME = "operation-name";
    public static final String APP_ID = "app-id";
    public static final String USER_ID = "user-id";
    public static final String API_METHOD = "api-method";
    public static final String EXPECTED_METHOD = "POST";
    public static final String EXPECTED_CORRELATION_ID = "660a9999-b37a-4776-9f48-d8fb7f3e05ed";
    public static final String EXPECTED_PARTITION = "osdu-opendes";
    public static final String EXPECTED_APP_ID = "appId";
    public static final String EXPECTED_OPERATION_NAME = "{POST [/query]}";
    public static final String EXPECTED_USER_ID = "a@b.com";

    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private FilterChain filterChain;
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private AuthUtils authUtils;
    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Mock
    private RequestMappingInfo requestMappingInfo;
    @Mock
    private HandlerMethod handlerMethod;
    @Mock
    private HandlerExecutionChain handlerExecutionChain;
    @Mock
    private JWTClaimsSet jwtClaimsSet;
    @InjectMocks
    private Slf4jMDCFilter sut;

    @BeforeEach
    public void setup() throws Exception {
        when(servletRequest.getMethod()).thenReturn(EXPECTED_METHOD);
        when(dpsHeaders.getCorrelationId()).thenReturn(EXPECTED_CORRELATION_ID);
        when(dpsHeaders.getPartitionId()).thenReturn(EXPECTED_PARTITION);
        when(dpsHeaders.getAppId()).thenReturn(EXPECTED_APP_ID);
        when(dpsHeaders.getAuthorization()).thenReturn("Bearer secure");

        when(servletRequest.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).thenReturn("path");
        when(requestMappingInfo.toString()).thenReturn(EXPECTED_OPERATION_NAME);
        when(handlerMethod.toString()).thenReturn("handlerMethod");
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = new HashMap<>();
        handlerMethods.put(requestMappingInfo, handlerMethod);
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(handlerMethods);
        when(requestMappingHandlerMapping.getHandler(any())).thenReturn(handlerExecutionChain);
    }

    @Test
    public void should_populate_MDC_context_map() throws Exception {
        when(handlerExecutionChain.getHandler()).thenReturn("handlerMethod");
        when(jwtClaimsSet.getSubject()).thenReturn(EXPECTED_USER_ID);
        when(authUtils.getClaimsFromJwtToken(any())).thenReturn(jwtClaimsSet);

        ArgumentCaptor<Map<String, String>> argCaptor = ArgumentCaptor.forClass(Map.class);
        try (MockedStatic<MDC> mockStatic = mockStatic(MDC.class)) {
            this.sut.doFilter(servletRequest, servletResponse, filterChain);

            verify(servletRequest, times(1)).getMethod();
            mockStatic.verify(() -> MDC.setContextMap(argCaptor.capture()));
            Map<String, String> contextMap = argCaptor.getValue();
            assertEquals(6, contextMap.size());
            assertEquals(EXPECTED_OPERATION_NAME, contextMap.get(OPERATION_NAME));
            assertEquals(EXPECTED_APP_ID, contextMap.get(APP_ID));
            assertEquals(EXPECTED_CORRELATION_ID, contextMap.get(DpsHeaders.CORRELATION_ID));
            assertEquals(EXPECTED_USER_ID, contextMap.get(USER_ID));
            assertEquals(EXPECTED_METHOD, contextMap.get(API_METHOD));
            assertEquals(EXPECTED_PARTITION, contextMap.get(DpsHeaders.DATA_PARTITION_ID));
        }
    }

    @Test
    public void should_skip_operationName_if_handler_not_found() throws Exception {
        when(jwtClaimsSet.getSubject()).thenReturn(EXPECTED_USER_ID);
        when(authUtils.getClaimsFromJwtToken(any())).thenReturn(jwtClaimsSet);

        ArgumentCaptor<Map<String, String>> argCaptor = ArgumentCaptor.forClass(Map.class);
        try (MockedStatic<MDC> mockStatic = mockStatic(MDC.class)) {
            this.sut.doFilter(servletRequest, servletResponse, filterChain);

            verify(servletRequest, times(1)).getMethod();
            mockStatic.verify(() -> MDC.setContextMap(argCaptor.capture()));
            Map<String, String> contextMap = argCaptor.getValue();
            assertEquals(6, contextMap.size());
            assertEquals("", contextMap.get(OPERATION_NAME));
            assertEquals(EXPECTED_APP_ID, contextMap.get(APP_ID));
            assertEquals(EXPECTED_CORRELATION_ID, contextMap.get(DpsHeaders.CORRELATION_ID));
            assertEquals(EXPECTED_USER_ID, contextMap.get(USER_ID));
            assertEquals(EXPECTED_METHOD, contextMap.get(API_METHOD));
            assertEquals(EXPECTED_PARTITION, contextMap.get(DpsHeaders.DATA_PARTITION_ID));
        }
    }

    @Test
    public void should_skip_user_id_if_sub_claim_not_present() throws Exception {
        when(jwtClaimsSet.getSubject()).thenReturn(null);
        when(authUtils.getClaimsFromJwtToken(any())).thenReturn(jwtClaimsSet);

        ArgumentCaptor<Map<String, String>> argCaptor = ArgumentCaptor.forClass(Map.class);
        try (MockedStatic<MDC> mockStatic = mockStatic(MDC.class)) {
            this.sut.doFilter(servletRequest, servletResponse, filterChain);

            verify(servletRequest, times(1)).getMethod();
            mockStatic.verify(() -> MDC.setContextMap(argCaptor.capture()));
            Map<String, String> contextMap = argCaptor.getValue();
            assertEquals(5, contextMap.size());
            assertEquals("", contextMap.get(OPERATION_NAME));
            assertEquals(EXPECTED_APP_ID, contextMap.get(APP_ID));
            assertEquals(EXPECTED_CORRELATION_ID, contextMap.get(DpsHeaders.CORRELATION_ID));
            assertNull(contextMap.get(USER_ID));
            assertEquals(EXPECTED_METHOD, contextMap.get(API_METHOD));
            assertEquals(EXPECTED_PARTITION, contextMap.get(DpsHeaders.DATA_PARTITION_ID));
        }
    }
}
