package com.worklog.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.worklog.common.exception.BusinessException;
import com.worklog.common.exception.ErrorCode;
import com.worklog.dailylog.DailyLogRepository;
import com.worklog.project.ProjectRepository;
import com.worklog.project.ProjectService;
import com.worklog.support.ServiceTestSupport;
import com.worklog.tag.Tag;
import com.worklog.tag.TagRepository;
import com.worklog.task.dto.TaskRequest;
import com.worklog.task.dto.TaskResponse;
import com.worklog.task.dto.TaskResultRequest;
import com.worklog.tasklog.TaskLogRepository;
import com.worklog.tasklog.TaskLogService;
import com.worklog.tasklog.dto.TaskLogRequest;

// 영구 삭제(되돌릴 수 없음)의 안전 규칙을 확인한다.
class PermanentDeleteServiceTest extends ServiceTestSupport {

	@Autowired
	private ProjectService projectService;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private TaskResultRepository taskResultRepository;
	@Autowired
	private TaskResultService taskResultService;
	@Autowired
	private TaskLogRepository taskLogRepository;
	@Autowired
	private TaskLogService taskLogService;
	@Autowired
	private DailyLogRepository dailyLogRepository;
	@Autowired
	private TagRepository tagRepository;
	@Autowired
	private TaskTagRepository taskTagRepository;

	private void assertErrorCode(Runnable action, ErrorCode expected) {
		assertThatThrownBy(action::run)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(expected);
	}

	@Test
	@DisplayName("보관한 업무를 영구 삭제하면 성과 항목·진행 메모·태그 연결도 함께 지워지고, 일일 기록은 남는다")
	void deleteTaskPermanently_removesChildrenButKeepsDailyLog() {
		Tag tag = tagRepository.save(new Tag(owner, "Spring"));
		TaskResponse task = taskService.createTask(owner.getId(), new TaskRequest("지울 업무", null,
				TaskPriority.MEDIUM, null, project.getId(), List.of(tag.getId()), List.of()));
		taskResultService.createResult(task.id(), owner.getId(), new TaskResultRequest("배포 시간", "30분", "5분"));
		taskLogService.createTaskLog(owner.getId(), LocalDate.of(2026, 9, 23), new TaskLogRequest(task.id(), "메모", 10));
		taskService.archiveTask(task.id(), owner.getId());

		taskService.deleteTaskPermanently(task.id(), owner.getId());

		assertThat(taskRepository.findById(task.id())).isEmpty();
		assertThat(taskResultRepository.count()).isZero();
		assertThat(taskLogRepository.count()).isZero();
		assertThat(taskTagRepository.count()).isZero();
		// 태그 자체와 일일 기록(회고 자리)은 그대로 남는다
		assertThat(tagRepository.findById(tag.getId())).isPresent();
		assertThat(dailyLogRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("보관하지 않은 업무는 영구 삭제할 수 없다 (NOT_ARCHIVED)")
	void deleteTaskPermanently_whenNotArchived_throwsConflict() {
		TaskResponse task = createTask("살아있는 업무");

		assertErrorCode(() -> taskService.deleteTaskPermanently(task.id(), owner.getId()), ErrorCode.NOT_ARCHIVED);
		assertThat(taskRepository.findById(task.id())).isPresent();
	}

	@Test
	@DisplayName("남의 업무는 영구 삭제할 수 없다 (TASK_ACCESS_DENIED)")
	void deleteTaskPermanently_otherMembersTask_throwsAccessDenied() {
		TaskResponse task = createTask("내 업무");
		taskService.archiveTask(task.id(), owner.getId());

		assertErrorCode(() -> taskService.deleteTaskPermanently(task.id(), other.getId()),
				ErrorCode.TASK_ACCESS_DENIED);
		assertThat(taskRepository.findById(task.id())).isPresent();
	}

	@Test
	@DisplayName("보관한 프로젝트에 업무가 없으면 영구 삭제된다")
	void deleteProjectPermanently_whenNoTasks_deletes() {
		projectService.archiveProject(project.getId(), owner.getId());

		projectService.deleteProjectPermanently(project.getId(), owner.getId());

		assertThat(projectRepository.findById(project.getId())).isEmpty();
	}

	@Test
	@DisplayName("프로젝트에 업무가 하나라도 남아 있으면(보관한 업무 포함) 영구 삭제할 수 없다 (PROJECT_HAS_TASKS)")
	void deleteProjectPermanently_whenTasksRemain_throwsConflict() {
		TaskResponse task = createTask("남은 업무");
		taskService.archiveTask(task.id(), owner.getId());
		projectService.archiveProject(project.getId(), owner.getId());

		assertErrorCode(() -> projectService.deleteProjectPermanently(project.getId(), owner.getId()),
				ErrorCode.PROJECT_HAS_TASKS);

		// 업무를 먼저 영구 삭제하면 프로젝트도 지울 수 있다
		taskService.deleteTaskPermanently(task.id(), owner.getId());
		projectService.deleteProjectPermanently(project.getId(), owner.getId());
		assertThat(projectRepository.findById(project.getId())).isEmpty();
	}

	@Test
	@DisplayName("보관하지 않은 프로젝트와 남의 프로젝트는 영구 삭제할 수 없다")
	void deleteProjectPermanently_notArchivedOrOthers_throws() {
		assertErrorCode(() -> projectService.deleteProjectPermanently(project.getId(), owner.getId()),
				ErrorCode.NOT_ARCHIVED);

		projectService.archiveProject(project.getId(), owner.getId());
		assertErrorCode(() -> projectService.deleteProjectPermanently(project.getId(), other.getId()),
				ErrorCode.PROJECT_ACCESS_DENIED);
		assertThat(projectRepository.findById(project.getId())).isPresent();
	}

}
