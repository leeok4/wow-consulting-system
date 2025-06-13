# Wow Consulting System - Backend

Este é o backend do sistema de agendamento da Wow Consulting, desenvolvido em Java com Spring Boot.

## Requisitos

- Java 17+
- Maven 3.8+

## Como rodar localmente

1. Instale as dependências:
   ```sh
   mvn clean install
   ```
2. Execute a aplicação:
   ```sh
   mvn spring-boot:run
   ```
   Ou rode o JAR gerado em `target/`:
   ```sh
   java -jar target/scheduling-system-0.0.1-SNAPSHOT.jar
   ```

## Testes

Para rodar os testes unitários e de integração:

```sh
mvn test
```

## Estrutura de pastas

- `controller/` - Controllers REST
- `service/` - Lógica de negócio
- `repository/` - Repositórios JPA
- `model/` - Entidades
- `dto/` - Data Transfer Objects
- `util/` - Utilitários

## Variáveis de ambiente

Configure as variáveis no arquivo `src/main/resources/application.yml`.

## Contribuição

1. Crie um fork do projeto
2. Crie uma branch (`git checkout -b feature/nome-feature`)
3. Faça commit das suas alterações
4. Envie um Pull Request

## Licença

MIT
