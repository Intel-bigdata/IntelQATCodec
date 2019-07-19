package com.intel.qat.codec.io.exception;

import java.io.IOException;

public class QatIOException extends IOException {
  private static final long serialVersionUID = 4506962738917911676L;
  private final QatErrorCode errorCode;

  public QatIOException(QatErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  @Override
  public String getMessage() {
    return String.format("[%s] %s", errorCode.name(), super.getMessage());
  }

  public QatErrorCode getErrorCode() {
    return errorCode;
  }
}