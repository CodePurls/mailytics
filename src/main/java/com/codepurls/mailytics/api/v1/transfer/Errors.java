package com.codepurls.mailytics.api.v1.transfer;

import java.util.ArrayList;
import java.util.List;

public class Errors {
  public interface Type {
    public String name();
  }

  public enum ErrorType implements Type {
    required, invalid, duplicate, global;

  }

  public static class Error {
    public Type   type;
    public String field;
    public String message;
  }

  public List<Error> errors = new ArrayList<>();

  public static Errors addError(Type type, String field, String message) {
    Errors er = new Errors();
    Error e = new Error();
    e.type = type;
    e.field = field;
    e.message = message;
    er.errors.add(e);
    return er;
  }

  public static Errors addTopLevelError(String msg) {
    Errors er = new Errors();
    Error e = new Error();
    e.type = ErrorType.global;
    e.message = msg;
    er.errors.add(e);
    return er;
  }
}
