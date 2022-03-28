import com.google.common.base.CharMatcher;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * IP工具类
 */
@Slf4j
public class IPUtil {

    private static final int IPV4_PART_COUNT = 4;
    private static final char IPV4_DELIMITER = '.';
    private static final CharMatcher IPV4_DELIMITER_MATCHER = CharMatcher.is(IPV4_DELIMITER);
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public static String getIPFromRequest(HttpServletRequest request) {
        if (request == null) {
            if (null == RequestContextHolder.getRequestAttributes()) {
                return null;
            }
            request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
                log.debug("ipList.split(\",\")[0]::{}", ipList.split(",")[0]);
                return ipList.split(",")[0];
            }
        }

        log.debug("request.getRemoteAddr()::{}", request.getRemoteAddr());
        return request.getRemoteAddr();
    }

    public static boolean isInternalIp(String ip) {
        try {
            byte[] addr = textToNumericFormatV4(ip);
            if (addr != null) {
                return internalIp(addr);
            }
            return false;
        } catch (Exception e) {
            log.error("error ip convert::{}", ip);
            return false;
        }
    }

    private static boolean internalIp(byte[] addr) {
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        //10.x.x.x/8
        final byte SECTION_1 = (byte) 0x0A;
        //172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        //192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                if (b1 == SECTION_6) {
                    return true;
                }
            default:
                return false;
        }
    }

    private static byte @Nullable [] textToNumericFormatV4(String ipString) {
        if (IPV4_DELIMITER_MATCHER.countIn(ipString) + 1 != IPV4_PART_COUNT) {
            return null; // Wrong number of parts
        }

        byte[] bytes = new byte[IPV4_PART_COUNT];
        int start = 0;
        // Iterate through the parts of the ip string.
        // Invariant: start is always the beginning of an octet.
        for (int i = 0; i < IPV4_PART_COUNT; i++) {
            int end = ipString.indexOf(IPV4_DELIMITER, start);
            if (end == -1) {
                end = ipString.length();
            }
            try {
                bytes[i] = parseOctet(ipString, start, end);
            } catch (NumberFormatException ex) {
                return null;
            }
            start = end + 1;
        }

        return bytes;
    }

    private static byte parseOctet(String ipString, int start, int end) {
        // Note: we already verified that this string contains only hex digits, but the string may still
        // contain non-decimal characters.
        int length = end - start;
        if (length <= 0 || length > 3) {
            throw new NumberFormatException();
        }
        // Disallow leading zeroes, because no clear standard exists on
        // whether these should be interpreted as decimal or octal.
        if (length > 1 && ipString.charAt(start) == '0') {
            throw new NumberFormatException();
        }
        int octet = 0;
        for (int i = start; i < end; i++) {
            octet *= 10;
            int digit = Character.digit(ipString.charAt(i), 10);
            if (digit < 0) {
                throw new NumberFormatException();
            }
            octet += digit;
        }
        if (octet > 255) {
            throw new NumberFormatException();
        }
        return (byte) octet;
    }
}
