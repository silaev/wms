package com.silaev.wms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
@EnableWebFlux
public class WmsApplication implements WebFluxConfigurer {
  @Value("${upload-file.max-disk-usage-per-part}")
  private long maxDiskUsagePerPart;
  @Value("${upload-file.max-parts}")
  private int maxParts;

  public static void main(String[] args) {
    //for debug during the run-time
    ReactorDebugAgent.init();

    SpringApplication.run(WmsApplication.class, args);
  }

  /**
   * Taken from <a href=https://stackoverflow.com/a/59538777/11773824">Spring WebFlux: set max file(s) size for uploading files</a>
   *
   * @param configurer
   */
  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    SynchronossPartHttpMessageReader partReader = new SynchronossPartHttpMessageReader();
    partReader.setMaxParts(maxParts);
    partReader.setMaxDiskUsagePerPart(maxDiskUsagePerPart);
    partReader.setEnableLoggingRequestDetails(true);

    MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(partReader);
    multipartReader.setEnableLoggingRequestDetails(true);

    configurer.defaultCodecs().multipartReader(multipartReader);
  }
}
