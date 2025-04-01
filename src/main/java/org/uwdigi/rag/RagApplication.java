package org.uwdigi.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, JdbcRepositoriesAutoConfiguration.class})
public class RagApplication {

  public static void main(String[] args) {
    SpringApplication.run(RagApplication.class, args);
  }
}
