package com.gomore.experiment.logging;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TeeUtil {
  private static final Logger logger = LoggerFactory.getLogger(TeeUtil.class);

  public static boolean isFormUrlEncoded(HttpServletRequest request) {
    String contentTypeStr = request.getContentType();
    if ("POST".equalsIgnoreCase(request.getMethod()) && contentTypeStr != null
        && contentTypeStr.startsWith(AccessConstants.X_WWW_FORM_URLECODED)) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isImageResponse(HttpServletResponse response) {
    String responseType = response.getContentType();
    if (responseType != null && responseType.startsWith(AccessConstants.IMAGE_CONTENT_TYPE)) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isBinaryContent(final HttpServletRequest request) {
    if (request.getContentType() == null) {
      return false;
    }
    return request.getContentType().startsWith("image")
        || request.getContentType().startsWith("video")
        || request.getContentType().startsWith("audio");
  }

  public static boolean isMultipart(final HttpServletRequest request) {
    return request.getContentType() != null
        && request.getContentType().startsWith("multipart/form-data");
  }

  public static boolean isJsonContent(HttpServletResponse response) {
    return response.getContentType() != null
        && response.getContentType().startsWith("application/json");
  }

  public static String printRequest(final TeeHttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nID=").append(request.getId()).append("\n");

    sb.append(request.getMethod()).append(" ").append(request.getRequestURI());
    if (request.getQueryString() != null) {
      sb.append("?").append(request.getQueryString());
    }
    sb.append(" ").append(request.getProtocol()).append("\n");

    Enumeration<String> enHeader = request.getHeaderNames();
    while (enHeader.hasMoreElements()) {
      final String name = enHeader.nextElement();
      sb.append(name).append(": ").append(request.getHeader(name)).append("\n");
    }
    if (request.getSession() != null) {
      sb.append("SESSION ID=").append(request.getSession().getId()).append("\n");
    }

    // 如果被标识不记录request body，则退出
    if (request.getAttribute(AccessConstants.TEE_FILTER_NOT_LOG_REQUEST_BODY) != null) {
      logger.debug("该请求已被标识不显示request body.");
      return sb.toString();
    }

    if (!isMultipart(request) && !isBinaryContent(request)) {
      String charEncoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding()
          : "UTF-8";
      try {
        byte[] payload = (byte[]) request.getAttribute(AccessConstants.LB_INPUT_BUFFER);
        if (payload != null && payload.length > 0) {
          sb.append(new String(payload, charEncoding)).append("\n");
        }
      } catch (UnsupportedEncodingException e) {
        logger.warn("Failed to parse request payload", e);
      }
    }

    return sb.toString();
  }

  public static String printResponse(final TeeHttpServletRequest request,
      TeeHttpServletResponse response, boolean onlyLogJsonResponse) {
    StringBuffer sb = new StringBuffer();

    sb.append("请求耗时: ")
        .append(System.currentTimeMillis()
            - (Long) request.getAttribute(AccessConstants.TEE_FILTER_START_TIME_PARAM))
        .append("毫秒");
    sb.append("\nID=").append(request.getId()).append("\n");

    int status = response.getStatus() == 0 ? HttpStatus.SC_OK : response.getStatus();
    sb.append(request.getProtocol()).append(" ").append(status).append(" ")
        .append(HttpStatus.getStatusText(status)).append("\n");
    for (String name : response.getHeaderNames()) {
      sb.append(name).append(": ").append(response.getHeader(name)).append("\n");
    }
    try {
      if (request.getSession() != null) {
        sb.append("SESSION ID=").append(request.getSession().getId()).append("\n");
      }
    } catch (Exception e) {
    }

    // 如果被标识不记录response，则退出
    if (request.getAttribute(AccessConstants.TEE_FILTER_NOT_LOG_RESPONSE_BODY) != null) {
      logger.debug("该请求已被标识不显示response body.");
      return sb.toString();
    }

    if (onlyLogJsonResponse) {
      // 只记录JSON格式的返回内容
      if (!isJsonContent(response)) {
        return sb.toString();
      }
    }

    String charEncoding = response.getCharacterEncoding() != null ? response.getCharacterEncoding()
        : "UTF-8";
    byte[] payload = (byte[]) request.getAttribute(AccessConstants.LB_OUTPUT_BUFFER);
    if (payload != null && payload.length > 0) {
      try {
        sb.append(new String(payload, charEncoding)).append("\n");
      } catch (UnsupportedEncodingException e) {
        logger.warn("Failed to parse response payload", e);
      }
    }
    return sb.toString();
  }

}
