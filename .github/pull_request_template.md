## Ticket

[PLS-XXX] - <Ticket Title>  <!-- also use this as the PR title -->

## What changed

<!-- One or two sentences. What does the API do now that it did not before? -->

## Why this way

<!-- Any decision a reviewer might question: locking strategy, status codes, schema choice. -->

## How I tested it

- [ ] Unit / integration tests added or updated
- [ ] Tried it manually (paste the curl or Swagger request and the response below)

```
curl ...
```

## Checklist

- [ ] One ticket per PR
- [ ] No secrets, `.env` or credentials committed
- [ ] Controllers return DTOs, not entities
- [ ] Errors go through the global exception handler
