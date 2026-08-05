# Contributing

Changes are reviewed in the public repository and then applied to the private authoritative source before the next deterministic export. A public pull request is not considered released until the corresponding private-source build, export checks, and public cold build pass.

Pull request CI checks the public file boundary, license, credentials, private references, and standalone build. Release metadata and source digest are verified only for synchronized `main` snapshots because contributors cannot author private source commit or Jenkins build metadata.

Do not include credentials, private URLs, private SDK source, generated build output, Sample applications, or Demo applications.
