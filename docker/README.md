# Subgraph Docker

This is a SpringBoot application wrapping the Apache Jena functionality and extending this with
Lock-Unlock features for authorization.

In this repo there's a GitHub Actions pipeline that builds it automatically and pushes it (from
`main` branch only) to the ~~[GitHub Container
Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)~~
Azure locked (pun intended!) Docker registry.

## Usage

Use the published image by:

```bash
$ docker pull <azure-container-registry>/lock-unlock/rewrite:0.0.1
```

Local build (run in the root of this repo):

```bash
$ docker build --build-arg LOCK_UNLOCK_VERSION=0.0.1 -t lock-unlock/subgraph:0.0.1 -f docker/Dockerfile .
```

Running (just) the docker container:

```bash
$ docker run -i --rm -p "8080:8080" --name LockUnlockSubgraphServer -t lock-unlock/subgraph:0.0.1
```
