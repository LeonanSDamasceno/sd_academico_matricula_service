package br.edu.ifgoiano.academico.matricula_service.service;

import br.edu.ifgoiano.academico.matricula_service.model.Matricula;
import br.edu.ifgoiano.academico.matricula_service.model.StatusMatricula;
import br.edu.ifgoiano.academico.matricula_service.repository.MatriculaRepository;

import br.edu.ifgoiano.grpc.LiberaVagaRequest;
import br.edu.ifgoiano.grpc.LiberaVagaResponse;
import br.edu.ifgoiano.grpc.TurmaGrpcServiceGrpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import br.edu.ifgoiano.academico.matricula_service.client.AlunoClient;

import java.util.List;

@Service
public class MatriculaService {

    private static final Logger log = LoggerFactory.getLogger(MatriculaService.class);

    private final MatriculaRepository matriculaRepository;
    private final AlunoClient alunoClient;

    @GrpcClient("turma-service")
    private TurmaGrpcServiceGrpc.TurmaGrpcServiceBlockingStub turmaGrpcStub;

    public MatriculaService(MatriculaRepository matriculaRepository, AlunoClient alunoClient) {
        this.matriculaRepository = matriculaRepository;
        this.alunoClient = alunoClient;
    }

    public Matricula criarMatricula(Long alunoId, Long turmaId) {
    boolean alunoExiste = alunoClient.alunoExiste(alunoId);

    if (!alunoExiste) {
        throw new IllegalStateException("Aluno informado não existe.");
    }

    boolean jaExisteMatriculaAtiva =
            matriculaRepository.existsByAlunoIdAndTurmaIdAndStatus(
                    alunoId,
                    turmaId,
                    StatusMatricula.ATIVA
            );

    if (jaExisteMatriculaAtiva) {
        throw new IllegalStateException("Aluno já possui matrícula ativa nesta turma.");
    }

        Matricula matricula = new Matricula(alunoId, turmaId);

        return matriculaRepository.save(matricula);
    }

    public List<Matricula> listarTodas() {
        return matriculaRepository.findAll();
    }

    public List<Matricula> listarPorAluno(Long alunoId) {
        return matriculaRepository.findByAlunoId(alunoId);
    }

    public List<Matricula> listarPorTurma(Long turmaId) {
        return matriculaRepository.findByTurmaId(turmaId);
    }

    public Matricula cancelarMatricula(Long alunoId, Long turmaId) {
        Matricula matricula = matriculaRepository
                .findByAlunoIdAndTurmaIdAndStatus(
                        alunoId,
                        turmaId,
                        StatusMatricula.ATIVA)
                .orElseThrow(() -> new IllegalStateException(
                        "Não existe matrícula ativa para este aluno nesta turma."));

        matricula.cancelar();
        Matricula matriculaCancelada = matriculaRepository.save(matricula);

        // Libera a vaga na turma via gRPC após cancelar a matrícula
        LiberaVagaResponse liberacao = turmaGrpcStub.liberarVaga(
                LiberaVagaRequest.newBuilder()
                        .setTurmaId(turmaId)
                        .build()
        );

        if (!liberacao.getSucesso()) {
            // Não reverte o cancelamento; apenas registra a inconsistência
            log.warn("Falha ao liberar vaga na turma {}: {}", turmaId, liberacao.getMensagem());
        }

        return matriculaCancelada;
    }
}
