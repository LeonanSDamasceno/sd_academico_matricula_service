package br.edu.ifgoiano.academico.matricula_service.service;

import br.edu.ifgoiano.academico.matricula_service.messaging.dto.MensagemNotificacao;
import br.edu.ifgoiano.academico.matricula_service.messaging.consumer.NotificationProducer;
import br.edu.ifgoiano.academico.matricula_service.model.Matricula;
import br.edu.ifgoiano.academico.matricula_service.model.StatusMatricula;
import br.edu.ifgoiano.academico.matricula_service.repository.MatriculaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatriculaService {

    private static final Logger logger = LoggerFactory.getLogger(MatriculaService.class);

    private final MatriculaRepository matriculaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationProducer notificationProducer;
    public MatriculaService(MatriculaRepository matriculaRepository,
                            RabbitTemplate rabbitTemplate,
                            NotificationProducer notificationProducer) {
        this.matriculaRepository = matriculaRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.notificationProducer = notificationProducer;
    }


    public Matricula criarMatricula(Long alunoId, Long turmaId) {
        logger.info("[MATRICULA-SERVICE] Criando matrícula - Aluno: {}, Turma: {}", alunoId, turmaId);

        boolean jaExisteMatriculaAtiva =
                matriculaRepository.existsByAlunoIdAndTurmaIdAndStatus(
                        alunoId,
                        turmaId,
                        StatusMatricula.ATIVA
                );

        if (jaExisteMatriculaAtiva) {
            logger.warn("[MATRICULA-SERVICE] Aluno {} já possui matrícula ativa na turma {}", alunoId, turmaId);
            throw new IllegalStateException("Aluno já possui matrícula ativa nesta turma.");
        }

        MensagemNotificacao mensagem = new MensagemNotificacao();
        mensagem.setAlunoId(alunoId);
        mensagem.setTurmaId(turmaId);
        mensagem.setTipo("MATRICULA_CRIADA");
        mensagem.setMensagem("Matrícula realizada com sucesso.");

        rabbitTemplate.convertAndSend("fila.notificacoes", mensagem);

        Matricula matricula = new Matricula(alunoId, turmaId);
        Matricula salva = matriculaRepository.save(matricula);

        logger.info("[MATRICULA-SERVICE] Matrícula realizada com sucesso - ID: {}, Aluno: {}, Turma: {}",
                salva.getId(), alunoId, turmaId);

        return salva;
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
        logger.info("[MATRICULA-SERVICE] Cancelando matrícula - Aluno: {}, Turma: {}", alunoId, turmaId);

        Matricula matricula = matriculaRepository
                .findByAlunoIdAndTurmaIdAndStatus(
                        alunoId,
                        turmaId,
                        StatusMatricula.ATIVA
                )
                .orElseThrow(() -> {
                    logger.warn("[MATRICULA-SERVICE] Nenhuma matrícula ativa encontrada - Aluno: {}, Turma: {}", alunoId, turmaId);
                    return new IllegalStateException("Não existe matrícula ativa para este aluno nesta turma.");
                });

        matricula.cancelar();
        Matricula cancelada = matriculaRepository.save(matricula);

        logger.info("[MATRICULA-SERVICE] Matrícula cancelada com sucesso - ID: {}, Aluno: {}, Turma: {}",
                cancelada.getId(), alunoId, turmaId);

        return cancelada;
    }
}