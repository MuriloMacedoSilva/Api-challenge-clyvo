# Estágio 1: Compilar a aplicação usando Maven e Java 21
# FROM maven:3.9.6-eclipse-temurin-21 AS build
# COPY . .
# RUN mvn clean package -DskipTests
#
# # Estágio 2: Executar a aplicação usando Java 21 Runtime (JRE)
# FROM eclipse-temurin:21-jre-jammy
# COPY --from=build /target/*.jar app.jar
# EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "app.jar"]


# Estágio 1: Em vez do Maven puro, usamos o container oficial do GraalVM
# Ele tem as ferramentas necessárias para transformar código Java em binário nativo Linux
FROM ghcr.io/graalvm/native-image-community:21 AS build

# Instala o Maven dentro do ambiente do GraalVM
RUN microdnf install -y maven

WORKDIR /app

# Copia todo o seu projeto para dentro do container
COPY . .

# A MUDANÇA ESSENCIAL: O "-Pnative" ativa o plugin do GraalVM que configuramos no pom.xml
# Isso não vai gerar um arquivo .jar, mas sim um arquivo executável binário puro
RUN mvn -Pnative clean package -DskipTests


# Estágio 2: Em vez de usar uma imagem com o Java instalado (JRE),
# usamos uma imagem Linux limpa (Ubuntu), pois o binário nativo roda sozinho!
FROM ubuntu:24.04

WORKDIR /app

# Busca o binário nativo gerado na pasta /target do estágio anterior
# IMPORTANTE: Mude "nome-do-seu-projeto" para o artifactId exato do seu pom.xml (em letras minúsculas)
COPY --from=build /app/target/nome-do-seu-projeto ./app

EXPOSE 8080

# Diferença crucial: Iniciamos o arquivo direto como um programa do sistema,
# sem precisar chamar o comando "java -jar"
ENTRYPOINT ["./app"]
