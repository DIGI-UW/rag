package org.uwdigi.rag.shared;

public class QueryResponse {
  private final String response;
  private final String sqlRun;

  public QueryResponse(String response, String sqlRun) {
    this.response = response;
    this.sqlRun = sqlRun;
  }

  public String getResponse() {
    return response;
  }

  public String getSqlRun() {
    return sqlRun;
  }
}
