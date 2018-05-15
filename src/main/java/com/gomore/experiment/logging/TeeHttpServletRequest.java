package com.gomore.experiment.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author Debenson
 * @since 0.1
 */
class TeeHttpServletRequest extends HttpServletRequestWrapper {

  private TeeServletInputStream inStream;
  private BufferedReader reader;
  boolean postedParametersMode = false;
  private long id;

  TeeHttpServletRequest(long requestId, HttpServletRequest request) {
    super(request);
    this.id = requestId;

    request.setAttribute(AccessConstants.TEE_FILTER_START_TIME_PARAM, System.currentTimeMillis());

    // we can't access the input stream and access the request parameters
    // at the same time
    if (TeeUtil.isFormUrlEncoded(request)) {
      postedParametersMode = true;
      // 这里仅仅列出参数名称，可能并不完全等于POST的内容（可能包含query参数）
      request.setAttribute(AccessConstants.LB_INPUT_BUFFER, getFormParameters(request));
    } else {
      inStream = new TeeServletInputStream(request);
      // add the contents of the input buffer as an attribute of the request
      request.setAttribute(AccessConstants.LB_INPUT_BUFFER, inStream.getInputBuffer());
      reader = new BufferedReader(new InputStreamReader(inStream));
    }
  }

  private byte[] getFormParameters(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();
    try {
      Enumeration<String> enumeration = request.getParameterNames();
      while (enumeration.hasMoreElements()) {
        String name = enumeration.nextElement();
        if (sb.length() != 0) {
          sb.append("&");
        }
        sb.append(name).append("=").append(request.getParameter(name));
      }
      return sb.toString().getBytes("utf-8");
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  byte[] getInputBuffer() {
    if (postedParametersMode) {
      throw new IllegalStateException("Call disallowed in postedParametersMode");
    }
    return inStream.getInputBuffer();
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    if (!postedParametersMode) {
      return inStream;
    } else {
      return super.getInputStream();
    }
  }

  @Override
  public BufferedReader getReader() throws IOException {
    if (!postedParametersMode) {
      return reader;
    } else {
      return super.getReader();
    }
  }

  public boolean isPostedParametersMode() {
    return postedParametersMode;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

}
