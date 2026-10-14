package com.worklog.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.worklog.support.ControllerTestSupport;

class PermanentDeleteControllerTest extends ControllerTestSupport {

	@Test
	@DisplayName("보관하지 않은 업무의 영구 삭제는 409 NOT_ARCHIVED, 보관 후에는 204이고 보관함에서도 사라진다")
	void taskPermanentDelete_requiresArchive() throws Exception {
		String token = signupAndLogin("a@test.com");
		long taskId = createTask(token, createProject(token, "p"), "t1");

		mockMvc.perform(delete("/api/tasks/" + taskId + "/permanent").header("Authorization", bearer(token)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_ARCHIVED"));

		mockMvc.perform(delete("/api/tasks/" + taskId).header("Authorization", bearer(token)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/tasks/" + taskId + "/permanent").header("Authorization", bearer(token)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/tasks").param("archived", "true").header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", bearer(token)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("업무가 남은 프로젝트는 409 PROJECT_HAS_TASKS, 업무를 먼저 지우면 204")
	void projectPermanentDelete_blockedByTasks() throws Exception {
		String token = signupAndLogin("a@test.com");
		long projectId = createProject(token, "p");
		long taskId = createTask(token, projectId, "t1");

		mockMvc.perform(delete("/api/tasks/" + taskId).header("Authorization", bearer(token)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", bearer(token)))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/projects/" + projectId + "/permanent").header("Authorization", bearer(token)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PROJECT_HAS_TASKS"));

		mockMvc.perform(delete("/api/tasks/" + taskId + "/permanent").header("Authorization", bearer(token)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/projects/" + projectId + "/permanent").header("Authorization", bearer(token)))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("남의 업무·프로젝트는 403, 토큰 없으면 401")
	void permanentDelete_security() throws Exception {
		String tokenA = signupAndLogin("a@test.com");
		String tokenB = signupAndLogin("b@test.com");
		long projectId = createProject(tokenA, "p");
		long taskId = createTask(tokenA, projectId, "t1");
		mockMvc.perform(delete("/api/tasks/" + taskId).header("Authorization", bearer(tokenA)));
		mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", bearer(tokenA)));

		mockMvc.perform(delete("/api/tasks/" + taskId + "/permanent").header("Authorization", bearer(tokenB)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TASK_ACCESS_DENIED"));
		mockMvc.perform(delete("/api/projects/" + projectId + "/permanent").header("Authorization", bearer(tokenB)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));

		mockMvc.perform(delete("/api/tasks/" + taskId + "/permanent")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/projects/" + projectId + "/permanent")).andExpect(status().isUnauthorized());
	}

}
