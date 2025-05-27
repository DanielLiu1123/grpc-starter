package grpcstarter.example;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import user.v1.BatchCreateUsersRequest;
import user.v1.BatchCreateUsersResponse;
import user.v1.CreateUserRequest;
import user.v1.CreateUserResponse;
import user.v1.DeleteUserRequest;
import user.v1.GetUserRequest;
import user.v1.GetUserResponse;
import user.v1.ListUsersRequest;
import user.v1.ListUsersResponse;
import user.v1.PatchUserRequest;
import user.v1.PatchUserResponse;
import user.v1.SearchUsersRequest;
import user.v1.SearchUsersResponse;
import user.v1.UpdateUserRequest;
import user.v1.UpdateUserResponse;
import user.v1.User;
import user.v1.UserServiceGrpc;

/**
 * gRPC service implementation for User management
 * Demonstrates integration between gRPC services and REST endpoints
 */
@Slf4j
@Service
public class UserServiceServer extends UserServiceGrpc.UserServiceImplBase {

    // In-memory storage for demo purposes
    private final Map<String, User> userStorage = new ConcurrentHashMap<>();

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        log.info("Creating user: {}", request.getUser().getUsername());

        try {
            User user = request.getUser().toBuilder()
                    .setUserId(UUID.randomUUID().toString())
                    .build();

            userStorage.put(user.getUserId(), user);

            CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setUser(user)
                    .setMessage("User created successfully via gRPC")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error creating user", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        log.info("Getting user: {}", request.getUserId());

        try {
            User user = userStorage.get(request.getUserId());

            if (user == null) {
                // Create a mock user if not found
                user = User.newBuilder()
                        .setUserId(request.getUserId())
                        .setUsername("mock_user")
                        .setEmail("mock@example.com")
                        .setFirstName("Mock")
                        .setLastName("User")
                        .setGender(User.Gender.MALE)
                        .setAge(25)
                        .setStatus(User.UserStatus.ACTIVE)
                        .build();
            }

            GetUserResponse response =
                    GetUserResponse.newBuilder().setUser(user).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting user", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
        log.info("Updating user: {}", request.getUserId());

        try {
            User updatedUser =
                    request.getUser().toBuilder().setUserId(request.getUserId()).build();

            userStorage.put(request.getUserId(), updatedUser);

            UpdateUserResponse response = UpdateUserResponse.newBuilder()
                    .setUser(updatedUser)
                    .setMessage("User updated successfully via gRPC")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error updating user", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void patchUser(PatchUserRequest request, StreamObserver<PatchUserResponse> responseObserver) {
        log.info("Patching user: {}", request.getUserId());

        try {
            User existingUser = userStorage.get(request.getUserId());
            User.Builder userBuilder;

            if (existingUser != null) {
                userBuilder = existingUser.toBuilder();
            } else {
                userBuilder = User.newBuilder().setUserId(request.getUserId());
            }

            // Apply partial updates
            if (!request.getUsername().isEmpty()) {
                userBuilder.setUsername(request.getUsername());
            }
            if (!request.getEmail().isEmpty()) {
                userBuilder.setEmail(request.getEmail());
            }
            if (!request.getFirstName().isEmpty()) {
                userBuilder.setFirstName(request.getFirstName());
            }
            if (!request.getLastName().isEmpty()) {
                userBuilder.setLastName(request.getLastName());
            }
            if (request.getGender() != User.Gender.GENDER_UNSPECIFIED) {
                userBuilder.setGender(request.getGender());
            }
            if (request.getAge() > 0) {
                userBuilder.setAge(request.getAge());
            }
            if (request.getStatus() != User.UserStatus.STATUS_UNSPECIFIED) {
                userBuilder.setStatus(request.getStatus());
            }

            User patchedUser = userBuilder.build();
            userStorage.put(request.getUserId(), patchedUser);

            PatchUserResponse response = PatchUserResponse.newBuilder()
                    .setUser(patchedUser)
                    .setMessage("User patched successfully via gRPC")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error patching user", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
        log.info("Deleting user: {}, soft delete: {}", request.getUserId(), request.getSoftDelete());

        try {
            if (request.getSoftDelete()) {
                // Soft delete - mark as deleted
                User existingUser = userStorage.get(request.getUserId());
                if (existingUser != null) {
                    User deletedUser = existingUser.toBuilder()
                            .setStatus(User.UserStatus.DELETED)
                            .build();
                    userStorage.put(request.getUserId(), deletedUser);
                }
            } else {
                // Hard delete - remove from storage
                userStorage.remove(request.getUserId());
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error deleting user", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        log.info("Listing users with page size: {}", request.getPageSize());

        try {
            // Mock implementation - return stored users
            ListUsersResponse.Builder responseBuilder = ListUsersResponse.newBuilder();

            userStorage.values().stream()
                    .filter(user -> {
                        // Apply status filter if specified
                        if (request.getStatusFilter() != User.UserStatus.STATUS_UNSPECIFIED) {
                            return user.getStatus() == request.getStatusFilter();
                        }
                        return true;
                    })
                    .filter(user -> {
                        // Apply gender filter if specified
                        if (request.getGenderFilter() != User.Gender.GENDER_UNSPECIFIED) {
                            return user.getGender() == request.getGenderFilter();
                        }
                        return true;
                    })
                    .limit(request.getPageSize() > 0 ? request.getPageSize() : 10)
                    .forEach(responseBuilder::addUsers);

            responseBuilder.setTotalCount(userStorage.size());
            responseBuilder.setNextPageToken("next_page_token");

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error listing users", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void searchUsers(SearchUsersRequest request, StreamObserver<SearchUsersResponse> responseObserver) {
        log.info("Searching users with query: {}", request.getQuery());

        try {
            SearchUsersResponse.Builder responseBuilder = SearchUsersResponse.newBuilder();

            // Mock search implementation
            userStorage.values().stream()
                    .filter(user -> {
                        // Simple text search
                        String query = request.getQuery().toLowerCase();
                        return user.getUsername().toLowerCase().contains(query)
                                || user.getEmail().toLowerCase().contains(query)
                                || user.getFirstName().toLowerCase().contains(query)
                                || user.getLastName().toLowerCase().contains(query);
                    })
                    .limit(request.getLimit() > 0 ? request.getLimit() : 10)
                    .forEach(responseBuilder::addUsers);

            // Mock facets
            Map<String, Integer> facets = new HashMap<>();
            facets.put("gender_male", 5);
            facets.put("gender_female", 3);
            facets.put("status_active", 7);
            facets.put("status_inactive", 1);

            responseBuilder.putAllFacets(facets);
            responseBuilder.setTotalCount(responseBuilder.getUsersCount());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error searching users", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void batchCreateUsers(
            BatchCreateUsersRequest request, StreamObserver<BatchCreateUsersResponse> responseObserver) {
        log.info("Batch creating {} users", request.getUsersCount());

        try {
            BatchCreateUsersResponse.Builder responseBuilder = BatchCreateUsersResponse.newBuilder();
            int successCount = 0;
            int failureCount = 0;

            for (User user : request.getUsersList()) {
                try {
                    User createdUser = user.toBuilder()
                            .setUserId(UUID.randomUUID().toString())
                            .build();

                    userStorage.put(createdUser.getUserId(), createdUser);
                    responseBuilder.addCreatedUsers(createdUser);
                    successCount++;

                } catch (Exception e) {
                    responseBuilder.addFailedUserIds(user.getUserId());
                    failureCount++;
                }
            }

            responseBuilder.setSuccessCount(successCount);
            responseBuilder.setFailureCount(failureCount);
            responseBuilder.setMessage(
                    String.format("Batch operation completed: %d success, %d failures", successCount, failureCount));

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in batch create users", e);
            responseObserver.onError(e);
        }
    }
}
