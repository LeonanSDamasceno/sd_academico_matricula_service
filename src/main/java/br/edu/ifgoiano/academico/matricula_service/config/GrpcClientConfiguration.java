package br.edu.ifgoiano.academico.matricula_service.config;

import br.edu.ifgoiano.grpc.TurmaGrpcServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration(proxyBeanMethods = false)
public class GrpcClientConfiguration {

    @Bean
    TurmaGrpcServiceGrpc.TurmaGrpcServiceBlockingStub turmaGrpcStub(
            GrpcChannelFactory channelFactory) {
        return TurmaGrpcServiceGrpc.newBlockingStub(
                channelFactory.createChannel("turma-service"));
    }
}
