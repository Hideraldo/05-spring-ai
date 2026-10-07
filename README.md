# 💰 Assistente Financeiro Inteligente com Spring AI

Projeto prático desenvolvido para o **Desafio de Projeto: Spring AI** da trilha *Spring Boot* da Digital Innovation One (DIO).

A aplicação consiste em uma API de gestão financeira pessoal (*budgeting*) com suporte multimodal (áudio e texto), integrando recursos de Inteligência Artificial Generativa com uma **arquitetura limpa em camadas (DDD)**.

---

## 📌 O que o projeto faz

A aplicação permite registrar e consultar despesas financeiras tanto via endpoints REST convencionais quanto por comandos de voz processados por IA:

1. **Entrada por Voz (Speech-to-Text)**: O usuário envia um áudio (ex.: *"Gastei 50 reais no mercado"* ou *"Quanto gastei na farmácia?"*).
2. **Transcrição de Áudio**: O modelo OpenAI Whisper transcreve o áudio em texto em português.
3. **Interpretação e Decisão Autônoma**: O Spring AI `ChatClient` analisa a intenção usando um prompt de sistema especializado.
4. **Tool Calling (Function Calling)**: A IA invoca automaticamente métodos de casos de uso da aplicação em Java para:
   - Cadastrar uma nova despesa (`persist-transaction`);
   - Listar despesas por categoria (`list-transactions-by-category`);
   - **Calcular o total de despesas por categoria (`get-total-spend-by-category`)** *(Melhoria implementada)*.
5. **Persistência de Dados**: Os dados são salvos e consultados de forma segura em um banco de dados MySQL via Spring Data JPA.
6. **Resposta em Voz (Text-to-Speech)**: A resposta textual gerada pela IA é convertida em um áudio MP3 retornado ao cliente.

---

## 🚀 Qual melhoria foi implementada

### 💡 Ideia Escolhida: Novas Ferramentas para Tool Calling (`get-total-spend-by-category`)

- **Problema identificado**: A aplicação original só permitia criar transações e listar transações individuais. Quando o usuário perguntava por voz *"Quanto eu já gastei no mercado?"*, o modelo precisava listar todos os itens e tentar somar os valores textualmente, o que frequentemente causa alucinações ou imprecisão matemática em LLMs.
- **Solução implementada**: Criamos uma nova ferramenta (`@Tool`) que delega o cálculo do somatório diretamente para a aplicação e para o banco de dados via query agregada (`SUM(amount)`).

### 📂 Arquivos modificados e criados:

1. **Domínio (`domain`)**:
   - `TransactionRepository.java`: adicionada a assinatura `long sumAmountByCategory(Category category)`.
2. **Infraestrutura de Persistência (`infrastructure/persistence`)**:
   - `TransactionEntityRepository.java`: adicionada a consulta JPQL `@Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.category = :category")`.
   - `JpaTransactionRepository.java`: implementado o método chamando o repositório JPA.
3. **Aplicação (`application`)**:
   - `GetTotalSpendByCategoryUseCase.java`: novo caso de uso anotado com `@Tool(name = "get-total-spend-by-category", description = "Calcula e retorna a soma total dos gastos para uma determinada categoria")`.
4. **Infraestrutura HTTP & Prompts (`infrastructure/http` e `resources`)**:
   - `TransactionController.java`: registrado o novo caso de uso no `chatClientBuilder.defaultTools(...)` e adicionado o endpoint REST `GET /transactions/{category}/total`.
   - `prompts/system-message.st`: prompt de sistema atualizado instruindo o assistente a acionar a nova ferramenta de cálculo quando o usuário solicitar o total gasto.

---

## 🛠️ Tecnologias Utilizadas

- **Java 25**
- **Spring Boot 4.0.5**
- **Spring AI 2.0.0-M4**
  - `ChatClient` com OpenAI GPT-4o-mini
  - `TranscriptionModel` com OpenAI Whisper (`whisper-1`)
  - `TextToSpeechModel` com OpenAI TTS (`gpt-4o-mini-tts`)
  - **Tool Calling (`@Tool`, `@ToolParam`)**
- **Spring Data JPA & Hibernate**
- **MySQL 9.6** (via Docker Compose integrado ao Spring Boot)
- **Lombok**
- **Gradle**

---

## ⚙️ Como Executar a Aplicação

### 1. Pré-requisitos
- **Java 25** (JDK 25) instalado e configurado no `JAVA_HOME`.
- **Docker & Docker Compose** em execução (para o container do MySQL).
- Chave de API da OpenAI (`OPENAI_API_KEY`).

### 2. Configurar a variável de ambiente

No terminal (PowerShell no Windows):
```powershell
$env:OPENAI_API_KEY="sua_chave_openai_aqui"
```

No Linux / macOS:
```bash
export OPENAI_API_KEY="sua_chave_openai_aqui"
```

### 3. Executar o projeto

Na pasta `05-spring-ai`:

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

> Graças ao módulo `spring-boot-docker-compose`, o container do MySQL (`compose.yml`) é iniciado e provisionado automaticamente durante o boot da aplicação.

---

## 🧪 Como Testar o Fluxo Principal

### 1. Cadastrar uma transação via REST
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Supermercado Semanal",
    "amount": 12550,
    "category": "GROCERIES"
  }'
```

### 2. Consultar o total gasto por categoria via REST (Nova funcionalidade)
```bash
curl -X GET http://localhost:8080/transactions/GROCERIES/total
```
*Retorno esperado:*
```json
125.5
```

### 3. Testar o fluxo multimodal por IA (Áudio para Áudio)

Envie um arquivo de áudio gravado com a sua voz (ou gerado) contendo:
> *"Gastei 45 reais na farmácia"* ou *"Quanto eu gastei no total em mercado?"*

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@caminho/para/pergunta.mp3" \
  --output resposta.mp3
```

Ao reproduzir o arquivo `resposta.mp3`, a IA responderá por voz com a confirmação exata da transação persistida ou com o total acumulado calculado pelo use case.

---

## 🧠 O que Aprendi Durante o Desafio

- **Spring AI na Prática**: Como configurar e unificar múltiplos recursos de IA generativa (Chat, Transcrição de Áudio e Text-to-Speech) em uma única aplicação Spring Boot de forma idiomática.
- **Poder do Tool Calling**: Compreensão de como LLMs interagem com sistemas legados e bancos relacionais através do fornecimento de contratos de ferramentas seguras (`@Tool`).
- **Arquitetura Limpa e DDD**: A importância de manter regras de negócio e contratos de repositório na camada de domínio, usando os recursos do Spring AI apenas como adaptadores de infraestrutura e aplicação.
- **Tratamento de Áudio**: Uso do modelo Whisper para transcrição com dicionário em português e do modelo TTS para retorno de áudio dinâmico.
