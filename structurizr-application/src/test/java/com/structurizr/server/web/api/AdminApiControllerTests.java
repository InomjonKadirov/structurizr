package com.structurizr.server.web.api;

import com.structurizr.configuration.StructurizrProperties;
import com.structurizr.server.domain.User;
import com.structurizr.server.domain.WorkspaceMetadata;
import com.structurizr.server.web.AbstractTestsBase;
import com.structurizr.server.web.MockSearchComponent;
import com.structurizr.server.web.MockWorkspaceComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AdminApiControllerTests extends AbstractTestsBase {

    private static final String ADMIN_API_KEY = "admin-1234567890";

    private AdminApiController controller;
    private Map<Long, WorkspaceMetadata> workspaces;
    private List<Long> deletedFromSearchIndex;

    @BeforeEach
    void setUp() {
        workspaces = new HashMap<>();
        deletedFromSearchIndex = new ArrayList<>();

        controller = new AdminApiController();
        controller.setWorkspaceComponent(new MockWorkspaceComponent() {
            @Override
            public List<WorkspaceMetadata> getWorkspaces() {
                return workspaces.values().stream().sorted((a, b) -> Long.compare(a.getId(), b.getId())).toList();
            }

            @Override
            public WorkspaceMetadata getWorkspaceMetadata(long workspaceId) {
                return workspaces.get(workspaceId);
            }

            @Override
            public void putWorkspaceMetadata(WorkspaceMetadata workspaceMetadata) {
                workspaces.put(workspaceMetadata.getId(), workspaceMetadata);
            }

            @Override
            public long createWorkspace(User user) {
                long workspaceId = workspaces.keySet().stream().reduce(0L, Long::max) + 1;
                WorkspaceMetadata workspaceMetadata = new WorkspaceMetadata(workspaceId);
                workspaceMetadata.setName("Workspace " + workspaceId);
                workspaceMetadata.setDescription("Description");
                workspaceMetadata.regenerateApiKey();
                workspaces.put(workspaceId, workspaceMetadata);

                return workspaceId;
            }

            @Override
            public boolean deleteWorkspace(long workspaceId) {
                return workspaces.remove(workspaceId) != null;
            }
        });
        controller.setSearchComponent(new MockSearchComponent() {
            @Override
            public void delete(long workspaceId) {
                deletedFromSearchIndex.add(workspaceId);
            }
        });

        WorkspaceMetadata workspace1 = new WorkspaceMetadata(1);
        workspace1.setName("Workspace 1");
        workspaces.put(1L, workspace1);

        WorkspaceMetadata workspace2 = new WorkspaceMetadata(2);
        workspace2.setName("Workspace 2");
        workspace2.setPublicWorkspace(true);
        workspaces.put(2L, workspace2);

        WorkspaceMetadata workspace3 = new WorkspaceMetadata(3);
        workspace3.setArchived(true);
        workspaces.put(3L, workspace3);
    }

    private void configureWithAdminApiKey() {
        Properties properties = new Properties();
        properties.setProperty(StructurizrProperties.API_KEY, new BCryptPasswordEncoder().encode(ADMIN_API_KEY));
        configureAsServerWithAuthenticationEnabled(properties);
    }

    @Test
    void getWorkspaces_ReturnsActiveWorkspaces_WhenAuthenticationIsDisabled() {
        configureAsServerWithAuthenticationDisabled();

        List<AdminWorkspaceApiResponse> result = controller.getWorkspaces(null).getWorkspaces();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals("Workspace 1", result.get(0).getName());
        assertEquals("/workspace/1", result.get(0).getPrivateUrl());
        assertNull(result.get(0).getPublicUrl());
        assertNull(result.get(0).getApiKey());
        assertEquals(2, result.get(1).getId());
        assertEquals("/share/2", result.get(1).getPublicUrl());
    }

    @Test
    void getWorkspaces_ReturnsAnError_WhenNoAdminApiKeyIsConfigured() {
        configureAsServerWithAuthenticationEnabled();

        HttpUnauthorizedException e = assertThrows(HttpUnauthorizedException.class, () -> controller.getWorkspaces(ADMIN_API_KEY));
        assertEquals("The admin API is not enabled (no admin API key has been configured)", e.getMessage());
    }

    @Test
    void getWorkspaces_ReturnsAnError_WhenNoApiKeyIsSpecified() {
        configureWithAdminApiKey();

        HttpUnauthorizedException e = assertThrows(HttpUnauthorizedException.class, () -> controller.getWorkspaces(""));
        assertEquals("API key must be provided", e.getMessage());
    }

    @Test
    void getWorkspaces_ReturnsAnError_WhenTheApiKeyIsIncorrect() {
        configureWithAdminApiKey();

        HttpUnauthorizedException e = assertThrows(HttpUnauthorizedException.class, () -> controller.getWorkspaces("0987654321"));
        assertEquals("Incorrect API key", e.getMessage());
    }

    @Test
    void getWorkspaces_ReturnsWorkspaces_WhenTheAdminApiKeyIsCorrect() {
        configureWithAdminApiKey();

        assertEquals(2, controller.getWorkspaces(ADMIN_API_KEY).getWorkspaces().size());
    }

    @Test
    void createWorkspace_ReturnsTheNewWorkspaceWithAWorkingApiKey() {
        configureWithAdminApiKey();

        AdminWorkspaceApiResponse result = controller.createWorkspace(ADMIN_API_KEY);

        assertEquals(4, result.getId());
        assertEquals("Workspace 4", result.getName());
        assertEquals("/workspace/4", result.getPrivateUrl());
        assertNotNull(result.getApiKey());
        assertTrue(workspaces.get(4L).isApiKeyValid(result.getApiKey()));
        assertNotEquals(result.getApiKey(), workspaces.get(4L).getApiKey()); // only the hash is stored
    }

    @Test
    void createWorkspace_ReturnsAnError_WhenTheApiKeyIsIncorrect() {
        configureWithAdminApiKey();

        assertThrows(HttpUnauthorizedException.class, () -> controller.createWorkspace("0987654321"));
        assertEquals(3, workspaces.size());
    }

    @Test
    void deleteWorkspace_DeletesTheWorkspaceAndRemovesItFromTheSearchIndex() {
        configureWithAdminApiKey();

        ApiResponse result = controller.deleteWorkspace(1, ADMIN_API_KEY);

        assertTrue(result.isSuccess());
        assertFalse(workspaces.containsKey(1L));
        assertEquals(List.of(1L), deletedFromSearchIndex);
    }

    @Test
    void deleteWorkspace_ReturnsNotFound_WhenTheWorkspaceDoesNotExist() {
        configureWithAdminApiKey();

        assertThrows(NotFoundApiException.class, () -> controller.deleteWorkspace(99, ADMIN_API_KEY));
        assertTrue(deletedFromSearchIndex.isEmpty());
    }

    @Test
    void deleteWorkspace_ReturnsAnError_WhenTheApiKeyIsIncorrect() {
        configureWithAdminApiKey();

        assertThrows(HttpUnauthorizedException.class, () -> controller.deleteWorkspace(1, "0987654321"));
        assertTrue(workspaces.containsKey(1L));
    }

}
