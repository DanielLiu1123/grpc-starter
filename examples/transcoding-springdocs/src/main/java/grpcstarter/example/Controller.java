package grpcstarter.example;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import test.TestBigMessageRequest;
import test.TestBigMessageResponse;
import test.TestBodyNotStarResponse;
import test.TestParameterResponse;
import test.User;

@RestController
public class Controller {

    @PostMapping("/hello")
    public User hello(@RequestBody User user) {
        return user;
    }

    // ========== Java Bean 嵌套 protobuf message 测试用例 ==========

    /**
     * Java POJO 包含 protobuf User 消息
     */
    @Data
    public static class UserWrapper {
        private String wrapperName;
        private User user;
        private LocalDateTime createdAt;
        private boolean isActive;
    }

    @PostMapping("/api/user-wrapper")
    public UserWrapper createUserWrapper(@RequestBody UserWrapper userWrapper) {
        return userWrapper;
    }

    /**
     * Java POJO 包含 protobuf PhoneNumber 嵌套消息
     */
    @Data
    public static class ContactInfo {
        private String contactId;
        private User.PhoneNumber primaryPhone;
        private List<User.PhoneNumber> additionalPhones;
        private String notes;
    }

    @PostMapping("/api/contact-info")
    public ContactInfo createContactInfo(@RequestBody ContactInfo contactInfo) {
        return contactInfo;
    }

    /**
     * Java POJO 包含 protobuf Address 嵌套消息
     */
    @Data
    public static class LocationInfo {
        private String locationId;
        private User.Address address;
        private Double latitude;
        private Double longitude;
        private Set<String> tags;
    }

    @PutMapping("/api/location/{locationId}")
    public LocationInfo updateLocation(@PathVariable String locationId, @RequestBody LocationInfo locationInfo) {
        locationInfo.setLocationId(locationId);
        return locationInfo;
    }

    // ========== Java Bean 嵌套 protobuf enum 测试用例 ==========

    /**
     * Java POJO 包含 protobuf Gender 枚举
     */
    @Data
    public static class UserProfile {
        private String profileId;
        private String displayName;
        private User.Gender gender;
        private Integer age;
        private List<User.Gender> allowedGenders;
        private Map<String, User.Gender> genderPreferences;
    }

    @PostMapping("/api/user-profile")
    public UserProfile createUserProfile(@RequestBody UserProfile userProfile) {
        return userProfile;
    }

    @GetMapping("/api/user-profile/{profileId}")
    public UserProfile getUserProfile(
            @PathVariable String profileId, @RequestParam(required = false) User.Gender filterGender) {
        UserProfile profile = new UserProfile();
        profile.setProfileId(profileId);
        profile.setGender(filterGender);
        return profile;
    }

    // ========== 混合类型组合测试用例 ==========

    /**
     * Java 基本类型与 protobuf 类型的组合
     */
    @Data
    public static class MixedTypeRequest {
        private String requestId;
        private Integer priority;
        private BigDecimal amount;
        private Boolean isUrgent;
        private User user;
        private User.Gender preferredGender;
        private List<String> tags;
        private Map<String, Object> metadata;
    }

    @PostMapping("/api/mixed-type")
    public MixedTypeRequest processMixedType(@RequestBody MixedTypeRequest request) {
        return request;
    }

    /**
     * Java 集合类型包含 protobuf 对象
     */
    @Data
    public static class CollectionWrapper {
        private String collectionId;
        private List<User> users;
        private Set<User.Gender> availableGenders;
        private Map<String, User> userMap;
        private Map<User.Gender, List<User>> usersByGender;
        private List<User.PhoneNumber> allPhoneNumbers;
    }

    @PostMapping("/api/collection-wrapper")
    public CollectionWrapper createCollectionWrapper(@RequestBody CollectionWrapper wrapper) {
        return wrapper;
    }

    @GetMapping("/api/collection-wrapper/{collectionId}")
    public CollectionWrapper getCollectionWrapper(
            @PathVariable String collectionId,
            @RequestParam(required = false) List<User.Gender> filterGenders,
            @RequestParam(defaultValue = "10") Integer limit) {
        CollectionWrapper wrapper = new CollectionWrapper();
        wrapper.setCollectionId(collectionId);
        wrapper.setAvailableGenders(filterGenders != null ? Set.copyOf(filterGenders) : null);
        return wrapper;
    }

    /**
     * 嵌套层级更深的复杂对象结构
     */
    @Data
    public static class NestedComplexStructure {
        private String structureId;
        private UserWrapper userWrapper;
        private List<ContactInfo> contactInfoList;
        private Map<String, LocationInfo> locationMap;
        private NestedLevel1 nestedLevel1;
    }

    @Data
    public static class NestedLevel1 {
        private String level1Id;
        private User user;
        private NestedLevel2 nestedLevel2;
        private List<UserProfile> profiles;
    }

    @Data
    public static class NestedLevel2 {
        private String level2Id;
        private Map<User.Gender, CollectionWrapper> genderCollections;
        private List<Map<String, User.PhoneNumber>> phoneNumberMaps;
    }

    @PostMapping("/api/nested-complex")
    public NestedComplexStructure createNestedComplex(@RequestBody NestedComplexStructure structure) {
        return structure;
    }

    @PatchMapping("/api/nested-complex/{structureId}")
    public NestedComplexStructure updateNestedComplex(
            @PathVariable String structureId, @RequestBody NestedComplexStructure structure) {
        structure.setStructureId(structureId);
        return structure;
    }

    // ========== 使用现有 protobuf 类型的测试用例 ==========

    /**
     * 测试 TestParameterRequest 和 TestParameterResponse
     */
    @GetMapping("/api/test-parameter/{pathString}/{pathInt}")
    public TestParameterResponse testParameter(
            @PathVariable String pathString,
            @PathVariable Integer pathInt,
            @RequestParam String queryString,
            @RequestParam(required = false) String queryStringOptional,
            @RequestParam Integer queryInt,
            @RequestParam(required = false) Integer queryIntOptional) {
        return TestParameterResponse.newBuilder().build();
    }

    /**
     * 测试 TestBigMessageRequest 和 TestBigMessageResponse
     */
    @PostMapping("/api/test-big-message")
    public TestBigMessageResponse testBigMessage(@RequestBody TestBigMessageRequest request) {
        return TestBigMessageResponse.newBuilder()
                .setUser(User.newBuilder()
                        .setId(123L)
                        .setName("Test User")
                        .setGender(User.Gender.MALE)
                        .build())
                .build();
    }

    /**
     * 测试 TestBodyNotStarRequest 和 TestBodyNotStarResponse
     */
    @PostMapping("/api/test-body-not-star")
    public TestBodyNotStarResponse testBodyNotStar(@RequestBody User userInfo) {
        return TestBodyNotStarResponse.newBuilder().build();
    }

    // ========== protobuf message 包含 Java 普通类型字段的测试用例 ==========

    /**
     * 混合 protobuf 和 Java 类型的响应包装器
     */
    @Data
    public static class ProtobufJavaResponse {
        private String responseId;
        private LocalDateTime timestamp;
        private Integer statusCode;
        private String message;
        private User userData;
        private TestBigMessageResponse protobufResponse;
        private List<String> warnings;
        private Map<String, Object> additionalData;
    }

    @PostMapping("/api/protobuf-java-response")
    public ProtobufJavaResponse createProtobufJavaResponse(@RequestBody User user) {
        ProtobufJavaResponse response = new ProtobufJavaResponse();
        response.setResponseId("resp-" + System.currentTimeMillis());
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(200);
        response.setMessage("Success");
        response.setUserData(user);
        response.setProtobufResponse(
                TestBigMessageResponse.newBuilder().setUser(user).build());
        return response;
    }

    /**
     * 批量操作的复杂类型
     */
    @Data
    public static class BatchOperation {
        private String batchId;
        private String operationType;
        private List<User> users;
        private List<TestBigMessageRequest> requests;
        private Map<String, TestParameterResponse> responses;
        private Set<User.Gender> targetGenders;
        private LocalDateTime scheduledTime;
        private Boolean isAsync;
    }

    @PostMapping("/api/batch-operation")
    public BatchOperation createBatchOperation(@RequestBody BatchOperation batchOperation) {
        return batchOperation;
    }

    @GetMapping("/api/batch-operation/{batchId}")
    public BatchOperation getBatchOperation(
            @PathVariable String batchId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) List<User.Gender> filterGenders) {
        BatchOperation operation = new BatchOperation();
        operation.setBatchId(batchId);
        operation.setOperationType(operationType);
        operation.setTargetGenders(filterGenders != null ? Set.copyOf(filterGenders) : null);
        return operation;
    }

    @PutMapping("/api/batch-operation/{batchId}/execute")
    public ProtobufJavaResponse executeBatchOperation(
            @PathVariable String batchId,
            @RequestBody BatchOperation operation,
            @RequestParam(defaultValue = "false") Boolean dryRun) {
        ProtobufJavaResponse response = new ProtobufJavaResponse();
        response.setResponseId("exec-" + batchId);
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(dryRun ? 202 : 200);
        response.setMessage(dryRun ? "Dry run completed" : "Execution completed");
        return response;
    }
}
