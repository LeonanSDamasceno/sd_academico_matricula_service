package br.edu.ifgoiano.academico.matricula_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal do microsserviço de matrícula.
 *
 * Este serviço representa o módulo responsável pelas operações de matrícula
 * do sistema acadêmico. Ao iniciar, ele se registra no Eureka Server para ser
 * descoberto por outros serviços da aplicação.
 */
@SpringBootApplication
public class MatriculaServiceApplication {

    /**
     * Método principal da aplicação.
     *
     * Inicializa o microsserviço de matrícula na porta configurada no arquivo
     * application.properties.
     *
     * @param args argumentos de execução da aplicação
     */
    public static void main(String[] args) {
        SpringApplication.run(MatriculaServiceApplication.class, args);
    }
}