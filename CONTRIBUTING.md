# Contributing to Fault Patrol Agent

Thank you for your interest in contributing to **Fault Patrol Agent**! We welcome contributions of all kinds: bug reports, feature suggestions, documentation enhancements, and pull requests.

## Code of Conduct

Please maintain a friendly, inclusive, and professional environment. Treat everyone with respect and constructive feedback.

## How to Contribute

### 1. Reporting Bugs & Proposing Features
- Search existing [Issues](../../issues) to check if your issue or idea has already been reported.
- If not, create a new issue using our issue templates.
- Please provide as much detail as possible: JDK version, environment, reproduction steps, error stack traces, and relevant logs.

### 2. Development Workflow

1. **Fork the Repository**:
   Fork `fault-patrol-agent` to your own GitHub account.

2. **Clone & Branch**:
   ```bash
   git clone https://github.com/your-username/fault-patrol-agent.git
   cd fault-patrol-agent
   git checkout -b feature/your-feature-name
   ```

3. **Development Guidelines**:
   - **Java / JDK**: Use JDK 17+.
   - **Architecture**: Adhere to Domain-Driven Design (DDD) module boundaries:
     - `fault-patrol-agent-api`: Pure DTOs and API interfaces. Zero domain logic.
     - `fault-patrol-agent-types`: Common utilities, design pattern abstractions, and enums.
     - `fault-patrol-agent-domain`: Core business domain logic, entities, value objects, and service interfaces.
     - `fault-patrol-agent-infrastructure`: Database POs, MyBatis DAOs, repository implementations, external adapters.
     - `fault-patrol-agent-trigger`: Webhook adapters, HTTP REST/SSE controllers, cron jobs.
     - `fault-patrol-agent-mcp-server`: MCP server tools (ensure all inspection tools remain strictly **read-only** and safe).
   - **Safety First**: Any tool or probe running in external environments must be read-only or gated by the Human-In-The-Loop (HITL) approval workflow.

4. **Verify Tests & Build**:
   Ensure all unit tests pass before submitting code:
   ```bash
   mvn clean test
   ```

5. **Commit Message Conventions**:
   Follow the conventional commit format:
   - `feat`: A new feature or capability
   - `fix`: A bug fix
   - `docs`: Documentation changes
   - `refactor`: Code refactoring without changing functionality
   - `test`: Adding or updating tests
   - `chore`: Build or dependency maintenance

6. **Submit a Pull Request**:
   - Push your branch to GitHub.
   - Open a Pull Request against `main`.
   - Provide a concise summary of the changes and link any related issues.

## License

By contributing code to Fault Patrol Agent, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
