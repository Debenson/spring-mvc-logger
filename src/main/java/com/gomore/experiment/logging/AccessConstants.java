package com.gomore.experiment.logging;

/**
 * @author Debenson
 * @since 0.1
 */
public class AccessConstants {

  public static final String LOGBACK_STATUS_MANAGER_KEY = "LOGBACK_STATUS_MANAGER";
  public static final String LB_INPUT_BUFFER = "LB_INPUT_BUFFER";
  public static final String LB_OUTPUT_BUFFER = "LB_OUTPUT_BUFFER";

  public static final String X_WWW_FORM_URLECODED = "application/x-www-form-urlencoded";

  public static final String IMAGE_CONTENT_TYPE = "image/";
  public static final String IMAGE_JPEG = "image/jpeg";
  public static final String IMAGE_GIF = "image/gif";
  public static final String IMAGE_PNG = "image/png";

  public static final String TEE_FILTER_INCLUDES_PARAM = "includes";
  public static final String TEE_FILTER_EXCLUDES_PARAM = "excludes";
  public static final String TEE_FILTER_EXCLUDE_URL_PATTERN = "excludeUrlPattern";
  public static final String TEE_FILTER_ESCAPE_REQUEST_BODY_PATTERNS_PARAM = "escapeRequestBodyPatterns";
  public static final String TEE_FILTER_ESCAPE_RESPONSE_BODY_PATTERNS_PARAM = "escapeResponseBodyPatterns";

  public static final String TEE_FILTER_ONLY_LOG_JSON_RESPONSE_PARAM = "onlyLogJsonResp";
  public static final String TEE_FILTER_START_TIME_PARAM = "__start time__";
  public static final String TEE_FILTER_NOT_LOG_REQUEST_BODY = "__not_log_request_body__";
  public static final String TEE_FILTER_NOT_LOG_RESPONSE_BODY = "__not_log_response_body__";

}
