package br.edu.ifgoiano.academico.matricula_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MatriculaServiceApplication {

  
    public static void main(String[] args) {
        SpringApplication.run(MatriculaServiceApplication.class, args);
    }
}