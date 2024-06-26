#!/usr/bin/env zsh

# Set variables
USER_HOME="${HOME}"
PROTOC_PATH="${USER_HOME}/.grpc-starter/protoc-$2"
PROTOC_GEN_GRPC_JAVA_PATH="${USER_HOME}/.grpc-starter/protoc-gen-grpc-java-$3"

if [ ! -f "${PROTOC_PATH}" ]; then
    curl --create-dirs -o "${PROTOC_PATH}" -L "https://repo1.maven.org/maven2/com/google/protobuf/protoc/$2/protoc-$2-osx-aarch_64.exe"
    chmod +x "${PROTOC_PATH}"
fi

if [ ! -f "${PROTOC_GEN_GRPC_JAVA_PATH}" ]; then
    curl --create-dirs -o "${PROTOC_GEN_GRPC_JAVA_PATH}" -L "https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/$3/protoc-gen-grpc-java-$3-osx-aarch_64.exe"
    chmod +x "${PROTOC_GEN_GRPC_JAVA_PATH}"
fi

# Create directory if not exists
mkdir -p "$1/generated/java"

find "$1/extracted-include-protos/main" -type f -path '*/user/*' -print0 | xargs -0 "${PROTOC_PATH}" --proto_path="$1/extracted-include-protos/main" --java_out="$1/generated/java" --grpc-java_out="$1/generated/java" --plugin=protoc-gen-grpc-java="${PROTOC_GEN_GRPC_JAVA_PATH}"
