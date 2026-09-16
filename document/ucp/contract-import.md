# UCP Phase 0 — review delivery 1 of 2

This delivery contains discovery-only runtime support and a partial, test-only
import of common type schemas. No commerce capability is implemented or advertised.

## Contract source

- Repository: https://github.com/Universal-Commerce-Protocol/ucp
- Release: `v2026-08-25`
- Commit: `cd78fb38e819de77d9b527d110476eccb876f1bd`
- Imported common types preserve upstream bytes from `source/schemas/common/types/`.
- Upstream LICENSE accompanies the test resources.

This partial import is not a closed schema bundle and is not used by the runtime
validator. Full manifest/hash/reference-closure tests, remaining schemas, REST
service descriptions, released scaffolds, the capability matrix, and verification
scripts under `scripts/ucp/` are delivered in review 2 after review 1 is merged.
Phase 0 delivery must not be considered complete until that second PR is merged.

The split is only for the review tool's changed-file limit, not a protocol change.
