package com.gomore.experiment.logging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * @author Debenson
 * @since 0.1
 */
class TeeHttpServletResponse extends HttpServletResponseWrapper {

  TeeServletOutputStream teeServletOutputStream;
  PrintWriter teeWriter;
  private long id;

  public TeeHttpServletResponse(long requestId, HttpServletResponse httpServletResponse) {
    super(httpServletResponse);
    this.id = requestId;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (teeServletOutputStream == null) {
      teeServletOutputStream = new TeeServletOutputStream(this.getResponse());
    }
    return teeServletOutputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (this.teeWriter == null) {
      this.teeWriter = new PrintWriter(
          new OutputStreamWriter(getOutputStream(), this.getResponse().getCharacterEncoding()),
          true);
    }
    return this.teeWriter;
  }

  @Override
  public void flushBuffer() {
    if (this.teeWriter != null) {
      this.teeWriter.flush();
    }
  }

  byte[] getOutputBuffer() {
    // teeServletOutputStream can be null if the getOutputStream method is never
    // called.
    if (teeServletOutputStream != null) {
      return teeServletOutputStream.getOutputStreamAsByteArray();
    } else {
      return null;
    }
  }

  void finish() throws IOException {
    if (this.teeWriter != null) {
      this.teeWriter.close();
    }
    if (this.teeServletOutputStream != null) {
      this.teeServletOutputStream.close();
    }
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
