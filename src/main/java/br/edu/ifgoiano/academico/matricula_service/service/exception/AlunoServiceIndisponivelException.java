package br.edu.ifgoiano.academico.matricula_service.service.exception;

/**
 * Exceção lançada quando o Matrícula Service não consegue
 * se comunicar com o Aluno Service.
 */
public class AlunoServiceIndisponivelException extends RuntimeException {

    public AlunoServiceIndisponivelException(
            String mensagem,
            Throwable causa) {

        super(mensagem, causa);
    }
}
