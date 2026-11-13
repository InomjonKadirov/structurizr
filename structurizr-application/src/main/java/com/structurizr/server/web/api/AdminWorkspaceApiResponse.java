package com.structurizr.server.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.structurizr.server.domain.WorkspaceMetadata;

/**
 * Fork addition (branch "inam"): one workspace as returned by the admin API.
 * Field names match com.structurizr.api.WorkspaceMetadata in structurizr-client.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class AdminWorkspaceApiResponse {

    private final long id;
    private final String name;
    private final String description;
    private final String apiKey;
    private final String privateUrl;
    private final String publicUrl;

    /**
     * @param workspaceMetadata     the workspace
     * @param apiKey                the plaintext API key, only known when it has just been issued (otherwise null)
     */
    AdminWorkspaceApiResponse(WorkspaceMetadata workspaceMetadata, String apiKey) {
        this.id = workspaceMetadata.getId();
        this.name = workspaceMetadata.getName();
        this.description = workspaceMetadata.getDescription();
        this.apiKey = apiKey;
        this.privateUrl = "/workspace/" + workspaceMetadata.getId();
        this.publicUrl = workspaceMetadata.isPublicWorkspace() ? "/share/" + workspaceMetadata.getId() : null;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getPrivateUrl() {
        return privateUrl;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

}
