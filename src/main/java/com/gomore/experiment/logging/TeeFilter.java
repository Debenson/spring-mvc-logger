package com.gomore.experiment.logging;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Debenson
 * @since 0.1
 */
public class TeeFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(TeeFilter.class);

  private boolean active;
  private boolean onlyLogJsonResponse = true;
  private List<Pattern> escapeRequestBodyPatterns = new ArrayList<>();
  private List<Pattern> escapeResponseBodyPatterns = new ArrayList<>();
  private List<Pattern> excludeUrlPatterns = new ArrayList<>();

  @Override
  public void destroy() {
    // NOP
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    if (!isNeedLog(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    final long requestId = IdWorker.getId();
    TeeHttpServletRequest teeRequest = new TeeHttpServletRequest(requestId,
        (HttpServletRequest) request);
    TeeHttpServletResponse teeResponse = new TeeHttpServletResponse(requestId,
        (HttpServletResponse) response);

    if (logger.isDebugEnabled()) {
      boolean matched = isEscapeRequestBodyMatched(teeRequest);
      if (matched) {
        teeRequest.setAttribute(AccessConstants.TEE_FILTER_NOT_LOG_REQUEST_BODY, matched);
      }
      logger.debug(TeeUtil.printRequest(teeRequest));
    }

    try {
      filterChain.doFilter(teeRequest, teeResponse);
      teeResponse.finish();

      // let the output contents be available for later use by
      // logback-access-logging
      teeRequest.setAttribute(AccessConstants.LB_OUTPUT_BUFFER, teeResponse.getOutputBuffer());
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    } catch (ServletException e) {
      e.printStackTrace();
      throw e;
    } finally {
      if (logger.isDebugEnabled()) {
        boolean matched = isEscapeResponseBodyMatched(teeRequest);
        if (matched) {
          teeRequest.setAttribute(AccessConstants.TEE_FILTER_NOT_LOG_RESPONSE_BODY, matched);
        }
        logger.debug(TeeUtil.printResponse(teeRequest, teeResponse, onlyLogJsonResponse));
      }
    }
  }

  private boolean isNeedLog(ServletRequest request) {
    return active && (request instanceof HttpServletRequest)
        && !isExcludeUrlPatternMatched((HttpServletRequest) request);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    String includeListAsStr = filterConfig
        .getInitParameter(AccessConstants.TEE_FILTER_INCLUDES_PARAM);
    String excludeListAsStr = filterConfig
        .getInitParameter(AccessConstants.TEE_FILTER_EXCLUDES_PARAM);

    String str = filterConfig
        .getInitParameter(AccessConstants.TEE_FILTER_ONLY_LOG_JSON_RESPONSE_PARAM);
    if (StringUtils.isNotBlank(str)) {
      onlyLogJsonResponse = Boolean.valueOf(str);
    }

    final String requestBodyEscapes = filterConfig
        .getInitParameter(AccessConstants.TEE_FILTER_ESCAPE_REQUEST_BODY_PATTERNS_PARAM);
    setEscapeRequestBodyPatterns(requestBodyEscapes);

    final String responseBodyEscapes = filterConfig
        .getInitParameter(AccessConstants.TEE_FILTER_ESCAPE_RESPONSE_BODY_PATTERNS_PARAM);
    setEscapeResponseBodyPatterns(responseBodyEscapes);

    final String excludeUrlPattern = filterConfig
        .getInitParameter(AccessConstants.TEE_FILTER_EXCLUDE_URL_PATTERN);
    setExcludeUrlPatterns(excludeUrlPattern);

    String localhostName = getLocalhostName();
    active = computeActivation(localhostName, includeListAsStr, excludeListAsStr);
    if (active) {
      System.out.println("TeeFilter will be ACTIVE on this host [" + localhostName + "]");
    } else {
      System.out.println("TeeFilter will be DISABLED on this host [" + localhostName + "]");
    }
  }

  static List<String> extractNameList(String nameListAsStr) {
    List<String> nameList = new ArrayList<String>();
    if (nameListAsStr == null) {
      return nameList;
    }

    nameListAsStr = nameListAsStr.trim();
    if (nameListAsStr.length() == 0) {
      return nameList;
    }

    String[] nameArray = nameListAsStr.split("[,;]");
    for (String n : nameArray) {
      n = n.trim();
      nameList.add(n);
    }
    return nameList;
  }

  static String getLocalhostName() {
    String hostname = "127.0.0.1";

    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException uhe) {
      uhe.printStackTrace();
    }
    return hostname;
  }

  static boolean computeActivation(String hostname, String includeListAsStr,
      String excludeListAsStr) {
    List<String> includeList = extractNameList(includeListAsStr);
    List<String> excludeList = extractNameList(excludeListAsStr);
    boolean inIncludesList = mathesIncludesList(hostname, includeList);
    boolean inExcludesList = mathesExcludesList(hostname, excludeList);
    return inIncludesList && (!inExcludesList);
  }

  static boolean mathesIncludesList(String hostname, List<String> includeList) {
    if (includeList.isEmpty())
      return true;
    return includeList.contains(hostname);
  }

  static boolean mathesExcludesList(String hostname, List<String> excludesList) {
    if (excludesList.isEmpty())
      return false;
    return excludesList.contains(hostname);
  }

  private void setEscapeRequestBodyPatterns(String parterns) {
    if (StringUtils.isBlank(parterns)) {
      return;
    }
    for (String pattern : extractNameList(parterns)) {
      if (StringUtils.isNotBlank(pattern)) {
        escapeRequestBodyPatterns.add(Pattern.compile(pattern));
      }
    }
  }

  private void setEscapeResponseBodyPatterns(String parterns) {
    if (StringUtils.isBlank(parterns)) {
      return;
    }
    for (String pattern : extractNameList(parterns)) {
      if (StringUtils.isNotBlank(pattern)) {
        escapeResponseBodyPatterns.add(Pattern.compile(pattern));
      }
    }
  }

  private void setExcludeUrlPatterns(String parterns) {
    if (StringUtils.isBlank(parterns)) {
      return;
    }
    for (String pattern : extractNameList(parterns)) {
      if (StringUtils.isNotBlank(pattern)) {
        excludeUrlPatterns.add(Pattern.compile(pattern));
      }
    }
  }

  private boolean isEscapeRequestBodyMatched(HttpServletRequest request) {
    final String url = request.getRequestURI();
    for (Pattern p : escapeRequestBodyPatterns) {
      if (p.matcher(url).find()) {
        return true;
      }
    }
    return false;
  }

  private boolean isEscapeResponseBodyMatched(HttpServletRequest request) {
    final String url = request.getRequestURI();
    for (Pattern p : escapeResponseBodyPatterns) {
      if (p.matcher(url).find()) {
        return true;
      }
    }
    return false;
  }

  private boolean isExcludeUrlPatternMatched(HttpServletRequest request) {
    final String url = request.getRequestURI();
    for (Pattern p : excludeUrlPatterns) {
      if (p.matcher(url).find()) {
        return true;
      }
    }
    return false;
  }

}
