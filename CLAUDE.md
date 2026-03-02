# CLAUDE.md

This file provides guidance for AI assistants (Claude and others) working in this repository. It documents the project structure, development conventions, and workflows to follow.

---

## Project Overview

**Repository:** `punitkumar04/ClaudeCodeApplication`
**Purpose:** This is a new, empty repository. Update this section as the project takes shape to describe what the application does, its main features, and its target users.

---

## Repository Status

This repository was initialized with no source code. All conventions and structures described below should be applied as development begins.

---

## Development Workflow

### Branching Strategy

- **Main branch:** `main` (or `master`) — protected, always deployable
- **Feature branches:** prefix with `feature/` (e.g., `feature/user-auth`)
- **Bug fix branches:** prefix with `fix/` (e.g., `fix/login-redirect`)
- **Claude-managed branches:** prefix with `claude/` (e.g., `claude/task-description-sessionid`)

Never push directly to `main`. All changes go through pull requests.

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

**Types:**
- `feat` — new feature
- `fix` — bug fix
- `docs` — documentation only
- `style` — formatting, no logic change
- `refactor` — code restructuring, no behavior change
- `test` — adding or fixing tests
- `chore` — tooling, dependencies, config

**Examples:**
```
feat(auth): add JWT-based login endpoint
fix(api): handle null response from user service
docs: update README with setup instructions
```

### Pull Requests

- Keep PRs small and focused on a single concern
- Include a clear description of what changed and why
- Reference any related issues (e.g., `Closes #42`)
- Ensure all CI checks pass before merging

---

## Project Structure

> Update this section as the project grows. Below is a recommended structure to follow:

```
ClaudeCodeApplication/
├── CLAUDE.md              # This file — AI assistant guidance
├── README.md              # Human-facing project documentation
├── .gitignore             # Git ignore rules
├── package.json           # (if Node.js) Dependencies and scripts
├── tsconfig.json          # (if TypeScript) Compiler configuration
├── .eslintrc.*            # Linting configuration
├── .prettierrc            # Code formatting configuration
├── src/                   # Application source code
│   ├── index.ts           # Application entry point
│   ├── components/        # UI components (if frontend)
│   ├── services/          # Business logic / service layer
│   ├── controllers/       # Route handlers / API controllers
│   ├── models/            # Data models / schemas
│   ├── utils/             # Shared utility functions
│   └── types/             # TypeScript type definitions
├── tests/                 # Test files (mirror src/ structure)
│   ├── unit/
│   └── integration/
├── docs/                  # Extended documentation
└── scripts/               # Build, deploy, and utility scripts
```

---

## Code Style and Conventions

### General Rules

- Prefer **clarity over cleverness** — code is read far more than it is written
- Keep functions small and focused on a single responsibility
- Avoid deep nesting; prefer early returns
- Do not add comments for self-evident code; comment only non-obvious logic
- Do not over-engineer: avoid abstractions, helpers, or utilities for one-time use
- Do not add error handling for scenarios that cannot happen; validate only at system boundaries

### Naming Conventions

| Context | Convention | Example |
|---|---|---|
| Variables / functions | `camelCase` | `getUserById` |
| Classes / interfaces | `PascalCase` | `UserService` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Files (JS/TS) | `kebab-case` | `user-service.ts` |
| Database tables | `snake_case` | `user_accounts` |
| CSS classes | `kebab-case` | `nav-container` |

### TypeScript (if applicable)

- Enable `strict` mode in `tsconfig.json`
- Prefer `interface` over `type` for object shapes
- Avoid `any`; use `unknown` when type is truly unknown
- Export types explicitly; do not re-export unless necessary
- Colocate types with the code that uses them; put shared types in `src/types/`

### Imports

- Use absolute imports over deep relative paths (configure with `tsconfig paths` or module aliases)
- Group imports: external packages first, then internal modules, then local files
- Remove unused imports

---

## Testing

> Update this section once a test framework is chosen.

### Recommended Conventions

- Mirror the `src/` structure inside `tests/`
- Unit tests: test individual functions/classes in isolation
- Integration tests: test interactions between modules or with external services
- Test file naming: `<module-name>.test.ts` (or `.spec.ts`)
- Aim for meaningful coverage on business logic; do not test implementation details

### Running Tests

```bash
# Once configured, document commands here
npm test            # Run all tests
npm run test:unit   # Run unit tests only
npm run test:watch  # Watch mode
```

---

## Environment and Configuration

- Store secrets and environment-specific values in `.env` files
- Never commit `.env` files — add them to `.gitignore`
- Provide a `.env.example` file listing all required variables with placeholder values
- Access environment variables only through a central config module (not scattered `process.env` calls)

**Example `.env.example`:**
```
DATABASE_URL=postgres://user:password@localhost:5432/dbname
API_KEY=your_api_key_here
NODE_ENV=development
PORT=3000
```

---

## Dependency Management

- Pin dependency versions in `package.json` (avoid `^` or `~` for production deps when stability matters)
- Review and audit dependencies before adding new ones (`npm audit`)
- Remove unused dependencies promptly
- Keep `devDependencies` and `dependencies` properly separated

---

## Security

- Never hardcode secrets, API keys, or credentials in source code
- Validate and sanitize all user input at system boundaries
- Avoid `eval()`, dynamic `require()`, or other code injection surfaces
- Follow the OWASP Top 10 as a baseline security checklist
- Use parameterized queries for all database interactions (never string concatenation for SQL)
- Set appropriate HTTP security headers (use a library like `helmet` for Express apps)

---

## AI Assistant Instructions

When working in this repository, Claude and other AI assistants should:

1. **Read before editing** — always read a file before modifying it
2. **Stay focused** — only make changes directly related to the task; do not refactor surrounding code
3. **No unnecessary files** — do not create documentation, README files, or boilerplate unless explicitly asked
4. **No over-engineering** — do not add abstraction layers, helpers, or utilities for one-time use
5. **No comments for obvious code** — only comment non-obvious logic
6. **Use the right branch** — develop on the designated `claude/` branch; never push to `main`
7. **Commit clearly** — use Conventional Commits format with a descriptive message
8. **Ask before destructive actions** — confirm before deleting files, dropping data, force-pushing, or modifying CI/CD
9. **Security first** — never introduce SQL injection, XSS, command injection, or other OWASP vulnerabilities
10. **Update this file** — as the project evolves, keep `CLAUDE.md` current with new conventions, scripts, and structure

---

## Common Tasks

> Populate this section with actual commands once the project is set up.

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Run linter
npm run lint

# Run formatter
npm run format

# Run tests
npm test
```

---

## Getting Help

- File issues at the repository's issue tracker
- For Claude Code CLI questions: https://github.com/anthropics/claude-code/issues
- Refer to project documentation in the `docs/` directory (once created)
