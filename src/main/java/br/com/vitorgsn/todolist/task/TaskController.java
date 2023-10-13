package br.com.vitorgsn.todolist.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.vitorgsn.todolist.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private ITaskRepository taskRepository;

    @PostMapping("/")
    public ResponseEntity create(@RequestBody TaskModel taskModel, HttpServletRequest request) {
        // Coleta o id do usuário enviado do filtro pelo atributo da requisição
        var idUserOnline = request.getAttribute("idUser");
        // Converte de string para UUID
        taskModel.setIdUser((UUID) idUserOnline);

        // Verifica se a data de início / data término passada pelo usuário é válida
        var currentDate = LocalDateTime.now();
        if (currentDate.isAfter(taskModel.getStartAt()) || currentDate.isAfter(taskModel.getEndAt())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("A data de início / data de término deve ser maior que a data atual!");
        }

        // Verifica se a data de término é válida, maior que a de início
        if (taskModel.getStartAt().isAfter(taskModel.getEndAt())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("A data de início deve ser menor do que a data de término!");
        }

        // Envia pro repository gravar no banco e retorna os dados da task para o
        // usuário que fez a requisição
        var task = this.taskRepository.save(taskModel);
        return ResponseEntity.status(HttpStatus.OK).body(task);
    }

    @GetMapping("/")
    public ResponseEntity<List<TaskModel>> list(HttpServletRequest request) {

        var idUserOnline = request.getAttribute("idUser");
        var tasks = this.taskRepository.findByIdUser((UUID) idUserOnline);

        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }

    @PutMapping("/{id}")
    public ResponseEntity update(@RequestBody TaskModel taskModel, HttpServletRequest request, @PathVariable UUID id) {

        var oldTask = this.taskRepository.findById(id).orElse(null);
        var idUser = request.getAttribute("idUser");

        // Validar se a task a ser atualizada existe
        if (oldTask == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Tarefa não encontrada.");
        }

        // Validar se o usuário que está tentando alterar a task é o proprietário
        if (!oldTask.getIdUser().equals(idUser)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("O Usuário não tem permissão para alterar essa tarefa.");
        }

        // Mescla de atributos da task existente com os atributos passados na requisição
        // Update parcial da task
        Utils.copyNonNullProperties(taskModel, oldTask);
        var taskUpdated = this.taskRepository.save(oldTask);

        return ResponseEntity.status(HttpStatus.OK).body(taskUpdated);
    }
}
