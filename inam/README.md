# inam fork of Structurizr

This fork of [structurizr/structurizr](https://github.com/structurizr/structurizr) adds features on top of upstream and keeps pulling upstream changes in.

## Branches

| Branch | Purpose |
|---|---|
| `main` | Exact copy of upstream `main`. Never commit here. |
| `inam` | Default branch: upstream plus the changes listed below. |

Upstream changes come in through `.github/workflows/inam-upstream-sync.yml`. Every Monday, or when you run it by hand, it fast-forwards `main` from upstream and opens a pull request `main → inam`. Review it and **merge** it. Do not rebase: `inam` is shared and deployed.

The sync works best with a repository secret `UPSTREAM_SYNC_TOKEN`, a personal access token with the `repo` and `workflow` scopes. Without the `workflow` scope, the sync fails whenever upstream edits its workflows. Pull requests opened with the default token also don't trigger CI.

To keep merges painless, changes are made as **new files** wherever possible. Upstream files are left alone.

## What the fork adds

- **Admin API** (`structurizr-application/.../server/web/api/AdminApiController.java`):
  - `GET /api/workspace` lists workspaces.
  - `POST /api/workspace` creates a workspace and returns its API key once.
  - `DELETE /api/workspace/{id}` deletes a workspace.

  It follows the contract of `AdminApiClient`, so the stock `create`, `pull` (without `-id`) and `delete` commands work. Because the class exists, the server no longer starts in open-core mode.

  Authorisation works like the rest of the API:
  - With `structurizr.authentication=none`, no key is needed.
  - Otherwise the `X-Authorization` header must match the bcrypt hash in `structurizr.apikey`.
- **Build from source**: `inam/Dockerfile`, `inam/build.sh` and `inam/VERSION`.
- **Local server**: `inam/run-server.sh`.
- **CI**: `.github/workflows/inam-ci.yml`.

## Version

`inam/VERSION` holds the one version, `<date of the upstream commit it is based on>-inam.<n>`. That value is the app version shown in the startup banner (passed as `-Dapp.revision`) and the Docker image tag.

- After merging an upstream sync, set the date to the new upstream commit's date and reset `n` to 1.
- After a change of your own, increase `n`.

## Build and run (this machine)

```bash
inam/build.sh
inam/run-server.sh       # http://localhost:8081, data in ~/structurizr-data
```

The server listens on `127.0.0.1` only. Open-core has no login (`authentication none`), so it must not be reachable from the network until authentication is added.

Admin API examples. They run the CLI from the same image; on Docker Desktop a container reaches the host's server through `host.docker.internal`:

```bash
IMG=structurizr-inam:$(cat inam/VERSION); URL=http://host.docker.internal:8081/api
docker run --rm "$IMG" create -url "$URL"                                   # prints the new ID and API key
docker run --rm -v "$PWD":/work -w /work "$IMG" push -url "$URL" -id 1 -w workspace.dsl
docker run --rm -v "$PWD":/work -w /work "$IMG" pull -url "$URL" -o workspaces   # no -id: every workspace
docker run --rm "$IMG" delete -url "$URL" -id 1 -f                          # -f skips the y/n prompt
```
