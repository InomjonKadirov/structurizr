package com.structurizr.server.web.api;

import com.structurizr.api.HttpHeaders;
import com.structurizr.configuration.Configuration;
import com.structurizr.configuration.StructurizrProperties;
import com.structurizr.server.component.search.SearchComponent;
import com.structurizr.server.component.workspace.WorkspaceComponent;
import com.structurizr.server.component.workspace.WorkspaceComponentException;
import com.structurizr.server.domain.WorkspaceMetadata;
import com.structurizr.server.web.AbstractController;
import com.structurizr.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Fork addition (branch "inam"): an implementation of the Structurizr admin API.
 *
 *  - GET /api/workspace           lists workspaces
 *  - POST /api/workspace          creates a workspace (the response carries its plaintext API key, once)
 *  - DELETE /api/workspace/{id}   deletes (or archives, if archiving is enabled) a workspace
 *
 * The contract matches com.structurizr.api.AdminApiClient, so the stock "create", "pull" (all workspaces)
 * and "delete" commands work against this server. Because this class exists, com.structurizr.server.Server
 * no longer adds the "open-core" profile, and NoOpAdminApiController is not loaded.
 *
 * Authorisation follows the rest of the server API: when authentication is disabled no API key is needed;
 * when it is enabled the X-Authorization header must match the bcrypt hash in "structurizr.apikey".
 */
@RestController
@Profile("command-server")
public class AdminApiController extends AbstractController {

    private static final Log log = LogFactory.getLog(AdminApiController.class);

    private WorkspaceComponent workspaceComponent;
    private SearchComponent searchComponent;

    @Autowired
    public void setWorkspaceComponent(WorkspaceComponent workspaceComponent) {
        this.workspaceComponent = workspaceComponent;
    }

    @Autowired
    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

    @RequestMapping(value = "/api/workspace", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    public AdminWorkspacesApiResponse getWorkspaces(
            @RequestHeader(name = HttpHeaders.X_AUTHORIZATION, required = false) String apiKey
    ) {
        authoriseRequest(apiKey);

        try {
            List<AdminWorkspaceApiResponse> workspaces = workspaceComponent.getWorkspaces().stream()
                    .filter(workspaceMetadata -> !workspaceMetadata.isArchived())
                    .map(workspaceMetadata -> new AdminWorkspaceApiResponse(workspaceMetadata, null))
                    .toList();

            return new AdminWorkspacesApiResponse(workspaces);
        } catch (WorkspaceComponentException e) {
            log.error(e);
            throw new ApiException("Could not get workspaces");
        }
    }

    @RequestMapping(value = "/api/workspace", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public AdminWorkspaceApiResponse createWorkspace(
            @RequestHeader(name = HttpHeaders.X_AUTHORIZATION, required = false) String apiKey
    ) {
        authoriseRequest(apiKey);

        try {
            long workspaceId = workspaceComponent.createWorkspace(getUser());
            WorkspaceMetadata workspaceMetadata = workspaceComponent.getWorkspaceMetadata(workspaceId);

            // the stored API key is a bcrypt hash, so issue a fresh key and hand back the plaintext once
            String workspaceApiKey = workspaceMetadata.regenerateApiKey();
            workspaceComponent.putWorkspaceMetadata(workspaceMetadata);

            return new AdminWorkspaceApiResponse(workspaceMetadata, workspaceApiKey);
        } catch (WorkspaceComponentException e) {
            log.error(e);
            throw new ApiException("Could not create workspace");
        }
    }

    @RequestMapping(value = "/api/workspace/{workspaceId}", method = RequestMethod.DELETE, produces = "application/json; charset=UTF-8")
    public ApiResponse deleteWorkspace(
            @PathVariable("workspaceId") long workspaceId,
            @RequestHeader(name = HttpHeaders.X_AUTHORIZATION, required = false) String apiKey
    ) {
        authoriseRequest(apiKey);

        WorkspaceMetadata workspaceMetadata = workspaceComponent.getWorkspaceMetadata(workspaceId);
        if (workspaceMetadata == null) {
            throw new NotFoundApiException();
        }

        try {
            boolean success = workspaceComponent.deleteWorkspace(workspaceId);
            if (success) {
                try {
                    searchComponent.delete(workspaceId);
                } catch (Exception e) {
                    log.error(e);
                }
            }

            return new ApiResponse(success);
        } catch (WorkspaceComponentException e) {
            log.error(e);
            throw new ApiException("Could not delete workspace " + workspaceId);
        }
    }

    private void authoriseRequest(String apiKey) {
        if (!Configuration.getInstance().isAuthenticationEnabled()) {
            return;
        }

        String adminApiKey = Configuration.getInstance().getProperty(StructurizrProperties.API_KEY);
        if (StringUtils.isNullOrEmpty(adminApiKey)) {
            throw new HttpUnauthorizedException("The admin API is not enabled (no admin API key has been configured)");
        }

        if (StringUtils.isNullOrEmpty(apiKey)) {
            throw new HttpUnauthorizedException("API key must be provided");
        }

        if (!new BCryptPasswordEncoder().matches(apiKey, adminApiKey)) {
            throw new HttpUnauthorizedException("Incorrect API key");
        }
    }

    @ExceptionHandler(HttpUnauthorizedException.class)
    @ResponseBody
    public ApiResponse handleUnauthorizedException(HttpUnauthorizedException exception, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return new ApiResponse(exception);
    }

    @ExceptionHandler(NotFoundApiException.class)
    @ResponseBody
    public ApiResponse handleNotFoundException(NotFoundApiException exception, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return new ApiResponse(false, "Workspace not found");
    }

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ApiResponse handleApiException(ApiException exception, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return new ApiResponse(exception);
    }

}
