package br.edu.ifgoiano.academico.matricula_service.service;

import br.edu.ifgoiano.academico.matricula_service.model.Matricula;
import br.edu.ifgoiano.academico.matricula_service.model.StatusMatricula;
import br.edu.ifgoiano.academico.matricula_service.repository.MatriculaRepository;

import br.edu.ifgoiano.grpc.LiberaVagaRequest;
import br.edu.ifgoiano.grpc.LiberaVagaResponse;
import br.edu.ifgoiano.grpc.ReservaVagaRequest;
import br.edu.ifgoiano.grpc.ReservaVagaResponse;
import br.edu.ifgoiano.grpc.TurmaGrpcServiceGrpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;

    @GrpcClient("turma-service")
    private TurmaGrpcServiceGrpc.TurmaGrpcServiceBlockingStub turmaGrpcStub;

    public MatriculaService(MatriculaRepository matriculaRepository) {
        this.matriculaRepository = matriculaRepository;
    }

    public Matricula criarMatricula(Long alunoId, Long turmaId) {
        boolean jaExisteMatriculaAtiva =
                matriculaRepository.existsByAlunoIdAndTurmaIdAndStatus(
                        alunoId,
                        turmaId,
                        StatusMatricula.ATIVA
                );

        if (jaExisteMatriculaAtiva) {
            throw new IllegalStateException("Aluno já possui matrícula ativa nesta turma.");
        }

        // Reserva a vaga na turma via gRPC antes de efetivar a matrícula
        ReservaVagaResponse reserva = turmaGrpcStub.reservarVaga(
                ReservaVagaRequest.newBuilder()
                        .setTurmaId(turmaId)
                        .build()
        );

        if (!reserva.getSucesso()) {
            throw new IllegalStateException(
                    "Não foi possível reservar vaga na turma: " + reserva.getMensagem()
            );
        }

        try {
            Matricula matricula = new Matricula(alunoId, turmaId);
            return matriculaRepository.save(matricula);
        } catch (RuntimeException ex) {
            // Compensa a reserva caso a persistência da matrícula falhe
            turmaGrpcStub.liberarVaga(
                    LiberaVagaRequest.newBuilder()
                            .setTurmaId(turmaId)
                            .build()
            );
            throw ex;
        }
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
                        StatusMatricula.ATIVA
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Não existe matrícula ativa para este aluno nesta turma."
                ));

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
            System.err.println(
                    "Aviso: falha ao liberar vaga na turma " + turmaId
                            + ": " + liberacao.getMensagem()
            );
        }

        return matriculaCancelada;
    }
}
