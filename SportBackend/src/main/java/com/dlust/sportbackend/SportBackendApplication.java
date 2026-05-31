package com.dlust.sportbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.dlust.sportbackend.Mapper")
public class SportBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportBackendApplication.class, args);
    }

}
