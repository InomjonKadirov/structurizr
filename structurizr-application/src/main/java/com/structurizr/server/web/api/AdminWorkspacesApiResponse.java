package com.structurizr.server.web.api;

import java.util.List;

/**
 * Fork addition (branch "inam"): the workspace list returned by GET /api/workspace.
 * The shape ({"workspaces": [...]}) matches com.structurizr.api.Workspaces in structurizr-client.
 */
class AdminWorkspacesApiResponse {

    private final List<AdminWorkspaceApiResponse> workspaces;

    AdminWorkspacesApiResponse(List<AdminWorkspaceApiResponse> workspaces) {
        this.workspaces = workspaces;
    }

    public List<AdminWorkspaceApiResponse> getWorkspaces() {
        return workspaces;
    }

}
