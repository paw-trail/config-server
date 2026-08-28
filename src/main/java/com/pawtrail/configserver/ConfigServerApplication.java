package com.pawtrail.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 설정 서버입니다.
 *
 * paw-trail/config 저장소를 읽어 각 서비스에 설정을 내려줍니다.
 * 요청을 받을 때마다 저장소를 다시 읽으므로, 설정을 바꾸는 것은 커밋으로 끝나며
 * 이 서버를 다시 띄울 필요가 없습니다.
 *
 * 이 서비스는 자기 설정을 config 저장소에서 받지 못합니다.
 * 저장소 주소를 알아야 저장소를 읽을 수 있기 때문이며,
 * 그래서 config 저장소에는 config-server.yml 이 존재하지 않습니다.
 *
 * 공통 모듈(com.pawtrail.common)을 의존하지 않습니다.
 * 공통 모듈의 TraceIdResponseAdvice 가 ResponseBodyAdvice 라서,
 * 설정 서버가 내려주는 Environment 응답까지 감싸면 클라이언트가 설정을 읽지 못합니다.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

}
