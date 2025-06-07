package grpcstarter.example;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import user.v1.BatchCreateUsersRequest;
import user.v1.BatchCreateUsersResponse;
import user.v1.CreateUserRequest;
import user.v1.CreateUserResponse;
import user.v1.GetUserResponse;
import user.v1.ListUsersResponse;
import user.v1.PatchUserRequest;
import user.v1.PatchUserResponse;
import user.v1.SearchUsersRequest;
import user.v1.SearchUsersResponse;
import user.v1.UpdateUserResponse;
import user.v1.User;

/**
 * REST Controller demonstrating Spring Boot + gRPC + OpenAPI integration
 * with comprehensive test cases for mixed Java and Protobuf types
 */
@RestController
@Tag(name = "User Management", description = "APIs for user management and mixed type testing")
public class UserController {

    // ========== Core User CRUD Operations ==========

    @PostMapping("/rest/v1/users")
    @Operation(summary = "Create a new user", description = "Creates a new user with the provided information")
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {
        User user = request.getUser().toBuilder()
                .setUserId(UUID.randomUUID().toString())
                .build();

        return CreateUserResponse.newBuilder()
                .setUser(user)
                .setMessage("User created successfully")
                .build();
    }

    @GetMapping("/rest/v1/users/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier")
    public GetUserResponse getUser(@PathVariable String userId, @RequestParam(required = false) List<String> fields) {
        // Mock user data
        User user = User.newBuilder()
                .setUserId(userId)
                .setUsername("john_doe")
                .setEmail("john@example.com")
                .setFirstName("John")
                .setLastName("Doe")
                .setGender(User.Gender.MALE)
                .setAge(30)
                .setStatus(User.UserStatus.ACTIVE)
                .build();

        return GetUserResponse.newBuilder().setUser(user).build();
    }

    @PutMapping("/rest/v1/users/{userId}")
    @Operation(summary = "Update user", description = "Updates an existing user with new information")
    public UpdateUserResponse updateUser(@PathVariable String userId, @RequestBody User user) {
        User updatedUser = user.toBuilder().setUserId(userId).build();

        return UpdateUserResponse.newBuilder()
                .setUser(updatedUser)
                .setMessage("User updated successfully")
                .build();
    }

    @PatchMapping("/rest/v1/users/{userId}")
    @Operation(summary = "Partially update user", description = "Updates specific fields of an existing user")
    public PatchUserResponse patchUser(@PathVariable String userId, @RequestBody PatchUserRequest request) {
        // Mock updated user
        User user = User.newBuilder()
                .setUserId(userId)
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setGender(request.getGender())
                .setAge(request.getAge())
                .setStatus(request.getStatus())
                .build();

        return PatchUserResponse.newBuilder()
                .setUser(user)
                .setMessage("User partially updated successfully")
                .build();
    }

    @DeleteMapping("/rest/v1/users/{userId}")
    @Operation(summary = "Delete user", description = "Deletes a user by their unique identifier")
    public void deleteUser(@PathVariable String userId, @RequestParam(defaultValue = "true") boolean softDelete) {
        // Mock deletion logic
    }

    @GetMapping("/rest/v1/users")
    @Operation(summary = "List users", description = "Retrieves a paginated list of users with optional filtering")
    public ListUsersResponse listUsers(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) User.UserStatus statusFilter,
            @RequestParam(required = false) User.Gender genderFilter,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false) List<String> sortBy) {
        // Mock user list
        List<User> users = new ArrayList<>();
        users.add(User.newBuilder()
                .setUserId("user1")
                .setUsername("alice")
                .setEmail("alice@example.com")
                .setFirstName("Alice")
                .setLastName("Smith")
                .setGender(User.Gender.FEMALE)
                .setAge(25)
                .setStatus(User.UserStatus.ACTIVE)
                .build());

        return ListUsersResponse.newBuilder()
                .addAllUsers(users)
                .setNextPageToken("next_page_token")
                .setTotalCount(1)
                .build();
    }

    @PostMapping("/rest/v1/users/search")
    @Operation(summary = "Search users", description = "Searches users based on complex criteria")
    public SearchUsersResponse searchUsers(@RequestBody SearchUsersRequest request) {
        // Mock search results
        List<User> users = new ArrayList<>();
        Map<String, Integer> facets = new HashMap<>();
        facets.put("gender_male", 10);
        facets.put("gender_female", 8);

        return SearchUsersResponse.newBuilder()
                .addAllUsers(users)
                .setTotalCount(0)
                .putAllFacets(facets)
                .build();
    }

    @PostMapping("/rest/v1/users/batch")
    @Operation(summary = "Batch create users", description = "Creates multiple users in a single operation")
    public BatchCreateUsersResponse batchCreateUsers(@RequestBody BatchCreateUsersRequest request) {
        List<User> createdUsers = new ArrayList<>();
        List<String> failedUserIds = new ArrayList<>();

        return BatchCreateUsersResponse.newBuilder()
                .addAllCreatedUsers(createdUsers)
                .addAllFailedUserIds(failedUserIds)
                .setMessage("Batch operation completed")
                .setSuccessCount(0)
                .setFailureCount(0)
                .build();
    }

    // ========== Java Bean with nested protobuf message test cases ==========

    @PostMapping("/rest/test/user-wrapper")
    @Operation(
            summary = "Test Java Bean with nested protobuf User",
            description = "Tests Java POJO containing protobuf User message")
    public UserWrapper createUserWrapper(@RequestBody UserWrapper userWrapper) {
        return userWrapper.toBuilder().createdAt(LocalDateTime.now()).build();
    }

    @PostMapping("/rest/test/contact-info")
    @Operation(
            summary = "Test Java Bean with nested protobuf PhoneNumber",
            description = "Tests Java POJO containing protobuf PhoneNumber nested message")
    public ContactInfo createContactInfo(@RequestBody ContactInfo contactInfo) {
        return contactInfo.toBuilder()
                .contactId(UUID.randomUUID().toString())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @PutMapping("/rest/test/location/{locationId}")
    @Operation(
            summary = "Test Java Bean with nested protobuf Address",
            description = "Tests Java POJO containing protobuf Address nested message")
    public LocationInfo updateLocation(@PathVariable String locationId, @RequestBody LocationInfo locationInfo) {
        return locationInfo.toBuilder()
                .locationId(locationId)
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    // ========== Java Bean with nested protobuf enum test cases ==========

    @PostMapping("/rest/test/user-profile")
    @Operation(
            summary = "Test Java Bean with protobuf enums",
            description = "Tests Java POJO containing various protobuf enum types")
    public UserProfile createUserProfile(@RequestBody UserProfile userProfile) {
        return userProfile.toBuilder().profileId(UUID.randomUUID().toString()).build();
    }

    @GetMapping("/rest/test/user-profile/{profileId}")
    @Operation(
            summary = "Get user profile with enum filtering",
            description = "Tests GET endpoint with protobuf enum query parameters")
    public UserProfile getUserProfile(
            @PathVariable String profileId,
            @RequestParam(required = false) User.Gender filterGender,
            @RequestParam(required = false) User.UserStatus filterStatus,
            @RequestParam(required = false) List<User.Theme> themes) {
        return UserProfile.builder()
                .profileId(profileId)
                .gender(filterGender)
                .status(filterStatus)
                .availableThemes(themes != null ? new HashSet<>(themes) : null)
                .build();
    }

    // ========== Mixed type combination test cases ==========

    @PostMapping("/rest/test/mixed-type")
    @Operation(
            summary = "Test mixed Java and protobuf types",
            description = "Tests combination of Java basic types with protobuf types")
    public MixedTypeRequest processMixedType(@RequestBody MixedTypeRequest request) {
        return request.toBuilder()
                .requestId(UUID.randomUUID().toString())
                .scheduledAt(LocalDateTime.now())
                .build();
    }

    @PostMapping("/rest/test/collection-wrapper")
    @Operation(
            summary = "Test Java collections with protobuf objects",
            description = "Tests Java collection types containing protobuf objects")
    public CollectionWrapper createCollectionWrapper(@RequestBody CollectionWrapper wrapper) {
        return wrapper.toBuilder().collectionId(UUID.randomUUID().toString()).build();
    }

    @GetMapping("/rest/test/collection-wrapper/{collectionId}")
    @Operation(
            summary = "Get collection with filtering",
            description = "Tests GET endpoint with complex protobuf enum filtering")
    public CollectionWrapper getCollectionWrapper(
            @PathVariable String collectionId,
            @RequestParam(required = false) List<User.Gender> filterGenders,
            @RequestParam(required = false) List<User.UserStatus> filterStatuses,
            @RequestParam(defaultValue = "10") Integer limit) {
        return CollectionWrapper.builder()
                .collectionId(collectionId)
                .availableGenders(filterGenders != null ? new HashSet<>(filterGenders) : null)
                .build();
    }

    @PostMapping("/rest/test/nested-complex")
    @Operation(
            summary = "Test deeply nested complex structures",
            description = "Tests deeply nested object structures with mixed types")
    public NestedComplexStructure createNestedComplex(@RequestBody NestedComplexStructure structure) {
        return structure.toBuilder()
                .structureId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @PatchMapping("/rest/test/nested-complex/{structureId}")
    @Operation(
            summary = "Partially update nested complex structure",
            description = "Tests PATCH operation on complex nested structures")
    public NestedComplexStructure updateNestedComplex(
            @PathVariable String structureId, @RequestBody NestedComplexStructure structure) {
        return structure.toBuilder()
                .structureId(structureId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== Protobuf message with Java field test cases ==========

    @PostMapping("/rest/test/protobuf-java-response")
    @Operation(
            summary = "Test protobuf message with Java fields",
            description = "Tests response containing both protobuf messages and Java types")
    public ProtobufJavaResponse createProtobufJavaResponse(@RequestBody User user) {
        CreateUserResponse protobufResponse = CreateUserResponse.newBuilder()
                .setUser(user)
                .setMessage("User processed successfully")
                .build();

        return ProtobufJavaResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .statusCode(200)
                .message("Success")
                .userData(user)
                .protobufResponse(protobufResponse)
                .processingCost(new BigDecimal("0.05"))
                .isSuccess(true)
                .build();
    }

    // ========== POJO Definitions ==========

    /**
     * Java POJO containing protobuf User message
     */
    @Builder(toBuilder = true)
    public record UserWrapper(
            String wrapperName, User user, LocalDateTime createdAt, boolean isActive, String description) {}

    /**
     * Java POJO containing protobuf PhoneNumber nested message
     */
    @Builder(toBuilder = true)
    public record ContactInfo(
            String contactId,
            User.PhoneNumber primaryPhone,
            List<User.PhoneNumber> additionalPhones,
            String notes,
            LocalDateTime lastUpdated) {}

    /**
     * Java POJO containing protobuf Address nested message
     */
    @Builder(toBuilder = true)
    public record LocationInfo(
            String locationId,
            User.Address address,
            Double latitude,
            Double longitude,
            Set<String> tags,
            LocalDateTime verifiedAt) {}

    /**
     * Java POJO containing protobuf enum types
     */
    @Builder(toBuilder = true)
    public record UserProfile(
            String profileId,
            String displayName,
            User.Gender gender,
            User.UserStatus status,
            User.Theme preferredTheme,
            Integer age,
            List<User.Gender> allowedGenders,
            Map<String, User.UserStatus> statusHistory,
            Set<User.Theme> availableThemes) {}

    /**
     * Combination of Java basic types and protobuf types
     */
    @Builder(toBuilder = true)
    public record MixedTypeRequest(
            String requestId,
            Integer priority,
            BigDecimal amount,
            Boolean isUrgent,
            User user,
            User.Gender preferredGender,
            User.UserStatus targetStatus,
            List<String> tags,
            Map<String, Object> metadata,
            LocalDateTime scheduledAt) {}

    /**
     * Java collection types containing protobuf objects
     */
    @Builder(toBuilder = true)
    public record CollectionWrapper(
            String collectionId,
            List<User> users,
            Set<User.Gender> availableGenders,
            Map<String, User> userMap,
            Map<User.Gender, List<User>> usersByGender,
            List<User.PhoneNumber> allPhoneNumbers,
            Map<User.UserStatus, Integer> statusCounts,
            Set<User.Address> uniqueAddresses) {}

    /**
     * Deeply nested complex object structure
     */
    @Builder(toBuilder = true)
    public record NestedComplexStructure(
            String structureId,
            UserWrapper userWrapper,
            List<ContactInfo> contactInfoList,
            Map<String, LocationInfo> locationMap,
            NestedLevel1 nestedLevel1,
            LocalDateTime createdAt) {}

    @Builder(toBuilder = true)
    public record NestedLevel1(
            String level1Id,
            User user,
            NestedLevel2 nestedLevel2,
            List<UserProfile> profiles,
            Map<User.Gender, CollectionWrapper> genderCollections) {}

    @Builder(toBuilder = true)
    public record NestedLevel2(
            String level2Id,
            Map<User.UserStatus, List<User>> usersByStatus,
            List<Map<String, User.PhoneNumber>> phoneNumberMaps,
            Set<User.Address> addressSet,
            Map<User.Theme, UserProfile> themeProfiles) {}

    /**
     * Response wrapper mixing protobuf and Java types
     */
    @Builder(toBuilder = true)
    public record ProtobufJavaResponse(
            String responseId,
            LocalDateTime timestamp,
            Integer statusCode,
            String message,
            User userData,
            CreateUserResponse protobufResponse,
            List<String> warnings,
            Map<String, Object> additionalData,
            BigDecimal processingCost,
            Boolean isSuccess) {}
}
